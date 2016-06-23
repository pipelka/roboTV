package org.xvdr.robotv.artwork;

import java.sql.Timestamp;
import java.util.Calendar;

public class Event {

    private int mContentId;
    private String mTitle;
    private String mSubTitle;
    private String mPlot;
    private int mDuration;
    private int mYear;
    private int mEventId;
    private long mStartTime;
    private int mChannelUid;

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

    static public int guessYearFromDescription(String description) {
        String[] words = description.split("[\\.,;)| ]");
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        for(String word : words) {
            word = word.replaceAll("[^\\d]", "");

            // skip empty
            if(word.isEmpty()) {
                continue;
            }

            // skip all not beginning with 1...
            char c = word.charAt(0);

            if(c != '1') {
                continue;
            }

            int year;

            try {
                year = Integer.parseInt(word);
            }
            catch(Exception e) {
                year = 0;
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

    void setStartTime(long startTime) {
        mStartTime = startTime;
    }

    public long getStartTime() {
        return mStartTime;
    }

    public Timestamp getTimestamp() {
        return new Timestamp(mStartTime * 1000);
    }
}
