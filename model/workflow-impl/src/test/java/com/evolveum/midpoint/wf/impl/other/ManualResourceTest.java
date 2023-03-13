/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.other;

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.ShadowAsserter;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.wf.impl.AbstractWfTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * This is an adaptation of model-intest manual resource test(s) aimed to verify workflow-related aspects
 * (e.g. completion, auditing, notifications) of manual provisioning cases.
 */
@ContextConfiguration(locations = { "classpath:ctx-workflow-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class ManualResourceTest extends AbstractWfTest {

    private static final File TEST_DIR = new File("src/test/resources/manual/");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final File RESOURCE_MANUAL_FILE = new File(TEST_DIR, "resource-manual.xml");
    private static final String RESOURCE_MANUAL_OID = "ffb52158-63dc-4f5b-b998-fb6bfcfc546e";

    private static final File ROLE_ONE_MANUAL_FILE = new File(TEST_DIR, "role-one-manual.xml");
    private static final String ROLE_ONE_MANUAL_OID = "0379f623-a3e9-405a-ba52-b1f7ecd2d46f";

    private static final String USER_WILL_NAME = "will";
    private static final String USER_WILL_GIVEN_NAME = "Will";
    private static final String USER_WILL_FAMILY_NAME = "Turner";
    private static final String USER_WILL_FULL_NAME = "Will Turner";
    private static final String USER_WILL_PASSWORD_OLD = "3lizab3th";

    private static final String ATTR_USERNAME = "username";
    private static final QName ATTR_USERNAME_QNAME = new QName(MidPointConstants.NS_RI, ATTR_USERNAME);

    private static final String ATTR_FULLNAME = "fullname";
    private static final QName ATTR_FULLNAME_QNAME = new QName(MidPointConstants.NS_RI, ATTR_FULLNAME);

    private static final String ATTR_DESCRIPTION = "description";
    private static final QName ATTR_DESCRIPTION_QNAME = new QName(MidPointConstants.NS_RI, ATTR_DESCRIPTION);

    private String userWillOid;
    private String accountWillOid;

    private XMLGregorianCalendar accountWillCompletionTimestampStart;
    private XMLGregorianCalendar accountWillCompletionTimestampEnd;

    private String willLastCaseOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        importObjectFromFile(RESOURCE_MANUAL_FILE, initResult);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        PrismObject<UserType> userWill = createUserWill();
        addObject(userWill, initTask, initResult);
        display("User will", userWill);
        userWillOid = userWill.getOid();

        importObjectFromFile(ROLE_ONE_MANUAL_FILE, initResult);

        // Turns on checks for connection in manual connector
        InternalsConfig.setSanityChecks(true);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    private PrismObject<UserType> createUserWill() throws SchemaException {
        PrismObject<UserType> user = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class).instantiate();
        user.asObjectable()
                .name(USER_WILL_NAME)
                .givenName(USER_WILL_GIVEN_NAME)
                .familyName(USER_WILL_FAMILY_NAME)
                .fullName(USER_WILL_FULL_NAME)
                .beginActivation().administrativeStatus(ActivationStatusType.ENABLED).<UserType>end()
                .beginCredentials().beginPassword().value(new ProtectedStringType().clearValue(USER_WILL_PASSWORD_OLD));
        return user;
    }

    @Test
    public void test000Sanity() throws Exception {
        OperationResult result = getTestOperationResult();

        ResourceType repoResource = repositoryService.getObject(ResourceType.class, RESOURCE_MANUAL_OID,
                null, result).asObjectable();
        assertNotNull("No connector ref", repoResource.getConnectorRef());

        String connectorOid = repoResource.getConnectorRef().getOid();
        assertNotNull("No connector ref OID", connectorOid);
        ConnectorType repoConnector = repositoryService
                .getObject(ConnectorType.class, connectorOid, null, result).asObjectable();
        assertNotNull(repoConnector);
        display("Manual Connector", repoConnector);

        // Check connector schema
        IntegrationTestTools.assertConnectorSchemaSanity(repoConnector, prismContext);

        PrismObject<UserType> userWill = getUser(userWillOid);
        assertUser(userWill, userWillOid, USER_WILL_NAME, USER_WILL_FULL_NAME, USER_WILL_GIVEN_NAME, USER_WILL_FAMILY_NAME);
        assertAdministrativeStatus(userWill, ActivationStatusType.ENABLED);
        assertUserPassword(userWill, USER_WILL_PASSWORD_OLD);
    }

    @Test
    public void test012TestConnection() throws Exception {
        // GIVEN
        Task task = getTestTask();

        // WHEN
        when();
        OperationResult testResult = modelService.testResource(RESOURCE_MANUAL_OID, task, task.getResult());

        // THEN
        then();
        display("Test result", testResult);
        TestUtil.assertSuccess("Test resource failed (result)", testResult);
    }

    @Test
    public void test100AssignWillRoleOne() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        // WHEN
        when();
        assignRole(userWillOid, ROLE_ONE_MANUAL_OID, task, result);

        // THEN
        then();
        display("result", result);
        willLastCaseOid = assertInProgress(result);

        PrismObject<UserType> userAfter = getUser(userWillOid);
        display("User after", userAfter);
        accountWillOid = getSingleLinkOid(userAfter);

        assertAccountWillAfterAssign(USER_WILL_FULL_NAME);

        displayDumpable("dummy audit", dummyAuditService);
        displayDumpable("dummy transport", dummyTransport);
    }

    /**
     * Case is closed. The operation is complete.
     */
    @Test
    public void test110CloseCaseAndRecomputeWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        dummyAuditService.clear();
        dummyTransport.clearMessages();

        // WHEN
        when();

        CaseType caseBefore = getCase(willLastCaseOid);
        display("Case before work item completion", caseBefore);

        List<CaseWorkItemType> workItems = caseBefore.getWorkItem();
        assertEquals("Wrong # of work items", 2, workItems.size());
        assertEquals("Wrong assignees", new HashSet<>(Arrays.asList(USER_ADMINISTRATOR_OID, USER_JACK.oid)), workItems.stream()
                .map(wi -> wi.getOriginalAssigneeRef().getOid())
                .collect(Collectors.toSet()));

        Optional<CaseWorkItemType> adminWorkItem = workItems.stream()
                .filter(wi -> USER_ADMINISTRATOR_OID.equals(wi.getOriginalAssigneeRef().getOid())).findAny();

        assertTrue("no admin work item", adminWorkItem.isPresent());

        caseManager.completeWorkItem(WorkItemId.of(adminWorkItem.get()),
                new AbstractWorkItemOutputType()
                        .outcome(OperationResultStatusType.SUCCESS.value()),
                null, task, result);

        accountWillCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // We need reconcile and not recompute here. We need to fetch the updated case status.
        reconcileUser(userWillOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        CaseType caseAfter = getCase(willLastCaseOid);
        display("Case after work item completion", caseAfter);

        accountWillCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        assertWillAfterCreateCaseClosed();

        displayDumpable("dummy audit", dummyAuditService);
        displayDumpable("dummy transport", dummyTransport);
    }

    @SuppressWarnings("SameParameterValue")
    private void assertAccountWillAfterAssign(String expectedFullName) throws Exception {
        ShadowAsserter<Void> shadowRepoAsserter = assertRepoShadow(accountWillOid)
                .assertConception()
                .pendingOperations()
                .singleOperation()
                .assertId()
                .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .delta()
                .display()
                .end()
                .end()
                .end()
                .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .end()
                .assertNoPassword();
        assertAttributeFromCache(shadowRepoAsserter, ATTR_FULLNAME_QNAME, expectedFullName);
        assertShadowActivationAdministrativeStatusFromCache(shadowRepoAsserter.getObject(), ActivationStatusType.ENABLED);

        ShadowAsserter<Void> shadowModelAsserter = assertModelShadow(accountWillOid)
                .assertName(USER_WILL_NAME)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertConception()
                .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .end()
                .assertNoPassword()
                .pendingOperations()
                .singleOperation()
                .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .end()
                .end();
        assertAttributeFromCache(shadowModelAsserter, ATTR_FULLNAME_QNAME, expectedFullName);
        assertShadowActivationAdministrativeStatusFromCache(shadowModelAsserter.getObject(), ActivationStatusType.ENABLED);

        assertWillCase(SchemaConstants.CASE_STATE_OPEN,
                shadowModelAsserter.pendingOperations().singleOperation().getOperation()
        );
    }

    @SuppressWarnings("SameParameterValue")
    private void assertWillCase(String expectedCaseState, PendingOperationType pendingOperation) throws ObjectNotFoundException, SchemaException {
        String pendingOperationRef = pendingOperation.getAsynchronousOperationReference();
        // Case number should be in willLastCaseOid. It will get there from operation result.
        assertNotNull("No async reference in pending operation", willLastCaseOid);
        assertCaseState(willLastCaseOid, expectedCaseState);
        assertEquals("Wrong case ID in pending operation", willLastCaseOid, pendingOperationRef);
    }

    private void assertWillAfterCreateCaseClosed() throws Exception {
        ShadowAsserter<Void> shadowRepoAsserter = assertRepoShadow(accountWillOid)
                .pendingOperations()
                .singleOperation()
                .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                .assertResultStatus(OperationResultStatusType.SUCCESS)
                .assertCompletionTimestamp(accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd)
                .end()
                .end()
                .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .end();
        assertAttributeFromCache(shadowRepoAsserter, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME);
        assertShadowActivationAdministrativeStatusFromCache(shadowRepoAsserter, ActivationStatusType.ENABLED);

        ShadowAsserter<Void> shadowModelAsserter = assertModelShadow(accountWillOid)
                .assertName(USER_WILL_NAME)
                .assertKind(ShadowKindType.ACCOUNT)
                .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .end();
        shadowRepoAsserter
                .assertLive();

        shadowModelAsserter
                .assertLive()
                .assertAdministrativeStatus(ActivationStatusType.ENABLED)
                .attributes()
                .assertValue(ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME)
                .end()
                .pendingOperations()
                .singleOperation()
                .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                .assertResultStatus(OperationResultStatusType.SUCCESS)
                .assertCompletionTimestamp(accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd)
                .end()
                .end();
        assertAttributeFromBackingStore(shadowModelAsserter, ATTR_DESCRIPTION_QNAME);
        assertShadowPassword(shadowModelAsserter);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
    }

    @SafeVarargs
    private <T> void assertAttribute(PrismObject<ShadowType> shadow, QName attrName, T... expectedValues) {
        assertAttribute(shadow.asObjectable(), attrName, expectedValues);
    }

    private void assertNoAttribute(PrismObject<ShadowType> shadow, QName attrName) {
        assertNoAttribute(shadow.asObjectable(), attrName);
    }

    @SuppressWarnings("SameParameterValue")
    private void assertAttributeFromCache(ShadowAsserter<?> shadowAsserter, QName attrQName, Object... attrVals) {
        assertAttributeFromCache(shadowAsserter.getObject(), attrQName, attrVals);
    }

    private void assertAttributeFromCache(PrismObject<ShadowType> shadow, QName attrQName, Object... attrVals) {
        assertAttribute(shadow, attrQName, attrVals);
    }

    @SuppressWarnings("SameParameterValue")
    private void assertAttributeFromBackingStore(ShadowAsserter<?> shadowAsserter, QName attrQName) {
        assertAttributeFromBackingStore(shadowAsserter.getObject(), attrQName);
    }

    private void assertAttributeFromBackingStore(PrismObject<ShadowType> shadow, QName attrQName) {
        assertNoAttribute(shadow, attrQName);
    }

    @SuppressWarnings("SameParameterValue")
    private void assertShadowActivationAdministrativeStatusFromCache(ShadowAsserter<?> shadowAsserter, ActivationStatusType expectedStatus) {
        assertShadowActivationAdministrativeStatusFromCache(shadowAsserter.getObject(), expectedStatus);
    }

    private void assertShadowActivationAdministrativeStatusFromCache(PrismObject<ShadowType> shadow, ActivationStatusType expectedStatus) {
        assertShadowActivationAdministrativeStatus(shadow, expectedStatus);
    }

    private void assertShadowActivationAdministrativeStatus(PrismObject<ShadowType> shadow, ActivationStatusType expectedStatus) {
        assertActivationAdministrativeStatus(shadow, expectedStatus);
    }

    private void assertShadowPassword(ShadowAsserter<?> shadowAsserter) {
        assertShadowPassword(shadowAsserter.getObject());
    }

    private void assertShadowPassword(PrismObject<ShadowType> shadow) {
        // pure manual resource should never "read" password
        assertNoShadowPassword(shadow);
    }

}
