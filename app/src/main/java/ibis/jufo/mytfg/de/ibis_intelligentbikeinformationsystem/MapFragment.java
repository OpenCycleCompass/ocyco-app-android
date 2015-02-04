package ibis.jufo.mytfg.de.ibis_intelligentbikeinformationsystem;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.osmdroid.ResourceProxy;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.ResourceProxyImpl;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class MapFragment extends Fragment {

    private MapView mMapView;
    private ResourceProxy mResourceProxy;
    private MyLocationNewOverlay mLocationOverlay;
    private CompassOverlay mCompassOverlay;
    private ScaleBarOverlay mScaleBarOverlay;

    // Log TAG
    protected static final String TAG = "IBis-MapFragment";

    GlobalVariables mGlobalVariables;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mResourceProxy = new ResourceProxyImpl(inflater.getContext().getApplicationContext());
        mMapView = new MapView(inflater.getContext(), 256, mResourceProxy);
        return mMapView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Context context = this.getActivity();
        final DisplayMetrics dm = context.getResources().getDisplayMetrics();

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
        //add overlays
        mMapView.getOverlays().add(this.mLocationOverlay);
        mMapView.getOverlays().add(this.mCompassOverlay);
        mMapView.getOverlays().add(this.mScaleBarOverlay);
        mMapView.getController().setZoom(18);
        startMapUpdates();

        //initialize global variable class
        mGlobalVariables = (GlobalVariables) getActivity().getApplicationContext();
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

    public void startMapUpdates() {
        Log.i(TAG, "startMapUpdates()");
        timerHandler.postDelayed(timerRunnable, 0);
    }

    public void updateMap() {
        Log.i(TAG, "updateMap()");
        //center at users position
        try {

            GeoPoint currentLocation = new GeoPoint(mGlobalVariables.getLocation().getLatitude(), mGlobalVariables.getLocation().getLongitude());
            mMapView.getController().setCenter(currentLocation);
        } catch (java.lang.NullPointerException e) {
            Log.i(TAG, "NullPointerException");
        }
    }
}