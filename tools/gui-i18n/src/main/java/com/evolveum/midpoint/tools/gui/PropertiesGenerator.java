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

package com.evolveum.midpoint.tools.gui;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
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

    private PropertiesStatistics stats;

    public PropertiesGenerator(GeneratorConfiguration config) {
        Validate.notNull(config, "Generator configuration must not be null.");
        this.config = config;
    }

    public void generate() {
        long time = System.currentTimeMillis();
        System.out.println("Starting...");

        try {
            for (Locale locale : config.getLocalesToCheck()) {
                stats = new PropertiesStatistics();
                System.out.println("Loading existing properties for " + locale + ".");

                List<File> existingFiles = new ArrayList<File>();
                existingFiles.addAll(reloadProperties(config.getBaseFolder(),
                        config.getRecursiveFolderToCheck(), true, locale, config.getTargetFolder()));

                existingFiles.addAll(reloadProperties(config.getBaseFolder(), config.getRecursiveFolderToCheck(),
                        false, locale, config.getTargetFolder()));

                cleanupTargetFolder(existingFiles, config.getTargetFolder());

                System.out.println("Changes for locale " + locale + ": " + stats);
            }
        } catch (Exception ex) {
            System.out.println("Something went horribly wrong...");
            ex.printStackTrace();
        }

        System.out.println("Finished. Time: " + (System.currentTimeMillis() - time));
    }

    private void cleanupTargetFolder(List<File> existingFiles, File target) throws IOException {
        Collection<File> files = FileUtils.listFiles(target, new String[]{"properties"}, true);

        for (File file : files) {
            if (existingFiles.contains(file)) {
                continue;
            }
            System.out.println("File to be deleted: " + file.getAbsolutePath());

            if (!config.isDisableBackup()) {
                File backupFile = new File(target, file.getName() + ".backup");
                FileUtils.moveFile(file, backupFile);
            } else {
                file.delete();
            }
        }
    }

    private List<File> reloadProperties(File parent, List<String> folders, boolean recursive,
            Locale locale, File target) throws IOException {
        List<File> actualTargetFiles = new ArrayList<File>();

        Properties baseProperties;
        Properties targetProperties;
        for (String path : folders) {
            File realFolder = new File(parent, path);

            Reader baseReader = null;
            Reader targetReader = null;
            Collection<File> files = FileUtils.listFiles(realFolder, new String[]{"properties"}, recursive);
            for (File file : files) {
                try {
                    File targetPropertiesFile = createTargetFile(file, target, locale);
                    actualTargetFiles.add(targetPropertiesFile);
                    if (targetPropertiesFile.exists() && !FileUtils.isFileNewer(file, targetPropertiesFile)) {
                        System.out.println("File was not modified: " + targetPropertiesFile.getName());
                        continue;
                    }

                    baseReader = new InputStreamReader(new FileInputStream(file), ENCODING);
                    baseProperties = new Properties();
                    baseProperties.load(baseReader);

                    targetProperties = new SortedProperties();
                    if (targetPropertiesFile.exists() && targetPropertiesFile.canRead()) {
                        targetReader = new InputStreamReader(new FileInputStream(targetPropertiesFile), ENCODING);
                        targetProperties.load(targetReader);
                    }

                    PropertiesStatistics stats = mergeProperties(baseProperties, targetProperties);
                    this.stats.increment(stats);

                    backupExistingAndSaveNewProperties(targetProperties, targetPropertiesFile);
                    System.out.println(targetPropertiesFile.getName() + ": " + stats);
                } finally {
                    IOUtils.closeQuietly(baseReader);
                    IOUtils.closeQuietly(targetReader);
                }
            }
        }

        return actualTargetFiles;
    }

    private File createTargetFile(File source, File targetDir, Locale locale) {
        String absolutePath = source.getParentFile().getAbsolutePath();
        int index = absolutePath.lastIndexOf("com/evolveum/midpoint");

        //create fileName as full qualified name (packages + properties file name and locale)
        String fileName = absolutePath.substring(index);//.replace("/", ".");
        if (StringUtils.isNotEmpty(fileName)) {
            fileName += "/";
        }
        fileName += source.getName().replace(".properties", "");
        fileName += "_" + locale;
        if ("utf-8".equals(ENCODING.toLowerCase())) {
            fileName += ".utf8";
        }
        fileName += ".properties";

        return new File(targetDir, fileName);
    }

    private PropertiesStatistics mergeProperties(Properties baseProperties, Properties targetProperties) {
        PropertiesStatistics stats = new PropertiesStatistics();

        Set<Object> keySet = baseProperties.keySet();
        for (Object key : keySet) {
            if (targetProperties.containsKey(key)) {
                continue;
            }

            targetProperties.setProperty((String) key, (String) baseProperties.get(key));
            stats.incrementAdded();
        }

        keySet = new HashSet<Object>();
        keySet.addAll(targetProperties.keySet());
        for (Object key : keySet) {
            if (baseProperties.containsKey(key)) {
                continue;
            }

            targetProperties.remove(key);
            stats.incrementDeleted();
        }

        return stats;
    }

    private void backupExistingAndSaveNewProperties(Properties properties, File target) throws IOException {
        if (target.exists()) {
            if (!config.isDisableBackup()) {
                File backupFile = new File(target.getParentFile(), target.getName() + ".backup");
                FileUtils.copyFile(target, backupFile);
            }
            target.delete();
        } else {
            System.out.println("Creating new file: " + target.getName());
        }

        File parent = target.getParentFile();
        if (!parent.exists() || !parent.isDirectory()) {
            FileUtils.forceMkdir(parent);
        }
        target.createNewFile();
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(target), Charset.forName(ENCODING));
            properties.store(writer, null);
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }
}
