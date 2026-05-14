package controllers;

import DAO.InterbankTransferDAO;
import DAO.InterbankTransferDAO.BankEntry;
import DAO.InterbankTransferDAO.TransferResult;
import DAO.InvoiceDAO;
import DAO.InvoiceDAO.InvoiceData;
import utils.DBConnection;
import utils.SessionManager;
import views.InterbankTransferView;

import javax.swing.*;
import java.awt.*;
import java.awt.Desktop;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * =====================================================================
 * InterbankTransferController — Điều phối Chuyển khoản Liên ngân hàng
 * =====================================================================
 * Đóng vai trò trung gian kết nối InterbankTransferView ↔ InterbankTransferDAO.
 * Sử dụng SwingWorker cho MỌI thao tác truy xuất cơ sở dữ liệu nhằm giữ cho
 * luồng giao diện (EDT - Event Dispatch Thread) luôn mượt mà, không bị đơ.
 *
 * Vòng đời của một giao dịch hoàn chỉnh:
 *
 * [Mở màn hình]
 * → loadBankList()        (SwingWorker) Đổ danh sách ngân hàng vào JComboBox
 * → loadSenderInfo()      (SwingWorker) Hiển thị tên + số dư người gửi
 *
 * [Người dùng chọn ngân hàng, nhập STK, bấm "Tra cứu"]
 * → handleLookup()        (SwingWorker) Truy vấn bảng ExternalAccounts
 * ✓ Tìm thấy     → Hiển thị tên người nhận, mở khóa nút Chuyển khoản
 * ✗ Không thấy   → Báo lỗi, xóa trắng tên người nhận
 *
 * [Người dùng nhập số tiền, bấm "Chuyển khoản"]
 * → handleTransfer()
 * → Xác thực đầu vào (Chạy ngay trên luồng UI, không tốn DB)
 * → Hiển thị hộp thoại xác nhận
 * → executeTransfer()   (SwingWorker) → Gọi DAO.executeInterbankTransfer()
 * ✓ Thành công → Hiện biên lai, đề xuất "In hóa đơn"
 * ✗ Thất bại   → Báo lỗi chi tiết, mở khóa lại các nút bấm
 *
 * Đảm bảo an toàn đa luồng (Thừa hưởng từ DAO):
 * - Số dư người gửi được đọc an toàn qua lệnh SELECT ... FOR UPDATE
 * - Cả 3 câu lệnh cập nhật DB được gói gọn trong 1 Transaction duy nhất
 * - Lỗi Deadlock/Timeout được DAO tự động thử lại; UI hiển thị thông báo thân thiện
 */
public class InterbankTransferController {

    private static final Logger        LOGGER    = Logger.getLogger(InterbankTransferController.class.getName());
    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,###");

    private final InterbankTransferView view;
    private final InterbankTransferDAO  dao;
    private final InvoiceDAO            invoiceDAO;

    // Lưu trữ tên người nhận sau khi Tra cứu thành công — bắt buộc phải có khi bấm Chuyển khoản
    private String cachedReceiverName = null;

    // Lưu trữ dữ liệu hóa đơn sau khi chuyển thành công — dùng cho nút "In hóa đơn"
    private InvoiceData lastInvoice = null;

    // ── Hàm Khởi tạo (Constructor) ───────────────────────────────────────────
    public InterbankTransferController(InterbankTransferView view) {
        this.view       = view;
        this.dao        = new InterbankTransferDAO();
        this.invoiceDAO = new InvoiceDAO();

        wireListeners();
        loadBankList();    // Chạy bất đồng bộ (Background)
        loadSenderInfo();  // Chạy bất đồng bộ (Background)
    }

    // =====================================================================
    // KẾT NỐI SỰ KIỆN (WIRING)
    // =====================================================================

    private void wireListeners() {

        // Nút Tra cứu
        view.addLookupListener(e -> handleLookup());

        // Nút Chuyển khoản
        view.addTransferListener(e -> handleTransfer());

        // Nút Quay lại
        view.addCancelListener(e -> closeAndReturnToMenu());

        // Xóa tên người nhận mỗi khi người dùng đổi ngân hàng hoặc số tài khoản.
        // Điều này ngăn chặn việc người dùng dùng kết quả "tra cứu" cũ để chuyển tiền cho STK mới.
        view.addBankChangeListener(e -> {
            cachedReceiverName = null;
            view.clearReceiverName();
            view.clearStatus();
        });
    }

