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
import com.prototypes.prototype.CreateCustomMap;
import com.prototypes.prototype.R;
import com.prototypes.prototype.login.LoginActivity;
import com.prototypes.prototype.signup.SignUpActivity;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class CustomMapAdaptor extends RecyclerView.Adapter<CustomMapAdaptor.MapViewHolder> {

    Context context;
    ArrayList<CustomMap> customMapArrayList;

    public CustomMapAdaptor(Context context, ArrayList<CustomMap> customMapArrayList) {
        this.context = context;
        this.customMapArrayList = customMapArrayList;
    }

    @NonNull
    @Override
    public CustomMapAdaptor.MapViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.custommap_item, parent, false);
        return new MapViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomMapAdaptor.MapViewHolder holder, int position) {
        CustomMap customMap = customMapArrayList.get(position);

        if (position == 0){
            holder.customMapTxt.setText(customMap.getName());
            Glide.with(context)
                    .load(R.drawable.add_map_icon)
                    .into(holder.customMapImg);
        }else{
            holder.customMapTxt.setText(customMap.getName());
            Glide.with(context)
                    .load(customMap.getImageUrl())
                    .into(holder.customMapImg); //TODO: add buffering img
        }

        // Custom map section Onclick listener
        holder.itemView.setOnClickListener(v -> {
            if (position == 0) {
                // Handle "Add" map click
                // Toast.makeText(context, "Add new map clicked", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(context, CreateCustomMap.class);
                context.startActivity(intent);
            } else {
                // Handle normal map click
                Toast.makeText(context, "Clicked on: " + customMap.getName(), Toast.LENGTH_SHORT).show();
            }
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

    public void addItemToTop(CustomMap map) {
        customMapArrayList.add(0, map); // Insert at the top
        notifyItemInserted(0);
    }
}
