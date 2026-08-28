package utility;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Common date and time helper methods.
 * @author Chang Han Yean
 */
public class DateUtils {
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateUtils() {
    }

    public static String getCurrentTimestamp() {
        return LocalDateTime.now().format(TIMESTAMP_FORMAT);
    }

    public static String getTodayDate() {
        return LocalDate.now().toString();
    }

    public static String getTodayBasicDate() {
        return LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    public static boolean isTodayTimestamp(String timestamp) {
        if (timestamp == null || timestamp.equalsIgnoreCase("N/A")) {
            return false;
        }
        return timestamp.startsWith(LocalDate.now().toString());
    }

    public static LocalDate parseDate(String date) {
        return LocalDate.parse(date);
    }

    public static boolean isBeforeToday(String date) {
        return parseDate(date).isBefore(LocalDate.now());
    }

    public static boolean isAfter(String firstDate, String secondDate) {
        return parseDate(firstDate).isAfter(parseDate(secondDate));
    }

    public static boolean isBefore(String firstDate, String secondDate) {
        return parseDate(firstDate).isBefore(parseDate(secondDate));
    }

    public static boolean isDateWithinStay(String date, String checkInDate, String checkOutDate) {
        LocalDate target = parseDate(date);
        LocalDate checkIn = parseDate(checkInDate);
        LocalDate checkOut = parseDate(checkOutDate);
        return !target.isBefore(checkIn) && target.isBefore(checkOut);
    }

    public static boolean isDateOverlap(String firstCheckIn, String firstCheckOut,
            String secondCheckIn, String secondCheckOut) {
        LocalDate firstIn = parseDate(firstCheckIn);
        LocalDate firstOut = parseDate(firstCheckOut);
        LocalDate secondIn = parseDate(secondCheckIn);
        LocalDate secondOut = parseDate(secondCheckOut);
        return firstIn.isBefore(secondOut) && firstOut.isAfter(secondIn);
    }

    public static int countNights(String checkInDate, String checkOutDate) {
        return (int) ChronoUnit.DAYS.between(parseDate(checkInDate), parseDate(checkOutDate));
    }
}
