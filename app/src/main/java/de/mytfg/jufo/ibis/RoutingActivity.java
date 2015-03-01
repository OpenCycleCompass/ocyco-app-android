package de.mytfg.jufo.ibis;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
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
import android.widget.Button;
import android.widget.EditText;
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


public class RoutingActivity extends ActionBarActivity implements TimePickerFragment.OnTimePickedListener {

    final String TAG = "RoutingActivity-class";
    RoutingDatabase mRDb;
    Button start_navigation;
    double tAnkEingTime;
    GlobalVariables mGlobalVariables;
    EditText editDistance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routing);
        //get and set up views
        start_navigation = (Button) findViewById(R.id.start_navigation);
        start_navigation.setEnabled(false);
        editDistance = (EditText) findViewById(R.id.enter_distance);
        //set up database, delete old database
        mRDb = new RoutingDatabase(this);
        mGlobalVariables = (GlobalVariables) getApplicationContext();
        mRDb.open();
        boolean deleted = mRDb.deleteDatabase();
        Log.i(TAG, "deleted " + deleted);
        mRDb.close();
    }

    public void onClickGenerateRoute(View view) {
        Log.i(TAG, "onClickGenerateRoute");
        //read addresses from edit text
        EditText start_address = (EditText) findViewById(R.id.start_address);
        String strStartAddress = start_address.getText().toString();
        EditText destination_address = (EditText) findViewById(R.id.destination_address);
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

    public void onClickStartNavigation(View view) {
        //TODO: switch for manual distance
        if (true) {
            //read text from EditText and convert to String
            editDistance = (EditText) findViewById(R.id.enter_distance);
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
            double dist;
            //create JSON Object
            JSONObject jObject = null;
            Location oldLocation = new Location("");
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
                    //Read from JSON Array
                    for (int i = 0; i < jArray.length(); i++) {
                        Location location = new Location("");
                        JSONObject oneObject = jArray.getJSONObject(i);
                        // Pulling items from the array
                        double lat = oneObject.getDouble("lat");
                        double lon = oneObject.getDouble("lon");
                        location.setLatitude(lat);
                        location.setLongitude(lon);
                        if (i != 0) {
                            dist = location.distanceTo(oldLocation);
                        } else {
                            dist = 0;
                        }
                        //insert into db
                        mRDb.insertData(lat, lon, dist);
                        oldLocation = location;
                    }
                    //get total dist, convert to km an round
                    double totalDist = mRDb.getTotalDist()/1000;
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
                .appendQueryParameter("end", DestinationAddress);
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
