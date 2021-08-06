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
package com.red5pro.server.plugin.simpleauth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.red5.server.adapter.IApplication;
import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.IContext;
import org.red5.server.api.listeners.IScopeListener;
import org.red5.server.api.scope.IGlobalScope;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.scope.ScopeType;
import org.red5.server.plugin.Red5Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

import com.red5pro.server.plugin.simpleauth.datasource.impl.Red5ProFileAuthenticationValidator;
import com.red5pro.server.plugin.simpleauth.impl.AuthenticatorProvider;
import com.red5pro.server.plugin.simpleauth.interfaces.IAuthenticationValidator;

/**
 * This class is the entry point of the simple auth plugin module. The plugin
 * class extends the Red5Plugin to implement necessary plugin lifecycle methods.
 * 
 * The logic scans each application on the server startup to locate the
 * 
 * <pre>
 * simpleAuthSecurity
 * </pre>
 * 
 * java bean which represents the configuration object for the plugin.The plugin
 * attaches itself dynamically to each scope where the configuration is defined.
 * 
 * Application defines global properties which are overridden via the
 * 
 * <pre>
 * Configuration
 * </pre>
 * 
 * object specified in each application via. To know more about the simple auth
 * plugin see <a href=
 * "https://www.red5pro.com/docs/server/authplugin.html">Documentation</a>.
 * 
 * @author Rajdeep Rath
 * @author Paul Gregoire
 *
 */
public class SimpleAuthPlugin extends Red5Plugin {

	private Logger log = LoggerFactory.getLogger(SimpleAuthPlugin.class);

	/**
	 * Unique plugin name
	 */
	public static final String NAME = "Red5-Pro-SimpleAuth";

	/**
	 * Configuration file for global settings
	 */
	private String configurationFile = "simple-auth-plugin.properties";

	/**
	 * Configuration object for plugin
	 */
	private Properties configuration = new Properties();

	/**
	 * Configuration object for plugin representing global defaults defined in
	 * configuration file
	 */
	private Configuration defaultConfiguration;

	/**
	 * Default AuthenticatorProvider built using default settings
	 */
	private AuthenticatorProvider defaultAuthProvider;

	/**
	 * Default AuthenticatorProvider implementation
	 */
	private IAuthenticationValidator defaultAuthValidator;

	/**
	 * Authentication credentials file for default validator implementation
	 */
	private String defaultAuthValidatorDataSource = "simple-auth-plugin.credentials";

	/**
	 * Map to contain AuthenticatorProvider reference for each application scope
	 */
	private Map<String, AuthenticatorProvider> scopeAuthenticationProviders = new HashMap<>();

	/**
	 * Map to contain IApplication reference for each application scope
	 */
	private Map<String, IApplication> appHandlerDelegates = new HashMap<>();

	/**
	 * Java bean name required to identify a auth plugin configuration in an
	 * application's context file
	 */
	private static String BEAN_ID = "simpleAuthSecurity";

	/**
	 * Default global settings for plugin's enabled or disabled state
	 */
	private boolean defaultActive;

	@Override
	public String getName() {
		return SimpleAuthPlugin.NAME;
	}

