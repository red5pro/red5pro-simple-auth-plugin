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
 *
 * <pre>
 * IApplication
 * </pre>
 *
 * interface.This class is used to intercept a client connection to the
 * application. For details on application hooks see <a href=
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
        if (scope == null) {
            this.scope = app;
        }
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
            log.warn("{}", msg, e);
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
