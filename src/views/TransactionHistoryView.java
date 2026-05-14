package views;

import models.Transaction;
import utils.SessionManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.List;

public class TransactionHistoryView extends JFrame {
    private JTable tblTransactions;
    private DefaultTableModel tableModel;
    private JButton btnRefresh;
    private JButton btnClose;
    private List<Transaction> transactionList; // Lưu danh sách giao dịch

    public TransactionHistoryView() {
        setTitle("Hệ thống ATM - Lịch sử giao dịch");
        setSize(1000, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new BorderLayout());

        // ─── Header ───────────────────────────────────────────────────
        JLabel lblHeader = new JLabel("LỊCH SỬ GIAO DỊCH", SwingConstants.CENTER);
        lblHeader.setFont(new Font("Arial", Font.BOLD, 18));
        lblHeader.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));
        add(lblHeader, BorderLayout.NORTH);

        // ─── Table Panel ───────────────────────────────────────────────
        String[] columnNames = {"Loại", "Số tiền (VNĐ)", "Từ tài khoản", "Tới tài khoản", "Trạng thái", "Thời gian"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Không cho phép chỉnh sửa
            }
        };

        tblTransactions = new JTable(tableModel);
        tblTransactions.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblTransactions.setFont(new Font("Arial", Font.PLAIN, 12));
        tblTransactions.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        tblTransactions.setRowHeight(25);

        // Căn chỉnh cột
        tblTransactions.getColumnModel().getColumn(0).setPreferredWidth(120); // Loại
        tblTransactions.getColumnModel().getColumn(1).setPreferredWidth(120); // Số tiền
        tblTransactions.getColumnModel().getColumn(2).setPreferredWidth(120); // Từ tài khoản
        tblTransactions.getColumnModel().getColumn(3).setPreferredWidth(120); // Tới tài khoản
        tblTransactions.getColumnModel().getColumn(4).setPreferredWidth(80);  // Trạng thái
        tblTransactions.getColumnModel().getColumn(5).setPreferredWidth(140); // Thời gian

        // ─── Bắt sự kiện click vào hàng ───────────────────────────────
        tblTransactions.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int selectedRow = tblTransactions.getSelectedRow();
                if (selectedRow != -1 && transactionList != null && selectedRow < transactionList.size()) {
                    Transaction transaction = transactionList.get(selectedRow);
                    showTransactionDetails(transaction);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(tblTransactions);
        add(scrollPane, BorderLayout.CENTER);

        // ─── Button Panel ─────────────────────────────────────────────
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        btnRefresh = new JButton("Làm mới");
        btnClose = new JButton("Đóng");

        btnRefresh.setFont(new Font("Arial", Font.BOLD, 12));
        btnClose.setFont(new Font("Arial", Font.BOLD, 12));

        btnPanel.add(btnRefresh);
        btnPanel.add(btnClose);
        add(btnPanel, BorderLayout.SOUTH);

        btnClose.addActionListener(e -> this.dispose());
    }

    // ─── Populate Data ────────────────────────────────────────────────
    public void loadTransactions(List<Transaction> transactions) {
        this.transactionList = transactions; // Lưu danh sách
        tableModel.setRowCount(0); // Xóa dữ liệu cũ

        String currentAccount = SessionManager.getCurrentCard().getAccountNumber();
        DecimalFormat fmt = new DecimalFormat("#,###");
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        for (Transaction transaction : transactions) {
            // Kiểm tra nếu là nhận tiền thì hiển thị "Nhận tiền"
            String transactionType = "Nhận tiền".equals(transaction.getReceiverAccountNumber()) || 
                                     (transaction.getReceiverAccountNumber() != null && 
                                      transaction.getReceiverAccountNumber().equals(currentAccount))
                    ? "Nhận tiền"
                    : getTransactionTypeName(transaction.getTypeId());
            
            String receiver = transaction.getReceiverAccountNumber() != null ?
                    transaction.getReceiverAccountNumber() : (transaction.getReceiverBankCode() != null ?
                    "Ngân hàng " + transaction.getReceiverBankCode() : "N/A");

            Object[] row = {
                    transactionType,
                    fmt.format(transaction.getAmount()),
                    transaction.getFromAccount(),
                    receiver,
                    transaction.getStatus(),
                    dateFormat.format(transaction.getCreatedAt())
            };
            tableModel.addRow(row);
        }
    }

    // ─── Hiển thị chi tiết giao dịch ───────────────────────────────────
    private void showTransactionDetails(Transaction transaction) {
        String currentAccount = SessionManager.getCurrentCard().getAccountNumber();
        DecimalFormat fmt = new DecimalFormat("#,###");
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        // Kiểm tra nếu là nhận tiền
        String transactionType = (transaction.getReceiverAccountNumber() != null && 
                                  transaction.getReceiverAccountNumber().equals(currentAccount))
                ? "Nhận tiền"
                : getTransactionTypeName(transaction.getTypeId());

        String details = "╔════════════════════════════════════════════════════╗\n" +
                         "║                                     CHI TIẾT GIAO DỊCH                                     ║\n" +
                         "╚════════════════════════════════════════════════════╝\n\n" +
                         "Loại giao dịch: " + transactionType + "\n" +
                         "Số tiền: " + fmt.format(transaction.getAmount()) + " VNĐ\n" +
                         "Từ tài khoản: " + transaction.getFromAccount() + "\n" +
                         "Tới tài khoản: " + (transaction.getReceiverAccountNumber() != null ?
                         transaction.getReceiverAccountNumber() : "N/A") + "\n" +
                         "Trạng thái: " + transaction.getStatus() + "\n" +
                         "Số dư trước: " + fmt.format(transaction.getBalanceBefore()) + " VNĐ\n" +
                         "Số dư sau: " + fmt.format(transaction.getBalanceAfter()) + " VNĐ\n" +
                         "Thời gian: " + dateFormat.format(transaction.getCreatedAt()) + "\n\n" +
                         "─────────────────────────────────────────────────────\n" +
                         "Ghi chú: " + transaction.getDescription();

        JOptionPane.showMessageDialog(this, details, "Chi tiết giao dịch", JOptionPane.INFORMATION_MESSAGE);
    }

    // ─── Helper: Get Transaction Type Name ─────────────────────────
    private String getTransactionTypeName(int typeId) {
        switch (typeId) {
            case 1: return "Rút tiền mặt";
            case 2: return "Chuyển khoản nội bộ";
            case 3: return "Chuyển khoản liên ngân hàng";
            case 4: return "Đổi mã PIN";
            case 5: return "Vấn tin số dư";
            default: return "Khác";
        }
    }

    // ─── Get Refresh Button Listener ──────────────────────────────────
    public JButton getRefreshButton() {
        return btnRefresh;
    }

    // ─── Show Message ────────────────────────────────────────────────
    public void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    public void showError(String error) {
        JOptionPane.showMessageDialog(this, error, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }
}
