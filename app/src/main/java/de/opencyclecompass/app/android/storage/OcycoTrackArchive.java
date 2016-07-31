package de.opencyclecompass.app.android.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;
import android.widget.Toast;

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

import de.opencyclecompass.app.android.R;

/**
 * OcycoTrackArchive archives multiple {@link OcycoTrack}s persistent.
 * Warning: not thread save
 */
public class OcycoTrackArchive {
    private static final String FILE_EXTENSION = ".ocycotrack";

    private Context context;

    private HashMap<UUID, OcycoTrack> trackCache;

    private SQLiteDatabase db;

    /**
     * Forbidden. Use {@link #OcycoTrackArchive(Context)} instead.
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
        // instantiate MetadataDbHelper class and get database
        MetadataDbHelper dbHelper = new MetadataDbHelper(this.context);
        db = dbHelper.getWritableDatabase();
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
        Cursor cursor = db.query(
                MetadataDbHelper.TABLE_NAME,
                new String[]{MetadataDbHelper.COL_UUID},
                null,
                null,
                null,
                null,
                MetadataDbHelper.COL_START_TIME
        );
        ArrayList<UUID> list = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                String uuidString = cursor.getString(
                        cursor.getColumnIndex(MetadataDbHelper.COL_UUID)
                );
                list.add(UUID.fromString(uuidString));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    /**
     * @return A list of OcycoTrackMetadata object for each track in track archive
     *
     * This wraps {@code getTrackMetadataList(null, null)}.
     * See {@link #getTrackMetadataList(String, String[])} for more details.
     */
    public List<OcycoTrackMetadata> getTrackMetadataList() {
        return getTrackMetadataList(null, null);
    }

