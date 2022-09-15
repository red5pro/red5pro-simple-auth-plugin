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
package com.red5pro.server.plugin.simpleauth.impl;

import org.red5.server.api.IConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.red5pro.server.plugin.simpleauth.AuthenticatorType;
import com.red5pro.server.plugin.simpleauth.Configuration;
import com.red5pro.server.plugin.simpleauth.interfaces.IAuthenticationValidator;
import com.red5pro.server.plugin.simpleauth.interfaces.ISimpleAuthAuthenticator;
import com.red5pro.server.so.ISharedObjectCapableConnection;
import com.red5pro.server.stream.mpegts.IMPEGTSConnection;
import com.red5pro.server.stream.restreamer.IConnectorShell;
import com.red5pro.server.stream.rtsp.IRTSPConnection;
import com.red5pro.server.stream.srt.ISRTConnection;
import com.red5pro.server.stream.webrtc.IWebRTCConnection;

/**
 * This class is the
 *
 * <pre>
 * ISimpleAuthAuthenticator
 * </pre>
 *
 * provider for the authentication mechanism. It selects and executes the
 * correct
 *
 * <pre>
 * ISimpleAuthAuthenticator
 * </pre>
 *
 * implementation for a incoming IConnection object based on connection type and
 * configuration settings.
 *
 * @author Rajdeep Rath
 *
 */
public class AuthenticatorProvider {

    private static Logger logger = LoggerFactory.getLogger(AuthenticatorProvider.class);

    /**
     * The IAuthenticationValidator object to pass on to authenticator
     * implementations
     */
    IAuthenticationValidator validator;

    /**
     * Global settings for enabling or disabling authentication
     */
    boolean enabled;

    /**
     * Authenticator implementation reference for rtmp
     */
    ISimpleAuthAuthenticator rtmpAuthenticator;

    /**
     * Authenticator implementation reference for rtsp
     */
    ISimpleAuthAuthenticator rtspAuthenticator;

    /**
     * Authenticator implementation reference for webrtc
     */
    ISimpleAuthAuthenticator webRtcAuthenticator;

    /**
     * Authenticator implementation reference for unconditional reject
     */
    ISimpleAuthAuthenticator grumpyAuthenticator;

    /**
     * Authenticator implementation reference for unconditional allow
     */
    ISimpleAuthAuthenticator happyAuthenticator;

    /**
     * Authenticator implementation reference for SRT
     */
    ISimpleAuthAuthenticator srtAuthenticator;

    /**
     * Authenticator implementation reference for mpegts
     */
    ISimpleAuthAuthenticator mpegtsAuthenticator;

    /**
     * Authenticator implementation reference for http
     */
    ISimpleAuthAuthenticator httpAuthenticator;

    /**
     * Authenticator implementation reference for websocket
     */
    ISimpleAuthAuthenticator wsAuthenticator;

    /**
     * Global settings for enabling or disabling rtmp authentication
     */
    private boolean secureRTMP = true;

    /**
     * Global settings for enabling or disabling rtsp authentication
     */
    private boolean secureRTSP = true;

    /**
     * Global settings for enabling or disabling rtc authentication
     */
    private boolean secureRTC = true;

    /**
     * Global settings for enabling or disabling srt authentication
     */
    private boolean secureSRT = true;

    /**
     * Global settings for enabling or disabling mpegts authentication
     */
    private boolean secureMPEGTS = true;

    /**
     * Global settings for enabling or disabling http authentication
     */
    private boolean secureHTTP = true;

    /**
     * Global settings for enabling or disabling websocket authentication
     */
    private boolean secureWS = true;

    /**
     * Global settings for allowing or disallowing query params checking rtmp
     * clients
     */
    private boolean rtmpAcceptsQueryParamsEnabled;

    /**
     * Global settings for allowed rtmp agent string
     */
    private String allowedRtmpAgents;

    /**
     * Constructor for AuthenticatorProvider
     */
    public AuthenticatorProvider() {

    }

    /**
     * Constructor for AuthenticatorProvider
     *
     * @param config
     *            The Configuration object
     */
    public AuthenticatorProvider(Configuration config) {
        this.setEnabled(config.isActive());
        this.setValidator(config.getValidator());
        this.setSecureRTMP(config.isRtmp());
        this.setSecureRTSP(config.isRtsp());
        this.setSecureRTC(config.isRtc());
        this.setSecureSRT(config.isSrt());
        this.setSecureMPEGTS(config.isMpegts());
        this.setSecureHTTP(config.isHttp());
        this.setSecureWS(config.isWs());
        this.setRtmpAcceptsQueryParamsEnabled(config.isRtmpAllowQueryParamsEnabled());
        this.setAllowedRtmpAgents(config.getAllowedRtmpAgents());
    }

