package de.opencyclecompass.app.android.ocycoApi;

import org.json.JSONObject;

/**
 * Wrapper for an API-Result
 */
public class OcycoApiResult {
    private JSONObject result;
    private int returnCode;
    private long timestamp;

    public OcycoApiResult(int httpCode, JSONObject result) {
        this.returnCode = httpCode;
        this.result = result;
        this.timestamp = (System.currentTimeMillis() / 1000);
    }

    public JSONObject getResult() {
        return this.result;
    }

    public int getReturnCode() {
        return this.returnCode;
    }

    protected long getTimestamp() {
        return this.timestamp;
    }
}
