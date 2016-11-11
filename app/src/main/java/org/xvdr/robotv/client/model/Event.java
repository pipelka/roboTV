package org.xvdr.robotv.client.model;

import android.text.TextUtils;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;

public class Event implements Serializable {

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

    private int contentId;
    private String title;
    private String shortText;
    private String description;
    private int duration;
    private int year;
    private int eventId;
    private long startTime;
    private int channelUid;

    private SeasonEpisodeHolder seasonEpisode = new SeasonEpisodeHolder();

    private static Pattern yearPattern = Pattern.compile("\\b(19|20)\\d{2}\\b");

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
        this.contentId = guessGenreFromSubTitle(contentId, subTitle, durationSec);
        this.channelUid = channelUid;

        // sometimes we can guess the sub genre from the title
        if(getGenre() == 0x40) {
            this.contentId = guessGenreFromSubTitle(contentId, title, durationSec);
        }

        year = guessYearFromDescription(subTitle + " " + plot);
        this.title = title;
        shortText = subTitle;
        description = plot;
        duration = durationSec;
        this.eventId = eventId;

        if (!getSeasonEpisode(plot, seasonEpisode)) {
            getSeasonEpisode(subTitle, seasonEpisode);
        }

        if(seasonEpisode.valid() && !isTvShow()) {
            this.contentId = 0x15;
        }
    }

    public void setShortText(String outline) {
        shortText = outline;
    }

    public int getGenre() {
        return contentId & 0xF0;
    }

    public int getContentId() {
        return contentId;
    }

    public int getYear() {
        return year;
    }

    public String getTitle() {
        return title;
    }

    public String getShortText() {
        return shortText;
    }

    public String getDescription() {
        return description;
    }

    public int getDuration() {
        return duration;
    }

    public int getEventId() {
        return eventId;
    }

    public int getChannelUid() {
        return channelUid;
    }

    public boolean isTvShow() {
        return (getContentId() == 0x15 || getContentId() == 0x23);
    }

    public SeasonEpisodeHolder getSeasionEpisode() {
        return seasonEpisode;
    }

    private static int guessYearFromDescription(String description) {
        int year;
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        Matcher matches = yearPattern.matcher(description);

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

    private static int guessGenreFromSubTitle(int contentId, String subtitle, int duration) {
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

    private static boolean getSeasonEpisode(String text, SeasonEpisodeHolder holder) {
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

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public Timestamp getTimestamp() {
        return new Timestamp(startTime * 1000);
    }

    @Override
    public String toString() {
        return "Event {" +
                "contentId=\'" + contentId + "\'" +
                ", title=\'" + title + "\'" +
                ", shortText=\'" + shortText + "\'" +
                ", description=\'" + description + "\'" +
                ", duration=\'" + duration + "\'" +
                ", year=\'" + year + "\'" +
                ", eventId=\'" + year + "\'" +
                ", startTime=\'" + year + "\'" +
                ", channelUid=\'" + year + "\'" +
                "}";
    }

}
