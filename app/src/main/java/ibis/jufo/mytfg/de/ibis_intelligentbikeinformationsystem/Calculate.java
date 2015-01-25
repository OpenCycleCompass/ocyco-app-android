package ibis.jufo.mytfg.de.ibis_intelligentbikeinformationsystem;

import android.location.Location;
import android.util.Log;

import java.util.Calendar;

public class Calculate {

    //location vars
    Location firstLoc;
    Location oldLoc;
    Location newLoc;

    //calculation and output vars
    //speed
    double vD; //Durchschnittsgeschwindikeit
    double vAkt; //aktuelle Geschwindigkeit
    double vDMuss; //notwendige Durchscnittsgeschwindigkeit, um am vorgegebenen Zeitpunkt das Ziel zu erreichen
    double vDunt; // Unterschied zwischen aktueller- und notwendiger Durchschnittsgeschwindigkeit
    //distance
    double sEing; //eingegebene Strecke
    double sGef; //gefahrene Strecke
    double sZuf; //zu fahrende Strecke
    //time
    double tGef; //gefahrene Zeit
    double tZuf; //zu fahrende Zeit
    double tAkt; // aktuelle Zeit
    double tAnk; // Ankunftszeit
    double tAnkEing; //Eingegebene, gewünschte Ankunftszeit
    double tAnkUnt; //Unterschied zwischen realer und gewünschter Ankunftszeit

    // Log TAG
    protected static final String TAG = "iBis-calculate-class";


    public void getData(Location location, Double sEingInput) {
        //get location
        oldLoc = newLoc;
        newLoc = location;
        if (oldLoc == null) {
            firstLoc = location;
        }
        //get distance
        sEing = sEingInput;
    }

    public boolean checkFirstLoc () {
        boolean firstLoc = false;
        if (oldLoc == null) {
            firstLoc=true;
        }
        return firstLoc;
    }


    public void calculateTimeVars(double tAnkEingTimeInput) {
        double tAnkEingTime = tAnkEingTimeInput;
        //get date in milliseconds
        final Calendar c = Calendar.getInstance();
        int current_hour = c.get(Calendar.HOUR_OF_DAY);
        int current_minute = c.get(Calendar.MINUTE);
        double milliSeconds = c.get(Calendar.MILLISECOND);
        double currentTimeMillis = (double) ((current_hour * 60 + current_minute) * 60 * 1000);
        double dateInMilliseconds = (milliSeconds - currentTimeMillis);
        //add date in milliseconds, convert to hours
        tAnkEing = (((dateInMilliseconds + tAnkEingTime)/1000)/60)/60;

        //get actual time in hours
        tAkt = ((currentTimeMillis / 1000)/60)/60;
    }


    public void calculateDrivenDistance() {
        //Calculate the distance between old and newLoc and add to sGef
        double dLon = 111.3 * (oldLoc.getLongitude() - newLoc.getLongitude());
        double dLat = 71.5 * (oldLoc.getLatitude() - newLoc.getLatitude());
        double lastDistance = Math.sqrt(dLon * dLon + dLat * dLat);
        sGef += lastDistance;
    }

    public void calculateDrivenTime() {
        //calculate driven time and convert to seconds
        tGef = (double) (newLoc.getTime() - (firstLoc.getTime()) / 1000);
    }


    public void math() {
        //average speed
        vD = sGef / tGef;
        Log.i(TAG, vD+"="+sGef+"/"+tGef);
        //distance to drive
        sZuf = sEing - sGef;
        Log.i(TAG, sZuf+"="+sEing+"-"+sGef);
        //time to drive
        tZuf = sZuf / vD;
        Log.i(TAG, tZuf+"="+sZuf+"/"+vD);
        //arrival time
        tAnk = tAkt + tZuf;
        Log.i(TAG, tAnk+"="+tAkt+"+"+tZuf);
        //difference between arrival and planed arrival time
        tAnkUnt = tAnkEing - tAnk;
        Log.i(TAG, tAnkUnt+"="+tAnkEing+"/"+tAnk);
        //necessary speed for arriving in time
        vDMuss = sZuf / (tAnkEing-tAnk);
        Log.i(TAG, vDMuss+"="+sZuf+"/"+tAnkEing+"-"+tAnk);
        //difference between real and necessary average speed
        vDunt = vD - vDMuss;
        Log.i(TAG, vDunt+"="+vD+"-"+vDMuss);
    }

    //create and declare Interface
    public static interface OnTransferDataListener {
        public abstract void onTransferData(double sGef, double sZuf, double vAkt, double vD, double tAnk, double tAnkUnt, double vDMuss, double vDUnt);
    }

    public OnTransferDataListener mOTDListener;

    public void output() {
        Log.i(TAG, sGef+" "+sZuf+" "+vAkt+" "+vD+" "+tAnk+" "+tAnkUnt+" "+vDMuss+" "+vDunt);
        //TODO: NullPointerException beheben...
        this.mOTDListener.onTransferData(sGef, sZuf, vAkt, vD, tAnk, tAnkUnt, vDMuss, vDunt);
    }


}
