package de.mytfg.jufo.ibis;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.security.SecureRandom;
import java.util.ArrayList;

public class GPSDatabase {
    // Log TAG
    protected static final String TAG = "GPSDatabase-class";
    public final String DBNAME = "GPSDatabase";
    public final int DBVERSION = 20;
    public final String COLUMN_ID = "Id";
    public final String COLUMN_LAT = "latitude";
    public final String COLUMN_LON = "longitude";
    public final String COLUMN_ALT = "altitude";
    public final String COLUMN_SPE = "speed";
    public final String COLUMN_TST = "timestamp";
    public final String COLUMN_ACC = "accuracy";
    public final String COLUMN_DIST = "distance"; ///distance to last point in meters
    public final String COLUMN_TDIFF = "tdiff"; ///time difference to last point in milliseconds
    public final String TABLENAME = "GPSData";
    public final String CREATERDB = "CREATE TABLE " + TABLENAME + "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
            COLUMN_LAT + " REAL NOT NULL, " +
            COLUMN_LON + " REAL NOT NULL, " +
            COLUMN_ALT + " REAL, " +
            COLUMN_SPE + " REAL, " +
            COLUMN_TST + " INTEGER NOT NULL, " +
            COLUMN_ACC + " REAL, " +
            COLUMN_DIST + " REAL, " +
            COLUMN_TDIFF + " INTEGER);";
    public SQLiteDatabase db;
    //timestamp vars
    public long startTst;
    public long stopTst;
    private Context context;
    //database variables
    private DbHelper dbHelper;
    // Last location
    private Location lastLocation = null;

    //constructor
    public GPSDatabase(Context context) {
        Log.i(TAG, "GPSDatabase Constructor");
        this.context = context;
        dbHelper = new DbHelper(context);
        startTst = System.currentTimeMillis() / 1000;
    }

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

    public int prepareDB() {
        int d_rows = db.delete(TABLENAME, COLUMN_TDIFF + " < 0.5", null);
        d_rows += db.delete(TABLENAME, COLUMN_DIST + " < 0.5", null);
        return d_rows;
    }

    public Intent sendToServer(Context c) {
        open();
        //Log.i(TAG, "sendToServer()");
        stopTst = System.currentTimeMillis() / 1000;

        // random cut begin and end of track
        SecureRandom random = new SecureRandom();
        int numRows = getNumRows();
        Log.i(TAG, "DB has " + numRows + " rows");
        Cursor cursor = db.rawQuery("SELECT " + COLUMN_ID + ", " + COLUMN_DIST + " FROM " + TABLENAME, null);
        double cutDistBegin = random.nextInt(80) + 20d;
        double cutDistEnd = random.nextInt(80) + 20d;
        double distBegin = 0d;
        double distEnd = 0d;
        int cutColIdBegin = -1;
        int cutColIdEnd = -1;
        cursor.moveToFirst();
        do {
            distBegin += cursor.getDouble(0);
            if(distBegin > cutDistBegin) {
                cutColIdBegin = cursor.getInt(1);
                break;
            }

        } while (cursor.moveToNext());
        cursor.moveToLast();
        do {
            distEnd += cursor.getDouble(0);
            if(distEnd > cutDistEnd) {
                cutColIdEnd = cursor.getInt(1);
                break;
            }
        } while (cursor.moveToPrevious());
        cursor.close();
        if ((cutColIdBegin != -1) && (cutColIdEnd != -1)) {
            int delRowsBegin = db.delete(TABLENAME, COLUMN_ID + " < " + cutColIdBegin, null);
            int delRowsEnd = db.delete(TABLENAME, COLUMN_ID + " > " + cutColIdEnd, null);
            Log.i(TAG, "Random cut: begin=" + delRowsBegin);
            Log.i(TAG, "Random cut: end=" + delRowsEnd);
        }
        numRows = getNumRows();
        Log.i(TAG, "DB has " + numRows + " rows");

        JSONArray data = new JSONArray();
        cursor = getAllRows();
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
        intent.putExtra("stopTst", stopTst);
        intent.putExtra("startTst", startTst);
        intent.putExtra("totalDist", getTotalDist());
        intent.putExtra("coordCnt", getNumRows());
        close();
        return intent;
    }

    public void open() throws SQLException {
        Log.i(TAG, "open()");
        db = dbHelper.getWritableDatabase();
    }

    public Cursor getAllRows() {
        return db.query(TABLENAME, new String[]{COLUMN_ID, COLUMN_LAT, COLUMN_LON, COLUMN_ALT, COLUMN_SPE, COLUMN_TST, COLUMN_ACC}, null, null, null, null, null);
    }

    public double getTotalDist() {
        double tdist;
        Cursor mCount = db.rawQuery("SELECT SUM(" + COLUMN_DIST + ") FROM " + TABLENAME, null);
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

    public void close() {
        Log.i(TAG, "close()");
        dbHelper.close();
    }

    public void deleteData() {
        db.delete(TABLENAME, null, null);
    }

    public void deleteDatabase() {
        //delete database
        context.deleteDatabase(DBNAME);
        Log.i(TAG, "database deleted");
    }

    public ArrayList<GeoPoint> getAllGeoPoints() {
        Cursor cursor = db.query(TABLENAME, new String[]{COLUMN_LAT, COLUMN_LON}, null, null, null, null, null);
        ArrayList<GeoPoint> data = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            GeoPoint point = new GeoPoint(cursor.getDouble(0), cursor.getDouble(1));
            data.add(point);
            cursor.moveToNext();
        }
        cursor.close();
        return data;
    }

    //creating a DbHelper
    public class DbHelper extends SQLiteOpenHelper {
        //DbHelper constructor
        public DbHelper(Context context) {
            super(context, DBNAME, null, DBVERSION);
            Log.i(TAG, DBVERSION + "");
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.i(TAG, "onCreate()");
            db.execSQL(CREATERDB);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLENAME + ";");
            onCreate(db);
        }
    }
}