package net.termer.twine.handler;

import io.vertx.core.Handler;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
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
                JsonObject jsonBody = r.getBodyAsJson();

                if(jsonBody != null) {
                    // Parse JSON to Map
                    Map<String, Object> json = jsonBody.getMap();

                    // Add data to form attributes and params
                    for(String key : json.keySet()) {
                        String str = json.get(key) == null ? json.get(key).toString() : "false";

                        r.request().formAttributes().add(key, str);
                        r.request().params().add(key, str);
                    }
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
