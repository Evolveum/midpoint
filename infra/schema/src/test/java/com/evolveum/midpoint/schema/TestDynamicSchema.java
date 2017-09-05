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

import static com.evolveum.midpoint.schema.TestConstants.EXTENSION_STRING_TYPE_ELEMENT;
import static com.evolveum.midpoint.schema.TestConstants.USER_ASSIGNMENT_1_ID;
import static com.evolveum.midpoint.schema.TestConstants.USER_FILE;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.IOException;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
public class TestDynamicSchema {

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}


	@Test
	public void testAssignmentExtensionContainerProperty() throws Exception {
		System.out.println("===[ testAssignmentExtensionContainerProperty ]===");

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
		System.out.println("===[ testAssignmentExtensionContainerItem ]===");

		// GIVEN
		PrismContainer<AssignmentType> assignmentExtensionContainer = parseUserAssignmentContainer();

		// WHEN
		PrismProperty<String> assignmentExtensionStringProperty = assignmentExtensionContainer.findOrCreateItem(
				new ItemPath(EXTENSION_STRING_TYPE_ELEMENT), PrismProperty.class);

		// THEN
		assertNotNull("stringType is null", assignmentExtensionStringProperty);
		assertNotNull("stringType has no definition", assignmentExtensionStringProperty.getDefinition());
		PrismAsserts.assertDefinition(assignmentExtensionStringProperty.getDefinition(), EXTENSION_STRING_TYPE_ELEMENT, DOMUtil.XSD_STRING, 0, -1);
	}

	@Test
	public void testAssignmentExtensionValueProperty() throws Exception {
		System.out.println("===[ testAssignmentExtensionValueProperty ]===");

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
		System.out.println("===[ testAssignmentExtensionValueItem ]===");

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
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		PrismObject<UserType> user = prismContext.parseObject(USER_FILE);
		System.out.println("Parsed user:");
		System.out.println(user.debugDump());

		return user.findContainer(
				new ItemPath(
						new NameItemPathSegment(UserType.F_ASSIGNMENT),
						new IdItemPathSegment(USER_ASSIGNMENT_1_ID),
						new NameItemPathSegment(AssignmentType.F_EXTENSION)));
	}

}
