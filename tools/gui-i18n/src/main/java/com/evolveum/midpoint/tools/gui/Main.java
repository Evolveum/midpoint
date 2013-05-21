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

import org.apache.commons.cli.*;

import java.io.File;
import java.util.Locale;

/**
 * @author lazyman
 */
public class Main {

    public static void main(String[] args) throws Exception {
        Options options = new Options();

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
        Option disableBackup = new Option("db", "disableBackup", false,
                "Disable backuping property files.");
        options.addOption(disableBackup);

        try {
            CommandLineParser parser = new GnuParser();
            CommandLine line = parser.parse(options, args);
            if (line.hasOption(help.getOpt())) {
                printHelp(options);
                return;
            }

            if (!line.hasOption(recursiveFolderToCheck.getOpt())
                    && !line.hasOption(nonRecursiveFolderToCheck.getOpt())) {
                printHelp(options);
                return;
            }

            GeneratorConfiguration config = new GeneratorConfiguration();
            if (line.hasOption(baseFolder.getOpt())) {
                config.setBaseFolder(new File(line.getOptionValue(baseFolder.getOpt())));
            }
            if (line.hasOption(targetFolder.getOpt())) {
                config.setTargetFolder(new File(line.getOptionValue(targetFolder.getOpt())));
            }
            if (line.hasOption(propertiesLocaleDelimiter.getOpt())) {
                config.setPropertiesLocaleDelimiter(line.getOptionValue(propertiesLocaleDelimiter.getOpt()));
            }
            if (line.hasOption(recursiveFolderToCheck.getOpt())) {
                String[] recursives = line.getOptionValues(recursiveFolderToCheck.getOpt());
                if (recursives != null && recursives.length > 0) {
                    for (String recursive : recursives) {
                        config.getRecursiveFolderToCheck().add(recursive);
                    }
                }
            }
            if (line.hasOption(nonRecursiveFolderToCheck.getOpt())) {
                String[] nonRecursives = line.getOptionValues(nonRecursiveFolderToCheck.getOpt());
                if (nonRecursives != null && nonRecursives.length > 0) {
                    for (String nonRecursive : nonRecursives) {
                        config.getNonRecursiveFolderToCheck().add(nonRecursive);
                    }
                }
            }

            if (line.hasOption(localesToCheck.getOpt())) {
                String[] locales = line.getOptionValues(localesToCheck.getOpt());
                for (String locale : locales) {
                    config.getLocalesToCheck().add(getLocaleFromString(locale));
                }
            }

            if (line.hasOption(disableBackup.getOpt())) {
                config.setDisableBackup(true);
            }

            PropertiesGenerator generator = new PropertiesGenerator(config);
            generator.generate();
        } catch (ParseException ex) {
            System.out.println("Error: " + ex.getMessage());
            printHelp(options);
        } catch (Exception ex) {
            System.out.println("Something is broken.");
            ex.printStackTrace();
        }
    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Main", options);
    }

    private static Locale getLocaleFromString(String localeString) {
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
