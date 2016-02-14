package org.xvdr.robotv.artwork;

import java.util.Calendar;

public class Event {

    private int mContentId;
    private String mTitle;
    private String mSubTitle;
    private String mPlot;
    private int mDuration;
    private int mYear;
    private int mEventId;

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

    private static int genreSoapMaxLength = 65 * 60; // 65 min

    public Event(int contentId, String title, String subTitle, String plot, int durationSec) {
        this(contentId, title, subTitle, plot, durationSec, 0);
    }

    public Event(int contentId, String title, String subTitle, String plot, int durationSec, int eventId) {
        mContentId = guessGenreFromSubtitle(contentId, subTitle, durationSec);
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

    static public int guessYearFromDescription(String description) {
        String[] words = description.split("[\\.,;)| ]");
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        for(String word: words) {
            word = word.replaceAll("[^\\d]", "");
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

    static int guessGenreFromSubtitle(int contentId, String subtitle, int duration) {
        if(subtitle == null) {
            return contentId;
        }

        // try to guess soap from subtitle keywords
        for(String word: genreSoap) {
            if(subtitle.toLowerCase().contains(word.toLowerCase())) {
                return 0x15;
            }
        }

        // try to guess soap or movie from subtitle keywords and event duration
        if(duration > 0) {
            for (String word : genreSoapOrMovie) {
                if (subtitle.toLowerCase().contains(word.toLowerCase())) {
                    return duration < genreSoapMaxLength ? 0x15 : 0x10;
                }
            }
        }

        if((contentId & 0xF0) == 0x10) {
            return contentId;
        }

        for(String word: genreFilm) {
            if(subtitle.toLowerCase().contains(word.toLowerCase())) {
                return 0x10;
            }
        }

        return contentId;
    }
}
