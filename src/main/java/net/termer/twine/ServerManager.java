package net.termer.twine;

import java.util.ArrayList;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.core.spi.cluster.NodeListener;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import io.vertx.spi.cluster.zookeeper.ZookeeperClusterManager;
import net.termer.twine.handler.*;

import static net.termer.twine.Twine.config;
import static net.termer.twine.Twine.logger;

/**
 * Utility class to manage internal Vert.x instance
 * @author termer
 * @since 1.0-alpha
 */
public class ServerManager {
	// Vert.x components (servers, etc)
	private static Vertx _vertx = null;
	private static HttpServer _http = null;
	private static HttpServer _redir = null;
	private static Router _router = null;
	
	// Handlers
	private static SessionStore _sessStore = null;
	private static SessionHandler _sess = null;
	private static StaticHandler _staticHandler = null;
	private static BodyHandler _bodyHandler = null;
	private static JsonBodyHandler _jsonBodyHandler = new JsonBodyHandler();
	private static final LoggingHandler _loggingHandler = new LoggingHandler();
	private static final DomainHandler _domainHandler = new DomainHandler();
	private static final NotFoundHandler _notFoundHandler = new NotFoundHandler();
	private static final ErrorHandler _errorHandler = new ErrorHandler();
	
	// Options
	private static HttpServerOptions _httpOps = null;
	
	// Extra
	private static TwineWebSocket _ws = null;
	private static final SimpleDateFormat cacheDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
	
	/**
	 * Initializes the server without starting it or registering handlers
	 * @return A future that returns the Vertx instance the server will use once it is initialized, or an exception if initialization failed
	 * @since 2.0
	 */
	protected static Future<Vertx> init() {
		return Future.future(promise -> {
			// Set cache date format timezone
			cacheDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

			// Check if clustering is enabled
			if((boolean) config().getNode("vertx.cluster.enable")) {
				// Configure cluster
				JsonObject zkConf = new JsonObject()
						.put("zookeeperHosts", String.join(",", (ArrayList<String>) config().getNode("vertx.cluster.hosts")))
						.put("connectTimeout", config().getNode("vertx.cluster.connectTimeout"))
						.put("sessionTimeout", config().getNode("vertx.cluster.sessionTimeout"))
						.put("rootPath", "io.vertx")
						.put("retry", new JsonObject()
								.put("initialSleepTime", config().getNode("vertx.cluster.retryInitialSleepTime"))
								.put("intervalTimes", config().getNode("vertx.cluster.retryIntervalTime"))
								.put("maxTimes", config().getNode("vertx.cluster.retryMaxTimes"))
						);

				ZookeeperClusterManager clusterMan = new ZookeeperClusterManager(zkConf);
				clusterMan.nodeListener(new NodeListener() {
					public void nodeAdded(String nodeID) {
						if(nodeID.equals(clusterMan.getNodeId())) {
							logger().info("Successfully joined the Zookeeper cluster");
						}
					}

					public void nodeLeft(String nodeID) {
						if(nodeID.equals(clusterMan.getNodeId())) {
							logger().info("Left the Zookeeper cluster");
						}
					}
				});

				// Create clustered Vert.x instance
				EventBusOptions ebOps = new EventBusOptions();
				VertxOptions vertxOps = new VertxOptions()
						.setClusterManager(clusterMan)
						.setEventBusOptions(ebOps)
						.setWorkerPoolSize((int) config().getNode("vertx.workerPoolSize"));
				Vertx.clusteredVertx(vertxOps).onComplete(res -> {
					if(res.succeeded()) {
						_vertx = res.result();

						// Run the rest of the initialization process
						_init();
						// Complete callback
						promise.complete(_vertx);
					} else {
						// Error occurred
						promise.fail(res.cause());
					}
				});

			} else {
				// Create normal Vert.x instance
				_vertx = Vertx.vertx();
				_init();
				promise.complete(_vertx);
			}
		});
	}
	
	// Completes the actions of init()
	private static void _init() {
		// Setup server
		_router = Router.router(_vertx);
		_httpOps = new HttpServerOptions()
			.setLogActivity((boolean) config().getNode("server.logging.enable"))
			.setCompressionSupported((boolean) config().getNode("server.compression"));
		
		// Instantiate WebSocket utility
		_ws = new TwineWebSocket(_vertx, (int) config().getNode("server.websocket.maxBytesStreaming"));

		// SSL
		if((boolean) config().getNode("server.https.enable")) {
			String jksPath = (String) config().getNode("server.https.keystore");
			String jksPwd = (String) config().getNode("server.https.keystorePassword");
			_httpOps.setKeyStoreOptions(new JksOptions()
				.setPath(jksPath)
				.setPassword(jksPwd)
			);
			_httpOps.setSsl(true);
		}

		// Create server
		_http = _vertx.createHttpServer(_httpOps);

		// Session (only if enabled)
		if((boolean) config().getNode("server.sessions")) {
			_sessStore = LocalSessionStore.create(vertx());
			_sess = SessionHandler.create(_sessStore);
			_router.route().handler(_sess);
		}

		// Logger
		_router.route().handler(_loggingHandler);

		// Upload limit
		_bodyHandler = BodyHandler.create();
		_bodyHandler
				.setBodyLimit((int) config().getNode("server.maxBodySize"));

		// Static handler
		_staticHandler = StaticHandler.create((String) config().getNode("server.static.directory"));
		_staticHandler
				.setIndexPage((String) config().getNode("server.static.indexPage"))
				.setAllowRootFileSystemAccess(false)
				.setCachingEnabled((boolean) config().getNode("server.static.caching"))
				.setDirectoryListing((boolean) config().getNode("server.static.enableListing"))
				.setIncludeHidden((boolean) config().getNode("server.static.includeHidden"))
				.setEnableRangeSupport((boolean) config().getNode("server.static.enableRangeSupport"));
	}

