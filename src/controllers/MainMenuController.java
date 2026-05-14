package controllers;

import views.InterbankTransferView;
import views.MainMenuView;
import views.WithdrawView;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * =====================================================================
 * MainMenuController — Điều phối Menu Chính (Đã tích hợp LNH)
 * =====================================================================
 * Lớp này đóng vai trò "người gác cổng" (Router/Navigator), xử lý các
 * sự kiện bấm nút trên màn hình chính và điều hướng người dùng đến
 * các tính năng tương ứng (Rút tiền, Chuyển khoản, Vấn tin...).
 *
 * Cơ chế hoạt động với các tính năng dạng Panel (như Chuyển khoản):
 * Các Controller như TransactionController hay InterbankTransferView
 * thực chất đang kế thừa JPanel (để dễ dàng tái sử dụng). Do đó, lớp này
 * sẽ tự động tạo ra một lớp vỏ Cửa sổ (JFrame wrapper) để bọc các Panel 
 * đó lại, đồng thời quản lý vòng đời: 
 * "Ẩn menu chính đi -> Mở cửa sổ tính năng -> Khi đóng tính năng thì hiện lại menu".
 */
public class MainMenuController {

    private final MainMenuView view;

    // ── Hàm khởi tạo (Constructor) ───────────────────────────────────────────
    public MainMenuController(MainMenuView view) {
        this.view = view;
        
        wireWithdrawButton();
        wireInternalTransferButton();
        wireInterbankTransferButton();
        wireBalanceButton();
        wireChangePinButton();
        wireHistoryButton();
    }

    // =====================================================================
    // KẾT NỐI SỰ KIỆN CÁC NÚT BẤM CHỨC NĂNG
    // =====================================================================

    // ── 1. RÚT TIỀN MẶT ──────────────────────────────────────────────────────
    private void wireWithdrawButton() {
        view.addWithdrawListener(e -> {
            // Rút tiền đã là một JFrame độc lập, chỉ cần khởi tạo và gọi
            WithdrawView withdrawView = new WithdrawView();
            new WithdrawController(withdrawView);
            withdrawView.setVisible(true);
            
            // Đóng menu chính (Vì màn hình rút tiền có nút Quay lại riêng để tạo mới Menu)
            view.dispose();
        });
    }

    // ── 2. CHUYỂN KHOẢN NỘI BỘ ───────────────────────────────────────────────
    private void wireInternalTransferButton() {
        view.addTransferListener(e -> {
            String accountNumber = utils.SessionManager.getCurrentCard().getAccountNumber();
            
            // Khởi tạo Panel chức năng chuyển khoản nội bộ
            TransactionController transferPanel = new TransactionController(accountNumber);
            
            // Tạo vỏ bọc JFrame
            JFrame frame = new JFrame("Chuyển khoản nội bộ — ABC Bank");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(860, 520);
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.add(transferPanel);
            
            // Bắt sự kiện: Khi đóng cửa sổ chuyển khoản thì hiện lại Menu chính
            frame.addWindowListener(new WindowAdapter() {
                @Override 
                public void windowClosing(WindowEvent e) { 
                    view.setVisible(true); 
                }
            });
            
            view.setVisible(false); // Ẩn Menu
            frame.setVisible(true); // Hiện tính năng
        });
    }

    // ── 3. CHUYỂN KHOẢN LIÊN NGÂN HÀNG ───────────────────────────────────────
    private void wireInterbankTransferButton() {
        view.addInterbankTransferListener(e -> {
            String accountNumber = utils.SessionManager.getCurrentCard().getAccountNumber();
            
            // Khởi tạo View và Controller cho chức năng Liên ngân hàng
            InterbankTransferView interbankPanel = new InterbankTransferView(accountNumber);
            new InterbankTransferController(interbankPanel);
            
            // Tạo vỏ bọc JFrame
            JFrame frame = new JFrame("Chuyển khoản liên ngân hàng — ABC Bank");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(900, 540);
            frame.setLocationRelativeTo(null);
            frame.setResizable(false);
            frame.add(interbankPanel);
            
            // Bắt sự kiện: Khi đóng cửa sổ thì quay về Menu chính
            frame.addWindowListener(new WindowAdapter() {
                @Override 
                public void windowClosing(WindowEvent e) { 
                    view.setVisible(true); 
                }
            });
            
            view.setVisible(false); // Ẩn Menu
            frame.setVisible(true); // Hiện tính năng
        });
    }

    // ── 4. VẤN TIN SỐ DƯ ─────────────────────────────────────────────────────
    private void wireBalanceButton() {
        view.addBalanceListener(e -> {
            String accountNo = utils.SessionManager.getCurrentCard().getAccountNumber();
            
            // Tạm thời gọi trực tiếp DAO để lấy số dư (Sau này có thể tách ra View riêng)
            DAO.AccountDAO accountDAO = new DAO.AccountDAO();
            models.Account account = accountDAO.getAccountByNumber(accountNo);
            
            if (account != null) {
                java.text.DecimalFormat fmt = new java.text.DecimalFormat("#,###");
                JOptionPane.showMessageDialog(view,
                        "Số tài khoản: " + accountNo + "\n" +
                        "Số dư khả dụng: " + fmt.format(account.getBalance()) + " VNĐ",
                        "Vấn tin số dư", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(view, "Không thể truy vấn số dư!",
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    // ── 5. ĐỔI MÃ PIN ────────────────────────────────────────────────────────
    private void wireChangePinButton() {
        view.addChangePinListener(e ->
                JOptionPane.showMessageDialog(view, "Tính năng đổi mã PIN đang được phát triển.",
                        "Thông báo", JOptionPane.INFORMATION_MESSAGE));
    }

    // ── 6. IN SAO KÊ ─────────────────────────────────────────────────────────
    private void wireHistoryButton() {
        view.addTransactionHistoryListener(e ->
                JOptionPane.showMessageDialog(view, "Tính năng in sao kê đang được phát triển.",
                        "Thông báo", JOptionPane.INFORMATION_MESSAGE));
    }
}