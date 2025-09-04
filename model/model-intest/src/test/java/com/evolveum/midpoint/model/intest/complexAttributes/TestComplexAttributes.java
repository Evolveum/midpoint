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

import com.evolveum.midpoint.prism.ValueSelector;
import com.evolveum.midpoint.test.DummyAddressBookScenario.Address;
import com.evolveum.midpoint.test.DummyAddressBookScenario.Email;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
        DummyObject john = addressBookScenario.person.add(PERSON_JOHN_NAME)
                .addAttributeValue(Person.AttributeNames.FIRST_NAME.local(), "John")
                .addAttributeValue(Person.AttributeNames.LAST_NAME.local(), "Doe")
                .addAttributeValue(Person.AttributeNames.TITLE.local(), "Ing.");

        // The IDs here (1, 2) are a temporary hack. Later, we'll use ConnId EmbeddedObject instances to represent
        // the data, so ConnId Name+UID will no longer be required.

        DummyObject johnPersonalAddress = addressBookScenario.address.addUnnamed()
                .addAttributeValue(Address.AttributeNames.TYPE.local(), TYPE_PERSONAL)
                .addAttributeValue(Address.AttributeNames.PRIMARY.local(), false)
                .addAttributeValue(Address.AttributeNames.STREET.local(), "123 Main St")
                .addAttributeValue(Address.AttributeNames.CITY.local(), "Spring")
                .addAttributeValue(Address.AttributeNames.ZIP.local(), "12345")
                .addAttributeValue(Address.AttributeNames.COUNTRY.local(), "USA");
        DummyObject johnWorkAddress = addressBookScenario.address.addUnnamed()
                .addAttributeValue(Address.AttributeNames.TYPE.local(), TYPE_WORK)
                .addAttributeValue(Address.AttributeNames.PRIMARY.local(), true)
                .addAttributeValue(Address.AttributeNames.STREET.local(), "456 Elm St")
                .addAttributeValue(Address.AttributeNames.CITY.local(), "Springfield")
                .addAttributeValue(Address.AttributeNames.ZIP.local(), "67890")
                .addAttributeValue(Address.AttributeNames.COUNTRY.local(), "USA");

        DummyObject johnPersonalEmail = addressBookScenario.email.addUnnamed()
                .addAttributeValue(Email.AttributeNames.TYPE.local(), TYPE_PERSONAL)
                .addAttributeValue(Email.AttributeNames.PRIMARY.local(), false)
                .addAttributeValue(Email.AttributeNames.VALUE.local(), "john@doe.org");
        DummyObject johnWorkEmail = addressBookScenario.email.addUnnamed()
                .addAttributeValue(Email.AttributeNames.TYPE.local(), TYPE_WORK)
                .addAttributeValue(Email.AttributeNames.PRIMARY.local(), true)
                .addAttributeValue(Email.AttributeNames.VALUE.local(), "john@evolveum.com");

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
                .forAttributeValue(Address.AttributeNames.TYPE.q(), TYPE_PERSONAL)
                .shadow()
                .assertObjectClass(Address.OBJECT_CLASS_NAME.xsd())
                .attributes()
                .simpleAttribute(Address.AttributeNames.TYPE.q()).singleValue().assertValue(TYPE_PERSONAL).end().end()
                .simpleAttribute(Address.AttributeNames.PRIMARY.q()).singleValue().assertValue(false).end().end()
                .simpleAttribute(Address.AttributeNames.STREET.q()).singleValue().assertValue("123 Main St").end().end();

        var shadowReadAgain = provisioningService.getObject(
                ShadowType.class, shadows.get(0).getOid(), readOnly(), task, result);
        assertShadowAfter(shadowReadAgain)
                .display();

        // We do not check the details here. The provisioning module behavior should be checked in the provisioning tests,
        // in particular in TestDummyComplexAttributes.
    }

    /**
     * Checks that the account and its complex attributes are correctly imported (and re-imported after a change).
     *
     * Tests complex items correlation based on business keys as well as automatic provenance-based mapping ranges.
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
                .executeOnForeground(result);

        and("john is found");
        var workAddressId = new Holder<Long>();
        // @formatter:off
        assertUserAfterByUsername(PERSON_JOHN_NAME)
                .assignments()
                .assertAssignments(1)
                .by().targetType(ArchetypeType.COMPLEX_TYPE).find()
                    .assertTargetOid(ARCHETYPE_PERSON.oid)
                .end()
                .end()
                .container(UserType.F_EMAIL)
                    .assertSize(2)
                    .value(ValueSelector.itemEquals(EmailAddressType.F_TYPE, TYPE_PERSONAL))
                        .assertPropertyValuesEqual(EmailAddressType.F_VALUE, "john@doe.org")
                        .assertPropertyValuesEqual(EmailAddressType.F_PRIMARY, false)
                    .end()
                    .value(ValueSelector.itemEquals(EmailAddressType.F_TYPE, TYPE_WORK))
                        .assertPropertyValuesEqual(EmailAddressType.F_VALUE, "john@evolveum.com")
                        .assertPropertyValuesEqual(EmailAddressType.F_PRIMARY, true)
                        .emitId(workAddressId)
                    .end();
        // @formatter:on

        when("john is changed on the resource");

        var john = addressBookScenario.person.getByNameRequired(PERSON_JOHN_NAME);
        var johnEmails = john.getLinkedObjects(Person.LinkNames.EMAIL.local());
        var personalEmail = johnEmails.stream()
                .filter(e -> TYPE_PERSONAL.equals(e.getAttributeValue(Email.AttributeNames.TYPE.local())))
                .findFirst().orElseThrow(() -> new AssertionError("No personal email for " + PERSON_JOHN_NAME));
        var workEmail = johnEmails.stream()
                .filter(e -> TYPE_WORK.equals(e.getAttributeValue(Email.AttributeNames.TYPE.local())))
                .findFirst().orElseThrow(() -> new AssertionError("No work email for " + PERSON_JOHN_NAME));

        // one email is added
        DummyObject johnNewWorkEmail = addressBookScenario.email.addUnnamed()
                .addAttributeValue(Email.AttributeNames.TYPE.local(), TYPE_PERSONAL)
                .addAttributeValue(Email.AttributeNames.PRIMARY.local(), true)
                .addAttributeValue(Email.AttributeNames.VALUE.local(), "john-new@doe.com");
        addressBookScenario.personEmail.add(john, johnNewWorkEmail);

        // one is changed
        workEmail.replaceAttributeValues(Email.AttributeNames.PRIMARY.local(), false);

        // one is deleted
        addressBookScenario.email.deleteById(personalEmail.getId());

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
                .end()
                .end()
                .container(UserType.F_EMAIL)
                    .assertSize(2)
                    .value(ValueSelector.itemEquals(EmailAddressType.F_TYPE, TYPE_PERSONAL))
                        .assertPropertyValuesEqual(EmailAddressType.F_VALUE, "john-new@doe.com")
                        .assertPropertyValuesEqual(EmailAddressType.F_PRIMARY, true)
                    .end()
                    .value(ValueSelector.itemEquals(EmailAddressType.F_TYPE, TYPE_WORK))
                        .assertPropertyValuesEqual(EmailAddressType.F_VALUE, "john@evolveum.com")
                        .assertPropertyValuesEqual(EmailAddressType.F_PRIMARY, false)
                        .assertId(workAddressId.getValue()) // business key was not changed -> the PCV ID should stay the same
                    .end();
        // @formatter:on
    }
}