    /**
     * Entry point
     */
    public void initialize() {
        // rtmp
        rtmpAuthenticator = new RTMPAuthenticator(rtmpAcceptsQueryParamsEnabled, allowedRtmpAgents);
        rtmpAuthenticator.setDataSource(validator);
        // rtsp
        rtspAuthenticator = new RTSPAuthenticator();
        rtspAuthenticator.setDataSource(validator);
        // webrtc
        webRtcAuthenticator = new RTCAuthenticator();
        webRtcAuthenticator.setDataSource(validator);
        // blocking
        grumpyAuthenticator = new BlockerAuthenticator();
        grumpyAuthenticator.setDataSource(validator);
        // pass-thru
        happyAuthenticator = new PassThruAuthenticator();
        happyAuthenticator.setDataSource(validator);
        // srt
        srtAuthenticator = new SRTAuthenticator();
        srtAuthenticator.setDataSource(validator);
        // mpeg-ts
        mpegtsAuthenticator = new MpegTsAuthenticator();
        mpegtsAuthenticator.setDataSource(validator);
        // http
        httpAuthenticator = new HTTPAuthenticator();
        httpAuthenticator.setDataSource(validator);
        // ws
        wsAuthenticator = new WebSocketAuthenticator();
        wsAuthenticator.setDataSource(validator);
    }

    /**
     * Constructor for AuthenticatorProvider
     *
     * @param validator
     *            An IAuthenticationValidator object
     */
    public AuthenticatorProvider(IAuthenticationValidator validator) {
        this.validator = validator;
    }

    /**
     * Returns appropriate ISimpleAuthAuthenticator implementation based on
     * IConnection type
     *
     * @param connection
     *            The client IConnection object to check
     * @return A ISimpleAuthAuthenticator which will be used to authenticate the
     *         connection
     */
    public ISimpleAuthAuthenticator getAuthenticator(IConnection connection) {
        if (!enabled) {
            return happyAuthenticator;
        } else {
            logger.info("getAuthenticator for {}", connection);
            if (connection instanceof IRTSPConnection) {
                if (secureRTSP) {
                    return rtspAuthenticator;
                } else {
                    return happyAuthenticator;
                }
            } else if (connection instanceof IWebRTCConnection) {
                if (secureRTC) {
                    return webRtcAuthenticator;
                } else {
                    return happyAuthenticator;
                }
            } else if (connection instanceof IMPEGTSConnection) {
                if (secureMPEGTS) {
                    return mpegtsAuthenticator;
                } else {
                    return happyAuthenticator;
                }
            } else if (connection instanceof ISRTConnection) {
                if (secureSRT) {
                    return srtAuthenticator;
                } else {
                    return happyAuthenticator;
                }
            } else if (connection instanceof IConnectorShell) {
                return happyAuthenticator;
            } else if (connection.getClass().getName().contains("RTMP")) {
                logger.info("RTMP based connection detected");
                if (secureRTMP) {
                    return rtmpAuthenticator;
                } else {
                    return happyAuthenticator;
                }
            } else if (connection instanceof ISharedObjectCapableConnection) {
                // XXX on the subject of SO support, we'll allow intra-node
                // comms via ISharedObjectCapableConnection type, but this
                // interface is not yet in commons, but auth links mega, so its
                // ok for now. Also returning "happy" since SO checks are not
                // enforced at the transport protocol level.
                return happyAuthenticator;
            } else {
                // unknown protocol
                logger.error("Unknown connection type {}", connection.getClass().getCanonicalName());
                return grumpyAuthenticator;
            }
        }
    }

    /**
     * Returns appropriate ISimpleAuthAuthenticator implementation based on
     * AuthenticatorType.
     *
     * @param type
     *            AuthenticatorType
     * @return An ISimpleAuthAuthenticator which will be used to authenticate
     */
    public ISimpleAuthAuthenticator getAuthenticator(AuthenticatorType type) {
        switch (type) {
            case RTMP:
                if (secureRTMP) {
                    return rtmpAuthenticator;
                }
                return happyAuthenticator;
            case RTSP:
                if (secureRTSP) {
                    return rtspAuthenticator;
                }
                return happyAuthenticator;
            case RTC:
                if (secureRTC) {
                    return webRtcAuthenticator;
                }
                return happyAuthenticator;
            case MPEGTS:
                if (secureMPEGTS) {
                    return mpegtsAuthenticator;
                }
                return happyAuthenticator;
            case SRT:
                if (secureSRT) {
                    return srtAuthenticator;
                }
                return happyAuthenticator;
            case HTTP:
                if (secureHTTP) {
                    return httpAuthenticator;
                }
                return happyAuthenticator;
            case WS:
                if (secureWS) {
                    return wsAuthenticator;
                }
                return happyAuthenticator;
            case HAPPY:
                return happyAuthenticator;
            default:
                logger.error("Unknown type {}", type);
            case GRUMPY:
                return grumpyAuthenticator;
        }
    }

