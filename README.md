# Twine
A fully featured application server built for boilerplate-less Vert.x web applications

![Twine logo](https://raw.githubusercontent.com/termermc/Twine/master/src/main/resources/logo_small.png)

## Getting Twine
To obtain a pre-compiled release of Twine, check the [releases](https://github.com/termermc/twine/releases) tab. To compile, download the source from the releases tab or clone the repository for the latest development version, then follow the **Building** guide. 

## Building
Before building, be sure you have at least JDK 8 and Gradle installed.
To build on Unix/Unix-like systems (Darwin, Linux, BSD) run `./gradlew build` and on Windows run `gradlew.bat build`.
The compiled executable will be in `build/libs/` as `twine-2.2-all.jar`.

## Getting Started
To start Twine, execute the jar with the `-s` option. This will generate all necessary configuration files and start the server. Once you start the server for the first time, terminate it and then observe the directory you executed in. Now you may proceed to the **Configuration** guide.

## A Note About Starting Twine
When starting Twine with `-s`, Twine will automatically create a new process with the proper JVM options to load dependencies from the `dependencies/` directory, and load module classes from the `modules/` directory. To avoid creating a new process, start the server using either `start.sh` or `start.bat` (found in this repository, or in the releases tab).

## Configuration
All configuration for Twine is done through a file named `twine.yml`.
The file itself is documented, and as such there isn't very much to go over here.
Opening the file will explain it in depth.

If you need to programmatically configure Twine, you can modify `twine.yml` options through command line options and environment variables.

To configure with command line options, you can specify values with the `--config=NODE:VALUE` option. So to set the port to 8080, you could specify `--config=server.port:8080`.
Multiple `--config` options can be used to configure as many values as you want.

To configure with environment variables, you must create variables beginning with `TW_CONF_`, followed by the config node. So to set the port to 8080 using environment variables, you can have a variable with the following value:
`TW_CONF_server_port=8080`. These variables are case-sensitive, so keep that in mind when configuring with them.
Additionally, `_` is equivalent to `.` in nodes specified with environment variables, making `server_port` equivalent to `server.port`.

The priority for `twine.yml` configurations is as follows: command line options > environment variables > `twine.yml` file.

## Enabling HTTPS
To enable HTTPS on your server, you'll need a JKS formatted keystore and a few adjustments to your `twine.yml` file. To generate a JKS keystore, you will need a valid certificate from an organization like LetsEncrypt. Once you have a certificate file, you'll need to package it into a `.jks` keystore with either Java's Keytool, or Keytool-Explorer (a GUI for keytool).

Once you've got a JKS format keystore, you'll need to set `server.https.enable` to `true`, set `server.https.keystore` to the path of your JKS keystore, and set `server.https.keystorePassword` to the keystore's password.

If you would like to redirect insecure HTTP traffic to the HTTPS version of your server, you can set that up by setting `server.https.redirect.enable` to true, and `server.https.redirect.port` to the port you would like the redirection server to run on. By default it is set to `80`.

## Adding Modules
To deploy a module jar, simply add it to the `modules/` directory, and start the server.

Module dependencies may be placed in the `dependencies/` directory.
This can be useful for sharing dependency jars between multiple modules, but is not necessary if module jars self-contain all of their dependencies.

It is recommended for modules to use dependency shading to avoid conflicts. 

## Clustering
Sometimes you'll want to cluster several Twine instances, be it between servers, or on the same machine for various purposes such as data sharing and EventBus communication.

To cluster instances, you'll need an [Apache Zookeeper](https://zookeeper.apache.org/) server.

Enabling clustering is done by setting `vertx.cluster.enable` to `true` in `twine.yml`.
From there, you need to configure the rest of the settings in `vertx.cluster` to match settings for your Zookeeper server.

## Running Twine Behind A Reverse Proxy
If you are running Twine behind a reverse proxy such as Nginx, you should make sure `server.respectXFF` is set to `true` in `twine.yml`, and that `X-Forwarded-For` and `Host` headers are being sent by the reverse proxy. 

## Module Development
See `DEVELOPMENT.md`.
