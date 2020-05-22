package net.termer.twine.utils;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Utility class to parse command line arguments.
 * @author termer
 * @since 1.0-alpha
 */
public class ArgParser {
	private HashMap<String, ArrayList<String>> _options = new HashMap<String, ArrayList<String>>();
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

				if(!_options.containsKey(argStr))
					_options.put(argStr, new ArrayList<String>());
				if(valStr != null && !_options.get(argStr).contains(valStr))
					_options.get(argStr).add(valStr);
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
	 * Returns all options as a HashMap of String ArrayLists.
	 * Option arguments that don't have a value will have null set as their value in the HashMap.
	 * @return all options
	 * @since 1.0-alpha
	 */
	public HashMap<String, ArrayList<String>> options() {
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
	 * Returns all values for the specified option.
	 * Returns an empty array if the option has no values, null if the option does not exist.
	 * @param option the option to get values from
	 * @return all values for the specified option
	 * @since 1.5
	 */
	public String[] optionValues(String option) {
		String[] vals = null;

		if(_options.containsKey(option))
			vals = _options.get(option).toArray(new String[0]);

	    return vals;
    }
	/**
	 * Returns the value of the specified option as a String.
	 * Returns null if the option does not have a value.
	 * @param option the name of the option
	 * @return the value of the option
     * @deprecated Use optionString(option, index)
	 * @since 1.0-alpha
	 */
	@Deprecated
	public String optionString(String option) {
	    String str = null;

	    if(_options.containsKey(option) && _options.get(option).size() > 0)
		    str = _options.get(option).get(0);

	    return str;
	}
    /**
     * Returns the value of the specified option at the specified index as a String
     * @param option the name of the option
     * @param index the index of the option
     * @return the value of the option at the specified index
     * @since 1.5
     */
	public String optionString(String option, int index) {
	    String str = null;

	    if(_options.containsKey(option) && _options.get(option).size() >= index)
	        str = _options.get(option).get(index);

	    return str;
    }
	/**
	 * Returns the value of the specified option as an int.
	 * Returns null if the option does not have a value.
	 * If the value of the option is not an int, this method will throw a NumberFormatException.
	 * @param option the name of the option
	 * @return the value of the option
     * @deprecated Use optionInt(option, index)
	 * @since 1.0-alpha
	 */
	@Deprecated
	public int optionInt(String option) {
        return Integer.parseInt(optionString(option, 0));
	}
    /**
     * Returns the value of the specified option at the specified index as an int
     * @param option the name of the option
     * @param index the index of the option
     * @return the value of the option at the specified index
     * @since 1.5
     */
    public int optionInt(String option, int index) {
        String str = null;

        if(_options.containsKey(option) && _options.get(option).size() >= index)
            str = _options.get(option).get(index);

        return Integer.parseInt(str);
    }
	/**
	 * Returns the value of the specified option as a char.
	 * NullPointerException is thrown if the options has no values.
	 * @param option the name of the option
	 * @return the value of the option
	 * @deprecated Use optionChar(option, index)
	 * @since 1.0-alpha
	 */
	@Deprecated
	public char optionChar(String option) {
		return optionString(option, 0).charAt(0);
	}
	/**
	 * Returns the value of the specified option at the specified index as a char.
	 * NullPointerException is thrown if the options has no value at the specified index.
	 * @param option the name of the option
	 * @param index the index of the option
	 * @return the value of the option
	 * @since 1.5
	 */
	public char optionChar(String option, int index) {
		String str = null;

		if(_options.containsKey(option) && _options.get(option).size() >= index)
			str = _options.get(option).get(index);

		return str.charAt(index);
	}
	/**
	 * Returns the value of the specified option as a double.
	 * Returns null if the option does not have a value.
	 * If the value of the option is not a double, this method will throw a NumberFormatException.
	 * @param option the name of the option
	 * @return the value of the option
	 * @deprecated Use optionDouble(option, index)
	 * @since 1.0-alpha
	 */
	@Deprecated
	public double optionDouble(String option) {
		return Double.parseDouble(optionString(option, 0));
	}
	/**
	 * Returns the value of the specified option at the specified index as a double.
	 * Returns null if the option does not have a value.
	 * If the value of the option is not a double, this method will throw a NumberFormatException.
	 * @param option the name of the option
	 * @param index the index of the option
	 * @return the value of the option
	 * @since 1.5
	 */
	public double optionDouble(String option, int index) {
		String str = null;

		if(_options.containsKey(option) && _options.get(option).size() >= index)
			str = _options.get(option).get(index);

		return Double.parseDouble(str);
	}

	/**
	 * Returns whether the specified option has a value.
	 * @param option the name of the option
	 * @return whether the option has a value
	 * @since 1.0-alpha
	 */
 	public boolean hasValue(String option) {
 		return _options.containsKey(option) && _options.get(option).size() > 0;
 	}
 }