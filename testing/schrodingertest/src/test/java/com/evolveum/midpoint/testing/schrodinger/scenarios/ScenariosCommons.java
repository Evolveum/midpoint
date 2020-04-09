/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.scenarios;

import java.io.File;

public class ScenariosCommons {

    protected static final File CSV_SOURCE_FILE = new File("./src/test/resources/midpoint-groups-authoritative.csv");
    protected static final String CSV_SOURCE_OLDVALUE = "target/midpoint.csv";
    protected static final File CSV_INITIAL_SOURCE_FILE = new File("./src/test/resources/midpoint-groups-authoritative-initial.csv");
    protected static final File CSV_UPDATED_SOURCE_FILE = new File("./src/test/resources/midpoint-groups-authoritative-updated.csv");
    protected static final File RESOURCE_CSV_GROUPS_AUTHORITATIVE_FILE = new File("./src/test/resources/resource-csv-groups-authoritative.xml");
    protected static final File USER_TEST_RAPHAEL_FILE = new File("./src/test/resources/user-raphael.xml");

    protected static final String RESOURCE_CSV_GROUPS_AUTHORITATIVE_NAME = "CSV (target with groups) authoritative";
    protected static final String CSV_RESOURCE_NAME= "Test CSV: username";

    protected static final String TEST_USER_DON_NAME= "donatello";
    protected static final String TEST_USER_PROTECTED_NAME= "chief";
    protected static final String TEST_USER_RAPHAEL_NAME = "raphael";

    protected static final String CSV_RESOURCE_ATTR_FILE_PATH= "File path";

}
