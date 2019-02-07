# SimpleAuth Plugin - Custom Validator Development Template

* [ABOUT](#about)
* [INSTALL DEPENDENCY](#install-dependency)
* [IMPORT INTO ECLIPSE](#import-into-eclipse)
* [BUILD](#build)
* [INSTALL / ACTIVATE](#install-and-activate)
* [TIPS](#tips)

---

## ABOUT
This document is to supplement the simple auth plugin custom validator development maven template, which can be used as a quickstart point for your own custom validator for the Simple auth plugin.

Information on Simple auth funtionality and behaviour is already [documented on red5pro.com](https://www.red5pro.com/docs/server/authplugin.html). It is recommended that you give it a read before creating your own validator.

This document follows up with the process of using the maven template to build, compile and deploy your own custom validator.

---

## SimpleAuth DEPENDENCY

### Maven
Add the red5pro repository to your maven `settings.xml` file to allow it to resolve dependency. Full settings file example below:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.1.0 http://maven.apache.org/xsd/settings-1.1.0.xsd" xmlns="http://maven.apache.org/SETTINGS/1.1.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <servers>
    <server>
      <username>${security.getCurrentUsername()}</username>
      <password>${security.getEscapedEncryptedPassword()!"*** Insert encrypted password here ***"}</password>
      <id>central</id>
    </server>
    <server>
      <username>${security.getCurrentUsername()}</username>
      <password>${security.getEscapedEncryptedPassword()!"*** Insert encrypted password here ***"}</password>
      <id>snapshots</id>
    </server>
  </servers>
  <profiles>
    <profile>
      <repositories>
        <repository>
          <snapshots>
            <enabled>false</enabled>
          </snapshots>
          <id>central</id>
          <name>libs-release</name>
          <url>https://red5pro.jfrog.io/red5pro/libs-release</url>
        </repository>
        <repository>
          <snapshots />
          <id>snapshots</id>
          <name>libs-snapshot</name>
          <url>https://red5pro.jfrog.io/red5pro/libs-snapshot</url>
        </repository>
      </repositories>
      <pluginRepositories>
        <pluginRepository>
          <snapshots>
            <enabled>false</enabled>
          </snapshots>
          <id>central</id>
          <name>plugins-release</name>
          <url>https://red5pro.jfrog.io/red5pro/plugins-release</url>
        </pluginRepository>
        <pluginRepository>
          <snapshots />
          <id>snapshots</id>
          <name>plugins-release</name>
          <url>https://red5pro.jfrog.io/red5pro/plugins-release</url>
        </pluginRepository>
      </pluginRepositories>
      <id>artifactory</id>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>artifactory</activeProfile>
  </activeProfiles>
</settings>
```

### Local install `simple-auth-plugin` dependency
1. Download the latest Red5 Pro server (it includes the simple-auth plugin)
2. Locate the plugin jar at: `{RED5_HOME}/plugins/red5pro-simple-auth-{version}-RELEASE.jar`
3. Install the jar in your maven repo using the following command:

```sh
mvn install:install-file "-Dfile=<path-to-simple-auth-plugin-jar>" "-DgroupId=com.red5pro" "-DartifactId=red5pro-simple-auth" "-Dversion=<simple-auth-plugin-version>" "-Dpackaging=jar"
```

**Example:**

```sh
mvn install:install-file "-Dfile=C:\Users\rajde\Documents\GitHub\red5pro-simple-auth-plugin\target\red5pro-simple-auth-3.4.4.jar" "-DgroupId=com.red5pro" "-DartifactId=red5pro-simple-auth" "-Dversion=3.4.4" "-Dpackaging=jar"
```

Once the dependency has been installed update the pom.xml to use the dependency

Update the pom.xml of the `custom-validator` project with the proper dependency to match the installed jar information:

```xml
<dependency>
    <groupId>com.red5pro</groupId>
    <artifactId>red5pro-simple-auth</artifactId>
    <version>5.6.5901</version>
</dependency>
```


> Make sure the version used in the pom matches the version that was installed as a dependency.

---

## IMPORT INTO ECLIPSE

* To create the Eclipse IDE files, run this from the project directory: `mvn eclipse:eclipse`

> The above project generates files needed by eclipse to import the project.

* The project can be imported into eclipse IDE using :

__File -> import -> Existing Maven Projects -> {custom-validator root folder}__

> JEE versions of eclipse (luna, mars, neon etc :) are needed to work with Red5 Pro projects.

---

## BUILD

You may compile and build the plugin jar using the following maven command: `mvn`

> The above command will generate the plugin jar in the project's `target` directory.

---

## INSTALL AND ACTIVATE

### Installing
To install the custom validator to work with your web application, copy the built jar - `custom-validator-1.0.0.jar` into the webapp's lib directory. Example: `{RED5_HOME}/webapps/live/WEB-INF/lib`. If the `lib` directory does not exist, you may need to create it.

This will ensure that the necessary classes are loaded as soon as Red5 Pro starts.

### Activating
To activate the custom validator, instantiate it in the web application's context file, ie: `red5-web.xml` via a java bean along side the simple auth plugin configuration bean.

**Example:**

```xml
<bean id="customValidator" class="com.red5pro.server.plugin.simpleauth.extension.sample.CustomAuthValidator" init-method="initialize">
      <property name="adapter" ref="web.handler" />
      <property name="context" ref="web.context" />
      <!-- <property name="myproperty" value="myvalue" /> -->
</bean>

<bean id="simpleAuthSecurity" class="com.red5pro.server.plugin.simpleauth.Configuration" >
      <property name="active" value="true" />
      <property name="rtmp" value="true" />
      <property name="rtsp" value="true" />
      <property name="rtc" value="true" />
      <property name="rtmpAllowQueryParamsEnabled" value="true" />
      <property name="allowedRtmpAgents" value="*" />
      <property name="validator" ref="customValidator" />
</bean>
```

## TIPS
* You can create your own new properties for the CustomAuthValidator class with getter and setter methods and wire them up by adding the property name and value in the `customValidator` bean.
* The custom-validator by `default` implements connection level authentication. To use intercept publish or playback events, make sure to return `true`, thereby allowing connection unconditionally.The `publish` and `playback` events succeed the `connect` event.
* To access connection parameters in `publish` or `playback` interceptors, you should extract parameters from connection object on `connect` and store them as attributes on the `IConnection` object.
