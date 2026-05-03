package com.example.walnex.transfer;

/**
 * Represents a contact the current user can transfer money to.
 * Sourced from Firestore (users collection) matched by phone number.
 */
public class TransferContact {

    public final String uid;           // Firestore UID of the recipient
    public final String fullName;      // Display name
    public final String phoneE164;     // e.g. "+923001234567"
    public final int    avatarRes;     // drawable resource id; 0 = use default

    public TransferContact(String uid, String fullName, String phoneE164, int avatarRes) {
        this.uid        = uid;
        this.fullName   = fullName;
        this.phoneE164  = phoneE164;
        this.avatarRes  = avatarRes;
    }
}