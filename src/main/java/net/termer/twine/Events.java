package net.termer.twine;

import java.util.ArrayList;
import java.util.HashMap;

import io.vertx.core.json.JsonObject;
import net.termer.twine.utils.TwineEvent;

import static net.termer.twine.ServerManager.vertx;

/**
 * Class to handle custom server event callbacks
 * @author termer
 * @since 1.0-alpha
 */
public class Events {
	/**
	 * Enumeration of event types
	 * @author termer
	 * @since 1.0-alpha
	 */
	public enum Type {
		/**
		 * Event fired when the server's configuration files are being reloaded.
		 * This event can be cancelled.
		 * @since 1.0-alpha
		 */
		CONFIG_RELOAD,
		/**
		 * Event fired when the server is starting.
		 * This event cannot be cancelled.
		 * @since 1.0-alpha
		 */
		SERVER_START,
		/**
		 * Event fired when the server is shutting down.
		 * This event can be cancelled.
		 * @since 1.0-alpha
		 */
		SERVER_STOP,
		/**
		 * Event fired when all modules are loaded.
		 * This event cannot be cancelled.
		 * @since 1.5
		 */
		MODULES_LOADED,
		/**
		 * Event fired when all modules' preinitialize() methods have been called.
		 * This event cannot be cancelled.
		 * @since 1.5
		 */
		MODULES_PREINITIALIZED,
		/**
		 * Event fired when all modules' initialize() methods have been called.
		 * This event cannot be cancelled.
		 * @since 1.5
		 */
		MODULES_INITIALIZED,
		/**
		 * Event fired when the server is initialized and is in a cluster.
		 * This event cannot be cancelled.
		 * @since 1.5
		 */
		CLUSTER_JOIN
	}
	
	private static final HashMap<Type, ArrayList<TwineEvent>> _events = new HashMap<>();
	private static final HashMap<Type, ArrayList<TwineEvent>> _async = new HashMap<>();
	
	/**
	 * Registers a blocking event callback.
	 * The important difference between async and blocking event handlers are that
	 * 1. Blocking handlers block Twine's main thread (doesn't affect Vert.x or the webserver), whereas async handlers are run on the Vert.x event loop, and
	 * 2. Blocking handlers can cancel the event they're called for, async handlers cannot.
	 * In general, if you do not need to cancel
	 * @param type the event type
	 * @param callback the callback
	 * @since 1.0-alpha
	 */
	public static void on(Type type, TwineEvent callback) {
		if(!_events.containsKey(type)) _events.put(type, new ArrayList<>());
		_events.get(type).add(callback);
	}
	/**
	 * Registers an async event callback.
	 * Async event callbacks cannot manipulate the event.
	 * @param type the event type
	 * @param callback the callback
	 * @since 1.0-alpha
	 */
	public static void onAsync(Type type, TwineEvent callback) {
		if(!_async.containsKey(type)) _async.put(type, new ArrayList<>());
		_async.get(type).add(callback);
	}
	
	/**
	 * Fires all callbacks (sync and async) for the specified event type
	 * @param type the event type
	 * @return whether the event is able to run (e.g. it wasn't cancelled by a callback)
	 * @since 1.0-alpha
	 */
	public static boolean fire(Type type) {
		// Avoid NullPointerException
		if(!_events.containsKey(type)) _events.put(type, new ArrayList<>());
		if(!_async.containsKey(type)) _async.put(type, new ArrayList<>());
		
		// Publish event to event bus
		vertx().eventBus().publish(
			"twine.events",
			new JsonObject()
				.put("event", type.toString())
				.put("instance", Twine.INSTANCE_ID)
		);
		
		// Fire async events on the Vert.x context
		vertx().runOnContext((_void) -> {
			for(TwineEvent evt : _async.get(type)) {
				// Feed callback a dummy Options object
				evt.callback(new TwineEvent.Options());
			}
		});
		
		// Fire blocking events
		TwineEvent.Options options = new TwineEvent.Options();
		for(TwineEvent evt : _events.get(type)) {
			evt.callback(options);
		}
		
		return !options.cancelled();
	}
}