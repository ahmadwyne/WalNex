package com.example.walnex;

public class BillerModel {
    public String id;
    public String category;      // "Electricity", "Water", etc.
    public String accountNumber; // registration / reference number
    public double dueAmount;
    public long   dueDateMs;     // epoch ms

    public BillerModel() {}

    public BillerModel(String id, String category, String accountNumber,
                       double dueAmount, long dueDateMs) {
        this.id            = id;
        this.category      = category;
        this.accountNumber = accountNumber;
        this.dueAmount     = dueAmount;
        this.dueDateMs     = dueDateMs;
    }
}
