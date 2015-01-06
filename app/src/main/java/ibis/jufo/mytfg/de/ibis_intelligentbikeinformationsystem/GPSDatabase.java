package ibis.jufo.mytfg.de.ibis_intelligentbikeinformationsystem;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class GPSDatabase {
    private Context context;
    private DbHelper dbHelper;
    public final String DBNAME="GPSDatabase";
    public final int DBVERSION=3;
    public SQLiteDatabase db;
    public final String COLUMN1="Id";
    public final String COLUMN2="latitude";
    public final String COLUMN3="longitude";
    public final String COLUMN4="timestamp";
    public final String TABLENAME="GPSData";
    public final String CREATERDB="create table GPSData(Id integer primary key autoincrement,latitude text not null, longitude text not null, timestamp text not null);";

    // Log TAG
    protected static final String TAG = "GPSDatabase-class";

    //constructor
    public GPSDatabase(Context context){
        Log.i(TAG, "GPSDatabase Constructor");
        this.context=context;
        dbHelper=new DbHelper(context);
    }

    //creating a DbHelper
    public class DbHelper extends SQLiteOpenHelper {
        //DbHelper constructor
        public DbHelper(Context context){
            super(context,DBNAME,null,DBVERSION);
            Log.i(TAG, "DbHelper");
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

    public long insertRows(String column2, String column3, String column4){
        Log.i(TAG, "insertRows()");
        ContentValues value=new ContentValues();
        value.put(COLUMN2, column2);
        value.put(COLUMN3, column3);
        value.put(COLUMN4, column4);
        return db.insert(TABLENAME, null, value);
    }
    public Cursor getAllRows(){
        Cursor cursor=db.query(TABLENAME, new String[]{COLUMN1, COLUMN2, COLUMN3, COLUMN4}, null, null, null, null, null);
        return cursor;
    }
    public void open() throws SQLException {
        Log.i(TAG, "open()");
        db= dbHelper.getWritableDatabase();
        //return true;
    }
    public void close(){
        Log.i(TAG, "close()");
        dbHelper.close();
        //return true;
    }

}