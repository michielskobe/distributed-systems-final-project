import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;

import java.sql.*;

public class BrokerTest {

    private static final String SQL_URL = "jdbc:mysql://dapp-broker-db.mysql.database.azure.com:3306/dapp-final-db?user=dapp&password=Nalu123456789!&useSSL=true&verifyServerCertificate=false";
    private static final String QUEUE_NAME = "dappqueue";

    public static void main(String[] args) {
        /* Fill DB with test data */
        /*for (int i = 1; i <= 1000; i++) {
            long orderId = insertOrder();
            if (orderId != -1) {
                insertItem(orderId, 1, "1", 1); // LED
                insertItem(orderId, 2, "1", 1); // Bicycle
                insertItem(orderId, 3, "1", 1); // Battery
                System.out.println("Inserted order #" + i + " with ID: " + orderId);
            }
        }*/

        /* Fill Azure Storage Queue with order ID's */
        QueueClient queueClient = new QueueClientBuilder()
                .endpoint("https://storageaccountdapp.queue.core.windows.net/")
                .queueName(QUEUE_NAME)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();

        System.out.println("Creating queue: " + QUEUE_NAME);
        queueClient.createIfNotExists();

        for (int i = 1; i <= 1000; i++) {
            queueClient.sendMessage(String.valueOf(i));
            System.out.println("Inserted order ID" + i + "to Azure Storage Queue");
        }
    }

    private static long insertOrder() {
        String sql = "INSERT INTO orders (user, status) VALUES ('user.brokertest@example.com', 'NEW')";
        try (Connection conn = DriverManager.getConnection(SQL_URL);
            PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error inserting order: " + e.getMessage());
        }
        return -1;
    }

    private static void insertItem(long orderId, int supplierId, String productId, int amount) {
        String sql = "INSERT INTO order_items (order_id, supplier_id, product_id, amount) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(SQL_URL);
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, orderId);
            stmt.setInt(2, supplierId);
            stmt.setString(3, productId);
            stmt.setInt(4, amount);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error inserting item: " + e.getMessage());
        }
    }
}
