package com.prototypes.prototype.custommap;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.prototypes.prototype.R;

import java.util.ArrayList;

public class CustomMapAdaptor extends RecyclerView.Adapter<CustomMapAdaptor.MapViewHolder> {

    Context context;
    ArrayList<CustomMap> customMaps;
    FragmentActivity activity;

    public CustomMapAdaptor(Context context, ArrayList<CustomMap> customMaps, FragmentActivity activity) {
        this.context = context;
        this.customMaps = customMaps;
        this.activity = activity;
    }

    @NonNull
    @Override
    public CustomMapAdaptor.MapViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.custommap_item, parent, false);
        return new MapViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomMapAdaptor.MapViewHolder holder, int position) {
        CustomMap customMap = customMaps.get(position);
        if (position == 0){
            holder.customMapTxt.setText(customMap.getName());
            Glide.with(context)
                    .load(R.drawable.add_map_icon)
                    .into(holder.customMapImg);
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(activity, CreateCustomMap.class);
                activity.startActivity(intent);
            });
        }else{
            holder.customMapTxt.setText(customMap.getName());
            Glide.with(context)
                    .load(customMap.getImageUrl())
                    .into(holder.customMapImg); //TODO: add buffering img
            holder.itemView.setOnClickListener(v -> {
                String mapId = customMap.getId();
                CustomMapFragment fragment = CustomMapFragment.newInstance(mapId);
                activity
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            });
        }


    }

    @Override
    public int getItemCount() {
        return customMaps.size();
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
        customMaps.add(0, map); // Insert at the top
        notifyItemInserted(0);
    }
}
