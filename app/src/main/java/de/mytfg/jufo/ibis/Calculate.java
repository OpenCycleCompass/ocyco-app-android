package de.mytfg.jufo.ibis;

import android.location.Location;

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
    double lastDistance;
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
            double timeDiff = (((newTime - oldTime) / 1000) / 60) / 60;
            vAkt = lastDistance / timeDiff;
        }
    }

    public void calculateTimeVars(double tAnkEingTimeInput) {
        //get date in milliseconds
        final Calendar c = Calendar.getInstance();
        int current_hour = c.get(Calendar.HOUR_OF_DAY);
        int current_minute = c.get(Calendar.MINUTE);
        double milliSeconds = System.currentTimeMillis();
        double timeInMillis = (double) ((current_hour * 60 + current_minute) * 60 * 1000);
        dateInMilliseconds = (milliSeconds - timeInMillis);
        //add date to time
        tAnkEing = (((tAnkEingTimeInput + dateInMilliseconds)/1000)/60)/60;
        //get actual time in hours
        tAkt = (((timeInMillis + dateInMilliseconds)/1000)/60)/60;
    }

    public void calculateDrivenDistance() {
        //Calculate the distance between old and newLoc in kilometers and add to sGef
        lastDistance = newLoc.distanceTo(oldLoc)/1000;
        sGef += lastDistance;
    }

    public void calculateDrivenTime() {
        //calculate driven time and convert to hours
        tGef = ((((newLoc.getTime() - firstLoc.getTime()) / 1000) / 60) / 60);
    }

    public void math() {
        //average speed
        vD = sGef / tGef;
        //distance to drive
        sZuf = sEing - sGef;
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
        return (tAnk-((((dateInMilliseconds)/1000)/60)/60));
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
