/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.complexAttributes;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.GetOperationOptions.readOnly;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType.ACCOUNT;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyObject;
import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.model.intest.associations.TestAssociations;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyAddressBookScenario;
import com.evolveum.midpoint.test.DummyAddressBookScenario.Person;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Tests the inbound processing of complex attributes.
 *
 * @see TestAssociations
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestComplexAttributes extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/complex-attributes");

    private static final String INTENT_PERSON = "person";

    private static final String TYPE_WORK = "work";
    private static final String TYPE_PERSONAL = "personal";

    private static final String PERSON_JOHN_NAME = "john";

    private static DummyAddressBookScenario addressBookScenario;

    private static final DummyTestResource RESOURCE_DUMMY_ADDRESS_BOOK = new DummyTestResource(
            TEST_DIR, "resource-dummy-address-book.xml", "a485e266-60c6-49f5-a71c-0b6d00eea2b5", "address-book",
            c -> addressBookScenario = DummyAddressBookScenario.on(c).initialize());

    private static final TestObject<ArchetypeType> ARCHETYPE_PERSON = TestObject.file(
            TEST_DIR, "archetype-person.xml", "15b006c5-ad64-40ee-90d8-ab21816a2c6f");

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
                ARCHETYPE_PERSON);

        RESOURCE_DUMMY_ADDRESS_BOOK.initAndTest(this, initTask, initResult);
        createCommonAddressBookObjects();
    }

    /**
     * These objects should be usable in all tests. They should not be modified by tests; if needed, a test should create
     * its own objects to work with.
     */
    private void createCommonAddressBookObjects() throws Exception {
        DummyObject john = addressBookScenario.person.add("john")
                .addAttributeValue(Person.AttributeNames.FIRST_NAME.local(), "John")
                .addAttributeValue(Person.AttributeNames.LAST_NAME.local(), "Doe")
                .addAttributeValue(Person.AttributeNames.TITLE.local(), "Ing.");

        DummyObject johnPersonalAddress = addressBookScenario.address.add("1")
                .addAttributeValue(DummyAddressBookScenario.Address.AttributeNames.TYPE.local(), TYPE_PERSONAL)
                .addAttributeValue(DummyAddressBookScenario.Address.AttributeNames.PRIMARY.local(), false)
                .addAttributeValue(DummyAddressBookScenario.Address.AttributeNames.STREET.local(), "123 Main St")
                .addAttributeValue(DummyAddressBookScenario.Address.AttributeNames.CITY.local(), "Spring")
                .addAttributeValue(DummyAddressBookScenario.Address.AttributeNames.ZIP.local(), "12345")
                .addAttributeValue(DummyAddressBookScenario.Address.AttributeNames.COUNTRY.local(), "USA");
        DummyObject johnWorkAddress = addressBookScenario.address.add("2")
                .addAttributeValue(DummyAddressBookScenario.Address.AttributeNames.TYPE.local(), TYPE_WORK)
                .addAttributeValue(DummyAddressBookScenario.Address.AttributeNames.PRIMARY.local(), true)
                .addAttributeValue(DummyAddressBookScenario.Address.AttributeNames.STREET.local(), "456 Elm St")
                .addAttributeValue(DummyAddressBookScenario.Address.AttributeNames.CITY.local(), "Springfield")
                .addAttributeValue(DummyAddressBookScenario.Address.AttributeNames.ZIP.local(), "67890")
                .addAttributeValue(DummyAddressBookScenario.Address.AttributeNames.COUNTRY.local(), "USA");

        DummyObject johnPersonalEmail = addressBookScenario.email.add("1")
                .addAttributeValue(DummyAddressBookScenario.Email.AttributeNames.TYPE.local(), TYPE_PERSONAL)
                .addAttributeValue(DummyAddressBookScenario.Email.AttributeNames.PRIMARY.local(), false)
                .addAttributeValue(DummyAddressBookScenario.Email.AttributeNames.VALUE.local(), "john@doe.org");
        DummyObject johnWorkEmail = addressBookScenario.email.add("2")
                .addAttributeValue(DummyAddressBookScenario.Email.AttributeNames.TYPE.local(), TYPE_WORK)
                .addAttributeValue(DummyAddressBookScenario.Email.AttributeNames.PRIMARY.local(), true)
                .addAttributeValue(DummyAddressBookScenario.Email.AttributeNames.VALUE.local(), "john@evolveum.com");

        addressBookScenario.personAddress.add(john, johnPersonalAddress);
        addressBookScenario.personAddress.add(john, johnWorkAddress);

        addressBookScenario.personEmail.add(john, johnPersonalEmail);
        addressBookScenario.personEmail.add(john, johnWorkEmail);
    }

    @Test
    public void test010Sanity() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        var resource = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_ADDRESS_BOOK.oid, null, task, result);
        displayDumpable("Address book resource", resource);
    }

    /** Checks that simply getting the account gets the correct results. A prerequisite for the following test. */
    @Test
    public void test100GetPerson() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("john is get");
        var query = Resource.of(RESOURCE_DUMMY_ADDRESS_BOOK.get())
                .queryFor(Person.OBJECT_CLASS_NAME.xsd())
                .and().item(Person.AttributeNames.NAME.path()).eq(PERSON_JOHN_NAME)
                .build();
        var shadows = modelService.searchObjects(ShadowType.class, query, null, task, result);

        then("john is found");

        display("shadows", shadows);
        assertThat(shadows).as("shadows").hasSize(1);

        assertShadowAfter(shadows.get(0))
                .assertKind(ACCOUNT)
                .assertIntent(INTENT_PERSON)
                .attributes()
                .referenceAttribute(Person.LinkNames.ADDRESS.q())
                .assertSize(2)
                .forPrimaryIdentifierValue("1")
                .shadow()
                .assertObjectClass(DummyAddressBookScenario.Address.OBJECT_CLASS_NAME.xsd())
                .attributes()
                .simpleAttribute(DummyAddressBookScenario.Address.AttributeNames.TYPE.q()).singleValue().assertValue(TYPE_PERSONAL).end().end()
                .simpleAttribute(DummyAddressBookScenario.Address.AttributeNames.PRIMARY.q()).singleValue().assertValue(false).end().end()
                .simpleAttribute(DummyAddressBookScenario.Address.AttributeNames.STREET.q()).singleValue().assertValue("123 Main St").end().end();

        var shadowReadAgain = provisioningService.getObject(
                ShadowType.class, shadows.get(0).getOid(), readOnly(), task, result);
        assertShadowAfter(shadowReadAgain)
                .display();

        // We do not check the details here. The provisioning module behavior should be checked in the provisioning tests,
        // in particular in TestDummyComplexAttributes.
    }

    /**
     * Checks that the account and its associations are correctly imported (and re-imported after a change).
     *
     * Tests assignment correlation as well as automatic provenance-based mapping ranges.
     */
    @Test
    public void test110ImportPerson() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("john is imported");
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_ADDRESS_BOOK.oid)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ACCOUNT, INTENT_PERSON))
                .withNameValue(PERSON_JOHN_NAME)
                .withTracing()
                .executeOnForeground(result);

        and("john is found");
        // @formatter:off
        assertUserAfterByUsername(PERSON_JOHN_NAME)
                .assignments()
                .assertAssignments(1)
                .by().targetType(ArchetypeType.COMPLEX_TYPE).find()
                    .assertTargetOid(ARCHETYPE_PERSON.oid)
                .end();
        // @formatter:on

        when("john is changed on the resource");