    /**
     * Returns the value of validator
     *
     * @return An IAuthenticationValidator object
     */
    public IAuthenticationValidator getValidator() {
        return validator;
    }

    /**
     * Sets the value of validator
     *
     * @param validator
     *            The IAuthenticationValidator to set
     */
    public void setValidator(IAuthenticationValidator validator) {
        this.validator = validator;
    }

    /**
     * Returns the global default state for rtmp authentication
     *
     * @return true if rtmp authentication is enabled, otherwise false
     */
    public boolean isSecureRTMP() {
        return secureRTMP;
    }

    /**
     * Sets the global default state for rtmp authentication
     *
     * @param secureRTMP
     *            Boolean value to set
     */
    public void setSecureRTMP(boolean secureRTMP) {
        this.secureRTMP = secureRTMP;
    }

    /**
     * Returns the global default state for rtsp authentication
     *
     * @return true if rtsp authentication is enabled, otherwise false
     */
    public boolean isSecureRTSP() {
        return secureRTSP;
    }

    /**
     * Sets the global default state for rtsp authentication
     *
     * @param secureRTSP
     *            Boolean value to set
     */
    public void setSecureRTSP(boolean secureRTSP) {
        this.secureRTSP = secureRTSP;
    }

    /**
     * Returns the global default state for rtc authentication
     *
     * @return true if rtc authentication is enabled, otherwise false
     */
    public boolean isSecureRTC() {
        return secureRTC;
    }

    /**
     * Sets the global default state for rtc authentication
     *
     * @param secureRTC
     *            Boolean value to rtc
     */
    public void setSecureRTC(boolean secureRTC) {
        this.secureRTC = secureRTC;
    }

    /**
     * Returns the global default state for srt authentication
     *
     * @return true if srt authentication is enabled, otherwise false
     */
    public boolean isSecureSRT() {
        return secureSRT;
    }

    /**
     * Sets the global default state for srt authentication
     *
     * @param secureSRT
     *            Boolean value to srt
     */
    public void setSecureSRT(boolean secureSRT) {
        this.secureSRT = secureSRT;
    }

    /**
     * Returns the global default state for mpegts authentication
     *
     * @return true if mpegts authentication is enabled, otherwise false
     */
    public boolean isSecureMPEGTS() {
        return secureMPEGTS;
    }

    /**
     * Sets the global default state for mpegts authentication
     *
     * @param secureMPEGTS
     *            Boolean value to mpegts
     */
    public void setSecureMPEGTS(boolean secureMPEGTS) {
        this.secureMPEGTS = secureMPEGTS;
    }

    /**
     * Returns the global default state for http authentication
     *
     * @return true if http authentication is enabled, otherwise false
     */
    public boolean isSecureHTTP() {
        return secureHTTP;
    }

    /**
     * Sets the global default state for http authentication
     *
     * @param secureHTTP
     *            Boolean value to http
     */
    public void setSecureHTTP(boolean secureHTTP) {
        this.secureHTTP = secureHTTP;
    }

    /**
     * Returns the global default state for ws authentication
     *
     * @return true if ws authentication is enabled, otherwise false
     */
    public boolean isSecureWS() {
        return secureWS;
    }

    /**
     * Sets the global default state for ws authentication
     *
     * @param secureWS
     *            Boolean value to ws
     */
    public void setSecureWS(boolean secureWS) {
        this.secureWS = secureWS;
    }

    /**
     * Returns the enabled state of the AuthenticationProvider
     *
     * @return true if the component is enabled, otherwise false
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the enabled state of the AuthenticationProvider
     *
     * @param enabled
     *            The Boolean value to set
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Returns the global state of rtmp query string check in authentication
     *
     * @return true if check is enabled, otherwise false
     */
    public boolean isRtmpAcceptsQueryParamsEnabled() {
        return rtmpAcceptsQueryParamsEnabled;
    }

    /**
     * Sets the global state of rtmp query string check in authentication
     *
     * @param rtmpAcceptsQueryParams
     *            The Boolean value to set
     */
    public void setRtmpAcceptsQueryParamsEnabled(boolean rtmpAcceptsQueryParams) {
        this.rtmpAcceptsQueryParamsEnabled = rtmpAcceptsQueryParams;
    }

    /**
     * Returns the global value for allowed rtmp agents
     *
     * @return comma separated string of rtmp agents or * for all
     */
    public String getAllowedRtmpAgents() {
        return allowedRtmpAgents;
    }

    /**
     * Sets the global value for allowed rtmp agents
     *
     * @param allowedRtmpAgents
     *            A comma separated string of rtmp agents or * for all
     */
    public void setAllowedRtmpAgents(String allowedRtmpAgents) {
        this.allowedRtmpAgents = allowedRtmpAgents;
    }

}
