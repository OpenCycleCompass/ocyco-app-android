package de.mytfg.jufo.ibis;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Locale;


public class RoutingActivity extends ActionBarActivity implements TimePickerFragment.OnTimePickedListener, AdapterView.OnItemSelectedListener {

    //LOG Tag
    private final static String TAG = "RoutingActivity-class";
    //self-written classes
    RoutingDatabase mRDb;
    GlobalVariables mGlobalVariables;
    //Timer for updating the map
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            if (navigate_from_current_position) {
                lookForStartPosition();
            }
            timerHandler.postDelayed(this, 500);
        }
    };
    //Views
    private EditText destination_address;
    private EditText start_address;
    private EditText editDistance;
    private TextView arrivalTime;
    private Spinner selectRouteType;
    private TextView loading_text;
    private ImageView loading_image;
    private Button start_navigation;
    private Button generate_route;
    private Button start_from_current_position;
    private Switch switch_manuelDistance;
    private Switch switch_userData;
    private Switch switch_timeFactor;
    //vars
    private double tAnkEingTime;
    private boolean manuel_distance, routing_with_user_data, navigate_from_current_position = false;
    private String route_type;
    private Location startLocation;
    //shared preferences
    private SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routing);
        //get and set up views
        start_navigation = (Button) findViewById(R.id.start_navigation);
        start_navigation.setEnabled(false);
        generate_route = (Button) findViewById(R.id.generate_route);
        start_from_current_position = (Button) findViewById(R.id.start_from_current_position);
        editDistance = (EditText) findViewById(R.id.enter_distance);
        start_address = (EditText) findViewById(R.id.start_address);
        destination_address = (EditText) findViewById(R.id.destination_address);
        loading_text = (TextView) findViewById(R.id.loading_text);
        arrivalTime = (TextView) findViewById(R.id.arrivalTime);
        loading_image = (ImageView) findViewById(R.id.loading_image);
        switch_manuelDistance = (Switch) findViewById(R.id.switch_manuelDistance);
        switch_userData = (Switch) findViewById(R.id.switch_userData);
        switch_timeFactor = (Switch) findViewById(R.id.switch_timeFactor);
        //global variables class
        mGlobalVariables = (GlobalVariables) getApplicationContext();
        //set up database, delete old database
        mRDb = new RoutingDatabase(this);
        mRDb.open();
        mRDb.deleteDatabase();
        mRDb.close();
        // configure select_route_type spinner
        selectRouteType = (Spinner) findViewById(R.id.select_route_type);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.route_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        selectRouteType.setAdapter(adapter);
        selectRouteType.setOnItemSelectedListener(this);
        // Restore preferences
        settings = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        editDistance.setText(settings.getString("distance", ""));
        start_address.setText(settings.getString("start_string", ""));
        destination_address.setText(settings.getString("dest_string", ""));
        //call updateUI()
        updateUI();

        //set onFocusListener
        start_address.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    navigate_from_current_position = false;
                    start_address.setText("");
                }
            }
        });
    }

    protected void onStop() {
        super.onStop();
        //creating a editor and add variables
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("start_string", start_address.getText().toString());
        editor.putString("dest_string", destination_address.getText().toString());
        editor.putString("distance", editDistance.getText().toString());
        // Commit the edits!
        editor.apply();
    }

    private void updateUI() {
        //enable / disable UI elements
        if (manuel_distance) {
            start_address.setEnabled(false);
            destination_address.setEnabled(false);
            editDistance.setEnabled(true);
            start_navigation.setEnabled(true);
            generate_route.setEnabled(false);
            start_from_current_position.setEnabled(false);
            switch_userData.setChecked(false);
            switch_userData.setEnabled(false);
            switch_timeFactor.setChecked(false);
            switch_timeFactor.setEnabled(false);
        } else {
            start_address.setEnabled(true);
            destination_address.setEnabled(true);
            editDistance.setEnabled(false);
            start_navigation.setEnabled(false);
            generate_route.setEnabled(true);
            start_from_current_position.setEnabled(true);
            switch_userData.setEnabled(true);
            switch_timeFactor.setEnabled(true);
        }
    }

    public void startFromCurrentPosition(View view) {
        // start tracking service for getting actual position
        navigate_from_current_position = true;
        Intent intent = new Intent(this, Tracking.class);
        startService(intent);
        start_address.setText(R.string.search_position);
        startLookingForCurrentLocation();
    }

    private void startLookingForCurrentLocation() {
        timerHandler.postDelayed(timerRunnable, 0);
    }

    private void lookForStartPosition() {
        try {
            startLocation = mGlobalVariables.getLocation();
            start_address.setText(startLocation.getLatitude() + "    " + startLocation.getLongitude());
            // focus on next edit text
            destination_address.setFocusable(true);
            destination_address.setFocusableInTouchMode(true);
            destination_address.requestFocus();
        } catch (Exception e) {
            //nothing
        }
    }

    public void onClickGenerateRoute(View view) {
        Log.i(TAG, "onClickGenerateRoute");
        //read addresses from edit text
        String strStartAddress = start_address.getText().toString();
        String strDestinationAddress = destination_address.getText().toString();
        makeUrl(strStartAddress, strDestinationAddress);
        //make URL String
        String lurl = makeUrl(strStartAddress, strDestinationAddress);
        //check connection
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            GetHttpTask getHttpTask = new GetHttpTask();
            getHttpTask.setType("upload");
            getHttpTask.execute(lurl);
        } else {
            //show error
            Context context = getApplicationContext();
            int duration = Toast.LENGTH_LONG;
            Toast toast = Toast.makeText(context, getString(R.string.upload_error_no_network_try_again_later), duration);
            toast.show();
        }
        showLoadingAnimation();
    }

    private String makeUrl(String StartAddress, String DestinationAddress) {
        Log.i(TAG, "makeURL");
        String optimize = (routing_with_user_data) ? "1" : "0";
        String lurlstr;
        Uri.Builder lurl;
        if (navigate_from_current_position) {
            lurl = Uri.parse(this.getString(R.string.api1_base_url) + this.getString(R.string.api1_get_route)).buildUpon().appendQueryParameter("start_lat", startLocation.getLatitude() + "").appendQueryParameter("start_lon", startLocation.getLongitude() + "").appendQueryParameter("end", DestinationAddress).appendQueryParameter("optimize", optimize).appendQueryParameter("profile", route_type);
        } else {
            lurl = Uri.parse(this.getString(R.string.api1_base_url) + this.getString(R.string.api1_get_route)).buildUpon().appendQueryParameter("start", StartAddress).appendQueryParameter("end", DestinationAddress).appendQueryParameter("optimize", optimize).appendQueryParameter("profile", route_type);
        }
        lurlstr = lurl.build().toString();
        Log.i(TAG, lurlstr);
        return lurlstr;
    }

    private void showLoadingAnimation() {
        // set content
        loading_text.setText(R.string.loading_text);
        loading_image.setImageResource(R.drawable.ic_launcher);
        //start rotation
        Animation rotation = AnimationUtils.loadAnimation(this, R.anim.rotation);
        loading_image.startAnimation(rotation);
    }

    private void removeLoadingAnimation() {
        // remove loading animation
        loading_text.setText("");
        loading_image.setImageResource(0);
    }

    //create and show the TimePickerFragment
    public void showTimePickerDialog(View v) {
        DialogFragment mTimePickerFragment = new TimePickerFragment();
        mTimePickerFragment.show(getSupportFragmentManager(), "timePicker");
    }

    //get picked time from TimePickerFragment via Interface
    public void onTimePicked(int hour, int minute) {
        showTime(hour, minute);

        tAnkEingTime = convertToMilliseconds(hour, minute);
        Log.i(TAG, "TimeVars tAnkEingTime onTimePicked" + tAnkEingTime);

        final Calendar c = Calendar.getInstance();
        int current_hour = c.get(Calendar.HOUR_OF_DAY);
        int current_minute = c.get(Calendar.MINUTE);

        //if set time is before current time add a day in milliseconds to set time
        if ((hour < current_hour) || (current_hour == hour) && (minute < current_minute)) {
            tAnkEingTime += 24 * 60 * 60 * 1000;
        }
        mGlobalVariables.settAnkEingTime(tAnkEingTime);

    }

    private void showTime(int hour, int minute) {
        //show picked time
        if (minute < 10) {
            arrivalTime.setText(hour + ":0" + minute + " Uhr");
        } else {
            arrivalTime.setText(hour + ":" + minute + " Uhr");
        }
    }

    //convert hour and minutes to milliseconds for mathematical operations @Calculation
    private double convertToMilliseconds(int hour, int minute) {
        return (double) ((hour * 60 + minute) * 60 * 1000);
    }

    String roundDecimals(double d) {
        return String.format(Locale.US, "%.2f", d);
    }

    public void onSwitchManualDistance(View view) {
        manuel_distance = switch_manuelDistance.isChecked();
        updateUI();
    }

    public void onSwitchUserData(View view) {
        routing_with_user_data = switch_userData.isChecked();
        if (switch_userData.isChecked()) {
            switch_timeFactor.setEnabled(true);
        } else {
            switch_timeFactor.setChecked(false);
            switch_timeFactor.setEnabled(false);
        }
    }

    public void onSwitchTimeFactor(View view) {
        mGlobalVariables.setUseTimeFactor(switch_timeFactor.isChecked());
    }

    public void onClickStartNavigation(View view) {
        boolean distanceExc = false;
        boolean timeExc = false;
        if (!manuel_distance) {
            mRDb.open();
            mGlobalVariables.setsEingTimeFactor(mRDb.getTotalDistTimeFactored());
            mRDb.close();
        }
        //read text from EditText and convert to String
        try {
            //try to convert String to Float
            Double sEing = Double.parseDouble(editDistance.getText().toString());
            mGlobalVariables.setsEing(sEing);
        } catch (Exception e) {
            distanceExc = true;
        }
        // open alert with correct text
        if (mGlobalVariables.gettAnkEingTime() == 0) {
            timeExc = true;
        }
        if (timeExc && distanceExc) {
            openAlert("Strecke und keine Uhrzeit");
        } else if (distanceExc) {
            openAlert("Strecke");
        } else if (timeExc) {
            openAlert("Uhrzeit");
        }
        // only start ShowDataActivity and Tracking Service, if both excs are false
        if (!timeExc && !distanceExc) {
            //start ShowDataActivity
            Intent intent = new Intent(this, ShowDataActivity.class);
            startActivity(intent);
            //start tracking service
            Intent intent2 = new Intent(this, Tracking.class);
            startService(intent2);
        }
    }

    private void openAlert(String missing_value) {
        //set up a new alert dialog
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(RoutingActivity.this);
        alertDialogBuilder.setTitle("Fehler!");
        alertDialogBuilder.setMessage("Sie haben keine " + missing_value + " eingegeben!");

        //create the OK Button and onClickListener
        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            //close dialog when clicked
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        //create and show alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (position) {
            case (0):
                route_type = "default";
            case (1):
                route_type = "shortest";
            case (2):
                route_type = "fastest";
            case (3):
                route_type = "scenery";
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // nothing

    }

    // Given a URL, establishes an HttpUrlConnection and retrieves
    // the web page content as a InputStream, which it returns as
    // a string.
    private String getUrl(String lurlstr) throws IOException {
        Log.i(TAG, "getURL");
        InputStream stream = null;
        // Only display the first 10 000 000 characters of the retrieved
        // web page content.
        final int len = 10000000;

        try {
            URL url = new URL(lurlstr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(5000 /* milliseconds */);
            Log.i(TAG, "Old User-Agent: " + conn.getRequestProperty("User-Agent"));
            conn.setRequestProperty("User-Agent", "iBis app");
            Log.i(TAG, "New User-Agent: " + conn.getRequestProperty("User-Agent"));

            // Get returned body from webserver:
            conn.setDoInput(true);
            conn.setRequestMethod("GET");

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

    // Reads an InputStream and converts it to a String.
    public String readIt(InputStream stream, int len) throws IOException {
        Log.i(TAG, "readIt");
        Reader reader;
        reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[len];
        reader.read(buffer);
        return new String(buffer);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_routing, menu);
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
            case R.id.action_show_data_activity:
                Intent intent_showData = new Intent(this, ShowDataActivity.class);
                startActivity(intent_showData);
                return true;
            case R.id.action_MainActivity:
                Intent intent_main = new Intent(this, MainActivity.class);
                startActivity(intent_main);
                return true;
            case R.id.action_info:
                Intent intent_info = new Intent(this, InfoActivity.class);
                startActivity(intent_info);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class GetHttpTask extends AsyncTask<String, Void, String> {
        String type = "";

        public void setType(String s) {
            Log.i(TAG, "setType");
            type = s;
        }

        @Override
        protected String doInBackground(String... urls) {
            Log.i(TAG, "doInBackground");
            try {
                return getUrl(urls[0]);
            } catch (IOException e) {
                return getString(R.string.upload_error_server_unreachable) + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Log.i(TAG, "onPostExecute");
            Log.i(TAG, "HTTP result: " + result);
            //create JSON Object
            JSONObject jObject = null;
            try {
                jObject = new JSONObject(result);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (jObject != null) {
                mRDb.open();
                try {
                    //get JSON Array
                    JSONArray jArray = jObject.getJSONArray("points");
                    // delete old database
                    mRDb.deleteData();
                    //read and insert points from jArrray
                    mRDb.readPointsArray(jArray);
                    //get total dist, convert to km an round
                    double totalDist = mRDb.getTotalDist() / 1000;
                    String totalDistRounded = roundDecimals(totalDist);
                    mRDb.close();
                    //show Toast
                    int duration = Toast.LENGTH_LONG;
                    Toast toast = Toast.makeText(getApplicationContext(), "Die Route wurde generiert, die Strecke beträgt " + totalDistRounded + "km", duration);
                    toast.show();
                    //enable start_navigation button in case of successful route generating
                    start_navigation.setEnabled(true);
                    //write totalDistRounded to edit text
                    editDistance.setText(totalDistRounded);

                } catch (JSONException e) {
                    e.printStackTrace();
                    int duration = Toast.LENGTH_LONG;
                    Toast toast = Toast.makeText(getApplicationContext(), R.string.rout_find_error, duration);
                    toast.show();
                    mRDb.close();
                }
                try {
                    //get JSON Array
                    double distance = jObject.getDouble("distance");
                    Log.i(TAG, "totalDistServer " + distance);
                    Log.i(TAG, "totalCntServer " + jObject.getLong("numpoints"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else {
                int duration = Toast.LENGTH_LONG;
                Toast toast = Toast.makeText(getApplicationContext(), R.string.upload_error_no_network_try_again_later, duration);
                toast.show();
            }
            removeLoadingAnimation();
        }
    }
}
