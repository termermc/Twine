package net.termer.twine;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.TimeZone;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.impl.MimeMapping;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.VirtualHostHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import io.vertx.spi.cluster.zookeeper.ZookeeperClusterManager;
import net.termer.twine.utils.Domains.Domain;
import net.termer.twine.documents.Documents;
import net.termer.twine.utils.RequestUtils;
import net.termer.twine.utils.Writer;

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
	private static SessionHandler _sess = null;
	private static StaticHandler _staticHandler = null;
	private static BodyHandler _bodyHandler = null;
	private static LoggingHandler _loggingHandler = new LoggingHandler();
	private static DomainHandler _domainHandler = new DomainHandler();
	private static NotFoundHandler _notFoundHandler = new NotFoundHandler();
	private static ErrorHandler _errorHandler = new ErrorHandler();
	
	// Options
	private static HttpServerOptions _httpOps = null;
	
	// Extra
	private static TwineWebsocket _ws = null;
	private static SimpleDateFormat cacheDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
	
	/**
	 * Initializes the server without starting it or registering handlers
	 * @since 1.0-alpha
	 */
	protected static void init(Handler<AsyncResult<Vertx>> callback) {
		// Set cache date format timezone
		cacheDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		// Check if clustering is enabled
		if((boolean) Twine.config().get("clusterEnable")) {
			// Configure cluster
			JsonObject zkConf = new JsonObject()
				.put("zookeeperHosts", (String) Twine.clusterConfig().get("clusterHosts"))
				.put("sessionTimeout", (int) Twine.clusterConfig().get("sessionTimeout"))
				.put("connectTimeout", (int) Twine.clusterConfig().get("connectTimeout"))
				.put("rootPath", "io.vertx")
				.put("retry", new JsonObject()
					.put("initialSleepTime", (int) Twine.clusterConfig().get("retryInitialSleepTime"))
					.put("intervalTimes", (int) Twine.clusterConfig().get("retryIntervalTime"))
					.put("maxTimes", (int) Twine.clusterConfig().get("retryMaxTimes")
				)
			);
			
			ZookeeperClusterManager clusterMan = new ZookeeperClusterManager(zkConf);
			
			// Create clustered Vert.x instance
			EventBusOptions ebOps = new EventBusOptions()
				.setClustered(true);
			VertxOptions vertxOps = new VertxOptions()
				.setClusterManager(clusterMan)
				.setEventBusOptions(ebOps)
				.setWorkerPoolSize((int) Twine.config().get("workerPoolSize"));
			Vertx.clusteredVertx(vertxOps, vertx -> {
				if(vertx.succeeded()) {
					_vertx = vertx.result();
					
					// Run the rest of the initialization process
					_init();
					// Complete callback
					callback.handle(vertx);
				} else {
					// Error occurred
					callback.handle(Future.failedFuture(vertx.cause()));
				}
			});
			
		} else {
			// Create normal Vert.x instance
			_vertx = Vertx.vertx();
			_init();
			callback.handle(Future.succeededFuture(_vertx));
		}
	}
	
	// Completes the actions of init()
	private static void _init() {
		// Setup server
		_router = Router.router(_vertx);
		_httpOps = new HttpServerOptions()
			.setLogActivity((boolean) Twine.config().get("httpLogging"))
			.setCompressionSupported((boolean) Twine.config().get("compression"));
		
		// Instantiate websocket utility
		_ws = new TwineWebsocket(_vertx, (int) Twine.config().get("wsMaxBytesStreaming"));
		
		// SSL
		if(((String) Twine.config().get("keystore")).length() > 0) {
			String jksPath = (String) Twine.config().get("keystore");
			String jksPwd = (String) Twine.config().get("keystorePassword");
			_httpOps.setKeyStoreOptions(new JksOptions()
				.setPath(jksPath)
				.setPassword(jksPwd)
			);
			_httpOps.setSsl(true);
		}
		
		// Create server
		_http = _vertx.createHttpServer(_httpOps);
		
		// Session (only if enabled)
		if((boolean) Twine.config().get("sessions")) {
			_router.route().handler(CookieHandler.create());
			SessionStore ss = LocalSessionStore.create(ServerManager.vertx());
			_sess = SessionHandler.create(ss);
			_router.route().handler(_sess);
		}
		
		// Logger
		_router.route().handler(_loggingHandler);
		
		// Upload limit
		_bodyHandler = BodyHandler.create();
		_bodyHandler
			.setBodyLimit((int) Twine.config().get("maxBodySize"));
		// Limit body size before anything is done
		_router.route().handler(_bodyHandler);
		
		// Static handler
		_staticHandler = StaticHandler.create((String) Twine.config().get("static"));
		_staticHandler
			.setIndexPage("index.html")
			.setAllowRootFileSystemAccess(false)
			.setCachingEnabled((boolean) Twine.config().get("staticCaching"))
			.setDirectoryListing((boolean) Twine.config().get("staticBrowser"))
			.setEnableRangeSupport(true);
	}
	
	/**
	 * Starts the server
	 * @since 1.0-alpha
	 */
	protected static void start() {
		// Setup websocket
		if((boolean) Twine.config().get("wsEnable"))
			_router.route(((String) Twine.config().get("wsEndpoint"))+'*').handler(_ws.build());
		
		// Domain and static handlers
		_router.route().handler(_domainHandler);
		_router.route().handler(_staticHandler);
		
		// Error handlers
		_router.errorHandler(404, _notFoundHandler);
		_router.errorHandler(500, _errorHandler);
		
		// Start server(s) if HTTP is enabled
		if(!(boolean) Twine.config().get("disableHttp")) {
			// Start server
			String addr = (String) Twine.config().get("ip");
			int port = (int) Twine.config().get("port");
			_http.requestHandler(_router).listen(port, addr);
			
			// Setup HTTPS redirection, if enabled
			if((boolean) Twine.config().get("httpsRedirect")) {
				int rport = (int) Twine.config().get("httpsRedirectPort");
				
				_redir = _vertx.createHttpServer();
				Router rrouter = Router.router(_vertx);
				_redir.requestHandler(rrouter);
				rrouter.route().handler(r -> {
					String locStr = "https://"+
									r.request().host()+
									r.request().path();
					if(r.request().query() != null) {
						locStr += '?'+r.request().query();
					}
					r.response().putHeader("Location", locStr);
					r.response().setStatusCode(301);
					r.response().end();
				});
				_redir.listen(rport, addr);
			}
		}
	}
	
	// Sends a file and handles range support
	private static void sendFile(RoutingContext r, File f) {
		// Advertise range support
		r.response().putHeader("Accept-Ranges", "bytes");
		r.response().putHeader("vary", "accept-encoding");
		
		// Write caching headers if enabled
		if((boolean) Twine.config().get("staticCaching")) {
			r.response().putHeader("date", cacheDateFormat.format(new Date()));
			r.response().putHeader("cache-control", "public, max-age=86400");
			r.response().putHeader("last-modified", cacheDateFormat.format(new Date(f.lastModified())));
		}
		
		// Check if range requested
		if(r.request().headers().get("Range") == null) {
			// Send file length on HEAD
			if(r.request().method() == HttpMethod.HEAD)
				r.response().putHeader("content-length", Long.toString(f.length()));
			
			// Correct plain text header
			if(MimeMapping.getMimeTypeForFilename(f.getName()) == "text/plain") {
				r.response().putHeader("Content-Type", "text/plain;charset=UTF-8");
			}
			
			// Send full file
			r.response().sendFile(f.getAbsolutePath());
		} else {
			// Resolve range parameters
			String rangeStr = r.request().headers().get("Range").substring(6);
			long off = Long.parseLong(rangeStr.split("-")[0]);
			long end = f.length();
			long len = end;
			if(!rangeStr.endsWith("-")) {
				end = Long.parseLong(rangeStr.split("-")[1]);
			}
			
			// Send segment length on HEAD
			if(r.request().method() == HttpMethod.HEAD)
				r.response().putHeader("content-length", Long.toString((end-off)+1));
			
			// Send headers
			r.response().setStatusCode(206);
			r.response().putHeader("Content-Range", "bytes "+off+"-"+(end-1)+"/"+len);
			
			// Send file part
			r.response().sendFile(f.getAbsolutePath(), off, Math.min(end+1, len));
		}
	}
	
	/**
	 * Returns the server's Router object
	 * @return the server's Router
	 * @since 1.0-alpha
	 */
	public static Router router() {
		return _router;
	}
	/**
	 * Returns the main Vertx instance
	 * @return the main Vertx instance
	 * @since 1.0-alpha
	 */
	public static Vertx vertx() {
		return _vertx;
	}
	/**
	 * Returns the TwineWebsocket to setup SockJS event bus bridge settings
	 * @return the TwineWebsocket for this server
	 * @since 1.0-alpha
	 */
	public static TwineWebsocket ws() {
		return _ws;
	}
	/**
	 * Returns the BodyHandler for this instance
	 * @return the BodyHandler
	 * @since 1.0-alpha
	 */
	public static BodyHandler bodyHandler() {
		return _bodyHandler;
	}
	
	/**
	 * Reloads all server-specified variables
	 * @since 1.0-alpha
	 */
	protected static void reloadVars() {
		_staticHandler
			.setWebRoot((String) Twine.config().get("static"))
			.setCachingEnabled((boolean) Twine.config().get("staticCaching"))
			.setDirectoryListing((boolean) Twine.config().get("staticBrowser"));
		_bodyHandler
			.setBodyLimit((int) Twine.config().get("maxBodySize"));
		_httpOps
			.setLogActivity((boolean) Twine.config().get("httpLogging"))
			.setCompressionSupported((boolean) Twine.config().get("compression"));
	}
	
	/**
	 * Registers a handler for the specified domain only (can use wildcards)
	 * @param domain the domain (or wildcard pattern)
	 * @param hdlr the handler
	 * @since 1.0-alpha
	 */
	public static void handler(String domain, Handler<RoutingContext> hdlr) {
		_router.route().handler(VirtualHostHandler.create(domain, hdlr));
	}
	/**
	 * Registers a GET handler for the specified domain only (can use wildcards)
	 * @param domain the domain (or wildcard pattern)
	 * @param hdlr the handler
	 * @since 1.0-alpha
	 */
	public static void get(String domain, Handler<RoutingContext> hdlr) {
		_router.get().handler(VirtualHostHandler.create(domain, hdlr));
	}
	/**
	 * Registers a POST handler for the specified domain only (can use wildcards)
	 * @param domain the domain (or wildcard pattern)
	 * @param hdlr the handler
	 * @since 1.0-alpha
	 */
	public static void post(String domain, Handler<RoutingContext> hdlr) {
		_router.post().handler(VirtualHostHandler.create(domain, hdlr));
	}
	/**
	 * Registers a handler for the specified domain only (can use wildcards)
	 * @param route the route to match (can use wildcards and parameters)
	 * @param domain the domain (or wildcard pattern)
	 * @param hdlr the handler
	 * @since 1.0-alpha
	 */
	public static void handler(String route, String domain, Handler<RoutingContext> hdlr) {
		_router.route(route).handler(VirtualHostHandler.create(domain, hdlr));
	}
	/**
	 * Registers a GET handler for the specified domain only (can use wildcards)
	 * @param route the route to match (can use wildcards and parameters)
	 * @param domain the domain (or wildcard pattern)
	 * @param hdlr the handler
	 * @since 1.0-alpha
	 */
	public static void get(String route, String domain, Handler<RoutingContext> hdlr) {
		_router.get(route).handler(VirtualHostHandler.create(domain, hdlr));
	}
	/**
	 * Registers a POST handler for the specified domain only (can use wildcards)
	 * @param route the route to match (can use wildcards and parameters)
	 * @param domain the domain (or wildcard pattern)
	 * @param hdlr the handler
	 * @since 1.0-alpha
	 */
	public static void post(String route, String domain, Handler<RoutingContext> hdlr) {
		_router.post(route).handler(VirtualHostHandler.create(domain, hdlr));
	}
	
	/**
	 * Handler class to deal with logging
	 * @author termer
	 * @since 1.0-alpha
	 */
	private static class LoggingHandler implements Handler<RoutingContext> {
		public void handle(RoutingContext r) {
			// Check if logging is enabled
			if((boolean) Twine.config().get("httpLogging")) {
				// Write access log asynchronously
				_vertx.executeBlocking(future -> {
					String str = new Date().toString()+
								 " "+r.request().method().name()+
								 " "+r.request().uri()+
								 " ("+r.request().remoteAddress().host()+
								 " "+r.request().headers().get("User-Agent")+
								 ")";
					System.out.println(str);
					try {
						Writer.append("access.log", str+'\n');
						future.complete();
					} catch (IOException e) {
						Twine.logger().error("Failed to write access log");
						e.printStackTrace();
						future.fail("Error writing to log file");
					}
				}, res -> {
					// Completed
				});
			}
			r.next();
		}
	}
	
	/**
	 * Handler class to deal with domain requests
	 * @author termer
	 * @since 1.0-alpha
	 */
	private static class DomainHandler implements Handler<RoutingContext> {
		public void handle(RoutingContext r) {
			try {
				// Check which domain the request is coming from
				String domain = RequestUtils.domain(r.request().host());
				
				// Check if there's an entry for this domain
				Domain dom = null;
				if(Twine.domains().exists(domain)) {
					dom = Twine.domains().byDomain(domain);
				} else {
					dom = Twine.domains().defaultDomain();
				}
				
				// Get file associated with the request path
				File f = new File(
					RequestUtils.pathToFile(
						URLDecoder.decode(r.request().path(), StandardCharsets.UTF_8.toString()), dom
					)
				);
				
				// Write response or pass it to another handler
				Domain domn = dom;
				_vertx.fileSystem().exists(f.getPath(), exists -> {
					// Handle errors
					if(exists.failed()) {
						Twine.logger().error("Failed to check if file "+f.getName()+"exists:");
						r.fail(exists.cause());
						return;
					}
					
					if(exists.result()) {
						try {
							// Handle processing HTML documents
							if(f.getName().endsWith(".html")) {
								Documents.process(f, domn, r, res -> {
									if(res.succeeded()) {
										// Send response if not ended
										if(!r.response().ended()) {
											if(r.response().headers().get("Content-Type") == null) {
												r.response().putHeader("content-type", "text/html");
											}
											r.response().end(res.result());
										}
									} else {
										Twine.logger().error("Failed to process document "+f.getName()+':');
										
										// Pass to error handler
										r.fail(res.cause());
									}
								});
							} else {
								// Send file with ranges enabled
								sendFile(r, f);
							}
						} catch (IOException e) {
							r.response().sendFile(domn.directory()+domn.serverError()).end();
							e.printStackTrace();
						}
					} else {
						r.next();
					}
				});
			} catch(Exception e) {
				Twine.logger().error("Unknown error occurred");
				e.printStackTrace();
				r.response().end("Unknown error occurred");
			}
		}
	}
	
	/**
	 * Handler class to deal with 404 errors
	 * @author termer
	 * @since 1.0-alpha
	 */
	private static class NotFoundHandler implements Handler<RoutingContext> {
		public void handle(RoutingContext r) {
			String domain = RequestUtils.domain(r.request().host());
			Domain dom = null;
			
			// Select domain
			if(Twine.domains().exists(domain)) {
				dom = Twine.domains().byDomain(domain);
			} else {
				dom = Twine.domains().defaultDomain();
			}
			
			try {
				// Only send 404 if not disabled in config
				if(dom.ignore404()) {
					r.response().setStatusCode(200);
				} else {
					r.response().setStatusCode(404);
				}
				// Process document only if it's HTML
				if(dom.notFound().endsWith(".html")) {
					Documents.process(new File(dom.directory()+dom.notFound()), dom, r, res -> {
						if(res.succeeded()) {
							// Write document to response
							String processed = res.result();
							
							// Write type if not already
							if(!r.response().ended()) {
								if(r.response().headers().get("Content-Type") == null) {
									r.response().putHeader("content-type", "text/html");
								}
								
								// End the response
								r.response().end(processed);
							}
						} else {
							// Pass to error handler
							r.fail(res.cause());
						}
					});
				} else {
					// Send a file with ranges enabled
					sendFile(r, new File(dom.directory()+dom.notFound()));
				}
			} catch (IOException e) {
				// Report error and send 500 page
				Twine.logger().error("Failed to process 404/not found document");
				r.fail(e);
			}
		}
	}
	
	/**
	 * Handler class to deal with server errors
	 * @author termer
	 * @since 1.0-alpha
	 */
	private static class ErrorHandler implements Handler<RoutingContext> {
		public void handle(RoutingContext r) {
			String domain = RequestUtils.domain(r.request().host());
			Domain dom = null;
			
			Twine.logger().error("Internal server error:");
			r.failure().printStackTrace();
			
			// Select domain
			if(Twine.domains().exists(domain)) {
				dom = Twine.domains().byDomain(domain);
			} else {
				dom = Twine.domains().defaultDomain();
			}
			
			try {
				// Send 500 error document
				r.response().sendFile(dom.directory()+dom.serverError());
			} catch(Exception e) {
				// Send generic message if sending file fails
				Twine.logger().error("Failed to send 500 document:");
				e.printStackTrace();
				r.response().end("Internal error");
			}
		}
	}
}