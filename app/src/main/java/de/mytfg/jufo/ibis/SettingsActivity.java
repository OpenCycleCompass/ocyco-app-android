package de.mytfg.jufo.ibis;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Calendar;


public class SettingsActivity extends ActionBarActivity implements TimePickerFragment.OnTimePickedListener {

    //Variables declaration
    public float FloatDistStartDest;
    double tAnkEingTime;
    float FloatTextSize;

    //create instance of GlobalVariables class
    GlobalVariables mGlobalVariable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        //initialize global variable class
        mGlobalVariable = (GlobalVariables) getApplicationContext();

        // Restore preferences
        SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        //get variables and set to global class
        //check, if collectData was set by another activity, else read from preferences
        if (!mGlobalVariable.isCollectDataSet()) {
            mGlobalVariable.setCollectData(settings.getBoolean("CollectData", false));
        }
        mGlobalVariable.setShowLocationOverlay(settings.getBoolean("showLocationOverlay", true));
        mGlobalVariable.setShowCompassOverlay(settings.getBoolean("showCompassOverlay", true));
        mGlobalVariable.setShowScaleBarOverlay(settings.getBoolean("showScaleBarOverlay", true));
        FloatDistStartDest = settings.getFloat("FloatDistStartDest", 0);
        FloatTextSize = settings.getFloat("FloatTextSize", 8);
        //set check boxes
        final CheckBox CBcollectData = (CheckBox) findViewById(R.id.CBCollectData);
        CBcollectData.setChecked(mGlobalVariable.isCollectData());
        final CheckBox cb_show_compassOverlay = (CheckBox) findViewById(R.id.cb_show_compassOverlay);
        cb_show_compassOverlay.setChecked(mGlobalVariable.isShow_compassOverlay());
        final CheckBox cb_show_locationOverlay = (CheckBox) findViewById(R.id.cb_show_locationOverlay);
        cb_show_locationOverlay.setChecked(mGlobalVariable.isShow_locationOverlay());
        final CheckBox cb_show_scaleBarOverlay = (CheckBox) findViewById(R.id.cb_show_scaleBarOverlay);
        cb_show_scaleBarOverlay.setChecked(mGlobalVariable.isShow_scaleBarOverlay());
        //set default text
        EditText editDistance = (EditText) findViewById(R.id.enter_distance);
        editDistance.setText(Float.toString(FloatDistStartDest));
        EditText enter_text_size = (EditText) findViewById(R.id.enter_text_size);
        enter_text_size.setText(Float.toString(FloatTextSize));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();

        //saving settings
        SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        //creating a editor and add variables
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("CollectData", mGlobalVariable.isCollectData());
        editor.putBoolean("showCompassOverlay", mGlobalVariable.isShow_compassOverlay());
        editor.putBoolean("showLocationOverlay", mGlobalVariable.isShow_locationOverlay());
        editor.putBoolean("showScaleBarOverlay", mGlobalVariable.isShow_scaleBarOverlay());
        editor.putFloat("FloatDistStartDest", FloatDistStartDest);
        editor.putFloat("FloatTextSize", FloatTextSize);
        // Commit the edits!
        editor.apply();
    }

    public void onCheckboxClicked(View view) {
        //check if the CheckBox is checked
        boolean checked = ((CheckBox) view).isChecked();

        // Check which checkbox was clicked
        switch (view.getId()) {
            case R.id.CBCollectData:
                mGlobalVariable.setCollectData(checked);
                break;
            case R.id.cb_show_locationOverlay:
                mGlobalVariable.setShowLocationOverlay(checked);
                break;
            case R.id.cb_show_compassOverlay:
                mGlobalVariable.setShowCompassOverlay(checked);
                break;
            case R.id.cb_show_scaleBarOverlay:
                mGlobalVariable.setShowScaleBarOverlay(checked);
                break;
        }
    }

    //called when save Button is clicked
    public void saveSettings(View view) {
        boolean exception = false;
        //read text from EditText and convert to String
        EditText editDistance = (EditText) findViewById(R.id.enter_distance);
        String StrEditText = editDistance.getText().toString();
        //try to convert String to Float
        try {
            FloatDistStartDest = Float.parseFloat(StrEditText);
        } catch (java.lang.NumberFormatException e) {
            exception = true;
            openAlert(StrEditText);
        }
        EditText enter_text_size = (EditText) findViewById(R.id.enter_text_size);
        String strEnterTxtSz = enter_text_size.getText().toString();
        //try to convert String to Float
        try {
            FloatTextSize = Float.parseFloat(strEnterTxtSz);
        } catch (java.lang.NumberFormatException e) {
            exception = true;
            openAlert(strEnterTxtSz);
        }
        double sEing = (double) FloatDistStartDest;
        mGlobalVariable.setSettingVars(tAnkEingTime, sEing, FloatTextSize);
        if (!exception) {
            //restart Tracking Service starts it's onStartCommand (NOT onCreate),
            //so checkOnline will be executed again
            Intent intent = new Intent(this, Tracking.class);
            startService(intent);

            //start ShowDataActivity
            Intent intent2 = new Intent(this, ShowDataActivity.class);
            startActivity(intent2);
        }
    }

    private void openAlert(String StrEditText) {
        //set up a new alert dialog
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(SettingsActivity.this);
        alertDialogBuilder.setTitle("Bitte geben sie eine Zahl ein!");
        alertDialogBuilder.setMessage("\"" + StrEditText + "\"" + " ist keine Zahl! ");

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_show_data:
                Intent intent2 = new Intent(this, ShowDataActivity.class);
                startActivity(intent2);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onClickstopOnlineTracking(View view) {
        //set collect data false
        mGlobalVariable.setCollectData(false);
        final CheckBox checkBox = (CheckBox) findViewById(R.id.CBCollectData);
        checkBox.setChecked(mGlobalVariable.isCollectData());
        Intent stopOnlineTrackingIntent = new Intent(this, Tracking.class);
        startService(stopOnlineTrackingIntent);
    }

    //create and show the TimePickerFragment
    public void showTimePickerDialog(View v) {
        DialogFragment mTimePickerFragment = new TimePickerFragment();
        mTimePickerFragment.show(getSupportFragmentManager(), "timePicker");
    }

    //get picked time from TimePickerFragment via Interface
    public void onTimePicked(int hour, int minute) {
        //show picked time
        TextView arrivalTime = (TextView) findViewById(R.id.arrivalTime);
        if (minute < 10) {
            arrivalTime.setText(hour + ":0" + minute + " Uhr");
        } else {
            arrivalTime.setText(hour + ":" + minute + " Uhr");
        }
        convertToMilliseconds(hour, minute);

        final Calendar c = Calendar.getInstance();
        int current_hour = c.get(Calendar.HOUR_OF_DAY);
        int current_minute = c.get(Calendar.MINUTE);

        //if set time is before current time add a day in milliseconds to set time
        if ((hour<current_hour)||(current_hour==hour)&&(minute<current_minute)) {
            tAnkEingTime += 24*60*60*1000;
        }
    }

    //convert hour and minutes to milliseconds for mathematical operations @Calculation
    public void convertToMilliseconds(int hour, int minute) {
        tAnkEingTime = (double) ((hour * 60 + minute) * 60 * 1000);
    }
}

