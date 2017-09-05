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
package com.evolveum.midpoint.schema.parser;

import static com.evolveum.midpoint.schema.TestConstants.*;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author semancik
 *
 */
public class TestParseUser extends AbstractObjectParserTest<UserType> {

	@Override
	protected File getFile() {
		return getFile(USER_FILE_BASENAME);
	}

	@Test
	public void testParseFileAsPCV() throws Exception {
		displayTestTitle("testParseFileAsPCV");
		processParsingsPCV(null, null);
	}

	@Test
	public void testParseFileAsPO() throws Exception {
		displayTestTitle("testParseFileAsPO");
		processParsingsPO(null, null, true);
	}

	@Test
	public void testParseRoundTripAsPCV() throws Exception{
		displayTestTitle("testParseRoundTripAsPCV");

		SerializationOptions o = SerializationOptions.createSerializeReferenceNames();
		processParsingsPCV(v -> getPrismContext().serializerFor(language).options(o).serialize(v), "s0");
		processParsingsPCV(v -> getPrismContext().serializerFor(language).options(o).root(new QName("dummy")).serialize(v), "s1");
		processParsingsPCV(v -> getPrismContext().serializerFor(language).options(o).root(SchemaConstantsGenerated.C_SYSTEM_CONFIGURATION).serialize(v), "s2");		// misleading item name
		processParsingsPCV(v -> getPrismContext().serializerFor(language).options(o).serializeRealValue(v.asContainerable()), "s3");
		processParsingsPCV(v -> getPrismContext().serializerFor(language).options(o).root(new QName("dummy")).serializeAnyData(v.asContainerable()), "s4");
	}

	@Test
	public void testParseRoundTripAsPO() throws Exception{
		displayTestTitle("testParseRoundTripAsPO");

		SerializationOptions o = SerializationOptions.createSerializeReferenceNames();
		processParsingsPO(v -> getPrismContext().serializerFor(language).options(o).serialize(v), "s0", true);
		processParsingsPO(v -> getPrismContext().serializerFor(language).options(o).root(new QName("dummy")).serialize(v), "s1", false);
		processParsingsPO(v -> getPrismContext().serializerFor(language).options(o).root(SchemaConstantsGenerated.C_SYSTEM_CONFIGURATION).serialize(v), "s2", false);		// misleading item name
		processParsingsPO(v -> getPrismContext().serializerFor(language).options(o).serializeRealValue(v.asObjectable()), "s3", false);
		processParsingsPO(v -> getPrismContext().serializerFor(language).options(o).root(new QName("dummy")).serializeAnyData(v.asObjectable()), "s4", false);
	}

	private void processParsingsPCV(SerializingFunction<PrismContainerValue<UserType>> serializer, String serId) throws Exception {
		processParsings(UserType.class, null, UserType.COMPLEX_TYPE, null, serializer, serId);
	}

	private void processParsingsPO(SerializingFunction<PrismObject<UserType>> serializer, String serId, boolean checkItemName) throws Exception {
		processObjectParsings(UserType.class, UserType.COMPLEX_TYPE, serializer, serId, checkItemName);
	}

	@Override
	protected void assertPrismContainerValueLocal(PrismContainerValue<UserType> value) throws SchemaException {
		PrismObject user = value.asContainerable().asPrismObject();
		user.checkConsistence();
		assertUserPrism(user, false);
		assertUserJaxb(value.asContainerable(), false);
	}

	@Override
	protected void assertPrismObjectLocal(PrismObject<UserType> user) throws SchemaException {
		assertUserPrism(user, true);
		assertUserJaxb(user.asObjectable(), true);
		user.checkConsistence(true, true);
	}



	void assertUserPrism(PrismObject<UserType> user, boolean isObject) {

		if (isObject) {
			assertEquals("Wrong oid", "2f9b9299-6f45-498f-bc8e-8d17c6b93b20", user.getOid());
		}
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
		List<Item<?,?>> assignmentExtensionItems = assignmentExtensionContainer.getValue().getItems();
		assertNotNull("No assignment extension items", assignmentExtensionItems);
		assertEquals("Wrong number of assignment extension items", 1, assignmentExtensionItems.size());
		PrismProperty<String> firstAssignmentExtensionItem = (PrismProperty<String>) assignmentExtensionItems.get(0);
		PrismAsserts.assertDefinition(firstAssignmentExtensionItem.getDefinition(), EXTENSION_INT_TYPE_ELEMENT, DOMUtil.XSD_INT, 0, -1);
		PrismPropertyValue<String> firstValueOfFirstAssignmentExtensionItem = firstAssignmentExtensionItem.getValues().get(0);
		assertEquals("Wrong value of "+EXTENSION_INT_TYPE_ELEMENT+" in assignment extension", 42, firstValueOfFirstAssignmentExtensionItem.getValue());

		PrismContainer<Containerable> constructionContainer = firstAssignmentValue.findContainer(AssignmentType.F_CONSTRUCTION);
		PrismAsserts.assertDefinition(constructionContainer.getDefinition(), AssignmentType.F_CONSTRUCTION, ConstructionType.COMPLEX_TYPE, 0, 1);
		List<Item<?,?>> constructionItems = constructionContainer.getValue().getItems();
		assertNotNull("No construction items", constructionItems);
		assertEquals("Wrong number of construction items", 1, constructionItems.size());
		PrismReference firstConstructionItem = (PrismReference) constructionItems.get(0);
		PrismAsserts.assertDefinition(firstConstructionItem.getDefinition(), ConstructionType.F_RESOURCE_REF, ObjectReferenceType.COMPLEX_TYPE, 0, 1);
		PrismReferenceValue firstValueOfFirstConstructionItem = firstConstructionItem.getValues().get(0);
		assertEquals("Wrong resource name", "resource1", PolyString.getOrig(firstValueOfFirstConstructionItem.getTargetName()));

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
		if (namespaces) {
			assertEquals("Wrong " + message + " filter namespace", PrismConstants.NS_QUERY, filterElement.getNamespaceURI());
		}
		assertEquals("Wrong "+message+" filter localName", "equal", filterElement.getLocalName());
	}

	private void assertFilter(String message, SearchFilterType filter) {
		assertNotNull("No "+message+" filter", filter);
	}

	private void assertUserJaxb(UserType userType, boolean isObject) throws SchemaException {
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
		assertFilterElement("ref filter (jaxb)", ref3Filter.getFilterClauseAsElement(getPrismContext()));
	}

    @Test
    public void testPrismConsistency() throws Exception {

        System.out.println("===[ testPrismConsistency ]===");

        // GIVEN
        PrismContext ctx = getPrismContext();
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
