/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.PrismJaxbProcessor;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExtensionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType.Filter;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static com.evolveum.midpoint.schema.TestConstants.*;

/**
 * @author semancik
 *
 */
public class TestParseUser {
	
	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}
	
	
	@Test
	public void testParseUserFile() throws SchemaException {
		System.out.println("===[ testParseUserFile ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		
		// WHEN
		PrismObject<UserType> user = prismContext.parseObject(USER_FILE);
		
		// THEN
		System.out.println("Parsed user:");
		System.out.println(user.dump());
		
		assertUser(user);
	}

	@Test
	public void testParseUserDom() throws SchemaException {
		System.out.println("===[ testParseUserDom ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		
		Document document = DOMUtil.parseFile(USER_FILE);
		Element userElement = DOMUtil.getFirstChildElement(document);
		
		// WHEN
		PrismObject<UserType> user = prismContext.parseObject(userElement);
		
		// THEN
		System.out.println("Parsed user:");
		System.out.println(user.dump());
		
		assertUser(user);
	}

	@Test
	public void testPrismParseJaxb() throws JAXBException, SchemaException, SAXException, IOException {
		System.out.println("===[ testPrismParseJaxb ]===");
		
		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		PrismJaxbProcessor jaxbProcessor = prismContext.getPrismJaxbProcessor();
		
		// WHEN
		UserType userType = jaxbProcessor.unmarshalObject(USER_FILE, UserType.class);
		
		// THEN
		System.out.println("Parsed user:");
		System.out.println(userType.asPrismObject().dump());
		
		assertUser(userType.asPrismObject());
	}
	
	/**
	 * The definition should be set properly even if the declared type is ObjectType. The Prism should determine
	 * the actual type.
	 */
	@Test
	public void testPrismParseJaxbObjectType() throws JAXBException, SchemaException, SAXException, IOException {
		System.out.println("===[ testPrismParseJaxbObjectType ]===");
		
		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		PrismJaxbProcessor jaxbProcessor = prismContext.getPrismJaxbProcessor();
		
		// WHEN
		ObjectType userType = jaxbProcessor.unmarshalObject(USER_FILE, ObjectType.class);
		
		// THEN
		System.out.println("Parsed user:");
		System.out.println(userType.asPrismObject().dump());
		
		assertUser(userType.asPrismObject());
	}
	
	/**
	 * Parsing in form of JAXBELement
	 */
	@Test
	public void testPrismParseJaxbElement() throws JAXBException, SchemaException, SAXException, IOException {
		System.out.println("===[ testPrismParseJaxbElement ]===");
		
		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		PrismJaxbProcessor jaxbProcessor = prismContext.getPrismJaxbProcessor();
		
		// WHEN
		JAXBElement<UserType> jaxbElement = jaxbProcessor.unmarshalElement(USER_FILE, UserType.class);
		UserType userType = jaxbElement.getValue();
		
		// THEN
		System.out.println("Parsed user:");
		System.out.println(userType.asPrismObject().dump());
		
		assertUser(userType.asPrismObject());
	}

	/**
	 * Parsing in form of JAXBELement, with declared ObjectType
	 */
	@Test
	public void testPrismParseJaxbElementObjectType() throws JAXBException, SchemaException, SAXException, IOException {
		System.out.println("===[ testPrismParseJaxbElementObjectType ]===");
		
		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		PrismJaxbProcessor jaxbProcessor = prismContext.getPrismJaxbProcessor();
		
		// WHEN
		JAXBElement<ObjectType> jaxbElement = jaxbProcessor.unmarshalElement(USER_FILE, ObjectType.class);
		ObjectType userType = jaxbElement.getValue();
		
		// THEN
		System.out.println("Parsed user:");
		System.out.println(userType.asPrismObject().dump());
		
		assertUser(userType.asPrismObject());
	}

	
	private void assertUser(PrismObject<UserType> user) {
		user.checkConsistence();
		assertUserPrism(user);
		assertUserJaxb(user.asObjectable());
	}

	private void assertUserPrism(PrismObject<UserType> user) {
		
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
		assertPropertyDefinition(user, "fullName", PolyStringType.COMPLEX_TYPE, 1, 1);
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
		Element accountRef3ValFilterElement = accountRef3Val.getFilter();
		assertFilter("ref3", accountRef3ValFilterElement);
	}
	
	private void assertFilter(String message, Element filterElement) {
		assertNotNull("No "+message+" filter", filterElement);
		assertEquals("Wrong "+message+" filter namespace", PrismConstants.NS_QUERY, filterElement.getNamespaceURI());
		assertEquals("Wrong "+message+" filter localName", "equal", filterElement.getLocalName());
	}

	private void assertUserJaxb(UserType userType) {
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
		Filter ref3Filter = ref3.getFilter();
		assertNotNull("No ref3 filter (jaxb,class)", ref3Filter);
		assertFilter("ref filter (jaxb)", ref3Filter.getFilter());
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
        PrismObjectDefinition<UserType> userDefinition = ctx.getSchemaRegistry().getObjectSchema().findObjectDefinitionByCompileTimeClass(UserType.class);

        // WHEN
        PrismObject<UserType> user = userDefinition.instantiate();
        user.setOid("12345");

        // THEN
        System.out.println("User:");
        System.out.println(user.dump());

        System.out.println("Checking consistency, 1st time.");
        user.checkConsistence();
        UserType userType = user.getValue().getValue();

        System.out.println("Checking consistency, 2nd time - after getValue().getValue().");
        user.checkConsistence();

        System.out.println("OK.");
    }


}
