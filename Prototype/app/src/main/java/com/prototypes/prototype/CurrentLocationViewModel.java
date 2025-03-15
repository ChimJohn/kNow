package com.prototypes.prototype;

import android.location.Location;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class CurrentLocationViewModel extends ViewModel {
    private final MutableLiveData<Location> currentLocation = new MutableLiveData<>();
    public void setCurrentLocation(Location location){
        currentLocation.setValue(location);
    }
    public LiveData<Location> getCurrentLocation(){
        return currentLocation;
    }
}
