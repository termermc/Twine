package net.termer.twine.utils;

/**
 * Utility class to deal with primitives
 * @since 1.5
 */
public class PrimitiveUtils {
    /**
     * Checks if the provided String represents a boolean
     * @param str the String to check
     * @return Whether the String represents a boolean
     * @since 1.5
     */
    public static boolean isBoolean(String str) {
        return str != null && str.equals("true") || str.equals("false");
    }

    /**
     * Checks if the provided String represents a byte
     * @param str the String to check
     * @return Whether the String represents a byte
     * @since 1.5
     */
    public static boolean isByte(String str) {
        try {
            Byte.parseByte(str);
            return true;
        } catch(NumberFormatException e) {
            return false;
        }
    }

    /**
     * Checks if the provided String represents a short
     * @param str the String to check
     * @return Whether the String represents a short
     * @since 1.5
     */
    public static boolean isShort(String str) {
        try {
            Short.parseShort(str);
            return true;
        } catch(NumberFormatException e) {
            return false;
        }
    }

    /**
     * Checks if the provided String represents an int
     * @param str the String to check
     * @return Whether the String represents an int
     * @since 1.5
     */
    public static boolean isInt(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch(NumberFormatException e) {
            return false;
        }
    }

    /**
     * Checks if the provided String represents a long
     * @param str the String to check
     * @return Whether the String represents a long
     * @since 1.5
     */
    public static boolean isLong(String str) {
        try {
            Long.parseLong(str);
            return true;
        } catch(NumberFormatException e) {
            return false;
        }
    }

    /**
     * Checks if the provided String represents a float
     * @param str the String to check
     * @return Whether the String represents a float
     * @since 1.5
     */
    public static boolean isFloat(String str) {
        try {
            Float.parseFloat(str);
            return true;
        } catch(NumberFormatException e) {
            return false;
        }
    }

    /**
     * Checks if the provided String represents a double
     * @param str the String to check
     * @return Whether the String represents a double
     * @since 1.5
     */
    public static boolean isDouble(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch(NumberFormatException e) {
            return false;
        }
    }
}