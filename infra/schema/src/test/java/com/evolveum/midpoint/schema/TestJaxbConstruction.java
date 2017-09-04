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

import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static com.evolveum.midpoint.schema.TestConstants.*;

import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.schema.util.SchemaTestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExtensionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

/**
 * @author semancik
 *
 */
public class TestJaxbConstruction {

	private static final String FAUX_RESOURCE_OID = "fuuuuuuuuuuu";
	private static final String ACCOUNT_NAME = "jack";

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

	@Test
	public void testUserConstruction() throws JAXBException, SchemaException {
		System.out.println("\n\n ===[ testUserConstruction ]===\n");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		UserType userType = new UserType();
		prismContext.adopt(userType);

		PrismObject<UserType> user = userType.asPrismObject();
		assertNotNull("No object definition after adopt", user.getDefinition());
		SchemaTestUtil.assertUserDefinition(user.getDefinition());

		// fullName: PolyString
		userType.setFullName(new PolyStringType("Čučoriedka"));
		PrismProperty<PolyString> fullNameProperty = user.findProperty(UserType.F_FULL_NAME);
		PolyString fullName = fullNameProperty.getRealValue();
		assertEquals("Wrong fullName orig", "Čučoriedka", fullName.getOrig());
		assertEquals("Wrong fullName norm", "cucoriedka", fullName.getNorm());

		// description: setting null value
		userType.setDescription(null);
		PrismProperty<String> descriptionProperty = user.findProperty(UserType.F_DESCRIPTION);
		assertNull("Unexpected description property "+descriptionProperty, descriptionProperty);

		// description: setting null value
		userType.setDescription("blah blah");
		descriptionProperty = user.findProperty(UserType.F_DESCRIPTION);
		assertEquals("Wrong description value", "blah blah", descriptionProperty.getRealValue());

		// description: resetting null value
		userType.setDescription(null);
		descriptionProperty = user.findProperty(UserType.F_DESCRIPTION);
		assertNull("Unexpected description property (after reset) "+descriptionProperty, descriptionProperty);

		// Extension
		ExtensionType extension = new ExtensionType();
		userType.setExtension(extension);

		user.checkConsistence();

		PrismContainer<Containerable> extensionContainer = user.findContainer(GenericObjectType.F_EXTENSION);
		checkExtension(extensionContainer,"user extension after setExtension");
		checkExtension(extension,"user extension after setExtension");

		AssignmentType assignmentType = new AssignmentType();
		userType.getAssignment().add(assignmentType);

		user.checkConsistence();
		user.assertDefinitions();

		// Assignment
		ExtensionType assignmentExtension = new ExtensionType();
		assignmentType.setExtension(assignmentExtension);

		user.assertDefinitions();
		user.checkConsistence();
		checkExtension(assignmentExtension,"assignment extension after setExtension");
		user.checkConsistence();
		user.assertDefinitions();

		// accountRef/account
		ObjectReferenceType accountRefType = new ObjectReferenceType();
		accountRefType.setOid(USER_ACCOUNT_REF_1_OID);
		MapXNode filterElement = createFilter();
		SearchFilterType filter = new SearchFilterType();
		filter.setFilterClauseXNode(filterElement);
		accountRefType.setFilter(filter);
		userType.getLinkRef().add(accountRefType);

		assertAccountRefs(userType, USER_ACCOUNT_REF_1_OID);
		user.checkConsistence();
		user.assertDefinitions();

		PrismReference accountRef = user.findReference(UserType.F_LINK_REF);
        assertEquals("1/ Wrong accountRef values", 1, accountRef.getValues().size());
        PrismReferenceValue accountRefVal0 = accountRef.getValue(0);
        SearchFilterType prismFilter = accountRefVal0.getFilter();
        assertNotNull("Filter have not passed", prismFilter);
        //assertTrue("Bad filter in reference", prismFilter instanceof EqualsFilter);
//        assertEquals("Difference filter", filterElement, prismFilter);

        ShadowType accountShadowType = new ShadowType();
        prismContext.adopt(accountShadowType);
        accountShadowType.setOid(USER_ACCOUNT_REF_1_OID);
        userType.getLink().add(accountShadowType);
        //value still should be only one... (reference was only resolved)
        assertEquals("2/ Wrong accountRef values", 1, user.findReference(UserType.F_LINK_REF).getValues().size());

		accountShadowType = new ShadowType();
        prismContext.adopt(accountShadowType);
		accountShadowType.setOid(USER_ACCOUNT_REF_2_OID);
		userType.getLink().add(accountShadowType);

        assertEquals("3/ Wrong accountRef values", 2, user.findReference(UserType.F_LINK_REF).getValues().size());

		assertAccountRefs(userType, USER_ACCOUNT_REF_1_OID, USER_ACCOUNT_REF_2_OID);
		user.checkConsistence();
		user.assertDefinitions();

		assertEquals("4/ Wrong accountRef values", 2, accountRef.getValues().size());
		PrismAsserts.assertReferenceValues(accountRef, USER_ACCOUNT_REF_1_OID, USER_ACCOUNT_REF_2_OID);

	}

