/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static com.evolveum.midpoint.model.intest.dummys.ScenariosConstants.HR_RESPONSIBILITY;
import static com.evolveum.midpoint.model.intest.dummys.ScenariosConstants.HR_RESPONSIBILITY_PATH;
import static com.evolveum.midpoint.prism.polystring.PolyString.getOrig;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;
import static com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification.ACCOUNT_DEFAULT;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.associations.TestAssociations;
import com.evolveum.midpoint.model.intest.dummys.DummyAdTrivialScenario;
import com.evolveum.midpoint.model.intest.dummys.DummyHrScenarioExtended;
import com.evolveum.midpoint.model.intest.dummys.DummyHrScenarioExtended.Contract;
import com.evolveum.midpoint.model.intest.dummys.DummyHrScenarioExtended.Person;
import com.evolveum.midpoint.model.intest.password.AbstractPasswordTest;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests the value metadata handling for objects, assignments, and other items.
 *
 * Related tests:
 *
 * . Credentials metadata are tested in {@link AbstractPasswordTest} and its subclasses.
 * . Approval and certification related metadata are tested in `workflow-impl` and `certification-impl` modules.
 * . Some association->assignment provenance metadata are checked in {@link TestAssociations}.
 * . Specialized (experimental) metadata handling is tested in {@link TestValueMetadata}.
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestBasicValueMetadata extends AbstractEmptyModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/metadata/basic");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestObject<ObjectTemplateType> TEMPLATE_GUEST = TestObject.file(
            TEST_DIR, "template-guest.xml", "855aec87-a7cf-424a-a85d-a1d53980381d");
    private static final TestObject<ArchetypeType> ARCHETYPE_GUEST = TestObject.file(
            TEST_DIR, "archetype-guest.xml", "2f6505dc-f576-4f23-beee-8c48f197f511");
    private static final TestObject<RoleType> ROLE_GUEST_VIA_TEMPLATE = TestObject.file(
            TEST_DIR, "role-guest-via-template.xml", "bdefc429-304f-4627-b63d-c36de191f989");

    private static final String ATTR_TYPE = "type";
    private static final String ATTR_PERSONAL_NUMBER = "personalNumber";
    private static final String ATTR_LOGIN = "login";
    private static final String ATTR_ORG_UNIT = "orgUnit";

    private static final String TYPE_DEFAULT = "default";
    private static final String TYPE_TEST = "test";
    private static final String INTENT_TEST = "test";
    private static final ResourceObjectTypeIdentification ACCOUNT_TEST =
            ResourceObjectTypeIdentification.of(ShadowKindType.ACCOUNT, INTENT_TEST);

    private static final String ENGINEERING = "Engineering";
    private static final String MANAGEMENT = "Management";
    private static final String MARKETING = "Marketing";

    private static final String CC_1000 = "cc1000";

    private static final String ORG_UNIT_MAPPING_NAME = "orgUnit-to-organizationalUnit";

    private static final ResourceObjectTypeIdentification ACCOUNT_PERSON =
            ResourceObjectTypeIdentification.of(ShadowKindType.ACCOUNT, "person");
    private static final ResourceObjectTypeIdentification ACCOUNT_PERSON_NEW =
            ResourceObjectTypeIdentification.of(ShadowKindType.ACCOUNT, "person-new");
    private static final ResourceObjectTypeIdentification GENERIC_ORG =
            ResourceObjectTypeIdentification.of(ShadowKindType.GENERIC, "org");
    private static final ResourceObjectTypeIdentification GENERIC_COST_CENTER =
            ResourceObjectTypeIdentification.of(ShadowKindType.GENERIC, "costCenter");

    private static final String ONE = "one";
    private static final String TWO = "two";
    private static final String THREE = "three";
    private static final String FOUR = "four";
    private static final String FIVE = "five";

    private static DummyHrScenarioExtended hrScenario;
    private static final DummyTestResource RESOURCE_DUMMY_HR = new DummyTestResource(
            TEST_DIR, "resource-dummy-hr.xml", "0493f2f6-a22f-4259-ac04-4797eabb7d32", "hr",
            c -> hrScenario = DummyHrScenarioExtended.on(c).initialize());

    private static final File HR_310_FILE = new File(TEST_DIR, "resource-dummy-hr-310.xml");
    private static final File HR_320_FILE = new File(TEST_DIR, "resource-dummy-hr-320.xml");

    private static final QName RI_CONTRACTS = new QName(NS_RI, "contracts");
    private static final QName RI_CONTRACTS_NEW = new QName(NS_RI, "contracts-new");

    private static final String CONTRACTS_INBOUND_MAPPING_NAME = "contracts-inbound";
    private static final String RESPONSIBILITIES_INBOUND_MAPPING_NAME = "responsibilities-inbound";

    private static final String RESP_SUPPORT = "support";
    private static final String RESP_DOCUMENTATION = "documentation";
    private static final String RESP_TROUBLESHOOTING = "troubleshooting";
    private static final String RESP_CONSULTING = "consulting";

    private static final DummyTestResource RESOURCE_DUMMY_PROVENANCE = new DummyTestResource(
            TEST_DIR, "resource-dummy-provenance.xml", "a1d2964a-5509-4060-93fc-b812e34429c6",
            "provenance",
            c -> {
                c.addAttrDef(c.getAccountObjectClass(), ATTR_TYPE, String.class, false, false);
                c.addAttrDef(c.getAccountObjectClass(), ATTR_LOGIN, String.class, false, false);
                c.addAttrDef(c.getAccountObjectClass(), ATTR_ORG_UNIT, String.class, false, false);
            });

    private static final DummyTestResource RESOURCE_DUMMY_MULTI_INBOUND = new DummyTestResource(
            TEST_DIR, "resource-dummy-multi-inbound.xml", "70c830d3-3838-4acd-83bf-04b0fdc8afd1",
            "multi-inbound",
            c -> {
                c.addAttrDef(c.getAccountObjectClass(), ATTR_PERSONAL_NUMBER, String.class, false, false);
                c.addAttrDef(c.getAccountObjectClass(), ATTR_LOGIN, String.class, false, false);
                c.addAttrDef(c.getAccountObjectClass(), ATTR_ORG_UNIT, String.class, false, false);
            });

    private static DummyAdTrivialScenario refactoringScenario;
    private static final DummyTestResource RESOURCE_DUMMY_FURIOUS_REFACTORING = new DummyTestResource(
            TEST_DIR, "resource-dummy-furious-refactoring-400.xml", "fb0f44f4-e877-4044-96ce-1af046ec9039",
            "furious-refactoring",
            c -> refactoringScenario = DummyAdTrivialScenario.on(c).initialize());

    private static final File FURIOUS_MAPPING_410_FILE = new File(TEST_DIR, "resource-dummy-furious-refactoring-410.xml");
    private static final File FURIOUS_MAPPING_420_FILE = new File(TEST_DIR, "resource-dummy-furious-refactoring-420.xml");
    private static final File FURIOUS_MAPPING_430_FILE = new File(TEST_DIR, "resource-dummy-furious-refactoring-430.xml");
    private static final File FURIOUS_MAPPING_440_FILE = new File(TEST_DIR, "resource-dummy-furious-refactoring-440.xml");
    private static final File FURIOUS_MAPPING_450_FILE = new File(TEST_DIR, "resource-dummy-furious-refactoring-450.xml");

    private static final String JACK_NAME = "jack";
    private static final String JOHN_NAME = "john";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        initTestObjects(initTask, initResult,
                TEMPLATE_GUEST,
                ARCHETYPE_GUEST,
                ROLE_GUEST_VIA_TEMPLATE);
        RESOURCE_DUMMY_PROVENANCE.initAndTest(this, initTask, initResult);
        RESOURCE_DUMMY_MULTI_INBOUND.initAndTest(this, initTask, initResult);
        RESOURCE_DUMMY_HR.initAndTest(this, initTask, initResult);
        createAndImportCommonHrObjects(initResult);
        RESOURCE_DUMMY_FURIOUS_REFACTORING.initAndTest(this, initTask, initResult);
    }

    /**
     * These objects should be usable in all tests. They should not be modified by tests; if needed, a test should create
     * its own objects to work with.
     */
    private void createAndImportCommonHrObjects(OperationResult result) throws Exception {
        hrScenario.orgUnit.add(ENGINEERING);
        hrScenario.costCenter.add(CC_1000);

        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_HR.oid)
                .withTypeIdentification(GENERIC_ORG)
                .withProcessingAllAccounts()
                .execute(result);
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_HR.oid)
                .withTypeIdentification(GENERIC_COST_CENTER)
                .withProcessingAllAccounts()
                .execute(result);
        assertOrgByName(ENGINEERING, "")
                .display();
        assertOrgByName(CC_1000, "")
                .display();
    }

    @Override
    protected PrismObject<UserType> getDefaultActor() {
        return userAdministrator;
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Test
    public void test100ObjectCreationAndModification() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given("a user");
        var user = new UserType()
                .name(getTestNameShort());

        when("user is added");
        var startTs = XmlTypeConverter.createXMLGregorianCalendar();
        addObject(user, task, result);
        var endTs = XmlTypeConverter.createXMLGregorianCalendar();

        then("creation metadata are present");
        assertUserAfter(user.getOid())
                .valueMetadataSingle()
                .assertCreateMetadataComplex(startTs, endTs)
                .assertModifyTimestampNotPresent();

        // TODO modification
    }

    @Test
    public void test110AssignmentCreationAndModification() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given("a user");
        var user = new UserType()
                .name(getTestNameShort())
                .assignment(ROLE_SUPERUSER.assignmentTo());

        when("user is added");
        var startTs = XmlTypeConverter.createXMLGregorianCalendar();
        addObject(user, task, result);
        var endTs = XmlTypeConverter.createXMLGregorianCalendar();

        then("creation metadata are present");
        assertUserAfter(user.getOid());

        // TODO modification
    }

    @Test
    public void test120AssignmentCreationViaMapping() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given("a user");
        var user = new UserType()
                .name(getTestNameShort())
                .assignment(ARCHETYPE_GUEST.assignmentTo());

        when("user is added");
        var startTs = XmlTypeConverter.createXMLGregorianCalendar();
        addObject(user, task, result);
        var endTs = XmlTypeConverter.createXMLGregorianCalendar();

        then("creation metadata are present");
        assertUserAfter(user.getOid());

        // TODO modification
    }

    @Test
    public void test130AssignmentCreationViaMappingAndExplicit() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given("a user");
        var user = new UserType()
                .name(getTestNameShort())
                .assignment(ARCHETYPE_GUEST.assignmentTo())
                .assignment(ROLE_GUEST_VIA_TEMPLATE.assignmentTo());

        when("user is added");
        var startTs = XmlTypeConverter.createXMLGregorianCalendar();
        addObject(user, task, result);
        var endTs = XmlTypeConverter.createXMLGregorianCalendar();

        then("creation metadata are present");
        assertUserAfter(user.getOid());

        // TODO modification
    }

    /**
     * Checks the provenance-based ranges for inbound processing. It uses the `ri:orgUnit` to `organizationalUnit` mapping,
     * with changing values for two accounts (having distinct object types) on the `provenance` resource.
     */
    @Test
    public void test200InboundProvenanceAndRanges() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var loginName = getTestNameShort();

        given("two accounts on 'provenance' resource");
        var firstAccount = RESOURCE_DUMMY_PROVENANCE.controller.addAccount(loginName + "-1")
                .addAttributeValue(ATTR_TYPE, TYPE_DEFAULT)
                .addAttributeValue(ATTR_LOGIN, loginName)
                .addAttributeValue(ATTR_ORG_UNIT, ENGINEERING);
        var secondAccount = RESOURCE_DUMMY_PROVENANCE.controller.addAccount(loginName + "-2")
                .addAttributeValue(ATTR_TYPE, TYPE_TEST)
                .addAttributeValue(ATTR_LOGIN, loginName)
                .addAttributeValue(ATTR_ORG_UNIT, MANAGEMENT);

        when("accounts are imported");
        importProvenanceAccounts(result);

        then("metadata are OK on organizationalUnit: default -> engineering, test -> management");
        assertUserByUsername(loginName, "after import")
                .assertOrganizationalUnits(ENGINEERING, MANAGEMENT)
                .valueMetadata(UserType.F_ORGANIZATIONAL_UNIT, origValueSelector(ENGINEERING))
                .singleValue()
                .assertMappingObjectOid(RESOURCE_DUMMY_PROVENANCE.oid)
                .assertMappingName(ORG_UNIT_MAPPING_NAME)
                .assertMappingObjectType(ACCOUNT_DEFAULT)
                .assertMappingAssociationType(null)
                .assertMappingTag(null)
                .end()
                .end()
                .valueMetadata(UserType.F_ORGANIZATIONAL_UNIT, origValueSelector(MANAGEMENT))
                .singleValue()
                .assertMappingObjectOid(RESOURCE_DUMMY_PROVENANCE.oid)
                .assertMappingName(ORG_UNIT_MAPPING_NAME)
                .assertMappingObjectType(ACCOUNT_TEST)
                .assertMappingAssociationType(null)
                .assertMappingTag(null)
                .end();

        when("for a test account, 'management' org unit is changed to 'marketing', and accounts are re-imported");
        secondAccount.replaceAttributeValue(ATTR_ORG_UNIT, MARKETING);
        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_PROVENANCE.oid);
        importProvenanceAccounts(result);

        then("metadata are OK on organizationalUnit: default -> engineering, test -> marketing");
        assertUserByUsername(loginName, "after re-import")
                .assertOrganizationalUnits(ENGINEERING, MARKETING)
                .valueMetadata(UserType.F_ORGANIZATIONAL_UNIT, origValueSelector(ENGINEERING))
                .singleValue()
                .assertMappingObjectOid(RESOURCE_DUMMY_PROVENANCE.oid)
                .assertMappingName(ORG_UNIT_MAPPING_NAME)
                .assertMappingObjectType(ACCOUNT_DEFAULT)
                .assertMappingAssociationType(null)
                .assertMappingTag(null)
                .end()
                .end()
                .valueMetadata(UserType.F_ORGANIZATIONAL_UNIT, origValueSelector(MARKETING))
                .singleValue()
                .assertMappingObjectOid(RESOURCE_DUMMY_PROVENANCE.oid)
                .assertMappingName(ORG_UNIT_MAPPING_NAME)
                .assertMappingObjectType(ACCOUNT_TEST)
                .assertMappingAssociationType(null)
                .assertMappingTag(null)
                .end();

        when("for a test account, 'marketing' org unit is changed to 'engineering', and accounts are re-imported");
        secondAccount.replaceAttributeValue(ATTR_ORG_UNIT, ENGINEERING);
        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_PROVENANCE.oid);
        importProvenanceAccounts(result);

        then("metadata are OK on organizationalUnit: default, test -> engineering");
        assertUserByUsername(loginName, "after re-import")
                .assertOrganizationalUnits(ENGINEERING)
                .valueMetadata(UserType.F_ORGANIZATIONAL_UNIT, origValueSelector(ENGINEERING))
                .assertSize(2)
                .value(v -> ACCOUNT_DEFAULT.asBean().equals(getMappingObjectType(v)))
                .assertMappingObjectOid(RESOURCE_DUMMY_PROVENANCE.oid)
                .assertMappingName(ORG_UNIT_MAPPING_NAME)
                .assertMappingObjectType(ACCOUNT_DEFAULT)
                .assertMappingAssociationType(null)
                .assertMappingTag(null)
                .end()
                .value(v -> ACCOUNT_TEST.asBean().equals(getMappingObjectType(v)))
                .assertMappingObjectOid(RESOURCE_DUMMY_PROVENANCE.oid)
                .assertMappingName(ORG_UNIT_MAPPING_NAME)
                .assertMappingObjectType(ACCOUNT_TEST)
                .assertMappingAssociationType(null)
                .assertMappingTag(null);

        when("for a test account, 'engineering' org unit is changed back to 'marketing', and accounts are re-imported");
        secondAccount.replaceAttributeValue(ATTR_ORG_UNIT, MARKETING);
        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_PROVENANCE.oid);
        importProvenanceAccounts(result);

        then("metadata are OK on organizationalUnit: default -> engineering, test -> marketing");
        assertUserByUsername(loginName, "after re-import")
                .assertOrganizationalUnits(ENGINEERING, MARKETING)
                .valueMetadata(UserType.F_ORGANIZATIONAL_UNIT, origValueSelector(ENGINEERING))
                .singleValue()
                .assertMappingObjectOid(RESOURCE_DUMMY_PROVENANCE.oid)
                .assertMappingName(ORG_UNIT_MAPPING_NAME)
                .assertMappingObjectType(ACCOUNT_DEFAULT)
                .assertMappingAssociationType(null)
                .assertMappingTag(null)
                .end()
                .end()
                .valueMetadata(UserType.F_ORGANIZATIONAL_UNIT, origValueSelector(MARKETING))
                .singleValue()
                .assertMappingObjectOid(RESOURCE_DUMMY_PROVENANCE.oid)
                .assertMappingName(ORG_UNIT_MAPPING_NAME)
                .assertMappingObjectType(ACCOUNT_TEST)
                .assertMappingAssociationType(null)
                .assertMappingTag(null)
                .end();

        when("for the accounts, org units are swapped, and accounts are re-imported");
        firstAccount.replaceAttributeValue(ATTR_ORG_UNIT, MARKETING);
        secondAccount.replaceAttributeValue(ATTR_ORG_UNIT, ENGINEERING);
        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_PROVENANCE.oid);
        importProvenanceAccounts(result);

        then("metadata are OK on organizationalUnit: default -> marketing, test -> engineering");
        assertUserByUsername(loginName, "after re-import")
                .assertOrganizationalUnits(ENGINEERING, MARKETING)
                .valueMetadata(UserType.F_ORGANIZATIONAL_UNIT, origValueSelector(MARKETING))
                .singleValue()
                .assertMappingObjectOid(RESOURCE_DUMMY_PROVENANCE.oid)
                .assertMappingName(ORG_UNIT_MAPPING_NAME)
                .assertMappingObjectType(ACCOUNT_DEFAULT)
                .assertMappingAssociationType(null)
                .assertMappingTag(null)
                .end()
                .end()
                .valueMetadata(UserType.F_ORGANIZATIONAL_UNIT, origValueSelector(ENGINEERING))
                .singleValue()
                .assertMappingObjectOid(RESOURCE_DUMMY_PROVENANCE.oid)
                .assertMappingName(ORG_UNIT_MAPPING_NAME)
                .assertMappingObjectType(ACCOUNT_TEST)
                .assertMappingAssociationType(null)
                .assertMappingTag(null)
                .end();

        when("an account is deleted and the resource is reconciled");
        RESOURCE_DUMMY_PROVENANCE.controller.deleteAccount(secondAccount.getName());
        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_PROVENANCE.oid);
        reconcileProvenanceAccounts(result);

        assertUserAfterByUsername(loginName);

        // FIXME the value is not removed on delete. The projector is currently not able to handle this.
        //  1. inbounds for projections that are gone, are skipped
        //  2. even if they were not, the inbound mappings are not started for individual attributes, as they are thought to be missing (why?!)

