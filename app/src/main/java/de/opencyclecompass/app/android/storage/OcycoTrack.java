package de.opencyclecompass.app.android.storage;

import android.location.Location;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.ListIterator;

/**
 * OcycoTrack class to store a track
 */
public class OcycoTrack {

    private ArrayList<OcycoLocation> locations;

    public OcycoTrackMetadata metadata;

    /**
     * default constructor
     */
    public OcycoTrack() {
        metadata = new OcycoTrackMetadata();
        // initial size of ArrayList: 128
        locations = new ArrayList<>(128);
    }

    /**
     * constructor for restoring track from file
     *
     * @param locations {@link ArrayList} of {@link OcycoLocation} object to use
     * @param metadata {@link OcycoTrackMetadata} object to use
     */
    public OcycoTrack(ArrayList<OcycoLocation> locations, OcycoTrackMetadata metadata) {
        this.locations = locations;
        this.metadata = metadata;
    }

    // append methods
    /**
     * append location
     *
     * {@link OcycoLocation} object will be appended at end of track
     *
     * @param loc {@link OcycoLocation} object
     */
    public void appendLocation(OcycoLocation loc) {
        if (!locations.isEmpty()) {
            loc.setDistanceTo(locations.get(locations.size() - 1));
            metadata.totalDistance += loc.getDistance();
            calculateDuration();
            calculatenumberOfLocations();
        }
        locations.add(loc);
    }

    /**
     * append location
     *
     * OcycoLocation created from {@link android.location.Location} object will be appended at end
     *  of track
     *
     * @param loc {@link android.location.Location} object
     */
    public void appendLocation(Location loc) {
        appendLocation(new OcycoLocation(loc));
    }

