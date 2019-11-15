package net.termer.twine.documents;

/**
 * Interface for document processing
 * @author termer
 * @since 1.0-alpha
 */
public interface DocumentProcessor {
	/**
	 * Processes or manipulates the provided DocumentOptions object
	 * @param options the document options
	 * @since 1.0-alpha
	 */
	public void process(DocumentOptions options);
}