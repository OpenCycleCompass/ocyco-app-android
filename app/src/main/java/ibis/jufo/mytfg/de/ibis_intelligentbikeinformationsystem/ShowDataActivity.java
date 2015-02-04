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


    // Log TAG
    protected static final String TAG = "IBisShowDataActivity-class";

    //create instance of GlobalVariables class
    GlobalVariables mGlobalVariable;


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

        updateUI();
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
        String x = String.format("%.2f", d);
        Log.i(TAG, "roundDecimals(double d)" + x);
        return x;
    }

    //read data from interface and write to info boxes
    public void showData() {
        Log.i(TAG, "showData()");
        //get variables from global class and round
        String sGef = roundDecimals(mGlobalVariable.getsGef());
        String sZuf = roundDecimals(mGlobalVariable.getsZuf());
        Log.i(TAG, "vAkt----------- " + mGlobalVariable.getvAkt());
        //double temp = mGlobalVariable.getvAkt();
        //String vAkt = roundDecimals(temp);
        String vAkt = roundDecimals(mGlobalVariable.getvAkt());
        String vD = roundDecimals(mGlobalVariable.getvD());
        String tAnk = roundDecimals(mGlobalVariable.gettAnk());
        String tAnkUnt = roundDecimals(mGlobalVariable.gettAnkUnt());
        String vDMuss = roundDecimals(mGlobalVariable.getvDMuss());
        String vDunt = roundDecimals(mGlobalVariable.getvDunt());
        //show in infoboxes
        TextView sGefBox = (TextView) findViewById(R.id.sGefBox);
        sGefBox.setText(sGef + "");
        TextView sZufBox = (TextView) findViewById(R.id.sZufBox);
        sZufBox.setText(sZuf + "");
        TextView vAktBox = (TextView) findViewById(R.id.vAktBox);
        vAktBox.setText(vAkt + "");
        TextView vDBox = (TextView) findViewById(R.id.vDBox);
        vDBox.setText(vD + "");
        TextView tAnkBox = (TextView) findViewById(R.id.tAnkBox);
        tAnkBox.setText(tAnk + "");
        TextView tAnkUntBox = (TextView) findViewById(R.id.tAnkUntBox);
        tAnkUntBox.setText(tAnkUnt + "");
        TextView vDMussBox = (TextView) findViewById(R.id.vDMussBox);
        vDMussBox.setText(vDMuss + "");
        TextView vDUntBox = (TextView) findViewById(R.id.vDUntBox);
        vDUntBox.setText(vDunt + "");
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