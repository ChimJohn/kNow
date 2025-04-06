package com.prototypes.prototype.firebase;

import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.prototypes.prototype.custommap.CustomMap;
import com.prototypes.prototype.story.Story;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreManager<T> {
    private FirebaseFirestore db;
    private Class<T> type;

    // Constructor for Dependency Injection and specifying the object type
    public FirestoreManager(FirebaseFirestore db, Class<T> type) {
        this.db = db;
        this.type = type;
    }

    // Write to a specific document
    // Inputs: collection, documentID and Object e.g. user object
    public void writeDocument(String collection, String documentId, T object, FirestoreCallback callback) {
        if (documentId == null) {
            db.collection(collection)
                    .add(object)
                    .addOnSuccessListener(unused -> callback.onSuccess())
                    .addOnFailureListener(callback::onFailure);
        } else {
            db.collection(collection).document(documentId)
                    .set(object)
                    .addOnSuccessListener(unused -> callback.onSuccess())
                    .addOnFailureListener(callback::onFailure);
        }
    }

    public void writeDocumentWithId(String collection, String documentId, T object, FirestoreCallback callback) {
        // If no documentId is provided, create a new document and add the ID as a field
        db.collection(collection)
                .add(object)
                .addOnSuccessListener(documentReference -> {
                    // Adding document ID to the object
                    Map<String, Object> updatedObject = new HashMap<>();
                    updatedObject.put("id", documentReference.getId());
                    // Update the document with the document ID fiel
                    documentReference.update(updatedObject)
                            .addOnSuccessListener(aVoid -> callback.onSuccess())
                            .addOnFailureListener(callback::onFailure);
                })
                .addOnFailureListener(callback::onFailure);

    }


    // Update specific attribute
    // Inputs: collection, documentID, attributeName in Firestore, new value
    public void updateDocument(String collection, String documentId, String attributeName, String updatedValue,
            FirestoreCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put(attributeName, updatedValue);
        db.collection(collection).document(documentId)
                .set(data, SetOptions.merge());
    }

    // Read a document
    // Inputs: collection, documentID
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

    // Query a collection for documents. Filters a field
    // Inputs: collection, filterField (Field in document), and filterValue (Value in field)
    public void queryDocuments(String collection, String filterField, String filterValue,
            FirestoreQueryCallback<T> callback) {
        db.collection(collection)
                .whereEqualTo(filterField, filterValue)
                // .orderBy(orderByField)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ArrayList<T> resultList = new ArrayList<>();
                    if (queryDocumentSnapshots.isEmpty()) {
                        callback.onEmpty(resultList);
                        return;
                    }
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        T object = document.toObject(type);

                        // Set document ID for Story/CustomMap objects
                        if (object instanceof Story) {
                            ((Story) object).setId(document.getId()); // Add this line
                        } else if (object instanceof CustomMap) {
                            ((CustomMap) object).setId(document.getId());
                        }

                        if (object != null) {
                            resultList.add(object);
                        }
                    }
                    callback.onSuccess(resultList);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // Query a collection for documents. Filters array field
    // Inputs: collection, arrayField (Field in document), arrayValue (Value in field)
    public void queryArrayInDocuments(String collection, String arrayField, String arrayValue,
            FirestoreQueryCallback<T> callback) {
        db.collection(collection)
                .whereArrayContains(arrayField, arrayValue)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ArrayList<T> resultList = new ArrayList<>();
                    if (queryDocumentSnapshots.isEmpty()) {
                        callback.onEmpty(resultList);
                        return;
                    }
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

    // Add to Array Field in document
    public void addToArray(String collection, String documentId, String arrayField,
            Object value, FirestoreCallback callback) {
        db.collection(collection)
                .document(documentId)
                .update(arrayField, FieldValue.arrayUnion(value))
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    // Remove from Array Field in document
    public void removeFromArray(String collection, String documentId, String arrayField,
            Object value, FirestoreCallback callback) {
        db.collection(collection)
                .document(documentId)
                .update(arrayField, FieldValue.arrayRemove(value))
                .addOnFailureListener(callback::onFailure)
                .addOnSuccessListener(aVoid -> callback.onSuccess());
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
        void onEmpty(ArrayList<T> results);

        void onSuccess(ArrayList<T> results);

        void onFailure(Exception e);
    }
}