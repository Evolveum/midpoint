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
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.SchemaRegistry;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.delta.ObjectDelta;
import com.evolveum.midpoint.schema.delta.PropertyDelta;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.ChangeType;
import com.evolveum.midpoint.schema.processor.PrismObject;
import com.evolveum.midpoint.schema.processor.ObjectDefinition;
import com.evolveum.midpoint.schema.processor.PropertyPath;
import com.evolveum.midpoint.schema.processor.PropertyValue;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.util.DiffUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * @author semancik
 *
 */
public class TestParseDiffPatch {
	
	private static final String TEST_DIR = "src/test/resources/diff/";

	@Test
	public void testUser() throws SchemaException, SAXException, IOException, JAXBException {
		System.out.println("===[ testUser ]===");
		
		SchemaRegistry reg = new SchemaRegistry();
		reg.initialize();
		
		Schema objectSchema = reg.getObjectSchema();
        assertNotNull(objectSchema);

        // "Automatic" parsing
        PrismObject<UserType> userBefore = objectSchema.parseObject(new File(TEST_DIR, "user-jack-before.xml"), UserType.class);
        
        ObjectDefinition<UserType> userDefinition = objectSchema.findObjectDefinitionByType(SchemaConstants.I_USER_TYPE);
        assertNotNull("UserType definition not found in object schema", userDefinition);
        
        // "Manual" parsing
        JAXBElement<UserType> jaxbElement = JAXBUtil.unmarshal(new File(TEST_DIR, "user-jack-after.xml"), UserType.class);
        UserType userTypeAfter = jaxbElement.getValue();
        PrismObject<UserType> userAfter = userDefinition.parseObjectType(userTypeAfter);
        
        // sanity
        assertFalse("Equals does not work", userBefore.equals(userAfter));
        
        // WHEN
        
        ObjectDelta<UserType> userDelta = userBefore.compareTo(userAfter);
        
        // THEN
        
        System.out.println("DELTA:");
        System.out.println(userDelta.dump());
        
        assertEquals("Wrong delta OID", userBefore.getOid(), userDelta.getOid());
        assertEquals("Wrong change type", ChangeType.MODIFY, userDelta.getChangeType());
        Collection<PropertyDelta> modifications = userDelta.getModifications();
        assertEquals("Unexpected number of modifications", 3, modifications.size());
        assertReplace(userDelta, new QName(SchemaConstants.NS_C,"fullName"), "Cpt. Jack Sparrow");
        assertAdd(userDelta, new QName(SchemaConstants.NS_C,"honorificPrefix"), "Cpt.");
        assertAdd(userDelta, new QName(SchemaConstants.NS_C,"locality"), "Tortuga");
        
        ObjectModificationType objectModificationType = userDelta.toObjectModificationType();
        System.out.println("Modification XML:");
        System.out.println(JAXBUtil.marshalWrap(objectModificationType));
        assertEquals("Wrong delta OID", userBefore.getOid(), objectModificationType.getOid());
        List<PropertyModificationType> propertyModifications = objectModificationType.getPropertyModification();
        assertEquals("Unexpected number of modifications", 3, propertyModifications.size());
        assertXmlMod(objectModificationType, new QName(SchemaConstants.NS_C,"fullName"), PropertyModificationTypeType.replace, "Cpt. Jack Sparrow");
        assertXmlMod(objectModificationType, new QName(SchemaConstants.NS_C,"honorificPrefix"), PropertyModificationTypeType.add, "Cpt.");
        assertXmlMod(objectModificationType, new QName(SchemaConstants.NS_C,"locality"), PropertyModificationTypeType.add, "Tortuga");
        
        // ROUNDTRIP
        
        userDelta.applyTo(userBefore);
        
        //assertEquals("Round trip failed", userAfter, userBefore);
        
        assertTrue("Not equivalent",userBefore.equivalent(userAfter));
        
        ObjectDelta<UserType> roundTripDelta = DiffUtil.diff(userBefore, userAfter);
        System.out.println("roundtrip DELTA:");
        System.out.println(roundTripDelta.dump());
        
        assertTrue("Roundtrip delta is not empty",roundTripDelta.isEmpty());
	}
	
