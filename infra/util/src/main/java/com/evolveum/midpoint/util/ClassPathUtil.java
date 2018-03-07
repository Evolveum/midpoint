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

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.google.common.collect.Multimap;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.io.*;
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

/**
 * @author Peter Prochazka
 * @author Radovan Semancik
 * @author Viliam Repan (lazyman)
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
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setScanners(new SubTypesScanner(false));
        builder.setUrls(ClasspathHelper.forPackage(packageName, LOGGER.getClass().getClassLoader()));
        builder.setInputsFilter(new FilterBuilder().includePackage(packageName));

        Reflections reflections = new Reflections(builder);

        Multimap<String, String> map = reflections.getStore().get(SubTypesScanner.class.getSimpleName());
        Set<String> types = new HashSet<>();

        for (String key : map.keySet()) {
            Collection<String> col = map.get(key);
            if (col == null) {
                continue;
            }

            for (String c : col) {
                String simpleName = c.replaceFirst(packageName + "\\.", "");
                if (simpleName.contains(".")) {
                    continue;
                }

                types.add(c);
            }
        }

        for (String type : types) {
            try {
                Class clazz = Class.forName(type);
                consumer.accept(clazz);
            } catch (ClassNotFoundException e) {
                LOGGER.error("Error during loading class {}. ", type);
            }
        }
    }

    /**
     * Extract specified source on class path to file system dst
     *
     * @param src source
     * @param dst destination
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

        String[] parts = srcUrl.toString().split("!/");
        if (parts.length == 3
                &&  parts[1].equals("WEB-INF/classes")) {
            // jar:file:<ABSOLUTE_PATH>/midpoint.war!/WEB-INF/classes!/initial-midpoint-home
            srcUrl = URI.create(parts[0] + "!/" + parts[1] + "/" + parts[2]);
        }

        LOGGER.trace("URL: {}", srcUrl);
        if (srcUrl.toString().contains("!/")) {
            String uri = srcUrl.toString().split("!/")[0].replace("jar:", "");
            // file:<ABSOLUTE_PATH>/midpoint.war
            URI srcFileUri = URI.create(uri);
            File srcFile = new File(srcFileUri);
            JarFile jar = new JarFile(srcFile);
            Enumeration<JarEntry> entries = jar.entries();
            JarEntry jarEntry;
            while (entries.hasMoreElements()) {
                jarEntry = entries.nextElement();

                // skip other files
                if (!jarEntry.getName().contains(srcPath)) {
                    LOGGER.trace("Not relevant: {}", jarEntry.getName());
                    continue;
                }

                // prepare destination file
                String entryName = jarEntry.getName();

                String filepath = entryName.substring(entryName.indexOf(srcPath) + srcPath.length());
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
}
