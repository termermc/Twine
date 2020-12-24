package net.termer.twine.domains;

/**
 * Utility class for retrieving information about a single domain's configuration
 * @author termer
 * @since 1.0-alpha
 */
public class Domain {
    private final String _name;
    private final String[] _hostnames;
    private final String _root;
    private final String _index;
    private final String _notFound;
    private final String _serverError;
    private final boolean _ignore404;
    private final CORS _cors;

    // Stores values
    protected Domain(String name, String[] hostnames, String root, String index, String notFound, String serverError, boolean ignore404, CORS cors) {
        _name = name;
        _hostnames = hostnames;
        if(!root.endsWith("/")) root+='/';
        _root = root;
        _index = index;
        _notFound = notFound;
        _serverError = serverError;
        _ignore404 = ignore404;
        _cors = cors;
    }

    /**
     * Returns the domain's assigned name
     * @return the domain's name
     * @since 1.0-alpha
     */
    public String name() {
        return _name;
    }

    /**
     * Returns the domain's actual hostname
     * @return the domain's actual hostname
     * @since 2.0
     */
    public String[] hostnames() {
        return _hostnames;
    }

    /**
     * Returns the domain's configured root directory
     * @return the domain's root directory
     * @since 2.0
     */
    public String root() {
        return _root;
    }

    /**
     * Returns the domain's configured index filename
     * @return the domain's index
     * @since 1.0-alpha
     */
    public String index() {
        return _index;
    }

    /**
     * Returns the domain's configured 404/not found document filename
     * @return the domain's 404 document
     * @since 1.0-alpha
     */
    public String notFound() {
        return _notFound;
    }

    /**
     * Returns the domain's configured 500/server error document filename
     * @return the domain's 500 document
     * @since 1.0-alpha
     */
    public String serverError() {
        return _serverError;
    }

    /**
     * Returns whether this domain's 404 document should be served with a 404 status code
     * @return whether a 404 status code should be sent
     * @since 1.0-alpha
     */
    public boolean ignore404() {
        return _ignore404;
    }

    /**
     * Returns CORS settings for this domain
     * @return CORS settings for this domain
     * @since 2.0
     */
    public CORS cors() {
        return _cors;
    }

    public String toString() {
        return _name+':'+String.join(",", _hostnames);
    }

    /**
     * Class to contain CORS settings
     */
    public static class CORS {
        private final boolean _enabled;
        private final String _allowedOrigin;
        private final String[] _allowedMethods;
        private final String[] _allowedHeaders;
        private final boolean _allowCredentials;

        // Default everything and disable
        protected CORS() {
            _enabled = false;
            _allowedOrigin = null;
            _allowedMethods = null;
            _allowedHeaders = null;
            _allowCredentials = false;
        }

        // Stores values
        protected CORS(boolean enabled, String allowedOrigin, String[] allowedMethods, String[] allowedHeaders, boolean allowCredentials) {
            _enabled = enabled;
            _allowedOrigin = allowedOrigin;
            _allowedMethods = allowedMethods;
            _allowedHeaders = allowedHeaders;
            _allowCredentials = allowCredentials;
        }

        /**
         * Returns whether CORS headers are enabled for this domain
         * @return Whether CORS headers are enabled for this domain
         * @since 2.0
         */
        public boolean enabled() {
            return _enabled;
        }

        /**
         * Returns the allowed origin for requests to this domain, or null if not specified.
         * The header this dictates is "Access-Control-Allow-Origin".
         * Possible values are a valid origin, "*", or "request-origin".
         * If the value is "request-origin", the value of the header will be changed to the "Origin" header of the request, or "*" if none was sent.
         * @return The allowed origin for requests to this domain, or null if not specified.
         * @since 2.0
         */
        public String allowedOrigin() {
            return _allowedOrigin;
        }

        /**
         * Returns all allowed HTTP request methods for AJAX/XHR requests to this domain, or null if not specified
         * @return All allowed HTTP request methods for requests to this domain, or null if not specified
         * @since 2.0
         */
        public String[] allowedMethods() {
            return _allowedMethods;
        }

        /**
         * Returns all allow extra HTTP headers for AJAX/XHR requests to this domain, or null if not specified
         * @return All allow extra HTTP headers for requests to this domain, or null if not specified
         * @since 2.0
         */
        public String[] allowedHeaders() {
            return _allowedHeaders;
        }

        /**
         * Returns whether to allow credentials such as cookies in AJAX/XHR requests to this domain
         * @return Whether to allow credentials such as cookies in AJAX/XHR requests to this domain
         * @since 2.0
         */
        public boolean allowCredentials() {
            return _allowCredentials;
        }
    }
}