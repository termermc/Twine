package net.termer.twine.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Random;

/**
 * String utility class
 * @author termer
 * @since 0.1
 */
public class StringFilter {
	/**
	 * A list of acceptable chars
	 * @since 1.0-alpha
	 */
	public static char[] acceptableChars = {'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','0','1','3','4','5','6','7','8','9','_'};
	
	/**
	 * Remove all unacceptable chars from a String
	 * @param str - the String to filter
	 * @return the filtered String
	 * @since 1.0-alpha
	 */
	public static String filter(String str) {
		String result = "";
		for(char ch : str.toLowerCase().toCharArray()) {
			boolean ok = false;
			for(char acceptableChar : acceptableChars) {
				if(ch==acceptableChar) {
					ok = true;
					break;
				}
			}
			if(ok) result+=ch;
		}
		return result;
	}
	
	/**
	 * Check if String only contains acceptable characters
	 * @param str - the String to check
	 * @return whether the String only contains acceptable characters
	 * @since 1.0-alpha
	 */
	public static boolean acceptableString(String str) {
		boolean ok = true;
		for(char ch : str.toLowerCase().toCharArray()) {
			boolean charOk = false;
			for(char acceptableChar : acceptableChars) {
				if(ch==acceptableChar) {
					charOk = true;
					break;
				}
			}
			if(!charOk) {
				ok = false;
				break;
			}
		}
		return ok;
	}
	
	/**
	 * Generate a String of the desired length using only acceptable characters
	 * @param length - the desired String length
	 * @return the generated String
	 * @since 1.0-alpha
	 */
	public static String generateString(int length) {
		String str = "";
		for(int i = 0; i < length; i++) {
			Random rand = new Random();
			str+=acceptableChars[rand.nextInt(acceptableChars.length-1)];
		}
		return str;
	}
	
	/**
	 * Check if all the chars in two Strings are the same
	 * @param str1 - the first String
	 * @param str2 - the second String
	 * @return whether the Strings are equivalent
	 * @since 1.0-alpha
	 */
	public static boolean same(String str1, String str2) {
		return str1.equals(str2);
	}
	
	/**
	 * Encode a String to be acceptable in a URI (including URLs)
	 * @param s - the String to encode
	 * @return the encoded String
	 * @since 1.0-alpha
	 */
	public static String encodeURIComponent(String s) {
	    String result;

	    try {
	        result = URLEncoder.encode(s, "UTF-8")
	                .replaceAll("\\+", "%20")
	                .replaceAll("\\%21", "!")
	                .replaceAll("\\%27", "'")
	                .replaceAll("\\%28", "(")
	                .replaceAll("\\%29", ")")
	                .replaceAll("\\%7E", "~");
	    } catch (UnsupportedEncodingException e) {
	        result = s;
	    }

	    return result;
	}
	
	/**
	 * Replaces all regular expression characters with their escaped versions
	 * @param regexString the String to escape
	 * @return the String with the escaped regex characters
	 * @since 1.0-alpha
	 */
	public static String escapeRegexCharacters(String regexString) {
		return regexString
				.replace("<", "\\<")
				.replace("(", "\\(")
				.replace("[", "\\[")
				.replace("{", "\\{")
				.replace("\\", "\\\\")
				.replace("^", "\\^")
				.replace("-", "\\-")
				.replace("=", "\\=")
				.replace("$", "\\$")
				.replace("!", "\\!")
				.replace("|", "\\|")
				.replace("]", "\\]")
				.replace("}", "\\}")
				.replace(")", "\\)")
				.replace("?", "\\?")
				.replace("*", "\\*")
				.replace("+", "\\+")
				.replace(".", "\\.")
				.replace(">", "\\>");
	}
	
	/**
	 * Removes all regular expression characters from the provided String
	 * @param regexString the String to process
	 * @return the String minus all regex characters
	 * @since 1.0-alpha
	 */
	public static String removeRegexCharacters(String regexString) {
		return regexString
				.replace("<", "")
				.replace("(", "")
				.replace("[", "")
				.replace("{", "")
				.replace("\\", "")
				.replace("^", "")
				.replace("-", "")
				.replace("=", "")
				.replace("$", "")
				.replace("!", "")
				.replace("|", "")
				.replace("]", "")
				.replace("}", "")
				.replace(")", "")
				.replace("?", "")
				.replace("*", "")
				.replace("+", "")
				.replace(".", "")
				.replace(">", "");
	}
	
	/**
	 * Escapes HTML by replacing less-than and greater-than symbols with &lt and &gt. 
	 * @param content the String to escape
	 * @return the escaped String
	 * @since 1.0-alpha
	 */
	public static String escapeHTML(String content) {
		return content
				.replace("&", "&amp;")
				.replace("\"", "&quot;")
				.replace("<", "&lt;")
				.replace(">", "&gt;");
	}
}