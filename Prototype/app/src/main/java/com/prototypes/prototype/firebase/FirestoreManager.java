package com.prototypes.prototype.firebase;

import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FirestoreManager<T> {
    private FirebaseFirestore db;
    private Class<T> type;

    // Constructor for Dependency Injection and specifying the object type
    public FirestoreManager(FirebaseFirestore db, Class<T> type) {
        this.db = db;
        this.type = type;
    }

    // Create or Update a document
    public void writeDocument(String collection, String documentId, T object, FirestoreCallback callback) {
        db.collection(collection).document(documentId)
                .set(object)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    // Read a document
    public void readDocument(String collection, String documentId, FirestoreReadCallback<T> callback) {
        db.collection(collection).document(documentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        T object = documentSnapshot.toObject(type);
                        callback.onSuccess(object);
                    } else {
                        callback.onFailure(new Exception("Document does not exist"));
                    }
                })
                .addOnFailureListener(callback::onFailure);

    }

    public void queryDocuments(String collection, String filterField, String filterValue ,FirestoreQueryCallback<T> callback) {
        db.collection(collection)
                .whereEqualTo(filterField, filterValue)
//                .orderBy(orderByField)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ArrayList<T> resultList = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        T object = document.toObject(type);
                        if (object != null) {
                            resultList.add(object);
                        }
                    }
                    callback.onSuccess(resultList);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // Delete a document
    public void deleteDocument(String collection, String documentId, FirestoreCallback callback) {
        db.collection(collection).document(documentId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    public interface FirestoreCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface FirestoreReadCallback<T> {
        void onSuccess(T object);
        void onFailure(Exception e);
    }

    // Callback interface for queries
    public interface FirestoreQueryCallback<T> {
        void onSuccess(ArrayList<T> results);
        void onFailure(Exception e);
    }
}
