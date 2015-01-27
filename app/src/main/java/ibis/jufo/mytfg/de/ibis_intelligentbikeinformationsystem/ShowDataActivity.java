package ibis.jufo.mytfg.de.ibis_intelligentbikeinformationsystem;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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


    // Log TAG
    protected static final String TAG = "IBisShowDataActivity-class";


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
        showData();
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

    //read data from interface and write to info boxes
    public void showData() {
        //get variables from global class
        final GlobalVariables mGlobalVariable = (GlobalVariables) getApplicationContext();
        double sGef = mGlobalVariable.getsGef();
        double sZuf = mGlobalVariable.getsZuf();
        double vAkt = mGlobalVariable.getvAkt();
        double vD = mGlobalVariable.getvD();
        double tAnk = mGlobalVariable.gettAnk();
        double tAnkUnt = mGlobalVariable.gettAnkUnt();
        double vDMuss = mGlobalVariable.getvDMuss();
        double vDunt = mGlobalVariable.getvDunt();

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