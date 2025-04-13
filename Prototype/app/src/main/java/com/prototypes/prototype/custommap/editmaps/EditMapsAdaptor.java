package com.prototypes.prototype.custommap.editmaps;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.prototypes.prototype.R;
import com.prototypes.prototype.custommap.CustomMap;

import java.util.ArrayList;

public class EditMapsAdaptor extends RecyclerView.Adapter<EditMapsAdaptor.EditMapsViewHolder> {
    Context context;
    ArrayList<CustomMap> customMapArrayList;

    public EditMapsAdaptor(Context context, ArrayList<CustomMap> customMapArrayList) {
        this.context = context;
        this.customMapArrayList = customMapArrayList;
    }

    @NonNull
    @Override
    public EditMapsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.edit_map_row, parent, false);
        return new EditMapsAdaptor.EditMapsViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull EditMapsViewHolder holder, int position) {
        CustomMap customMap = customMapArrayList.get(position);
        holder.napName.setText(customMap.getName());
        Glide.with(context)
                .load(customMap.getImageUrl())
                .into(holder.mapImage); //TODO: add buffering img

//        holder.itemView.setOnClickListener(v -> Toast.makeText(context, "Clicked on: " + customMap.getName(), Toast.LENGTH_SHORT).show());
        holder.btnDelete.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle("Delete Map")
                    .setMessage("Are you sure you want to delete \"" + customMap.getName() + "\"?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        String docId = customMap.getId();
                        if (docId != null && !docId.isEmpty()) {
                            FirebaseFirestore.getInstance()
                                    .collection("map")
                                    .document(docId)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        int bindingAdapterPosition = holder.getBindingAdapterPosition();
                                        customMapArrayList.remove(bindingAdapterPosition);
                                        notifyItemRemoved(bindingAdapterPosition);
                                        notifyItemRangeChanged(bindingAdapterPosition, customMapArrayList.size());
                                        Toast.makeText(context, "Deleted " + customMap.getName(), Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(context, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        } else {
                            Toast.makeText(context, "Document ID not found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });    }

    @Override
    public int getItemCount() {
        return customMapArrayList.size();
    }

    public static class EditMapsViewHolder extends  RecyclerView.ViewHolder{
        ImageView mapImage;
        TextView napName;
        ImageButton btnDelete;
        public EditMapsViewHolder(@NonNull View itemView) {
            super(itemView);
            mapImage = itemView.findViewById(R.id.ivCustomMap);
            napName = itemView.findViewById(R.id.tvCustomMap);
            btnDelete = itemView.findViewById(R.id.btnDeleteMap);
        }
    }
}
