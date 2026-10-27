package util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/** Reusable validation for servlet request parameters. */
public final class InputValidator {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE = Pattern.compile("^[+()0-9 .-]{7,25}$");

    private InputValidator() { }

    public static boolean isValidEmail(String value) {
        return value != null && value.length() <= 254 && EMAIL.matcher(value.trim()).matches();
    }

    public static boolean isValidPhone(String value) {
        return value == null || value.isBlank() || PHONE.matcher(value.trim()).matches();
    }

    public static boolean isValidName(String value) {
        return value != null && !value.isBlank() && value.trim().length() <= 100;
    }

    public static boolean isValidStay(LocalDate checkIn, LocalDate checkOut) {
        return checkIn != null && checkOut != null && checkOut.isAfter(checkIn);
    }

    public static LocalDate parseDate(String value) {
        try { return LocalDate.parse(value); }
        catch (DateTimeParseException | NullPointerException e) { return null; }
    }

    public static Long parsePositiveId(String value) {
        try {
            long id = Long.parseLong(value);
            return id > 0 ? id : null;
        } catch (NumberFormatException | NullPointerException e) {
            return null;
        }
    }

    public static boolean isNonNegativeMoney(BigDecimal value) {
        return value != null && value.signum() >= 0;
    }
}
