package views;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class ChangePINView extends JFrame {
    private JPasswordField txtOldPin;
    private JPasswordField txtNewPin;
    private JPasswordField txtConfirmPin;
    private JButton btnConfirm;
    private JButton btnCancel;

    public ChangePINView() {
        setTitle("Hệ thống ATM - Đổi mã PIN");
        setSize(450, 350);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new BorderLayout());

        // ─── Header ───────────────────────────────────────────────────
        JLabel lblHeader = new JLabel("ĐỔI MÃ PIN", SwingConstants.CENTER);
        lblHeader.setFont(new Font("Arial", Font.BOLD, 18));
        lblHeader.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));
        add(lblHeader, BorderLayout.NORTH);

        // ─── Form Panel ───────────────────────────────────────────────
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 15));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        // Row 1: Mã PIN cũ
        formPanel.add(new JLabel("Mã PIN cũ:"));
        txtOldPin = new JPasswordField();
        txtOldPin.setFont(new Font("Arial", Font.PLAIN, 14));
        formPanel.add(txtOldPin);

        // Row 2: Mã PIN mới
        formPanel.add(new JLabel("Mã PIN mới:"));
        txtNewPin = new JPasswordField();
        txtNewPin.setFont(new Font("Arial", Font.PLAIN, 14));
        formPanel.add(txtNewPin);

        // Row 3: Xác nhận PIN mới
        formPanel.add(new JLabel("Xác nhận mã PIN mới:"));
        txtConfirmPin = new JPasswordField();
        txtConfirmPin.setFont(new Font("Arial", Font.PLAIN, 14));
        formPanel.add(txtConfirmPin);

        add(formPanel, BorderLayout.CENTER);

        // ─── Button Panel ─────────────────────────────────────────────
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        btnConfirm = new JButton("Xác nhận");
        btnCancel = new JButton("Hủy");

        btnConfirm.setFont(new Font("Arial", Font.BOLD, 14));
        btnCancel.setFont(new Font("Arial", Font.BOLD, 14));

        btnPanel.add(btnConfirm);
        btnPanel.add(btnCancel);

        add(btnPanel, BorderLayout.SOUTH);

        // ─── Mặc định: Nút Hủy đóng cửa sổ ───────────────────────────
        btnCancel.addActionListener(e -> this.dispose());
    }

    // ─── Getters ──────────────────────────────────────────────────────
    public String getOldPin() {
        return new String(txtOldPin.getPassword()).trim();
    }

    public String getNewPin() {
        return new String(txtNewPin.getPassword()).trim();
    }

    public String getConfirmPin() {
        return new String(txtConfirmPin.getPassword()).trim();
    }

    // ─── Listeners ────────────────────────────────────────────────────
    public void addConfirmListener(ActionListener listener) {
        btnConfirm.addActionListener(listener);
    }

    // ─── Message Dialog ───────────────────────────────────────────────
    public void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    public void showError(String error) {
        JOptionPane.showMessageDialog(this, error, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    public void clearFields() {
        txtOldPin.setText("");
        txtNewPin.setText("");
        txtConfirmPin.setText("");
    }
}