	@Test
	public void testUserReal() throws SchemaException, SAXException, IOException, JAXBException {
		System.out.println("===[ testUserReal ]===");
		
		SchemaRegistry reg = new SchemaRegistry();
		reg.initialize();
		
		Schema objectSchema = reg.getObjectSchema();
        assertNotNull(objectSchema);

        String userBeforeXml = MiscUtil.readFile(new File(TEST_DIR, "user-real-before.xml"));
        String userAfterXml = MiscUtil.readFile(new File(TEST_DIR, "user-real-after.xml"));
                
        // WHEN
        
        ObjectDelta<UserType> userDelta = DiffUtil.diff(userBeforeXml, userAfterXml, UserType.class, objectSchema);
        
        // THEN
        
        System.out.println("DELTA:");
        System.out.println(userDelta.dump());
        
        assertEquals("Wrong delta OID", "2f9b9299-6f45-498f-bc8e-8d17c6b93b20", userDelta.getOid());
        assertEquals("Wrong change type", ChangeType.MODIFY, userDelta.getChangeType());
        Collection<PropertyDelta> modifications = userDelta.getModifications();
        assertEquals("Unexpected number of modifications", 3, modifications.size());
        assertAdd(userDelta, new QName(SchemaConstants.NS_C,"eMailAddress"), "jack@blackpearl.com");
        assertReplace(userDelta, new QName(SchemaConstants.NS_C,"locality"), "World's End");
        assertReplace(userDelta, SchemaConstants.PATH_ACTIVATION_ENABLE, false);
	}
	
	@Test
	public void testAddDelta() throws SchemaException, SAXException, IOException {
		System.out.println("===[ testAddDelta ]===");
		
		// GIVEN
		SchemaRegistry reg = new SchemaRegistry();
		reg.initialize();
		
		Schema objectSchema = reg.getObjectSchema();
        assertNotNull(objectSchema);

        // WHEN
        ObjectDelta<UserType> userDelta = DiffUtil.diff(null,new File(TEST_DIR, "user-jack-after.xml"), UserType.class, objectSchema);

        //THEN
        System.out.println("DELTA:");
        System.out.println(userDelta.dump());
        
        assertEquals("Wrong delta OID", "deadbeef-c001-f00d-1111-222233330001", userDelta.getOid());
        assertEquals("Wrong change type", ChangeType.ADD, userDelta.getChangeType());
        
        // TODO
	}

	@Test
	public void testTask() throws SchemaException, SAXException, IOException, JAXBException {
		System.out.println("===[ testTask ]===");
		
		SchemaRegistry reg = new SchemaRegistry();
		reg.initialize();
		
		Schema objectSchema = reg.getObjectSchema();
        assertNotNull(objectSchema);

        // WHEN
        
        ObjectDelta<TaskType> diffDelta = DiffUtil.diff(new File(TEST_DIR, "task-before.xml"), 
        		new File(TEST_DIR, "task-after.xml"), TaskType.class, objectSchema);
        
        // THEN
        
        System.out.println("DELTA:");
        System.out.println(diffDelta.dump());
        
        assertEquals("Wrong delta OID", "91919191-76e0-59e2-86d6-3d4f02d3ffff", diffDelta.getOid());
        assertEquals("Wrong change type", ChangeType.MODIFY, diffDelta.getChangeType());
        Collection<PropertyDelta> modifications = diffDelta.getModifications();
        assertEquals("Unexpected number of modifications", 1, modifications.size());
        assertDelete(diffDelta, SchemaConstants.PATH_EXTENSION.subPath(new QName("http://midpoint.evolveum.com/xml/ns/public/provisioning/liveSync-1.xsd","token")), 480);
        
        // Convert to XML form. This should include xsi:type to pass the type information
        
        ObjectModificationType objectModificationType = diffDelta.toObjectModificationType();
        System.out.println("Modification XML:");
        System.out.println(JAXBUtil.marshalWrap(objectModificationType));
        
        // Check for xsi:type
        Element tokenElement = (Element) objectModificationType.getPropertyModification().get(0).getValue().getAny().get(0);
        assertTrue("No xsi:type in token",DOMUtil.hasXsiType(tokenElement));
        
        // parse back delta
        
        ObjectDelta<TaskType> patchDelta = ObjectDelta.createDelta(objectModificationType, objectSchema, TaskType.class);
        
        // ROUNDTRIP
        
        PrismObject<TaskType> taskPatch = objectSchema.parseObject(new File(TEST_DIR, "task-before.xml"), TaskType.class);
        
        // patch
        patchDelta.applyTo(taskPatch);
        
        System.out.println("Task after roundtrip patching");
        System.out.println(taskPatch.dump());
        
        PrismObject<TaskType> taskAfter = objectSchema.parseObject(new File(TEST_DIR, "task-after.xml"), TaskType.class);
                
        assertTrue("Not equivalent",taskPatch.equivalent(taskAfter));
        
        ObjectDelta<TaskType> roundTripDelta = DiffUtil.diff(taskPatch, taskAfter);
        System.out.println("roundtrip DELTA:");
        System.out.println(roundTripDelta.dump());
        
        assertTrue("Roundtrip delta is not empty",roundTripDelta.isEmpty());
        
	}

	
	private void assertReplace(ObjectDelta<?> userDelta, QName propertyName, Object... expectedValues) {
		PropertyDelta propertyDelta = userDelta.getPropertyDelta(propertyName);
		assertNotNull("Property delta for "+propertyName+" not found",propertyDelta);
		assertSet(propertyName, propertyDelta.getValuesToReplace(), expectedValues);
	}

