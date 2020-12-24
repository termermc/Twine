package net.termer.twine.modules;

/**
 * Interface to be implemented by Twine modules
 * @author termer
 * @since 1.0-alpha
 */
public interface TwineModule {
	/**
	 * Enumeration to signify what priority at which a module should be loaded
	 * @author termer
	 * @since 1.0-alpha
	 */
	enum Priority {
		HIGH,
		MEDIUM,
		LOW
	}
	
	/**
	 * The name of the module
	 * @return the name of the module
	 * @since 1.0-alpha
	 */
	String name();
	/**
	 * The priority at which this module should be loaded
	 * @return this module's load priority
	 * @since 1.0-alpha
	 */
	Priority priority();
	/**
	 * The version of Twine for which this module was designed.
	 * End it with a "+" to signify that any version after the provided version may be used.
	 * Example: "1.0+" 
	 * @return the version of Twine for which this module was designed
	 * @since 1.0-alpha
	 */
	String twineVersion();
	
	/**
	 * The method called when the module is loaded
	 * @since 1.0-alpha
	 */
	void initialize();
	/**
	 * The method called when the module is shut down
	 * @since 1.0-alpha
	 */
	void shutdown();
	
	/**
	 * The method called before any handlers are registered by Twine.
	 * Any handlers registered here will have to handle body parsing themselves, as no BodyHandler will be registered until initalize() has been called.
	 * Appropriate for upload handlers, and other types of handlers that need to stream request body content.
	 * @since 1.3
	 */
	void preinitialize();
}
