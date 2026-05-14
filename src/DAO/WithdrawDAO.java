package DAO;

import models.ATM;
import services.CashDispenserService;
import services.CashDispenserService.DispenseResult;
import utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WithdrawDAO {
    private static final Logger LOGGER = Logger.getLogger(WithdrawDAO.class.getName());
    private static final double MIN_BALANCE_VND = 50_000.0;
    private static final double DAILY_LIMIT_VND = 20_000_000;
    private static final String CURRENT_ATM_ID = "ATM001";

    private final CashDispenserService cashDispenserService = new CashDispenserService();

    public WithdrawResult executeWithdrawal(String accountNumber, double amount) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Khóa Account
            double balanceBefore;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT Balance FROM Accounts WHERE AccountNumber = ? AND Status = 'Active' FOR UPDATE")) {
                ps.setString(1, accountNumber);
                ps.setQueryTimeout(10);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return WithdrawResult.failure("Tài khoản không tồn tại hoặc đã bị khóa.");
                    }
                    balanceBefore = rs.getDouble("Balance");
                }
            }

            // 2. Validate số dư
            if (balanceBefore < amount + MIN_BALANCE_VND) {
                conn.rollback();
                return WithdrawResult.failure("Số dư không đủ! (Cần giữ lại 50.000 VNĐ tối thiểu)");
            }

            // 3. Check hạn mức ngày
            double todayTotal;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(SUM(Amount), 0) AS TodayTotal FROM Transactions " +
                    "WHERE FromAccount = ? AND TypeID = 1 AND Status = 'Success' AND DATE(CreatedAt) = CURDATE()")) {
                ps.setString(1, accountNumber);
                try (ResultSet rs = ps.executeQuery()) {
                    todayTotal = rs.next() ? rs.getDouble("TodayTotal") : 0;
                }
            }
            if (todayTotal + amount > DAILY_LIMIT_VND) {
                conn.rollback();
                return WithdrawResult.failure("Vượt hạn mức rút tiền trong ngày (20.000.000 VNĐ)!");
            }

            // 4. Khóa ATM và tính toán tiền nhả ra
            ATM atm;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT ATMID, Location, Count500k, Count200k, Count100k, Count50k, TotalCash, Status " +
                    "FROM ATMs WHERE ATMID = ? FOR UPDATE")) {
                ps.setString(1, CURRENT_ATM_ID);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return WithdrawResult.failure("Không tìm thấy thông tin máy ATM.");
                    }
                    atm = new ATM(rs.getString("ATMID"), rs.getString("Location"), rs.getInt("Count500k"),
                            rs.getInt("Count200k"), rs.getInt("Count100k"), rs.getInt("Count50k"),
                            rs.getDouble("TotalCash"), rs.getString("Status"));
                }
            }

            DispenseResult dispense = cashDispenserService.calculateAndDispense(atm, amount);
            if (!dispense.success) {
                conn.rollback();
                return WithdrawResult.failure(dispense.errorMessage);
            }

            double balanceAfter = balanceBefore - amount;

            // 5. Trừ tiền Account
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE Accounts SET Balance = Balance - ? WHERE AccountNumber = ?")) {
                ps.setDouble(1, amount);
                ps.setString(2, accountNumber);
                ps.executeUpdate();
            }

            // 6. Cập nhật số tờ tiền trong ATM
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE ATMs SET Count500k=?, Count200k=?, Count100k=?, Count50k=? WHERE ATMID=?")) {
                ps.setInt(1, atm.getCount500k());
                ps.setInt(2, atm.getCount200k());
                ps.setInt(3, atm.getCount100k());
                ps.setInt(4, atm.getCount50k());
                ps.setString(5, atm.getAtmId());
                ps.executeUpdate();
            }

            // 7. Ghi Log
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Transactions (TypeID, FromAccount, ATMID, Amount, BalanceBefore, BalanceAfter, Description, Status) " +
                    "VALUES (1, ?, ?, ?, ?, ?, 'Rút tiền mặt tại ATM', 'Success')")) {
                ps.setString(1, accountNumber);
                ps.setString(2, CURRENT_ATM_ID);
                ps.setDouble(3, amount);
                ps.setDouble(4, balanceBefore);
                ps.setDouble(5, balanceAfter);
                ps.executeUpdate();
            }

            conn.commit();
            return WithdrawResult.success(amount, balanceAfter, dispense);

        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) {}
            if ("40001".equals(e.getSQLState()) || e.getErrorCode() == 1205) {
                return WithdrawResult.failure("Hệ thống đang bận, vui lòng thử lại sau giây lát.");
            }
            LOGGER.log(Level.SEVERE, "SQL error", e);
            return WithdrawResult.failure("Lỗi hệ thống: " + e.getMessage());
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
        }
    }

    // Class chứa kết quả trả về
    public static class WithdrawResult {
        public final boolean success;
        public final double amount;
        public final double newBalance;
        public final DispenseResult dispense;
        public final String errorMessage;

        private WithdrawResult(boolean success, double amount, double newBalance, DispenseResult dispense, String errorMessage) {
            this.success = success; this.amount = amount; this.newBalance = newBalance;
            this.dispense = dispense; this.errorMessage = errorMessage;
        }
        public static WithdrawResult success(double amt, double bal, DispenseResult disp) { return new WithdrawResult(true, amt, bal, disp, null); }
        public static WithdrawResult failure(String msg) { return new WithdrawResult(false, 0, 0, null, msg); }
    }
}