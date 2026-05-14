package DAO;

import utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TransferDAO {

    private static final Logger logger = Logger.getLogger(TransferDAO.class.getName());

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long BASE_BACKOFF_MS = 100;

    // =========================================================
    // Cập nhật SQL khớp 100% với Database ATMSimulation của bạn
    // =========================================================

    private static final String SQL_LOCK_AND_READ =
            "SELECT AccountNumber, Balance, Status FROM Accounts " +
            "WHERE AccountNumber = ? AND Status = 'Active' " +
            "FOR UPDATE";

    private static final String SQL_UPDATE_BALANCE =
            "UPDATE Accounts SET Balance = Balance + ? WHERE AccountNumber = ?";

    private static final String SQL_LOG_TRANSACTION =
            "INSERT INTO Transactions " +
            "(TypeID, FromAccount, ReceiverAccountNumber, Amount, BalanceBefore, BalanceAfter, Description, Status) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, 'Success')";


    // =========================================================
    // Public API: Trả về String[] {Mã_GD, Số_Dư_Mới} cho Controller
    // =========================================================

    public String[] transfer(String fromAccountId, String toAccountId, double amount, String description)
            throws TransferException {

        // Validate cơ bản
        if (fromAccountId == null || toAccountId == null) {
            throw new TransferException("Số tài khoản không được để trống.", TransferException.ErrorCode.INVALID_INPUT);
        }
        if (fromAccountId.equals(toAccountId)) {
            throw new TransferException("Không thể tự chuyển tiền cho chính mình.", TransferException.ErrorCode.INVALID_INPUT);
        }
        if (amount <= 0) {
            throw new TransferException("Số tiền chuyển phải lớn hơn 0.", TransferException.ErrorCode.INVALID_INPUT);
        }

        int attempt = 0;
        while (attempt < MAX_RETRY_ATTEMPTS) {
            attempt++;
            try {
                // Gọi hàm xử lý và hứng mảng String[] trả về
                String[] result = attemptTransfer(fromAccountId, toAccountId, amount, description);
                
                logger.info(String.format("Transfer SUCCESS: %s → %s | Amount: %.0f | Attempt: %d",
                        fromAccountId, toAccountId, amount, attempt));
                        
                return result; // Trả về cho Controller đi in hóa đơn

            } catch (DeadlockException e) {
                if (attempt >= MAX_RETRY_ATTEMPTS) {
                    logger.severe(String.format("Transfer FAILED after %d attempts (deadlock): %s → %s",
                            MAX_RETRY_ATTEMPTS, fromAccountId, toAccountId));
                    throw new TransferException(
                            "Hệ thống đang quá tải giao dịch. Vui lòng thử lại sau.",
                            TransferException.ErrorCode.DEADLOCK_EXHAUSTED);
                }

                long waitMs = BASE_BACKOFF_MS * (1L << (attempt - 1));
                logger.warning(String.format("Deadlock on attempt %d. Retrying in %dms...", attempt, waitMs));
                try {
                    Thread.sleep(waitMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new TransferException("Giao dịch bị gián đoạn.", TransferException.ErrorCode.INTERRUPTED);
                }
            }
        }
        return null;
    }


    // =========================================================
    // Core Transfer Logic: Trả về String[]
    // =========================================================

    private String[] attemptTransfer(String fromAccountId, String toAccountId,
                                 double amount, String description)
            throws TransferException, DeadlockException {

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

            // Ép thứ tự khóa (Canonical Ordering) chống Deadlock
            String firstLockId  = fromAccountId.compareTo(toAccountId) < 0 ? fromAccountId : toAccountId;
            String secondLockId = fromAccountId.compareTo(toAccountId) < 0 ? toAccountId   : fromAccountId;

            AccountSnapshot firstAccount  = lockAndReadAccount(conn, firstLockId);
            AccountSnapshot secondAccount = lockAndReadAccount(conn, secondLockId);

            AccountSnapshot sender   = fromAccountId.equals(firstLockId) ? firstAccount : secondAccount;
            AccountSnapshot receiver = fromAccountId.equals(firstLockId) ? secondAccount : firstAccount;

            // Kiểm tra số dư của người gửi
            if (sender.balance < amount) {
                conn.rollback();
                throw new TransferException(
                        String.format("Tài khoản không đủ số dư. Hiện có: %,.0f VNĐ", sender.balance),
                        TransferException.ErrorCode.INSUFFICIENT_FUNDS);
            }

            double senderBalanceBefore = sender.balance;
            double newSenderBalance    = senderBalanceBefore - amount;

            // Cộng / Trừ tiền nguyên tử
            updateBalance(conn, fromAccountId, -amount);
            updateBalance(conn, toAccountId,   +amount);

            // Ghi log giao dịch (TypeID = 2 cho Chuyển khoản nội bộ)
            // Đồng thời lấy ID giao dịch (txId) vừa được tự động sinh ra trong DB
            String txId = logTransaction(conn, 2, fromAccountId, toAccountId, amount, 
                                         senderBalanceBefore, newSenderBalance, description);

            conn.commit();

            // Đóng gói trả về cho Controller {Mã_GD, Số_Dư_Mới}
            return new String[]{txId, String.valueOf(newSenderBalance)};

        } catch (SQLException e) {
            rollbackSilently(conn);
            if ("40001".equals(e.getSQLState())) {
                throw new DeadlockException("Deadlock detected.", e);
            }
            if ("HY000".equals(e.getSQLState()) && e.getErrorCode() == 1205) {
                throw new DeadlockException("Lock wait timeout.", e);
            }

            logger.log(Level.SEVERE, "Lỗi SQL khi chuyển khoản", e);
            throw new TransferException("Lỗi CSDL: " + e.getMessage(),
                    TransferException.ErrorCode.DATABASE_ERROR);

        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }


    // =========================================================
    // Private Helpers
    // =========================================================

    private AccountSnapshot lockAndReadAccount(Connection conn, String accountId)
            throws SQLException, TransferException {

        try (PreparedStatement ps = conn.prepareStatement(SQL_LOCK_AND_READ)) {
            ps.setString(1, accountId);
            ps.setQueryTimeout(10);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new TransferException(
                            "Không tìm thấy tài khoản hoặc tài khoản bị khóa: " + accountId,
                            TransferException.ErrorCode.ACCOUNT_NOT_FOUND);
                }
                AccountSnapshot snap = new AccountSnapshot();
                snap.accountId = rs.getString("AccountNumber");
                snap.balance   = rs.getDouble("Balance");
                snap.status    = rs.getString("Status");
                return snap;
            }
        }
    }

    private void updateBalance(Connection conn, String accountId, double delta) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_BALANCE)) {
            ps.setDouble(1, delta);
            ps.setString(2, accountId);
            ps.executeUpdate();
        }
    }

    private String logTransaction(Connection conn, int typeId, String fromAccount, String toAccount, 
                                  double amount, double balanceBefore, double balanceAfter, 
                                  String description) throws SQLException {
        
        // Dùng Statement.RETURN_GENERATED_KEYS để lấy ID tự tăng (TransactionID)
        try (PreparedStatement ps = conn.prepareStatement(SQL_LOG_TRANSACTION, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, typeId);
            ps.setString(2, fromAccount);
            ps.setString(3, toAccount);
            ps.setDouble(4, amount);
            ps.setDouble(5, balanceBefore);
            ps.setDouble(6, balanceAfter);
            ps.setString(7, description);
            ps.executeUpdate();
            
            // Lấy ID vừa được insert
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return String.valueOf(rs.getInt(1)); // Trả về TransactionID
                }
            }
        }
        return "TXN-" + System.currentTimeMillis(); // Fallback nếu DB không hỗ trợ
    }

    private void rollbackSilently(Connection conn) {
        if (conn != null) {
            try { conn.rollback(); } catch (SQLException e) {
                logger.log(Level.SEVERE, "Lỗi Rollback!", e);
            }
        }
    }

    // =========================================================
    // Inner Classes
    // =========================================================

    private static class AccountSnapshot {
        String accountId;
        double balance;
        String status;
    }

    private static class DeadlockException extends Exception {
        DeadlockException(String message, Throwable cause) { super(message, cause); }
    }

    public static class TransferException extends Exception {
        public enum ErrorCode {
            INVALID_INPUT, ACCOUNT_NOT_FOUND, INSUFFICIENT_FUNDS, DEADLOCK_EXHAUSTED, INTERRUPTED, DATABASE_ERROR
        }
        private final ErrorCode errorCode;
        TransferException(String message, ErrorCode errorCode) {
            super(message);
            this.errorCode = errorCode;
        }
        public ErrorCode getErrorCode() { return errorCode; }
    }
}