/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.correlation;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType.*;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;

import com.evolveum.midpoint.model.api.WorkflowService;
import com.evolveum.midpoint.model.impl.correlator.CorrelationCaseManager;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.CsvResource;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Here we test the correlation that uses an ID Match implementation (real or dummy one).
 *
 * There are two source systems: `AIS` (Academic Information System) and `HR` (Human Resources).
 * They provide accounts using import, live sync, and reconciliation activities.
 */
public abstract class AbstractIdMatchTest extends AbstractCorrelationTest {

    public static final File TEST_DIR = new File(AbstractCorrelationTest.TEST_DIR, "idmatch");

    private static final CsvResource RESOURCE_AIS = new CsvResource(TEST_DIR, "resource-ais.xml",
            "89d4fce0-f378-453a-a4f7-438efff10cfe", "resource-ais.csv",
            "aisId,givenName,familyName,dateOfBirth,nationalId,emailAddress");

    private static final TestTask TASK_IMPORT_AIS = new TestTask(TEST_DIR, "task-import-ais.xml",
            "95ebbf1e-9c71-4870-a1fb-dc47ce6856c9", 30000);

    @Autowired CorrelationCaseManager correlationCaseManager;
    @Autowired WorkflowService caseService;

    /** This is the initialized object (retrieved from the repo). */
    private ResourceType resourceAis;

    private UserType ian;
    private UserType mary;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        RESOURCE_AIS.initialize(initTask, initResult);
        assertSuccess(
                modelService.testResource(RESOURCE_AIS.oid, initTask));
        resourceAis = repositoryService.getObject(ResourceType.class, RESOURCE_AIS.oid, null, initResult).asObjectable();