	@Override
	public void doStart() throws IOException {
		try {
			log.debug("Starting Red5 Professional, Simple Auth Plugin");
			log.trace("Loading properties");
			Resource res = getConfResource(context, configurationFile);
			if (!res.exists()) {
				log.debug("Properties not found in conf, creating default configuration");
				// Build default configuration
				configuration.put("simpleauth.default.active", "false");
				configuration.put("simpleauth.default.defaultAuthValidatorDataSource", defaultAuthValidatorDataSource);
				configuration.put("simpleauth.default.rtmp", "true");
				configuration.put("simpleauth.default.rtsp", "true");
				configuration.put("simpleauth.default.rtc", "true");
				configuration.put("simpleauth.default.srt", "true");
				configuration.put("simpleauth.default.mpegts", "true");
				configuration.put("simpleauth.default.http", "true");
				configuration.put("simpleauth.default.ws", "true");
				configuration.put("simpleauth.default.rtmp.queryparams", "true");
				configuration.put("simpleauth.default.rtmp.agents", "*");
				// creates a new configuration properties file
				addConfResource(configuration, configurationFile, "SimpleAuth Properties\n");
			} else {
				if (log.isDebugEnabled()) {
					log.debug("Properties found in classpath: {}", res.getFile().getAbsolutePath());
				}
				InputStream in = res.getInputStream();
				configuration.load(in);
				in.close();
			}
			if (log.isDebugEnabled()) {
				log.debug("Properties: {}", configuration);
			}
			log.debug("Plugin configuration loaded");

			boolean defaultActive = Boolean
					.parseBoolean(configuration.getProperty("simpleauth.default.active", "false"));
			this.defaultActive = defaultActive;
			if (log.isDebugEnabled()) {
				log.debug("default active {}", defaultActive);
			}

			defaultAuthValidatorDataSource = configuration
					.getProperty("simpleauth.default.defaultAuthValidatorDataSource");
			if (log.isDebugEnabled()) {
				log.debug("defaultAuthValidatorDataSource {}", defaultAuthValidatorDataSource);
			}

			boolean rtmpSecurityEnabled = Boolean.parseBoolean(configuration.getProperty("simpleauth.default.rtmp"));
			if (log.isDebugEnabled()) {
				log.debug("rtmpSecurityEnabled {}", rtmpSecurityEnabled);
			}

			boolean rtspSecurityEnabled = Boolean.parseBoolean(configuration.getProperty("simpleauth.default.rtsp"));
			if (log.isDebugEnabled()) {
				log.debug("rtspSecurityEnabled {}", rtspSecurityEnabled);
			}

			boolean rtcSecurityEnabled = Boolean.parseBoolean(configuration.getProperty("simpleauth.default.rtc"));
			if (log.isDebugEnabled()) {
				log.debug("rtcSecurityEnabled {}", rtcSecurityEnabled);
			}

			boolean srtSecurityEnabled = Boolean.parseBoolean(configuration.getProperty("simpleauth.default.srt"));
			if (log.isDebugEnabled()) {
				log.debug("srtSecurityEnabled {}", srtSecurityEnabled);
			}

			boolean mpegtsSecurityEnabled = Boolean
					.parseBoolean(configuration.getProperty("simpleauth.default.mpegts"));
			if (log.isDebugEnabled()) {
				log.debug("mpegtsSecurityEnabled {}", mpegtsSecurityEnabled);
			}

			boolean httpSecurityEnabled = Boolean.parseBoolean(configuration.getProperty("simpleauth.default.http"));
			if (log.isDebugEnabled()) {
				log.debug("httpSecurityEnabled {}", httpSecurityEnabled);
			}

			boolean wsSecurityEnabled = Boolean.parseBoolean(configuration.getProperty("simpleauth.default.ws"));
			if (log.isDebugEnabled()) {
				log.debug("wsSecurityEnabled {}", wsSecurityEnabled);
			}

			boolean rtmpAllowQueryParams = Boolean
					.parseBoolean(configuration.getProperty("simpleauth.default.rtmp.queryparams"));
			if (log.isDebugEnabled()) {
				log.debug("rtmpAllowQueryParams {}", rtmpAllowQueryParams);
			}

			String allowedRtmpAgents = configuration.getProperty("simpleauth.default.rtmp.agents", "*");
			if (log.isDebugEnabled()) {
				log.debug("allowedRtmpAgents {}", allowedRtmpAgents);
			}
			// Prepare default validator
			Resource fileDataSource = getConfResource(context, defaultAuthValidatorDataSource);
			if (!fileDataSource.exists()) {
				Properties credentials = new Properties();
				// creates a new credentials file
				addConfResource(credentials, defaultAuthValidatorDataSource,
						"SimpleAuth Credentials\n[ Add username and password as key-value pair separated by a space (one per line) ]\nExample: testuser testpass\n");
				// once the file is created, get the resource
				fileDataSource = getConfResource(context, defaultAuthValidatorDataSource);
			}
			// default credentials file
			defaultAuthValidator = new Red5ProFileAuthenticationValidator(fileDataSource.getFile().getAbsolutePath());
			defaultAuthValidator.initialize();

			// default configuration
			defaultConfiguration = new Configuration();
			defaultConfiguration.setValidator(defaultAuthValidator);
			defaultConfiguration.setRtmp(rtmpSecurityEnabled);
			defaultConfiguration.setRtsp(rtspSecurityEnabled);
			defaultConfiguration.setRtc(rtcSecurityEnabled);
			defaultConfiguration.setSrt(srtSecurityEnabled);
			defaultConfiguration.setMpegts(mpegtsSecurityEnabled);
			defaultConfiguration.setHttp(httpSecurityEnabled);
			defaultConfiguration.setWs(wsSecurityEnabled);
			defaultConfiguration.setRtmpAllowQueryParamsEnabled(rtmpAllowQueryParams);
			defaultConfiguration.setAllowedRtmpAgents(allowedRtmpAgents);
			defaultConfiguration.setActive(defaultActive);

			// prepare default authentication provider
			defaultAuthProvider = new AuthenticatorProvider(defaultConfiguration);
			defaultAuthProvider.initialize();
			defaultAuthProvider.setEnabled(defaultConfiguration.isActive());

			// scope level auth provider lookup map
			scanScopeForAuthOverrides();
			log.debug("Simple Auth Plugin Ready");
		} catch (Throwable t) {
			log.error("Error on start", t);
		}
	}

