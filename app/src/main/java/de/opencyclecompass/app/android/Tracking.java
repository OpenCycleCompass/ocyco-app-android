package de.opencyclecompass.app.android;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import de.opencyclecompass.app.android.storage.OcycoTrack;
import de.opencyclecompass.app.android.util.Utils;

public class Tracking extends Service implements LocationListener, OnConnectionFailedListener, ConnectionCallbacks {

    // The desired interval for location updates. Inexact. Updates may be more or less frequent.
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 3000;
    // The fastest rate for active location updates. Exact. Updates will never be more frequent than this value.
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
    // Log TAG
    protected static final String TAG = "Tracking";
    // Provides the entry point to Google Play services.
    protected GoogleApiClient mGoogleApiClient;
    //Stores parameters for requests to the FusedLocationProviderApi.
    protected LocationRequest mLocationRequest;
    //location vars
    protected Location mCurrentLocation;
    //create a new instance of classes
    Calculate mCalculate = new Calculate();
    private boolean saveData = true;
    private String accNotiStr;
    private NotificationCompat.Builder mBuilder;
    // Gets an instance of the NotificationManager service
    private NotificationManager mNotifyMgr;

    private OcycoTrack track;

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "onLocationChanged()");
        mCurrentLocation = location;
        //write position to OcycoApplication class
        OcycoApplication.setLocation(mCurrentLocation);
        //only save data, if accuracy is ok
        if (checkAccuracy(location.getAccuracy())) {
            updateDatabase();
            //update Notification
            int num_rows = track.metadata.getNumberOfLocations();
            double total_dist = track.metadata.getTotalDistance();
            String s_total_dist = Utils.roundDecimals(total_dist / 1000d);
            // Update notification, if online tracking ist running
            if (OcycoApplication.isOnline_tracking_running()) {
                mBuilder.setContentText(accNotiStr + getString(R.string.tracking_status_active) + " - " + num_rows + getString(R.string.coordinates) + s_total_dist + " " + getString(R.string.km));
                // Sets an ID for the notification
                int mNotificationId = 42;
                // Builds the notification and issues it.
                mNotifyMgr.notify(mNotificationId, mBuilder.build());
            }
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
        track.appendLocation(mCurrentLocation);
    }

    private void callCalculate() {
        Log.i(TAG, "callCalculate()");
        mCalculate.getData(mCurrentLocation, OcycoApplication.getsEing());
        //only call mathematical methods, if this is not the first location - else there will be a NPE
        if (!mCalculate.checkFirstLoc()) {
            mCalculate.calculateTimeVars(OcycoApplication.gettAnkEingTime());
            mCalculate.calculateDrivenDistance(track.metadata.getTotalDistance());
            Log.i(TAG, "sEing tf callCalc " + (track.metadata.getTotalDistance()));
            mCalculate.calculateDrivenTime();
            mCalculate.calculateSpeed();
            mCalculate.math(OcycoApplication.isUseTimeFactor(), OcycoApplication.getsEingTimeFactor() / 1000d);
            Log.i(TAG, "sEing tf callCalc " + (OcycoApplication.getsEingTimeFactor() / 1000d));
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
            OcycoApplication.setCalculationVars(sGef, sZuf, vAkt, vD, tAnk, tAnkUnt, vDMuss, vDunt);

        }

    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate()");
        super.onCreate();
        // Delete old track by overwriting it with a new one
        track = new OcycoTrack();

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
    }

    /**
     * Builds a {@link GoogleApiClient}.
     * Uses the {@link GoogleApiClient.Builder#addApi(com.google.android.gms.common.api.Api)} method to request
     * the {@link LocationServices} API.
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
        //read Extra data from intent, if exists
        try {
            if (intent.hasExtra("stopOnlineTracking")) {
                if (intent.getBooleanExtra("stopOnlineTracking", true)) {
                    stopOnlineTracking();
                }
            } else if (!OcycoApplication.isOnline_tracking_running() && OcycoApplication.isCollect_data()) {
                startOnlineTracking();
            }
        }
        catch (Exception e) {
            int mNotificationId = 42;
            NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mNotifyMgr.cancel(mNotificationId);
            stopSelf();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void stopOnlineTracking() {
        Log.i(TAG, "stopOnlineTracking()");
        //cancel notificationStopOnlineTracking
        int mNotificationId = 42;
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(mNotificationId);
        if ((track.metadata.getNumberOfLocations() < 10)
                || !track.removeRandomStartEnd()) {
            // don't upload empty or too short track
            Toast.makeText(this, getString(R.string.upload_track_error_too_short),
                    Toast.LENGTH_LONG).show();
            track = new OcycoTrack();
        }
        else {
            OcycoApplication.trackArchive.add(track);
            Intent intent = new Intent(this, UploadTrackActivity.class);
            intent.putExtra("track", track.metadata.getUuid().toString());
            intent.putExtra("fromArchive", false);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        OcycoApplication.setOnline_tracking_running(false);
        stopSelf();
    }

    private void startOnlineTracking() {
        Log.i(TAG, "startOnlineTracking()");
        OcycoApplication.setOnline_tracking_running(true);
        // Create Notification with track info
        mBuilder.setContentText(accNotiStr + getString(R.string.tracking_status_active));
        // Builds the notification and issues it.
        mNotifyMgr.notify(42, mBuilder.build());
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        disconnect();
        super.onDestroy();
    }

    private void disconnect () {
        if (OcycoApplication.isOnline_tracking_running()) {
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

        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } catch(SecurityException se) {
            // Missing permissions (e.g. PRIORITY_HIGH_ACCURACY for ACCESS_FINE_LOCATION)
            // TODO: Actually inform the user that the permissions prevent OCYCO from working properly
            Log.i(TAG, "startLocationUpdates(): SecurityException: " + se.getMessage());
        }
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
