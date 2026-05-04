package services;

import models.ATM;

public class CashDispenserService {

    // Trả về true nếu tính toán chia tiền thành công và cập nhật lại đối tượng ATM
    public boolean calculateAndDispense(ATM atm, double amountToWithdraw) {
        if (amountToWithdraw > atm.getTotalCash()) {
            return false; // Máy không đủ tổng tiền
        }

        // Tạo biến tạm để tính toán, tránh làm hỏng dữ liệu gốc nếu thất bại giữa chừng
        int temp500k = atm.getCount500k();
        int temp200k = atm.getCount200k();
        int temp100k = atm.getCount100k();
        int temp50k = atm.getCount50k();
        double remainingAmount = amountToWithdraw;

        // Thuật toán chia tiền (Greedy Algorithm)
        int need500k = (int) (remainingAmount / 500000);
        int take500k = Math.min(need500k, temp500k);
        remainingAmount -= take500k * 500000;
        temp500k -= take500k;

        int need200k = (int) (remainingAmount / 200000);
        int take200k = Math.min(need200k, temp200k);
        remainingAmount -= take200k * 200000;
        temp200k -= take200k;

        int need100k = (int) (remainingAmount / 100000);
        int take100k = Math.min(need100k, temp100k);
        remainingAmount -= take100k * 100000;
        temp100k -= take100k;

        int need50k = (int) (remainingAmount / 50000);
        int take50k = Math.min(need50k, temp50k);
        remainingAmount -= take50k * 50000;
        temp50k -= take50k;

        // Nếu chia hết số tiền cần rút
        if (remainingAmount == 0) {
            // Cập nhật lại đối tượng ATM
            atm.setCount500k(temp500k);
            atm.setCount200k(temp200k);
            atm.setCount100k(temp100k);
            atm.setCount50k(temp50k);
            return true;
        }

        return false; // ATM có tổng tiền đủ nhưng không có mệnh giá phù hợp
    }
}