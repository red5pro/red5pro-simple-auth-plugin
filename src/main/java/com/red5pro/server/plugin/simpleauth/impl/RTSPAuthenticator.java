package com.red5pro.server.plugin.simpleauth.impl;

import java.util.HashMap;
import java.util.Map;

import org.red5.server.api.IConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.red5pro.server.plugin.simpleauth.interfaces.IAuthenticationValidator;
import com.red5pro.server.plugin.simpleauth.interfaces.ISimpleAuthAuthenticator;

/**
 * This class is a authenticator implementation of
 * <tt>ISimpleAuthAuthenticator</tt>, which is used to handle authentication for
 * RTSP clients.
 * 
 * @author Rajdeep Rath
 *
 */
public class RTSPAuthenticator implements ISimpleAuthAuthenticator {

	/**
	 * Logger
	 */
	private static Logger logger = LoggerFactory.getLogger(RTSPAuthenticator.class);

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
		logger.debug("RTSPAuthenticator initialized");
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
			if (params.length == 0 || params[0] == null)
				throw new Exception("Missing connection parameter(s)");

			Map<String, Object> map = new HashMap<String, Object>();
			for (int i = 0; i < params.length; i++) {
				String[] param = String.valueOf(params[i]).split("=");
				map.put(param[0], param[1]);
			}

			if (!map.containsKey("username") || !map.containsKey("password"))
				throw new Exception("Missing authentication parameter(s)");

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

	@Override
	public boolean isAllowQueryParams() {
		return allowQueryParams;
	}

	@Override
	public void setAllowQueryParams(boolean allowQueryParams) {
		this.allowQueryParams = allowQueryParams;
	}

}
