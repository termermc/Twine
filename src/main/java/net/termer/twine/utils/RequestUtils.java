package net.termer.twine.utils;

import java.util.ArrayList;
import java.util.Collections;

import io.vertx.core.Future;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServerRequest;
import net.termer.twine.documents.Documents;
import net.termer.twine.domains.*;

import static net.termer.twine.ServerManager.*;
import static net.termer.twine.Twine.config;

/**
 * Utility class for dealing with requests
 * @author termer
 * @since 1.0-alpha
 */
public class RequestUtils {
	/**
	 * Transforms the specified host string into a domain string, or returns "default" if host is null
	 * @param host the host string
	 * @return the domain string
	 * @since 1.0-alpha
	 */
	public static String domain(String host) {
		// Return "default" for null hosts
		if(host == null) {
			return "default";
		} else {
			String dom = host.toLowerCase();
			if(host.contains(":")) {
				dom = host.split(":")[0];
			}
			return dom;
		}
	}

	/**
	 * Returns a list of all possible paths on disk for the specified path on the provided domain
	 * @param path The path to get possible on disk paths for
	 * @param dom The domain this path is on
	 * @return A list of all possible paths on disk for the specified path on the provided domain
	 * @since 2.0
	 */
	public static String[] possibleFilePaths(String path, Domain dom) {
		String pth = path;

		if(pth.equals("/")) {
			return new String[] { dom.root()+dom.index() };
		} else {
			// Pre-process path and clean it
			if(pth.startsWith("..")) pth = pth.substring(2);
			if(pth.startsWith("/")) pth = pth.substring(1);
			if(pth.endsWith("/")) pth = pth.substring(0, pth.length()-1);
			pth = dom.root()+pth;

			// List of possible paths, defaulting to containing just the provided path
			ArrayList<String> paths = new ArrayList<>(Collections.singleton(pth));

			// For every registered document extension, add an index file with that extension
			for(String ext : Documents.extensions())
				paths.add(pth+"/index."+ext);

			return paths.toArray(new String[0]);
		}
	}

	/**
	 * Resolves what file the provided path should point to, taking into account directory indexes with extensions registered in Documents.
	 * If no file for the path is found, this method will return null.
	 * @param path The path to determine a file for
	 * @param dom The domain that this path resides on
	 * @return A future that returns the path to the file selected, or null if no file is found
	 * @since 2.0
	 */
	public static Future<String> resolveFileByPath(String path, Domain dom) {
		return Future.future(promise -> {
			CallbackChain<String> chain = new CallbackChain<>();
			String[] possiblePaths = possibleFilePaths(path, dom);
			FileSystem fs = vertx().fileSystem();

			// For each possible path, create a callback in the chain to check if it exists
			for(String possiblePath : possiblePaths) {
				final String pth = possiblePath;

				chain.then(action -> {
					fs.exists(pth).onComplete(res -> {
						if(res.succeeded()) {
							// Check if the file exists, then check whether it's a file
							if(res.result()) {
								// Check if it's a file
								fs.props(pth).onComplete(propsRes -> {
									if(propsRes.succeeded()) {
										if(propsRes.result().isRegularFile()) {
											action.end(pth);
										} else {
											action.next();
										}
									} else {
										action.fail(propsRes.cause());
									}
								});
							} else {
								action.next();
							}
						} else {
							action.fail(res.cause());
						}
					});
				});
			}

			// Get result from chain
			chain.onEnd(res -> {
				if(res.succeeded()) {
					promise.complete(res.result());
				} else {
					promise.fail(res.cause());
				}
			}).execute();
		});
	}

	/**
	 * Returns the IP address of provided request's connection, optionally respecting X-Forwarded-For headers
	 * @param req The request to get the IP address for
	 * @param respectXForwardedFor Whether to respect X-Forwarded-For headers
	 * @return The IP address of provided request's connection
	 * @since 2.0
	 */
	public static String resolveIp(HttpServerRequest req, boolean respectXForwardedFor) {
		if(respectXForwardedFor && req.headers().contains("X-Forwarded-For")) {
			return req.getHeader("X-Forwarded-For");
		} else {
			return req.connection().remoteAddress().host();
		}
	}
	/**
	 * Returns the IP address of provided request's connection, following Twine's "respectXFF" setting for respecting X-Forwarded-For headers.
	 * This is method is recommended for Twine modules as it automatically respects Twine's configuration.
	 * @param req The request to get the IP address for
	 * @return The IP address of provided request's connection
	 * @since 2.0
	 */
	public static String resolveIp(HttpServerRequest req) {
		return resolveIp(req, (boolean) config().getNode("server.respectXFF"));
	}
}