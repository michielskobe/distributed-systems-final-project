package org.broker;

import org.springframework.web.bind.annotation.*;
import java.sql.*;

@RestController
@RequestMapping("/transaction_check")
public class ReservationStatusController {

    private static final String SQL_URL = "jdbc:mysql://dapp-broker-db.mysql.database.azure.com:3306/dapp-final-db?user=dapp&password=Nalu123456789!&useSSL=true&verifyServerCertificate=false";
    @GetMapping("/{reservationId}")
    public StatusResponse getReservationStatus(@PathVariable String reservationId) {
        String status = getOrderStatus(reservationId);
        int code = mapStatusToCode(status);
        return new StatusResponse(code);
    }

    private String getOrderStatus(String reservationId) {
        String sql = "SELECT status FROM orders WHERE reservation_id = ?";
        try (Connection conn = DriverManager.getConnection(SQL_URL);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reservationId);
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

    private int mapStatusToCode(String status) {
        return switch (status) {
            case "COMPLETED" -> 1; // Commited
            case "FAILED" -> 4; // RESERVATION aborted/rolled back
            default -> 0; // No decision yet
        };
    }

    public static class StatusResponse {
        public int status;
        public StatusResponse(int status) {
            this.status = status;
        }
    }
}
