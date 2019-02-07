package com.red5pro.server.plugin.simpleauth.impl;

import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.red5.server.api.IConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.red5pro.server.plugin.simpleauth.interfaces.IAuthenticationValidator;
import com.red5pro.server.plugin.simpleauth.interfaces.ISimpleAuthAuthenticator;

/**
 * This class is a authenticator implementation of
 * <tt>ISimpleAuthAuthenticator</tt>, which is used to handle authentication for
 * WebRTC clients.
 * 
 * @author Rajdeep Rath
 *
 */
public class RTCAuthenticator implements ISimpleAuthAuthenticator {

	/**
	 * Logger
	 */
	private static Logger logger = LoggerFactory.getLogger(RTCAuthenticator.class);

	/**
	 * The IAuthenticationValidator object for this authenticator
	 */
	private IAuthenticationValidator source;

	/**
	 * Flag to enable/disable connection params check on query params
	 */
	private boolean allowQueryParams;

	/**
	 * Authenticator entry point
	 */
	public void initialize() {
		// initialization tasks
		logger.debug("RTCAuthenticator initialized");
	}

	@Override
	public void setDataSource(IAuthenticationValidator source) {
		this.source = source;
	}

	@Override
	public IAuthenticationValidator getDataSource() {
		return source;
	}

	@Override
	public boolean authenticate(IConnection connection, Object[] params) {

		try {
			Map<String, Object> map = getParametersMap(connection.getConnectParams());

			if (!map.containsKey("username") || !map.containsKey("password"))
				throw new Exception("Missing connection parameter(s)");

			String username = String.valueOf(map.get("username"));
			String password = String.valueOf(map.get("password"));
			Object[] rest = new Object[1];
			rest[0] = map;

			return source.onConnectAuthenticate(username, password, rest);

		} catch (Exception e) {
			logger.error("Error authenticating connection " + e.getMessage());
		}

		return false;
	}

	/**
	 * Extracts connections params from query string for clients and returns as a
	 * cleaned up Map
	 * 
	 * @param content
	 *            The raw query params map
	 * @return processed parameters map
	 */
	private Map<String, Object> getParametersMap(Map<String, Object> content) {
		Map<String, Object> map = new HashMap<String, Object>();

		Iterator<Entry<String, Object>> it = content.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Object> pair = it.next();
			String key = pair.getKey();
			String value;
			String charset = StandardCharsets.UTF_8.name();
			try {
				value = URLDecoder.decode(String.valueOf(pair.getValue()), charset);
			} catch (Exception e) {
				value = String.valueOf(pair.getValue());
			}

			if (key.indexOf("?") == 0)
				key = key.replace("?", "");
			map.put(key, value);
		}

		return map;

	}

	@Override
	public boolean isAllowQueryParams() {
		return allowQueryParams;
	}

	@Override
	public void setAllowQueryParams(boolean allowQueryParams) {
		this.allowQueryParams = allowQueryParams;
	}

}
