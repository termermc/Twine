package net.termer.twine.handler;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import net.termer.twine.documents.Documents;
import net.termer.twine.domains.*;
import net.termer.twine.utils.RequestUtils;
import net.termer.twine.utils.ResponseUtils;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static net.termer.twine.Twine.*;

/**
 * Handler class to deal with domain requests
 * @author termer
 * @since 2.0
 */
public class DomainHandler implements Handler<RoutingContext> {
    public void handle(RoutingContext r) {
        try {
            // Resolve domain
            String domain = RequestUtils.domain(r.request().host());
            Domain dom = domains().byHostnameOrDefault(domain);

            // Setup CORS headers
            if(dom.cors().enabled()) {
                // Origin
                if(dom.cors().allowedOrigin() != null) {
                    String origin = dom.cors().allowedOrigin();

                    if(origin.equals("request-origin"))
                        if(r.request().headers().contains("Origin"))
                            origin = r.request().getHeader("Origin");
                        else
                            origin = "*";

                    r.response().putHeader("Access-Control-Allow-Origin", origin);
                }

                // Allowed AJAX/XHR methods
                if(dom.cors().allowedMethods() != null)
                    r.response().putHeader("Access-Control-Allow-Methods", String.join(", ", dom.cors().allowedMethods()));

                // Allowed AJAX/XHR headers
                if(dom.cors().allowedHeaders() != null)
                    r.response().putHeader("Access-Control-Allow-Headers", String.join(", ", dom.cors().allowedHeaders()));

                // Allow credentials
                r.response().putHeader("Access-Control-Allow-Credentials", Boolean.toString(dom.cors().allowCredentials()));
            }

            // Decode path
            String path = URLDecoder.decode(r.request().path(), StandardCharsets.UTF_8.toString());

            // Get file associated with the request path
            RequestUtils.resolveFileByPath(path, dom).onComplete(res -> {
                if(res.succeeded()) {
                    // Check if the path returned is null, meaning the file could not be found
                    if(res.result() == null) {
                        r.next();
                        return;
                    }

                    // File object for path (used for parsing filename, not for using I/O functions)
                    File file = new File(res.result());

                    // Handle processing document
                    if(Documents.isValidExtension(file.getName())) {
                        Documents.process(file, dom, r).onComplete(docRes -> {
                            if(docRes.succeeded()) {
                                // Send response if not ended
                                if(!r.response().ended()) {
                                    // Set content type to text/html if not already set
                                    if(r.response().headers().get("Content-Type") == null)
                                        r.response().putHeader("content-type", ResponseUtils.mimeForFilename(file.getName()));

                                    // Send processed document
                                    r.response().end(docRes.result());
                                }
                            } else {
                                logger().error("Failed to process document "+file.getName());

                                // Pass to error handler
                                r.fail(docRes.cause());
                            }
                        });
                    } else if(!r.response().ended()) {
                        // Send file with ranges enabled
                        ResponseUtils.sendFileRanged(r, file.getAbsolutePath(), (boolean) config().getNode("server.static.caching"));
                    }
                } else {
                    r.fail(res.cause());
                }
            });
        } catch(Exception e) {
            logger().error("Unknown error occurred");
            e.printStackTrace();
            r.response().end("Unknown error occurred");
        }
    }
}