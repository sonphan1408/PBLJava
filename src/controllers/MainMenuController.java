// File: src/controllers/MainMenuController.java
package controllers;

import views.MainMenuView;
import views.TransactionView;
import views.WithdrawView;

import javax.swing.*;
import java.awt.event.ActionListener;


public class MainMenuController {

    private final MainMenuView view;

    public MainMenuController(MainMenuView view) {
        this.view = view;

        wireWithdrawButton();
        wireTransferButton();   // SỬA LỖI #1: Đây là dòng code bị thiếu trước đó
        wireBalanceButton();
        wireChangePinButton();
        wireHistoryButton();

    }

    // ── RÚT TIỀN ─────────────────────────────────────────────────────────────
    private void wireWithdrawButton() {
        view.addWithdrawListener(e -> {
            WithdrawView withdrawView = new WithdrawView();
            new WithdrawController(withdrawView);
            withdrawView.setVisible(true);
            view.dispose();
        });
    }

    // ── CHUYỂN KHOẢN ─────────────────────────────────────────────────────────
    private void wireTransferButton() {
        view.addTransferListener(e -> {
            String accountNumber = utils.SessionManager.getCurrentCard().getAccountNumber();

            // 1. Tạo Giao diện (TransactionView chính là JPanel)
            TransactionView transferView = new TransactionView(accountNumber);

            // 2. Khởi tạo Controller và nạp View vào để nó quản lý logic
            new TransactionController(transferView, accountNumber);

            // 3. Tạo cửa sổ JFrame và thêm VIEW vào (chứ không thêm Controller)
            JFrame transferFrame = new JFrame("Chuyển khoản nội bộ");
            transferFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            transferFrame.setSize(820, 500);
            transferFrame.setLocationRelativeTo(null);
            transferFrame.setResizable(false);
            transferFrame.add(transferView);

            // Khi cửa sổ chuyển khoản bị đóng, quay trở lại menu chính
            transferFrame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    // Hiển thị lại menu chính nếu người dùng tắt cửa sổ này
                    view.setVisible(true);
                }
            });

            view.setVisible(false);
        });
    }

    // ── VẤN TIN SỐ DƯ ───────────────────────────────────────────────────────
    private void wireBalanceButton() {
        view.addBalanceListener(e -> {

            String accountNumber = utils.SessionManager.getCurrentCard().getAccountNumber();
            DAO.AccountDAO accountDAO = new DAO.AccountDAO();
            models.Account account = accountDAO.getAccountByNumber(accountNumber);
            if (account != null) {
                java.text.DecimalFormat fmt = new java.text.DecimalFormat("#,###");
                JOptionPane.showMessageDialog(view,
                        "Số tài khoản: " + accountNumber + "\n" +
                        "Số dư khả dụng: " + fmt.format(account.getBalance()) + " VNĐ",
                        "Vấn tin số dư", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(view, "Không thể truy vấn số dư!",
                        "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    // ── ĐỔI MÃ PIN ──────────────────────────────────────────────────────────
    private void wireChangePinButton() {
        view.addChangePinListener(e -> {

            JOptionPane.showMessageDialog(view,
                    "Tính năng đổi mã PIN đang được phát triển.",
                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    // ── IN SAO KÊ ────────────────────────────────────────────────────────────
    private void wireHistoryButton() {
        view.addTransactionHistoryListener(e -> {

            JOptionPane.showMessageDialog(view,
                    "Tính năng in sao kê đang được phát triển.",
                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        });
    }


}