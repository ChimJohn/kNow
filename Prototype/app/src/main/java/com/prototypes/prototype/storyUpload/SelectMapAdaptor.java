package com.prototypes.prototype.storyUpload;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.prototypes.prototype.R;
import com.prototypes.prototype.custommap.CustomMap;

import java.util.ArrayList;

public class SelectMapAdaptor extends RecyclerView.Adapter<SelectMapAdaptor.MapChipHolder>{

    Context context;
    ArrayList<CustomMap> customMapArrayList;
    private final ArrayList<String> selectedCustomMapsId = new ArrayList<>();

    public SelectMapAdaptor(Context context, ArrayList<CustomMap> customMapArrayList) {
        this.context = context;
        this.customMapArrayList = customMapArrayList;
    }

    @NonNull
    @Override
    public MapChipHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.custom_map_chip, parent, false);
        return new SelectMapAdaptor.MapChipHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MapChipHolder holder, int position) {
        CustomMap customMap = customMapArrayList.get(position);
        holder.mapChip.setText(customMap.getName());
        holder.mapChip.setOnClickListener(v -> {
            if (holder.mapChip.isChecked()) {
                selectedCustomMapsId.add(customMap.getId());
                Log.d("SelectMapAdaptor", "Selected: " + customMap.getId());
            } else {
                selectedCustomMapsId.remove(customMap.getId());
                Log.d("SelectMapAdaptor", "Unselected: " + customMap.getId());
            }
        });
    }
    public ArrayList<String> getSelectedCustomMapsId() {
        return selectedCustomMapsId;
    }
    @Override
    public int getItemCount() {
        return customMapArrayList.size();
    }
    public static class MapChipHolder extends RecyclerView.ViewHolder{
        Chip mapChip;
        public MapChipHolder(@NonNull View itemView) {
            super(itemView);
            mapChip = itemView.findViewById(R.id.chipCustomMap);
        }
    }
}
