package models;

import java.sql.Date;

public class Card {
    private String cardNumber;
    private String accountNumber;
    private String pin;
    private int failedAttempts;
    private String status;
    private Date expiryDate;

    // Constructors
    public Card() {}

    public Card(String cardNumber, String accountNumber, String pin, int failedAttempts, String status, Date expiryDate) {
        this.cardNumber = cardNumber;
        this.accountNumber = accountNumber;
        this.pin = pin;
        this.failedAttempts = failedAttempts;
        this.status = status;
        this.expiryDate = expiryDate;
    }

    // Getters and Setters
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }

    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Date expiryDate) { this.expiryDate = expiryDate; }
}