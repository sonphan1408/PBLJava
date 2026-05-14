package views;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

public class TransactionView extends JPanel {

    // ── UI Constants ────────────────────────────────────────────────────────
    private static final Color PRIMARY    = new Color(0, 86, 163);
    private static final Color SUCCESS    = new Color(34, 139, 34);
    private static final Color DANGER     = new Color(200, 30, 30);
    private static final Color BG         = new Color(245, 247, 250);
    private static final Color WHITE      = Color.WHITE;
    private static final Font  TITLE_FONT = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font  FIELD_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    public static final DecimalFormat MONEY_FMT = new DecimalFormat("#,###");

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

    public TransactionView(String loggedInAccountNo) {
        setLayout(new BorderLayout(0, 0));
        setBackground(BG);
        setBorder(new EmptyBorder(24, 32, 24, 32));
        initComponents(loggedInAccountNo);
    }

    private void initComponents(String loggedInAccountNo) {
        JLabel title = new JLabel("CHUYỂN KHOẢN NỘI BỘ", SwingConstants.CENTER);
        title.setFont(TITLE_FONT);
        title.setForeground(PRIMARY);
        title.setBorder(new EmptyBorder(0, 0, 20, 0));
        add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 24, 0));
        center.setBackground(BG);
        center.add(buildSenderPanel(loggedInAccountNo));
        center.add(buildReceiverPanel());
        add(center, BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildSenderPanel(String loggedInAccountNo) {
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

    // Các hàm trợ giúp UI
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

    // ── Getters (Để Controller lấy dữ liệu người dùng nhập) ───────────────
    public String getReceiverAccount() { return txtReceiverAccount.getText().trim(); }
    public String getAmount() { return txtAmount.getText().trim().replaceAll("[,.]", ""); }
    public String getContent() { return txtContent.getText().trim(); }
    public String getReceiverName() { return lblReceiverName.getText(); }
    public String getSenderName() { return lblSenderName.getText(); }

    // ── Setters (Để Controller cập nhật màn hình) ─────────────────────────
    public void setSenderInfo(String name, String balanceStr) {
        lblSenderName.setText(name);
        lblSenderBalance.setText(balanceStr + " VNĐ");
    }

    public void setReceiverName(String name, boolean isValid) {
        lblReceiverName.setText(name);
        lblReceiverName.setForeground(isValid ? new Color(0, 100, 0) : DANGER);
    }

    public void showStatus(String message, Color color) {
        lblStatus.setText(message);
        lblStatus.setForeground(color);
    }

    public void setTransferButtonsEnabled(boolean enabled) {
        btnTransfer.setEnabled(enabled);
        btnReset.setEnabled(enabled);
        btnLookup.setEnabled(enabled);
    }

    public void setPrintInvoiceVisible(boolean visible) {
        btnPrintInvoice.setVisible(visible);
    }

    public void resetFields() {
        txtReceiverAccount.setText("");
        lblReceiverName.setText("(Nhập STK rồi bấm Tra cứu)");
        lblReceiverName.setForeground(Color.GRAY);
        txtAmount.setText("");
        lblStatus.setText(" ");
        btnPrintInvoice.setVisible(false);
    }

    // ── Cung cấp màu hằng số cho Controller dùng ──────────────────────────
    public Color getPrimaryColor() { return PRIMARY; }
    public Color getDangerColor() { return DANGER; }
    public Color getSuccessColor() { return SUCCESS; }

    // ── Đăng ký sự kiện (Lắng nghe click chuột) ───────────────────────────
    public void addLookupListener(ActionListener listener) { btnLookup.addActionListener(listener); }
    public void addTransferListener(ActionListener listener) { btnTransfer.addActionListener(listener); }
    public void addResetListener(ActionListener listener) { btnReset.addActionListener(listener); }
    public void addPrintInvoiceListener(ActionListener listener) { btnPrintInvoice.addActionListener(listener); }
}