package models;

import java.sql.Timestamp;

public class Account {
    private String accountNumber;
    private int customerId;
    private double balance;
    private String status;
    private Timestamp createdAt;

    // Constructor rỗng
    public Account() {}

    // Constructor đầy đủ
    public Account(String accountNumber, int customerId, double balance, String status, Timestamp createdAt) {
        this.accountNumber = accountNumber;
        this.customerId = customerId;
        this.balance = balance;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters và Setters
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}