package views;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

public class WithdrawView extends JFrame {


    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,###");
    
    private static final Color PRIMARY_COLOR   = new Color(0, 86, 163);
    private static final Color SECONDARY_COLOR = new Color(100, 149, 237);
    private static final Color DANGER_COLOR    = new Color(180, 60, 60);
    private static final Color TEXT_INFO       = new Color(0, 100, 180);
    private static final Color TEXT_WARNING    = new Color(150, 100, 0);
    private static final Color BG_COLOR        = new Color(245, 247, 250);
    
    private static final Font TITLE_FONT  = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font INFO_FONT   = new Font("Segoe UI", Font.ITALIC, 14);
    private static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 15);


    private JButton btn500k, btn1M, btn2M, btn5M, btnOther, btnCancel;
    private JLabel  lblBalance;
    private JLabel  lblStatus;


    public WithdrawView() {
        setTitle("Hệ thống ATM - Rút tiền");
        setSize(550, 480);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_COLOR);

        initComponents();
    }

    // ── UI Initialization ────────────────────────────────────────────────
    private void initComponents() {
        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildTopPanel() {
        JPanel topPanel = new JPanel(new GridLayout(3, 1));
        topPanel.setBackground(BG_COLOR);
        topPanel.setBorder(new EmptyBorder(24, 0, 10, 0));

        JLabel lblTitle = new JLabel("VUI LÒNG CHỌN SỐ TIỀN CẦN RÚT", SwingConstants.CENTER);
        lblTitle.setFont(TITLE_FONT);
        lblTitle.setForeground(PRIMARY_COLOR);

        lblBalance = new JLabel("Số dư khả dụng: Đang tải...", SwingConstants.CENTER);
        lblBalance.setFont(INFO_FONT);
        lblBalance.setForeground(TEXT_INFO);

        lblStatus = new JLabel(" ", SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD | Font.ITALIC, 12));
        lblStatus.setForeground(TEXT_WARNING);

        topPanel.add(lblTitle);
        topPanel.add(lblBalance);
        topPanel.add(lblStatus);
        
        return topPanel;
    }

    private JPanel buildCenterPanel() {
        JPanel centerPanel = new JPanel(new GridLayout(2, 2, 20, 20));
        centerPanel.setBackground(BG_COLOR);
        centerPanel.setBorder(new EmptyBorder(10, 40, 10, 40));

        btn500k = createAmountButton();
        btn1M   = createAmountButton();
        btn2M   = createAmountButton();
        btn5M   = createAmountButton();

        centerPanel.add(btn500k);
        centerPanel.add(btn1M);
        centerPanel.add(btn2M);
        centerPanel.add(btn5M);
        
        return centerPanel;
    }

    private JPanel buildBottomPanel() {
        JPanel bottomPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        bottomPanel.setBackground(BG_COLOR);
        bottomPanel.setBorder(new EmptyBorder(10, 40, 30, 40));

        btnOther  = createActionButton("SỐ KHÁC", SECONDARY_COLOR);
        btnCancel = createActionButton("QUAY LẠI", DANGER_COLOR);

        bottomPanel.add(btnOther);
        bottomPanel.add(btnCancel);
        
        return bottomPanel;
    }

    // ── Component Helpers ────────────────────────────────────────────────
    private JButton createAmountButton() {
        JButton btn = new JButton();
        btn.setFont(BUTTON_FONT);
        btn.setBackground(PRIMARY_COLOR);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createActionButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(BUTTON_FONT);
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ── Controller API ───────────────────────────────────────────────────
    

    public void setPresetAmounts(long[] amounts) {
        JButton[] buttons = {btn500k, btn1M, btn2M, btn5M};
        for (int i = 0; i < buttons.length && i < amounts.length; i++) {
            buttons[i].setText(MONEY_FMT.format(amounts[i]) + " VNĐ");
            buttons[i].putClientProperty("amount", amounts[i]);
        }
    }

    public void setAllButtonsEnabled(boolean enabled) {
        for (JButton btn : new JButton[]{btn500k, btn1M, btn2M, btn5M, btnOther, btnCancel}) {
            btn.setEnabled(enabled);
        }
    }

    public void setStatus(String status) {
        lblStatus.setText(status == null || status.isBlank() ? " " : status);
    }

    public void setBalance(String balanceStr) {
        lblBalance.setText("Số dư khả dụng: " + balanceStr);
    }

    public void addPresetAmountListener(ActionListener listener) {
        btn500k.addActionListener(listener);
        btn1M.addActionListener(listener);
        btn2M.addActionListener(listener);
        btn5M.addActionListener(listener);
    }

    public void addOtherAmountListener(ActionListener listener) {
        btnOther.addActionListener(listener);
    }

    public void addCancelListener(ActionListener listener) {
        btnCancel.addActionListener(listener);
    }

    public void showMessage(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    public String promptCustomAmount() {
        return JOptionPane.showInputDialog(this,
                "Nhập số tiền cần rút (bội số của 50.000 VNĐ):",
                "Rút số tiền khác",
                JOptionPane.QUESTION_MESSAGE);
    }
}