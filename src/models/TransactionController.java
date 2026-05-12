package controllers;

import DAO.InvoiceDAO;
import DAO.InvoiceDAO.InvoiceData;
import utils.DBConnection;
import utils.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.Desktop;
import java.io.File;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.Date;

public class TransactionController extends JPanel {
    private static final Color PRIMARY   = new Color(0, 86, 163);  
    private static final Color SUCCESS   = new Color(34, 139, 34);
    private static final Color DANGER    = new Color(200, 30, 30);
    private static final Color BG        = new Color(245, 247, 250);
    private static final Color WHITE     = Color.WHITE;
    private static final Font  TITLE_FONT = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font  LABEL_FONT = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font  FIELD_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,###");

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

    private String       loggedInAccountNo;  
    private InvoiceData  lastInvoice;        
    private Connection   dbConnection;       
    private final InvoiceDAO invoiceDAO = new InvoiceDAO();
    public TransactionController(String accountNo, Connection connection) {
        this.loggedInAccountNo = accountNo;
        this.dbConnection      = connection;

        setLayout(new BorderLayout(0, 0));
        setBackground(BG);
        setBorder(new EmptyBorder(24, 32, 24, 32));

        initComponents();
        loadSenderInfo();
    }


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
        btnLookup.setFocusPainted(false);
        btnLookup.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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

        btnTransfer = createButton("Chuyển khoản", new Color(0, 120, 215));
        btnPrintInvoice = createButton("🖨  In hóa đơn", new Color(40, 167, 69));
        btnReset = createButton("Làm mới", new Color(108, 117, 125));

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

