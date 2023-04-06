# SimpleAuth Plugin

* [ABOUT](#about)
* [PLUGIN CONFIGURATIONS](#plugin-configurations)
  * [DEFAULT CONFIGURATION](#default-configuration)
    * [PLUGIN CONFIGURATION PROPERTIES](#plugin-configuration-properties)
  * [NOTE ON RTMP AGENTS](#note-on-rtmp-agents)
* [VALIDATORS](#validators)
  * [FILE AUTHENTICATION VALIDATOR](#the-red5-pro-file-authentication-validator)
  * [ROUNDTRIP AUTHENTICATION VALIDATOR](#the-red5-pro-roundtrip-authentication-validator)
* [SPECIAL NOTE (FOR APPLICATION DEVELOPERS)](#special-note-for-application-developers)
* [ADVANCE - EXTENDING THE PLUGIN](#advance---extending-the-plugin)
* [CLUSTER SUPPORT](#cluster-support)
* [ECLIPSE](#eclipse)
* [BUILD / COMPILE](#build-compile)

---

## ABOUT
red5pro-simple-auth-plugin is a simple authentication plugin for red5pro which enables you to add simple `connection level authentication` for RTMP, RTSP and WebRTC clients.

Plugin can be configured to attach security to a webapp on-demand by adding a security configuration to the context file (red5-web.xml) of the webapp or by allowing to automatically apply security to each web application deployed on the server.

The default security configuration of the plugin authenticates connection parameters against a simple .properties file which contains username-password as property-value records. When the plugin is configured to apply security automatically to each webapp, it uses the `simple-auth-plugin.credentials` file located at RED5_HOME/conf directory. Custom security configuration at application level allows you to override this for each application.

The plugin allows you to enable/disable security for different connection types (RTMP / RTSP / RTC) individually.

---

## PLUGIN CONFIGURATIONS
The plugin can be configured via its configuration file  - __RED5_HOME/conf/simple-auth-plugin.properties__.

---

### DEFAULT CONFIGURATION
The default plugin configuration should look as shown below:

```
# Simple-auth Plugin Properties

# Default state of server wide security
simpleauth.default.active=false

# Default data source filename [ Default authentication validator is file based ]
simpleauth.default.defaultAuthValidatorDataSource=simple-auth-plugin.credentials

# Default state of rtmp security offered by the plugin
simpleauth.default.rtmp=true

# Default state of rtsp security offered by the plugin
simpleauth.default.rtsp=true

# Default state of rtc security offered by the plugin
simpleauth.default.rtc=true

# Global state of rtmp security allowing authentication via query parameters
simpleauth.default.rtmp.queryparams=true

# Allowed rtmp agents
simpleauth.default.rtmp.agents=*
```

---

#### PLUGIN CONFIGURATION PROPERTIES
| Property  |  Type  | Description  | Default value  |   |
|---|---|---|---|---|
| simpleauth.default.active  | Boolean  | Defines whether the plugin applies security to all applications by default or not.  | false  |   |
| simpleauth.default.defaultAuthValidatorDataSource  | String  | Defines the name of the default properties file (in RED5-HOME/conf) used for authentication  |  simple-auth-plugin.credentials |   |
| simpleauth.default.rtmp  | Boolean  | Defines the state of rtmp security when security is applies to all applications by default  | true  |   |
| simpleauth.default.rtsp  | Boolean  | Defines the state of rtsp security when security is applies to all applications by default  | true  |   |
| simpleauth.default.rtc  | Boolean  | Defines the state of rtc security when security is applies to all applications by default  | true  |   |
| simpleauth.default.rtmp.queryparams  | Boolean  | Defines whether rtmp authentication parameters can be provided in query string or not  |  true |   |
 simpleauth.default.rtmp.agents  | String  | Describes the list of rtmp client types (agents) allowed to connect. This value is extracted from the rtmp client's handshake.By default all client types are allowed. It can be used to block certain types fo rtmp clients from connecting. The list can be a semicolon separated list of agent streings or * | *  |   |

---

### NOTE ON RTMP AGENTS

RTMP agent string is obtained by red5 during handshake with the rtmp client. Each specific type of rtmp client provides a agent string to identify its type. This parameter is popularly addressed as the __flashVer__ string in the RTMP handshake. There are various types of RTMP clients such as flash player for windows / mac / linux, Adobe flash media live encoder (FMLE), WireCast encoder etc: Each client type can be identified by the value it provided in the __flashVer__ string. For more information checkout __[wikipedia](https://en.wikipedia.org/wiki/Real-Time_Messaging_Protocol#Connect)__

The auth module is designed to look for client's __flashVer__ in the list of permissible agent string specified. * : implies that all agent types are allowed. The check does not look for exact match, but rather whether the __flashVer__ is contained in any one of our agent strings.

---

## VALIDATORS
Validators are the core classes responsible for carrying out the authentication. While the simple auth plugin core takes care of accepting connection parameters, decoding them and checking for the basic security compliance such as mandatory params, client type etc, the validator component implements the actual authentication. Only the validator component knows the `data source` that it has to validate the credentials & other params against. The simple auth core has no knowledge of that.

By default the simple auth plugin is designed to provide a two sample validators out of the box - `Red5ProFileAuthenticationValidator` & `RoundTripAuthValidator`.Other than these default implementations, you can also write your own custom validators for your particular use case that can validate client parameters against your own data source. To know more about implemnting custom validators, check out the  - **[Extending the plugin](#advance---extending-the-plugin)** section.

In the sections below we will be taking a look at the sample validator implementations provided with the `simple auth plugin` out of the box. You can select which validator is best for your use case by reading through their descriptions.

### The Red5 Pro File Authentication Validator
![Red5ProFileAuthenticationValidator Flow](/assets/red5profileauthvalidator.png?raw=true "File Authentication Validator Flow")

The Red5Pro File Authentication Validator implemented by the class `Red5ProFileAuthenticationValidator`, is a simple filesystem validator. The class seeks its data source as a simple `.properties` file on the server's filesystem. The properties file is expected to contain `username-password` as `property-value` combinations.

The validator expects the default location of the file to be `{RED5_HOME}/conf/simple-auth-plugin.credentials`. However a different file can also be specified using the validator's configuration options as long as the format is same.

__NOTE:__ The `Red5ProFileAuthenticationValidator` component only implements `connection` level security and it does not distinguish between `subscribers` & `publishers`.

For detailed information on how to setup the `Red5 Pro File Authentication Validator` check out the [validator's documentation](red5profileauthvalidator.md).

### The Red5 Pro RoundTrip Authentication Validator
![RoundTripAuthValidator Flow](/assets/red5proroundtripauthvalidator.png?raw=true "RoundTrip Authentication Validator Flow")

The Red5Pro RoundTrip Authentication Validator implemented by the class `RoundTripAuthValidator`, is a remote server  validator. The class seeks its data source on a remote endpoint which accepts parameters from `RoundTripAuthValidator`, carries out the authentication using the intended business logic implemented and finally returns `json` response back to the validator.

__NOTE:__ The `RoundTripAuthValidator` component implements `stream` level security as it allows explicit authentication for permissions regarding `publish` & `subscribe` actions.

For detailed information on how to setup the `Red5 Pro File Authentication Validator` check out the [validator's documentation](red5proroundtripauthvalidator.md).

## SPECIAL NOTE (FOR APPLICATION DEVELOPERS)
To get this plugin to work properly with your application it is important to follow the application lifecycle. The plugin intercepts the invocation of the method - `public boolean appConnect(IConnection conn, Object[] params)`. Hence it is important that your application's main class `MultithreadedApplicationAdapter` calls the super method properly.

Your application class must make a call to the super method as shown in the snippet.

```java
@Override
public boolean appConnect(IConnection conn, Object[] params) {
    // your custom logic here
    return super.appConnect(conn, params);
}
```

> Returning a `true` or `false` directly will make your application get out of the plugin's call chain.

---

## ADVANCE - EXTENDING THE PLUGIN

### STEP 1

__Create Your Own Custom Validator__

If you with to get more out of this plugin such as authenticating against different source etc, you can implement your own validator class by implementing the `IAuthenticationValidator` interface.

__Example :__

```java
public class CustomSourceValidator implements IAuthenticationValidator {

    private static Logger logger = Red5LoggerFactory.getLogger(CustomSourceValidator.class, "CustomSourceValidator");

    private Object dataSource;

    public CustomSourceValidator() {
    }

    public CustomSourceValidator(Object dataSource) {
       this.dataSource = dataSource;
    }

    @Override
    public void initialize() {
       // load / initialize your data source object here
    }

    @Override
    public boolean onConnectAuthenticate(String username, String password, Object[] rest) {
      try {
          // authenticate here and return a true if authentication is successful, else return false
      } catch(Exception e) {
          logger.error("Error validating credentials : " + e.getMessage());
          return false;
      }
    }

    public Object getDataSource() {
       return dataSource;
    }

    public void setDataSource(Object dataSource) {
       this.dataSource = dataSource;
    }
}
```

> NOTE:

The last parameter of the method `onConnectAuthenticate`will contain connection parameters if a RTMP client passed parameters as array , otherwise the first object of the array will contain a java Map, if the RTMP client pass parameters in query string. For RTSP and RTC clients also the first object of the array will contain a java Map. You will need to iterate over the map to get the parameters and their values as key-value pairs.

### STEP 2
Before you can configure the webapp to use your custom validator, it must be deployed on the server. Package your validator classes as a `jar` and deploy the `jar` in either the webapp's lib directory (`RED5_HOME/webapps/{app}/WEB-INF/lib`), or in the server's lib directory (`RED5_HOME/lib`).

### STEP 3
Instantiate your custom validator using spring in `red5-web.xml` and pass it as a reference to the `simpleAuthSecurity` configuration bean.

```xml
<bean id="authDataValidator" class="com.example.CustomSourceValidator" init-method="initialize">
    <property name="dataSource" ref="{data-source-object}" />
</bean>

<bean id="simpleAuthSecurity" class="com.red5pro.server.plugin.simpleauth.Configuration" >
    <property name="active" value="true" />
    <property name="rtmp" value="true" />
    <property name="rtsp" value="true" />
    <property name="rtc" value="true" />
    <property name="rtmpAllowQueryParamsEnabled" value="true" />
    <property name="allowedRtmpAgents" value="*" />
    <property name="validator" ref="authDataValidator" />
</bean>
```

The plugin will now use your custom validator to validate the authentication info.

For developer convenience, a [`custom-validator-development-kit` has been included with this repository](#red5-pro-simpleauth-custom-validator-kit). The kit is primarily a `maven` project with a basic custom validator class setup. you can utilize the template to build your own custom validator, package it as a `jar` and deploy it along side the simple-auth plugin on the server, with your custom validator configuration in yourt targeted webapp.

---

## CLUSTER SUPPORT
This authentication plugin can also be used with [Red5 pro clusters](https://www.red5pro.com/docs/server/clusters.html) built manually or using [streammanager](https://www.red5pro.com/docs/autoscale/).

Clustering involves a group of interconnected nodes (origins and edges). A publisher stream is streamed from an origin to one or more edges by the Red5 pro `restreamer`. In the context of a Red5 pro ecosystem ,the restreamer itself is a connection. Hence when the simple auth plugin is turned on, the authentication restriction applies to the `restreamer` as well.

The `restreamer` consists of two types of connections (IConnection objects). First one is used to `pull` the stream from the source and the second one to publish the stream on edge. The first connection appears as an instance of `org.red5.server.net.rtmp.RTMPMinaConnection`, where as the second one is of type `com.red5pro.server.stream.restreamer.IConnectorShell`.

The `red5pro-simple-auth-plugin` checks the connection class name for each `IConnection` object during authentication and allows the `com.red5pro.server.stream.restreamer.IConnectorShell` connection to passthrough unconditionally. However the `RTMPMinaConnection` restreamer connection, needs to be authenticated explicitly.

The `RTMPMinaConnection` restreamer connection carries `cluster-restreamer-context`,  `cluster-restreamer-name` and `restreamer` connection parameters. The param `restreamer` has a value set to the cluster password. The simple auth plugin analyses and translates there connection parameters to authenticable parameters authenticates the `IConnection` against a specific data source.

To authenticate a `restreamer` connection at origin server, make sure to add the folowing credentials to your authentication datasource.

```
username : cluster-restreamer
password: <cluster-password>
```

When extending the `red5pro-simple-auth-plugin` busing a `IAuthenticationValidator` or implementing your own custom plugin that needs to work on a Red5 Pro cluster, make sure to check for the `IConnection` type. You can use the following sample snippet to check for a connection types.

```java
IConnection connection = Red5.getConnectionLocal();
if (connection instanceof RTMPMinaConnection || connection instanceof RTMPTConnection) {
    // rtmp client - [ dont forget to check connection params ]
} else if (connection instanceof IRTSPConnection) {
    // rtsp client
} else if (connection instanceof IWebRTCConnection) {
    // WebRTC client
} else if (connection instanceof IConnectorShell) {
    // pass through
} else {
    // unknown protocol
}
```

---

## ECLIPSE
* To create the Eclipse IDE files, run this from the project (app or plugin) directory: `mvn eclipse:eclipse`

* The project can be imported into eclipse IDE using :
__File -> import -> Existing Maven Projects -> {red5pro-simple-auth-plugin root folder}__

---

## BUILD / COMPILE
You may compile and build the plugin jar using the following maven command: `mvn`. To deploy `mvn clean deploy -P release` (IR5 Use only).

## PLUGINS SERVER BUILD
Modify `https://github.com/infrared5/red5pro-plugins/blob/master/remote-plugins.json` to build with a different branch for this repository (IR5 Use only).