	@Test
	public void testUserConstructionBeforeAdopt() throws Exception {
		System.out.println("\n\n ===[ testUserConstructionBeforeAdopt ]===\n");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		UserType userType = new UserType();

		userType.setFullName(new PolyStringType("Čučoriedka"));
		userType.setDescription("blah blah");
		ExtensionType extension = new ExtensionType();
		userType.setExtension(extension);

		AssignmentType assignmentType = new AssignmentType();
		userType.getAssignment().add(assignmentType);


		// Assignment
		ExtensionType assignmentExtension = new ExtensionType();
		assignmentType.setExtension(assignmentExtension);


		// accountRef/account
		ObjectReferenceType accountRefType = new ObjectReferenceType();
		accountRefType.setOid(USER_ACCOUNT_REF_1_OID);

		MapXNode filterElement = createFilter();
		SearchFilterType filter = new SearchFilterType();
		filter.setFilterClauseXNode(filterElement);
		accountRefType.setFilter(filter);
		userType.getLinkRef().add(accountRefType);


		ShadowType accountShadowType = new ShadowType();
        accountShadowType.setOid(USER_ACCOUNT_REF_1_OID);
        userType.getLink().add(accountShadowType);

		accountShadowType = new ShadowType();
		accountShadowType.setOid(USER_ACCOUNT_REF_2_OID);
		userType.getLink().add(accountShadowType);

		// WHEN
		prismContext.adopt(userType);

		// THEN
		PrismObject<UserType> user = userType.asPrismObject();
		assertNotNull("No object definition after adopt", user.getDefinition());
		SchemaTestUtil.assertUserDefinition(user.getDefinition());

		// fullName: PolyString
		PrismProperty<PolyString> fullNameProperty = user.findProperty(UserType.F_FULL_NAME);
		user.checkConsistence();
		user.assertDefinitions();


		PolyString fullName = fullNameProperty.getRealValue();
		assertEquals("Wrong fullName orig", "Čučoriedka", fullName.getOrig());
		assertEquals("Wrong fullName norm", "cucoriedka", fullName.getNorm());

		PrismProperty<String> descriptionProperty = user.findProperty(UserType.F_DESCRIPTION);
		assertEquals("Wrong description value", "blah blah", descriptionProperty.getRealValue());

		PrismContainer<Containerable> extensionContainer = user.findContainer(GenericObjectType.F_EXTENSION);
		checkExtension(extensionContainer,"user extension");
		checkExtension(extension,"user extension");

		PrismReference accountRef = user.findReference(UserType.F_LINK_REF);
        assertEquals("Wrong accountRef values", 2, accountRef.getValues().size());
		PrismAsserts.assertReferenceValues(accountRef, USER_ACCOUNT_REF_1_OID, USER_ACCOUNT_REF_2_OID);

        PrismReferenceValue accountRefVal0 = accountRef.getValue(0);
        SearchFilterType prismFilter = accountRefVal0.getFilter();
        assertNotNull("Filter have not passed", prismFilter);
        // assertTrue("Wrong filter in reference " , prismFilter instanceof EqualsFilter);
//        assertEquals("Difference filter", filterElement, prismFilter);

		assertAccountRefs(userType, USER_ACCOUNT_REF_1_OID, USER_ACCOUNT_REF_2_OID);

		user.assertDefinitions();
		user.checkConsistence();

	}


	private void assertAccountRefs(UserType userType, String... accountOids) {
		List<ObjectReferenceType> accountRefs = userType.getLinkRef();
		assertEquals("Wrong number of accountRefs", accountOids.length, accountRefs.size());
		for (String expectedAccountOid: accountOids) {
			assertAccountRef(accountRefs, expectedAccountOid);
		}
	}

