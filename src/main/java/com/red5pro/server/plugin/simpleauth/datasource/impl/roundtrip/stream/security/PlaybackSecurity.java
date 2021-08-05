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

import java.util.Iterator;
import java.util.Map;

import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IStreamPlaybackSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.red5pro.server.plugin.simpleauth.datasource.impl.roundtrip.RoundTripAuthValidator;

/**
 * This class implements the <pre>IStreamPlaybackSecurity</pre> interface to
 * intercept stream subscribe action. The implementation captures necessary
 * playback request params and passes them to remote server via the
 * `RoundTripAuthValidator` class for authentication.
 * 
 * Subscriber request is accepted or rejected based on remote server validation
 * response.
 * 
 * @author Rajdeep Rath
 *
 */

public class PlaybackSecurity implements IStreamPlaybackSecurity {

	private static Logger logger = LoggerFactory.getLogger(PlaybackSecurity.class);

	private RoundTripAuthValidator roundTripAuthValidator;

	public PlaybackSecurity(RoundTripAuthValidator roundTripAuthValidator) {
		this.roundTripAuthValidator = roundTripAuthValidator;
	}

	@Override
	public boolean isPlaybackAllowed(IScope scope, String name, int start, int length, boolean flushPlaylist) {
		IConnection connection = Red5.getConnectionLocal();

		Map<String, Object> attrs = connection.getAttributes();
		Iterator<Map.Entry<String, Object>> it = attrs.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Object> pair = it.next();
			logger.debug("key : {} value : {}", pair.getKey(), pair.getValue());
		}

		return roundTripAuthValidator.onPlaybackAuthenticate(connection, scope, name);
	}

}
