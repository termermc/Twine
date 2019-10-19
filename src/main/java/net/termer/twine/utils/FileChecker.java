package net.termer.twine.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import net.termer.twine.Twine;

/**
 * Utility class to check files and directories (and create them).
 * @author termer
 * @since 1.0-alpha
 */
public class FileChecker {
	/**
	 * Checks if the specified file paths exist.
	 * Paths ending with "/" are recognized as directories, otherwise they are treated as normal files.
	 * @param paths the file paths
	 * @since 1.0-alpha
	 */
	public static boolean filesPresent(String[] paths) {
		boolean present = true;
		for(String path : paths) {
			boolean dir = path.endsWith("/");
			File file = new File(path);
			
			if(file.exists()) {
				if(dir && !file.isDirectory()) {
					present = false;
					break;
				}
			} else {
				present = false;
				break;
			}
		}
		return present;
	}
	
	/**
	 * Creates the specified file paths if they do not exist.
	 * Paths ending with "/" are recognized as directories, otherwise they are treated as normal files.
	 * @param paths the file paths
	 * @since 1.0-alpha
	 */
	public static void createIfNotPresent(String[] paths) {
		for(String path : paths) {
			if(path.startsWith("./")) path = path.substring(2);
			File file = new File(path);
			if(path.endsWith("/")) {
				if(file.exists()) {
					if(!file.isDirectory()) file.mkdirs();
				} else {
					file.mkdirs();
				}
			} else {
				// Create parent directory if required
				if(path.contains("/")) {
					// Cut off filename
					File pDir = new File(path.substring(0, path.lastIndexOf('/')));
					
					if(!pDir.exists()) pDir.mkdirs();
				}
				if(!file.exists()) {
					// Check resources
					try {
						InputStream is = FileChecker.class.getClassLoader().getResourceAsStream("resources/"+path);
						
						if(is != null) {
							FileOutputStream fos = new FileOutputStream(file);
							// Write resource contents to file
							while(is.available() > 0) {
								fos.write(is.read());
							}
							fos.close();
							is.close();
						} else {
							file.createNewFile();
						}
					} catch (IOException e) {
						Twine.logger().error("Failed to create file \""+path+"\"");
						e.printStackTrace();
					}
				}
			}
		}
	}
	/**
	 * Creates the specified file paths if they do not exist.
	 * Will only check the array of paths if the first path does not exist.
	 * Paths ending with "/" are recognized as directories, otherwise they are treated as normal files.
	 * @param requiredPath the path required to not exist for the rest of the paths to be check
	 * @param paths the file paths
	 * @since 1.0-alpha
	 */
	public static void createIfNotPresent(String requiredPath, String[] paths) {
		// Only create if required path is not present
		if(!new File(requiredPath).exists())
			createIfNotPresent(paths);
	}
	/**
	 * Deletes the specified file paths.
	 * Paths ending with "/" are recognized as directories, otherwise they are treated as normal files.
	 * @param paths the file paths
	 * @since 1.0-alpha
	 */
	public static void delete(String[] paths) {
		for(String path : paths) {
			if(path.startsWith("./")) path = path.substring(2);
			if(path.endsWith("/")) {
				File dir = new File(path);
				for(File f : dir.listFiles()) {
					if(f.isDirectory()) {
						delete(new String[] {f.getAbsolutePath()+'/'});
						f.delete();
					} else {
						f.delete();
					}
				}
				dir.delete();
			} else {
				new File(path).delete();
			}
		}
	}
}
