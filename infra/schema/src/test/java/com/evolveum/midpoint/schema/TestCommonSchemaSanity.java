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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * @author semancik
 *
 */
public class TestCommonSchemaSanity {
	
	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		DebugUtil.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(new MidPointPrismContextFactory());
	}
	
	@Test
	public void testCommonSchema() {
		System.out.println("===[ testCommonSchema ]===");

		// WHEN
		// The context should have parsed common schema and also other midPoint schemas
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		
		// THEN
		// THEN
		assertNotNull("No prism context", prismContext);
		
		SchemaRegistry schemaRegistry = prismContext.getSchemaRegistry();
		assertNotNull("No schema registry in context", schemaRegistry);
		
		System.out.println("Schema registry:");
		System.out.println(schemaRegistry.dump());

		PrismSchema objectSchema = schemaRegistry.getObjectSchema();
		System.out.println("Object schema:");
		System.out.println(objectSchema.dump());
		
		// Assert USER definition
		PrismObjectDefinition<UserType> userDefinition = objectSchema.findObjectDefinitionByElementName(new QName(SchemaConstants.NS_COMMON,"user"));
		assertNotNull("No user definition", userDefinition);
		System.out.println("User definition:");
		System.out.println(userDefinition.dump());
		
		PrismObjectDefinition<UserType> userDefinitionByClass = schemaRegistry.findObjectDefinitionByCompileTimeClass(UserType.class);
		assertTrue("Different user def", userDefinition == userDefinitionByClass);

		assertEquals("Wrong compile-time class in user definition", UserType.class, userDefinition.getCompileTimeClass());
		PrismAsserts.assertPropertyDefinition(userDefinition, ObjectType.F_NAME, DOMUtil.XSD_STRING, 0, 1);
//		PrismAsserts.assertPropertyDefinition(userDefinition, USER_DESCRIPTION_QNAME, DOMUtil.XSD_STRING, 0, 1);
//		PrismAsserts.assertPropertyDefinition(userDefinition, USER_FULLNAME_QNAME, DOMUtil.XSD_STRING, 1, 1);
//		PrismAsserts.assertPropertyDefinition(userDefinition, USER_GIVENNAME_QNAME, DOMUtil.XSD_STRING, 1, 1);
//		PrismAsserts.assertPropertyDefinition(userDefinition, USER_FAMILYNAME_QNAME, DOMUtil.XSD_STRING, 1, 1);
//		PrismAsserts.assertPropertyDefinition(userDefinition, USER_ADDITIONALNAMES_QNAME, DOMUtil.XSD_STRING, 0, -1);
		assertFalse("User definition is marked as runtime", userDefinition.isRuntimeSchema());
		
//		PrismContainerDefinition extensionContainer = userDefinition.findContainerDefinition(USER_EXTENSION_QNAME);
//		assertDefinition(extensionContainer, USER_EXTENSION_QNAME, DOMUtil.XSD_ANY, 0, 1);
//		assertTrue("Extension is not runtime", extensionContainer.isRuntimeSchema());
//		assertTrue("Extension is not empty", extensionContainer.getDefinitions().isEmpty());
//		
//		PrismContainerDefinition activationContainer = userDefinition.findContainerDefinition(USER_ACTIVATION_QNAME);
//		assertDefinition(activationContainer, USER_ACTIVATION_QNAME, ACTIVATION_TYPE_QNAME, 0, 1);
//		assertFalse("Activation is runtime", activationContainer.isRuntimeSchema());
//		assertEquals("Activation size", 1, activationContainer.getDefinitions().size());
//		assertPropertyDefinition(activationContainer, USER_ENABLED_QNAME, DOMUtil.XSD_BOOLEAN, 1, 1);
//		
//		PrismContainerDefinition assignmentContainer = userDefinition.findContainerDefinition(USER_ASSIGNMENT_QNAME);
//		assertDefinition(assignmentContainer, USER_ASSIGNMENT_QNAME, ASSIGNMENT_TYPE_QNAME, 0, -1);
//		assertFalse("Assignment is runtime", assignmentContainer.isRuntimeSchema());
//		assertEquals("Assignment size", 1, assignmentContainer.getDefinitions().size());
//		assertPropertyDefinition(assignmentContainer, USER_DESCRIPTION_QNAME, DOMUtil.XSD_STRING, 0, 1);
//		
//		PrismReferenceDefinition accountRefDef = userDefinition.findItemDefinition(USER_ACCOUNTREF_QNAME, PrismReferenceDefinition.class);
//		assertDefinition(accountRefDef, USER_ACCOUNTREF_QNAME, OBJECT_REFERENCE_TYPE_QNAME, 0, -1);
//		assertEquals("Wrong target type in accountRef", ACCOUNT_TYPE_QNAME, accountRefDef.getTargetTypeName());
//		assertEquals("Wrong composite object element name in accountRef", USER_ACCOUNT_QNAME, accountRefDef.getCompositeObjectElementName());
//		
//		
//		
//		// Assert ACCOUNT definition
//		PrismObjectDefinition<AccountType> accountDefinition = objectSchema.findObjectDefinitionByElementName(new QName(NS_FOO,"account"));
//		assertNotNull("No user definition", accountDefinition);
//		System.out.println("User definition:");
//		System.out.println(accountDefinition.dump());
//		
//		PrismObjectDefinition<AccountType> accountDefinitionByClass = schemaRegistry.findObjectDefinitionByCompileTimeClass(AccountType.class);
//		assertTrue("Different user def", accountDefinition == accountDefinitionByClass);
//
//		assertEquals("Wrong compile-time class in account definition", AccountType.class, accountDefinition.getCompileTimeClass());
//		assertPropertyDefinition(accountDefinition, ACCOUNT_NAME_QNAME, DOMUtil.XSD_STRING, 0, 1);
//		assertPropertyDefinition(accountDefinition, ACCOUNT_DESCRIPTION_QNAME, DOMUtil.XSD_STRING, 0, 1);
//		assertFalse("Account definition is marked as runtime", accountDefinition.isRuntimeSchema());
//		
//		PrismContainerDefinition attributesContainer = accountDefinition.findContainerDefinition(ACCOUNT_ATTRIBUTES_QNAME);
//		assertDefinition(attributesContainer, ACCOUNT_ATTRIBUTES_QNAME, ATTRIBUTES_TYPE_QNAME, 0, 1);
//		assertTrue("Attributes is NOT runtime", attributesContainer.isRuntimeSchema());
		
	}

}
