package net.termer.twine.handler;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import net.termer.twine.documents.Documents;
import net.termer.twine.domains.*;
import net.termer.twine.utils.RequestUtils;
import net.termer.twine.utils.ResponseUtils;

import java.io.File;

import static net.termer.twine.ServerManager.vertx;
import static net.termer.twine.Twine.*;

/**
 * Handler class to deal with 404 errors
 * @author termer
 * @since 2.0
 */
public class NotFoundHandler implements Handler<RoutingContext> {
    public void handle(RoutingContext r) {
        // Resolve domain
        String domain = RequestUtils.domain(r.request().host());
        Domain dom = domains().byHostnameOrDefault(domain);

        // Only send 404 if not disabled in config
        r.response().setStatusCode(dom.ignore404() ? 200 : 404);

        // Get File object for not found file
        File file = new File(dom.root()+dom.notFound());

        // Write response or pass it to another handler
        vertx().fileSystem().exists(file.getPath()).onComplete(exists -> {
            // Handle errors
            if(exists.failed()) {
                logger().error("Failed to check if file "+file.getName()+"exists");
                r.fail(exists.cause());
                return;
            }

            if(exists.result()) {
                // Handle processing documents
                if(Documents.isValidExtension(file.getName())) {
                    Documents.process(file, dom, r).onComplete(res -> {
                        if(res.succeeded()) {
                            // Write document to response
                            String processed = res.result();

                            // Write type if not already
                            if(!r.response().ended()) {
                                // Set content type to text/html if not already set
                                if(r.response().headers().get("Content-Type") == null)
                                    r.response().putHeader("content-type", "text/html;charset=UTF-8");

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
                    ResponseUtils.sendFileRanged(r, file.getAbsolutePath(), (boolean) config().getNode("server.static.caching"));
                }
            } else {
                // Send generic 404 message if the 404 file cannot be found
                r.end("Not found");
            }
        });
    }
}