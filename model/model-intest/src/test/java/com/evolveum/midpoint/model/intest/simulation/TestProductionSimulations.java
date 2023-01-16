/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.simulation;

import com.evolveum.midpoint.model.test.SimulationResult;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.TaskExecutionMode;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.util.List;

import static com.evolveum.midpoint.model.test.CommonInitialObjects.*;

/**
 * Runs the basic simulations in {@link TaskExecutionMode#SIMULATED_PRODUCTION}.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestProductionSimulations extends AbstractBasicSimulationExecutionTest {

    @Override
    TaskExecutionMode getExecutionMode() {
        return TaskExecutionMode.SIMULATED_PRODUCTION;
    }

    /**
     * Tests simulated disabling of a user and an account.
     *
     * Present in "production simulation" because the account has to really exist on a resource.
     */
    @Test
    public void test800DisableUserAndAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a user with an account");
        UserType user = createUserWithAccount("test800", task, result);

        objectsCounter.remember(result);

        when("user is disabled in simulation");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(ACTIVATION_ADMINISTRATIVE_STATUS_PATH)
                .replace(ActivationStatusType.DISABLED)
                .asObjectDelta(user.getOid());

        SimulationResult simResult =
                executeInSimulationMode(
                        List.of(delta),
                        getExecutionMode(), getDefaultSimulationDefinition(), task, result);

        then("everything is OK");
        assertSuccess(result);

        and("no new objects should be created, no deltas really executed");
        objectsCounter.assertNoNewObjects(result);
        simResult.assertNoExecutedNorAuditedDeltas();

        and("simulation result is OK");
        assertTest800Deltas(simResult, false);
        if (isNativeRepository()) {
            assertTest800Deltas(simResult, true);
        }
    }

    private void assertTest800Deltas(SimulationResult simResult, boolean persistent) throws CommonException {
        // @formatter:off
        assertProcessedObjects(simResult, persistent)
                .display()
                .by().objectType(UserType.class).changeType(ChangeType.MODIFY).find()
                    .assertEventTags(TAG_FOCUS_DISABLED)
                .end()
                .by().objectType(ShadowType.class).changeType(ChangeType.MODIFY).find()
                    .assertEventTags(TAG_PROJECTION_DISABLED)
                .end()
                .assertSize(2);
        // @formatter:on
    }

    private UserType createUserWithAccount(String name, Task task, OperationResult result) throws CommonException {
        UserType user = new UserType()
                .name(name)
                .assignment(new AssignmentType()
                        .construction(new ConstructionType()
                                .resourceRef(RESOURCE_SIMPLE_PRODUCTION_TARGET.oid, ResourceType.COMPLEX_TYPE)));
        addObject(user, task, result);
        return user;
    }

    /**
     * Tests simulated renaming of a user and an account.
     *
     * Present in "production simulation" because the account has to really exist on a resource.
     */
    @Test
    public void test810RenameUserAndAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a user with an account");
        UserType user = createUserWithAccount("test810", task, result);

        objectsCounter.remember(result);

        when("user is renamed in simulation");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_NAME)
                .replace(PolyString.fromOrig("test810a"))
                .asObjectDelta(user.getOid());

        SimulationResult simResult =
                executeInSimulationMode(
                        List.of(delta),
                        getExecutionMode(), getDefaultSimulationDefinition(), task, result);

        then("everything is OK");
        assertSuccess(result);

        and("no new objects should be created, no deltas really executed");
        objectsCounter.assertNoNewObjects(result);
        simResult.assertNoExecutedNorAuditedDeltas();

        and("simulation result is OK");
        assertTest810Deltas(simResult, false);
        if (isNativeRepository()) {
            assertTest810Deltas(simResult, true);
        }
    }

    private void assertTest810Deltas(SimulationResult simResult, boolean persistent) throws CommonException {
        // @formatter:off
        assertProcessedObjects(simResult, persistent)
                .display()
                .by().objectType(UserType.class).changeType(ChangeType.MODIFY).find()
                    .assertEventTags(TAG_FOCUS_NAME_CHANGED)
                .end()
                .by().objectType(ShadowType.class).changeType(ChangeType.MODIFY).find()
                    .assertEventTags(TAG_PROJECTION_NAME_CHANGED, TAG_PROJECTION_IDENTIFIER_CHANGED)
                .end()
                .assertSize(2);
        // @formatter:on
    }

    /**
     * Tests simulated password change of a user and an account.
     *
     * Present in "production simulation" because the account has to really exist on a resource.
     */
    @Test
    public void test820ChangePasswordOfUserAndAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a user with an account");
        UserType user = createUserWithAccount("test820", task, result);

        objectsCounter.remember(result);

        when("user password is changed in simulation");
        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE)
                .replace(new ProtectedStringType().clearValue("test"))
                .asObjectDelta(user.getOid());

        SimulationResult simResult =
                executeInSimulationMode(
                        List.of(delta),
                        getExecutionMode(), getDefaultSimulationDefinition(), task, result);

        then("everything is OK");
        assertSuccess(result);

        and("no new objects should be created, no deltas really executed");
        objectsCounter.assertNoNewObjects(result);
        simResult.assertNoExecutedNorAuditedDeltas();

        and("simulation result is OK");
        assertTest820Deltas(simResult, false);
        if (isNativeRepository()) {
            assertTest820Deltas(simResult, true);
        }
    }

    private void assertTest820Deltas(SimulationResult simResult, boolean persistent) throws CommonException {
        // @formatter:off
        assertProcessedObjects(simResult, persistent)
                .display()
                .by().objectType(UserType.class).changeType(ChangeType.MODIFY).find()
                    .assertEventTags()
                .end()
                .by().objectType(ShadowType.class).changeType(ChangeType.MODIFY).find()
                    .assertEventTags(TAG_PROJECTION_PASSWORD_CHANGED)
                .end()
                .assertSize(2);
        // @formatter:on
    }
}
