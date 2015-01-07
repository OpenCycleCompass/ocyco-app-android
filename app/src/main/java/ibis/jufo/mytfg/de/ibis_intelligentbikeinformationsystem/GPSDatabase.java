package ibis.jufo.mytfg.de.ibis_intelligentbikeinformationsystem;

import android.content.ContentValues;
import android.content.Context;
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
    public final int DBVERSION=5;
    public SQLiteDatabase db;
    public final String COLUMN1="Id";
    public final String COLUMN2="latitude";
    public final String COLUMN3="longitude";
    public final String COLUMN4="altitude";
    public final String COLUMN5="timestamp";
    public final String TABLENAME="GPSData";
    public final String CREATERDB="create table GPSData(Id integer primary key autoincrement, latitude text not null, longitude text not null, altitude test null, timestamp text not null);";

    public String serverTrack_id = "";
    public int serverNodes = -1;
    public int serverCreated = -1;

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

    public long insertRows(String column2, String column3, String column4, String column5){
        Log.i(TAG, "insertRows()");
        ContentValues value=new ContentValues();
        value.put(COLUMN2, column2);
        value.put(COLUMN3, column3);
        value.put(COLUMN4, column4);
        value.put(COLUMN5, column5);
        return db.insert(TABLENAME, null, value);
    }
    public Cursor getAllRows(){
        Cursor cursor=db.query(TABLENAME, new String[]{COLUMN1, COLUMN2, COLUMN3, COLUMN4, COLUMN5}, null, null, null, null, null);
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

    public int sendToServer() {
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
        
        String metadata = "&user_token=ibis_549f4fd2e22254.10943175&newtrack=newtrack&name=app&comment=bla&length=15&duration=100";
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
    }
    private String getHttp(String url) throws IOException {
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
    }
}