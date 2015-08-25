package de.mytfg.jufo.ibis;

import android.app.Application;
import android.location.Location;

import org.acra.*;
import org.acra.annotation.*;
import org.acra.sender.HttpSender;

@ReportsCrashes(
        httpMethod = HttpSender.Method.PUT,
        reportType = HttpSender.Type.JSON,
        formUri = "https://acra.mytfg.de/acra-ibis/_design/acra-storage/_update/report",
        formUriBasicAuthLogin = "ibis_android",
        formUriBasicAuthPassword = "IPNMRXhAuN/YstodKoGdQbxUPkg=",

        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.crash_toast
)

public class GlobalVariables extends Application {

    //The variables can be set and read from every Activity or Service!
    private double sGef, sZuf, vAkt, vD, tAnk, tAnkUnt, vDMuss, vDunt, tAnkEingTime, sEing, sEingTimeFactor;
    private float textSize;
    private Location location;
    private boolean show_locationOverlay, show_compassOverlay, show_scaleBarOverlay, collect_data,
    auto_center = true, use_time_factor,
            changed_settings, auto_rotate, align_north = false, online_tracking_running;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialise ACRA:
        ACRA.init(this);
    }

    public void setCalculationVars(double sGefIn, double sZufIn, double vAktIn, double vDIn, double tAnkIn, double tAnkUntIn, double vDMussIn, double vDuntIn) {
        sGef = sGefIn;
        sZuf = sZufIn;
        vAkt = vAktIn;
        vD = vDIn;
        tAnk = tAnkIn;
        tAnkUnt = tAnkUntIn;
        vDMuss = vDMussIn;
        vDunt = vDuntIn;
    }

    public void setSettingVars(float textSizeIn) {
        textSize = textSizeIn;
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

    public boolean isOnline_tracking_running() {return online_tracking_running;}

    public boolean isAlign_north() {
        return align_north;
    }

    public void setAlign_north(boolean align_northIn) {
        align_north = align_northIn;
    }

    public boolean isAuto_rotate() {
        return auto_rotate;
    }

    public void setAuto_rotate(boolean auto_rotateIn) {
        auto_rotate = auto_rotateIn;
    }

    public boolean isChanged_settings() {
        return changed_settings;
    }

    public void setChanged_settings(boolean changed_settingsIn) {
        changed_settings = changed_settingsIn;
    }

    public boolean isUseTimeFactor() {
        return use_time_factor;
    }

    public void setUseTimeFactor(boolean tfIn) {
        use_time_factor = tfIn;
    }

    public boolean isAutoCenter() {
        return auto_center;
    }

    public void setAutoCenter(boolean auto_center_in) {
        auto_center = auto_center_in;
    }

    public boolean isCollect_data() {
        return collect_data;
    }

    public void setCollect_data(boolean collect_dataIn) {
        collect_data = collect_dataIn;
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

    public void setsEingTimeFactor(double sEingTimeVarIn) {
        sEingTimeFactor = sEingTimeVarIn;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location locationIn) {
        location = locationIn;
    }

    public double getsEing() {
        return sEing;
    }

    public void setOnline_tracking_running (boolean online_tracking_runningIn){
        online_tracking_running=online_tracking_runningIn;
    }

    public void setsEing(double sEingIn) {
        sEing = sEingIn;
    }

    public double gettAnkEingTime() {
        return tAnkEingTime;
    }

    public void settAnkEingTime(double tAnkEingTimeIn) {
        tAnkEingTime = tAnkEingTimeIn;
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

