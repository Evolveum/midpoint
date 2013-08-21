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

package com.evolveum.midpoint.tools.ninja;

import org.apache.commons.cli.*;

/**
 * @author lazyman
 */
public class Main {

    public static final Option help = new Option("h", "help", false, "Prints this help.");
    public static final Option validate = new Option("v", "validate", false,
            "Validate SQL database by repository context loading and Hibernate2DDL validate. " +
                    "Validation is against <midpoint.home> folder.");
    public static final Option create = new Option("c", "create", true,
            "Create tables with sql script provided by this option.");
    public static final Option importOp = new Option("i", "import", true,
            "Import objects from XML file provided by this option.");
    public static final Option driver = new Option("d", "driver", true, "JDBC driver class");
    public static final Option url = new Option("u", "url", true, "JDBC url");
    public static final Option username = new Option("U", "username", true, "JDBC username");
    public static final Option password = new Option("p", "password", true, "JDBC password");
    public static final Option Password = new Option("P", "password-prompt", false, "JDBC password (prompt)");
    public static final Option exportOp = new Option("e", "export", true,
            "Export objects to XML file provided by this option.");


    public static void main(String[] args) {
        Options options = new Options();
        options.addOption(help);
        options.addOption(validate);
        options.addOption(create);
        options.addOption(importOp);
        options.addOption(exportOp);
        options.addOption(driver);
        options.addOption(url);
        options.addOption(username);
        options.addOption(password);
        options.addOption(Password);

        try {
            CommandLineParser parser = new GnuParser();
            CommandLine line = parser.parse(options, args);
            if (line.getOptions().length == 0 || line.hasOption(help.getOpt())) {
                printHelp(options);
                return;
            }

            //repository validation, if proper option is present
            boolean valid = validate(line, options);
            //import DDL, if proper option is present
            if (line.hasOption(create.getOpt())) {
                ImportDDL ddl = new ImportDDL(createDDLConfig(line));
                if (!ddl.execute()) {
                    System.out.println("DLL import was unsuccessful, skipping other steps.");
                    return;
                }

                //repository validation after DDL import, if proper option is present
                valid = validate(line, options);
            }

            //import objects, only if repository validation didn't fail (in case it was tested)
            if (valid && line.hasOption(importOp.getOpt())) {
                String path = line.getOptionValue(importOp.getOpt());
                ImportObjects objects = new ImportObjects(path);
                objects.execute();
            }

            if (valid && line.hasOption(exportOp.getOpt())) {
                String path = line.getOptionValue(exportOp.getOpt());
                ExportObjects objects = new ExportObjects(path);
                objects.execute();
            }
        } catch (ParseException ex) {
            System.out.println("Error: " + ex.getMessage());
            printHelp(options);
        } catch (Exception ex) {
            System.out.println("Exception occurred, reason: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private static ImportDDLConfig createDDLConfig(CommandLine line) {
        ImportDDLConfig config = new ImportDDLConfig();
        config.setDriver(line.getOptionValue(driver.getOpt()));
        config.setUrl(line.getOptionValue(url.getOpt()));
        config.setUsername(line.getOptionValue(username.getOpt()));
        config.setPassword(line.getOptionValue(password.getOpt()));
        config.setPromptForPassword(line.hasOption(Password.getOpt()));

        config.setFilePath(line.getOptionValue(create.getOpt()));

        return config;
    }

    private static boolean validate(CommandLine line, Options options) {
        if (!line.hasOption(validate.getOpt())) {
            System.out.println("Skipping repository validation.");
            return true;
        }

        RepoValidator validator = new RepoValidator();
        boolean valid = validator.execute();
        if (!valid) {
            System.out.println("Validation was unsuccessful, skipping other steps.");
        }

        return valid;
    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Main [-c <arg>][-h][-i <arg>][-e <arg>][-v][-d <arg>][-u <arg>][-U <arg>][-p <arg>][-P]",
                options);
    }
}
