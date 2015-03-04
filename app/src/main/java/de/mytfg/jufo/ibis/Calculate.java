package de.mytfg.jufo.ibis;

import android.location.Location;
import android.util.Log;

import java.util.Calendar;

public class Calculate {

    //location vars
    private Location firstLoc;
    private Location oldLoc;
    private Location newLoc;

    //calculation and output vars
    //speed
    private double vD; //Durchschnittsgeschwindikeit
    private double vAkt; //aktuelle Geschwindigkeit
    private double vDMuss; //notwendige Durchscnittsgeschwindigkeit, um am vorgegebenen Zeitpunkt das Ziel zu erreichen
    private double vDunt; // Unterschied zwischen aktueller- und notwendiger Durchschnittsgeschwindigkeit
    //distance
    double sEing; //eingegebene Strecke
    private double sGef; //gefahrene Strecke
    private double sZuf; //zu fahrende Strecke
    double lastDistance;
    //time
    private double tGef; //gefahrene Zeit
    private double tZuf; //zu fahrende Zeit
    private double tAkt; // aktuelle Zeit
    private double tAnk; // Ankunftszeit
    private double tAnkEing; //Eingegebene, gewünschte Ankunftszeit
    private double tAnkUnt; //Unterschied zwischen realer und gewünschter Ankunftszeit

    double dateInMilliseconds;

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

    public boolean checkFirstLoc() {
        return (oldLoc == null);
    }


    public void calculateSpeed() {
        if (newLoc.hasSpeed()) {
            vAkt = (newLoc.getSpeed()) * 3.6;
        } else {
            //time difference between last GPS points in hours
            double oldTime = oldLoc.getTime();
            double newTime = newLoc.getTime();
            double timeDiff = (((newTime - oldTime) / 1000d) / 60d) / 60d;
            vAkt = lastDistance / timeDiff;
        }
    }

    public void calculateTimeVars(double tAnkEingTimeInput) {
        Log.i("timeVars", "tAnkEing:" + tAnkEingTimeInput);
        //get date in milliseconds
        final Calendar c = Calendar.getInstance();
        int current_hour = c.get(Calendar.HOUR_OF_DAY);
        int current_minute = c.get(Calendar.MINUTE);
        double milliSeconds = System.currentTimeMillis();
        double timeInMillis = ((current_hour * 60d + current_minute) * 60d * 1000d);
        Log.i("timeVars", "timeInMs:" + timeInMillis);
        dateInMilliseconds = (milliSeconds - timeInMillis);
        Log.i("timeVars", "dateInMs:" + dateInMilliseconds);
        //add date to time
        tAnkEing = (((tAnkEingTimeInput + dateInMilliseconds) / 1000d) / 60d) / 60d;
        //get actual time in hours
        tAkt = (((timeInMillis + dateInMilliseconds) / 1000d) / 60d) / 60d;
    }

    public void calculateDrivenDistance(double dist) {
        sGef = dist;
    }

    public void calculateDrivenTime() {
        //calculate driven time and convert to hours
        Log.i("TimeCalc", "newLocTime" + newLoc.getTime() + "firstLocTime" + firstLoc.getTime());
        tGef = ((newLoc.getTime() - firstLoc.getTime()) / 1000d / 60d / 60d);
    }

    public void math(boolean use_time_factor, double sEingTimeFactor) {
        //average speed
        vD = sGef / tGef;
        //distance to drive
        if (use_time_factor) {
            sZuf = sEingTimeFactor - sGef;
        } else {
            sZuf = sEing - sGef;
        }
        //time to drive
        tZuf = sZuf / vD;
        //arrival time
        tAnk = (tAkt + tZuf);
        //difference between arrival and planed arrival time
        tAnkUnt = tAnk - tAnkEing;
        //necessary speed for arriving in time
        vDMuss = sZuf / (tAnkEing - tAkt);
        //difference between real and necessary average speed
        vDunt = vDMuss - vD;
    }

    //getters
    public double getsGef() {
        return sGef;
    }

    public double getsZuf() {
        return sZuf;
    }

    public double getvAkt() {
        return vAkt;
    }

    public double getvD() {
        return vD;
    }

    public double gettAnk() {
        //convert to hours
        return (tAnk - ((((dateInMilliseconds) / 1000d) / 60d) / 60d));
    }

    public double gettAnkUnt() {
        return tAnkUnt;
    }

    public double getvDMuss() {
        return vDMuss;
    }

    public double getvDunt() {
        return vDunt;
    }
}
