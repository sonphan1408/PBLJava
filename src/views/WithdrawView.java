package views;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class WithdrawView extends JFrame {
    private JButton btn500k, btn1M, btn2M, btn5M, btnOther, btnCancel;
    private JLabel lblBalance; // Thêm nhãn hiển thị số dư

    public WithdrawView() {
        setTitle("Hệ thống ATM - Rút tiền");
        setSize(550, 450);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // --- Bố cục Panel phía trên (Chứa Tiêu đề và Số dư) ---
        JPanel topPanel = new JPanel(new GridLayout(2, 1));
        topPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));

        JLabel lblTitle = new JLabel("VUI LÒNG CHỌN SỐ TIỀN CẦN RÚT", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 18));

        lblBalance = new JLabel("Số dư khả dụng: Đang tải...", SwingConstants.CENTER);
        lblBalance.setFont(new Font("Arial", Font.ITALIC, 14));
        lblBalance.setForeground(Color.BLUE); // Cho màu xanh để dễ nhìn

        topPanel.add(lblTitle);
        topPanel.add(lblBalance);
        add(topPanel, BorderLayout.NORTH);

        // --- Bố cục Panel trung tâm (Chứa các nút chức năng) ---
        JPanel centerPanel = new JPanel(new GridLayout(3, 2, 20, 20));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 40, 30, 40));

        btn500k = new JButton("500.000 VNĐ");
        btn1M = new JButton("1.000.000 VNĐ");
        btn2M = new JButton("2.000.000 VNĐ");
        btn5M = new JButton("5.000.000 VNĐ");
        btnOther = new JButton("SỐ KHÁC");
        btnCancel = new JButton("QUAY LẠI");

        Font btnFont = new Font("Arial", Font.BOLD, 14);
        JButton[] buttons = {btn500k, btn1M, btn2M, btn5M, btnOther, btnCancel};
        for (JButton btn : buttons) {
            btn.setFont(btnFont);
            centerPanel.add(btn);
        }

        add(centerPanel, BorderLayout.CENTER);
    }

    // Hàm này để Controller truyền số dư vào giao diện
    public void setBalance(String balanceStr) {
        lblBalance.setText("Số dư khả dụng: " + balanceStr);
    }

    public void addAmountListener(ActionListener listener) {
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
        JOptionPane.showMessageDialog(this, msg);
    }

    public String promptCustomAmount() {
        return JOptionPane.showInputDialog(this, "Nhập số tiền cần rút ít nhất là 50.000 VNĐ):");
    }
}