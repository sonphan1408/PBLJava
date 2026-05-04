package DAO;

import models.Transaction;
import utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class TransactionDAO {

    public boolean logTransaction(Transaction t) {
        String query = "INSERT INTO Transactions (TypeID, FromAccount, ReceiverAccountNumber, ReceiverBankCode, ATMID, Amount, BalanceBefore, BalanceAfter, Description, Status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, t.getTypeId());
            pstmt.setString(2, t.getFromAccount());
            pstmt.setString(3, t.getReceiverAccountNumber()); // Có thể NULL
            pstmt.setString(4, t.getReceiverBankCode());      // Có thể NULL
            pstmt.setString(5, t.getAtmId());                 // Có thể NULL
            pstmt.setDouble(6, t.getAmount());
            pstmt.setDouble(7, t.getBalanceBefore());
            pstmt.setDouble(8, t.getBalanceAfter());
            pstmt.setString(9, t.getDescription());
            pstmt.setString(10, t.getStatus());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}