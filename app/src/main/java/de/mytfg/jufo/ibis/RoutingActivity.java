package de.mytfg.jufo.ibis;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
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
    final String TAG = "RoutingActivity-class";
    //Views
    EditText destination_address;
    EditText start_address;
    EditText editDistance;
    Spinner selectRouteType;
    TextView loading_text;
    ImageView loading_image;
    Button start_navigation;
    Button generate_route;
    Switch switch_manuelDistance;
    //self-written classes
    RoutingDatabase mRDb;
    GlobalVariables mGlobalVariables;
    //vars
    double tAnkEingTime;
    boolean manuel_distance;
    String route_type;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routing);
        //get and set up views
        start_navigation = (Button) findViewById(R.id.start_navigation);
        start_navigation.setEnabled(false);
        generate_route = (Button) findViewById(R.id.generate_route);
        editDistance = (EditText) findViewById(R.id.enter_distance);
        start_address = (EditText) findViewById(R.id.start_address);
        destination_address = (EditText) findViewById(R.id.destination_address);
        loading_text = (TextView) findViewById(R.id.loading_text);
        loading_image = (ImageView) findViewById(R.id.loading_image);
        switch_manuelDistance = (Switch) findViewById(R.id.switch_manuelDistance);
        //global variables class
        mGlobalVariables = (GlobalVariables) getApplicationContext();
        //set up database, delete old database
        mRDb = new RoutingDatabase(this);
        mRDb.open();
        mRDb.deleteDatabase();
        mRDb.close();
        // configure select_route_type spinner
        selectRouteType = (Spinner) findViewById(R.id.select_route_type);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.route_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        selectRouteType.setAdapter(adapter);
        selectRouteType.setOnItemSelectedListener(this);
        //call updateUI()
        updateUI();
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

    public void showLoadingAnimation () {
        // set content
        loading_text.setText(R.string.loading_text);
        loading_image.setImageResource(R.drawable.ic_launcher);
        //start rotation
        Animation rotation = AnimationUtils.loadAnimation(this, R.anim.rotation);
        loading_image.startAnimation(rotation);
    }

    public void removeLoadingAnimation() {
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
        //show picked time
        TextView arrivalTime = (TextView) findViewById(R.id.arrivalTime);
        if (minute < 10) {
            arrivalTime.setText(hour + ":0" + minute + " Uhr");
        } else {
            arrivalTime.setText(hour + ":" + minute + " Uhr");
        }
        convertToMilliseconds(hour, minute);

        final Calendar c = Calendar.getInstance();
        int current_hour = c.get(Calendar.HOUR_OF_DAY);
        int current_minute = c.get(Calendar.MINUTE);

        //if set time is before current time add a day in milliseconds to set time
        if ((hour < current_hour) || (current_hour == hour) && (minute < current_minute)) {
            tAnkEingTime += 24 * 60 * 60 * 1000;
        }
        mGlobalVariables.settAnkEingTime(tAnkEingTime);

    }

    //convert hour and minutes to milliseconds for mathematical operations @Calculation
    public void convertToMilliseconds(int hour, int minute) {
        tAnkEingTime = (double) ((hour * 60 + minute) * 60 * 1000);
    }

    String roundDecimals(double d) {
        return String.format(Locale.US, "%.2f", d);
    }

    public void onSwitchManualDistance(View view) {
        manuel_distance = switch_manuelDistance.isChecked();
        updateUI();
    }

    public void updateUI() {
        //enable / disable UI elements
        if (manuel_distance) {
            start_address.setEnabled(false);
            destination_address.setEnabled(false);
            editDistance.setEnabled(true);
            start_navigation.setEnabled(true);
            generate_route.setEnabled(false);
        } else {
            start_address.setEnabled(true);
            destination_address.setEnabled(true);
            editDistance.setEnabled(false);
            start_navigation.setEnabled(false);
            generate_route.setEnabled(true);
        }
    }

    public void onClickStartNavigation(View view) {
        //read text from EditText and convert to String
        Double sEing = Double.parseDouble(editDistance.getText().toString());
        //try to convert String to Float
        mGlobalVariables.setsEing(sEing);
        //start ShowDataActivity
        Intent intent = new Intent(this, ShowDataActivity.class);
        startActivity(intent);
        //start tracking service
        Intent intent2 = new Intent(this, Tracking.class);
        startService(intent2);
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

            }
            removeLoadingAnimation();
        }
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

    private String makeUrl(String StartAddress, String DestinationAddress) {
        Log.i(TAG, "makeURL");
        String lurlstr;
        Uri.Builder lurl;
        lurl = Uri.parse(this.getString(R.string.api1_base_url) + this.getString(R.string.api1_get_route))
                .buildUpon()
                .appendQueryParameter("start", StartAddress)
                .appendQueryParameter("end", DestinationAddress)
                .appendQueryParameter("profile", route_type);
        lurlstr = lurl.build().toString();
        Log.i(TAG, lurlstr);
        return lurlstr;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_routing, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
