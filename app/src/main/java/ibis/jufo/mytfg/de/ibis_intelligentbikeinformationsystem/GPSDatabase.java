package ibis.jufo.mytfg.de.ibis_intelligentbikeinformationsystem;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.StrictMode;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class GPSDatabase {
    private Context context;
    private DbHelper dbHelper;
    public final String DBNAME="GPSDatabase";
    public final int DBVERSION=13;
    public SQLiteDatabase db;
    public final String COLUMN_ID="Id";
    public final String COLUMN_LAT="latitude";
    public final String COLUMN_LON="longitude";
    public final String COLUMN_ALT="altitude";
    public final String COLUMN_SPE="speed";
    public final String COLUMN_TST="timestamp";
    public final String TABLENAME="GPSData";
    public final String CREATERDB="create table GPSData(Id integer primary key autoincrement, latitude text not null, longitude text not null, altitude text, speed text, timestamp text not null);";

    public String serverTrack_id = "";
    public int serverNodes = -1;
    public int serverCreated = -1;

    public long startTst;
    public long stopTst;

    // Log TAG
    protected static final String TAG = "GPSDatabase-class";

    //constructor
    public GPSDatabase(Context context){
        Log.i(TAG, "GPSDatabase Constructor");
        this.context=context;
        dbHelper=new DbHelper(context);
        startTst = System.currentTimeMillis()/1000;
    }

    //creating a DbHelper
    public class DbHelper extends SQLiteOpenHelper {
        //DbHelper constructor
        public DbHelper(Context context){
            super(context,DBNAME,null,DBVERSION);
            Log.i(TAG, DBVERSION+"");
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

    public long insertRows(String lat, String lon, String alt, String spe, String tst){
        Log.i(TAG, "insertRows()");
        ContentValues value=new ContentValues();
        value.put(COLUMN_LAT, lat);
        value.put(COLUMN_LON, lon);
        value.put(COLUMN_ALT, alt);
        value.put(COLUMN_SPE, spe);
        value.put(COLUMN_TST, tst);
        Log.i(TAG, value+"value");
        return db.insert(TABLENAME, null, value);
    }
    public Cursor getAllRows(){
        Cursor cursor=db.query(TABLENAME, new String[]{COLUMN_ID, COLUMN_LAT, COLUMN_LON, COLUMN_ALT, COLUMN_SPE, COLUMN_TST}, null, null, null, null, null);
        Log.i(TAG, cursor+"cursor");
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

    public Intent sendToServer(Context c) {
        Log.i(TAG, "sendToServer()");
        stopTst = System.currentTimeMillis()/1000;
        JSONArray data = new JSONArray();
        Cursor cursor = getAllRows();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            JSONObject point = new JSONObject();
            try {
                point.put("lat", cursor.getString(1));
                point.put("lon", cursor.getString(2));
                point.put("alt", cursor.getString(3));
                point.put("spe", cursor.getString(4));
                point.put("tst", cursor.getString(5));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            data.put(point);
            cursor.moveToNext();
        }
        String data_string = data.toString();
        Log.i(TAG, data_string);


        Intent intent = new Intent(c, UploadTrackActivity.class);

        intent.putExtra("data", data_string);
        intent.putExtra("stopTst", stopTst);
        intent.putExtra("startTst", startTst);
        return intent;

        /*String metadata = "&user_token=ibis_549f4fd2e22254.10943175&newtrack=newtrack&name=app&comment=bla&length=15&duration=100";
        String url = context.getString(R.string.UrlPushTrackData)+http_get_string+metadata;

        // Allow network on main thread: bad style
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        String httpResponse;
        try {
            httpResponse = getHttp(url);
            JSONObject json;
            try {
                json = new JSONObject(httpResponse);

                if(json.has("error")){
                    serverTrack_id = json.getString("error");
                    return 1;
                } else if(json.has("nodes") && json.has("track_id") && json.has("created")){
                    serverNodes = json.getInt("nodes");
                    serverCreated = json.getInt("created");
                    serverTrack_id = json.getString("track_id");
                    return 0;
                } else {
                    return 2;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 3;
        */
        //return 0;
    }
    /*private String getHttp(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.connect();
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return inputStreamToString(connection.getInputStream());
        } else {
            return "";
        }
    }

    private String inputStreamToString(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line).append("\n");
        }
        reader.close();
        return builder.toString();
    }*/

    public void deleteDatabase() {
        //delete database
        context.deleteDatabase(DBNAME);
        Log.i(TAG, "database deleted");
    }
}