package controllers;

import views.*;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainMenuController {

    private final MainMenuView view;

    public MainMenuController(MainMenuView view) {
        this.view = view;

        wireWithdrawButton();
        wireTransferButton();
        wireInterbankTransferButton();
        wireBalanceButton();
        wireChangePinButton();
        wireHistoryButton();
    }

    // ── 1. RÚT TIỀN ─────────────────────────────────────────────────────────────
    private void wireWithdrawButton() {
        view.addWithdrawListener(e -> {
            WithdrawView withdrawView = new WithdrawView();
            new WithdrawController(withdrawView);
            withdrawView.setVisible(true);
            view.dispose();
        });
    }

    // ── 2. CHUYỂN KHOẢN NỘI BỘ ─────────────────────────────────────────────────
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

            // ĐÃ FIX LỖI MERGE DƯỚI ĐÂY: Thêm sự kiện đóng cửa sổ và hiển thị frame
            transferFrame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    view.setVisible(true); // Hiển thị lại Menu chính khi đóng
                }
            });

            view.setVisible(false); // Ẩn Menu chính
            transferFrame.setVisible(true); // Hiện cửa sổ chuyển khoản
        }); // <-- ĐÃ FIX: Thêm dấu đóng ngoặc }); bị thiếu
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

            view.setVisible(false);
            frame.setVisible(true);
        });
    }

    // ── 4. VẤN TIN SỐ DƯ ─────────────────────────────────────────────────────
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

    // ── 5. ĐỔI MÃ PIN ──────────────────────────────────────────────────────────
    private void wireChangePinButton() {
        view.addChangePinListener(e -> {
            ChangePINView changePinView = new ChangePINView();
            new ChangePINController(changePinView);
            changePinView.setVisible(true);
        });
    }

    // ── IN SAO KÊ ────────────────────────────────────────────────────────────
    private void wireHistoryButton() {
        view.addTransactionHistoryListener(e -> {
            TransactionHistoryView historyView = new TransactionHistoryView();
            new TransactionHistoryController(historyView);
            historyView.setVisible(true);
        });
    }
}