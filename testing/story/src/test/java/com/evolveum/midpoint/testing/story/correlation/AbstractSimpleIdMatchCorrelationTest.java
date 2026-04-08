/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.testing.story.correlation;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.CsvTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.testng.annotations.Test;

import java.io.File;
import java.util.Objects;

import static com.evolveum.midpoint.schema.constants.MidPointConstants.NS_RI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests ID Match correlation in the most simple case:
 *
 * 1. Single source resource: SIS
 * 2. Data is matched against pre-existing midPoint users
 */
public abstract class AbstractSimpleIdMatchCorrelationTest extends AbstractIdMatchCorrelationTest {

    public static final File TEST_DIR = new File(AbstractCorrelationTest.TEST_DIR, "idmatch/simple");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestObject<ObjectTemplateType> OBJECT_TEMPLATE_USER =
            TestObject.file(TEST_DIR, "object-template-user.xml", "0da34c23-628a-40b2-866a-51e6e81ebb1f");

    private static final CsvTestResource RESOURCE_SIS = new CsvTestResource(TEST_DIR, "resource-sis.xml",
            "10f012cd-17dc-45dd-886d-2d53aa889fd7", "resource-sis.csv",
            "sisId,firstName,lastName,born,nationalId");

    private static final ItemName SIS_ID_NAME = new ItemName(NS_RI, "sisId");

    private String johnOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        addObject(OBJECT_TEMPLATE_USER, initTask, initResult);

        RESOURCE_SIS.initAndTest(this, initTask, initResult);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    /**
     * Imports John from SIS. It's the only user, so no correlation conflicts here.
     */
    @Test
    public void test100ImportJohnFromSis() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("John is in SIS as #1");
        RESOURCE_SIS.appendLine("1,John,Smith,2004-02-06,040206/1328");

        when("import for #1 is run");
        importAccountsRequest()
                .withResourceOid(RESOURCE_SIS.oid)
                .withNamingAttribute(SIS_ID_NAME)
                .withNameValue("1")
                .execute(result);