	private void assertAccountRef(List<ObjectReferenceType> accountRefs, String expectedAccountOid) {
		for (ObjectReferenceType accountRef: accountRefs) {
			if (accountRef.getOid().equals(expectedAccountOid)) {
				return;
			}
		}
		AssertJUnit.fail("acountRef for oid "+expectedAccountOid+" was not found");
	}

	/**
	 * Similar to testUserConstruction, but some operations are done in a different order
	 * e.g. assignment is filled in first then set to the user.
	 */
	@Test
	public void testUserConstructionReverse() throws JAXBException, SchemaException {
		System.out.println("\n\n ===[ testUserConstructionReverse ]===\n");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		UserType userType = new UserType(prismContext);

		PrismObject<UserType> user = userType.asPrismObject();
		assertNotNull("No object definition after adopt", user.getDefinition());

		// Extension
		ExtensionType extension = new ExtensionType();
		userType.setExtension(extension);

		user.checkConsistence();

		PrismContainer<Containerable> extensionContainer = user.findContainer(GenericObjectType.F_EXTENSION);
		checkExtension(extensionContainer,"user extension after setExtension");
		checkExtension(extension,"user extension after setExtension");

		AssignmentType assignmentType = new AssignmentType(prismContext);

		ExtensionType assignmentExtension = new ExtensionType();
		assignmentType.setExtension(assignmentExtension);

		PrismContainerValue<ExtensionType> assignmentExtensionValueFromJaxb = assignmentExtension.asPrismContainerValue();
		PrismProperty<Integer> intProperty = assignmentExtensionValueFromJaxb.findOrCreateProperty(EXTENSION_INT_TYPE_ELEMENT);
		intProperty.setRealValue(15);

		PrismProperty<String> stringProperty = assignmentExtensionValueFromJaxb.findOrCreateItem(EXTENSION_STRING_TYPE_ELEMENT, PrismProperty.class);
		stringProperty.setRealValue("fifteen men on a dead man chest");

		// Adding assignemnt to the user should cause application of definitions
		userType.getAssignment().add(assignmentType);

		PrismAsserts.assertDefinition(assignmentType.asPrismContainerValue().getParent().getDefinition(),
				UserType.F_ASSIGNMENT, AssignmentType.COMPLEX_TYPE, 0, -1);
		PrismAsserts.assertDefinition(assignmentExtensionValueFromJaxb.getParent().getDefinition(),
				AssignmentType.F_EXTENSION, ExtensionType.COMPLEX_TYPE, 0, 1);
		assertTrue("assignment extension definition is not runtime", assignmentExtensionValueFromJaxb.getParent().getDefinition().isRuntimeSchema());
		assertTrue("assignment extension definition is not dynamic", assignmentExtensionValueFromJaxb.getParent().getDefinition().isDynamic());
		PrismAsserts.assertDefinition(intProperty.getDefinition(), EXTENSION_INT_TYPE_ELEMENT, DOMUtil.XSD_INT, 0, -1);
		PrismAsserts.assertDefinition(stringProperty.getDefinition(), EXTENSION_STRING_TYPE_ELEMENT, DOMUtil.XSD_STRING, 0, -1);

		user.assertDefinitions();
		user.checkConsistence();
	}



	@Test
	public void testGenericConstruction() throws JAXBException, SchemaException {
		System.out.println("\n\n ===[ testGenericConstruction ]===\n");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		GenericObjectType genericType = new GenericObjectType();
		prismContext.adopt(genericType);

		PrismObject<GenericObjectType> generic = genericType.asPrismObject();
		assertNotNull("No object definition after adopt", generic.getDefinition());

		// WHEN
		ExtensionType extension = new ExtensionType();
		genericType.setExtension(extension);

		// THEN
		generic.checkConsistence();

		PrismContainer<Containerable> extensionContainer = generic.findContainer(GenericObjectType.F_EXTENSION);
		checkExtension(extensionContainer,"user extension after setExtension");
		checkExtension(extension,"user extension after setExtension");

		generic.checkConsistence();
	}

