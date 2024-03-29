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
package com.red5pro.server.plugin.simpleauth.datasource.impl.roundtrip.stream.security;

import java.util.List;

import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.api.so.ISharedObjectSecurity;

import com.red5pro.server.plugin.simpleauth.datasource.impl.roundtrip.RoundTripAuthValidator;

/**
 * This class implements the
 *
 * <pre>
 * ISharedObjectSecurity
 * </pre>
 *
 * interface to intercept shared object actions. The implementation captures
 * necessary params and passes them to remote server via the
 * `RoundTripAuthValidator` class for authentication.
 *
 * Shared object request is accepted or rejected based on remote server
 * validation response.
 *
 * @author Paul Gregoire
 *
 */
public class SharedObjectSecurity extends SecurityAdapter implements ISharedObjectSecurity {

    public SharedObjectSecurity(RoundTripAuthValidator roundTripAuthValidator) {
        super(roundTripAuthValidator);
    }

    @Override
    public boolean isCreationAllowed(IScope scope, String name, boolean persistent) {
        // an npe is possible farther down, if the connection isn't available here
        IConnection connection = Red5.getConnectionLocal();
        if (connection != null) {
            // attrs inspection is only for debug level logging, using a guard for
            // optimization
            logConnectionParameters(connection);
            return roundTripAuthValidator.onSharedObjectAuthenticate(connection, scope, name);
        }
        // default result if a connection isn't present
        return defaultResponse;
    }

    @Override
    public boolean isConnectionAllowed(ISharedObject so) {
        // an npe is possible farther down, if the connection isn't available here
        IConnection connection = Red5.getConnectionLocal();
        if (connection != null) {
            // attrs inspection is only for debug level logging, using a guard for
            // optimization
            logConnectionParameters(connection);
            // XXX may have to switch from the actual so being the "scope" to the parent, if
            // theres an issue
            return roundTripAuthValidator.onSharedObjectAuthenticate(connection, so.getParent(), so.getName());
        }
        // default result if a connection isn't present
        return defaultResponse;
    }

    @Override
    public boolean isWriteAllowed(ISharedObject so, String key, Object value) {
        return defaultResponse;
    }

    @Override
    public boolean isDeleteAllowed(ISharedObject so, String key) {
        return defaultResponse;
    }

    @Override
    public boolean isSendAllowed(ISharedObject so, String message, List<?> arguments) {
        return defaultResponse;
    }

}
