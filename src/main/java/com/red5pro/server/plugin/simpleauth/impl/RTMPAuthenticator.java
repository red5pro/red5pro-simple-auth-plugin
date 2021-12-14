//
// Copyright © 2015 Infrared5, Inc. All rights reserved.
//
// The accompanying code comprising examples for use solely in conjunction with Red5 Pro (the "Example Code")
// is  licensed  to  you  by  Infrared5  Inc.  in  consideration  of  your  agreement  to  the  following
// license terms  and  conditions.  Access,  use,  modification,  or  redistribution  of  the  accompanying
// code  constitutes your acceptance of the following license terms and conditions.
//
// Permission is hereby granted, free of charge, to you to use the Example Code and associated documentation
// files (collectively, the "Software") without restriction, including without limitation the rights to use,
// copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the following conditions:
//
// The Software shall be used solely in conjunction with Red5 Pro. Red5 Pro is licensed under a separate end
// user  license  agreement  (the  "EULA"),  which  must  be  executed  with  Infrared5,  Inc.
// An  example  of  the EULA can be found on our website at: https://account.red5pro.com/assets/LICENSE.txt.
//
// The above copyright notice and this license shall be included in all copies or portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,  INCLUDING  BUT
// NOT  LIMITED  TO  THE  WARRANTIES  OF  MERCHANTABILITY, FITNESS  FOR  A  PARTICULAR  PURPOSE  AND
// NONINFRINGEMENT.   IN  NO  EVENT  SHALL INFRARED5, INC. BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
// WHETHER IN  AN  ACTION  OF  CONTRACT,  TORT  OR  OTHERWISE,  ARISING  FROM,  OUT  OF  OR  IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
package com.red5pro.server.plugin.simpleauth.impl;

import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;

import com.red5pro.server.plugin.simpleauth.interfaces.SimpleAuthAuthenticatorAdapter;
import com.red5pro.server.plugin.simpleauth.utils.Utils;

/**
 * This class is a authenticator implementation of
 * 
 * <pre>
 * ISimpleAuthAuthenticator
 * </pre>
 * 
 * , which is used to handle authentication for RTMP clients.
 * 
 * @author Rajdeep Rath
 *
 */
public class RTMPAuthenticator extends SimpleAuthAuthenticatorAdapter {

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

	@Override
	public boolean authenticate(IConnection connection, Object[] params) {
		logger.debug("authenticate:\n{}\nparams: {}", connection, params);
		// set connection local or std rtmp processes wont find the connection where
		// they expect it
		Red5.setConnectionLocal(connection);
		try {
			String username = null;
			String password = null;
			Object[] rest = null;
			String flashVer = null;
			Map<String, Object> connMap = connection.getConnectParams();// .get("flashVer");
			if (connMap.containsKey("flashVer")) {
				flashVer = String.valueOf(connMap.get("flashVer"));
			}
			boolean validAgent = validateAgent(flashVer);
			if (!validAgent) {
				throw new Exception("Invalid / unallowed agent type " + flashVer);
			}
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
						if (!map.containsKey("username") || !map.containsKey("password")) {
							throw new Exception("Missing connection parameter(s)");
						}
						username = URLDecoder.decode(String.valueOf(map.get("username")), "UTF-8");
						password = URLDecoder.decode(String.valueOf(map.get("password")), "UTF-8");
					}
					rest = new Object[1];
					rest[0] = map;
				} catch (Exception e) {
					if (params.length == 0 || params.length < 2) {
						throw new Exception("Missing connection parameter(s)");
					}
					// arguments found
					username = URLDecoder.decode(String.valueOf(params[0]), "UTF-8");
					password = URLDecoder.decode(String.valueOf(params[1]), "UTF-8");
					rest = Arrays.copyOf(params, params.length);
				}
			}
			return source.onConnectAuthenticate(username, password, rest);
		} catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.warn("Error authenticating connection", e);
			} else {
				logger.error("Error authenticating connection {}", e.getMessage());
			}
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
					if (name.indexOf("?") == 0) {
						name = name.replace("?", "");
					}
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