        then("John should be imported");
        johnOid = assertUserAfterByUsername("smith1")
                .assertGivenName("John")
                .assertFamilyName("Smith")
                .assertLinks(1, 0)
                .assertExtensionValue("sisId", "1")
                .assertExtensionValue("dateOfBirth", "2004-02-06")
                .assertExtensionValue("nationalId", "040206/1328")
                .getOid();
    }

    /**
     * Imports another variant of John from SIS. The National ID does not match, so manual correlation is needed.
     *
     * Note that if manual correlation determines the account belongs to `smith1`, the import would result
     * in failure: the resource is not multi-account one, so one midPoint user cannot have two accounts!
     */
    @Test
    public void test110ImportJohnFromSisDisputed() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("John is in SIS as #2");
        RESOURCE_SIS.appendLine("2,John,Smith,2004-02-06,040206/132x");

        when("import for #2 is run");
        importAccountsRequest()
                .withResourceOid(RESOURCE_SIS.oid)
                .withNamingAttribute(SIS_ID_NAME)
                .withNameValue("2")
                .execute(result);

        then("Correlation case should be created");

        PrismObject<ShadowType> a2 = findShadowByPrismName("2", RESOURCE_SIS.get(), result);
        assertShadowAfter(a2)
                .assertCorrelationSituation(CorrelationSituationType.UNCERTAIN);
        CaseType aCase = correlationCaseManager.findCorrelationCase(a2.asObjectable(), true, result);
        assertThat(aCase).as("correlation case").isNotNull();
        assertCase(aCase, "case")
                .displayXml();

        assertUserByUsername("smith1", "after import")
                .display()
                .assertLinks(1, 0); // The account should not be linked yet
    }

    /**
     * Resolves the disputed case created in the previous test by selecting the existing owner.
     *
     * The resolution must fail in the real import/projector path, and the case must remain open.
     */
    @Test
    public void test115ResolveDisputedCaseAsExistingOwnerFailsAndKeepsCaseOpen() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("previously created disputed case for SIS account #2");
        PrismObject<ShadowType> a2Before = findShadowByPrismName("2", RESOURCE_SIS.get(), result);
        CaseType aCaseBefore = correlationCaseManager.findCorrelationCase(a2Before.asObjectable(), true, result);
        assertThat(aCaseBefore).as("correlation case before resolution").isNotNull();

        String caseOid = aCaseBefore.getOid();

        when("resolving the case");
        assertThatThrownBy(() -> resolveCase(aCaseBefore, johnOid, task, result))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("already exists in lens context");

        then("error should be reported and the case should remain open");
        assertResultTreeContainsMessage(result);

        CaseType caseAfter = getCase(caseOid);
        assertCase(caseAfter, "case after failed resolution")
                .display()
                .assertOpen()
                .workItems()
                    .single()
                        .assertNotClosed();

        PrismObject<ShadowType> a2After = findShadowByPrismName("2", RESOURCE_SIS.get(), result);
        ShadowCorrelationStateType correlationState = a2After.asObjectable().getCorrelation();
        assertThat(correlationState).as("correlation state").isNotNull();
        assertThat(correlationState.getCorrelationCaseCloseTimestamp())
                .as("correlation case close timestamp")
                .isNull();
        assertThat(correlationState.getPerformerRef())
                .as("correlation performer refs")
                .isEmpty();
        assertThat(correlationState.getPerformerComment())
                .as("correlation performer comments")
                .isEmpty();

        assertUserByUsername("smith1", "after case resolution")
                .display()
                .assertLinks(1, 0); // The account should not be linked, because of the error
    }

    /**
     * Change the data and retry the correlation.
     *
     * Here we do the manual correlation again, this time instructing midPoint to create a new user.
     */
    @Test
    public void test120ImportJohnFromSisDisputedAgain() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("data are changed");
        // We cannot use existing ID of 2, because ID Match may remember the assigned reference ID
        RESOURCE_SIS.replaceLine("2,.*", "3,John,Smith,2004-02-06,040206/1329");

        given("clearing the correlation state");
        PrismObject<ShadowType> a2 = findShadowByPrismName("2", RESOURCE_SIS.get(), result);
        correlationService.clearCorrelationState(a2.getOid(), result);

        when("import for #3 is run");
        importAccountsRequest()
                .withResourceOid(RESOURCE_SIS.oid)
                .withNamingAttribute(SIS_ID_NAME)
                .withNameValue("3")
                .execute(result);

        then("Correlation case should be created");

        PrismObject<ShadowType> a3 = findShadowByPrismName("3", RESOURCE_SIS.get(), result);
        assertShadowAfter(a3)
                .assertCorrelationSituation(CorrelationSituationType.UNCERTAIN);
        CaseType aCase = correlationCaseManager.findCorrelationCase(a3.asObjectable(), true, result);
        assertThat(aCase).as("correlation case").isNotNull();
        assertCase(aCase, "case")
                .displayXml();

        assertUserByUsername("smith1", "after import")
                .display()
                .assertLinks(1, 0);

        when("resolving the case (to new owner)");
        resolveCase(aCase, null, task, result);

        then("new user should be created");
        assertSuccess(result);

        assertUserByUsername("smith1", "after case resolution")
                .display()
                .assertLinks(1, 0); // No change should be here

        assertUserByUsername("smith2", "after case resolution")
                .display()
                .assertLinks(1, 0);
    }

    private void assertResultTreeContainsMessage(OperationResult result) {
        assertThat(
                result.getResultStream()
                        .map(OperationResult::getMessage)
                        .filter(Objects::nonNull)
                        .anyMatch(message -> message.contains("already exists in lens context")))
                .as("operation result tree should contain message fragment '%s'", "already exists in lens context")
                .isTrue();
    }
}
