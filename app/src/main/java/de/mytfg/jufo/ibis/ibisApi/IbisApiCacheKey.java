package de.mytfg.jufo.ibis.ibisApi;

import org.json.JSONObject;

/**
 * A key for internal use in the API Cache.
 */
public class IbisApiCacheKey {

    private final String resource;
    private final String method;
    private final JSONObject params;

    public IbisApiCacheKey(String resource, String method, JSONObject params) {
        this.resource = resource;
        this.method = method;
        this.params = params;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IbisApiCacheKey)) return false;
        IbisApiCacheKey key = (IbisApiCacheKey) o;
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
