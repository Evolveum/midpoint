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
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * @author semancik
 *
 */
public class TestParseDiffPatch {
	
	private static final String TEST_DIR = "src/test/resources/diff/";
	
	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		DebugUtil.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

	@Test
	public void testUser() throws SchemaException, SAXException, IOException, JAXBException {
		System.out.println("===[ testUser ]===");

        PrismObject<UserType> userBefore = PrismTestUtil.parseObject(new File(TEST_DIR, "user-jack-before.xml"));                
        PrismObject<UserType> userAfter = PrismTestUtil.parseObject(new File(TEST_DIR, "user-jack-after.xml"));
        
        // sanity
        assertFalse("Equals does not work", userBefore.equals(userAfter));
        
        // WHEN
        
        ObjectDelta<UserType> userDelta = userBefore.diff(userAfter);
        
        // THEN
        
        System.out.println("DELTA:");
        System.out.println(userDelta.dump());
        
        assertEquals("Wrong delta OID", userBefore.getOid(), userDelta.getOid());
        assertEquals("Wrong change type", ChangeType.MODIFY, userDelta.getChangeType());
        Collection<? extends ItemDelta> modifications = userDelta.getModifications();
        assertEquals("Unexpected number of modifications", 3, modifications.size());
        PrismAsserts.assertPropertyReplace(userDelta, new QName(SchemaConstants.NS_C,"fullName"), "Cpt. Jack Sparrow");
        PrismAsserts.assertPropertyAdd(userDelta, new QName(SchemaConstants.NS_C,"honorificPrefix"), "Cpt.");
        PrismAsserts.assertPropertyAdd(userDelta, new QName(SchemaConstants.NS_C,"locality"), "Tortuga");
        
        ObjectModificationType objectModificationType = DeltaConvertor.toObjectModificationType(userDelta);
        System.out.println("Modification XML:");
        System.out.println(PrismTestUtil.marshalWrap(objectModificationType));
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
		
        String userBeforeXml = MiscUtil.readFile(new File(TEST_DIR, "user-real-before.xml"));
        String userAfterXml = MiscUtil.readFile(new File(TEST_DIR, "user-real-after.xml"));
                
        // WHEN
        
        ObjectDelta<UserType> userDelta = DiffUtil.diff(userBeforeXml, userAfterXml, UserType.class, PrismTestUtil.getPrismContext());
        
        // THEN
        
        System.out.println("DELTA:");
        System.out.println(userDelta.dump());
        
        assertEquals("Wrong delta OID", "2f9b9299-6f45-498f-bc8e-8d17c6b93b20", userDelta.getOid());
        assertEquals("Wrong change type", ChangeType.MODIFY, userDelta.getChangeType());
        Collection<? extends ItemDelta> modifications = userDelta.getModifications();
        assertEquals("Unexpected number of modifications", 3, modifications.size());
        PrismAsserts.assertPropertyAdd(userDelta, new QName(SchemaConstants.NS_C,"emailAddress"), "jack@blackpearl.com");
        PrismAsserts.assertPropertyReplace(userDelta, new QName(SchemaConstants.NS_C,"locality"), "World's End");
        PrismAsserts.assertPropertyReplace(userDelta, SchemaConstants.PATH_ACTIVATION_ENABLE, false);
	}
	
