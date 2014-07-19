/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.schema;

import static com.evolveum.midpoint.schema.TestConstants.*;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.parser.DomParser;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExtensionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author semancik
 *
 */
public abstract class TestParseUser {
	
	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}
	
	protected abstract String getSubdirName();
	
	protected abstract String getLanguage();
	
	protected abstract String getFilenameSuffix();
	
	protected File getCommonSubdir() {
		return new File(COMMON_DIR_PATH, getSubdirName());
	}
	
	protected File getFile(String baseName) {
		return new File(getCommonSubdir(), baseName+"."+getFilenameSuffix());
	}
	
	@Test
	public void testParseUserFile() throws Exception {
		final String TEST_NAME = "testParseUserFile";
		PrismTestUtil.displayTestTitle(TEST_NAME);

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		
		// WHEN
		PrismObject<UserType> user = prismContext.parseObject(getFile(USER_FILE_BASENAME));
		// THEN
		System.out.println("Parsed user:");
		System.out.println(user.debugDump());
		
		String serialized = prismContext.serializeObjectToString(user, getLanguage());
		System.out.println("Serialized: \n" +serialized);
		
		PrismObject<UserType> reparsedUser = prismContext.parseObject(serialized);
		
		assertUser(user);
	}

	@Test
	public void testParseUserDom() throws Exception {
		final String TEST_NAME = "testParseUserDom";
		PrismTestUtil.displayTestTitle(TEST_NAME);

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		
//		Document document = DOMUtil.parseFile(USER_FILE);
//		Element userElement = DOMUtil.getFirstChildElement(document);
		
		// WHEN
		PrismObject<UserType> user = prismContext.parseObject(getFile(USER_FILE_BASENAME));
		
		// THEN
		System.out.println("Parsed user:");
		System.out.println(user.debugDump());
		
		assertUser(user);
	}
	
	@Test
	public void testParseUserRoundTrip() throws Exception{
		final String TEST_NAME = "testParseUserRoundTrip";
		PrismTestUtil.displayTestTitle(TEST_NAME);

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		
//		Document document = DOMUtil.parseFile(USER_FILE);
//		Element userElement = DOMUtil.getFirstChildElement(document);
		
		// WHEN
		PrismObject<UserType> user = prismContext.parseObject(getFile(USER_FILE_BASENAME));
		
		// THEN
		System.out.println("Parsed user:");
		System.out.println(user.debugDump());
		
		assertUser(user);
		
		
		String serializedUser = prismContext.serializeObjectToString(user, getLanguage());
		System.out.println("Serialized user:");
		System.out.println(serializedUser);
		
		// REPARSE
		PrismObject<UserType> reparsedUser = prismContext.parseObject(serializedUser);
		
		// THEN
		System.out.println("Parsed user:");
		System.out.println(reparsedUser.debugDump());
		
		assertUser(reparsedUser);
		
		// and some sanity checks
		
		assertTrue("User not equals", user.equals(reparsedUser));
		
		ObjectDelta<UserType> delta = user.diff(reparsedUser);
		assertTrue("Delta not empty", delta.isEmpty());
		
	}

	
	void assertUser(PrismObject<UserType> user) throws SchemaException {
		user.checkConsistence();
		assertUserPrism(user);
		assertUserJaxb(user.asObjectable());
		
		user.checkConsistence(true, true);
	}

	void assertUserPrism(PrismObject<UserType> user) {
		
		assertEquals("Wrong oid", "2f9b9299-6f45-498f-bc8e-8d17c6b93b20", user.getOid());
//		assertEquals("Wrong version", "42", user.getVersion());
		PrismObjectDefinition<UserType> usedDefinition = user.getDefinition();
		assertNotNull("No user definition", usedDefinition);
		PrismAsserts.assertObjectDefinition(usedDefinition, new QName(SchemaConstantsGenerated.NS_COMMON, "user"),
				UserType.COMPLEX_TYPE, UserType.class);
		assertEquals("Wrong class in user", UserType.class, user.getCompileTimeClass());
		UserType userType = user.asObjectable();
		assertNotNull("asObjectable resulted in null", userType);
		
		assertPropertyValue(user, "name", PrismTestUtil.createPolyString("jack"));
		assertPropertyDefinition(user, "name", PolyStringType.COMPLEX_TYPE, 0, 1);
		assertPropertyValue(user, "fullName", new PolyString("Jack Sparrow", "jack sparrow"));
		assertPropertyDefinition(user, "fullName", PolyStringType.COMPLEX_TYPE, 0, 1);
		assertPropertyValue(user, "givenName", new PolyString("Jack", "jack"));
		assertPropertyDefinition(user, "givenName", PolyStringType.COMPLEX_TYPE, 0, 1);
		assertPropertyValue(user, "familyName", new PolyString("Sparrow", "sparrow"));
		assertPropertyDefinition(user, "familyName", PolyStringType.COMPLEX_TYPE, 0, 1);
	
		assertPropertyDefinition(user, "organizationalUnit", PolyStringType.COMPLEX_TYPE, 0, -1);
		assertPropertyValues(user, "organizationalUnit", 
				new PolyString("Brethren of the Coast", "brethren of the coast"),
				new PolyString("Davie Jones' Locker", "davie jones locker"));
		
//		PrismContainer extension = user.getExtension();
//		assertContainerDefinition(extension, "extension", DOMUtil.XSD_ANY, 0, 1);
//		PrismContainerValue extensionValue = extension.getValue();
//		assertTrue("Extension parent", extensionValue.getParent() == extension);
//		assertNull("Extension ID", extensionValue.getId());
		
		ItemPath admStatusPath = new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS);
		PrismProperty<ActivationStatusType> admStatusProperty1 = user.findProperty(admStatusPath);
		PrismAsserts.assertDefinition(admStatusProperty1.getDefinition(), ActivationType.F_ADMINISTRATIVE_STATUS, SchemaConstants.C_ACTIVATION_STATUS_TYPE, 0, 1);
		assertNotNull("Property "+admStatusPath+" not found", admStatusProperty1);
		PrismAsserts.assertPropertyValue(admStatusProperty1, ActivationStatusType.ENABLED);
		
