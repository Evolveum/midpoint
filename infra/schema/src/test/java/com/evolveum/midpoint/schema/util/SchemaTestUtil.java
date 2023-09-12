/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.util;

import static com.evolveum.midpoint.schema.TestConstants.USER_EXTENSION_TYPE_NAME;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import javax.xml.namespace.QName;

/**
 * @author semancik
 *
 */
public class SchemaTestUtil {

    public static void assertUserDefinition(PrismObjectDefinition<UserType> userDefinition) {
        assertEquals("Wrong compile-time class in user definition", UserType.class, userDefinition.getCompileTimeClass());

        assertFocusDefinition(userDefinition.getComplexTypeDefinition(), "user", USER_EXTENSION_TYPE_NAME, 1);

        PrismAsserts.assertPropertyDefinition(userDefinition, UserType.F_FULL_NAME, SchemaConstants.T_POLY_STRING_TYPE, 0, 1);
        PrismAsserts.assertItemDefinitionDisplayName(userDefinition, UserType.F_FULL_NAME, "UserType.fullName");
        PrismAsserts.assertItemDefinitionDisplayOrder(userDefinition, UserType.F_FULL_NAME, 100);
        PrismAsserts.assertPropertyDefinition(userDefinition, UserType.F_GIVEN_NAME, PrismConstants.POLYSTRING_TYPE_QNAME, 0, 1);
        PrismAsserts.assertPropertyDefinition(userDefinition, UserType.F_FAMILY_NAME, PrismConstants.POLYSTRING_TYPE_QNAME, 0, 1);
        PrismAsserts.assertPropertyDefinition(userDefinition, UserType.F_ADDITIONAL_NAME, PrismConstants.POLYSTRING_TYPE_QNAME, 0, 1);
    }

    public static void assertFocusDefinition(ComplexTypeDefinition complexTypeDefinition, String defDesc, QName expectedExtensionTypeName, int expectedExtensionItemDefs) {
        assertNotNull("No "+defDesc+" definition", complexTypeDefinition);

        PrismAsserts.assertPropertyDefinition(complexTypeDefinition, ObjectType.F_NAME, PolyStringType.COMPLEX_TYPE, 0, 1);
        PrismAsserts.assertItemDefinitionDisplayName(complexTypeDefinition, ObjectType.F_NAME, "ObjectType.name");
        PrismAsserts.assertItemDefinitionDisplayOrder(complexTypeDefinition, ObjectType.F_NAME, 0);
        PrismAsserts.assertPropertyDefinition(complexTypeDefinition, ObjectType.F_DESCRIPTION, DOMUtil.XSD_STRING, 0, 1);
        PrismAsserts.assertItemDefinitionDisplayName(complexTypeDefinition, ObjectType.F_DESCRIPTION, "ObjectType.description");
        PrismAsserts.assertItemDefinitionDisplayOrder(complexTypeDefinition, ObjectType.F_DESCRIPTION, 10);
        assertFalse(""+defDesc+" definition is marked as runtime", complexTypeDefinition.isRuntimeSchema());

        PrismContainerDefinition extensionContainer = complexTypeDefinition.findContainerDefinition(UserType.F_EXTENSION);
        PrismAsserts.assertDefinition(extensionContainer, UserType.F_EXTENSION, expectedExtensionTypeName, 0, 1);
        assertTrue("Extension is NOT runtime", extensionContainer.isRuntimeSchema());
        //assertTrue("Extension is NOT dynamic", extensionContainer.isDynamic());
        assertEquals("Extension size", expectedExtensionItemDefs, extensionContainer.getDefinitions().size());
        PrismAsserts.assertItemDefinitionDisplayName(complexTypeDefinition, UserType.F_EXTENSION, "ObjectType.extension");
        PrismAsserts.assertItemDefinitionDisplayOrder(complexTypeDefinition, UserType.F_EXTENSION, 1000);

        PrismContainerDefinition<ActivationType> activationContainer = complexTypeDefinition.findContainerDefinition(UserType.F_ACTIVATION);
        PrismAsserts.assertDefinition(activationContainer, UserType.F_ACTIVATION, ActivationType.COMPLEX_TYPE, 0, 1);
        assertFalse("Activation is runtime", activationContainer.isRuntimeSchema());
        assertEquals("Activation size", 12, activationContainer.getDefinitions().size());
        PrismAsserts.assertPropertyDefinition(activationContainer, ActivationType.F_ADMINISTRATIVE_STATUS, SchemaConstants.C_ACTIVATION_STATUS_TYPE, 0, 1);

        PrismContainerDefinition<AssignmentType> assignmentContainer = complexTypeDefinition.findContainerDefinition(UserType.F_ASSIGNMENT);
        PrismAsserts.assertDefinition(assignmentContainer, UserType.F_ASSIGNMENT, AssignmentType.COMPLEX_TYPE, 0, -1);
        assertFalse("Assignment is runtime", assignmentContainer.isRuntimeSchema());
        assertEquals("Assignment definition size", 25, assignmentContainer.getDefinitions().size());

        PrismContainerDefinition<ConstructionType> constructionContainer = assignmentContainer.findContainerDefinition(AssignmentType.F_CONSTRUCTION);
        PrismAsserts.assertDefinition(constructionContainer, AssignmentType.F_CONSTRUCTION, ConstructionType.COMPLEX_TYPE, 0, 1);
        assertFalse("Construction is runtime", constructionContainer.isRuntimeSchema());

        PrismReferenceDefinition accountRefDef = complexTypeDefinition.findItemDefinition(UserType.F_LINK_REF, PrismReferenceDefinition.class);
        PrismAsserts.assertDefinition(accountRefDef, UserType.F_LINK_REF, ObjectReferenceType.COMPLEX_TYPE, 0, -1);
        assertEquals("Wrong target type in accountRef", ShadowType.COMPLEX_TYPE, accountRefDef.getTargetTypeName());

        PrismContainerDefinition<MetadataType> metadataContainer = complexTypeDefinition.findContainerDefinition(UserType.F_METADATA);
        assertFalse("Metadata is runtime", metadataContainer.isRuntimeSchema());
        assertFalse("Metadata is dynamic", metadataContainer.isDynamic());
        assertTrue("Metadata is NOT operational", metadataContainer.isOperational());
        assertEquals("Metadata size", 23, metadataContainer.getDefinitions().size());

        PrismReferenceDefinition tenantRefDef = complexTypeDefinition.findItemDefinition(UserType.F_TENANT_REF, PrismReferenceDefinition.class);
        PrismAsserts.assertDefinition(tenantRefDef, UserType.F_TENANT_REF, ObjectReferenceType.COMPLEX_TYPE, 0, 1);
        assertEquals("Wrong target type in tenantRef", ShadowType.COMPLEX_TYPE, accountRefDef.getTargetTypeName());
    }
}
