/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.tools.gui;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

/**
 * @author lazyman
 */
public class PropertiesGenerator {

    private static final String ENCODING = "utf-8";
    private GeneratorConfiguration config;

    public PropertiesGenerator(GeneratorConfiguration config) {
        Validate.notNull(config, "Generator configuration must not be null.");
        this.config = config;
    }

    public void generate() {
        long time = System.currentTimeMillis();
        System.out.println("Starting...");
        //todo fix page.title key!!!!
        try {
            System.out.println("Checking new properties.");
            Properties newProperties = loadNewProperties();
            System.out.println("Loaded " + newProperties.size() + " keys.");

            for (Locale locale : config.getLocalesToCheck()) {
                System.out.println("Loading existing properties for " + locale + ".");
                Properties existingProperties = loadExistingProperties(locale);
                System.out.println("Loaded " + existingProperties.size() + " keys, merging.");

                Set<Object> keySet = existingProperties.keySet();
                for (Object key : keySet) {
                    if (!newProperties.containsKey(key)) {
                        newProperties.setProperty("#" + (String) key, (String) existingProperties.get(key));
                        continue;
                    }

                    newProperties.setProperty((String) key, (String) existingProperties.get(key));
                }
                System.out.println("Merge finished, backing up old file and saving.");
                backupExistingAndSaveNewProperties(newProperties, locale);
            }
        } catch (Exception ex) {
            System.out.println("Something went horribly wrong...");
            ex.printStackTrace();
        }

        System.out.println("Finished. Time: " + (System.currentTimeMillis() - time));
    }

    private void backupExistingAndSaveNewProperties(Properties properties, Locale locale) throws IOException {
        File file = createExistingFile(locale);
        if (file.exists()) {
            File backupFile = new File(file.getParentFile(), file.getName() + ".backup");
            FileUtils.copyFile(file, backupFile);
            file.delete();
        }

        file.createNewFile();
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(file), Charset.forName(ENCODING));
            properties.store(writer, "Generated for locale " + locale);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    private File createExistingFile(Locale locale) {
        String fileName = config.getPropertiesBaseName() + "_" + locale + ".properties";
        return new File(config.getTargetFolder(), fileName);
    }

    private Properties loadExistingProperties(Locale locale) throws IOException {
        File file = createExistingFile(locale);

        Properties properties = new SortedProperties();
        if (!file.exists() || !file.canRead()) {
            return properties;
        }

        Reader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(file), ENCODING);
            properties.load(reader);
        } finally {
            IOUtils.closeQuietly(reader);
        }

        return properties;
    }

    private Properties loadNewProperties() throws IOException {
        Properties properties = new SortedProperties();

        loadExistingProperties(properties, config.getBaseFolder(), config.getRecursiveFolderToCheck(), true);
        loadExistingProperties(properties, config.getBaseFolder(), config.getNonRecursiveFolderToCheck(), false);

        return properties;
    }

    private void loadExistingProperties(Properties properties, File parent, List<String> folders,
            boolean recursive) throws IOException {

        for (String path : folders) {
            File realFolder = new File(parent, path);

            Reader reader = null;
            Collection<File> files = FileUtils.listFiles(realFolder, new String[]{"properties"}, recursive);
            for (File file : files) {
                try {
                    reader = new InputStreamReader(new FileInputStream(file), ENCODING);
                    properties.load(reader);
                } finally {
                    IOUtils.closeQuietly(reader);
                }
            }
        }
    }
}
