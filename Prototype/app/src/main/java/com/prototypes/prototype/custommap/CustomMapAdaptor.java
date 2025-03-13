package com.prototypes.prototype.custommap;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.prototypes.prototype.R;

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
        holder.customMapTxt.setText(customMap.getName());
        Glide.with(context)
                .load(customMap.getImageUrl())
                .into(holder.customMapImg); //TODO: add buffering img
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
