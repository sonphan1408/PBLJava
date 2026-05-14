package main;

import controllers.AuthController;
import views.LoginView;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            LoginView loginView = new LoginView();
            new AuthController(loginView);
            loginView.setVisible(true);
        });
    }
}