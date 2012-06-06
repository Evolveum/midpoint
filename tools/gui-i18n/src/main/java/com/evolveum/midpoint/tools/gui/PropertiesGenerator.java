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

    public PropertiesGenerator(GeneratorConfiguration config) {
        Validate.notNull(config, "Generator configuration must not be null.");
        this.config = config;
    }

    public void generate() {
        long time = System.currentTimeMillis();
        System.out.println("Starting...");

        try {
            PropertiesStatistics stats;
            for (Locale locale : config.getLocalesToCheck()) {
                stats = new PropertiesStatistics();
                System.out.println("Loading existing properties for " + locale + ".");

                PropertiesStatistics partial = reloadProperties(config.getBaseFolder(),
                        config.getRecursiveFolderToCheck(), true, locale, config.getTargetFolder());
                stats.increment(partial);
                partial = reloadProperties(config.getBaseFolder(), config.getRecursiveFolderToCheck(),
                        false, locale, config.getTargetFolder());
                stats.increment(partial);

                System.out.println("Changes for locale " + locale + ": " + stats);
            }
        } catch (Exception ex) {
            System.out.println("Something went horribly wrong...");
            ex.printStackTrace();
        }

        System.out.println("Finished. Time: " + (System.currentTimeMillis() - time));
    }

    private PropertiesStatistics reloadProperties(File parent, List<String> folders, boolean recursive,
            Locale locale, File target) throws IOException {
        PropertiesStatistics fullStats = new PropertiesStatistics();

        Properties baseProperties;
        Properties targetProperties;
        for (String path : folders) {
            File realFolder = new File(parent, path);

            Reader baseReader = null;
            Reader targetReader = null;
            Collection<File> files = FileUtils.listFiles(realFolder, new String[]{"properties"}, recursive);
            for (File file : files) {
                try {
                    baseReader = new InputStreamReader(new FileInputStream(file), ENCODING);
                    baseProperties = new Properties();
                    baseProperties.load(baseReader);

                    String absolutePath = file.getParentFile().getAbsolutePath();
                    int index = absolutePath.lastIndexOf("com/evolveum/midpoint");

                    //create fileName as full qualified name (packages + properties file name and locale)
                    String fileName = absolutePath.substring(index);//.replace("/", ".");
                    if (StringUtils.isNotEmpty(fileName)) {
                        fileName += "/";
                    }
                    fileName += file.getName().replace(".properties", "");
                    fileName += "_" + locale;
                    if ("utf-8".equals(ENCODING.toLowerCase())) {
                        fileName += ".utf8";
                    }
                    fileName += ".properties";

                    targetProperties = new SortedProperties();
                    File targetPropertiesFile = new File(target, fileName);
                    if (targetPropertiesFile.exists() && targetPropertiesFile.canRead()) {
                        targetReader = new InputStreamReader(new FileInputStream(targetPropertiesFile), ENCODING);
                        targetProperties.load(targetReader);
                    }

                    PropertiesStatistics stats = mergeProperties(baseProperties, targetProperties);
                    backupExistingAndSaveNewProperties(targetProperties, targetPropertiesFile);

                    fullStats.incrementAdded(stats.getAdded());
                    fullStats.incrementDeleted(stats.getDeleted());

                    System.out.println(new File(target, fileName).getName() + ": " + stats);
                } finally {
                    IOUtils.closeQuietly(baseReader);
                    IOUtils.closeQuietly(targetReader);
                }
            }
        }

        return fullStats;
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

        keySet = targetProperties.keySet();
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
