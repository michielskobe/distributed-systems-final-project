package org.broker;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.queue.*;
import com.azure.storage.queue.models.*;
import java.util.List;
import java.util.concurrent.*;
import java.net.http.*;
import java.net.URI;
import java.sql.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BackgroundWorker {

    // Storage Queue configuration
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
        this.queueClient = new QueueClientBuilder()
                .endpoint(QUEUE_ENDPOINT)
                .queueName(QUEUE_NAME)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();

        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();

        this.queueClient.createIfNotExists();

        // Create threadpool
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
        } else {
            System.err.println("Order processing failed, message left in queue for retry.");
        }
    }

    /**
     * Process orders with suppliers via REST
     * @param message The queue message (JSON with order info, e.g. orderId + productIds)
     * @return true if order is successfully processed, false if rollback/error
     */
    private boolean processOrder(String message) {
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            String orderId = jsonNode.has("orderId") ? jsonNode.get("orderId").asText() : null;
            if (orderId == null) {
                System.err.println("Error: No orderId found in message: " + message);
                return false;
            }

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
        return postToSupplier(supplierUrl + "/rollback_reserve", order);
    }
    private boolean postToSupplier(String url, String order) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("x-api-key", SUPPLIER_API_KEY)
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
        return List.of(
                "https://rgbeast.francecentral.cloudapp.azure.com/RGBeast",
                "https://crank-wankers.francecentral.cloudapp.azure.com/Crank-Wankers"
        );
    }

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

    public static void main(String[] args) {
        new BackgroundWorker().start();
    }
}