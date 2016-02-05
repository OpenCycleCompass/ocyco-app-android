package de.mytfg.jufo.ibis.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class Utils {

    /**
     * Reads an InputStream and converts it to a String.
     * @param stream the InputStream
     * @return string from the stream
     * @throws IOException
     */
    public static String readStream(InputStream stream) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(stream));
        StringBuilder total = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            total.append(line);
        }
        return total.toString();
    }

    /**
     * rounds double to two decimal places and return as string
     * @param d input double number
     * @return number rounded to tro decimal places as string
     */
    public static String roundDecimals(double d) {
        return String.format("%.2f", d);
    }

    /**
     * format time (interval)
     * @param ms time interval in milliseconds
     * @return sting with formatted time interval
     */
    public static String formatTime(long ms) {
        long hours = TimeUnit.MILLISECONDS.toHours(ms);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms);
        return String.format(
                "%02dh %02dmin %02ds",
                hours,
                minutes - TimeUnit.HOURS.toMinutes(hours),
                TimeUnit.MILLISECONDS.toSeconds(ms) - TimeUnit.MINUTES.toSeconds(minutes)
        );
    }

    /**
     * get date time in "dd.MM.yyyy HH:mm:ss" format from timestamp
     * @param ms timestamp (milliseconds)
     * @return formatted date time
     */
    public static String getDateTime(long ms) {
        // get date time in "dd.MM.yyyy HH:mm:ss" format
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.GERMAN);
        return sdf.format(new Date(ms));
    }

    /**
     * parse date time (sting) formatted as pattern (string)
     * @param pattern date time pattern (see {@link SimpleDateFormat})
     * @param date the date time string to parse
     * @return timestamp from date (milliseconds)
     */
    public static long parseDateTime(String pattern, String date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, Locale.GERMAN);
        Date convertedDate = new Date();
        try {
            convertedDate = dateFormat.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return convertedDate.getTime();
    }
}