	/**
	 * Creates a new properties file.
	 * 
	 * @param props
	 *            Properties to store
	 * @param path
	 * @param comments
	 */
	private void addConfResource(Properties props, String path, String comments) {
		// red5 server conf directory property is set by red5 bootstrap
		String confDir = System.getProperty("red5.config_root");
		Path uri = Paths.get(String.format("%s/%s", confDir, path));
		OutputStream os = null;
		try {
			os = Files.newOutputStream(uri, new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.WRITE,
					StandardOpenOption.TRUNCATE_EXISTING});
			log.debug("Creating configuration file {}", uri.toAbsolutePath());
			props.store(os, comments);
		} catch (IOException e) {
			log.warn("Exception adding conf resource: {}", uri, e);
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
				}
			}
		}
	}

	/**
	 * Looks for a specified local configuration resource.
	 * 
	 * @param context
	 * @param path
	 * @return Resource
	 */
	private Resource getConfResource(ApplicationContext context, String path) {
		Resource res = context.getResource(String.format("classpath:/conf/%s", path));
		if (!res.exists()) {
			// red5 server conf directory property is set by red5 bootstrap
			String confDir = System.getProperty("red5.config_root");
			log.debug("Conf dir: {}", confDir);
			res = context.getResource(String.format("file:%s/%s", confDir, path));
		}
		return res;
	}

	/**
	 * Scans each application on the server to look for plugin configuration bean.
	 * If configuration is found the plugin monitors the scope for connections to
	 * validate. If the plugin settings are enabled globally it will automatically
	 * attach itself to each application.
	 */
	private void scanScopeForAuthOverrides() {
		IScopeListener scopeListener = new IScopeListener() {
			@Override
			public void notifyScopeCreated(IScope scope) {
				final String scopeName = scope.getName();
				// log.info("Scope created {}", scopeName);
				if (scope.getType() == ScopeType.APPLICATION) {
					if (log.isDebugEnabled()) {
						log.debug("Scanning scope {} for custom simple auth configuration", scopeName);
					}
					IContext context = scope.getContext();
					if (context.hasBean(BEAN_ID)) {
						configureCustomContext(scope, context);
					} else {
						configureContext(scope, context);
					}
				}
			}

			@Override
			public void notifyScopeRemoved(IScope scope) {
				// log.info("Scope removed {}", scope.getName());
				if (scope.getType() == ScopeType.APPLICATION) {
					// clean up the authentication items in the give scope
					cleanUp(scope);
				}
			}

		};
		server.addListener(scopeListener);

		/**********************************************************************/

		log.debug("Setting handlers for apps that might have already started up");
		Iterator<IGlobalScope> inter = server.getGlobalScopes();
		while (inter.hasNext()) {
			IGlobalScope gscope = inter.next();
			gscope.getBasicScopeNames(ScopeType.APPLICATION).forEach(sApp -> {
				IScope issc = (IScope) gscope.getBasicScope(ScopeType.APPLICATION, sApp);
				IContext context = issc.getContext();
				if (context.hasBean(BEAN_ID)) {
					configureCustomContext(issc, context);
				} else {
					configureContext(issc, context);
				}
			});
		}
	}

	@Override
	public void doStop() throws Exception {
		log.info("Stop plugin");
	}

	/**
	 * Enable or disable a scope's authentication provider.
	 * 
	 * @param scopeName
	 *            scopes name
	 * @param enable
	 *            the enabled state
	 */
	public void enableAuthenticatorProvider(String scopeName, boolean enable) {
		AuthenticatorProvider scopeAuthProvider = scopeAuthenticationProviders.get(scopeName);
		if (scopeAuthProvider != null && scopeAuthProvider.isEnabled() != enable) {
			scopeAuthProvider.setEnabled(enable);
		}
	}

	/**
	 * Returns whether or not a given scope's authentication provider is enabled.
	 * 
	 * @param scopeName
	 *            scopes name
	 * @return true if enabled and false otherwise
	 */
	public boolean isAuthenticatorProviderEnabled(String scopeName) {
		AuthenticatorProvider scopeAuthProvider = scopeAuthenticationProviders.get(scopeName);
		if (scopeAuthProvider != null) {
			return scopeAuthProvider.isEnabled();
		}
		return false;
	}

	/**
	 * Configure a scope's authentication provider, replacing any existing entry.
	 * 
	 * @param scopeName
	 *            scopes name
	 * @param configuration
	 *            scopes configuration properties
	 * @return true if configured and false otherwise
	 */
	public boolean configureAuthenticatorProvider(String scopeName, Properties configuration) {
		// round-about way to locate our scope, but it works
		Iterator<IGlobalScope> inter = server.getGlobalScopes();
		while (inter.hasNext()) {
			IGlobalScope gscope = inter.next();
			IScope scope = (IScope) gscope.getBasicScope(ScopeType.APPLICATION, scopeName);
			if (scope != null) {
				// clean up existing entry
				cleanUp(scope);
				// configure the scope as specified
				configureScope(scope, configuration);
				// return true; break out
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the string representing the configuration file name
	 * 
	 * @return The configuration file name
	 */
	public String getConfigurationFile() {
		return configurationFile;
	}

	/**
	 * Sets the string representing the configuration file name
	 * 
	 * @param configurationFile
	 *            The file name to set
	 */
	public void setConfigurationFile(String configurationFile) {
		this.configurationFile = configurationFile;
	}

	public Properties getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Properties configuration) {
		this.configuration = configuration;
	}

	public AuthenticatorProvider getDefaultAuthProvider() {
		return defaultAuthProvider;
	}

	public void setDefaultAuthProvider(AuthenticatorProvider defaultAuthProvider) {
		this.defaultAuthProvider = defaultAuthProvider;
	}

	public IAuthenticationValidator getDefaultAuthValidator() {
		return defaultAuthValidator;
	}

	public void setDefaultAuthValidator(IAuthenticationValidator defaultAuthValidator) {
		this.defaultAuthValidator = defaultAuthValidator;
	}

	public String getDefaultAuthValidatorDataSource() {
		return defaultAuthValidatorDataSource;
	}

	public void setDefaultAuthValidatorDataSource(String defaultAuthValidatorDataSource) {
		this.defaultAuthValidatorDataSource = defaultAuthValidatorDataSource;
	}

	public Map<String, AuthenticatorProvider> getScopeAuthenticationProviders() {
		return scopeAuthenticationProviders;
	}

	public void setScopeAuthenticationProviders(Map<String, AuthenticatorProvider> scopeAuthenticationProviders) {
		this.scopeAuthenticationProviders = scopeAuthenticationProviders;
	}

	public Configuration getDefaultConfiguration() {
		return defaultConfiguration;
	}

	public void setDefaultConfiguration(Configuration defaultConfiguration) {
		this.defaultConfiguration = defaultConfiguration;
	}

	/**
	 * Returns the IAuthenticationValidator for a given app / scope name.
	 * 
	 * @param scopeName
	 *            scopes name
	 * @return IAuthenticationValidator if an AuthenticatorProvider is registered
	 *         for the scope name, otherwise null for not found
	 */
	public IAuthenticationValidator getAuthValidator(String scopeName) {
		IAuthenticationValidator authValidator = null;
		AuthenticatorProvider authProvider = scopeAuthenticationProviders.get(scopeName);
		if (authProvider != null) {
			authValidator = authProvider.getValidator();
		}
		return authValidator;
	}

	/**
	 * Returns the AuthenticatorProvider for a given app / scope name.
	 * 
	 * @param scopeName
	 *            scopes name
	 * @return AuthenticatorProvider if registered for given scope name or null if
	 *         not found
	 */
	public AuthenticatorProvider getAuthProvider(String scopeName) {
		AuthenticatorProvider authProvider = scopeAuthenticationProviders.get(scopeName);
		return authProvider;
	}

	/**
	 * Configures a scope/context without a custom configuration.
	 * 
	 * @param scope
	 * @param context
	 */
	private void configureContext(IScope scope, IContext context) {
		final String scopeName = scope.getName();
		log.debug("No custom override for scope {}", scopeName);
		final MultiThreadedApplicationAdapter adapter = (MultiThreadedApplicationAdapter) scope.getHandler();
		// Register security on app unconditionally if default.active=true
		if (adapter != null && defaultActive) {
			log.debug("Activating security automatically and registering application event handler for scope {}",
					scopeName);
			scopeAuthenticationProviders.put(scopeName, defaultAuthProvider);

			IApplication delegate = new AppEventMonitor(defaultAuthProvider, scope);
			appHandlerDelegates.put(scopeName, delegate);

			adapter.addListener(delegate);
		}
	}

	/**
	 * Configures a scope/context with a custom configuration.
	 * 
	 * @param scope
	 * @param context
	 */
	private void configureCustomContext(IScope scope, IContext context) {
		final String scopeName = scope.getName();
		log.debug("Custom override for scope {}", scopeName);
		final MultiThreadedApplicationAdapter adapter = (MultiThreadedApplicationAdapter) scope.getHandler();
		try {
			Configuration simpleAuthCustom = (Configuration) context.getBean(BEAN_ID);
			if (log.isDebugEnabled()) {
				log.debug("Custom configuration security enabled {}", simpleAuthCustom.isActive());
				log.debug("Custom configuration rtmp security {}", simpleAuthCustom.isRtmp());
				log.debug("Custom configuration rtsp security {}", simpleAuthCustom.isRtsp());
				log.debug("Custom configuration rtc security {}", simpleAuthCustom.isRtc());
				log.debug("Custom configuration srt security {}", simpleAuthCustom.isSrt());
				log.debug("Custom configuration mpegts security {}", simpleAuthCustom.isMpegts());
				log.debug("Custom configuration http security {}", simpleAuthCustom.isHttp());
				log.debug("Custom configuration ws security {}", simpleAuthCustom.isWs());
				log.debug("Custom configuration rtmp agents {}", simpleAuthCustom.getAllowedRtmpAgents());
				log.debug("Custom configuration rtmp url auth {}", simpleAuthCustom.isRtmpAllowQueryParamsEnabled());
				log.debug("Custom configuration validator {}", simpleAuthCustom.getValidator());
			}
			// App level plugin configuration
			AuthenticatorProvider scopeAuthProvider = new AuthenticatorProvider();
			scopeAuthProvider.setEnabled(simpleAuthCustom.isActive());
			// if no custom validator defined use default else use custom
			if (simpleAuthCustom.getValidator() != null) {
				scopeAuthProvider.setValidator(simpleAuthCustom.getValidator());
			} else {
				scopeAuthProvider.setValidator(defaultAuthValidator);
			}
			if (simpleAuthCustom.isRtmpUpdated()) {
				scopeAuthProvider.setSecureRTMP(simpleAuthCustom.isRtmp());
			} else {
				scopeAuthProvider.setSecureRTMP(defaultConfiguration.isRtmp());
			}
			if (simpleAuthCustom.isRtspUpdated()) {
				scopeAuthProvider.setSecureRTSP(simpleAuthCustom.isRtsp());
			} else {
				scopeAuthProvider.setSecureRTSP(defaultConfiguration.isRtsp());
			}
			if (simpleAuthCustom.isRtcUpdated()) {
				scopeAuthProvider.setSecureRTC(simpleAuthCustom.isRtc());
			} else {
				scopeAuthProvider.setSecureRTC(defaultConfiguration.isRtc());
			}
			if (simpleAuthCustom.isSrtUpdated()) {
				scopeAuthProvider.setSecureSRT(simpleAuthCustom.isSrt());
			} else {
				scopeAuthProvider.setSecureSRT(defaultConfiguration.isSrt());
			}
			if (simpleAuthCustom.isMpegtsUpdated()) {
				scopeAuthProvider.setSecureMPEGTS(simpleAuthCustom.isMpegts());
			} else {
				scopeAuthProvider.setSecureMPEGTS(defaultConfiguration.isMpegts());
			}
			if (simpleAuthCustom.isHttpUpdated()) {
				scopeAuthProvider.setSecureHTTP(simpleAuthCustom.isHttp());
			} else {
				scopeAuthProvider.setSecureHTTP(defaultConfiguration.isHttp());
			}
			if (simpleAuthCustom.isWsUpdated()) {
				scopeAuthProvider.setSecureWS(simpleAuthCustom.isWs());
			} else {
				scopeAuthProvider.setSecureWS(defaultConfiguration.isWs());
			}
			if (simpleAuthCustom.isRtmpAllowQueryParamsUpdated()) {
				scopeAuthProvider.setRtmpAcceptsQueryParamsEnabled(simpleAuthCustom.isRtmpAllowQueryParamsEnabled());
			} else {
				scopeAuthProvider
						.setRtmpAcceptsQueryParamsEnabled(defaultConfiguration.isRtmpAllowQueryParamsEnabled());
			}
			if (simpleAuthCustom.isAllowedRtmpAgentsUpdated() && simpleAuthCustom.getAllowedRtmpAgents() != null
					&& simpleAuthCustom.getAllowedRtmpAgents().length() > 2) {
				scopeAuthProvider.setAllowedRtmpAgents(simpleAuthCustom.getAllowedRtmpAgents());
			} else {
				scopeAuthProvider.setAllowedRtmpAgents(defaultConfiguration.getAllowedRtmpAgents());
			}
			// initialize the scope authenticator
			scopeAuthProvider.initialize();
			if (log.isDebugEnabled()) {
				log.debug("Scope authenticator provider configuration");
				log.debug("Scope configuration security enabled {}", scopeAuthProvider.isEnabled());
				log.debug("Scope configuration rtmp security {}", scopeAuthProvider.isSecureRTMP());
				log.debug("Scope configuration rtsp security {}", scopeAuthProvider.isSecureRTSP());
				log.debug("Scope configuration rtc security {}", scopeAuthProvider.isSecureRTC());
				log.debug("Scope configuration srt security {}", scopeAuthProvider.isSecureSRT());
				log.debug("Scope configuration mpegts security {}", scopeAuthProvider.isSecureMPEGTS());
				log.debug("Scope configuration http security {}", scopeAuthProvider.isSecureHTTP());
				log.debug("Scope configuration ws security {}", scopeAuthProvider.isSecureWS());
				log.debug("Scope configuration rtmp agents {}", scopeAuthProvider.getAllowedRtmpAgents());
				log.debug("Scope configuration rtmp url auth {}", scopeAuthProvider.isRtmpAcceptsQueryParamsEnabled());
				log.debug("Scope configuration validator {}", scopeAuthProvider.getValidator());
			}
			// Register the custom auth provider configuration
			if (adapter != null) {
				log.debug("Registering application event handler for {}", scopeName);
				scopeAuthenticationProviders.put(scopeName, scopeAuthProvider);

				IApplication delegate = new AppEventMonitor(scopeAuthProvider, scope);
				appHandlerDelegates.put(scopeName, delegate);

				adapter.addListener(delegate);
			}
		} catch (Exception e) {
			log.error("Error reading configuration override from {}", scopeName, e);
		}
	}

	/**
	 * Configures the specified scope with the supplied configuration.
	 * 
	 * @param scope
	 * @param configuration
	 */
	private void configureScope(IScope scope, Properties configuration) {
		final String scopeName = scope.getName();
		log.debug("Configure scope {} with {}", scopeName, configuration);
		// get the app context so we can write the configuration
		ApplicationContext context = scope.getContext().getApplicationContext();
		// from here on, we're affecting a app / context, not the plugin context
		boolean active = Boolean.parseBoolean(configuration.getProperty("simpleauth.default.active"));
		log.debug("active {}", active);
		String authValidatorDataSource = configuration.getProperty("simpleauth.default.defaultAuthValidatorDataSource");
		// if its null, use the plugins default
		if (authValidatorDataSource == null) {
			authValidatorDataSource = defaultAuthValidatorDataSource;
		}
		log.debug("authValidatorDataSource {}", authValidatorDataSource);
		boolean rtmpSecurityEnabled = Boolean.parseBoolean(configuration.getProperty("simpleauth.default.rtmp"));
		log.debug("rtmpSecurityEnabled {}", rtmpSecurityEnabled);
		boolean rtspSecurityEnabled = Boolean.parseBoolean(configuration.getProperty("simpleauth.default.rtsp"));
		log.debug("rtspSecurityEnabled {}", rtspSecurityEnabled);
		boolean rtcSecurityEnabled = Boolean.parseBoolean(configuration.getProperty("simpleauth.default.rtc"));
		log.debug("rtcSecurityEnabled {}", rtcSecurityEnabled);
		boolean srtSecurityEnabled = Boolean.parseBoolean(configuration.getProperty("simpleauth.default.srt"));
		log.debug("srtSecurityEnabled {}", srtSecurityEnabled);
		boolean mpegtsSecurityEnabled = Boolean.parseBoolean(configuration.getProperty("simpleauth.default.mpegts"));
		log.debug("mpegtsSecurityEnabled {}", mpegtsSecurityEnabled);
		boolean httpSecurityEnabled = Boolean.parseBoolean(configuration.getProperty("simpleauth.default.http"));
		log.debug("httpSecurityEnabled {}", httpSecurityEnabled);
		boolean wsSecurityEnabled = Boolean.parseBoolean(configuration.getProperty("simpleauth.default.ws"));
		log.debug("wsSecurityEnabled {}", wsSecurityEnabled);
		boolean rtmpAllowQueryParams = Boolean
				.parseBoolean(configuration.getProperty("simpleauth.default.rtmp.queryparams"));
		log.debug("rtmpAllowQueryParams {}", rtmpAllowQueryParams);
		String allowedRtmpAgents = configuration.getProperty("simpleauth.default.rtmp.agents", "*");
		log.debug("allowedRtmpAgents {}", allowedRtmpAgents);
		// Prepare the scope validator
		Resource fileDataSource = getConfResource(context, authValidatorDataSource);
		if (!fileDataSource.exists()) {
			Properties credentials = new Properties();
			// creates a new credentials file
			addConfResource(credentials, authValidatorDataSource,
					"SimpleAuth Credentials\n[ Add username and password as key-value pair separated by a space (one per line) ]\nExample: testuser testpass\n");
			// once the file is created, get the resource
			fileDataSource = getConfResource(context, authValidatorDataSource);
		}
		IAuthenticationValidator authValidator;
		try {
			authValidator = new Red5ProFileAuthenticationValidator(fileDataSource.getFile().getAbsolutePath());
			authValidator.initialize();
			// default configuration
			Configuration scopeConfiguration = new Configuration();
			scopeConfiguration.setValidator(authValidator);
			scopeConfiguration.setRtmp(rtmpSecurityEnabled);
			scopeConfiguration.setRtsp(rtspSecurityEnabled);
			scopeConfiguration.setRtc(rtcSecurityEnabled);
			scopeConfiguration.setSrt(srtSecurityEnabled);
			scopeConfiguration.setMpegts(mpegtsSecurityEnabled);
			scopeConfiguration.setHttp(httpSecurityEnabled);
			scopeConfiguration.setWs(wsSecurityEnabled);
			scopeConfiguration.setRtmpAllowQueryParamsEnabled(rtmpAllowQueryParams);
			scopeConfiguration.setAllowedRtmpAgents(allowedRtmpAgents);
			scopeConfiguration.setActive(active);
			// prepare default authentication provider
			AuthenticatorProvider scopeAuthProvider = new AuthenticatorProvider(scopeConfiguration);
			scopeAuthProvider.initialize();
			scopeAuthProvider.setEnabled(scopeConfiguration.isActive());
			// Register the custom auth provider configuration
			MultiThreadedApplicationAdapter adapter = (MultiThreadedApplicationAdapter) scope.getHandler();
			if (adapter != null) {
				log.debug("Registering application event handler for {}", scopeName);
				scopeAuthenticationProviders.put(scopeName, scopeAuthProvider);

				IApplication delegate = new AppEventMonitor(scopeAuthProvider, scope);
				appHandlerDelegates.put(scopeName, delegate);

				adapter.addListener(delegate);
			}
		} catch (IOException e) {
			log.warn("Authentication configuration of {} failed", scopeName, e);
		}
	}

	/**
	 * Cleans up a scope; removing delegate, provider, etc...
	 * 
	 * @param scope
	 */
	private void cleanUp(IScope scope) {
		final String scopeName = scope.getName();
		log.debug("Cleaning up scope {}", scopeName);
		// Deregister AuthenticatorProvider
		if (scopeAuthenticationProviders.containsKey(scopeName)) {
			@SuppressWarnings("unused")
			AuthenticatorProvider provider = scopeAuthenticationProviders.remove(scopeName);
			provider = null;
		}
		// Deregister IApplication delegate
		if (appHandlerDelegates.containsKey(scopeName)) {
			MultiThreadedApplicationAdapter adapter = (MultiThreadedApplicationAdapter) scope.getHandler();
			if (adapter != null) {
				IApplication delegate = appHandlerDelegates.remove(scopeName);
				adapter.removeListener(delegate);
				delegate = null;
			}
		}
	}

}
