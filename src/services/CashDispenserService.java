package services;

import models.ATM;
import java.util.logging.Logger;

/**
 * =====================================================================
 * CashDispenserService — Intelligent Cash Allocation (Backtracking)
 * =====================================================================
 * * Nhiệm vụ duy nhất: Tính toán số lượng tờ tiền cần nhả ra dựa trên số 
 * tiền yêu cầu và kho tiền hiện tại của máy ATM.
 *
 * CẢI TIẾN CỐT LÕI (So với bản cũ):
 * 1. Thuật toán Quay lui (Backtracking): Loại bỏ hoàn toàn lỗi của 
 * thuật toán Tham lam (Greedy) khi máy ATM bị thiếu hụt một loại mệnh giá 
 * nhưng vẫn có thể bù đắp bằng các mệnh giá nhỏ hơn.
 * 2. An toàn kiểu dữ liệu (Type Safety): Sử dụng `long` thay cho `double` 
 * trong toàn bộ phép toán để loại trừ sai số dấu phẩy động (Floating-point error).
 * 3. Bảo vệ dữ liệu (Data Protection): Kho tiền của ATM chỉ bị trừ đi 
 * khi thuật toán đã chắc chắn 100% tìm ra được phương án trả tiền hợp lệ.
 */
public class CashDispenserService {

    private static final Logger LOGGER = Logger.getLogger(CashDispenserService.class.getName());

    // ── Business Rules & Constants ───────────────────────────────────────
    private static final long[] DENOMS = {500_000L, 200_000L, 100_000L, 50_000L};
    private static final long MIN_WITHDRAW = 50_000L;
    private static final long MAX_WITHDRAW = 50_000_000L;

    // =====================================================================
    // Public API
    // =====================================================================

    public DispenseResult calculateAndDispense(ATM atm, double amountToWithdraw) {

        // 1. Kiểm tra trạng thái máy ATM
        if (atm == null || !"Active".equalsIgnoreCase(atm.getStatus())) {
            return DispenseResult.failure(FailureReason.ATM_UNAVAILABLE, 
                    "Máy ATM hiện đang tạm ngưng hoạt động hoặc không tồn tại.");
        }

        // Ép kiểu sang `long` để tính toán chính xác tuyệt đối
        long amount = Math.round(amountToWithdraw);

        // 2. Validate tính hợp lệ của số tiền yêu cầu
        DispenseResult validationError = validateAmount(amount, Math.round(atm.getTotalCash()));
        if (validationError != null) {
            return validationError;
        }

        // 3. Khởi tạo dữ liệu cho thuật toán Backtracking
        int[] inventory = {atm.getCount500k(), atm.getCount200k(), atm.getCount100k(), atm.getCount50k()};
        int[] take      = new int[4]; // Mảng lưu trữ số tờ tiền sẽ lấy ra

        // 4. Chạy thuật toán tìm tổ hợp tiền
        boolean canDispense = findCombination(amount, inventory, take, 0);

        if (!canDispense) {
            LOGGER.warning(String.format("[ATM %s] Failed to find combination for %,d VNĐ.", atm.getAtmId(), amount));
            return DispenseResult.failure(FailureReason.NO_SUITABLE_DENOMINATION,
                    "Máy ATM không có cơ cấu tiền phù hợp. Vui lòng nhập số tiền khác.");
        }

        // 5. Nếu thành công, cập nhật kho tiền thực tế của Object ATM
        atm.setCount500k(inventory[0] - take[0]);
        atm.setCount200k(inventory[1] - take[1]);
        atm.setCount100k(inventory[2] - take[2]);
        atm.setCount50k (inventory[3] - take[3]);

        LOGGER.info(String.format("[ATM %s] Dispensing %,d VNĐ — 500k×%d, 200k×%d, 100k×%d, 50k×%d",
                atm.getAtmId(), amount, take[0], take[1], take[2], take[3]));

        return DispenseResult.success(amount, take[0], take[1], take[2], take[3]);
    }

    // =====================================================================
    // Core Algorithm (Backtracking)
    // =====================================================================

