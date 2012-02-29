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
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.PropertyPathSegment;
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
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ExtensibleObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ExtensionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * @author semancik
 *
 */
public class TestCommonSchemaSanity {
	
	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		DebugUtil.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}
	
	@Test
	public void testCommonSchema() {
		System.out.println("===[ testCommonSchema ]===");

		// WHEN
		// The context should have parsed common schema and also other midPoint schemas
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		
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
		PrismAsserts.assertPropertyDefinition(userDefinition, ObjectType.F_DESCRIPTION, DOMUtil.XSD_STRING, 0, 1);
		PrismAsserts.assertPropertyDefinition(userDefinition, UserType.F_FULL_NAME, DOMUtil.XSD_STRING, 1, 1);
		PrismAsserts.assertPropertyDefinition(userDefinition, UserType.F_GIVEN_NAME, DOMUtil.XSD_STRING, 1, 1);
		PrismAsserts.assertPropertyDefinition(userDefinition, UserType.F_FAMILY_NAME, DOMUtil.XSD_STRING, 1, 1);
		PrismAsserts.assertPropertyDefinition(userDefinition, UserType.F_ADDITIONAL_NAMES, DOMUtil.XSD_STRING, 0, -1);
		assertFalse("User definition is marked as runtime", userDefinition.isRuntimeSchema());
		
		PrismContainerDefinition extensionContainer = userDefinition.findContainerDefinition(ExtensibleObjectType.F_EXTENSION);
		PrismAsserts.assertDefinition(extensionContainer, ExtensibleObjectType.F_EXTENSION, ExtensionType.COMPLEX_TYPE, 0, 1);
		assertTrue("Extension is not runtime", extensionContainer.isRuntimeSchema());
		assertTrue("Extension is not empty", extensionContainer.getDefinitions().isEmpty());
		
		PrismContainerDefinition activationContainer = userDefinition.findContainerDefinition(UserType.F_ACTIVATION);
		PrismAsserts.assertDefinition(activationContainer, UserType.F_ACTIVATION, ActivationType.COMPLEX_TYPE, 0, 1);
		assertFalse("Activation is runtime", activationContainer.isRuntimeSchema());
		assertEquals("Activation size", 3, activationContainer.getDefinitions().size());
		PrismAsserts.assertPropertyDefinition(activationContainer, ActivationType.F_ENABLED, DOMUtil.XSD_BOOLEAN, 0, 1);
		
		PrismContainerDefinition assignmentContainer = userDefinition.findContainerDefinition(UserType.F_ASSIGNMENT);
		PrismAsserts.assertDefinition(assignmentContainer, UserType.F_ASSIGNMENT, AssignmentType.COMPLEX_TYPE, 0, -1);
		assertFalse("Assignment is runtime", assignmentContainer.isRuntimeSchema());
		assertEquals("Assignment size", 4, assignmentContainer.getDefinitions().size());
		PrismAsserts.assertPropertyDefinition(assignmentContainer, AssignmentType.F_ACCOUNT_CONSTRUCTION, AccountConstructionType.COMPLEX_TYPE, 0, 1);
		
		PrismReferenceDefinition accountRefDef = userDefinition.findItemDefinition(UserType.F_ACCOUNT_REF, PrismReferenceDefinition.class);
		PrismAsserts.assertDefinition(accountRefDef, UserType.F_ACCOUNT_REF, ObjectReferenceType.COMPLEX_TYPE, 0, -1);
		assertEquals("Wrong target type in accountRef", AccountShadowType.COMPLEX_TYPE, accountRefDef.getTargetTypeName());
		assertEquals("Wrong composite object element name in accountRef", UserType.F_ACCOUNT, accountRefDef.getCompositeObjectElementName());
		
		
		
		// Assert ACCOUNT definition
		PrismObjectDefinition<AccountShadowType> accountDefinition = objectSchema.findObjectDefinitionByElementName(
				new QName(SchemaConstants.NS_COMMON,"account"));
		assertNotNull("No user definition", accountDefinition);
		System.out.println("User definition:");
		System.out.println(accountDefinition.dump());
		
		PrismObjectDefinition<AccountShadowType> accountDefinitionByClass = 
			schemaRegistry.findObjectDefinitionByCompileTimeClass(AccountShadowType.class);
		assertTrue("Different user def", accountDefinition == accountDefinitionByClass);

		assertEquals("Wrong compile-time class in account definition", AccountShadowType.class, accountDefinition.getCompileTimeClass());
		PrismAsserts.assertPropertyDefinition(accountDefinition, AccountShadowType.F_NAME, DOMUtil.XSD_STRING, 0, 1);
		PrismAsserts.assertPropertyDefinition(accountDefinition, AccountShadowType.F_DESCRIPTION, DOMUtil.XSD_STRING, 0, 1);
		assertFalse("Account definition is marked as runtime", accountDefinition.isRuntimeSchema());
		
		PrismContainerDefinition attributesContainer = accountDefinition.findContainerDefinition(AccountShadowType.F_ATTRIBUTES);
		PrismAsserts.assertDefinition(attributesContainer, AccountShadowType.F_ATTRIBUTES, ResourceObjectShadowAttributesType.COMPLEX_TYPE, 0, 1);
		assertTrue("Attributes is NOT runtime", attributesContainer.isRuntimeSchema());
		
	}
		
	public static void assertPropertyValue(PrismContainer<?> container, String propName, Object propValue) {
		QName propQName = new QName(SchemaConstants.NS_COMMON, propName);
		PrismAsserts.assertPropertyValue(container, propQName, propValue);
	}

}
