package net.termer.twine.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Utility class to read files
 * @author termer
 * @since 1.0-alpha
 */
public class Reader {
	/**
	 * Reads the provided file to a String
	 * @param file the file
	 * @return the contents of the file as a String
	 * @throws IOException if reading the file fails
	 * @since 1.0-alpha
	 */
	public static String read(File file) throws IOException {
		StringBuilder sb = new StringBuilder();
		
		FileInputStream fin = new FileInputStream(file);
		while(fin.available() > 0) {
			sb.append((char)fin.read());
		}
		fin.close();
		
		return sb.toString();
	}
	/**
	 * Reads the provided file to a String
	 * @param path the file path
	 * @return the contents of the file as a String
	 * @throws IOException if reading the file fails
	 * @since 1.0-alpha
	 */
	public static String read(String path) throws IOException {
		return read(new File(path));
	}
	
	/**
	 * Reads all lines in a file into a String array
	 * @param file the file to read
	 * @return the file's lines in an array
	 * @throws IOException if reading the file fails
	 * @since 1.0-alpha
	 */
	public static String[] lines(File file) throws IOException {
		ArrayList<String> lns = new ArrayList<String>();
		StringBuilder tmp = new StringBuilder();
		
		FileInputStream fin = new FileInputStream(file);
		while(fin.available() > 0) {
			char c = (char)fin.read();
			// Dump buffer at newlines
			if(c == '\n') {
				lns.add(tmp.toString());
				tmp = new StringBuilder();
			} else {
				tmp.append(c);
			}
		}
		lns.add(tmp.toString());
		fin.close();
		
		return lns.toArray(new String[0]);
	}
	
	/**
	 * Reads all lines in a file into a String array
	 * @param path the file path
	 * @return the file's lines in an array
	 * @throws IOException if reading the file fails
	 * @since 1.0-alpha
	 */
	public static String[] lines(String path) throws IOException {
		return lines(new File(path));
	}
}