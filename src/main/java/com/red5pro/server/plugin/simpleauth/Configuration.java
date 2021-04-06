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
package com.red5pro.server.plugin.simpleauth;

import com.red5pro.server.plugin.simpleauth.interfaces.IAuthenticationValidator;

/**
 * This class provides the base configuration for the authentication
 * logic.Different aspects of the authentication mechanism can be configured
 * using the different properties.
 * 
 * You can create your own custom validator and pass it to the plugin via the
 * <tt>Configuration</tt>. To know more about creating a custom validator,
 * extending the plugin, configurations and more see <a href=
 * "https://www.red5pro.com/docs/server/authplugin.html#plugin-configuration">Documentation</a>.
 * 
 * @author Rajdeep Rath
 *
 */
public class Configuration {

	/**
	 * Property to enable or disable authentication
	 */
	private boolean active;

	/**
	 * Property to check if the <code>active</code> property was changed beyond its
	 * default value
	 */
	private boolean activeUpdated;

	/**
	 * Property to enable or disable RTMP authentication
	 */
	private boolean rtmp;

	/**
	 * Property to check if the <code>rtmp</code> property was changed beyond its
	 * default value
	 */
	private boolean rtmpUpdated;

	/**
	 * Property to enable or disable RTSP authentication
	 */
	private boolean rtsp;

	/**
	 * Property to check if the <code>rtsp</code> property was changed beyond its
	 * default value
	 */
	private boolean rtspUpdated;

	/**
	 * Property to enable or disable WebRTC authentication
	 */
	private boolean rtc;

	/**
	 * Property to check if the <code>rtc</code> property was changed beyond its
	 * default value
	 */
	private boolean rtcUpdated;

	/**
	 * Property to enable or disable params via query string for RTMP clients
	 */
	private boolean rtmpAllowQueryParamsEnabled;

	/**
	 * Property to check if the <code>rtmpAllowQueryParamsEnabled</code> property
	 * was changed beyond its default value
	 */
	private boolean rtmpAllowQueryParamsUpdated;

	/**
	 * Comma separated list of allowed RTMP agents. An agent is identified by the
	 * <code>flashVer</code> string
	 */
	private String allowedRtmpAgents;

	/**
	 * Property to check if the <code>allowedRtmpAgents</code> property was changed
	 * beyond its default value
	 */
	private boolean allowedRtmpAgentsUpdated;

	/**
	 * Property to reference the authentication validator.
	 */
	private IAuthenticationValidator validator;

	/**
	 * Constructor for Configuration class
	 */
	public Configuration() {

	}

	/**
	 * Constructor for Configuration class
	 * 
	 * @param active
	 *            Enables or disables the configuration
	 */
	public Configuration(boolean active) {
		this.active = active;
	}

	/**
	 * Returns the the value of <code>active</code>
	 * 
	 * @return the value of <code>active</code>
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * Sets the the value of <code>active</code>
	 * 
	 * @param active
	 *            The boolean value to set
	 */
	public void setActive(boolean active) {
		this.active = active;
		this.activeUpdated = true;
	}

	/**
	 * Returns the IAuthenticationValidator reference
	 * 
	 * @return The IAuthenticationValidator object
	 */
	public IAuthenticationValidator getValidator() {
		return validator;
	}

	/**
	 * Sets the IAuthenticationValidator for thsi configuration
	 * 
	 * @param validator
	 *            A IAuthenticationValidator object
	 */
	public void setValidator(IAuthenticationValidator validator) {
		this.validator = validator;
	}

	/**
	 * Returns the value of <code>rtmp</code>
	 * 
	 * @return true if rtmp authentication is enabled otherwise false
	 */
	public boolean isRtmp() {
		return rtmp;
	}

	/**
	 * Sets the the value of <code>rtmp</code>
	 * 
	 * @param rtmp
	 *            The boolean value to set to enable or disable rtmp authentication
	 */
	public void setRtmp(boolean rtmp) {
		this.rtmp = rtmp;
		this.rtmpUpdated = true;
	}

