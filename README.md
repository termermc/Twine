# Twine
A fully featured application server built for boilerplate-less Vert.x web applications 

## Getting Twine
To obtain a pre-compiled release of Twine, check the [releases](https://github.com/termermc/twine/releases) tab. To compile, download the source from the releases tab or clone the repository for the latest development version, then follow the **Building** guide. 

## Building
Before building, be sure you have at least JDK 8 and Gradle installed.
To build on Unix/Unix-like systems (Darwin, Linux, BSD) run `./gradlew shadowJar` and on Windows run `gradlew.bat shadowJar`.
The compiled executable will be in `build/libs/` as `twine-all.jar`.

## Getting Started
To start Twine, execute the jar with the `-s` option. This will generate all necessary configuration files and start the server. Once you start the server for the first time, terminate it and then observe the directory you executed in. Now you may proceed to the **Configuration** guide.

## A Note About Starting Twine
When starting Twine with `-s`, Twine will automatically create a new process with the proper JVM options to load dependencies from the `dependencies/` directory, and load module classes from the `modules/` directory. To avoid creating a new process, start the server using either `start.sh` or `start.bat` (found in this repository, or in the releases tab).

## Configuration
Before configuring, you must first locate the files you must configure. By this point, Twine will have generated the following configuring files:
 - twine.yml
 - domains.yml
 - cluster.yml

`twine.yml` is, unsurprisingly, Twine's main configuration file, and is where all the real server settings are configured. Let's take a look at the [default twine.yml configuration](https://github.com/termermc/Twine/blob/master/src/main/resources/resources/twine.yml). Since most fields are annotated with comments explaining their usages, we're not going to over all of them from here, but a few do need some additional explanation.

The `keystore` and `keystorePassword` fields are for enabling SSL (HTTPS). `keystore` is for the path of your `.jks` keystore file, and `keystorePassword` is for the keystore's password. See the **Enabling SSL** guide for more info.

The `scripting` and `scriptExceptions` fields are for managing embedded Java scripting with Beanshell. To enable scripting, set `scripting` to true. If any exceptions occur and you want to append the error to the webpage output, set `scriptExceptions` to true. See **Embedded Java Scripting** for more details on scripting.

`domains.yml` is the domain configuration file for Twine which is used for manipulating settings for domains and how they behave. As is with all Twine configuration files, the [default domain.yml configuration](https://github.com/termermc/Twine/blob/master/src/main/resources/resources/domains.yml) is mostly self-describing and can be easily understood from reading the comments. The first Yaml document in this file contains one field, called `defaultDomain`. This field specifies which domain should be served by default if the server is accessed from a domain without a configuration assigned to it. Take a look at the next few documents and you should get a pretty good idea of how to create new domain configurations.

`cluster.yml` is the cluster configuration file for Twine. It won't be used for anything unless `clusterEnable` is set to true in `twine.yml`, so unless you're doing any clustering, don't worry about it. For more on clustering, see the **Clustering** guide.

## Enabling SSL
To enable SSL/HTTPS on your server, you'll need a JKS formatted keystore and a few adjustments to your `twine.yml` file. To generate a JKS keystore, you will need a valid certificate from an organization like LetsEncrypt. Once you have a certificate file, you'll need to package it into a `.jks` keystore with either Java's Keytool, or Keytool-Explorer (a GUI for keytool).
Once you've got a JKS format keystore, all you'll need to do is point your `keystore` field in `twine.yml` to the path of your JKS keystore, and set `keystorePassword` to the keystore's password.

## Adding Modules
To deploy a module jar, simply add it to the `modules/` directory, and start the server. Any dependencies the module has must be placed in the `dependencies/` directory.

## Embedded Java Scripting
A slightly less orthodox feature in Twine is the ability to script in a Java-like language in a sort of PHP fashion. To enable scripting, set `script` to `true` in `twine.yml`. To enable scripting on an HTML file, begin it with `<!--TES-->` (TES stands for Twine Embedded Scripting). To use Java in the file, use `<?java [java/beanshell code] ?>`. Anything inside of the `<?java` and `?>` tags will be executed, and any output will be insert inline.

Scripts run inside of HTML documents have access to a variable called `out`. More info on `out` can be found [here](https://termer.net/javadoc/twine/1.0/net/termer/twine/documents/Documents.Out.html). The other variables are domain (an instance of the [Domain](https://termer.net/javadoc/twine/1.0/net/termer/twine/utils/Domains.Domain.html) representing the domain the page was accessed from), name (the name of the document), request (the Vert.x HttpServerRequest for the request), response (the Vert.x HttpServerResponse for the request), and route (the Vert.x RoutingContext for the request).

Below is an example of a document that prints the path of the request:
```java
<!--TES-->
<h1>TES Example</h1>
<p>Path: <?java
	// Output path
	out.append(request.path(), true) // Specify `true` to sanitize output
?></p>
```

## Clustering
Sometimes you'll want to cluster several Twine instances, be it between servers, or on the same machine for various reasons. To cluster instances, you'll need an [Apache Zookeeper](https://zookeeper.apache.org/) server, and to set `clusterEnable` to `true` in `twine.yml`. In `cluster.yml`, add the Zookeeper host to `clusterHosts` (comma separated) and adjust the rest of the file to your liking.

## Module Development
See `DEVELOPMENT.md`.
