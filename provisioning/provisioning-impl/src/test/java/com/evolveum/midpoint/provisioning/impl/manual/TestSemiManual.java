/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.manual;

import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestSemiManual extends AbstractManualResourceTest {

    private static final File CSV_SOURCE_FILE = new File(TEST_DIR, "semi-manual.csv");
    private static final File CSV_TARGET_FILE = new File("target/semi-manual.csv");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        resource = addResource(initResult);
        resourceType = resource.asObjectable();

        FileUtils.copyFile(CSV_SOURCE_FILE, CSV_TARGET_FILE);
    }

    @Override
    protected File getResourceFile() {
        return RESOURCE_SEMI_MANUAL_FILE;
    }

    @Override
    protected boolean supportsBackingStore() {
        return true;
    }

    protected PrismObject<ResourceType> addResource(OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, EncryptionException, IOException {
        PrismObject<ResourceType> resource = prismContext.parseObject(getResourceFile());
        fillInConnectorRef(resource, MANUAL_CONNECTOR_TYPE, result);
        fillInAdditionalConnectorRef(resource, "csv", CSV_CONNECTOR_TYPE, result);
        CryptoUtil.encryptValues(protector, resource);
        display("Adding resource ", resource);
        String oid = repositoryService.addObject(resource, null, result);
        resource.setOid(oid);
        return resource;
    }

    @Override
    protected void assertResourceSchemaBeforeTest(Element resourceXsdSchemaElementBefore) {
        AssertJUnit.assertNull("Resource schema sneaked in before test connection", resourceXsdSchemaElementBefore);
    }

    @Override
    protected int getNumberOfAccountAttributeDefinitions() {
        return 5;
    }

    @Override
    protected void backingStoreAddWill() throws IOException {
        appendToCsv(new String[] { ACCOUNT_WILL_USERNAME, ACCOUNT_WILL_FULLNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL, "", "false", ACCOUNT_WILL_PASSWORD_OLD });
    }

    @Override
    protected void backingStoreUpdateWill(String newFullName, ActivationStatusType newAdministrativeStatus, String password) throws IOException {
        String disabled;
        if (newAdministrativeStatus == ActivationStatusType.ENABLED) {
            disabled = "false";
        } else {
            disabled = "true";
        }
        replaceInCsv(new String[] { ACCOUNT_WILL_USERNAME, newFullName, ACCOUNT_WILL_DESCRIPTION_MANUAL, "", disabled, password });
    }

    private void appendToCsv(String[] data) throws IOException {
        String line = formatCsvLine(data) + "\n";
        Files.write(Paths.get(CSV_TARGET_FILE.getPath()), line.getBytes(), StandardOpenOption.APPEND);
    }

    private void replaceInCsv(String[] data) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(CSV_TARGET_FILE.getPath()));
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String[] cols = line.split(",");
            if (cols[0].matches("\"" + data[0] + "\"")) {
                lines.set(i, formatCsvLine(data));
            }
        }
        Files.write(Paths.get(CSV_TARGET_FILE.getPath()), lines, StandardOpenOption.WRITE);
    }

    private String formatCsvLine(String[] data) {
        return Arrays.stream(data).map(s -> "\"" + s + "\"").collect(Collectors.joining(","));
    }

    @Override
    protected void assertShadowPassword(PrismObject<ShadowType> shadow) {
        // CSV password is readable
        PrismProperty<PolyStringType> passValProp = shadow.findProperty(SchemaConstants.PATH_PASSWORD_VALUE);
        assertNotNull("No password value property in " + shadow + ": " + passValProp, passValProp);
    }
}
