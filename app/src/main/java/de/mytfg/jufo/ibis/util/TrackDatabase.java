package de.mytfg.jufo.ibis.util;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.security.SecureRandom;
import java.util.ArrayList;

import de.mytfg.jufo.ibis.UploadTrackActivity;

public class TrackDatabase {
    private static final String TAG = "TrackDatabase-class";
    protected String DBNAME; // = "GPSDatabase";
    protected final int DBVERSION = 20;
    private final String COLUMN_ID = "Id";
    private final String COLUMN_LAT = "latitude";
    private final String COLUMN_LON = "longitude";
    private final String COLUMN_ALT = "altitude";
    private final String COLUMN_SPE = "speed";
    private final String COLUMN_TST = "timestamp";
    private final String COLUMN_ACC = "accuracy";
    private final String COLUMN_DIST = "distance";  // distance to last point in meters
    private final String COLUMN_TDIFF = "tdiff";    // time difference to last point in milliseconds
    private final String COLUMN_TIMEFACTOR = "time_factor";
    private final String TABLENAME = "GPSData";
    private final String CREATERDB = "CREATE TABLE " + TABLENAME + "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            COLUMN_LAT + " REAL NOT NULL, " +
            COLUMN_LON + " REAL NOT NULL, " +
            COLUMN_ALT + " REAL, " +
            COLUMN_SPE + " REAL, " +
            COLUMN_TST + " INTEGER, " +
            COLUMN_ACC + " REAL, " +
            COLUMN_DIST + " REAL, " +
            COLUMN_TDIFF + " INTEGER, " +
            COLUMN_TIMEFACTOR + " REAL);";
    // context
    private Context context;
    // SQLite database
    private SQLiteDatabase db;
    // database variables
    private DbHelper dbHelper;
    // Last location used to calculate tdiff column
    private Location lastLocation = null;

    //constructor
    public TrackDatabase(Context context, String name) {
        Log.i(TAG, "TrackDatabase constructor");
        dbHelper = new DbHelper(context);
        this.context = context;
        this.DBNAME = name;
        open();
    }

    // insert methods
    public long insertLocation(Location loc) {
        ContentValues value = new ContentValues();
        value.put(COLUMN_LAT, loc.getLatitude());
        value.put(COLUMN_LON, loc.getLongitude());
        value.put(COLUMN_ALT, loc.getAltitude());
        value.put(COLUMN_SPE, loc.getSpeed());
        value.put(COLUMN_TST, loc.getTime());
        value.put(COLUMN_ACC, loc.getAccuracy());
        double dist;
        long tdiff;
        if (lastLocation == null) {
            dist = 0;
            tdiff = 0;
            lastLocation = loc;
        } else {
            dist = loc.distanceTo(lastLocation);
            tdiff = loc.getTime() - lastLocation.getTime();
            lastLocation = loc;
        }
        value.put(COLUMN_DIST, dist);
        value.put(COLUMN_TDIFF, tdiff);
        return db.insert(TABLENAME, null, value);
    }

    public void readPointsArray(JSONArray jArray) {
        Location oldLocation = new Location("");
        double dist;
        for (int i = 0; i < jArray.length(); i++) {
            try {
                Location location = new Location("");
                JSONObject oneObject = jArray.getJSONObject(i);
                // Pulling items from the array
                double lat = oneObject.getDouble("lat");
                double lon = oneObject.getDouble("lon");
                double tf;
                if (oneObject.has("time_factor")) {
                    tf = oneObject.getDouble("time_factor");
                } else {
                    tf = 1d;
                }
                location.setLatitude(lat);
                location.setLongitude(lon);
                if (i != 0) {
                    dist = location.distanceTo(oldLocation);
                } else {
                    dist = 0;
                }
                //insert into db
                insertData(lat, lon, dist, tf);
                oldLocation = location;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public long insertData(double lat, double lon, double dist, double tf) {
        ContentValues value = new ContentValues();
        value.put(COLUMN_LAT, lat);
        value.put(COLUMN_LON, lon);
        value.put(COLUMN_DIST, dist);
        value.put(COLUMN_TIMEFACTOR, tf);
        return db.insert(TABLENAME, null, value);
    }



    // delete and drop methods

    public void deleteData() {
        db.delete(TABLENAME, null, null);
    }

    public void deleteDatabase() {
        //delete database
        context.deleteDatabase(DBNAME);
        Log.i(TAG, "database deleted");
    }


    // data manipulating methods
    // previous prepareDB():
    @Deprecated
    public int removeTdiffDistTooShort(double threshold) {
        String thresholdString = Double.toString(threshold);
        int d_rows = db.delete(TABLENAME, COLUMN_TDIFF + " < " + thresholdString, null);
        d_rows += db.delete(TABLENAME, COLUMN_DIST + " < " + thresholdString, null);
        return d_rows;
    }

    // random cut begin and end of track
    public boolean removeRandomStartEnd() {
        SecureRandom random = new SecureRandom();
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_ID + ", " + COLUMN_DIST + " FROM " + TABLENAME, null);
        double cutDistBegin = random.nextInt(80) + 20d;
        double cutDistEnd = random.nextInt(80) + 20d;
        double distBegin = 0d;
        double distEnd = 0d;
        int cutColIdBegin = -1;
        int cutColIdEnd = -1;
        if (cursor.moveToFirst()) {
            do {
                distBegin += cursor.getDouble(1);
                if (distBegin > cutDistBegin) {
                    cutColIdBegin = cursor.getInt(0);
                    break;
                }

            } while (cursor.moveToNext());
        }
        else {
            // don't upload empty track
            return false;
        }
        if (cursor.moveToLast()) {
            do {
                distEnd += cursor.getDouble(1);
                if (distEnd > cutDistEnd) {
                    cutColIdEnd = cursor.getInt(0);
                    break;
                }
            } while (cursor.moveToPrevious());
        }
        else {
            // don't upload empty track
            return false;
        }
        cursor.close();
        if ((cutColIdBegin != -1) && (cutColIdEnd != -1)) {
            int delRowsBegin = db.delete(TABLENAME, COLUMN_ID + " < " + cutColIdBegin, null);
            int delRowsEnd = db.delete(TABLENAME, COLUMN_ID + " > " + cutColIdEnd, null);
            Log.i(TAG, "Random cut: begin=" + delRowsBegin);
            Log.i(TAG, "Random cut: end=" + delRowsEnd);
        }
        return true;
    }


    // read methods
    @Nullable
    public ArrayList<GeoPoint> getGeoPointArrayList() {
        Cursor cursor = db.query(TABLENAME, new String[]{COLUMN_LAT, COLUMN_LON}, null, null, null, null, null);
        ArrayList<GeoPoint> data = new ArrayList<>();
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                GeoPoint point = new GeoPoint(cursor.getDouble(0), cursor.getDouble(1));
                data.add(point);
                cursor.moveToNext();
            }
            cursor.close();
            return data;
        }
        else {
            cursor.close();
            return null;
        }
    }

