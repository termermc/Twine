package net.termer.twine.documents;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import net.termer.twine.ServerManager;
import net.termer.twine.Twine;
import net.termer.twine.documents.processor.ScriptProcessor;
import net.termer.twine.utils.Domains.Domain;
import net.termer.twine.utils.Reader;

/**
 * Utility class to process documents
 * @author termer
 * @since 1.0-alpha
 */
public class Documents {
	// Document processors
	private static ArrayList<DocumentProcessor> _procs = new ArrayList<DocumentProcessor>();
	private static ArrayList<DocumentProcessor> _procsBlocking = new ArrayList<DocumentProcessor>();
	
	// File extensions to process
	private static ArrayList<String> _extensions = new ArrayList<String>();
	
	// Script processor
	private static ScriptProcessor _scriptProc = new ScriptProcessor();
	
	/**
	 * Processes the provided document using available DocumentProcessors
	 * @param doc The document to process
	 * @param name The name of the document
	 * @param domain The domain from which the document was accessed
	 * @param route The RoutingContext for this document retrieval
	 * @param handler The handler to deal with the result of this process
	 * @since 1.0
	 */
	public static void process(String doc, String name, Domain domain, RoutingContext route, Handler<AsyncResult<String>> handler) {
		DocumentOptions ops = new DocumentOptions(doc, name, domain, _procs.toArray(new DocumentProcessor[0]), route);
		
		// Run registered processors
		ops.execute(res -> {
			if(res.succeeded()) {
				// Execute blocking processors (if any)
				if(_procsBlocking.size() > 0) {
					ServerManager.vertx().executeBlocking(promise -> {
						ops
							.processors(_procsBlocking.toArray(new DocumentProcessor[0]))
							.execute(opsRes -> {
								if(opsRes.succeeded()) {
									// Return result
									promise.complete();
								} else {
									promise.fail(opsRes.cause());
								}
							});
					}, codeRes -> {
						if(codeRes.succeeded()) {
							// Execute script processor
							_scriptProc.process(ops);
							
							// Finish
							handler.handle(Future.succeededFuture(ops.content()));
						} else {
							handler.handle(Future.failedFuture(codeRes.cause()));
						}
					});
				} else {
					// Execute script processor
					_scriptProc.process(ops);
					
					// Finish
					handler.handle(Future.succeededFuture(ops.content()));
				}
			} else {
				handler.handle(Future.failedFuture(res.cause()));
			}
		});
	}
	
	/**
	 * Processes the provided document using available ScriptProcessors and scripts
	 * @param doc The document
	 * @param domain The domain from which the document was accessed
	 * @param route The RoutingContext for this document retrieval
	 * @param handler The handler to deal with the result of this process
	 * @throws IOException if reading the document fails
	 * @since 1.0
	 */
	public static void process(File doc, Domain domain, RoutingContext route, Handler<AsyncResult<String>> handler) throws IOException {
		// Read file
		ServerManager.vertx().fileSystem().readFile(doc.getAbsolutePath(), res -> {
			if(res.succeeded()) {
				String document = res.result().toString(Charset.defaultCharset());
				
				// Pass to normal processor
				process(document, doc.getName(), domain, route, handler);
			} else {
				handler.handle(Future.failedFuture(res.cause()));
			}
		});
	}
	
	/**
	 * Registers a normal (non-blocking) document processor
	 * @param proc The DocumentProcessor
	 * @since 1.0
	 */
	public static void processor(DocumentProcessor proc) {
		_procs.add(proc);
	}
	/**
	 * Registers a blocking DocumentProcessor. Note that blocking processors are executed after non-blocking ones.
	 * @param proc The DocumentProcessor
	 * @since 1.0
	 */
	public static void blockingProcessor(DocumentProcessor proc) {
		_procsBlocking.add(proc);
	}
	
	/**
	 * Utility class to pass to embedded scripts
	 * @author termer
	 * @since 1.0-alpha
	 */
	public static class Out {
		private Domain _dom;
		private StringBuilder _sb = new StringBuilder();
		
		public Out(Domain domain) {
			_dom = domain;
		}
		
		/**
		 * Appends a value to this Out object
		 * @param val the value to append
		 * @param sanitize whether to sanitize the appended content for HTML injection
		 * @return this Out object, so it can be used fluently
		 * @since 1.0-alpha
		 */
		public Out append(Object val, boolean sanitize) {
			if(val == null) {
				_sb.append("null");
			} else {
				_sb.append(sanitize ? clean(val.toString()) : val.toString());
			}
			return this;
		}
		/**
		 * Appends a value to this Out object
		 * @param val the value to append
		 * @return this Out object, so it can be used fluently
		 * @since 1.0-alpha
		 */
		public Out append(Object val) {
			append(val, false);
			return this;
		}
		/**
		 * Appends the specified file (relative to the domain associated with this Out) to this Out object.
		 * Recursively appends all files in a directory if specified path is a directory
		 * @param path the path of the file or directory
		 * @param whether to sanitize the file(s) for HTML injection
		 * @return this Out object, so it can be used fluently
		 * @throws IOException if reading the file(s) fails
		 * @since 1.0-alpha
		 */
		public Out include(String path, boolean sanitize) throws IOException {
			if(path.startsWith("/")) path = path.substring(1);
			File f = new File(_dom.directory()+path);
			
			if(f.isDirectory()) {
				for(File file : f.listFiles()) {
					if(file.isDirectory()) {
						include(file.getPath().substring(_dom.directory().length()));
					} else {
						String content = Reader.read(file);
						
						// Remove script header
						if(content.startsWith("<!--TES-->")) {
							content = content.substring(10);
						}
						_sb.append(sanitize ? clean(content) : content);
					}
				}
			} else {
				String content = Reader.read(f);
				
				// Remove script header
				if(content.startsWith("<!--TES-->")) {
					content = content.substring(10);
				}
				_sb.append(sanitize ? clean(content) : content);
			}
			
			return this;
		}
		/**
		 * Appends the specified file (relative to the domain associated with this Out) to this Out object.
		 * Appends all files in a directory if specified path is one
		 * @param path the path of the file or directory
		 * @return this Out object, so it can be used fluently
		 * @throws IOException if reading the file(s) fails
		 * @since 1.0-alpha
		 */
		public Out include(String path) throws IOException {
			include(path, false);
			
			return this;
		}
		/**
		 * Appends an HTML <br/> tag to this Out object
		 * @return this Out object, so it can be used fluently
		 * @since 1.0-alpha
		 */
		public Out br() {
			_sb.append("<br/>");
			return this;
		}
		
		/**
		 * Returns the value of this Out object
		 * @since 1.0-alpha
		 */
		public String toString() {
			return _sb.toString();
		}
		/**
		 * Sanitizes the provided HTML
		 * @param html the html to sanitize
		 * @return the sanitized HTML
		 * @since 1.0-alpha
		 */
		public String clean(String html) {
			return html
					.replace("&", "&amp;")
					.replace("<", "&lt;")
					.replace(">", "&gt;")
					.replace("\"", "&quot;");
		}
	}
}