package models;

public class ATM {
    private String atmId;
    private String location;
    private int count500k;
    private int count200k;
    private int count100k;
    private int count50k;
    private double totalCash;
    private String status;

    public ATM() {}

    public ATM(String atmId, String location, int count500k, int count200k, int count100k, int count50k, double totalCash, String status) {
        this.atmId = atmId;
        this.location = location;
        this.count500k = count500k;
        this.count200k = count200k;
        this.count100k = count100k;
        this.count50k = count50k;
        this.totalCash = totalCash;
        this.status = status;
    }

    // Getters and Setters
    public String getAtmId() { return atmId; }
    public void setAtmId(String atmId) { this.atmId = atmId; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getCount500k() { return count500k; }
    public void setCount500k(int count500k) { this.count500k = count500k; }

    public int getCount200k() { return count200k; }
    public void setCount200k(int count200k) { this.count200k = count200k; }

    public int getCount100k() { return count100k; }
    public void setCount100k(int count100k) { this.count100k = count100k; }

    public int getCount50k() { return count50k; }
    public void setCount50k(int count50k) { this.count50k = count50k; }

    public double getTotalCash() { return totalCash; }
    public void setTotalCash(double totalCash) { this.totalCash = totalCash; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}