	/**
	 * Returns the value of <code>rtsp</code>
	 * 
	 * @return true if rtsp authentication is enabled otherwise false
	 */
	public boolean isRtsp() {
		return rtsp;
	}

	/**
	 * Sets the the value of <code>rtsp</code>
	 * 
	 * @param rtsp
	 *            The boolean value to set to enable or disable rtsp authentication
	 */
	public void setRtsp(boolean rtsp) {
		this.rtsp = rtsp;
		this.rtspUpdated = true;
	}

	/**
	 * Returns the value of <code>rtc</code>
	 * 
	 * @return true if WebRTC authentication is enabled otherwise false
	 */
	public boolean isRtc() {
		return rtc;
	}

	/**
	 * Sets the the value of <code>rtc</code>
	 * 
	 * @param rtc
	 *            The boolean value to set to enable or disable rtc authentication
	 */
	public void setRtc(boolean rtc) {
		this.rtc = rtc;
		this.rtcUpdated = true;
	}

	/**
	 * Returns the value of <code>rtmpAllowQueryParamsEnabled</code>
	 * 
	 * @return true if query params checking is allowed for rtmp clients otherwise
	 *         false
	 */
	public boolean isRtmpAllowQueryParamsEnabled() {
		return rtmpAllowQueryParamsEnabled;
	}

	/**
	 * Sets the the value of <code>rtmpAllowQueryParams</code>
	 * 
	 * @param rtmpAllowQueryParams
	 *            The boolean value to set to allow or disallow checking query
	 *            params for rtmp clients
	 */
	public void setRtmpAllowQueryParamsEnabled(boolean rtmpAllowQueryParams) {
		this.rtmpAllowQueryParamsEnabled = rtmpAllowQueryParams;
		this.rtmpAllowQueryParamsUpdated = true;
	}

	/**
	 * Returns the value of <code>allowedRtmpAgents</code>
	 * 
	 * @return comma separated list of allowed rtmp agent strings
	 */
	public String getAllowedRtmpAgents() {
		return allowedRtmpAgents;
	}

	/**
	 * Sets the the value of <code>allowedRtmpAgents</code>
	 * 
	 * @param allowedRtmpAgents
	 *            The comma separated list of rtmp agents
	 */
	public void setAllowedRtmpAgents(String allowedRtmpAgents) {
		this.allowedRtmpAgents = allowedRtmpAgents;
		this.allowedRtmpAgentsUpdated = true;
	}

	/**
	 * Returns the value of <code>rtmpUpdated</code>
	 * 
	 * @return true if rtmp authentication state was changed beyond default
	 *         otherwise false
	 */
	protected boolean isRtmpUpdated() {
		return rtmpUpdated;
	}

	/**
	 * Returns the value of <code>rtspUpdated</code>
	 * 
	 * @return true if rtsp authentication state was changed beyond default
	 *         otherwise false
	 */
	protected boolean isRtspUpdated() {
		return rtspUpdated;
	}

	/**
	 * Returns the value of <code>rtcUpdated</code>
	 * 
	 * @return true if rtc authentication state was changed beyond default otherwise
	 *         false
	 */
	protected boolean isRtcUpdated() {
		return rtcUpdated;
	}

	/**
	 * Returns the value of <code>rtmpAllowQueryParamsUpdated</code>
	 * 
	 * @return true if allowed rtmp agents list was changed beyond default otherwise
	 *         false
	 */
	protected boolean isRtmpAllowQueryParamsUpdated() {
		return rtmpAllowQueryParamsUpdated;
	}

	/**
	 * Returns the value of <code>allowedRtmpAgentsUpdated</code>
	 * 
	 * @return true if permissions for allowing query params for rtmp clients
	 *         changed beyond default otherwise false
	 */
	protected boolean isAllowedRtmpAgentsUpdated() {
		return allowedRtmpAgentsUpdated;
	}

	/**
	 * Returns the value of <code>activeUpdated</code>
	 * 
	 * @return true if configuration's active state was changed beyond default
	 *         otherwise false
	 */
	protected boolean isActiveUpdated() {
		return activeUpdated;
	}

}