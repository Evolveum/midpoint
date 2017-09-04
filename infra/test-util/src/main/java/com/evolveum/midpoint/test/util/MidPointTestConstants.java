package com.evolveum.midpoint.test.util;

import java.io.File;

public class MidPointTestConstants {

	public static final String TEST_RESOURCES_PATH = "src/test/resources";
	public static File TEST_RESOURCES_DIR = new File (TEST_RESOURCES_PATH);
	public static File OBJECTS_DIR = new File(TEST_RESOURCES_DIR, "objects");

    // copied from TestProtector - unfortunately these values are needed both in prism and in other modules
    public static final String KEYSTORE_PATH = "src/test/resources/keystore.jceks";
    public static final String KEYSTORE_PASSWORD = "changeit";


}
