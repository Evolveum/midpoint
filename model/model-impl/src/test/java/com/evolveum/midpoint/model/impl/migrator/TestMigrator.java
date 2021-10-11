/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.migrator;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.AbstractSpringTest;
import com.evolveum.midpoint.tools.testng.UnusedTestElement;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import difflib.DiffUtils;
import difflib.Patch;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.testng.AssertJUnit.*;

@UnusedTestElement("reason unknown, but it FAILS")
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestMigrator extends AbstractSpringTest {

    @Autowired PrismContext prismContext;
    @Autowired Migrator migrator;

    public static final File TEST_DIR = new File("src/test/resources/migrator");
    private static final File TEST_DIR_BEFORE = new File(TEST_DIR, "before");
    private static final File TEST_DIR_AFTER = new File(TEST_DIR, "after");

    @Test
    public void testMigrateUserTemplate() throws Exception {
        for (File beforeFile: TEST_DIR_BEFORE.listFiles()) {
            String beforeName = beforeFile.getName();
            if (!beforeName.endsWith(".xml")) {
                continue;
            }
            File afterFile = new File(TEST_DIR_AFTER, beforeName);

            assertSimpleMigration(beforeFile, afterFile);
        }
    }

    @Test
    public void testUserCredentials() throws Exception{
        PrismObject<UserType> oldUser = prismContext.parseObject(new File(TEST_DIR + "/user-migrate-credentials.xml"));

        PrismObject<UserType> newUser = migrator.migrate(oldUser);

        UserType newUserType = newUser.asObjectable();

        assertNull("Credentials in migrated object must be null.", newUserType.getCredentials());
        assertNotNull("Migrated user must contain assignment.", newUserType.getAssignment());
        assertEquals("Migrated user must contain 1 assignment.", newUserType.getAssignment().size(), 1);

        AssignmentType superUserRole = newUserType.getAssignment().get(0);

        assertNotNull("Target ref in the user's assignment must not be null.", superUserRole.getTargetRef());
        assertEquals(superUserRole.getTargetRef().getOid(), SystemObjectsType.ROLE_SUPERUSER.value());
    }

    private <O extends ObjectType> void assertSimpleMigration(File fileOld, File fileNew) throws SchemaException, IOException {
        // GIVEN
        PrismObject<O> objectOld = prismContext.parseObject(fileOld);

        // WHEN
        PrismObject<O> objectNew = migrator.migrate(objectOld);

        // THEN

        IntegrationTestTools.display("Migrated object "+fileOld.getName(), objectNew);
        assertNotNull("No migrated object "+fileOld.getName(), objectNew);

        IntegrationTestTools.display("Migrated object "+fileOld.getName(), objectNew);
        String migratedXml = prismContext.xmlSerializer().serialize(objectNew);
        IntegrationTestTools.display("Migrated object XML "+fileOld.getName(), migratedXml);

        PrismObject<O> expectedObject = prismContext.parseObject(fileNew);
        IntegrationTestTools.display("Expected object "+fileOld.getName(), expectedObject);
        String expectedXml = prismContext.xmlSerializer().serialize(expectedObject);
        IntegrationTestTools.display("Expected object XML "+fileOld.getName(), expectedXml);

        List<String> expectedXmlLines = MiscUtil.splitLines(expectedXml);
        Patch patch = DiffUtils.diff(expectedXmlLines, MiscUtil.splitLines(migratedXml));
        List<String> diffLines = DiffUtils.generateUnifiedDiff(fileOld.getPath(), fileNew.getPath(), expectedXmlLines, patch, 3);
        IntegrationTestTools.display("XML textual diff", StringUtils.join(diffLines, '\n'));

        PrismAsserts.assertEquivalent("Unexpected migration result for "+fileOld.getName(), expectedObject, objectNew);
        assertEquals("Unexpected element name for "+fileOld.getName(), expectedObject.getElementName(), objectNew.getElementName());
    }
}
