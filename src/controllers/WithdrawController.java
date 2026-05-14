package controllers;

import DAO.WithdrawDAO;
import DAO.WithdrawDAO.WithdrawResult;
import utils.SessionManager;
import views.MainMenuView;
import views.WithdrawView;
import utils.DBConnection;

import javax.swing.*;
import java.text.DecimalFormat;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class WithdrawController {

    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,###");
    private final WithdrawView view;
    private final WithdrawDAO withdrawDAO = new WithdrawDAO(); // Gọi DAO

    public WithdrawController(WithdrawView view) {
        this.view = view;
        wireButtonListeners();
        loadCurrentBalance();
    }

    private void wireButtonListeners() {
        long[] presets = {500_000L, 1_000_000L, 2_000_000L, 5_000_000L};
        view.setPresetAmounts(presets);

        view.addPresetAmountListener(e -> {
            Long amount = (Long) ((JButton) e.getSource()).getClientProperty("amount");
            if (amount != null) handleWithdrawRequest(amount.doubleValue());
        });

        view.addOtherAmountListener(e -> {
            String input = view.promptCustomAmount();
            if (input == null || input.isBlank()) return;
            try {
                handleWithdrawRequest(Double.parseDouble(input.trim().replaceAll("[.,\\s]", "")));
            } catch (NumberFormatException ex) {
                view.showMessage("Vui lòng chỉ nhập số nguyên!");
            }
        });

        view.addCancelListener(e -> returnToMainMenu());
    }

    private void loadCurrentBalance() {
        String accountNo = SessionManager.getCurrentCard().getAccountNumber();
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                // Có thể dời luôn đoạn SELECT này vào AccountDAO cho triệt để
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement ps = conn.prepareStatement("SELECT Balance FROM Accounts WHERE AccountNumber = ?")) {
                    ps.setString(1, accountNo);
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next() ? MONEY_FMT.format(rs.getDouble("Balance")) : null;
                    }
                }
            }
            @Override
            protected void done() {
                try {
                    String balance = get();
                    view.setBalance(balance != null ? balance + " VNĐ" : "Lỗi đọc số dư");
                } catch (Exception e) {
                    view.setBalance("Lỗi kết nối CSDL");
                }
            }
        }.execute();
    }

    private void handleWithdrawRequest(double amount) {
        if (amount <= 0 || amount % 50_000 != 0) {
            view.showMessage("Số tiền không hợp lệ!\nVui lòng nhập bội số của 50.000 VNĐ.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(view,
                String.format("Xác nhận rút %s VNĐ?", MONEY_FMT.format(amount)),
                "Xác nhận giao dịch", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        view.setAllButtonsEnabled(false);
        view.setStatus("Đang xử lý...");

        new SwingWorker<WithdrawResult, Void>() {
            @Override
            protected WithdrawResult doInBackground() {
                // Gọi DAO xử lý DB
                String accountNo = SessionManager.getCurrentCard().getAccountNumber();
                return withdrawDAO.executeWithdrawal(accountNo, amount);
            }

            @Override
            protected void done() {
                view.setAllButtonsEnabled(true);
                view.setStatus("");
                try {
                    WithdrawResult result = get();
                    if (result.success) {
                        showSuccessReceipt(result);
                        returnToMainMenu();
                    } else {
                        view.showMessage(result.errorMessage);
                    }
                } catch (Exception e) {
                    view.showMessage("Lỗi hệ thống: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void showSuccessReceipt(WithdrawResult result) {
        String msg = String.format("✔ Giao dịch thành công!\n\nChi tiết tiền nhận:\n%s\n\nSố dư sau giao dịch: %s VNĐ",
                result.dispense.buildNoteBreakdown(), MONEY_FMT.format(result.newBalance));
        JOptionPane.showMessageDialog(view, msg, "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }

    private void returnToMainMenu() {
        view.dispose();
        SwingUtilities.invokeLater(() -> {
            MainMenuView mainView = new MainMenuView();
            new MainMenuController(mainView);
            mainView.setVisible(true);
        });
    }
}