package de.opencyclecompass.app.android.ocycoApi;

import org.json.JSONObject;

/**
 * A key for internal use in the API Cache.
 */
public class OcycoApiCacheKey {

    private final String resource;
    private final String method;
    private final JSONObject params;

    public OcycoApiCacheKey(String resource, String method, JSONObject params) {
        this.resource = resource;
        this.method = method;
        this.params = params;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OcycoApiCacheKey)) return false;
        OcycoApiCacheKey key = (OcycoApiCacheKey) o;
        return (this.resource.equals(key.resource)) && (this.method.equals(key.method)) && (this.params.equals(key.params));
    }

    @Override
    public int hashCode() {
        int result = resource.hashCode();
        result = 31 * result + method.hashCode();
        result = 31 * result + params.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return this.method + " " + this.resource + " & " + this.params.toString();
    }
}
