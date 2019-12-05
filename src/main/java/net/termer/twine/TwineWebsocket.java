package net.termer.twine;

import java.util.ArrayList;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;

/**
 * Utility class to build SockJSHandler objects with Event Bus bridging easier
 * @author termer
 * @since 1.0-alpha
 */
public class TwineWebsocket {
	private Vertx _vertx = null;
	private int _maxBytes = -1;
	private BridgeOptions _bridge = new BridgeOptions();
	private SockJSHandlerOptions _sjsOps = new SockJSHandlerOptions();
	private ArrayList<PermittedOptions> _inbound = new ArrayList<PermittedOptions>();
	private ArrayList<PermittedOptions> _outbound = new ArrayList<PermittedOptions>();
	
	public TwineWebsocket(Vertx vertx, int maxBytesStreaming) {
		_vertx = vertx;
		_maxBytes = maxBytesStreaming;
		
		_sjsOps.setMaxBytesStreaming(_maxBytes);
	}
	
	/**
	 * Returns the BridgeOptions for the websocket handler built by this utility
	 * @return the BridgeOptions
	 * @since 1.0-alpha
	 */
	public BridgeOptions bridgeOptions() {
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
	 * Allows websocket clients to send messages to the specified event bus channel
	 * @param channel The channel to allow
	 * @return this, to be used fluently
	 * @since 1.0-alpha
	 */
	public TwineWebsocket inbound(String channel) {
		_inbound.add(new PermittedOptions().setAddress(channel));
		return this;
	}
	/**
	 * Allows websocket clients to receive messages from the specified event bus channel
	 * @param channel The channel to allow
	 * @return this, to be used fluently
	 * @since 1.0-alpha
	 */
	public TwineWebsocket outbound(String channel) {
		_outbound.add(new PermittedOptions().setAddress(channel));
		return this;
	}
	/**
	 * Allows websocket clients to sesnd messages to all channels that match the specified channel regex
	 * @param channel The channel to allow
	 * @return this, to be used fluently
	 * @since 1.0-alpha
	 */
	public TwineWebsocket inboundRegex(String channelRegex) {
		_inbound.add(new PermittedOptions().setAddressRegex(channelRegex));
		return this;
	}
	/**
	 * Allows websocket clients to receive messages from all channels that match the specified channel regex
	 * @param channel The channel to allow
	 * @return this, to be used fluently
	 * @since 1.0-alpha
	 */
	public TwineWebsocket outboundRegex(String channelRegex) {
		_outbound.add(new PermittedOptions().setAddressRegex(channelRegex));
		return this;
	}
	/**
	 * Clears all allowed inbound event bus channels
	 * @return this, to be used fluently
	 * @since 1.0-alpha
	 */
	public TwineWebsocket clearInbound() {
		_inbound.clear();
		return this;
	}
	/**
	 * Clears all allowed outbound event bus channels
	 * @return this, to be used fluently
	 * @since 1.0-alpha
	 */
	public TwineWebsocket clearOutbound() {
		_outbound.clear();
		return this;
	}
	
	/**
	 * Creates a new SockJSHandler from the applied settings
	 * @return a new Router for handling SockJS connections
	 * @since 1.0-alpha
	 */
	protected Router build() {
		_bridge.setInboundPermitted(_inbound);
		_bridge.setOutboundPermitted(_outbound);
		return SockJSHandler
					.create(_vertx, _sjsOps)
					.bridge(_bridge);
	}
}
