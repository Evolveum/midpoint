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
package com.evolveum.midpoint.schema.util;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExtensionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

/**
 * @author semancik
 *
 */
public class SchemaTestUtil {
	
	public static void assertUserDefinition(PrismObjectDefinition<UserType> userDefinition) {
		assertNotNull("No user definition", userDefinition);
		assertEquals("Wrong compile-time class in user definition", UserType.class, userDefinition.getCompileTimeClass());
		PrismAsserts.assertPropertyDefinition(userDefinition, ObjectType.F_NAME, PolyStringType.COMPLEX_TYPE, 0, 1);
		PrismAsserts.assertItemDefinitionDisplayName(userDefinition, ObjectType.F_NAME, "Name");
		PrismAsserts.assertItemDefinitionDisplayOrder(userDefinition, ObjectType.F_NAME, 0);
		PrismAsserts.assertPropertyDefinition(userDefinition, ObjectType.F_DESCRIPTION, DOMUtil.XSD_STRING, 0, 1);
		PrismAsserts.assertItemDefinitionDisplayName(userDefinition, ObjectType.F_DESCRIPTION, "Description");
		PrismAsserts.assertItemDefinitionDisplayOrder(userDefinition, ObjectType.F_DESCRIPTION, 10);
		PrismAsserts.assertPropertyDefinition(userDefinition, UserType.F_FULL_NAME, SchemaConstants.T_POLY_STRING_TYPE, 1, 1);
		PrismAsserts.assertItemDefinitionDisplayName(userDefinition, UserType.F_FULL_NAME, "Full Name");
		PrismAsserts.assertItemDefinitionDisplayOrder(userDefinition, UserType.F_FULL_NAME, 100);
		PrismAsserts.assertPropertyDefinition(userDefinition, UserType.F_GIVEN_NAME, PrismConstants.POLYSTRING_TYPE_QNAME, 0, 1);
		PrismAsserts.assertPropertyDefinition(userDefinition, UserType.F_FAMILY_NAME, PrismConstants.POLYSTRING_TYPE_QNAME, 0, 1);
		PrismAsserts.assertPropertyDefinition(userDefinition, UserType.F_ADDITIONAL_NAME, PrismConstants.POLYSTRING_TYPE_QNAME, 0, 1);
		assertFalse("User definition is marked as runtime", userDefinition.isRuntimeSchema());
		
		PrismContainerDefinition extensionContainer = userDefinition.findContainerDefinition(UserType.F_EXTENSION);
		PrismAsserts.assertDefinition(extensionContainer, UserType.F_EXTENSION, ExtensionType.COMPLEX_TYPE, 0, 1);
		assertTrue("Extension is NOT runtime", extensionContainer.isRuntimeSchema());
		assertTrue("Extension is NOT dynamic", extensionContainer.isDynamic());
		assertEquals("Extension size", 0, extensionContainer.getDefinitions().size());
		PrismAsserts.assertItemDefinitionDisplayName(userDefinition, UserType.F_EXTENSION, "Extension");
		PrismAsserts.assertItemDefinitionDisplayOrder(userDefinition, UserType.F_EXTENSION, 1000);

		PrismContainerDefinition activationContainer = userDefinition.findContainerDefinition(UserType.F_ACTIVATION);
		PrismAsserts.assertDefinition(activationContainer, UserType.F_ACTIVATION, ActivationType.COMPLEX_TYPE, 0, 1);
		assertFalse("Activation is runtime", activationContainer.isRuntimeSchema());
		assertEquals("Activation size", 3, activationContainer.getDefinitions().size());
		PrismAsserts.assertPropertyDefinition(activationContainer, ActivationType.F_ADMINISTRATIVE_STATUS, SchemaConstants.C_ACTIVATION_STATUS_TYPE, 0, 1);
		
		PrismContainerDefinition assignmentContainer = userDefinition.findContainerDefinition(UserType.F_ASSIGNMENT);
		PrismAsserts.assertDefinition(assignmentContainer, UserType.F_ASSIGNMENT, AssignmentType.COMPLEX_TYPE, 0, -1);
		assertFalse("Assignment is runtime", assignmentContainer.isRuntimeSchema());
		assertEquals("Assignment size", 7, assignmentContainer.getDefinitions().size());
		PrismAsserts.assertPropertyDefinition(assignmentContainer, AssignmentType.F_ACCOUNT_CONSTRUCTION, ConstructionType.COMPLEX_TYPE, 0, 1);
		
		PrismReferenceDefinition accountRefDef = userDefinition.findItemDefinition(UserType.F_LINK_REF, PrismReferenceDefinition.class);
		PrismAsserts.assertDefinition(accountRefDef, UserType.F_LINK_REF, ObjectReferenceType.COMPLEX_TYPE, 0, -1);
		assertEquals("Wrong target type in accountRef", ShadowType.COMPLEX_TYPE, accountRefDef.getTargetTypeName());
		assertEquals("Wrong composite object element name in accountRef", UserType.F_LINK, accountRefDef.getCompositeObjectElementName());
	}
}
