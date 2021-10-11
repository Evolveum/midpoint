/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.assertNotNull;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;
import static com.evolveum.midpoint.schema.TestConstants.*;

import java.io.IOException;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 */
public class TestDynamicSchema extends AbstractSchemaTest {

    @Test
    public void testAssignmentExtensionContainerProperty() throws Exception {
        // GIVEN
        PrismContainer<AssignmentType> assignmentExtensionContainer = parseUserAssignmentContainer();

        // WHEN
        PrismProperty<String> assignmentExtensionStringProperty = assignmentExtensionContainer.findOrCreateProperty(EXTENSION_STRING_TYPE_ELEMENT);

        // THEN
        assertNotNull("stringType is null", assignmentExtensionStringProperty);
        assertNotNull("stringType has no definition", assignmentExtensionStringProperty.getDefinition());
        PrismAsserts.assertDefinition(assignmentExtensionStringProperty.getDefinition(), EXTENSION_STRING_TYPE_ELEMENT, DOMUtil.XSD_STRING, 0, -1);
    }

    @Test
    public void testAssignmentExtensionContainerItem() throws Exception {
        // GIVEN
        PrismContainer<AssignmentType> assignmentExtensionContainer = parseUserAssignmentContainer();

        // WHEN
        PrismProperty<String> assignmentExtensionStringProperty = assignmentExtensionContainer.findOrCreateItem(
                EXTENSION_STRING_TYPE_ELEMENT, PrismProperty.class);

        // THEN
        assertNotNull("stringType is null", assignmentExtensionStringProperty);
        assertNotNull("stringType has no definition", assignmentExtensionStringProperty.getDefinition());
        PrismAsserts.assertDefinition(assignmentExtensionStringProperty.getDefinition(), EXTENSION_STRING_TYPE_ELEMENT, DOMUtil.XSD_STRING, 0, -1);
    }

    @Test
    public void testAssignmentExtensionValueProperty() throws Exception {
        // GIVEN
        PrismContainer<AssignmentType> assignmentExtensionContainer = parseUserAssignmentContainer();
        PrismContainerValue<AssignmentType> assignmentExtensionContainerValue = assignmentExtensionContainer.getValue();

        // WHEN
        PrismProperty<String> assignmentExtensionStringProperty = assignmentExtensionContainerValue.findOrCreateProperty(EXTENSION_STRING_TYPE_ELEMENT);

        // THEN
        assertNotNull("stringType is null", assignmentExtensionStringProperty);
        assertNotNull("stringType has no definition", assignmentExtensionStringProperty.getDefinition());
        PrismAsserts.assertDefinition(assignmentExtensionStringProperty.getDefinition(), EXTENSION_STRING_TYPE_ELEMENT, DOMUtil.XSD_STRING, 0, -1);
    }

    @Test
    public void testAssignmentExtensionValueItem() throws Exception {
        // GIVEN
        PrismContainer<AssignmentType> assignmentExtensionContainer = parseUserAssignmentContainer();
        PrismContainerValue<AssignmentType> assignmentExtensionContainerValue = assignmentExtensionContainer.getValue();

        // WHEN
        PrismProperty<String> assignmentExtensionStringProperty = assignmentExtensionContainerValue.findOrCreateItem(
                EXTENSION_STRING_TYPE_ELEMENT, PrismProperty.class);

        // THEN
        assertNotNull("stringType is null", assignmentExtensionStringProperty);
        assertNotNull("stringType has no definition", assignmentExtensionStringProperty.getDefinition());
        PrismAsserts.assertDefinition(assignmentExtensionStringProperty.getDefinition(), EXTENSION_STRING_TYPE_ELEMENT, DOMUtil.XSD_STRING, 0, -1);
    }

    private PrismContainer<AssignmentType> parseUserAssignmentContainer() throws SchemaException, IOException {
        PrismContext prismContext = getPrismContext();
        PrismObject<UserType> user = prismContext.parseObject(USER_FILE);
        System.out.println("Parsed user:");
        System.out.println(user.debugDump());

        return user.findContainer(ItemPath.create(UserType.F_ASSIGNMENT, USER_ASSIGNMENT_1_ID, AssignmentType.F_EXTENSION));
    }
}
