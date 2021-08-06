package com.red5pro.server.plugin.simpleauth.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.red5.server.adapter.StatefulScopeWrappingAdapter;
import org.red5.server.api.IContext;
import org.red5.server.api.plugin.IRed5Plugin;
import org.red5.server.api.scope.IScope;
import org.red5.server.plugin.PluginRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import com.google.gson.JsonObject;
import com.red5pro.server.plugin.simpleauth.AuthenticatorType;
import com.red5pro.server.plugin.simpleauth.SimpleAuthPlugin;
import com.red5pro.server.plugin.simpleauth.datasource.impl.roundtrip.RoundTripAuthValidator;
import com.red5pro.server.plugin.simpleauth.impl.HTTPAuthenticator;
import com.red5pro.server.plugin.simpleauth.interfaces.IAuthenticationValidator;

/**
 * Attempts authentication on a given request.
 * 
 * https://www.red5pro.com/docs/plugins/round-trip-auth/overview/
 *
 * Add to the applications web.xml
 * 
 * <pre>
	&lt;filter&gt;
	    &lt;filter-name&gt;authServlet&lt;/filter-name&gt;
	    &lt;filter-class&gt;com.red5pro.server.plugin.simpleauth.servlet.AuthServlet&lt;/filter-class&gt;
	&lt;/filter&gt;
	&lt;filter-mapping&gt;
	    &lt;filter-name&gt;authServlet&lt;/filter-name&gt;
	    &lt;url-pattern&gt;*.m4*&lt;/url-pattern&gt;
	&lt;/filter-mapping&gt;
	&lt;filter-mapping&gt;
	    &lt;filter-name&gt;authServlet&lt;/filter-name&gt;
	    &lt;url-pattern&gt;*.m3u8&lt;/url-pattern&gt;
	&lt;/filter-mapping&gt;
	&lt;filter-mapping&gt;
	    &lt;filter-name&gt;authServlet&lt;/filter-name&gt;
	    &lt;url-pattern&gt;*.ts&lt;/url-pattern&gt;
	&lt;/filter-mapping&gt;
 * </pre>
 *
 * @author Paul Gregoire
 */
public class AuthServlet implements Filter {

	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(AuthServlet.class);

	private volatile ApplicationContext appCtx;

	private SimpleAuthPlugin plugin;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		Optional<IRed5Plugin> opt = Optional.ofNullable(PluginRegistry.getPlugin(SimpleAuthPlugin.NAME));
		if (opt.isPresent()) {
			plugin = (SimpleAuthPlugin) opt.get();
		}
	}

	@Override
	public void destroy() {
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		// XXX should we check / expect any headers?

		// XXX check for user/passwd and / or just a token
		String username = null, password = null, token = "", type = "subscriber";
		// if stream name doesnt come via params get it from the url minus any extension
		// like m3u8, ts, etc
		String streamName = null;
		Iterator<String> paramNames = httpRequest.getParameterNames().asIterator();
		while (paramNames.hasNext()) {
			String paramName = paramNames.next();
			if (IAuthenticationValidator.TOKEN.equals(paramName)) {
				token = httpRequest.getParameter(paramName);
			} else if (IAuthenticationValidator.USERNAME.equals(paramName)) {
				username = httpRequest.getParameter(paramName);
			} else if (IAuthenticationValidator.PASSWORD.equals(paramName)) {
				password = httpRequest.getParameter(paramName);
			} else if ("type".equals(paramName)) {
				type = httpRequest.getParameter(paramName);
			} else if ("streamName".equals(paramName)) {
				streamName = httpRequest.getParameter(paramName);
			}
		}
		// process token and / or u:p combo
		if (token != null || (username != null && password != null)) {
			if (appCtx == null) {
				// XXX should we be looking for apps? validating that they exist?
				appCtx = (ApplicationContext) request.getServletContext()
						.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
				// if theres no context then this is not running in a red5 app
				if (appCtx == null) {
					// return an error
					httpResponse.sendError(500, "No application context found");
				}
				log.info("App: {}", appCtx.getDisplayName());
			}
			// use one level higher than MultithreadedAppAdapter since we only need the
			// scope
			StatefulScopeWrappingAdapter app = (StatefulScopeWrappingAdapter) appCtx.getBean("web.handler");
			// applications scope
			IScope appScope = app.getScope();
			IContext context = appScope.getContext();
			log.debug("Application scope: {} context: {}", appScope, context);
			// get the request uri
			String requestedURI = httpRequest.getRequestURI();
			String pathInfo = httpRequest.getPathInfo();
			log.debug("Request URI: {} Path Info: {}", requestedURI, pathInfo);
			// if pathInfo is null then set it to single slash
			if (pathInfo == null) {
				// a pathinfo will be null if theres no trailing slash or context after the
				// servlet name in the url
				pathInfo = "/";
			}
			// ensure we've got a stream name
			if (streamName == null) {

			}
			// ensure we've got a plugin reference
			if (plugin == null) {
				Optional<IRed5Plugin> opt = Optional.ofNullable(PluginRegistry.getPlugin(SimpleAuthPlugin.NAME));
				if (opt.isPresent()) {
					plugin = (SimpleAuthPlugin) opt.get();
				}
			}
			try {
				// use the http session for storage of params etc
				HttpSession session = httpRequest.getSession();
				// get the validator
				IAuthenticationValidator validator = plugin.getAuthValidator(appCtx.getDisplayName());
				if (validator instanceof RoundTripAuthValidator) {
					log.debug("Using RoundTripAuthValidator");
					// perform the validation via round-trip
					JsonObject result = ((RoundTripAuthValidator) validator).authenticateOverHttp(type, username,
							password, token, streamName);
					if (result.get("result").getAsBoolean()) {
						session.setAttribute("roletype", type);
						session.setAttribute("streamID", streamName);
						if (result.has("url")) {
							String url = result.get("url").getAsString();
							session.setAttribute("signedURL", url);
						}
						// continue down the chain
						chain.doFilter(request, response);
					}
				} else {
					log.debug("Using HTTPAuthenticator");
					Map<String, String> paramsMap = new HashMap<>();
					httpRequest.getParameterNames().asIterator().forEachRemaining((name) -> {
						paramsMap.put(name, httpRequest.getParameter(name));
					});
					log.debug("Parameters map: {}", paramsMap);
					Object[] rest = new Object[1];
					rest[0] = paramsMap;
					if (((HTTPAuthenticator) validator).authenticate(AuthenticatorType.HTTP, session, rest)) {
						session.setAttribute("roletype", type);
						session.setAttribute("streamID", streamName);
						// continue down the chain
						chain.doFilter(request, response);
					}
				}
			} catch (Exception e) {
				// return an error
				httpResponse.sendError(500, "Authentication failed");
			}
		}
		// return an error
		httpResponse.sendError(401, "Unauthorized request");
	}

}
