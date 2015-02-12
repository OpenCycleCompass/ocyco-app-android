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
        boolean firstLoc = false;
        if (oldLoc == null) {
            firstLoc = true;
        }
        return firstLoc;
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
        tAnkEing = ((tAnkEingTimeInput / 1000) / 60) / 60;
        //get date in milliseconds
        final Calendar c = Calendar.getInstance();
        int current_hour = c.get(Calendar.HOUR_OF_DAY);
        int current_minute = c.get(Calendar.MINUTE);
        double milliSeconds = System.currentTimeMillis();
        double timeInMillis = (double) ((current_hour * 60 + current_minute) * 60 * 1000);
        double dateInMilliseconds = (milliSeconds - timeInMillis);

        //get actual time in hours
        tAkt = (((milliSeconds - dateInMilliseconds) / 1000) / 60) / 60;
    }


    public void calculateDrivenDistance() {
        //Calculate the distance between old and newLoc and add to sGef
        double dLon = 111.3 * (oldLoc.getLongitude() - newLoc.getLongitude());
        double dLat = 71.5 * (oldLoc.getLatitude() - newLoc.getLatitude());
        lastDistance = Math.sqrt(dLon * dLon + dLat * dLat);
        sGef += lastDistance;
    }

    public void calculateDrivenTime() {
        //calculate driven time and convert to hours
        double newTime = newLoc.getTime();
        double firstTime = firstLoc.getTime();
        tGef = ((((newTime - firstTime) / 1000) / 60) / 60);
    }


    public void math() {
        //average speed
        vD = sGef / tGef;
        //distance to drive
        sZuf = sEing - sGef;
        //time to drive
        tZuf = sZuf / vD;
        //arrival time
        tAnk = tAkt + tZuf;
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
        return tAnk;
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
