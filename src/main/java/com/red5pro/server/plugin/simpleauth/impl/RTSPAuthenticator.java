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
