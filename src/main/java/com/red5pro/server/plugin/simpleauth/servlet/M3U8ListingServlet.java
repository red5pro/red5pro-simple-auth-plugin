package com.red5pro.server.plugin.simpleauth.servlet;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.adapter.StatefulScopeWrappingAdapter;
import org.red5.server.api.IContext;
import org.red5.server.api.plugin.IRed5Plugin;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IStreamFilenameGenerator;
import org.red5.server.plugin.PluginRegistry;
import org.red5.server.stream.DefaultStreamFilenameGenerator;
import org.red5.server.util.ScopeUtils;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.web.context.WebApplicationContext;

import com.google.gson.JsonObject;
import com.red5pro.media.MediaFile;
import com.red5pro.media.storage.CloudstoragePlugin;
import com.red5pro.plugin.IPluginService;
import com.red5pro.server.plugin.simpleauth.AuthenticatorType;
import com.red5pro.server.plugin.simpleauth.SimpleAuthPlugin;
import com.red5pro.server.plugin.simpleauth.datasource.impl.roundtrip.RoundTripAuthValidator;
import com.red5pro.server.plugin.simpleauth.impl.HTTPAuthenticator;
import com.red5pro.server.plugin.simpleauth.interfaces.IAuthenticationValidator;

/**
 * Returns a listing of all the m3u8 playlist files as JSON.
 *
 * Add to the applications web.xml
 *
 * <pre>
    &lt;servlet&gt;
        &lt;servlet-name&gt;playlists&lt;/servlet-name&gt;
        &lt;servlet-class&gt;com.red5pro.server.plugin.simpleauth.servlet.M3U8ListingServlet&lt;/servlet-class&gt;
    &lt;/servlet&gt;
    &lt;servlet-mapping&gt;
        &lt;servlet-name&gt;playlists&lt;/servlet-name&gt;
        &lt;url-pattern&gt;/playlists/*&lt;/url-pattern&gt;
    &lt;/servlet-mapping&gt;
 * </pre>
 *
 * <pre>
 * {"playlists":{"name":"mystream","lastModified":20283083038,"length":8202,"url":"https://bucketname.s3.amazonaws.com/vod/hls/mystream.m3u8"}}
 * </pre>
 *
 * @author Paul Gregoire
 */
public class M3U8ListingServlet extends HttpServlet {

    private static final long serialVersionUID = 7483239742960060377L;

    private static Logger log = Red5LoggerFactory.getLogger(M3U8ListingServlet.class, "red5pro");

    private static final byte[] JSON_START = "{\"playlists\":[".getBytes();

    private static final byte[] JSON_END = "]}".getBytes();

    private static final byte[] COMMA = ",".getBytes();

    private static final String JSON_ENTRY_TEMPLATE = "{\"name\":\"%s\",\"lastModified\":%d,\"length\":%d,\"url\":\"%s\"}";

    // state of service configured or not
    private static AtomicBoolean serviceConfigured = new AtomicBoolean(false);

    // service used to list buckets in S3
    private static IPluginService serviceS3;

    // service used to list buckets in Google Storage
    private static IPluginService serviceGStorage;

    // service used to list buckets in DigitalOcean Spaces
    private static IPluginService serviceDOStorage;

    // used when absolute paths are specified
    private static String streamsBaseDirectory;

    // whether or not to access the cloud
    private boolean useCloud;

    // authentication plugin
    private static SimpleAuthPlugin authPlugin;

