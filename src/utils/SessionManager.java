package utils;

import models.Card;

public class SessionManager {
    private static Card currentCard; // Lưu thông tin thẻ đang đăng nhập

    public static void setCurrentCard(Card card) {
        currentCard = card;
    }

    public static Card getCurrentCard() {
        return currentCard;
    }

    public static void logout() {
        currentCard = null;
    }

    public static boolean isLoggedIn() {
        return currentCard != null;
    }
}