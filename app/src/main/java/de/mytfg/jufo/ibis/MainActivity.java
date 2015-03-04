package de.mytfg.jufo.ibis;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class MainActivity extends ActionBarActivity {

    //views
    Button stop_tracking_button;
    Button start_tracking_button;

    GlobalVariables mGlobalVars;

    public void openSettings(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void onClickFindRoute2(View view) {
        Intent intent = new Intent(this, RoutingActivity.class);
        startActivity(intent);
    }

    public void onClickStartTracking(View view) {
        if (!mGlobalVars.isTrackingRunning()) {
            mGlobalVars.setCollectData(true);
            enableButtons();
            Intent intent = new Intent(this, Tracking.class);
            startService(intent);
        }
    }

    //Timer for updating the map
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            enableButtons();
            timerHandler.postDelayed(this, 500);
        }
    };

    public void startUIUpdates() {
        timerHandler.postDelayed(timerRunnable, 0);
    }

    public void onClickStopTracking(View view) {
        if (mGlobalVars.isTrackingRunning()) {
            mGlobalVars.setCollectData(false);
            enableButtons();
            Intent intent = new Intent(this, Tracking.class);
            startService(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // initialize
        mGlobalVars = (GlobalVariables) getApplicationContext();
        start_tracking_button = (Button) findViewById(R.id.button_start_tracking);
        stop_tracking_button = (Button) findViewById(R.id.button_stop_tracking);
        startUIUpdates();
    }

    public void enableButtons() {
        //enable buttons - status of trackingRunning is not even changed,
        //when this statement is executed!
        start_tracking_button.setEnabled(!mGlobalVars.isTrackingRunning());
        stop_tracking_button.setEnabled(mGlobalVars.isTrackingRunning());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent_settings = new Intent(this, SettingsActivity.class);
                startActivity(intent_settings);
                return true;
            case R.id.action_show_data_activity:
                Intent intent_showData = new Intent(this, ShowDataActivity.class);
                startActivity(intent_showData);
                return true;
            case R.id.action_RoutingActivity:
                Intent intent_routing = new Intent(this, RoutingActivity.class);
                startActivity(intent_routing);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}