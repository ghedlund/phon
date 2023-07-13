package ca.phon.formatter;

import java.text.ParseException;

/**
 * Media time formatter for numbers
 */
public class MediaTimeFormatter implements Formatter<Number> {

    /**
     * Return times in minutes and seconds (short form: e.g., 1:3.5)
     *
     * @param number if number is a float times is interpreted as a values in seconds, milliseconds otherwise
     * @return formatted time value
     */
    public static String timeToMinutesAndSeconds(Number number) {
        return (new MediaTimeFormatter()).format(number);
    }

    public static String timeToPaddedMinutesAndSeconds(Number number) {
        return (new MediaTimeFormatter(MediaTimeFormatStyle.PADDED_MINUTES_AND_SECONDS)).format(number);
    }

    public static String timeToMilliseconds(Number number) {
        return (new MediaTimeFormatter(MediaTimeFormatStyle.MILLISECONDS)).format(number);
    }

    public static float parseTimeToSeconds(String text) throws ParseException {
        return parseTimeToMilliseconds(text) / 1000.0f;
    }

    public static long parseTimeToMilliseconds(String text) throws ParseException {
        long timeInMs = (new MediaTimeFormatter()).parse(text).longValue();
        return timeInMs;
    }

    private final MediaTimeFormatStyle formatStyle;

    public MediaTimeFormatter() {
        this(MediaTimeFormatStyle.MINUTES_AND_SECONDS);
    }

    public MediaTimeFormatter(MediaTimeFormatStyle formatStyle) {
        this.formatStyle = formatStyle;
    }

    @Override
    public String format(Number obj) {
        MediaTimeFormat format = new MediaTimeFormat(formatStyle);
        return format.format(obj);
    }

    @Override
    public Number parse(String text) throws ParseException {
        MediaTimeFormat format = new MediaTimeFormat();
        return (Long)format.parseObject(text);
    }

}