//		PrismProperty validFromProperty = user.findProperty(new PropertyPath(UserType.F_ACTIVATION, ActivationType.F_VALID_FROM));
//		assertNotNull("Property "+ActivationType.F_VALID_FROM+" not found", validFromProperty);
//		PrismAsserts.assertPropertyValue(validFromProperty, USER_JACK_VALID_FROM);
		
		PrismContainer<AssignmentType> assignmentContainer = user.findContainer(UserType.F_ASSIGNMENT);
		PrismAsserts.assertDefinition(assignmentContainer.getDefinition(), UserType.F_ASSIGNMENT, AssignmentType.COMPLEX_TYPE, 0, -1);
		assertEquals("Wrong number of assignment values", 1, assignmentContainer.getValues().size());
		PrismContainerValue<AssignmentType> firstAssignmentValue = assignmentContainer.getValues().iterator().next();
		
		PrismContainer<Containerable> assignmentExtensionContainer = firstAssignmentValue.findContainer(AssignmentType.F_EXTENSION);
		PrismAsserts.assertDefinition(assignmentExtensionContainer.getDefinition(), AssignmentType.F_EXTENSION, ExtensionType.COMPLEX_TYPE, 0, 1);
		List<Item<?>> assignmentExtensionItems = assignmentExtensionContainer.getValue().getItems();
		assertNotNull("No assignment extension items", assignmentExtensionItems);
		assertEquals("Wrong number of assignment extension items", 1, assignmentExtensionItems.size());
		PrismProperty<String> firstAssignmentExtensionItem = (PrismProperty<String>) assignmentExtensionItems.get(0);
		PrismAsserts.assertDefinition(firstAssignmentExtensionItem.getDefinition(), EXTENSION_INT_TYPE_ELEMENT, DOMUtil.XSD_INT, 0, -1);
		PrismPropertyValue<String> firstValueOfFirstAssignmentExtensionItem = firstAssignmentExtensionItem.getValues().get(0);
		assertEquals("Wrong value of "+EXTENSION_INT_TYPE_ELEMENT+" in assignment extension", 42, firstValueOfFirstAssignmentExtensionItem.getValue());
		
		// TODO: check accountConstruction
		
		PrismReference accountRef = user.findReference(UserType.F_LINK_REF);
		assertEquals("Wrong number of accountRef values", 3, accountRef.getValues().size());
		PrismAsserts.assertReferenceValue(accountRef, USER_ACCOUNT_REF_1_OID);
		PrismAsserts.assertReferenceValue(accountRef, USER_ACCOUNT_REF_2_OID);
		PrismAsserts.assertReferenceValue(accountRef, USER_ACCOUNT_REF_3_OID);
		
		PrismReferenceValue accountRef1Val = accountRef.findValueByOid(USER_ACCOUNT_REF_1_OID);
		assertNotNull("No object in ref1 (prism)", accountRef1Val.getObject());
		assertNotNull("No object definition in ref1 (prism)", accountRef1Val.getObject().getDefinition());
		assertEquals("Wrong ref1 oid (prism)", USER_ACCOUNT_REF_1_OID, accountRef1Val.getOid());
		assertEquals("Wrong ref1 type (prism)", ShadowType.COMPLEX_TYPE, accountRef1Val.getTargetType());
		
		PrismReferenceValue accountRef3Val = accountRef.findValueByOid(USER_ACCOUNT_REF_3_OID);
		assertEquals("Wrong ref3 oid (prism)",  USER_ACCOUNT_REF_3_OID, accountRef3Val.getOid());
		assertEquals("Wrong ref3 type (prism)", ShadowType.COMPLEX_TYPE, accountRef3Val.getTargetType());
		assertEquals("Wrong ref3 description (prism)", "This is third accountRef", accountRef3Val.getDescription());
		SearchFilterType accountRef3ValFilterElement = accountRef3Val.getFilter();
		assertFilter("ref3", accountRef3ValFilterElement);
	}
	
	private void assertFilterElement(String message, Element filterElement) {
		assertNotNull("No "+message+" filter", filterElement);
		System.out.println("Filter element "+message);
		System.out.println(DOMUtil.serializeDOMToString(filterElement));
		assertEquals("Wrong "+message+" filter namespace", PrismConstants.NS_QUERY, filterElement.getNamespaceURI());
		assertEquals("Wrong "+message+" filter localName", "equal", filterElement.getLocalName());
	}
	
	private void assertFilter(String message, SearchFilterType filter) {
		assertNotNull("No "+message+" filter", filter);
	}

	private void assertUserJaxb(UserType userType) throws SchemaException {
		assertEquals("Wrong name", PrismTestUtil.createPolyStringType("jack"), userType.getName());
		assertEquals("Wrong fullName (orig)", "Jack Sparrow", userType.getFullName().getOrig());
        assertEquals("Wrong fullName (norm)", "jack sparrow", userType.getFullName().getNorm());
		assertEquals("Wrong givenName (orig)", "Jack", userType.getGivenName().getOrig());
		assertEquals("Wrong givenName (norm)", "jack", userType.getGivenName().getNorm());
		assertEquals("Wrong familyName (orig)", "Sparrow", userType.getFamilyName().getOrig());
		assertEquals("Wrong familyName (norm)", "sparrow", userType.getFamilyName().getNorm());

		ActivationType activation = userType.getActivation();
		assertNotNull("No activation", activation);
		assertEquals("User not enabled", ActivationStatusType.ENABLED, activation.getAdministrativeStatus());
		
		List<ObjectReferenceType> accountRefs = userType.getLinkRef();
		assertNotNull("No accountRef list", accountRefs);
		assertEquals("Wrong number of list entries", 3, accountRefs.size());
		
		ObjectReferenceType ref1 = ObjectTypeUtil.findRef(USER_ACCOUNT_REF_1_OID, accountRefs);
		assertEquals("Wrong ref1 oid (jaxb)", USER_ACCOUNT_REF_1_OID, ref1.getOid());
		assertEquals("Wrong ref1 type (jaxb)", ShadowType.COMPLEX_TYPE, ref1.getType());

		ObjectReferenceType ref2 = ObjectTypeUtil.findRef(USER_ACCOUNT_REF_2_OID, accountRefs);
		assertEquals("Wrong ref2 oid (jaxb)", USER_ACCOUNT_REF_2_OID, ref2.getOid());
		assertEquals("Wrong ref2 type (jaxb)", ShadowType.COMPLEX_TYPE, ref2.getType());
		
		ObjectReferenceType ref3 = ObjectTypeUtil.findRef(USER_ACCOUNT_REF_3_OID, accountRefs);
		assertEquals("Wrong ref3 oid (jaxb)", USER_ACCOUNT_REF_3_OID, ref3.getOid());
		assertEquals("Wrong ref3 type (jaxb)", ShadowType.COMPLEX_TYPE, ref3.getType());
		SearchFilterType ref3Filter = ref3.getFilter();
		assertNotNull("No ref3 filter (jaxb,class)", ref3Filter);
		assertFilterElement("ref filter (jaxb)", ref3Filter.getFilterClauseAsElement());
	}

	private void assertPropertyDefinition(PrismContainer<?> container, String propName, QName xsdType, int minOccurs,
			int maxOccurs) {
		QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
		PrismAsserts.assertPropertyDefinition(container, propQName, xsdType, minOccurs, maxOccurs);
	}
	
	public static void assertPropertyValue(PrismContainer<?> container, String propName, Object propValue) {
		QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
		PrismAsserts.assertPropertyValue(container, propQName, propValue);
	}

	public static <T> void assertPropertyValues(PrismContainer<?> container, String propName, T... expectedValues) {
		QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
		PrismAsserts.assertPropertyValue(container, propQName, expectedValues);
	}

    @Test
    public void testPrismConsistency() throws Exception {

        System.out.println("===[ testPrismConsistency ]===");

        // GIVEN
        PrismContext ctx = PrismTestUtil.getPrismContext();
        PrismObjectDefinition<UserType> userDefinition = ctx.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);

        // WHEN
        PrismObject<UserType> user = userDefinition.instantiate();
        user.setOid("12345");

        // THEN
        System.out.println("User:");
        System.out.println(user.debugDump());

        System.out.println("Checking consistency, 1st time.");
        user.checkConsistence();
        UserType userType = user.getValue().getValue();

        System.out.println("Checking consistency, 2nd time - after getValue().getValue().");
        user.checkConsistence();

        System.out.println("OK.");
    }


}
