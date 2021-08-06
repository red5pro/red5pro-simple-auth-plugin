
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
    <url-pattern>*.m4*</url-pattern>
</filter-mapping>
<filter-mapping>
    <filter-name>authServlet</filter-name>
    <url-pattern>*.m3u8</url-pattern>
</filter-mapping>
<filter-mapping>
    <filter-name>authServlet</filter-name>
    <url-pattern>*.ts</url-pattern>
</filter-mapping>
```

Quick test / verification

Open VLC network link: `http://localhost:5080/live/stream1.m3u8?username=testuser&password=testpass` __replacing the variables/parameters as needed__.

