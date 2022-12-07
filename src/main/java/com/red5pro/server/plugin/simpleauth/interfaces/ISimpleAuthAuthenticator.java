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
package com.red5pro.server.plugin.simpleauth.interfaces;

import org.red5.server.api.IConnection;

import com.red5pro.server.plugin.simpleauth.AuthenticatorType;

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
     * This method is called when a client tries to connect to your application.
     *
     * @param type
     *            AuthenticatorType connection implementation hint
     * @param connection
     *            The Object representing the client, may not be implementation of
     *            IConnection
     * @param params
     *            The connection parameters provided by the client. For rtsp, rtc
     *            and rtmp clients (which pass params via query params) the
     *            parameters are available as a java Map in the first element of the
     *            array.
     * @return true if connection should be allowed, otherwise false
     */
    public boolean authenticate(AuthenticatorType type, Object connection, Object[] params);

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
