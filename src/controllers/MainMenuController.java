// File: src/controllers/MainMenuController.java
package controllers;

import views.MainMenuView;
import views.WithdrawView;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MainMenuController {
    private MainMenuView view;

    public MainMenuController(MainMenuView view) {
        this.view = view;

        // Gắn sự kiện click cho nút Rút tiền
        this.view.addWithdrawListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Logic mở màn hình Rút tiền nằm ở đây
                WithdrawView withdrawView = new WithdrawView();
                 new controllers.WithdrawController(withdrawView);
                withdrawView.setVisible(true);
                view.dispose(); // Đóng menu chính
            }
        });
    }
}