        TASK_IMPORT_AIS.initialize(this, initTask, initResult); // importing in closed state
    }

    /**
     * First import: No ambiguities.
     *
     * Here we import two persons, with no ambiguities. Both should correlated to distinct new users.
     */
    @Test
    public void test100ImportNoAmbiguities() throws CommonException, IOException {
        given();
        OperationResult result = getTestOperationResult();

        RESOURCE_AIS.appendLine("1,Ian,Smith,2004-02-06,0402061328,ian@evolveum.com");
        RESOURCE_AIS.appendLine("2,Mary,Smith,2006-04-10,0604101993,mary@evolveum.com");

        when();
        TASK_IMPORT_AIS.rerun(result);

        then();
        // @formatter:off
        TASK_IMPORT_AIS.assertAfter()
                .display()
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .progress()
                        .assertCommitted(2, 0, 0)
                    .end()
                    .synchronizationStatistics()
                        .display()
                        .assertTransition(null, UNMATCHED, LINKED, null, 2, 0, 0)
                        .assertTransitions(1)
                    .end();

        ian = findUserByEmail("ian@evolveum.com");
        assertUser(ian, "Ian after")
                .display()
                .assertGivenName("Ian")
                .assertEmployeeNumber("1")
                .singleLink()
                    .resolveTarget()
                        .display();

        mary = findUserByEmail("mary@evolveum.com");
        assertUser(mary, "Mary after")
                .display()
                .assertGivenName("Mary")
                .assertEmployeeNumber("2")
                .singleLink()
                    .resolveTarget()
                        .display();
        // @formatter:on
    }

    /**
     * Automatically resolved ambiguities.
     *
     * Adding a third person. The existing two should be synchronized without correlation,
     * and the third one should be correlated to existing owner.
     */
    @Test
    public void test110ImportIanSlightlyModified() throws CommonException, IOException {
        given();
        OperationResult result = getTestOperationResult();

        // A slight change in the given name. ID Match should automatically assign the same ID.
        RESOURCE_AIS.appendLine("3,Jan,Smith,2004-02-06,0402061328,ian@evolveum.com");

        when();
        TASK_IMPORT_AIS.rerun(result);

        then();
        // @formatter:off
        TASK_IMPORT_AIS.assertAfter()
                .display()
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .progress()
                        .assertCommitted(3, 0, 0)
                    .end()
                    .synchronizationStatistics()
                        .display()
                        // two existing accounts (Ian, Mary)
                        .assertTransition(LINKED, LINKED, LINKED, null, 2, 0, 0)
                        // account #3 - it is unlinked because the owner exists (but account is not linked yet)
                        .assertTransition(null, UNLINKED, LINKED, null, 1, 0, 0)
                        .assertTransitions(2)
                    .end();
        // @formatter:on

        assertUser(findUserByEmail("ian@evolveum.com"), "Ian after")
                .display()
                .assertName(ian.getName().getOrig()) // unchanged
                .assertGivenName("Jan") // updated
                .assertEmployeeNumber("3"); // updated

        assertUser(findUserByEmail("mary@evolveum.com"), "Mary after")
                .display()
                .assertName(mary.getName().getOrig()) // unchanged
                .assertGivenName("Mary") // unchanged
                .assertEmployeeNumber("2"); // unchanged
    }

    /**
     * Manually resolvable (single) ambiguity - will be resolved to the same person.
     *
     * A case should be created.
     */
    @Test
    public void test120ImportIanSingleAmbiguity() throws CommonException, IOException {
        given();
        OperationResult result = getTestOperationResult();

        // National ID without the last four digits. The algorithm should stop and ask the operator.
        RESOURCE_AIS.appendLine("4,Ian,Smith,2004-02-06,040206----,ian@evolveum.com");

        when();
        TASK_IMPORT_AIS.rerun(result);

        then();
        // @formatter:off
        TASK_IMPORT_AIS.assertAfter()
                .display()
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .display()
                    .progress()
                        .assertCommitted(4, 0, 0)
                    .end()
                    .synchronizationStatistics()
                        .display()
                        // three existing accounts (Ian, Mary, Jan)
                        .assertTransition(LINKED, LINKED, LINKED, null, 3, 0, 0)
                        // newly added account
                        .assertTransition(null, DISPUTED, DISPUTED, null, 1, 0, 0)
                        .assertTransitions(2)
                    .end();
        // @formatter:on

        assertUser(findUserByEmail("ian@evolveum.com"), "Ian after")
                .display()
                .assertName(ian.getName().getOrig()) // unchanged
                .assertGivenName("Jan") // unchanged from previous test
                .assertEmployeeNumber("3"); // unchanged from previous test

        assertUser(findUserByEmail("mary@evolveum.com"), "Mary after")
                .display()
                .assertName(mary.getName().getOrig()) // unchanged
                .assertGivenName("Mary") // unchanged
                .assertEmployeeNumber("2"); // unchanged

        PrismObject<ShadowType> newShadow = findShadowByPrismName("4", resourceAis.asPrismObject(), result);

        assertShadow(newShadow, "after")
                .display()
                .assertSynchronizationSituation(DISPUTED)
                .assertMatchReferenceId(null) // not matched yet
                .assertHasMatchRequestId();

        CaseType correlationCase = correlationCaseManager.findCorrelationCase(newShadow.asObjectable(), false, result);
        assertThat(correlationCase).as("case").isNotNull();

        // @formatter:off
        assertCase(correlationCase, "correlation case")
                .display()
                .assertOpen()
                .workItems()
                    .assertWorkItems(1);
        // @formatter:on
    }

    /**
     * Resolve the case created in the previous test. (By confirming it is the same person.)
     */
    @Test
    public void test130ResolveAmbiguityAsExisting() throws CommonException {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ShadowType newShadow = findShadowByPrismName("4", resourceAis.asPrismObject(), result).asObjectable();
        CaseType correlationCase = java.util.Objects.requireNonNull(
                correlationCaseManager.findCorrelationCase(newShadow, true, result));
        WorkItemId workItemId = WorkItemId.of(correlationCase.getWorkItem().get(0));
        AbstractWorkItemOutputType output = new AbstractWorkItemOutputType(prismContext)
                .outcome(SchemaConstants.CORRELATION_OPTION_PREFIX + ian.getName().getOrig()); // unqualified should be OK here

        when();
        caseService.completeWorkItem(workItemId, output, task, result);

        then();
        assertSuccess(result);
    }

    /**
     * Reimport manually resolved ambiguity introduced in previous tests.
     *
     * Import should succeed and the case should be updated.
     */
    @Test
    public void test140ReimportIanSingleAmbiguity() throws CommonException {
        given();
        OperationResult result = getTestOperationResult();

        when();
        TASK_IMPORT_AIS.rerun(result);

        then();
        // @formatter:off
        TASK_IMPORT_AIS.assertAfter()
                .display()
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .display()
                    .progress()
                        .assertCommitted(4, 0, 0)
                    .end()
                    .synchronizationStatistics()
                        .display()
                        // three imported accounts (Ian, Mary, Jan)
                        .assertTransition(LINKED, LINKED, LINKED, null, 3, 0, 0)
                        // newly resolved account
                        .assertTransition(DISPUTED, UNLINKED, LINKED, null, 1, 0, 0)
                        .assertTransitions(2)
                    .end();
        // @formatter:on

        assertUser(findUserByEmail("ian@evolveum.com"), "Ian after")
                .display()
                .assertName(ian.getName().getOrig()) // unchanged
                .assertGivenName("Ian") // updated from 4
                .assertEmployeeNumber("4"); // updated from 4

        assertUser(findUserByEmail("mary@evolveum.com"), "Mary after")
                .display()
                .assertName(mary.getName().getOrig()) // unchanged
                .assertGivenName("Mary") // unchanged
                .assertEmployeeNumber("2"); // unchanged

        PrismObject<ShadowType> newShadow = findShadowByPrismName("4", resourceAis.asPrismObject(), result);

        assertShadow(newShadow, "after")
                .display()
                .assertSynchronizationSituation(LINKED)
                .assertMatchReferenceId(ian.getName().getOrig());
                // match request ID may or may not be there

        CaseType correlationCase = correlationCaseManager.findCorrelationCase(newShadow.asObjectable(), false, result);
        assertThat(correlationCase).as("case").isNotNull();

        // @formatter:off
        assertCase(correlationCase, "correlation case")
                .display()
                .assertClosed()
                .workItems()
                    .assertWorkItems(1);
        // @formatter:on
    }

    /**
     * Manually resolvable ambiguity (two options) - will be resolved to existing person.
     *
     * A new (different) case should be created.
     */
    @Test
    public void test150ImportIanTwoOptions() throws CommonException, IOException {
        given();
        OperationResult result = getTestOperationResult();

        // National ID without the last four digits. The algorithm should stop and ask the operator.
        RESOURCE_AIS.appendLine("5,Ian,Smith,2004-02-06,0402060000,ian@evolveum.com");

        when();
        TASK_IMPORT_AIS.rerun(result);

        then();
        // @formatter:off
        TASK_IMPORT_AIS.assertAfter()
                .display()
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .display()
                    .progress()
                        .assertCommitted(5, 0, 0)
                    .end()
                    .synchronizationStatistics()
                        .display()
                        // four existing accounts (1-4)
                        .assertTransition(LINKED, LINKED, LINKED, null, 4, 0, 0)
                        // newly added account
                        .assertTransition(null, DISPUTED, DISPUTED, null, 1, 0, 0)
                        .assertTransitions(2)
                    .end();
        // @formatter:on

        assertUser(findUserByEmail("ian@evolveum.com"), "Ian after")
                .display()
                .assertName(ian.getName().getOrig()) // unchanged
                .assertGivenName("Ian") // unchanged
                .assertEmployeeNumber("4"); // unchanged

        assertUser(findUserByEmail("mary@evolveum.com"), "Mary after")
                .display()
                .assertName(mary.getName().getOrig()) // unchanged
                .assertGivenName("Mary") // unchanged
                .assertEmployeeNumber("2"); // unchanged

        PrismObject<ShadowType> newShadow = findShadowByPrismName("5", resourceAis.asPrismObject(), result);

        assertShadow(newShadow, "after")
                .display()
                .assertSynchronizationSituation(DISPUTED)
                .assertMatchReferenceId(null) // not matched yet
                .assertHasMatchRequestId();

        CaseType correlationCase = correlationCaseManager.findCorrelationCase(newShadow.asObjectable(), true, result);
        assertThat(correlationCase).as("case").isNotNull();

        // @formatter:off
        assertCase(correlationCase, "correlation case")
                .display()
                .assertOpen()
                .workItems()
                    .assertWorkItems(1);
        // @formatter:on

        var xml = prismContext.xmlSerializer().serialize(correlationCase.asPrismObject());
        displayValue("Case XML", xml);
    }

    private @NotNull UserType findUserByEmail(@NotNull String email) throws SchemaException {
        return MiscUtil.extractSingletonRequired(
                repositoryService.searchObjects(
                        UserType.class,
                        queryFor(UserType.class)
                                .item(UserType.F_EMAIL_ADDRESS).eq(email)
                                .build(),
                        null,
                        getTestOperationResult()),
                () -> new AssertionError("Multiple users with email: " + email),
                () -> new AssertionError("No user with email: " + email))
                .asObjectable();
    }
}
