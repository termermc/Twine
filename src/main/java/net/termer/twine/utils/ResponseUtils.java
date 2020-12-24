package net.termer.twine.utils;

import io.vertx.core.Future;
import io.vertx.core.file.FileProps;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.impl.MimeMapping;
import io.vertx.ext.web.RoutingContext;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import static net.termer.twine.ServerManager.*;

/**
 * Utility class for manipulating HTTP responses and sending data to them
 * @author termer
 * @since 2.0
 */
public class ResponseUtils {
    private static final SimpleDateFormat cacheDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

    /**
     * Send a file and respect byte range requests, and optionally sends caching headers for the file
     * @param context The RoutingContext for the request to get the byte range from, and response to send the file to
     * @param path The path of the file to send
     * @param sendCachingHeaders Whether to send caching headers for the file
     * @return A future that is completed once the file is sent or failed to send
     */
    public static Future<Void> sendFileRanged(RoutingContext context, String path, boolean sendCachingHeaders) {
        FileSystem fs = vertx().fileSystem();

        return Future.future(promise -> {
            // Open path to check if it's a regular file and fetch its size
            fs.props(path).onComplete(fileRes -> {
                if(fileRes.failed()) {
                    promise.fail(fileRes.cause());
                    return;
                }

                FileProps props = fileRes.result();

                // Make sure path is a regular file
                if(!props.isRegularFile()) {
                    // Cannot send non-regular files
                    promise.fail("Path is not a file, and therefore cannot be sent ranged");
                    return;
                }

                File file = new File(path);

                // Advertise range support
                context.response().putHeader("Accept-Ranges", "bytes");
                context.response().putHeader("vary", "accept-encoding");

                // Write caching headers if enabled
                if(sendCachingHeaders) {
                    context.response().putHeader("date", cacheDateFormat.format(new Date()));
                    context.response().putHeader("cache-control", "public, max-age=86400");
                    context.response().putHeader("last-modified", cacheDateFormat.format(new Date(props.lastModifiedTime())));
                }

                // Check if range requested
                if (context.request().headers().get("Range") == null) {
                    // Send file length on HEAD
                    if (context.request().method() == HttpMethod.HEAD)
                        context.response().putHeader("content-length", Long.toString(props.size()));

                    // Send correct Content-Type
                    String mime = mimeForFilename(file.getName());
                    if(mime != null)
                        context.response().putHeader("Content-Type", mime);

                    // Send full file
                    context.response().sendFile(file.getAbsolutePath());
                } else {
                    // Resolve range parameters
                    String rangeStr = context.request().headers().get("Range").substring(6);
                    long off = Long.parseLong(rangeStr.split("-")[0]);
                    long end = props.size();
                    long len = end;

                    if (!rangeStr.endsWith("-"))
                        end = Long.parseLong(rangeStr.split("-")[1]);

                    // Send segment length on HEAD
                    if (context.request().method() == HttpMethod.HEAD)
                        context.response().putHeader("content-length", Long.toString((end - off) + 1));

                    // Send headers
                    context.response().setStatusCode(206);
                    context.response().putHeader("Content-Range", "bytes " + off + "-" + (end - 1) + "/" + len);

                    // Send file part
                    context.response().sendFile(file.getAbsolutePath(), off, Math.min(end + 1, len));
                }
            });
        });
    }

    /**
     * Send a file and respect byte range requests, and sends caching headers for the file
     * @param context The RoutingContext for the request to get the byte range from, and response to send the file to
     * @param path The path of the file to send
     * @return A future that is completed once the file is sent or failed to send
     */
    public static Future<Void> sendFileRanged(RoutingContext context, String path) {
        return sendFileRanged(context, path, true);
    }

    /**
     * Returns the correct MIME type for the specified filename, or null if none exists
     * @param filename The filename to get MIME type for
     * @return The correct MIME type for the specified filename
     * @since 2.0
     */
    public static String mimeForFilename(String filename) {
        String mime = MimeMapping.getMimeTypeForFilename(filename);

        if(mime != null && mime.startsWith("text/") && !mime.endsWith("charset=UTF-8"))
            mime += ";charset=UTF-8";

        return mime;
    }
}