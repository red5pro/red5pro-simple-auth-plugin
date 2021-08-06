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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.red5.server.BaseConnection;
import org.red5.server.api.Red5;

import com.red5pro.server.plugin.simpleauth.AuthenticatorType;
import com.red5pro.server.plugin.simpleauth.interfaces.IAuthenticationValidator;
import com.red5pro.server.plugin.simpleauth.interfaces.SimpleAuthAuthenticatorAdapter;

/**
 * This class is a authenticator implementation for SRT and is based on
 * 
 * <pre>
 * RTCAuthAuthenticator
 * </pre>
 * 
 * , which is used to handle authentication for WebRTC clients.
 * 
 * @author Rajdeep Rath
 * @author Paul Gregoire
 *
 */
public class HTTPAuthenticator extends SimpleAuthAuthenticatorAdapter {

	@Override
	public boolean authenticate(AuthenticatorType type, Object connection, Object[] params) {
		Red5.setConnectionLocal(new HTTPWrapperConnection(connection));
		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) params[0];
			String username = (String) map.get(IAuthenticationValidator.USERNAME);
			String password = (String) map.get(IAuthenticationValidator.PASSWORD);
			return source.onConnectAuthenticate(username, password, params);
		} catch (Exception e) {
			logger.error("Error authenticating connection {}", e.getMessage());
		} finally {
			Red5.setConnectionLocal(null);
		}
		return false;
	}
	
	class HTTPWrapperConnection extends BaseConnection {

		HttpSession session;
		
		HTTPWrapperConnection(Object connection) {
			super("TRANSIENT");
			// connection here should be the HttpRequest
			HttpServletRequest httpRequest = (HttpServletRequest) connection;
			session = httpRequest.getSession();
		}

		@Override
		public boolean setAttribute(String name, Object value) {
			session.setAttribute(name, value);
			return true;
		}

		@Override
		public String getProtocol() {
			return "HTTP";
		}

		@Override
		public Encoding getEncoding() {
			return Encoding.RAW;
		}

		@Override
		public void ping() {
		}

		@Override
		public int getLastPingTime() {
			return 0;
		}

		@Override
		public void setBandwidth(int mbits) {
		}

		@Override
		public long getReadBytes() {
			return 0;
		}

		@Override
		public long getWrittenBytes() {
			return 0;
		}
		
	}

}