//        then("metadata are OK on organizationalUnit: default -> marketing");
//        assertUserAfterByUsername(loginName)
//                .assertOrganizationalUnits(MARKETING)
//                .valueMetadata(UserType.F_ORGANIZATIONAL_UNIT, v -> MARKETING.equals(getOrig(v.<PolyString>getRealValue())))
//                .singleValue()
//                .assertMappingObjectOid(RESOURCE_DUMMY_PROVENANCE.oid)
//                .assertMappingName(ORG_UNIT_MAPPING_NAME)
//                .assertMappingObjectType(ACCOUNT_DEFAULT)
//                .assertMappingAssociationType(null)
//                .assertMappingTag(null);
    }

    private void importProvenanceAccounts(OperationResult result) throws CommonException, IOException {
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_PROVENANCE.oid)
                .withWholeObjectClass(RI_ACCOUNT_OBJECT_CLASS)
                .withProcessingAllAccounts()
                .execute(result);
    }

    private void reconcileProvenanceAccounts(OperationResult result) throws CommonException, IOException {
        reconcileAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_PROVENANCE.oid)
                .withWholeObjectClass(RI_ACCOUNT_OBJECT_CLASS)
                .withProcessingAllAccounts()
                .execute(result);
    }

    /**
     * Checks the provenance-based ranges for inbound processing. It uses the `ri:orgUnit` to `organizationalUnit` mapping,
     * with changing values for three accounts (having distinct shadow tags) on the `multi-intent` resource.
     */
    @Test
    public void test210InboundProvenanceAndRangesWithTags() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var loginName = getTestNameShort();

        var firstPersonalNumber = "10701111";
        var secondPersonalNumber = "10702222";
        var thirdPersonalNumber = "10903333";

        given("three related accounts on 'multi-inbound' resource");
        RESOURCE_DUMMY_MULTI_INBOUND.controller.addAccount(loginName + "-1")
                .addAttributeValue(ATTR_PERSONAL_NUMBER, firstPersonalNumber)
                .addAttributeValue(ATTR_LOGIN, loginName)
                .addAttributeValue(ATTR_ORG_UNIT, ENGINEERING);
        var secondAccount = RESOURCE_DUMMY_MULTI_INBOUND.controller.addAccount(loginName + "-2")
                .addAttributeValue(ATTR_PERSONAL_NUMBER, secondPersonalNumber)
                .addAttributeValue(ATTR_LOGIN, loginName)
                .addAttributeValue(ATTR_ORG_UNIT, ENGINEERING);
        var thirdAccount = RESOURCE_DUMMY_MULTI_INBOUND.controller.addAccount(loginName + "-3")
                .addAttributeValue(ATTR_PERSONAL_NUMBER, thirdPersonalNumber)
                .addAttributeValue(ATTR_LOGIN, loginName)
                .addAttributeValue(ATTR_ORG_UNIT, MANAGEMENT);

        when("accounts are imported");
        importMultiInboundAccounts(result);

        then("metadata are OK on organizationalUnit: 1st, 2nd -> engineering, 3rd -> management");
        assertUserByUsername(loginName, "after import")
                .valueMetadata(UserType.F_ORGANIZATIONAL_UNIT, origValueSelector(ENGINEERING))
                .assertSize(2)
                .value(v -> firstPersonalNumber.equals(getMappingSpecificationTag(v)))
                .assertMappingObjectOid(RESOURCE_DUMMY_MULTI_INBOUND.oid)
                .assertMappingName(ORG_UNIT_MAPPING_NAME)
                .assertMappingObjectType(ACCOUNT_DEFAULT)
                .assertMappingAssociationType(null)
                .assertMappingTag(firstPersonalNumber)
                .end()
                .value(v -> secondPersonalNumber.equals(getMappingSpecificationTag(v)))
                .assertMappingObjectOid(RESOURCE_DUMMY_MULTI_INBOUND.oid)
                .assertMappingName(ORG_UNIT_MAPPING_NAME)
                .assertMappingObjectType(ACCOUNT_DEFAULT)
                .assertMappingAssociationType(null)
                .assertMappingTag(secondPersonalNumber)
                .end()
                .end()
                .valueMetadata(UserType.F_ORGANIZATIONAL_UNIT, origValueSelector(MANAGEMENT))
                .singleValue()
                .assertMappingObjectOid(RESOURCE_DUMMY_MULTI_INBOUND.oid)
                .assertMappingName(ORG_UNIT_MAPPING_NAME)
                .assertMappingObjectType(ACCOUNT_DEFAULT)
                .assertMappingAssociationType(null)
                .assertMappingTag(thirdPersonalNumber)
                .end();

        when("organization units are changed on the accounts and accounts are re-imported");
        secondAccount.replaceAttributeValue(ATTR_ORG_UNIT, MANAGEMENT);
        thirdAccount.replaceAttributeValue(ATTR_ORG_UNIT, MARKETING);
        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_MULTI_INBOUND.oid);
        importMultiInboundAccounts(result);

        then("metadata are OK on organizationalUnit: 1st -> engineering, 2nd -> management, 3rd -> marketing");
        assertUserByUsername(loginName, "after re-import")
                .valueMetadata(UserType.F_ORGANIZATIONAL_UNIT, origValueSelector(ENGINEERING))
                .singleValue()
                .assertMappingObjectOid(RESOURCE_DUMMY_MULTI_INBOUND.oid)
                .assertMappingObjectType(ACCOUNT_DEFAULT)
                .assertMappingAssociationType(null)
                .assertMappingTag(firstPersonalNumber)
                .end()
                .end()
                .valueMetadata(UserType.F_ORGANIZATIONAL_UNIT, origValueSelector(MANAGEMENT))
                .singleValue()
                .assertMappingObjectOid(RESOURCE_DUMMY_MULTI_INBOUND.oid)
                .assertMappingObjectType(ACCOUNT_DEFAULT)
                .assertMappingAssociationType(null)
                .assertMappingTag(secondPersonalNumber)
                .end()
                .end()
                .valueMetadata(UserType.F_ORGANIZATIONAL_UNIT, origValueSelector(MARKETING))
                .singleValue()
                .assertMappingObjectOid(RESOURCE_DUMMY_MULTI_INBOUND.oid)
                .assertMappingObjectType(ACCOUNT_DEFAULT)
                .assertMappingAssociationType(null)
                .assertMappingTag(thirdPersonalNumber)
                .end();

        when("organization unit is removed from the 3rd account; and accounts are re-imported");
        thirdAccount.replaceAttributeValues(ATTR_ORG_UNIT);
        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_MULTI_INBOUND.oid);
        importMultiInboundAccounts(result);

        then("metadata are OK on organizationalUnit: 1st -> engineering, 2nd -> management");
        assertUserByUsername(loginName, "after re-import")
                .assertOrganizationalUnits(ENGINEERING, MANAGEMENT)
                .valueMetadata(UserType.F_ORGANIZATIONAL_UNIT, origValueSelector(ENGINEERING))
                .singleValue()
                .assertMappingObjectOid(RESOURCE_DUMMY_MULTI_INBOUND.oid)
                .assertMappingObjectType(ACCOUNT_DEFAULT)
                .assertMappingAssociationType(null)
                .assertMappingTag(firstPersonalNumber)
                .end()
                .end()
                .valueMetadata(UserType.F_ORGANIZATIONAL_UNIT, origValueSelector(MANAGEMENT))
                .singleValue()
                .assertMappingObjectOid(RESOURCE_DUMMY_MULTI_INBOUND.oid)
                .assertMappingObjectType(ACCOUNT_DEFAULT)
                .assertMappingAssociationType(null)
                .assertMappingTag(secondPersonalNumber)
                .end()
                .end();

        when("2nd account is removed; and accounts are reconciled");
        RESOURCE_DUMMY_MULTI_INBOUND.controller.deleteAccount(secondAccount.getName());
        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_MULTI_INBOUND.oid);
        reconcileMultiInboundAccounts(result);

        // FIXME the value is not removed; it is the same situation as in the above test

