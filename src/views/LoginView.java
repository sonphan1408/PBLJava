package views;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class LoginView extends JFrame {
    private JTextField txtCardNumber;
    private JPasswordField txtPin;
    private JButton btnLogin;
    private JButton btnExit;

    public LoginView() {
        setTitle("Hệ thống ATM - Đăng nhập");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new BorderLayout());

        // Header
        JLabel lblHeader = new JLabel("VUI LÒNG ĐƯA THẺ VÀO (NHẬP SỐ THẺ)", SwingConstants.CENTER);
        lblHeader.setFont(new Font("Arial", Font.BOLD, 16));
        lblHeader.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(lblHeader, BorderLayout.NORTH);

        // Form Pannel
        JPanel formPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        formPanel.add(new JLabel("Số thẻ:"));
        txtCardNumber = new JTextField();
        formPanel.add(txtCardNumber);

        formPanel.add(new JLabel("Mã PIN:"));
        txtPin = new JPasswordField();
        formPanel.add(txtPin);

        add(formPanel, BorderLayout.CENTER);

        // Button Panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        btnLogin = new JButton("Đăng nhập");
        btnExit = new JButton("Hủy / Rút thẻ");
        btnPanel.add(btnLogin);
        btnPanel.add(btnExit);
        add(btnPanel, BorderLayout.SOUTH);

        // Thoát ứng dụng khi nhấn Hủy
        btnExit.addActionListener(e -> System.exit(0));
    }

    public String getCardNumber() {
        return txtCardNumber.getText().trim();
    }

    public String getPin() {
        return new String(txtPin.getPassword()).trim();
    }

    public void addLoginListener(ActionListener listener) {
        btnLogin.addActionListener(listener);
    }

    public void showMessage(String msg) {
        JOptionPane.showMessageDialog(this, msg);
    }

    public void clearFields() {
        txtCardNumber.setText("");
        txtPin.setText("");
    }
}