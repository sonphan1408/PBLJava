package views;

import DAO.InterbankTransferDAO.BankEntry;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * =====================================================================
 * InterbankTransferView — Giao diện Chuyển khoản Liên ngân hàng
 * =====================================================================
 * Bảng điều khiển (UI panel) dành cho chức năng chuyển khoản khác ngân hàng.
 * Được thiết kế để nhúng vào bên trong một JFrame bởi InterbankTransferController,
 * tương tự như cách TransactionController được nhúng trong MainMenuController.
 *
 * Bố cục (Layout): BorderLayout chia làm 3 khu vực chính:
 * - NORTH  — Thanh tiêu đề màn hình
 * - CENTER — Hai thẻ (card) đặt song song: Thông tin người gửi | Form chuyển khoản
 * - SOUTH  — Các nút thao tác + Nhãn thông báo trạng thái
 *
 * Tuân thủ tuyệt đối nguyên tắc "View thụ động" (Dumb View) trong MVC:
 * - Giao diện chỉ chứa các thành phần UI và mở ra các cổng Getters/Setters.
 * - KHÔNG chứa logic nghiệp vụ, KHÔNG gọi Database.
 * - Controller sẽ gọi Setters để cập nhật màn hình và dùng Getters để
 * lấy dữ liệu do người dùng nhập vào.
 */
public class InterbankTransferView extends JPanel {

    // ── Hệ màu và Font chuẩn (Đồng bộ tuyệt đối với TransactionController) ──
    private static final Color PRIMARY  = new Color(0, 86, 163);
    private static final Color SUCCESS  = new Color(34, 139, 34);
    private static final Color DANGER   = new Color(200, 30, 30);
    private static final Color WARNING  = new Color(180, 100, 0);
    private static final Color BG       = new Color(245, 247, 250);
    private static final Color WHITE    = Color.WHITE;
    private static final Font  TITLE_F  = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font  LABEL_F  = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font  FIELD_F  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font  BTN_F    = new Font("Segoe UI", Font.BOLD, 13);

    // ── Các thành phần hiển thị thông tin người gửi ──────────────────────────
    private final JTextField txtSenderAccount;
    private final JLabel     lblSenderName;
    private final JLabel     lblSenderBalance;

    // ── Các thành phần Form chuyển khoản ─────────────────────────────────────
    private final JComboBox<BankEntry> cmbBank;
    private final JTextField           txtReceiverAccNum;
    private final JLabel               lblReceiverName;
    private final JTextField           txtAmount;
    private final JTextField           txtDescription;

    // ── Các nút hành động và thông báo ───────────────────────────────────────
    private final JButton btnLookup;
    private final JButton btnTransfer;
    private final JButton btnCancel;
    private final JLabel  lblStatus;

