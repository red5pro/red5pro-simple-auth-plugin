# HTTPAuthentication

Configure the application `red5-web.xml` to support `http` authentication via the `simpleAuthSecurity` bean adding `<property name="http" value="true" />`. The bean looks like so:

```xml
<bean id="simpleAuthSecurity" class="com.red5pro.server.plugin.simpleauth.Configuration" >
    <property name="active" value="true" />
    <property name="rtmp" value="true" />
    <property name="rtsp" value="true" />
    <property name="rtc" value="true" />
    <property name="http" value="true" />
    <property name="rtmpAllowQueryParamsEnabled" value="true" />
    <property name="allowedRtmpAgents" value="*" />
    <property name="validator" ref="roundTripValidator" />
</bean>
``` 

Configure the application `web.xml` to include the `AuthServlet`

```xml
<filter>
    <filter-name>authServlet</filter-name>
    <filter-class>com.red5pro.server.plugin.simpleauth.servlet.AuthServlet</filter-class>
</filter>
<filter-mapping>
    <filter-name>authServlet</filter-name>
    <url-pattern>*.m3u8</url-pattern>
</filter-mapping>
<filter-mapping>
    <filter-name>authServlet</filter-name>
    <url-pattern>*.ts</url-pattern>
</filter-mapping>
<filter-mapping>
    <filter-name>authServlet</filter-name>
    <url-pattern>*.m4*</url-pattern>
</filter-mapping>
```

**Note** Client applications which use new session requests for each file or segment, such as newer VLC apps, will not include a query string with the authentication parameters with requests beyond the first for the playlist. To prevent authentication failures after the playlist is returned to the client, the `*.ts` filter mapping has to be disabled or removed from the `web.xml`. The updated webapp config would look like so:

```xml
<filter>
    <filter-name>authServlet</filter-name>
    <filter-class>com.red5pro.server.plugin.simpleauth.servlet.AuthServlet</filter-class>
</filter>
<filter-mapping>
    <filter-name>authServlet</filter-name>
    <url-pattern>*.m3u8</url-pattern>
</filter-mapping>
```

## StreamManager

To implement auth in StreamManager, modify the `live` webapp `web.xml`, replacing the standard `M3U8ListingServlet` with this entry:

```xml
<servlet>
    <servlet-name>playlists</servlet-name>
    <servlet-class>com.red5pro.server.plugin.simpleauth.servlet.M3U8ListingServlet</servlet-class>
</servlet>
<servlet-mapping>
    <servlet-name>playlists</servlet-name>
    <url-pattern>/playlists/*</url-pattern>
</servlet-mapping>
```

This replaces the `com.red5pro.stream.transform.mpegts.server.M3U8ListingServlet` class with `com.red5pro.server.plugin.simpleauth.servlet.M3U8ListingServlet` which provides auth vs the other which allows any request.

## Quick Testing

Quick test / verification

Open VLC network link: `http://localhost:5080/live/stream1.m3u8?username=testuser&password=testpass` __replacing the variables/parameters as needed__.

