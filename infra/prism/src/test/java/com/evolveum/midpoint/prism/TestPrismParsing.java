/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.prism;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.*;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Collection;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public abstract class TestPrismParsing {

	protected abstract String getSubdirName();

	protected abstract String getFilenameSuffix();

	protected File getCommonSubdir() {
		return new File(COMMON_DIR_PATH, getSubdirName());
	}

	protected File getFile(String baseName) {
		return new File(getCommonSubdir(), baseName+"."+getFilenameSuffix());
	}

	@BeforeSuite
	public void setupDebug() {
		PrettyPrinter.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
	}

	protected abstract String getOutputFormat();

	@Test
	public void test100PrismParseFile() throws Exception {
		final String TEST_NAME = "test100PrismParseFile";
		PrismInternalTestUtil.displayTestTitle(TEST_NAME);
		// GIVEN
		PrismContext prismContext = constructInitializedPrismContext();

		// WHEN
		PrismObject<UserType> user = prismContext.parseObject(getFile(USER_JACK_FILE_BASENAME));

		// THEN
		System.out.println("User:");
		System.out.println(user.debugDump());
		assertNotNull(user);

		assertUserJack(user, true);
	}

	@Test
	public void test110PrismParseFileNoNs() throws Exception {
		final String TEST_NAME = "test110PrismParseFileNoNs";
		PrismInternalTestUtil.displayTestTitle(TEST_NAME);
		// GIVEN
		PrismContext prismContext = constructInitializedPrismContext();

		// WHEN
		PrismObject<UserType> user = prismContext.parseObject(getFile(USER_JACK_NO_NS_BASENAME));

		// THEN
		System.out.println("User:");
		System.out.println(user.debugDump());
		assertNotNull(user);

		assertUserJack(user, true);
	}

	@Test
	public void test120PrismParseFileObject() throws Exception {
		final String TEST_NAME = "test120PrismParseFileObject";
		PrismInternalTestUtil.displayTestTitle(TEST_NAME);

		// GIVEN
		PrismContext prismContext = constructInitializedPrismContext();

		// WHEN
		PrismObject<UserType> user = prismContext.parseObject(getFile(USER_JACK_OBJECT_BASENAME));

		// THEN
		System.out.println("User:");
		System.out.println(user.debugDump());
		assertNotNull(user);

		assertUserJack(user, true);
	}

	@Test
	public void test130PrismParseFileAdhoc() throws Exception {
		final String TEST_NAME = "test130PrismParseFileAdhoc";
		PrismInternalTestUtil.displayTestTitle(TEST_NAME);

		// GIVEN
		PrismContext prismContext = constructInitializedPrismContext();

		// WHEN
		PrismObject<UserType> user = prismContext.parseObject(getFile(USER_JACK_ADHOC_BASENAME));

		// THEN
		System.out.println("User:");
		System.out.println(user.debugDump());
		assertNotNull(user);

		assertUserAdhoc(user, true);
	}

	@Test
	public void test200RoundTrip() throws Exception {
		final String TEST_NAME = "test200RoundTrip";
		PrismInternalTestUtil.displayTestTitle(TEST_NAME);

		roundTrip(getFile(USER_JACK_FILE_BASENAME));
	}

	@Test
	public void test210RoundTripNoNs() throws Exception {
		final String TEST_NAME = "test210RoundTripNoNs";
		PrismInternalTestUtil.displayTestTitle(TEST_NAME);

		roundTrip(getFile(USER_JACK_NO_NS_BASENAME));
	}

	@Test
	public void test220RoundTripObject() throws Exception {
		final String TEST_NAME = "test220RoundTripObject";
		PrismInternalTestUtil.displayTestTitle(TEST_NAME);

		roundTrip(getFile(USER_JACK_OBJECT_BASENAME));
	}


	private void roundTrip(File file) throws SchemaException, SAXException, IOException {

		// GIVEN
		PrismContext prismContext = constructInitializedPrismContext();
		PrismObject<UserType> originalUser = prismContext.parseObject(file);

		System.out.println("Input parsed user:");
		System.out.println(originalUser.debugDump());
		assertNotNull(originalUser);

		// precondition
		assertUserJack(originalUser, true);

		// WHEN
		// We need to serialize with composite objects during roundtrip, otherwise the result will not be equal
		String userXml = prismContext.serializerFor(getOutputFormat()).serialize(originalUser);

		// THEN
		System.out.println("Serialized user:");
		System.out.println(userXml);
		assertNotNull(userXml);

		validateXml(userXml, prismContext);

		// WHEN
		PrismObject<UserType> parsedUser = prismContext.parseObject(userXml);
		System.out.println("Re-parsed user:");
		System.out.println(parsedUser.debugDump());
		assertNotNull(parsedUser);

		assertUserJack(parsedUser, true);

		ObjectDelta<UserType> diff = DiffUtil.diff(originalUser, parsedUser);
		System.out.println("Diff:");
		System.out.println(diff.debugDump());

		assertTrue("Diff: "+diff, diff.isEmpty());

		assertTrue("Users not equal", originalUser.equals(parsedUser));
	}


	@Test
	public void test230RoundTripAdhoc() throws Exception {
		final String TEST_NAME = "test230RoundTripAdhoc";
		PrismInternalTestUtil.displayTestTitle(TEST_NAME);

		roundTripAdhoc(getFile(USER_JACK_ADHOC_BASENAME));
	}

	private void roundTripAdhoc(File file) throws SchemaException, SAXException, IOException {
		// GIVEN
		PrismContext prismContext = constructInitializedPrismContext();
		PrismObject<UserType> originalUser = prismContext.parseObject(file);

		System.out.println("Input parsed user:");
		System.out.println(originalUser.debugDump());
		assertNotNull(originalUser);

		// precondition
		assertUserAdhoc(originalUser, true);

		// WHEN
		// We need to serialize with composite objects during roundtrip, otherwise the result will not be equal
		String userXml = prismContext.serializeObjectToString(originalUser, getOutputFormat());

		// THEN
		System.out.println("Serialized user:");
		System.out.println(userXml);
		assertNotNull(userXml);

		validateXml(userXml, prismContext);

		// WHEN
		PrismObject<UserType> parsedUser = prismContext.parseObject(userXml);
		System.out.println("Re-parsed user:");
		System.out.println(parsedUser.debugDump());
		assertNotNull(parsedUser);

		assertUserAdhoc(parsedUser, true);

		assertTrue("Users not equal", originalUser.equals(parsedUser));
	}


	@Test
	public void test300MeleeContext() throws Exception {
		final String TEST_NAME = "test300MeleeContext";
		PrismInternalTestUtil.displayTestTitle(TEST_NAME);

		// GIVEN
		PrismContext prismContext = constructInitializedPrismContext();

		PrismObject<UserType> userJack = prismContext.parseObject(getFile(USER_JACK_FILE_BASENAME));
		PrismContainer<Containerable> meleeContextContainer = userJack.findOrCreateContainer(new ItemPath(UserType.F_EXTENSION, EXTENSION_MELEE_CONTEXT_ELEMENT));
		PrismReference opponentRef = meleeContextContainer.findOrCreateReference(EXTENSION_MELEE_CONTEXT_OPPONENT_REF_ELEMENT);
		PrismObject<UserType> userBarbossa = prismContext.parseObject(getFile(USER_BARBOSSA_FILE_BASENAME));
		// Cosmetics to make sure the equivalence assert below works
		userBarbossa.setElementName(EXTENSION_MELEE_CONTEXT_OPPONENT_ELEMENT);
		PrismReferenceValue opponentRefValue = new PrismReferenceValue();
		opponentRefValue.setObject(userBarbossa);
		opponentRef.add(opponentRefValue);

		System.out.println("User jack:");
		System.out.println(userJack.debugDump());

		// WHEN
		String elementJack = prismContext.serializeObjectToString(userJack, getOutputFormat());

		// THEN
		System.out.println("Serialized user jack:");
		System.out.println(elementJack);

		// TODO: see that there is really the serialized barbossa

		// WHEN
		PrismObject<UserType> reparsedUserJack = prismContext.parseObject(elementJack);

		// THEN
		System.out.println("Re-parsed user jack:");
		System.out.println(reparsedUserJack.debugDump());

		PrismReference reparsedOpponentRef = reparsedUserJack.findReference(new ItemPath(UserType.F_EXTENSION, EXTENSION_MELEE_CONTEXT_ELEMENT, EXTENSION_MELEE_CONTEXT_OPPONENT_REF_ELEMENT));
		assertNotNull("No opponent ref (reparsed)", reparsedOpponentRef);
		PrismReferenceValue reparsedOpponentRefValue = reparsedOpponentRef.getValue();
		assertNotNull("No opponent ref value (reparsed)", reparsedOpponentRefValue);
		PrismObject<UserType> reparsedUserBarbossa = reparsedOpponentRefValue.getObject();
		assertNotNull("No object in opponent ref value (reparsed)", reparsedUserBarbossa);

		PrismAsserts.assertEquivalent("User barbossa", userBarbossa, reparsedUserBarbossa);

		// Make the original user jack suitable for comparison.
		// Some of the accountRef information is (intentionally) lost in re-parsing therefore erase it before comparing
		PrismReference jackAccountRef = userJack.findReference(UserType.F_ACCOUNT_REF);
		for (PrismReferenceValue accRefVal: jackAccountRef.getValues()) {
			String oid = accRefVal.getOid();
			QName targetType = accRefVal.getTargetType();
			accRefVal.setObject(null);
			accRefVal.setOid(oid);
			accRefVal.setTargetType(targetType);
		}

		PrismAsserts.assertEquivalent("User jack", userJack, reparsedUserJack);

	}

	@Test
	public void test400UserWill() throws Exception {
		final String TEST_NAME = "test400UserWill";
		PrismInternalTestUtil.displayTestTitle(TEST_NAME);

		// GIVEN
		PrismContext prismContext = constructInitializedPrismContext();

		// WHEN
		PrismObject<UserType> user = prismContext.parseObject(getFile(USER_WILL_FILE_BASENAME));

		// THEN
		System.out.println("User:");
		System.out.println(user.debugDump());
		assertNotNull(user);

		assertUserWill(user);
	}

	@Test
	public void test410UserWillRoundTrip() throws Exception {
		final String TEST_NAME = "test410UserWillRoundTrip";
		PrismInternalTestUtil.displayTestTitle(TEST_NAME);

		// GIVEN
		PrismContext prismContext = constructInitializedPrismContext();

		// WHEN
		PrismObject<UserType> user = prismContext.parseObject(getFile(USER_WILL_FILE_BASENAME));

		// THEN
		System.out.println("User:");
		System.out.println(user.debugDump());
		assertNotNull(user);

		assertUserWill(user);

		// WHEN
		String serialized = prismContext.serializerFor(getOutputFormat()).serialize(user);

		// THEN
		assertNotNull(serialized);
		System.out.println("Serialized user:");
		System.out.println(serialized);

		// WHEN
		PrismObject<UserType> reparsedUser = prismContext.parseObject(serialized);

		// THEN
		System.out.println("Re-parsed user:");
		System.out.println(reparsedUser.debugDump());
		assertNotNull(reparsedUser);

		assertUserWill(reparsedUser);

	}

	@Test
	public void test500UserElisabethRoundTrip() throws Exception {
		final String TEST_NAME = "test500UserElisabethRoundTrip";
		PrismInternalTestUtil.displayTestTitle(TEST_NAME);

		// GIVEN
		PrismContext prismContext = constructInitializedPrismContext();

		// WHEN
		PrismObject<UserType> user = prismContext.parseObject(getFile(USER_ELISABETH_FILE_BASENAME));

		// THEN
		System.out.println("User:");
		System.out.println(user.debugDump());
		assertNotNull(user);

		assertUserElisabeth(user);

		// WHEN
		String serialized = prismContext.serializeObjectToString(user, getOutputFormat());

		// THEN
		assertNotNull(serialized);
		System.out.println("Serialized user:");
		System.out.println(serialized);

		// WHEN
		PrismObject<UserType> reparsedUser = prismContext.parseObject(serialized);

		// THEN
		System.out.println("Re-parsed user:");
		System.out.println(reparsedUser.debugDump());
		assertNotNull(reparsedUser);

		assertUserElisabeth(reparsedUser);

	}

	protected void assertUserAdhoc(PrismObject<UserType> user, boolean expectRawInConstructions) throws SchemaException {
		user.checkConsistence();
		assertUserJackContent(user, expectRawInConstructions);
		assertUserExtensionAdhoc(user);
		assertVisitor(user, 58);
	}


	private void assertUserExtensionAdhoc(PrismObject<UserType> user) {

		PrismContainer<?> extension = user.getExtension();
		assertContainerDefinition(extension, "extension", DOMUtil.XSD_ANY, 0, 1);
		PrismContainerValue<?> extensionValue = extension.getValue();
		assertTrue("Extension parent", extensionValue.getParent() == extension);
		assertNull("Extension ID", extensionValue.getId());
		PrismAsserts.assertPropertyValue(extension, USER_ADHOC_BOTTLES_ELEMENT, 20);

		ItemPath bottlesPath = new ItemPath(new QName(NS_FOO,"extension"), USER_ADHOC_BOTTLES_ELEMENT);
		PrismProperty<Integer> bottlesProperty = user.findProperty(bottlesPath);
		assertNotNull("Property "+bottlesPath+" not found", bottlesProperty);
		PrismAsserts.assertPropertyValue(bottlesProperty, 20);
		PrismPropertyDefinition bottlesPropertyDef = bottlesProperty.getDefinition();
		assertNotNull("No definition for bottles", bottlesPropertyDef);
		PrismAsserts.assertDefinition(bottlesPropertyDef, USER_ADHOC_BOTTLES_ELEMENT, DOMUtil.XSD_INT, 1, -1);
		assertTrue("Bottles definition is NOT dynamic", bottlesPropertyDef.isDynamic());

	}

	private void assertUserWill(PrismObject<UserType> user) throws SchemaException {
		user.checkConsistence();
		user.assertDefinitions("test");
		assertUserWillExtension(user);
		assertVisitor(user,55);
	}

	private void assertUserWillExtension(PrismObject<UserType> user) {

		PrismContainer<?> extension = user.getExtension();
		assertContainerDefinition(extension, "extension", DOMUtil.XSD_ANY, 0, 1);
		PrismContainerValue<?> extensionValue = extension.getValue();
		assertTrue("Extension parent", extensionValue.getParent() == extension);
		assertNull("Extension ID", extensionValue.getId());

		PrismProperty<String> stringType = extension.findProperty(EXTENSION_STRING_TYPE_ELEMENT);
		PrismAsserts.assertPropertyValue(stringType, "BARbar", "FOObar");
		PrismPropertyDefinition stringTypePropertyDef = stringType.getDefinition();
        PrismAsserts.assertDefinition(stringTypePropertyDef, EXTENSION_STRING_TYPE_ELEMENT, DOMUtil.XSD_STRING, 0, -1);
        assertNull("'Indexed' attribute on 'stringType' property is not null", stringTypePropertyDef.isIndexed());

        PrismProperty<String> singleStringType = extension.findProperty(EXTENSION_SINGLE_STRING_TYPE_ELEMENT);
        PrismAsserts.assertPropertyValue(singleStringType, "foobar");
        PrismPropertyDefinition singleStringTypePropertyDef = singleStringType.getDefinition();
        PrismAsserts.assertDefinition(singleStringTypePropertyDef, EXTENSION_SINGLE_STRING_TYPE_ELEMENT, DOMUtil.XSD_STRING, 0, 1);
        assertNull("'Indexed' attribute on 'singleStringType' property is not null", singleStringTypePropertyDef.isIndexed());

        PrismProperty<Double> doubleType = extension.findProperty(EXTENSION_DOUBLE_TYPE_ELEMENT);
        PrismAsserts.assertPropertyValue(doubleType, 3.1415926535);
        PrismPropertyDefinition doubleTypePropertyDef = doubleType.getDefinition();
        PrismAsserts.assertDefinition(doubleTypePropertyDef, EXTENSION_DOUBLE_TYPE_ELEMENT, DOMUtil.XSD_DOUBLE, 0, -1);
        assertNull("'Indexed' attribute on 'doubleType' property is not null", doubleTypePropertyDef.isIndexed());

        PrismProperty<Integer> intType = extension.findProperty(EXTENSION_INT_TYPE_ELEMENT);
        PrismAsserts.assertPropertyValue(intType, 42);
        PrismPropertyDefinition intTypePropertyDef = intType.getDefinition();
        PrismAsserts.assertDefinition(intTypePropertyDef, EXTENSION_INT_TYPE_ELEMENT, DOMUtil.XSD_INT, 0, -1);
        assertNull("'Indexed' attribute on 'intType' property is not null", intTypePropertyDef.isIndexed());

        PrismProperty<BigInteger> integerType = extension.findProperty(EXTENSION_INTEGER_TYPE_ELEMENT);
        PrismAsserts.assertPropertyValue(integerType, new BigInteger("19134702400093278081449423917"));
        PrismPropertyDefinition integerTypePropertyDef = integerType.getDefinition();
        PrismAsserts.assertDefinition(integerTypePropertyDef, EXTENSION_INTEGER_TYPE_ELEMENT, DOMUtil.XSD_INTEGER, 0, -1);
        assertNull("'Indexed' attribute on 'integerType' property is not null", integerTypePropertyDef.isIndexed());

        PrismProperty<Long> longType = extension.findProperty(EXTENSION_LONG_TYPE_ELEMENT);
        PrismAsserts.assertPropertyValue(longType, 299792458L);
        PrismPropertyDefinition longTypePropertyDef = longType.getDefinition();
        PrismAsserts.assertDefinition(longTypePropertyDef, EXTENSION_LONG_TYPE_ELEMENT, DOMUtil.XSD_LONG, 0, -1);
        assertNull("'Indexed' attribute on 'longType' property is not null", longTypePropertyDef.isIndexed());

        PrismProperty<XMLGregorianCalendar> dateType = extension.findProperty(EXTENSION_DATE_TYPE_ELEMENT);
        PrismAsserts.assertPropertyValue(dateType, XmlTypeConverter.createXMLGregorianCalendar(1975, 5, 30, 22, 30, 0));
        PrismPropertyDefinition dateTypePropertyDef = dateType.getDefinition();
        PrismAsserts.assertDefinition(dateTypePropertyDef, EXTENSION_DATE_TYPE_ELEMENT, DOMUtil.XSD_DATETIME, 0, -1);
        assertNull("'Indexed' attribute on 'longType' property is not null", dateTypePropertyDef.isIndexed());

        PrismProperty<Duration> durationType = extension.findProperty(EXTENSION_DURATION_TYPE_ELEMENT);
        PrismAsserts.assertPropertyValue(durationType, XmlTypeConverter.createDuration(true, 17, 3, 2, 0, 0, 0));
        PrismPropertyDefinition durationTypePropertyDef = durationType.getDefinition();
        PrismAsserts.assertDefinition(durationTypePropertyDef, EXTENSION_DURATION_TYPE_ELEMENT, DOMUtil.XSD_DURATION, 0, -1);
        assertNull("'Indexed' attribute on 'longType' property is not null", durationTypePropertyDef.isIndexed());

        PrismContainer<?> locationsType = extension.findContainer(EXTENSION_LOCATIONS_ELEMENT);
        PrismContainerDefinition<?> localtionsDef = locationsType.getDefinition();
        PrismAsserts.assertDefinition(localtionsDef, EXTENSION_LOCATIONS_ELEMENT, EXTENSION_LOCATIONS_TYPE_QNAME, 0, -1);

        PrismProperty<String> ignoredType = extension.findProperty(EXTENSION_IGNORED_TYPE_ELEMENT);
		PrismAsserts.assertPropertyValue(ignoredType, "this is just a fiction");
		PrismPropertyDefinition ignoredTypePropertyDef = ignoredType.getDefinition();
        PrismAsserts.assertDefinition(ignoredTypePropertyDef, EXTENSION_IGNORED_TYPE_ELEMENT, DOMUtil.XSD_STRING, 0, -1);
        assertNull("'Indexed' attribute on 'ignoredType' property is not null", ignoredTypePropertyDef.isIndexed());
        assertTrue("'Ignored' attribute on 'ignoredType' property is not true", ignoredTypePropertyDef.isIgnored());

        PrismProperty<String> indexedString = extension.findProperty(EXTENSION_INDEXED_STRING_TYPE_ELEMENT);
        PrismPropertyDefinition indexedStringPropertyDef = indexedString.getDefinition();
        PrismAsserts.assertDefinition(indexedStringPropertyDef, EXTENSION_SINGLE_STRING_TYPE_ELEMENT, DOMUtil.XSD_STRING, 0, -1);
        assertEquals("'Indexed' attribute on 'singleStringType' property is wrong", Boolean.FALSE, indexedStringPropertyDef.isIndexed());

		ItemPath barPath = new ItemPath(new QName(NS_FOO,"extension"), EXTENSION_BAR_ELEMENT);
		PrismProperty<String> barProperty = user.findProperty(barPath);
		assertNotNull("Property "+barPath+" not found", barProperty);
		PrismAsserts.assertPropertyValue(barProperty, "BAR");
		PrismPropertyDefinition barPropertyDef = barProperty.getDefinition();
		assertNotNull("No definition for bar", barPropertyDef);
		PrismAsserts.assertDefinition(barPropertyDef, EXTENSION_BAR_ELEMENT, DOMUtil.XSD_STRING, 1, -1);
		assertNull("'Indexed' attribute on 'bar' property is not null", barPropertyDef.isIndexed());

        PrismProperty<?> multi = extension.findProperty(EXTENSION_MULTI_ELEMENT);
        PrismPropertyDefinition multiPropertyDef = multi.getDefinition();
        PrismAsserts.assertDefinition(multiPropertyDef, EXTENSION_MULTI_ELEMENT, DOMUtil.XSD_STRING, 1, -1);
        assertNull("'Indexed' attribute on 'multi' property is not null", multiPropertyDef.isIndexed());


		PrismAsserts.assertPropertyValue(extension, EXTENSION_BAR_ELEMENT, "BAR");
		PrismAsserts.assertPropertyValue(extension, EXTENSION_NUM_ELEMENT, 42);
		Collection<PrismPropertyValue<Object>> multiPVals = extension.findProperty(EXTENSION_MULTI_ELEMENT).getValues();
		assertEquals("Multi",3,multiPVals.size());

    }

	private void assertUserElisabeth(PrismObject<UserType> user) throws SchemaException {
		user.checkConsistence();
		user.assertDefinitions("test");
		assertUserElisabethExtension(user);
		//assertVisitor(user,55);
	}

	private void assertUserElisabethExtension(PrismObject<UserType> user) {

		PrismContainer<?> extension = user.getExtension();
		assertContainerDefinition(extension, "extension", DOMUtil.XSD_ANY, 0, 1);
		PrismContainerValue<?> extensionValue = extension.getValue();
		assertTrue("Extension parent", extensionValue.getParent() == extension);
		assertNull("Extension ID", extensionValue.getId());

//		PrismProperty<String> stringType = extension.findProperty(EXTENSION_STRING_TYPE_ELEMENT);
//		PrismAsserts.assertPropertyValue(stringType, "BARbar", "FOObar");
//		PrismPropertyDefinition stringTypePropertyDef = stringType.getDefinition();
//		PrismAsserts.assertDefinition(stringTypePropertyDef, EXTENSION_STRING_TYPE_ELEMENT, DOMUtil.XSD_STRING, 0, -1);
//		assertNull("'Indexed' attribute on 'stringType' property is not null", stringTypePropertyDef.isIndexed());

		PrismProperty<String> secondaryStringType = extension.findProperty(EXTENSION_SECONDARY_SECONDARY_STRING_TYPE_ELEMENT);
		PrismAsserts.assertPropertyValue(secondaryStringType, "string1");
		PrismPropertyDefinition secondaryStringTypePropertyDef = secondaryStringType.getDefinition();
		PrismAsserts.assertDefinition(secondaryStringTypePropertyDef, EXTENSION_SINGLE_STRING_TYPE_ELEMENT, DOMUtil.XSD_STRING, 0, -1);
		assertNull("'Indexed' attribute on 'secondaryStringType' property is not null", secondaryStringTypePropertyDef.isIndexed());
	}

	protected void validateXml(String xmlString, PrismContext prismContext) throws SAXException, IOException {
	}

}
