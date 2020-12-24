package net.termer.twine;

import java.util.ArrayList;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;

/**
 * Utility class to build SockJSHandler objects with Event Bus bridging easier
 * @author termer
 * @since 1.0-alpha
 */
public class TwineWebSocket {
	private Vertx _vertx = null;
	private final SockJSBridgeOptions _bridge = new SockJSBridgeOptions();
	private final SockJSHandlerOptions _sjsOps = new SockJSHandlerOptions();
	private final ArrayList<PermittedOptions> _inbound = new ArrayList<>();
	private final ArrayList<PermittedOptions> _outbound = new ArrayList<>();
	
	public TwineWebSocket(Vertx vertx, int maxBytesStreaming) {
		_vertx = vertx;

		_sjsOps.setMaxBytesStreaming(maxBytesStreaming);
	}
	
	/**
	 * Returns the BridgeOptions for the WebSocket handler built by this utility
	 * @return the BridgeOptions
	 * @since 1.0-alpha
	 */
	public SockJSBridgeOptions bridgeOptions() {
		return _bridge;
	}
	/**
	 * Returns the options for the handler built by this utility
	 * @return the SockJSHandlerOptions for this utility
	 * @since 1.0-alpha
	 */
	public SockJSHandlerOptions handlerOptions() {
		return _sjsOps;
	}
	
	/**
	 * Allows WebSocket clients to send messages to the specified event bus channel
	 * @param channel The channel to allow
	 * @return this, to be used fluently
	 * @since 1.0-alpha
	 */
	public TwineWebSocket inbound(String channel) {
		_inbound.add(new PermittedOptions().setAddress(channel));
		return this;
	}
	/**
	 * Allows WebSocket clients to receive messages from the specified event bus channel
	 * @param channel The channel to allow
	 * @return this, to be used fluently
	 * @since 1.0-alpha
	 */
	public TwineWebSocket outbound(String channel) {
		_outbound.add(new PermittedOptions().setAddress(channel));
		return this;
	}
	/**
	 * Allows WebSocket clients to sesnd messages to all channels that match the specified channel regex
	 * @param channelRegex The channel to allow
	 * @return this, to be used fluently
	 * @since 1.0-alpha
	 */
	public TwineWebSocket inboundRegex(String channelRegex) {
		_inbound.add(new PermittedOptions().setAddressRegex(channelRegex));
		return this;
	}
	/**
	 * Allows WebSocket clients to receive messages from all channels that match the specified channel regex
	 * @param channelRegex The channel to allow
	 * @return this, to be used fluently
	 * @since 1.0-alpha
	 */
	public TwineWebSocket outboundRegex(String channelRegex) {
		_outbound.add(new PermittedOptions().setAddressRegex(channelRegex));
		return this;
	}
	/**
	 * Clears all allowed inbound event bus channels
	 * @return this, to be used fluently
	 * @since 1.0-alpha
	 */
	public TwineWebSocket clearInbound() {
		_inbound.clear();
		return this;
	}
	/**
	 * Clears all allowed outbound event bus channels
	 * @return this, to be used fluently
	 * @since 1.0-alpha
	 */
	public TwineWebSocket clearOutbound() {
		_outbound.clear();
		return this;
	}
	
	/**
	 * Creates a new SockJSHandler from the applied settings
	 * @return a new Router for handling SockJS connections
	 * @since 1.0-alpha
	 */
	protected Router build() {
		_bridge.setInboundPermitteds(_inbound);
		_bridge.setOutboundPermitteds(_outbound);

		return SockJSHandler
					.create(_vertx, _sjsOps)
					.bridge(_bridge);
	}
}
