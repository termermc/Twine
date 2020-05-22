package net.termer.twine.modules;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import net.lingala.zip4j.ZipFile;
import net.termer.twine.Events;
import net.termer.twine.Twine;
import net.termer.twine.modules.TwineModule.Priority;

/**
 * utility class to load and interact with modules
 * @author termer
 * @since 1.0-alpha
 */
public class ModuleManager {
	private static ArrayList<TwineModule> _modules = new ArrayList<TwineModule>();
	private static HashMap<Priority, CopyOnWriteArrayList<TwineModule>> _priorities = new HashMap<Priority, CopyOnWriteArrayList<TwineModule>>();
	
	/**
	 * Loads all modules and dependencies
	 * @throws IOException if loading a module fails
	 * @since 1.0-alpha
	 */
	public static void loadModules() throws IOException {
		ArrayList<URL> urls = new ArrayList<URL>();
		
		// Begin loading module launch classes
		ArrayList<String> launchClasses = new ArrayList<String>();
		
		for(File jar : new File("modules/").listFiles()) {
			if(jar.getName().toLowerCase().endsWith(".jar")) {
				ZipFile zf = new ZipFile(jar.getAbsolutePath());
				// Check if jar is a valid zip
				if(zf.isValidZipFile()) {
					urls.add(new URL("file:"+jar.getAbsolutePath()));
					JarFile jf = new JarFile(jar.getAbsolutePath());
					Enumeration<JarEntry> ent = jf.entries();
					// Enumerate classes
					while(ent.hasMoreElements()) {
						String name = ent.nextElement().getName();
						if(name.toLowerCase().endsWith(".class")) {
							String clazz = name.replace("/", ".").replace(".class", "");
							// Add launch class if valid
							if(clazz.endsWith("Module")) {
								launchClasses.add(clazz);
							}
						}
					}
					jf.close();
				} else {
					throw new IOException("File is not a valid jarfile");
				}
			}
		}
		
		// Loop through launch classes
		URLClassLoader ucl = new URLClassLoader(urls.toArray(new URL[0]));
		for(String launchClass : launchClasses) {
			try {
				Class<?> cls = ucl.loadClass(launchClass);
				for(Class<?> inter : cls.getInterfaces()) {
					// If class implements TwineModule, add it to the modules array
					if(inter.getTypeName().equals("net.termer.twine.modules.TwineModule")) {
						_modules.add((TwineModule) cls.newInstance());
						break;
					}
				}
			} catch(ClassNotFoundException | InstantiationException | IllegalAccessException e) {
				Twine.logger().error("Failed to load module class \""+launchClass+"\":");
				e.printStackTrace();
			}
		}
		ucl.close();

		// Fire MODULES_LOADED event
		Events.fire(Events.Type.MODULES_LOADED);
		
		// Loop through modules and sort them
		_priorities.put(Priority.LOW, new CopyOnWriteArrayList<TwineModule>());
		_priorities.put(Priority.MEDIUM, new CopyOnWriteArrayList<TwineModule>());
		_priorities.put(Priority.HIGH, new CopyOnWriteArrayList<TwineModule>());
		for(TwineModule module : _modules) {
			if(compatible(module.twineVersion()) || (boolean) Twine.config().get("ignoreModuleCheck")) {
				_priorities.get(module.priority()).add(module);
			} else {
				Twine.logger().error("Module \""+module.name()+"\" is written for Twine version \""+module.twineVersion()+"\" which is incompatible with version "+Twine.version()+".");
				Twine.logger().error("The module will not be loaded.");
			}
		}
	}
	
	/**
	 * Runs the initialize() methods on all modules according to their priority
	 * @since 1.3
	 */
	public static void runModuleInits() {
		// Loop through and execute module initializers
		for(TwineModule m : _priorities.get(Priority.HIGH)) {
			init(m, Priority.HIGH);
		}
		for(TwineModule m : _priorities.get(Priority.MEDIUM)) {
			init(m, Priority.MEDIUM);
		}
		for(TwineModule m : _priorities.get(Priority.LOW)) {
			init(m, Priority.LOW);
		}

		// Fire MODULES_INITIALIZED event
		Events.fire(Events.Type.MODULES_INITIALIZED);
	}
	/**
	 * Runs the preinitialize() methods on all modules according to their priority
	 * @since 1.3
	 */
	public static void runModulePreInits() {
		// Loop through and execute module initializers
		for(TwineModule m : _priorities.get(Priority.HIGH)) {
			preinit(m, Priority.HIGH);
		}
		for(TwineModule m : _priorities.get(Priority.MEDIUM)) {
			preinit(m, Priority.MEDIUM);
		}
		for(TwineModule m : _priorities.get(Priority.LOW)) {
			preinit(m, Priority.LOW);
		}

		// Fire MODULES_PREINITIALIZED event
		Events.fire(Events.Type.MODULES_PREINITIALIZED);
	}
	
