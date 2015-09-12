package de.mytfg.jufo.ibis.ibisApi;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import de.mytfg.jufo.ibis.util.Utils;

/**
 * This class is an abstract wrapper to access the MyTFG API.
 * It allows to call API functions by name with given parameters.
 */
public class IbisApi {
    private static String apiResource;
    private static String apiMethod;
    private static IbisApiCallback callback;

    /**
     * Calls an API function with given parameters. Calls the given callback function when request
     * finished.
     * @param apiResource The path (name) of the API-function to call.
     * @param params Parameters to pass to the function.
     * @param callback Function to call when request finished (or timed out).
     */
    public static void call(String apiResource, String apiMethod, JSONObject params, IbisApiCallback callback) {
        new IbisApi.RequestTask(apiResource, apiMethod, params, callback).execute();
    }

    private static class RequestTask extends AsyncTask<Void, Void, String> {
        private String apiResource;
        private String apiMethod;
        private JSONObject parameters;
        private IbisApiCallback callback;

        private String baseURL = "https://ibis.jufo.mytfg.de/api2";

        private int responseCode = -1;

        private RequestTask(String apiResource, String apiMethod, JSONObject params, IbisApiCallback callback) {
            this.apiResource = apiResource;
            this.apiMethod = apiMethod;
            this.parameters = params;
            this.callback = callback;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL url = new URL(baseURL + apiResource);
                HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
                connection.setReadTimeout(15000);
                connection.setConnectTimeout(10000);
                connection.setRequestMethod(apiMethod);
                connection.setDoInput(true);
                if(apiMethod.equals("POST") || apiMethod.equals("PUT")) {
                    connection.setDoOutput(true);
                    OutputStream os = connection.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(
                            new OutputStreamWriter(os, "UTF-8"));
                    writer.write(parameters.toString());
                    writer.flush();
                    writer.close();
                    os.close();
                }
                return Utils.readStream(connection.getInputStream());
            } catch (MalformedURLException ex) {
                return null;
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result == null) {
                callback.callback(responseCode, null);
            } else {
                try {
                    JSONObject jsonResult = new JSONObject(result);
                    callback.callback(responseCode, jsonResult);
                } catch (Exception ex) {
                    callback.callback(responseCode, null);
                }
            }
        }
    }
}
