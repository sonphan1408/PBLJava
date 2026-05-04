package DAO;

import models.Card;
import utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CardDAO {

    // Lấy thông tin thẻ từ Database bằng Số thẻ
    public Card getCardByNumber(String cardNumber) {
        Card card = null;
        String query = "SELECT * FROM Cards WHERE CardNumber = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, cardNumber);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                card = new Card(
                        rs.getString("CardNumber"),
                        rs.getString("AccountNumber"),
                        rs.getString("PIN"),
                        rs.getInt("FailedAttempts"),
                        rs.getString("Status"),
                        rs.getDate("ExpiryDate")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return card;
    }

    // Cập nhật số lần nhập sai mã PIN và trạng thái (nếu cần khóa)
    public void updateFailedAttemptsAndStatus(String cardNumber, int attempts, String status) {
        String query = "UPDATE Cards SET FailedAttempts = ?, Status = ? WHERE CardNumber = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, attempts);
            pstmt.setString(2, status);
            pstmt.setString(3, cardNumber);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}