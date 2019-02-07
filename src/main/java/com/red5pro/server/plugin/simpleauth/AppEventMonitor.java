package com.red5pro.server.plugin.simpleauth;

import org.red5.server.adapter.IApplication;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.scope.IScope;
import org.red5.server.exception.ClientRejectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.red5pro.server.plugin.simpleauth.impl.AuthenticatorProvider;
import com.red5pro.server.plugin.simpleauth.interfaces.ISimpleAuthAuthenticator;

/**
 * This class works as a application level hook by implementing the
 * <tt>IApplication</tt>interface.This class is used to intercept a client
 * connection to the application. For details on application hooks see <a href=
 * "http://red5.org/javadoc/red5-server-common/org/red5/server/adapter/IApplication.html">IApplication</a>
 * 
 * @author Rajdeep Rath
 *
 */
public class AppEventMonitor implements IApplication {

	/**
	 * Logger
	 */
	private Logger log = LoggerFactory.getLogger(AppEventMonitor.class);

	/**
	 * The authentication provider
	 */
	AuthenticatorProvider provider;

	/**
	 * Stores reference to the scope that this class monitors
	 */
	IScope scope;

	/**
	 * Constructor for class AppEventMonitor
	 * 
	 * @param provider
	 *            The authentication provider object reference
	 */
	public AppEventMonitor(AuthenticatorProvider provider) {
		this.provider = provider;
	}

	/**
	 * Constructor for class AppEventMonitor
	 * 
	 * @param provider
	 *            The authentication provider object reference
	 * @param scope
	 *            The application scope that this class monitors.
	 */
	public AppEventMonitor(AuthenticatorProvider provider, IScope scope) {
		this.provider = provider;
		this.scope = scope;
	}

	@Override
	public boolean appStart(IScope app) {
		if (scope == null)
			this.scope = app;

		return true;
	}

	@Override
	public boolean appConnect(IConnection conn, Object[] params) {
		log.debug("appConnect");

		boolean authenticated;

		try {
			ISimpleAuthAuthenticator authenticator = provider.getAuthenticator(conn);
			authenticated = authenticator.authenticate(conn, params);

			if (!authenticated) {
				log.warn("Rejecting client connection due to failed authentication");
				throw new ClientRejectedException("Access denied due to authentication failure");
			}

			return true;
		} catch (Exception e) {
			String msg = "Access denied due to unknown error in authentication routine";
			log.warn("msg");

			throw new ClientRejectedException(msg);
		}
	}

	@Override
	public boolean appJoin(IClient client, IScope app) {
		return true;
	}

	@Override
	public void appDisconnect(IConnection conn) {
		log.debug("appDisconnect");
	}

	@Override
	public void appLeave(IClient client, IScope app) {
		log.debug("appLeave");
	}

	@Override
	public void appStop(IScope app) {

	}

	@Override
	public boolean roomStart(IScope room) {
		return true;
	}

	@Override
	public boolean roomConnect(IConnection conn, Object[] params) {
		return true;
	}

	@Override
	public boolean roomJoin(IClient client, IScope room) {
		log.debug("roomJoin");

		return true;
	}

	@Override
	public void roomDisconnect(IConnection conn) {
		log.debug("roomDisconnect");
	}

	@Override
	public void roomLeave(IClient client, IScope room) {
		log.debug("roomLeave");
	}

	@Override
	public void roomStop(IScope room) {
		log.debug("roomStop");
	}

}
