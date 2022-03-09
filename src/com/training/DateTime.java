package com.training;

import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Comparator.comparingInt;

public class DateTime {

    //The java.time package is based on the Joda-Time library
    // The new package was developed under JSR-310: Date and Time API, and supports the ISO 8601 standard.
    // It correctly adjusts for leap years and daylight savings rules in individual regions.
    // Work with the factory methods in classes like
    // Instant,
    // Duration,
    // Period,
    // LocalDate,
    // LocalTime,
    // LocalDateTime,
    // ZonedDateTime

    //For dates, the basic format is yyyy-MM-dd. For times, the format is hh:mm:ss.sss

    /*
       Instant.now():       2017-06-20T17:27:08.184Z
       LocalDate.now():     2017-06-20
       LocalTime.now():     13:27:08.318
       LocalDateTime.now(): 2017-06-20T13:27:08.319

       ZonedDateTime.now(): 2017-06-20T13:27:08.319-04:00[America/New_York]
        *There are two types of zone IDs:
            1. Fixed offsets, relative to UTC/Greenwich, like -05:00
            2. Geographical regions, like America/Chicago
        The rules for offset changes come from the ZoneRules class, where the rules are loaded from a
        ZoneRulesProvider. The ZoneRules class has methods such as isDaylightSavings(Instant)
       */

