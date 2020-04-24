/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static com.evolveum.midpoint.util.MiscUtil.extractSingleton;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Various tests related to navigation the links between objects.
 * See also https://wiki.evolveum.com/display/midPoint/Linked+objects.
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLinkedObjects extends AbstractEmptyModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/linked");

    private static final TestResource<ArchetypeType> ARCHETYPE_TOKEN = new TestResource<>(TEST_DIR, "archetype-token.xml", "e7bff8d1-cebd-4fbe-b935-64cfc2f22f52");
    private static final TestResource<ServiceType> SERVICE_MEDALLION = new TestResource<>(TEST_DIR, "service-medallion.xml", "8734f795-f6b4-4cc5-843b-6307aaf88f9d");
    private static final TestResource<UserType> USER_CAVIN = new TestResource<>(TEST_DIR, "user-cavin.xml", "04753be2-f0f1-4292-8f24-48b0eedfcce3");
    private static final TestResource<UserType> USER_ZUMMI = new TestResource<>(TEST_DIR, "user-zummi.xml", "3224fccd-27fa-45b5-8cf3-497a0d2dd892");
    private static final TestResource<UserType> USER_GRUFFY = new TestResource<>(TEST_DIR, "user-gruffy.xml", "30b59b40-2875-410d-8731-482743eb6de2");
    private static final TestResource<UserType> USER_GRAMMI = new TestResource<>(TEST_DIR, "user-grammi.xml", "041d0c03-c322-4e0d-89ba-a2d49b732674");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        addObject(ARCHETYPE_TOKEN, initTask, initResult);
        addObject(SERVICE_MEDALLION, initTask, initResult);

        addObject(USER_CAVIN, initTask, initResult);
        repoAdd(USER_ZUMMI, initResult);
        repoAdd(USER_GRUFFY, initResult);
        repoAdd(USER_GRAMMI, initResult);
    }

    @Test
    public void test000Sanity() throws Exception {
        assertUser(USER_CAVIN.oid, "after init")
                .assertOrganizationalUnits()
                .display();
        assertService(SERVICE_MEDALLION.oid, "after init")
                .assertDescription("Not held")
                .display();
    }

    /**
     * Cavin's grandfather gives medallion to him.
     * We should observe correct data on both the medallion and its holder.
     */
    @Test
    public void test100GiveMedallionToCavin() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        // @formatter:off
        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                    .add(ObjectTypeUtil.createAssignmentTo(SERVICE_MEDALLION.object, SchemaConstants.ORG_DEFAULT))
                .asObjectDelta(USER_CAVIN.oid);
        // @formatter:on

        executeChanges(delta, null, task, result);

        then();
        assertUserAfter(USER_CAVIN.oid)
                .assertOrganizationalUnits("medallion holders");

        recomputeFocus(ServiceType.class, SERVICE_MEDALLION.oid, task, result); // should occur automatically
        assertServiceAfter(SERVICE_MEDALLION.oid)
                .assertDescription("Held by cavin (Cavin)");
    }

    /**
     * Cavin passes the medallion to Zummi.
     * Phase one is that it is unassigned from Cavin.
     */
    @Test
    public void test110PassMedallionToZummiPhaseOne() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        refresh(USER_CAVIN, task, result);
        AssignmentType medallionAssignment = extractSingleton(USER_CAVIN.getObjectable().getAssignment());

        when();
        // @formatter:off
        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                    .delete(medallionAssignment.clone())
                .asObjectDelta(USER_CAVIN.oid);
        // @formatter:on

        executeChanges(delta, null, task, result);

        then();
        assertUserAfter(USER_CAVIN.oid)
                .assertOrganizationalUnits();

        recomputeFocus(ServiceType.class, SERVICE_MEDALLION.oid, task, result); // should occur automatically
        assertServiceAfter(SERVICE_MEDALLION.oid)
                .assertDescription("Not held");
    }

    /**
     * Cavin passes the medallion to Zummi.
     * Phase two is that it is assigned to Zummi.
     */
    @Test
    public void test120PassMedallionToZummiPhaseTwo() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        // @formatter:off
        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                    .add(ObjectTypeUtil.createAssignmentTo(SERVICE_MEDALLION.object, SchemaConstants.ORG_DEFAULT))
                .asObjectDelta(USER_ZUMMI.oid);
        // @formatter:on

        executeChanges(delta, null, task, result);

        then();
        assertUserAfter(USER_ZUMMI.oid)
                .assertOrganizationalUnits("medallion holders");

        recomputeFocus(ServiceType.class, SERVICE_MEDALLION.oid, task, result); // should occur automatically
        assertServiceAfter(SERVICE_MEDALLION.oid)
                .assertDescription("Held by zummi (Zummi Gummi)");
    }

}
