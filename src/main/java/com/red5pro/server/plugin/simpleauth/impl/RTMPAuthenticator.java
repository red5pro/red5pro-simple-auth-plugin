package com.red5pro.server.plugin.simpleauth.impl;

import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.red5.server.api.IConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.red5pro.server.plugin.simpleauth.interfaces.IAuthenticationValidator;
import com.red5pro.server.plugin.simpleauth.interfaces.ISimpleAuthAuthenticator;
import com.red5pro.server.plugin.simpleauth.utils.Utils;

/**
 * This class is a authenticator implementation of
 * <tt>ISimpleAuthAuthenticator</tt>, which is used to handle authentication for
 * RTMP clients.
 * 
 * @author Rajdeep Rath
 *
 */
public class RTMPAuthenticator implements ISimpleAuthAuthenticator {

	/**
	 * Logger
	 */
	private static Logger logger = LoggerFactory.getLogger(RTMPAuthenticator.class);

	/**
	 * The IAuthenticationValidator object for this authenticator
	 */
	private IAuthenticationValidator source;

	/**
	 * Flag to enable/disable connection params check on query params
	 */
	private boolean allowQueryParams;

	/**
	 * List of rtmp agents. Default must be *
	 */
	private String allowedRtmpAgents = "*";

	/**
	 * Constructor for RTMPAuthenticator
	 */
	public RTMPAuthenticator() {

	}

	/**
	 * Constructor for RTMPAuthenticator
	 * 
	 * @param allowQueryParams
	 *            Boolean value to set for <code>allowQueryParams</code>
	 */
	public RTMPAuthenticator(boolean allowQueryParams) {
		this.allowQueryParams = allowQueryParams;
	}

	/**
	 * Constructor for RTMPAuthenticator
	 * 
	 * @param allowQueryParams
	 *            Boolean value to set for <code>allowQueryParams</code>
	 * @param allowedRtmpAgents
	 *            Comma separated string of rtmp agents to allow
	 */
	public RTMPAuthenticator(boolean allowQueryParams, String allowedRtmpAgents) {
		this.allowQueryParams = allowQueryParams;
		this.allowedRtmpAgents = allowedRtmpAgents;
	}

	/**
	 * Authenticator entry point
	 */
	public void initialize() {
		// initialization tasks
		logger.debug("RTMPAuthenticator initialized");
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
			String username = null;
			String password = null;
			Object[] rest = null;
			String flashVer = null;

			Map<String, Object> connMap = connection.getConnectParams();// .get("flashVer");

			if (connMap.containsKey("flashVer"))
				flashVer = String.valueOf(connMap.get("flashVer"));

			boolean validAgent = validateAgent(flashVer);
			if (!validAgent)
				throw new Exception("Invalid / unallowed agent type " + flashVer);

			if (!allowQueryParams) {
				if (params.length == 0 || params.length < 2) {
					// no arguments passed
					throw new Exception("Missing connection parameter(s)");
				} else {
					// arguments passed
					username = URLDecoder.decode(String.valueOf(params[0]), "UTF-8");
					password = URLDecoder.decode(String.valueOf(params[1]), "UTF-8");

					rest = Arrays.copyOf(params, params.length);
				}
			} else {
				try {
					// we wont look at arguments at all if {allowQueryParams} is enabled
					Map<String, Object> map = getParametersMap(connection.getConnectParams());

					// Handling a cluster-restreamer connection
					if (map.containsKey("cluster-restreamer-context") && map.containsKey("cluster-restreamer-name")) {
						if (map.containsKey("restreamer")) {
							username = "cluster-restreamer";
							password = String.valueOf(map.get("restreamer"));
						} else {
							// hard check
							if (Utils.isRestreamer(connection)) {
								return true;
							} else {
								throw new Exception("Invalid cluster restreamer parameter(s)!");
							}
						}
					} else {
						if (!map.containsKey("username") || !map.containsKey("password"))
							throw new Exception("Missing connection parameter(s)");

						username = URLDecoder.decode(String.valueOf(map.get("username")), "UTF-8");
						password = URLDecoder.decode(String.valueOf(map.get("password")), "UTF-8");
					}

					rest = new Object[1];
					rest[0] = map;
				} catch (Exception e) {
					if (params.length == 0 || params.length < 2)
						throw new Exception("Missing connection parameter(s)");

					// arguments found
					username = URLDecoder.decode(String.valueOf(params[0]), "UTF-8");
					password = URLDecoder.decode(String.valueOf(params[1]), "UTF-8");

					rest = Arrays.copyOf(params, params.length);
				}
			}

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
			Object value = pair.getValue();

			if (key.equals("queryString")) {
				Map<String, Object> qmap = new HashMap<String, Object>();
				String[] parameters = String.valueOf(value).split("&");
				for (int i = 0; i < parameters.length; i++) {
					String[] param = String.valueOf(parameters[i]).split("=");
					String name = param[0];
					if (name.indexOf("?") == 0)
						name = name.replace("?", "");
					String val = param[1];
					qmap.put(name, val);
				}

				map.putAll(qmap);
			}

			map.put(key, value);
		}

		return map;

	}

	/**
	 * Checks and validates rtmp agent string.
	 * 
	 * @param agentString
	 *            The rtmp agent string to check
	 * @return true if agent is allowed, otherwise false
	 * @throws Exception
	 */
	private boolean validateAgent(String agentString) throws Exception {
		if (allowedRtmpAgents.equals("*")) {
			return true;
		} else {
			String[] candidates = allowedRtmpAgents.split(";");
			for (int i = 0; i < candidates.length; i++) {
				String candidate = candidates[i].trim().toUpperCase();
				if (agentString.toUpperCase().contains(candidate)) {
					return true;
				}
			}
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

	/**
	 * Returns list of allowed rtmp agents
	 * 
	 * @return comma separated list of agent strings to allow
	 */
	public String getAllowedRtmpAgents() {
		return allowedRtmpAgents;
	}

	/**
	 * Sets the list of allowed rtmp agents
	 * 
	 * @param allowedRtmpAgents
	 *            The comma separated list of agent strings to allow
	 */
	public void setAllowedRtmpAgents(String allowedRtmpAgents) {
		this.allowedRtmpAgents = allowedRtmpAgents;
	}

}
