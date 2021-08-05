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
package com.red5pro.server.plugin.simpleauth.datasource.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import org.red5.server.api.IContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.red5pro.server.plugin.simpleauth.interfaces.IAuthenticationValidator;

/**
 * This class is a sample implementation of the
 * 
 * <pre>
 * IAuthenticationValidator
 * </pre>
 * 
 * interface. It is meant to serve as a default placeholder and an example for a
 * 
 * <pre>
 * IAuthenticationValidator
 * </pre>
 * 
 * implementation. The implementation has access to your data source which
 * provides a means to validate credentials and other parameters.
 * 
 * This sample implementation uses filesystem as a data source for validating
 * username/password passed by clients.
 * 
 * @author Rajdeep Rath
 *
 */
public class Red5ProFileAuthenticationValidator implements IAuthenticationValidator {

	/**
	 * Logger
	 */
	private static Logger logger = LoggerFactory.getLogger(Red5ProFileAuthenticationValidator.class);

	/**
	 * Red5 Context object. This is helpful in resolving resources on the file
	 * system
	 */
	private IContext context;

	/**
	 * Properties object intended to load the credentials information from
	 * filesystem
	 */
	private Properties authInformation;

	/**
	 * The name or absolute of the properties file resource which contains
	 * credentials to validate against
	 */
	private String dataSource;

	/**
	 * Constructor for Red5ProFileAuthenticationValidator
	 */
	public Red5ProFileAuthenticationValidator() {

	}

	/**
	 * Constructor for Red5ProFileAuthenticationValidator
	 * 
	 * @param dataSource
	 *            The absolute path of the properties file resource
	 */
	public Red5ProFileAuthenticationValidator(String dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Constructor for Red5ProFileAuthenticationValidator.
	 * <p>
	 * When the context is not provided the file lookup uses absolute path pattern.
	 * In presence of the context object, the lookup is relative to the application.
	 * </p>
	 * 
	 * @param context
	 *            The red5 context object.
	 * @param dataSource
	 *            The name of the properties file resource
	 */
	public Red5ProFileAuthenticationValidator(IContext context, String dataSource) {
		this.context = context;
		this.dataSource = dataSource;
	}

	@Override
	public void initialize() {
		loadDataSource();
	}

	/**
	 * Loads the content of the resource containing authentication information into
	 * memory
	 */
	private void loadDataSource() {
		try {
			logger.debug("Loading data source");

			File propertiesFile = null;

			if (context != null)
				propertiesFile = context.getApplicationContext().getResource(dataSource).getFile();
			else
				propertiesFile = new File(dataSource);

			if (!propertiesFile.exists())
				throw new IOException("Datasource not found");

			authInformation = new Properties();
			authInformation.load(new FileInputStream(propertiesFile));

			Enumeration<?> e = authInformation.propertyNames();
			while (e.hasMoreElements()) {
				String key = (String) e.nextElement();
				String value = authInformation.getProperty(key);
				logger.debug("Key : " + key + ", Value : " + value);
			}

		} catch (IOException e) {
			e.printStackTrace();
			logger.error("Error initializing data source " + e.getMessage());
		}
	}

	@Override
	public boolean onConnectAuthenticate(String username, String password, Object[] rest) {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Authenticating connection for username " + username + " and password " + password);
			}

			if (authInformation.containsKey(username)) {
				String pass = String.valueOf(authInformation.getProperty(username));
				if (pass.equalsIgnoreCase(password)) {
					return true;
				}
			}
			return false;
		} catch (Exception e) {
			logger.error("Error reading credentials : " + e.getMessage());
			return false;
		}
	}

	/**
	 * Returns the value of
	 * 
	 * <pre>
	 * dataSource
	 * </pre>
	 * 
	 * @return The string representing the datasource
	 */
	public String getDataSource() {
		return dataSource;
	}

	/**
	 * Sets the value for
	 * 
	 * <pre>
	 * dataSource
	 * </pre>
	 * 
	 * @param dataSource
	 *            The string to set as the datasource
	 */
	public void setDataSource(String dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Returns the value of
	 * 
	 * <pre>
	 * authInformation
	 * </pre>
	 * 
	 * @return The Properties object containing authentication information
	 */
	public Properties getAuthInformation() {
		return authInformation;
	}

	/**
	 * Sets the value of
	 * 
	 * <pre>
	 * authInformation
	 * </pre>
	 * 
	 * @param authInformation
	 *            The Properties object to set
	 */
	public void setAuthInformation(Properties authInformation) {
		this.authInformation = authInformation;
	}

	/**
	 * Returns the value of
	 * 
	 * <pre>
	 * context
	 * </pre>
	 * 
	 * @return The IContext object
	 */
	public IContext getContext() {
		return context;
	}

	/**
	 * Sets the value of
	 * 
	 * <pre>
	 * context
	 * </pre>
	 * 
	 * @param context
	 *            The IContext to return
	 */
	public void setContext(IContext context) {
		this.context = context;
	}

}
