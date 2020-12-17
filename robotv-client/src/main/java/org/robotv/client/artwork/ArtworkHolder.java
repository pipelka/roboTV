package org.robotv.client.artwork;

import android.text.TextUtils;

import java.io.Serializable;

public class ArtworkHolder implements Serializable {

    private String posterUrl;
    private String backgroundUrl;
    protected String title;

    public ArtworkHolder(String posterUrl, String backgroundUrl) {
        this.posterUrl = posterUrl;
        this.backgroundUrl = backgroundUrl;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public String getBackgroundUrl() {
        return backgroundUrl;
    }

    public void setPosterUrl(String url) {
        this.posterUrl = url;
    }

    public void setBackgroundUrl(String url) {
        this.backgroundUrl = url;
    }

    public boolean hasPoster() {
        return !TextUtils.isEmpty(posterUrl) && !posterUrl.equals("x");
    }

    public boolean hasBackground() {
        return !TextUtils.isEmpty(backgroundUrl) && !backgroundUrl.equals("x");
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public boolean hasArtwork() {
        return hasPoster() || hasBackground();
    }
}