//        var dummyJohn = addressBookScenario.person.getByNameRequired(PERSON_JOHN_NAME);
//        var dummyMedicine = addressBookScenario.orgUnit.getByNameRequired(ORG_MEDICINE_NAME);
//        var dummyCc1000 = addressBookScenario.costCenter.getByNameRequired(CC_1000_NAME);
//        var dummyCc1100 = addressBookScenario.costCenter.getByNameRequired(CC_1100_NAME);
//
//        // one contract is added
//        var dummyContractMedicine = addressBookScenario.contract.add(JOHN_MEDICINE_CONTRACT_ID);
//        addressBookScenario.personContract.add(dummyJohn, dummyContractMedicine);
//        addressBookScenario.contractOrgUnit.add(dummyContractMedicine, dummyMedicine);
//        addressBookScenario.contractCostCenter.add(dummyContractMedicine, dummyCc1100);
//
//        // one is changed
//        var dummyContractSciences = addressBookScenario.contract.getByNameRequired(JOHN_SCIENCES_CONTRACT_ID);
//        dummyContractSciences.replaceAttributeValues(
//                DummyHrScenarioExtended.Contract.AttributeNames.NOTE.local(), "reviewed");
//        addressBookScenario.contractCostCenter.delete(dummyContractSciences, dummyCc1000);
//        addressBookScenario.contractCostCenter.add(dummyContractSciences, dummyCc1100);
//
//        // one is deleted
//        addressBookScenario.contract.deleteByName(JOHN_LAW_CONTRACT_ID);

        when("john is re-imported");
        importAccountsRequest()
                .withResourceOid(RESOURCE_DUMMY_ADDRESS_BOOK.oid)
                .withTypeIdentification(ResourceObjectTypeIdentification.of(ACCOUNT, INTENT_PERSON))
                .withNameValue(PERSON_JOHN_NAME)
                .executeOnForeground(result);

        and("john is updated; TODO describe the changes");
        // @formatter:off
        assertUserAfterByUsername(PERSON_JOHN_NAME)
                .assignments()
                .assertAssignments(1)
                .by().targetType(ArchetypeType.COMPLEX_TYPE).find()
                    .assertTargetOid(ARCHETYPE_PERSON.oid)
                .end();
        // @formatter:on
    }
}
