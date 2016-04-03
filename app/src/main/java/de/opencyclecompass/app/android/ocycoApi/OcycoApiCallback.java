package de.opencyclecompass.app.android.ocycoApi;

import org.json.JSONObject;

public interface OcycoApiCallback {
    /**
     * Called by the call-Method when request finished.
     * @param responseCode The HTTP Response Code.
     * @param result Data returned by the API as JSON.
     */
    void callback(int responseCode, JSONObject result);
}
