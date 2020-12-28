package org.robotv.client.model;

import android.annotation.SuppressLint;
import android.text.TextUtils;

import org.robotv.client.artwork.ArtworkHolder;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;

public class Event extends ArtworkHolder {

    public static class SeasonEpisodeHolder implements Serializable {
        public int season = 0;
        public int episode = 0;

        public boolean valid() {
            return season > 0 && episode > 0;
        }

        @Override
        public String toString() {
            return "SeasonEpisodeHolder {" +
                    "season='" + season + "'" +
                    ", episode='" + episode + "'" +
                    "}";
        }
    }

    private static class StringHolder {
        public String string;
    }

    private int contentId;
    private String shortText;
    private String description;
    private final int duration;
    private int year;
    private final int eventId;
    private long startTime;
    private int channelUid;
    private long vpsTime;
    private long parentalRating;
    private String posterUrl;
    private String backgroundUrl;

    private final SeasonEpisodeHolder seasonEpisode = new SeasonEpisodeHolder();

    private static final Pattern yearPattern = Pattern.compile("\\b(19|20)\\d{2}\\b");

    private static final Pattern[] seasonEpisodePatterns = {
            Pattern.compile("\\b(\\d).+[S|s]taffel.+[F|f]olge\\W+(\\d+)\\b"),
            Pattern.compile("\\b[S|s]taffel\\W+(\\d).+[F|f]olge\\W+(\\d+)\\b"),
            Pattern.compile("\\b[S|s]taffel\\W+(\\d).+[E|e]pisode\\W+(\\d+)\\b"),
            Pattern.compile("\\bS(\\d+).+E(\\d+)\\b"),
            Pattern.compile("\\bs\\W+(\\d+)\\W+ep\\W+(\\d+)\\b")
    };

    private static final Pattern[] episodeSeasonPatterns = {
            Pattern.compile("\\b(\\d).+[F|f]olge.+[S|s]taffel\\W++(\\d+)\\b"),
            Pattern.compile("\\bE(\\d+).+S(\\d+)\\b"),
            Pattern.compile("\\bE[P|p]\\.?\\W+(\\d+)+\\W+\\(?\\W+(\\d+)\\b\\)")
    };

    private static final Pattern[] episodeOnlyPatterns = {
            Pattern.compile("\\b[F|f]olge\\W+(\\d+)\\b"),
            Pattern.compile("\\b(\\d+)\\W+[F|f]olge\\b"),
            Pattern.compile("\\b[E|e]pisode\\W+(\\d)+\\b")
    };

    private static final String[] genreFilm = {
        "abenteuerfilm",
        "actiondrama",
        "actionfilm",
        "animationsfilm",
        "agentenfilm",
        "fantasy",
        "fantastisch",
        "fantasyfilm",
        "fantasy-action",
        "fantasy-film",
        "gangsterfilm",
        "heimatfilm",
        "horrorfilm",
        "historienfilm",
        "jugendfilm",
        "katastrophenfilm",
        "komödie",
        "kriegsfilm",
        "kriminalfilm",
        "liebesdrama",
        "liebesfilm",
        "martial arts",
        "monumentalfilm",
        "mysteryfilm",
        "politthriller",
        "roadmovie",
        "spielfilm",
        "thriller",
        "tragikomödie",
        "western",
        "zeichentrickfilm"
    };

    private static final String[] genreSoap = {
        "actionserie",
        "comedyserie",
        "comedy-serie",
        "crime-serie",
        "dramedyserie",
        "folge ",
        "jugendserie",
        "kinderserie",
        "kochshow",
        "krimireihe",
        "krimiserie",
        "krimi-serie",
        "kriminalserie",
        "krankenhausserie",
        "mysteryserie",
        "politserie",
        "polizeiserie",
        "serie",
        "sitcom",
        "thriller-serie",
        "unterhaltungsserie",
        "zeichentrickserie"
    };

    private static final String[] genreSoapOrMovie = {
        "abenteuer",
        "action",
        "animation",
        "comedy",
        "crime",
        "drama",
        "dramedy",
        "krimi",
        "mystery",
        "science-fiction",
        "scifi",
        "zeichentrick"
    };

    private static final String[] genreSportTitle = {
        "fußball",
        "fussball",
        "tennis",
        "slalom",
        "rtl",
        "abfahrt",
        "ski",
        "eishockey"
    };

    private static final int[] genreSportTitleId = {
        0x43,
        0x43,
        0x44,
        0x49,
        0x49,
        0x49,
        0x49,
        0x49
    };

    private final static int genreSoapMaxLength = 65 * 60; // 65 min

    public Event(Event event) {
        this(
            event.contentId,
            event.title,
            event.shortText,
            event.description,
            event.startTime,
            event.duration,
            event.eventId,
            event.channelUid,
            event.posterUrl,
            event.backgroundUrl
        );

        parentalRating = event.parentalRating;
        year = event.year;
        vpsTime = event.vpsTime;
        posterUrl = event.posterUrl;
        backgroundUrl = event.backgroundUrl;
    }

    public Event(int contentId, String title, String subTitle, String plot, long startTime, int durationSec) {
        this(contentId, title, subTitle, plot, startTime, durationSec, 0);
    }

    public Event(int contentId, String title, String subTitle, String plot, long startTime, int durationSec, int eventId) {
        this(contentId, title, subTitle, plot, startTime, durationSec, eventId, 0);
    }

