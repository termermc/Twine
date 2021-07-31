package net.termer.twine;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Random;

import io.vertx.core.http.HttpServerRequest;
import net.termer.twine.documents.Documents;
import net.termer.twine.domains.Domains;
import net.termer.twine.exceptions.ConfigException;
import net.termer.twine.utils.*;
import net.termer.twine.utils.files.BlockingFileChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.termer.twine.Events.Type;
import net.termer.twine.modules.ModuleManager;

/**
 * Main Twine class
 * @author termer
 * @since 1.0-alpha
 */
public class Twine {
	/**
	 * The unique ID of this Twine instance.
	 * This ID will be sent along with all Twine server events when publishing to the event bus.
	 * @since 1.0-alpha
	 */
	public static final float INSTANCE_ID = new Random().nextInt(Integer.MAX_VALUE);
	
	// Instance variables
	private static ArgParser _args;
	private static final String _verStr = "2.2";
	private static final int _verInt = 11;
	private static final Logger _logger = LoggerFactory.getLogger(Twine.class);
	private static YamlConfig _conf = null;
	private static Domains _domains = null;
	private static boolean _firstConf = true;
	
	public static void main(String[] args) {
		// Start main thread name
		Thread.currentThread().setName("Twine");
		
		// Read arguments
		_args = new ArgParser(args);
		
		// Check options/flags
		if(_args.option("help") || _args.flag('h')) {
            // Fetch the jar path, and extract its name
            String[] jarPath = Twine.class.getProtectionDomain().getCodeSource().getLocation().getFile().split("/");
            String jar = jarPath[jarPath.length - 1];

            System.out.println("java -jar twine-" + _verStr + ".jar [OPTIONS]...");
            System.out.println("\n"
                    + "-s, --start             starts the server\n"
                    + "-h, --help              prints this message\n"
                    + "-v, --version           prints the version of the server\n"
                    + "-m, --skip-modules      skips loading modules\n"
                    + "-r, --recreate-config   recreates the main twine.yml\n"
                    + "--config=NODE:VALUE      overrides any value in twine.yml\n"
                    + "--classpath-loaded      forces Twine to assume dependencies are loaded\n"
                    + "\n"
                    + "Examples:\n"
                    + "  java -jar " + jar + " -rm  Starts the server while recreating all configs and with modules skipped\n"
                    + "\n"
                    + "For more info on Twine, along with its source code, you can visit its GitHub page: <https://github.com/termermc/twine>");
        } else if(_args.option("recreate-config") || _args.flag('r')) {
            // Delete configs and recreate them
            if (_args.flag('r') || _args.option("recreate-configs")) {
                try {
                    System.out.print("Recreating twine.yml...");

                    // Rename current config to "twine.yml.old"
                    File conf = new File("twine.yml");
                    if(conf.exists() && !conf.renameTo(new File("twine.yml.old")))
                        throw new IOException("Failed to rename twine.yml to twine.yml.old");

                    // Create default
                    BlockingFileChecker.createIfNotPresent(new String[] { "twine.yml" });

                    // Finished
                    System.out.println("Done");
                    System.out.println("The previous config is available as \"twine.yml.old\" and can be safely deleted if you do not need it");
                } catch(IOException e) {
                    logger().error("Error occurred while trying to recreate config:");
                    e.printStackTrace();
                }
            }
		} else if(_args.option("version") || _args.flag('v')) {
		    // Print Twine version
			System.out.println("Twine version "+_verStr);
		} else if(_args.option("start") || _args.flag('s')) {
			// Check if restart need for proper classpath
			if(_args.option("classpath-loaded")) {
				// Check files and directories
				logger().info("Initializing files...");

				try {
					// Create default files
					BlockingFileChecker.createIfNotPresent("twine.yml", new String[] {
							"domains/default/index.html",
							"domains/default/404.html",
							"domains/default/500.html",
							"domains/default/logo.png",
					});
					BlockingFileChecker.createIfNotPresent(new String[] {
							"twine.yml",
							"access.log",
							"configs/",
							"static/",
							"modules/",
							"dependencies/"
					});

					logger().info("Loading configs...");
					_conf = new YamlConfig("twine.yml");

					try {
						// Load all configurations (reloadConfigurations() is just load when it's first called)
						reloadConfigurations();

						// Initialize server so components will be available for modules
						ServerManager.init().onComplete(r -> {
							if (r.succeeded()) {
								// Execute in worker thread
								Thread worker = new Thread(() -> {
									// Catch any further initialization errors
									try {
										// Load modules
										ModuleManager.loadModules();

										// Execute pre-initialization methods if modules are enabled
										if (!_args.flag('m') && !_args.option("skip-modules")) {
											logger().info("Starting modules...");
											ModuleManager.runModulePreInits();
										}

										// Finish initialization
										ServerManager.finishInit();

										// Execute initialization methods if modules are enabled
										if (!_args.flag('m') && !_args.option("skip-modules")) {
											ModuleManager.runModuleInits();
										}

										// Start server
										logger().info("Starting server...");
										ServerManager.start().onComplete(startRes -> {
											if (startRes.succeeded()) {
												Events.fire(Type.SERVER_START);

												// Register shutdown hook
												Thread sdHook = new Thread(Twine::_shutdown);
												sdHook.setName("Twine-Shutdown");
												Runtime.getRuntime().addShutdownHook(sdHook);

												// Startup complete
												logger().info("Startup complete.");
											} else {
												logger().error("Failed to start server:");
												startRes.cause().printStackTrace();
											}
										});
									} catch (IOException e) {
										logger().error("Failed to start server:");
										e.printStackTrace();
									}
								});
								worker.setName("Twine");
								worker.start();
							} else {
								logger().error("Failed to start server:");
								r.cause().printStackTrace();
							}
						});
					} catch(IOException e) {
						logger().error("Failed to start server:");
						e.printStackTrace();
					} catch(ConfigException e) {
						logger().error("Failed to start server because of configuration error:");
						logger().error(e.getPath()+": "+e.getMessage());
					}
				} catch(IOException e) {
					logger().error("Failed to initialize file:");
					e.printStackTrace();
				}
			} else {
				// Retrieve jar path
				String jarPath = Twine.class.getProtectionDomain().getCodeSource().getLocation().getPath();
				
				// Collect arguments
				ArrayList<String> pArgs = new ArrayList<>();
				pArgs.add("java");
				pArgs.add("-classpath");
				pArgs.add(jarPath+':'+"dependencies/*:modules/*");
				pArgs.add(Twine.class.getName());
				pArgs.add("--classpath-loaded");
				Collections.addAll(pArgs, args);
				
				System.out.println("NOTICE: Creating new process using \"dependencies/\" and \"modules/\" in the classpath. To disable, start with --classpath-loaded.");
				
				try {
					// Initialize process creator
					ProcessBuilder builder = new ProcessBuilder(pArgs);
					
					// Redirect process I/O
					builder.redirectError(Redirect.INHERIT);
					builder.redirectInput(Redirect.INHERIT);
					builder.redirectOutput(Redirect.INHERIT);
					
					// Start process and wait for it to end
					builder.start().waitFor();
				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
				}
			}
		} else {
			System.out.println("To start Twine, specify the -s/--start option. To print help, specify the -h/--help option.");
		}
	}
	
