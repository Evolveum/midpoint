/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import java.io.File;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Tests the "custom indexing" feature.
 *
 * Some parts run on native repo only.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestIndexing extends AbstractEmptyModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/indexing");

    private static final TestObject<ObjectTemplateType> TEMPLATE_PERSON =
            TestObject.file(TEST_DIR, "template-person.xml", "74a2112a-0ecc-4c09-818a-1d9e234e8e6f");
    private static final TestObject<ArchetypeType> ARCHETYPE_PERSON =
            TestObject.file(TEST_DIR, "archetype-person.xml", "ace64f7e-dec0-4ff4-a3e8-728b46fad6f3");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        addObject(TEMPLATE_PERSON, initTask, initResult);
        addObject(ARCHETYPE_PERSON, initTask, initResult);
    }

    @Test
    public void test100AddUser() throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // @formatter:off
        when("user is added");
        String oid = addObject(
                new UserType()
                        .name("alice100")
                        .givenName("Alice")
                        .familyName("Black")
                        .costCenter("CCx-1/100")
                        .assignment(new AssignmentType()
                                .targetRef(ARCHETYPE_PERSON.oid, ArchetypeType.COMPLEX_TYPE)),
                task,
                result);

        then("indexing is OK");
        assertUserAfter(getUserFull(oid))
                .displayXml()
                .identities()
                    .normalizedData()
                        .assertNormalizedItem("givenName.polyStringNorm", "alice")
                        .assertNormalizedItem("givenName.polyStringNorm.prefix3", "ali")
                        .assertNormalizedItem("familyName.polyStringNorm", "black")
                        .assertNormalizedItem("costCenter.original", "CCx-1/100")
                    .end();
        // @formatter:on
    }

    @Test
    public void test110ModifyUserAddModifyProperty() throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("user is added");
        String oid = addObject(
                new UserType()
                        .name("alice110")
                        .givenName("Alice")
                        .familyName("Black")
                        .assignment(new AssignmentType()
                                .targetRef(ARCHETYPE_PERSON.oid, ArchetypeType.COMPLEX_TYPE)),
                task,
                result);

        then("indexing is OK");
        // @formatter:off
        assertUserAfter(getUserFull(oid))
                .displayXml()
                .identities()
                    .normalizedData()
                        .assertNormalizedItem("givenName.polyStringNorm", "alice")
                        .assertNormalizedItem("givenName.polyStringNorm.prefix3", "ali")
                        .assertNormalizedItem("familyName.polyStringNorm", "black")
                    .end();
        // @formatter:on

        when("user is modified");
        executeChanges(
                prismContext.deltaFor(UserType.class)
                        .item(UserType.F_FAMILY_NAME).replace(createPolyString("Green"))
                        .item(UserType.F_COST_CENTER).replace("Aa-2/200")
                        .asObjectDelta(oid),
                null,
                task,
                result);

        then("indexing is OK");
        // @formatter:off
        assertUserAfter(getUserFull(oid))
                .displayXml()
                .identities()
                    .normalizedData()
                        .assertNormalizedItem("givenName.polyStringNorm", "alice")
                        .assertNormalizedItem("givenName.polyStringNorm.prefix3", "ali")
                        .assertNormalizedItem("familyName.polyStringNorm", "green")
                        .assertNormalizedItem("costCenter.original", "Aa-2/200")
                .end();
        // @formatter:on
    }
}
