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
 * This class implements the <tt>IStreamPlaybackSecurity</tt> interface to
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
