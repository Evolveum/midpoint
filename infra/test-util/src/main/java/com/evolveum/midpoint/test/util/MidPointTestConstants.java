/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.util;

import java.io.File;

public class MidPointTestConstants {

    public static final String TEST_RESOURCES_PATH = "src/test/resources";
    public static final File TEST_RESOURCES_DIR = new File (TEST_RESOURCES_PATH);
    public static final File OBJECTS_DIR = new File(TEST_RESOURCES_DIR, "objects");
    public static final String TARGET_DIR_PATH = "target";

    // copied from TestProtector - unfortunately these values are needed both in prism and in other modules
    public static final String KEYSTORE_PATH = "src/test/resources/keystore.jceks";
    public static final String KEYSTORE_PASSWORD = "changeit";


}
