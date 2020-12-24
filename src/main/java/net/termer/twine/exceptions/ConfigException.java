package net.termer.twine.exceptions;

/**
 * Exception to be thrown in the case of errors in config files
 * @author termer
 * @since 2.0
 */
public class ConfigException extends Exception {
    // Path to the config file the error originated in
    private final String _path;

    /**
     * Instantiates a new ConfigException
     * @param path Path to the config file the error originated in
     * @param msg The Exception message
     * @since 2.0
     */
    public ConfigException(String path, String msg) {
        super(msg);
        _path = path;
    }

    /**
     * Returns the path to the config file this error originated in
     * @return The path to the config file this error originated in
     * @since 2.0
     */
    public String getPath() {
        return _path;
    }
}
