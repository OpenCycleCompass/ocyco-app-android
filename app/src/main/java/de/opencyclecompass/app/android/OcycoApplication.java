package de.opencyclecompass.app.android;

import android.app.Application;
import android.location.Location;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender;

import de.opencyclecompass.app.android.storage.OcycoTrackArchive;

@ReportsCrashes(
        httpMethod = HttpSender.Method.PUT,
        reportType = HttpSender.Type.JSON,
        formUri = "https://acra.mytfg.de/acra-ibis/_design/acra-storage/_update/report",
        formUriBasicAuthLogin = "ibis_android",
        formUriBasicAuthPassword = "IPNMRXhAuN/YstodKoGdQbxUPkg=",

        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.crash_toast
)

public class OcycoApplication extends Application {

    //The variables can be set and read from every Activity or Service!
    private static double sGef;
    private static double sZuf;
    private static double vAkt;
    private static double vD;
    private static double tAnk;
    private static double tAnkUnt;
    private static double vDMuss;
    private static double vDunt;
    private static double tAnkEingTime;
    private static double sEing;
    private static double sEingTimeFactor;
    private static float textSize;
    private static Location location;
    private static boolean show_locationOverlay;
    private static boolean show_compassOverlay;
    private static boolean show_scaleBarOverlay;
    private static boolean collect_data;
    private static boolean auto_center = true;
    private static boolean use_time_factor;
    private static boolean changed_settings;
    private static boolean auto_rotate;
    private static boolean align_north = false;
    private static boolean online_tracking_running;

    public static OcycoTrackArchive trackArchive;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialise ACRA:
        ACRA.init(this);

        // create track archive
        trackArchive = new OcycoTrackArchive(this);
    }

    public static void setCalculationVars(double sGefIn, double sZufIn, double vAktIn, double vDIn, double tAnkIn, double tAnkUntIn, double vDMussIn, double vDuntIn) {
        sGef = sGefIn;
        sZuf = sZufIn;
        vAkt = vAktIn;
        vD = vDIn;
        tAnk = tAnkIn;
        tAnkUnt = tAnkUntIn;
        vDMuss = vDMussIn;
        vDunt = vDuntIn;
    }

    public static void setSettingVars(float textSizeIn) {
        textSize = textSizeIn;
    }

    public static void setShowLocationOverlay(boolean show_locationOverlay_in) {
        show_locationOverlay = show_locationOverlay_in;
    }

    public static void setShowCompassOverlay(boolean show_compassOverlay_in) {
        show_compassOverlay = show_compassOverlay_in;
    }

    public static void setShowScaleBarOverlay(boolean show_scaleBarOverlay_in) {
        show_scaleBarOverlay = show_scaleBarOverlay_in;
    }

    public static boolean isOnline_tracking_running() {return online_tracking_running;}

    public static boolean isAlign_north() {
        return align_north;
    }

    public static void setAlign_north(boolean align_northIn) {
        align_north = align_northIn;
    }

    public static boolean isAuto_rotate() {
        return auto_rotate;
    }

    public static void setAuto_rotate(boolean auto_rotateIn) {
        auto_rotate = auto_rotateIn;
    }

    public static boolean isChanged_settings() {
        return changed_settings;
    }

    public static void setChanged_settings(boolean changed_settingsIn) {
        changed_settings = changed_settingsIn;
    }

    public static boolean isUseTimeFactor() {
        return use_time_factor;
    }

    public static void setUseTimeFactor(boolean tfIn) {
        use_time_factor = tfIn;
    }

    public static boolean isAutoCenter() {
        return auto_center;
    }

    public static void setAutoCenter(boolean auto_center_in) {
        auto_center = auto_center_in;
    }

    public static boolean isCollect_data() {
        return collect_data;
    }

    public static void setCollect_data(boolean collect_dataIn) {
        collect_data = collect_dataIn;
    }

    public static boolean isShow_locationOverlay() {
        return show_locationOverlay;
    }

    public static boolean isShow_compassOverlay() {
        return show_compassOverlay;
    }

    public static boolean isShow_scaleBarOverlay() {
        return show_scaleBarOverlay;
    }

    public static double getsEingTimeFactor() {
        return sEingTimeFactor;
    }

    public static void setsEingTimeFactor(double sEingTimeVarIn) {
        sEingTimeFactor = sEingTimeVarIn;
    }

    public static Location getLocation() {
        return location;
    }

    public static void setLocation(Location locationIn) {
        location = locationIn;
    }

    public static double getsEing() {
        return sEing;
    }

    public static void setOnline_tracking_running (boolean online_tracking_runningIn){
        online_tracking_running=online_tracking_runningIn;
    }

    public static void setsEing(double sEingIn) {
        sEing = sEingIn;
    }

    public static double gettAnkEingTime() {
        return tAnkEingTime;
    }

    public static void settAnkEingTime(double tAnkEingTimeIn) {
        tAnkEingTime = tAnkEingTimeIn;
    }

    public static float getTextSize() {
        return textSize;
    }

    public static double getsGef() {
        return sGef;
    }

    public static double getsZuf() {
        return sZuf;
    }

    public static double getvAkt() {
        return vAkt;
    }

    public static double getvD() {
        return vD;
    }

    public static double gettAnk() {
        return tAnk;
    }

    public static double gettAnkUnt() {
        return tAnkUnt;
    }

    public static double getvDMuss() {
        return vDMuss;
    }

    public static double getvDunt() {
        return vDunt;
    }
}

