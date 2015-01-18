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

    public void math () {
        //TODO: Main part - mathematical operation to calculate the output vars

    }

    public void output () {
        //TODO: send the output vars to ShowDataActivity

    }


}
