package net.termer.twine.utils.files;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Utility class for writing to files in a blocking fashion
 * @author termer
 * @since 2.0
 */
public class BlockingWriter {
	/**
	 * Writes the provided String to the specified file
	 * @param path the path to the file
	 * @param content the content to write
	 * @throws IOException whether writing to the file fails 
	 * @since 2.0
	 */
	public static void write(String path, String content) throws IOException {
		FileOutputStream fos = new FileOutputStream(path);
		fos.write(content.getBytes());
		fos.close();
	}
	
	/**
	 * Appends the provided content to the specified file
	 * @param path the path to the file
	 * @param content the content to append
	 * @throws IOException if appending to the file fails
	 * @since 2.0
	 */
	public static void append(String path, String content) throws IOException {
		FileWriter fw = new FileWriter(path, true);
		BufferedWriter bw = new BufferedWriter(fw);
	    PrintWriter out = new PrintWriter(bw);
		out.print(content);
		out.close();
		bw.close();
		fw.close();
	}
}
