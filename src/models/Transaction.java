package models;

import java.sql.Timestamp;

public class Transaction {
    private int transactionId;
    private int typeId;
    private String fromAccount;
    private String receiverAccountNumber;
    private String receiverBankCode;
    private String atmId;
    private double amount;
    private double balanceBefore;
    private double balanceAfter;
    private String description;
    private String status;
    private Timestamp createdAt;

    public Transaction() {}

    public Transaction(int transactionId, int typeId, String fromAccount, String receiverAccountNumber,
                       String receiverBankCode, String atmId, double amount, double balanceBefore,
                       double balanceAfter, String description, String status, Timestamp createdAt) {
        this.transactionId = transactionId;
        this.typeId = typeId;
        this.fromAccount = fromAccount;
        this.receiverAccountNumber = receiverAccountNumber;
        this.receiverBankCode = receiverBankCode;
        this.atmId = atmId;
        this.amount = amount;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getTransactionId() { return transactionId; }
    public void setTransactionId(int transactionId) { this.transactionId = transactionId; }

    public int getTypeId() { return typeId; }
    public void setTypeId(int typeId) { this.typeId = typeId; }

    public String getFromAccount() { return fromAccount; }
    public void setFromAccount(String fromAccount) { this.fromAccount = fromAccount; }

    public String getReceiverAccountNumber() { return receiverAccountNumber; }
    public void setReceiverAccountNumber(String receiverAccountNumber) { this.receiverAccountNumber = receiverAccountNumber; }

    public String getReceiverBankCode() { return receiverBankCode; }
    public void setReceiverBankCode(String receiverBankCode) { this.receiverBankCode = receiverBankCode; }

    public String getAtmId() { return atmId; }
    public void setAtmId(String atmId) { this.atmId = atmId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public double getBalanceBefore() { return balanceBefore; }
    public void setBalanceBefore(double balanceBefore) { this.balanceBefore = balanceBefore; }

    public double getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(double balanceAfter) { this.balanceAfter = balanceAfter; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}