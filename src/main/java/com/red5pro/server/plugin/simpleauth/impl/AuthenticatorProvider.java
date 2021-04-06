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

import org.red5.server.api.IConnection;
import org.red5.server.net.rtmp.RTMPMinaConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.red5pro.server.plugin.simpleauth.Configuration;
import com.red5pro.server.plugin.simpleauth.interfaces.IAuthenticationValidator;
import com.red5pro.server.plugin.simpleauth.interfaces.ISimpleAuthAuthenticator;

import org.red5.server.net.rtmpt.RTMPTConnection;
import com.red5pro.server.stream.rtsp.IRTSPConnection;
import com.red5pro.server.stream.restreamer.IConnectorShell;
import com.red5pro.server.stream.webrtc.IWebRTCConnection;

/**
 * This class is the <tt>ISimpleAuthAuthenticator</tt> provider for the
 * authentication mechanism. It selects and executes the correct
 * <tt>ISimpleAuthAuthenticator</tt> implementation for a incoming IConnection
 * object based on connection type and configuration settings.
 * 
 * @author Rajdeep Rath
 *
 */
public class AuthenticatorProvider {

	private static Logger logger = LoggerFactory.getLogger(AuthenticatorProvider.class);

	/**
	 * The IAuthenticationValidator object to pass on to authenticator
	 * implementations
	 */
	IAuthenticationValidator validator;

	/**
	 * Global settings for enabling or disabling authentication
	 */
	boolean enabled;

	/**
	 * Authenticator implementation reference for rtmp
	 */
	ISimpleAuthAuthenticator rtmpAuthenticator;

	/**
	 * Authenticator implementation reference for rtsp
	 */
	ISimpleAuthAuthenticator rtspAuthenticator;

	/**
	 * Authenticator implementation reference for webrtc
	 */
	ISimpleAuthAuthenticator webRtcAuthenticator;

	/**
	 * Authenticator implementation reference for unconditional reject
	 */
	ISimpleAuthAuthenticator grumpyAuthenticator;

	/**
	 * Authenticator implementation reference for unconditional allow
	 */
	ISimpleAuthAuthenticator happyAuthenticator;

	/**
	 * Global settings for enabling or disabling rtmp authentication
	 */
	private boolean secureRTMP = true;

	/**
	 * Global settings for enabling or disabling rtsp authentication
	 */
	private boolean secureRTSP = true;

	/**
	 * Global settings for enabling or disabling rtc authentication
	 */
	private boolean secureRTC = true;

	/**
	 * Global settings for allowing or disallowing query params checking rtmp
	 * clients
	 */
	private boolean rtmpAcceptsQueryParamsEnabled;

	/**
	 * Global settings for allowed rtmp agent string
	 */
	private String allowedRtmpAgents;

	/**
	 * Constructor for AuthenticatorProvider
	 */
	public AuthenticatorProvider() {

	}

	/**
	 * Constructor for AuthenticatorProvider
	 * 
	 * @param config
	 *            The Configuration object
	 */
	public AuthenticatorProvider(Configuration config) {
		this.setEnabled(config.isActive());
		this.setValidator(config.getValidator());
		this.setSecureRTMP(config.isRtmp());
		this.setSecureRTSP(config.isRtsp());
		this.setSecureRTC(config.isRtc());
		this.setRtmpAcceptsQueryParamsEnabled(config.isRtmpAllowQueryParamsEnabled());
		this.setAllowedRtmpAgents(config.getAllowedRtmpAgents());
	}

	/**
	 * Entry point
	 */
	public void initialize() {

		rtmpAuthenticator = new RTMPAuthenticator(rtmpAcceptsQueryParamsEnabled, allowedRtmpAgents);
		rtmpAuthenticator.setDataSource(validator);

		rtspAuthenticator = new RTSPAuthenticator();
		rtspAuthenticator.setDataSource(validator);

		webRtcAuthenticator = new RTCAuthenticator();
		webRtcAuthenticator.setDataSource(validator);

		grumpyAuthenticator = new BlockerAuthenticator();
		grumpyAuthenticator.setDataSource(validator);

		happyAuthenticator = new PassThruAuthenticator();
		happyAuthenticator.setDataSource(validator);
	}

	/**
	 * Constructor for AuthenticatorProvider
	 * 
	 * @param validator
	 *            An IAuthenticationValidator object
	 */
	public AuthenticatorProvider(IAuthenticationValidator validator) {
		this.validator = validator;
	}