    /**
     * Thuật toán Backtracking tìm tổ hợp tiền.
     * Thử lấy số lượng tối đa của mệnh giá lớn nhất, nếu bị tắc đường (không gom đủ số tiền)
     * thì lùi lại, nhả bớt mệnh giá lớn ra và thử dùng các mệnh giá nhỏ hơn.
     */
    private boolean findCombination(long remaining, int[] inventory, int[] take, int index) {
        // Base case 1: Đã gom đủ số tiền yêu cầu
        if (remaining == 0) return true;
        
        // Base case 2: Đã duyệt hết các mệnh giá mà vẫn chưa đủ tiền
        if (index >= DENOMS.length) return false;

        // Tính số lượng tờ tối đa có thể lấy của mệnh giá hiện tại
        int maxPossible = (int) (remaining / DENOMS[index]);
        int maxTake = Math.min(maxPossible, inventory[index]);

        // Thử nghiệm từ việc lấy nhiều tờ nhất có thể, giảm dần xuống 0
        for (int i = maxTake; i >= 0; i--) {
            take[index] = i;
            long newRemaining = remaining - (i * DENOMS[index]);
            
            // Đệ quy đi sâu vào nhánh mệnh giá nhỏ hơn
            if (findCombination(newRemaining, inventory, take, index + 1)) {
                return true; // Nếu nhánh này thành công, báo về ngay
            }
        }
        
        // Backtrack: Xóa dấu vết nếu tất cả các thử nghiệm ở mệnh giá này đều thất bại
        take[index] = 0;
        return false; 
    }

    // =====================================================================
    // Private Helpers
    // =====================================================================

    private DispenseResult validateAmount(long amount, long atmTotalCash) {
        if (amount <= 0) {
            return DispenseResult.failure(FailureReason.INVALID_AMOUNT, "Số tiền rút phải lớn hơn 0.");
        }
        if (amount < MIN_WITHDRAW) {
            return DispenseResult.failure(FailureReason.INVALID_AMOUNT, String.format("Số tiền rút tối thiểu là %,d VNĐ.", MIN_WITHDRAW));
        }
        if (amount > MAX_WITHDRAW) {
            return DispenseResult.failure(FailureReason.INVALID_AMOUNT, String.format("Số tiền rút tối đa mỗi lần là %,d VNĐ.", MAX_WITHDRAW));
        }
        if (amount % DENOMS[3] != 0) { // DENOMS[3] là 50.000
            return DispenseResult.failure(FailureReason.INVALID_AMOUNT, String.format("Số tiền phải là bội số của %,d VNĐ.", DENOMS[3]));
        }
        if (amount > atmTotalCash) {
            return DispenseResult.failure(FailureReason.ATM_INSUFFICIENT_CASH, "Máy ATM không đủ tiền để thực hiện giao dịch này.");
        }
        return null; // Không có lỗi
    }

    // =====================================================================
    // Value Objects & Enums
    // =====================================================================

    public static class DispenseResult {
        public final boolean success;
        public final long totalAmount;
        public final int count500k, count200k, count100k, count50k;
        public final FailureReason failureReason;
        public final String errorMessage;

        private DispenseResult(boolean success, long totalAmount, int count500k, int count200k, 
                               int count100k, int count50k, FailureReason reason, String error) {
            this.success = success;
            this.totalAmount = totalAmount;
            this.count500k = count500k;
            this.count200k = count200k;
            this.count100k = count100k;
            this.count50k = count50k;
            this.failureReason = reason;
            this.errorMessage = error;
        }

        static DispenseResult success(long amount, int c500k, int c200k, int c100k, int c50k) {
            return new DispenseResult(true, amount, c500k, c200k, c100k, c50k, null, null);
        }

        static DispenseResult failure(FailureReason reason, String message) {
            return new DispenseResult(false, 0, 0, 0, 0, 0, reason, message);
        }

        /**
         * Xây dựng chuỗi hiển thị hóa đơn chi tiết các tờ tiền nhả ra.
         */
        public String buildNoteBreakdown() {
            if (!success) return "";
            StringBuilder sb = new StringBuilder();
            String sep = "─".repeat(32) + "\n";
            if (count500k > 0) sb.append(String.format("  500.000 VNĐ  ×  %d%n", count500k));
            if (count200k > 0) sb.append(String.format("  200.000 VNĐ  ×  %d%n", count200k));
            if (count100k > 0) sb.append(String.format("  100.000 VNĐ  ×  %d%n", count100k));
            if (count50k  > 0) sb.append(String.format("   50.000 VNĐ  ×  %d%n", count50k));
            sb.append(sep);
            sb.append(String.format("  Tổng cộng: %,d VNĐ", totalAmount));
            return sb.toString();
        }
    }

    public enum FailureReason {
        ATM_UNAVAILABLE,
        INVALID_AMOUNT,
        ATM_INSUFFICIENT_CASH,
        NO_SUITABLE_DENOMINATION
    }
}