	@Test
	public void testAccountConstruction() throws JAXBException, SchemaException {
		System.out.println("\n\n ===[ testAccountConstruction ]===\n");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		ShadowType accountType = new ShadowType();
		prismContext.adopt(accountType);

		PrismObject<ShadowType> account = accountType.asPrismObject();
		assertNotNull("No object definition after adopt", account.getDefinition());

		// WHEN
		accountType.setName(PrismTestUtil.createPolyStringType(ACCOUNT_NAME));
		ObjectReferenceType resourceRefType = new ObjectReferenceType();
		resourceRefType.setOid(FAUX_RESOURCE_OID);
		resourceRefType.setType(ResourceType.COMPLEX_TYPE);
		accountType.setResourceRef(resourceRefType);

		// THEN (prism)
		account.checkConsistence();
		PrismAsserts.assertPropertyValue(account, ShadowType.F_NAME, PrismTestUtil.createPolyString(ACCOUNT_NAME));
		PrismReference resourceRef = account.findReference(ShadowType.F_RESOURCE_REF);
		assertNotNull("No resourceRef", resourceRef);
    	PrismReferenceValue resourceRefVal = resourceRef.getValue();
    	assertNotNull("No resourceRef value", resourceRefVal);
    	assertEquals("Wrong OID in resourceRef value", FAUX_RESOURCE_OID, resourceRefVal.getOid());
    	assertEquals("Wrong type in resourceRef value", ResourceType.COMPLEX_TYPE, resourceRefVal.getTargetType());
//    	Element filter = resourceRefVal.getFilter();
//    	assertNotNull("No filter in resourceRef value", filter);

		// THEN (JAXB)
		assertEquals("Wrong name (JAXB)", PrismTestUtil.createPolyStringType(ACCOUNT_NAME), accountType.getName());
		resourceRefType = accountType.getResourceRef();
		assertNotNull("No resourceRef (JAXB)", resourceRefType);
		assertEquals("Wrong OID in resourceRef (JAXB)", FAUX_RESOURCE_OID, resourceRefType.getOid());
		assertEquals("Wrong type in resourceRef (JAXB)", ResourceType.COMPLEX_TYPE, resourceRefType.getType());
//    	Filter filter = connectorRef.getFilter();
//    	assertNotNull("No filter in connectorRef value (JAXB)", filter);
//    	Element filterElement = filter.getFilter();
//    	assertNotNull("No filter element in connectorRef value (JAXB)", filterElement);

	}

    @Test
    public void testExtensionTypeConstruction() throws Exception {
    	System.out.println("\n\n ===[ testExtensionTypeConstruction ]===\n");

    	// GIVEN
    	PrismContext prismContext = PrismTestUtil.getPrismContext();

        GenericObjectType object = new GenericObjectType();
        prismContext.adopt(object.asPrismObject(), GenericObjectType.class);

        ExtensionType extension = new ExtensionType();
        object.setExtension(extension);

        PrismContainerValue extValue = extension.asPrismContainerValue();
        assertNotNull("No extension definition", extValue.getParent().getDefinition());

        // WHEN
        Item item = extValue.findOrCreateItem(SchemaTestConstants.EXTENSION_STRING_TYPE_ELEMENT);

        // THEN
        assertNotNull(item);
        object.asPrismObject().checkConsistence();
    }

