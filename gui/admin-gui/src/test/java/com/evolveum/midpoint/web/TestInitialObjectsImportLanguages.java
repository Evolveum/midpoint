/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.init.PostInitialDataImport;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

/**
 * Tests that initial objects (built-in, from classpath) and post-initial objects
 * (from MP_HOME/post-initial-objects) can be provided not only in XML, but also
 * in JSON and YAML format.
 *
 * The built-in JSON/YAML objects are imported during Spring context initialization
 * (see test-languages directory under src/test/resources/initial-objects).
 */
@ContextConfiguration(locations = {"classpath:ctx-admin-gui-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestInitialObjectsImportLanguages extends AbstractModelIntegrationTest {

    // UUIDs are mandatory, as the repository (sqale) stores OIDs as UUIDs.
    private static final String JSON_INITIAL_OBJECT_OID = "a1b2c3d4-0001-4001-8001-000000000001";
    private static final String YAML_INITIAL_OBJECT_OID = "a1b2c3d4-0002-4002-8002-000000000002";

    private static final String POST_INITIAL_JSON_OBJECT_OID = "b2c3d4e5-0002-4002-8002-000000000012";
    private static final String POST_INITIAL_YAML_OBJECT_OID = "b2c3d4e5-0003-4003-8003-000000000013";
    private static final String POST_INITIAL_XML_OBJECT_OID = "b2c3d4e5-0001-4001-8001-000000000011";

    private static final String POST_INITIAL_XML_FILE = "005-test-post-initial-xml.xml";
    private static final String POST_INITIAL_JSON_FILE = "010-test-post-initial-json.json";
    private static final String POST_INITIAL_YAML_FILE = "020-test-post-initial-yaml.yaml";
    private static final String POST_INITIAL_UNSUPPORTED_FILE = "030-test-unsupported.txt";

    private static final String POST_INITIAL_XML_CONTENT = """
            <?xml version="1.0" encoding="UTF-8"?>
            <lookupTable oid="%s" xmlns="http://midpoint.evolveum.com/xml/ns/public/common/common-3">
                <name>Test Post-Initial XML Object</name>
                <description>Lookup table imported from an XML file during post-initial import.</description>
                <row>
                    <key>post-xml-key</key>
                    <label>Post-initial XML key</label>
                </row>
            </lookupTable>
            """.formatted(POST_INITIAL_XML_OBJECT_OID);

    private static final String POST_INITIAL_JSON_CONTENT = """
            {
              "@ns" : "http://midpoint.evolveum.com/xml/ns/public/common/common-3",
              "lookupTable" : {
                "oid" : "%s",
                "name" : "Test Post-Initial JSON Object",
                "description" : "Lookup table imported from a JSON file during post-initial import.",
                "row" : [ {
                  "key" : "post-json-key",
                  "label" : "Post-initial JSON key"
                } ]
              }
            }
            """.formatted(POST_INITIAL_JSON_OBJECT_OID);

    private static final String POST_INITIAL_YAML_CONTENT = """
            ---
            '@ns': "http://midpoint.evolveum.com/xml/ns/public/common/common-3"
            lookupTable:
              oid: "%s"
              name: "Test Post-Initial YAML Object"
              description: "Lookup table imported from a YAML file during post-initial import."
              row:
              - key: "post-yaml-key"
                label: "Post-initial YAML key"
            """.formatted(POST_INITIAL_YAML_OBJECT_OID);

    @Autowired private PostInitialDataImport postInitialDataImport;
    @Autowired private MidpointConfiguration configuration;

    /** The built-in JSON and YAML initial objects must be imported during context initialization. */
    @Test
    public void test001BuiltInJsonAndYamlInitialObjectsImported() throws Exception {
        login(USER_ADMINISTRATOR_USERNAME);

        assertInitialObjectImported(JSON_INITIAL_OBJECT_OID, "Test JSON Initial Object");
        assertInitialObjectImported(YAML_INITIAL_OBJECT_OID, "Test YAML Initial Object");
    }

    /** JSON, YAML and unsupported files in MP_HOME/post-initial-objects are handled as documented. */
    @Test
    public void test002PostInitialImportJsonAndYaml() throws Exception {
        // GIVEN - midpoint home with post-initial-objects directory
        String midpointHome = configuration.getMidpointHome();
        File postInitialObjectsDir = new File(midpointHome, "post-initial-objects");
        deleteFiles(postInitialObjectsDir);
        postInitialObjectsDir.mkdirs();

        File xmlFile = new File(postInitialObjectsDir, POST_INITIAL_XML_FILE);
        File jsonFile = new File(postInitialObjectsDir, POST_INITIAL_JSON_FILE);
        File yamlFile = new File(postInitialObjectsDir, POST_INITIAL_YAML_FILE);
        File unsupportedFile = new File(postInitialObjectsDir, POST_INITIAL_UNSUPPORTED_FILE);
        Files.writeString(xmlFile.toPath(), POST_INITIAL_XML_CONTENT, StandardCharsets.UTF_8);
        Files.writeString(jsonFile.toPath(), POST_INITIAL_JSON_CONTENT, StandardCharsets.UTF_8);
        Files.writeString(yamlFile.toPath(), POST_INITIAL_YAML_CONTENT, StandardCharsets.UTF_8);
        Files.writeString(unsupportedFile.toPath(), "this is not an object", StandardCharsets.UTF_8);

        // WHEN - post-initial import runs
        postInitialDataImport.init();

        // THEN - XML, JSON and YAML objects are imported and the files are marked as done
        login(USER_ADMINISTRATOR_USERNAME);
        assertInitialObjectImported(POST_INITIAL_XML_OBJECT_OID, "Test Post-Initial XML Object");
        assertInitialObjectImported(POST_INITIAL_JSON_OBJECT_OID, "Test Post-Initial JSON Object");
        assertInitialObjectImported(POST_INITIAL_YAML_OBJECT_OID, "Test Post-Initial YAML Object");
        assertTrue("XML file should be renamed with .done suffix",
                new File(midpointHome + "post-initial-objects/" + POST_INITIAL_XML_FILE + ".done").exists());
        assertTrue("JSON file should be renamed with .done suffix",
                new File(midpointHome + "post-initial-objects/" + POST_INITIAL_JSON_FILE + ".done").exists());
        assertTrue("YAML file should be renamed with .done suffix",
                new File(midpointHome + "post-initial-objects/" + POST_INITIAL_YAML_FILE + ".done").exists());

        // AND - unsupported file is left untouched
        assertTrue("Unsupported file should be left untouched", unsupportedFile.exists());
        assertFalse("Unsupported file should not be marked as done",
                new File(postInitialObjectsDir, POST_INITIAL_UNSUPPORTED_FILE + ".done").exists());

        // CLEANUP - do not leave files that would be considered on the next run
        deleteFiles(postInitialObjectsDir);
    }

    private void assertInitialObjectImported(String oid, String expectedName) throws Exception {
        Task task = createPlainTask("getObject");
        OperationResult result = new OperationResult("getObject");
        PrismObject<LookupTableType> object = modelService.getObject(
                LookupTableType.class, oid, null, task, result);
        assertNotNull("Initial object " + oid + " was not imported", object);
        assertEquals("Initial object " + oid + " has unexpected name",
                expectedName, object.asObjectable().getName().getOrig());
    }

    private void deleteFiles(File dir) throws IOException {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!file.delete()) {
                        throw new IOException("Unable to delete " + file);
                    }
                }
            }
        }
    }
}
