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
package com.red5pro.server.plugin.simpleauth.datasource.impl.roundtrip;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.red5.server.adapter.IApplication;
import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.red5pro.server.plugin.simpleauth.datasource.impl.roundtrip.model.AuthData;
import com.red5pro.server.plugin.simpleauth.datasource.impl.roundtrip.stream.security.PlaybackSecurity;
import com.red5pro.server.plugin.simpleauth.datasource.impl.roundtrip.stream.security.PublishSecurity;
import com.red5pro.server.plugin.simpleauth.interfaces.IAuthenticationValidator;
import com.red5pro.server.plugin.simpleauth.utils.Utils;

/**
 * This class is a sample implementation of the
 * <tt>IAuthenticationValidator</tt> interface. It is meant to serve as a
 * default placeholder and an example for a <tt>IAuthenticationValidator</tt>
 * implementation. The implementation has access to your data source which
 * provides a means to validate credentials and other parameters.
 *
 * This sample implementation uses a remote server for validating parameters
 * passed by clients.
 *
 * @author Rajdeep Rath
 *
 */
public class RoundTripAuthValidator implements IAuthenticationValidator, IApplication {

	/**
	 * Logger
	 */
	private static Logger logger = LoggerFactory.getLogger(RoundTripAuthValidator.class);

	/**
	 * Username parameter name constant
	 */
	public static final String USERNAME = "username";

	/**
	 * Password parameter name constant
	 */
	public static final String PASSWORD = "password";

	/**
	 * Token parameter name constant
	 */
	public static final String TOKEN = "token";

	/**
	 * ConnectionResponseData parameter name constant. Used by clients to provide
	 * additional data
	 */
	public static final String AUTHENTICATION_RESPONSE_DATA = "authenticationResponseData";

	/**
	 * StreamId parameter name constant
	 */
	public static final String STREAMID = "streamID";

	/**
	 * StreamId parameter name constant
	 */
	public static final String ROLE_TYPE = "roletype";

	/**
	 * StreamId parameter name constant
	 */
	public static final String URL = "url";

	/**
	 * StreamId parameter name constant
	 */
	public static final String SIGNED_URL = "signedURL";

	/**
	 * Result parameter name constant
	 */
	public static final String RESULT = "result";

	/**
	 * Publisher parameter name constant
	 */
	public static final String PUBLISHER = "publisher";

	/**
	 * Publisher parameter name constant
	 */
	public static final String SUBSCRIBER = "subscriber";

	/**
	 * Property to store validation endpoint path (relative to root)
	 */
	private String validateEndPoint;

	/**
	 * Property to store invalidation endpoint path (relative to root)
	 */
	private String invalidateEndPoint;

	/**
	 * Optional authentication mode. If set to true, clients are allowed access
	 * initially and validated in a parallel thread instead of blocking access for
	 * authentication
	 */
	private boolean lazyAuth = false;

	/**
	 * Property to indicate whether `token` parameter is optional or mandatory
	 */
	private boolean clientTokenRequired = false;

	/**
	 * Red5 Context object. This is helpful in resolving resources on the file
	 * system
	 */
	private IContext context;

	/**
	 * Red5 application object. This gives access to the parent application
	 */
	private MultiThreadedApplicationAdapter adapter;

	/**
	 * Gson object for serializing/deserializing JSON
	 */
	private Gson gson;

	/**
	 * Defines the HTTP client timeout
	 */
	private static final int TIMEOUT = 9000;

	private ExecutorService threadPoolExecutor;

	/**
	 * Stores the remote server access protocol (HTTP/HTTPS)
	 */
	private String protocol;

	/**
	 * Stores remote server host name
	 */
	private String host;

	/**
	 * Stores remote access port (80 or 443 or custom)
	 */
	private String port;

