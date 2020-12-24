package net.termer.twine.utils.files;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import net.termer.twine.Twine;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class to check files and directories (and create them) in a blocking fashion
 * @author termer
 * @since 2.0
 */
public class BlockingFileChecker {
	/**
	 * Checks if the specified file paths exist.
	 * Paths ending with "/" are recognized as directories, otherwise they are treated as normal files.
	 * @param paths the file paths
	 * @return whether the paths specified all exist
	 * @since 2.0
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
	 * Additionally, if a file's path matches a file in the Twine jar's resources, it will be copied to the location of the file to create if it does not already exist.
	 * @param paths the file paths
	 * @throws IOException If an error occurs while trying to create files or directories
	 * @since 2.0
	 */
	public static void createIfNotPresent(@NotNull String[] paths) throws IOException {
		for(String path : paths) {
			if(path.startsWith("./")) path = path.substring(2);
			File file = new File(path);

			if(path.endsWith("/")) {
				if(file.exists()) {
					if(!file.isDirectory() && !file.mkdirs())
						throw new IOException("Failed to create directory "+file.getPath());
				} else if(!file.mkdirs()) {
					throw new IOException("Failed to create directory "+file.getPath());
				}
			} else {
				// Create parent directory if required
				if(path.contains("/")) {
					// Cut off filename
					File pDir = new File(path.substring(0, path.lastIndexOf('/')));
					
					if(!pDir.exists() && !pDir.mkdirs())
						throw new IOException("Failed to create directory "+pDir.getPath()+" for file "+file.getPath());
				}

				if(!file.exists()) {
					// Check resources
					try {
						InputStream is = BlockingFileChecker.class.getClassLoader().getResourceAsStream("resources/"+path);

						if(is != null) {
							FileOutputStream fos = new FileOutputStream(file);

							// Write resource contents to file
							while(is.available() > 0) {
								fos.write(is.read());
							}
							fos.close();
							is.close();
						} else if(!file.createNewFile()) {
							throw new IOException("Failed to create file "+file.getPath());
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
	 * Additionally, if a file's path matches a file in the Twine jar's resources, it will be copied to the location of the file to create if it does not already exist.
	 * @param requiredPath the path required to not exist for the rest of the paths to be check
	 * @param paths the file paths
	 * @throws IOException If an error occurs while trying to create files or directories
	 * @since 2.0
	 */
	public static void createIfNotPresent(@NotNull String requiredPath, @NotNull String[] paths) throws IOException {
		// Only create if required path is not present
		if(!new File(requiredPath).exists())
			createIfNotPresent(paths);
	}

	/**
	 * Deletes the specified file paths.
	 * Paths ending with "/" are recognized as directories, otherwise they are treated as normal files.
	 * @param paths the file paths
	 * @throws IOException If a file or directory cannot be deleted
	 * @since 2.0
	 */
	public static void delete(String[] paths) throws IOException {
		for(String path : paths) {
			if(path.startsWith("./")) path = path.substring(2);
			if(path.endsWith("/")) {
				File dir = new File(path);
				for(File f : Objects.requireNonNull(dir.listFiles())) {
					if(f.isDirectory()) {
						delete(new String[] {f.getAbsolutePath()+'/'});
					}

					if(f.exists() && !f.delete())
						throw new IOException("Failed to delete file "+f.getPath());
				}

				if(dir.exists() && !dir.delete()) {
					throw new IOException("Failed to delete directory "+dir.getPath());
				}
			} else {
				File file = new File(path);

				if(file.exists() && !new File(path).delete()) {
					throw new IOException("Failed to delete file " + path);
				}
			}
		}
	}
}