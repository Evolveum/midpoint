/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.labs;

import com.evolveum.midpoint.testing.schrodinger.AbstractSchrodingerTest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author skublik
 */

public class AbstractLabTest extends AbstractSchrodingerTest {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractLabTest.class);

    protected static final String LAB_DIRECTORY = "./src/test/resources/labs/";
    protected static final String LAB_SOURCES_DIRECTORY = LAB_DIRECTORY + "sources/";
    protected static final String LAB_FUNDAMENTAL_DIRECTORY = LAB_DIRECTORY + "fundamental/";
    protected static final String LAB_ADVANCED_DIRECTORY = LAB_DIRECTORY + "advanced/";

    protected static final File EXTENSION_SCHEMA_FILE = new File(LAB_DIRECTORY +"schema/extension-example.xsd");
    protected static final File CSV_1_SOURCE_FILE = new File(LAB_SOURCES_DIRECTORY + "csv-1.csv");
    protected static final File CSV_1_SOURCE_FILE_7_3 = new File(LAB_SOURCES_DIRECTORY + "csv-1-7-3.csv");
    protected static final File CSV_2_SOURCE_FILE = new File(LAB_SOURCES_DIRECTORY + "csv-2.csv");
    protected static final File CSV_3_SOURCE_FILE = new File(LAB_SOURCES_DIRECTORY + "csv-3.csv");
    protected static final File HR_SOURCE_FILE = new File(LAB_SOURCES_DIRECTORY + "source.csv");
    protected static final File HR_SOURCE_FILE_7_4_PART_1 = new File(LAB_SOURCES_DIRECTORY + "source-7-4-part-1.csv");
    protected static final File HR_SOURCE_FILE_7_4_PART_2 = new File(LAB_SOURCES_DIRECTORY + "source-7-4-part-2.csv");
    protected static final File HR_SOURCE_FILE_7_4_PART_3 = new File(LAB_SOURCES_DIRECTORY + "source-7-4-part-3.csv");
    protected static final File HR_SOURCE_FILE_7_4_PART_4 = new File(LAB_SOURCES_DIRECTORY + "source-7-4-part-4.csv");
    protected static final File HR_SOURCE_FILE_10_1 = new File(LAB_SOURCES_DIRECTORY + "source-10-1.csv");
    protected static final File HR_SOURCE_FILE_10_2_PART1 = new File(LAB_SOURCES_DIRECTORY + "source-10-2-part1.csv");
    protected static final File HR_SOURCE_FILE_10_2_PART2 = new File(LAB_SOURCES_DIRECTORY + "source-10-2-part2.csv");
    protected static final File HR_SOURCE_FILE_10_2_PART3 = new File(LAB_SOURCES_DIRECTORY + "source-10-2-part3.csv");
    protected static final File HR_SOURCE_FILE_11_1 = new File(LAB_SOURCES_DIRECTORY + "source-11-1.csv");

    protected static final String DIRECTORY_CURRENT_TEST = "labTests";
    protected static final String EXTENSION_SCHEMA_NAME = "extension-example.xsd";
    protected static final String CSV_1_FILE_SOURCE_NAME = "csv-1.csv";
    protected static final String CSV_1_RESOURCE_NAME = "CSV-1 (Document Access)";
    protected static final String CSV_1_RESOURCE_OID = "10000000-9999-9999-0000-a000ff000002";
    protected static final String CSV_2_FILE_SOURCE_NAME = "csv-2.csv";
    protected static final String CSV_2_RESOURCE_NAME = "CSV-2 (Canteen Ordering System)";
    protected static final String CSV_2_RESOURCE_OID = "10000000-9999-9999-0000-a000ff000003";
    protected static final String CSV_3_FILE_SOURCE_NAME = "csv-3.csv";
    protected static final String CSV_3_RESOURCE_NAME = "CSV-3 (LDAP)";
    protected static final String CSV_3_RESOURCE_OID = "10000000-9999-9999-0000-a000ff000004";
    protected static final String HR_FILE_SOURCE_NAME = "source.csv";
    protected static final String HR_RESOURCE_NAME = "ExAmPLE, Inc. HR Source";
    protected static final String NOTIFICATION_FILE_NAME = "notification.txt";

    protected static final String PASSWORD_ATTRIBUTE_NAME = "User password attribute name";
    protected static final String UNIQUE_ATTRIBUTE_NAME = "Unique attribute name";

    protected static final String CSV_1_UNIQUE_ATTRIBUTE_NAME = "login";
    protected static final String CSV_1_PASSWORD_ATTRIBUTE_NAME = "password";
    protected static final String CSV_1_ACCOUNT_OBJECT_CLASS_LINK = "AccountObjectClass (Default Account)";
    protected static final String ARCHETYPE_EMPLOYEE_PLURAL_LABEL = "Employees";

    protected static final List<String> CSV_1_RESOURCE_ATTRIBUTES = Arrays.asList("login", "lname", "groups", "enumber", "phone", "dep", "fname", "dis");

    protected static File csv1TargetFile;
    protected static File csv2TargetFile;
    protected static File csv3TargetFile;
    protected static File hrTargetFile;
    protected static File notificationFile;

    protected File getTestTargetDir() throws IOException {
        if (testTargetDir == null) {
            initTestDirectory(DIRECTORY_CURRENT_TEST, false);
        }
        return testTargetDir;
    }

}
