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
    protected static final String LAB_OBJECTS_DIRECTORY = LAB_DIRECTORY + "objects/";
    protected static final String LAB_SOURCES_DIRECTORY = LAB_DIRECTORY + "sources/";

    protected static final File EXTENSION_SCHEMA_FILE = new File(LAB_DIRECTORY +"schema/extension-example.xsd");
    protected static final File CSV_1_SIMPLE_RESOURCE_FILE = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-1-document-access-simple.xml");
    protected static final File CSV_1_RESOURCE_FILE = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-1-document-access.xml");
    protected static final File CSV_1_SOURCE_FILE = new File(LAB_SOURCES_DIRECTORY + "csv-1.csv");
    protected static final File CSV_1_SOURCE_FILE_7_3 = new File(LAB_SOURCES_DIRECTORY + "csv-1-7-3.csv");
    protected static final File NUMERIC_PIN_FIRST_NONZERO_POLICY_FILE = new File(LAB_OBJECTS_DIRECTORY + "valuePolicies/numeric-pin-first-nonzero-policy.xml");
    protected static final File CSV_2_RESOURCE_FILE = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-2-canteen.xml");
    protected static final File CSV_2_SOURCE_FILE = new File(LAB_SOURCES_DIRECTORY + "csv-2.csv");
    protected static final File CSV_3_RESOURCE_FILE = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-3-ldap.xml");
    protected static final File CSV_3_SOURCE_FILE = new File(LAB_SOURCES_DIRECTORY + "csv-3.csv");
    protected static final File HR_NO_EXTENSION_RESOURCE_FILE = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-hr-noextension.xml");
    protected static final File HR_RESOURCE_FILE_8_1 = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-hr.xml");
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
    protected static final File KIRK_USER_FILE = new File("./src/test/resources/labs/objects/users/kirk-user.xml");
    protected static final File KIRK_USER_TIBERIUS_FILE = new File("./src/test/resources/labs/objects/users/kirk-tiberius-user.xml");
    protected static final File ARCHETYPE_EMPLOYEE_FILE = new File(LAB_OBJECTS_DIRECTORY + "archetypes/archetype-employee.xml");
    protected static final File INTERNAL_EMPLOYEE_ROLE_FILE = new File(LAB_OBJECTS_DIRECTORY + "roles/role-internal-employee.xml");
    protected static final File SYSTEM_CONFIGURATION_FILE_5_7 = new File(LAB_OBJECTS_DIRECTORY + "systemConfiguration/system-configuration-5-7.xml");
    protected static final File CSV_2_RESOURCE_FILE_5_5 = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-2-canteen-5-5.xml");
    protected static final File SECRET_I_ROLE_FILE = new File(LAB_OBJECTS_DIRECTORY + "roles/role-secret-i.xml");
    protected static final File SECRET_II_ROLE_FILE = new File(LAB_OBJECTS_DIRECTORY + "roles/role-secret-ii.xml");
    protected static final File HR_SYNCHRONIZATION_TASK_FILE = new File(LAB_OBJECTS_DIRECTORY + "tasks/task-opendj-livesync-full.xml");
    protected static final File ARCHETYPE_EXTERNAL_FILE = new File(LAB_OBJECTS_DIRECTORY + "archetypes/archetype-external.xml");
    protected static final File INCOGNITO_ROLE_FILE = new File(LAB_OBJECTS_DIRECTORY + "roles/role-incognito.xml");
    protected static final File ORG_EXAMPLE_FILE = new File(LAB_OBJECTS_DIRECTORY + "org/org-example.xml");
    protected static final File ARCHETYPE_ORG_COMPANY_FILE = new File(LAB_OBJECTS_DIRECTORY + "archetypes/archetype-org-company.xml");
    protected static final File ARCHETYPE_ORG_FUNCTIONAL_FILE = new File(LAB_OBJECTS_DIRECTORY + "archetypes/archetype-org-functional.xml");
    protected static final File ARCHETYPE_ORG_GROUP_LIST_FILE = new File(LAB_OBJECTS_DIRECTORY + "archetypes/archetype-org-group-list.xml");
    protected static final File ARCHETYPE_ORG_GROUP_FILE = new File(LAB_OBJECTS_DIRECTORY + "archetypes/archetype-org-group.xml");
    protected static final File OBJECT_TEMPLATE_USER_FILE = new File(LAB_OBJECTS_DIRECTORY + "objectTemplate/object-template-example-user.xml");
    protected static final File ORG_SECRET_OPS_FILE = new File(LAB_OBJECTS_DIRECTORY + "org/org-secret-ops.xml");
    protected static final File CSV_3_RESOURCE_FILE_8_1 = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-3-ldap-8-1.xml");

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