    // ── Hàm Khởi tạo (Constructor) ───────────────────────────────────────────
    public InterbankTransferView(String senderAccountNo) {
        setLayout(new BorderLayout(0, 0));
        setBackground(BG);
        setBorder(new EmptyBorder(24, 32, 24, 32));

        // ── NORTH: Khu vực Tiêu đề ──────────────────────────────────────────
        JLabel lblTitle = new JLabel("CHUYỂN KHOẢN LIÊN NGÂN HÀNG", SwingConstants.CENTER);
        lblTitle.setFont(TITLE_F);
        lblTitle.setForeground(PRIMARY);
        lblTitle.setBorder(new EmptyBorder(0, 0, 18, 0));
        add(lblTitle, BorderLayout.NORTH);

        // ── CENTER: Hai thẻ thông tin (Cards) đặt cạnh nhau ─────────────────
        JPanel center = new JPanel(new GridLayout(1, 2, 24, 0));
        center.setBackground(BG);

        // — Thẻ thông tin người gửi —
        JPanel senderCard = createCard("Thông tin người gửi");

        txtSenderAccount = new JTextField(senderAccountNo);
        txtSenderAccount.setEditable(false);
        txtSenderAccount.setBackground(new Color(235, 235, 235));
        txtSenderAccount.setFont(FIELD_F);

        lblSenderName    = new JLabel("Đang tải...");
        lblSenderBalance = new JLabel("Đang tải...");
        lblSenderName.setFont(FIELD_F);
        lblSenderBalance.setFont(FIELD_F);
        lblSenderBalance.setForeground(new Color(0, 120, 0));

        addRow(senderCard, "Số tài khoản:",   txtSenderAccount);
        addRow(senderCard, "Chủ tài khoản:",  lblSenderName);
        addRow(senderCard, "Số dư hiện tại:", lblSenderBalance);
        center.add(senderCard);

        // — Thẻ nhập liệu chuyển khoản —
        JPanel formCard = createCard("Thông tin chuyển khoản");

        // Danh sách thả xuống (Combobox) chọn Ngân hàng
        cmbBank = new JComboBox<>();
        cmbBank.setFont(FIELD_F);
        cmbBank.setBackground(WHITE);

        // Ô nhập STK và nút Tra cứu nằm trên cùng một hàng
        txtReceiverAccNum = new JTextField();
        txtReceiverAccNum.setFont(FIELD_F);

        btnLookup = new JButton("Tra cứu");
        btnLookup.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnLookup.setBackground(PRIMARY);
        btnLookup.setForeground(WHITE);
        btnLookup.setFocusPainted(false);
        btnLookup.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLookup.setPreferredSize(new Dimension(90, 28));

        JPanel receiverRow = new JPanel(new BorderLayout(6, 0));
        receiverRow.setBackground(WHITE);
        receiverRow.add(txtReceiverAccNum, BorderLayout.CENTER);
        receiverRow.add(btnLookup, BorderLayout.EAST);

        lblReceiverName = new JLabel("(Chọn ngân hàng và nhập STK, sau đó bấm Tra cứu)");
        lblReceiverName.setFont(FIELD_F);
        lblReceiverName.setForeground(Color.GRAY);

        txtAmount = new JTextField();
        txtAmount.setFont(FIELD_F);

        txtDescription = new JTextField("Chuyen khoan lien ngan hang");
        txtDescription.setFont(FIELD_F);

        addRow(formCard, "Ngân hàng nhận:",   cmbBank);
        addRowCustom(formCard, "Số TK nhận:", receiverRow);
        addRow(formCard, "Tên người nhận:",   lblReceiverName);
        addRow(formCard, "Số tiền (VNĐ):",    txtAmount);
        addRow(formCard, "Nội dung:",         txtDescription);
        center.add(formCard);

        add(center, BorderLayout.CENTER);

        // ── SOUTH: Khu vực nút bấm và nhãn trạng thái ───────────────────────
        JPanel south = new JPanel(new BorderLayout(0, 10));
        south.setBackground(BG);
        south.setBorder(new EmptyBorder(18, 0, 0, 0));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        btnPanel.setBackground(BG);

        btnTransfer = createStyledButton("Chuyển khoản", new Color(0, 120, 215));
        btnCancel   = createStyledButton("Quay lại",     new Color(108, 117, 125));

        btnPanel.add(btnTransfer);
        btnPanel.add(btnCancel);

        lblStatus = new JLabel(" ", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblStatus.setOpaque(true);
        lblStatus.setBackground(BG);

        south.add(btnPanel,  BorderLayout.NORTH);
        south.add(lblStatus, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);
    }

    // =====================================================================
    // PUBLIC API — Controller sẽ gọi các hàm này để giao tiếp với View
    // =====================================================================

    public void setBankList(List<BankEntry> banks) {
        cmbBank.removeAllItems();
        for (BankEntry b : banks) cmbBank.addItem(b);
    }

    public BankEntry getSelectedBank() {
        return (BankEntry) cmbBank.getSelectedItem();
    }

    public String getReceiverAccNum() {
        return txtReceiverAccNum.getText().trim();
    }

    public String getAmountText() {
        return txtAmount.getText().trim().replaceAll("[.,\\s]", "");
    }

    public String getDescription() {
        String d = txtDescription.getText().trim();
        return d.isEmpty() ? "Chuyen khoan lien ngan hang" : d;
    }

    public void setSenderName(String name) {
        lblSenderName.setText(name != null ? name.toUpperCase() : "Không tìm thấy");
    }

    public void setSenderBalance(String formattedBalance) {
        lblSenderBalance.setText(formattedBalance + " VNĐ");
    }

    public void setReceiverName(String name) {
        if (name != null) {
            lblReceiverName.setText(name.toUpperCase());
            lblReceiverName.setForeground(new Color(0, 100, 0));
        } else {
            lblReceiverName.setText("Không tìm thấy tài khoản");
            lblReceiverName.setForeground(DANGER);
        }
    }

    public void clearReceiverName() {
        lblReceiverName.setText("(Chọn ngân hàng và nhập STK, sau đó bấm Tra cứu)");
        lblReceiverName.setForeground(Color.GRAY);
    }

    // ── Cập nhật thanh trạng thái phía dưới ───────────────────────────────
    public void showStatus(String message, Color color) {
        lblStatus.setText(message == null || message.isBlank() ? " " : message);
        lblStatus.setForeground(color);

        // ĐÃ FIX LỖI ĐÈ CHỮ: Pha trộn màu trực tiếp thay vì dùng Alpha (độ trong suốt)
        // Công thức pha màu nền nhạt đi (tương đương alpha=18) để Swing không bị lỗi render
        int r = (color.getRed() * 18 + BG.getRed() * 237) / 255;
        int g = (color.getGreen() * 18 + BG.getGreen() * 237) / 255;
        int b = (color.getBlue() * 18 + BG.getBlue() * 237) / 255;

        lblStatus.setBackground(new Color(r, g, b));

        // Bắt buộc Swing phải vẽ lại dòng này để xóa sạch bóng mờ cũ
        lblStatus.repaint();
    }

    public void clearStatus() {
        lblStatus.setText(" ");
        lblStatus.setBackground(BG);
        lblStatus.repaint();
    }

    // ── Khóa/Mở khóa nút bấm (Chống lỗi Double-submit) ────────────────────
    public void setButtonsEnabled(boolean enabled) {
        btnLookup.setEnabled(enabled);
        btnTransfer.setEnabled(enabled);
        btnCancel.setEnabled(enabled);
        cmbBank.setEnabled(enabled);
    }

    public void addLookupListener(ActionListener listener) {
        btnLookup.addActionListener(listener);
    }

    public void addTransferListener(ActionListener listener) {
        btnTransfer.addActionListener(listener);
    }

    public void addCancelListener(ActionListener listener) {
        btnCancel.addActionListener(listener);
    }

    public void addBankChangeListener(ActionListener listener) {
        cmbBank.addActionListener(listener);
    }

    public void showMessage(String msg) {
        JOptionPane.showMessageDialog(this, msg);
    }

    public int showConfirmDialog(String msg, String title) {
        return JOptionPane.showConfirmDialog(this, msg, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
    }

    // =====================================================================
    // Các hàm hỗ trợ vẽ UI — Tái sử dụng thiết kế của TransactionController
    // =====================================================================

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
        row.setBackground(WHITE);
        row.setBorder(new EmptyBorder(6, 0, 6, 0));
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(LABEL_F);
        lbl.setPreferredSize(new Dimension(145, 24));
        row.add(lbl, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        panel.add(row);
    }

    private void addRowCustom(JPanel panel, String labelText, JPanel field) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(WHITE);
        row.setBorder(new EmptyBorder(6, 0, 6, 0));
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(LABEL_F);
        lbl.setPreferredSize(new Dimension(145, 24));
        field.setBackground(WHITE);
        row.add(lbl, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        panel.add(row);
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(BTN_F);
        btn.setBackground(bg);
        btn.setForeground(WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(160, 38));
        return btn;
    }

    public Color colorSuccess() { return SUCCESS; }
    public Color colorDanger()  { return DANGER;  }
    public Color colorWarning() { return WARNING; }
    public Color colorPrimary() { return PRIMARY; }
}