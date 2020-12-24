package net.termer.twine.documents;

import io.vertx.core.Future;

/**
 * Interface for document processing
 * @author termer
 * @since 1.0-alpha
 */
public interface DocumentProcessor {
	/**
	 * Processes or manipulates the provided DocumentOptions object
	 * @param options the document options
	 * @since 2.0
	 */
	void process(DocumentOptions options);
}