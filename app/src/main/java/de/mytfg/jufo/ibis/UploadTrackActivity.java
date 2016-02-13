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
import android.support.v7.app.AppCompatActivity;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Locale;

import de.mytfg.jufo.ibis.storage.IbisTrack;
import de.mytfg.jufo.ibis.util.TransparentLoadingOverlay;
import de.mytfg.jufo.ibis.util.Utils;

public class UploadTrackActivity extends AppCompatActivity {
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
    private Button button_UploadTrackTLater;
    private String token = null;
    private boolean uploadPublic;
    private TransparentLoadingOverlay mTLoadingOverlay;

    private IbisTrack data;
    private long duration; // milliseconds
    private double distance;

    private long track_source;

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
        button_UploadTrackTLater = (Button) findViewById(R.id.button_UploadTrackLater);
        mTLoadingOverlay = new TransparentLoadingOverlay(this);
        prefs = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        prefs_edit = prefs.edit();

        uploadPublic = prefs.getBoolean("upload_public", false);

        switch_UploadTrackPublic.setChecked(uploadPublic);

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
        if (incomingIntent.hasExtra("track")) {
            track_source = incomingIntent.getLongExtra("track", -1);
            if (track_source == -1l) {
                Toast.makeText(this,getString(R.string.upload_track_error_no_track),
                        Toast.LENGTH_LONG).show();
                return;
            }
            else if (track_source == 0) {
                data = IbisApplication.mGPSDB;
                duration = IbisApplication.mGPSDB.metaData.getDuration();
                distance = IbisApplication.mGPSDB.metaData.getTotalDistance();
            } else {
                ObjectInputStream input;
                String filename = Long.toString(track_source) + ".track";

                try {
                    input = new ObjectInputStream(new FileInputStream(new File(new File(getFilesDir(),"")+ File.separator+filename)));
                    data = (IbisTrack) input.readObject();
                    input.close();
                    duration = data.metaData.getDuration();
                    distance = data.metaData.getTotalDistance();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this,getString(R.string.upload_track_error_no_track),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                button_UploadTrackTLater.setVisibility(View.INVISIBLE);
                button_DeleteTrack.setVisibility(View.INVISIBLE);
            }
        }
        else {
            track_source = -1;
            Toast.makeText(this,
                    getString(R.string.upload_track_error_no_track), Toast.LENGTH_LONG).show();
            data = null;
            duration = 0;
            distance = 0;
        }

        initUI();
        updateUI();
    }

    // Initialize GUI
    private void initUI() {

        editText_UploadTrackDuration.setText(
                Utils.formatTime(duration));
        editText_UploadTrackLength.setText(
                String.format("%s km", Utils.roundDecimals(distance/1000.0)));
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
        String lurl = makeUrl();
        Log.i(TAG, lurl);
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            mTLoadingOverlay.show();
            GetHttpTask getHttpTask = new GetHttpTask();
            getHttpTask.setType("upload");
            getHttpTask.execute(lurl, data.getJSONArray().toString());
        } else {
            Toast.makeText(
                    getApplicationContext(),
                    getString(R.string.upload_error_no_network_try_again_later),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    // Upload Track Later
    public void uploadTrackLater(View v) {
        String trackName = String.valueOf(System.currentTimeMillis());
        SharedPreferences sharedPrefs = getSharedPreferences(
                getString(R.string.preference_file_key_upload_later), Context.MODE_PRIVATE);
        SharedPreferences.Editor sharedPrefsEditor = sharedPrefs.edit();
        // save track to file
        String filename = trackName + ".track";
        ObjectOutput out;
        try {
            out = new ObjectOutputStream(
                    new FileOutputStream(new File(getFilesDir(),"") + File.separator + filename)
            );
            out.writeObject(data);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // append trackName to trackName list
        if (sharedPrefs.contains("trackNameList")) {
            String oldTrackNameList = sharedPrefs.getString("trackNameList", "");
            sharedPrefsEditor.putString("trackNameList", oldTrackNameList + trackName + ";");
        }
        else {
            sharedPrefsEditor.putString("trackNameList", trackName + ";");
        }
        // commit/apply shared preferences
        sharedPrefsEditor.apply();
        // show toast
        Toast.makeText(
                getApplicationContext(),
                getString(R.string.upload_track_later_success),
                Toast.LENGTH_LONG
        ).show();
        // disable buttons
        button_UploadTrackTokenRegenerate.setEnabled(false);
        button_UploadTrack.setEnabled(false);
        button_UploadTrackTLater.setEnabled(false);
        button_DeleteTrack.setEnabled(false);
        //delete track database data
        IbisApplication.mGPSDB.deleteData();
    }

    private String makeUrl() {
        String lurlstr;
        Uri.Builder lurl;
        lurl = Uri.parse(this.getString(R.string.api1_base_url) + this.getString(R.string.api1_pushtrack_new)).buildUpon().appendQueryParameter("duration", Long.toString(duration)).appendQueryParameter("length", Double.toString(distance)).appendQueryParameter("user_token", token);
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
            conn.setRequestProperty("User-Agent", "iBis app");
            Log.i(TAG, "New User-Agent: " + conn.getRequestProperty("User-Agent"));

            // Get returned body from webserver:
            conn.setDoInput(true);

            if (ldata != null) {
                conn.setRequestMethod("POST");
                Log.i(TAG, "POST: " + ldata);

                // Write POST Data:
                conn.setDoOutput(true);

                //List<NameValuePair> params = new ArrayList<>();
                //params.add(new BasicNameValuePair("data", ldata));

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));

                String post = "data" + "=" + ldata;
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
            return Utils.readStream(stream);

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
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
        //delete track database data
        IbisApplication.mGPSDB.deleteData();
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
            mTLoadingOverlay.dismiss();
            switch (type) {
                case "token":
                    String ltoken;
                    try {
                        JSONObject json = new JSONObject(result);
                        if (json.has("token")) {
                            ltoken = json.getString("token");
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
                            // disable button to prevent multiple uploads
                            button_UploadTrack.setEnabled(false);
                            button_DeleteTrack.setEnabled(false);
                            button_UploadTrackTokenRegenerate.setEnabled(false);
                            button_UploadTrackTLater.setEnabled(false);

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

                            // delete old track
                            if (track_source == 0) {
                                data.deleteData();
                            }
                            else {
                                // TODO delete track track_source from files and shared preferences
                            }
                        } else if (json.has("error")) {
                            notification = getString(R.string.error) + json.getString("error");
                        } else {
                            notification = getString(R.string.unknownError);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        notification = getString(R.string.httpJsonReturnNotificationErrTryCatchHttpJson);
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
                        // Gets an instance of the NotificationManager service
                        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                        // Builds the notification and issues it.
                        mNotifyMgr.notify(43, mBuilder.build());

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

