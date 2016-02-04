package de.mytfg.jufo.ibis.storage;

import android.location.Location;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.ListIterator;

/**
 * TrackDatabaseMemory class to store a track
 */
public class TrackDatabaseMemory {
    private ArrayList<IbisLocation> locations;

    private double totalDistance;

    /**
     * default constructor
     */
    public TrackDatabaseMemory() {
        // initial size of ArrayList: 128
        locations = new ArrayList<>(128);
        totalDistance = 0.0;
    }

    /**
     * append location
     *
     * {@link IbisLocation} object will be appended at end of track
     *
     * @param loc {@link IbisLocation} object
     */
    public void appendLocation(IbisLocation loc) {
        if (!locations.isEmpty()) {
            loc.setDistanceTo(locations.get(locations.size() - 1));
            totalDistance += loc.getDistance();
        }
        locations.add(loc);
    }

    /**
     * append location
     *
     * IbisLocation created from {@link android.location.Location} object will be appended at end
     *  of track
     *
     * @param loc {@link android.location.Location} object
     */
    public void appendLocation(Location loc) {
        appendLocation(new IbisLocation(loc));
    }

    // append methods
    /**
     * append multiple {@link IbisLocation}s to track
     * {@link IbisLocation} are created from jsonLocationArray
     *
     * jsonLocationArray contains {@link org.json.JSONObject}s containing "lat", "lon" and
     *  optionally "time_factor"
     *
     * @param jsonLocationArray jsonLocationArray
     */
    public void appendJsonLocationArray(JSONArray jsonLocationArray) {
        // ensure ArrayList capacity before appending multiple IbisLocations
        locations.ensureCapacity(locations.size() + jsonLocationArray.length());
        IbisLocation oldLocation = null;
        for (int i = 0; i < jsonLocationArray.length(); i++) {
            try {
                // construct new IbisLocation from latitude and longitude
                IbisLocation location = new IbisLocation(
                        jsonLocationArray.getJSONObject(i).getDouble("lat"),
                        jsonLocationArray.getJSONObject(i).getDouble("lon")
                );
                // set timeFactor if present in jsonLocationArray
                if (jsonLocationArray.getJSONObject(i).has("time_factor")) {
                    location.setTimeFactor(jsonLocationArray.getJSONObject(i).getDouble("time_factor"));
                }
                // set calculated distance to last object
                // distance is set to IbisLocation.DISTANCE_INVALID if calculation impossible
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
        calculateTotalDistance();
    }

    // data manipulating methods
    /**
     * removes all locations from track
     */
    public void deleteData() {
        locations.clear();
        totalDistance = 0.0;
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
        double cutDistBegin = random.nextInt(80) + 20d;
        double cutDistEnd = random.nextInt(80) + 20d;
        double distBegin = 0d;
        double distEnd = 0d;
        int cutIndexBegin = -1;
        int cutIndexEnd = -1;

        // return false if track is too short
        if (getTotalDistance() <= (cutDistBegin+cutDistEnd)) {
            return false;
        }

        // iterate through list until sum of distances is <= cutDistBegin
        ListIterator<IbisLocation> locationsIterator = locations.listIterator();
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
        calculateTotalDistance();
        return true;
    }

    /**
     * recalculate distances between locations of track
     * for each {@link IbisLocation} (except the first one) to distance to the previous one
     *  is calculated and saved
     */
    public void recalculateDistances() {
        // set distance of first {@link IbisLocation} to DISTANCE_INVALID (handled as zero)
        if (locations.size() > 0) {
            locations.get(0).setDistance(IbisLocation.DISTANCE_INVALID);
        }
        // for loop begins at second element (index 1)
        for (int i = 1; i < locations.size(); i++) {
            locations.get(i).setDistanceTo(locations.get(i - 1));
        }
    }


    // read methods
    /**
     * Get total distance of track
     *
     * requires {@link IbisLocation}s distances to be set,
     *  maybe you should recalculate them using {@see recalculateDistances()}
     */
    private void calculateTotalDistance() {
        totalDistance = 0.0;
        for (IbisLocation location : locations) {
            if (location.getDistance() != IbisLocation.DISTANCE_INVALID) {
                totalDistance += location.getDistance();
            }
        }
    }

    /**
     * Get total distance of track
     *
     * You can to recalculate distances between locations using {@see recalculateDistances()}
     * before using this method to get a more precise result.
     *
     * The total distance is calculated on each modification made to the location list
     *
     * @return total distance of track
     */
    public double getTotalDistance() {
        return totalDistance;
    }

    /**
     * Calculate estimated time it takes to drive the track
     *
     * @return sum of distance between all points multiplied by timeFactor
     */
    public double getTotalTime() {
        double totalTime = 0.0;
        for (IbisLocation location : locations) {
            if (location.getDistance() != IbisLocation.DISTANCE_INVALID) {
                totalTime += (location.getDistance() * location.getTimeFactor());
            }
        }
        return totalTime;
    }

    /**
     * @return number of locations in track
     */
    public int getNumberOfLocations() {
        return locations.size();
    }

    /**
     * @return {@link ArrayList} of {@link GeoPoint}s from all locations in the track
     */
    public ArrayList<GeoPoint> getGeoPointArrayList() {
        // create array of OsmDroid GeoPoints with same size as locations list
        ArrayList<GeoPoint> data = new ArrayList<>(locations.size());
        for (IbisLocation location : locations) {
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
     * @return duration of track in milliseconds or -1 if track is empty
     */
    public long getDuration() {
        if (locations.isEmpty()) {
            return -1;
        }
        return (locations.get(locations.size() - 1).getTimestamp() - locations.get(0).getTimestamp());
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
        for (IbisLocation location : locations) {
            JSONObject point = new JSONObject();
            try {
                point.put("lat", location.getLatitude());
                point.put("lon", location.getLongitude());
                point.put("alt", location.getAltitude());
                point.put("spe", location.getSpeed());
                point.put("tst", (double) location.getTimestamp() / 1000);
                point.put("acc", location.getAccuracy());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            data.put(point);
        }
        return data;
    }
}
