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
package com.evolveum.midpoint.prism;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.*;

import static org.testng.AssertJUnit.assertNotNull;

import java.io.IOException;

import javax.xml.namespace.QName;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.foo.ObjectFactory;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class TestPrismContext {
	
	private static final String NS_FOO = "http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd";
	
	@BeforeSuite
	public void setupDebug() {
		DebugUtil.setDefaultNamespacePrefix("http://midpoint.evolveum.com/xml/ns");
	}

	@Test
	public void testPrismContextConstruction() throws SchemaException, SAXException, IOException {
		System.out.println("===[ testPrismContextConstruction ]===");
		
		// WHEN
		PrismContext prismContext = constructInitializedPrismContext();
		
		// THEN
		assertNotNull("No prism context", prismContext);
		
		SchemaRegistry schemaRegistry = prismContext.getSchemaRegistry();
		assertNotNull("No schema registry in context", schemaRegistry);
		
		System.out.println("Schema registry:");
		System.out.println(schemaRegistry.dump());

		PrismSchema objectSchema = schemaRegistry.getObjectSchema();
		System.out.println("Object schema:");
		System.out.println(objectSchema.dump());
		
		PrismObjectDefinition<UserType> userDefinition = objectSchema.findObjectDefinitionByElementName(new QName(NS_FOO,"user"));
		assertNotNull("No user definition", userDefinition);
		System.out.println("User definition:");
		System.out.println(userDefinition.dump());
		
		PrismObjectDefinition<UserType> userDefinitionByClass = schemaRegistry.findObjectDefinitionByCompileTimeClass(UserType.class);
		assertTrue("Different user def", userDefinition == userDefinitionByClass);

		assertEquals("Wrong compile-time class in user definition", UserType.class, userDefinition.getCompileTimeClass());
		assertPropertyDefinition(userDefinition, USER_NAME_QNAME, DOMUtil.XSD_STRING, 0, 1);
		assertPropertyDefinition(userDefinition, USER_DESCRIPTION_QNAME, DOMUtil.XSD_STRING, 0, 1);
		assertPropertyDefinition(userDefinition, USER_FULLNAME_QNAME, DOMUtil.XSD_STRING, 1, 1);
		assertPropertyDefinition(userDefinition, USER_GIVENNAME_QNAME, DOMUtil.XSD_STRING, 1, 1);
		assertPropertyDefinition(userDefinition, USER_FAMILYNAME_QNAME, DOMUtil.XSD_STRING, 1, 1);
		assertPropertyDefinition(userDefinition, USER_ADDITIONALNAMES_QNAME, DOMUtil.XSD_STRING, 0, -1);
		
		PrismContainerDefinition extensionContainer = userDefinition.findContainerDefinition(USER_EXTENSION_QNAME);
		assertDefinition(extensionContainer, USER_EXTENSION_QNAME, DOMUtil.XSD_ANY, 0, 1);
		assertTrue("Extension is not runtime", extensionContainer.isRuntimeSchema());
		assertTrue("Extension is not empty", extensionContainer.getDefinitions().isEmpty());
		
		PrismContainerDefinition activationContainer = userDefinition.findContainerDefinition(USER_ACTIVATION_QNAME);
		assertDefinition(activationContainer, USER_ACTIVATION_QNAME, ACTIVATION_TYPE_QNAME, 0, 1);
		assertFalse("Activation is runtime", activationContainer.isRuntimeSchema());
		assertEquals("Activation size", 1, activationContainer.getDefinitions().size());
		assertPropertyDefinition(activationContainer, USER_ENABLED_QNAME, DOMUtil.XSD_BOOLEAN, 1, 1);
		
		PrismContainerDefinition assignmentContainer = userDefinition.findContainerDefinition(USER_ASSIGNMENT_QNAME);
		assertDefinition(assignmentContainer, USER_ASSIGNMENT_QNAME, ASSIGNMENT_TYPE_QNAME, 0, -1);
		assertFalse("Assignment is runtime", assignmentContainer.isRuntimeSchema());
		assertEquals("Assignment size", 1, assignmentContainer.getDefinitions().size());
		assertPropertyDefinition(assignmentContainer, USER_DESCRIPTION_QNAME, DOMUtil.XSD_STRING, 0, 1);
		
		PrismReferenceDefinition accountRefDef = userDefinition.findItemDefinition(USER_ACCOUNTREF_QNAME, PrismReferenceDefinition.class);
		assertDefinition(accountRefDef, USER_ACCOUNTREF_QNAME, OBJECT_REFERENCE_TYPE_QNAME, 0, -1);
		assertEquals("Wrong target type in accountRef", ACCOUNT_TYPE_QNAME, accountRefDef.getTargetTypeName());
		assertEquals("Wrong composite object element name in accountRef", USER_ACCOUNT_QNAME, accountRefDef.getCompositeObjectElementName());
	}

}
