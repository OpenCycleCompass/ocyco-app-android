package ibis.jufo.mytfg.de.ibis_intelligentbikeinformationsystem;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

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

    // Log TAG
    protected static final String TAG = "UploadTrackActivity-class";

    private EditText editText_UploadTrackName;
    private EditText editText_UploadTrackCom;
    private EditText editText_UploadTrackDuration;
    private EditText editText_UploadTrackLength;
    private EditText editText_UploadTrackToken;
    private TextView textView_UploadTrackId;

    private Button button_UploadTrack;

    private String data = "[]"; // empty, but valid JSON
    //private String token = "";
    private long startTst;
    private long stopTst;
    private long length = 0;

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

        button_UploadTrack = (Button) findViewById(R.id.button_UploadTrack);

        button_UploadTrack.setEnabled(false);

        // Only accept a-z, A-Z, "-" and "_" as name
        editText_UploadTrackName.setFilters(new InputFilter[]{
                new InputFilter() {
                    public CharSequence filter(CharSequence src, int start, int end, Spanned dst, int dstart, int dend) {
                        if (src.equals("")) { // for backspace
                            return src;
                        }
                        if (src.toString().matches("[a-zA-Z_-]+")) {
                            return src;
                        }
                        return "";
                    }
                }
        });

        //receiving intent
        Intent incomingIntent = getIntent();
        if (incomingIntent.hasExtra("data")) {
            data = incomingIntent.getStringExtra("data");
        }
        if (incomingIntent.hasExtra("startTst")) {
            startTst = incomingIntent.getLongExtra("startTst", 0);
        }
        if (incomingIntent.hasExtra("stopTst")) {
            stopTst = incomingIntent.getLongExtra("stopTst", 0);
        }

        length = calcLength();

        updateUI();
    }

    // Update GUI Fields
    private void updateUI() {
        editText_UploadTrackDuration.setText((stopTst - startTst) + "");

        long llength = calcLength();
        editText_UploadTrackLength.setText(llength + "");

        editText_UploadTrackName.setText(this.getString(R.string.upload_track_name_default));
        editText_UploadTrackCom.setText(this.getString(R.string.upload_track_com_default));

        String ltoken = getToken();
        if (ltoken != null) {
            editText_UploadTrackToken.setText(ltoken);
            button_UploadTrack.setEnabled(true);
        }
    }

    private String makeUrl() {
        long llength = calcLength();
        String lurl;
        lurl = Uri.parse(this.getString(R.string.api1_base_url) + this.getString(R.string.api1_pushtrack_new))
                .buildUpon()
                .appendQueryParameter("name", editText_UploadTrackName.getText().toString())
                .appendQueryParameter("comment", editText_UploadTrackCom.getText().toString())
                .appendQueryParameter("duration", (stopTst - startTst) + "")
                .appendQueryParameter("length", llength + "")
                .appendQueryParameter("user_token", getToken())
                .build().toString();
        return lurl;
    }

    public long calcLength() {
        // TODO: calculate length from GPS points
        // Evtl. in andere oder eigene Klasse auslagern?
        length = 42;
        return length;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    public void debugShowUrl(View v) {
        // Generate / concatenate url
        String lurl = makeUrl();
        // Show url as "Toast" message
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(context, lurl, duration);
        toast.show();
    }

    public void debugShowData(View v) {
        // Generate / concatenate url
        String ldata = data;
        // Show url as "Toast" message
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(context, ldata, duration);
        toast.show();
    }

    public void getToken(View v) {
        String lurl = "" + this.getString(R.string.api1_base_url) + this.getString(R.string.api1_token_new);
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

    // Upload Track
    public void uploadTrack(View v) {
        // Disable Button to prevent multiple uploads
        button_UploadTrack.setEnabled(false);
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


    // Given a URL, establishes an HttpUrlConnection and retrieves
    // the web page content as a InputStream, which it returns as
    // a string.
    private String getUrl(String lurlstr, String ldata) throws IOException {
        InputStream stream = null;
        // Only display the first 10 000 000 characters of the retrieved
        // web page content.
        final int len = 10000000;

        try {
            URL url = new URL(lurlstr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(3000 /* milliseconds */);
            conn.setConnectTimeout(5000 /* milliseconds */);
            Log.i(TAG, "Old User-Agent: " + conn.getRequestProperty("User-Agent"));
            conn.setRequestProperty("User-Agent", "iBis app");
            Log.i(TAG, "New User-Agent: " + conn.getRequestProperty("User-Agent"));

            // Get returned body from webserver:
            conn.setDoInput(true);

            if (ldata != null) {
                conn.setRequestMethod("POST");
                //conn.addRequestProperty("data", ldata); // wrong
                Log.i(TAG, "POST: " + ldata);

                // Write POST Data:
                conn.setDoOutput(true);

                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("data", ldata));

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));

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
            return readIt(stream, len);

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
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
        }

        return result.toString();
    }

    // Reads an InputStream and converts it to a String.
    public String readIt(InputStream stream, int len) throws IOException {
        Reader reader;
        reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[len];
        reader.read(buffer);
        return new String(buffer);
    }

    // Uses AsyncTask to create a task away from the main UI thread. This task takes a
    // URL string and uses it to create an HttpUrlConnection. Once the connection
    // has been established, the AsyncTask downloads the contents of the webpage as
    // an InputStream. Finally, the InputStream is converted into a string, which is
    // displayed in the UI by the AsyncTask's onPostExecute method.
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
                    try {
                        JSONObject json = new JSONObject(result);
                        if (json.has("track_id")) {
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
                                created_s = getDate(json.getLong("created")) + "";
                            }
                            if (json.has("nodes")) {
                                nodes_s = json.getLong("nodes") + "";
                            }

                            // getString(R.string.upload_track_success_trackid);
                            notification = "Track \"" + track_id + "\" mit " + nodes_s + " GPS-Koordinaten erstellt am " + created_s;
                        } else if (json.has("error")) {
                            notification = getString(R.string.error) + json.getString("error");

                            // Enable upload button to make second upload possible
                            button_UploadTrack.setEnabled(true);
                        } else {
                            notification = getString(R.string.unknownError);

                            // Enable upload button to make second upload possible
                            button_UploadTrack.setEnabled(true);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        notification = getString(R.string.httpJsonReturnNotificationErrTryCatchHttpJson);

                        // Enable upload button to make second upload possible
                        button_UploadTrack.setEnabled(true);
                    } finally {
                        // Create notification
                        Intent intentIbisWeb = new Intent(Intent.ACTION_VIEW, Uri.parse("https://ibis.jufo.mytfg.de/map.php"));
                        PendingIntent pIntentIbisWeb = PendingIntent.getActivity(getApplicationContext(), 0, intentIbisWeb, 0);
                        NotificationCompat.Builder mBuilder =
                                new NotificationCompat.Builder(getBaseContext())
                                        .setContentIntent(pIntentIbisWeb)
                                        .setSmallIcon(R.mipmap.ic_launcher)
                                        .setContentTitle(getString(R.string.app_name_short) + getString(R.string.trackUploaded))
                                        .setContentText(notification)
                                        .setStyle(new NotificationCompat.BigTextStyle().bigText(notification));
                        // Sets an ID for the notification
                        int mNotificationId = 43;
                        // Gets an instance of the NotificationManager service
                        NotificationManager mNotifyMgr =
                                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
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

    private String getDate(long timestamp) {
        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
        calendar.setTimeInMillis(timestamp * 1000);
        return DateFormat.format("dd. MM. yyyy, HH:mm", calendar).toString() + "h";
    }

    public void saveToken(String t) {
        SharedPreferences prefs = getSharedPreferences(getString(R.string.preference_file_key), (Context.MODE_MULTI_PROCESS));
        SharedPreferences.Editor prefs_edit = prefs.edit();
        if (prefs.contains("token")) {
            String old_token = prefs.getString("token", "");
            String old_tokenlist = prefs.getString("oldtokenlist", "");
            prefs_edit.putString("oldtokenlist", old_tokenlist + ";" + old_token);
        }
        prefs_edit.putString("token", t);
        prefs_edit.apply();
    }

    public String getToken() {
        SharedPreferences prefs = getSharedPreferences(getString(R.string.preference_file_key), (Context.MODE_MULTI_PROCESS));
        return prefs.getString("token", null);
    }
}

