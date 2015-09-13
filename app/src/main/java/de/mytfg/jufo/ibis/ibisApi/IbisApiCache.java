package de.mytfg.jufo.ibis.ibisApi;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


/**
 * Used to Cache API-Calls.
 */
public class IbisApiCache {
    // This is the default timeout to recall the API.
    public static final long defaultTimeout = 604800; // 1 week

    private static Map<IbisApiCacheKey, IbisApiResult> cache = new HashMap<>();

    /**
     * Calls the MyTFG API or returns a cached return for this call.
     * Cached result is used, if last call to this function is less seconds
     * than the defaultTimeout ago.
     * @param apiFunction The API function to call.
     * @param params Params to pass to the API.
     * @param callback A callback function to call after the API-call finishes.
     * @param timeout The timeout in seconds for the cache.
     */
    public static void call(String apiFunction, String apiMethod, final JSONObject params,
                            final IbisApiCallback callback, long timeout) {

        final IbisApiCacheKey key = new IbisApiCacheKey(apiFunction, apiMethod, params);

        if (cache.containsKey(key)) {
            IbisApiResult cachedResult = cache.get(key);
            if (cachedResult.getTimestamp() + timeout > (System.currentTimeMillis()/1000)) {
                // Cache is up-to-date
                callback.callback(cachedResult.getReturnCode(), cachedResult.getResult());

                return;
            }
        }

        // No cache entry: Call API
        IbisApiCallback cacheCallback = new IbisApiCallback() {
            @Override
            public void callback(int responseCode, JSONObject result) {
                // put solely successful results into cache
                if (responseCode >= 200 && responseCode < 300) {
                    cache.put(key, new IbisApiResult(responseCode, result));
                }
                callback.callback(responseCode, result);
            }
        };
        IbisApi.call(apiFunction, apiMethod, params, cacheCallback);
    }
}
