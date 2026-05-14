package controllers;

import DAO.TransferDAO; // Import DAO mới
import DAO.InvoiceDAO;
import DAO.InvoiceDAO.InvoiceData;
import utils.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TransactionController extends JPanel {

    private static final Logger LOGGER = Logger.getLogger(TransactionController.class.getName());

    // ── UI Constants ────────────────────────────────────────────────────────
    private static final Color PRIMARY    = new Color(0, 86, 163);
    private static final Color SUCCESS    = new Color(34, 139, 34);
    private static final Color DANGER     = new Color(200, 30, 30);
    private static final Color BG         = new Color(245, 247, 250);
    private static final Color WHITE      = Color.WHITE;
    private static final Font  TITLE_FONT = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font  LABEL_FONT = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font  FIELD_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,###");

    // ── UI Components ────────────────────────────────────────────────────────
    private JTextField txtSenderAccount;
    private JLabel     lblSenderName;
    private JLabel     lblSenderBalance;
    private JTextField txtReceiverAccount;
    private JLabel     lblReceiverName;
    private JButton    btnLookup;
    private JTextField txtAmount;
    private JTextField txtContent;
    private JButton    btnTransfer;
    private JButton    btnPrintInvoice;
    private JButton    btnReset;
    private JLabel     lblStatus;

    // ── State ────────────────────────────────────────────────────────────────
    private final String     loggedInAccountNo;
    private       InvoiceData lastInvoice;
    private final InvoiceDAO  invoiceDAO = new InvoiceDAO();
    private final TransferDAO transferDAO = new TransferDAO(); // Khởi tạo DAO

    public TransactionController(String accountNo) {
        this.loggedInAccountNo = accountNo;
        setLayout(new BorderLayout(0, 0));
        setBackground(BG);
        setBorder(new EmptyBorder(24, 32, 24, 32));
        initComponents();
        resetForm(); // Tải thông tin ban đầu
    }

    // [Các hàm initComponents, buildSenderPanel, buildReceiverPanel, buildBottomPanel giữ nguyên như cũ...]
    // (Vì phần UI của bạn đã làm rất tốt, mình tập trung sửa phần xử lý logic bên dưới)

    private void initComponents() {
        JLabel title = new JLabel("CHUYỂN KHOẢN NỘI BỘ", SwingConstants.CENTER);
        title.setFont(TITLE_FONT);
        title.setForeground(PRIMARY);
        title.setBorder(new EmptyBorder(0, 0, 20, 0));
        add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 24, 0));
        center.setBackground(BG);
        center.add(buildSenderPanel());
        center.add(buildReceiverPanel());
        add(center, BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildSenderPanel() {
        JPanel panel = createCard("Thông tin người gửi");
        lblSenderName    = new JLabel("...");
        lblSenderBalance = new JLabel("...");
        txtSenderAccount = new JTextField(loggedInAccountNo);
        txtSenderAccount.setEditable(false);
        txtSenderAccount.setBackground(new Color(235, 235, 235));
        lblSenderName.setFont(FIELD_FONT);
        lblSenderBalance.setFont(FIELD_FONT);
        lblSenderBalance.setForeground(new Color(0, 120, 0));
        addRow(panel, "Số tài khoản:", txtSenderAccount);
        addRow(panel, "Chủ tài khoản:", lblSenderName);
        addRow(panel, "Số dư hiện tại:", lblSenderBalance);
        return panel;
    }

    private JPanel buildReceiverPanel() {
        JPanel panel = createCard("Thông tin chuyển khoản");
        txtReceiverAccount = new JTextField();
        txtReceiverAccount.setFont(FIELD_FONT);
        lblReceiverName = new JLabel("(Nhập STK rồi bấm Tra cứu)");
        lblReceiverName.setFont(FIELD_FONT);
        lblReceiverName.setForeground(Color.GRAY);
        btnLookup = new JButton("Tra cứu");
        btnLookup.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnLookup.setBackground(PRIMARY);
        btnLookup.setForeground(WHITE);
        btnLookup.addActionListener(e -> lookupReceiver());
        JPanel rowReceiver = new JPanel(new BorderLayout(8, 0));
        rowReceiver.setBackground(WHITE);
        rowReceiver.add(txtReceiverAccount, BorderLayout.CENTER);
        rowReceiver.add(btnLookup, BorderLayout.EAST);
        txtAmount  = new JTextField();
        txtContent = new JTextField("Chuyen khoan");
        txtAmount.setFont(FIELD_FONT);
        txtContent.setFont(FIELD_FONT);
        addRowCustom(panel, "Số tài khoản nhận:", rowReceiver);
        addRow(panel, "Tên người nhận:", lblReceiverName);
        addRow(panel, "Số tiền (VNĐ):", txtAmount);
        addRow(panel, "Nội dung:", txtContent);
        return panel;
    }

    private JPanel buildBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(20, 0, 0, 0));
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        btnPanel.setBackground(BG);
        btnTransfer     = createButton("Chuyển khoản", new Color(0, 120, 215));
        btnPrintInvoice = createButton("🖨  In hóa đơn", new Color(40, 167, 69));
        btnReset        = createButton("Làm mới", new Color(108, 117, 125));
        btnPrintInvoice.setVisible(false);
        btnTransfer.addActionListener(e -> executeTransfer());
        btnPrintInvoice.addActionListener(e -> printInvoice());
        btnReset.addActionListener(e -> resetForm());
        btnPanel.add(btnTransfer);
        btnPanel.add(btnPrintInvoice);
        btnPanel.add(btnReset);
        lblStatus = new JLabel(" ", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblStatus.setOpaque(true);
        lblStatus.setBackground(BG);
        panel.add(btnPanel, BorderLayout.NORTH);
        panel.add(lblStatus, BorderLayout.SOUTH);
        return panel;
    }

    private void lookupReceiver() {
        String receiverNo = txtReceiverAccount.getText().trim();
        if (receiverNo.isEmpty() || receiverNo.equals(loggedInAccountNo)) {
            showStatus("Số tài khoản nhận không hợp lệ!", DANGER);
            return;
        }

        btnLookup.setEnabled(false);
        showStatus("Đang tra cứu...", PRIMARY);

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                // Bạn có thể giữ lại hàm dbLookupReceiver cũ hoặc chuyển nó vào AccountDAO
                return dbLookupReceiver(receiverNo);
            }

            @Override
            protected void done() {
                btnLookup.setEnabled(true);
                try {
                    String name = get();
                    if (name != null) {
                        lblReceiverName.setText(name.toUpperCase());
                        lblReceiverName.setForeground(new Color(0, 100, 0));
                        showStatus("Tìm thấy tài khoản.", PRIMARY);
                    } else {
                        lblReceiverName.setText("Không tìm thấy tài khoản");
                        lblReceiverName.setForeground(DANGER);
                        showStatus("Tài khoản không tồn tại!", DANGER);
                    }
                } catch (Exception e) {
                    showStatus("Lỗi tra cứu: " + e.getMessage(), DANGER);
                }
            }
        }.execute();
    }

    /**
     * HÀM QUAN TRỌNG NHẤT: Đã được sửa để gọi TransferDAO
     */
    private void executeTransfer() {
        String receiverNo   = txtReceiverAccount.getText().trim();
        String amountStr    = txtAmount.getText().trim().replaceAll("[,.]", "");
        String content      = txtContent.getText().trim();
        String receiverName = lblReceiverName.getText();

        // Validate cơ bản trên UI
        if (receiverNo.isEmpty() || amountStr.isEmpty()) {
            showStatus("Vui lòng điền đầy đủ thông tin!", DANGER); return;
        }

        double amount;
        try { amount = Double.parseDouble(amountStr); } 
        catch (NumberFormatException ex) { showStatus("Số tiền không hợp lệ!", DANGER); return; }

        int confirm = JOptionPane.showConfirmDialog(this,
                String.format("Xác nhận chuyển %s VNĐ tới %s?", MONEY_FMT.format(amount), receiverName),
                "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        setTransferButtonsEnabled(false);
        showStatus("Đang xử lý...", PRIMARY);

        new SwingWorker<TransferResult, Void>() {
            @Override
            protected TransferResult doInBackground() {
                try {
                    // GỌI THẲNG DAO: DAO đã lo liệu SELECT FOR UPDATE, Check số dư, Deadlock Retry
                    String[] result = transferDAO.transfer(loggedInAccountNo, receiverNo, amount, content);
                    
                    String txId = result[0];
                    double newBalance = Double.parseDouble(result[1]);

                    // Tạo dữ liệu hóa đơn ngay tại đây
                    InvoiceData invoice = new InvoiceData();
                    invoice.transactionId = txId;
                    invoice.senderAccountNo = loggedInAccountNo;
                    invoice.senderName = lblSenderName.getText();
                    invoice.receiverAccountNo = receiverNo;
                    invoice.receiverName = receiverName;
                    invoice.amount = amount;
                    invoice.content = content;
                    invoice.balanceAfter = newBalance;
                    invoice.transactionTime = new Date();

                    return TransferResult.success(txId, newBalance, invoice);

                } catch (TransferDAO.TransferException e) {
                    // Bắt các lỗi nghiệp vụ từ DAO
                    return TransferResult.failure(e.getMessage());
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Unexpected error", e);
                    return TransferResult.failure("Lỗi hệ thống: " + e.getMessage());
                }
            }

            @Override
            protected void done() {
                setTransferButtonsEnabled(true);
                try {
                    TransferResult result = get();
                    if (result.success) {
                        lblSenderBalance.setText(MONEY_FMT.format(result.newSenderBalance) + " VNĐ");
                        showStatus("✔ Thành công! Mã GD: " + result.txId, SUCCESS);
                        lastInvoice = result.invoice;
                        btnPrintInvoice.setVisible(true);
                    } else {
                        showStatus(result.errorMessage, DANGER);
                    }
                } catch (Exception e) {
                    showStatus("Lỗi: " + e.getMessage(), DANGER);
                }
            }
        }.execute();
    }

    // Các hàm phụ trợ SQL đơn giản phục vụ hiển thị (Lookup, LoadInfo)
    private String dbLookupReceiver(String receiverNo) throws java.sql.SQLException {
        try (java.sql.Connection conn = utils.DBConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(
                     "SELECT FullName FROM Accounts a JOIN Customers c ON a.CustomerID = c.CustomerID WHERE AccountNumber = ?")) {
            ps.setString(1, receiverNo);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("FullName") : null;
            }
        }
    }

    private void loadSenderInfo() {
        new SwingWorker<String[], Void>() {
            @Override
            protected String[] doInBackground() throws Exception {
                try (java.sql.Connection conn = utils.DBConnection.getConnection();
                     java.sql.PreparedStatement ps = conn.prepareStatement(
                             "SELECT c.FullName, a.Balance FROM Accounts a JOIN Customers c ON a.CustomerID = c.CustomerID WHERE a.AccountNumber = ?")) {
                    ps.setString(1, loggedInAccountNo);
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return new String[]{rs.getString("FullName"), MONEY_FMT.format(rs.getDouble("Balance"))};
                        }
                    }
                }
                return new String[]{"N/A", "0"};
            }
            @Override
            protected void done() {
                try {
                    String[] res = get();
                    lblSenderName.setText(res[0].toUpperCase());
                    lblSenderBalance.setText(res[1] + " VNĐ");
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    // [Các hàm trợ giúp UI như createButton, showStatus, resetForm... giữ nguyên]

    private void resetForm() {
        txtReceiverAccount.setText("");
        lblReceiverName.setText("(Nhập STK rồi bấm Tra cứu)");
        txtAmount.setText("");
        lblStatus.setText(" ");
        btnPrintInvoice.setVisible(false);
        loadSenderInfo();
    }

    private void setTransferButtonsEnabled(boolean enabled) {
        btnTransfer.setEnabled(enabled);
        btnReset.setEnabled(enabled);
        btnLookup.setEnabled(enabled);
    }

    private void showStatus(String message, Color color) {
        lblStatus.setText(message);
        lblStatus.setForeground(color);
    }

    private JPanel createCard(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(PRIMARY), title));
        return panel;
    }

    private void addRow(JPanel panel, String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(WHITE);
        row.add(new JLabel(label), BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        panel.add(row);
    }

    private void addRowCustom(JPanel panel, String label, JPanel field) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(WHITE);
        row.add(new JLabel(label), BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        panel.add(row);
    }

    private JButton createButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(WHITE);
        btn.setFocusPainted(false);
        return btn;
    }

    private void printInvoice() {
        if (lastInvoice != null) {
            String path = invoiceDAO.exportInvoiceToTxt(lastInvoice);
            JOptionPane.showMessageDialog(this, "Hóa đơn đã được lưu tại: " + path);
        }
    }

    private static class TransferResult {
        final boolean success;
        final String txId;
        final double newSenderBalance;
        final InvoiceData invoice;
        final String errorMessage;

        private TransferResult(boolean success, String txId, double newBalance, InvoiceData invoice, String error) {
            this.success = success; this.txId = txId; this.newSenderBalance = newBalance;
            this.invoice = invoice; this.errorMessage = error;
        }
        static TransferResult success(String id, double bal, InvoiceData inv) { return new TransferResult(true, id, bal, inv, null); }
        static TransferResult failure(String msg) { return new TransferResult(false, null, 0, null, msg); }
    }
}