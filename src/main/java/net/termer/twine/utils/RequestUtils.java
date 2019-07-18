package net.termer.twine.utils;

import java.io.File;

import net.termer.twine.utils.Domains.Domain;

/**
 * Utility class for dealing with requests
 * @author termer
 * @since 1.0-alpha
 */
public class RequestUtils {
	/**
	 * Transforms the specified host string into a domain string
	 * @param host the host string
	 * @return the domain string
	 * @since 1.0-alpha
	 */
	public static String domain(String host) {
		String dom = host.toLowerCase();
		if(host.contains(":")) {
			dom = host.split(":")[0];
		}
		return dom;
	}
	
	public static String pathToFile(String path, Domain dom) {
		String pth = path;
		
		if(pth.equals("/")) {
			pth = dom.directory()+dom.index();
		} else {
			if(pth.startsWith("..")) pth = pth.substring(2);
			if(pth.endsWith("/")) pth += "index.html";
			if(pth.startsWith("/")) pth = pth.substring(1);
			pth = dom.directory()+pth;
			if(!pth.endsWith("/") && new File(pth).isDirectory()) pth+="/index.html";
		}
		
		return pth;
	}
}