	/**
	 * Reloads all server configuration files
	 * @throws IOException If loading configuration files fail
	 * @throws ConfigException If any configuration files have errors in them
	 * @since 1.0-alpha
	 */
	@SuppressWarnings("unchecked")
	public static void reloadConfigurations() throws IOException, ConfigException {
		boolean proceed = true;
		if(ServerManager.vertx() != null)
			proceed = Events.fire(Type.CONFIG_RELOAD);
		
		if(proceed) {
			_conf.load();

			// Modify config based on environment variables
			for(String envKey : System.getenv().keySet()) {
				if(envKey.startsWith("TW_CONF_")) {
					String node = envKey.substring(8);
					String val = System.getenv(envKey).replace("_", ".");

					if(PrimitiveUtils.isBoolean(val)) {
						_conf.tempSetNode(node, Boolean.parseBoolean(val));
					} else if(PrimitiveUtils.isInt(val)) {
						_conf.tempSetNode(node, Integer.parseInt(val));
					} else if(PrimitiveUtils.isDouble(val)) {
						_conf.tempSetNode(node, Double.parseDouble(val));
					} else {
						_conf.tempSetNode(node, val);
					}
				}
			}

			// Modify config based on CLI options
			if(_args.option("config")) {
				for(String op : _args.optionValues("config")) {
					if (op.contains(":")) {
						String node = op.substring(0, op.indexOf(':'));
						String val = op.substring(op.indexOf(':') + 1);

						if(PrimitiveUtils.isBoolean(val)) {
							_conf.tempSetNode(node, Boolean.parseBoolean(val));
						} else if(PrimitiveUtils.isInt(val)) {
							_conf.tempSetNode(node, Integer.parseInt(val));
						} else if(PrimitiveUtils.isDouble(val)) {
							_conf.tempSetNode(node, Double.parseDouble(val));
						} else {
							_conf.tempSetNode(node, val);
						}
					}
				}
			}
			
			// Parse domains
			_domains = new Domains((Map<String, Map<String, Object>>) _conf.getNode("server.domains"), (String) _conf.getNode("server.defaultDomain"));
			
			// Only run after first run
			if(_firstConf) {
				_firstConf = false;
			} else {
				ServerManager.reloadVars();
			}
		}
	}
	/**
	 * Calls all shutdown methods on Modules, and then shuts down Twine
	 * @since 1.3
	 */
	public static void shutdown() {
		if(Events.fire(Type.SERVER_STOP)) {
			System.exit(0);
		}
	}
	// Shuts down Twine, calls module shutdown hooks
	private static void _shutdown() {
		logger().info("Shutting down down Twine...");
		logger().info("Shutting down modules...");
		ModuleManager.shutdownModules();
		logger().info("Shutting down Vert.x...");
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
	 * Returns the version integer of this release.
	 * The version integer will increment at every release, regardless of its type.
	 * @return This release's version integer
	 * @since 1.2-alpha
	 */
	public static int versionInt() {
		return _verInt;
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
	 * Returns the server's configured domains
	 * @return all configured domains
	 * @since 1.0-alpha
	 */
	public static Domains domains() {
		return _domains;
	}
}
