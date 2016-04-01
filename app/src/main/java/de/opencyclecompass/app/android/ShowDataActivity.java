package de.opencyclecompass.app.android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;

public class ShowDataActivity extends AppCompatActivity {

    // Log TAG
    protected static final String TAG = "ShowDataActivity";

    //alert dialog vars
    AlertDialog.Builder alertDialogBuilder;
    AlertDialog alertDialog;
    //Timer for updating the info boxes
    Handler timerHandler = new Handler();
    boolean noGPSAlertOpen;
    //info boxes
    private TextView sGefBox;
    private TextView sZufBox;
    private TextView vAktBox;
    private TextView vDBox;
    private TextView tAnkBox;
    private TextView tAnkUntBox;
    private TextView vDMussBox;
    private TextView vDUntBox;
    private Menu menu;
    private String tAnkMinStr;
    private boolean accuracyAlert, oldAccuracyAlert;
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            oldAccuracyAlert = accuracyAlert;
            if (OcycoApplication.getLocation() != null) {
                noGPSAlertOpen = false;
                if (OcycoApplication.getLocation().getAccuracy() < 20) {
                    showData();
                    accuracyAlert = false;
                } else {
                    accuracyAlert = true;
                }
                //check, if accuracy alert is necessary
                if (accuracyAlert != oldAccuracyAlert) {
                    //check which accuracy alert
                    if (accuracyAlert) {
                        openAccuracyAlert(false);
                    } else {
                        openAccuracyAlert(true);
                    }
                }
            } else {
                if (!noGPSAlertOpen) openAccuracyAlert(false);
                noGPSAlertOpen = true;
            }

