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
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

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
	public void testAssignmentExtensionContainerProperty() throws SchemaException {
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
	public void testAssignmentExtensionContainerItem() throws SchemaException {
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
	public void testAssignmentExtensionValueProperty() throws SchemaException {
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
	public void testAssignmentExtensionValueItem() throws SchemaException {
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
	
	
	
	
	private PrismContainer<AssignmentType> parseUserAssignmentContainer() throws SchemaException {
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		PrismObject<UserType> user = prismContext.parseObject(USER_FILE);
		System.out.println("Parsed user:");
		System.out.println(user.dump());
		
		return user.findContainer(
				new ItemPath(
						new NameItemPathSegment(UserType.F_ASSIGNMENT),
						new IdItemPathSegment(USER_ASSIGNMENT_1_ID),
						new NameItemPathSegment(AssignmentType.F_EXTENSION)));
	}

}
