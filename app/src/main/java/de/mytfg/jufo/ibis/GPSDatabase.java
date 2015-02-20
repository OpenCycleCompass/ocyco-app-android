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


public class GPSDatabase {
    private Context context;
    //database variables
    private DbHelper dbHelper;
    public final String DBNAME = "GPSDatabase";
    public final int DBVERSION = 20;
    public SQLiteDatabase db;
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
    public final String CREATERDB = "CREATE TABLE "+TABLENAME+"("+COLUMN_ID+" INTEGER PRIMARY KEY AUTOINCREMENT," +
            COLUMN_LAT+" REAL NOT NULL, " +
            COLUMN_LON+" REAL NOT NULL, " +
            COLUMN_ALT+" REAL, " +
            COLUMN_SPE+" REAL, " +
            COLUMN_TST+" INTEGER NOT NULL, " +
            COLUMN_ACC+" REAL, " +
            COLUMN_DIST+" REAL, " +
            COLUMN_TDIFF+" INTEGER);";
    //timestamp vars
    public long startTst;
    public long stopTst;
    // Last location
    private Location lastLocation = null;
    // Log TAG
    protected static final String TAG = "GPSDatabase-class";

    //constructor
    public GPSDatabase(Context context) {
        Log.i(TAG, "GPSDatabase Constructor");
        this.context = context;
        dbHelper = new DbHelper(context);
        startTst = System.currentTimeMillis() / 1000;
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
        }
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
        if(lastLocation==null) {
            dist = 0;
            tdiff = 0;
            lastLocation = loc;
        } else {
            dist = loc.distanceTo(lastLocation);
            tdiff = loc.getTime()-lastLocation.getTime();
            lastLocation = loc;
        }
        value.put(COLUMN_DIST, dist);
        value.put(COLUMN_TDIFF, tdiff);
        return db.insert(TABLENAME, null, value);
    }

    public int prepareDB() {
        int d_rows = db.delete(TABLENAME, COLUMN_TDIFF+" < 0.5", null);
        d_rows += db.delete(TABLENAME, COLUMN_DIST+" < 0.5", null);
        return d_rows;
    }

    public Cursor getAllRows() {
        return db.query(TABLENAME, new String[]{COLUMN_ID, COLUMN_LAT, COLUMN_LON, COLUMN_ALT, COLUMN_SPE, COLUMN_TST, COLUMN_ACC}, null, null, null, null, null);
    }

    public double getTotalDist() {
        double tdist;
        Cursor mCount = db.rawQuery("SELECT SUM("+COLUMN_DIST+") FROM " + TABLENAME, null);
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

    public void open() throws SQLException {
        Log.i(TAG, "open()");
        db = dbHelper.getWritableDatabase();
    }

    public void close() {
        Log.i(TAG, "close()");
        dbHelper.close();
    }

    public Intent sendToServer(Context c) {
        //Log.i(TAG, "sendToServer()");
        stopTst = System.currentTimeMillis() / 1000;
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
                point.put("tst", (double)cursor.getLong(5)/1000);
                point.put("acc", cursor.getDouble(6));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            data.put(point);
            cursor.moveToNext();
        }
        String data_string = data.toString();
        Log.i(TAG, "data_string"+data_string);
        // Return intent to start UploadTrackActivity with track data attached
        Intent intent = new Intent(c, UploadTrackActivity.class);
        intent.putExtra("data", data_string);
        intent.putExtra("stopTst", stopTst);
        intent.putExtra("startTst", startTst);
        intent.putExtra("totalDist", getTotalDist());
        intent.putExtra("coordCnt", getNumRows());
        return intent;
    }

    public void deleteDatabase() {
        //delete database
        context.deleteDatabase(DBNAME);
        Log.i(TAG, "database deleted");
    }
}