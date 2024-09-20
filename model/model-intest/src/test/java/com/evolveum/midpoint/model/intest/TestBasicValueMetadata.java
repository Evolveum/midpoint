/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.password.AbstractPasswordTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Tests the value metadata handling for objects and assignments.
 *
 * Related tests:
 *
 * . Credentials metadata are tested in {@link AbstractPasswordTest} and its subclasses.
 * . Approval and certification related metadata are tested in `workflow-impl` and `certification-impl` modules.
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestBasicValueMetadata extends AbstractEmptyModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/metadata/basic");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestObject<ObjectTemplateType> TEMPLATE_GUEST = TestObject.file(
            TEST_DIR, "template-guest.xml", "855aec87-a7cf-424a-a85d-a1d53980381d");
    private static final TestObject<ArchetypeType> ARCHETYPE_GUEST = TestObject.file(
            TEST_DIR, "archetype-guest.xml", "2f6505dc-f576-4f23-beee-8c48f197f511");
    private static final TestObject<RoleType> ROLE_GUEST_VIA_TEMPLATE = TestObject.file(
            TEST_DIR, "role-guest-via-template.xml", "bdefc429-304f-4627-b63d-c36de191f989");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        initTestObjects(initTask, initResult,
                TEMPLATE_GUEST,
                ARCHETYPE_GUEST,
                ROLE_GUEST_VIA_TEMPLATE);
    }

    @Override
    protected PrismObject<UserType> getDefaultActor() {
        return userAdministrator;
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Test
    public void test100ObjectCreationAndModification() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given("a user");
        var user = new UserType()
                .name(getTestNameShort());

        when("user is added");
        var startTs = XmlTypeConverter.createXMLGregorianCalendar();
        addObject(user, task, result);
        var endTs = XmlTypeConverter.createXMLGregorianCalendar();

        then("creation metadata are present");
        assertUserAfter(user.getOid())
                .valueMetadataSingle()
                .assertCreateMetadataComplex(startTs, endTs)
                .assertModifyTimestampNotPresent();

        // TODO modification
    }

    @Test
    public void test110AssignmentCreationAndModification() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given("a user");
        var user = new UserType()
                .name(getTestNameShort())
                .assignment(ROLE_SUPERUSER.assignmentTo());

        when("user is added");
        var startTs = XmlTypeConverter.createXMLGregorianCalendar();
        addObject(user, task, result);
        var endTs = XmlTypeConverter.createXMLGregorianCalendar();

        then("creation metadata are present");
        assertUserAfter(user.getOid());

        // TODO modification
    }

    @Test
    public void test120AssignmentCreationViaMapping() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given("a user");
        var user = new UserType()
                .name(getTestNameShort())
                .assignment(ARCHETYPE_GUEST.assignmentTo());

        when("user is added");
        var startTs = XmlTypeConverter.createXMLGregorianCalendar();
        traced(() -> addObject(user, task, result));
        var endTs = XmlTypeConverter.createXMLGregorianCalendar();

        then("creation metadata are present");
        assertUserAfter(user.getOid());

        // TODO modification
    }

    @Test
    public void test130AssignmentCreationViaMappingAndExplicit() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given("a user");
        var user = new UserType()
                .name(getTestNameShort())
                .assignment(ARCHETYPE_GUEST.assignmentTo())
                .assignment(ROLE_GUEST_VIA_TEMPLATE.assignmentTo());

        when("user is added");
        var startTs = XmlTypeConverter.createXMLGregorianCalendar();
        traced(() -> addObject(user, task, result));
        var endTs = XmlTypeConverter.createXMLGregorianCalendar();

        then("creation metadata are present");
        assertUserAfter(user.getOid());

        // TODO modification
    }
}
