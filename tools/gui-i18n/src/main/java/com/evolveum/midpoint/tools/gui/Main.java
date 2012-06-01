package com.evolveum.midpoint.tools.gui;/*
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

import org.apache.commons.cli.*;

import java.io.File;
import java.util.Locale;

/**
 * @author lazyman
 */
public class Main {

//    private String propertiesBaseName = "Messages";
//    private String propertiesLocaleDelimiter = "_";
//    private File targetFolder;
//
//    private File baseFolder;
//
//    private List<Locale> localesToCheck;
//    private List<String> recursiveFolderToCheck;
//    private List<String> nonRecursiveFolderToCheck;

    public static void main(String[] args) throws Exception {
        Options options = new Options();

        Option propertiesBaseName = new Option("p", "baseName", true,
                "Base name for localization files. Default is 'Messages'.");
        options.addOption(propertiesBaseName);
        Option propertiesLocaleDelimiter = new Option("d", "delimiter", true,
                "Delimiter for locale name in properties file. Default is '_' (underscore).");
        options.addOption(propertiesLocaleDelimiter);
        Option targetFolder = new Option("t", "targetFolder", true,
                "Target folder where properties file is generated.");
        targetFolder.setRequired(true);
        options.addOption(targetFolder);
        Option baseFolder = new Option("b", "baseFolder", true,
                "Base folder used for properties files searching.");
        baseFolder.setRequired(true);
        options.addOption(baseFolder);
        Option localesToCheck = new Option("l", "locale", true,
                "Locales to check.");
        localesToCheck.setRequired(true);
        options.addOption(localesToCheck);
        Option recursiveFolderToCheck = new Option("r", "folderRecursive", true,
                "Folder used for recursive search for properties files.");
        options.addOption(recursiveFolderToCheck);
        Option nonRecursiveFolderToCheck = new Option("n", "folderNonRecursive", true,
                "Folder used for non recursive search for properties files.");
        options.addOption(nonRecursiveFolderToCheck);
        Option help = new Option("h", "help", false, "Print this help.");
        options.addOption(help);

        try {
            CommandLineParser parser = new GnuParser();
            CommandLine line = parser.parse(options, args);
            if (line.hasOption(help.getOpt())) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("Main", options);
                return;
            }

            //todo create config from options...
            GeneratorConfiguration config = new GeneratorConfiguration();
            config.setBaseFolder(new File("/home/lazyman/Work/evolveum/midpoint/trunk/gui/admin-gui-v2/src/main"));
            config.getRecursiveFolderToCheck().add("/java/com/evolveum/midpoint/web");
            config.getLocalesToCheck().add(new Locale("sk", "SK"));
            config.getLocalesToCheck().add(new Locale("en", "US"));
            config.setTargetFolder(new File("./tools/gui-i18n/src/main/java"));

            PropertiesGenerator generator = new PropertiesGenerator(config);
            generator.generate();
        } catch (ParseException ex) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Main", options);
        } catch (Exception ex) {
            System.out.println("Something is broken.");
            ex.printStackTrace();
        }
    }

    public static Locale getLocaleFromString(String localeString) {
        if (localeString == null) {
            return null;
        }
        localeString = localeString.trim();
        if (localeString.toLowerCase().equals("default")) {
            return Locale.getDefault();
        }

        // Extract language
        int languageIndex = localeString.indexOf('_');
        String language = null;
        if (languageIndex == -1) {
            // No further "_" so is "{language}" only
            return new Locale(localeString, "");
        } else {
            language = localeString.substring(0, languageIndex);
        }

        // Extract country
        int countryIndex = localeString.indexOf('_', languageIndex + 1);
        String country = null;
        if (countryIndex == -1) {
            // No further "_" so is "{language}_{country}"
            country = localeString.substring(languageIndex + 1);
            return new Locale(language, country);
        } else {
            // Assume all remaining is the variant so is "{language}_{country}_{variant}"
            country = localeString.substring(languageIndex + 1, countryIndex);
            String variant = localeString.substring(countryIndex + 1);
            return new Locale(language, country, variant);
        }
    }
}
