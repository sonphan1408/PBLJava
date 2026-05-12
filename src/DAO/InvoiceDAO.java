package DAO;

import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class InvoiceDAO {

    private static final String INVOICE_DIR = "invoices/";
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,###");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private static final SimpleDateFormat FILE_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");

    public static class InvoiceData {
        public String transactionId;     
        public String senderAccountNo;   
        public String senderName;
        public String receiverAccountNo; 
        public String receiverName;       
        public double amount;             
        public String content;            
        public double balanceBefore;     
        public double balanceAfter;       
        public Date transactionTime;      
        public String status;             

        public InvoiceData() {
            this.transactionTime = new Date();
            this.status = "THÀNH CÔNG";
        }
    }

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

    public String buildInvoiceContent(InvoiceData data) {
        StringBuilder sb = new StringBuilder();

        String line  = "=".repeat(55);
        String dash  = "-".repeat(55);

        sb.append(line).append("\n");
        sb.append(center("NGÂN HÀNG TMCP ABC BANK", 55)).append("\n");
        sb.append(center("HÓA ĐƠN CHUYỂN KHOẢN NỘI BỘ", 55)).append("\n");
        sb.append(line).append("\n\n");

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

    public void printInvoiceToConsole(InvoiceData data) {
        System.out.println(buildInvoiceContent(data));
    }

    private String generateFileName(InvoiceData data) {
        String timestamp = FILE_DATE_FORMAT.format(
                data.transactionTime != null ? data.transactionTime : new Date());
        String txId = (data.transactionId != null) ? data.transactionId : "UNKNOWN";
        return "HoaDon_" + txId + "_" + timestamp + ".txt";
    }

    private void ensureInvoiceDirectory() {
        File dir = new File(INVOICE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private String center(String text, int width) {
        if (text == null) return "";
        int pad = (width - text.length()) / 2;
        if (pad <= 0) return text;
        return " ".repeat(pad) + text;
    }

    private String safe(String s) {
        return s != null ? s : "";
    }
}