    private void loadSenderInfo() {
        try {
            String sql = """
                    SELECT c.FullName, a.Balance
                    FROM Accounts a
                    JOIN Customers c ON a.CustomerID = c.CustomerID
                    WHERE a.AccountNumber = ?
                    """;
            PreparedStatement ps = dbConnection.prepareStatement(sql);
            ps.setString(1, loggedInAccountNo);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                lblSenderName.setText(rs.getString("FullName").toUpperCase());
                lblSenderBalance.setText(MONEY_FMT.format(rs.getDouble("Balance")) + " VNĐ");
            }
            rs.close(); ps.close();
        } catch (SQLException e) {
            showStatus("Lỗi tải thông tin tài khoản: " + e.getMessage(), DANGER);
        }
    }

    private void lookupReceiver() {
        String receiverNo = txtReceiverAccount.getText().trim();
        if (receiverNo.isEmpty()) {
            showStatus("Vui lòng nhập số tài khoản người nhận!", DANGER);
            return;
        }
        if (receiverNo.equals(loggedInAccountNo)) {
            showStatus("Không thể chuyển khoản cho chính mình!", DANGER);
            return;
        }

        try {
            String sql = """
                    SELECT c.FullName
                    FROM Accounts a
                    JOIN Customers c ON a.CustomerID = c.CustomerID
                    WHERE a.AccountNumber = ? AND a.Status = 'Active'
                    """;
            PreparedStatement ps = dbConnection.prepareStatement(sql);
            ps.setString(1, receiverNo);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                lblReceiverName.setText(rs.getString("FullName").toUpperCase());
                lblReceiverName.setForeground(new Color(0, 100, 0));
                showStatus("Tìm thấy tài khoản người nhận.", PRIMARY);
            } else {
                lblReceiverName.setText("Không tìm thấy tài khoản");
                lblReceiverName.setForeground(DANGER);
                showStatus("Số tài khoản không tồn tại hoặc đã bị khóa!", DANGER);
            }
            rs.close(); ps.close();
        } catch (SQLException e) {
            showStatus("Lỗi tra cứu: " + e.getMessage(), DANGER);
        }
    }

    private void executeTransfer() {
        String receiverNo   = txtReceiverAccount.getText().trim();
        String amountStr    = txtAmount.getText().trim().replaceAll("[,.]", "");
        String content      = txtContent.getText().trim();
        String receiverName = lblReceiverName.getText();

        if (receiverNo.isEmpty()) {
            showStatus("Vui lòng nhập số tài khoản người nhận!", DANGER); return;
        }
        if (receiverName.startsWith("(") || receiverName.equals("Không tìm thấy tài khoản")) {
            showStatus("Vui lòng tra cứu tài khoản người nhận hợp lệ!", DANGER); return;
        }
        if (amountStr.isEmpty()) {
            showStatus("Vui lòng nhập số tiền!", DANGER); return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException ex) {
            showStatus("Số tiền không hợp lệ!", DANGER); return;
        }
        if (amount <= 0) {
            showStatus("Số tiền phải lớn hơn 0!", DANGER); return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                String.format("Xác nhận chuyển %s VNĐ\nTới tài khoản: %s - %s?",
                        MONEY_FMT.format(amount), receiverNo, receiverName),
                "Xác nhận giao dịch",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        doDbTransfer(receiverNo, receiverName, amount, content);
    }
    private void doDbTransfer(String receiverNo, String receiverName,
                               double amount, String content) {
        try {
            dbConnection.setAutoCommit(false);

            double balanceBefore = getBalance(loggedInAccountNo);
            if (balanceBefore < amount) {
                showStatus("Số dư không đủ để thực hiện giao dịch!", DANGER);
                dbConnection.rollback();
                return;
            }
            updateBalance(loggedInAccountNo, -amount);

            updateBalance(receiverNo, amount);

            double balanceAfter = balanceBefore - amount;

            String txId = insertTransactionLog(loggedInAccountNo, receiverNo, amount, content, balanceBefore, balanceAfter);

            dbConnection.commit();
            dbConnection.setAutoCommit(true);

            lblSenderBalance.setText(MONEY_FMT.format(balanceAfter) + " VNĐ");
            showStatus("✔ Chuyển khoản thành công! Mã GD: " + txId, SUCCESS);

            lastInvoice = new InvoiceData();
            lastInvoice.transactionId    = txId;
            lastInvoice.senderAccountNo  = loggedInAccountNo;
            lastInvoice.senderName       = lblSenderName.getText();
            lastInvoice.receiverAccountNo = receiverNo;
            lastInvoice.receiverName     = receiverName;
            lastInvoice.amount           = amount;
            lastInvoice.content          = content;
            lastInvoice.balanceBefore    = balanceBefore;
            lastInvoice.balanceAfter     = balanceAfter;
            lastInvoice.transactionTime  = new Date();
            lastInvoice.status           = "THÀNH CÔNG";

            btnPrintInvoice.setVisible(true);

        } catch (SQLException e) {
            try { dbConnection.rollback(); } catch (SQLException ignored) {}
            showStatus("Lỗi giao dịch: " + e.getMessage(), DANGER);
            e.printStackTrace();
        }
    }


    private void printInvoice() {
        if (lastInvoice == null) {
            showStatus("Không có dữ liệu hóa đơn!", DANGER);
            return;
        }

        String filePath = invoiceDAO.exportInvoiceToTxt(lastInvoice);

        if (filePath != null) {
            // Preview trong dialog
            String content = invoiceDAO.buildInvoiceContent(lastInvoice);
            showInvoicePreview(content, filePath);
        } else {
            showStatus("Xuất hóa đơn thất bại! Vui lòng thử lại.", DANGER);
        }
    }
    private void showInvoicePreview(String content, String filePath) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Hóa đơn chuyển khoản", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(480, 520);
        dialog.setLocationRelativeTo(this);

        JTextArea textArea = new JTextArea(content);
        textArea.setFont(new Font("Courier New", Font.PLAIN, 12));
        textArea.setEditable(false);
        textArea.setMargin(new Insets(12, 16, 12, 16));

        JScrollPane scroll = new JScrollPane(textArea);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        btnPanel.setBackground(BG);

        JButton btnOpen = createButton("📂 Mở file", PRIMARY);
        JButton btnClose = createButton("Đóng", new Color(108, 117, 125));

        btnOpen.addActionListener(e -> {
            try {
                Desktop.getDesktop().open(new File(filePath));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog,
                        "Không thể mở file. Đường dẫn:\n" + filePath);
            }
        });
        btnClose.addActionListener(e -> dialog.dispose());

        btnPanel.add(btnOpen);
        btnPanel.add(btnClose);

        dialog.add(scroll, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        JLabel pathLabel = new JLabel("  Đã lưu: " + filePath, SwingConstants.LEFT);
        pathLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        pathLabel.setForeground(Color.GRAY);
        pathLabel.setBorder(new EmptyBorder(4, 8, 4, 8));
        dialog.add(pathLabel, BorderLayout.NORTH);

        showStatus("✔ Hóa đơn đã lưu tại: " + filePath, SUCCESS);
        dialog.setVisible(true);
    }

    private void resetForm() {
        txtReceiverAccount.setText("");
        lblReceiverName.setText("(Nhập STK rồi bấm Tra cứu)");
        lblReceiverName.setForeground(Color.GRAY);
        txtAmount.setText("");
        txtContent.setText("Chuyen khoan");
        lblStatus.setText(" ");
        lblStatus.setBackground(BG);
        btnPrintInvoice.setVisible(false);
        lastInvoice = null;
        loadSenderInfo();
    }

    private double getBalance(String accountNo) throws SQLException {
        String sql = "SELECT Balance FROM Accounts WHERE AccountNumber = ?";
        PreparedStatement ps = dbConnection.prepareStatement(sql);
        ps.setString(1, accountNo);
        ResultSet rs = ps.executeQuery();
        double bal = rs.next() ? rs.getDouble("Balance") : 0;
        rs.close(); ps.close();
        return bal;
    }

    private void updateBalance(String accountNo, double delta) throws SQLException {
        String sql = "UPDATE Accounts SET Balance = Balance + ? WHERE AccountNumber = ?";
        PreparedStatement ps = dbConnection.prepareStatement(sql);
        ps.setDouble(1, delta);
        ps.setString(2, accountNo);
        ps.executeUpdate();
        ps.close();
    }

    private String insertTransactionLog(String senderNo, String receiverNo,
                                         double amount, String content,
                                         double balanceBefore, double balanceAfter) throws SQLException {
        String sql = """
                INSERT INTO Transactions
                    (TypeID, FromAccount, ReceiverAccountNumber, ReceiverBankCode,
                     ATMID, Amount, BalanceBefore, BalanceAfter, Description, Status)
                VALUES (2, ?, ?, NULL, NULL, ?, ?, ?, ?, 'Success')
                """;
        PreparedStatement ps = dbConnection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
        ps.setString(1, senderNo);
        ps.setString(2, receiverNo);
        ps.setDouble(3, amount);
        ps.setDouble(4, balanceBefore);
        ps.setDouble(5, balanceAfter);
        ps.setString(6, content);
        ps.executeUpdate();
  
        ResultSet keys = ps.getGeneratedKeys();
        String txId = keys.next() ? String.valueOf(keys.getInt(1)) : "N/A";
        keys.close(); ps.close();
        return txId;
    }

    private JPanel createCard(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(PRIMARY, 1),
                        title, TitledBorder.LEFT, TitledBorder.TOP,
                        new Font("Segoe UI", Font.BOLD, 13), PRIMARY),
                new EmptyBorder(12, 16, 16, 16)));
        return panel;
    }

    private void addRow(JPanel panel, String labelText, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(panel.getBackground());
        row.setBorder(new EmptyBorder(6, 0, 6, 0));
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(LABEL_FONT);
        lbl.setPreferredSize(new Dimension(150, 24));
        if (field instanceof JTextField tf) tf.setFont(FIELD_FONT);
        row.add(lbl, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        panel.add(row);
    }

    private void addRowCustom(JPanel panel, String labelText, JPanel field) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(panel.getBackground());
        row.setBorder(new EmptyBorder(6, 0, 6, 0));
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(LABEL_FONT);
        lbl.setPreferredSize(new Dimension(150, 24));
        field.setBackground(panel.getBackground());
        row.add(lbl, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        panel.add(row);
    }

    private JButton createButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(bg);
        btn.setForeground(WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(160, 38));
        return btn;
    }

    private void showStatus(String message, Color color) {
        lblStatus.setText(message);
        lblStatus.setForeground(color);
        lblStatus.setBackground(new Color(
                color.getRed(), color.getGreen(), color.getBlue(), 20));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("ABC Bank - Chuyển khoản");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(820, 480);
            frame.setLocationRelativeTo(null);
            String accountNo = SessionManager.getCurrentCard().getAccountNumber();
            frame.add(new TransactionController(accountNo, DBConnection.getConnection()));
            frame.setVisible(true);
        });
    }
}
