package com.prototypes.prototype.custommap;

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
import com.prototypes.prototype.login.LoginActivity;
import com.prototypes.prototype.signup.SignUpActivity;

import java.util.ArrayList;

public class OtherCustomMapAdaptor extends RecyclerView.Adapter<OtherCustomMapAdaptor.MapViewHolder> {

    Context context;
    ArrayList<CustomMap> customMapArrayList;

    public OtherCustomMapAdaptor(Context context, ArrayList<CustomMap> customMapArrayList) {
        this.context = context;
        this.customMapArrayList = customMapArrayList;
    }

    @NonNull
    @Override
    public OtherCustomMapAdaptor.MapViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.custommap_item, parent, false);
        return new MapViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull OtherCustomMapAdaptor.MapViewHolder holder, int position) {
        CustomMap customMap = customMapArrayList.get(position);
        holder.customMapTxt.setText(customMap.getName());
        Glide.with(context)
                .load(customMap.getImageUrl())
                .into(holder.customMapImg); //TODO: add buffering img

        // Custom map section Onclick listener
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, CustomMapFull.class);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return customMapArrayList.size();
    }

    public static class MapViewHolder extends  RecyclerView.ViewHolder{
        ImageView customMapImg;
        TextView customMapTxt;
        public MapViewHolder(@NonNull View itemView) {
            super(itemView);
            customMapImg = itemView.findViewById(R.id.ivCustomMap);
            customMapTxt = itemView.findViewById(R.id.tvCustomMap);
        }
    }

}
