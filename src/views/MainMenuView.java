package views;

import utils.SessionManager;
import controllers.AuthController; // Nhập để dùng khi đăng xuất quay lại màn hình Login

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class MainMenuView extends JFrame {
    private JButton btnWithdraw;
    private JButton btnTransfer;
    private JButton btnBalance;
    private JButton btnChangePin;
    private JButton btnTransactionHistory;
    private JButton btnLogout;

    public MainMenuView() {
        // Thiết lập JFrame
        setTitle("Hệ thống ATM - Menu Chính");
        setSize(550, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Căn giữa màn hình
        setResizable(false);
        setLayout(new BorderLayout());

        // --- Phần Header (Hiển thị lời chào và che giấu số thẻ) ---
        String cardNumber = SessionManager.isLoggedIn() ? SessionManager.getCurrentCard().getCardNumber() : "Khách";
        String maskedCard = cardNumber.length() >= 4 ? "**** **** **** " + cardNumber.substring(cardNumber.length() - 4) : cardNumber;

        JPanel headerPanel = new JPanel(new GridLayout(2, 1));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        JLabel lblWelcome = new JLabel("XIN CHÀO QUÝ KHÁCH", SwingConstants.CENTER);
        lblWelcome.setFont(new Font("Arial", Font.BOLD, 20));
        lblWelcome.setForeground(Color.BLUE);

        JLabel lblCard = new JLabel("Số thẻ: " + maskedCard, SwingConstants.CENTER);
        lblCard.setFont(new Font("Arial", Font.PLAIN, 14));

        headerPanel.add(lblWelcome);
        headerPanel.add(lblCard);
        add(headerPanel, BorderLayout.NORTH);

        // --- Phần Center (Các nút chức năng ATM) ---
        // Sử dụng GridLayout 3 hàng x 2 cột, khoảng cách các nút là 15px
        JPanel menuPanel = new JPanel(new GridLayout(3, 2, 15, 15));
        menuPanel.setBorder(BorderFactory.createEmptyBorder(10, 40, 30, 40));

        btnWithdraw = new JButton("RÚT TIỀN");
        btnTransfer = new JButton("CHUYỂN KHOẢN");
        btnBalance = new JButton("VẤN TIN SỐ DƯ");
        btnChangePin = new JButton("ĐỔI MÃ PIN");
        btnTransactionHistory = new JButton("Lịch sử");
        btnLogout = new JButton("ĐĂNG XUẤT / RÚT THẺ");

        // Định dạng chung cho các nút
        Font btnFont = new Font("Arial", Font.BOLD, 14);
        JButton[] buttons = {btnWithdraw, btnBalance, btnTransfer, btnChangePin, btnTransactionHistory, btnLogout};

        for (JButton btn : buttons) {
            btn.setFont(btnFont);
            btn.setFocusPainted(false); // Bỏ viền khi click
            menuPanel.add(btn);
        }

        add(menuPanel, BorderLayout.CENTER);

        // --- Xử lý sự kiện mặc định cho nút Đăng xuất ---
        btnLogout.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Bạn có chắc chắn muốn kết thúc giao dịch và rút thẻ?",
                    "Xác nhận",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm == JOptionPane.YES_OPTION) {
                SessionManager.logout(); // Xóa phiên đăng nhập
                this.dispose(); // Đóng Menu

                // Mở lại màn hình đăng nhập
                SwingUtilities.invokeLater(() -> {
                    views.LoginView loginView = new views.LoginView();
                    new AuthController(loginView);
                    loginView.setVisible(true);
                });
            }
        });
    }


    public void addWithdrawListener(ActionListener listener) {
        btnWithdraw.addActionListener(listener);

    }

    public void addTransferListener(ActionListener listener) {
        btnTransfer.addActionListener(listener);
    }

    public void addBalanceListener(ActionListener listener) {
        btnBalance.addActionListener(listener);
    }

    public void addChangePinListener(ActionListener listener) {
        btnChangePin.addActionListener(listener);
    }

    public void addTransactionHistoryListener(ActionListener listener) {
        btnTransactionHistory.addActionListener(listener);
    }
}