    public JSONArray getJSONArray() {
        JSONArray data = new JSONArray();
        Cursor cursor = getAllRows();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            JSONObject point = new JSONObject();
            try {
                point.put("lat", cursor.getDouble(1));
                point.put("lon", cursor.getDouble(2));
                point.put("alt", cursor.getDouble(3));
                point.put("spe", cursor.getDouble(4));
                point.put("tst", (double) cursor.getLong(5) / 1000);
                point.put("acc", cursor.getDouble(6));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            data.put(point);
            cursor.moveToNext();
        }
        cursor.close();
        return data;
    }

    public double getTotalDist() {
        double tdist;
        Cursor mCount = db.rawQuery("SELECT SUM(" + COLUMN_DIST + ") FROM " + TABLENAME, null);
        mCount.moveToFirst();
        tdist = mCount.getDouble(0);
        mCount.close();
        return tdist;
    }

    public double getTotalTime() {
        double tdist;
        Cursor mCount = db.rawQuery("SELECT SUM(" + COLUMN_DIST + " * " + COLUMN_TIMEFACTOR + ") FROM " + TABLENAME, null);
        mCount.moveToFirst();
        tdist = mCount.getDouble(0);
        mCount.close();
        return tdist;
    }

    public int getNumRows() {
        int num;
        Cursor mCount = db.rawQuery("SELECT COUNT(*) FROM " + TABLENAME, null);
        mCount.moveToFirst();
        num = mCount.getInt(0);
        mCount.close();
        return num;
    }


    // deprecated methods
    @Deprecated
    @Nullable
    public Intent sendToServer(Context c) {
        open();
        JSONArray data = new JSONArray();
        Cursor cursor = getAllRows();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            JSONObject point = new JSONObject();
            try {
                point.put("lat", cursor.getDouble(1));
                point.put("lon", cursor.getDouble(2));
                point.put("alt", cursor.getDouble(3));
                point.put("spe", cursor.getDouble(4));
                point.put("tst", (double) cursor.getLong(5) / 1000);
                point.put("acc", cursor.getDouble(6));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            data.put(point);
            cursor.moveToNext();
        }
        cursor.close();
        String data_string = data.toString();
        Log.i(TAG, "data_string" + data_string);
        // Return intent to start UploadTrackActivity with track data attached
        Intent intent = new Intent(c, UploadTrackActivity.class);
        intent.putExtra("data", data_string);
        // intent.putExtra("stopTst", stopTst);
        // intent.putExtra("startTst", startTst);
        intent.putExtra("totalDist", getTotalDist());
        intent.putExtra("coordCnt", getNumRows());
        close();
        return intent;
    }


    // internal methods
    private void open() throws SQLException {
        Log.i(TAG, "open()");
        db = dbHelper.getWritableDatabase();
    }

    private void open_ro() {
        Log.i(TAG, "open_ro()");
        db = dbHelper.getReadableDatabase();
    }

    public void close() {
        Log.i(TAG, "close()");
        db.close();
    }

    private Cursor getAllRows() {
        return db.query(TABLENAME, new String[]{COLUMN_ID, COLUMN_LAT, COLUMN_LON, COLUMN_ALT,
                COLUMN_SPE, COLUMN_TST, COLUMN_ACC}, null, null, null, null, null);
    }

    // DbHelper class
    private class DbHelper extends SQLiteOpenHelper {
        //DbHelper constructor
        public DbHelper(Context context) {
            super(context, DBNAME, null, DBVERSION);
            Log.i(TAG, DBVERSION + "");
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.i(TAG, "DbHelper onCreate()");
            db.execSQL(CREATERDB);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLENAME + ";");
            onCreate(db);
        }
    }
}
