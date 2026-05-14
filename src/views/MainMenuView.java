package views;

import utils.SessionManager;
import controllers.AuthController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;


public class MainMenuView extends JFrame {

    private JButton btnWithdraw;
    private JButton btnTransfer;           // Chuyển khoản nội bộ
    private JButton btnInterbankTransfer;  // MỚI: Chuyển khoản liên ngân hàng
    private JButton btnBalance;
    private JButton btnChangePin;
    private JButton btnTransactionHistory;
    private JButton btnLogout;

    // ── Hàm Khởi tạo (Constructor) ───────────────────────────────────────────
    public MainMenuView() {
        setTitle("Hệ thống ATM - Menu Chính");
        setSize(580, 520);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new BorderLayout());

        // ── KHOẢNG TRÊN (Header): Thông tin lời chào & Số thẻ ────────────────
        String cardNumber = SessionManager.isLoggedIn()
                ? SessionManager.getCurrentCard().getCardNumber() : "Khách";
        String maskedCard = cardNumber.length() >= 4
                ? "**** **** **** " + cardNumber.substring(cardNumber.length() - 4)
                : cardNumber;

        JPanel headerPanel = new JPanel(new GridLayout(2, 1));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 16, 0));

        JLabel lblWelcome = new JLabel("XIN CHÀO QUÝ KHÁCH", SwingConstants.CENTER);
        lblWelcome.setFont(new Font("Arial", Font.BOLD, 20));
        lblWelcome.setForeground(Color.BLUE);

        JLabel lblCard = new JLabel("Số thẻ: " + maskedCard, SwingConstants.CENTER);
        lblCard.setFont(new Font("Arial", Font.PLAIN, 14));

        headerPanel.add(lblWelcome);
        headerPanel.add(lblCard);
        add(headerPanel, BorderLayout.NORTH);

        // ── KHU VỰC GIỮA: Các nút chức năng chính (Lưới 3 hàng × 2 cột) ──────
        JPanel menuPanel = new JPanel(new GridLayout(3, 2, 15, 15));
        menuPanel.setBorder(BorderFactory.createEmptyBorder(10, 40, 16, 40));

        btnWithdraw           = new JButton("RÚT TIỀN");
        btnBalance            = new JButton("VẤN TIN SỐ DƯ");
        btnTransfer           = new JButton("CHUYỂN KHOẢN NB");
        btnChangePin          = new JButton("ĐỔI MÃ PIN");
        btnInterbankTransfer  = new JButton("CHUYỂN KHOẢN LNH");  // Tính năng mới
        btnTransactionHistory = new JButton("IN SAO KÊ");

        Font btnFont = new Font("Arial", Font.BOLD, 13);
        JButton[] menuButtons = {
            btnWithdraw, btnBalance,
            btnTransfer, btnChangePin,
            btnInterbankTransfer, btnTransactionHistory
        };
        for (JButton btn : menuButtons) {
            btn.setFont(btnFont);
            btn.setFocusPainted(false);
            menuPanel.add(btn);
        }
        add(menuPanel, BorderLayout.CENTER);


        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.setBorder(BorderFactory.createEmptyBorder(0, 40, 24, 40));

        btnLogout = new JButton("ĐĂNG XUẤT / RÚT THẺ");
        btnLogout.setFont(new Font("Arial", Font.BOLD, 13));
        btnLogout.setFocusPainted(false);
        btnLogout.setBackground(new Color(200, 60, 60));
        btnLogout.setForeground(Color.WHITE);

        southPanel.add(btnLogout, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);


        btnLogout.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    this, "Bạn có chắc chắn muốn kết thúc giao dịch và rút thẻ?",
                    "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                // Xóa thông tin phiên đăng nhập
                SessionManager.logout();
                this.dispose(); // Đóng menu hiện tại
                
                // Mở lại màn hình đăng nhập
                SwingUtilities.invokeLater(() -> {
                    views.LoginView loginView = new views.LoginView();
                    new AuthController(loginView);
                    loginView.setVisible(true);
                });
            }
        });
    }


    public void addWithdrawListener(ActionListener l)           { btnWithdraw.addActionListener(l); }
    public void addTransferListener(ActionListener l)           { btnTransfer.addActionListener(l); }
    public void addInterbankTransferListener(ActionListener l)  { btnInterbankTransfer.addActionListener(l); } // MỚI
    public void addBalanceListener(ActionListener l)            { btnBalance.addActionListener(l); }
    public void addChangePinListener(ActionListener l)          { btnChangePin.addActionListener(l); }
    public void addTransactionHistoryListener(ActionListener l) { btnTransactionHistory.addActionListener(l); }
}