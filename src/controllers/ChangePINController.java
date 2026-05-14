package controllers;

import DAO.CardDAO;
import models.Card;
import utils.SessionManager;
import views.ChangePINView;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ChangePINController {
    private ChangePINView view;
    private CardDAO cardDAO;

    public ChangePINController(ChangePINView view) {
        this.view = view;
        this.cardDAO = new CardDAO();
        this.view.addConfirmListener(new ConfirmListener());
    }

    class ConfirmListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String oldPin = view.getOldPin();
            String newPin = view.getNewPin();
            String confirmPin = view.getConfirmPin();

            // ─── Kiểm tra nhập đầy đủ ─────────────────────────────────
            if (oldPin.isEmpty() || newPin.isEmpty() || confirmPin.isEmpty()) {
                view.showError("Vui lòng nhập đầy đủ thông tin!");
                return;
            }

            // ─── Kiểm tra PIN cũ ───────────────────────────────────────
            Card currentCard = SessionManager.getCurrentCard();
            if (!currentCard.getPin().equals(oldPin)) {
                view.showError("Mã PIN cũ không chính xác!");
                view.clearFields();
                return;
            }

            // ─── Kiểm tra độ dài PIN (tối thiểu 4 ký tự) ──────────────
            if (newPin.length() < 4) {
                view.showError("Mã PIN mới phải có ít nhất 4 ký tự!");
                view.clearFields();
                return;
            }

            // ─── Kiểm tra PIN mới và xác nhận trùng nhau ──────────────
            if (!newPin.equals(confirmPin)) {
                view.showError("Mã PIN xác nhận không khớp!");
                view.clearFields();
                return;
            }

            // ─── Kiểm tra PIN mới khác PIN cũ ────────────────────────
            if (newPin.equals(oldPin)) {
                view.showError("Mã PIN mới phải khác PIN cũ!");
                view.clearFields();
                return;
            }

            // ─── Cập nhật PIN vào Database ────────────────────────────
            boolean success = cardDAO.updatePin(currentCard.getCardNumber(), newPin);

            if (success) {
                // Cập nhật SessionManager
                currentCard.setPin(newPin);
                SessionManager.setCurrentCard(currentCard);

                view.showMessage("Đổi mã PIN thành công!");
                view.dispose();
            } else {
                view.showError("Đổi mã PIN thất bại. Vui lòng thử lại!");
                view.clearFields();
            }
        }
    }
}
