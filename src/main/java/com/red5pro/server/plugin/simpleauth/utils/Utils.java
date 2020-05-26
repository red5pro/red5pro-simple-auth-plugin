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
package com.red5pro.server.plugin.simpleauth.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.red5.server.api.IConnection;

/**
 * Utility class
 * 
 * @author Rajdeep Rath
 *
 */
public class Utils {

	/**
	 * Check for Restreamer RTMP connection on origin
	 * 
	 * @param connection
	 *            The connection object to check
	 * @return true if connection is a restreamer otherwise false
	 */
	public static boolean isRestreamer(IConnection connection) {
		Map<String, Object> map = getParametersMap(connection.getConnectParams());

		String objectEncoding = String.valueOf(map.get("objectEncoding"));
		String capabilities = String.valueOf(map.get("capabilities"));
		String flashVer = String.valueOf(map.get("flashVer"));

		if (map.containsKey("cluster-restreamer-name")) {
			return true;
		} else if (capabilities.equals("15.0") && objectEncoding.equals("0.0") && flashVer.equals("WIN 11,2,202,235")) {
			return true;
		}

		return false;
	}

	/**
	 * Prepares a cleaned up java HashMap from a provided raw HashMap
	 * 
	 * @param content
	 *            The raw HashMap containing connection parameters
	 * @return processed HashMap of connection parameters
	 */
	private static Map<String, Object> getParametersMap(Map<String, Object> content) {
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
}
