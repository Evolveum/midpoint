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
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.tools.ninja;

import org.apache.commons.cli.*;
import org.apache.commons.lang.StringUtils;

/**
 * @author lazyman
 */
public class Main {

    public static void main(String[] args) {
        Options options = new Options();

        Option help = new Option("h", "help", false,
                "Prints this help.");
        options.addOption(help);
        Option validate = new Option("v", "validate", true,
                "Validate SQL database by repository context loading and Hibernate2DDL validate. " +
                        "Validation is against <midpoint.home> folder path provided by this option.");
        options.addOption(validate);
        Option create = new Option("c", "create", true,
                "Create tables with sql script provided by this option.");
        options.addOption(create);
        Option importOp = new Option("i", "import", true,
                "Import objects from XML file provided by this option.");
        options.addOption(importOp);

        try {
            CommandLineParser parser = new GnuParser();
            CommandLine line = parser.parse(options, args);
            if (line.getOptions().length == 0 || line.hasOption(help.getOpt())) {
                printHelp(options);
                return;
            }

            if (!validate(line, validate, options)) {
                return;
            }

            if (line.hasOption(create.getOpt())) {
                String path = line.getOptionValue(create.getOpt());
                ImportDDL ddl = null;//new ImportDDL(path);
                if (!ddl.execute()) {
                    System.out.println("DLL import was unsuccessful, skipping other steps.");
                    return;
                }

                if (!validate(line, validate, options)) {
                    return;
                }
            }

            if (line.hasOption(importOp.getOpt())) {
                String path = line.getOptionValue(importOp.getOpt());
                ImportObjects objects = new ImportObjects(path);
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

    private static boolean validate(CommandLine line, Option validate, Options options) {
        if (!line.hasOption(validate.getOpt())) {
            return true;
        }

        String path = line.getOptionValue(validate.getOpt());
        if (StringUtils.isEmpty(path)) {
            System.out.println("Path for <midpoint.home> folder was not defined.");
            printHelp(options);
            return false;
        }

        RepoValidator validator = new RepoValidator(path);
        boolean valid = validator.execute();
        if (!valid) {
            System.out.println("Validation was unsuccessful, skipping other steps.");
        }

        return valid;
    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Main [-c <arg>][-h][-i <arg>][-v <arg>]", options);
    }
}