	public void setProtocol(String p) {
		protocol = p;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setHost(String h) {
		host = h;
	}

	public String getHost() {
		return host;
	}

	public void setPort(String p) {
		port = p;
	}

	public String getPort() {
		return port;
	}

	@Override
	public void initialize() {

		logger.info("initialization part");

		if (this.lazyAuth) {
			threadPoolExecutor = Executors.newCachedThreadPool();
		}

		gson = new Gson();

		if (adapter != null) {
			adapter.registerStreamPublishSecurity(new PublishSecurity(this));
			adapter.registerStreamPlaybackSecurity(new PlaybackSecurity(this));
			adapter.addListener(this);
		} else {
			logger.error("hmph! Something is wrong. I dont have access to application adapter");
		}

		logger.debug("adapter = {}", adapter);
		logger.debug("context = {}", context);

		logger.debug("auth host = {}", this.host);
		logger.debug("auth port = {}", this.port);
		logger.debug("auth protocol = {}", this.protocol);

		logger.debug("authEndPoint = {}", validateEndPoint);
		logger.debug("invalidateEndPoint = {}", invalidateEndPoint);
		logger.debug("lazyAuth = {}", lazyAuth);
	}

	@Override
	public boolean onConnectAuthenticate(String username, String password, Object[] rest) {
		IConnection connection = Red5.getConnectionLocal();

		if (username == null || password == null) {
			logger.error("One or more missing parameter(s). Parameter 'username' and/or 'password' not provided");
			return false;
		}

		// just store parameters as we will be using these later
		connection.setAttribute(USERNAME, username);
		connection.setAttribute(PASSWORD, password);

		logger.debug("Parameters 'username' and 'password' stored on client connection! {}", connection);

		String token = null;

		if (rest.length == 1) {
			if (rest[0] instanceof Map) {
				Map<String, Object> map = (Map<String, Object>) rest[0];
				// collect token if exists
				if (map.containsKey(TOKEN)) {
					token = String.valueOf(map.get(TOKEN));
					connection.setAttribute(TOKEN, token);

					logger.debug("Parameter 'token' stored on client connection {}", connection);
				} else if (clientTokenRequired && !username.equals("cluster-restreamer")) {
					logger.error("Client 'token' is required but was not provided by connection {}.", connection);
					return false;
				}
			} else {
				logger.error("Unexpected parameter!. Expected Map got {}", rest[0].getClass().getName());
				return false;
			}
		} else if (rest.length > 1) {
			logger.info("rtmp");
			// probably rtmp connection params
			if (rest.length >= 3) {
				// expect token as third param
				token = String.valueOf(rest[2]);
				connection.setAttribute(TOKEN, token);
				logger.debug("Parameter 'token' stored on client connection {}", connection);
			} else if (clientTokenRequired && !username.equals("cluster-restreamer")) {
				logger.error("Client 'token' is required but was not provided by connection {}.", connection);
				return false;
			}
		}

		return true;
	}

	/**
	 * Authenticates a publisher connection, via remote server
	 *
	 * @param conn
	 *            The IConnection object representing the connection
	 * @param scope
	 *            The scope where client is attempting to publishing
	 * @param stream
	 *            The stream name that the client is trying to publish
	 *
	 * @return Boolean true if validation is successful, otherwise false
	 */
	public boolean onPublishAuthenticate(IConnection conn, IScope scope, String stream) {
		String username = conn.getStringAttribute(USERNAME);
		String password = conn.getStringAttribute(PASSWORD);
		String token = (conn.hasAttribute(TOKEN)) ? conn.getStringAttribute(TOKEN) : "";
		String type = PUBLISHER;
		String contextPath = conn.getScope().getContextPath();
		if (contextPath.charAt(0) == '/') {
			contextPath = contextPath.substring(1);
		}

		if (password != null && username != null) {
			if (!this.lazyAuth) {
				try {
					JsonObject result = authenticateOverHttp(type, username, password, token, stream, contextPath);
					boolean canStream = result.get(RESULT).getAsBoolean();
					if (canStream) {
						conn.setAttribute(ROLE_TYPE, type);
						conn.setAttribute(STREAMID, stream);

						if (result.has(URL)) {
							String url = result.get(URL).getAsString();
							conn.setAttribute(SIGNED_URL, url);
						} else {
							logger.debug("No Signed URL supplied");
						}

						if (result.has(AUTHENTICATION_RESPONSE_DATA)) {
							String url = result.get(AUTHENTICATION_RESPONSE_DATA).getAsString();
							conn.setAttribute(AUTHENTICATION_RESPONSE_DATA, url);
						}
					}
					return canStream;
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
			} else {
				this.threadPoolExecutor
						.submit(new LazyAuthentication(conn, type, username, password, token, stream, contextPath));
				return true;
			}
		}

		return true;
	}

	/**
	 * Authenticates a subscriber connection, via remote server
	 *
	 * @param conn
	 *            The IConnection object representing the connection
	 * @param scope
	 *            The scope where client is attempting to subscribe
	 * @param stream
	 *            The stream name that the client is trying to subscribe to
	 *
	 * @return Boolean true if validation is successful, otherwise false
	 */
	public boolean onPlaybackAuthenticate(IConnection conn, IScope scope, String stream) {
		String username = conn.getStringAttribute(USERNAME);
		String password = conn.getStringAttribute(PASSWORD);
		String token = (conn.hasAttribute(TOKEN)) ? conn.getStringAttribute(TOKEN) : "";
		String type = SUBSCRIBER;
		String contextPath = conn.getScope().getContextPath();
		if (contextPath.charAt(0) == '/') {
			contextPath = contextPath.substring(1);
		}

		if (password != null && username != null) {
			if (username.equals("cluster-restreamer")) {
				return Utils.validateClusterReStreamer(password);
			} else if (!this.lazyAuth) {
				try {
					JsonObject result = authenticateOverHttp(type, username, password, token, stream, contextPath);
					boolean canStream = result.get(RESULT).getAsBoolean();
					if (canStream) {
						conn.setAttribute(ROLE_TYPE, type);
						conn.setAttribute(STREAMID, stream);

						if (result.has(URL)) {
							String url = result.get(URL).getAsString();
							conn.setAttribute(SIGNED_URL, url);
						}
						if (result.has(AUTHENTICATION_RESPONSE_DATA)) {
							String url = result.get(AUTHENTICATION_RESPONSE_DATA).getAsString();
							conn.setAttribute(AUTHENTICATION_RESPONSE_DATA, url);
						}
					}
					return canStream;
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
			} else {
				this.threadPoolExecutor
						.submit(new LazyAuthentication(conn, type, username, password, token, stream, contextPath));
				return true;
			}
		}

		return true;
	}

	/**
	 * Method to authentication/validate client via remote server over http/https
	 *
	 * @param type
	 *            The client type to validate - `publisher or subscriber`
	 * @param username
	 *            The `username` parameter provided by the client
	 * @param password
	 *            The `password` parameter provided by the client
	 * @param token
	 *            The `token` parameter provided by the client
	 * @param stream
	 *            The `stream name` for which validation is required
	 *
	 * @return JsonObject JSON payload response from the remote server
	 */
	public JsonObject authenticateOverHttp(String type, String username, String password, String token, String stream,
			String contextPath) {
		JsonObject result = null;
		CloseableHttpClient client = null;

		try {
			AuthData data = new AuthData();
			data.setType(type);
			data.setUsername(username);
			data.setPassword(password);
			data.setToken(token);
			data.setStreamID(stream);
			data.setScope(contextPath);

			String json = gson.toJson(data);

			client = HttpClients.createDefault();

			HttpPost httpPost = new HttpPost(this.protocol + this.host + ":" + this.port + this.validateEndPoint);

			StringEntity entity = new StringEntity(json);
			httpPost.setEntity(entity);
			httpPost.setHeader("Accept", "application/json");
			httpPost.setHeader("Content-type", "application/json");

			RequestConfig.Builder requestConfig = RequestConfig.custom();
			requestConfig.setConnectTimeout(TIMEOUT);
			requestConfig.setConnectionRequestTimeout(TIMEOUT);
			requestConfig.setSocketTimeout(TIMEOUT);

			httpPost.setConfig(requestConfig.build());

			CloseableHttpResponse response = client.execute(httpPost);

			if (response.getStatusLine().getStatusCode() == 200) {
				String responseBody = EntityUtils.toString(response.getEntity());
				logger.info("responseBody = {}", responseBody);

				JsonParser parser = new JsonParser();
				JsonElement obj = parser.parse(responseBody);

				result = obj.getAsJsonObject();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (client != null) {
				try {
					client.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return result;
	}

	/**
	 * Method to invalidate client via remote server over http/https
	 *
	 * @param username
	 *            The `username` parameter provided by the client
	 * @param password
	 *            The `password` parameter provided by the client
	 * @param token
	 *            The `token` parameter provided by the client
	 * @param stream
	 *            The `stream name` for which validation is required
	 *
	 * @return JsonObject JSON payload response from the remote server
	 */
	public JsonObject invalidateCredentialsOverHttp(String username, String password, String token, String stream,
			String contextPath) {
		JsonObject result = null;
		CloseableHttpClient client = null;

		try {
			AuthData data = new AuthData();
			data.setUsername(username);
			data.setPassword(password);
			data.setToken(token);
			data.setStreamID(stream);
			data.setScope(contextPath);

			String json = gson.toJson(data);

			client = HttpClients.createDefault();

			HttpPost httpPost = new HttpPost(this.protocol + this.host + ":" + this.port + this.invalidateEndPoint);

			StringEntity entity = new StringEntity(json);
			httpPost.setEntity(entity);
			httpPost.setHeader("Accept", "application/json");
			httpPost.setHeader("Content-type", "application/json");
			CloseableHttpResponse response = client.execute(httpPost);

			if (response.getStatusLine().getStatusCode() == 200) {
				String responseBody = EntityUtils.toString(response.getEntity());
				logger.info("responseBody = {}", responseBody);

				JsonParser parser = new JsonParser();
				JsonElement obj = parser.parse(responseBody);

				result = obj.getAsJsonObject();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (client != null) {
				try {
					client.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return result;
	}

	/**
	 * Async task class, used for `lazy` validation/invalidation
	 *
	 * @author Rajdeep Rath
	 *
	 */
	class LazyAuthentication implements Runnable {

		String type, username, password, token, stream, contextPath;
		IConnection conn;

		public LazyAuthentication(IConnection conn, String type, String username, String password, String token,
				String stream, String contextPath) {
			this.conn = conn;
			this.type = type;
			this.username = username;
			this.password = password;
			this.token = token;
			this.stream = stream;
			this.contextPath = contextPath;
		}

		@Override
		public void run() {
			boolean canStream = false;

			try {
				JsonObject result = authenticateOverHttp(type, username, password, token, stream, contextPath);
				canStream = result.get(RESULT).getAsBoolean();
				if (!canStream && conn.isConnected()) {
					logger.warn("Closing connected client due to authentication failure");
					conn.close();
				} else {
					if (result.has(URL)) {
						String url = result.get(URL).getAsString();
						conn.setAttribute(SIGNED_URL, url);
					}
					if (result.has(AUTHENTICATION_RESPONSE_DATA)) {
						String url = result.get(AUTHENTICATION_RESPONSE_DATA).getAsString();
						conn.setAttribute(AUTHENTICATION_RESPONSE_DATA, url);
					}
				}

			} catch (Exception e) {
				canStream = false;
				e.printStackTrace();
			}
		}
	}

	public boolean isLazyAuth() {
		return lazyAuth;
	}

	public void setLazyAuth(boolean lazyAuth) {
		this.lazyAuth = lazyAuth;
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

	@Override
	public void appDisconnect(IConnection conn) {

		if (conn.hasAttribute(ROLE_TYPE) && conn.getStringAttribute(ROLE_TYPE).equals("publisher")
				&& conn.hasAttribute(STREAMID) && conn.hasAttribute(USERNAME) && conn.hasAttribute(PASSWORD)) {

			String username = conn.getStringAttribute(USERNAME);
			String password = conn.getStringAttribute(PASSWORD);
			String streamID = conn.getStringAttribute(STREAMID);
			String token = conn.getStringAttribute(TOKEN);
			String contextPath = conn.getScope().getContextPath();
			if (contextPath.charAt(0) == '/') {
				contextPath = contextPath.substring(1);
			}

			if (invalidateEndPoint != null && invalidateEndPoint.length() > 3) {
				invalidateCredentialsOverHttp(username, password, token, streamID, contextPath);
			}
		}
	}

	@Override
	public boolean appStart(IScope app) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean appConnect(IConnection conn, Object[] params) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean appJoin(IClient client, IScope app) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void appLeave(IClient client, IScope app) {
		// TODO Auto-generated method stub

	}

	@Override
	public void appStop(IScope app) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean roomStart(IScope room) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean roomConnect(IConnection conn, Object[] params) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean roomJoin(IClient client, IScope room) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void roomDisconnect(IConnection conn) {
		// TODO Auto-generated method stub

	}

	@Override
	public void roomLeave(IClient client, IScope room) {
		// TODO Auto-generated method stub

	}

	@Override
	public void roomStop(IScope room) {
		// TODO Auto-generated method stub

	}

	public String getValidateCredentialsEndPoint() {
		return validateEndPoint;
	}

	public void setValidateCredentialsEndPoint(String validateEndPoint) {
		this.validateEndPoint = validateEndPoint;
	}

	public String getinvalidateCredentialsEndPoint() {
		return invalidateEndPoint;
	}

	public void setinvalidateCredentialsEndPoint(String invalidateEndPoint) {
		this.invalidateEndPoint = invalidateEndPoint;
	}

	public boolean getClientTokenRequired() {
		return clientTokenRequired;
	}

	public void setClientTokenRequired(boolean tokenRequired) {
		this.clientTokenRequired = tokenRequired;
	}

}