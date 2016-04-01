package de.opencyclecompass.app.android.storage;

import android.location.Location;

import java.io.Serializable;

/**
 * OcycoLocation class representing a location in a track
 */
public class OcycoLocation implements Serializable {
    public static final double DISTANCE_INVALID = -1;
    public static final double TIMEINTERVAL_INVALID = -1;
    public static final double NO_TIMEFACTOR = 1.0;

    private double latitude;
    private double longitude;
    private double altitude;
    private double speed;
    private double accuracy;
    private long timestamp; // milliseconds

    private double distance; // to next/last OcycoLocation (in meter)
    private double timeInterval; // to next/last OcycoLocation

    private double timeFactor; // correction factor for speed

    /**
     * default constructor
     */
    public OcycoLocation() {
        this.latitude = 0.0;
        this.longitude = 0.0;
        this.altitude = 0.0;
        this.speed = 0.0;
        this.accuracy = 0.0;
        this.timestamp = 0;
        this.distance = DISTANCE_INVALID;
        this.timeInterval = TIMEINTERVAL_INVALID;
        this.timeFactor = NO_TIMEFACTOR;
    }

    /**
     * copy constructor
     */
    public OcycoLocation(OcycoLocation location) {
        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
        this.altitude = location.getAltitude();
        this.speed = location.getSpeed();
        this.accuracy = location.getAccuracy();
        this.timestamp = location.getTimestamp();
        this.distance = location.getDistance();
        this.timeInterval = location.getTimeInterval();
        this.timeFactor = location.getTimeFactor();
    }

    /**
     * Constructor to create an OcycoLocation from a {@link android.location.Location} object
     * @param location the location object to read value from
     */
    public OcycoLocation(Location location) {
        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
        this.altitude = location.getAltitude();
        this.speed = (double)location.getSpeed();
        this.accuracy = (double)location.getAccuracy();
        this.timestamp = location.getTime();
        this.distance = DISTANCE_INVALID;
        this.timeInterval = TIMEINTERVAL_INVALID;
        this.timeFactor = NO_TIMEFACTOR;
    }

    /**
     * constructor to construct {@link OcycoLocation} from given latitude and longitude.
     * Other values are 0 or appropriate INVALID values
     * @param latitude the latitude
     * @param longitude the longitude
     */
    public OcycoLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = 0.0;
        this.speed = 0.0;
        this.accuracy = 0.0;
        this.timestamp = 0;
        this.distance = DISTANCE_INVALID;
        this.timeInterval = TIMEINTERVAL_INVALID;
        this.timeFactor = NO_TIMEFACTOR;
    }

    /**
     * constructor to construct {@link OcycoLocation} from given values
     * @param latitude latitude
     * @param longitude longitude
     * @param altitude altitude
     * @param speed speed
     * @param accuracy accuracy
     * @param timestamp timestamp
     */
    public OcycoLocation(double latitude, double longitude, double altitude, double speed,
                         double accuracy, long timestamp) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.speed = speed;
        this.accuracy = accuracy;
        this.timestamp = timestamp;
        this.distance = DISTANCE_INVALID;
        this.timeInterval = TIMEINTERVAL_INVALID;
        this.timeFactor = NO_TIMEFACTOR;
    }

    /**
     * convert this to an {@link android.location.Location} object
     * @return this as {@link android.location.Location} object
     */
    public Location toLocation() {
        Location location = new Location("");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setAltitude(altitude);
        location.setSpeed((float)speed);
        location.setAccuracy((float) accuracy);
        location.setTime(timestamp);
        return location;
    }

    /**
     * Calculate distance to {@link OcycoLocation} {@code to_location}
     * @param to location to calculate distance to
     * @return the calculated distance in meter
     */
    public double distanceTo(OcycoLocation to) {
        if (to == null) {
            return DISTANCE_INVALID;
        }
        // Haversine formula (https://en.wikipedia.org/wiki/Haversine_formula)
        double earthRadius = 6371000; // meters
        double phi1 = degree2radiant(latitude);
        double phi2 = degree2radiant(to.getLatitude());
        double deltaPhi = Math.abs(degree2radiant(to.getLatitude() - latitude));
        double deltaLambda = Math.abs(degree2radiant(to.getLongitude() - longitude));
        double a = (Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2)) + (Math.cos(phi1) * Math.cos(phi2) * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2));
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return earthRadius * c;
    }

    private double degree2radiant(double degree) {
        return degree * (Math.PI/180);
    }


    ////////////////////
    // Getter and Setter
    ////////////////////

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public double getSpeed() {
        return speed;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getDistance() {
        return distance;
    }

    public double getTimeInterval() {
        return timeInterval;
    }

    public double getTimeFactor() {
        return timeFactor;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    /**
     * Calculate distance to {@link OcycoLocation} {@code to_location} and set as distance
     * @param to_location location to calculate distance to
     */
    public void setDistanceTo(OcycoLocation to_location) {
        this.distance = distanceTo(to_location);
    }

    public void setTimeInterval(double timeInterval) {
        this.timeInterval = timeInterval;
    }

    public void setTimeFactor(double timeFactor) {
        this.timeFactor = timeFactor;
    }
}