            timerHandler.postDelayed(this, 500);
        }
    };
    private boolean dialogExists = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //show activity on lock screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        //set content view
        setContentView(R.layout.activity_show_data);
        //alert dialog for accuracy alerts
        alertDialogBuilder = new AlertDialog.Builder(ShowDataActivity.this);
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
        //start tracking
        Intent intent = new Intent(this, Tracking.class);
        startService(intent);
    }

    private void setTextSize() {
        float textSize = OcycoApplication.getTextSize();
        if (textSize != 0) {
            sGefBox.setTextSize(TypedValue.TYPE_STRING, textSize);
            sZufBox.setTextSize(TypedValue.TYPE_STRING, textSize);
            vAktBox.setTextSize(TypedValue.TYPE_STRING, textSize);
            vDBox.setTextSize(TypedValue.TYPE_STRING, textSize);
            tAnkBox.setTextSize(TypedValue.TYPE_STRING, textSize);
            tAnkUntBox.setTextSize(TypedValue.TYPE_STRING, textSize);
            vDMussBox.setTextSize(TypedValue.TYPE_STRING, textSize);
            vDUntBox.setTextSize(TypedValue.TYPE_STRING, textSize);
        }
    }

    private void updateUI() {
        Log.i(TAG, "updateUI()");
        timerHandler.postDelayed(timerRunnable, 0);
    }

    private void openAccuracyAlert(boolean confirm) {
        if (dialogExists) {
            alertDialog.dismiss();
            dialogExists = false;
        }
        if (confirm) {
            alertDialogBuilder.setTitle("Positionsbestimmung erfolgreich!");
            alertDialogBuilder.setMessage((int) OcycoApplication.getLocation().getAccuracy() + "m Abweichung ist akzeptabel für die Navigation, sie können nun beginnen!");
            //create the OK Button and onClickListener
            alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                //close dialog when clicked
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                    dialogExists = false;
                }
            });

        } else {
            alertDialogBuilder.setTitle("Positionsbestimmung zu ungenau!");
            if (OcycoApplication.getLocation() != null) {
                alertDialogBuilder.setMessage((int) OcycoApplication.getLocation().getAccuracy() + "m Abweichung sind zu ungenau zum Navigieren! Haben Sie GPS aktiviert? Signal wird gesucht...");
            } else {
                alertDialogBuilder.setMessage("Kein Signal! Haben Sie GPS aktiviert? Signal wird gesucht...");
            }
        }
        //create and show alert dialog
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
        dialogExists = true;
    }

    //read data from global var class and write to info boxes
    private void showData() {
        Log.i(TAG, "showData()");
        //get variables from global class and round
        String sGef = roundDecimals(OcycoApplication.getsGef()) + " km";
        String sZuf = roundDecimals(OcycoApplication.getsZuf()) + " km";
        String vAkt = roundDecimals(OcycoApplication.getvAkt()) + " km/h";
        String vD = roundDecimals(OcycoApplication.getvD()) + " km/h";
        int tAnkDays = 0;
        //get the time and format it (tAnk)
        double tAnkD = OcycoApplication.gettAnk();
        int tAnkStd = (int) tAnkD;
        int tAnkMin = (int) Math.round(((tAnkD - tAnkStd) * 60));
        tAnkMinStr = Integer.toString(tAnkMin);
        if (tAnkMin < 10) {
            tAnkMinStr = "0" + tAnkMin;
        }
        String tAnk = tAnkStd + ":" + tAnkMinStr + " Uhr";
        if (tAnkStd > 23) {
            tAnkDays = tAnkStd / 24;
            tAnkStd = tAnkStd - tAnkDays * 24;
            tAnk = tAnkStd + ":" + tAnkMinStr + " Uhr" + System.getProperty("line.separator") + "in " + tAnkDays + " Tagen";
        }
        //get the time and format it (tAnkUnt)
        double tAnkUntD = OcycoApplication.gettAnkUnt();
        int tAnkUntStd = (int) tAnkUntD;
        int tAnkUntMin = (int) Math.round(((tAnkUntD - tAnkUntStd) * 60));
        String tAnkUnt = tAnkUntStd + "h " + tAnkUntMin + "min";
        String vDMuss = roundDecimals(OcycoApplication.getvDMuss()) + " km/h";
        String vDunt = roundDecimals(OcycoApplication.getvDunt()) + " km/h";
        //show in info boxes
        sGefBox.setText(sGef + "");
        sZufBox.setText(sZuf + "");
        vAktBox.setText(vAkt + "");
        vDBox.setText(vD + "");
        tAnkBox.setText(tAnk + "");
        tAnkUntBox.setText(tAnkUnt + "");
        vDMussBox.setText(vDMuss + "");
        vDUntBox.setText(vDunt + "");
        //do not show unrealistic high values
        if (tAnkDays > 2) {
            tAnkBox.setText("--:--");
            tAnkUntBox.setText("--:--");
        }
        //set color
        Context context = getApplicationContext();
        if (OcycoApplication.gettAnkUnt() < 0) {
            tAnkUntBox.setTextColor(ContextCompat.getColor(context, R.color.good_value));
        } else if (OcycoApplication.gettAnkUnt() > 0) {
            tAnkUntBox.setTextColor(ContextCompat.getColor(context, R.color.bad_value));
        }
        if (OcycoApplication.getvDunt() < 0) {
            vDUntBox.setTextColor(ContextCompat.getColor(context, R.color.good_value));
        } else if (OcycoApplication.getvDunt() > 0) {
            vDUntBox.setTextColor(ContextCompat.getColor(context, R.color.bad_value));
        }
        //set vDMuss & vDunt "---", if it is later then the wanted arrival time
        if (OcycoApplication.getvDMuss() < 0) {
            vDMussBox.setText("---");
            vDUntBox.setText("---");
            vDUntBox.setTextColor(ContextCompat.getColor(context, R.color.default_black));
        }
    }

    private String roundDecimals(double d) {
        return String.format("%.2f", d);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_show_data, menu);
        this.menu = menu;
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
            case R.id.action_MainActivity:
                Intent intent_main = new Intent(this, MainActivity.class);
                startActivity(intent_main);
                return true;
            case R.id.action_RoutingActivity:
                Intent intent_routing = new Intent(this, RoutingActivity.class);
                startActivity(intent_routing);
                return true;
            case R.id.auto_center:
                item.setChecked(!item.isChecked());
                OcycoApplication.setAutoCenter(item.isChecked());
                //only enable auto rotation, if auto centering is enabled
                MenuItem auto_rotate = menu.findItem(R.id.auto_rotate);
                auto_rotate.setEnabled(item.isChecked());
                auto_rotate.setChecked(false);
                //set global variables
                OcycoApplication.setAuto_rotate(false);
                OcycoApplication.setAlign_north(true);
                return true;
            case R.id.auto_rotate:
                item.setChecked(!item.isChecked());
                OcycoApplication.setAuto_rotate(item.isChecked());
                OcycoApplication.setAlign_north(!item.isChecked());
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