//        then("metadata are OK on organizationalUnit: 1st -> engineering");
//        assertUserAfterByUsername(loginName)
//                .assertOrganizationalUnits(ENGINEERING);
    }

    private void importMultiInboundAccounts(OperationResult result) throws CommonException, IOException {
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_MULTI_INBOUND.oid)
                .withTypeIdentification(ACCOUNT_DEFAULT)
                .withProcessingAllAccounts()
                .execute(result);
    }

    private void reconcileMultiInboundAccounts(OperationResult result) throws CommonException, IOException {
        reconcileAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_MULTI_INBOUND.oid)
                .withTypeIdentification(ACCOUNT_DEFAULT)
                .withProcessingAllAccounts()
                .execute(result);
    }

    /**
     * Tests the behavior of complex inbound mappings for assignments; the top-level ones as well as inner ones.
     *
     * Multi-valued items in the assignment should have their own provenance metadata (provided by inner mappings),
     * and should respect them when ranges are calculated.
     */
    @Test
    public void test300ComplexInboundAssignmentMappings() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given("a HR account with engineering contract");
        var john = hrScenario.person.add(JOHN_NAME)
                .addAttributeValue(Person.AttributeNames.FIRST_NAME.local(), "John")
                .addAttributeValue(Person.AttributeNames.LAST_NAME.local(), "Smith");
        var engineeringContract = hrScenario.contract.add("john-engineering")
                .addAttributeValues(Contract.AttributeNames.RESPONSIBILITY.local(), RESP_SUPPORT);
        hrScenario.personContract.add(john, engineeringContract);
        hrScenario.contractOrgUnit.add(john, hrScenario.orgUnit.getByNameRequired(ENGINEERING));
        hrScenario.contractCostCenter.add(john, hrScenario.costCenter.getByNameRequired(CC_1000));

        when("account is imported");
        importHrAccounts(result);

        then("assignment and responsibility metadata are OK; import -> support");
        var asserter = assertUserByUsername(JOHN_NAME, "after import");
        var assignmentAsserter = asserter.assignments().single();
        assignmentAsserter
                .valueMetadataSingle()
                .assertMappingObjectOid(RESOURCE_DUMMY_HR.oid)
                .assertMappingObjectType(ACCOUNT_PERSON)
                .assertMappingAssociationType(RI_CONTRACTS)
                .assertMappingName(CONTRACTS_INBOUND_MAPPING_NAME)
                .assertMappingTag(null)
                .end()
                .extension()
                .assertPropertyValuesEqual(HR_RESPONSIBILITY, RESP_SUPPORT)
                .end()
                .valueMetadata(HR_RESPONSIBILITY_PATH, valueSelector(RESP_SUPPORT))
                .singleValue()
                .assertMappingObjectOid(RESOURCE_DUMMY_HR.oid)
                .assertMappingName(RESPONSIBILITIES_INBOUND_MAPPING_NAME)
                .assertMappingObjectType(ACCOUNT_PERSON)
                .assertMappingAssociationType(RI_CONTRACTS)
                .assertMappingTag(null);

        var userOid = asserter.getOid();
        var assignmentId = assignmentAsserter.getAssignment().getId();

        when("responsibilities are added manually");
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT, assignmentId, AssignmentType.F_EXTENSION, HR_RESPONSIBILITY)
                        .add(
                                addValueMetadata(RESP_DOCUMENTATION),
                                addValueMetadata(RESP_TROUBLESHOOTING))
                        .asObjectDelta(userOid),
                null, task, result);

        then("assignment and responsibility metadata are OK; import -> support, user -> documentation, troubleshooting");
        assertUserByUsername(JOHN_NAME, "after manual change")
                .assignments()
                .single()
                .valueMetadataSingle()
                .assertCreateChannel(CHANNEL_IMPORT_URI) // originally created by import
                .assertModifyChannel(CHANNEL_USER_URI) // then modified manually
                .assertMappingObjectOid(RESOURCE_DUMMY_HR.oid)
                .assertMappingObjectType(ACCOUNT_PERSON)
                .assertMappingAssociationType(RI_CONTRACTS)
                .assertMappingName(CONTRACTS_INBOUND_MAPPING_NAME)
                .assertMappingTag(null)
                .end()
                .extension()
                .assertPropertyValuesEqual(HR_RESPONSIBILITY, RESP_SUPPORT, RESP_DOCUMENTATION, RESP_TROUBLESHOOTING)
                .end()
                .valueMetadata(HR_RESPONSIBILITY_PATH, valueSelector(RESP_SUPPORT))
                .singleValue()
                .assertMappingObjectOid(RESOURCE_DUMMY_HR.oid)
                .assertMappingName(RESPONSIBILITIES_INBOUND_MAPPING_NAME)
                .assertMappingObjectType(ACCOUNT_PERSON)
                .assertMappingAssociationType(RI_CONTRACTS)
                .assertMappingTag(null)
                .end()
                .end()
                .valueMetadata(HR_RESPONSIBILITY_PATH, valueSelector(RESP_DOCUMENTATION))
                .singleValue()
                .assertNoMappingSpec()
                .assertAcquisitionChannel(CHANNEL_USER_URI)
                .end()
                .end()
                .valueMetadata(HR_RESPONSIBILITY_PATH, valueSelector(RESP_TROUBLESHOOTING))
                .singleValue()
                .assertNoMappingSpec()
                .assertAcquisitionChannel(CHANNEL_USER_URI)
                .end()
                .end();

        when("a responsibility of 'documentation' is added and accounts are re-imported");
        engineeringContract.addAttributeValue(Contract.AttributeNames.RESPONSIBILITY.local(), RESP_DOCUMENTATION);
        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_HR.oid);
        importHrAccounts(result);

        then("metadata are OK; import -> support, documentation; user -> documentation, troubleshooting");
        assertUserByUsername(JOHN_NAME, "after import")
                .assignments()
                .single()
                .valueMetadataSingle()
                .assertCreateChannel(CHANNEL_IMPORT_URI) // originally created by import
                .assertModifyChannel(CHANNEL_IMPORT_URI) // then modified manually and then by import
                .assertMappingObjectOid(RESOURCE_DUMMY_HR.oid)
                .assertMappingObjectType(ACCOUNT_PERSON)
                .assertMappingAssociationType(RI_CONTRACTS)
                .assertMappingName(CONTRACTS_INBOUND_MAPPING_NAME)
                .assertMappingTag(null)
                .end()
                .extension()
                .assertPropertyValuesEqual(HR_RESPONSIBILITY, RESP_SUPPORT, RESP_DOCUMENTATION, RESP_TROUBLESHOOTING)
                .end()
                .valueMetadata(HR_RESPONSIBILITY_PATH, valueSelector(RESP_SUPPORT))
                .singleValue()
                .assertMappingObjectOid(RESOURCE_DUMMY_HR.oid)
                .assertMappingName(RESPONSIBILITIES_INBOUND_MAPPING_NAME)
                .assertMappingObjectType(ACCOUNT_PERSON)
                .assertMappingAssociationType(RI_CONTRACTS)
                .assertMappingTag(null)
                .end()
                .end()
                .valueMetadata(HR_RESPONSIBILITY_PATH, valueSelector(RESP_DOCUMENTATION))
                .assertSize(2)
                .value(v -> getMappingSpecification(v) == null)
                .assertNoMappingSpec()
                .assertAcquisitionChannel(CHANNEL_USER_URI)
                .end()
                .value(v -> getMappingSpecification(v) != null)
                .assertMappingObjectOid(RESOURCE_DUMMY_HR.oid)
                .assertMappingName(RESPONSIBILITIES_INBOUND_MAPPING_NAME)
                .assertMappingObjectType(ACCOUNT_PERSON)
                .assertMappingAssociationType(RI_CONTRACTS)
                .assertMappingTag(null)
                .end()
                .end()
                .valueMetadata(HR_RESPONSIBILITY_PATH, valueSelector(RESP_TROUBLESHOOTING))
                .singleValue()
                .assertNoMappingSpec()
                .assertAcquisitionChannel(CHANNEL_USER_URI)
                .end()
                .end();

        when("responsibilities on the HR resource are replaced by 'consulting' and accounts are re-imported");
        engineeringContract.replaceAttributeValue(Contract.AttributeNames.RESPONSIBILITY.local(), RESP_CONSULTING);
        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_HR.oid);
        importHrAccounts(result);

        then("metadata are OK; user -> documentation, troubleshooting; import -> consulting");
        assertUserByUsername(JOHN_NAME, "after import")
                .assignments()
                .single()
                .valueMetadataSingle()
                .assertCreateChannel(CHANNEL_IMPORT_URI) // originally created by import
                .assertModifyChannel(CHANNEL_IMPORT_URI) // then modified manually and then by import (2x)
                .assertMappingObjectOid(RESOURCE_DUMMY_HR.oid)
                .assertMappingObjectType(ACCOUNT_PERSON)
                .assertMappingAssociationType(RI_CONTRACTS)
                .assertMappingName(CONTRACTS_INBOUND_MAPPING_NAME)
                .assertMappingTag(null)
                .end()
                .extension()
                .assertPropertyValuesEqual(HR_RESPONSIBILITY, RESP_DOCUMENTATION, RESP_TROUBLESHOOTING, RESP_CONSULTING)
                .end()
                .valueMetadata(HR_RESPONSIBILITY_PATH, valueSelector(RESP_DOCUMENTATION))
                .singleValue()
                .assertNoMappingSpec()
                .assertAcquisitionChannel(CHANNEL_USER_URI)
                .end()
                .end()
                .valueMetadata(HR_RESPONSIBILITY_PATH, valueSelector(RESP_TROUBLESHOOTING))
                .singleValue()
                .assertNoMappingSpec()
                .assertAcquisitionChannel(CHANNEL_USER_URI)
                .end()
                .end()
                .valueMetadata(HR_RESPONSIBILITY_PATH, valueSelector(RESP_CONSULTING))
                .singleValue()
                .assertMappingObjectOid(RESOURCE_DUMMY_HR.oid)
                .assertMappingName(RESPONSIBILITIES_INBOUND_MAPPING_NAME)
                .assertMappingObjectType(ACCOUNT_PERSON)
                .assertMappingAssociationType(RI_CONTRACTS)
                .assertMappingTag(null)
                .end();
    }

    /**
     * Tests the renaming of an association type and intent.
     *
     * The assignment correlation is not changed.
     *
     * - depends on {@link #test300ComplexInboundAssignmentMappings()}
     * - changes the definition of the HR resource!
     */
    @Test
    public void test310RenamingAssociationTypeAndIntent() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given("association type and intent are renamed");
        updateHrResource(HR_310_FILE);

        when("accounts are imported");
        importHrAccountsNew(result);

        then("metadata are OK; documentation, troubleshooting <- user; consulting <- import (old + new)");
        assertUserByUsername(JOHN_NAME, "after import")
                .assignments()
                .single()
                // There's a single metadata currently here, and it's the old one. The reason is that even if new mapping spec
                // is in place, the old spec is still there, as there is no migration (MID-10084), plus a new one is not provided,
                // because the assignment was created in the past, not now - this is reported by MID-10084 as well.
                .valueMetadataSingle()
                .assertCreateChannel(CHANNEL_IMPORT_URI) // originally created by import
                .assertModifyChannel(CHANNEL_IMPORT_URI) // then modified manually and then by import (2x)
                .assertMappingObjectOid(RESOURCE_DUMMY_HR.oid)
                .assertMappingObjectType(ACCOUNT_PERSON) // old
                .assertMappingAssociationType(RI_CONTRACTS) // old
                .assertMappingName(CONTRACTS_INBOUND_MAPPING_NAME)
                .assertMappingTag(null)
                .end()
                .extension()
                .assertPropertyValuesEqual(HR_RESPONSIBILITY, RESP_DOCUMENTATION, RESP_TROUBLESHOOTING, RESP_CONSULTING)
                .end()
                .valueMetadata(HR_RESPONSIBILITY_PATH, valueSelector(RESP_DOCUMENTATION))
                .singleValue()
                .assertNoMappingSpec()
                .assertAcquisitionChannel(CHANNEL_USER_URI)
                .end()
                .end()
                .valueMetadata(HR_RESPONSIBILITY_PATH, valueSelector(RESP_TROUBLESHOOTING))
                .singleValue()
                .assertNoMappingSpec()
                .assertAcquisitionChannel(CHANNEL_USER_URI)
                .end()
                .end()
                .valueMetadata(HR_RESPONSIBILITY_PATH, valueSelector(RESP_CONSULTING))
                .assertSize(2) // old spec is still here (MID-10084)
                .value(v -> ACCOUNT_PERSON.asBean().equals(getMappingObjectType(v)))
                .assertMappingObjectOid(RESOURCE_DUMMY_HR.oid)
                .assertMappingName(RESPONSIBILITIES_INBOUND_MAPPING_NAME)
                .assertMappingObjectType(ACCOUNT_PERSON)
                .assertMappingAssociationType(RI_CONTRACTS)
                .assertMappingTag(null)
                .end()
                .value(v -> ACCOUNT_PERSON_NEW.asBean().equals(getMappingObjectType(v)))
                .assertMappingObjectOid(RESOURCE_DUMMY_HR.oid)
                .assertMappingName(RESPONSIBILITIES_INBOUND_MAPPING_NAME)
                .assertMappingObjectType(ACCOUNT_PERSON_NEW)
                .assertMappingAssociationType(RI_CONTRACTS_NEW)
                .assertMappingTag(null)
                .end();
    }

    /**
     * Continuing testing the renaming of an association type and intent: we change the rule for assignment correlation,
     * so that new assignment should be created.
     *
     * All user data about responsibilities are gone, as the assignment is re-created.
     *
     * - depends on previous tests;
     * - changes the definition of the HR resource.
     */
    @Test
    public void test320RenamingAssociationTypeAndIntent() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given("correlation rule for assignments is changed");
        updateHrResource(HR_320_FILE);

        when("accounts are imported");
        importHrAccountsNew(result);

        then("metadata are OK; consulting <- import");
        assertUserByUsername(JOHN_NAME, "after import")
                .assignments()
                .single()
                // There's a single metadata currently here, and it's the old one. The reason is that even if new mapping spec
                // is in place, the old spec is still there, as there is no migration (MID-10084), plus a new one is not provided,
                // because the assignment was created in the past, not now - this is reported by MID-10084 as well.
                .valueMetadataSingle()
                .assertCreateChannel(CHANNEL_IMPORT_URI) // created by this import
                .assertMappingObjectOid(RESOURCE_DUMMY_HR.oid)
                .assertMappingObjectType(ACCOUNT_PERSON_NEW)
                .assertMappingAssociationType(RI_CONTRACTS_NEW)
                .assertMappingName(CONTRACTS_INBOUND_MAPPING_NAME)
                .assertMappingTag(null)
                .end()
                .extension()
                .assertPropertyValuesEqual(HR_RESPONSIBILITY, RESP_CONSULTING)
                .end()
                .valueMetadata(HR_RESPONSIBILITY_PATH, valueSelector(RESP_CONSULTING))
                .singleValue()
                .assertMappingObjectOid(RESOURCE_DUMMY_HR.oid)
                .assertMappingName(RESPONSIBILITIES_INBOUND_MAPPING_NAME)
                .assertMappingObjectType(ACCOUNT_PERSON_NEW)
                .assertMappingAssociationType(RI_CONTRACTS_NEW)
                .assertMappingTag(null)
                .end();
    }

    private PrismPropertyValue<?> addValueMetadata(Object realValue) throws SchemaException {
        var ppv = prismContext.itemFactory().createPropertyValue(realValue);
        ppv.getValueMetadata().addMetadataValue(
                new ValueMetadataType()
                        .provenance(new ProvenanceMetadataType()
                                .acquisition(new ProvenanceAcquisitionType()
                                        .channel(CHANNEL_USER_URI)))
                        .asPrismContainerValue());
        return ppv;
    }

    private void importHrAccounts(OperationResult result) throws CommonException, IOException {
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_HR.oid)
                .withTypeIdentification(ACCOUNT_PERSON)
                .withProcessingAllAccounts()
                .execute(result);
    }

    private void importHrAccountsNew(OperationResult result) throws CommonException, IOException {
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_HR.oid)
                .withTypeIdentification(ACCOUNT_PERSON_NEW)
                .withProcessingAllAccounts()
                .withTracing()
                .execute(result);
    }

    /**
     * This is a start of a series of tests that will test naming and renaming various configuration items:
     * mapping, object types, and association types.
     */
    @Test
    public void test400UnnamedMapping() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given("an account on 'furious-refactoring' resource");
        var jack = refactoringScenario.account.add(JACK_NAME)
                .addAttributeValue(DummyAdTrivialScenario.Account.AttributeNames.DESCRIPTION.local(), ONE);

        when("account is imported");
        importFuriousRefactoringAccounts(result);

        then("account is imported with the description 'one'");
        assertUserByUsername(JACK_NAME, "after import")
                .assertOrganizationalUnits(ONE)
                .valueMetadata(UserType.F_ORGANIZATIONAL_UNIT, origValueSelector(ONE))
                .singleValue()
                .assertMappingObjectOid(RESOURCE_DUMMY_FURIOUS_REFACTORING.oid)
                .assertMappingName(null)
                .assertMappingObjectType(ACCOUNT_DEFAULT)
                .assertMappingAssociationType(null)
                .assertMappingTag(null);

        when("account description is changed to 'two' and account is re-imported");
        jack.replaceAttributeValue(DummyAdTrivialScenario.Account.AttributeNames.DESCRIPTION.local(), TWO);
        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_FURIOUS_REFACTORING.oid);
        importFuriousRefactoringAccounts(result);

        then("both values of 'one' and 'two' are there, as automatic ranges do not work without mapping names");
        assertUserByUsername(JACK_NAME, "after import")
                .assertOrganizationalUnits(ONE, TWO)
                .valueMetadata(UserType.F_ORGANIZATIONAL_UNIT, origValueSelector(ONE))
                .singleValue()
                .assertMappingObjectOid(RESOURCE_DUMMY_FURIOUS_REFACTORING.oid)
                .assertMappingName(null)
                .assertMappingObjectType(ACCOUNT_DEFAULT)
                .assertMappingAssociationType(null)
                .assertMappingTag(null)
                .end()
                .end()
                .valueMetadata(UserType.F_ORGANIZATIONAL_UNIT, origValueSelector(TWO))
                .singleValue()
                .assertMappingObjectOid(RESOURCE_DUMMY_FURIOUS_REFACTORING.oid)
                .assertMappingName(null)
                .assertMappingObjectType(ACCOUNT_DEFAULT)
                .assertMappingAssociationType(null)
                .assertMappingTag(null)
                .end()
                .end();
    }

    /** The mapping gets its first name. */
    @Test
    public void test410InitialMappingName() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given("mapping has now a name");
        updateFuriousMappingResource(FURIOUS_MAPPING_410_FILE);

        when("accounts are imported");
        importFuriousRefactoringAccounts(result);

        then("provenance: one <- unnamed, two <- unnamed, named (as currently there's the value of 'two' on resource)");
        assertUserByUsername(JACK_NAME, "after import")
                .assertOrganizationalUnits(ONE, TWO)
                .valueMetadata(UserType.F_ORGANIZATIONAL_UNIT, origValueSelector(ONE))
                .singleValue()
                .assertMappingObjectOid(RESOURCE_DUMMY_FURIOUS_REFACTORING.oid)
                .assertMappingName(null)
                .assertMappingObjectType(ACCOUNT_DEFAULT)
                .assertMappingAssociationType(null)
                .assertMappingTag(null)
                .end()
                .end()
                .valueMetadata(UserType.F_ORGANIZATIONAL_UNIT, origValueSelector(TWO))
                .assertSize(2)
                .value(v -> getMappingName(v) == null)
                .assertMappingObjectOid(RESOURCE_DUMMY_FURIOUS_REFACTORING.oid)
                .assertMappingName(null)
                .assertMappingObjectType(ACCOUNT_DEFAULT)
                .assertMappingAssociationType(null)
                .assertMappingTag(null)
                .end()
                .value(v -> getMappingName(v) != null)
                .assertMappingObjectOid(RESOURCE_DUMMY_FURIOUS_REFACTORING.oid)
                .assertMappingName("description-inbound")
                .assertMappingObjectType(ACCOUNT_DEFAULT)
                .assertMappingAssociationType(null)
                .assertMappingTag(null)
                .end()
                .end();

        when("description is changed from 'two' to 'three', and accounts are imported");
        refactoringScenario.account.getByNameRequired(JACK_NAME)
                .replaceAttributeValue(DummyAdTrivialScenario.Account.AttributeNames.DESCRIPTION.local(), THREE);
        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_FURIOUS_REFACTORING.oid);
        importFuriousRefactoringAccounts(result);

        then("provenance: one <- unnamed, two <- unnamed, three <- named");
        assertUserByUsername(JACK_NAME, "after import")
                .assertOrganizationalUnits(ONE, TWO, THREE)
                .valueMetadata(UserType.F_ORGANIZATIONAL_UNIT, origValueSelector(ONE))
                .singleValue()
                .assertMappingObjectOid(RESOURCE_DUMMY_FURIOUS_REFACTORING.oid)
                .assertMappingName(null)
                .assertMappingObjectType(ACCOUNT_DEFAULT)
                .assertMappingAssociationType(null)
                .assertMappingTag(null)
                .end()
                .end()
                .valueMetadata(UserType.F_ORGANIZATIONAL_UNIT, origValueSelector(TWO))
                .singleValue()
                .assertMappingObjectOid(RESOURCE_DUMMY_FURIOUS_REFACTORING.oid)
                .assertMappingName(null)
                .assertMappingObjectType(ACCOUNT_DEFAULT)
                .assertMappingAssociationType(null)
                .assertMappingTag(null)
                .end()
                .end()
                .valueMetadata(UserType.F_ORGANIZATIONAL_UNIT, origValueSelector(THREE))
                .singleValue()
                .assertMappingObjectOid(RESOURCE_DUMMY_FURIOUS_REFACTORING.oid)
                .assertMappingName("description-inbound")
                .assertMappingObjectType(ACCOUNT_DEFAULT)
                .assertMappingAssociationType(null)
                .assertMappingTag(null)
                .end()
                .end();
    }

    /** The mapping gets an additional specification, to clean up old metadata. */
    @Test
    public void test420AddAdditionalMappingSpecification() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given("mapping has now an additional specification");
        updateFuriousMappingResource(FURIOUS_MAPPING_420_FILE);

        when("accounts are imported");
        importFuriousRefactoringAccounts(result);

        then("provenance: three <- named (all remnants are gone)");
        assertUserByUsername(JACK_NAME, "after import")
                .assertOrganizationalUnits(THREE)
                .valueMetadata(UserType.F_ORGANIZATIONAL_UNIT, origValueSelector(THREE))
                .singleValue()
                .assertMappingObjectOid(RESOURCE_DUMMY_FURIOUS_REFACTORING.oid)
                .assertMappingName("description-inbound")
                .assertMappingObjectType(ACCOUNT_DEFAULT)
                .assertMappingAssociationType(null)
                .assertMappingTag(null)
                .end()
                .end();
    }

    /** Our motivated engineer renames the mapping as he seems fit. But he forgets to add mapping alias. */
    @Test
    public void test430RenamingMapping() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given("mapping has now a different name");
        updateFuriousMappingResource(FURIOUS_MAPPING_430_FILE);

        when("description is changed from 'three' to 'four', and accounts are imported");
        refactoringScenario.account.getByNameRequired(JACK_NAME)
                .replaceAttributeValue(DummyAdTrivialScenario.Account.AttributeNames.DESCRIPTION.local(), FOUR);
        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_FURIOUS_REFACTORING.oid);
        importFuriousRefactoringAccounts(result);

        then("provenance: three <- old name, four <- new name");
        assertUserByUsername(JACK_NAME, "after import")
                .assertOrganizationalUnits(THREE, FOUR)
                .valueMetadata(UserType.F_ORGANIZATIONAL_UNIT, origValueSelector(THREE))
                .singleValue()
                .assertMappingObjectOid(RESOURCE_DUMMY_FURIOUS_REFACTORING.oid)
                .assertMappingName("description-inbound")
                .assertMappingObjectType(ACCOUNT_DEFAULT)
                .assertMappingAssociationType(null)
                .assertMappingTag(null)
                .end()
                .end()
                .valueMetadata(UserType.F_ORGANIZATIONAL_UNIT, origValueSelector(FOUR))
                .singleValue()
                .assertMappingObjectOid(RESOURCE_DUMMY_FURIOUS_REFACTORING.oid)
                .assertMappingName("description-to-organizationalUnit")
                .assertMappingObjectType(ACCOUNT_DEFAULT)
                .assertMappingAssociationType(null)
                .assertMappingTag(null)
                .end()
                .end();
    }

    /** Mapping alias is added, rectifying the situation. */
    @Test
    public void test440AddingMappingAlias() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given("mapping has now an alias");
        updateFuriousMappingResource(FURIOUS_MAPPING_440_FILE);

        when("accounts are imported");
        importFuriousRefactoringAccounts(result);

        then("provenance: four <- new name");
        assertUserByUsername(JACK_NAME, "after import")
                .assertOrganizationalUnits(FOUR)
                .valueMetadata(UserType.F_ORGANIZATIONAL_UNIT, origValueSelector(FOUR))
                .singleValue()
                .assertMappingObjectOid(RESOURCE_DUMMY_FURIOUS_REFACTORING.oid)
                .assertMappingName("description-to-organizationalUnit")
                .assertMappingObjectType(ACCOUNT_DEFAULT)
                .assertMappingAssociationType(null)
                .assertMappingTag(null)
                .end()
                .end();
    }

    /** This time the mapping is renamed correctly - with adding the mapping alias at the same time. */
    @Test
    public void test450RenamingMappingCorrectly() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given("mapping was renamed, with an alias");
        updateFuriousMappingResource(FURIOUS_MAPPING_450_FILE);

        when("description is changed from 'four' to 'five', and accounts are imported");
        refactoringScenario.account.getByNameRequired(JACK_NAME)
                .replaceAttributeValue(DummyAdTrivialScenario.Account.AttributeNames.DESCRIPTION.local(), FIVE);
        invalidateShadowCacheIfNeeded(RESOURCE_DUMMY_FURIOUS_REFACTORING.oid);
        importFuriousRefactoringAccounts(result);

        then("provenance: five <- newest name");
        assertUserByUsername(JACK_NAME, "after import")
                .assertOrganizationalUnits(FIVE)
                .valueMetadata(UserType.F_ORGANIZATIONAL_UNIT, origValueSelector(FIVE))
                .singleValue()
                .assertMappingObjectOid(RESOURCE_DUMMY_FURIOUS_REFACTORING.oid)
                .assertMappingName("description-to-organizationalUnit-final-name")
                .assertMappingObjectType(ACCOUNT_DEFAULT)
                .assertMappingAssociationType(null)
                .assertMappingTag(null)
                .end()
                .end();
    }

    private static ValueSelector<?> origValueSelector(@NotNull String origValue) {
        return v -> origValue.equals(getOrig(v.<PolyString>getRealValue()));
    }

    private static @NotNull ValueSelector<PrismValue> valueSelector(@NotNull String value) {
        return v -> value.equals(v.getRealValue());
    }

    private static ResourceObjectTypeIdentificationType getMappingObjectType(PrismContainerValue<ValueMetadataType> v) {
        return getMappingSpecification(v).getObjectType();
    }

    private static String getMappingName(PrismContainerValue<ValueMetadataType> v) {
        return getMappingSpecification(v).getMappingName();
    }

    private static String getMappingSpecificationTag(PrismContainerValue<ValueMetadataType> v) {
        return getMappingSpecification(v).getTag();
    }

    private static MappingSpecificationType getMappingSpecification(PrismContainerValue<ValueMetadataType> v) {
        return getProvenance(v).getMappingSpecification();
    }

    private static ProvenanceMetadataType getProvenance(PrismContainerValue<ValueMetadataType> v) {
        return Objects.requireNonNull(v.<ValueMetadataType>getRealValue())
                .getProvenance();
    }

    @SuppressWarnings("SameParameterValue")
    private void updateHrResource(File file) throws FileNotFoundException, CommonException {
        var task = getTestTask();
        var result = task.getResult();
        importObjectFromFile(file, new ImportOptionsType().overwrite(true), task, result);
        modelService.testResource(RESOURCE_DUMMY_HR.oid, task, result);
    }

    private void updateFuriousMappingResource(File file) throws FileNotFoundException, CommonException {
        var task = getTestTask();
        var result = task.getResult();
        importObjectFromFile(file, new ImportOptionsType().overwrite(true), task, result);
        modelService.testResource(RESOURCE_DUMMY_FURIOUS_REFACTORING.oid, task, result);
    }

    private void importFuriousRefactoringAccounts(OperationResult result) throws CommonException, IOException {
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_FURIOUS_REFACTORING.oid)
                .withTypeIdentification(ACCOUNT_DEFAULT)
                .withProcessingAllAccounts()
                .execute(result);
    }
}
