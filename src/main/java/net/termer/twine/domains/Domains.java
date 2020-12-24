package net.termer.twine.domains;

import net.termer.twine.exceptions.ConfigException;
import net.termer.twine.utils.RequestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for retrieving information about domain configurations
 * @author termer
 * @since 1.0-alpha
 */
public class Domains {
	private final Domain[] _domains;
	private final Domain _default;
	// HashMap for quicker access to domains by their hostname without having to iterate over domains manually
	// Key: hostname (example.com), value: Domain object
	private final HashMap<String, Domain> _domainsMap = new HashMap<>();
	
	/**
	 * Converts the provided raw YAML maps into a Domains object
	 * @param domainsMap The domains Map to index
	 * @param defaultName The name of the default domain
	 * @throws ConfigException If there any entries are missing fields or have other similar problems
	 * @since 2.0
	 */
	public Domains(Map<String, Map<String, Object>> domainsMap, String defaultName) throws ConfigException {
		ArrayList<Domain> domains = new ArrayList<>();
		for(String name: domainsMap.keySet()) {
			Map<String, Object> dom = domainsMap.get(name);

			// List of hostnames
			ArrayList<String> hostnames = new ArrayList<>();

			// Add to hostnames from field
			if(dom.containsKey("hostname")) {
				// Validate field value
				if(dom.get("hostname") instanceof String) {
					hostnames.add((String) dom.get("hostname"));
				} else {
					throw new ConfigException("twine.yml", "Field \"hostname\" must contain a string");
				}
			} else if(dom.containsKey("hostnames")) {
				// Validate field value
				if(dom.get("hostnames") instanceof ArrayList<?>) {
					// Add hostnames and validate them
					for(Object hostname : (ArrayList<?>) dom.get("hostnames")) {
						if(hostname instanceof String) {
							hostnames.add((String) hostname);
						} else {
							throw new ConfigException("twine.yml", "Items in field \"hostnames\" must be strings");
						}
					}
				} else {
					throw new ConfigException("twine.yml", "Field \"hostname\" must contain a list");
				}
			} else {
				throw new ConfigException("twine.yml", "Must provide either \"hostname\" or \"hostnames\" field");
			}

			// The CORS settings for the domain
			Domain.CORS cors;
			if(dom.containsKey("cors")) {
				Map<String, Object> corsMap = (Map<String, Object>) dom.get("cors");

				if((boolean) corsMap.get("enable")) {
					cors = new Domain.CORS(
							true,
							(String) corsMap.get("allowOrigin"),
							((ArrayList<String>) corsMap.get("allowMethods")).toArray(new String[0]),
							((ArrayList<String>) corsMap.get("allowHeaders")).toArray(new String[0]),
							(boolean) corsMap.get("allowCredentials")
					);
				} else {
					cors = new Domain.CORS();
				}
			} else {
				cors = new Domain.CORS();
			}

			// Create domain object
			Domain domainObj = new Domain(
					name,
					hostnames.toArray(new String[0]),
					(String) dom.get("root"),
					(String) dom.get("index"),
					(String) dom.get("notFound"),
					(String) dom.get("serverError"),
					dom.get("ignore404") != null && (boolean) dom.get("ignore404"),
					cors
			);

			// Add it to list and put it in the quick access map
			domains.add(domainObj);
			for(String hostname : hostnames) {
				_domainsMap.put(hostname, domainObj);
			}
		}

		_domains = domains.toArray(new Domain[0]);
		_default = byName(defaultName);
	}

	/**
	 * Returns all domains
	 * @return All domains
	 * @since 2.0
	 */
	public Domain[] all() {
		return _domains;
	}
	
	/**
	 * Returns the configured default domain
	 * @return The default domain
	 * @since 1.0-alpha
	 */
	public Domain defaultDomain() {
		return _default;
	}
	
	/**
	 * Returns the Domain object for the specified hostname, or null if it doesn't exist
	 * @param hostname The hostname
	 * @return The corresponding Domain object
	 * @since 2.0
	 */
	public Domain byHostname(String hostname) {
		return _domainsMap.get(hostname);
	}
	/**
	 * Returns the Domain object for the specified Host header, or null if it doesn't exist, or the default domain if the Host header is null.
	 * This method differs from byHostname because it takes a raw Host header instead of hostname.
	 * The Host header will be have its port stripped out, or if it is null, the default domain will be returned.
	 * @param host The Host header
	 * @return The corresponding Domain object
	 * @since 2.0
	 */
	public Domain byHostHeader(String host) {
		return _domainsMap.get(RequestUtils.domain(host));
	}
	/**
	 * Returns the Domain object for the specified name/alias, or null if it doesn't exist
	 * @param name the name/alias of the domain
	 * @return The Domain object for the requested domain
	 * @since 1.0-alpha
	 */
	public Domain byName(String name) {
		Domain dom = null;

		for(Domain d : _domains) {
			if(d.name().equals(name)) {
				dom = d;
				break;
			}
		}
		
		return dom;
	}

	/**
	 * Returns the Domain object for the specified hostname, or the default Domain if it doesn't exist
	 * @param hostname The hostname
	 * @return The corresponding Domain object or the default Domain
	 * @since 2.0
	 */
	public Domain byHostnameOrDefault(String hostname) {
		Domain dom = byHostname(hostname);

		if(dom == null)
			return defaultDomain();
		else
			return dom;
	}
	/**
	 * Returns the Domain object for the specified Host header, or the default domain if the Host header is null or if the domain doesn't exist.
	 * This method differs from byHostnameOrDefault because it takes a raw Host header instead of hostname.
	 * The Host header will be have its port stripped out, or if it is null, the default domain will be returned.
	 * @param host The Host header
	 * @return The corresponding Domain object or the default Domain
	 * @since 2.0
	 */
	public Domain byHostHeaderOrDefault(String host) {
		Domain dom = byHostHeader(host);

		if(dom == null)
			return defaultDomain();
		else
			return dom;
	}
	/**
	 * Returns the Domain object for the specified name/alias, or the default Domain if it doesn't exist
	 * @param name the name/alias of the domain
	 * @return The Domain object for the requested domain or the default Domain
	 * @since 2.0
	 */
	public Domain byNameOrDefault(String name) {
		Domain dom = byName(name);

		if(dom == null)
			return defaultDomain();
		else
			return dom;
	}
	
	/**
	 * Returns whether a configuration exists for the specified hostname
	 * @param hostname The hostname
	 * @return Whether a configuration exists for the specified hostname
	 * @since 1.0-alpha
	 */
	public boolean exists(String hostname) {
		return _domainsMap.containsKey(hostname);
	}
}
