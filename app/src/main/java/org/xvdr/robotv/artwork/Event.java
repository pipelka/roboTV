package org.xvdr.robotv.artwork;

import android.text.TextUtils;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;

public class Event implements Serializable {

    static private final String TAG = "Event";

    public class SeasonEpisodeHolder implements Serializable {
        public int season = 0;
        public int episode = 0;

        public boolean valid() {
            return season > 0 && episode > 0;
        }

        @Override
        public String toString() {
            return "SeasonEpisodeHolder {" +
                    "season=\'" + season + "\'" +
                    ", episode=\'" + episode + "\'" +
                    "}";
        }
    }

    private int mContentId;
    private String mTitle;
    private String mSubTitle;
    private String mPlot;
    private int mDuration;
    private int mYear;
    private int mEventId;
    private long mStartTime;
    private int mChannelUid;
    private SeasonEpisodeHolder seasonEpisode = new SeasonEpisodeHolder();

    private static Pattern mYearPattern = Pattern.compile("\\b(19|20)\\d{2}\\b");

    private static Pattern seasonEpisodePatterns[] = {
            Pattern.compile("\\b(\\d).+[S|s]taffel.+[F|f]olge\\W+(\\d+)\\b"),
            Pattern.compile("\\b[S|s]taffel\\W+(\\d).+[F|f]olge\\W+(\\d+)\\b"),
            Pattern.compile("\\b[S|s]taffel\\W+(\\d).+[E|e]pisode\\W+(\\d+)\\b"),
            Pattern.compile("\\bS(\\d+).+E(\\d+)\\b"),
            Pattern.compile("\\bs\\W+(\\d+)\\W+ep\\W+(\\d+)\\b")
    };

    private static Pattern episodeSeasonPatterns[] = {
            Pattern.compile("\\b(\\d).+[F|f]olge.+[S|s]taffel\\W++(\\d+)\\b"),
            Pattern.compile("\\bE(\\d+).+S(\\d+)\\b")
    };

    private static Pattern episodeOnlyPatterns[] = {
            Pattern.compile("\\b[F|f]olge\\W+(\\d+)\\b"),
            Pattern.compile("\\b(\\d+)\\W+[F|f]olge\\b"),
            Pattern.compile("\\b[E|e]pisode\\W+(\\d)+\\b")
    };

    private static String[] genreFilm = {
        "abenteuerfilm",
        "actiondrama",
        "actionfilm",
        "animationsfilm",
        "agentenfilm",
        "fantasyfilm",
        "fantasy-film",
        "gangsterfilm",
        "heimatfilm",
        "horrorfilm",
        "historienfilm",
        "jugendfilm",
        "kömödie",
        "kriegsfilm",
        "kriminalfilm",
        "monumentalfilm",
        "mysteryfilm",
        "spielfilm",
        "thriller",
        "zeichentrickfilm"
    };

    private static String[] genreSoap = {
        "folge ",
        "serie",
        "sitcom"
    };

    private static String[] genreSoapOrMovie = {
        "action",
        "comedy",
        "drama",
        "krimi",
        "science-fiction",
        "scifi"
    };

    private static String[] genreSportTitle = {
        "fußball",
        "fussball",
        "tennis",
        "slalom",
        "rtl",
        "abfahrt",
        "ski",
        "eishockey"
    };

    private static int[] genreSportTitleId = {
        0x43,
        0x43,
        0x44,
        0x49,
        0x49,
        0x49,
        0x49,
        0x49
    };

    private static int genreSoapMaxLength = 65 * 60; // 65 min

    public Event(int contentId, String title, String subTitle, String plot, int durationSec) {
        this(contentId, title, subTitle, plot, durationSec, 0);
    }

    public Event(int contentId, String title, String subTitle, String plot, int durationSec, int eventId) {
        this(contentId, title, subTitle, plot, durationSec, eventId, 0);
    }

    public Event(int contentId, String title, String subTitle, String plot, int durationSec, int eventId, int channelUid) {
        mContentId = guessGenreFromSubTitle(contentId, subTitle, durationSec);
        mChannelUid = channelUid;

        // sometimes we can guess the sub genre from the title
        if(getGenre() == 0x40) {
            mContentId = guessGenreFromSubTitle(contentId, title, durationSec);
        }

        mYear = guessYearFromDescription(subTitle + " " + plot);
        mTitle = title;
        mSubTitle = subTitle;
        mPlot = plot;
        mDuration = durationSec;
        mEventId = eventId;

        if (!getSeasonEpisode(plot, seasonEpisode)) {
            getSeasonEpisode(subTitle, seasonEpisode);
        }

        if(seasonEpisode.valid() && !isTvShow()) {
            mContentId = 0x15;
        }
    }

