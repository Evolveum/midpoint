/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.correlation;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.CsvResource;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.testng.annotations.Test;

import java.io.File;

import static com.evolveum.midpoint.schema.constants.MidPointConstants.NS_RI;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests ID Match correlation in the most simple case:
 *
 * 1. Single source resource: SIS
 * 2. Data is matched against pre-existing midPoint users
 */
public abstract class AbstractSimpleIdMatchCorrelationTest extends AbstractIdMatchCorrelationTest {

    public static final File TEST_DIR = new File(AbstractCorrelationTest.TEST_DIR, "idmatch/simple");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestResource<ObjectTemplateType> OBJECT_TEMPLATE_USER =
            new TestResource<>(TEST_DIR, "object-template-user.xml", "0da34c23-628a-40b2-866a-51e6e81ebb1f");

    private static final CsvResource RESOURCE_SIS = new CsvResource(TEST_DIR, "resource-sis.xml",
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

        when("resolving the case");
        resolveCase(aCase, johnOid, task, result);

        then("error should be reported");
        assertFailure(result);

        // May be fragile. Adapt as needed.
        assertThat(result.getMessage()).as("error message").contains("already exists in lens context");

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
}
