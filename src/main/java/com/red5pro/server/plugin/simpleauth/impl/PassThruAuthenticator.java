package com.red5pro.server.plugin.simpleauth.impl;

import org.red5.server.api.IConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.red5pro.server.plugin.simpleauth.interfaces.IAuthenticationValidator;
import com.red5pro.server.plugin.simpleauth.interfaces.ISimpleAuthAuthenticator;

/**
 * This class is a authenticator implementation of
 * <tt>ISimpleAuthAuthenticator</tt>, which is used to allow incoming client
 * connections unconditionally.
 * 
 * @author Rajdeep Rath
 *
 */
public class PassThruAuthenticator implements ISimpleAuthAuthenticator {

	/**
	 * Logger
	 */
	private static Logger logger = LoggerFactory.getLogger(PassThruAuthenticator.class);

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
		return true;
	}

	@Override
	public void setAllowQueryParams(boolean allowQueryParams) {
		// TODO Auto-generated method stub
		this.allowQueryParams = allowQueryParams;
	}

	@Override
	public boolean isAllowQueryParams() {
		// TODO Auto-generated method stub
		return allowQueryParams;
	}

}
