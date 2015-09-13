package de.mytfg.jufo.ibis;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    //Timer for updating the map
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            enableButtons();
            timerHandler.postDelayed(this, 500);
        }
    };
    //views
    private Button stop_tracking_button;
    private Button start_tracking_button;

    public void openSettings(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void onClickFindRoute2(View view) {
        Intent intent = new Intent(this, RoutingActivity.class);
        startActivity(intent);
    }

    public void onClickStartTracking(View view) {
        if (IbisApplication.isCollect_data()) {
            enableButtons();
            Intent intent = new Intent(this, Tracking.class);
            startService(intent);
        }
        else {
            openAlert();
        }
    }

    private void openAlert() {
        //set up a new alert dialog
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setTitle("Einstellungen ändern!");
        alertDialogBuilder.setMessage("Bitte stimmen Sie in den Einstellungen dem Sammeln von Nutzerdaten zu!");

        //create the OK Button and onClickListener
        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            //close dialog when clicked
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        //create and show alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void enableButtons() {
        //enable buttons - status of trackingRunning is not even changed,
        //when this statement is executed!
        start_tracking_button.setEnabled(!IbisApplication.isOnline_tracking_running());
        stop_tracking_button.setEnabled(IbisApplication.isOnline_tracking_running());
    }

    public void onClickStopTracking(View view) {
        enableButtons();
        Intent intent = new Intent(this, Tracking.class);
        intent.putExtra("stopOnlineTracking", true);
        startService(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // initialize
        // Restore preferences
        SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        //get variables and set to global class
        IbisApplication.setCollect_data(settings.getBoolean("CollectData", false));
        start_tracking_button = (Button) findViewById(R.id.button_start_tracking);
        stop_tracking_button = (Button) findViewById(R.id.button_stop_tracking);
        startUIUpdates();
    }

    public void startUIUpdates() {
        timerHandler.postDelayed(timerRunnable, 0);
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
            case R.id.action_info:
                Intent intent_info = new Intent(this, InfoActivity.class);
                startActivity(intent_info);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}