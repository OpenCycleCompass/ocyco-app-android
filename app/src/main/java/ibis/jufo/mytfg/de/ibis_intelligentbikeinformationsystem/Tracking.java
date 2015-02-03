/**
 * Created by raphael on 28.12.14.
 * iBis Tracking Service
 * receives location updates, save to SQLite.
 */
package ibis.jufo.mytfg.de.ibis_intelligentbikeinformationsystem;

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

    // Log TAG
    protected static final String TAG = "IBisTracking-class";

    //Variables declaration
    public boolean CollectData;

    // The desired interval for location updates. Inexact. Updates may be more or less frequent.
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    // The fastest rate for active location updates. Exact. Updates will never be more frequent than this value.
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // Provides the entry point to Google Play services.
    protected GoogleApiClient mGoogleApiClient;

    //Stores parameters for requests to the FusedLocationProviderApi.
    protected LocationRequest mLocationRequest;

    //Represents a geographical location.
    protected Location mCurrentLocation;

    protected String mLastUpdateTime;

    public GPSDatabase mGPSDb;

    boolean sendErrorAccuracy = false;
    boolean sendConfirmAccuracy = false;
    boolean saveData = true;

    // Notification
    // Sets an ID for the notification
    private int mNotificationId = 42;
    private NotificationCompat.Builder mBuilder;
    // Gets an instance of the NotificationManager service
    private NotificationManager mNotifyMgr;

    //create a new instance of classes
    Calculate mCalculate = new Calculate();
    GlobalVariables mGlobalVariable;


    @Override
    //Very mystical code...
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void checkOnline() {
        Log.i(TAG, "checkOnline()");
        if (CollectData) {
            startOnlineTracking();
        } else {
            stopOnlineTracking();
        }
    }

    public void startOnlineTracking() {
        Log.i(TAG, "startOnlineTracking()");
        // Create Notification with track info
        mBuilder.setContentText(getString(R.string.tracking_status_active));
        // Builds the notification and issues it.
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }

    public void stopOnlineTracking() {
        //TODO: stop uploading track data

        Log.i(TAG, "stopOnlineTracking()");

        //cancel notification
        // Remove Notification
        int mNotificationId = 42;
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(mNotificationId);
    }

    public void stopLocationUpdates() {
        Log.i(TAG, "stopTracking()");
        // Stop LocationListener
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);

    }

    protected void startLocationUpdates() {
        Log.i(TAG, "startLocationUpdates()");
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * LocationServices API.
     */
    protected void buildGoogleApiClient() {
        Log.i(TAG, "buildGoogleApiClient()");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        createLocationRequest();
    }

    protected void createLocationRequest() {
        Log.i(TAG, "createLocationRequest()");
        mLocationRequest = new LocationRequest();
        // Positionsbestimmung mindestens ca. alle 5 Sekunden (5000ms)
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        // Positionsbestimmung höchstens jede Sekunde (1000ms)
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        // Hohe Genauigkeit
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onLocationChanged(Location location) {

        // Nur zum testen
        if (!CollectData) {
            stopSelf();
        }
        Log.i(TAG, "onLocationChanged()");
        mCurrentLocation = location;
        mLastUpdateTime = Long.toString(System.currentTimeMillis() / 1000L);
        checkAccuracy(location.getAccuracy());
        //only save data, if accuracy is ok
        Log.i(TAG, saveData + "saveData");
        if (saveData) {
            updateDatabase();
        }
        mGPSDb.open();
        int num_rows = mGPSDb.getNumRows();
        mGPSDb.close();
        // Update notification
        mBuilder.setContentText(getString(R.string.tracking_status_active)+ " - " + num_rows + " GPS Punkte aufgezeichnet");
        // Sets an ID for the notification
        int mNotificationId = 42;
        // Builds the notification and issues it.
        mNotifyMgr.notify(mNotificationId, mBuilder.build());

        callCalculate();
    }

    public void callCalculate() {
        Log.i(TAG, "callCalculate()");
        mCalculate.getData(mCurrentLocation, mGlobalVariable.getsEing());
        //only call mathematical methods, if this is not the first location - else there will be a NPE
        if (!mCalculate.checkFirstLoc()) {
            mCalculate.calculateTimeVars(mGlobalVariable.gettAnkEingTime());
            mCalculate.calculateDrivenDistance();
            mCalculate.calculateDrivenTime();
            mCalculate.calculateSpeed();
            mCalculate.math();
            //get Variables from calculation
            double sGef = mCalculate.getsGef();
            double sZuf = mCalculate.getsZuf();
            double vAkt = mCalculate.getvAkt();
            Log.i(TAG, "vAktCallCalc "+vAkt);
            double vD = mCalculate.getvD();
            double tAnk = mCalculate.gettAnk();
            double tAnkUnt = mCalculate.gettAnkUnt();
            double vDMuss = mCalculate.getvDMuss();
            double vDunt = mCalculate.getvDunt();
            //write Variables to global class
            mGlobalVariable.setCalculationVars(sGef, sZuf, vAkt, vD, tAnk, tAnkUnt, vDMuss, vDunt);

        }

    }

    public void checkAccuracy(Float accuracy) {
        Log.i(TAG, "checkAccuracy " + accuracy);
        Log.i(TAG, accuracy + " Accuracy");
        if (accuracy > 20 && !sendErrorAccuracy) {
            saveData = false;
            Intent intent = new Intent(this, ShowDataActivity.class);
            intent.putExtra("KeyAccuracy", accuracy);
            intent.putExtra("KeyDoNotRestart", true);
            intent.putExtra("KeyErrOrConfirm", 1);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            //sendErrorAccuracy ist to avoid, that ShowDataActivity is recreated at every Location update
            sendErrorAccuracy = true;
        } else if (accuracy < 20 && !sendConfirmAccuracy) {
            Log.i(TAG, saveData + "saveData && blabla");
            saveData = true;
            Intent intent = new Intent(this, ShowDataActivity.class);
            intent.putExtra("KeyAccuracy", accuracy);
            intent.putExtra("KeyDoNotRestart", true);
            intent.putExtra("KeyErrOrConfirm", 0);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            //sendConfirmAccuracy ist to avoid, that ShowDataActivity is recreated at every Location update
            sendConfirmAccuracy = true;

        }
    }


    public void updateDatabase() {
        Log.i(TAG, "updateDatabase()");
        //Convert to String for Database
        String lat = mCurrentLocation.getLatitude() + "";
        String lon = mCurrentLocation.getLongitude() + "";
        String alt = mCurrentLocation.getAltitude() + "";
        String spe = mCurrentLocation.getSpeed() + "";
        String tst = mLastUpdateTime + "";
        mGPSDb.open();
        mGPSDb.insertRows(lat, lon, alt, spe, tst);
        mGPSDb.close();
        //write position to GlobalVariables class
        mGlobalVariable.setLocation(mCurrentLocation);
    }


    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate()");
        super.onCreate();

        // Create Notification
        Intent tracking_showIntent = new Intent(this, ShowDataActivity.class);
        PendingIntent tracking_showPendingIntent = PendingIntent.getActivity(this, 0, tracking_showIntent, 0);

        Intent tracking_stopIntent = new Intent(this, SettingsActivity.class);
        tracking_stopIntent.putExtra("callMethod", "stopOnlineTracking");
        // TODO SettingsActivity should call stopOnlineTracking() in onCreate if intent.getExtra("callMethod") is "stopOnlineTracking"
        PendingIntent tracking_stopPendingIntent = PendingIntent.getActivity(this, 0, tracking_stopIntent, 0);

        mBuilder = new NotificationCompat.Builder(this);

        mBuilder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentIntent(tracking_showPendingIntent) // default action (sole action on
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

        //create Database
        mGPSDb = new GPSDatabase(this.getApplicationContext());
        //initialize global variable class
        mGlobalVariable = (GlobalVariables) getApplicationContext();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand()");
        //read extra
        try {
            CollectData = intent.getBooleanExtra("Key", false);
        } catch (java.lang.NullPointerException e) {
            stopSelf();
        }

        checkOnline();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        // Save Data: sendToServer()
        if (true) {
            mGPSDb.open();
            // Start Intent returned by mGPSDb.sendToServer()
            // intent has track data as "Extra"
            Intent intent = mGPSDb.sendToServer(this);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            mGPSDb.close();
        }
        stopLocationUpdates();
        mGPSDb.deleteDatabase();
        mGoogleApiClient.disconnect();

        // remove tracking notification
        mNotifyMgr.cancel(mNotificationId);

        super.onDestroy();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "onConnected()");
        startLocationUpdates();
        // Evtl startLocationUpdates() falls LocationUpdates aktiv sein sollte und GoogleApiClient reconnected
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
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        // -> Wir tun nix
    }

}
