package net.termer.twine.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

/**
 * Utility class to access YAML-format config files
 * @author termer
 * @since 1.0-alpha
 */
public class YamlConfig {
	private String _path;
	private Map<String, Object> _map = null;
	private Yaml _yml;
	
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
	 * Loads the config file
	 * @throws IOException if the path assigned to this Config does not exist
	 * @since 1.0-alpha
	 */
	public void load() throws IOException {
		_map = _yml.load(new FileInputStream(_path));
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
	 * Returns whether the specified key exists in this Config
	 * @param key the key to check
	 * @return whether the key exists
	 * @since 1.0-alpha
	 */
	public boolean hasKey(String key) {
		return _map.containsKey(key);
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
	 * Returns this Config in the form of a Map object
	 * @return the Config in a map
	 * @since 1.0-alpha
	 */
	public Map<String, Object> toMap() {
		return _map;
	}
	
	// Static creation methods
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
		YamlConfig cfg = null;
		
		if(f.exists() && f.isFile()) {
			cfg = new YamlConfig(path);
			
			// Check values against file
			boolean rewrite = false;
			LinkedHashMap<String, Object> newVals = new LinkedHashMap<String, Object>();
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