    private void loadBankList() {
        new SwingWorker<List<BankEntry>, Void>() {
            @Override
            protected List<BankEntry> doInBackground() {
                return dao.getAllBanks();
            }

            @Override
            protected void done() {
                try {
                    List<BankEntry> banks = get();
                    if (banks.isEmpty()) {
                        view.clearStatus();
                        view.showStatus("Không tải được danh sách ngân hàng!", view.colorDanger());
                    } else {
                        view.setBankList(banks);
                    }
                } catch (Exception e) {
                    view.clearStatus();
                    view.showStatus("Lỗi khi tải danh sách ngân hàng.", view.colorDanger());
                    LOGGER.log(Level.WARNING, "Failed to load bank list", e);
                }
            }
        }.execute();
    }

    private void loadSenderInfo() {
        String accountNo = SessionManager.getCurrentCard().getAccountNumber();

        new SwingWorker<String[], Void>() {
            @Override
            protected String[] doInBackground() throws Exception {
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement ps = conn.prepareStatement(
                             "SELECT c.FullName, a.Balance " +
                                     "FROM Accounts a " +
                                     "JOIN Customers c ON a.CustomerID = c.CustomerID " +
                                     "WHERE a.AccountNumber = ?")) {
                    ps.setString(1, accountNo);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return new String[]{
                                    rs.getString("FullName"),
                                    MONEY_FMT.format(rs.getDouble("Balance"))
                            };
                        }
                    }
                }
                return new String[]{"Không tìm thấy", "0"};
            }

            @Override
            protected void done() {
                try {
                    String[] info = get();
                    view.setSenderName(info[0]);
                    view.setSenderBalance(info[1]);
                } catch (Exception e) {
                    view.setSenderName("Lỗi tải thông tin");
                    view.setSenderBalance("--");
                    LOGGER.log(Level.WARNING, "Failed to load sender info", e);
                }
            }
        }.execute();
    }

    private void handleLookup() {
        BankEntry selectedBank = view.getSelectedBank();
        String    accNum       = view.getReceiverAccNum();

        if (selectedBank == null) {
            view.clearStatus();
            view.showStatus("Vui lòng chọn ngân hàng!", view.colorDanger());
            return;
        }
        if (accNum.isEmpty()) {
            view.clearStatus();
            view.showStatus("Vui lòng nhập số tài khoản người nhận!", view.colorDanger());
            return;
        }

        String myAccNo = SessionManager.getCurrentCard().getAccountNumber();
        if (accNum.equals(myAccNo) && selectedBank.bankCode.equals("ABC")) {
            view.clearStatus();
            view.showStatus("Không thể chuyển khoản cho chính mình!", view.colorDanger());
            return;
        }

        view.setButtonsEnabled(false);
        view.clearStatus();
        view.showStatus("Đang tra cứu tài khoản...", view.colorPrimary());
        cachedReceiverName = null; // Xóa bỏ kết quả cũ (nếu có)

        final String bankCode = selectedBank.bankCode;
        final String bankName = selectedBank.bankName;

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return dao.lookupExternalAccount(accNum, bankCode);
            }

            @Override
            protected void done() {
                view.setButtonsEnabled(true);
                try {
                    String name = get();
                    if (name != null) {
                        cachedReceiverName = name;
                        view.setReceiverName(name);
                        view.clearStatus();
                        view.showStatus(
                                "Tìm thấy tài khoản tại " + bankName + ". Vui lòng kiểm tra và nhập số tiền.",
                                view.colorSuccess());
                    } else {
                        cachedReceiverName = null;
                        view.setReceiverName(null); // Sẽ hiển thị "Không tìm thấy"
                        view.clearStatus();
                        view.showStatus(
                                "Không tìm thấy tài khoản " + accNum + " tại " + bankName + "!",
                                view.colorDanger());
                    }
                } catch (Exception e) {
                    cachedReceiverName = null;
                    view.clearStatus();
                    view.showStatus("Lỗi tra cứu: " + e.getMessage(), view.colorDanger());
                    LOGGER.log(Level.WARNING, "Lookup error", e);
                }
            }
        }.execute();
    }

    private void handleTransfer() {
        if (cachedReceiverName == null) {
            view.clearStatus();
            view.showStatus("Vui lòng tra cứu và xác nhận tài khoản người nhận trước!", view.colorDanger());
            return;
        }

        BankEntry selectedBank = view.getSelectedBank();
        String    accNum       = view.getReceiverAccNum();
        String    amountStr    = view.getAmountText();
        String    description  = view.getDescription();

        // ── Xác thực đầu vào (Trên luồng giao diện — Không gọi DB) ────────
        if (selectedBank == null || accNum.isEmpty()) {
            view.clearStatus();
            view.showStatus("Thông tin người nhận không hợp lệ. Vui lòng tra cứu lại!", view.colorDanger());
            return;
        }
        if (amountStr.isEmpty()) {
            view.clearStatus();
            view.showStatus("Vui lòng nhập số tiền cần chuyển!", view.colorDanger());
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException ex) {
            view.clearStatus();
            view.showStatus("Số tiền không hợp lệ! Vui lòng chỉ nhập chữ số.", view.colorDanger());
            return;
        }

        if (amount <= 0) {
            view.clearStatus();
            view.showStatus("Số tiền phải lớn hơn 0!", view.colorDanger()); return;
        }
        if (amount % 1000 != 0) {
            view.clearStatus();
            view.showStatus("Số tiền phải là bội số của 1.000 VNĐ!", view.colorDanger()); return;
        }
        if (amount > 50_000_000) {
            view.clearStatus();
            view.showStatus("Số tiền tối đa mỗi lần chuyển là 50.000.000 VNĐ!", view.colorDanger()); return;
        }

        // ── Hộp thoại xác nhận ────────────────────────────────────────────
        String confirmMsg = String.format(
                "Xác nhận chuyển khoản liên ngân hàng?\n\n" +
                        "  Ngân hàng nhận : %s\n" +
                        "  Số TK nhận     : %s\n" +
                        "  Tên người nhận : %s\n" +
                        "  Số tiền        : %s VNĐ\n" +
                        "  Nội dung       : %s",
                selectedBank.fullName, accNum,
                cachedReceiverName.toUpperCase(),
                MONEY_FMT.format(amount), description);

        int confirm = view.showConfirmDialog(confirmMsg, "Xác nhận giao dịch liên ngân hàng");
        if (confirm != JOptionPane.YES_OPTION) return;

        // ── Đẩy tác vụ chuyển khoản xuống chạy ngầm (Background thread) ────
        view.setButtonsEnabled(false);
        view.clearStatus();
        view.showStatus("Đang xử lý giao dịch...", view.colorPrimary());

        final double   finalAmount       = amount;
        final String   finalBankCode     = selectedBank.bankCode;
        final String   finalBankName     = selectedBank.bankName;
        final String   finalReceiverName = cachedReceiverName;
        final String   senderAccNo       = SessionManager.getCurrentCard().getAccountNumber();
        final String   senderName        = getSenderNameFromView();

        new SwingWorker<TransferResult, Void>() {
            @Override
            protected TransferResult doInBackground() {
                return dao.executeInterbankTransfer(
                        senderAccNo, senderName,
                        accNum, finalBankCode, finalBankName,
                        finalReceiverName,
                        finalAmount, description);
            }

            @Override
            protected void done() {
                view.setButtonsEnabled(true);
                try {
                    TransferResult result = get();

                    if (result.success) {
                        view.clearStatus();
                        view.showStatus(
                                "✔ Chuyển khoản thành công! Mã GD: " + result.txId,
                                view.colorSuccess());
                        // Làm mới lại số dư sau khi giao dịch thành công
                        loadSenderInfo();
                        // Tạo và hiển thị biên lai
                        lastInvoice = buildInvoiceData(result);
                        showSuccessReceipt(result);
                    } else {
                        view.clearStatus();
                        view.showStatus(result.errorMessage, view.colorDanger());
                    }

                } catch (Exception e) {
                    view.clearStatus();
                    view.showStatus("Lỗi không xác định: " + e.getMessage(), view.colorDanger());
                    LOGGER.log(Level.SEVERE, "Unexpected error in transfer worker", e);
                }
            }
        }.execute();
    }

    // =====================================================================
    // BIÊN LAI: Hiển thị sau khi chuyển khoản thành công
    // =====================================================================

    private void showSuccessReceipt(TransferResult result) {
        String receiptText = String.format(
                "╔══════════════════════════════════════════════╗\n"  +
                        "║       GIAO DỊCH THÀNH CÔNG                   ║\n"  +
                        "╚══════════════════════════════════════════════╝\n\n" +
                        "  Mã giao dịch   : %s\n"   +
                        "  Ngân hàng nhận : %s\n"   +
                        "  Số TK nhận     : %s\n"   +
                        "  Tên người nhận : %s\n\n" +
                        "  Số tiền đã CK  : %s VNĐ\n" +
                        "  Số dư trước GD : %s VNĐ\n" +
                        "  Số dư sau GD   : %s VNĐ\n\n" +
                        "  Nội dung       : %s",
                result.txId,
                result.bankName + " (" + result.bankCode + ")",
                result.externalAccNum,
                result.receiverName.toUpperCase(),
                MONEY_FMT.format(result.amount),
                MONEY_FMT.format(result.balanceBefore),
                MONEY_FMT.format(result.balanceAfter),
                result.description);

        // Tạo hộp thoại tùy chỉnh có kèm nút "In hóa đơn"
        JTextArea textArea = new JTextArea(receiptText);
        textArea.setFont(new Font("Courier New", Font.PLAIN, 12));
        textArea.setEditable(false);
        textArea.setBackground(new Color(245, 255, 245));
        textArea.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(480, 260));
        scrollPane.setBorder(null);

        JButton btnPrint = new JButton("🖨  In hóa đơn");
        btnPrint.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnPrint.setBackground(new Color(40, 167, 69));
        btnPrint.setForeground(Color.WHITE);
        btnPrint.setFocusPainted(false);

        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnRow.add(btnPrint);
        btnRow.add(btnClose);

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(view),
                "Kết quả giao dịch", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout(0, 10));
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(btnRow, BorderLayout.SOUTH);
        dialog.getRootPane().setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        dialog.pack();
        dialog.setLocationRelativeTo(view);

        btnClose.addActionListener(e -> dialog.dispose());

        btnPrint.addActionListener(e -> {
            if (lastInvoice != null) {
                String filePath = invoiceDAO.exportInvoiceToTxt(lastInvoice);
                if (filePath != null) {
                    try {
                        Desktop.getDesktop().open(new File(filePath));
                        view.clearStatus();
                        view.showStatus("✔ Hóa đơn đã lưu tại: " + filePath, view.colorSuccess());
                    } catch (Exception ex) {
                        view.showMessage("Hóa đơn đã lưu tại:\n" + filePath);
                    }
                } else {
                    view.showMessage("Xuất hóa đơn thất bại!");
                }
            }
            dialog.dispose();
        });

        dialog.setVisible(true);
    }

    // =====================================================================
    // CÁC HÀM HỖ TRỢ
    // =====================================================================

    /** Xây dựng dữ liệu InvoiceData cho công cụ in hóa đơn chung (InvoiceDAO) */
    private InvoiceData buildInvoiceData(TransferResult result) {
        InvoiceData inv = new InvoiceData();
        inv.transactionId     = result.txId;
        inv.senderAccountNo   = result.fromAccountNo;
        inv.senderName        = result.senderName;
        inv.receiverAccountNo = result.externalAccNum + " (" + result.bankName + ")";
        inv.receiverName      = result.receiverName;
        inv.amount            = result.amount;
        inv.content           = result.description;
        inv.balanceBefore     = result.balanceBefore;
        inv.balanceAfter      = result.balanceAfter;
        inv.transactionTime   = new Date();
        inv.status            = "THÀNH CÔNG";
        return inv;
    }

    /**
     * Lấy tên người gửi đã được tải lên nhãn giao diện trước đó.
     * Tránh việc phải query DB thêm một lần nữa khi thực thi chuyển khoản.
     */
    private String getSenderNameFromView() {
        // Nhãn lblSenderName trên View được gán qua setSenderName() (đã chuyển thành in hoa).
        // Ta dùng trực tiếp số tài khoản thẻ trong phiên đăng nhập làm fallback dự phòng.
        return SessionManager.getCurrentCard().getAccountNumber();
    }

    private void closeAndReturnToMenu() {
        // Tìm cửa sổ cha (JFrame hoặc JDialog) và đóng nó lại.
        // MainMenuController sẽ tự động hiển thị lại menu chính thông qua WindowListener.
        Window parent = SwingUtilities.getWindowAncestor(view);
        if (parent != null) parent.dispatchEvent(
                new java.awt.event.WindowEvent(parent, java.awt.event.WindowEvent.WINDOW_CLOSING));
    }
}