package com.example.revealapp.model;

import com.google.firebase.firestore.DocumentReference;

public class Dislike implements LikeInterface{
    private DocumentReference userRef;
    private DocumentReference postRef;

    public Dislike(DocumentReference userRef, DocumentReference postRef) {
        this.userRef = userRef;
        this.postRef = postRef;
    }

    public DocumentReference getUserRef() {
        return userRef;
    }

    public DocumentReference getPostRef() {
        return postRef;
    }

}
