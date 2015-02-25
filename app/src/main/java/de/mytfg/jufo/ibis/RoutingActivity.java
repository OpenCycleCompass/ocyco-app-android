package de.mytfg.jufo.ibis;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
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


public class RoutingActivity extends ActionBarActivity {

    final String TAG = "RoutingActivity-class";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routing);
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
            //TODO: Json Objekt empfangen und auswerten
            //create JSON Object
            JSONObject jObject = null;
            try {
                jObject = new JSONObject(result);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (jObject != null) {
                try {
                    //get JSON Array
                    JSONArray jArray = jObject.getJSONArray("points");
                    //Read from JSON Array
                    for (int i = 0; i < jArray.length(); i++) {
                        JSONObject oneObject = jArray.getJSONObject(i);
                        // Pulling items from the array
                        double lat = oneObject.getDouble("lat");
                        double lon = oneObject.getDouble("lon");
                    }
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
