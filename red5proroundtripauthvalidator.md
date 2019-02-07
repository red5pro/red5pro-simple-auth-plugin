# Red5 Pro RoundTrip Authentication Validator

---

## ABOUT
The Red5Pro RoundTrip Authentication Validator implemented by the class `RoundTripAuthValidator`, is a remote server validator. As the name suggests it implements a more secure & robust authentication mechanism using the server to server validation.

The validator class seeks validation from a remote endpoint which accepts parameters from `RoundTripAuthValidator`, carries out the authentication using the intended business logic implemented and finally returns `json` response back to the validator.

This validator is best suited when only application connection level security is required and there is a need to handle `publishers` & `subscribers` separately.

---

## MECHANISM
![RoundTripAuthValidator Flow](/assets/red5proroundtripauthvalidator.png?raw=true "RoundTrip Authentication Validator Flow")

As the application starts up, the validator is initialized with the configuration parameters and validation/invalidation endpoint hooks.

The client needs to directly login into the business application server to recieve a unique secure `token`. The token can be seen as a mark of identity (as a session) that indicates that the client is `authentic` and can access services.

When a client attempts to `connect` to the application, it must provide the username, password and optional `token` parameters during the connection attempt. The simple auth checks to see if the parameters have been provided or not. If one or more params are missing the client is immediately rejected.

If credentials are provided, the validator allows connection to the client and wait for a stream action. When the client attempts to publish or subscribe to a stream, the validator determines the intent (`publish or subscribe`) and the stream name on which the action is requested. the validator sends the credentials, the stream name, the client type (`publisher/subscriber`) and the optional token parameter.

The remote server (typically a business application server), validates the credentials along with validating whether the client type has permission for the intended stream action or not. the server then returns its answer back to the validator, which then determines whether to allow or deny the stream action request based on the response data from the remote server.

The token parameter is added for additional security and is therefore optional. The passing of the parameter can be made mandatory using the validator configuration `boolean` param `clientTokenRequired`.

---

## PREREQUISITES FOR SETTING UP AUTHENTICATION
The round trip authentication consists of three components mainly. - The `simple-auth-plugin` jar, the `validator instantiation` in the webapp's `red5-web.xml` file and the `remote authentication` service.

You need to have the authentication service running for the system to work. A sample of the authentication server implementation is shared as the [mock Nodejs service](nodejs-mock-service). Once the server is available you can configure necessary endpoint params as explained in the next section.

Make sure to double check your server endpoints for errors and expected responses before you configure it on your instance.

---

### PREPARING THE REMOTE AUTHENTICATION SERVER
As mentioned earlier, you can set up your validation server in any technology as long as you keep the `endpoints` and `response json` format the same. To help you with understanding the flow and also to provide an out of the box working the same server, we have provisioned the [mock Nodejs service](nodejs-mock-service) source code in this repository. Using the mock server code you can either implement your own server or use the provided code as a starting point for your own auth server as a `nodejs` service.

The steps below explain the various components of the nodejs mock server and how to set it up to work with the `RoundTripAuthenticator`.

