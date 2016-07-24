package de.opencyclecompass.app.android.storage;

import java.util.UUID;

/**
 * OcycoTrackMetadata contains the metadata for one {@link OcycoTrack}
 * (totalDistance, startTime, duration, numberOfLocations, uuid, publicId, uploaded)
 */
public class OcycoTrackMetadata {
    public static final long PUBLIC_ID_INVALID = -1L;

    protected double totalDistance;
    protected long startTime;
    protected long duration;
    protected int numberOfLocations;

    protected UUID uuid;
    protected long publicId;
    protected boolean uploaded;

    /**
     * default constructor, generates a new (random) {@link UUID}, sets startTime to the current time and
     * publicId to {@link #PUBLIC_ID_INVALID}
     */
    public OcycoTrackMetadata() {
        startTime = System.currentTimeMillis();
        uuid = UUID.randomUUID();
        publicId = PUBLIC_ID_INVALID;
        totalDistance = 0.0;
    }

    /**
     * constructor to restore an {@link OcycoTrackMetadata} object from the SQLite database
     * @param totalDistance totalDistance
     * @param startTime startTime
     * @param duration duration
     * @param numberOfLocations numberOfLocations
     * @param uuid uuid
     * @param publicId publicId
     * @param uploaded uploaded
     */
    public OcycoTrackMetadata(double totalDistance,
                              long startTime,
                              long duration,
                              int numberOfLocations,
                              UUID uuid,
                              long publicId,
                              boolean uploaded) {
        this.totalDistance = totalDistance;
        this.startTime = startTime;
        this.duration = duration;
        this.numberOfLocations = numberOfLocations;
        this.uuid = uuid;
        this.publicId = publicId;
        this.uploaded = uploaded;
    }

    public UUID getUuid() {
        return uuid;
    }

    public long getPublicId() {
        return publicId;
    }

    public boolean hasPublicId() {
        return (publicId != PUBLIC_ID_INVALID);
    }

    /**
     * @return timestamp of track start
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Get total distance of track in meter
     *
     * You can to recalculate distances between locations using {@link OcycoTrack#recalculateDistances()}
     * before using this method to get a more precise result.
     *
     * The total distance is calculated on each modification made to the location list
     *
     * @return total distance of track in meter
     */
    public double getTotalDistance() {
        return totalDistance;
    }

    /**
     * @return number of locations in track
     */
    public int getNumberOfLocations() {
        return numberOfLocations;
    }

    /**
     * @return duration of track in milliseconds or -1 if track is empty
     */
    public long getDuration() {
        return duration;
    }

    public boolean isUploaded() {
        return uploaded;
    }

    public void setUploaded(boolean uploaded) {
        this.uploaded = uploaded;
    }
}
