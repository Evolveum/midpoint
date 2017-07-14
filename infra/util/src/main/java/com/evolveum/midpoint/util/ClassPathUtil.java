/*
 * Copyright (c) 2010-2016 Evolveum
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
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jboss.vfs.*;

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
			} else if ("jar".contentEquals(protocol) || "zip".contentEquals(protocol) ) {
				classes.addAll(getFromJar(candidateUrl, packageName));
			} else if ("vfs".contentEquals(protocol)) {
				classes.addAll(getFromVfs(candidateUrl, packageName));
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

	/**
	 * get classes from directory
	 *
	 * @param candidateUrl
	 * @param packageName
	 * @return
	 */
	@SuppressWarnings("rawtypes")
    private static Collection<Class> getFromVfs(URL candidateUrl, String packageName) {

        @SuppressWarnings("rawtypes")
        Set<Class> classes = new HashSet<>();

        // JBoss Virtual File System
        try {
            Object content = candidateUrl.getContent();
            if (content != null && content instanceof VirtualFile) {
                VirtualFile vf = (VirtualFile) content;
                if (vf.isDirectory()) {
                    for (VirtualFile file : vf.getChildrenRecursively()) {
                        Class clazz = getClassFromFileName(file.getName(), packageName, file.getPathName());
                        if (clazz != null)
                            classes.add(clazz);
                    }
                } else {
                    Class clazz = getClassFromFileName(vf.getName(), packageName, vf.getPathName());
                    if (clazz != null)
                        classes.add(clazz);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error while attempting to get content from URL: " + candidateUrl.getPath(), e);
            return classes;
        }

        return classes;
    }

    private static Class getClassFromFileName(String fileName, String packageName, String filePath) {
        if (!fileName.endsWith(".class")) {
            return null;
        }
        try {
            LOGGER.trace("DIR Candidate: {}", filePath);
            return Class.forName(packageName + "." + fileName.replace(".class", ""));
        } catch (ClassNotFoundException e) {
            LOGGER.error("Error during loading class {} from {}. ", fileName, filePath);
            return null;
        }
    }
}