    public int getGenre() {
        return mContentId & 0xF0;
    }

    public int getContentId() {
        return mContentId;
    }

    public int getYear() {
        return mYear;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getSubTitle() {
        return mSubTitle;
    }

    public String getPlot() {
        return mPlot;
    }

    public int getDuration() {
        return mDuration;
    }

    public int getEventId() {
        return mEventId;
    }

    public int getChannelUid() {
        return mChannelUid;
    }

    public boolean isTvShow() {
        return (getContentId() == 0x15 || getContentId() == 0x23);
    }

    public SeasonEpisodeHolder getSeasionEpisode() {
        return seasonEpisode;
    }

    static public int guessYearFromDescription(String description) {
        int year = 0;
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        Matcher matches = mYearPattern.matcher(description);

        while(matches.find()) {
            String word = matches.group();

            try {
                year = parseInt(word);
            }
            catch(Exception e) {
                return 0;
            }

            // found a match
            if(year > 1940 && year <= currentYear) {
                return year;
            }
        }

        return 0;
    }

    static public int guessGenreFromSubTitle(int contentId, String subtitle, int duration) {
        if(subtitle == null) {
            return contentId;
        }

        subtitle = subtitle.toLowerCase();

        // try to assign sport sub genre
        if((contentId & 0xF0) == 0x40) {
            int index = 0;

            for(String word : genreSportTitle) {
                if(subtitle.contains(word.toLowerCase())) {
                    return genreSportTitleId[index];
                }

                index++;
            }
        }

        // try to guess soap from subtitle keywords
        for(String word : genreSoap) {
            if(subtitle.contains(word.toLowerCase())) {
                return 0x15;
            }
        }

        // try to guess soap or movie from subtitle keywords and event duration
        if(duration > 0) {
            for(String word : genreSoapOrMovie) {
                if(subtitle.contains(word.toLowerCase())) {
                    return duration < genreSoapMaxLength ? 0x15 : 0x10;
                }
            }
        }

        if((contentId & 0xF0) == 0x10) {
            return contentId;
        }

        for(String word : genreFilm) {
            if(subtitle.toLowerCase().contains(word.toLowerCase())) {
                return 0x10;
            }
        }

        return contentId;
    }

    static public boolean getSeasonEpisode(String text, SeasonEpisodeHolder holder) {
        if(TextUtils.isEmpty(text)) {
            return false;
        }

        for(Pattern seasonEpisodePattern : seasonEpisodePatterns) {
            Matcher matches = seasonEpisodePattern.matcher(text);

            if (matches.find()) {
                holder.season = parseInt(matches.group(1));
                holder.episode = parseInt(matches.group(2));
                return true;
            }
        }

        for(Pattern seasonEpisodePattern : episodeSeasonPatterns) {
            Matcher matches = seasonEpisodePattern.matcher(text);

            if (matches.find()) {
                holder.season = parseInt(matches.group(2));
                holder.episode = parseInt(matches.group(1));
                return true;
            }
        }

        for(Pattern seasonEpisodePattern : episodeOnlyPatterns) {
            Matcher matches = seasonEpisodePattern.matcher(text);

            if (matches.find()) {
                holder.season = 1;
                holder.episode = parseInt(matches.group(1));
                return true;
            }
        }

        return false;
    }

    void setStartTime(long startTime) {
        mStartTime = startTime;
    }

    public long getStartTime() {
        return mStartTime;
    }

    public Timestamp getTimestamp() {
        return new Timestamp(mStartTime * 1000);
    }

    @Override
    public String toString() {
        return "Event {" +
                "mContentId=\'" + mContentId + "\'" +
                ", mTitle=\'" + mTitle + "\'" +
                ", mSubTitle=\'" + mSubTitle + "\'" +
                ", mPlot=\'" + mPlot + "\'" +
                ", mDuration=\'" + mDuration + "\'" +
                ", mYear=\'" + mYear + "\'" +
                ", mEventId=\'" + mYear + "\'" +
                ", mStartTime=\'" + mYear + "\'" +
                ", mChannelUid=\'" + mYear + "\'" +
                "}";
    }

}
