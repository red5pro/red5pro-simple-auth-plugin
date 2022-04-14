package com.red5pro.server.plugin.simpleauth.datasource.impl.roundtrip.stream.security;

import java.util.Iterator;
import java.util.Map;

import org.red5.server.api.IConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.red5pro.server.plugin.simpleauth.datasource.impl.roundtrip.RoundTripAuthValidator;

/**
 * Base adapter for security implementations.
 * 
 * @author Paul Gregoire
 *
 */
public abstract class SecurityAdapter {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	protected boolean isDebug = logger.isDebugEnabled();

	protected final RoundTripAuthValidator roundTripAuthValidator;

	// default response for allowed or not
	protected boolean defaultResponse = true;

	protected SecurityAdapter(RoundTripAuthValidator roundTripAuthValidator) {
		this.roundTripAuthValidator = roundTripAuthValidator;
	}

	protected void logConnectionParameters(IConnection connection) {
		if (isDebug) {
			Map<String, Object> attrs = connection.getAttributes();
			Iterator<Map.Entry<String, Object>> it = attrs.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, Object> pair = it.next();
				logger.debug("key : {} value : {}", pair.getKey(), pair.getValue());
			}
		}
	}

	public void setDefaultResponse(boolean defaultResponse) {
		this.defaultResponse = defaultResponse;
	}

}
