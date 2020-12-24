package net.termer.twine.handler;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import net.termer.twine.domains.*;
import net.termer.twine.utils.RequestUtils;

import static net.termer.twine.Twine.*;

/**
 * Handler class to deal with server errors
 * @author termer
 * @since 2.0
 */
public class ErrorHandler implements Handler<RoutingContext> {
    public void handle(RoutingContext r) {
        // Resolve domain
        String domain = RequestUtils.domain(r.request().host());
        Domain dom = domains().byHostnameOrDefault(domain);

        logger().error("Internal server error:");
        r.failure().printStackTrace();

        // Send 500 error document
        r.response().sendFile(dom.root()+dom.serverError()).onComplete(res -> {
            if(res.failed()) {
                // Send generic message if sending file fails
                logger().error("Failed to send 500 document:");
                res.cause().printStackTrace();
                if(!r.response().ended())
                    r.response().end("Internal error");
            }
        });
    }
}