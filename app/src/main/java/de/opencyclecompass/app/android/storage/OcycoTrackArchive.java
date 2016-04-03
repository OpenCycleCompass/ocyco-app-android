package de.opencyclecompass.app.android.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import org.acra.ACRA;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * OcycoTrackArchive archives multiple {@link OcycoTrack}s persistent.
 * Warning: not thread save
 */
public class OcycoTrackArchive {
    private static final String PREFS = "OcycoTrackArchive-Prefs";
    private static final String PREFS_TRACKLIST = "PREFS_TRACKLIST";
    private static final String PREFS_TRACKIDMAPPING = "PREFS_TRACKIDMAPPING";
    private static final String FILE_EXTENSION = ".track";
    private static final String FILE_EXTENSION_META = ".metadata";

    private Context context;
    private SharedPreferences sharedPrefs;

    private ArrayList<UUID> trackUuidList;
    private HashMap<UUID, OcycoTrack> trackCache;
    private ArrayList<OcycoTrack.MetaData> trackMetadataList;
    private HashMap<UUID, OcycoTrack.MetaData> trackMetadata;
    private HashMap<Long, UUID> publicIdUuidMap;

    /**
     * Forbidden. Use {@code OcycoTrackArchive(Context context)} instead.
     */
    private OcycoTrackArchive() {
        // forbidden constructor
    }

    /**
     * Constructor
     * @param context android context needed to write files
     */
    public OcycoTrackArchive(Context context) {
        // save context from constructor
        this.context = context;
        // open shared preferences
        sharedPrefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        // create HashMap for track cache
        trackCache = new HashMap<>();

        // read list of track from shared preferences
        trackUuidList = new ArrayList<>();
        trackMetadataList = new ArrayList<>();
        trackMetadata = new HashMap<>();
        if (sharedPrefs.contains(PREFS_TRACKLIST)) {
            // read list of archived tracks from shard preferences
            String trackNameList = sharedPrefs.getString(PREFS_TRACKLIST, "");
            String[] trackUuidsAsStringArray = trackNameList.split(";");
            trackUuidList.ensureCapacity(trackUuidsAsStringArray.length);
            trackMetadataList.ensureCapacity(trackUuidsAsStringArray.length);
            for (String aTrack_names_string : trackUuidsAsStringArray) {
                if (aTrack_names_string.isEmpty()) {
                    continue;
                }
                // read track metadata from file for each track and put into trackMetadataList
                try {
                    UUID uuid = UUID.fromString(aTrack_names_string);
                    OcycoTrack.MetaData metadata = readTrackMetadataFromFile(uuid);
                    trackUuidList.add(uuid);
                    trackMetadataList.add(metadata);
                    trackMetadata.put(uuid, metadata);
                }
                catch (IllegalArgumentException e) {
                    ACRA.getErrorReporter().putCustomData(PREFS_TRACKLIST, trackNameList);
                    ACRA.getErrorReporter().putCustomData("uuid", aTrack_names_string);
                    ACRA.getErrorReporter().handleException(e);
                }
            }
        }

        // create and fill hash map of publicId to UUID mappings
        publicIdUuidMap = new HashMap<>();
        if (sharedPrefs.contains(PREFS_TRACKIDMAPPING)) {
            // read list of archived tracks from shard preferences
            String trackIdMap = sharedPrefs.getString(PREFS_TRACKIDMAPPING, "");
            String[] trackKeyValuesArray = trackIdMap.split(";");
            for (String trackKeyValue : trackKeyValuesArray) {
                if (trackKeyValue.isEmpty()) {
                    continue;
                }
                String[] trackKeyValueArray = trackKeyValue.split(":");
                if(!trackKeyValueArray[0].isEmpty() && !trackKeyValueArray[1].isEmpty()) {
                    try {
                        publicIdUuidMap.put(
                                Long.parseLong(trackKeyValueArray[0]),
                                UUID.fromString(trackKeyValueArray[1])
                        );
                    } catch (IllegalArgumentException e) {
                        ACRA.getErrorReporter().putCustomData(PREFS_TRACKIDMAPPING, trackIdMap);
                        ACRA.getErrorReporter().putCustomData("uuid-id-mapping", trackKeyValue);
                        ACRA.getErrorReporter().handleException(e);
                    }
                }
            }
        }
    }

    /**
     * @return a copy of the internal list of {@link UUID}s of the archived tracks
     */
    public List<UUID> getTrackUuidList() {
        return new ArrayList<>(trackUuidList);
    }

    /**
     * @return a copy of the internal list of {@link UUID}s of the archived tracks
     */
    public List<OcycoTrack.MetaData> getTrackMetadataList() {
        return new ArrayList<>(trackMetadataList);
    }

