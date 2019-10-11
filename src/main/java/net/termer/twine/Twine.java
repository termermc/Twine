package net.termer.twine;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import net.termer.twine.Events.Type;
import net.termer.twine.modules.ModuleManager;
import net.termer.twine.utils.ArgParser;
import net.termer.twine.utils.YamlConfig;
import net.termer.twine.utils.Domains;
import net.termer.twine.utils.FileChecker;

/**
 * Main Twine class
 * @author termer
 * @since 1.0-alpha
 */
public class Twine {
	/**
	 * The unique ID of this Twine instance.
	 * This ID will be sent along with all Twine server events when publishing to the event bus.
	 */
	public static final float INSTANCE_ID = new Random().nextInt();
	
	// Instance variables
	private static ArgParser _args;
	private static String _verStr = "1.0-alpha";
	private static Logger _logger = LoggerFactory.getLogger(Twine.class);
	private static YamlConfig _conf = null;
	private static YamlConfig _clusterConf = null;
	private static Domains _domains = null;
	private static boolean _firstConf = true;
	
	public static void main(String[] args) {
		// Read arguments
		_args = new ArgParser(args);
		
		// Check options/flags
		if(_args.option("help") || _args.flag('h')) {
			System.out.println("java -jar twine-"+_verStr+".jar [OPTIONS]...");
			System.out.println("\n"
					+ "-s, --start             starts the server\n"
					+ "-h, --help              prints this message\n"
					+ "-v, --version           prints the version of the server\n"
					+ "-m, --skip-modules      skips loading modules\n"
					+ "-r, --recreate-configs  recreates all config files\n"
					+ "--config=KEY:VALUE      overrides any value in twine.yml\n"
					+ "\n"
					+ "Examples:"
					+ "  java -jar twine-"+_verStr+".jar -rm  Starts the server while recreating all configs and with modules skipped\n"
					+ "\n"
					+ "For more info on Twine, along with its source code, you can visit its GitHub page: <https://github.com/termermc/twine>");
		} else if(_args.option("version") || _args.flag('v')) {
			System.out.println("Twine version "+_verStr);
		} else if(_args.option("start") || _args.flag('s')) {
			// Check files and directories
			logger().info("Initializing files...");
			
			// Delete configs if --recreate-configs is enabled
			if(_args.flag('r') || _args.option("recreate-configs")) {
				FileChecker.delete(new String[] {
					"twine.yml",
					"domains.yml",
					"configs/"
				});
			}
			FileChecker.createIfNotPresent(new String[] {
				"twine.yml",
				"cluster.yml",
				"domains.yml",
				"access.log",
				"domains/default/index.html",
				"domains/default/404.html",
				"domains/default/500.html",
				"domains/default/logo.png",
				"configs/",
				"static/",
				"modules/",
				"dependencies/"
			});
			
			logger().info("Loading configs...");
			_conf = new YamlConfig("twine.yml");
			_clusterConf = new YamlConfig("cluster.yml");
			try {
				// Load all configurations (reloadConfigurations() is just load when it's first called)
				reloadConfigurations();
				
				// Initialize server so components will be available for modules
				ServerManager.init(r -> {
					if(r.succeeded()) {
						// Catch any further initialization errors
						try {
							// Load modules
							if(!_args.flag('m') && !_args.option("skip-modules")) {
								logger().info("Loading modules...");
								ModuleManager.loadModules();
							}
							
							// Start server
							logger().info("Starting server...");
							ServerManager.start();
							
							Events.fire(Type.SERVER_START);
							
							// Startup complete
							logger().info("Startup complete.");
							
						} catch (IOException e) {
							logger().error("Failed to start server");
							e.printStackTrace();
						}
					} else {
						logger().error("Failed to start server");
						r.cause().printStackTrace();
					}
				});
			} catch (IOException e) {
				logger().error("Failed to start server");
				e.printStackTrace();
			}
		} else {
			System.out.println("To start Twine, specify the -s/--start option. To print help, specify the -h/--help option.");
		}
	}
	
	/**
	 * Reloads all server configuration files
	 * @throws IOException if loading configuration files fail
	 * @since 1.0-alpha
	 */
	@SuppressWarnings("unchecked")
	public static void reloadConfigurations() throws IOException {
		boolean proceed = true;
		if(ServerManager.vertx() != null)
			proceed = Events.fire(Type.CONFIG_RELOAD);
		
		if(proceed) {
			_conf.load();
			_clusterConf.load();
			// Modify config based on CLI options
			if(_args.option("config")) {
				String op = _args.optionString("config");
				if(op.contains(":")) {
					String key = op.substring(0, op.indexOf(':'));
					String val = op.substring(op.indexOf(':')+1);
					if(val.equals("true") || val.equals("false")) {
						_conf.tempSet(key, Boolean.parseBoolean(val));
					} else {
						try {
							_conf.tempSet(key, Integer.parseInt(val));
						} catch(NumberFormatException e) {
							_conf.tempSet(key, val);
						}
					}
				}
			}
			
			// Parse domains.yml
			Yaml yml = new Yaml();
			ArrayList<Map<String, Object>> maps = new ArrayList<Map<String, Object>>(); 
			for(Object obj : yml.loadAll(new FileInputStream("domains.yml"))) {
				if(obj instanceof Map<?, ?>) {
					maps.add((Map<String, Object>) obj);
				}
			}
			_domains = new Domains(maps);
			
			// Only run after first run
			if(_firstConf) {
				_firstConf = false;
			} else {
				ServerManager.reloadVars();
			}
		}
	}
	public static void shutdown() {
		if(!Events.fire(Type.SERVER_STOP)) {
			logger().info("Shutting down down Twine...");
			logger().info("Shutting down modules...");
			ModuleManager.shutdownModules();
			logger().info("Shuttdown down Vert.x...");
			ServerManager.vertx().close(r -> {
				if(r.succeeded()) {
					logger().info("Vert.x successfully shutdown.");
				} else {
					logger().error("Failed to shutdown Vert.x!");
				}
				logger().info("Exiting!");
				System.exit(0);
			});
		}
	}
	
	/**
	 * Returns CLI arguments passed to the server on startup
	 * @return CLI server arguments
	 * @since 1.0-alpha
	 */
	public static ArgParser serverArgs() {
		return _args;
	}
	/**
	 * Returns the version String of this Twine instance
	 * @return the version String
	 * @since 1.0-alpha
	 */
	public static String version() {
		return _verStr;
	}
	/**
	 * Returns the Twine Logger object
	 * @return the logger
	 * @since 1.0-alpha
	 */
	public static Logger logger() {
		return _logger;
	}
	/**
	 * Returns the contents of twine.yml
	 * @return the server configuration file
	 * @since 1.0-alpha
	 */
	public static YamlConfig config() {
		return _conf;
	}
	/**
	 * Returns the contents of cluster.yml
	 * @return The server cluster configuration file
	 * @since 1.0-alpha
	 */
	public static YamlConfig clusterConfig() {
		return _clusterConf;
	}
	/**
	 * Returns the server's configured domains
	 * @return all configured domains
	 * @since 1.0-alpha
	 */
	public static Domains domains() {
		return _domains;
	}
}
