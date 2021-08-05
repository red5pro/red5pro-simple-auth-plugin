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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.red5.server.api.IConnection;

import com.red5pro.server.plugin.simpleauth.interfaces.SimpleAuthAuthenticatorAdapter;

/**
 * This class is a authenticator implementation for SRT and is based on 
 * <pre>RTCAuthAuthenticator</pre>, which is used to handle authentication for
 * WebRTC clients.
 * 
 * @author Rajdeep Rath
 * @author Paul Gregoire
 *
 */
public class SRTAuthenticator extends SimpleAuthAuthenticatorAdapter {

	@Override
	public boolean authenticate(IConnection connection, Object[] params) {
		try {
			Map<String, Object> map = getParametersMap(connection.getConnectParams());
			if (!map.containsKey("username") || !map.containsKey("password")) {
				throw new Exception("Missing connection parameter(s)");
			}
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
			if (key.indexOf("?") == 0) {
				key = key.replace("?", "");
			}
			map.put(key, value);
		}
		return map;
	}

}
