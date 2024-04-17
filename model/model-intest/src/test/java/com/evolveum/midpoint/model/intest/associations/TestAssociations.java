/*
 * Copyright (C) 2020-21 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.associations;

import java.io.File;
import java.io.IOException;

import com.evolveum.icf.dummy.resource.DummyObject;
import com.evolveum.midpoint.model.intest.TestEntitlements;
import com.evolveum.midpoint.model.intest.associations.DummyHrScenarioExtended.CostCenter;
import com.evolveum.midpoint.model.intest.associations.DummyHrScenarioExtended.OrgUnit;
import com.evolveum.midpoint.model.intest.gensync.TestAssociationInbound;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.util.Resource;

import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;

import javax.xml.namespace.QName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the inbound/outbound processing of native associations.
 * Later may be extended to other aspects and/or to simulated associations.
 *
 * @see TestEntitlements
 * @see TestAssociationInbound
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAssociations extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/associations");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final String NS_HR = "http://midpoint.evolveum.com/xml/ns/samples/hr";
    private static final String NS_DMS = "http://midpoint.evolveum.com/xml/ns/samples/dms";

    private static final ItemName HR_COST_CENTER = new ItemName(NS_HR, "costCenter");

    private static final String LEVEL_READ = "read";
    private static final String LEVEL_WRITE = "write";
    private static final String LEVEL_ADMIN = "admin";
    private static final QName RELATION_READ = new QName(NS_DMS, LEVEL_READ);
    private static final QName RELATION_WRITE = new QName(NS_DMS, LEVEL_WRITE);
    private static final QName RELATION_ADMIN = new QName(NS_DMS, LEVEL_ADMIN);

    private static final String INTENT_PERSON = "person";
    private static final String INTENT_COST_CENTER = "costCenter";

    private static final String INTENT_DEFAULT = "default";
    private static final String INTENT_DOCUMENT = "document";

    private static final String INTENT_GROUP = "group";

    private static final String ORG_SCIENCES_NAME = "sciences";
    private static final String ORG_LAW_NAME = "law";
    private static final String CC_1000_NAME = "cc1000";

    private static DummyHrScenarioExtended hrScenario;
    private static DummyDmsScenario dmsScenario;
    private static DummyAdTrivialScenario adScenario;

    private static final DummyTestResource RESOURCE_DUMMY_HR = new DummyTestResource(
            TEST_DIR, "resource-dummy-hr.xml", "ded54130-8ce5-4c8d-ac30-c3bf4fc82337", "hr",
            c -> hrScenario = DummyHrScenarioExtended.on(c).initialize());

    private static final DummyTestResource RESOURCE_DUMMY_DMS = new DummyTestResource(
            TEST_DIR, "resource-dummy-dms.xml", "d77da617-ee78-46f7-8a15-cde88193308d", "dms",
            c -> dmsScenario = DummyDmsScenario.on(c).initialize());

    private static final DummyTestResource RESOURCE_DUMMY_AD = new DummyTestResource(
            TEST_DIR, "resource-dummy-ad.xml", "a817af1e-a1ef-4dcf-aab4-04e266c93e74", "ad",
            c -> adScenario = DummyAdTrivialScenario.on(c).initialize());

    private static final TestObject<ArchetypeType> ARCHETYPE_PERSON = TestObject.file(
            TEST_DIR, "archetype-person.xml", "184a5aa5-3e28-46c7-b9ed-a1dabaacc11d");
    private static final TestObject<ArchetypeType> ARCHETYPE_COST_CENTER = TestObject.file(
            TEST_DIR, "archetype-costCenter.xml", "eb49f576-5813-4988-9dd1-91e418c65be6");
    private static final TestObject<ArchetypeType> ARCHETYPE_DOCUMENT = TestObject.file(
            TEST_DIR, "archetype-document.xml", "ce92f877-9f22-44cf-9ef1-f55675760eb0");

    // HR objects
    private OrgType orgCc1000;

    // DMS objects
    private ServiceType serviceGuide;

    // AD objects
    private DummyObject dummyAdministrators;
    private DummyObject dummyGuests;
    private DummyObject dummyTesters;
    private RoleType roleAdministrators;
    private RoleType roleGuests;
    private RoleType roleTesters;

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

        RESOURCE_DUMMY_AD.initAndTest(this, initTask, initResult);
        createCommonAdObjects();
        importGroups();
    }

    /**
     * These objects should be usable in all tests. They should not be modified by tests; if needed, a test should create
     * its own objects to work with.
     */
    private void createCommonHrObjects() throws Exception {
        DummyObject sciences = hrScenario.orgUnit.add(ORG_SCIENCES_NAME)
                .addAttributeValues(OrgUnit.AttributeNames.DESCRIPTION.local(), "Faculty of Sciences");
        DummyObject law = hrScenario.orgUnit.add(ORG_LAW_NAME)
                .addAttributeValues(OrgUnit.AttributeNames.DESCRIPTION.local(), "Faculty of Law");

        DummyObject cc1000 = hrScenario.costCenter.add(CC_1000_NAME)
                .addAttributeValues(CostCenter.AttributeNames.DESCRIPTION.local(), CC_1000_NAME);

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

        orgCc1000 = assertOrgByName(CC_1000_NAME, "after")
                .display()
                .getObjectable();
    }

    private void createCommonDmsObjects() throws Exception {
        DummyObject jack = dmsScenario.account.add("jack");
        DummyObject guide = dmsScenario.document.add("guide");
        DummyObject jackCanReadGuide = dmsScenario.access.add("jack-can-read-guide");
        jackCanReadGuide.addAttributeValues(DummyDmsScenario.Access.AttributeNames.LEVEL.local(), LEVEL_READ);
        DummyObject jackCanWriteGuide = dmsScenario.access.add("jack-can-write-guide");
        jackCanWriteGuide.addAttributeValues(DummyDmsScenario.Access.AttributeNames.LEVEL.local(), LEVEL_WRITE);

        dmsScenario.accountAccess.add(jack, jackCanReadGuide);
        dmsScenario.accessDocument.add(jackCanReadGuide, guide);

        dmsScenario.accountAccess.add(jack, jackCanWriteGuide);
        dmsScenario.accessDocument.add(jackCanWriteGuide, guide);
    }

    private void importDocuments() throws Exception {
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_DMS.oid)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ShadowKindType.GENERIC, INTENT_DOCUMENT))
                .withProcessingAllAccounts()
                .executeOnForeground(getTestOperationResult());

        serviceGuide = assertServiceByName("guide", "after")
                .display()
                .getObjectable();
    }

    private void createCommonAdObjects() {
        dummyAdministrators = adScenario.group.add("administrators");
        dummyGuests = adScenario.group.add("guests");
        dummyTesters = adScenario.group.add("testers");

        DummyObject jim = adScenario.account.add("jim");
        adScenario.accountGroup.add(jim, dummyAdministrators);
    }

    private void importGroups() throws Exception {
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_AD.oid)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ShadowKindType.ENTITLEMENT, INTENT_GROUP))
                .withProcessingAllAccounts()
                .executeOnForeground(getTestOperationResult());

        roleAdministrators = assertRoleByName("administrators", "after").display().getObjectable();
        roleGuests = assertRoleByName("guests", "after").display().getObjectable();
        roleTesters = assertRoleByName("testers", "after").display().getObjectable();
    }

    /** Checks that simply getting the account gets the correct results. A prerequisite for the following test. */
    @Test
    public void test100GetExistingHrPerson() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("john is get");
        var query = Resource.of(RESOURCE_DUMMY_HR.get())
                .queryFor(DummyHrScenarioExtended.Person.OBJECT_CLASS_NAME.xsd())
                .and().item(DummyHrScenarioExtended.Person.AttributeNames.NAME.path()).eq("john")
                .build();
        var shadows = modelService.searchObjects(ShadowType.class, query, null, task, result);

        then("john is found");

        display("shadows", shadows);
        assertThat(shadows).as("shadows").hasSize(1);

        assertShadowAfter(shadows.get(0))
                .assertKind(ShadowKindType.ACCOUNT)
                .assertIntent(INTENT_PERSON)
                .associations()
                .assertSize(1)
                .association(DummyHrScenarioExtended.Person.LinkNames.CONTRACT.q())
                .assertSize(2);

        // We do not check the details here. The provisioning module behavior should be checked in the provisioning tests,
        // in particular in TestDummyAssociations / TestDummyNativeAssociations.
    }

    /** Checks that the account and its associations are correctly imported. */
    @Test
    public void test110ImportExistingHrPerson() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("john is imported");
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_HR.oid)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ShadowKindType.ACCOUNT, INTENT_PERSON))
                .withNameValue("john")
                .executeOnForeground(result);

        then("orgs are there (they were created on demand)");
        var orgSciencesOid = assertOrgByName(ORG_SCIENCES_NAME, "after")
                .display()
                .getOid();
        var orgLawOid = assertOrgByName(ORG_LAW_NAME, "after")
                .display()
                .getOid();

        and("john is found");
        // @formatter:off
        assertUserAfterByUsername("john")
                .assignments()
                .assertAssignments(3)
                .by().targetType(ArchetypeType.COMPLEX_TYPE).find()
                    .assertTargetOid(ARCHETYPE_PERSON.oid)
                .end()
                .by().identifier("contract:10703321").find()
                    .assertTargetRef(orgSciencesOid, OrgType.COMPLEX_TYPE)
                    .assertOrgRef(orgCc1000.getOid(), OrgType.COMPLEX_TYPE)
                    .assertSubtype("contract")
                    .extension()
                        .assertPropertyValuesEqual(HR_COST_CENTER, CC_1000_NAME)
                    .end()
                    .assertDescription("needs review")
                .end()
                .by().identifier("contract:10409314").find()
                    .assertTargetRef(orgLawOid, OrgType.COMPLEX_TYPE)
                    .assertOrgRef(orgCc1000.getOid(), OrgType.COMPLEX_TYPE)
                    .assertSubtype("contract")
                    .extension()
                        .assertPropertyValuesEqual(HR_COST_CENTER, CC_1000_NAME)
                    .end()
                    .assertDescription(null)
                .end()
                .end()
                .assertOrganizations(ORG_SCIENCES_NAME, ORG_LAW_NAME);
        // @formatter:on
    }

    @Test
    public void test200GetExistingDmsAccount() throws Exception {
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
                .assertSize(2);

        // Details not checked here, see test100
    }

    @Test
    public void test210ImportExistingDmsAccount() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("jack is imported");
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_DMS.oid)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ShadowKindType.ACCOUNT, INTENT_DEFAULT))
                .withNameValue("jack")
                .executeOnForeground(result);

        then("jack is found");
        // @formatter:off
        assertUserAfterByUsername("jack")
                .assignments()
                .assertAssignments(2)
                .by().identifier("guide:read").find()
                    .assertTargetRef(serviceGuide.getOid(), ServiceType.COMPLEX_TYPE, RELATION_READ)
                    .assertSubtype("documentAccess")
                .end()
                .by().identifier("guide:write").find()
                    .assertTargetRef(serviceGuide.getOid(), ServiceType.COMPLEX_TYPE, RELATION_WRITE)
                    .assertSubtype("documentAccess")
                .end();
        // @formatter:on
    }

    @Test
    public void test300GetExistingAdAccount() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("jim is get");
        var query = Resource.of(RESOURCE_DUMMY_AD.get())
                .queryFor(DummyAdTrivialScenario.Account.OBJECT_CLASS_NAME.xsd())
                .and().item(DummyAdTrivialScenario.Account.AttributeNames.NAME.path()).eq("jim")
                .build();
        var shadows = modelService.searchObjects(ShadowType.class, query, null, task, result);

        then("jim is found");

        display("shadows", shadows);
        assertThat(shadows).as("shadows").hasSize(1);

        assertShadowAfter(shadows.get(0))
                .associations()
                .association(DummyAdTrivialScenario.Account.LinkNames.GROUP.q())
                .assertSize(1);

        // Details not checked here, see test100
    }

    @Test
    public void test310ImportExistingAdAccount() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("jim is imported");
        importAdAccount("jim", result);

        then("jim is found");
        // @formatter:off
        assertUserAfterByUsername("jim")
                .assignments()
                .assertAssignments(1)
                .by().targetOid(roleAdministrators.getOid()).find()
                    .assertTargetRef(roleAdministrators.getOid(), RoleType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT)
                    .assertSubtype("groupMembership")
                .end();
        // @formatter:on
    }

    /** Imports AD account throughout its lifecycle: from creation, through modifications, to deletion. */
    @Test(enabled = false) // doesn't work yet
    public void test320ImportChangingAdAccount() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        String name = "test320";

        given("AD account (guests)");
        DummyObject account = adScenario.account.add(name);
        adScenario.accountGroup.add(account, dummyGuests);

        when("account is imported");
        importAdAccount(name, result);

        then("user is found");
        // @formatter:off
        assertUserAfterByUsername(name)
                .assignments()
                .assertAssignments(1)
                .by().targetOid(roleGuests.getOid()).find()
                    .assertTargetRef(roleGuests.getOid(), RoleType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT)
                    .assertSubtype("groupMembership")
                .end();
        // @formatter:on

        when("a membership of testers is added and the account is re-imported");
        adScenario.accountGroup.add(account, dummyTesters);
        importAdAccount(name, result);

        then("user has 2 assignments");
        // @formatter:off
        assertUserAfterByUsername(name)
                .assignments()
                .assertAssignments(2)
                .by().targetOid(roleGuests.getOid()).find()
                    .assertTargetRef(roleGuests.getOid(), RoleType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT)
                    .assertSubtype("groupMembership")
                .end()
                .by().targetOid(roleTesters.getOid()).find()
                    .assertTargetRef(roleTesters.getOid(), RoleType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT)
                    .assertSubtype("groupMembership")
                .end();
        // @formatter:on
    }

    private void importAdAccount(String name, OperationResult result) throws CommonException, IOException {
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_AD.oid)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ShadowKindType.ACCOUNT, INTENT_DEFAULT))
                .withNameValue(name)
                .withTracingProfile(createModelAndProvisioningLoggingTracingProfile())
                .executeOnForeground(result);
    }
}
