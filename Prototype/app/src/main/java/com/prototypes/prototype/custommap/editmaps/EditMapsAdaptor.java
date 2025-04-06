package com.prototypes.prototype.custommap.editmaps;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, EditMapDetails.class);
            intent.putExtra("mapId", customMap.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return customMapArrayList.size();
    }

    public static class EditMapsViewHolder extends  RecyclerView.ViewHolder{
        ImageView mapImage;
        TextView napName;
        public EditMapsViewHolder(@NonNull View itemView) {
            super(itemView);
            mapImage = itemView.findViewById(R.id.ivCustomMap);
            napName = itemView.findViewById(R.id.tvCustomMap);
        }
    }
}
