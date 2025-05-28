package org.broker;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.queue.*;
import com.azure.storage.queue.models.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.http.*;
import java.net.URI;
import java.sql.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class BackgroundWorker {

    // Storage Queue configuration
    // IMPORTANT: storage queue needs set up with 'az login' in terminal
    private static final String QUEUE_NAME = "dappqueue";
    private static final String QUEUE_ENDPOINT = "https://storageaccountdapp.queue.core.windows.net/";

    // Azure SQL DB configuration
    private static final String SQL_URL = "jdbc:sqlserver://dapp-db.database.windows.net:1433;database=dapp-final-db;user=database@dapp-db;password=Nalu123456789!;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;";

    // Supplier config
    private static final String SUPPLIER_API_KEY = "fa3b2c9c-a96d-48a8-82ad-0cb775dd3e5d";

    // Threading config
    private static final int THREAD_POOL_SIZE = 8;
    private static final int POLLING_INTERVAL_MS = 1000;

    private final QueueClient queueClient;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private final ScheduledExecutorService poller;
    private final ExecutorService workers;

    public BackgroundWorker() {
        // Initialize Azure Storage Queue
        this.queueClient = new QueueClientBuilder()
                .endpoint(QUEUE_ENDPOINT)
                .queueName(QUEUE_NAME)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
        this.queueClient.createIfNotExists();

        // Initialize broker's HttpClient and ObjectMapper
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();

        // Create thread pool
        this.workers = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        // Poller which periodically checks for messages
        this.poller = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        System.out.println("Starting background worker for queue: " + QUEUE_NAME);

        poller.scheduleAtFixedRate(() -> {
            try {
                Iterable<QueueMessageItem> messages = queueClient.receiveMessages(10);
                for (QueueMessageItem receivedMessage : messages) {
                    workers.submit(() -> processAndDelete(receivedMessage));
                }
            } catch (Exception ex) {
                System.err.println("Error polling queue: " + ex.getMessage());
            }
        }, 0, POLLING_INTERVAL_MS, TimeUnit.MILLISECONDS);

    }

    private void processAndDelete(QueueMessageItem receivedMessage) {
        String messageBody = receivedMessage.getBody().toString();
        System.out.println("Processing message: " + messageBody);

        boolean success = processOrder(messageBody);
        if (success) {
            queueClient.deleteMessage(receivedMessage.getMessageId(), receivedMessage.getPopReceipt());
            System.out.println("Message deleted: " + messageBody);
        } else {
            try {
                JsonNode jsonNode = objectMapper.readTree(messageBody);
                String orderId = jsonNode.has("orderId") ? jsonNode.get("orderId").asText() : null;

                if (orderId != null) {
                    String status = getOrderStatus(orderId);
                    if (status.equals("FAILED")) {
                        // It's terminal, delete message
                        queueClient.deleteMessage(receivedMessage.getMessageId(), receivedMessage.getPopReceipt());
                        System.out.printf("Order %s failed. Message deleted from queue.\n", orderId);
                    } else if (status.equals("PENDING")) {
                        // Do not delete — allow retry
                        System.out.printf("Order %s is still pending. Will retry.\n", orderId);
                    } else {
                        System.out.printf("Order %s is in unexpected status: %s. Will retry.\n", orderId, status);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Process orders with suppliers via REST.
     * 1. For each supplier, get the product id and amount from DB columns.
     * 2. Generate a UUID for reservation_id.
     * 3. Build a JSON payload: {"reservation_details":[{"id":"X", "amount":"Y"}], "reservation_id":"uuid"}
     * 4. Send to /reserve endpoint.
     */
    private boolean processOrder(String message) {
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            String orderId = jsonNode.has("orderId") ? jsonNode.get("orderId").asText() : null;

            if (orderId == null) {
                System.err.println("Error: No orderId found in message: " + message);
                return false;
            }

            if (!orderIdExistsInDatabase(orderId)) {
                System.err.println("Error: OrderId does not exist in database: " + orderId);
                return false;
            }

            if (getOrderStatus(orderId).equals("COMPLETED")) {
                System.out.println("Order already completed: " + orderId);
                return true;
            }

            updateOrderStatus(orderId, "PENDING");
            String reservationId = UUID.randomUUID().toString();
            if (reservationId == null) return false;
            updateReservationId(orderId, reservationId);

            List<String> supplierEndpoints = getSupplierEndpoints();

            boolean successfullyReserved = reserveFromAllSuppliers(orderId, reservationId, supplierEndpoints);
            if (!successfullyReserved) {
                rollbackAll(supplierEndpoints, reservationId);
                updateOrderStatus(orderId, "FAILED");
                return false;
            }

            boolean successfullyCommitted = commitAllSuppliers(supplierEndpoints, reservationId);
            if (!successfullyCommitted) {
                rollbackAll(supplierEndpoints, reservationId);
                updateOrderStatus(orderId, "FAILED");
                return false;
            }

            updateOrderStatus(orderId, "COMPLETED");
            return true;

        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    // === Helper-methods for distributed commit ===
    private boolean reserveFromAllSuppliers(String orderId, String reservationId, List<String> supplierEndpoints) {
        try {
            int supplierNumber = 1;
            for (String supplierUrl : supplierEndpoints) {
                SupplierProductInfo info = getSupplierProductInfo(orderId, supplierNumber);
                if (info == null) {
                    System.err.printf("Aborting transaction %s", reservationId);
                    System.err.printf("No product info for order %s and supplier %d\n", orderId, supplierNumber);
                    return false;
                }

                String payload = createReservationPayload(info, reservationId);
                SupplierResponse response = reserveWithSupplier(supplierUrl, payload);

                if (!response.ok) {
                    System.err.printf("Aborting transaction %s", reservationId);
                    System.err.printf("Reserve NOK for supplier %s. Response: %s\n", supplierUrl, response.body);
                    return false;
                }
                supplierNumber++;
            }
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private String createReservationPayload(SupplierProductInfo info, String reservationId) throws JsonProcessingException {
        ObjectNode detail = objectMapper.createObjectNode();
        detail.put("id", info.productId);
        detail.put("amount", String.valueOf(info.amount));

        ObjectNode payload = objectMapper.createObjectNode();
        payload.putArray("reservation_details").add(detail);
        payload.put("reservation_id", reservationId);
        // TODO: Fix callbackUrl
        String callbackUrl = "http://localhost:8080/reservation_status/" + reservationId;
        payload.put("callback", callbackUrl);

        return objectMapper.writeValueAsString(payload);
    }

    private boolean commitAllSuppliers(List<String> supplierEndpoints, String reservationId) {
        try {
            for (String supplierUrl : supplierEndpoints) {
                String payload = createCommitPayload(reservationId);
                SupplierResponse response = commitSupplier(supplierUrl, payload);

                if (!response.ok) {
                    System.err.printf("Aborting transaction %s", reservationId);
                    System.err.printf("Commit NOK for supplier %s. Response: %s\n", supplierUrl, response.body);
                    return false;
                }
            }
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private String createCommitPayload(String reservationId) throws JsonProcessingException {
        ObjectNode commitDetail = objectMapper.createObjectNode();
        commitDetail.put("reservation_id", reservationId);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.putArray("commit_details").add(commitDetail);

        return objectMapper.writeValueAsString(payload);
    }

    private void rollbackAll(List<String> supplierEndpoints, String reservationId) {
        for (String supplierUrl : supplierEndpoints) {
            rollbackSupplierWithReservationId(supplierUrl, reservationId);
        }
    }

    // === Helper-class for product info ===
    private static class SupplierProductInfo {
        String productId;
        int amount;
        SupplierProductInfo(String productId, int amount) {
            this.productId = productId;
            this.amount = amount;
        }
    }

    // === Helper-class for supplier response ===
    private static class SupplierResponse {
        boolean ok;
        String body;
        int statusCode;

        SupplierResponse(boolean ok, String body, int statusCode) {
            this.ok = ok;
            this.body = body;
            this.statusCode = statusCode;
        }
    }

    private SupplierProductInfo getSupplierProductInfo(String orderId, int supplierNumber) {
        String idCol = "supplier_" + supplierNumber + "_id";
        String amountCol = "supplier_" + supplierNumber + "_amount";
        String sql = String.format("SELECT %s, %s FROM orders WHERE id = ?", idCol, amountCol);
        try (Connection conn = DriverManager.getConnection(SQL_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String productId = rs.getString(idCol);
                    int amount = rs.getInt(amountCol);
                    if (productId != null && !productId.isEmpty() && amount > 0) {
                        return new SupplierProductInfo(productId, amount);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching supplier product info: " + e.getMessage());
        }
        return null;
    }

    private void rollbackSupplierWithReservationId(String supplierUrl, String reservationId) {
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            ObjectNode rollbackDetail = objectMapper.createObjectNode();
            rollbackDetail.put("reservation_id", reservationId);
            payload.putArray("rollback_details").add(rollbackDetail);
            String payloadStr = objectMapper.writeValueAsString(payload);
            SupplierResponse response = rollbackSupplier(supplierUrl, payloadStr);
            if (!response.ok) {
                System.err.printf("Rollback NOK for supplier %s. Response: %s\n", supplierUrl, response.body);
            }
        } catch (Exception e) {
            System.err.println("Error building rollback payload: " + e.getMessage());
        }
    }

    // === Helper-methods for HTTP-requests ===
    private SupplierResponse reserveWithSupplier(String supplierUrl, String orderPayload) {
        return postToSupplier(supplierUrl + "/reserve", orderPayload);
    }
    private SupplierResponse commitSupplier(String supplierUrl, String orderPayload) {
        return postToSupplier(supplierUrl + "/commit", orderPayload);
    }
    private SupplierResponse rollbackSupplier(String supplierUrl, String orderPayload) {
        return postToSupplier(supplierUrl + "/rollback_reserve", orderPayload);
    }

    /**
     * Returns SupplierResponse.ok = true only if:
     * - HTTP response is 200
     * - The JSON body contains {"status": "OK"}
     */
    private SupplierResponse postToSupplier(String url, String payload) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("x-api-key", SUPPLIER_API_KEY)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            boolean isOk = false;
            String body = response.body();
            int code = response.statusCode();

            if (code == 200) {
                try {
                    JsonNode respJson = objectMapper.readTree(body);
                    String status = respJson.has("status") ? respJson.get("status").asText() : "";
                    if ("OK".equalsIgnoreCase(status)) {
                        isOk = true;
                    } else {
                        System.err.printf("Supplier at %s responded with status: %s, body: %s\n", url, status, body);
                    }
                } catch (Exception ex) {
                    System.err.printf("Invalid JSON response from supplier at %s: %s\n", url, body);
                }
            } else {
                System.err.printf("HTTP error from supplier at %s: %d, body: %s\n", url, code, body);
            }
            return new SupplierResponse(isOk, body, code);
        } catch (Exception ex) {
            System.err.println("Error contacting supplier: " + url + " - " + ex.getMessage());
            return new SupplierResponse(false, ex.getMessage(), -1);
        }
    }

    private List<String> getSupplierEndpoints() {
        return List.of(
                "https://rgbeast.francecentral.cloudapp.azure.com/RGBeast",
                "https://crank-wankers.francecentral.cloudapp.azure.com/Crank-Wankers",
                "https://battery-bastards.francecentral.cloudapp.azure.com/Battery-Bastards"
        );
    }

    private String getOrderStatus(String orderId) {
        String sql = "SELECT status FROM orders WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(SQL_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("status");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error reading order status: " + e.getMessage());
        }
        return "UNKNOWN";
    }

    private void updateOrderStatus(String orderId, String newStatus) {
        String oldStatus = getOrderStatus(orderId);
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(SQL_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus);
            stmt.setString(2, orderId);
            int updated = stmt.executeUpdate();
            if (updated > 0) {
                System.out.printf("Order status updated from %s to %s for order ID: %s\n", oldStatus, newStatus, orderId);
            } else {
                System.err.printf("No rows updated for order ID: %s (status: %s)\n", orderId, newStatus);
            }
        } catch (SQLException e) {
            System.err.println("Error updating order status in SQL DB: " + e.getMessage());
        }
    }

    private void updateReservationId(String orderId, String reservationId) {
        String sql = "UPDATE orders SET reservation_id = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(SQL_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reservationId);
            stmt.setString(2, orderId);
            int updated = stmt.executeUpdate();
            if (updated > 0) {
                System.out.printf("Reservation id updated to %s for order ID: %s\n", reservationId, orderId);
            } else {
                System.err.printf("No rows updated for order ID: %s\n", orderId);
            }
        } catch (SQLException e) {
            System.err.println("Error updating reservation id in SQL DB: " + e.getMessage());
        }
    }

    private boolean orderIdExistsInDatabase(String orderId) {
        String sql = "SELECT COUNT(*) FROM orders WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(SQL_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    return count > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking orderId in database: " + e.getMessage());
        }
        return false;
    }

    public static void main(String[] args) {
        new BackgroundWorker().start();
    }
}