/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * 
 */
package com.evolveum.midpoint.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.jetty.jndi.local.localContextRoot;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author Radovan Semancik
 * 
 */
public class ClassPathUtil {

	public static Trace LOGGER = TraceManager.getTrace(ClassPathUtil.class);

	public static Set<Class> listClasses(Package pkg) {
		return listClasses(pkg.getName());
	}
	
	public static Set<Class> listClasses(String packageName) {

		Set<Class> classes = new HashSet<Class>();

		String path = packageName.replace('.', '/');
		Enumeration<URL> resources = null;
		try {// HACK this is not available use LOGGER
			resources = LOGGER.getClass().getClassLoader().getResources(path);
		} catch (IOException e) {
			LOGGER.error("Classloader scaning error for " + path, e);
		}

		while (resources.hasMoreElements()) {
			URL candidateUrl = resources.nextElement();
			LOGGER.trace("Candidates from: " + candidateUrl);

			// test if it is a directory or JAR
            String protocol = candidateUrl.getProtocol(); 
            if ("file".contentEquals(protocol)) {
            	classes.addAll(getFromDirectory(candidateUrl, packageName));
            } else if ("jar".contentEquals(protocol) || "zip".contentEquals(protocol)) {
            	classes.addAll(getFromJar(candidateUrl, packageName));
            } else {
                LOGGER.warn("Unsupported protocol for candidate URL {}", candidateUrl);
            }		
        }

		return classes;
	}

	/**
	 * Extract specified source on class path to file system dst
	 * 
	 * @param src
	 *            source
	 * @param dst
	 *            destination
	 * @return successful extraction
	 */
	public static Boolean extractFileFromClassPath(String src, String dst) {
		InputStream is = ClassPathUtil.class.getClassLoader().getResourceAsStream(src);
		if (null == is) {
			LOGGER.error("Unable to find file {} for extraction to {}", src, dst);
			return false;
		}

		// Copy content
		OutputStream out = null;
		try {
			out = new FileOutputStream(dst);
		} catch (FileNotFoundException e) {
			LOGGER.error("Unable to open destination file " + dst + ":", e);
			return false;
		}
		byte buf[] = new byte[655360];
		int len;
		try {
			while ((len = is.read(buf)) > 0) {
				try {
					out.write(buf, 0, len);
				} catch (IOException e) {
					LOGGER.error("Unable to write file " + dst + ":", e);
					return false;
				}
			}
		} catch (IOException e) {
			LOGGER.error("Unable to read file " + src + " from classpath", e);
			return false;
		}
		try {
			out.close();
		} catch (IOException e) {
			LOGGER.error("Unable to close file " + dst + ":", e);
			return false;
		}
		try {
			is.close();
		} catch (IOException e) {
			LOGGER.error("This never happend:", e);
			return false;
		}

		return true;
	}

	/**
	 * Get clasess from JAR
	 * 
	 * @param candidateUrl
	 * @param packageName
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private static Collection<? extends Class> getFromJar(URL srcUrl, String packageName) {
		@SuppressWarnings("rawtypes")
		Set<Class> classes = new HashSet<Class>();
		// sample:
		// file:/C:/.m2/repository/test-util/1.9-SNAPSHOT/test-util-1.9-SNAPSHOT.jar!/test-data/opendj.template
		// output:
		// file/C:/.m2/repository/test-util/1.9-SNAPSHOT/test-util-1.9-SNAPSHOT.jar
		String srcName = srcUrl.getPath().split("!/")[0];

		//solution to make it work in Weblogic, because we have to make the path absolute, that means with scheme, that means basically with prefix file:/
		//in Tomcat the form is jar:file:/
		//in Weblogic the form is only zip:/
		LOGGER.trace("srcUrl.getProtocol(): {}", srcUrl.getProtocol());
		
		if ("zip".equals(srcUrl.getProtocol())) {
			srcName = "file:" + srcName;
		}
		LOGGER.trace("srcName: {}", srcName);
		
		// Probably hepls fix error in windows with URI
		File jarTmp = null;
		try {
			jarTmp = new File(new URI(srcName));
		} catch (URISyntaxException ex) {
			LOGGER.error("Error converting jar " + srcName + " name to URI:", ex);
			return classes;
		}

		if (!jarTmp.isFile()) {
			LOGGER.error("Is {} not a file.", srcName);
		}

		JarFile jar = null;
		try {
			jar = new JarFile(jarTmp);
		} catch (IOException ex) {
			LOGGER.error("Error during open JAR " + srcName, ex);
			return classes;
		}
		String path = packageName.replace('.', '/');
		Enumeration<JarEntry> entries = jar.entries();
		LOGGER.trace("PATH:" + path);

		JarEntry e;
		while (entries.hasMoreElements()) {
			e = entries.nextElement();
			// get name and replace inner class
			String name = e.getName().replace('$', '/');
			// NOTICE: inner class are seperated and anonymous are part of the
			// listing !!

			// skip other files in other packas and skip non class files
			if (!name.contains(path) || !name.contains(".class")) {
				continue;
			}
			// Skip all that are not in package
			if (name.matches(path + "/.+/.*.class")) {
				continue;
			}

			LOGGER.trace("JAR Candidate: {}", name);
			try {// to create class

				// Convert name back to package
				classes.add(Class.forName(name.replace('/', '.').replace(".class", "")));
			} catch (ClassNotFoundException ex) {
				LOGGER.error("Error during loading class {} from {}. ", name, jar.getName());
			}
		}

		try {
			jar.close();
		} catch (IOException ex) {
			LOGGER.error("Error during close JAR {} " + srcName, ex);
			return classes;
		}

		return classes;
	}

	/**
	 * get classes from directory
	 * 
	 * @param candidateUrl
	 * @param packageName
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private static Collection<Class> getFromDirectory(URL candidateUrl, String packageName) {

		@SuppressWarnings("rawtypes")
		Set<Class> classes = new HashSet<Class>();

		// Directory preparation
		File dir = null;
		try {
			dir = new File(candidateUrl.toURI());
		} catch (URISyntaxException e) {
			LOGGER.error("NEVER HAPPEND -- Wrong URI: " + candidateUrl.getPath(), e);
			return classes;
		}

		// Skip if it is directory
		if (!dir.isDirectory()) {
			LOGGER.warn("   Skip: {} is not a directory", candidateUrl.getPath());
			return classes;
		}

		// List directory
		String[] dirList = dir.list();
		for (int i = 0; i < dirList.length; i++) {
			// skip directories
			if (!dirList[i].contains(".class")) {
				continue;
			}
			try {// to create class
				LOGGER.trace("DIR Candidate: {}", dirList[i]);
				classes.add(Class.forName(packageName + "." + dirList[i].replace(".class", "")));
			} catch (ClassNotFoundException e) {
				LOGGER.error("Error during loading class {} from {}. ", dirList[i], dir.getAbsolutePath());
			}
		}
		return classes;
	}
}
