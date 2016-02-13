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

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.bonuspack.overlays.Polyline;
import org.osmdroid.tileprovider.IRegisterReceiver;
import org.osmdroid.tileprovider.MapTileProviderArray;
import org.osmdroid.tileprovider.modules.MapTileDownloader;
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

import java.util.ArrayList;

import de.mytfg.jufo.ibis.storage.IbisTrack;

public class MapFragment extends Fragment {

    // Log TAG
    protected static final String TAG = "IBis-MapFragment";

    Polyline oldDrivenPolyline;

    //map view and overlays
    private MapView mMapView;

    private boolean mapVisible = false;

    @Override
    public void onResume() {
        super.onResume();
        mapVisible = true;
        Log.i(TAG, "onResume()");
        // call timerRunnable.run() frequently
        timerHandler.post(timerRunnable);
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause()");
        // stop calling timerRunnable.run() frequently
        mapVisible = false;
        super.onPause();
    }

    //Timer for updating the map
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if(mapVisible) {
                updateMap();
                timerHandler.postDelayed(this, 500);
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //context
        final Context context_activity = getActivity();
        //register receiver
        final IRegisterReceiver mRegisterReceiver = new SimpleRegisterReceiver(context_activity.getApplicationContext());
        //tile server url
        final String[] url = new String[]{"http://tile.thunderforest.com/cycle/"};
        //tile source
        final ITileSource mTileSource = new XYTileSource("cyclemap", ResourceProxy.string.cyclemap, 1, 18, 256, ".png", url);
        //file cache provider
        final TileWriter mTileWriter = new TileWriter();
        final MapTileFilesystemProvider fileSystemProvider = new MapTileFilesystemProvider(mRegisterReceiver, mTileSource);
        //download modular tile provider
        final NetworkAvailabliltyCheck mNetworkAvailablityCheck = new NetworkAvailabliltyCheck(context_activity);
        final MapTileDownloader downloaderProvider = new MapTileDownloader(mTileSource, mTileWriter, mNetworkAvailablityCheck);
        //create the tile provider array
        final MapTileProviderArray mMapTileProviderArray = new MapTileProviderArray(mTileSource, mRegisterReceiver, new MapTileModuleProviderBase[]{fileSystemProvider, downloaderProvider});
        //create map
        mMapView = new MapView(context_activity, 256, new DefaultResourceProxyImpl(context_activity), mMapTileProviderArray);
        //and return it
        return mMapView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onActivityCreated()");
        super.onActivityCreated(savedInstanceState);

        final Context context = this.getActivity();
        final DisplayMetrics dm = context.getResources().getDisplayMetrics();
        //set zoom and touch controls
        mMapView.setBuiltInZoomControls(true);
        mMapView.setMultiTouchControls(true);
        //crate and enable CompassOverlay
        CompassOverlay mCompassOverlay = new CompassOverlay(context, new InternalCompassOrientationProvider(context), mMapView);
        mCompassOverlay.enableCompass();
        //crate and enable MyLocationOverlay
        MyLocationNewOverlay mLocationOverlay = new MyLocationNewOverlay(context, new GpsMyLocationProvider(context), mMapView);
        mLocationOverlay.enableMyLocation();
        //crate and enable ScaleBarOverlay
        ScaleBarOverlay mScaleBarOverlay = new ScaleBarOverlay(context);
        mScaleBarOverlay.setCentred(true);
        mScaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, 10);
        //check, if settings were changed by user, else activate overlays by default
        if (!IbisApplication.isChanged_settings()) {
            IbisApplication.setShowLocationOverlay(true);
            IbisApplication.setShowCompassOverlay(true);
            IbisApplication.setShowScaleBarOverlay(true);
        }
        //add overlays
        if (IbisApplication.isShow_locationOverlay()) {
            mMapView.getOverlays().add(mLocationOverlay);
        }
        if (IbisApplication.isShow_compassOverlay()) {
            mMapView.getOverlays().add(mCompassOverlay);
        }
        if (IbisApplication.isShow_scaleBarOverlay()) {
            mMapView.getOverlays().add(mScaleBarOverlay);
        }
        mMapView.getController().setZoom(18);
        // add half-transparent blue polyline
        mMapView.getOverlays().add(this.createPolylineFromDB(IbisApplication.mRDB, 0x880040FF));
        startMapUpdates();
    }

    private Polyline createPolylineFromDB(IbisTrack lTDB, int color) {
        Log.i(TAG, "createDrivenPolyline()");
        //create waypoints Array
        Log.i(TAG, "database size: " + lTDB.metaData.getNumberOfLocations() + " rows");
        Polyline polyline = new Polyline(getActivity().getApplicationContext());
        polyline.setColor(color);
        ArrayList<GeoPoint> waypoints = lTDB.getGeoPointArrayList();
        if (waypoints != null) {
            Log.i(TAG, "waypoints.size()=" + waypoints.size());
            //create and set up the polyline
            polyline.setPoints(waypoints);
        }
        // return empty Polyline instead of null if db is empty
        return polyline;
    }

    private void startMapUpdates() {
        timerHandler.post(timerRunnable);
    }

    private void updateMap() {
        Log.i(TAG, "updateMap() (only if map is visible)");
        //center at users position,rotate map
        if (IbisApplication.isAutoCenter()) {
            if(IbisApplication.getLocation() != null) {
                GeoPoint currentLocation = new GeoPoint(IbisApplication.getLocation().getLatitude(), IbisApplication.getLocation().getLongitude());
                Log.i(TAG, "Geopoint" + currentLocation);
                mMapView.getController().setCenter(currentLocation);
                // create polyline for driven route
                Polyline drivenPolyline = this.createPolylineFromDB(IbisApplication.mGPSDB, 0x88E77E00); // half-transparent orange
                // add polyline
                mMapView.getOverlays().add(drivenPolyline);
                // delete old polyline
                if (oldDrivenPolyline!=null){
                    mMapView.getOverlays().remove(oldDrivenPolyline);
                }
                oldDrivenPolyline=drivenPolyline;
            }
            else {
                Log.i(TAG, "Location is null");
            }
        }
        if ((IbisApplication.isAuto_rotate()) && (IbisApplication.getLocation().getSpeed() > 1)) {
            mMapView.setMapOrientation(360.0f - (IbisApplication.getLocation().getBearing()));
        } else if (IbisApplication.isAlign_north()) {
            mMapView.setMapOrientation(360.0f);
        }
    }
}