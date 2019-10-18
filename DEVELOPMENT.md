# Module Development
Twine's main feature is the ability to deploy modules on an instance. Modules are packages written in Java (or any other JVM language) that have the ability to use Twine and Vert.x's functionality. To create a new module, you'll need to create a new Java project and include the Twine jar in the classpath. Once you've included it, create a new class called `Module` that implements the interface [TwineModule](https://termer.net/javadoc/twine/1.0-alpha/net/termer/twine/modules/TwineModule.html).

## Javadoc
The Twine Javadoc for module development is located [here](https://termer.net/javadoc/twine/1.0-alpha/).

## TwineModule Interface
The methods that are defined in `TwineModule` are as follows:

`name` - Should return the name of the module

`priority` - The priority that this module should be loaded in. `LOW` will be loaded last and shutdown first, `HIGH` will be loaded first and shutdown last. `MEDIUM` is in between.

`twineVersion` - The version of Twine this module was built for. It's a String that's meant to describe the name. For example, `1.0`, `1.0-alpha`, or `1.0-beta`. You can also append `+` to the end of the String to denote that it may be used on any version after the specified version as well as the specified version.

`initialize` - The method that is called when the module is started.

`shutdown` - The method that is called when the module is being shutdown.

## Example Module
Here's an example of a Twine module that creates the route `/hello/:name` on the default domain that returns `Hello <name>`.

```java
public class Module implements TwineModule {

	@Override
	public void initialize() {
		// Get default domain
		Domain defaultDomain = Twine.domains().byName("default");
		
		// Register route
		ServerManager.get(defaultDomain.domain(), "/hello/:name", route -> {
			// Get name from path parameter
			String name = route.pathParam("name");
			
			// End request
			route.response().end("Hello "+name);
		});
	}

	@Override
	public String name() {
		return "Example Module";
	}

	@Override
	public Priority priority() {
		return Priority.LOW;
	}

	@Override
	public void shutdown() {
		Twine.logger().info("Example Module is shutting down!");
	}

	@Override
	public String twineVersion() {
		return "1.0-alpha+";
	}
}
``` 

For more info on the Vert.x API, check out the [Vert.x](https://vertx.io/docs/vertx-core/java/) and [Vert.x-Web](https://vertx.io/docs/vertx-web/java/) docs.

## Deploying Your Module
To deploy your module, compile it, place it in Twine's `modules/` directory, and its dependencies in the `dependencies/` directory.