    // whether or not to use authentication
    private boolean useAuth;

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // service lookup first and only once (if app doesnt start before the plugin
        if (serviceConfigured.compareAndSet(false, true)) {
            // look-up cloudstorage plugin
            Optional<IRed5Plugin> opt = Optional.ofNullable(PluginRegistry.getPlugin(CloudstoragePlugin.NAME));
            if (opt.isPresent()) {
                CloudstoragePlugin cloudPlugin = (CloudstoragePlugin) opt.get();
                // retrieve the s3 lister from cloudstorage plugin
                serviceS3 = cloudPlugin.getService("S3BucketLister");
                // retrieve the google storage lister from cloudstorage plugin
                serviceGStorage = cloudPlugin.getService("GStorageBucketLister");
                // retrieve the digitalocean spaces lister from cloudstorage plugin
                serviceDOStorage = cloudPlugin.getService("DOBucketLister");
                // get the streams dir
                streamsBaseDirectory = cloudPlugin.getProperty("streams.dir", "/tmp");
            }
            // look-up authentication plugin
            Optional<IRed5Plugin> optAuth = Optional.ofNullable(PluginRegistry.getPlugin(SimpleAuthPlugin.NAME));
            if (optAuth.isPresent()) {
                authPlugin = (SimpleAuthPlugin) optAuth.get();
            }
            if (authPlugin != null) {
                useAuth = Boolean.valueOf(authPlugin.getConfiguration().getProperty("simpleauth.default.http"));
            }
        }
        // Compile a list of m3u8 playlist files and minimal info (like oflaDemo) and
        // return it as JSON
        // 1. Look in the local "app" streams directory
        // 2. Look in S3 using cloudstorage plugin
        // 3. Look in Google Storage using cloudstorage plugin
        // 4. Look in DigitalOcean Space using cloudstorage plugin
        // get the application context for the app in which this servlet is running
        ApplicationContext appCtx = (ApplicationContext) getServletContext()
                .getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        // if theres no context then this is not running in a red5 app
        if (appCtx == null) {
            // return an error
            resp.sendError(500, "No application context found");
        } else {
            // use one level higher than MultithreadedAppAdapter since we only need the
            // scope
            StatefulScopeWrappingAdapter app = (StatefulScopeWrappingAdapter) appCtx.getBean("web.handler");
            // applications scope
            IScope appScope = app.getScope();
            log.debug("Application scope: {}", appScope);
            String scopeName = appScope.getName();
            // look for a custom filename gen class
            IStreamFilenameGenerator filenameGenerator = (IStreamFilenameGenerator) ScopeUtils.getScopeService(appScope,
                    IStreamFilenameGenerator.class, DefaultStreamFilenameGenerator.class);
            // determine if cloud should be accessed by checking the class name for the
            // generator
            String generatorName = filenameGenerator.getClass().getName();
            // get the request uri
            String requestedURI = req.getRequestURI();
            String pathInfo = req.getPathInfo();
            log.debug("Request URI: {} Path Info: {}", requestedURI, pathInfo);
            // if pathInfo is null then set it to single slash
            if (pathInfo == null) {
                // a pathinfo will be null if theres no trailing slash or context after the
                // servlet name in the url
                pathInfo = "/";
            }
            // do authentication check if enabled, first
            if (useAuth) {
                HttpServletRequest httpRequest = (HttpServletRequest) req;
                HttpServletResponse httpResponse = (HttpServletResponse) resp;
                // use the http session for storage of params etc, invalidate on error
                HttpSession session = httpRequest.getSession(); // XXX check for user/passwd and / or just a token
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
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Parameter - {} {}", paramName, httpRequest.getParameter(paramName));
                        }
                    }
                }
                log.info("Parameters - username: {} password: {} token: {} type: {} streamName: {}", username, password, token, type,
                        streamName);
                if (StringUtils.isNotBlank(token) || (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)
                        && !"undefined".equals(username) && !"undefined".equals(password))) {
                    try {
                        // get the validator
                        IAuthenticationValidator validator = authPlugin.getAuthValidator(scopeName);
                        if (validator instanceof RoundTripAuthValidator) {
                            log.debug("Using RoundTripAuthValidator");
                            // perform the validation via round-trip
                            JsonObject result = ((RoundTripAuthValidator) validator).authenticateOverHttp(type, username, password, token,
                                    "*"); // wildcard the stream
                            // successful result passes thru
                            if (result != null && !result.get("result").getAsBoolean()) {
                                session.invalidate();
                                // return an error
                                httpResponse.sendError(401, "Unauthorized request via RoundTripAuth");
                                return;
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
                            // successful result passes thru
                            if (!((HTTPAuthenticator) validator).authenticate(AuthenticatorType.HTTP, session, rest)) {
                                session.invalidate();
                                // return an error
                                httpResponse.sendError(401, "Unauthorized request via HTTPAuthenticator");
                                return;
                            }
                        }
                    } catch (Exception e) {
                        session.invalidate();
                        // return an error
                        httpResponse.sendError(500, "Authentication failed");
                        return;
                    }
                } else {
                    session.invalidate();
                    // return an error
                    httpResponse.sendError(412, "Precondition failed");
                    return;
                }
            }
            // set response type
            resp.setContentType("application/json");
            // get the output stream
            ServletOutputStream out = resp.getOutputStream();
            // output first part of the json
            out.write(JSON_START);
            // check if cloud or local recordings are requested
            String takeFromCloud = req.getParameter("useCloud");
            useCloud = takeFromCloud != null && takeFromCloud.equals("true");
            // use the string to prevent dependency on the cloudstorage plugin
            if (useCloud && !"com.red5pro.media.storage.s3.S3FilenameGenerator".equals(generatorName)
                    && !"com.red5pro.media.storage.gstorage.GStorageFilenameGenerator".equals(generatorName)
                    && !"com.red5pro.media.storage.digitalocean.DOFilenameGenerator".equals(generatorName)
                    && !"com.red5pro.media.storage.azure.AzureFilenameGenerator".equals(generatorName)) {
                log.debug("Cloud plugin not enabled");
                out.write(JSON_END);
                return;
            }
            log.debug("Returning recordings from {}", useCloud ? "cloud storage" : "local storage");
            // keep track of the entries written so we can comma separate the logical entry
            // sets
            int writeCount = 0;
            if (!useCloud) {
                // if theres path info, append it to the streams dir for the search
                if (!filenameGenerator.resolvesToAbsolutePath()) {
                    log.debug("Using relative path");
                    // get all the resources under streams and its possible subdirs
                    try {
                        IContext context = appScope.getContext();
                        if (log.isTraceEnabled()) {
                            log.trace("Application context resources: {}", Arrays.toString(context.getResources("*")));
                        }
                        String searchPattern = String.format("streams%s/*.m3u8", ("/".equals(pathInfo) ? "/**" : pathInfo));
                        log.debug("Search pattern: {}", searchPattern);
                        Resource[] resources = context.getResources(searchPattern);
                        log.debug("Local Resources {}", Arrays.toString(resources));
                        for (int i = 0; i < resources.length; i++) {
                            Resource res = resources[i];
                            String uri = res.getURI().toString();
                            String url = uri.substring(uri.lastIndexOf("streams/") + 8);
                            out.write(String.format(JSON_ENTRY_TEMPLATE, res.getFilename(), res.lastModified(), res.contentLength(), url)
                                    .getBytes());
                            // comma or not to comma that is the question
                            if (i < (resources.length - 1)) {
                                out.write(COMMA);
                            }
                            if (i % 10 == 0) {
                                out.flush();
                            }
                        }
                        // set the write count
                        writeCount = resources.length;
                    } catch (IOException e) {
                        log.warn("Exception building list of files", e);
                    }
                } else {
                    log.debug("Using absolute path");
                    try {
                        IContext context = appScope.getContext();
                        String searchPattern = String.format("file:%s%s/*.m3u8", streamsBaseDirectory,
                                ("/".equals(pathInfo) ? "/**" : pathInfo));
                        log.debug("Search pattern: {}", searchPattern);
                        Resource[] resources = context.getResources(searchPattern);
                        log.debug("Local Resources {}", Arrays.toString(resources));
                        for (int i = 0; i < resources.length; i++) {
                            Resource res = resources[i];
                            String uri = res.getURI().toString();
                            String url = uri.substring(uri.lastIndexOf("streams/"));
                            out.write(String.format(JSON_ENTRY_TEMPLATE, res.getFilename(), res.lastModified(), res.contentLength(), url)
                                    .getBytes());
                            // comma or not to comma that is the question
                            if (i < (resources.length - 1)) {
                                out.write(COMMA);
                            }
                            if (i % 10 == 0) {
                                out.flush();
                            }
                        }
                        // set the write count
                        writeCount = resources.length;
                    } catch (IOException e) {
                        log.warn("Exception building list of files", e);
                    }
                }
            } else {
                // s3
                if (serviceS3 != null) {
                    // null pathInfo == / (root) in most cases, but for s3 bucket root expects ""
                    String path = "/".equals(pathInfo) ? scopeName : scopeName + pathInfo;
                    List<Resource> resources = serviceS3.handleResourceRequest(path, MediaFile.TYPE_PLAYLIST);
                    log.debug("S3 Resources {}", resources);
                    // if we wrote previous entries and we have s3 playlists as well, add a comma to
                    // separate the sections
                    if (writeCount > 0 && resources.size() > 0) {
                        out.write(COMMA);
                    }
                    int length = resources.size();
                    for (int i = 0; i < length; i++) {
                        Resource res = resources.get(i);
                        out.write(
                                String.format(JSON_ENTRY_TEMPLATE, res.getFilename(), res.lastModified(), res.contentLength(), res.getURL())
                                        .getBytes());
                        // comma or not to comma that is the question
                        if (i < (length - 1)) {
                            out.write(COMMA);
                        }
                        if (i % 10 == 0) {
                            out.flush();
                        }
                    }
                }
                // gs
                if (serviceGStorage != null) {
                    // gstorage allows wildcards
                    // https://cloud.google.com/storage/docs/gsutil/addlhelp/WildcardNames
                    String path = "/".equals(pathInfo) ? scopeName : scopeName + pathInfo;
                    List<Resource> resources = serviceGStorage.handleResourceRequest(path, MediaFile.TYPE_PLAYLIST);
                    log.debug("GS Resources {}", resources);
                    // if we wrote previous entries and we have gs playlists as well, add a comma to
                    // separate the sections
                    if (writeCount > 0 && resources.size() > 0) {
                        out.write(COMMA);
                    }
                    int length = resources.size();
                    for (int i = 0; i < length; i++) {
                        Resource res = resources.get(i);
                        out.write(
                                String.format(JSON_ENTRY_TEMPLATE, res.getFilename(), res.lastModified(), res.contentLength(), res.getURL())
                                        .getBytes());
                        // comma or not to comma that is the question
                        if (i < (length - 1)) {
                            out.write(COMMA);
                        }
                        if (i % 10 == 0) {
                            out.flush();
                        }
                    }
                }
                // do
                if (serviceDOStorage != null) {
                    String path = "/".equals(pathInfo) ? scopeName : scopeName + pathInfo;
                    List<Resource> resources = serviceDOStorage.handleResourceRequest(path, MediaFile.TYPE_PLAYLIST);
                    log.debug("GS Resources {}", resources);
                    // if we wrote previous entries and we have gs playlists as well, add a comma to
                    // separate the sections
                    if (writeCount > 0 && resources.size() > 0) {
                        out.write(COMMA);
                    }
                    int length = resources.size();
                    for (int i = 0; i < length; i++) {
                        Resource res = resources.get(i);
                        out.write(
                                String.format(JSON_ENTRY_TEMPLATE, res.getFilename(), res.lastModified(), res.contentLength(), res.getURL())
                                        .getBytes());
                        // comma or not to comma that is the question
                        if (i < (length - 1)) {
                            out.write(COMMA);
                        }
                        if (i % 10 == 0) {
                            out.flush();
                        }
                    }
                }
            }
            out.write(JSON_END);
        }
    }

}
