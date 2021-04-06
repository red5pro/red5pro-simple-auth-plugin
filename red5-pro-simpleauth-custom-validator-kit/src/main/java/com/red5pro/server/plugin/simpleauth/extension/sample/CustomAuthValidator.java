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
package com.red5pro.server.plugin.simpleauth.extension.sample;

import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.Red5;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.red5pro.server.plugin.simpleauth.interfaces.IAuthenticationValidator;

public class CustomAuthValidator implements IAuthenticationValidator {

    private static Logger logger = LoggerFactory.getLogger(CustomAuthValidator.class);

    private IContext context;

    private MultiThreadedApplicationAdapter adapter;

    @Override
    public void initialize() {

        logger.info("CustomAuthValidator initializing...");

        if (adapter != null) {
            // Enable the line below to activate publish interceptor
            //adapter.registerStreamPublishSecurity(new PublishSecurity(this));

            // Enable the line below to activate playback interceptor
            //adapter.registerStreamPlaybackSecurity(new PlaybackSecurity(this));
        } else {
            logger.error("Something is wrong. i dont have access to application adapter");
        }

        logger.debug("adapter = {}", adapter);
        logger.debug("context = {}", context);
    }

    /**
     * Return true or false to determine whether client can connect or not.
     * Note : if you want to extract custom params its a good thing to check connection type first.
     * If you want to do publish and playback validation, then make sure to return true in this method unconditionally. 
     */
    @Override
    public boolean onConnectAuthenticate(String arg0, String arg1, Object[] arg2) {

        logger.info("Client attempting to connect...");

        IConnection conn = Red5.getConnectionLocal();

        if (ConnectionUtils.isRTC(conn)) {
            // do something special
        } else if (ConnectionUtils.isRTMP(conn)) {
            // do something special
        } else if (ConnectionUtils.isRTSP(conn)) {
            // do something special
        }

        return true;
    }

    public MultiThreadedApplicationAdapter getAdapter() {
        return adapter;
    }

    public void setAdapter(MultiThreadedApplicationAdapter adapter) {
        this.adapter = adapter;
    }

    public IContext getContext() {
        return context;
    }

    public void setContext(IContext context) {
        this.context = context;
    }

}
