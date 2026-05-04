package controllers;

import DAO.CardDAO;
import models.Card;
import utils.SessionManager;
import views.LoginView;
import views.MainMenuView;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Date;
import java.time.LocalDate;

public class AuthController {
    private LoginView view;
    private CardDAO cardDAO;

    public AuthController(LoginView view) {
        this.view = view;
        this.cardDAO = new CardDAO();
        this.view.addLoginListener(new LoginListener());
    }

    class LoginListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String cardNumber = view.getCardNumber();
            String pin = view.getPin();

            if (cardNumber.isEmpty() || pin.isEmpty()) {
                view.showMessage("Vui lòng nhập đầy đủ Số thẻ và Mã PIN!");
                return;
            }

            // Gọi DAO để lấy dữ liệu thẻ từ CSDL
            Card card = cardDAO.getCardByNumber(cardNumber);

            if (card == null) {
                view.showMessage("Thẻ không tồn tại trong hệ thống!");
                view.clearFields();
                return;
            }

            // Kiểm tra trạng thái thẻ
            if ("Locked".equals(card.getStatus())) {
                view.showMessage("Thẻ của bạn đã bị khóa. Vui lòng liên hệ ngân hàng!");
                view.clearFields();
                return;
            }

            // Kiểm tra thẻ hết hạn
            Date currentDate = Date.valueOf(LocalDate.now());
            if (card.getExpiryDate().before(currentDate)) {
                view.showMessage("Thẻ của bạn đã hết hạn!");
                view.clearFields();
                return;
            }

            // Kiểm tra mã PIN
            if (card.getPin().equals(pin)) {
                // Đăng nhập thành công
                // Reset số lần nhập sai về 0
                cardDAO.updateFailedAttemptsAndStatus(cardNumber, 0, "Active");

                // Lưu session
                SessionManager.setCurrentCard(card);

                view.showMessage("Đăng nhập thành công!");
                view.dispose(); // Đóng màn hình đăng nhập

                // TODO: Mở MainMenuView tại đây
                 MainMenuView mainMenuView = new MainMenuView();
                new controllers.MainMenuController(mainMenuView);
                 mainMenuView.setVisible(true);



            } else {
                // Nhập sai PIN
                int attempts = card.getFailedAttempts() + 1;
                String status = "Active";

                if (attempts >= 3) {
                    status = "Locked";
                    cardDAO.updateFailedAttemptsAndStatus(cardNumber, attempts, status);
                    view.showMessage("Bạn đã nhập sai mã PIN 3 lần. Thẻ đã bị khóa!");
                } else {
                    cardDAO.updateFailedAttemptsAndStatus(cardNumber, attempts, status);
                    view.showMessage("Mã PIN không chính xác! Bạn còn " + (3 - attempts) + " lần thử.");
                }
            }
        }
    }
}