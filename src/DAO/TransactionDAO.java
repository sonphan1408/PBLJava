package DAO;

import models.Transaction;
import utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {

    // Lấy danh sách giao dịch của một tài khoản (cả gửi và nhận)
    public List<Transaction> getTransactionsByAccount(String accountNumber, int limit) {
        List<Transaction> transactions = new ArrayList<>();
        String query = "SELECT * FROM Transactions WHERE FromAccount = ? OR ReceiverAccountNumber = ? ORDER BY CreatedAt DESC LIMIT ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, accountNumber);
            pstmt.setString(2, accountNumber);
            pstmt.setInt(3, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Transaction transaction = new Transaction(
                        rs.getInt("TransactionID"),
                        rs.getInt("TypeID"),
                        rs.getString("FromAccount"),
                        rs.getString("ReceiverAccountNumber"),
                        rs.getString("ReceiverBankCode"),
                        rs.getString("ATMID"),
                        rs.getDouble("Amount"),
                        rs.getDouble("BalanceBefore"),
                        rs.getDouble("BalanceAfter"),
                        rs.getString("Description"),
                        rs.getString("Status"),
                        rs.getTimestamp("CreatedAt")
                );
                transactions.add(transaction);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transactions;
    }

    // Lấy tất cả giao dịch của một tài khoản (cả gửi và nhận, không giới hạn)
    public List<Transaction> getAllTransactionsByAccount(String accountNumber) {
        List<Transaction> transactions = new ArrayList<>();
        String query = "SELECT * FROM Transactions WHERE FromAccount = ? OR ReceiverAccountNumber = ? ORDER BY CreatedAt DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, accountNumber);
            pstmt.setString(2, accountNumber);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Transaction transaction = new Transaction(
                        rs.getInt("TransactionID"),
                        rs.getInt("TypeID"),
                        rs.getString("FromAccount"),
                        rs.getString("ReceiverAccountNumber"),
                        rs.getString("ReceiverBankCode"),
                        rs.getString("ATMID"),
                        rs.getDouble("Amount"),
                        rs.getDouble("BalanceBefore"),
                        rs.getDouble("BalanceAfter"),
                        rs.getString("Description"),
                        rs.getString("Status"),
                        rs.getTimestamp("CreatedAt")
                );
                transactions.add(transaction);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transactions;
    }

    // Lưu giao dịch mới
    public boolean saveTransaction(int typeId, String fromAccount, String receiverAccountNumber,
                                   String receiverBankCode, String atmId, double amount,
                                   double balanceBefore, double balanceAfter, String description, String status) {
        String query = "INSERT INTO Transactions (TypeID, FromAccount, ReceiverAccountNumber, ReceiverBankCode, " +
                       "ATMID, Amount, BalanceBefore, BalanceAfter, Description, Status, CreatedAt) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, typeId);
            pstmt.setString(2, fromAccount);
            pstmt.setString(3, receiverAccountNumber);
            pstmt.setString(4, receiverBankCode);
            pstmt.setString(5, atmId);
            pstmt.setDouble(6, amount);
            pstmt.setDouble(7, balanceBefore);
            pstmt.setDouble(8, balanceAfter);
            pstmt.setString(9, description);
            pstmt.setString(10, status);

            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
