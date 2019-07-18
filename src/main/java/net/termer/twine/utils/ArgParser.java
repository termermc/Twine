package net.termer.twine.utils;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Utility class to parse command line arguments.
 * @author Ian Byrd
 * @since 1.0-alpha
 */
public class ArgParser {
	private HashMap<String, String> _options = new HashMap<String, String>();
	private Character[] _flags;
	private String[] _args;
	
	/**
	 * Instantiates a new ArgParser object and parses the provided arguments.
	 * @param args the command line arguments to parse
	 * @since 1.0-alpha
	 */
	public ArgParser(String[] args) {
		ArrayList<Character> flags = new ArrayList<Character>();
		ArrayList<String> tmpArgs = new ArrayList<String>();
		
		for(String arg : args) {
			if(arg.startsWith("--") && arg.length() > 2) {
				// Option argument
				String argStr = arg.substring(2);
				String valStr = null;
				
				// Check if option has value and is valid
				if(argStr.contains("=") && argStr.length() > argStr.indexOf('=')+1) {
					valStr = argStr.substring(argStr.indexOf('=')+1);
					argStr = argStr.split("=")[0];
				}
				
				_options.put(argStr, valStr);
			} else if(arg.startsWith("-") && arg.length() > 1 && arg.charAt(1) != '-') {
				// Flag argument
				String flagStr = arg.substring(1);
				
				// Add all characters in flag argument as flags
				for(char fchar : flagStr.toCharArray()) {
					if(!flags.contains(fchar))
						flags.add(fchar);
				}
			} else {
				// Normal argument
				tmpArgs.add(arg);
			}
		}
		
		// Assign values
		_flags = flags.toArray(new Character[0]);
		_args = tmpArgs.toArray(new String[0]);
	}
	
	/**
	 * Returns all normal (non-flag/option arguments).
	 * @return all normal arguments
	 * @since 1.0-alpha
	 */
	public String[] arguments() {
		return _args;
	}
	/**
	 * Returns all flag arguments.
	 * @return all flag arguments
	 * @since 1.0-alpha
	 */
	public Character[] flags() {
		return _flags;
	}
	/**
	 * Returns all options as a HashMap.
	 * Option arguments that don't have a value will have null set as their value in the HashMap.
	 * @return all options
	 * @since 1.0-alpha
	 */
	public HashMap<String, String> options() {
		return _options;
	}
	
	/**
	 * Returns whether the specified option exists.
	 * @param name the name of the option
	 * @return whether the option exists
	 * @since 1.0-alpha
	 */
	public boolean option(String name) {
		return _options.containsKey(name);
	}
	/**
	 * Returns whether the specified flag exists.
	 * @param flag the flag char
	 * @return whether the flag exists
	 * @since 1.0-alpha
	 */
	public boolean flag(char flag) {
		boolean exists = false;
		
		for(char f : _flags) {
			if(f == flag) {
				exists = true;
				break;
			}
		}
		
		return exists;
	}
	
	/**
	 * Returns the value of the specified option as a String.
	 * Returns null if the option does not have a value.
	 * @param option the name of the option
	 * @return the value of the option
	 * @since 1.0-alpha
	 */
	public String optionString(String option) {
		return _options.get(option);
	}
	/**
	 * Returns the value of the specified option as an int.
	 * Returns null if the option does not have a value.
	 * If the value of the option is not an int, this method will throw a NumberFormatException.
	 * @param option the name of the option
	 * @return the value of the option
	 * @since 1.0-alpha
	 */
	public int optionInt(String option) {
		return Integer.parseInt(_options.get(option));
	}
	/**
	 * Returns the value of the specified option as a char.
	 * Returns null if the option does not have a value.
	 * @param option the name of the option
	 * @return the value of the option
	 * @since 1.0-alpha
	 */
	public char optionChar(String option) {
		return _options.get(option).charAt(0);
	}
	/**
	 * Returns the value of the specified option as a double.
	 * Returns null if the option does not have a value.
	 * If the value of the option is not a double, this method will throw a NumberFormatException.
	 * @param option the name of the option
	 * @return the value of the option
	 * @since 1.0-alpha
	 */
	public double optionDouble(String option) {
		return Double.parseDouble(_options.get(option));
	}
	
	/**
	 * Returns whether the specified option has a value.
	 * @param option the name of the option
	 * @return whether the option has a value
	 * @since 1.0-alpha
	 */
	public boolean hasValue(String option) {
		return _options.get(option) != null;
	}
}