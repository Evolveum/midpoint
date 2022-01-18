/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.correlation;

import static com.evolveum.midpoint.model.api.correlator.CorrelationResult.Status.*;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.model.impl.correlator.CorrelationCaseManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.model.api.correlator.CorrelationResult;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.impl.sync.CorrelationService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;

/**
 * Tests expression correlator, including creation and resolution of manual correlation cases.
 *
 * Scenario:
 *
 * - users: X, Y, Z
 * - accounts: a1, a2, a3, ...
 *
 * Correlators returns various combinations for users for individual accounts (see the individual tests).
 * Manual cases are sometimes created, and sometimes also resolved.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestExpressionCorrelator extends AbstractInternalModelIntegrationTest {

    protected static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "correlator/correlation/expression");

    private static final String ATTR_CORRELATION_CODE = "correlationCode";

    private static final DummyTestResource DUMMY_RESOURCE_SOURCE = new DummyTestResource(
            TEST_DIR, "resource-dummy.xml", "db8bb18c-f5fe-4c05-91c2-99e9658b82c7", "source",
            controller ->
                    controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                            ATTR_CORRELATION_CODE, String.class, false, false));

    private static final TestResource<UserType> USER_X =
            new TestResource<>(TEST_DIR, "user-x.xml", "f2cb9158-3e3f-40c5-84da-7859c2da5535");
    private static final TestResource<UserType> USER_Y =
            new TestResource<>(TEST_DIR, "user-y.xml", "712c127f-1320-4fa9-95fb-f833feac5f58");
    private static final TestResource<UserType> USER_Z =
            new TestResource<>(TEST_DIR, "user-z.xml", "87f52bbe-8873-4683-adcb-c52a18f63c13");

    @Autowired private CorrelationService correlationService;
    @Autowired private CorrelationCaseManager correlationCaseManager;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResource(DUMMY_RESOURCE_SOURCE, initTask, initResult);
        repoAdd(USER_X, initResult);
        repoAdd(USER_Y, initResult);
        repoAdd(USER_Z, initResult);
    }

    /**
     * The correlation code returns no owner.
     */
    @Test
    public void test100NoOwner() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        String accountName = getTestNameShort();
        DummyAccount account = DUMMY_RESOURCE_SOURCE.controller.addAccount(accountName);
        account.addAttributeValue(ATTR_CORRELATION_CODE, "[]");

        when();
        CorrelationResult correlationResult = correlateAccount(accountName, task, result);

        then();
        assertCorrelationResult(correlationResult, NO_OWNER, null);
        assertNoCorrelationCase(accountName, task, result);
    }

    /**
     * The correlation code returns X.
     */
    @Test
    public void test110OwnerX() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        String accountName = getTestNameShort();
        DummyAccount account = DUMMY_RESOURCE_SOURCE.controller.addAccount(accountName);
        account.addAttributeValue(ATTR_CORRELATION_CODE, ownersCode(USER_X));

        when();
        CorrelationResult correlationResult = correlateAccount(accountName, task, result);

        then();
        assertCorrelationResult(correlationResult, EXISTING_OWNER, USER_X.oid);
        assertNoCorrelationCase(accountName, task, result);
    }

    /**
     * The correlation code returns X and Y. Because manual case is not created automatically, no case should be there.
     */
    @Test
    public void test120OwnersXY() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        String accountName = getTestNameShort();
        DummyAccount account = DUMMY_RESOURCE_SOURCE.controller.addAccount(accountName);
        account.addAttributeValue(ATTR_CORRELATION_CODE, ownersCode(USER_X, USER_Y));

        when();
        CorrelationResult correlationResult = correlateAccount(accountName, task, result);

        then();
        assertCorrelationResult(correlationResult, UNCERTAIN, null);
        assertNoCorrelationCase(accountName, task, result);
    }

    /**
     * The correlation code returns X and Y. Correlation case is requested to be created.
     */
    @Test
    public void test130OwnersXYWithCase() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        String accountName = getTestNameShort();
        DummyAccount account = DUMMY_RESOURCE_SOURCE.controller.addAccount(accountName);
        account.addAttributeValue(ATTR_CORRELATION_CODE,
                requestCaseCode() + ownersCode(USER_X, USER_Y));

        when();
        CorrelationResult correlationResult = correlateAccount(accountName, task, result);

        then();
        assertCorrelationResult(correlationResult, UNCERTAIN, null);
        assertCorrelationCase(accountName, task, result);
    }

    /**
     * The correlation code returns empty list but a custom potential matches.
     */
    @Test
    public void test140CustomPotentialMatches() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        String customCode = "import com.evolveum.midpoint.xml.ns._public.common.common_3.*\n\n"
                + "correlationContext.requestManualCorrelation([\n"
                + "\tnew BuiltInCorrelationPotentialMatchType()\n"
                + "\t\t.candidateOwnerRef('f2cb9158-3e3f-40c5-84da-7859c2da5535', UserType.COMPLEX_TYPE),\n"
                + "\tnew BuiltInCorrelationPotentialMatchType()\n"
                + "\t\t.candidateOwnerRef('712c127f-1320-4fa9-95fb-f833feac5f58', UserType.COMPLEX_TYPE)])\n"
                + "[]";

        String accountName = getTestNameShort();
        DummyAccount account = DUMMY_RESOURCE_SOURCE.controller.addAccount(accountName);
        account.addAttributeValue(ATTR_CORRELATION_CODE, customCode);

        when();
        CorrelationResult correlationResult = correlateAccount(accountName, task, result);

        then();
        assertCorrelationResult(correlationResult, UNCERTAIN, null);
        assertCorrelationCase(accountName, task, result);
    }

    private CaseType assertCorrelationCase(String accountName, Task task, OperationResult result) throws CommonException {
        ShadowType shadow = getAccountByName(accountName, task, result);
        CaseType correlationCase = correlationCaseManager.findCorrelationCase(shadow, false, result);
        assertThat(correlationCase).as("correlation case").isNotNull();
        displayDumpable("correlation case", correlationCase);
        return correlationCase;
    }

    private void assertNoCorrelationCase(String accountName, Task task, OperationResult result) throws CommonException {
        ShadowType shadow = getAccountByName(accountName, task, result);
        CaseType correlationCase = correlationCaseManager.findCorrelationCase(shadow, false, result);
        assertThat(correlationCase).as("correlation case").isNull();
    }

    private String requestCaseCode() {
        return "correlationContext.requestManualCorrelation()\n";
    }

    private String ownersCode(TestResource<?>... owners) {
        return Arrays.stream(owners)
                .map(o -> o.oid)
                .map(oid -> "com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef('"
                        + oid + "', com.evolveum.midpoint.schema.constants.ObjectTypes.USER)")
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private void assertCorrelationResult(CorrelationResult correlationResult, CorrelationResult.Status expectedStatus,
            String expectedOid) {
        displayDumpable("correlation result", correlationResult);
        assertThat(correlationResult.getStatus()).as("correlation result status").isEqualTo(expectedStatus);
        ObjectType owner = correlationResult.getOwner();
        String oid = owner != null ? owner.getOid() : null;
        assertThat(oid).as("correlated owner OID").isEqualTo(expectedOid);
    }

    private CorrelationResult correlateAccount(String accountName, Task task, OperationResult result) throws CommonException {
        ShadowType shadow = getAccountByName(accountName, task, result);
        return correlationService.correlate(shadow, task, result);
    }

    private @NotNull ShadowType getAccountByName(String name, Task task, OperationResult result)
            throws CommonException {
        List<PrismObject<ShadowType>> objects = provisioningService.searchObjects(
                ShadowType.class,
                createAccountAttributeQuery(DUMMY_RESOURCE_SOURCE.getObjectable(), SchemaConstants.ICFS_NAME, name),
                null,
                task,
                result);
        return MiscUtil.extractSingletonRequired(objects,
                () -> new AssertionError("Multiple objects named " + name + " found: " + objects),
                () -> new AssertionError("No object named " + name + " found"))
                .asObjectable();
    }
}
