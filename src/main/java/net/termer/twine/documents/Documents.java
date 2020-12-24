package net.termer.twine.documents;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.termer.twine.domains.Domain;

import static net.termer.twine.ServerManager.*;

/**
 * Utility class to process documents
 * @author termer
 * @since 1.0-alpha
 */
public class Documents {
	// Document processors
	private static final ArrayList<DocumentProcessor> _procs = new ArrayList<>();
	
	// File extensions to process
	private static final ArrayList<String> _extensions = new ArrayList<>(Collections.singletonList("html"));
	
	/**
	 * Processes the provided document using available DocumentProcessors
	 * @param doc The document to process
	 * @param name The name of the document
	 * @param extension The extension of the document to process
	 * @param domain The domain from which the document was accessed
	 * @param route The RoutingContext for this document retrieval
	 * @return A future that returns the result of this process
	 * @since 2.0
	 */
	public static Future<String> process(String doc, String name, String extension, Domain domain, RoutingContext route) {
		return Future.future(promise -> {
			if(_procs.size() > 0) {
				DocumentOptions ops = new DocumentOptions(doc, name, extension, domain, _procs.toArray(new DocumentProcessor[0]), route);

				// Run registered processors and return result
				ops.execute().onComplete(res -> {
					if(res.succeeded()) {
						promise.complete(res.result().content());
					} else {
						promise.fail(res.cause());
					}
				});
			} else {
				// Since there are no processors, just complete with the initial content
				promise.complete(doc);
			}
		});
	}
	
	/**
	 * Processes the provided document using available ScriptProcessors and scripts
	 * @param doc The document
	 * @param domain The domain from which the document was accessed
	 * @param route The RoutingContext for this document retrieval
	 * @return A future that returns the result of this process
	 * @since 2.0
	 */
	public static Future<String> process(File doc, Domain domain, RoutingContext route) {
		return Future.future(promise -> {
			// Read file
			vertx().fileSystem().readFile(doc.getAbsolutePath()).onComplete(res -> {
				if(res.succeeded()) {
					String document = res.result().toString(Charset.defaultCharset());

					// Pass to normal processor
					String name = doc.getName();
					String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')+1) : "";
					process(document, name, ext, domain, route).onComplete(promise);
				} else {
					promise.fail(res.cause());
				}
			});
		});
	}
	
	/**
	 * Registers a normal (non-blocking) document processor
	 * @param proc The DocumentProcessor
	 * @since 2.0
	 */
	public static void registerProcessor(DocumentProcessor proc) {
		_procs.add(proc);
	}
	
	/**
	 * Registers a file extension to be run through document processors
	 * @param extension The file extension to register, e.g. "txt"
	 * @since 1.0
	 */
	public static void registerExtension(String extension) {
		_extensions.add(extension);
	}
	
	/**
	 * Checks if the provided filename contains a valid extension to be processed
	 * @param filename The name of the file to check the extension of
	 * @return Whether this filename contains a valid extension to be processed
	 * @since 2.0
	 */
	public static boolean isValidExtension(String filename) {
		boolean valid = false;
		
		// Check extension against list of extensions
		if(filename.contains(".")) {
			String[] parts = filename.split("\\.");
			valid = _extensions.contains(parts[parts.length-1]);
		}
		
		return valid;
	}

	/**
	 * Returns all registered document extensions
	 * @return All registered document extensions
	 * @since 2.0
	 */
	public static String[] extensions() {
		return _extensions.toArray(new String[0]);
	}
}