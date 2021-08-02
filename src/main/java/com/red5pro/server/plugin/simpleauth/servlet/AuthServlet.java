package com.red5pro.server.plugin.simpleauth.servlet;

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

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

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
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
		String user = null, passwd = null, token = null;
		Iterator<String> paramNames = httpRequest.getParameterNames().asIterator();
		while (paramNames.hasNext()) {
			String paramName = paramNames.next();
			if ("token".equals(paramName)) {
				token = httpRequest.getParameter(paramName);
			} else if ("user".equals(paramName)) {
				user = httpRequest.getParameter(paramName);
			} else if ("passwd".equals(paramName)) {
				passwd = httpRequest.getParameter(paramName);
			}
		}
		// process token and / or u:p combo
		if (token != null || (user != null && passwd != null)) {

			if (appCtx == null) {
				// XXX should we be looking for apps? validating that they exist?
				appCtx = (ApplicationContext) request.getServletContext()
						.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
				// if theres no context then this is not running in a red5 app
				if (appCtx == null) {
					// return an error
					httpResponse.sendError(500, "No application context found");
				}
			}

			chain.doFilter(request, response);
		} else {
			httpResponse.sendError(401, "Unauthorized request");
		}
	}

}
