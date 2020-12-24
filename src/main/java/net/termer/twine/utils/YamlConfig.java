package net.termer.twine.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import net.termer.twine.exceptions.ConfigException;
import org.yaml.snakeyaml.Yaml;

/**
 * Utility class to access YAML-format config files in a blocking fashion
 * @author termer
 * @since 1.0-alpha
 */
public class YamlConfig {
	private final String _path;
	private Map<String, Object> _map = null;
	private final Yaml _yml;
	
	/**
	 * Instantiates a new Config object
	 * @param path the path to the config file
	 * @since 1.0-alpha
	 */
	public YamlConfig(String path) {
		_path = path;
		_yml = new Yaml();
	}

	/**
	 * Returns the path to this config file
	 * @return The path to this config file
	 * @since 2.0
	 */
	public String path() {
		return _path;
	}
	
	/**
	 * Loads the config file
	 * @throws IOException if the path assigned to this Config does not exist
	 * @since 1.0-alpha
	 */
	public void load() throws IOException {
		_map = _yml.load(new FileInputStream(_path));
	}

	/**
	 * Returns whether this config file has been loaded
	 * @return Whether this config file has been loaded
	 * @since 2.0
	 */
	public boolean loaded() {
		return _map != null;
	}
	
	/**
	 * Returns the value for the specified key
	 * @param key the key
	 * @return the value corresponding to the key
	 * @since 1.0-alpha
	 */
	public Object get(String key) {
		return _map.get(key);
	}
	/**
	 * Returns a field's value from this config based on the provided node.
	 * A node will be a field name + "." for any sub-fields.
	 * Example: "server.websocket.enable" would be equivalent to get("server").get("websocket").get("enable").
	 * @param node The node to fetch from this config
	 * @return A field's value from this config based on the provided node.
	 * @since 2.0
	 */
	public Object getNode(String node) {
		if(node.contains(".")) {
			Object val = _map;
			String curNode = node+'.';

			while(curNode.contains(".")) {
				String field = curNode.substring(0, curNode.indexOf('.'));

				if(val instanceof Map<?, ?>) {
					val = ((Map<?, ?>) val).get(field);
					curNode = curNode.substring(curNode.indexOf('.') + 1);
				} else {
					return null;
				}
			}

			return val;
		} else {
			return _map.get(node);
		}
	}
	
	/**
	 * Returns whether the specified key exists in this Config
	 * @param key the key to check
	 * @return whether the key exists
	 * @since 1.0-alpha
	 */
	public boolean hasKey(String key) {
		return _map.containsKey(key);
	}
	/**
	 * Returns whether the specified node exists in this Config
	 * @param node The node to check
	 * @return Whether the specified node exists in this Config
	 * @since 2.0
	 */
	public boolean hasNode(String node) {
		return getNode(node) != null;
	}
	
	/**
	 * Temporarily sets a value.
	 * Value is not written to file, and is reset when load() is called.
	 * @param key the key
	 * @param value the temporary value
	 * @since 1.0-alpha
	 */
	public void tempSet(String key, Object value) {
		_map.put(key, value);
	}

	/**
	 * Temporarily sets the value of the specified node.
	 * Value is not written to file, and is reset when load() is called.
	 * @param node The node to set
	 * @param value The temporary value
	 * @throws ConfigException If a field assignment in the node conflicts with an existing field
	 * @since 2.0
	 */
	public void tempSetNode(String node, Object value) throws ConfigException {
		if (node.contains(".")) {
			Map<String, Object> val = _map;
			String curNode = node.substring(0, node.lastIndexOf('.')+1);

			while(curNode.contains(".")) {
				String field = curNode.substring(0, curNode.indexOf('.'));

				if(!val.containsKey(field)) {
					val.put(field, new HashMap<String, Object>());
				} else if(!(val.get(field) instanceof Map<?, ?>)) {
					throw new ConfigException(_path, "Field conflicts with an existing field named " + field + " of type " + val.get(field).getClass().getSimpleName());
				}

				val = (Map<String, Object>) val.get(field);
				curNode = curNode.substring(curNode.indexOf('.') + 1);
			}

			val.put(node.substring(node.lastIndexOf('.')+1), value);
		} else {
			_map.put(node, value);
		}
	}
	
	/**
	 * Returns this Config in the form of a Map object
	 * @return the Config in a map
	 * @since 1.0-alpha
	 */
	public Map<String, Object> toMap() {
		return _map;
	}

	/*
	 * Static creation methods
	 */
	/**
	 * Creates a new Config file and returns its value
	 * @param path the path to create the file
	 * @param values the values to write to the file
	 * @return the newly created Config file
	 * @throws IOException if writing to the file fails
	 * @since 1.0-alpha
	 */
	public static YamlConfig create(String path, LinkedHashMap<String, Object> values) throws IOException {
		Yaml yml = new Yaml();
		FileWriter writer = new FileWriter(path);
		yml.dump(values, writer);
		
		return new YamlConfig(path);
	}
	/**
	 * Creates a new Config file with the provided values, or reads it if it already exists.
	 * If the file exists but does not have all the provided keys, it will re-create them.
	 * @param path the path to the Config file
	 * @param values the values to create or to check the file against
	 * @return the Config file
	 * @throws IOException if reading or writing the file fails
	 * @since 1.0-alpha
	 */
	public static YamlConfig createIfNotPresent(String path, LinkedHashMap<String, Object> values) throws IOException {
		File f = new File(path);
		YamlConfig cfg;
		
		if(f.exists() && f.isFile()) {
			cfg = new YamlConfig(path);
			
			// Check values against file
			boolean rewrite = false;
			LinkedHashMap<String, Object> newVals = new LinkedHashMap<>();
			for(String key : values.keySet()) {
				if(cfg.hasKey(key)) {
					newVals.put(key, cfg.get(key));
				} else {
					rewrite = true;
					newVals.put(key, values.get(key));
				}
			}
			// Rewrite the file, replacing missing values
			if(rewrite) {
				cfg = create(path, newVals);
			}
		} else {
			cfg = create(path, values);
		}
		
		return cfg;
	}
}