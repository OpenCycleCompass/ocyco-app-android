package de.mytfg.jufo.ibis;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class UploadTrackActivity extends ActionBarActivity {

    public static final String data_empty = "[]"; // empty, but valid JSON
    private String data = data_empty;
    // Log TAG
    protected static final String TAG = "UploadTrackAct-class";
    //shared preferences
    SharedPreferences prefs;
    SharedPreferences.Editor prefs_edit;
    private EditText editText_UploadTrackName;
    private EditText editText_UploadTrackCom;
    private EditText editText_UploadTrackDuration;
    private EditText editText_UploadTrackLength;
    private EditText editText_UploadTrackToken;
    private TextView textView_UploadTrackId;
    private Switch switch_UploadTrackPublic;
    private TextView textView_UploadTrackName;
    private TextView textView_UploadTrackCom;
    private Button button_UploadTrack;
    private Button button_DeleteTrack;
    private Button button_UploadTrackTokenRegenerate;
    private String token = null;
    private long startTst;
    private long stopTst;
    private double length;
    private boolean uploadPublic;
    private GPSDatabase mGPSDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_track);
        editText_UploadTrackName = (EditText) findViewById(R.id.editText_UploadTrackName);
        editText_UploadTrackCom = (EditText) findViewById(R.id.editText_UploadTrackCom);
        editText_UploadTrackDuration = (EditText) findViewById(R.id.editText_UploadTrackDuration);
        editText_UploadTrackLength = (EditText) findViewById(R.id.editText_UploadTrackLength);
        editText_UploadTrackToken = (EditText) findViewById(R.id.editText_UploadTrackToken);
        textView_UploadTrackId = (TextView) findViewById(R.id.textView_UploadTrackId);
        switch_UploadTrackPublic = (Switch) findViewById(R.id.switch_UploadTrackPublic);
        textView_UploadTrackName = (TextView) findViewById(R.id.textView_UploadTrackName);
        textView_UploadTrackCom = (TextView) findViewById(R.id.textView_UploadTrackCom);
        button_DeleteTrack = (Button) findViewById(R.id.button_DeleteTrack);
        button_UploadTrack = (Button) findViewById(R.id.button_UploadTrack);
        button_UploadTrackTokenRegenerate = (Button) findViewById(R.id.button_UploadTrackTokenRegenerate);

        prefs = getSharedPreferences(getString(R.string.preference_file_key), (Context.MODE_MULTI_PROCESS));
        prefs_edit = prefs.edit();

        uploadPublic = prefs.getBoolean("upload_public", false);

        switch_UploadTrackPublic.setChecked(uploadPublic);

        button_UploadTrack.setEnabled(false);

        // Create Database
        mGPSDb = new GPSDatabase(this.getApplicationContext());

        // Only accept a-z, A-Z, "-" and "_" as name
        editText_UploadTrackName.setFilters(new InputFilter[]{new InputFilter() {
            public CharSequence filter(CharSequence src, int start, int end, Spanned dst, int dstart, int dend) {
                if (src.equals("")) { // for backspace
                    return src;
                }
                if (src.toString().matches("[a-zA-Z_-]+")) {
                    return src;
                }
                return "";
            }
        }});

        //receiving intent
        Intent incomingIntent = getIntent();
        if (incomingIntent.hasExtra("data")) {
            data = incomingIntent.getStringExtra("data");
        }
        startTst = incomingIntent.getLongExtra("startTst", 0);
        stopTst = incomingIntent.getLongExtra("stopTst", 0);
        length = incomingIntent.getDoubleExtra("totalDist", 0) / 1000;

        //mGlobalVariables = (GlobalVariables) getApplicationContext();
        //length = mGlobalVariables.getsGef();

        initUI();
        updateUI();
    }

    // Initialize GUI
    private void initUI() {

        editText_UploadTrackDuration.setText(formatTime(stopTst - startTst));
        editText_UploadTrackLength.setText(roundDecimals(length) + " km");
        uploadPublic = prefs.getBoolean("upload_public", false);
        token = prefs.getString("upload_token", null);
        if (token == null) {
            editText_UploadTrackToken.setText(R.string.loading);
            getToken();
        } else {
            editText_UploadTrackToken.setText(token);
        }


        String lname = prefs.getString("upload_name", null);
        if (lname != null) {
            editText_UploadTrackName.setText(lname);
        } else {
            editText_UploadTrackName.setText(this.getString(R.string.upload_track_name_default));
        }

        String lcom = prefs.getString("upload_com", null);
        if (lcom != null) {
            editText_UploadTrackCom.setText(lcom);
        } else {
            editText_UploadTrackCom.setText(this.getString(R.string.upload_track_com_default));
        }

        String ltoken = prefs.getString("upload_token", null);
        if (ltoken != null) {
            editText_UploadTrackToken.setText(ltoken);
            button_UploadTrack.setEnabled(true);
        }

        // Make editTexts not editable
        editText_UploadTrackToken.setKeyListener(null);
        editText_UploadTrackLength.setKeyListener(null);
        editText_UploadTrackDuration.setKeyListener(null);
    }

    // Update GUI (enable/disable name and comment editText)
    private void updateUI() {
        if (!uploadPublic) {
            editText_UploadTrackName.setEnabled(false);
            editText_UploadTrackCom.setEnabled(false);
            textView_UploadTrackName.setEnabled(false);
            textView_UploadTrackCom.setEnabled(false);
        } else {
            editText_UploadTrackName.setEnabled(true);
            editText_UploadTrackCom.setEnabled(true);
            textView_UploadTrackName.setEnabled(true);
            textView_UploadTrackCom.setEnabled(true);
        }
    }

    private String formatTime(double secondsInput) {
        String time;
        //calculate hours, seconds and minutes
        int seconds = (int) Math.round(secondsInput % 60);
        int minutes = (int) Math.round(((secondsInput - seconds) % 3600) / 60);
        int hours = (int) Math.round((secondsInput - seconds - minutes * 60) / 3600);
        String hoursStrg = putZero(hours);
        String minutesStrg = putZero(minutes);
        String secondsStrg = putZero(seconds);
        time = hoursStrg + "h " + minutesStrg + "min " + secondsStrg + "s";
        return time;
    }

    private String roundDecimals(double d) {
        return String.format("%.2f", d);
    }

    private void getToken() {
        String lurl = this.getString(R.string.api1_base_url) + this.getString(R.string.api1_token_new);
        Log.i(TAG, lurl);
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            GetHttpTask getHttpTask = new GetHttpTask();
            getHttpTask.setType("token");
            getHttpTask.execute(lurl);
        } else {
            Context context = getApplicationContext();
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(context, getString(R.string.upload_error_no_network_try_again_later), duration);
            toast.show();
        }
    }

    //put 0 before value, if != 0 and < 10
    private String putZero(int time_value_in) {
        String time_value_out;
        if ((time_value_in != 0) && (time_value_in < 10)) {
            time_value_out = "0" + time_value_in;
        } else {
            time_value_out = Integer.toString(time_value_in);
        }
        return time_value_out;
    }

    public void onSwitchPublic(View v) {
        if (switch_UploadTrackPublic.isChecked()) {
            uploadPublic = true;
            Log.i(TAG, "onSwitchPublic(): checked");
        } else {
            uploadPublic = false;
            Log.i(TAG, "onSwitchPublic(): not checked");
        }
        updateUI();
        savePublicToPrefs(uploadPublic);
    }

    private void savePublicToPrefs(Boolean p) {
        prefs_edit.putBoolean("upload_public", p);
        prefs_edit.apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent_settings = new Intent(this, SettingsActivity.class);
                startActivity(intent_settings);
                return true;
            case R.id.action_MainActivity:
                Intent intent_main = new Intent(this, MainActivity.class);
                startActivity(intent_main);
                return true;
            case R.id.action_RoutingActivity:
                Intent intent_routing = new Intent(this, RoutingActivity.class);
                startActivity(intent_routing);
                return true;
            case R.id.action_info:
                Intent intent_info = new Intent(this, InfoActivity.class);
                startActivity(intent_info);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onClickGetToken(View view) {
        getToken();
    }

    // Upload Track
    public void uploadTrack(View v) {
        // Disable Button to prevent multiple uploads
        button_DeleteTrack.setEnabled(false);
        button_UploadTrack.setEnabled(false);
        button_UploadTrackTokenRegenerate.setEnabled(false);
        String lurl = makeUrl();
        Log.i(TAG, lurl);
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            GetHttpTask getHttpTask = new GetHttpTask();
            getHttpTask.setType("upload");
            getHttpTask.execute(lurl, data);
        } else {
            Context context = getApplicationContext();
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(context, getString(R.string.upload_error_no_network_try_again_later), duration);
            toast.show();
        }
    }

    private String makeUrl() {
        String lurlstr;
        Uri.Builder lurl;
        lurl = Uri.parse(this.getString(R.string.api1_base_url) + this.getString(R.string.api1_pushtrack_new)).buildUpon().appendQueryParameter("duration", Long.toString(stopTst - startTst)).appendQueryParameter("length", Double.toString(length * 1000)).appendQueryParameter("user_token", token);
        if (uploadPublic) {
            lurl.appendQueryParameter("name", editText_UploadTrackName.getText().toString()).appendQueryParameter("comment", editText_UploadTrackCom.getText().toString()).appendQueryParameter("public", "true");
        } else {
            lurl.appendQueryParameter("name", "none").appendQueryParameter("comment", "none").appendQueryParameter("public", "false");
        }
        lurlstr = lurl.build().toString();
        return lurlstr;
    }

    // Given a URL, establishes an HttpUrlConnection and retrieves
    // the web page content as a InputStream, which it returns as
    // a string.
    private String getUrl(String lurlstr, String ldata) throws IOException {
        InputStream stream = null;

        try {
            URL url = new URL(lurlstr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(30000 /* milliseconds */);
            conn.setConnectTimeout(50000 /* milliseconds */);
            Log.i(TAG, "Old User-Agent: " + conn.getRequestProperty("User-Agent"));
            conn.setRequestProperty("User-Agent", "iBis app");
            Log.i(TAG, "New User-Agent: " + conn.getRequestProperty("User-Agent"));

            // Get returned body from webserver:
            conn.setDoInput(true);

            if (ldata != null) {
                conn.setRequestMethod("POST");
                Log.i(TAG, "POST: " + ldata);

                // Write POST Data:
                conn.setDoOutput(true);

                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("data", ldata));

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));

                String post = getQuery(params);
                Log.i(TAG, "POST string: " + post);
                writer.write(post);
                writer.flush();
                writer.close();
                os.close();

            } else {
                conn.setRequestMethod("GET");
            }

            // Starts the connection
            conn.connect();
            Log.i(TAG, "getUrl(): Response Code: " + conn.getResponseCode());
            stream = conn.getInputStream();

            // Convert the InputStream into a string
            return readIt(stream);

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    private String getQuery(List<NameValuePair> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (NameValuePair pair : params) {
            if (first) first = false;
            else result.append("&");

            result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
        }

        return result.toString();
    }

    // Reads an InputStream and converts it to a String.
    public String readIt(InputStream stream) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(stream));
        StringBuilder total = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            total.append(line);
        }
        return total.toString();
    }

    private String getDate(long timestamp) {
        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
        calendar.setTimeInMillis(timestamp * 1000);
        return DateFormat.format("dd. MM. yyyy, HH:mm", calendar).toString() + "h";
    }

    private void saveToken(String t) {
        token = t;
        if (prefs.contains("upload_token")) {
            String old_token = prefs.getString("upload_token", "");
            String old_tokenlist = prefs.getString("upload_oldtokenlist", "");
            prefs_edit.putString("upload_oldtokenlist", old_tokenlist + ";" + old_token);
        }
        prefs_edit.putString("upload_token", t);
        prefs_edit.apply();
    }

    public void openDeleteTrackAlert(View view) {
        //set up a new alert dialog
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(UploadTrackActivity.this);
        alertDialogBuilder.setTitle("Wirklich löschen?");
        alertDialogBuilder.setMessage("Möchten sie den aufgezeichneten Track wirklich löschen?");

        //create the OK Button and onClickListener
        alertDialogBuilder.setPositiveButton("Ja", new DialogInterface.OnClickListener() {
            //close dialog when clicked
            public void onClick(DialogInterface dialog, int id) {
                deleteTrack();
                dialog.cancel();
            }
        });
        //create the cancel Button and onClickListener
        alertDialogBuilder.setNegativeButton("Nein", new DialogInterface.OnClickListener() {
            //close dialog when clicked
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        //create and show alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    public void deleteTrack() {
        //delete track
        mGPSDb.open();
        mGPSDb.deleteData();
        mGPSDb.close();
        //show Toast
        Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.upload_track_deleted), Toast.LENGTH_LONG);
        toast.show();
        //go back to MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private class GetHttpTask extends AsyncTask<String, Void, String> {
        String type = "";

        public void setType(String s) {
            type = s;
        }

        @Override
        protected String doInBackground(String... urls) {
            try {
                switch (type) {
                    case "upload":
                        return getUrl(urls[0], urls[1]);
                    case "token":
                        return getUrl(urls[0], null);
                    default:
                        return getUrl(urls[0], null);
                }
            } catch (IOException e) {
                return getString(R.string.upload_error_server_unreachable);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Log.i(TAG, "HTTP result: " + result);
            switch (type) {
                case "token":
                    String ltoken;
                    try {
                        JSONObject json = new JSONObject(result);
                        if (json.has("token")) {
                            ltoken = json.getString("token");
                            button_UploadTrack.setEnabled(true);
                            saveToken(ltoken);
                        } else if (json.has("error")) {
                            ltoken = json.getString("error");
                        } else {
                            ltoken = "Unknown error";
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        ltoken = "Error in JSONObject";
                    }
                    editText_UploadTrackToken.setText(ltoken);
                    break;
                case "upload":
                    String notification = getString(R.string.unknownError);
                    boolean success = false;
                    try {
                        JSONObject json = new JSONObject(result);
                        if (json.has("track_id")) {
                            success = true;
                            // Disable button to prevent multiple uploads
                            button_UploadTrack.setEnabled(false);

                            // Get track_id
                            String track_id = json.getString("track_id");

                            // Set textView_UploadTrackId to track_id
                            textView_UploadTrackId.setText(track_id);

                            // Prepare notification string with track_id, date and nodes:
                            String created_s = "";
                            String nodes_s = "";
                            if (json.has("created")) {
                                created_s = getDate(json.getLong("created"));
                            }
                            if (json.has("nodes")) {
                                nodes_s = Long.toString(json.getLong("nodes"));
                            }

                            // getString(R.string.upload_track_success_trackid);
                            notification = "Track \"" + track_id + "\" mit " + nodes_s + " GPS-Koordinaten erstellt am " + created_s;
                        } else if (json.has("error")) {
                            notification = getString(R.string.error) + json.getString("error");

                            // Enable upload button to make second upload possible
                            button_UploadTrack.setEnabled(true);
                            button_UploadTrackTokenRegenerate.setEnabled(true);
                        } else {
                            notification = getString(R.string.unknownError);

                            // Enable upload button to make second upload possible
                            button_UploadTrack.setEnabled(true);
                            button_UploadTrackTokenRegenerate.setEnabled(true);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        notification = getString(R.string.httpJsonReturnNotificationErrTryCatchHttpJson);

                        // Enable upload button to make second upload possible
                        button_UploadTrack.setEnabled(true);
                    } finally {
                        // Create notification
                        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getBaseContext())
                                .setSmallIcon(R.mipmap.ic_launcher)
                                .setContentTitle(getString(R.string.app_name_short) + getString(R.string.trackUploaded))
                                .setContentText(notification)
                                .setStyle(new NotificationCompat.BigTextStyle().bigText(notification));
                        if(success) {
                            // default action (on success): open https://ibis.jufo.mytfg.de/WebUI/index.html
                            String tokens = prefs.getString("upload_token", "");
                            String oldtokens = prefs.getString("upload_oldtokenlist", "");
                            if (!oldtokens.equals("")) {
                                tokens = tokens + ";" + oldtokens;
                            }
                            tokens = "#token(" + tokens + ")";
                            Intent intentIbisWeb = new Intent(Intent.ACTION_VIEW, Uri.parse("https://ibis.jufo.mytfg.de/WebUI/index.html" + tokens));
                            PendingIntent pIntentIbisWeb = PendingIntent.getActivity(getApplicationContext(), 0, intentIbisWeb, 0);
                            mBuilder.setContentIntent(pIntentIbisWeb);
                        }
                        // Sets an ID for the notification
                        int mNotificationId = 43;
                        // Gets an instance of the NotificationManager service
                        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                        // Builds the notification and issues it.
                        mNotifyMgr.notify(mNotificationId, mBuilder.build());

                        // Additionally to notification show Toast message
                        Context context = getApplicationContext();
                        int duration = Toast.LENGTH_LONG;
                        Toast toast = Toast.makeText(context, notification, duration);
                        toast.show();
                    }
                    break;
            }
        }
    }
}