    private static void tryLocal() {
        LocalDate moonLandingDate = LocalDate.of(1969, Month.JULY, 20); // 1969-07-20
        LocalTime moonLandingTime = LocalTime.of(20, 18); //  20:18

        LocalTime walkTime = LocalTime.of(20, 2, 56, 150_000_000);
        LocalDateTime walk = LocalDateTime.of(moonLandingDate, walkTime); // 1969-07-20T20:02:56.150


        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate start = LocalDate.of(2017, Month.FEBRUARY, 2);
        LocalDate end = start.plusDays(3);
        end.format(formatter);
        // start.plusWeeks(5);  start.plusMonths(7); start.plusYears(2);

        DateTimeFormatter formatter2 = DateTimeFormatter.ISO_LOCAL_TIME;
        LocalTime start2 = LocalTime.of(11, 30, 0, 0);
        LocalTime end2 = start2.plusNanos(1_000_000);
        end2.format(formatter2);
        // start2.plusSeconds(20);  start2.plusMinutes(45); start2.plusHours(5);


        Period period = Period.of(2, 3, 4); // 2 years, 3 months, 4 days
        LocalDateTime start3 = LocalDateTime.of(2017, Month.FEBRUARY, 2, 11, 30);
        LocalDateTime end3 = start3.plus(period);
        display("2019-05-06T11:30:00", end3.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        end3 = start3.plus(3, ChronoUnit.HALF_DAYS);
        display("2017-02-03T23:30:00", end3.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        end3 = start3.minus(period);
        display("2014-10-29T11:30:00", end3.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        end3 = start3.minus(2, ChronoUnit.CENTURIES);
        display("1817-02-02T11:30:00", end3.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        end3 = start3.plus(3, ChronoUnit.MILLENNIA);
        display("5017-02-02T11:30:00", end3.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

    }

    private static void tryInstant() {
        var now = Instant.now();
        System.out.println(Year.now());
        System.out.println(Month.JANUARY.getValue());

    }

    private static void tryZone() {
        //The complete list of available region IDs comes from the static getAvailableZoneIds method
        Set<String> zones = ZoneId.getAvailableZoneIds();

        LocalDateTime dateTime = LocalDateTime.of(2017, Month.JULY, 4, 13, 20, 10);
        ZonedDateTime nyc = dateTime.atZone(ZoneId.of("America/New_York"));
        //2017-07-04T13:20:10-04:00[America/New_York]

        //the withZoneSameInstant method allows you to take one ZonedDateTime and find out what it would be in another time zone.
        ZonedDateTime london = nyc.withZoneSameInstant(ZoneId.of("Europe/London"));
        //2017-07-04T18:20:10+01:00[Europe/London]

    }

    public void temporalField() {
        //The version with TemporalField lets the field resolve the date to make it valid. For instance,
        // takes the last day of January and tries to change the month to February. According to the Javadocs,
        // the system chooses the previous valid date, which in this case is the last day of February.

        LocalDateTime start = LocalDateTime.of(2017, Month.JANUARY, 31, 11, 30);
        LocalDateTime end = start.with(ChronoField.MONTH_OF_YEAR, 2);
        display("2017-02-28T11:30:00", end.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    public void adjusters() throws Exception {
        LocalDateTime start = LocalDateTime.of(2017, Month.FEBRUARY, 2, 11, 30);
        LocalDateTime end = start.with(TemporalAdjusters.firstDayOfNextMonth());
        // assertEquals("2017-03-01T11:30", end.toString());

        end = start.with(TemporalAdjusters.next(DayOfWeek.THURSDAY));
        // assertEquals("2017-02-09T11:30", end.toString());

        end = start.with(TemporalAdjusters.previousOrSame(DayOfWeek.THURSDAY));
        // assertEquals("2017-02-02T11:30", end.toString());
    }

    public class PaydayAdjuster {
        public static Temporal adjustInto(Temporal input) {
            LocalDate date = LocalDate.from(input);
            int day = date.getDayOfMonth() < 15 ? 15 : date.with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth();

            date = date.withDayOfMonth(day);
            if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                date = date.with(TemporalAdjusters.previous(DayOfWeek.FRIDAY));
            }

            return input.with(date);
        }
    }

    public static void payDayWithMethodRef() {

        IntStream.rangeClosed(1, 14)
                .mapToObj(day -> LocalDate.of(2017, Month.JULY, day))
                .forEach(date -> System.out.println(date.with(PaydayAdjuster::adjustInto).getDayOfMonth() + " == 14"));

        IntStream.rangeClosed(15, 31)
                .mapToObj(day -> LocalDate.of(2017, Month.JULY, day))
                .forEach(date -> System.out.println(date.with(PaydayAdjuster::adjustInto).getDayOfMonth() + " == 31"));
    }

    //The TemporalQuery interface is used as the argument to the query method on temporal objects.
    // Method to calculate days until Talk Like A Pirate Day
    private long daysUntilPirateDay(TemporalAccessor temporal) {
        int day = temporal.get(ChronoField.DAY_OF_MONTH);
        int month = temporal.get(ChronoField.MONTH_OF_YEAR);
        int year = temporal.get(ChronoField.YEAR);
        LocalDate date = LocalDate.of(year, month, day);
        LocalDate tlapd = LocalDate.of(year, Month.SEPTEMBER, 19);
        if (date.isAfter(tlapd)) {
            tlapd = tlapd.plusYears(1);
        }
        return ChronoUnit.DAYS.between(date, tlapd);
    }

    //Using a TemporalQuery via a method reference
    public void pirateDay() {
        IntStream.range(10, 19)
                .mapToObj(n -> LocalDate.of(2017, Month.SEPTEMBER, n))
                .forEach(date -> System.out.println(date.query(this::daysUntilPirateDay) <= 9));

        IntStream.rangeClosed(20, 30)
                .mapToObj(n -> LocalDate.of(2017, Month.SEPTEMBER, n))
                .forEach(date -> {
                    Long days = date.query(this::daysUntilPirateDay);
                    System.out.println(days >= 354 && days < 365);
                });
    }

    // Converting java.util.Date to java.time.LocalDate via Instant
    public LocalDate convertFromUtilDateUsingInstant(Date date) {
        //Since java.util.Date includes date and time information but no time zone,6 it represents an
        // Instant in the new API. Applying the atZone method on the system default time zone reapplies
        // the time zone. Then you can extract the LocalDate from the resulting ZonedDateTime.
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    //Converting java.util classes to java.time classes
    public class ConvertDate {
        public LocalDate convertFromSqlDatetoLD(java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }

        public java.sql.Date convertToSqlDateFromLD(LocalDate localDate) {
            return java.sql.Date.valueOf(localDate);
        }

        public LocalDateTime convertFromTimestampToLDT(Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }

        public Timestamp convertToTimestampFromLDT(LocalDateTime localDateTime) {
            return Timestamp.valueOf(localDateTime);
        }
    }

    // Converting a java.util.Date to a java.time.LocalDate
    public LocalDate convertUtilDateToLocalDate(java.util.Date date) {
        return new java.sql.Date(date.getTime()).toLocalDate();
    }

    // Converting from java.util.Calendar to java.time.ZonedDateTime
    public ZonedDateTime convertFromCalendar(Calendar cal) {
        return ZonedDateTime.ofInstant(cal.toInstant(), cal.getTimeZone().toZoneId());
    }

    // Using getter methods from Calendar to LocalDateTime
    public LocalDateTime convertFromCalendarUsingGetters(Calendar cal) {
        return LocalDateTime.of(cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.HOUR),
                cal.get(Calendar.MINUTE),
                cal.get(Calendar.SECOND));
    }


    // Generating and parsing a timestamp string
    public LocalDateTime convertFromUtilDateToLDUsingString(Date date) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return LocalDateTime.parse(df.format(date), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    // Converting java.util.Dat to java.time.LocalDate
    public LocalDate convertFromUtilDate(Date date) {
        return LocalDate.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    // Parsing and Formatting
    public static void parsingAndFormatting() {
        LocalDate date = LocalDate.of(2017, Month.MARCH, 13);

        System.out.println("Full   : " + date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))); // Monday, March 13, 2017
        System.out.println("Long   : " + date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))); // March 13, 2017
        System.out.println("Medium : " + date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))); // Mar 13, 2017
        System.out.println("Short  : " + date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))); // 3/13/17
        System.out.println("France : " + date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                .withLocale(Locale.FRANCE))); //lundi 13 mars 2017
        System.out.println("India  : " + date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                .withLocale(new Locale("hin", "IN")))); //Monday, March 13, 2017
        System.out.println("Brazil : " + date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                .withLocale(new Locale("pt", "BR")))); //Segunda-feira, 13 de Março de 2017
        System.out.println("Japan  : " + date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                .withLocale(Locale.JAPAN))); //2017年3月13日
        Locale loc = new Locale.Builder().setLanguage("sr").setScript("Latn").setRegion("RS").build();
        System.out.println("Serbian: " + date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                .withLocale(loc))); //ponedeljak, 13. mart 2017.


        // Defining your own format pattern
        ZonedDateTime moonLanding = ZonedDateTime.of(
                LocalDate.of(1969, Month.JULY, 20), LocalTime.of(20, 18), ZoneId.of("UTC"));
        System.out.println(moonLanding.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)); // 1969-07-20T20:18:00Z[UTC]

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu/MMMM/dd hh:mm:ss a zzz GG");
        System.out.println(moonLanding.format(formatter)); // 1969/July/20 08:18:00 PM UTC AD

        formatter = DateTimeFormatter.ofPattern("uuuu/MMMM/dd hh:mm:ss a VV xxxxx");
        System.out.println(moonLanding.format(formatter)); //1969/July/20 08:18:00 PM UTC +00:00

    }

    //Finding Time Zones with Unusual Offsets
    public static void FunnyOffsets() {
        Instant instant = Instant.now();
        ZonedDateTime current = instant.atZone(ZoneId.systemDefault());
        System.out.printf("Current time is %s%n%n", current);

        System.out.printf("%10s %20s %13s%n", "Offset", "ZoneId", "Time");
        ZoneId.getAvailableZoneIds().stream()
                .map(ZoneId::of)
                .filter(zoneId -> {
                    ZoneOffset offset = instant.atZone(zoneId).getOffset();
                    return offset.getTotalSeconds() % (60 * 60) != 0;
                })
                .sorted(comparingInt(zoneId -> instant.atZone(zoneId).getOffset().getTotalSeconds()))
                .forEach(zoneId -> {
                    ZonedDateTime zdt = current.withZoneSameInstant(zoneId);
                    System.out.printf("%10s %25s %10s%n",
                            zdt.getOffset(), zoneId,
                            zdt.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)));
                });

            /*
            Time zones offset by non-hour amounts
            Current time is 2016-08-08T23:12:44.264-04:00[America/New_York]

                    Offset               ZoneId          Time
                    -09:30         Pacific/Marquesas    5:42 PM
                    -04:30           America/Caracas   10:42 PM
                    -02:30          America/St_Johns   12:42 AM
                    -02:30       Canada/Newfoundland   12:42 AM
                    +04:30                      Iran    7:42 AM
                    +04:30               Asia/Tehran    7:42 AM
                    +04:30                Asia/Kabul    7:42 AM
                    +05:30              Asia/Kolkata    8:42 AM
                    +05:30              Asia/Colombo    8:42 AM
                    +05:30             Asia/Calcutta    8:42 AM
                    +05:45            Asia/Kathmandu    8:57 AM
                    +05:45             Asia/Katmandu    8:57 AM
                    +06:30              Asia/Rangoon    9:42 AM
                    +06:30              Indian/Cocos    9:42 AM
                    +08:45           Australia/Eucla   11:57 AM
                    +09:30           Australia/North   12:42 PM
                    +09:30      Australia/Yancowinna   12:42 PM
                    +09:30        Australia/Adelaide   12:42 PM
                    +09:30     Australia/Broken_Hill   12:42 PM
                    +09:30           Australia/South   12:42 PM
                    +09:30          Australia/Darwin   12:42 PM
                    +10:30       Australia/Lord_Howe    1:42 PM
                    +10:30             Australia/LHI    1:42 PM
                    +11:30           Pacific/Norfolk    2:42 PM
                    +12:45                   NZ-CHAT    3:57 PM
                    +12:45           Pacific/Chatham    3:57 PM
                */
    }

    //Finding Region Names from Offsets

    //   Getting region names given an offset
    public static List<String> getRegionNamesForOffset(ZoneOffset offset) {
        LocalDateTime now = LocalDateTime.now();
        return ZoneId.getAvailableZoneIds().stream()
                .map(ZoneId::of)
                .filter(zoneId -> now.atZone(zoneId).getOffset().equals(offset))
                .map(ZoneId::toString)
                .sorted()
                .collect(Collectors.toList());
    }

    // Get region names for a given offset
    public static List<String> getRegionNamesForZoneId(ZoneId zoneId) {
        LocalDateTime now = LocalDateTime.now();
        ZonedDateTime zdt = now.atZone(zoneId);
        ZoneOffset offset = zdt.getOffset();

        return getRegionNamesForOffset(offset);
    }
    // This works for any given ZoneId.

    // Getting the current region names
    public void getRegionNamesForSystemDefault() {
        ZonedDateTime now = ZonedDateTime.now();
        ZoneId zoneId = now.getZone();
        List<String> names = getRegionNamesForZoneId(zoneId);

        System.out.println(names.contains(zoneId.getId()));
    }

    //  Getting region names given an hour and minute offset
    public static List<String> getRegionNamesForOffset(int hours, int minutes) {
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(hours, minutes);
        return getRegionNamesForOffset(offset);
    }

    // Testing region names for a given offset
    public void getRegionNamesForGMT() throws Exception {
        List<String> names = getRegionNamesForOffset(0, 0);

        System.out.println(names.contains("GMT"));
        System.out.println(names.contains("Etc/GMT"));
        System.out.println(names.contains("Etc/UTC"));
        System.out.println(names.contains("UTC"));
        System.out.println(names.contains("Etc/Zulu"));
    }

    public void getRegionNamesForNepal() throws Exception {
        List<String> names = getRegionNamesForOffset(5, 45);

        System.out.println(names.contains("Asia/Kathmandu"));
        System.out.println(names.contains("Asia/Katmandu"));
    }

    public void getRegionNamesForChicago() throws Exception {
        ZoneId chicago = ZoneId.of("America/Chicago");
        List<String> names = getRegionNamesForZoneId(chicago);

        System.out.println(names.contains("America/Chicago"));
        System.out.println(names.contains("US/Central"));
        System.out.println(names.contains("Canada/Central"));
        System.out.println(names.contains("Etc/GMT+5") || names.contains("Etc/GMT+6"));
    }
    //A complete list of region names can be found in Wikipedia at
    // https://en.wikipedia.org/wiki/List_of_tz_database_time_zones.


    //Time Between Events
    //Days to Election Day
    /*
        LocalDate electionDay = LocalDate.of(2020, Month.NOVEMBER, 3);
        LocalDate today = LocalDate.now();

        System.out.printf("%d day(s) to go...%n",  ChronoUnit.DAYS.between(today, electionDay));
    */

    //Using the Period class
    //If you’re interested in a breakdown into years, months, and days, use the Period class.

    //  Using Period to get days, months, and years
    /*
    LocalDate electionDay = LocalDate.of(2020, Month.NOVEMBER, 3);
    LocalDate today = LocalDate.now();

    Period until = today.until(electionDay);

    years  = until.getYears();
    months = until.getMonths();
    days   = until.getDays();
    System.out.printf("%d year(s), %d month(s), and %d day(s)%n",   years, months, days);

        // Equivalent to Period.between(today, electionDay)
*/



   // Using the Duration class
  //  The Duration class represents an amount of time in terms of seconds and nanoseconds, which makes it suitable for
    //  working with Instant.

    /*
   // Timing a method
        public static double getTiming(Instant start, Instant end) {
            return Duration.between(start, end).toMillis() / 1000.0;
        }

        Instant start = Instant.now();
        // ... call method to be timed ...
        Instant end = Instant.now();
        System.out.println(getTiming(start, end) + " seconds");
       // This is a “poor developer’s” approach to timing a method, but it is easy.

        The Duration class has conversion methods: toDays, toHours, toMillis, toMinutes, and toNanos,
        which is why the getTiming method in Example used toMillis and divided by 1,000.
     */

    //Because the java.time classes are immutable, any instance method that seems to modify one,
    // like plus, minus, or with, produces a new instance.

    public static void main(String[] args) {
//        display("Days in Feb in a leap year: 29" +  Month.FEBRUARY.length(true));
//        display("Day of year for first day of Aug (leap year): 214" + Month.AUGUST.firstDayOfYear(true));
//        display("Month.of(1): JANUARY" + Month.of(1));
//        display("Adding two months: MARCH" + Month.JANUARY.plus(2));
//        display("Subtracting a month: FEBRUARY" + Month.MARCH.minus(1));

        payDayWithMethodRef();
    }

    private static void display(Object... object) {
        System.out.println(object);
    }

}

