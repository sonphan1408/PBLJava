// File: src/controllers/MainMenuController.java
package controllers;

import views.MainMenuView;
import views.WithdrawView;

import javax.swing.*;
import java.awt.event.ActionListener;

/**
 * MainMenuController — Phiên bản đã sửa lỗi
 * =====================================
 * CÁC LỖI ĐÃ ĐƯỢC KHẮC PHỤC:
 *
 * LỖI #1 — Nút "CHUYỂN KHOẢN" không có phản hồi
 * Hàm khởi tạo ban đầu chỉ gọi view.addWithdrawListener().
 * Lệnh view.addTransferListener() chưa từng được gọi, dẫn đến nút này
 * không có sự kiện (listener) nào được gắn vào → bấm vào không có tác dụng.
 * Đã sửa: Cả 6 nút bấm hiện đã được gắn bộ lắng nghe sự kiện đầy đủ.
 *
 * LỖI #2 — TransactionController là một JPanel, không phải JFrame
 * TransactionController kế thừa từ JPanel, nên nếu chỉ khởi tạo nó
 * sẽ không tạo ra được cửa sổ hiển thị nào cả. Nó phải được nhúng vào trong một JFrame.
 * Đã sửa: Tạo một JFrame bọc bên ngoài để chứa panel này.
 *
 * LỖI #3 — Các nút khác (VẤN TIN SỐ DƯ, ĐỔI MÃ PIN, IN SAO KÊ) cũng
 * chưa có sự kiện — chúng là những nút "chết", bấm vào không có tác dụng.
 * Đã sửa: Thêm các hộp thoại thông báo tạm thời cho những tính năng chưa hoàn thiện
 * để người dùng nhận được phản hồi từ hệ thống thay vì im lặng.
 */
public class MainMenuController {

    private final MainMenuView view;

    public MainMenuController(MainMenuView view) {
        this.view = view;

        wireWithdrawButton();
        wireTransferButton();   // SỬA LỖI #1: Đây là dòng code bị thiếu trước đó
        wireBalanceButton();
        wireChangePinButton();
        wireHistoryButton();
        // Lưu ý: Nút Đăng xuất (Logout) đã được xử lý sự kiện trực tiếp bên trong file MainMenuView
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

            // SỬA LỖI #2: TransactionController là một JPanel — cần bọc nó trong một cửa sổ JFrame.
            TransactionController transferPanel = new TransactionController(accountNumber);

            JFrame transferFrame = new JFrame("Chuyển khoản nội bộ");
            transferFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            transferFrame.setSize(820, 500);
            transferFrame.setLocationRelativeTo(null);
            transferFrame.setResizable(false);
            transferFrame.add(transferPanel);

            // Khi cửa sổ chuyển khoản bị đóng, quay trở lại menu chính
            transferFrame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    // Hiển thị lại menu chính nếu người dùng tắt cửa sổ này
                    view.setVisible(true);
                }
            });

            view.setVisible(false); // Ẩn menu chính trong lúc đang thực hiện chuyển khoản
            transferFrame.setVisible(true);
        });
    }

    // ── VẤN TIN SỐ DƯ ───────────────────────────────────────────────────────
    private void wireBalanceButton() {
        view.addBalanceListener(e -> {
            // Việc cần làm: Triển khai BalanceView + BalanceController
            // Tạm thời, hiển thị số dư trực tiếp từ phiên đăng nhập (session) để nút không bị vô dụng
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
            // Việc cần làm: Triển khai ChangePinView + ChangePinController
            JOptionPane.showMessageDialog(view,
                    "Tính năng đổi mã PIN đang được phát triển.",
                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    // ── IN SAO KÊ ────────────────────────────────────────────────────────────
    private void wireHistoryButton() {
        view.addTransactionHistoryListener(e -> {
            // Việc cần làm: Triển khai TransactionHistoryView + Controller
            JOptionPane.showMessageDialog(view,
                    "Tính năng in sao kê đang được phát triển.",
                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        });
    }
}