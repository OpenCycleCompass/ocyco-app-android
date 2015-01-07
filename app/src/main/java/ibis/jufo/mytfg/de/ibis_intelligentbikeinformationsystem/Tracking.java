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
import android.text.format.DateFormat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.Calendar;
import java.util.Locale;


public class Tracking extends Service implements LocationListener, OnConnectionFailedListener, ConnectionCallbacks {

    // Log TAG
    protected static final String TAG = "IBisTracking-class";

    //Variables declaration
    public boolean CollectData;

    //The desired interval for location updates. Inexact. Updates may be more or less frequent.
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    /* The fastest rate for active location updates. Exact. Updates will never be more frequent
    * than this value.
    */
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
        // TODO: funktioniert so nicht :(
        Intent tracking_showIntent = new Intent(this, ShowDataActivity.class);
        tracking_showIntent.putExtra("methodName", "showTrackInfo");
        PendingIntent tracking_showPendingIntent = PendingIntent.getActivity(this, 0, tracking_showIntent, 0);

        Intent tracking_stopIntent = new Intent(this, SettingsActivity.class);
        tracking_showIntent.putExtra("methodName", "stopTracking");
        PendingIntent tracking_stopPendingIntent = PendingIntent.getActivity(this, 0, tracking_stopIntent, 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(getString(R.string.tracking_status_active))
                        .addAction(R.drawable.ic_launcher, getString(R.string.tracking_stop), tracking_stopPendingIntent)
                        .addAction(R.drawable.ic_launcher, getString(R.string.tracking_show_tracking), tracking_showPendingIntent)
                        .setOngoing(true);
        // Sets an ID for the notification
        int mNotificationId = 42;
        // Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
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
        if(!CollectData){
            stopSelf();
        }

        Log.i(TAG, "onLocationChanged()");
        mCurrentLocation = location;
        mLastUpdateTime = Long.toString(System.currentTimeMillis()/1000L);
        updateDatabase();
    }

    public void updateDatabase(){
        Log.i(TAG, "updateDatabase()");
        //Convert to String for Database
        String lat = mCurrentLocation.getLatitude() + "";
        String lon = mCurrentLocation.getLongitude() + "";
        String alt = mCurrentLocation.getAltitude() + "";
        String tst = mLastUpdateTime + "";
        mGPSDb.open();
        mGPSDb.insertRows(lat, lon, alt, tst);
        mGPSDb.close();

    }


    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate()");
        super.onCreate();
        //build SQLite database

        //build and connect Api Client
        buildGoogleApiClient();
        mGoogleApiClient.connect();

        //create Database
        mGPSDb = new GPSDatabase(this.getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand()");
        //read extra and write to CollectData
        CollectData = intent.getBooleanExtra("Key", false);
        checkOnline();
        return super.onStartCommand(intent, flags, startId);
    }

    private String getDate(long timestamp) {
        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
        calendar.setTimeInMillis(timestamp*1000);
        return DateFormat.format("dd. MM. yyyy, HH:mm", calendar).toString()+"h";
    }
    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        // Save Data: sendToServer()
        if(true){
            mGPSDb.open();
            int returnCode = mGPSDb.sendToServer();
            String notification;
            Log.i(TAG, returnCode+"");
            switch (returnCode){
                case 1: // json has error
                    notification = mGPSDb.serverTrack_id;
                    break;
                case 2: // no known json field
                    notification = getString(R.string.httpJsonReturnNotificationNoField);
                    break;
                case 3: // http communication or json failed
                    notification = getString(R.string.httpJsonReturnNotificationErrTryCatchHttpJson);
                    break;
                case 0: // success
                    notification = "Track \""+mGPSDb.serverTrack_id + "\" mit " + mGPSDb.serverNodes + " GPS-Koordinaten erstellt am " + getDate(mGPSDb.serverCreated);
                    break;
                default:
                    notification = getString(R.string.unknownError);
                    break;
            }
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.ic_launcher)
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
            mGPSDb.close();
        }
        stopLocationUpdates();
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
