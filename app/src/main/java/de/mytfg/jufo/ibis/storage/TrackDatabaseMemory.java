package de.mytfg.jufo.ibis.storage;

import android.content.Context;
import android.location.Location;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

public class TrackDatabaseMemory {
    private LinkedList<IbisLocation> locations;

    private double totalDistance;

    /**
     * default constructor
     */
    public TrackDatabaseMemory() {
        locations = new LinkedList<>();
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
            loc.setDistanceTo(locations.getLast());
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
        recalcDistances();
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
        recalcDistances();
        calculateTotalDistance();
        return true;
    }

    /**
     * recalculate distances between locations of track
     * for each {@link IbisLocation} (except the first one) to distanec to the previous one
     *  is calculated and saved
     */
    public void recalcDistances() {
        // set distance of first {@link IbisLocation} to DISTANCE_INVALID (handled as zero)
        if (locations.size() > 0) {
            locations.get(0).setDistance(IbisLocation.DISTANCE_INVALID);
        }
        /*
        // for loop begins at second element (index 1)
        // O(n^2) (?)
        for (int i = 1; i < locations.size(); i++) {
            locations.get(i).setDistanceTo(locations.get(i - 1));
        }
        // OR
        */
        // two iterators, one element offset
        // O(n) (?)
        ListIterator<IbisLocation> iterator_before = locations.listIterator(0);
        ListIterator<IbisLocation> iterator = locations.listIterator(1);
        while (iterator.hasNext()) {
            iterator.next().setDistanceTo(iterator_before.next());
        }
    }


    // read methods
    /**
     * Get total distance of track
     *
     * requires {@link IbisLocation}s distances to be set,
     *  maybe you should recalculate them using {@see recalcDistances()}
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
     * You can to recalculate distances between locations using {@see recalcDistances()}
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


    // DEPRECATED methods
    /**
     * @deprecated
     * constructor for backward compatibility
     *
     * @param context unused
     * @param name unused
     */
    @Deprecated
    public TrackDatabaseMemory(Context context, String name) {
        this();
    }

    /**
     * @deprecated
     * use {@see appendLocation()} instead
     *
     * @param loc {@link android.location.Location} object to create {@link IbisLocation} from
     * @return unused
     */
    @Deprecated
    public long insertLocation(Location loc) {
        appendLocation(loc);
        return 0;
    }

    /**
     * @deprecated
     * use {@see appendJsonLocationArray(JSONArray jsonLocationArray)} instead
     *
     * @param jArray jArray
     */
    @Deprecated
    public void readPointsArray(JSONArray jArray) {
        appendJsonLocationArray(jArray);
    }

    /**
     * @deprecated
     * use {@code appendLocation(IbisLocation location)} instead
     *
     * @param lat lat
     * @param lon lon
     * @param dist dist
     * @param tf tf
     * @return unused (backward compatibility)
     */
    @Deprecated
    public long insertData(double lat, double lon, double dist, double tf) {
        IbisLocation location = new IbisLocation(lat, lon);
        location.setDistance(dist);
        location.setTimeFactor(tf);
        appendLocation(location);
        return 0;
    }

    /**
     * @deprecated
     * use {@code deleteData()} instead
     *
     * removes all locations from track
     */
    @Deprecated
    public void deleteDatabase() {
        deleteData();
    }

    /**
     * @deprecated
     * use {@code getNumberOfLocations()} instead
     *
     * @return number of locations in track
     */
    @Deprecated
    public int getNumRows() {
        return getNumberOfLocations();
    }

    /**
     * @deprecated
     * use {@code getTotalDistance()} instead
     *
     * @return total distance of track
     */
    @Deprecated
    public double getTotalDist() {
        return getTotalDistance();
    }
}
