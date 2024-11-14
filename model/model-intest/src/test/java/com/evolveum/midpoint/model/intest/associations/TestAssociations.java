/*
 * Copyright (C) 2020-21 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.associations;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import com.evolveum.icf.dummy.resource.DummyObject;
import com.evolveum.midpoint.model.intest.TestEntitlements;
import com.evolveum.midpoint.model.intest.dummys.DummyAdTrivialScenario;
import com.evolveum.midpoint.model.intest.dummys.DummyDmsScenario;
import com.evolveum.midpoint.model.intest.dummys.DummyHrScenarioExtended;
import com.evolveum.midpoint.model.intest.dummys.DummyHrScenarioExtended.CostCenter;
import com.evolveum.midpoint.model.intest.dummys.DummyHrScenarioExtended.OrgUnit;
import com.evolveum.midpoint.model.intest.gensync.TestAssociationInbound;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.util.Resource;

import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.asserter.*;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;

import static com.evolveum.midpoint.model.intest.dummys.ScenariosConstants.*;
import static com.evolveum.midpoint.model.test.CommonInitialObjects.MARK_MANAGED;
import static com.evolveum.midpoint.model.test.CommonInitialObjects.MARK_UNMANAGED;
import static com.evolveum.midpoint.schema.GetOperationOptions.createReadOnlyCollection;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.ICFS_NAME;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType.ENTITLEMENT;

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

    private static final String ROLE_INDUCING_GROUP_TEMPLATE = "role-inducing-group-template.xml";
    private static final String ROLE_INDUCING_GROUP_TEMPLATE_2 = "role-inducing-group-template-2.xml";

    private static final String INTENT_PERSON = "person";
    private static final String INTENT_COST_CENTER = "costCenter";

    private static final String INTENT_DEFAULT = "default";
    private static final String INTENT_DOCUMENT = "document";

    private static final ResourceObjectTypeIdentification TYPE_GROUP =
            ResourceObjectTypeIdentification.of(ENTITLEMENT, "group");

    private static final String ORG_SCIENCES_NAME = "sciences";
    private static final String ORG_LAW_NAME = "law";
    private static final String ORG_MEDICINE_NAME = "medicine";
    private static final String CC_1000_NAME = "cc1000";
    private static final String CC_1100_NAME = "cc1100";

    private static final String PERSON_JOHN_NAME = "john";

    private static final String JOHN_SCIENCES_CONTRACT_ID = "10703321";
    private static final String JOHN_SCIENCES_CONTRACT_ASSIGNMENT_ID = "contract:" + JOHN_SCIENCES_CONTRACT_ID;
    private static final String JOHN_LAW_CONTRACT_ID = "10409314";
    private static final String JOHN_LAW_CONTRACT_ASSIGNMENT_ID = "contract:" + JOHN_LAW_CONTRACT_ID;
    private static final String JOHN_MEDICINE_CONTRACT_ID = "10104921";
    private static final String JOHN_MEDICINE_CONTRACT_ASSIGNMENT_ID = "contract:" + JOHN_MEDICINE_CONTRACT_ID;

    private static final String SERVICE_GUIDE_NAME = "guide";

    private static final String ROLE_ADMINISTRATORS_NAME = "administrators";
    private static final String ROLE_GUESTS_NAME = "guests";
    private static final String ROLE_TESTERS_NAME = "testers";
    private static final String ROLE_OPERATORS_NAME = "operators";

    private static final ItemName RI_APP_GROUP = ItemName.from(NS_RI, "appGroup");
    private static final ItemName RI_GENERIC_GROUP = ItemName.from(NS_RI, "genericGroup");

    private static final ItemName RI_USER_DOCUMENT_ACCESS = ItemName.from(NS_RI, "userDocumentAccess");

    private static DummyHrScenarioExtended hrScenario;
    private static DummyDmsScenario dmsScenario;
    private static DummyDmsScenario dmsScenarioNonTolerant;
    private static DummyAdTrivialScenario adScenario;
    private static DummyAdTrivialScenario adTwoGroupTypesScenario;

    private static final DummyTestResource RESOURCE_DUMMY_HR = new DummyTestResource(
            TEST_DIR, "resource-dummy-hr.xml", "ded54130-8ce5-4c8d-ac30-c3bf4fc82337", "hr",
            c -> hrScenario = DummyHrScenarioExtended.on(c).initialize());

    private static final DummyTestResource RESOURCE_DUMMY_DMS = new DummyTestResource(
            TEST_DIR, "resource-dummy-dms.xml", "d77da617-ee78-46f7-8a15-cde88193308d", "dms",
            c -> dmsScenario = DummyDmsScenario.on(c).initialize());

    private static final DummyTestResource RESOURCE_DUMMY_DMS_NON_TOLERANT = new DummyTestResource(
            TEST_DIR, "resource-dummy-dms-non-tolerant.xml", "204871af-6a87-4d93-a23e-34bdc1a89196",
            "dms-non-tolerant",
            c -> dmsScenarioNonTolerant = DummyDmsScenario.on(c).initialize());

    private static final DummyTestResource RESOURCE_DUMMY_AD = new DummyTestResource(
            TEST_DIR, "resource-dummy-ad.xml", "a817af1e-a1ef-4dcf-aab4-04e266c93e74", "ad",
            c -> adScenario = DummyAdTrivialScenario.on(c).initialize());

    private static final DummyTestResource RESOURCE_DUMMY_AD_TWO_GROUP_TYPES = new DummyTestResource(
            TEST_DIR, "resource-dummy-ad-two-group-types.xml", "1c77ef70-61de-4666-8221-5edbb426b000",
            "ad-two-group-types",
            c -> adTwoGroupTypesScenario = DummyAdTrivialScenario.on(c).initialize());

    private static final TestObject<ArchetypeType> ARCHETYPE_PERSON = TestObject.file(
            TEST_DIR, "archetype-person.xml", "184a5aa5-3e28-46c7-b9ed-a1dabaacc11d");
    private static final TestObject<ArchetypeType> ARCHETYPE_COST_CENTER = TestObject.file(
            TEST_DIR, "archetype-costCenter.xml", "eb49f576-5813-4988-9dd1-91e418c65be6");
    private static final TestObject<ArchetypeType> ARCHETYPE_DOCUMENT = TestObject.file(
            TEST_DIR, "archetype-document.xml", "ce92f877-9f22-44cf-9ef1-f55675760eb0");
    private static final TestObject<ArchetypeType> ARCHETYPE_DOCUMENT_NON_TOLERANT = TestObject.file(
            TEST_DIR, "archetype-document-non-tolerant.xml", "737dc161-2df6-45b3-8201-036745f9e51a");
    private static final TestObject<ArchetypeType> ARCHETYPE_AD_ROLE = TestObject.file(
            TEST_DIR, "archetype-ad-role.xml", "5200a309-554d-46c7-a551-b8a4fdc26a18");
    private static final TestObject<ArchetypeType> ARCHETYPE_APP_ROLE = TestObject.file(
            TEST_DIR, "archetype-app-role.xml", "79d020a0-6cc5-4f47-8525-548ebd930b4a");
    private static final TestObject<ArchetypeType> ARCHETYPE_GENERIC_AD_ROLE = TestObject.file(
            TEST_DIR, "archetype-generic-ad-role.xml", "ba9ae6c7-362b-41d4-b713-3a0040945b0c");

    private static final TestObject<?> ARCHETYPE_ORG_WITH_GROUP = TestObject.file(
            TEST_DIR, "archetype-org-with-group.xml", "ccbc679a-c9b5-4e2d-9027-8578695a7ff5");

    private final ZonedDateTime sciencesContractFrom = ZonedDateTime.now();

    // HR objects
    private OrgType orgCc1000;
    private OrgType orgCc1100;

    // DMS objects
    private ServiceType serviceGuide;

    // AD objects
    @SuppressWarnings("FieldCanBeLocal") private DummyObject dummyAdministrators;
    private DummyObject dummyGuests;
    private DummyObject dummyTesters;
    @SuppressWarnings({ "FieldCanBeLocal", "unused" }) private DummyObject dummyOperators;

    /** Managed by midPoint. */
    private RoleType roleAdministrators;
    @SuppressWarnings({ "FieldCanBeLocal", "unused" }) private String shadowAdministratorsOid;

    /** Managed by midPoint. */
    private RoleType roleGuests;
    private String shadowGuestsOid;

    /** Managed externally (i.e., tolerant), see {@link #importGroups(OperationResult)} */
    private RoleType roleTesters;
    private String shadowTestersOid;

    /** Managed by midPoint */
    private RoleType roleOperators;
    private String shadowOperatorsOid;

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Override
    protected boolean requiresNativeRepository() {
        return true;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        CommonInitialObjects.addMarks(this, initTask, initResult);

        repoAdd(CommonInitialObjects.SERVICE_ORIGIN_INTERNAL, initResult);

        initTestObjects(initTask, initResult,
                ARCHETYPE_PERSON, ARCHETYPE_COST_CENTER, ARCHETYPE_DOCUMENT, ARCHETYPE_DOCUMENT_NON_TOLERANT,
                ARCHETYPE_AD_ROLE, ARCHETYPE_APP_ROLE, ARCHETYPE_GENERIC_AD_ROLE, ARCHETYPE_ORG_WITH_GROUP);

        // The subresult is created to avoid failing on benign warnings from the above objects' initialization
        var subResult = initResult.createSubresult("initializeResources");
        try {
            RESOURCE_DUMMY_HR.initAndTest(this, initTask, subResult);
            createCommonHrObjects();
            importCostCenters(subResult);

            RESOURCE_DUMMY_DMS.initAndTest(this, initTask, subResult);
            createCommonDmsObjects();
            importDocuments(subResult);

            RESOURCE_DUMMY_DMS_NON_TOLERANT.initAndTest(this, initTask, subResult);

            RESOURCE_DUMMY_AD.initAndTest(this, initTask, subResult);
            createCommonAdObjects();
            importGroups(subResult);

            RESOURCE_DUMMY_AD_TWO_GROUP_TYPES.initAndTest(this, initTask, subResult);
        } finally {
            subResult.close();
        }
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
        hrScenario.orgUnit.add(ORG_MEDICINE_NAME)
                .addAttributeValues(OrgUnit.AttributeNames.DESCRIPTION.local(), "Faculty of Medicine");

        DummyObject cc1000 = hrScenario.costCenter.add(CC_1000_NAME)
                .addAttributeValues(CostCenter.AttributeNames.DESCRIPTION.local(), CC_1000_NAME);
        hrScenario.costCenter.add(CC_1100_NAME)
                .addAttributeValues(CostCenter.AttributeNames.DESCRIPTION.local(), CC_1100_NAME);

        DummyObject john = hrScenario.person.add(PERSON_JOHN_NAME)
                .addAttributeValue(DummyHrScenarioExtended.Person.AttributeNames.FIRST_NAME.local(), "John")
                .addAttributeValue(DummyHrScenarioExtended.Person.AttributeNames.LAST_NAME.local(), "Doe")
                .addAttributeValue(DummyHrScenarioExtended.Person.AttributeNames.TITLE.local(), "Ing.");

        DummyObject johnContractSciences = hrScenario.contract.add(JOHN_SCIENCES_CONTRACT_ID)
                .addAttributeValues(DummyHrScenarioExtended.Contract.AttributeNames.NOTE.local(), "needs review")
                .addAttributeValues(DummyHrScenarioExtended.Contract.AttributeNames.VALID_FROM.local(), List.of(sciencesContractFrom));

        DummyObject johnContractLaw = hrScenario.contract.add(JOHN_LAW_CONTRACT_ID);

        hrScenario.personContract.add(john, johnContractSciences);
        hrScenario.contractOrgUnit.add(johnContractSciences, sciences);
        hrScenario.contractCostCenter.add(johnContractSciences, cc1000);

        hrScenario.personContract.add(john, johnContractLaw);
        hrScenario.contractOrgUnit.add(johnContractLaw, law);
        hrScenario.contractCostCenter.add(johnContractLaw, cc1000);

        // beware, john's data is changed in test methods
    }

    private void importCostCenters(OperationResult result) throws Exception {
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_HR.oid)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ShadowKindType.GENERIC, INTENT_COST_CENTER))
                .withProcessingAllAccounts()
                .executeOnForeground(result);

        orgCc1000 = assertOrgByName(CC_1000_NAME, "after")
                .display()
                .getObjectable();
        orgCc1100 = assertOrgByName(CC_1100_NAME, "after")
                .display()
                .getObjectable();
    }

    private void createCommonDmsObjects() throws Exception {
        DummyObject jack = dmsScenario.account.add("jack");
        DummyObject guide = dmsScenario.document.add(SERVICE_GUIDE_NAME);
        DummyObject jackCanReadGuide = dmsScenario.access.add("jack-can-read-guide");
        jackCanReadGuide.addAttributeValues(DummyDmsScenario.Access.AttributeNames.LEVEL.local(), LEVEL_READ);
        DummyObject jackCanWriteGuide = dmsScenario.access.add("jack-can-write-guide");
        jackCanWriteGuide.addAttributeValues(DummyDmsScenario.Access.AttributeNames.LEVEL.local(), LEVEL_WRITE);

        dmsScenario.accountAccess.add(jack, jackCanReadGuide);
        dmsScenario.accessDocument.add(jackCanReadGuide, guide);

        dmsScenario.accountAccess.add(jack, jackCanWriteGuide);
        dmsScenario.accessDocument.add(jackCanWriteGuide, guide);
    }

    private void importDocuments(OperationResult result) throws Exception {
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_DMS.oid)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ShadowKindType.GENERIC, INTENT_DOCUMENT))
                .withProcessingAllAccounts()
                .executeOnForeground(result);

        serviceGuide = assertServiceByName(SERVICE_GUIDE_NAME, "after")
                .display()
                .getObjectable();
    }

    private void createCommonAdObjects() {
        dummyAdministrators = adScenario.group.add(ROLE_ADMINISTRATORS_NAME);
        dummyGuests = adScenario.group.add(ROLE_GUESTS_NAME);
        dummyTesters = adScenario.group.add(ROLE_TESTERS_NAME);
        dummyOperators = adScenario.group.add(ROLE_OPERATORS_NAME);

        DummyObject jim = adScenario.account.add("jim");
        adScenario.accountGroup.add(jim, dummyAdministrators);
    }

    private void importGroups(OperationResult result) throws Exception {
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_AD.oid)
                .withTypeIdentification(TYPE_GROUP)
                .withProcessingAllAccounts()
                .executeOnForeground(result);

        var administratorsAsserter = assertRoleByName(ROLE_ADMINISTRATORS_NAME, "after").display();
        roleAdministrators = administratorsAsserter.getObjectable();
        shadowAdministratorsOid = administratorsAsserter.singleLink().getOid();

        var guestsAsserter = assertRoleByName(ROLE_GUESTS_NAME, "after").display();
        roleGuests = guestsAsserter.getObjectable();
        shadowGuestsOid = guestsAsserter.singleLink().getOid();

        var testersAsserter = assertRoleByName(ROLE_TESTERS_NAME, "after").display();
        roleTesters = testersAsserter.getObjectable();
        shadowTestersOid = testersAsserter.singleLink().getOid();
        markShadow(shadowTestersOid, MARK_UNMANAGED.oid, getTestTask(), getTestOperationResult());

        var operatorsAsserter = assertRoleByName(ROLE_OPERATORS_NAME, "after").display();
        roleOperators = operatorsAsserter.getObjectable();
        shadowOperatorsOid = operatorsAsserter.singleLink().getOid();
    }

    @Test
    public void test010Sanity() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        var resource = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_HR.oid, null, task, result);
        displayDumpable("HR resource", resource);

        // Hacky code but it's just a sanity check
        var contractsInboundExpression = resource.asObjectable()
                .getSchemaHandling()
                .getAssociationType().get(0)
                .getSubject().getAssociation().getInbound().get(0)
                .getExpression();
        var evaluator = (AssociationSynchronizationExpressionEvaluatorType)
                contractsInboundExpression.getExpressionEvaluator().get(0).getValue();
        var pcvId = evaluator.getAttribute().get(0).getId();
        assertThat(pcvId)
                .withFailMessage("PCV IDs are not set in the expression evaluators")
                .isNotNull();
    }

    /** Checks that simply getting the account gets the correct results. A prerequisite for the following test. */
    @Test
    public void test100GetHrPerson() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("john is get");
        var query = Resource.of(RESOURCE_DUMMY_HR.get())
                .queryFor(DummyHrScenarioExtended.Person.OBJECT_CLASS_NAME.xsd())
                .and().item(DummyHrScenarioExtended.Person.AttributeNames.NAME.path()).eq(PERSON_JOHN_NAME)
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

        var shadowReadAgain = provisioningService.getObject(
                ShadowType.class, shadows.get(0).getOid(), createReadOnlyCollection(), task, result);
        assertShadowAfter(shadowReadAgain)
                .display();

        // We do not check the details here. The provisioning module behavior should be checked in the provisioning tests,
        // in particular in TestDummyAssociations / TestDummyNativeAssociations.
    }

    /**
     * Checks that the account and its associations are correctly imported (and re-imported after a change).
     *
     * Tests assignment correlation as well as automatic provenance-based mapping ranges.
     */
    @Test
    public void test110ImportHrPerson() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("john is imported");
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_HR.oid)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ShadowKindType.ACCOUNT, INTENT_PERSON))
                .withNameValue(PERSON_JOHN_NAME)
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
        assertUserAfterByUsername(PERSON_JOHN_NAME)
                .assignments()
                .assertAssignments(3)
                .by().targetType(ArchetypeType.COMPLEX_TYPE).find()
                    .assertTargetOid(ARCHETYPE_PERSON.oid)
                .end()
                .by().identifier(JOHN_SCIENCES_CONTRACT_ASSIGNMENT_ID).find()
                    .assertTargetRef(orgSciencesOid, OrgType.COMPLEX_TYPE)
                    .assertOrgRef(orgCc1000.getOid(), OrgType.COMPLEX_TYPE)
                    .extension()
                        .assertPropertyValuesEqual(HR_COST_CENTER, CC_1000_NAME)
                    .end()
                    .assertDescription("needs review")
                    //.assertValidFrom(XmlTypeConverter.createXMLGregorianCalendar(sciencesContractFrom)) // TODO enable when done
                .end()
                .by().identifier(JOHN_LAW_CONTRACT_ASSIGNMENT_ID).find()
                    .assertTargetRef(orgLawOid, OrgType.COMPLEX_TYPE)
                    .assertOrgRef(orgCc1000.getOid(), OrgType.COMPLEX_TYPE)
                    .extension()
                        .assertPropertyValuesEqual(HR_COST_CENTER, CC_1000_NAME)
                    .end()
                    .assertDescription(null)
                .end()
                .end();
                //.assertOrganizations(ORG_SCIENCES_NAME, ORG_LAW_NAME); // FIXME will be fixed later
        // @formatter:on

        when("john is changed on HR resource");

        var dummyJohn = hrScenario.person.getByNameRequired(PERSON_JOHN_NAME);
        var dummyMedicine = hrScenario.orgUnit.getByNameRequired(ORG_MEDICINE_NAME);
        var dummyCc1000 = hrScenario.costCenter.getByNameRequired(CC_1000_NAME);
        var dummyCc1100 = hrScenario.costCenter.getByNameRequired(CC_1100_NAME);

        // one contract is added
        var dummyContractMedicine = hrScenario.contract.add(JOHN_MEDICINE_CONTRACT_ID);
        hrScenario.personContract.add(dummyJohn, dummyContractMedicine);
        hrScenario.contractOrgUnit.add(dummyContractMedicine, dummyMedicine);
        hrScenario.contractCostCenter.add(dummyContractMedicine, dummyCc1100);

        // one is changed
        var dummyContractSciences = hrScenario.contract.getByNameRequired(JOHN_SCIENCES_CONTRACT_ID);
        dummyContractSciences.replaceAttributeValues(
                DummyHrScenarioExtended.Contract.AttributeNames.NOTE.local(), "reviewed");
        hrScenario.contractCostCenter.delete(dummyContractSciences, dummyCc1000);
        hrScenario.contractCostCenter.add(dummyContractSciences, dummyCc1100);

        // one is deleted
        hrScenario.contract.deleteByName(JOHN_LAW_CONTRACT_ID);

        when("john is re-imported");
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_HR.oid)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ShadowKindType.ACCOUNT, INTENT_PERSON))
                .withNameValue(PERSON_JOHN_NAME)
                .executeOnForeground(result);

        then("new org is there (created on demand)");
        var orgMedicineOid = assertOrgByName(ORG_MEDICINE_NAME, "after")
                .display()
                .getOid();

        and("john is updated; one assignment is added, one is changed, one is deleted");
        // @formatter:off
        assertUserAfterByUsername(PERSON_JOHN_NAME)
                .assignments()
                .assertAssignments(3)
                .by().targetType(ArchetypeType.COMPLEX_TYPE).find()
                    .assertTargetOid(ARCHETYPE_PERSON.oid)
                .end()
                .by().identifier(JOHN_SCIENCES_CONTRACT_ASSIGNMENT_ID).find()
                    .assertTargetRef(orgSciencesOid, OrgType.COMPLEX_TYPE)
                    .assertOrgRef(orgCc1100.getOid(), OrgType.COMPLEX_TYPE)
                    .extension()
                        .assertPropertyValuesEqual(HR_COST_CENTER, CC_1100_NAME)
                    .end()
                    .assertDescription("reviewed")
                .end()
                .by().identifier(JOHN_MEDICINE_CONTRACT_ASSIGNMENT_ID).find()
                    .assertTargetRef(orgMedicineOid, OrgType.COMPLEX_TYPE)
                    .assertOrgRef(orgCc1100.getOid(), OrgType.COMPLEX_TYPE)
                    .extension()
                        .assertPropertyValuesEqual(HR_COST_CENTER, CC_1100_NAME)
                    .end()
                    .assertDescription(null)
                .end()
                .by().identifier(JOHN_LAW_CONTRACT_ASSIGNMENT_ID).assertNone()
                .end();
        // @formatter:on
    }

    @Test
    public void test200GetDmsAccount() throws Exception {
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
    public void test210ImportDmsAccount() throws Exception {
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
                    .valueMetadataSingle()
                        .assertMappingName("accesses-inbound")
                        .assertMappingObjectOid(RESOURCE_DUMMY_DMS.oid)
                        .assertMappingObjectType(ResourceObjectTypeIdentification.ACCOUNT_DEFAULT)
                        .assertMappingAssociationType(RI_USER_DOCUMENT_ACCESS)
                    .end()
                .end()
                .by().identifier("guide:write").find()
                    .assertTargetRef(serviceGuide.getOid(), ServiceType.COMPLEX_TYPE, RELATION_WRITE)
                    .valueMetadataSingle()
                        .assertMappingName("accesses-inbound")
                        .assertMappingObjectOid(RESOURCE_DUMMY_DMS.oid)
                        .assertMappingObjectType(ResourceObjectTypeIdentification.ACCOUNT_DEFAULT)
                        .assertMappingAssociationType(RI_USER_DOCUMENT_ACCESS)
                    .end()
                .end();
        // @formatter:on
    }

    /**
     * Creates a DMS account, gradually provisioning it with associations. Uses tolerant association definition.
     *
     * . First, read access to `security-policy` is provisioned via midPoint.
     * . Then, admin access to `security-policy` is added manually on the resource.
     * . Afterwards, write access to `security-policy` is provisioned via midPoint. Admin access should _remain_.
     */
    @Test
    public void test220ProvisionDmsAccountTolerant() throws Exception {
        executeProvisionDmsAccountTest(
                ARCHETYPE_DOCUMENT, dmsScenario, RESOURCE_DUMMY_DMS, true);
    }

    /**
     * Creates a DMS account, gradually provisioning it with associations. Uses non-tolerant association definition.
     *
     * . First, read access to `security-policy` is provisioned via midPoint.
     * . Then, admin access to `security-policy` is added manually on the resource.
     * . Afterwards, write access to `security-policy` is provisioned via midPoint. Admin access should be _deleted_.
     */
    @Test
    public void test230ProvisionDmsAccountNonTolerant() throws Exception {
        executeProvisionDmsAccountTest(
                ARCHETYPE_DOCUMENT_NON_TOLERANT, dmsScenarioNonTolerant, RESOURCE_DUMMY_DMS_NON_TOLERANT, false);
    }

    private void executeProvisionDmsAccountTest(
            TestObject<ArchetypeType> documentArchetype,
            DummyDmsScenario dmsScenario,
            DummyTestResource resource,
            boolean tolerant) throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var userName = "user-" + getTestNameShort();
        var documentName = "security-policy-" + getTestNameShort();

        given("security policy document");
        var document = new ServiceType()
                .name(documentName)
                .assignment(documentArchetype.assignmentTo())
                .assignment(resource.assignmentTo(ShadowKindType.GENERIC, INTENT_DOCUMENT));
        var documentOid = addObject(document, task, result);
        assertServiceAfter(documentOid)
                .assertLiveLinks(1);

        when("user with 'read' access is created and provisioned");
        var user = new UserType()
                .name(userName)
                .assignment(new AssignmentType()
                        .targetRef(documentOid, ServiceType.COMPLEX_TYPE, RELATION_READ));
        var userOid = addObject(user, task, result);
        assertUserAfter(userOid)
                .assertLiveLinks(1);

        then("the account with read access to document exists");
        // @formatter:off
        assertUserAfter(userOid)
                .withObjectResolver(createSimpleModelObjectResolver())
                .assignments()
                    .by().targetOid(documentOid).targetRelation(RELATION_READ).find().end()
                    .assertAssignments(1)
                .end()
                .links().by().resourceOid(resource.oid).find()
                .resolveTarget()
                .display()
                .associations()
                .association(DummyDmsScenario.Account.LinkNames.ACCESS.q())
                .singleValue()
                .associationObject()
                .attributes()
                .assertValue(DummyDmsScenario.Access.AttributeNames.LEVEL.q(), LEVEL_READ)
                .singleReferenceValueShadow(DummyDmsScenario.Access.LinkNames.DOCUMENT.q())
                .assertOrigValues(ICFS_NAME, documentName);
        // @formatter:on

        when("manually adding admin access");
        var dummyAdminAccess = dmsScenario.access.add(RandomStringUtils.randomAlphabetic(10));
        dummyAdminAccess.addAttributeValues(DummyDmsScenario.Access.AttributeNames.LEVEL.local(), LEVEL_ADMIN);
        dmsScenario.accountAccess.add(dmsScenario.account.getByNameRequired(userName), dummyAdminAccess);
        dmsScenario.accessDocument.add(dummyAdminAccess, dmsScenario.document.getByNameRequired(documentName));

        invalidateShadowCacheIfNeeded(resource.oid);

        and("provisioning the user with write access to the document");
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                        .add(new AssignmentType()
                                .targetRef(documentOid, ServiceType.COMPLEX_TYPE, RELATION_WRITE))
                        .asObjectDelta(userOid),
                null, task, result);

        assertSuccess(result);

        then("the account with read and write access to the document exists");
        // @formatter:off
        UserAsserter<?> userAsserter = assertUserAfter(userOid)
                .withObjectResolver(createSimpleModelObjectResolver())
                .assignments()
                    .by().targetOid(documentOid).targetRelation(RELATION_READ).find().end()
                    .by().targetOid(documentOid).targetRelation(RELATION_WRITE).find().end()
                    .assertAssignments(tolerant ? 3 : 2)
                .end()
                .links().by().resourceOid(resource.oid).find()
                .resolveTarget()
                .display()
                .associations()
                .association(DummyDmsScenario.Account.LinkNames.ACCESS.q())
                .singleValueSatisfying(
                        sav -> LEVEL_WRITE.equals(sav
                                .getAttributesContainerRequired()
                                .findSimpleAttribute(DummyDmsScenario.Access.AttributeNames.LEVEL.q())
                                .getRealValue(String.class)))
                    .associationObject()
                        .attributes()
                            .singleReferenceValueShadow(DummyDmsScenario.Access.LinkNames.DOCUMENT.q())
                                .assertOrigValues(ICFS_NAME, documentName)
                            .end()
                        .end()
                    .end()
                .end()
                .singleValueSatisfying(
                        sav -> LEVEL_READ.equals(sav
                                .getAttributesContainerRequired()
                                .findSimpleAttribute(DummyDmsScenario.Access.AttributeNames.LEVEL.q())
                                .getRealValue(String.class)))
                    .associationObject()
                        .attributes()
                            .singleReferenceValueShadow(DummyDmsScenario.Access.LinkNames.DOCUMENT.q())
                                .assertOrigValues(ICFS_NAME, documentName)
                            .end()
                        .end()
                    .end()
                .end()
                .assertSize(tolerant ? 3 : 2)
                .end()
                .end()
                .end()
                .end()
                .end();

        if (tolerant) {
            // Here we check tolerated association, and corresponding (inbound-created) assignment
            userAsserter
                    .assignments()
                        .by().targetOid(documentOid).targetRelation(RELATION_ADMIN).find().end()
                    .end()
                    .links().by().resourceOid(resource.oid).find()
                    .resolveTarget()
                    .associations()
                    .association(DummyDmsScenario.Account.LinkNames.ACCESS.q())
                    .singleValueSatisfying(
                            sav -> LEVEL_ADMIN.equals(sav
                                    .getAttributesContainerRequired()
                                    .findSimpleAttribute(DummyDmsScenario.Access.AttributeNames.LEVEL.q())
                                    .getRealValue(String.class)))
                        .associationObject()
                            .attributes()
                                .singleReferenceValueShadow(DummyDmsScenario.Access.LinkNames.DOCUMENT.q())
                                    .assertOrigValues(ICFS_NAME, documentName)
                                .end()
                            .end()
                        .end()
                    .end();
        }
        // @formatter:on
    }

    @Test
    public void test300GetAdAccount() throws Exception {
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
    public void test310ImportAdAccount() throws Exception {
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
                    .valueMetadataSingle()
                        .assertMappingName("groups-inbound")
                        .assertMappingObjectOid(RESOURCE_DUMMY_AD.oid)
                        .assertMappingObjectType(ResourceObjectTypeIdentification.ACCOUNT_DEFAULT)
                    .end()
                .end();
        // @formatter:on
    }

    /** Imports AD account throughout its lifecycle: from creation, through modifications, to deletion. */
    @Test
    public void test320ImportChangingAdAccount() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var userName = getTestNameShort();

        given("AD account (in guests group)");
        DummyObject account = adScenario.account.add(userName);
        adScenario.accountGroup.add(account, dummyGuests);

        when("account is imported");
        importAdAccount(userName, result);

        then("user is found");
        // @formatter:off
        assertUserAfterByUsername(userName)
                .assignments()
                .assertAssignments(1)
                .by().targetOid(roleGuests.getOid()).find()
                    .assertTargetRef(roleGuests.getOid(), RoleType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT)
                .end();
        // @formatter:on

        when("a membership of testers is added and the account is re-imported");
        adScenario.accountGroup.add(account, dummyTesters);
        importAdAccount(userName, result);

        then("user has 2 assignments");
        // @formatter:off
        assertUserAfterByUsername(userName)
                .assignments()
                .assertAssignments(2)
                .by().targetOid(roleGuests.getOid()).find()
                    .assertTargetRef(roleGuests.getOid(), RoleType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT)
                .end()
                .by().targetOid(roleTesters.getOid()).find()
                    .assertTargetRef(roleTesters.getOid(), RoleType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT)
                .end();
        // @formatter:on
    }

    /**
     * Creates a AD account, gradually provisioning it with associations.
     * Uses both static and dynamic (mark-based) tolerance.
     *
     * . The user has `guests` membership initially.
     * . Then, the `administrators` and `testers` is added manually on the resource.
     * . After that, `operators` membership is added via midPoint, leading to the following:
     * .. `administrators` should be gone because of non-tolerant setting,
     * .. `testers` should remain because of the mark.
     */
    @Test
    public void test330ProvisionAdAccount() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var userName = "user-" + getTestNameShort();

        given("'testers' are marked as TOLERATED (just checking)");
        var testersShadow = provisioningService.getShadow(shadowTestersOid, null, task, result);
        displayDumpable("testers before", testersShadow);
        assertThat(testersShadow.getEffectiveOperationPolicyRequired().getSynchronize().getMembership().getTolerant())
                .as("testers tolerance override")
                .isEqualTo(true);

        when("user with 'guests' membership is created");
        var user = new UserType()
                .name(userName)
                .assignment(new AssignmentType()
                        .targetRef(roleGuests.getOid(), RoleType.COMPLEX_TYPE));
        var userOid = addObject(user, task, result);

        then("the account in 'guests' exists");
        // @formatter:off
        assertUserAfter(userOid)
                .withObjectResolver(createSimpleModelObjectResolver())
                .assertAssignments(1)
                .singleLink()
                .resolveTarget()
                .display()
                .associations()
                .association(DummyAdTrivialScenario.Account.LinkNames.GROUP.q())
                .singleValue()
                .assertSingleObjectRef(shadowGuestsOid);
        // @formatter:on

        when("manually adding 'administrators' and 'testers' membership");
        adScenario.accountGroup.add(
                adScenario.account.getByNameRequired(userName),
                adScenario.group.getByNameRequired(ROLE_ADMINISTRATORS_NAME));
        adScenario.accountGroup.add(
                adScenario.account.getByNameRequired(userName),
                adScenario.group.getByNameRequired(ROLE_TESTERS_NAME));

        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_AD.oid);

        and("provisioning the user with 'operators' membership");
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT)
                        .add(new AssignmentType()
                                .targetRef(roleOperators.getOid(), RoleType.COMPLEX_TYPE))
                        .asObjectDelta(userOid),
                null, task, result);

        assertSuccess(result);

        then("the account has membership of: guests, testers, operators");
        // @formatter:off
        assertUserAfter(userOid)
                .withObjectResolver(createSimpleModelObjectResolver())
                .assignments()
                    .assertRole(roleGuests.getOid())
                    .assertRole(roleTesters.getOid())
                    .assertRole(roleOperators.getOid())
                    .assertAssignments(3)
                .end()
                .singleLink()
                    .resolveTarget()
                        .display()
                        .associations()
                            .association(DummyAdTrivialScenario.Account.LinkNames.GROUP.q())
                                .forShadowOid(shadowGuestsOid).end()
                                .forShadowOid(shadowTestersOid).end()
                                .forShadowOid(shadowOperatorsOid).end()
                                .assertSize(3)
                            .end()
                        .end()
                    .end()
                .end();
        // @formatter:on
    }

    /** Importing association value whose owning role is indirectly assigned. No assignment should be created. */
    @Test
    public void test340ImportIndirectlyMatched() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var businessRoleName = "business-role-" + getTestNameShort();
        var userName = "user-" + getTestNameShort();

        given("a business role exists that includes 'administrators'");
        var businessRole = new RoleType()
                .name(businessRoleName)
                .inducement(new AssignmentType()
                        .targetRef(roleAdministrators.getOid(), RoleType.COMPLEX_TYPE));
        addObject(businessRole, task, result);

        when("user with the business role is created");
        var user = new UserType()
                .name(userName)
                .assignment(new AssignmentType()
                        .targetRef(businessRole.getOid(), RoleType.COMPLEX_TYPE));
        addObject(user, task, result);

        and("user is reconciled");
        reconcileUser(user.getOid(), task, result);

        then("user has only the single assignment, not the one with 'administrators' role");
        assertUserAfter(user.getOid())
                .assignments()
                .assertRole(businessRole.getOid())
                .assertNoRole(roleAdministrators.getOid())
                .assertAssignments(1);
    }

    /** Membership of an unmanaged group is synchronized only in resource -> midPoint direction. */
    @Test
    public void test350TestUnmanagedGroupMembership() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var userName = "user-" + getTestNameShort();
        var secondUserName = "second-user-" + getTestNameShort();
        var groupName = "group-" + getTestNameShort();

        given("account and group on the resource");
        var dummyAccount = adScenario.account.add(userName);
        var dummyGroup = adScenario.group.add(groupName);
        adScenario.accountGroup.add(dummyAccount, dummyGroup);

        and("group is imported, with shadow marked as Unmanaged");
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_AD.oid)
                .withWholeObjectClass(adScenario.group.getObjectClassName().xsd())
                .withNameValue(groupName)
                .executeOnForeground(result);
        var roleAsserter = assertRoleByName(groupName, "after first import")
                .display();
        var groupShadowOid = roleAsserter
                .singleLink()
                .getOid();
        markShadow(groupShadowOid, MARK_UNMANAGED.oid, task, result);
        var roleOid = roleAsserter.getOid();

        when("account (member of the group) is imported");
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_AD.oid)
                .withNameValue(userName)
                .executeOnForeground(result);

        then("the assignment to the role is created");
        assertUserAfterByUsername(userName)
                .assignments()
                .assertRole(roleOid);

        when("second user is given the newly imported role");
        var secondUser = new UserType()
                .name(secondUserName)
                .assignment(new AssignmentType()
                        .targetRef(roleOid, RoleType.COMPLEX_TYPE));
        var secondUserOid = addObject(secondUser, task, result);

        then("user account is created, but without membership");
        assertUser(secondUserOid, "second user after creation")
                .display()
                .withObjectResolver(createSimpleModelObjectResolver())
                .singleLink()
                .resolveTarget()
                .display()
                .associations()
                .assertValuesCount(0);
    }

    /** Membership of a managed group is synchronized only in midPoint -> resource direction. */
    @Test
    public void test360TestManagedGroupMembership() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var userName = "user-" + getTestNameShort();
        var secondUserName = "second-user-" + getTestNameShort();
        var groupName = "group-" + getTestNameShort();

        given("account and group on the resource");
        var dummyAccount = adScenario.account.add(userName);
        var dummyGroup = adScenario.group.add(groupName);
        adScenario.accountGroup.add(dummyAccount, dummyGroup);

        and("group is imported, with shadow marked as Managed");
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_AD.oid)
                .withWholeObjectClass(adScenario.group.getObjectClassName().xsd())
                .withNameValue(groupName)
                .executeOnForeground(result);
        var roleAsserter = assertRoleByName(groupName, "after first import")
                .display();
        var groupShadowOid = roleAsserter
                .singleLink()
                .getOid();
        markShadow(groupShadowOid, MARK_MANAGED.oid, task, result);
        var roleOid = roleAsserter.getOid();

        when("account (member of the group) is imported");
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_AD.oid)
                .withNameValue(userName)
                .executeOnForeground(result);

        then("the assignment to the role is NOT created");
        assertUserAfterByUsername(userName)
                .assignments()
                .assertNoRole(roleOid)
                .assertAssignments(0);

        when("second user is given the newly imported role");
        var secondUser = new UserType()
                .name(secondUserName)
                .assignment(new AssignmentType()
                        .targetRef(roleOid, RoleType.COMPLEX_TYPE));
        var secondUserOid = addObject(secondUser, task, result);

        then("user account is created, with the membership");
        assertUser(secondUserOid, "second user after creation")
                .display()
                .withObjectResolver(createSimpleModelObjectResolver())
                .assignments()
                .assertRole(roleOid)
                .assertAssignments(1)
                .end()
                .singleLink()
                .resolveTarget()
                .display()
                .associations()
                .association(DummyAdTrivialScenario.Account.LinkNames.GROUP.q())
                .assertShadowOids(groupShadowOid);

        when("second user's group membership is deleted on the resource and the user is reconciled");
        adScenario.accountGroup.delete(
                adScenario.account.getByNameRequired(secondUserName),
                adScenario.group.getByNameRequired(groupName));
        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_AD.oid);
        reconcileUser(secondUserOid, task, result);

        then("the second user's assignment is untouched and the group membership is restored");
        assertUser(secondUserOid, "second user after reconciliation")
                .display()
                .withObjectResolver(createSimpleModelObjectResolver())
                .assignments()
                .assertRole(roleOid)
                .assertAssignments(1)
                .end()
                .singleLink()
                .resolveTarget()
                .display()
                .associations()
                .association(DummyAdTrivialScenario.Account.LinkNames.GROUP.q())
                .assertShadowOids(groupShadowOid);
    }

    /**
     * A group membership is first imported (into assignment), then managed via midPoint.
     *
     * We make sure that even if the membership is deleted on the resource, it is restored by midPoint.
     */
    @Test
    public void test370TestInboundToManagedMembership() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var userName = "user-" + getTestNameShort();
        var groupName = "group-" + getTestNameShort();

        var scenario = createSingleAccountSingleGroup(userName, groupName, result);

        when("group is marked as managed");
        markShadow(scenario.groupShadowOid(), MARK_MANAGED.oid, task, result);

        and("user's group membership is deleted on the resource and the user is reconciled");
        adScenario.accountGroup.delete(scenario.dummyAccount(), scenario.dummyGroup());
        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_AD.oid);
        reconcileUser(scenario.userOid(), task, result);

        then("the second user's assignment is untouched and the group membership is restored");
        assertUser(scenario.userOid(), "user after reconciliation")
                .display()
                .withObjectResolver(createSimpleModelObjectResolver())
                .assignments()
                .assertRole(scenario.roleOid())
                .assertAssignments(1)
                .end()
                .singleLink()
                .resolveTarget()
                .display()
                .associations()
                .association(DummyAdTrivialScenario.Account.LinkNames.GROUP.q())
                .assertShadowOids(scenario.groupShadowOid());
    }

    /** Testing whether we can induce the group membership explicitly via shadow OID (the legacy way). MID-9994. */
    @Test
    public void test380InducingGroupMembershipViaShadowOid() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var userName = "user-" + getTestNameShort();
        var roleName = "role-" + getTestNameShort();
        var groupName = "group-" + getTestNameShort();

        given("group on the resource, marked as managed");
        adScenario.group.add(groupName);
        var groupShadowOid = findShadowRequest()
                .withResource(RESOURCE_DUMMY_AD.getObjectable())
                .withWholeObjectClass(adScenario.group.getObjectClassName().xsd())
                .withNameValue(groupName)
                .findRequired(task, result)
                .getOidRequired();
        markShadow(groupShadowOid, MARK_MANAGED.oid, task, result);

        and("role inducing that group membership");
        var roleOid = "f8a613d0-feb3-4a8f-bc69-c9515ab3c3d4";
        TestObject<RoleType> role = TestObject.templateFile(
                TEST_DIR, ROLE_INDUCING_GROUP_TEMPLATE, roleOid,
                Map.of("#OID#", roleOid, "#NAME#", roleName, "#GROUP#", groupShadowOid));
        role.init(this, task, result);

        when("role is assigned to a user");
        var user = new UserType()
                .name(userName)
                .assignment(role.assignmentTo());
        addObject(user, task, result);

        then("user has the group membership");
        assertSuccess(result);
        assertUserAfter(user.getOid())
                .withObjectResolver(createSimpleModelObjectResolver())
                .singleLink()
                .resolveTarget()
                .display()
                .associations()
                .association(DummyAdTrivialScenario.Account.LinkNames.GROUP.q())
                .assertShadowOids(groupShadowOid);

        // Clean-up objects, to avoid synchronization of no-owner managed group (causes problems here: addFocus, no mappings)
        deleteObject(UserType.class, user.getOid(), task, result);
        deleteObject(RoleType.class, roleOid, task, result);
        deleteObject(ShadowType.class, groupShadowOid, task, result);
    }

    /** Testing whether we can induce the group membership explicitly (the legacy way) - for two association types. MID-9994. */
    @Test
    public void test385InducingGroupMembershipViaShadowOidForTwoGroupTypes() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var userName = "user-" + getTestNameShort();
        var appRoleName = "app-role-" + getTestNameShort();
        var genericRoleName = "generic-role-" + getTestNameShort();
        var appGroupName = "app-group-" + getTestNameShort();
        var genericGroupName = "generic-group-" + getTestNameShort();

        given("groups on the resource, marked as managed");
        adTwoGroupTypesScenario.group.add(appGroupName)
                .addAttributeValue(DummyAdTrivialScenario.Group.AttributeNames.TYPE.local(), "application");
        adTwoGroupTypesScenario.group.add(genericGroupName)
                .addAttributeValue(DummyAdTrivialScenario.Group.AttributeNames.TYPE.local(), "generic");
        var appGroupShadowOid = findShadowRequest()
                .withResource(RESOURCE_DUMMY_AD_TWO_GROUP_TYPES.getObjectable())
                .withWholeObjectClass(adTwoGroupTypesScenario.group.getObjectClassName().xsd())
                .withNameValue(appGroupName)
                .findRequired(task, result)
                .getOidRequired();
        markShadow(appGroupShadowOid, MARK_MANAGED.oid, task, result);
        var genericGroupShadowOid = findShadowRequest()
                .withResource(RESOURCE_DUMMY_AD_TWO_GROUP_TYPES.getObjectable())
                .withWholeObjectClass(adTwoGroupTypesScenario.group.getObjectClassName().xsd())
                .withNameValue(genericGroupName)
                .findRequired(task, result)
                .getOidRequired();
        markShadow(genericGroupShadowOid, MARK_MANAGED.oid, task, result);

        and("role inducing app group membership");
        var appRoleOid = "94a565ef-7724-40b8-92d0-20d84aca62cf";
        TestObject<RoleType> appRole = TestObject.templateFile(
                TEST_DIR, ROLE_INDUCING_GROUP_TEMPLATE_2, appRoleOid,
                Map.of("#OID#", appRoleOid,
                        "#NAME#", appRoleName,
                        "#ASSOCIATION_NAME#", "appGroup",
                        "#GROUP#", appGroupShadowOid));
        appRole.init(this, task, result);

        and("role inducing generic group membership");
        var genericRoleOid = "ad8b95b2-9f1b-45dc-ac4a-0e3bbcb46905";
        TestObject<RoleType> genericRole = TestObject.templateFile(
                TEST_DIR, ROLE_INDUCING_GROUP_TEMPLATE_2, genericRoleOid,
                Map.of("#OID#", genericRoleOid,
                        "#NAME#", genericRoleName,
                        "#ASSOCIATION_NAME#", "genericGroup",
                        "#GROUP#", genericGroupShadowOid));
        genericRole.init(this, task, result);

        when("roles are assigned to a user");
        var user = new UserType()
                .name(userName)
                .assignment(appRole.assignmentTo())
                .assignment(genericRole.assignmentTo());
        addObject(user, task, result);

        then("user has the group memberships");
        assertSuccess(result);
        assertUserAfter(user.getOid())
                .withObjectResolver(createSimpleModelObjectResolver())
                .singleLink()
                .resolveTarget()
                .display()
                .associations()
                .association(RI_APP_GROUP)
                .assertShadowOids(appGroupShadowOid)
                .end()
                .association(RI_GENERIC_GROUP)
                .assertShadowOids(genericGroupShadowOid);

        // Clean-up objects, to avoid synchronization of no-owner managed group (causes problems here: addFocus, no mappings)
        deleteObject(UserType.class, user.getOid(), task, result);
        deleteObject(RoleType.class, appRoleOid, task, result);
        deleteObject(ShadowType.class, appGroupShadowOid, task, result);
        deleteObject(RoleType.class, genericRoleOid, task, result);
        deleteObject(ShadowType.class, genericGroupShadowOid, task, result);
    }

    /**
     * Testing whether we can induce the group membership explicitly via `associationFromLink`
     * with custom projection discriminator in a metarole.
     *
     *   org-child --> org-parent
     *       |             |
     *       |             |
     *       V             V
     *     archetype:org-with-group
     *
     * The archetype provides a group (for the org), plus a group membership to the user (with custom order constraint).
     *
     * The user in this test has an assignment to `org-child` (providing membership in both child and parent groups)
     * and `operators` role, testing the adding of membership via built-in mechanism.
     */
    @Test(description = "MID-10209")
    public void test390InducingGroupMembershipViaAssociationFromLink() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        var orgParentName = "org-parent-" + getTestNameShort();
        var orgChildName = "org-child-" + getTestNameShort();
        var userName = "user-" + getTestNameShort();

        given("org-parent and org-child are created");
        var orgParent = new OrgType()
                .name(orgParentName)
                .assignment(ARCHETYPE_ORG_WITH_GROUP.assignmentTo());
        var orgParentOid = addObject(orgParent, task, result);
        var parentGroupShadowOid = assertOrg(orgParentOid, "parent org before")
                .singleLink()
                .getOid();

        var orgChild = new OrgType()
                .name(orgChildName)
                .assignment(ARCHETYPE_ORG_WITH_GROUP.assignmentTo())
                .assignment(new AssignmentType()
                        .targetRef(orgParentOid, OrgType.COMPLEX_TYPE));
        var orgChildOid = addObject(orgChild, task, result);
        var childGroupShadowOid = assertOrg(orgChildOid, "child org before")
                .singleLink()
                .getOid();

        when("org-child is assigned to a user");
        var user = new UserType()
                .name(userName)
                .assignment(new AssignmentType()
                        .targetRef(orgChildOid, OrgType.COMPLEX_TYPE))
                .assignment(new AssignmentType()
                        .targetRef(roleOperators.getOid(), RoleType.COMPLEX_TYPE));
        addObject(user, task, result);

        then("user is a member of both groups (parent and child)");
        assertSuccess(result);
        assertUserAfter(user.getOid())
                .withObjectResolver(createSimpleModelObjectResolver())
                .singleLink()
                .resolveTarget()
                .display()
                .associations()
                .association(DummyAdTrivialScenario.Account.LinkNames.GROUP.q())
                .assertShadowOids(parentGroupShadowOid, childGroupShadowOid, shadowOperatorsOid);
    }

    /**
     * A group is imported, membership transformed into assignments. Now the group is deleted on the resource.
     * MidPoint should deal with it gracefully.
     *
     * MID-9917
     */
    @Test
    public void test400DeletingGroupWithMembership() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        // will use user recomputation (with reconcile option set)
        var userName1 = "user1-" + getTestNameShort();
        var groupName1 = "group1-" + getTestNameShort();
        var scenario1 = createSingleAccountSingleGroup(userName1, groupName1, result);

        // will use accounts reconciliation
        var userName2 = "user2-" + getTestNameShort();
        var groupName2 = "group2-" + getTestNameShort();
        var scenario2 = createSingleAccountSingleGroup(userName2, groupName2, result);

        when("groups are deleted on the resource");
        adScenario.group.deleteByName(groupName1);
        adScenario.group.deleteByName(groupName2);
        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_AD.oid);

        and("groups are reconciled");
        reconcileAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_AD.oid)
                .withTypeIdentification(TYPE_GROUP)
                .withProcessingAllAccounts()
                .execute(result);

        then("roles are gone");
        assertNoObject(RoleType.class, scenario1.roleOid());
        assertNoObject(RoleType.class, scenario2.roleOid());

        when("user1 is recomputed (reconciled)");
        reconcileUser(scenario1.userOid(), task, result);

        then("everything is OK, assignment is gone");

        // Expression evaluator yields FATAL_ERROR because of ObjectNotFoundException, but the mapping (as ending without
        // an exception) records that as SUCCESS. Maybe unexpected, but that's how it is now. And as it's a minor result,
        // it gets removed afterwards. NOTE: When running with tracing, the FATAL_ERROR can be found deep inside.
        assertSuccess(result);

        assertUserAfter(scenario1.userOid())
                .assertAssignments(0);

        when("accounts are reconciled");
        reconcileAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_AD.oid)
                .withTypeIdentification(ResourceObjectTypeIdentification.ACCOUNT_DEFAULT)
                .withProcessingAllAccounts()
                .execute(result);

        then("assignment is gone");
        assertUserAfter(scenario2.userOid())
                .assertAssignments(0);
    }

    private void importAdAccount(String name, OperationResult result) throws CommonException, IOException {
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_AD.oid)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ShadowKindType.ACCOUNT, INTENT_DEFAULT))
                .withNameValue(name)
                .executeOnForeground(result);
    }

    private @NotNull SingleAccountSingleGroupScenario createSingleAccountSingleGroup(
            String userName, String groupName, OperationResult result)
            throws CommonException, IOException {

        given("account and group on the resource");
        var dummyAccount = adScenario.account.add(userName);
        var dummyGroup = adScenario.group.add(groupName);
        adScenario.accountGroup.add(dummyAccount, dummyGroup);

        and("group is imported");
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_AD.oid)
                .withWholeObjectClass(adScenario.group.getObjectClassName().xsd())
                .withNameValue(groupName)
                .executeOnForeground(result);
        var roleAsserter = assertRoleByName(groupName, "after first import")
                .display();
        var groupShadowOid = roleAsserter
                .singleLink()
                .getOid();
        var roleOid = roleAsserter.getOid();

        when("account (member of the group) is imported");
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_AD.oid)
                .withNameValue(userName)
                .executeOnForeground(result);

        then("the assignment to the role is created");
        var userOid = assertUserAfterByUsername(userName)
                .assignments()
                .assertRole(roleOid)
                .assertAssignments(1)
                .end()
                .getOid();

        return new SingleAccountSingleGroupScenario(dummyAccount, dummyGroup, groupShadowOid, roleOid, userOid);
    }

    private record SingleAccountSingleGroupScenario(
            DummyObject dummyAccount,
            DummyObject dummyGroup,
            String groupShadowOid,
            String roleOid,
            String userOid
    ) {
    }
}