    /**
     * append multiple {@link OcycoLocation}s to track
     * {@link OcycoLocation} are created from jsonLocationArray
     *
     * jsonLocationArray contains {@link org.json.JSONObject}s containing "lat", "lon" and
     *  optionally "time_factor"
     *
     * @param jsonLocationArray jsonLocationArray
     */
    public void appendJsonLocationArray(JSONArray jsonLocationArray) {
        // ensure ArrayList capacity before appending multiple OcycoLocations
        locations.ensureCapacity(locations.size() + jsonLocationArray.length());
        OcycoLocation oldLocation = null;
        for (int i = 0; i < jsonLocationArray.length(); i++) {
            try {
                // construct new OcycoLocation from latitude and longitude
                OcycoLocation location = new OcycoLocation(
                        jsonLocationArray.getJSONObject(i).getDouble("lat"),
                        jsonLocationArray.getJSONObject(i).getDouble("lon")
                );
                // set timeFactor if present in jsonLocationArray
                if (jsonLocationArray.getJSONObject(i).has("time_factor")) {
                    location.setTimeFactor(jsonLocationArray.getJSONObject(i).getDouble("time_factor"));
                }
                // set calculated distance to last object
                // distance is set to OcycoLocation.DISTANCE_INVALID if calculation impossible
                location.setDistance(location.distanceTo(oldLocation));
                // append to list
                locations.add(location);
                oldLocation = location;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        // Recalculate distances and total distance
        recalculateDistances();
        calculateMetaData();
    }

    // data manipulating methods
    /**
     * removes all locations from track
     */
    public void deleteData() {
        locations.clear();
        calculateMetaData();
    }

    /**
     * random cut begin and end of track
     *
     * maximum total 200m are cut of, at least 20m per end
     *
     * @return true on success or false on failure, eg. if track is too short
     */
    public boolean removeRandomStartEnd() {
        SecureRandom random = new SecureRandom();
        double cutDistBegin = random.nextInt(80) + 20d; // meter
        double cutDistEnd = random.nextInt(80) + 20d; // meter
        double distBegin = 0d;
        double distEnd = 0d;
        int cutIndexBegin = -1;
        int cutIndexEnd = -1;

        // return false if track is too short
        if (metadata.getTotalDistance() <= (cutDistBegin+cutDistEnd)) {
            return false;
        }

        // iterate through list until sum of distances is <= cutDistBegin
        ListIterator<OcycoLocation> locationsIterator = locations.listIterator();
        while (locationsIterator.hasNext() && (distBegin <= cutDistBegin)) {
            cutIndexBegin = locationsIterator.nextIndex();
            distBegin += locationsIterator.next().getDistance();
        }
        // remove elements from list
        if (cutIndexBegin != -1) {
            locations.subList(0, cutIndexBegin).clear();
        }

        // iterate through list (from end to begin) until sum of distances is <= cutDistEnd
        locationsIterator = locations.listIterator(locations.size());
        while (locationsIterator.hasPrevious() && (distEnd <= cutDistEnd)) {
            cutIndexEnd = locationsIterator.previousIndex();
            distEnd += locationsIterator.previous().getDistance();
        }
        // remove elements from list
        if (cutIndexEnd != -1) {
            locations.subList(cutIndexEnd, locations.size()).clear();
        }

        // success
        // Recalculate distances and total distance
        recalculateDistances();
        calculateMetaData();
        return true;
    }

    /**
     * recalculate distances between locations of track
     * for each {@link OcycoLocation} (except the first one) to distance to the previous one
     *  is calculated and saved
     */
    public void recalculateDistances() {
        // set distance of first {@link OcycoLocation} to DISTANCE_INVALID (handled as zero)
        if (locations.size() > 0) {
            locations.get(0).setDistance(OcycoLocation.DISTANCE_INVALID);
        }
        // for loop begins at second element (index 1)
        for (int i = 1; i < locations.size(); i++) {
            locations.get(i).setDistanceTo(locations.get(i - 1));
        }
    }


    /**
     * Calculate the total distance, duration and number of locations of the track
     *
     * requires {@link OcycoLocation}s distances to be set,
     *  maybe you should recalculate them using {@link #recalculateDistances()}
     */
    private void calculateMetaData() {
        calculateDistance();
        calculateDuration();
        calculatenumberOfLocations();
    }

    /**
     * Calculate the total distance of the track
     *
     * requires {@link OcycoLocation}s distances to be set,
     *  maybe you should recalculate them using {@link #recalculateDistances()}
     */
    private void calculateDistance() {
        metadata.totalDistance = 0.0;
        for (OcycoLocation location : locations) {
            if (location.getDistance() != OcycoLocation.DISTANCE_INVALID) {
                metadata.totalDistance += location.getDistance();
            }
        }
    }

    /**
     * Calculate the duration of the track
     */
    private void calculateDuration() {
        if (locations.isEmpty()) {
            metadata.duration = -1;
        }
        else {
            metadata.duration = (
                    locations.get(locations.size() - 1).getTimestamp() -
                            locations.get(0).getTimestamp()
            );
        }
    }


    /**
     * Calculate the number of locations of the track
     */
    private void calculatenumberOfLocations() {
        metadata.numberOfLocations = locations.size();
    }

    /**
     * Calculate estimated time it takes to drive the track
     *
     * @return sum of distance between all points multiplied by timeFactor
     */
    public double getTotalTime() {
        double totalTime = 0.0;
        for (OcycoLocation location : locations) {
            if (location.getDistance() != OcycoLocation.DISTANCE_INVALID) {
                totalTime += (location.getDistance() * location.getTimeFactor());
            }
        }
        return totalTime;
    }

    /**
     * @return {@link ArrayList} of {@link GeoPoint}s from all locations in the track
     */
    public ArrayList<GeoPoint> getGeoPointArrayList() {
        // create array of OsmDroid GeoPoints with same size as locations list
        ArrayList<GeoPoint> data = new ArrayList<>(locations.size());
        for (OcycoLocation location : locations) {
            GeoPoint point = new GeoPoint(
                    location.getLatitude(),
                    location.getLongitude(),
                    location.getAltitude()
            );
            data.add(point);
        }
        return data;
    }

    /**
     * This method creates a {@link JSONArray} of {@link JSONObject}s from all locations in the
     *  track.
     *
     * The {@link JSONObject} contain the elements "lat", "lon", "alt", "spe", "tst" and "acc".
     *  "tst" is the timestamp in milliseconds divided by 1000 as a double
     *
     * @return {@link JSONArray} of {@link JSONObject}s from all locations in the track
     */
    public JSONArray getJSONArray() {
        JSONArray data = new JSONArray();
        for (OcycoLocation location : locations) {
            JSONObject point = new JSONObject();
            try {
                point.put("lat", location.getLatitude());
                point.put("lon", location.getLongitude());
                point.put("alt", location.getAltitude());
                point.put("spe", location.getSpeed());
                point.put("tst", (double) location.getTimestamp() / 1000.0);
                point.put("tst_ms", location.getTimestamp());
                point.put("acc", location.getAccuracy());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            data.put(point);
        }
        return data;
    }

    /**
     * @return {@link ArrayList} of {@link OcycoLocation}s: the internal locations member
     */
    public ArrayList<OcycoLocation> getLocations() {
        return locations;
    }
}
