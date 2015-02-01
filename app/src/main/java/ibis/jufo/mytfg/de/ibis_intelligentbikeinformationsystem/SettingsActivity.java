package ibis.jufo.mytfg.de.ibis_intelligentbikeinformationsystem;

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

public class SettingsActivity extends ActionBarActivity implements TimePickerFragment.OnTimePickedListener {

    //Variables declaration
    public boolean CollectData = false;
    public float FloatDistStartDest;
    double tAnkEingTime;

    //create instance of GlobalVariables class
    GlobalVariables mGlobalVariable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Restore preferences
        SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        //get variables
        CollectData = settings.getBoolean("CollectData", false);
        FloatDistStartDest = settings.getFloat("FloatDistStartDest", 0);
        //setting check box
        final CheckBox checkBox = (CheckBox) findViewById(R.id.CBCollectData);
        checkBox.setChecked(CollectData);
        //set Text to enter_distance
        EditText editDistance = (EditText) findViewById(R.id.enter_distance);
        editDistance.setText(Float.toString(FloatDistStartDest));

        //initialize global variable class
        mGlobalVariable = (GlobalVariables) getApplicationContext();

        // call stopOnlineTracking() if SettingsActivity has benn started
        // from notification action "Tracking Beenden"
        Bundle bundle = getIntent().getExtras();
        if(bundle != null) {
            if (bundle.getString("callMethod") == "stopOnlineTracking") {
                stopOnlineTracking();
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    protected void onStop(){
        super.onStop();

        //saving settings
        //ATTENTION! onStop() is executed AFTER onCreate(), with onStop() saved data
        //can NOT be read by the next Activity's onCreate()!!!
        SharedPreferences settings = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        //creating a editor
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("CollectData", CollectData);
        editor.putFloat("FloatDistStartDest", FloatDistStartDest);
        // Commit the edits!
        editor.apply();
    }

    public void onCheckboxClicked(View view) {
        // Check if the CheckBox is checked
        boolean checked = ((CheckBox) view).isChecked();

        // Check which checkbox was clicked
        switch (view.getId()) {
            case R.id.CBCollectData:
                CollectData = checked;
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
        double sEing = (double) FloatDistStartDest;
        mGlobalVariable.setSettingVars(tAnkEingTime, sEing);
        if (!exception) {
            //restart Tracking Service starts it's onStartCommand (NOT onCreate),
            //so checkOnline will be executed again
            Intent intent = new Intent(this, Tracking.class);
            intent.putExtra("Key", CollectData);
            startService(intent);

            //start ShowDataActivity
            Intent intent2 = new Intent(this, ShowDataActivity.class);
            intent2.putExtra("Key", CollectData);
            startActivity(intent2);
        }
    }

    private void openAlert(String StrEditText) {
        //set up a new alert dialog
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(SettingsActivity.this);
        alertDialogBuilder.setTitle("Bitte geben sie eine Zahl ein!");
        alertDialogBuilder.setMessage("\""+StrEditText+"\""+" ist keine Zahl! ");

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
        stopOnlineTracking();
    }

    public void stopOnlineTracking() {
        //set collect data false
        CollectData = false;
        final CheckBox checkBox = (CheckBox) findViewById(R.id.CBCollectData);
        checkBox.setChecked(CollectData);
        //restart Tracking Service starts it's onStartCommand (NOT onCreate),
        //so checkOnline will be executed again
        Intent intent = new Intent(this, Tracking.class);
        intent.putExtra("Key", CollectData);
        startService(intent);
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
        arrivalTime.setText(hour + ":" + minute + " Uhr");
        convertToMilliseconds(hour, minute);
    }

    //convert hour and minutes to milliseconds for mathematical operations @Calculation
    public void convertToMilliseconds(int hour, int minute) {
        tAnkEingTime = (double) ((hour * 60 + minute) * 60 * 1000);
    }
}