    @Test
	public void testResourceConstruction() throws Exception {
		System.out.println("\n\n ===[ testResourceConstruction ]===\n");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		ResourceType resourceType = new ResourceType();
		prismContext.adopt(resourceType);

		PrismObject<ResourceType> resource = resourceType.asPrismObject();
		assertNotNull("No object definition after adopt", resource.getDefinition());

		// name: PolyString
		resourceType.setName(new PolyStringType("Môj risórs"));
		PrismProperty<PolyString> fullNameProperty = resource.findProperty(ResourceType.F_NAME);
		PolyString fullName = fullNameProperty.getRealValue();
		assertEquals("Wrong name orig", "Môj risórs", fullName.getOrig());
		assertEquals("Wrong name norm", "moj risors", fullName.getNorm());

		// description: setting null value
		resourceType.setDescription(null);
		PrismProperty<String> descriptionProperty = resource.findProperty(UserType.F_DESCRIPTION);
		assertNull("Unexpected description property "+descriptionProperty, descriptionProperty);

		// description: setting null value
		resourceType.setDescription("blah blah");
		descriptionProperty = resource.findProperty(UserType.F_DESCRIPTION);
		assertEquals("Wrong description value", "blah blah", descriptionProperty.getRealValue());

		// description: resetting null value
		resourceType.setDescription(null);
		descriptionProperty = resource.findProperty(UserType.F_DESCRIPTION);
		assertNull("Unexpected description property (after reset) "+descriptionProperty, descriptionProperty);

		// Extension
		ExtensionType extension = new ExtensionType();
		resourceType.setExtension(extension);

		resource.checkConsistence();

		PrismContainer<Containerable> extensionContainer = resource.findContainer(GenericObjectType.F_EXTENSION);
		checkExtension(extensionContainer,"resource extension after setExtension");
		checkExtension(extension,"resource extension after setExtension");

		// Schema
		XmlSchemaType xmlSchemaType = new XmlSchemaType();
		CachingMetadataType cachingMetadata = new CachingMetadataType();
		cachingMetadata.setSerialNumber("serial123");
		xmlSchemaType.setCachingMetadata(cachingMetadata);

		resourceType.setSchema(xmlSchemaType);

		SchemaDefinitionType schemaDefinition = new SchemaDefinitionType();
		Element xsdSchemaElement = DOMUtil.createElement(DOMUtil.XSD_SCHEMA_ELEMENT);
		schemaDefinition.getAny().add(xsdSchemaElement);
		xmlSchemaType.setDefinition(schemaDefinition);

		PrismContainer<Containerable> schemaContainer = resource.findContainer(ResourceType.F_SCHEMA);
		assertNotNull("No schema container", schemaContainer);

		// TODO

		// Schema: null
		resourceType.setSchema(null);
		schemaContainer = resource.findContainer(ResourceType.F_SCHEMA);
		assertNull("Unexpected schema container", schemaContainer);

	}

	private void checkExtension(PrismContainer<Containerable> extensionContainer, String sourceDescription) {
		assertNotNull("No extension container in "+sourceDescription+" (prism)", extensionContainer);
		assertNotNull("No extension definition in "+sourceDescription+" (prism)", extensionContainer.getDefinition());
		assertTrue("Not runtime in definition in "+sourceDescription+" (prism)", extensionContainer.getDefinition().isRuntimeSchema());
	}

	private void checkExtension(ExtensionType extension, String sourceDescription) throws SchemaException {
		PrismContainerValue<ExtensionType> extensionValueFromJaxb = extension.asPrismContainerValue();
		assertNotNull("No extension container in "+sourceDescription+" (jaxb)", extensionValueFromJaxb);
		assertNotNull("No extension definition in "+sourceDescription+" (jaxb)", extensionValueFromJaxb.getParent().getDefinition());
		assertTrue("Not runtime in definition in "+sourceDescription+" (jaxb)", extensionValueFromJaxb.getParent().getDefinition().isRuntimeSchema());

		PrismProperty<Integer> intProperty = extensionValueFromJaxb.findOrCreateProperty(EXTENSION_INT_TYPE_ELEMENT);
		PrismAsserts.assertDefinition(intProperty.getDefinition(), EXTENSION_INT_TYPE_ELEMENT, DOMUtil.XSD_INT, 0, -1);
		intProperty.setRealValue(15);

		PrismProperty<String> stringProperty = extensionValueFromJaxb.findOrCreateItem(EXTENSION_STRING_TYPE_ELEMENT, PrismProperty.class);
		PrismAsserts.assertDefinition(stringProperty.getDefinition(), EXTENSION_STRING_TYPE_ELEMENT, DOMUtil.XSD_STRING, 0, -1);
		stringProperty.setRealValue("fifteen men on a dead man chest");
	}

	private MapXNode createFilter(){

        MapXNode filter = new MapXNode();
        MapXNode equalsElement = new MapXNode();
		filter.put(new QName(SchemaConstantsGenerated.NS_QUERY, "equal"), equalsElement);

        PrimitiveXNode<ItemPathType> pathElement = new PrimitiveXNode<>(new ItemPathType(new ItemPath(new QName("name"))));
        equalsElement.put(new QName(SchemaConstantsGenerated.NS_QUERY, "path"), pathElement);

        PrimitiveXNode<String> valueElement = new PrimitiveXNode<>("čučoriedka");
        equalsElement.put(new QName(SchemaConstantsGenerated.NS_QUERY, "value"), valueElement);
		return filter;
	}

}
