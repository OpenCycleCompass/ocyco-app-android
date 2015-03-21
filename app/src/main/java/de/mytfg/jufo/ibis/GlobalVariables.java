package de.mytfg.jufo.ibis;

import android.app.Application;
import android.location.Location;

public class GlobalVariables extends Application {

    //The variables can be set and read from every Activity or Service!
    private double sGef, sZuf, vAkt, vD, tAnk, tAnkUnt, vDMuss, vDunt, tAnkEingTime, sEing,
            sEingTimeFactor;
    private float textSize;
    private Location location;
    private boolean show_locationOverlay, show_compassOverlay, show_scaleBarOverlay, collectData,
            collectDataSet, auto_center = true, trackingRunning = false, use_time_factor,
            changed_settings, auto_rotate, align_north=false;


    public void setCalculationVars(double sGefIn, double sZufIn, double vAktIn, double vDIn, double
            tAnkIn, double tAnkUntIn, double vDMussIn, double vDuntIn) {
        sGef = sGefIn;
        sZuf = sZufIn;
        vAkt = vAktIn;
        vD = vDIn;
        tAnk = tAnkIn;
        tAnkUnt = tAnkUntIn;
        vDMuss = vDMussIn;
        vDunt = vDuntIn;
    }

    public void setChanged_settings(boolean changed_settingsIn) {
        changed_settings = changed_settingsIn;
    }

    public void setAlign_north (boolean align_northIn) { align_north=align_northIn; }

    public void setAuto_rotate(boolean auto_rotateIn) {
        auto_rotate = auto_rotateIn;
    }

    public void setsEingTimeFactor(double sEingTimeVarIn) {
        sEingTimeFactor = sEingTimeVarIn;
    }

    public void setUseTimeFactor(boolean tfIn) {
        use_time_factor = tfIn;
    }

    public void setTrackingRunning(boolean trRunIn) {
        trackingRunning = trRunIn;
    }

    public void setAutoCenter(boolean auto_center_in) {
        auto_center = auto_center_in;
    }

    public void setLocation(Location locationIn) {
        location = locationIn;
    }

    public void setSettingVars(float textSizeIn) {
        textSize = textSizeIn;
    }

    public void setsEing(double sEingIn) {
        sEing = sEingIn;
    }

    public void settAnkEingTime(double tAnkEingTimeIn) {
        tAnkEingTime = tAnkEingTimeIn;
    }

    public void setShowLocationOverlay(boolean show_locationOverlay_in) {
        show_locationOverlay = show_locationOverlay_in;
    }

    public void setShowCompassOverlay(boolean show_compassOverlay_in) {
        show_compassOverlay = show_compassOverlay_in;
    }

    public void setShowScaleBarOverlay(boolean show_scaleBarOverlay_in) {
        show_scaleBarOverlay = show_scaleBarOverlay_in;
    }

    public void setCollectData(boolean collectDataIn) {
        collectData = collectDataIn;
        collectDataSet = true;
    }

    public boolean isAlign_north() { return align_north; }

    public boolean isAuto_rotate() { return auto_rotate; }

    public boolean isChanged_settings() {
        return changed_settings;
    }

    public boolean isUseTimeFactor() {
        return use_time_factor;
    }

    public boolean isTrackingRunning() {
        return trackingRunning;
    }

    public boolean isAutoCenter() {
        return auto_center;
    }

    public boolean isCollectData() {
        return collectData;
    }

    public boolean isCollectDataSet() {
        return collectDataSet;
    }

    public boolean isShow_locationOverlay() {
        return show_locationOverlay;
    }

    public boolean isShow_compassOverlay() {
        return show_compassOverlay;
    }

    public boolean isShow_scaleBarOverlay() {
        return show_scaleBarOverlay;
    }

    public double getsEingTimeFactor() {
        return sEingTimeFactor;
    }

    public Location getLocation() {
        return location;
    }

    public double getsEing() {
        return sEing;
    }

    public double gettAnkEingTime() {
        return tAnkEingTime;
    }

    public float getTextSize() {
        return textSize;
    }

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