	/**
	 * Returns appropriate ISimpleAuthAuthenticator implementation based on
	 * IConnection type
	 * 
	 * @param connection
	 *            The client IConnection object to check
	 * @return A ISimpleAuthAuthenticator which will be used to authenticate the
	 *         connection
	 */
	public ISimpleAuthAuthenticator getAuthenticator(IConnection connection) {
		if (!enabled) {
			return happyAuthenticator;
		} else {
			if (connection instanceof RTMPMinaConnection || connection instanceof RTMPTConnection) {
				if (secureRTMP) {
					return rtmpAuthenticator;
				} else {
					return happyAuthenticator;
				}
			} else if (connection instanceof IRTSPConnection) {
				if (secureRTSP) {
					return rtspAuthenticator;
				} else {
					return happyAuthenticator;
				}
			} else if (connection instanceof IWebRTCConnection) {
				if (secureRTC) {
					return webRtcAuthenticator;
				} else {
					return happyAuthenticator;
				}
			} else if (connection instanceof IConnectorShell) {
				return happyAuthenticator;
			} else {
				// unknown protocol
				logger.error("Unknown connection type " + connection.getClass().getCanonicalName());
				return grumpyAuthenticator;
			}
		}
	}

	/**
	 * Returns the value of validator
	 * 
	 * @return An IAuthenticationValidator object
	 */
	public IAuthenticationValidator getValidator() {
		return validator;
	}

	/**
	 * Sets the value of validator
	 * 
	 * @param validator
	 *            The IAuthenticationValidator to set
	 */
	public void setValidator(IAuthenticationValidator validator) {
		this.validator = validator;
	}

	/**
	 * Returns the global default state for rtmp authentication
	 * 
	 * @return true if rtmp authentication is enabled, otherwise false
	 */
	public boolean isSecureRTMP() {
		return secureRTMP;
	}

	/**
	 * Sets the global default state for rtmp authentication
	 * 
	 * @param secureRTMP
	 *            Boolean value to set
	 */
	public void setSecureRTMP(boolean secureRTMP) {
		this.secureRTMP = secureRTMP;
	}

	/**
	 * Returns the global default state for rtsp authentication
	 * 
	 * @return true if rtsp authentication is enabled, otherwise false
	 */
	public boolean isSecureRTSP() {
		return secureRTSP;
	}

	/**
	 * Sets the global default state for rtsp authentication
	 * 
	 * @param secureRTSP
	 *            Boolean value to set
	 */
	public void setSecureRTSP(boolean secureRTSP) {
		this.secureRTSP = secureRTSP;
	}

	/**
	 * Returns the global default state for rtc authentication
	 * 
	 * @return true if rtc authentication is enabled, otherwise false
	 */
	public boolean isSecureRTC() {
		return secureRTC;
	}

	/**
	 * Sets the global default state for rtsp authentication
	 * 
	 * @param secureRTC
	 *            Boolean value to rtc
	 */
	public void setSecureRTC(boolean secureRTC) {
		this.secureRTC = secureRTC;
	}

	/**
	 * Returns the enabled state of the AuthenticationProvider
	 * 
	 * @return true if the component is enabled, otherwise false
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Sets the enabled state of the AuthenticationProvider
	 * 
	 * @param enabled
	 *            The Boolean value to set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Returns the global state of rtmp query string check in authentication
	 * 
	 * @return true if check is enabled, otherwise false
	 */
	public boolean isRtmpAcceptsQueryParamsEnabled() {
		return rtmpAcceptsQueryParamsEnabled;
	}

	/**
	 * Sets the global state of rtmp query string check in authentication
	 * 
	 * @param rtmpAcceptsQueryParams
	 *            The Boolean value to set
	 */
	public void setRtmpAcceptsQueryParamsEnabled(boolean rtmpAcceptsQueryParams) {
		this.rtmpAcceptsQueryParamsEnabled = rtmpAcceptsQueryParams;
	}

	/**
	 * Returns the global value for allowed rtmp agents
	 * 
	 * @return comma separated string of rtmp agents or <tt>*</tt> for all
	 */
	public String getAllowedRtmpAgents() {
		return allowedRtmpAgents;
	}

	/**
	 * Sets the global value for allowed rtmp agents
	 * 
	 * @param allowedRtmpAgents
	 *            A comma separated string of rtmp agents or <tt>*</tt> for all
	 */
	public void setAllowedRtmpAgents(String allowedRtmpAgents) {
		this.allowedRtmpAgents = allowedRtmpAgents;
	}
}
