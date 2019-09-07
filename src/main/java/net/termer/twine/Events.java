package net.termer.twine;

import java.util.ArrayList;
import java.util.HashMap;

import net.termer.twine.utils.TwineEvent;

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
	public static enum Type {
		CONFIG_RELOAD,
		SERVER_START,
		SERVER_STOP
	}
	
	private static HashMap<Type, ArrayList<TwineEvent>> _events = new HashMap<Type, ArrayList<TwineEvent>>();
	private static HashMap<Type, ArrayList<TwineEvent>> _async = new HashMap<Type, ArrayList<TwineEvent>>();
	
	/**
	 * Registers a blocking event callback
	 * @param type the event type
	 * @param callback the callback
	 * @since 1.0-alpha
	 */
	public static void on(Type type, TwineEvent callback) {
		if(!_events.containsKey(type)) _events.put(type, new ArrayList<TwineEvent>());
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
		if(!_async.containsKey(type)) _async.put(type, new ArrayList<TwineEvent>());
		_async.get(type).add(callback);
	}
	
	/**
	 * Fires all callbacks (sync and async) for the specified event type
	 * @param type the event type
	 * @return whether the event was marked as cancelled by a callback
	 * @since 1.0-alpha
	 */
	protected static boolean fire(Type type) {
		// Avoid NullPointException
		if(!_events.containsKey(type)) _events.put(type, new ArrayList<TwineEvent>());
		if(!_async.containsKey(type)) _async.put(type, new ArrayList<TwineEvent>());
		
		// Fire async events
		for(TwineEvent evt : _async.get(type)) {
			new Thread() {
				public void run() {
					// Feed callback a dummy Options object
					evt.callback(new TwineEvent.Options());
				}
			}.start();
		}
		
		// Fire blocking events
		TwineEvent.Options options = new TwineEvent.Options();
		for(TwineEvent evt : _events.get(type)) {
			evt.callback(options);
		}
		
		return options.cancelled();
	}
}