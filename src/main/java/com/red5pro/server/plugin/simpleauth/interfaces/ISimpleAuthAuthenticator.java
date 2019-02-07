package com.red5pro.server.plugin.simpleauth.interfaces;

import org.red5.server.api.IConnection;

/**
 * This interface is used to implement a authenticator. An authenticator is the
 * core implementation which knows the logic for authenticating a client,
 * including what type of client it is and how to extract parameters from it. It
 * is recommended that you implement a separate authenticator for each protocol
 * type that your client may use to connect to the application.
 * 
 * @author Rajdeep Rath
 *
 */
public interface ISimpleAuthAuthenticator {

	/**
	 * Sets the IAuthenticationValidator for the implementing authenticator
	 * 
	 * @param source
	 *            The IAuthenticationValidator to set
	 */
	public void setDataSource(IAuthenticationValidator source);

	/**
	 * Returns the IAuthenticationValidator for implementing authenticator
	 * 
	 * @return The IAuthenticationValidator
	 */
	public IAuthenticationValidator getDataSource();

	/**
	 * This method is called when a client tries to connect to your application.
	 * 
	 * @param connection
	 *            The IConnection object representing the client
	 * @param params
	 *            The connection parameters provided by the client. For rtsp, rtc
	 *            and rtmp clients (which pass params via query params) the
	 *            parameters are available as a java Map in the first element of the
	 *            array.
	 * @return true if connection should be allowed, otherwise false
	 */
	public boolean authenticate(IConnection connection, Object[] params);

	/**
	 * Sets whether the authenticator allows connection parameters via query params
	 * or not. This is valid for rtmp clients or only
	 * 
	 * @param allowQueryParams
	 *            boolean value to set
	 */
	void setAllowQueryParams(boolean allowQueryParams);

	/**
	 * Returns whether the authenticator allows connection parameters via query
	 * params.
	 * 
	 * @return true if query params are allowed for rtmp authentication, otherwise
	 *         false
	 */
	boolean isAllowQueryParams();
}
