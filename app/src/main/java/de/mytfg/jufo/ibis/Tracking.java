package de.mytfg.jufo.ibis;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public class Tracking extends Service implements LocationListener, OnConnectionFailedListener, ConnectionCallbacks {

    // The desired interval for location updates. Inexact. Updates may be more or less frequent.
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 3000;
    // The fastest rate for active location updates. Exact. Updates will never be more frequent than this value.
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
    // Log TAG
    protected static final String TAG = "IBisTracking-class";
    // Provides the entry point to Google Play services.
    protected GoogleApiClient mGoogleApiClient;
    //Stores parameters for requests to the FusedLocationProviderApi.
    protected LocationRequest mLocationRequest;
    //location vars
    protected Location mCurrentLocation;
    //create a new instance of classes
    Calculate mCalculate = new Calculate();
    GlobalVariables mGlobalVariable;
    private GPSDatabase mGPSDb;
    private boolean saveData = true;
    private String accNotiStr;
    // Notification
    // Sets an ID for the notification
    private int mNotificationId = 42;
    private NotificationCompat.Builder mBuilder;
    // Gets an instance of the NotificationManager service
    private NotificationManager mNotifyMgr;

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "onLocationChanged()");
        mCurrentLocation = location;
        //write position to GlobalVariables class
        mGlobalVariable.setLocation(mCurrentLocation);
        //only save data, if accuracy is ok
        if (mGlobalVariable.isCollectData() && checkAccuracy(location.getAccuracy())) {
            updateDatabase();
            //update Notification
            mGPSDb.open();
            int num_rows = mGPSDb.getNumRows();
            double total_dist = mGPSDb.getTotalDist();
            mGPSDb.close();
            String s_total_dist = roundDecimals(total_dist / 1000d);
            // Update notification
            mBuilder.setContentText(accNotiStr + getString(R.string.tracking_status_active) + " - " + num_rows + getString(R.string.coordinates) + s_total_dist + " " + getString(R.string.km));
            // Sets an ID for the notification
            int mNotificationId = 42;
            // Builds the notification and issues it.
            mNotifyMgr.notify(mNotificationId, mBuilder.build());
        }
        callCalculate();
    }

    private boolean checkAccuracy(Float accuracy) {
        if (accuracy > 20) {
            saveData = false;
            accNotiStr = "GPS wird gesucht ...";
        } else if (accuracy < 20) {
            saveData = true;
            accNotiStr = "";

        }
        return saveData;
    }

    private void updateDatabase() {
        Log.i(TAG, "updateDatabase()");
        mGPSDb.open();
        mGPSDb.insertLocation(mCurrentLocation);
        mGPSDb.close();
    }

    private String roundDecimals(double d) {
        return String.format("%.2f", d);
    }

    private void callCalculate() {
        Log.i(TAG, "callCalculate()");
        mCalculate.getData(mCurrentLocation, mGlobalVariable.getsEing());
        //only call mathematical methods, if this is not the first location - else there will be a NPE
        if (!mCalculate.checkFirstLoc()) {
            mCalculate.calculateTimeVars(mGlobalVariable.gettAnkEingTime());
            mGPSDb.open();
            mCalculate.calculateDrivenDistance(mGPSDb.getTotalDist());
            Log.i(TAG, "sEing tf callCalc"+(mGPSDb.getTotalDist()));
            mGPSDb.close();
            mCalculate.calculateDrivenTime();
            mCalculate.calculateSpeed();
            mCalculate.math(mGlobalVariable.isUseTimeFactor(), mGlobalVariable.getsEingTimeFactor() / 1000d);
            Log.i(TAG, "sEing tf callCalc"+(mGlobalVariable.getsEingTimeFactor() / 1000d));
            //get Variables from calculation
            double sGef = mCalculate.getsGef();
            double sZuf = mCalculate.getsZuf();
            double vAkt = mCalculate.getvAkt();
            double vD = mCalculate.getvD();
            double tAnk = mCalculate.gettAnk();
            double tAnkUnt = mCalculate.gettAnkUnt();
            double vDMuss = mCalculate.getvDMuss();
            double vDunt = mCalculate.getvDunt();
            //write Variables to global class
            mGlobalVariable.setCalculationVars(sGef, sZuf, vAkt, vD, tAnk, tAnkUnt, vDMuss, vDunt);

        }

    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate()");
        super.onCreate();
        // Create Database
        mGPSDb = new GPSDatabase(this.getApplicationContext());
        // Delete old data from database
        mGPSDb.open();
        mGPSDb.deleteDatabase();
        mGPSDb.close();

        accNotiStr = getString(R.string.tracking_gps_searching);

        // Create Notification
        Intent tracking_showIntent = new Intent(this, ShowDataActivity.class);
        tracking_showIntent.setFlags(tracking_showIntent.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
        PendingIntent tracking_showPendingIntent = PendingIntent.getActivity(this, 0, tracking_showIntent, 0);

        Intent tracking_stopIntent = new Intent(this, Tracking.class);
        tracking_stopIntent.putExtra("stopOnlineTracking", true);
        PendingIntent tracking_stopPendingIntent = PendingIntent.getService(this, 0, tracking_stopIntent, 0);

        mBuilder = new NotificationCompat.Builder(this);

        mBuilder.setSmallIcon(R.mipmap.ic_launcher).setContentTitle(getString(R.string.app_name)).setContentIntent(tracking_showPendingIntent)
                // default action (sole action on
                // android < 4.2) is to start ShowDataActivity
                .addAction(R.drawable.ic_action_cancel, getString(R.string.tracking_stop_notification), tracking_stopPendingIntent)
                        // Action Button: start SettingsActivity and call stopOnlineTracking()
                .addAction(R.drawable.ic_action_map, getString(R.string.tracking_show_tracking), tracking_showPendingIntent)
                        // Action Button: start ShowDataActivity
                .setOngoing(true); // notification is permanent

        // Gets an instance of the NotificationManager service
        mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        //build and connect Api Client
        buildGoogleApiClient();
        mGoogleApiClient.connect();
        Log.i(TAG, "checkOnline(): conn");

        //initialize global variable class
        mGlobalVariable = (GlobalVariables) getApplicationContext();
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * LocationServices API.
     */
    protected void buildGoogleApiClient() {
        Log.i(TAG, "buildGoogleApiClient()");
        mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(LocationServices.API).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
        createLocationRequest();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        // Positionsbestimmung mindestens ca. alle 5 Sekunden (5000ms)
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        // Positionsbestimmung höchstens jede Sekunde (1000ms)
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        // Hohe Genauigkeit
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand()");
        //read Extra data from intent. If Service was started by the system, there will be a RuntimeExc.
        try {
            if (intent.hasExtra("stopOnlineTracking")) {
                if (intent.getBooleanExtra("stopOnlineTracking", false)) {
                    mGlobalVariable.setCollectData(false);
                }
            }
        } catch (RuntimeException e) {
            stopSelf();
        }
        checkOnline();
        return super.onStartCommand(intent, flags, startId);
    }

    public void checkOnline() {
        Log.i(TAG, "checkOnline(): " + "CollectData: " + mGlobalVariable.isCollectData() + " Running: " + mGlobalVariable.isTrackingRunning());
        if (mGlobalVariable.isCollectData() && !mGlobalVariable.isTrackingRunning()) {
            startOnlineTracking();
        } else if (mGlobalVariable.isTrackingRunning()) {
            //mGlobalVariable.setCollectData(false);
            stopOnlineTracking();
        }
    }

    private void startOnlineTracking() {
        Log.i(TAG, "startOnlineTracking()");
        mGlobalVariable.setTrackingRunning(true);
        // Create Notification with track info
        mBuilder.setContentText(accNotiStr + getString(R.string.tracking_status_active));
        // Builds the notification and issues it.
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }

    private void stopOnlineTracking() {
        Log.i(TAG, "stopOnlineTracking()");
        //cancel notificationStopOnlineTracking
        int mNotificationId = 42;
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(mNotificationId);
        // Start Intent returned by mGPSDb.sendToServer()
        // intent has track data as "Extra"
        mGPSDb.open();
        //ATTENTION: can have the effect, that the UploadActivity will not be started!
        int d_rows = mGPSDb.prepareDB();
        Log.i(TAG, Integer.toString(d_rows) + " " + getString(R.string.tracking_prepare_coords_removed_filter1));
        mGPSDb.close();
        Intent intent = mGPSDb.sendToServer(this);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //only start activity, if data isn't empty
        //if (!intent.getStringExtra("data").equals("[]")) {
        if (!intent.getStringExtra("data").equals(UploadTrackActivity.data_empty)) { // valid but empty JSON defined in UploadTrackActivity ("[]")
            startActivity(intent);
        }
        mGPSDb.open();
        mGPSDb.deleteDatabase();
        mGPSDb.close();
        mGlobalVariable.setTrackingRunning(false);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        super.onDestroy();
        if (mGlobalVariable.isCollectData()) {
            stopOnlineTracking();
        }
        stopLocationUpdates();
        mGoogleApiClient.disconnect();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void stopLocationUpdates() {
        Log.i(TAG, "stopLocationUpdates()");
        // Stop LocationListener
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "onConnected()");
        startLocationUpdates();
        // Evtl startLocationUpdates() falls LocationUpdates aktiv sein sollte und GoogleApiClient reconnected
    }

    protected void startLocationUpdates() {
        Log.i(TAG, "startLocationUpdates()");
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "onConnectionSuspended()");
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "onConnectionFailed()");
        /* Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        onConnectionFailed.
        -> Wir tun nix */
    }

}
