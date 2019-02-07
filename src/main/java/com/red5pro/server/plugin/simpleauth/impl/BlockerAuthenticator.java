package com.red5pro.server.plugin.simpleauth.impl;

import org.red5.server.api.IConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.red5pro.server.plugin.simpleauth.interfaces.IAuthenticationValidator;
import com.red5pro.server.plugin.simpleauth.interfaces.ISimpleAuthAuthenticator;

/**
 * This class is a authenticator implementation of
 * <tt>ISimpleAuthAuthenticator</tt>, which is used to reject incoming client
 * connections unconditionally.
 * 
 * @author Rajdeep Rath
 *
 */
public class BlockerAuthenticator implements ISimpleAuthAuthenticator {

	private static Logger logger = LoggerFactory.getLogger(BlockerAuthenticator.class);

	private IAuthenticationValidator source;

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
		return false;
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
