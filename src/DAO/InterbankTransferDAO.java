package DAO;

import utils.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * =====================================================================
 * InterbankTransferDAO — Xử lý CSDL cho Chuyển khoản Liên ngân hàng
 * =====================================================================
 * Chịu trách nhiệm cho toàn bộ thao tác cơ sở dữ liệu của tính năng này.
 * Cung cấp 3 phương thức chính để Controller gọi:
 *
 * 1. getAllBanks()              — Lấy danh sách ngân hàng để đổ vào JComboBox
 * 2. lookupExternalAccount()    — Tra cứu tên người nhận từ ngân hàng ngoài
 * 3. executeInterbankTransfer() — Thực thi chuyển tiền nguyên tử (khóa dòng + rollback)
 *
 * An toàn đa luồng (Concurrency safety):
 * - Hàm executeInterbankTransfer sử dụng SELECT ... FOR UPDATE trên dòng
 * tài khoản của người gửi, giành được khóa cấp độ dòng (row-level lock)
 * trước khi tính toán số dư. Kỹ thuật này đồng bộ với WithdrawController
 * và TransactionController để chống lỗi Lost Update (mất mát dữ liệu).
 * - Cả 3 bảng (Accounts, ExternalAccounts, Transactions) được cập nhật
 * bên trong cùng một Transaction duy nhất → Đảm bảo Commit toàn vẹn hoặc Rollback toàn bộ.
 */
public class InterbankTransferDAO {

    private static final Logger LOGGER = Logger.getLogger(InterbankTransferDAO.class.getName());

    // ── Hằng số TypeID (khớp với bảng TransactionTypes trong CSDL) ─────────
    private static final int TYPE_INTERBANK_TRANSFER = 3;

    // ── Số dư tối thiểu người gửi phải giữ lại sau khi chuyển ───────────────
    private static final double MIN_BALANCE_VND = 50_000.0;

    // =====================================================================
    // 1. Lấy danh sách ngân hàng liên kết
    // =====================================================================

