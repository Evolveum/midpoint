/*
 * Copyright (C) 2020-21 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.associations;

import java.io.File;

import com.evolveum.icf.dummy.resource.DummyObject;
import com.evolveum.midpoint.model.intest.associations.DummyHrScenarioExtended.CostCenter;
import com.evolveum.midpoint.model.intest.associations.DummyHrScenarioExtended.OrgUnit;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.test.DummyHrScenario;

import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the inbound processing of native associations.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestInboundAssociations extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/associations");
    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final String INTENT_PERSON = "person";
    private static final String INTENT_COST_CENTER = "costCenter";

    private static final String INTENT_DEFAULT = "default";
    private static final String INTENT_DOCUMENT = "document";

    private static DummyHrScenarioExtended hrScenario;
    private static DummyDmsScenario dmsScenario;

    private static final DummyTestResource RESOURCE_DUMMY_HR = new DummyTestResource(
            TEST_DIR, "resource-dummy-hr.xml", "ded54130-8ce5-4c8d-ac30-c3bf4fc82337", "hr",
            c -> hrScenario = DummyHrScenarioExtended.on(c).initialize());

    private static final DummyTestResource RESOURCE_DUMMY_DMS = new DummyTestResource(
            TEST_DIR, "resource-dummy-dms.xml", "d77da617-ee78-46f7-8a15-cde88193308d", "dms",
            c -> dmsScenario = DummyDmsScenario.on(c).initialize());

    private static final TestObject<ArchetypeType> ARCHETYPE_PERSON = TestObject.file(
            TEST_DIR, "archetype-person.xml", "184a5aa5-3e28-46c7-b9ed-a1dabaacc11d");
    private static final TestObject<ArchetypeType> ARCHETYPE_COST_CENTER = TestObject.file(
            TEST_DIR, "archetype-costCenter.xml", "eb49f576-5813-4988-9dd1-91e418c65be6");
    private static final TestObject<ArchetypeType> ARCHETYPE_DOCUMENT = TestObject.file(
            TEST_DIR, "archetype-document.xml", "ce92f877-9f22-44cf-9ef1-f55675760eb0");

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initTestObjects(initTask, initResult,
                ARCHETYPE_PERSON, ARCHETYPE_COST_CENTER, ARCHETYPE_DOCUMENT);

        RESOURCE_DUMMY_HR.initAndTest(this, initTask, initResult);
        createCommonHrObjects();
        importCostCenters();

        RESOURCE_DUMMY_DMS.initAndTest(this, initTask, initResult);
        createCommonDmsObjects();
        importDocuments();
    }

    /** These objects should be usable in all tests. */
    private void createCommonHrObjects() throws Exception {
        DummyObject sciences = hrScenario.orgUnit.add("sciences")
                .addAttributeValues(OrgUnit.AttributeNames.DESCRIPTION.local(), "Faculty of Sciences");
        DummyObject law = hrScenario.orgUnit.add("law")
                .addAttributeValues(OrgUnit.AttributeNames.DESCRIPTION.local(), "Faculty of Law");

        DummyObject cc1000 = hrScenario.costCenter.add("cc1000")
                .addAttributeValues(CostCenter.AttributeNames.DESCRIPTION.local(), "cc1000");

        DummyObject john = hrScenario.person.add("john")
                .addAttributeValue(DummyHrScenarioExtended.Person.AttributeNames.FIRST_NAME.local(), "John")
                .addAttributeValue(DummyHrScenarioExtended.Person.AttributeNames.LAST_NAME.local(), "Doe")
                .addAttributeValue(DummyHrScenarioExtended.Person.AttributeNames.TITLE.local(), "Ing.");

        DummyObject johnContractSciences = hrScenario.contract.add("10703321")
                .addAttributeValues(DummyHrScenarioExtended.Contract.AttributeNames.NOTE.local(), "needs review");

        DummyObject johnContractLaw = hrScenario.contract.add("10409314");

        hrScenario.personContract.add(john, johnContractSciences);
        hrScenario.contractOrgUnit.add(johnContractSciences, sciences);
        hrScenario.contractCostCenter.add(johnContractSciences, cc1000);

        hrScenario.personContract.add(john, johnContractLaw);
        hrScenario.contractOrgUnit.add(johnContractLaw, law);
        hrScenario.contractCostCenter.add(johnContractLaw, cc1000);
    }

    private void importCostCenters() throws Exception {
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_HR.oid)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ShadowKindType.GENERIC, INTENT_COST_CENTER))
                .withProcessingAllAccounts()
                .executeOnForeground(getTestOperationResult());

        assertOrgByName("cc1000", "after")
                .display();
    }

    /** Temporary. Later we create these objects via midPoint/outbounds. */
    private void createCommonDmsObjects() {
        DummyObject jack = dmsScenario.account.add("jack");
        DummyObject guide = dmsScenario.document.add("guide");
        DummyObject jackCanReadGuide = dmsScenario.access.add("jack-can-read-guide");

        dmsScenario.accountAccess.add(jack, jackCanReadGuide);
        dmsScenario.accessDocument.add(jackCanReadGuide, guide);
    }

    /** Temporary. Later we create all via outbounds. */
    private void importDocuments() throws Exception {
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_DMS.oid)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ShadowKindType.GENERIC, INTENT_DOCUMENT))
                .withProcessingAllAccounts()
                .executeOnForeground(getTestOperationResult());

        assertServiceByName("guide", "after")
                .display();
    }

    @Test
    public void test100GetJohn() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("john is get");
        var query = Resource.of(RESOURCE_DUMMY_HR.get())
                .queryFor(DummyHrScenario.Person.OBJECT_CLASS_NAME.xsd())
                .and().item(DummyHrScenario.Person.AttributeNames.NAME.path()).eq("john")
                .build();
        var shadows = modelService.searchObjects(ShadowType.class, query, null, task, result);

        then("john is found");

        display("shadows", shadows);
        assertThat(shadows).as("shadows").hasSize(1);

        assertShadowAfter(shadows.get(0))
                .associations()
                .association(DummyHrScenario.Person.LinkNames.CONTRACT.q())
                .assertSize(2);
    }

    @Test
    public void test110ImportJohn() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("john is imported");
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_HR.oid)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ShadowKindType.ACCOUNT, INTENT_PERSON))
                .withNameValue("john")
                .withTracingProfile(createModelAndProvisioningLoggingTracingProfile())
                .executeOnForeground(result);

        then("john is found");
        assertUserAfterByUsername("john");
    }

    @Test
    public void test200GetJack() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("john is get");
        var query = Resource.of(RESOURCE_DUMMY_DMS.get())
                .queryFor(DummyDmsScenario.Account.OBJECT_CLASS_NAME.xsd())
                .and().item(DummyDmsScenario.Account.AttributeNames.NAME.path()).eq("jack")
                .build();
        var shadows = modelService.searchObjects(ShadowType.class, query, null, task, result);

        then("jack is found");

        display("shadows", shadows);
        assertThat(shadows).as("shadows").hasSize(1);

        assertShadowAfter(shadows.get(0))
                .associations()
                .association(DummyDmsScenario.Account.LinkNames.ACCESS.q())
                .assertSize(1);
    }

    @Test
    public void test210ImportJack() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("jack is imported");
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_DMS.oid)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ShadowKindType.ACCOUNT, INTENT_DEFAULT))
                .withNameValue("jack")
                .withTracingProfile(createModelAndProvisioningLoggingTracingProfile())
                .executeOnForeground(result);

        then("jack is found");
        assertUserAfterByUsername("jack");
    }

}