### Configure NodeJs
The nodejs service is available [here](#nodejs-mock-service).

This NodeJs service simulates the business application server's API. That is it has some exposed endpoints to validate and invalidate the username and password supplied. The mock service does not do any validation on the inputs that it receives. This means for example that username/passwords validity are not checked.

#### Prerequisites
Copy the folder `NodeJS mock service` into the server where you want to deploy Node.js.

You will need to install nodejs on the server:

`sudo apt-get install nodejs-legacy`

#### Configuration
Open `index.js`. In the top rows of the document locate the comment `BEGINNING OF CONFIGURATION`.
After it there will be three variables that need to be updated with your custom values.
The variables are:

* `host`: It is the host where the nodejs service is deployed. Replace "127.0.0.1" with the local IP address of the server
* `port`: It is the port that you opened for the service. Example 3000.

#### How to run
Start the server with the command

`node index.js`

If you open in a browser `http://<host>:<port>` you will get a few forms to test the API. The server's console will output the values received. The browser instead will show you the responses from the node server.

#### What to expect
The console will show you three possible outputs:

* Validate credentials: called by the webapp to verify the username/password supplied by a publisher or subscriber

        validate credentials called
        type: publisher
        username: testuser
        password: testpass
        streamID: stream1

* Invalidate credentials: called by the webapp after a publisher stops publishing. It will invalidate the credentials that the publisher used to publish.

        invalidate credentials called
        username: testuser
        password: testpass
        streamID: stream1

#### API CALLS AND RESPONSES FOR REMOTE SERVER

##### Validate Credentials

**Description**
Invoked by the `RoundTripValidator` to `validate` a client of a given type (`publisher/subscriber`) for a specified stream name.

**REQUEST**

* **ENDPOINT**: `validateCredentials`
* **METHOD**:  `POST`
* **DATA**:

```json
{
    "username": "<username>",
    "password": "<password>",
    "token": "<token",
    "type": "<type>",
    "streamID": "<stream-id>"
}
```

**RESPONSE**

* **Success**: HTTP CODE `200`
* **Data**:

```json
{
    "result": "<boolean>",
    "url": "<optional-arbitrary-url>"
}
```

** `result` contains a boolean value indicating whether client action is permitted or denied**
** `url` can be used to pass back an arbitrary URL to the authenticated client. The value fo the `url` attribute is stored on the `IConnection` object by property name `signedURL`. The parameter can be accessed by the server side code using the [getStringAttribute method](http://red5.org/javadoc/red5-server-common/org/red5/server/AttributeStore.html#getStringAttribute-java.lang.String-) on the `IConnection` object.

```java
IConnection conn = Red5.getConnectionLocal();
String url = conn.getStringAttribute("signedURL");

```

##### Invalidate Credentials

**Description**

Invoked by the `RoundTripValidator` to `invalidate` a client of a given type (`publisher/subscriber`) for a specified stream name. Invalidate can be used to revoke a user `permission` or expire a `token`.

**REQUEST**

* **ENDPOINT**: `invalidateCredentials`
* **METHOD**:  `POST`
* **DATA**:

```json
{
    "username": "<username>",
    "password": "<password>",
    "token": "<token",
    "type": "<type>",
    "streamID": "<stream-id>"
}

```

**RESPONSE**

* **Success**: HTTP CODE `200`
* **Data**:

```json
{
    "result": "<boolean>"
}
```

** `result` contains a boolean value indicating whether client action is permitted or denied**

---

### ENABLING SECURITY ON YOUR WEBAPP
To enable security on your web application, you need to add & configure the `Simple Auth Plugin` security bean along with the `validator` bean to your web application's context file - `red5-web.xml` as explained below.

---

### APPLICATION LEVEL CONFIGURATION
To attach simple auth plugin to a webapp using the `RoundTripAuthValidator` validator, you need to specify the core plugin configuration bean along with the validator bean to use for authentication, in the application's context (`red5-web.xml`) file.

__Example 1:__  Attaching plugin security to the `live` webapp using `RoundTripAuthValidator` for authentication with standard configuration settings.

**STEP 1**

To apply security to the live application, you can add the security configuration to `RED5_HOME/webapps/live/WEB-INF/red5-web.xml` as shown below :

```xml
<bean id="roundTripValidator" class="com.red5pro.server.plugin.simpleauth.datasource.impl.roundtrip.RoundTripAuthValidator" init-method="initialize">
    <property name="adapter" ref="web.handler" />
    <property name="context" ref="web.context" />
    <property name="protocol" value="${server.protocol}" />
    <property name="host" value="${server.host}" />
    <property name="port" value="${server.port}" />
    <property name="validateCredentialsEndPoint" value="${server.validateCredentialsEndPoint}"/>
    <property name="invalidateCredentialsEndPoint" value="${server.invalidateCredentialsEndPoint}"/>
    <property name="clientTokenRequired" value="true"/>
</bean>

<bean id="simpleAuthSecurity" class="com.red5pro.server.plugin.simpleauth.Configuration" >
    <property name="active" value="true" />
    <property name="rtmp" value="true" />
    <property name="rtsp" value="true" />
    <property name="rtc" value="true" />
    <property name="rtmpAllowQueryParamsEnabled" value="true" />
    <property name="allowedRtmpAgents" value="*" />
    <property name="validator" ref="roundTripValidator" />
</bean>
```

**STEP 2**

And in your webapp's red5-web.properties file, add the following section:

```
server.validateCredentialsEndPoint=/validateCredentials
server.invalidateCredentialsEndPoint=/invalidateCredentials
server.host=localhost
server.port=3000
server.protocol=http://
```

The property values are substituted from the `red5-web.properties` file into the `red5-web.xml` file at runtime.

With the following configuration applied, the validation server is assumed to be running on `localhost`, at port `3000`. The server will be expecting client validation request at `http://localhost:3000/validateCredentials` & invalidate request at `http://localhost:3000/invalidateCredentials`. The plugin configuration is set to force authentication on `RTMP`, `RTSP` and `WebRTC` connections.

---

### APPLICATION LEVEL CONFIGURATION BEAN PROPERTIES
Following parameters are allowed in a bean configuration at the application level (configured in application's red5-web.xml).

**CORE**

| Property  | Type  | Description  |    |
|---|---|---|---|
| active  | Boolean  | Sets the state of security for the application  |   |
| rtmp  | Boolean  | Sets the state of RTMP security for the application  |   |
| rtsp  | Boolean  | Sets the state of RTSP security for the application  |   |
| rtc  | Boolean  | Sets the state of WebRTC security for the application  |   |
| rtmpAllowQueryParamsEnabled  | Boolean  | Sets the state of query string based authentication for RTMP clients  |   |
| allowedRtmpAgents  | String  | Sets the list of allowed RTMP agent strings separated by semicolons. By default, all agent string is allowed.  |   |

**VALIDATOR**

| Property  | Type  | Description  |    |
|---|---|---|---|
| context  | Reference  | The reference to the `web.context` bean   |   |
| adapter  | Reference  | The reference to thr `web.handler` bean, which indicates the Application  |   |
| protocol  | String  | The remote validation server protocol (`HTTP or HTTPS`) to use  |   |
| host  | String  | The remote validation server host (`hostname or IP`)  |   |
| port  | String  | The remote validation server port (`80 or 443 or other`)  |   |
| validateCredentialsEndPoint  | String  | The remote server client `validation` endpoint `URI` relative to the server root  |   |
| invalidateCredentialsEndPoint  | String  | The remote server client `invalidation` endpoint `URI` relative to the server root  |   |
| clientTokenRequired  | Boolean  | Specifies whether `token` parameter is a `required` or `optional` param in client request   |   |

---

### CLIENT AUTHENTICATION
RTMP, RTSP and WebRTC clients must provide the required connection parameters when attempting to establish a connection with the server. The plugin will extract expected parameters and validate their presence locally first, before transmitting them to the remote server.

__Given below are some snippets, explaining how authentication can be achieved for different client types.__

---

#### Authenticating RTMP Clients
RTMP clients must pass authentication parameters (username & password) using the connection arguments in [NetConnection.connect](http://help.adobe.com/en_US/FlashPlatform/reference/actionscript/3/flash/net/NetConnection.html#connect())

__Example A__

```actionscript3
var nc:NetConnection = new NetConnection();
nc.addEventListener(NetStatusEvent.NET_STATUS, onStatus);
nc.connect("rtmp://localhost/myapp", "testuser", "testpass", "mytoken");

function onStatus(ns:NetStatusEvent):void {
   trace(ns.info.code);
}

```

> Username and password should be the first two parameters in the arguments array being sent to Red5pro.

With the `simpleauth.default.rtmp.queryparams=true` in the plugin configuration file or using the `rtmpAllowQueryParamsEnabled` property of configuration bean set to `true`, RTMP clients can also pass parameters in the query string.

__Example B__

```actionscript3
var nc:NetConnection = new NetConnection();
nc.addEventListener(NetStatusEvent.NET_STATUS, onStatus);
nc.connect("rtmp://localhost/myapp?username=testuser&password=testpass&token=mytoken");

function onStatus(ns:NetStatusEvent):void {
    trace(ns.info.code);
}

```

#### Authenticating RTSP Clients
RTSP clients (Android & IOS) must pass authentication parameters (username & password) using the `R5Configuration` object in the SDK.

__Android Example__

```java
R5Configuration config = new R5Configuration(R5StreamProtocol.RTSP,
    TestContent.GetPropertyString("host"),
    TestContent.GetPropertyInt("port"),
    TestContent.GetPropertyString("context"),
    TestContent.GetPropertyFloat("buffer_time"));

config.setParameters("username=testuser;password=testpass;token=mytoken;");
R5Connection connection = new R5Connection(config);
```

__IOS Example__

```objective-c
Swift
func getConfig()->R5Configuration{
    // Set up the configuration
    let config = R5Configuration()
    config.host = Testbed.getParameter("host") as! String
    config.port = Int32(Testbed.getParameter("port") as! Int)
    config.contextName = Testbed.getParameter("context") as! String
    config.parameters = @"username=testuser;password=testpass;token=mytoken";
    config.`protocol` = 1;
    config.buffer_time = Testbed.getParameter("buffer_time") as! Float
    return config
}
```

#### Authenticating WebRTC Clients
WebRTC clients (Using Red5pro HTML5 sdk) must pass authentication parameters using the `connectionParams` property of the `baseConfiguration` object.

__Example:__

```js
  var baseConfiguration = {
    host: window.targetHost,
    app: 'myapp',
    iceServers: iceServers,
    bandwidth: desiredBandwidth,
    connectionParams: {username: "testuser", password: "testpass", token: "mytoken"}
  };
```

---

## USE CASES

### EFFECTIVE USE CASE SCENARIOS
* When you have users holding an account on your business server.
* When you need to know who is using it. (hence the username/password parameters)
* One - many applications (1 broadcaster => N subscribers), where you need to distinguish between publishers and subscribers.
* Two-way chat where every user is both publisher and subscriber. (Not optimal but effective)

### INEFFECTIVE USE CASE SCENARIOS
* When you need anonymous usage of your application (no fixed credentials for users).

__NOTE:__ The plugin can be adapted to suit an anonymous usage scenario by filling up the `username` & `password` params with anonymous/dummy data while using the `token` as the main parameter for accessing the service.
