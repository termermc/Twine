package net.termer.twine.handler;

import io.vertx.core.Handler;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.List;
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
                        // Only add attribute if not null
                        Object val = json.get(key);
                        if(val != null) {
                            String str;

                            // Handle specific JSON types and serialize them
                            if(val instanceof Map)
                                str = new JsonObject((Map<String, Object>) val).toString();
                            else if(val instanceof List)
                                str = new JsonArray((List<Object>) val).toString();
                            else
                                str = val.toString();

                            r.request().formAttributes().add(key, str);
                            r.request().params().add(key, str);
                        }
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
