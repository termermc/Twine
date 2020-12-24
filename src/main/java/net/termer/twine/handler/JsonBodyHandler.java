package net.termer.twine.handler;

import io.vertx.core.Handler;
import io.vertx.core.json.DecodeException;
import io.vertx.ext.web.RoutingContext;

import java.util.Map;

/**
 * Handler class to deal with JSON request bodies
 * @author termer
 * @since 2.0
 */
public class JsonBodyHandler implements Handler<RoutingContext> {
    public void handle(RoutingContext r) {
        // Check if Content-Type is JSON
        String contentType = r.request().getHeader("Content-Type");
        if(contentType != null && (contentType.equalsIgnoreCase("application/json") || contentType.equalsIgnoreCase("text/json"))) {
            try {
                // Parse JSON to Map
                Map<String, Object> json = r.getBodyAsJson().getMap();

                // Add data to form attributes and params
                for(String key : json.keySet()) {
                    r.request().formAttributes().add(key, json.get(key).toString());
                    r.request().params().add(key, json.get(key).toString());
                }
            } catch (DecodeException e) {
                // The body had a syntax error in it, send a 400 error
                r.response().setStatusCode(400);
                r.response().setStatusMessage("Malformed JSON body");
                r.response().end();
            }
        }

        // Pass to next handler if the response hasn't already ended
        if(!r.response().ended())
            r.next();
    }
}
