/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.simulation;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.test.ObjectsCounter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Real execution of operations against development-mode components.
 *
 * Currently fails.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestRealExecution extends AbstractSimulationsTest {

    private final ObjectsCounter objectsCounter = new ObjectsCounter(FocusType.class, ShadowType.class);

    /**
     * Creating a user with a linked account on development-mode resource.
     */
    @Test
    public void test100CreateUserWithLinkedDevelopmentAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        objectsCounter.remember(result);
        dummyAuditService.clear();

        given("a user");
        UserType user = new UserType()
                .name("test100")
                .linkRef(
                        ObjectTypeUtil.createObjectRefWithFullObject(
                                createAccount()));

        when("user is created");
        executeChanges(user.asPrismObject().createAddDelta(), null, task, result);

        // TODO Maybe we should report at least warning or partial error, because the (requested) linkRef was not created.
        assertSuccessAndNoShadow("test100", result);

        displayDumpable("audit", dummyAuditService);
        // TODO add audit asserts
    }

    private ShadowType createAccount() {
        return new ShadowType()
                .resourceRef(RESOURCE_SIMPLE_DEVELOPMENT_TARGET.oid, ResourceType.COMPLEX_TYPE)
                .objectClass(RI_ACCOUNT_OBJECT_CLASS)
                .kind(ShadowKindType.ACCOUNT)
                .intent("default");
        // Name should be computed by mappings
    }

    private void assertSuccessAndNoShadow(String username, OperationResult result) throws CommonException {
        then("everything is OK");
        assertSuccess(result);

        and("a single user is created (no shadows)");
        objectsCounter.assertUserOnlyIncrement(1, result);

        and("the user is OK, no linkRef");
        assertUserAfterByUsername(username)
                .assertLinks(0, 0);
    }

    /**
     * As {@link #test100CreateUserWithLinkedDevelopmentAccount()} but the account is assigned, not linked.
     */
    @Test
    public void test110CreateUserWithAssignedDevelopmentAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        objectsCounter.remember(result);
        dummyAuditService.clear();

        given("a user");
        UserType user = new UserType()
                .name("test110")
                .assignment(
                        new AssignmentType()
                                .construction(
                                        new ConstructionType()
                                                .resourceRef(RESOURCE_SIMPLE_DEVELOPMENT_TARGET.oid, ResourceType.COMPLEX_TYPE)));

        when("user is created");
        executeChanges(user.asPrismObject().createAddDelta(), null, task, result);

        assertSuccessAndNoShadow("test110", result);

        assertUserAfterByUsername("test110")
                .assertAssignments(1);

        displayDumpable("audit", dummyAuditService);
        // TODO add audit asserts
    }
}
