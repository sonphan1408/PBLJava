package  controllers;

import DAO.AccountDAO;
import DAO.ATMDAO;
import DAO.TransactionDAO;
import models.Account;
import models.ATM;
import models.Transaction;
import services.CashDispenserService;
import utils.SessionManager;
import views.MainMenuView;
import views.WithdrawView;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class WithdrawController {
    private WithdrawView view;
    private AccountDAO accountDAO;
    private ATMDAO atmDAO;
    private TransactionDAO transactionDAO;
    private CashDispenserService cashDispenserService;

    private final String CURRENT_ATM_ID = "ATM001";

    public WithdrawController(WithdrawView view) {
        this.view = view;
        this.accountDAO = new AccountDAO();
        this.atmDAO = new ATMDAO();
        this.transactionDAO = new TransactionDAO();
        this.cashDispenserService = new CashDispenserService();

        // Gắn sự kiện cho các nút
        this.view.addAmountListener(new PresetAmountListener());
        this.view.addOtherAmountListener(new OtherAmountListener());
        this.view.addCancelListener(e -> returnToMainMenu());

        // Gọi hàm load số dư lên màn hình ngay khi mở form
        loadCurrentBalance();
    }

    // --- HÀM MỚI: Lấy số dư và hiển thị ---
    private void loadCurrentBalance() {
        String accountNumber = SessionManager.getCurrentCard().getAccountNumber();
        Account account = accountDAO.getAccountByNumber(accountNumber);
        if (account != null) {
            // Định dạng số tiền có dấu phẩy phân cách hàng nghìn
            String formattedBalance = String.format("%,.0f VNĐ", account.getBalance());
            view.setBalance(formattedBalance);
        } else {
            view.setBalance("Lỗi không tải được số dư");
        }
    }

    private void returnToMainMenu() {
        view.dispose();
        MainMenuView mainMenuView = new MainMenuView();
         new controllers.MainMenuController(mainMenuView);// Chú ý: Nhớ gọi lại MainMenuController như đã fix lúc nãy
        mainMenuView.setVisible(true);
    }

    private void processWithdrawal(double amount) {
        if (amount <= 0 || amount % 50000 != 0) {
            view.showMessage("Số tiền rút phải ít nhất là  50.000 VNĐ!");
            return;
        }

        String accountNumber = SessionManager.getCurrentCard().getAccountNumber();
        Account account = accountDAO.getAccountByNumber(accountNumber);

        if (account.getBalance() < amount + 50000) {
            view.showMessage("Số dư trong tài khoản không đủ để thực hiện giao dịch!");
            return;
        }

        ATM atm = atmDAO.getATMById(CURRENT_ATM_ID);
        if (!cashDispenserService.calculateAndDispense(atm, amount)) {
            view.showMessage("Máy ATM hiện tại không đủ cơ cấu tiền mệnh giá phù hợp. Vui lòng nhập số khác!");
            return;
        }

        double newBalance = account.getBalance() - amount;
        boolean isUpdatedAcc = accountDAO.updateBalance(accountNumber, newBalance);
        boolean isUpdatedATM = atmDAO.updateATMCash(atm);

        if (isUpdatedAcc && isUpdatedATM) {
            Transaction trans = new Transaction(
                    0, 1, accountNumber, null, null, CURRENT_ATM_ID,
                    amount, account.getBalance(), newBalance,
                    "Rút tiền mặt tại ATM", "Success", null
            );
            transactionDAO.logTransaction(trans);

            view.showMessage("Giao dịch thành công! Vui lòng nhận tiền và thẻ.");
            returnToMainMenu();
        } else {
            view.showMessage("Lỗi hệ thống trong quá trình xử lý giao dịch!");
        }
    }

    class PresetAmountListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JButton source = (JButton) e.getSource();
            double amount = 0;
            switch (source.getText()) {
                case "500.000 VNĐ": amount = 500000; break;
                case "1.000.000 VNĐ": amount = 1000000; break;
                case "2.000.000 VNĐ": amount = 2000000; break;
                case "5.000.000 VNĐ": amount = 5000000; break;
            }

            int confirm = JOptionPane.showConfirmDialog(view, "Xác nhận rút " + String.format("%,.0f VNĐ", amount) + "?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                processWithdrawal(amount);
            }
        }
    }

    class OtherAmountListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String input = view.promptCustomAmount();
            if (input != null && !input.isEmpty()) {
                try {
                    double amount = Double.parseDouble(input);
                    processWithdrawal(amount);
                } catch (NumberFormatException ex) {
                    view.showMessage("Vui lòng chỉ nhập số!");
                }
            }
        }
    }
}