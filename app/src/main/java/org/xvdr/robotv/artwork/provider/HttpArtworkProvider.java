package org.xvdr.robotv.artwork.provider;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;

public abstract class HttpArtworkProvider extends ArtworkProvider {

    private final static String TAG = "HttpArtworkProvider";

    private int mTimeoutMs = 2000;
    private int mDelayAfterRequestMs = 0;

    public void setTimeout(int timeoutMs) {
        mTimeoutMs = timeoutMs;
    }

    public void setDelayAfterRequest(int delayMs) {
        mDelayAfterRequestMs = delayMs;
    }

    protected boolean checkUrlExists(String urlString) throws IOException {
        Log.d(TAG, "checking url: " + urlString);

        URL url;

        try {
            url = new URL(urlString);
        }
        catch (MalformedURLException e) {
            return false;
        }

        HttpURLConnection huc = (HttpURLConnection)url.openConnection();
        huc.setRequestMethod("GET");
        huc.connect();

        int status = huc.getResponseCode();
        if(status == 200) {
            return true;
        }

        Log.d(TAG, "failed (status: " + status + ")");
        return false;
    }

    protected String getResponseFromServer(String url) throws IOException {
        Log.d(TAG, "reading url: " + url);

        BufferedReader inputStream;

        URL jsonUrl = new URL(url);
        URLConnection dc = jsonUrl.openConnection();

        dc.setConnectTimeout(mTimeoutMs);
        dc.setReadTimeout(mTimeoutMs);

        inputStream = new BufferedReader(new InputStreamReader(dc.getInputStream()));

        String result = inputStream.readLine();

        if(mDelayAfterRequestMs > 0) {
            try {
                TimeUnit.MILLISECONDS.sleep(mDelayAfterRequestMs);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return result;
    }
}