	/**
	 * Finishes initialization (registers handlers for after module preinitialize() calls)
	 * @since 1.3
	 */
	protected static void finishInit() {
		// Setup body handlers before anything is done
		_router.route().handler(_bodyHandler);
		_router.route().handler(_jsonBodyHandler);
	}

	/**
	 * Starts the server
	 * @return A future that returns the Vertx instance that this server is using once the server has started, or an exception if startup failed
	 * @since 2.0
	 */
	protected static Future<Vertx> start() {
		return Future.future(promise -> {
			// Setup WebSocket
			if((boolean) config().getNode("server.websocket.enable"))
				_router.mountSubRouter(((String) config().getNode("server.websocket.endpoint")), _ws.build());

			// Domain and static handlers
			_router.route().handler(_domainHandler);
			_router.route().handler(_staticHandler);

			// Error handlers
			_router.errorHandler(404, _notFoundHandler);
			_router.errorHandler(500, _errorHandler);

			// Start server(s) if HTTP is enabled
			if(!(boolean) config().getNode("server.enable")) {
				// Notice about lack of HTTP server
				logger().info("No HTTP server will be started because disableHttp is enabled");

				// Complete callback
				promise.complete();
			} else {
				// Collect server bind info
				String addr = (String) config().getNode("server.ip");
				int port = (int) config().getNode("server.port");

				// Start server
				_http.requestHandler(_router).listen(port, addr).onComplete(r -> {
					if(r.succeeded()) {
						logger().info("Server listening on "+config().getNode("server.ip")+':'+r.result().actualPort());

						// Setup HTTPS redirection, if HTTPS and redirection is enabled
						if((boolean) config().getNode("server.https.enable") && (boolean) config().getNode("server.https.redirect.enable")) {
							// Port to run redirect server on
							int redirPort = (int) config().getNode("server.https.redirect.port");

							// Create redirect server
							_redir = _vertx.createHttpServer();

							// Redirect all requests to the HTTPS version of them
							_redir.requestHandler(req -> {
								String locStr = "https://" +
										req.host() +
										req.path();

								if(req.query() != null) {
									locStr += '?' + req.query();
								}

								req.response().putHeader("Location", locStr);
								req.response().setStatusCode(301);
								req.response().end();
							});

							// Start redirect server
							_redir.listen(redirPort, addr).onComplete(redirRes -> {
								if(redirRes.succeeded()) {
									// Emit CLUSTER_JOIN event if clustering is enabled
									if((boolean) config().getNode("vertx.cluster.enable"))
										Events.fire(Events.Type.CLUSTER_JOIN);

									promise.complete();
								} else {
									promise.fail(redirRes.cause());
								}
							});
						} else {
							// Emit CLUSTER_JOIN event if clustering is enabled
							if((boolean) config().getNode("vertx.cluster.enable"))
								Events.fire(Events.Type.CLUSTER_JOIN);

							promise.complete();
						}
					} else {
						promise.fail(r.cause());
					}
				});
			}
		});
	}
	
	/**
	 * Returns the server's Router object
	 * @return The server's Router
	 * @since 1.0-alpha
	 */
	public static Router router() {
		return _router;
	}
	/**
	 * Returns the main Vertx instance
	 * @return The main Vertx instance
	 * @since 1.0-alpha
	 */
	public static Vertx vertx() {
		return _vertx;
	}
	/**
	 * Returns the TwineWebSocket to setup SockJS event bus bridge settings
	 * @return The TwineWebSocket for this server
	 * @since 1.0-alpha
	 */
	public static TwineWebSocket ws() {
		return _ws;
	}
	/**
	 * Returns the BodyHandler for this instance
	 * @return The BodyHandler
	 * @since 1.0-alpha
	 */
	public static BodyHandler bodyHandler() {
		return _bodyHandler;
	}
	/**
	 * Returns the StaticHandler for this instance
	 * @return The StaticHandler
	 * @since 1.3
	 */
	public static StaticHandler staticHandler() {
		return _staticHandler;
	}
	/**
	 * Returns the SessionHandler for this instance
	 * @return The SessionHandler
	 * @since 1.3
	 */
	public static SessionHandler sessionHandler() {
		return _sess;
	}

	/**
	 * Returns the SessionStore for this instance
	 * @return The SessionStore
	 * @since 2.0
	 */
	public static SessionStore sessionStore() {
		return _sessStore;
	}
	
	/**
	 * Reloads all server-specified variables
	 * @since 1.0-alpha
	 */
	protected static void reloadVars() {
		_staticHandler
				.setWebRoot((String) config().getNode("server.static.directory"))
				.setCachingEnabled((boolean) config().getNode("server.static.caching"))
				.setDirectoryListing((boolean) config().getNode("server.static.enableListing"));
		_bodyHandler
				.setBodyLimit((int) config().getNode("server.maxBodySize"));
		_httpOps
				.setLogActivity((boolean) config().getNode("server.logging.enable"))
				.setCompressionSupported((boolean) Twine.config().getNode("server.compression"));
	}
}