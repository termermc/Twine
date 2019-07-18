package net.termer.twine.documents;

import net.termer.twine.utils.Domains.Domain;

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
	
	/**
	 * Utility class to manipulate a document or how it's treated
	 * @author termer
	 * @since 1.0-alpha
	 */
	public static class DocumentOptions {
		private String _content;
		private String _name;
		private Domain _domain;
		
		public DocumentOptions(String content, String name, Domain domain) {
			_content = content;
			_name = name;
			_domain = domain;
		}
		
		/**
		 * Return's the document's content
		 * @return the document's content
		 * @since 1.0-alpha
		 */
		public String content() {
			return _content;
		}
		/**
		 * Returns the name (usually filename) of the document
		 * @return the name of the document
		 * @since 1.0-alpha
		 */
		public String name() {
			return _name;
		}
		/**
		 * Returns the domain which this document is being served from
		 * @return this document's domain
		 * @since 1.0-alpha
		 */
		public Domain domain() {
			return _domain;
		}
		
		/**
		 * Replaces all instances of the provided instance String with the provided replacement String
		 * @param instance the instance String to replace
		 * @param replacement the String with which to replace the instance String
		 * @since 1.0-alpha
		 */
		public void replace(String instance, String replacement) {
			_content = _content.replace(instance, replacement);
		}
		/**
		 * Replaces all instances of the provided instance regex with the provided replacement String
		 * @param instance the instance regex to replace
		 * @param replacement the String with which to replace the regex String
		 * @since 1.0-alpha
		 */
		public void replaceRegex(String instance, String replacement) {
			_content = _content.replaceAll(instance, replacement);
		}
		/**
		 * Sets the document's content
		 * @param content the content to set
		 * @since 1.0-alpha
		 */
		public void set(String content) {
			_content = content;
		}
	}
}
