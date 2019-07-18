package net.termer.twine.documents;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import bsh.EvalError;
import bsh.Interpreter;
import io.vertx.ext.web.RoutingContext;
import net.termer.twine.Twine;
import net.termer.twine.documents.DocumentProcessor.DocumentOptions;
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
	
	/**
	 * Processes the provided document using available ScriptProcessors and scripts
	 * @param doc the document to process
	 * @param name the name of the document
	 * @param domain the domain from which the document was accessed
	 * @param route the RoutingContext for this document retrieval
	 * @return the processed document
	 * @since 1.0-alpha
	 */
	public static String process(String doc, String name, Domain domain, RoutingContext route, HashMap<String, Object> vars) {
		DocumentOptions ops = new DocumentOptions(doc, name, domain);
		
		// Run registered processors
		for(DocumentProcessor proc : _procs) {
			proc.process(ops);
		}
		String cont = ops.content();
		
		// Check if scripting is enabled
		if((Boolean) Twine.config().get("scripting")) {
			if(cont.startsWith("<!--TES-->")) {
				cont = cont.substring(11);
				if(cont.startsWith("!")) cont = cont.substring(1);
				
				// Process scripts
				if(cont.contains("<?java") && cont.contains("?>")) {
					vars.put("domain", domain);
					vars.put("name", name);
					vars.put("request", route.request());
					vars.put("response", route.response());
					vars.put("route", route);
					
					int index = 0;
					boolean proceed = true;
					Interpreter inter = new Interpreter();
					
					// Add variables
					String[] keys = vars.keySet().toArray(new String[0]);
					Object[] values = vars.values().toArray(new Object[0]);
					
					for(int i = 0; i < keys.length; i++) {
						try {
							inter.set(keys[i], values[i]);
						} catch (EvalError ex) {
							ex.printStackTrace();
						}
					}
					
					// While there are more scripts available
					while(proceed) {
						int opening = cont.indexOf("<?java", index)+6;
						int closing = cont.indexOf("?>", index);
						if(opening > 5 && closing > -1) {
							String script = cont.substring(opening, closing);
							
							Out result = new Out(domain);
							try {
								inter.set("out", result);
								inter.eval(script.trim());
							} catch (Exception e) {
								e.printStackTrace();
								// Append error if enabled
								if((Boolean) Twine.config().get("scriptExceptions")) {
									result.append(e.getMessage());
								}
							}
							cont = cont.replace("<?java"+script+"?>", result.toString());
							index++;
						} else {
							proceed = false;
						}
					}
				}
			}
		}
		
		return cont;
	}
	
	/**
	 * Processes the provided document using available ScriptProcessors and scripts
	 * @param doc the document
	 * @param domain the domain from which the document was accessed
	 * @param route the RoutingContext for this document retrieval
	 * @return the processed document
	 * @throws IOException if reading the document fails
	 * @since 1.0-alpha
	 */
	public static String process(File doc, Domain domain, RoutingContext route, HashMap<String, Object> vars) throws IOException {
		StringBuilder sb = new StringBuilder();
		
		// Read file
		FileInputStream fin = new FileInputStream(doc);
		while(fin.available() > 0) {
			sb.append((char)fin.read());
		}
		fin.close();
		
		return process(sb.toString(), doc.getName(), domain, route, vars);
	}
	
	/**
	 * Registers a document processor
	 * @param proc the DocumentProcessor
	 * @since 1.0-alpha
	 */
	public static void processor(DocumentProcessor proc) {
		_procs.add(proc);
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