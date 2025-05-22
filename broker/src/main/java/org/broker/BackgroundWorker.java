package org.broker;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.queue.*;
import com.azure.storage.queue.models.*;
import java.util.List;

// Allow HTTP requests to supplier
import java.net.http.*;
import java.net.URI;

// JDBC imports for SQL communication
import java.sql.*;

// Jackson for JSON parsing
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BackgroundWorker {

    // Storage Queue configuration
    private static final String QUEUE_NAME = "dappqueue";
    private static final String QUEUE_ENDPOINT = "https://storageaccountdapp.queue.core.windows.net/";

    // Azure SQL DB configuration
    private static final String SQL_URL = "jdbc:sqlserver://dapp-db.database.windows.net:1433;database=dapp-final-db;user=database@dapp-db;password=Nalu123456789!;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;";

    private final QueueClient queueClient;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper; // Jackson instance

    public BackgroundWorker() {
        this.queueClient = new QueueClientBuilder()
                .endpoint(QUEUE_ENDPOINT)
                .queueName(QUEUE_NAME)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();

        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public void start() {
        System.out.println("Starting background worker for queue: " + QUEUE_NAME);

        // TODO: change polling loop to scheduled executor
        while (true) {
            queueClient.receiveMessages(10).forEach(
                    receivedMessage -> {
                        System.out.println("Received message: " + receivedMessage.getBody().toString());

                        boolean success = processOrder(receivedMessage.getBody().toString());
                        if (success) {
                            queueClient.deleteMessage(receivedMessage.getMessageId(), receivedMessage.getPopReceipt());
                        } else {
                            System.err.println("Order processing failed, message left in queue for retry.");
                        }
                    }
            );

            sleep(5000);
        }
    }

    /**
     * Process orders with suppliers via REST
     * @param message The queue message (JSON with order info, e.g. orderId + productIds)
     * @return true if order is successfully processed, false if rollback/error
     */
    private boolean processOrder(String message) {
        try {
            // 1. Parse JSON to extract orderId, etc.
            JsonNode jsonNode = objectMapper.readTree(message);
            String orderId = jsonNode.has("orderId") ? jsonNode.get("orderId").asText() : null;
            if (orderId == null) {
                System.err.println("Error: No orderId found in message: " + message);
                return false;
            }

            // (Optional) Extract more info from the JSON if needed

            // 2. Reserve with all suppliers
            boolean allReserved = true;
            for (String supplierUrl : getSupplierEndpoints()) {
                if (!reserveWithSupplier(supplierUrl, message)) {
                    allReserved = false;
                    break;
                }
            }
            if (!allReserved) {
                for (String supplierUrl : getSupplierEndpoints()) {
                    rollbackSupplier(supplierUrl, message);
                }
                updateOrderStatus(orderId, "FAILED");
                return false;
            }

            // 3. Commit with all suppliers
            boolean allCommitted = true;
            for (String supplierUrl : getSupplierEndpoints()) {
                if (!commitSupplier(supplierUrl, message)) {
                    allCommitted = false;
                    break;
                }
            }
            if (!allCommitted) {
                for (String supplierUrl : getSupplierEndpoints()) {
                    rollbackSupplier(supplierUrl, message);
                }
                updateOrderStatus(orderId, "FAILED");
                return false;
            }

            // 4. Update order status in database
            updateOrderStatus(orderId, "COMPLETED");
            return true;

        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    // === Helper-methods for HTTP-requests ===
    private boolean reserveWithSupplier(String supplierUrl, String order) {
        return postToSupplier(supplierUrl + "/reserve", order);
    }
    private boolean commitSupplier(String supplierUrl, String order) {
        return postToSupplier(supplierUrl + "/commit", order);
    }
    private boolean rollbackSupplier(String supplierUrl, String order) {
        return postToSupplier(supplierUrl + "/rollback", order);
    }
    private boolean postToSupplier(String url, String order) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(order))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception ex) {
            System.err.println("Error contacting supplier: " + url + " - " + ex.getMessage());
            return false;
        }
    }

    private List<String> getSupplierEndpoints() {
        // TODO: add and change supplier URLs
        return List.of(
                "https://supplier1.azurewebsites.net/api",
                "https://supplier2.azurewebsites.net/api"
        );
    }

    /**
     * Update the order status in Azure SQL database.
     * Expects a valid orderId.
     */
    private void updateOrderStatus(String orderId, String status) {
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";
        try (Connection conn = DriverManager.getConnection(SQL_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setString(2, orderId);
            int updated = stmt.executeUpdate();
            if (updated > 0) {
                System.out.printf("Order status updated to %s for order ID: %s\n", status, orderId);
            } else {
                System.err.printf("No rows updated for order ID: %s (status: %s)\n", orderId, status);
            }
        } catch (SQLException e) {
            System.err.println("Error updating order status in SQL DB: " + e.getMessage());
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    // Entry point
    public static void main(String[] args) {
        new BackgroundWorker().start();
    }
}