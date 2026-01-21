package net.renars.orbital.utils;

public class StringUtils {
    // RFC 5322 regex email validēšana --Renars
    public static boolean isValidEmail(String email) {
        String regex = "^[a-zA-Z0-9_!#$%&’*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$";
        return email != null && email.matches(regex);
    }
}
