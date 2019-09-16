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

import org.red5.server.api.IConnection;

public class ConnectionUtils {

    static String RTSPCONNECTION = "com.red5pro.server.stream.rtsp.RTSPMinaConnection";

    static String RTMPCONNECTION = "org.red5.server.net.rtmp.RTMPMinaConnection";

    static String RTCCONNECTION = "com.red5pro.webrtc.RTCConnection";

    /**
     * Returns human readable string for a given IConnection type
     *  
     * @param connection
     * @return
     */
    public static String getConnectionType(IConnection connection) {
        String connectionClassName = connection.getClass().getCanonicalName();

        if (connectionClassName.equalsIgnoreCase(RTMPCONNECTION)) {
            return "rtmp";
        } else if (connectionClassName.equalsIgnoreCase(RTSPCONNECTION)) {
            return "rtsp";
        } else if (connectionClassName.equalsIgnoreCase(RTCCONNECTION)) {
            return "rtc";
        }

        return null;
    }

    /**
     * Returns boolean true if connection is a RTMPMinaConnection object, false otherwise
     * 
     * @param connection
     * @return
     */
    public static boolean isRTMP(IConnection connection) {
        String connectionClassName = connection.getClass().getCanonicalName();
        return connectionClassName.equalsIgnoreCase(RTMPCONNECTION);
    }

    /**
     * Returns boolean true if connection is a RTSPMinaConnection object, false otherwise
     * 
     * @param connection
     * @return
     */
    public static boolean isRTSP(IConnection connection) {
        String connectionClassName = connection.getClass().getCanonicalName();
        return connectionClassName.equalsIgnoreCase(RTSPCONNECTION);
    }

    /**
     * Returns boolean true if connection is a RTCConnection object, false otherwise
     * 
     * @param connection
     * @return
     */
    public static boolean isRTC(IConnection connection) {
        String connectionClassName = connection.getClass().getCanonicalName();
        return connectionClassName.equalsIgnoreCase(RTCCONNECTION);
    }

}