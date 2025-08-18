/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import static com.evolveum.midpoint.schema.GetOperationOptions.*;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.ICFS_NAME_PATH;

import java.io.File;
import java.util.Collection;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.internals.InternalsConfig;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyObject;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.test.DummyAddressBookScenario;
import com.evolveum.midpoint.test.DummyAddressBookScenario.Address;
import com.evolveum.midpoint.test.DummyAddressBookScenario.Email;
import com.evolveum.midpoint.test.DummyAddressBookScenario.Person;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

/**
 * Testing the complex attributes.
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummyComplexAttributes extends AbstractDummyTest {

    private static final File RESOURCE_DUMMY_ADDRESS_BOOK_FILE = new File(TEST_DIR, "resource-dummy-address-book.xml");

    private static final String INTENT_PERSON = "person";
    private static final ResourceObjectTypeIdentification TYPE_PERSON =
            ResourceObjectTypeIdentification.of(ShadowKindType.ACCOUNT, INTENT_PERSON);

    private static final String TYPE_PERMANENT = "permanent";
    private static final String TYPE_TEMPORARY = "temporary";
    private static final String TYPE_WORK = "work";
    private static final String TYPE_PERSONAL = "personal";

    private DummyAddressBookScenario addressBookScenario;

    @Override
    protected File getResourceDummyFile() {
        return RESOURCE_DUMMY_ADDRESS_BOOK_FILE;
    }

    @Override
    protected void extraDummyResourceInit() throws Exception {
        addressBookScenario = DummyAddressBookScenario.on(dummyResourceCtl);
        addressBookScenario.initialize();

        createCommonAddressBookObjects();

        var task = getTestTask();
        var result = task.getResult();

        OperationResult testResult = provisioningService.testResource(RESOURCE_DUMMY_OID, task, result);
        assertSuccess(testResult);

        resource = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
        resourceBean = resource.asObjectable();
    }

    /** These objects should be usable in all tests. */
    private void createCommonAddressBookObjects() throws Exception {
        DummyObject john = addressBookScenario.person.add("john")
                .addAttributeValue(Person.AttributeNames.FIRST_NAME.local(), "John")
                .addAttributeValue(Person.AttributeNames.LAST_NAME.local(), "Doe")
                .addAttributeValue(Person.AttributeNames.TITLE.local(), "Ing.");

        DummyObject johnPermanentAddress = addressBookScenario.address.add("1")
                .addAttributeValue(Address.AttributeNames.TYPE.local(), TYPE_PERMANENT)
                .addAttributeValue(Address.AttributeNames.PRIMARY.local(), false)
                .addAttributeValue(Address.AttributeNames.STREET.local(), "123 Main St")
                .addAttributeValue(Address.AttributeNames.CITY.local(), "Spring")
                .addAttributeValue(Address.AttributeNames.ZIP.local(), "12345")
                .addAttributeValue(Address.AttributeNames.COUNTRY.local(), "USA");
        DummyObject johnTemporaryAddress = addressBookScenario.address.add("2")
                .addAttributeValue(Address.AttributeNames.TYPE.local(), TYPE_TEMPORARY)
                .addAttributeValue(Address.AttributeNames.PRIMARY.local(), true)
                .addAttributeValue(Address.AttributeNames.STREET.local(), "456 Elm St")
                .addAttributeValue(Address.AttributeNames.CITY.local(), "Springfield")
                .addAttributeValue(Address.AttributeNames.ZIP.local(), "67890")
                .addAttributeValue(Address.AttributeNames.COUNTRY.local(), "USA");

        DummyObject johnPersonalEmail = addressBookScenario.email.add("1")
                .addAttributeValue(Email.AttributeNames.TYPE.local(), TYPE_PERSONAL)
                .addAttributeValue(Email.AttributeNames.PRIMARY.local(), false)
                .addAttributeValue(Email.AttributeNames.VALUE.local(), "john@doe.org");
        DummyObject johnWorkEmail = addressBookScenario.email.add("2")
                .addAttributeValue(Email.AttributeNames.TYPE.local(), TYPE_WORK)
                .addAttributeValue(Email.AttributeNames.PRIMARY.local(), true)
                .addAttributeValue(Email.AttributeNames.VALUE.local(), "john@evolveum.com");

        addressBookScenario.personAddress.add(john, johnPermanentAddress);
        addressBookScenario.personAddress.add(john, johnTemporaryAddress);

        addressBookScenario.personEmail.add(john, johnPersonalEmail);
        addressBookScenario.personEmail.add(john, johnWorkEmail);
    }

    /** Are complex attributes in the schema? */
    @Test
    public void test100CheckResourceSchema() throws Exception {
        given("the resource schema, fetched from the resource");
        SchemaDefinitionType definition = resourceBean.getSchema().getDefinition();
        displayValue("schema definition",
                prismContext.xmlSerializer().root(new QName("schema")).serializeRealValue(definition));

        when("getting schema for 'person' class and type");
        var schema = Resource.of(resource).getCompleteSchemaRequired();
        var personClass = schema.findObjectClassDefinitionRequired(Person.OBJECT_CLASS_NAME.xsd());
        var personType = schema.getObjectTypeDefinitionRequired(TYPE_PERSON);
        displayDumpable("person OC schema", personClass);
        displayDumpable("person type schema", personType);

        then("there is a reference attribute 'person->address' in both class and type");
        personClass.findReferenceAttributeDefinitionRequired(Person.LinkNames.ADDRESS.q());
        personType.findReferenceAttributeDefinitionRequired(Person.LinkNames.ADDRESS.q());

        then("there is no association 'person->address' in class nor in type");
        assertThat(personClass.findAssociationDefinition(Person.LinkNames.ADDRESS.q())).isNull();
        assertThat(personClass.findAssociationDefinition(Person.LinkNames.ADDRESS.q())).isNull();
    }

    /** Just read an account with complex attributes (in various modes: get vs search, read-write vs read-only). */
    @Test
    public void test110GetObjectWithComplexAttributes() throws Exception {
        executeSearchForJohn(null);
        var oid = executeSearchForJohn(readOnly());

        executeGetJohn(oid, null);
        executeGetJohn(oid, readOnly());
    }

    /** Now reading the account with `noFetch` option (only if caching is turned on). */
    @Test
    public void test120GetObjectWithComplexAttributesNoFetch() throws Exception {
        skipTestIf(!InternalsConfig.isShadowCachingOnByDefault(), "caching is not turned on");

        executeSearchForJohn(noFetch());
        var oid = executeSearchForJohn(createNoFetchReadOnlyCollection());

        executeGetJohn(oid, noFetch());
        executeGetJohn(oid, createNoFetchReadOnlyCollection());
    }

    private String executeSearchForJohn(Collection<SelectorOptions<GetOperationOptions>> options)
            throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("searching for person 'john'");
        var objects = provisioningService.searchObjects(
                ShadowType.class,
                Resource.of(resource)
                        .queryFor(Person.OBJECT_CLASS_NAME.xsd())
                        .and().item(ICFS_NAME_PATH).eq("john")
                        .build(),
                options, task, result);

        then("there is a person 'john'");
        assertThat(objects).as("persons named john").hasSize(1);
        PrismObject<ShadowType> john = objects.get(0);

        assertJohn(john);

        return john.getOid();
    }

    private void executeGetJohn(String oid, Collection<SelectorOptions<GetOperationOptions>> options)
            throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("getting person 'john' by OID");
        var john = provisioningService.getObject(ShadowType.class, oid, options, task, result);

        then("John is OK");
        assertJohn(john);
    }

    private void assertJohn(PrismObject<ShadowType> shadow) {
        assertShadow(shadow, "john")
                .display()
                .assertEffectiveOperationsDeeply()
                .attributes()
                .referenceAttribute(Person.LinkNames.ADDRESS.q())
                .assertSize(2)
                .forPrimaryIdentifierValue("1")
                .shadow()
                .assertObjectClass(Address.OBJECT_CLASS_NAME.xsd())
                .attributes()
                .simpleAttribute(Address.AttributeNames.TYPE.q()).singleValue().assertValue(TYPE_PERMANENT).end().end()
                .simpleAttribute(Address.AttributeNames.PRIMARY.q()).singleValue().assertValue(false).end().end()
                .simpleAttribute(Address.AttributeNames.STREET.q()).singleValue().assertValue("123 Main St").end().end()
                .end()
                .end()
                .end()
                .forPrimaryIdentifierValue("2")
                .shadow()
                .assertObjectClass(Address.OBJECT_CLASS_NAME.xsd())
                .attributes()
                .simpleAttribute(Address.AttributeNames.TYPE.q()).singleValue().assertValue(TYPE_TEMPORARY).end().end()
                .simpleAttribute(Address.AttributeNames.PRIMARY.q()).singleValue().assertValue(true).end().end()
                .simpleAttribute(Address.AttributeNames.STREET.q()).singleValue().assertValue("456 Elm St").end().end()
                .end()
                .end()
                .end()
                .end()
                .referenceAttribute(Person.LinkNames.EMAIL.q())
                .assertSize(2)
                .forPrimaryIdentifierValue("1")
                .shadow()
                .assertObjectClass(Email.OBJECT_CLASS_NAME.xsd())
                .attributes()
                .simpleAttribute(Email.AttributeNames.TYPE.q()).singleValue().assertValue(TYPE_PERSONAL).end().end()
                .simpleAttribute(Email.AttributeNames.PRIMARY.q()).singleValue().assertValue(false).end().end()
                .simpleAttribute(Email.AttributeNames.VALUE.q()).singleValue().assertValue("john@doe.org").end().end()
                .end()
                .display("john's personal email (shadow)");
    }
}