    public Event(int contentId, String title, String subTitle, String plot, long startTime, int durationSec, int eventId, int channelUid) {
        this(contentId, title, subTitle, plot, startTime, durationSec, eventId, channelUid, "x", "x");
    }

    public Event(int contentId, String title, String subTitle, String plot, long startTime, int durationSec, int eventId, int channelUid, String posterUrl, String backgroundUrl) {
        super(posterUrl, backgroundUrl);

        this.contentId = guessGenreFromSubTitle(contentId, subTitle, durationSec);
        this.channelUid = channelUid;

        if(!TextUtils.isEmpty(title)) {
            this.title = title.trim();
        }

        if(!TextUtils.isEmpty(subTitle)) {
            this.shortText = subTitle.trim();
        }
        else {
            this.shortText = "";
        }

        // trim & replace special chars
        plot = (plot == null) ? "" : plot;
        plot = plot.trim().replace("\\n", " ");

        this.description = plot;

        this.duration = durationSec;
        this.eventId = eventId;
        this.startTime = startTime;

        // sometimes we can guess the sub genre from the title
        if(getGenre() == 0x40) {
            this.contentId = guessGenreFromSubTitle(contentId, title, durationSec);
        }

        this.year = guessYearFromDescription(subTitle + " " + plot);

        StringHolder resultHolder = new StringHolder();

        if (!getSeasonEpisode(this.description, seasonEpisode, resultHolder)) {
            if(getSeasonEpisode(this.shortText, seasonEpisode, resultHolder))  {
                this.shortText = resultHolder.string;
            }
        }
        else {
            this.description = resultHolder.string;
        }

        if(seasonEpisode.valid() && !isTvShow()) {
            this.contentId = 0x15;
        }

        if(!isTvShow() && guessSoapFromDescription(this.description)) {
            this.contentId = 0x15;
        }

        // user-defined (maybe movie) if duration > 70 min
        if(durationSec > 70 * 60) {
            this.contentId = 0xF0;

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
        return title == null ? "" : title;
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

    public long getParentalRating() {
        return parentalRating;
    }

    public void setParentalRating(long parentalRating) {
        this.parentalRating = parentalRating;
    }

    public void setChannelUid(int channelUid) {
        this.channelUid = channelUid;
    }

    public int getChannelUid() {
        return channelUid;
    }

    public String getDate() {
        return DateFormat
                .getDateInstance(DateFormat.MEDIUM)
                .format(new Date((getStartTime() * 1000)));
    }


    @SuppressLint("SimpleDateFormat")
    public String getTime() {
        return new SimpleDateFormat("HH:mm").format(new Date((getStartTime() * 1000)));
    }

    public String getDateTime() {
        return getDate() + " " + getTime();
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
        if(TextUtils.isEmpty(subtitle)) {
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

    private static boolean guessSoapFromDescription(String plot) {
        if(TextUtils.isEmpty(plot)) {
            return false;
        }

        plot = plot.toLowerCase();

        // try to guess soap from keywords
        for(String word : genreSoap) {
            if(plot.startsWith(word.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    private static String matchesCut(Matcher m) {
        m.reset();
        return m.replaceFirst("").trim().replaceFirst("^[\\W]+", "");
    }

    private static boolean getSeasonEpisode(String text, SeasonEpisodeHolder holder, StringHolder resultHolder) {
        if(TextUtils.isEmpty(text)) {
            return false;
        }

        for(Pattern seasonEpisodePattern : seasonEpisodePatterns) {
            Matcher matches = seasonEpisodePattern.matcher(text);

            if (matches.find()) {
                holder.season = parseInt(matches.group(1));
                holder.episode = parseInt(matches.group(2));

                if(resultHolder != null) {
                    resultHolder.string = matchesCut(matches);
                }

                return true;
            }
        }

        for(Pattern seasonEpisodePattern : episodeSeasonPatterns) {
            Matcher matches = seasonEpisodePattern.matcher(text);

            if (matches.find()) {
                holder.season = parseInt(matches.group(2));
                holder.episode = parseInt(matches.group(1));

                if(resultHolder != null) {
                    resultHolder.string = matchesCut(matches);
                }

                return true;
            }
        }

        for(Pattern seasonEpisodePattern : episodeOnlyPatterns) {
            Matcher matches = seasonEpisodePattern.matcher(text);

            if (matches.find()) {
                holder.season = 1;
                holder.episode = parseInt(matches.group(1));

                if(resultHolder != null) {
                    resultHolder.string = matchesCut(matches);
                }

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

    public long getEndTime() {
        return startTime + duration;
    }

    public Timestamp getTimestamp() {
        return new Timestamp(startTime * 1000);
    }

    public long getVpsTime() {
        return vpsTime;
    }

    public void setVpsTime(long vpsTime) {
        this.vpsTime = vpsTime;
    }

    @Override
    public String toString() {
        return "Event {" +
                "contentId='" + contentId + "'" +
                ", title='" + title + "'" +
                ", shortText='" + shortText + "'" +
                ", description='" + description + "'" +
                ", duration='" + duration + "'" +
                ", year='" + year + "'" +
                ", eventId='" + eventId + "'" +
                ", startTime='" + startTime + "'" +
                ", channelUid='" + channelUid + "'" +
                ", vpsTime='" + vpsTime + "'" +
                ", posterUrl='" + posterUrl + "'" +
                ", backgroundUrl='" + backgroundUrl + "'" +
                "}";
    }

}
