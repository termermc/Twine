package net.termer.twine.handler;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import net.termer.twine.domains.Domain;
import net.termer.twine.utils.RequestUtils;
import net.termer.twine.utils.files.BlockingWriter;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static net.termer.twine.Twine.*;

/**
 * Handler class to deal with logging
 * @author termer
 * @since 2.0
 */
public class LoggingHandler implements Handler<RoutingContext> {
    private final BlockingQueue<String> writeQueue = new LinkedBlockingQueue<>();

    public LoggingHandler() {
        // Setup and start writing thread
        Thread writerThread = new Thread(() -> {
            while(true) {
                try {
                    String ln = writeQueue.take()+'\n';
                    BlockingWriter.append((String) config().getNode("server.logging.file"), ln);
                } catch (IOException e) {
                    logger().error("Failed to write to access log:");
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    logger().error("Failed to read access log message queue:");
                    e.printStackTrace();
                }
            }
        });
        writerThread.setName("LogWritingThread");
        writerThread.setDaemon(true);
        writerThread.start();
    }

    public void handle(RoutingContext r) {
        // Check if logging is enabled
        if((boolean) config().getNode("server.logging.enable")) {
            // Resolve domain
            String domain = RequestUtils.domain(r.request().host());
            Domain dom = domains().byHostnameOrDefault(domain);

            // Create log line
            String ln = new Date().toString()+
                    " "+r.request().method().name()+
                    " "+r.request().uri()+
                    " ["+dom.name()+
                    "] ("+r.request().remoteAddress().host()+
                    " "+r.request().headers().get("User-Agent")+
                    ")";

            // Print it
            System.out.println(ln);

            // Put line in the write queue
            writeQueue.add(ln);
        }

        // Pass to next handler
        r.next();
    }
}