package DAO;

import models.Account;
import utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AccountDAO {

    public Account getAccountByNumber(String accountNumber) {
        Account account = null;
        String query = "SELECT * FROM Accounts WHERE AccountNumber = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, accountNumber);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                account = new Account(
                        rs.getString("AccountNumber"),
                        rs.getInt("CustomerID"),
                        rs.getDouble("Balance"),
                        rs.getString("Status"),
                        rs.getTimestamp("CreatedAt")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return account;
    }

    public boolean updateBalance(String accountNumber, double newBalance) {
        String query = "UPDATE Accounts SET Balance = ? WHERE AccountNumber = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setDouble(1, newBalance);
            pstmt.setString(2, accountNumber);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}