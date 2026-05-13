package com.example.walnex;

import com.google.firebase.firestore.DocumentId;

/**
 * Firestore model for a user card stored at users/{uid}/cards/{cardId}.
 */
public class CardModel {

    public static final String TYPE_COSMIC_PURPLE = "cosmic_purple";
    public static final String TYPE_EMERALD_GREEN = "emerald_green";
    public static final String TYPE_OCEAN_BLUE    = "ocean_blue";
    public static final String TYPE_ROSE_WAVE     = "rose_wave";

    @DocumentId
    private String id;
    private String holderName;
    private String lastFour;
    private double balance;
    private String currency;
    private String cardType;
    private long   createdAt;

    // Required by Firestore
    public CardModel() {}

    public CardModel(String holderName, String lastFour,
                     double balance, String currency, String cardType) {
        this.holderName = holderName;
        this.lastFour   = lastFour;
        this.balance    = balance;
        this.currency   = currency;
        this.cardType   = cardType;
        this.createdAt  = System.currentTimeMillis();
    }

    public String getId()              { return id; }
    public void   setId(String v)      { this.id = v; }

    public String getHolderName()           { return holderName; }
    public void   setHolderName(String v)   { this.holderName = v; }

    public String getLastFour()             { return lastFour; }
    public void   setLastFour(String v)     { this.lastFour = v; }

    public double getBalance()              { return balance; }
    public void   setBalance(double v)      { this.balance = v; }

    public String getCurrency()             { return currency; }
    public void   setCurrency(String v)     { this.currency = v; }

    public String getCardType()             { return cardType; }
    public void   setCardType(String v)     { this.cardType = v; }

    public long   getCreatedAt()            { return createdAt; }
    public void   setCreatedAt(long v)      { this.createdAt = v; }
}