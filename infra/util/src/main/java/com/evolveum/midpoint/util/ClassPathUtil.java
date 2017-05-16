/*
 * Copyright (c) 2010-2017 Evolveum
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
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author Peter Prochazka
 * @author Radovan Semancik
 */
public class ClassPathUtil {

	public static Trace LOGGER = TraceManager.getTrace(ClassPathUtil.class);

	public static Set<Class> listClasses(Package pkg) {
		return listClasses(pkg.getName());
	}
	
	public static Set<Class> listClasses(String packageName) {
		Set<Class> classes = new HashSet<Class>();
		searchClasses(packageName, c -> classes.add(c));
		return classes;
	}
	
	/**
	 * This is not entirely reliable method.
	 * Maybe it would be better to rely on Spring ClassPathScanningCandidateComponentProvider
	 */
	public static void searchClasses(String packageName, Consumer<Class> consumer) {
		String path = packageName.replace('.', '/');
		Enumeration<URL> resources = null;
		// HACK this is not available use LOGGER
		ClassLoader classLoader = LOGGER.getClass().getClassLoader();
		LOGGER.trace("Classloader: {} : {}", classLoader, classLoader.getClass());
		try {
			resources = classLoader.getResources(path);
		} catch (IOException e) {
			LOGGER.error("Classloader scaning error for " + path, e);
		}

		while (resources.hasMoreElements()) {
			URL candidateUrl = resources.nextElement();
			LOGGER.trace("Candidates from: " + candidateUrl);

			// test if it is a directory or JAR
            String protocol = candidateUrl.getProtocol(); 
            if ("file".contentEquals(protocol)) {
            	getFromDirectory(candidateUrl, packageName, consumer);
            } else if ("jar".contentEquals(protocol) || "zip".contentEquals(protocol)) {
            	getFromJar(candidateUrl, packageName, consumer);
            } else {
                LOGGER.warn("Unsupported protocol for candidate URL {}", candidateUrl);
            }		
        }

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
	public static boolean extractFileFromClassPath(String src, String dst) {
		InputStream is = ClassPathUtil.class.getClassLoader().getResourceAsStream(src);
		if (null == is) {
			LOGGER.error("Unable to find file {} for extraction to {}", src, dst);
			return false;
		}

		return copyFile(is, src, dst);
	}
	
	public static boolean copyFile(InputStream srcStream, String srcName, String dstPath) {
		OutputStream dstStream = null;
		try {
			dstStream = new FileOutputStream(dstPath);
		} catch (FileNotFoundException e) {
			LOGGER.error("Unable to open destination file " + dstPath + ":", e);
			return false;
		}
		return copyFile(srcStream, srcName, dstStream, dstPath);
	}
	
	public static boolean copyFile(InputStream srcStream, String srcName, File dstFile) {
		OutputStream dstStream = null;
		try {
			dstStream = new FileOutputStream(dstFile);
		} catch (FileNotFoundException e) {
			LOGGER.error("Unable to open destination file " + dstFile + ":", e);
			return false;
		}
		return copyFile(srcStream, srcName, dstStream, dstFile.toString());
	}
	
	public static boolean copyFile(InputStream srcStream, String srcName, OutputStream dstStream, String dstName) {
		byte buf[] = new byte[655360];
		int len;
		try {
			while ((len = srcStream.read(buf)) > 0) {
				try {
					dstStream.write(buf, 0, len);
				} catch (IOException e) {
					LOGGER.error("Unable to write file " + dstName + ":", e);
					return false;
				}
			}
		} catch (IOException e) {
			LOGGER.error("Unable to read file " + srcName + " from classpath", e);
			return false;
		}
		try {
			dstStream.close();
		} catch (IOException e) {
			LOGGER.error("Unable to close file " + dstName + ":", e);
			return false;
		}
		try {
			srcStream.close();
		} catch (IOException e) {
			LOGGER.error("This never happend:", e);
			return false;
		}

		return true;
	}
	
	/**
	 * Extracts all files in a directory on a classPath (system resource) to
	 * a directory on a file system.
	 */
	public static boolean extractFilesFromClassPath(String srcPath, String dstPath, boolean overwrite) throws URISyntaxException, IOException {
		URL src = ClassPathUtil.class.getClassLoader().getResource(srcPath);
		if (src == null) {
			LOGGER.debug("No resource for {}", srcPath);
			return false;
		}
		URI srcUrl = src.toURI();
//		URL srcUrl = ClassLoader.getSystemResource(srcPath);
		LOGGER.trace("URL: {}", srcUrl);
		if (srcUrl.getPath().contains("!/")) {
			URI srcFileUri = new URI(srcUrl.getPath().split("!/")[0]);		// e.g. file:/C:/Documents%20and%20Settings/user/.m2/repository/com/evolveum/midpoint/infra/test-util/2.1-SNAPSHOT/test-util-2.1-SNAPSHOT.jar
			File srcFile = new File(srcFileUri);
			JarFile jar = new JarFile(srcFile);
			Enumeration<JarEntry> entries = jar.entries();
			JarEntry jarEntry;
			while (entries.hasMoreElements()) {
				jarEntry = entries.nextElement();

				// skip other files
				if (!jarEntry.getName().contains(srcPath)) {
					LOGGER.trace("Not relevant: ", jarEntry.getName());
					continue;
				}
				
				// prepare destination file
				String filepath = jarEntry.getName().substring(srcPath.length());
				File dstFile = new File(dstPath, filepath);
				
				if (!overwrite && dstFile.exists()) {
					LOGGER.debug("Skipping file {}: exists", dstFile);
					continue;
				}
				
				if (jarEntry.isDirectory()) {
					dstFile.mkdirs();
					continue;
				}
				
				InputStream is = ClassLoader.getSystemResourceAsStream(jarEntry.getName());
				LOGGER.debug("Copying {} from {} to {} ", jarEntry.getName(), srcFile, dstFile);
				copyFile(is, jarEntry.getName(), dstFile);
			}
			jar.close();
		} else {
			try {
				File file = new File(srcUrl);
				File[] files = file.listFiles();
				for (File subFile : files) {
					File dstFile = new File(dstPath, subFile.getName());
					if (subFile.isDirectory()) {
						LOGGER.debug("Copying directory {} to {} ", subFile, dstFile);
						MiscUtil.copyDirectory(subFile, dstFile);
					} else {
						LOGGER.debug("Copying file {} to {} ", subFile, dstFile);
						MiscUtil.copyFile(subFile, dstFile);
					}
				}
			} catch (Exception ex) {
				throw new IOException(ex);
			}
		}
		return true;
	}

	/**
	 * Get clasess from JAR
	 * 
	 * @param srcUrl
	 * @param packageName
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private static void getFromJar(URL srcUrl, String packageName, Consumer<Class> consumer) {
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
			return;
		}

		if (!jarTmp.isFile()) {
			LOGGER.error("Is {} not a file.", srcName);
		}

		JarFile jar = null;
		try {
			jar = new JarFile(jarTmp);
		} catch (IOException ex) {
			LOGGER.error("Error during open JAR " + srcName, ex);
			return;
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
				Class clazz = Class.forName(name.replace('/', '.').replace(".class", ""));
				consumer.accept(clazz);
			} catch (ClassNotFoundException ex) {
				LOGGER.error("Error during loading class {} from {}. ", name, jar.getName());
			}
		}

		try {
			jar.close();
		} catch (IOException ex) {
			LOGGER.error("Error during close JAR {} " + srcName, ex);
			return;
		}

	}

	/**
	 * get classes from directory
	 * 
	 * @param candidateUrl
	 * @param packageName
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private static void getFromDirectory(URL candidateUrl, String packageName, Consumer<Class> consumer) {

		// Directory preparation
		File dir = null;
		try {
			dir = new File(candidateUrl.toURI());
		} catch (URISyntaxException e) {
			LOGGER.error("NEVER HAPPEND -- Wrong URI: " + candidateUrl.getPath(), e);
			return;
		}

		// Skip if it is directory
		if (!dir.isDirectory()) {
			LOGGER.warn("   Skip: {} is not a directory", candidateUrl.getPath());
			return;
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
				Class<?> clazz = Class.forName(packageName + "." + dirList[i].replace(".class", ""));
				consumer.accept(clazz);
			} catch (ClassNotFoundException e) {
				LOGGER.error("Error during loading class {} from {}. ", dirList[i], dir.getAbsolutePath());
			}
		}
	}
}
