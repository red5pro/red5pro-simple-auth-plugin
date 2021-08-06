package com.red5pro.server.plugin.simpleauth.datasource.impl.jwt;

import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.DefaultJwtSignatureValidator;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.spec.SecretKeySpec;

import org.red5.server.adapter.IApplication;
import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IStreamPlaybackSecurity;
import org.red5.server.api.stream.IStreamPublishSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.red5pro.server.plugin.simpleauth.interfaces.IAuthenticationValidator;
import com.red5pro.server.plugin.simpleauth.utils.Utils;

public class JWTValidator implements IAuthenticationValidator {

	/**
	 * Logger
	 */
	private static Logger logger = LoggerFactory.getLogger(JWTValidator.class);

	/**
	 * JWS Clock skew seconds parameter name constant
	 */
	public static final int CLOCK_SKEW_SECONDS = 180;

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
	 * Property to store JWT secret or path to public key
	 */
	private String jwtSecret;

	/**
	 * Property to store JWT key to verify a token
	 */
	private Key verificationKey;

	/**
	 * Property to store JWT Signature Algorithm
	 */
	private String jwtSignatureAlgorithm;

	/**
	 * Red5 Context object. This is helpful in resolving resources on the file
	 * system
	 */
	private IContext context;

	/**
	 * Red5 application object. This gives access to the parent application
	 */
	private MultiThreadedApplicationAdapter adapter;

	public void setJwtSecret(String jwtSecret) {
		this.jwtSecret = jwtSecret;
	}

	public String getJwtSecret() {
		return jwtSecret;
	}

	public void setJwtSignatureAlgorithm(String jwtSignatureAlgorithm) {
		this.jwtSignatureAlgorithm = jwtSignatureAlgorithm;
	}

	public String getJwtSignatureAlgorithm() {
		return this.jwtSignatureAlgorithm;
	}

	@Override
	public void initialize() {
		logger.info("initialization part");
		logger.debug("adapter = {}", adapter);
		logger.debug("context = {}", context);

		verificationKey = getKey();
		logger.debug("loaded jwt verification key with algorithm = {}", verificationKey.getAlgorithm());
	}

	private Key getKey() {
		try {
			switch (jwtSignatureAlgorithm) {
				case "HS256" :
				case "HS384" :
				case "HS512" :
					SignatureAlgorithm sa = SignatureAlgorithm.forName(jwtSignatureAlgorithm);
					return new SecretKeySpec(jwtSecret.getBytes(), sa.getJcaName());
				case "RS256" :
				case "RS384" :
				case "RS512" :
					return readRSAPublicKey(new File(jwtSecret));
				case "PS256" :
				case "PS384" :
				case "PS512" :
				case "ES256" :
				case "ES384" :
				case "ES512" :
				default :
					logger.warn("Unsupported algorithm found: {}", jwtSignatureAlgorithm);
					return null;
			}
		} catch (Exception e) {
			logger.warn("", e);
		}

		return null;
	}

	private PublicKey readRSAPublicKey(File file) throws Exception {
		byte[] encoded = getBytes(file);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
		return keyFactory.generatePublic(keySpec);
	}

	private byte[] getBytes(File file) throws Exception {
		String key = new String(Files.readAllBytes(file.toPath()), Charset.defaultCharset());

		String publicKeyPEM = key.replace("-----BEGIN PUBLIC KEY-----", "").replaceAll(System.lineSeparator(), "")
				.replace("-----END PUBLIC KEY-----", "");

		return Base64.getDecoder().decode(publicKeyPEM);
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
					return isValid(token);
				} else if (username.equals("cluster-restreamer")) {
					return Utils.validateClusterReStreamer(password);
				} else {
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
				return isValid(token);
			} else if (username.equals("cluster-restreamer")) {
				return Utils.validateClusterReStreamer(password);
			} else {
				logger.error("Client 'token' is required but was not provided by connection {}.", connection);
				return false;
			}
		}

		return true;
	}

	private boolean isValid(String jwt) {
		if (verificationKey == null || jwt == null) {
			logger.warn("Found invalid verification key or jwt token");
			return false;
		}

		try {
			// the parser throws exceptions if the token is not valid
			Jwts.parser().setAllowedClockSkewSeconds(CLOCK_SKEW_SECONDS).setSigningKey(verificationKey)
					.parseClaimsJws(jwt);
			return true;
		} catch (Exception e) {
			logger.warn("", e);
		}
		return false;
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
}
