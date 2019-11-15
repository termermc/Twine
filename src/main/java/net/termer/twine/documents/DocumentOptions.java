package net.termer.twine.documents;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import net.termer.twine.utils.Domains.Domain;

/**
 * Utility class to manipulate a document or how it's treated
 * @author termer
 * @since 1.0
 */
public class DocumentOptions {
	private final RoutingContext _route;
	private DocumentProcessor[] _procs;
	private Domain _domain;
	private String _name;
	private String _content;
	private int _procIndex = 0;
	private Handler<AsyncResult<DocumentOptions>> _handler = null;
	
	/**
	 * Instantiates a new DocumentOptions object
	 * @param content The content of the document being processed
	 * @param name The name of the document being processed
	 * @param domain The domain of the document being processed
	 * @param processors The processors to execute on this document
	 * @param route The RoutingContext object for processors to use
	 * @since 1.0
	 */
	protected DocumentOptions(String content, String name, Domain domain, DocumentProcessor[] processors, RoutingContext route) {
		_content = content;
		_name = name;
		_domain = domain;
		_procs = processors;
		_route = route;
	}
	
	/**
	 * Assigns new DocumentProcessors
	 * @param procs The new DocumentProcessors
	 * @return This, to be used fluently
	 * @since 1.0
	 */
	protected DocumentOptions processors(DocumentProcessor[] procs) {
		_procs = procs;
		return this;
	}
	
	/**
	 * Returns the RoutingContext for the request that requested this document
	 * @return The request's RoutingContext object
	 * @since 1.0
	 */
	public RoutingContext route() {
		return _route;
	}
	
	/**
	 * Return's the document's content
	 * @return The document's content
	 * @since 1.0
	 */
	public String content() {
		return _content;
	}
	/**
	 * Sets the document's content
	 * @param content The content to set
	 * @return This, to be used fluently
	 * @since 1.0
	 */
	public DocumentOptions content(String content) {
		_content = content;
		return this;
	}
	
	/**
	 * Returns the name (usually filename) of the document
	 * @return The name of the document
	 * @since 1.0
	 */
	public String name() {
		return _name;
	}
	/**
	 * Sets the name returned by name(). Does not actually change the name of the document being processed on disk.
	 * @param name The new name of the document
	 * @return This, to be used fluently
	 * @since 1.0
	 */
	public DocumentOptions name(String name) {
		_name = name;
		return this;
	}
	
	/**
	 * Returns the domain which this document is being served from
	 * @return This document's domain
	 * @since 1.0
	 */
	public Domain domain() {
		return _domain;
	}
	/**
	 * Sets the domain returned by domain(). Does not actually change the location of the document on disk.
	 * @param domain The new domain of the document
	 * @return This, to be used fluently
	 * @since 1.0
	 */
	public DocumentOptions domain(Domain domain) {
		_domain = domain;
		return this;
	}
	
	/**
	 * Replaces all instances of the provided instance String with the provided replacement String
	 * @param instance The instance String to replace
	 * @param replacement The String with which to replace the instance String
	 * @return This, to be used fluently
	 * @since 1.0
	 */
	public DocumentOptions replace(String instance, String replacement) {
		_content = _content.replace(instance, replacement);
		return this;
	}
	/**
	 * Replaces all instances of the provided instance regex with the provided replacement String
	 * @param instance The instance regex to replace
	 * @param replacement The String with which to replace the regex String
	 * @return This, to be used fluently
	 * @since 1.0
	 */
	public DocumentOptions replaceRegex(String instance, String replacement) {
		_content = _content.replaceAll(instance, replacement);
		return this;
	}
	
	/**
	 * Executes all processors and then returns the finished DocumentOptions object
	 * @param handler The handler to receive the finished document
	 * @since 1.0
	 */
	public void execute(Handler<AsyncResult<DocumentOptions>> handler) {
		_procIndex = 0;
		if(_procs.length > 0)
			_procs[0].process(this);
		else
			handler.handle(Future.succeededFuture(this));
	}
	/**
	 * Executes the next DocumentProcessor, or finishes if there is none
	 * @since 1.0
	 */
	public void next() {
		_procIndex++;
		if(_procIndex < _procs.length)
			_procs[_procIndex].process(this);
		else
			_handler.handle(Future.succeededFuture(this));
	}
	/**
	 * Ends all processors and finishes the result
	 * @since 1.0
	 */
	public void end() {
		_procIndex = _procs.length;
		_handler.handle(Future.succeededFuture(this));
	}
	/**
	 * Ends all processors with an error
	 * @param error The error which caused this processor to fail
	 * @since 1.0
	 */
	public void fail(Throwable error) {
		_procIndex = _procs.length;
		_handler.handle(Future.failedFuture(error));
	}
}