/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.ICFS_NAME;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_METADATA_LAST_PROVISIONING_TIMESTAMP;

import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.test.ObjectsCounter;
import com.evolveum.midpoint.model.test.TestSimulationResult;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Real execution of operations against development-mode components.
 *
 * On native repository only.
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
                .linkRef(createLinkRefWithFullObject(RESOURCE_SIMPLE_DEVELOPMENT_TARGET));

        when("user is created");
        executeChanges(user.asPrismObject().createAddDelta(), null, task, result);

        // TODO Maybe we should report at least warning or partial error, because the (requested) linkRef was not created.
        assertSuccessAndNoShadow("test100", result);

        displayDumpable("audit", dummyAuditService);
        // TODO add audit asserts
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
                        createAssignmentValue(RESOURCE_SIMPLE_DEVELOPMENT_TARGET));

        when("user is created");
        executeChanges(user.asPrismObject().createAddDelta(), null, task, result);

        assertSuccessAndNoShadow("test110", result);

        assertUserAfterByUsername("test110")
                .assertAssignments(1);

        displayDumpable("audit", dummyAuditService);
        // TODO add audit asserts
    }

    /**
     * Link an account "by value" on development-mode resource.
     *
     * As {@link #test100CreateUserWithLinkedDevelopmentAccount()} but the user is pre-existing.
     */
    @Test
    public void test120LinkDevelopmentAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a user is in repository");
        String userOid = addUser("test120", task, result);

        objectsCounter.remember(result);
        dummyAuditService.clear();

        when("account is linked");
        executeChanges(
                createLinkRefDelta(userOid, RESOURCE_SIMPLE_DEVELOPMENT_TARGET),
                null, task, result);

        // TODO Maybe we should report at least warning or partial error, because the (requested) linkRef was not created.
        assertSuccessAndNoNewObjects("test120", result);

        displayDumpable("audit", dummyAuditService);
        // TODO add audit asserts
        // There is a SYNCHRONIZATION/EXECUTION event ... why?
    }

    /**
     * Assign an account on development-mode resource.
     *
     * As {@link #test110CreateUserWithAssignedDevelopmentAccount()} but the user is pre-existing.
     */
    @Test
    public void test130AssignDevelopmentAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a user is in repository");
        String userOid = addUser("test130", task, result);

        objectsCounter.remember(result);
        dummyAuditService.clear();

        when("account is assigned");
        executeChanges(
                createAssignmentDelta(userOid, RESOURCE_SIMPLE_DEVELOPMENT_TARGET),
                null, task, result);

        assertSuccessAndNoNewObjects("test130", result);

        displayDumpable("audit", dummyAuditService);
        // TODO add audit asserts
    }

    private void assertSuccessAndNoNewObjects(String name, OperationResult result) throws CommonException {
        then("everything is OK");
        assertSuccess(result);

        and("no new objects should be there");
        objectsCounter.assertNoNewObjects(result);

        and("the user is OK, no linkRef");
        assertUserAfterByUsername(name)
                .assertLinks(0, 0);
    }

    /**
     * Creates (and then modifies) a user with an account that has an attribute in "report changes only" mode.
     */
    @Test
    public void test200ManageReportOnlyAttribute() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        objectsCounter.remember(result);
        dummyAuditService.clear();

        given("a user");
        UserType user = new UserType()
                .name("test200")
                .telephoneNumber("1234567890")
                .emailAddress("should-be@ignored")
                .linkRef(createLinkRefWithFullObject(RESOURCE_SIMPLE_PRODUCTION_TARGET));

        when("user is created");
        ObjectDelta<UserType> addDelta = user.asPrismObject().createAddDelta();
        TestSimulationResult simResult = executeWithSimulationResult(
                List.of(addDelta), TaskExecutionMode.PRODUCTION, null, task, result);

        then("everything is OK");
        assertSuccess(result);

        and("the user is OK, with one linkRef");
        var userAfter = findUserByUsername("test200");
        var shadowOid = assertUserAfter(userAfter)
                .assertLinks(1, 0)
                .singleLink()
                .getOid();

        and("account has no 'non-apply' attributes");
        assertShadowAfter(getShadowModel(shadowOid))
                .attributes()
                .assertSize(2); // name + uid, no telephoneNumber nor mail!

        and("in audit there are no 'non-apply' attributes");
        displayDumpable("audit", dummyAuditService);
        dummyAuditService.assertExecutionDeltas(3);
        ObjectDeltaOperation<ShadowType> shadowOdo = dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        assertShadow(shadowOdo.getObjectDelta().getObjectToAdd(), "shadow in audit")
                .display()
                .attributes()
                .assertSize(2); // name + uid, no telephoneNumber nor mail!

        and("there is one delta in the simulation result");
        // @formatter:off
        assertProcessedObjectsAfter(simResult)
                .assertSize(1)
                .by().objectType(ShadowType.class).changeType(ChangeType.ADD).find()
                .delta()
                    .assertAdd()
                    .objectToAdd()
                    .asShadow()
                    .attributes()
                        .assertValue(ICFS_NAME, "test200")
                        .assertValue(ATTR_RI_TELEPHONE_NUMBER, "1234567890")
                        .assertValue(ATTR_RI_MAIL, "should-be@ignored") // FIXME temporary
                        .assertSize(3); // icfs:uid is not available, as this delta was not put through provisioning
        // @formatter:on

        when("user is reconciled");
        dummyAuditService.clear();
        TestSimulationResult simResult2 = executeWithSimulationResult(
                List.of(userAfter.createModifyDelta()), ModelExecuteOptions.create().reconcile(),
                TaskExecutionMode.PRODUCTION, null, task, result);

        then("everything is OK (after reconciliation)");
        assertSuccess(result);

        and("the user is OK, with one linkRef (after reconciliation)");
        var shadowOid2 = assertUserAfterByUsername("test200")
                .assertLinks(1, 0)
                .singleLink()
                .getOid();
        assertThat(shadowOid2).isEqualTo(shadowOid);

        and("account has no 'non-apply' attributes (after reconciliation)");
        assertShadowAfter(getShadowModel(shadowOid))
                .attributes()
                .assertSize(2); // name + uid, still no telephoneNumber nor mail!

        and("in audit there are no 'non-apply' attributes (after reconciliation)");
        displayDumpable("audit", dummyAuditService);

        dummyAuditService.assertExecutionDeltas(2);
        ObjectDeltaOperation<UserType> userOdo2 = dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        // model thinks there is some provisioning - this may change in future
        assertDelta(userOdo2.getObjectDelta(), "user after reconciliation")
                .assertModified(PATH_METADATA_LAST_PROVISIONING_TIMESTAMP);
        // this delta is metadata-only, because the only "meat" was cut out ... TODO shouldn't we skip it?
        ObjectDeltaOperation<ShadowType> shadowOdo2 = dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        assertDelta(shadowOdo2.getObjectDelta(), "shadow after reconciliation")
                .assertNoRealResourceObjectModifications()
                .assertModified(ShadowType.F_METADATA);

        and("there is one delta in the simulation result");
        // @formatter:off
        assertProcessedObjectsAfter(simResult2)
                .assertSize(1)
                .by().objectType(ShadowType.class).changeType(ChangeType.MODIFY).find()
                .delta()
                    .assertModify()
                    .assertModification(
                            ShadowType.F_ATTRIBUTES.append(ATTR_RI_TELEPHONE_NUMBER),
                            null,
                            "1234567890")
                    .assertModifications(1);
        // @formatter:on
    }

    /**
     * As {@link #test200ManageReportOnlyAttribute()} but here we don't touch the report-only attribute.
     * Nothing should be reported.
     */
    @Test
    public void test210UntouchedReportOnlyAttribute() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        objectsCounter.remember(result);
        dummyAuditService.clear();

        given("a user");
        UserType user = new UserType()
                .name("test210")
                .emailAddress("ignored")
                .linkRef(createLinkRefWithFullObject(RESOURCE_SIMPLE_PRODUCTION_TARGET));

        when("user is created");
        ObjectDelta<UserType> addDelta = user.asPrismObject().createAddDelta();
        TestSimulationResult simResult = executeWithSimulationResult(
                List.of(addDelta), TaskExecutionMode.PRODUCTION, null, task, result);

        then("everything is OK");
        assertSuccess(result);

        and("the user is OK, with one linkRef");
        var userAfter = findUserByUsername("test210");
        var shadowOid = assertUserAfter(userAfter)
                .assertLinks(1, 0)
                .singleLink()
                .getOid();

        and("account has no 'non-apply' attributes");
        assertShadowAfter(getShadowModel(shadowOid))
                .attributes()
                .assertSize(2); // name + uid, no mail!

        and("in audit there are no 'non-apply' attributes");
        displayDumpable("audit", dummyAuditService);
        dummyAuditService.assertExecutionDeltas(3);
        ObjectDeltaOperation<ShadowType> shadowOdo = dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        assertShadow(shadowOdo.getObjectDelta().getObjectToAdd(), "shadow in audit")
                .display()
                .attributes()
                .assertSize(2); // name + uid, no mail!

        and("there is no delta in the simulation result");
        assertProcessedObjectsAfter(simResult)
                .assertSize(0);

        when("user is reconciled");
        dummyAuditService.clear();
        TestSimulationResult simResult2 = executeWithSimulationResult(
                List.of(userAfter.createModifyDelta()), ModelExecuteOptions.create().reconcile(),
                TaskExecutionMode.PRODUCTION, null, task, result);

        then("everything is OK (after reconciliation)");
        assertSuccess(result);

        and("the user is OK, with one linkRef (after reconciliation)");
        var shadowOid2 = assertUserAfterByUsername("test210")
                .assertLinks(1, 0)
                .singleLink()
                .getOid();
        assertThat(shadowOid2).isEqualTo(shadowOid);

        and("account has no 'non-apply' attributes (after reconciliation)");
        assertShadowAfter(getShadowModel(shadowOid))
                .attributes()
                .assertSize(2); // name + uid, still no mail!

        and("in audit there are no 'non-apply' attributes (after reconciliation)");
        displayDumpable("audit", dummyAuditService);

//        dummyAuditService.assertExecutionDeltas(2);
//        ObjectDeltaOperation<UserType> userOdo2 = dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
//        // model thinks there is some provisioning - this may change in future
//        assertDelta(userOdo2.getObjectDelta(), "user after reconciliation")
//                .assertModified(PATH_METADATA_LAST_PROVISIONING_TIMESTAMP);
//        // this delta is metadata-only, because the only "meat" was cut out ... TODO shouldn't we skip it?
//        ObjectDeltaOperation<ShadowType> shadowOdo2 = dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
//        assertDelta(shadowOdo2.getObjectDelta(), "shadow after reconciliation")
//                .assertNoRealResourceObjectModifications()
//                .assertModified(ShadowType.F_METADATA);

        and("there is nothing in the simulation result");
        assertProcessedObjectsAfter(simResult2)
                .assertSize(0);
    }
}
