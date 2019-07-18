package net.termer.twine.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for retrieving information about domain configurations
 * @author termer
 * @since 1.0-alpha
 */
public class Domains {
	// Key: domain name (example.com), value: Domain object
	private HashMap<String, Domain> _domains = new HashMap<String, Domain>(); 
	private Domain _default;
	
	/**
	 * Converts the provided raw YAML maps into a Domains object
	 * @param yml the DomainsYaml object
	 * @since 1.0-alpha
	 */
	public Domains(ArrayList<Map<String, Object>> yml) {
		String defName = null;
		for(int i = 0; i < yml.size(); i++) {
			Map<String, Object> dom = yml.get(i);
			if(i > 0) {
				_domains.put((String) dom.get("domain"),
					new Domain(
						(String) dom.get("name"),
						(String) dom.get("domain"),
						(String) dom.get("dir"),
						(String) dom.get("index"),
						(String) dom.get("notFound"),
						(String) dom.get("serverError"),
						dom.get("ignore404") == null ? false : (Boolean) dom.get("ignore404")
					)
				);
			} else {
				defName = (String) dom.get("defaultDomain");
			}
		}
		
		_default = byName(defName);
	}
	
	/**
	 * Returns the configured default domain
	 * @return the default domain
	 * @since 1.0-alpha
	 */
	public Domain defaultDomain() {
		return _default;
	}
	
	/**
	 * Returns the Domain object for the specified domain
	 * @param domain the domain
	 * @return the corresponding Domain object
	 * @since 1.0-alpha
	 */
	public Domain byDomain(String domain) {
		return _domains.get(domain);
	}
	/**
	 * Returns the Domain object for the specified name/alias
	 * @param name the name/alias of the domain
	 * @return the Domain object for the requested domain
	 * @since 1.0-alpha
	 */
	public Domain byName(String name) {
		Domain dom = null;
		
		for(Domain d : _domains.values()) {
			if(d.name().equals(name)) {
				dom = d;
				break;
			}
		}
		
		return dom;
	}
	
	/**
	 * Returns whether a configuration exists for the specified domain
	 * @param domain the domain
	 * @return whether a configuration exists for the domain
	 * @since 1.0-alpha
	 */
	public boolean exists(String domain) {
		return _domains.containsKey(domain);
	}
	
	/**
	 * Utility class for retrieving information about a single domain's configuration
	 * @author termer
	 * @since 1.0-alpha
	 */
	public class Domain {
		private String _name;
		private String _domain;
		private String _dir;
		private String _index;
		private String _notFound;
		private String _serverError;
		private boolean _ignore404;
		
		// Stores values
		protected Domain(String name, String domain, String dir, String index, String notFound, String serverError, boolean ignore404) {
			_name = name;
			_domain = domain;
			_dir = dir;
			if(!_dir.endsWith("/")) _dir+='/';
			_index = index;
			_notFound = notFound;
			_serverError = serverError;
			_ignore404 = ignore404;
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
		 * Returns the domain's actual domain name
		 * @return the domain's domain name
		 * @since 1.0-alpha
		 */
		public String domain() {
			return _domain;
		}
		
		/**
		 * Returns the domain's configured directory
		 * @return the domain's directory
		 * @since 1.0-alpha
		 */
		public String directory() {
			return _dir;
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
		
		public String toString() {
			return _name+':'+_domain;
		}
	}
}
