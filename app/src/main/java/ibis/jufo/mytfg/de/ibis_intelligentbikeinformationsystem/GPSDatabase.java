package ibis.jufo.mytfg.de.ibis_intelligentbikeinformationsystem;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;

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
    public int sendToServer(){
        Log.i(TAG, "sendToServer()");
        JSONArray data = new JSONArray();
        Cursor cursor = getAllRows();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            JSONObject point = new JSONObject();
            try {
                point.put("lat", cursor.getString(1));
                point.put("lon", cursor.getString(2));
                //point.put("alt", cursor.getString(4));
                point.put("time", cursor.getString(3));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            data.put(point);
            cursor.moveToNext();
        }
        String http_get_string = data.toString();
        Log.i(TAG, http_get_string);
        /*String url = "https://ibis.jufo.mytfg.de/api1/pushtrack.php?data="+http_get_string+"&";
        InputStream content = null;
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response = httpclient.execute(new HttpGet(url));
        } catch (Exception e) {
            Log.i(TAG, "Network exception", e);
        }
        */
        return 0;
    }
}