    /**
     * @return a copy of the internal list of {@link UUID}s of the archived tracks
     */
    public HashMap<UUID, OcycoTrack.MetaData> getTrackMetadataMap() {
        return new HashMap<>(trackMetadata);
    }

    /**
     * @return the number of archived tracks
     */
    public int getNumberOfTracks() {
        return trackUuidList.size();
    }

    /**
     * Returns the local {@link UUID} of a track for a given public ID
     * @param publicId the known public track ID
     * @return the local {@link UUID} of the same track if the track exist, else {@code null}
     */
    @Nullable
    public UUID getTrackUuid(long publicId) {
        if (!publicIdUuidMap.containsKey(publicId)) {
            return null;
        }
        else {
            return publicIdUuidMap.get(publicId);
        }
    }

    /**
     * Gets the {@link OcycoTrack} object for the given public track ID
     * @param publicTrackId the known public track ID
     * @return a (possible from file read) {@link OcycoTrack} object, or null if a track for the
     *  given public track ID does not exist
     */
    @Nullable
    public OcycoTrack get(long publicTrackId) {
        if (!publicIdUuidMap.containsKey(publicTrackId)) {
            return null;
        }
        else {
            return get(publicIdUuidMap.get(Long.valueOf(publicTrackId)));
        }
    }

    /**
     * Gets the {@link OcycoTrack} object for the given track {@link UUID}
     * @param trackUuid the known track {@link UUID}
     * @return a {@link OcycoTrack} object, or null if a track for the given UUID does not exist
     */
    @Nullable
    public OcycoTrack get(UUID trackUuid) {
        if (!trackUuidList.contains(trackUuid)) {
            return null;
        } else if (trackCache.containsKey(trackUuid)) {
            return trackCache.get(trackUuid);
        } else {
            return readTrackFromFile(trackUuid);
        }
    }

    /**
     * Add a track to the track archive.
     * The track is stored persistent in a file.
     * @param track {@link OcycoTrack} object to save
     */
    public void add(OcycoTrack track) {
        trackUuidList.add(track.metaData.getUuid());
        saveTrackUuidList();
        // put public ID into map if it exists
        if (track.metaData.hasPublicId()) {
            publicIdUuidMap.put(track.metaData.getPublicId(), track.metaData.getUuid());
        }
        // put track metadata into list and map
        trackMetadataList.add(track.metaData);
        trackMetadata.put(track.metaData.getUuid(), track.metaData);
        // put track into cache
        trackCache.put(track.metaData.getUuid(), track);
        // save track to file (from cache)
        saveTrackToFile(track.metaData.getUuid());
    }

    /**
     * Delete track of given {@link UUID}
     * @param trackUuid the {@link UUID} of the track
     */
    public boolean delete(UUID trackUuid) {
        // remove track from track list and save track list
        trackUuidList.remove(trackUuid);
        saveTrackUuidList();
        // remove track metadata from list and map
        trackMetadataList.remove(trackMetadata.get(trackUuid));
        trackMetadata.remove(trackUuid);
        // remove OcycoTrack object from trackCache
        trackCache.remove(trackUuid);
        // delete track and metadata files
        String filename = trackUuid.toString() + FILE_EXTENSION;
        String filenameMetadata = trackUuid.toString() + FILE_EXTENSION_META;
        File file = new File(new File(context.getFilesDir(), "") + File.separator + filename);
        File fileMetadata = new File(new File(context.getFilesDir(), "") + File.separator
                + filenameMetadata);
        return (file.delete() && fileMetadata.delete());
    }

    /**
     * Delete all track from track archive
     */
    public void deleteAll() {
        // copy trackUuidList, because delete() method modifies trackUuiDList
        ArrayList<UUID> localTrackUuidList = new ArrayList<>(trackUuidList);
        for (UUID uuid : localTrackUuidList) {
            delete(uuid);
        }
    }

    /**
     * Saves changes on track to file.
     *
     * @param trackUuid the {@link UUID} of the track to save
     * @return true on success, false if write failed or track with {@code trackUuid} does not exist
     */
    public boolean update(UUID trackUuid) {
        return saveTrackToFile(trackUuid);
    }

    /**
     * Saves changes on track to file.
     *
     * @param trackUuid the {@link UUID} of the track to save
     * @return true on success, false if write failed or track with {@code trackUuid} does not exist
     */
    public boolean updateMetadata(UUID trackUuid) {
        return saveTrackToFile(trackUuid);
    }

