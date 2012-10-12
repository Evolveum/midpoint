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

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import static com.evolveum.midpoint.schema.TestConstants.*;

import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBException;

import com.evolveum.midpoint.prism.*;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.schema.util.SchemaTestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ExtensionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectReferenceType.Filter;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;

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
		Element filterElement = DOMUtil.getDocument().createElementNS(NS_EXTENSION, "fooFilter");
		ObjectReferenceType.Filter filter = new ObjectReferenceType.Filter();
		filter.setFilter(filterElement);
		accountRefType.setFilter(filter);
		userType.getAccountRef().add(accountRefType);
		
		assertAccountRefs(userType, USER_ACCOUNT_REF_1_OID);
		user.checkConsistence();
		user.assertDefinitions();

		PrismReference accountRef = user.findReference(UserType.F_ACCOUNT_REF);
        assertEquals("1/ Wrong accountRef values", 1, accountRef.getValues().size());
        PrismReferenceValue accountRefVal0 = accountRef.getValue(0);
        Element prismFilterElement = accountRefVal0.getFilter();
        assertNotNull("Filter have not passed", prismFilterElement);
        assertEquals("Difference filter", filterElement, prismFilterElement);

        AccountShadowType accountShadowType = new AccountShadowType();
        accountShadowType.setOid(USER_ACCOUNT_REF_1_OID);
        userType.getAccount().add(accountShadowType);
        //value still should be only one... (reference was only resolved)
        assertEquals("2/ Wrong accountRef values", 1, user.findReference(UserType.F_ACCOUNT_REF).getValues().size());

		accountShadowType = new AccountShadowType();
		accountShadowType.setOid(USER_ACCOUNT_REF_2_OID);
		userType.getAccount().add(accountShadowType);

        assertEquals("3/ Wrong accountRef values", 2, user.findReference(UserType.F_ACCOUNT_REF).getValues().size());
		
		assertAccountRefs(userType, USER_ACCOUNT_REF_1_OID, USER_ACCOUNT_REF_2_OID);
		user.checkConsistence();
		user.assertDefinitions();
		
		assertEquals("4/ Wrong accountRef values", 2, accountRef.getValues().size());
		PrismAsserts.assertReferenceValues(accountRef, USER_ACCOUNT_REF_1_OID, USER_ACCOUNT_REF_2_OID);
		
	}

	private void assertAccountRefs(UserType userType, String... accountOids) {
		List<ObjectReferenceType> accountRefs = userType.getAccountRef();
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
		
		UserType userType = new UserType();
		prismContext.adopt(userType);
		
		PrismObject<UserType> user = userType.asPrismObject();
		assertNotNull("No object definition after adopt", user.getDefinition());
		
		// Extension
		ExtensionType extension = new ExtensionType();
		userType.setExtension(extension);
		
		user.checkConsistence();
				
		PrismContainer<Containerable> extensionContainer = user.findContainer(GenericObjectType.F_EXTENSION);
		checkExtension(extensionContainer,"user extension after setExtension");
		checkExtension(extension,"user extension after setExtension");
		
		AssignmentType assignmentType = new AssignmentType();
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
		
		AccountShadowType accountType = new AccountShadowType();
		prismContext.adopt(accountType);
		
		PrismObject<AccountShadowType> account = accountType.asPrismObject();
		assertNotNull("No object definition after adopt", account.getDefinition());
		
		// WHEN
		accountType.setName(PrismTestUtil.createPolyStringType(ACCOUNT_NAME));
		ObjectReferenceType resourceRefType = new ObjectReferenceType();
		resourceRefType.setOid(FAUX_RESOURCE_OID);
		resourceRefType.setType(ResourceType.COMPLEX_TYPE);
		accountType.setResourceRef(resourceRefType);
		
		// THEN (prism)
		account.checkConsistence();
		PrismAsserts.assertPropertyValue(account, AccountShadowType.F_NAME, PrismTestUtil.createPolyString(ACCOUNT_NAME));
		PrismReference resourceRef = account.findReference(AccountShadowType.F_RESOURCE_REF);
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

}
