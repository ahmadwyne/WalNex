package com.example.walnex;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class CardRepository {

    private static final String COL_USERS = "users";
    private static final String COL_CARDS = "cards";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String uid;

    public CardRepository(String uid) {
        this.uid = uid;
    }

    public void addCard(CardModel card,
                        OnSuccessListener<DocumentReference> onSuccess,
                        OnFailureListener onFailure) {
        db.collection(COL_USERS)
          .document(uid)
          .collection(COL_CARDS)
          .add(card)
          .addOnSuccessListener(onSuccess)
          .addOnFailureListener(onFailure);
    }

    public void getCards(OnSuccessListener<QuerySnapshot> onSuccess,
                         OnFailureListener onFailure) {
        db.collection(COL_USERS)
          .document(uid)
          .collection(COL_CARDS)
          .orderBy("createdAt", Query.Direction.ASCENDING)
          .get()
          .addOnSuccessListener(onSuccess)
          .addOnFailureListener(onFailure);
    }
}