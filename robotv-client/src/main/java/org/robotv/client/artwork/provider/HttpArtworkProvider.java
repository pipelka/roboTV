package org.robotv.client.artwork.provider;

import android.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

abstract class HttpArtworkProvider extends ArtworkProvider {

    private final static String TAG = "HttpArtworkProvider";

    private final static Object lock = new Object();

    private int mDelayAfterRequestMs = 0;
    private final OkHttpClient client;

    HttpArtworkProvider() {
        this(5);
    }

    HttpArtworkProvider(int concurrentConnections) {
        client = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(concurrentConnections, 5, TimeUnit.MINUTES))
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .build();
    }

    void setDelayAfterRequest(int delayMs) {
        mDelayAfterRequestMs = delayMs;
    }

    boolean checkUrlExists(String address) throws IOException {
        HttpUrl httpUrl = HttpUrl.parse(address);
        if(httpUrl == null) {
            return false;
        }

        Request request = new Request.Builder().url(httpUrl).build();
        Response response = client.newCall(request).execute();

        int status = response.code();

        if(status == 200) {
            return true;
        }

        Log.d(TAG, "failed (status: " + status + ")");
        return false;
    }

    String getResponseFromServer(String address) throws IOException {
        long startTime;

        HttpUrl httpUrl = HttpUrl.parse(address);
        if(httpUrl == null) {
            return "";
        }

        Request request;
        Response response;

        if(mDelayAfterRequestMs > 0) {
            synchronized (lock) {
                startTime = System.currentTimeMillis();

                request = new Request.Builder().url(httpUrl).build();
                response = client.newCall(request).execute();

                long timeSpent = System.currentTimeMillis() - startTime;
                long delay = mDelayAfterRequestMs - timeSpent;

                if(delay > 0) {
                    Log.d(TAG, String.format("wait for %d ms", delay));
                    try {
                        TimeUnit.MILLISECONDS.sleep(delay);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        else {
            request = new Request.Builder().url(httpUrl).build();
            response = client.newCall(request).execute();
        }

        ResponseBody body = response.body();
        if(body != null) {
            return body.string();
        }

        return "";
    }

    protected OkHttpClient getClient() {
        return client;
    }

}
