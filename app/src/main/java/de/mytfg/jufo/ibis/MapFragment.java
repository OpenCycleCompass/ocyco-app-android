package de.mytfg.jufo.ibis;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.bonuspack.overlays.Polyline;
import org.osmdroid.tileprovider.IRegisterReceiver;
import org.osmdroid.tileprovider.MapTileProviderArray;
import org.osmdroid.tileprovider.modules.GEMFFileArchive;
import org.osmdroid.tileprovider.modules.IArchiveFile;
import org.osmdroid.tileprovider.modules.MapTileDownloader;
import org.osmdroid.tileprovider.modules.MapTileFileArchiveProvider;
import org.osmdroid.tileprovider.modules.MapTileFilesystemProvider;
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;
import org.osmdroid.tileprovider.modules.NetworkAvailabliltyCheck;
import org.osmdroid.tileprovider.modules.TileWriter;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MapFragment extends Fragment {

    //map view and overlays
    private MapView mMapView;
    private MyLocationNewOverlay mLocationOverlay;
    private CompassOverlay mCompassOverlay;
    private ScaleBarOverlay mScaleBarOverlay;
    // Log TAG
    protected static final String TAG = "IBis-MapFragment";
    //global var class
    GlobalVariables mGlobalVariables;
    RoutingDatabase mRDB;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //context
        final Context context_activity = getActivity();
        //register receiver
        final IRegisterReceiver mRegisterReceiver = new SimpleRegisterReceiver(context_activity.getApplicationContext());
        //tile server url
        final String[] url = new String[]{"http://tile.thunderforest.com/cycle/"};
        //tile source
        final ITileSource mTileSource = new XYTileSource("cyclemap", ResourceProxy.string.cyclemap, 1, 18, 64, ".png", url);
        //file cache provider
        final TileWriter mTileWriter = new TileWriter();
        final MapTileFilesystemProvider fileSystemProvider = new MapTileFilesystemProvider(mRegisterReceiver, mTileSource);
        //archive file provider
        final File mArchiveFile = new File(new File(""), "archiveFile");
        GEMFFileArchive mGemfFileArchive = null;
        try {
            mGemfFileArchive = GEMFFileArchive.getGEMFFileArchive(mArchiveFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        MapTileFileArchiveProvider mFileArchiveProvider = new MapTileFileArchiveProvider(mRegisterReceiver, mTileSource, new IArchiveFile[]{mGemfFileArchive});
        //download modular tile provider
        final NetworkAvailabliltyCheck mNetworkAvailablityCheck = new NetworkAvailabliltyCheck(context_activity);
        final MapTileDownloader downloaderProvider = new MapTileDownloader(mTileSource, mTileWriter, mNetworkAvailablityCheck);
        //create the tile provider array
        final MapTileProviderArray mMapTileProviderArray = new MapTileProviderArray(mTileSource, mRegisterReceiver, new MapTileModuleProviderBase[]{fileSystemProvider, mFileArchiveProvider, downloaderProvider});
        //create map
        mMapView = new MapView(context_activity, 64, new DefaultResourceProxyImpl(context_activity), mMapTileProviderArray);
        //and return it
        return mMapView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onActivityCreated()");
        super.onActivityCreated(savedInstanceState);

        final Context context = this.getActivity();
        final DisplayMetrics dm = context.getResources().getDisplayMetrics();
        //initialize global variable class
        mGlobalVariables = (GlobalVariables) getActivity().getApplicationContext();
        //initialize RoutingDatabase
        mRDB = new RoutingDatabase(getActivity().getApplicationContext());
        //set zoom and touch controls
        mMapView.setBuiltInZoomControls(true);
        mMapView.setMultiTouchControls(true);
        //crate and enable CompassOverlay
        this.mCompassOverlay = new CompassOverlay(context, new InternalCompassOrientationProvider(context), mMapView);
        mCompassOverlay.enableCompass();
        //crate and enable MyLocationOverlay
        this.mLocationOverlay = new MyLocationNewOverlay(context, new GpsMyLocationProvider(context), mMapView);
        mLocationOverlay.enableMyLocation();
        //crate and enable ScaleBarOverlay
        mScaleBarOverlay = new ScaleBarOverlay(context);
        mScaleBarOverlay.setCentred(true);
        mScaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, 10);
        //check, if settings were changed by user, else activate overlays by default
        if (!mGlobalVariables.isChanged_settings()) {
            mGlobalVariables.setShowLocationOverlay(true);
            mGlobalVariables.setShowCompassOverlay(true);
            mGlobalVariables.setShowScaleBarOverlay(true);
        }
        //add overlays
        if (mGlobalVariables.isShow_locationOverlay()) {
            mMapView.getOverlays().add(this.mLocationOverlay);
        }
        if (mGlobalVariables.isShow_compassOverlay()) {
            mMapView.getOverlays().add(this.mCompassOverlay);
        }
        if (mGlobalVariables.isShow_scaleBarOverlay()) {
            mMapView.getOverlays().add(this.mScaleBarOverlay);
        }
        mMapView.getController().setZoom(18);
        mMapView.getOverlays().add(this.createPolyline());
        startMapUpdates();

    }

    private Polyline createPolyline() {
        Log.i(TAG, "createPolyline()");
        //create waypoints Array
        ArrayList<GeoPoint> waypoints = new ArrayList<>();
        //get waypoints
        mRDB.open();
        JSONArray allPoints = mRDB.getAllPoints();
        mRDB.close();
        try {
            for (int i = 0; i < allPoints.length(); i++) {
                JSONObject oneObject = allPoints.getJSONObject(i);
                // Pulling items from the array
                double lat = oneObject.getDouble("lat");
                double lon = oneObject.getDouble("lon");
                //adding to waypoints array
                waypoints.add(new GeoPoint(lat, lon));
            }
            Log.i(TAG, "for beendet");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //create and set up the polyline
        Polyline routeOverlay = new Polyline(getActivity().getApplicationContext());
        routeOverlay.setPoints(waypoints);
        routeOverlay.setColor(0x880040FF/*half-transparent blue*/);

        return routeOverlay;
    }


    //Timer for updating the map
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {

            updateMap();
            timerHandler.postDelayed(this, 500);
        }
    };

    private void startMapUpdates() {
        timerHandler.postDelayed(timerRunnable, 0);
    }

    private void updateMap() {
        Log.i(TAG, "updateMap()");
        //center at users position,rotate map
        try {
            if (mGlobalVariables.isAutoCenter()) {
                GeoPoint currentLocation = new GeoPoint(mGlobalVariables.getLocation().getLatitude(), mGlobalVariables.getLocation().getLongitude());
                Log.i(TAG, "Geopoint" + currentLocation);
                mMapView.getController().setCenter(currentLocation);
            }
            if ((mGlobalVariables.isAuto_rotate())&&(mGlobalVariables.getLocation().getSpeed() > 1)) {
                mMapView.setMapOrientation(360.0f - (mGlobalVariables.getLocation().getBearing()));
            }
            else if (mGlobalVariables.isAlign_north()) {
                mMapView.setMapOrientation(360.0f);
            }
        } catch (java.lang.NullPointerException e) {
            Log.i(TAG, "NullPointerException");
        }
    }
}