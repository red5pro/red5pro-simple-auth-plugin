package com.red5pro.server.plugin.simpleauth.interfaces;

/**
 * The interface to implement your own custom validator for the authentication
 * mechanism.
 * 
 * @author Rajdeep Rath
 *
 */
public interface IAuthenticationValidator {

	/**
	 * This method is triggered when a new instance is created via bean declaration
	 * in the <tt>red5-web.xml</tt> file of the web application.
	 */
	void initialize();

	/**
	 * This method is called when a client attempts to connect to the application.
	 * The core of the authentication plugin automatically extracts and passes
	 * necessary client parameters to this method.
	 * 
	 * @param username
	 *            The username param passed by the client
	 * @param password
	 *            The password param passed by the client
	 * @param rest
	 *            An object of arrays representing rest of the params.For
	 *            <code>rtsp</code>, <code>rtc</code> and <code>rtmp</code> clients
	 *            (that pass params via query string),the parameter map is contained
	 *            in the first object of the array.ie <code>rest[0]</code>
	 * 
	 *            <p>
	 *            You can also access the IConnection object in the attempt directly
	 *            using <code>Red5.getConnectionLocal()</code>
	 *            </p>
	 * 
	 * @return true to accept the connection or false to reject it.
	 */
	boolean onConnectAuthenticate(String username, String password, Object[] rest);
}
