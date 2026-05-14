package controllers;

import DAO.TransferDAO;
import DAO.InvoiceDAO;
import DAO.InvoiceDAO.InvoiceData;
import views.TransactionView;
import utils.DBConnection;

import javax.swing.*;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TransactionController {

    private static final Logger LOGGER = Logger.getLogger(TransactionController.class.getName());

    // Tham chiếu đến giao diện
    private TransactionView view;

    // State
    private final String loggedInAccountNo;
    private InvoiceData lastInvoice;
    private final InvoiceDAO invoiceDAO = new InvoiceDAO();
    private final TransferDAO transferDAO = new TransferDAO();

    public TransactionController(TransactionView view, String accountNo) {
        this.view = view;
        this.loggedInAccountNo = accountNo;

        // Gắn sự kiện từ View vào các hàm xử lý bên dưới
        this.view.addLookupListener(e -> lookupReceiver());
        this.view.addTransferListener(e -> executeTransfer());
        this.view.addPrintInvoiceListener(e -> printInvoice());
        this.view.addResetListener(e -> resetForm());

        // Tải thông tin ban đầu
        resetForm();
    }

    private void lookupReceiver() {
        String receiverNo = view.getReceiverAccount();
        if (receiverNo.isEmpty() || receiverNo.equals(loggedInAccountNo)) {
            view.showStatus("Số tài khoản nhận không hợp lệ!", view.getDangerColor());
            return;
        }

        view.setTransferButtonsEnabled(false);
        view.showStatus("Đang tra cứu...", view.getPrimaryColor());

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return dbLookupReceiver(receiverNo);
            }

            @Override
            protected void done() {
                view.setTransferButtonsEnabled(true);
                try {
                    String name = get();
                    if (name != null) {
                        view.setReceiverName(name.toUpperCase(), true);
                        view.showStatus("Tìm thấy tài khoản.", view.getPrimaryColor());
                    } else {
                        view.setReceiverName("Không tìm thấy tài khoản", false);
                        view.showStatus("Tài khoản không tồn tại!", view.getDangerColor());
                    }
                } catch (Exception e) {
                    view.showStatus("Lỗi tra cứu: " + e.getMessage(), view.getDangerColor());
                }
            }
        }.execute();
    }

    private void executeTransfer() {
        String receiverNo   = view.getReceiverAccount();
        String amountStr    = view.getAmount();
        String content      = view.getContent();
        String receiverName = view.getReceiverName();

        // Validate cơ bản trên UI
        if (receiverNo.isEmpty() || amountStr.isEmpty()) {
            view.showStatus("Vui lòng điền đầy đủ thông tin!", view.getDangerColor());
            return;
        }

        double amount;
        try { amount = Double.parseDouble(amountStr); }
        catch (NumberFormatException ex) { view.showStatus("Số tiền không hợp lệ!", view.getDangerColor()); return; }

        int confirm = JOptionPane.showConfirmDialog(view,
                String.format("Xác nhận chuyển %s VNĐ tới %s?", TransactionView.MONEY_FMT.format(amount), receiverName),
                "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        view.setTransferButtonsEnabled(false);
        view.showStatus("Đang xử lý...", view.getPrimaryColor());

        new SwingWorker<TransactionResult, Void>() {
            @Override
            protected TransactionResult doInBackground() {
                try {
                    String[] result = transferDAO.transfer(loggedInAccountNo, receiverNo, amount, content);

                    String txId = result[0];
                    double newBalance = Double.parseDouble(result[1]);

                    InvoiceData invoice = new InvoiceData();
                    invoice.transactionId = txId;
                    invoice.senderAccountNo = loggedInAccountNo;
                    invoice.senderName = view.getSenderName();
                    invoice.receiverAccountNo = receiverNo;
                    invoice.receiverName = receiverName;
                    invoice.amount = amount;
                    invoice.content = content;
                    invoice.balanceAfter = newBalance;
                    invoice.transactionTime = new Date();

                    return TransactionResult.success(txId, newBalance, invoice);

                } catch (TransferDAO.TransferException e) {
                    return TransactionResult.failure(e.getMessage());
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Unexpected error", e);
                    return TransactionResult.failure("Lỗi hệ thống: " + e.getMessage());
                }
            }

            @Override
            protected void done() {
                view.setTransferButtonsEnabled(true);
                try {
                    TransactionResult result = get();
                    if (result.success) {
                        view.setSenderInfo(view.getSenderName(), TransactionView.MONEY_FMT.format(result.newSenderBalance));
                        view.showStatus("✔ Thành công! Mã GD: " + result.txId, view.getSuccessColor());
                        lastInvoice = result.invoice;
                        view.setPrintInvoiceVisible(true);
                    } else {
                        view.showStatus(result.errorMessage, view.getDangerColor());
                    }
                } catch (Exception e) {
                    view.showStatus("Lỗi: " + e.getMessage(), view.getDangerColor());
                }
            }
        }.execute();
    }

    private void printInvoice() {
        if (lastInvoice != null) {
            String path = invoiceDAO.exportInvoiceToTxt(lastInvoice);
            JOptionPane.showMessageDialog(view, "Hóa đơn đã được lưu tại: " + path);
        }
    }

    private void resetForm() {
        view.resetFields();
        loadSenderInfo();
    }

    // Giữ nguyên các hàm phụ trợ SQL
    private String dbLookupReceiver(String receiverNo) throws java.sql.SQLException {
        try (java.sql.Connection conn = DBConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(
                     "SELECT FullName FROM Accounts a JOIN Customers c ON a.CustomerID = c.CustomerID WHERE AccountNumber = ?")) {
            ps.setString(1, receiverNo);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("FullName") : null;
            }
        }
    }

    private void loadSenderInfo() {
        new SwingWorker<String[], Void>() {
            @Override
            protected String[] doInBackground() throws Exception {
                try (java.sql.Connection conn = DBConnection.getConnection();
                     java.sql.PreparedStatement ps = conn.prepareStatement(
                             "SELECT c.FullName, a.Balance FROM Accounts a JOIN Customers c ON a.CustomerID = c.CustomerID WHERE a.AccountNumber = ?")) {
                    ps.setString(1, loggedInAccountNo);
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return new String[]{rs.getString("FullName"), TransactionView.MONEY_FMT.format(rs.getDouble("Balance"))};
                        }
                    }
                }
                return new String[]{"N/A", "0"};
            }
            @Override
            protected void done() {
                try {
                    String[] res = get();
                    view.setSenderInfo(res[0].toUpperCase(), res[1]);
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    // Lớp chứa kết quả trả về
    private static class TransactionResult {
        final boolean success;
        final String txId;
        final double newSenderBalance;
        final InvoiceData invoice;
        final String errorMessage;

        private TransactionResult(boolean success, String txId, double newBalance, InvoiceData invoice, String error) {
            this.success = success; this.txId = txId; this.newSenderBalance = newBalance;
            this.invoice = invoice; this.errorMessage = error;
        }
        static TransactionResult success(String id, double bal, InvoiceData inv) { return new TransactionResult(true, id, bal, inv, null); }
        static TransactionResult failure(String msg) { return new TransactionResult(false, null, 0, null, msg); }
    }
}