    /**
     * @param selection A filter declaring which track matadata to return,
     *                  formatted as an SQL WHERE clause (excluding the WHERE itself).
     *                  Passing null will return all rows for the given table.
     *                  E.g.: {@code MetadataDbHelper.COL_UUID + "=?"}
     *
     * @param selectionArgs You may include ?s in selection,
     *                      which will be replaced by the values from selectionArgs,
     *                      in order that they appear in the selection.
     *                      The values will be bound as Strings.
     *
     * @return A list of OcycoTrackMetadata object for each track in track archive
     */
    private List<OcycoTrackMetadata> getTrackMetadataList(String selection, String[] selectionArgs) {
        Cursor cursor = db.query(
                MetadataDbHelper.TABLE_NAME,
                new String[]{
                        MetadataDbHelper.COL_TOTAL_DISTANCE,
                        MetadataDbHelper.COL_START_TIME,
                        MetadataDbHelper.COL_DURATION,
                        MetadataDbHelper.COL_NUMBER_OF_LOCATIONS,
                        MetadataDbHelper.COL_UUID,
                        MetadataDbHelper.COL_PUBLIC_ID,
                        MetadataDbHelper.COL_UPLOADED
                },
                selection,
                selectionArgs,
                null,
                null,
                MetadataDbHelper.COL_START_TIME
        );
        ArrayList<OcycoTrackMetadata> list = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                double totalDistance = cursor.getDouble(cursor.getColumnIndex(MetadataDbHelper.COL_TOTAL_DISTANCE));
                long startTime = cursor.getLong(cursor.getColumnIndex(MetadataDbHelper.COL_START_TIME));
                long duration = cursor.getLong(cursor.getColumnIndex(MetadataDbHelper.COL_DURATION));
                int numberOfLocations = cursor.getInt(cursor.getColumnIndex(MetadataDbHelper.COL_NUMBER_OF_LOCATIONS));
                String uuidString = cursor.getString(cursor.getColumnIndex(MetadataDbHelper.COL_UUID));
                int publicId = cursor.getInt(cursor.getColumnIndex(MetadataDbHelper.COL_PUBLIC_ID));
                boolean uploaded = (cursor.getInt(cursor.getColumnIndex(MetadataDbHelper.COL_UPLOADED)) != 0);
                OcycoTrackMetadata metadata = new OcycoTrackMetadata(totalDistance, startTime, duration,numberOfLocations, UUID.fromString(uuidString), publicId, uploaded);
                list.add(metadata);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    /**
     * @return the number of archived tracks
     */
    public long getNumberOfTracks() {
        return DatabaseUtils.queryNumEntries(db, MetadataDbHelper.TABLE_NAME);
    }

    /**
     * Returns the local {@link UUID} of a track for a given public ID
     * @param publicId the known public track ID
     * @return the local {@link UUID} of the same track if the track exist, else {@code null}
     */
    @Nullable
    public UUID getTrackUuid(long publicId) {
        Cursor cursor = db.query(
                MetadataDbHelper.TABLE_NAME,
                new String[]{MetadataDbHelper.COL_UUID},
                MetadataDbHelper.COL_PUBLIC_ID + "=?",
                new String[]{String.valueOf(publicId)},
                null,
                null,
                MetadataDbHelper.COL_START_TIME
        );
        String uuidString = cursor.getString(cursor.getColumnIndex(MetadataDbHelper.COL_UUID));
        cursor.close();
        if (cursor.moveToFirst()) {
            return UUID.fromString(uuidString);
        }
        return null;
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
        if (trackUuid == null) {
            return null;
        }
        if (trackCache.containsKey(trackUuid)) {
            return trackCache.get(trackUuid);
        }
        else {
            return readTrackFromFile(trackUuid);
        }
    }

    /**
     * Add a track to the track archive.
     * The track is stored persistent in a file.
     * @param track {@link OcycoTrack} object to save
     */
    public void add(OcycoTrack track) {
        // put track into cache
        trackCache.put(track.metadata.getUuid(), track);

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(MetadataDbHelper.COL_TOTAL_DISTANCE, track.metadata.getTotalDistance());
        values.put(MetadataDbHelper.COL_START_TIME, track.metadata.getStartTime());
        values.put(MetadataDbHelper.COL_DURATION, track.metadata.getDuration());
        values.put(MetadataDbHelper.COL_NUMBER_OF_LOCATIONS, track.metadata.getNumberOfLocations());
        values.put(MetadataDbHelper.COL_UUID, track.metadata.getUuid().toString());
        values.put(MetadataDbHelper.COL_PUBLIC_ID, track.metadata.getPublicId());
        values.put(MetadataDbHelper.COL_UPLOADED, track.metadata.isUploaded() ? 1 : 0);
        // Insert the new row
        db.insert(MetadataDbHelper.TABLE_NAME, null, values);

        // save track to file (from cache)
        saveTrackToFile(track.metadata.getUuid());
    }

    /**
     * Delete track of given {@link UUID}
     * @param trackUuid the {@link UUID} of the track
     */
    public void delete(UUID trackUuid) {
        // remove track object from trackCache
        trackCache.remove(trackUuid);

        // delete track file
        String filename = trackUuid.toString() + FILE_EXTENSION;
        File file = new File(new File(context.getFilesDir(), "") + File.separator + filename);
        file.delete();

        // remove track from metadata db
        db.delete(MetadataDbHelper.TABLE_NAME,
                MetadataDbHelper.COL_UUID + "=?",
                new String[]{trackUuid.toString()}
        );
    }

    /**
     * Delete all track from track archive
     */
    public void deleteAll() {
        // clear trackCache entries
        trackCache.clear();

        // delete all track files
        List<UUID> list = getTrackUuidList();
        for (UUID trackUuid : list) {
            String filename = trackUuid.toString() + FILE_EXTENSION;
            File file = new File(new File(context.getFilesDir(), "") + File.separator + filename);
            file.delete();
        }
        // delete all track metadata database entries
        db.delete(MetadataDbHelper.TABLE_NAME, null, null);
    }

    /**
     * Saves changes on track to file.
     *
     * @param trackUuid the {@link UUID} of the track to save
     * @return true on success, false if write failed or track with {@code trackUuid} does not exist
     */
    public boolean update(UUID trackUuid) {
        OcycoTrack track = get(trackUuid);
        if (track == null) {
            return false;
        }
        // Update metadata:
        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(MetadataDbHelper.COL_TOTAL_DISTANCE, track.metadata.getTotalDistance());
        values.put(MetadataDbHelper.COL_START_TIME, track.metadata.getStartTime());
        values.put(MetadataDbHelper.COL_DURATION, track.metadata.getDuration());
        values.put(MetadataDbHelper.COL_NUMBER_OF_LOCATIONS, track.metadata.getNumberOfLocations());
        values.put(MetadataDbHelper.COL_UUID, track.metadata.getUuid().toString());
        values.put(MetadataDbHelper.COL_PUBLIC_ID, track.metadata.getPublicId());
        values.put(MetadataDbHelper.COL_UPLOADED, track.metadata.isUploaded() ? 1 : 0);
        // Update the row
        db.update(
                MetadataDbHelper.TABLE_NAME,
                values,
                MetadataDbHelper.COL_UUID + "=?",
                new String[]{trackUuid.toString()}
        );

        // Update track data:
        return saveTrackToFile(trackUuid);
    }

    /**
     * Read track from file.
     * @param trackUuid UUID of track to read, must not be {@code null}
     * @return a new created {@link OcycoTrack} object, or null if an error occurred.
     */
    @Nullable
    private OcycoTrack readTrackFromFile(UUID trackUuid) {
        // Load metadata from database
        List<OcycoTrackMetadata> metadataList = getTrackMetadataList(
                MetadataDbHelper.COL_UUID + "=?",
                new String[]{trackUuid.toString()}
        );
        if (metadataList.isEmpty()) {
            Toast.makeText(
                    this.context,
                    R.string.track_archive_error_reading_file,
                    Toast.LENGTH_LONG
            ).show();
            return null;
        }

        // Read track location list from file
        ObjectInputStream input;
        String filename = trackUuid.toString() + FILE_EXTENSION;
        try {
            input = new ObjectInputStream(
                    new FileInputStream(
                            new File(new File(context.getFilesDir(),"")+ File.separator+filename)
                    )
            );

            // Read ArrayList of OcycoLocation objects and check for consistency
            Object object = input.readObject();
            input.close();
            ArrayList<OcycoLocation> locations;
            // Check it's an ArrayList
            if (object instanceof ArrayList<?>) {
                // Get the List
                ArrayList<?> arrayList = (ArrayList<?>) object;
                locations = new ArrayList<>(arrayList.size());
                if (arrayList.size() > 0) {
                    for (int i = 0; i < arrayList.size(); i++) {
                        // Still not enough for a type
                        Object o = arrayList.get(i);
                        if (o instanceof OcycoLocation) {
                            OcycoLocation location = (OcycoLocation) o;
                            locations.add(location);
                        }
                        else {
                            Toast.makeText(
                                    this.context,
                                    R.string.track_archive_error_reading_array,
                                    Toast.LENGTH_LONG
                            ).show();
                            return null;
                        }
                    }
                }
            }
            else {
                Toast.makeText(
                        this.context,
                        R.string.track_archive_error_reading_array,
                        Toast.LENGTH_LONG
                ).show();
                return null;
            }

            // Create and return OcycoTrack object:
            OcycoTrack track = new OcycoTrack(locations, metadataList.get(0));
            // put track into cache
            trackCache.put(track.metadata.getUuid(), track);
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
     * Only one files is written: The {@code .ocycotrack}-file contains the list of OcycoLocation
     * objects.
     * Metadata is stored in the SQLite database.
     *
     * @param trackUuid the {@link UUID} of the track to save
     * @return true on success, false if write failed or track with {@code trackUuid} does not exist
     */
    private boolean saveTrackToFile(UUID trackUuid) {
        if (!trackCache.containsKey(trackUuid)) {
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
            out.writeObject(trackCache.get(trackUuid).getLocations());
            out.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleException(e);
            return false;
        }
    }
}