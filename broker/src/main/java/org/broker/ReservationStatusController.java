package org.broker;

import org.springframework.web.bind.annotation.*;
import java.sql.*;

@RestController
@RequestMapping("/reservation_status")
public class ReservationStatusController {

    private static final String SQL_URL = "jdbc:sqlserver://dapp-db.database.windows.net:1433;database=dapp-final-db;user=database@dapp-db;password=Nalu123456789!;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;";

    @GetMapping("/{reservationId}")
    public StatusResponse getReservationStatus(@PathVariable String reservationId) {
        String status = getOrderStatus(reservationId);
        return new StatusResponse(status);
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

    public static class StatusResponse {
        public String status;
        public StatusResponse(String status) {
            this.status = status;
        }
    }
}
