package com.red5pro.server.plugin.simpleauth.datasource.impl.roundtrip.stream.security;

import java.util.Iterator;
import java.util.Map;

import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IStreamPublishSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.red5pro.server.plugin.simpleauth.datasource.impl.roundtrip.RoundTripAuthValidator;

/**
 * This class implements the <tt>IStreamPublishSecurity</tt> interface to
 * intercept stream publish action. The implementation captures necessary
 * publish params and passes them to remote server via the
 * `RoundTripAuthValidator` class for authentication.
 * 
 * Publisher request is accepted or rejected based on remote server validation
 * response.
 * 
 * @author Rajdeep Rath
 *
 */
public class PublishSecurity implements IStreamPublishSecurity {

	private static Logger logger = LoggerFactory.getLogger(PublishSecurity.class);

	private RoundTripAuthValidator roundTripAuthValidator;

	public PublishSecurity(RoundTripAuthValidator roundTripAuthValidator) {
		this.roundTripAuthValidator = roundTripAuthValidator;
	}

	@Override
	public boolean isPublishAllowed(IScope scope, String name, String mode) {
		IConnection connection = Red5.getConnectionLocal();

		Map<String, Object> attrs = connection.getAttributes();
		Iterator<Map.Entry<String, Object>> it = attrs.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Object> pair = it.next();
			logger.debug("key : {} value : {}", pair.getKey(), pair.getValue());
		}

		return roundTripAuthValidator.onPublishAuthenticate(connection, scope, name);
	}

}
