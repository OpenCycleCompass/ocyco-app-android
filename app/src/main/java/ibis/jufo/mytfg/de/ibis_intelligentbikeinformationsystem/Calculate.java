package ibis.jufo.mytfg.de.ibis_intelligentbikeinformationsystem;


import android.location.Location;

public class Calculate {

    //location vars
    Location firstLoc;
    Location oldLoc;
    Location newLoc;

    //calculation and output vars
    //speed
    Float vD; //Durchschnittsgeschwindikeit
    Float vAkt; //aktuelle Geschwindigkeit
    Float vDMuss; //notwendige Durchscnittsgeschwindigkeit, um am vorgegebenen Zeitpunkt das Ziel zu erreichen
    Float vDunt; // Unterschied zwischen aktueller- und notwendiger Durchschnittsgeschwindigkeit
    //distance
    Float sGef; //gefahrene Strecke
    Float sZuf; //zu fahrende Strecke
    //time
    Float tGef; //gefahrene Zeit
    Float tZuf; //zu fahrende Zeit
    Float tAkt; // aktuelle Zeit
    Float tAnk; // Ankunftszeit
    Float tAnkEing; //Eingegebene, gewünschte Ankunftszeit
    Float tAnkUnt; //Unterschied zwischen realer und gewünschter Ankunftszeit

    public void getLocation (Location location) {
        oldLoc = newLoc;
        newLoc = location;
        if (oldLoc == null){
            firstLoc = location;
        }

    }


    public void calculateDrivenDistance () {
        //TODO: Calculate the distance between old and newLoc

    }

    public void math () {
        //TODO: Main part - mathematical operation to calculate the output vars

    }

    public void output () {
        //TODO: send the output vars to ShowDataActivity

    }


}
