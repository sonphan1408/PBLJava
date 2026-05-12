package DAO;

import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * InvoiceDAO - Xử lý logic xuất hóa đơn chuyển khoản ra file .txt
 */
public class InvoiceDAO {

    private static final String INVOICE_DIR = "invoices/";
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");

    // ==================== MODEL NỘI BỘ ====================

    /**
     * Lưu thông tin hóa đơn chuyển khoản
     */
    public static class InvoiceData {
        public String transactionId;       // Mã giao dịch
        public String senderAccountNo;    // Số tài khoản người gửi
        public String senderName;         // Tên người gửi
        public String receiverAccountNo;  // Số tài khoản người nhận
        public String receiverName;       // Tên người nhận
        public double amount;             // Số tiền chuyển
        public String content;            // Nội dung chuyển khoản
        public double balanceBefore;      // Số dư trước giao dịch
        public double balanceAfter;       // Số dư sau giao dịch
        public Date transactionTime;      // Thời gian giao dịch
        public String status;             // Trạng thái: "THÀNH CÔNG" / "THẤT BẠI"

        public InvoiceData() {
            this.transactionTime = new Date();
            this.status = "THÀNH CÔNG";
        }
    }

    // ==================== PHƯƠNG THỨC CHÍNH ====================

    /**
     * Xuất hóa đơn ra file .txt
     * Trả về đường dẫn file đã tạo, hoặc null nếu thất bại
     */
    public String exportInvoiceToTxt(InvoiceData data) {
        ensureInvoiceDirectory();

        String fileName = generateFileName(data);
        String filePath = INVOICE_DIR + fileName;

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), "UTF-8"))) {

            writer.write(buildInvoiceContent(data));
            System.out.println("[InvoiceDAO] Xuất hóa đơn thành công: " + filePath);
            return filePath;

        } catch (IOException e) {
            System.err.println("[InvoiceDAO] Lỗi khi xuất hóa đơn: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Xây dựng nội dung hóa đơn dạng text
     */
    public String buildInvoiceContent(InvoiceData data) {
        StringBuilder sb = new StringBuilder();

        String line  = "=".repeat(55);
        String dash  = "-".repeat(55);

        sb.append(line).append("\n");
        sb.append(center("NGÂN HÀNG TMCP ABC BANK", 55)).append("\n");
        sb.append(center("HÓA ĐƠN CHUYỂN KHOẢN NỘI BỘ", 55)).append("\n");
        sb.append(line).append("\n\n");

        // Mã giao dịch & thời gian
        sb.append(String.format("%-25s: %s%n", "Mã giao dịch", safe(data.transactionId)));
        sb.append(String.format("%-25s: %s%n", "Thời gian",
                data.transactionTime != null ? DATE_FORMAT.format(data.transactionTime) : "N/A"));
        sb.append(String.format("%-25s: %s%n", "Trạng thái", safe(data.status)));
        sb.append("\n");

        sb.append(dash).append("\n");
        sb.append(center("THÔNG TIN NGƯỜI CHUYỂN", 55)).append("\n");
        sb.append(dash).append("\n");
        sb.append(String.format("%-25s: %s%n", "Số tài khoản", safe(data.senderAccountNo)));
        sb.append(String.format("%-25s: %s%n", "Chủ tài khoản", safe(data.senderName)));
        sb.append(String.format("%-25s: %s VNĐ%n", "Số dư trước GD",
                MONEY_FORMAT.format(data.balanceBefore)));
        sb.append(String.format("%-25s: %s VNĐ%n", "Số dư sau GD",
                MONEY_FORMAT.format(data.balanceAfter)));
        sb.append("\n");

        sb.append(dash).append("\n");
        sb.append(center("THÔNG TIN NGƯỜI NHẬN", 55)).append("\n");
        sb.append(dash).append("\n");
        sb.append(String.format("%-25s: %s%n", "Số tài khoản", safe(data.receiverAccountNo)));
        sb.append(String.format("%-25s: %s%n", "Chủ tài khoản", safe(data.receiverName)));
        sb.append("\n");

        sb.append(dash).append("\n");
        sb.append(center("CHI TIẾT GIAO DỊCH", 55)).append("\n");
        sb.append(dash).append("\n");
        sb.append(String.format("%-25s: %s VNĐ%n", "Số tiền chuyển",
                MONEY_FORMAT.format(data.amount)));
        sb.append(String.format("%-25s: %s%n", "Nội dung CK", safe(data.content)));
        sb.append("\n");

        sb.append(line).append("\n");
        sb.append(center("Cảm ơn quý khách đã sử dụng dịch vụ!", 55)).append("\n");
        sb.append(center("Hotline: 1900 xxxx  |  abc-bank.vn", 55)).append("\n");
        sb.append(line).append("\n");

        return sb.toString();
    }

    /**
     * In hóa đơn ra console (dùng để preview hoặc debug)
     */
    public void printInvoiceToConsole(InvoiceData data) {
        System.out.println(buildInvoiceContent(data));
    }

    // ==================== TIỆN ÍCH ====================

    /** Tạo tên file theo mã GD và thời gian */
    private String generateFileName(InvoiceData data) {
        String timestamp = FILE_DATE_FORMAT.format(
                data.transactionTime != null ? data.transactionTime : new Date());
        String txId = (data.transactionId != null) ? data.transactionId : "UNKNOWN";
        return "HoaDon_" + txId + "_" + timestamp + ".txt";
    }

    /** Tạo thư mục lưu hóa đơn nếu chưa tồn tại */
    private void ensureInvoiceDirectory() {
        File dir = new File(INVOICE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /** Căn giữa chuỗi trong width ký tự */
    private String center(String text, int width) {
        if (text == null) return "";
        int pad = (width - text.length()) / 2;
        if (pad <= 0) return text;
        return " ".repeat(pad) + text;
    }

    /** Trả về chuỗi an toàn (tránh null) */
    private String safe(String s) {
        return s != null ? s : "";
    }
}
