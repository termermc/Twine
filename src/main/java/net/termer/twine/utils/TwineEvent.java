package net.termer.twine.utils;

/**
 * Interface for Twine-specific events
 * @author termer
 * @since 1.0-alpha
 */
public interface TwineEvent {
	void callback(Options options);
	
	/**
	 * Utility class to manipulate events.
	 * Event options have no effect on asynchronous callbacks.
	 * @author termer
	 * @since 1.0-alpha
	 */
	class Options {
		private boolean _cancelled = false;
		
		/**
		 * Cancels this event
		 * @since 1.0-alpha
		 */
		public void cancel() {
			_cancelled = true;
		}
		/**
		 * Sets whether the event should be cancelled
		 * @param cancel whether to cancel event
		 * @since 1.0-alpha
		 */
		public void setCancelled(boolean cancel) {
			_cancelled = cancel;
		}
		/**
		 * Whether this event has been cancelled
		 * @return whether event is cancelled
		 * @since 1.0-alpha
		 */
		public boolean cancelled() {
			return _cancelled;
		}
	}
}
