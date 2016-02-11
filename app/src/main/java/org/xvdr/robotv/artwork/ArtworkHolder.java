package org.xvdr.robotv.artwork;

public class ArtworkHolder {

    static final String TAG = "ArtworkHolder";

    public final String title;
    public final String plot;
    private int contentId;

    protected String mPosterUrl;
    protected String mBackgroundUrl;

    public ArtworkHolder(String title, int contentId) {
        this(title, contentId, null);
    }

    public ArtworkHolder(String title, int contentId, String plot) {
        this(title, contentId, plot, "", "");
    }

    public ArtworkHolder(String title, int contentId, String plot, String posterUrl, String backgroundUrl) {
        this.title = title;
        this.contentId = contentId;
        this.plot = plot;

        mPosterUrl = posterUrl;
        mBackgroundUrl = backgroundUrl;
    }

    public String getPosterUrl() {
        return mPosterUrl;
    }

    public void setPosterUrl(String url) {
        mPosterUrl = url;
    }

    public String getBackgroundUrl() {
        return mBackgroundUrl;
    }

    public void setBackgroundUrl(String url) {
        mBackgroundUrl = url;
    }

    public boolean hasPoster() {
        return !mPosterUrl.isEmpty();
    }

    public boolean hasBackground() {
        return !mBackgroundUrl.isEmpty();
    }

    public int getGenre() {
        return contentId & 0xF0;
    }

    public void setContentId(int contentid) {
        this.contentId = contentid;
    }

    public int getContentId() {
        return this.contentId;
    }
}
