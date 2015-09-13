package de.mytfg.jufo.ibis.ibisApi;

import org.json.JSONObject;

public interface IbisApiCallback {
    /**
     * Called by the call-Method when request finished.
     * @param responseCode The HTTP Response Code.
     * @param result Data returned by the API as JSON.
     */
    void callback(int responseCode, JSONObject result);
}
