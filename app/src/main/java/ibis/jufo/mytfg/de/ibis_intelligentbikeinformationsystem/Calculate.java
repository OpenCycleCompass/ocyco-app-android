package ibis.jufo.mytfg.de.ibis_intelligentbikeinformationsystem;


import android.location.Location;


public class Calculate {

    //location vars
    Location firstLoc;
    Location oldLoc;
    Location newLoc;

    //calculation and output vars
    //speed
    Double vD; //Durchschnittsgeschwindikeit
    Double vAkt; //aktuelle Geschwindigkeit
    Double vDMuss; //notwendige Durchscnittsgeschwindigkeit, um am vorgegebenen Zeitpunkt das Ziel zu erreichen
    Double vDunt; // Unterschied zwischen aktueller- und notwendiger Durchschnittsgeschwindigkeit
    //distance
    Double sGef; //gefahrene Strecke
    Double sZuf; //zu fahrende Strecke
    //time
    Double tGef; //gefahrene Zeit
    Double tZuf; //zu fahrende Zeit
    Double tAkt; // aktuelle Zeit
    Double tAnk; // Ankunftszeit
    Double tAnkEing; //Eingegebene, gewünschte Ankunftszeit
    Double tAnkUnt; //Unterschied zwischen realer und gewünschter Ankunftszeit

    public void getLocation (Location location) {
        oldLoc = newLoc;
        newLoc = location;
        if (oldLoc == null){
            firstLoc = location;
        }

    }


    public void calculateDrivenDistance () {
        //Calculate the distance between old and newLoc and add to sGef
        Double dLon = 111.3 * (oldLoc.getLongitude() - newLoc.getLongitude());
        Double dLat = 71.5 * (oldLoc.getLatitude() - newLoc.getLatitude());
        Double lastDistance = Math.sqrt(dLon * dLon + dLat * dLat);
        sGef += lastDistance;
    }

    public void calculateDrivenTime () {
        //calculate driven time and convert to seconds
        tGef = (double)((firstLoc.getTime() - newLoc.getTime())/1000);
    }

    public void getTimeVars (Double timeInput) {
        //set tAnkEing
        tAnkEing = timeInput;
        //get actual time
        tAkt = (double) newLoc.getTime();

    }


    public void math () {
        //average speed
        vD = sGef / tGef;
        //time to drive
        tZuf = sZuf / vD;
        //arrival time
        tAnk = tAkt + tZuf;
        //difference between arrival and planed arrival time
        tAnkUnt = tAnkEing - tAnk;
        //necessary speed for arriving in time
        vDMuss = sZuf / tZuf;
        //difference between real and necessary average speed
        vDunt = vD - vDMuss;
    }

    public void output () {
        //TODO: send the output vars to ShowDataActivity

    }


}