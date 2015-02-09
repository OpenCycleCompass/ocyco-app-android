package ibis.jufo.mytfg.de.ibis_intelligentbikeinformationsystem;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;


public class ShowDataActivity extends ActionBarActivity {

    boolean CollectData;
    boolean doNotRestart;
    float accuracy;

    long startTime = 0;

    String tAnkMinStr;


    // Log TAG
    protected static final String TAG = "IBisShowDataActivity-class";

    //create instance of GlobalVariables class
    GlobalVariables mGlobalVariable;

    //info boxes
    TextView sGefBox;
    TextView sZufBox;
    TextView vAktBox;
    TextView vDBox;
    TextView tAnkBox;
    TextView tAnkUntBox;
    TextView vDMussBox;
    TextView vDUntBox;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_data);

        //receiving intent
        Intent incomingIntent = getIntent();
        CollectData = incomingIntent.getBooleanExtra("Key", false);

        Intent incomingIntent2 = getIntent();
        accuracy = incomingIntent2.getFloatExtra("KeyAccuracy", 0);
        doNotRestart = incomingIntent2.getBooleanExtra("KeyDoNotRestart", false);
        int ErrOrConfirm = incomingIntent2.getIntExtra("KeyErrOrConfirm", 2);
        Log.i(TAG, ErrOrConfirm + "ErrOrConfirm");
        if (ErrOrConfirm == 0) {
            openAccuracyConfirm(accuracy);
        }
        if (ErrOrConfirm == 1) {
            openAccuracyAlert(accuracy);

        }
        // Start tracking Service, if Activity wasn't started from Tracking service
        if (!doNotRestart) {
            /*Intent intent = new Intent(this, Tracking.class);
            intent.putExtra("Key", CollectData);
            startService(intent); */
        }
        //initialize global variable class
        mGlobalVariable = (GlobalVariables) getApplicationContext();
        //info box text fields
        sGefBox = (TextView) findViewById(R.id.sGefBox);
        sZufBox = (TextView) findViewById(R.id.sZufBox);
        vAktBox = (TextView) findViewById(R.id.vAktBox);
        vDBox = (TextView) findViewById(R.id.vDBox);
        tAnkBox = (TextView) findViewById(R.id.tAnkBox);
        tAnkUntBox = (TextView) findViewById(R.id.tAnkUntBox);
        vDMussBox = (TextView) findViewById(R.id.vDMussBox);
        vDUntBox = (TextView) findViewById(R.id.vDUntBox);


        setTextSize();
        updateUI();
    }

    private void setTextSize() {
        float textSize = mGlobalVariable.getTextSize();
        Log.i(TAG, "setTextSize() " + textSize);
        sGefBox.setTextSize(0x00000003, textSize);
        sZufBox.setTextSize(0x00000003, textSize);
        vAktBox.setTextSize(0x00000003, textSize);
        vDBox.setTextSize(0x00000003, textSize);
        tAnkBox.setTextSize(0x00000003, textSize);
        tAnkUntBox.setTextSize(0x00000003, textSize);
        vDMussBox.setTextSize(0x00000003, textSize);
        vDUntBox.setTextSize(0x00000003, textSize);
    }


    private void openAccuracyAlert(Float accuracy) {
        Log.i(TAG, "Err");
        //set up a new alert dialog
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ShowDataActivity.this);
        alertDialogBuilder.setTitle("Positionsbestimmung zu ungenau!");
        alertDialogBuilder.setMessage(accuracy + "m Abweichung sind zu ungenau zum Navigieren! Haben Sie GPS aktiviert? Signal wird gesucht...");

        //create and show alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void openAccuracyConfirm(Float accuracy) {
        Log.i(TAG, "Confirm");
        //set up a new alert dialog
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ShowDataActivity.this);
        alertDialogBuilder.setTitle("Positionsbestimmung erfolgreich!");
        alertDialogBuilder.setMessage(accuracy + "m Abweichung ist akzeptabel für die Navigation, sie können nun beginnen!");
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

    //Timer for updating the info boxes
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            Log.i(TAG, "run()");
            if (accuracy < 20) {
                showData();
            }

            timerHandler.postDelayed(this, 500);
        }
    };

    public void updateUI() {
        Log.i(TAG, "updateUI()");
        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);
    }

    String roundDecimals(double d) {
        return String.format("%.2f", d);
    }

    //read data from interface and write to info boxes
    public void showData() {
        Log.i(TAG, "showData()");
        //get variables from global class and round
        String sGef = roundDecimals(mGlobalVariable.getsGef()) + " km";
        String sZuf = roundDecimals(mGlobalVariable.getsZuf()) + " km";
        String vAkt = roundDecimals(mGlobalVariable.getvAkt()) + " km/h";
        String vD = roundDecimals(mGlobalVariable.getvD()) + " km/h";
        //get the time and format it
        double tAnkD = mGlobalVariable.gettAnk();
        int tAnkStd = (int) tAnkD;
        int tAnkMin = (int) Math.round(((tAnkD - tAnkStd) * 60));
        tAnkMinStr = Integer.toString(tAnkMin);
        if (tAnkMin < 10) {
            tAnkMinStr = "0" + tAnkMin;
        }
        String tAnk = tAnkStd + ":" + tAnkMinStr + " Uhr";
        if (tAnkStd > 23) {
            int tAnkDays = tAnkStd / 24;
            tAnkStd = tAnkStd - tAnkDays * 24;
            tAnk = tAnkStd + ":" + tAnkMinStr + " Uhr" + System.getProperty("line.separator") + "in " + tAnkDays + " Tagen";
        }
        //get the time and format it
        double tAnkUntD = mGlobalVariable.gettAnkUnt();
        int tAnkUntStd = (int) tAnkUntD;
        int tAnkUntMin = (int) Math.round(((tAnkUntD - tAnkUntStd) * 60));
        String tAnkUnt = tAnkUntStd + "h " + tAnkUntMin + "min";
        String vDMuss = roundDecimals(mGlobalVariable.getvDMuss()) + " km/h";
        String vDunt = roundDecimals(mGlobalVariable.getvDunt()) + " km/h";
        //show in infoboxes
        sGefBox.setText(sGef + "");
        sZufBox.setText(sZuf + "");
        vAktBox.setText(vAkt + "");
        vDBox.setText(vD + "");
        tAnkBox.setText(tAnk + "");
        tAnkUntBox.setText(tAnkUnt + "");
        vDMussBox.setText(vDMuss + "");
        vDUntBox.setText(vDunt + "");
        //set color
        if (mGlobalVariable.gettAnkUnt() < 0) {
            tAnkUntBox.setTextColor(getResources().getColor(R.color.good_value));
        } else if (mGlobalVariable.gettAnkUnt() > 0) {
            tAnkUntBox.setTextColor(getResources().getColor(R.color.bad_value));
        }
        if (mGlobalVariable.getvDunt() < 0) {
            vDUntBox.setTextColor(getResources().getColor(R.color.good_value));
        } else if (mGlobalVariable.getvDunt() > 0) {
            vDUntBox.setTextColor(getResources().getColor(R.color.bad_value));
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_show_data, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}