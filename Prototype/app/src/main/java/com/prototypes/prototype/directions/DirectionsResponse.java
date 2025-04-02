package com.prototypes.prototype.directions;

import com.google.gson.annotations.SerializedName;

import java.util.List;

//use serialized name to map JSON to Gson easily
public class DirectionsResponse {
    @SerializedName("routes")
    public List<Route> routes;

    public static class Route {
        @SerializedName("overview_polyline")
        public OverviewPolyline overviewPolyline;
    }

    public static class OverviewPolyline {
        @SerializedName("points")
        public String points;
    }
}