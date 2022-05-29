package ad044.orps.util;

public class ParseUtil {

    public static boolean isBoolean(String str) {
        return str.equalsIgnoreCase("true") || str.equalsIgnoreCase("false");
    }

    public static boolean isUnsignedInt(String str) {
        return str.chars().allMatch(Character::isDigit);
    }
}