	@Test
	public void testAddDelta() throws SchemaException, SAXException, IOException {
		System.out.println("===[ testAddDelta ]===");
		
        // WHEN
        ObjectDelta<UserType> userDelta = DiffUtil.diff(null,new File(TEST_DIR, "user-jack-after.xml"), UserType.class, PrismTestUtil.getPrismContext());

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
		
        // WHEN
        
        ObjectDelta<TaskType> diffDelta = DiffUtil.diff(new File(TEST_DIR, "task-before.xml"), 
        		new File(TEST_DIR, "task-after.xml"), TaskType.class, PrismTestUtil.getPrismContext());
        
        // THEN
        
        System.out.println("DELTA:");
        System.out.println(diffDelta.dump());
        
        assertEquals("Wrong delta OID", "91919191-76e0-59e2-86d6-3d4f02d3ffff", diffDelta.getOid());
        assertEquals("Wrong change type", ChangeType.MODIFY, diffDelta.getChangeType());
        Collection<? extends ItemDelta> modifications = diffDelta.getModifications();
        assertEquals("Unexpected number of modifications", 1, modifications.size());
        PrismAsserts.assertPropertyDelete(diffDelta, SchemaConstants.PATH_EXTENSION.subPath(
        		new QName("http://midpoint.evolveum.com/xml/ns/public/provisioning/liveSync-1.xsd","token")), 480);
        
        // Convert to XML form. This should include xsi:type to pass the type information
        
        ObjectModificationType objectModificationType =  DeltaConvertor.toObjectModificationType(diffDelta);
        System.out.println("Modification XML:");
        System.out.println(PrismTestUtil.marshalWrap(objectModificationType));
        
        // Check for xsi:type
        Element tokenElement = (Element) objectModificationType.getPropertyModification().get(0).getValue().getAny().get(0);
        assertTrue("No xsi:type in token",DOMUtil.hasXsiType(tokenElement));
        
        // parse back delta
        ObjectDelta<TaskType> patchDelta = DeltaConvertor.createObjectDelta(objectModificationType, 
        		TaskType.class, PrismTestUtil.getPrismContext());
        
        // ROUNDTRIP
        
        PrismObject<TaskType> taskPatch = PrismTestUtil.parseObject(new File(TEST_DIR, "task-before.xml"));
        
        // patch
        patchDelta.applyTo(taskPatch);
        
        System.out.println("Task after roundtrip patching");
        System.out.println(taskPatch.dump());
        
        PrismObject<TaskType> taskAfter = PrismTestUtil.parseObject(new File(TEST_DIR, "task-after.xml"));
                
        assertTrue("Not equivalent",taskPatch.equivalent(taskAfter));
        
        ObjectDelta<TaskType> roundTripDelta = DiffUtil.diff(taskPatch, taskAfter);
        System.out.println("roundtrip DELTA:");
        System.out.println(roundTripDelta.dump());
        
        assertTrue("Roundtrip delta is not empty",roundTripDelta.isEmpty());
        
	}
	
	@Test
	public void testResourceRoundTrip() throws SchemaException, SAXException, IOException, JAXBException {
		System.out.println("===[ testResourceRoundTrip ]===");

        PrismObject<ResourceType> resourceBefore = PrismTestUtil.parseObject(new File(TEST_DIR, "resource-before.xml"));                
        PrismObject<ResourceType> resourceAfter = PrismTestUtil.parseObject(new File(TEST_DIR, "resource-after.xml"));
        
        // sanity
        assertFalse("Equals does not work", resourceBefore.equals(resourceAfter));
        
        // WHEN
        
        ObjectDelta<ResourceType> resourceDelta = resourceBefore.diff(resourceAfter);
        
        // THEN
        
        System.out.println("DELTA:");
        System.out.println(resourceDelta.dump());
        
        // ROUNDTRIP
        resourceDelta.applyTo(resourceBefore);
        
        System.out.println("Resource after roundtrip:");
        System.out.println(resourceBefore.dump());
        
        ObjectDelta<ResourceType> roundTripDelta1 = resourceBefore.diff(resourceAfter);
        System.out.println("roundtrip DELTA 1:");
        System.out.println(roundTripDelta1.dump());        
        assertTrue("Resource roundtrip 1 failed", roundTripDelta1.isEmpty());

        ObjectDelta<ResourceType> roundTripDelta2 = resourceAfter.diff(resourceBefore);
        System.out.println("roundtrip DELTA 2:");
        System.out.println(roundTripDelta2.dump());        
        assertTrue("Resource roundtrip 2 failed", roundTripDelta2.isEmpty());

        
        PrismAsserts.assertEquivalent("Resources after roundtrip not equivalent", resourceAfter, resourceBefore);
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