	private void assertAdd(ObjectDelta<?> userDelta, QName propertyName, Object... expectedValues) {
		PropertyDelta propertyDelta = userDelta.getPropertyDelta(propertyName);
		assertNotNull("Property delta for "+propertyName+" not found",propertyDelta);
		assertSet(propertyName, propertyDelta.getValuesToAdd(), expectedValues);
	}
	
	private void assertDelete(ObjectDelta<?> userDelta, QName propertyName, Object... expectedValues) {
		PropertyDelta propertyDelta = userDelta.getPropertyDelta(propertyName);
		assertNotNull("Property delta for "+propertyName+" not found",propertyDelta);
		assertSet(propertyName, propertyDelta.getValuesToDelete(), expectedValues);
	}

	private void assertReplace(ObjectDelta<?> userDelta, PropertyPath propertyPath, Object... expectedValues) {
		PropertyDelta propertyDelta = userDelta.getPropertyDelta(propertyPath);
		assertNotNull("Property delta for "+propertyPath+" not found",propertyDelta);
		assertSet(propertyPath.last(), propertyDelta.getValuesToReplace(), expectedValues);
	}

	private void assertAdd(ObjectDelta<?> userDelta, PropertyPath propertyPath, Object... expectedValues) {
		PropertyDelta propertyDelta = userDelta.getPropertyDelta(propertyPath);
		assertNotNull("Property delta for "+propertyPath+" not found",propertyDelta);
		assertSet(propertyPath.last(), propertyDelta.getValuesToAdd(), expectedValues);
	}
	
	private void assertDelete(ObjectDelta<?> userDelta, PropertyPath propertyPath, Object... expectedValues) {
		PropertyDelta propertyDelta = userDelta.getPropertyDelta(propertyPath);
		assertNotNull("Property delta for "+propertyPath+" not found",propertyDelta);
		assertSet(propertyPath.last(), propertyDelta.getValuesToDelete(), expectedValues);
	}

	private void assertSet(QName propertyName, Collection<PropertyValue<Object>> valuesFromDelta, Object[] expectedValues) {
		assertEquals("Wrong number of values",expectedValues.length, valuesFromDelta.size());
		for (PropertyValue<Object> valueToReplace: valuesFromDelta) {
			boolean found = false;
			for (Object value: expectedValues) {
				if (value.equals(valueToReplace.getValue())) {
					found = true;
				}
			}
			if (!found) {
				AssertJUnit.fail("Unexpected value "+valueToReplace+" in delta for "+propertyName);
			}
		}
	}

	private void assertXmlMod(ObjectModificationType objectModificationType, QName propertyName,
			PropertyModificationTypeType modType, String... expectedValues) {
		for (PropertyModificationType mod: objectModificationType.getPropertyModification()) {
			List<Object> elements = mod.getValue().getAny();
			assertFalse(elements.isEmpty());
			Object first = elements.get(0);
			QName elementQName = JAXBUtil.getElementQName(first);
			if (propertyName.equals(elementQName)) {
				assertEquals(modType, mod.getModificationType());
				assertEquals(expectedValues.length, elements.size());
				for (Object element: elements) {
					boolean found = false;
					for (String expectedValue: expectedValues) {
						Element domElement = (Element)element;
						if (expectedValue.equals(domElement.getTextContent())) {
							found = true;
						}
					}
					assertTrue(found);
				}
			}
		}
	}


}
