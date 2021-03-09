/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.labs.advanced;

import com.evolveum.midpoint.testing.schrodinger.labs.AbstractLabTest;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
/**
 * @author honchar
 */
public class AbstractAdvancedLabTest extends AbstractLabTest {

    protected static final File EXTENSION_SCHEMA_FILE = new File(LAB_ADVANCED_DIRECTORY + "schema/extension-example.xsd");
    protected static final String POST_INITIAL_OBJECTS_DIR = LAB_ADVANCED_DIRECTORY + "post-initial-objects";

    @BeforeClass(alwaysRun = true, dependsOnMethods = { "springTestContextBeforeTestClass" })
    @Override
    protected void springTestContextPrepareTestInstance() throws Exception {
        String home = System.getProperty("midpoint.home");
        File mpHomeDir = new File(home);
        if (!mpHomeDir.exists()) {
            super.springTestContextPrepareTestInstance();
        }

        File schemaDir = new File(home, "schema");
        if (!schemaDir.mkdir()) {
            if (schemaDir.exists()) {
                FileUtils.cleanDirectory(schemaDir);
            } else {
                throw new IOException("Creation of directory \"" + schemaDir.getAbsolutePath() + "\" unsuccessful");
            }
        }
        File schemaFile = new File(schemaDir, EXTENSION_SCHEMA_NAME);
        FileUtils.copyFile(EXTENSION_SCHEMA_FILE, schemaFile);

        File postInitObjectsDir = new File(home, "post-initial-objects");
        if (!postInitObjectsDir.mkdir()) {
            if (postInitObjectsDir.exists()) {
                FileUtils.cleanDirectory(postInitObjectsDir);
            } else {
                throw new IOException("Creation of directory \"" + postInitObjectsDir.getAbsolutePath() + "\" unsuccessful");
            }
        }
        File postInitObjectsSourceDir = new File(POST_INITIAL_OBJECTS_DIR);
        File[] objList = postInitObjectsSourceDir.listFiles();
        Arrays.sort(objList);
        for (File postInitFile : objList) {
            File objFile = new File(postInitObjectsDir, postInitFile.getName());
            FileUtils.copyFile(postInitFile, objFile);
        }

        super.springTestContextPrepareTestInstance();
    }


}