	/**
	 * Executes the shutdown methods for all loaded modules.
	 * Blocks the thread until all the modules' `shutdown` methods are executed.
	 * @since 1.0-alpha
	 */
	public static void shutdownModules() {
		// Check if modules were disabled on startup
		if(!(Twine.serverArgs().flag('m') || Twine.serverArgs().option("skip-modules"))) {
			for(TwineModule m : _priorities.get(Priority.LOW)) {
				sdMod(m);
			}
			for(TwineModule m : _priorities.get(Priority.MEDIUM)) {
				sdMod(m);
			}
			for(TwineModule m : _priorities.get(Priority.HIGH)) {
				sdMod(m);
			}
		}
	}

	/**
	 * Returns all loaded modules
	 * @return all loaded modules
	 * @since 1.5
	 */
	public static TwineModule[] modules() {
		return _modules.toArray(new TwineModule[0]);
	}
	/**
	 * Returns all modules with the provided priority
	 * @param priority the priority of modules to return
	 * @return all modules with the provided priority
	 * @since 1.0
	 */
	public static TwineModule[] modules(Priority priority) {
		return _priorities.get(priority).toArray(new TwineModule[0]);
	}

	// Shuts down a module
	private static void sdMod(TwineModule m) {
		try {
			m.shutdown();
		} catch(AbstractMethodError e) {
			Twine.logger().error("Module \""+m.name()+"\" does not contain a shutdown method.");
			Twine.logger().error("It will be ignored.");
		} catch(Exception e) {
			Twine.logger().error("Error occurred while shutting down module \""+m.name()+"\":");
			Twine.logger().error(e.getClass().getName()+": "+e.getMessage()+"");
			for(StackTraceElement ste : e.getStackTrace()) {
				Twine.logger().error(ste.getClassName()+"("+ste.getFileName()+":"+Integer.toString(ste.getLineNumber())+")");
			}
		}
	}
	
	// Initializes the provided TwineModule
	private static void init(TwineModule m, Priority p) {
		Twine.logger().info("Initializing module \""+m.name()+"\"...");
		try {
			m.initialize();
		} catch(AbstractMethodError e) {
			Twine.logger().error("Module \""+m.name()+"\" does not contain an initilization method.");
			Twine.logger().error("The module will not be loaded.");
		} catch(Exception e) {
			Twine.logger().error("Error occurred while initializing module \""+m.name()+"\":");
			Twine.logger().error(e.getClass().getName()+": "+e.getMessage()+"");
			for(StackTraceElement ste : e.getStackTrace()) {
				Twine.logger().error(ste.getClassName()+"("+ste.getFileName()+":"+Integer.toString(ste.getLineNumber())+")");
			}
			Twine.logger().info("The module will be removed from the modules stack, but can still be referenced by other modules.");
			_modules.remove(m);
			_priorities.get(p).remove(m);
		}
	}
	// Pre-Initializes the provided TwineModule
	private static void preinit(TwineModule m, Priority p) {
		Twine.logger().info("Pre-initializing module \""+m.name()+"\"...");
		try {
			m.preinitialize();
		} catch(AbstractMethodError e) {
			Twine.logger().warn("Module \""+m.name()+"\" does not contain a pre-initilization method.");
		} catch(Exception e) {
			Twine.logger().error("Error occurred while pre-initializing module \""+m.name()+"\":");
			Twine.logger().error(e.getClass().getName()+": "+e.getMessage()+"");
			for(StackTraceElement ste : e.getStackTrace()) {
				Twine.logger().error(ste.getClassName()+"("+ste.getFileName()+":"+Integer.toString(ste.getLineNumber())+")");
			}
			Twine.logger().info("The module will be removed from the modules stack, but can still be referenced by other modules.");
			_modules.remove(m);
			_priorities.get(p).remove(m);
		}
	}
	
	// Returns whether the specified compatible version String is compatible with this version of Twine
	private static boolean compatible(String ver) {
		// Parse out version numbers
		String sver = Twine.version().toLowerCase();
		int lvl = verLvl(sver);
		if(sver.contains("-")) sver = sver.split("-")[0];
		double sverNum = Double.parseDouble(sver);
		
		int vlvl = verLvl(ver);
		boolean plus = ver.endsWith("+");
		if(ver.contains("-")) ver = ver.split("-")[0];
		
		// Trim off "+"
		if(ver.endsWith("+"))
			ver = ver.substring(0, ver.length()-1);
		
		// Parse version number
		double verNum = Double.parseDouble(ver);
		
		// Resolve compatibility
		boolean compat = false;
		if(plus) {
			if(lvl >= vlvl && vlvl > 1 && sverNum >= verNum) {
				compat = true;
			}
		} else if(lvl == vlvl && sverNum == verNum) {
			compat = true;
		}
		
		return compat;
	}
	
	// Determines the release level (alpha, beta, release) of a version String
	private static int verLvl(String ver) {
		ver = ver.toLowerCase();
		if(ver.endsWith("+")) ver = ver.substring(0, ver.length()-1);
		int vlvl = 0;
		if(ver.contains("-")) {
			String type = ver.split("-")[1];
			if(type.equals("beta")) vlvl= 1;
		} else {
			vlvl = 2;
		}
		return vlvl;
	}
}