    /**
     * Persistent save {@code trackUuidList} to shared preferences.
     */
    private void saveTrackUuidList() {
        StringBuilder stringBuilder1 = new StringBuilder();
        for (UUID uuid: trackUuidList) {
            stringBuilder1.append(uuid.toString());
            stringBuilder1.append(";");
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        for(HashMap.Entry<Long, UUID> entry : publicIdUuidMap.entrySet()){
            stringBuilder2.append(entry.getKey().toString());
            stringBuilder2.append(":");
            stringBuilder2.append(entry.getValue().toString());
        }
        SharedPreferences.Editor sharedPrefsEditor = sharedPrefs.edit();
        sharedPrefsEditor.putString(PREFS_TRACKLIST, stringBuilder1.toString());
        sharedPrefsEditor.putString(PREFS_TRACKIDMAPPING, stringBuilder2.toString());
        sharedPrefsEditor.apply();
    }

    /**
     * Read track from file.
     * @param trackUuid UUID of track to read, must not be {@code null}
     * @return a new created {@link OcycoTrack} object, or null if an error occurred.
     */
    @Nullable
    private OcycoTrack readTrackFromFile(UUID trackUuid) {
        ObjectInputStream input;
        ObjectInputStream inputMetadata;
        String filename = trackUuid.toString() + FILE_EXTENSION;
        try {
            input = new ObjectInputStream(
                    new FileInputStream(
                            new File(new File(context.getFilesDir(),"")+ File.separator+filename)
                    )
            );
            OcycoTrack track = (OcycoTrack) input.readObject();
            input.close();

            // read metadata file (could contain newer information thcn track file) and override
            //  metadata field
            inputMetadata = new ObjectInputStream(
                    new FileInputStream(
                            new File(new File(context.getFilesDir(),"")+ File.separator+filename)
                    )
            );
            OcycoTrack.MetaData metadata = (OcycoTrack.MetaData) inputMetadata.readObject();
            inputMetadata.close();
            track.metaData = metadata;

            return track;
        } catch (Exception e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleException(e);
        }
        return null;
    }


    /**
     * Read track from file.
     * @param trackUuid UUID of track to read, must not be {@code null}
     * @return a new created {@link OcycoTrack} object, or null if an error occurred.
     */
    @Nullable
    private OcycoTrack.MetaData readTrackMetadataFromFile(UUID trackUuid) {
        ObjectInputStream input;
        String filename = trackUuid.toString() + FILE_EXTENSION_META;
        try {
            input = new ObjectInputStream(
                    new FileInputStream(
                            new File(new File(context.getFilesDir(),"")+ File.separator+filename)
                    )
            );
            OcycoTrack.MetaData metadata = (OcycoTrack.MetaData) input.readObject();
            input.close();
            return metadata;
        } catch (Exception e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleException(e);
        }
        return null;
    }

    /**
     * Saves track with {@code trackUuid} to file.
     * If track with same {@link UUID} already exists it will be overridden.
     * Two files are written: a {@code .track} and a {@code .metadata} file. The metadata file is
     *  redundant, but can be read independent from the main track file.
     *
     * @param trackUuid the {@link UUID} of the track to save
     * @return true on success, false if write failed or track with {@code trackUuid} does not exist
     */
    private boolean saveTrackToFile(UUID trackUuid) {
        if(!trackUuidList.contains(trackUuid)) {
            return false;
        }
        String filename = trackUuid.toString() + FILE_EXTENSION;
        ObjectOutput out;
        try {
            out = new ObjectOutputStream(
                    new FileOutputStream(
                            new File(context.getFilesDir(),"") + File.separator + filename,
                            false
                    )
            );
            out.writeObject(trackCache.get(trackUuid));
            out.close();
            // write metadata by calling saveTrackMetadataToFile()
            return saveTrackMetadataToFile(trackUuid);
        } catch (IOException e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleException(e);
        }
        return false;
    }

    /**
     * Saves track metadata from trach with {@code trackUuid} to file.
     * Only one file is written: the {@code .metadata} file. The metadata file is
     *  redundant, but can be read independent from the main track file.
     * The track file will not contain valid metadata if this method is used to save updated track
     *  metadata
     *
     * @param trackUuid the {@link UUID} of the track to save
     * @return true on success, false if write failed or track with {@code trackUuid} does not exist
     */
    private boolean saveTrackMetadataToFile(UUID trackUuid) {
        if(!trackUuidList.contains(trackUuid)) {
            return false;
        }
        String filenameMeta = trackUuid.toString() + FILE_EXTENSION_META;
        ObjectOutput outMeta;
        try {
            outMeta = new ObjectOutputStream(
                    new FileOutputStream(
                            new File(context.getFilesDir(),"") + File.separator + filenameMeta,
                            false
                    )
            );
            outMeta.writeObject(trackCache.get(trackUuid).metaData);
            outMeta.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleException(e);
        }
        return false;
    }
}