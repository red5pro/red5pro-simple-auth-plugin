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

		if (capabilities.equals("15.0") && objectEncoding.equals("0.0") && flashVer.equals("WIN 11,2,202,235")) {
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
