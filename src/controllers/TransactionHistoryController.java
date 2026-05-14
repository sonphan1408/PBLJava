package controllers;

import DAO.TransactionDAO;
import models.Transaction;
import utils.SessionManager;
import views.TransactionHistoryView;

import java.util.List;

public class TransactionHistoryController {
    private TransactionHistoryView view;
    private TransactionDAO transactionDAO;

    public TransactionHistoryController(TransactionHistoryView view) {
        this.view = view;
        this.transactionDAO = new TransactionDAO();

        // Load dữ liệu giao dịch
        loadTransactions();

        // Setup listener cho nút Làm mới
        view.getRefreshButton().addActionListener(e -> loadTransactions());
    }

    // ─── Load Transactions ────────────────────────────────────────────
    private void loadTransactions() {
        try {
            String accountNumber = SessionManager.getCurrentCard().getAccountNumber();
            List<Transaction> transactions = transactionDAO.getAllTransactionsByAccount(accountNumber);

            if (transactions.isEmpty()) {
                view.showMessage("Không có giao dịch nào.");
            } else {
                view.loadTransactions(transactions);
            }
        } catch (Exception e) {
            e.printStackTrace();
            view.showError("Lỗi khi tải lịch sử giao dịch!");
        }
    }
}