    /**
     * Lấy toàn bộ danh sách ngân hàng ngoài để hiển thị lên JComboBox.
     * Trả về một danh sách rỗng (không bao giờ trả về null) nếu có lỗi CSDL,
     * giúp giao diện không bị sập (tránh lỗi NullPointerException).
     *
     * @return Danh sách các đối tượng BankEntry được sắp xếp theo Tên ngân hàng
     */
    public List<BankEntry> getAllBanks() {
        List<BankEntry> banks = new ArrayList<>();
        String sql = "SELECT BankCode, BankName, FullName FROM ExternalBanks ORDER BY BankName";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                banks.add(new BankEntry(
                        rs.getString("BankCode"),
                        rs.getString("BankName"),
                        rs.getString("FullName")
                ));
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi tải danh sách ngân hàng", e);
        }
        return banks;
    }

    // =====================================================================
    // 2. Tra cứu tài khoản ngân hàng ngoài
    // =====================================================================

    /**
     * Tra cứu tài khoản ngân hàng ngoài dựa trên Số tài khoản VÀ Mã ngân hàng.
     * Bắt buộc phải có cả hai trường — vì số tài khoản không phải là duy nhất
     * trên toàn hệ thống (VD: VCB và BIDV đều có thể có STK "0041000123456").
     *
     * @param externalAccNum  Số tài khoản do người dùng nhập vào
     * @param bankCode        Mã ngân hàng (BankCode) được chọn từ JComboBox
     * @return Tên chủ tài khoản (AccountHolder) nếu tìm thấy, null nếu không có
     * @throws SQLException Ném lỗi ra ngoài để SwingWorker của Controller có thể
     * phân biệt giữa lỗi CSDL và việc "không tìm thấy"
     */
    public String lookupExternalAccount(String externalAccNum, String bankCode) throws SQLException {
        String sql = "SELECT AccountHolder FROM ExternalAccounts " +
                     "WHERE ExternalAccNum = ? AND BankCode = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, externalAccNum);
            ps.setString(2, bankCode);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("AccountHolder") : null;
            }
        }
    }

    // =====================================================================
    // 3. Thực thi chuyển khoản liên ngân hàng
    // =====================================================================

    /**
     * Thực thi toàn bộ quá trình chuyển khoản dưới dạng một Transaction nguyên tử.
     *
     * Trình tự thực thi (Tất cả nằm trong một Transaction):
     * Bước 1 — Khóa tài khoản gửi:    SELECT ... FOR UPDATE trên bảng Accounts
     * Bước 2 — Kiểm tra số dư:        Kiểm tra SAU KHI khóa (Chống lỗi TOCTOU)
     * Bước 3 — Xác thực người nhận:   Kiểm tra lại xem tài khoản đích còn tồn tại không
     * Bước 4 — Trừ tiền người gửi:    UPDATE Accounts SET Balance = Balance - amount
     * Bước 5 — Cộng tiền người nhận:  UPDATE ExternalAccounts SET Balance = Balance + amount
     * Bước 6 — Ghi log giao dịch:     INSERT INTO Transactions (TypeID=3, có lưu ReceiverBankCode)
     * Bước 7 — COMMIT (hoặc ROLLBACK nếu có bất kỳ ngoại lệ nào xảy ra)
     *
     * @param fromAccountNo   Số tài khoản nội bộ của người gửi
     * @param senderName      Tên người gửi (dùng để in hóa đơn/log)
     * @param externalAccNum  Số tài khoản của người nhận bên ngoài
     * @param bankCode        Mã ngân hàng nhận (VD: "VCB")
     * @param bankName        Tên hiển thị của ngân hàng nhận (dùng để in hóa đơn)
     * @param receiverName    Tên người nhận (đã được lấy từ bước tra cứu trước đó)
     * @param amount          Số tiền chuyển (VNĐ)
     * @param description     Nội dung chuyển khoản
     * @return TransferResult — Kết quả thành công kèm dữ liệu in biên lai, hoặc thất bại kèm lý do
     */
    public TransferResult executeInterbankTransfer(
            String fromAccountNo,
            String senderName,
            String externalAccNum,
            String bankCode,
            String bankName,
            String receiverName,
            double amount,
            String description) {

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Bắt đầu Transaction

            // ── BƯỚC 1: Khóa dòng tài khoản người gửi (SELECT ... FOR UPDATE) ──────────
            // Giành được khóa độc quyền cấp độ dòng. Mọi luồng khác đang cố gắng
            // trừ tiền tài khoản này sẽ bị chặn (block) cho đến khi ta commit.
            double balanceBefore;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT Balance FROM Accounts " +
                    "WHERE AccountNumber = ? AND Status = 'Active' FOR UPDATE")) {
                ps.setString(1, fromAccountNo);
                ps.setQueryTimeout(10);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return TransferResult.failure("Tài khoản nguồn không tồn tại hoặc đã bị khóa.");
                    }
                    balanceBefore = rs.getDouble("Balance");
                }
            }

            // ── BƯỚC 2: Kiểm tra số dư SAU KHI đã giành được khóa ────────
            // Việc kiểm tra trước khi khóa sẽ gây ra lỗi Time-Of-Check-Time-Of-Use (TOCTOU).
            // Ta chỉ kiểm tra trên giá trị "sống" đã được khóa an toàn.
            if (balanceBefore < amount + MIN_BALANCE_VND) {
                conn.rollback();
                return TransferResult.failure(String.format(
                        "Số dư không đủ!\nSố dư hiện tại: %,.0f VNĐ\n" +
                        "Cần chuyển: %,.0f VNĐ + Số dư tối thiểu: %,.0f VNĐ",
                        balanceBefore, amount, MIN_BALANCE_VND));
            }

            // ── BƯỚC 3: Xác thực lại người nhận bên trong Transaction ─────────
            // Người dùng đã "Tra cứu" trước đó, nhưng tài khoản có thể đã bị xóa
            // trong khoảng thời gian đó. Ta cần xác thực lại một lần nữa.
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT AccountHolder FROM ExternalAccounts " +
                    "WHERE ExternalAccNum = ? AND BankCode = ?")) {
                ps.setString(1, externalAccNum);
                ps.setString(2, bankCode);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return TransferResult.failure(
                                "Tài khoản người nhận không còn tồn tại trong hệ thống ngân hàng " + bankName + ".");
                    }
                }
            }

            double balanceAfter = balanceBefore - amount;

            // ── BƯỚC 4: Trừ tiền người gửi (Bảng Accounts nội bộ) ───────────
            // Sử dụng "Balance = Balance - ?" thay vì set một con số tuyệt đối
            // để đảm bảo an toàn tối đa trong môi trường đa luồng.
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE Accounts SET Balance = Balance - ? WHERE AccountNumber = ?")) {
                ps.setDouble(1, amount);
                ps.setString(2, fromAccountNo);
                int rows = ps.executeUpdate();
                if (rows != 1) {
                    conn.rollback();
                    return TransferResult.failure("Lỗi cập nhật số dư tài khoản người gửi.");
                }
            }

            // ── BƯỚC 5: Cộng tiền người nhận (Bảng ExternalAccounts) ─────────
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE ExternalAccounts SET Balance = Balance + ? " +
                    "WHERE ExternalAccNum = ? AND BankCode = ?")) {
                ps.setDouble(1, amount);
                ps.setString(2, externalAccNum);
                ps.setString(3, bankCode);
                int rows = ps.executeUpdate();
                if (rows != 1) {
                    conn.rollback();
                    return TransferResult.failure("Lỗi cập nhật số dư tài khoản người nhận.");
                }
            }

            // ── BƯỚC 6: Ghi log giao dịch ──────────────────────────────
            // TypeID = 3 cho chuyển khoản liên ngân hàng. ReceiverBankCode được gán
            // để phân biệt với chuyển khoản nội bộ (TypeID = 2).
            String generatedTxId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO Transactions " +
                    "(TypeID, FromAccount, ReceiverAccountNumber, ReceiverBankCode, ATMID, " +
                    " Amount, BalanceBefore, BalanceAfter, Description, Status) " +
                    "VALUES (?, ?, ?, ?, NULL, ?, ?, ?, ?, 'Success')",
                    PreparedStatement.RETURN_GENERATED_KEYS)) {

                ps.setInt(1, TYPE_INTERBANK_TRANSFER);
                ps.setString(2, fromAccountNo);
                ps.setString(3, externalAccNum);
                ps.setString(4, bankCode);          // Trường quan trọng: Mã ngân hàng nhận
                ps.setDouble(5, amount);
                ps.setDouble(6, balanceBefore);
                ps.setDouble(7, balanceAfter);
                ps.setString(8, description);
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    generatedTxId = keys.next() ? String.valueOf(keys.getInt(1)) : "N/A";
                }
            }

            // ── BƯỚC 7: Commit — Giải phóng tất cả các khóa FOR UPDATE ───────────
            conn.commit();

            LOGGER.info(String.format(
                "[InterbankDAO] THÀNH CÔNG: %s → %s@%s | Số tiền=%,.0f | Mã GD=%s",
                fromAccountNo, externalAccNum, bankCode, amount, generatedTxId));

            return TransferResult.success(
                    generatedTxId, balanceBefore, balanceAfter,
                    fromAccountNo, senderName,
                    externalAccNum, bankCode, bankName, receiverName,
                    amount, description);

        } catch (SQLException e) {
            // Luôn Rollback nếu có bất kỳ ngoại lệ SQL nào xảy ra
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) {
                    LOGGER.log(Level.SEVERE, "NGHIÊM TRỌNG: Quá trình Rollback thất bại!", ex);
                }
            }
            LOGGER.log(Level.SEVERE, "Lỗi SQL trong quá trình chuyển khoản liên ngân hàng", e);

            // SQLState 40001 = Deadlock; errorCode 1205 = Hết thời gian chờ khóa (Lock wait timeout)
            if ("40001".equals(e.getSQLState()) || e.getErrorCode() == 1205) {
                return TransferResult.failure("Hệ thống đang bận, vui lòng thử lại sau giây lát.");
            }
            return TransferResult.failure("Lỗi cơ sở dữ liệu: " + e.getMessage());

        } finally {
            // Luôn khôi phục autoCommit và đóng kết nối — ngay cả khi có lỗi
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); }
                catch (SQLException ignored) {}
            }
        }
    }

    // =====================================================================
    // Các lớp đối tượng trả về (Value Objects) cho Controller
    // =====================================================================

    /**
     * Đại diện cho một dòng dữ liệu từ bảng ExternalBanks.
     * Dùng để đổ dữ liệu vào JComboBox — phương thức toString() sẽ trả về tên hiển thị.
     */
    public static class BankEntry {
        public final String bankCode;
        public final String bankName;
        public final String fullName;

        public BankEntry(String bankCode, String bankName, String fullName) {
            this.bankCode = bankCode;
            this.bankName = bankName;
            this.fullName = fullName;
        }

        /** Chuỗi được hiển thị trên danh sách thả xuống của JComboBox */
        @Override
        public String toString() {
            return bankName + " (" + bankCode + ")";
        }
    }

    /**
     * Kết quả của hàm executeInterbankTransfer.
     * Khi thành công: chứa toàn bộ dữ liệu giao dịch để in hóa đơn.
     * Khi thất bại: chứa thông báo lỗi chi tiết cho người dùng.
     */
    public static class TransferResult {
        public final boolean success;
        public final String  errorMessage;

        // Các trường dùng cho biên lai (Chỉ có giá trị khi success = true)
        public final String  txId;
        public final double  balanceBefore;
        public final double  balanceAfter;
        public final String  fromAccountNo;
        public final String  senderName;
        public final String  externalAccNum;
        public final String  bankCode;
        public final String  bankName;
        public final String  receiverName;
        public final double  amount;
        public final String  description;

        private TransferResult(boolean success, String errorMessage,
                               String txId, double balanceBefore, double balanceAfter,
                               String fromAccountNo, String senderName,
                               String externalAccNum, String bankCode, String bankName,
                               String receiverName, double amount, String description) {
            this.success        = success;
            this.errorMessage   = errorMessage;
            this.txId           = txId;
            this.balanceBefore  = balanceBefore;
            this.balanceAfter   = balanceAfter;
            this.fromAccountNo  = fromAccountNo;
            this.senderName     = senderName;
            this.externalAccNum = externalAccNum;
            this.bankCode       = bankCode;
            this.bankName       = bankName;
            this.receiverName   = receiverName;
            this.amount         = amount;
            this.description    = description;
        }

        public static TransferResult success(
                String txId, double balanceBefore, double balanceAfter,
                String fromAccountNo, String senderName,
                String externalAccNum, String bankCode, String bankName,
                String receiverName, double amount, String description) {
            return new TransferResult(true, null, txId, balanceBefore, balanceAfter,
                    fromAccountNo, senderName, externalAccNum, bankCode, bankName,
                    receiverName, amount, description);
        }

        public static TransferResult failure(String message) {
            return new TransferResult(false, message,
                    null, 0, 0, null, null, null, null, null, null, 0, null);
        }
    }
}