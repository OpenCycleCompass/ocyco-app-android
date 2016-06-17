package de.opencyclecompass.app.android.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
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
    private static final String FILE_EXTENSION = ".track";

    private Context context;

    private HashMap<UUID, OcycoTrack> trackCache;

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
        // create HashMap for track cache
        trackCache = new HashMap<>();
    }

    /**
     * Metadata helper table containing table definitions, structure and SQL statements.
     */
    public class MetadataDbHelper extends SQLiteOpenHelper {
        // If you change the database schema, you must increment the database version.
        public static final int DATABASE_VERSION = 1;

        public static final String DATABASE_NAME = "OcycoTrackMetadata.sqlite";

        public static final String TABLE_NAME = "MetadataTable";
        public static final String ID = "id";
        public static final String COL_TOTAL_DISTANCE = "totalDistance";
        public static final String COL_START_TIME = "startTime";
        public static final String COL_DURATION = "duration";
        public static final String COL_NUMBER_OF_LOCATIONS = "numberOfLocations";
        public static final String COL_UUID = "uuid";
        public static final String COL_PUBLIC_ID = "publicId";
        public static final String COL_UPLOADED = "uploaded";

        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        ID + " INTEGER PRIMARY KEY," +
                        COL_TOTAL_DISTANCE + " TEXT, " +
                        COL_START_TIME + " TEXT, " +
                        COL_DURATION + " TEXT, " +
                        COL_NUMBER_OF_LOCATIONS + " TEXT, " +
                        COL_UUID + " TEXT, " +
                        COL_PUBLIC_ID + " TEXT, " +
                        COL_UPLOADED + " TEXT)";
        private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + TABLE_NAME;

        public MetadataDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_ENTRIES);
        }
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Upgrade database if necessary
        }
    }


    /**
     * @return a copy of the internal list of {@link UUID}s of the archived tracks
     */
    public List<UUID> getTrackUuidList() {
        // TODO: generate list from database
        // BETTER: provide list interface methods
        return new ArrayList<>();
    }

    /**
     * @return the number of archived tracks
     */
    public int getNumberOfTracks() {
        // TODO
        return 42;
    }

    /**
     * Returns the local {@link UUID} of a track for a given public ID
     * @param publicId the known public track ID
     * @return the local {@link UUID} of the same track if the track exist, else {@code null}
     */
    @Nullable
    public UUID getTrackUuid(long publicId) {
        // TODO
        return new UUID(42, 42);
    }

    /**
     * Gets the {@link OcycoTrack} object for the given public track ID
     * @param publicTrackId the known public track ID
     * @return a (possible from file read) {@link OcycoTrack} object, or null if a track for the
     *  given public track ID does not exist
     */
    @Nullable
    public OcycoTrack get(long publicTrackId) {
        return get(getTrackUuid(publicTrackId));
    }

    /**
     * Gets the {@link OcycoTrack} object for the given track {@link UUID}
     * @param trackUuid the known track {@link UUID}
     * @return a {@link OcycoTrack} object, or null if a track for the given UUID does not exist
     */
    @Nullable
    public OcycoTrack get(UUID trackUuid) {
        // TODO: return track from cache or read track from file if it exists
        return null;
    }

    /**
     * Add a track to the track archive.
     * The track is stored persistent in a file.
     * @param track {@link OcycoTrack} object to save
     */
    public void add(OcycoTrack track) {
        // TODO: store metadata


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
        // TODO: remove track from track list and save track list


        // remove OcycoTrack object from trackCache
        trackCache.remove(trackUuid);
        // delete track and metadata files
        String filename = trackUuid.toString() + FILE_EXTENSION;
        File file = new File(new File(context.getFilesDir(), "") + File.separator + filename);
        return file.delete();
    }

    /**
     * Delete all track from track archive
     */
    public void deleteAll() {
        // TODO: delete all files in dir, delete database
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
     * Read track from file.
     * @param trackUuid UUID of track to read, must not be {@code null}
     * @return a new created {@link OcycoTrack} object, or null if an error occurred.
     */
    @Nullable
    private OcycoTrack readTrackFromFile(UUID trackUuid) {
        ObjectInputStream input;
        String filename = trackUuid.toString() + FILE_EXTENSION;
        try {
            input = new ObjectInputStream(
                    new FileInputStream(
                            new File(new File(context.getFilesDir(),"")+ File.separator+filename)
                    )
            );
            OcycoTrack track = (OcycoTrack) input.readObject();
            input.close();

            return track;
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
        // TODO check if track exists
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
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleException(e);
        }
        return false;
    }
}