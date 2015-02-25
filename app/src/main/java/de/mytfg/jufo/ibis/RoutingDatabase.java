package de.mytfg.jufo.ibis;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class RoutingDatabase {
    private Context context;
    //database variables
    private DbHelper dbHelper;
    public final String DBNAME = "RoutingDatabase";
    public final int DBVERSION = 1;
    public SQLiteDatabase db;
    public final String COLUMN_ID = "Id";
    public final String COLUMN_LAT = "latitude";
    public final String COLUMN_LON = "longitude";
    public final String COLUMN_DIST = "distance"; ///distance to last point in meters
    public final String TABLENAME = "RoutingData";
    public final String CREATERDB = "CREATE TABLE "+TABLENAME+"("+COLUMN_ID+" INTEGER PRIMARY KEY AUTOINCREMENT," +
            COLUMN_LAT+" REAL NOT NULL, " +
            COLUMN_LON+" REAL NOT NULL, " +
            COLUMN_DIST+" REAL, ";
    //timestamp vars
    public long startTst;
    // Log TAG
    protected static final String TAG = "RoutingDatabase-class";

    //constructor
    public RoutingDatabase(Context context) {
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

    public double getTotalDist() {
        double tdist;
        Cursor mCount = db.rawQuery("SELECT SUM("+COLUMN_DIST+") FROM " + TABLENAME, null);
        mCount.moveToFirst();
        tdist = mCount.getDouble(0);
        mCount.close();
        return tdist;
    }

    public void open() throws SQLException {
        Log.i(TAG, "open()");
        db = dbHelper.getWritableDatabase();
    }

    public void close() {
        Log.i(TAG, "close()");
        dbHelper.close();
    }

    public void deleteDatabase() {
        //delete database
        context.deleteDatabase(DBNAME);
        Log.i(TAG, "database deleted");
    }
}