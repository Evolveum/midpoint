/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.prism;

import static org.testng.AssertJUnit.assertTrue;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.*;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.util.Collection;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.delta.*;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.foo.ActivationType;
import com.evolveum.midpoint.prism.foo.AssignmentType;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 */
public class TestDelta extends AbstractPrismTest {

	@Test
    public void testDeltaPaths() throws Exception {
		final String TEST_NAME="testDeltaPaths";
		displayTestTitle(TEST_NAME);

		PrismPropertyDefinition<String> descDefinition = new PrismPropertyDefinitionImpl<>(UserType.F_DESCRIPTION,
				DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());
		PropertyDelta<String> delta1 = new PropertyDelta<>(descDefinition, PrismTestUtil.getPrismContext());
		delta1.addValueToAdd(new PrismPropertyValue<>("add1"));
		assertPath(delta1, new ItemPath(UserType.F_DESCRIPTION));

		PrismReferenceDefinitionImpl referenceDefinition = new PrismReferenceDefinitionImpl(UserType.F_PARENT_ORG_REF,
                OBJECT_REFERENCE_TYPE_QNAME, PrismTestUtil.getPrismContext());
        ReferenceDelta delta2 = new ReferenceDelta(referenceDefinition, PrismTestUtil.getPrismContext());
        delta2.addValueToAdd(new PrismReferenceValue("oid1"));
        assertPath(delta2, new ItemPath(UserType.F_PARENT_ORG_REF));

    	PrismContainerValue<AssignmentType> assignmentValue1 = new PrismContainerValue<>(PrismTestUtil.getPrismContext());
    	// The value id is null
    	assignmentValue1.setPropertyRealValue(AssignmentType.F_DESCRIPTION, ASSIGNMENT_PATLAMA_DESCRIPTION, PrismTestUtil.getPrismContext());
		ObjectDelta<UserType> assObjDelta1 = ObjectDelta.createModificationAddContainer(UserType.class, USER_FOO_OID,
				UserType.F_ASSIGNMENT, PrismTestUtil.getPrismContext(), assignmentValue1);
		ItemDelta<?,?> assDelta1 = assObjDelta1.getModifications().iterator().next();
		assertPath(assDelta1, new ItemPath(UserType.F_ASSIGNMENT));

		PrismContainerValue<AssignmentType> assignmentValue2 = new PrismContainerValue<>(PrismTestUtil.getPrismContext());
    	assignmentValue1.setId(USER_ASSIGNMENT_1_ID);
    	assignmentValue1.setPropertyRealValue(AssignmentType.F_DESCRIPTION, "jamalalicha patlama paprtala", PrismTestUtil.getPrismContext());
		ObjectDelta<UserType> assObjDelta2 = ObjectDelta.createModificationAddContainer(UserType.class, USER_FOO_OID,
				UserType.F_ASSIGNMENT, PrismTestUtil.getPrismContext(), assignmentValue2);
		ItemDelta<?,?> assDelta2 = assObjDelta2.getModifications().iterator().next();
		assertPath(assDelta2, new ItemPath(UserType.F_ASSIGNMENT));

		PrismPropertyDefinition<String> assDescDefinition = new PrismPropertyDefinitionImpl<>(AssignmentType.F_DESCRIPTION,
				DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());
		ItemPath itemPathAssDescNoId = new ItemPath(UserType.F_ASSIGNMENT, AssignmentType.F_DESCRIPTION);
		PropertyDelta<String> propDelta2 = new PropertyDelta<>(itemPathAssDescNoId, descDefinition, PrismTestUtil.getPrismContext());
		assertPath(propDelta2, itemPathAssDescNoId);

		ItemPath itemPathAssDesc1Id = new ItemPath(
				new NameItemPathSegment(UserType.F_ASSIGNMENT),
				new IdItemPathSegment(USER_ASSIGNMENT_1_ID),
				new NameItemPathSegment(AssignmentType.F_DESCRIPTION));
		PropertyDelta<String> propDelta3 = new PropertyDelta<>(itemPathAssDesc1Id, descDefinition, PrismTestUtil.getPrismContext());
		assertPath(propDelta3, itemPathAssDesc1Id);

	}

	private void assertPath(ItemDelta<?,?> delta, ItemPath expectedPath) {
		assertEquals("Wrong path in "+delta, expectedPath, delta.getPath());
	}

	@Test
    public void testPropertyDeltaMerge01() throws Exception {
		final String TEST_NAME="testPropertyDeltaMerge01";
		displayTestTitle(TEST_NAME);

		// GIVEN
		PrismPropertyDefinition propertyDefinition = new PrismPropertyDefinitionImpl(UserType.F_DESCRIPTION,
				DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());

		PropertyDelta<String> delta1 = new PropertyDelta<String>(propertyDefinition, PrismTestUtil.getPrismContext());
		delta1.addValueToAdd(new PrismPropertyValue<>("add1"));

		PropertyDelta<String> delta2 = new PropertyDelta<String>(propertyDefinition, PrismTestUtil.getPrismContext());
		delta2.addValueToAdd(new PrismPropertyValue<>("add2"));

		// WHEN
		delta1.merge(delta2);

		// THEN
		System.out.println("Merged delta:");
		System.out.println(delta1.debugDump());

		PrismAsserts.assertNoReplace(delta1);
		PrismAsserts.assertAdd(delta1, "add1", "add2");
		PrismAsserts.assertNoDelete(delta1);
	}

	@Test
    public void testPropertyDeltaMerge02() throws Exception {
		final String TEST_NAME="testPropertyDeltaMerge02";
		displayTestTitle(TEST_NAME);

		// GIVEN
		PrismPropertyDefinition propertyDefinition = new PrismPropertyDefinitionImpl(UserType.F_DESCRIPTION,
				DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());

		PropertyDelta<String> delta1 = new PropertyDelta<String>(propertyDefinition, PrismTestUtil.getPrismContext());
		delta1.addValueToDelete(new PrismPropertyValue<>("del1"));

		PropertyDelta<String> delta2 = new PropertyDelta<String>(propertyDefinition, PrismTestUtil.getPrismContext());
		delta2.addValueToDelete(new PrismPropertyValue<>("del2"));

		// WHEN
		delta1.merge(delta2);

		// THEN
		System.out.println("Merged delta:");
		System.out.println(delta1.debugDump());

		PrismAsserts.assertNoReplace(delta1);
		PrismAsserts.assertNoAdd(delta1);
		PrismAsserts.assertDelete(delta1, "del1", "del2");
	}

	@Test
    public void testPropertyDeltaMerge03() throws Exception {
		final String TEST_NAME="testPropertyDeltaMerge03";
		displayTestTitle(TEST_NAME);

		// GIVEN
		PrismPropertyDefinition propertyDefinition = new PrismPropertyDefinitionImpl(UserType.F_DESCRIPTION,
				DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());

		PropertyDelta<String> delta1 = new PropertyDelta<String>(propertyDefinition, PrismTestUtil.getPrismContext());
		delta1.addValueToAdd(new PrismPropertyValue<>("add1"));
		delta1.addValueToDelete(new PrismPropertyValue<>("del1"));

		PropertyDelta<String> delta2 = new PropertyDelta<String>(propertyDefinition, PrismTestUtil.getPrismContext());
		delta2.addValueToAdd(new PrismPropertyValue<>("add2"));
		delta2.addValueToDelete(new PrismPropertyValue<>("del2"));

		// WHEN
		delta1.merge(delta2);

		// THEN
		System.out.println("Merged delta:");
		System.out.println(delta1.debugDump());

		PrismAsserts.assertNoReplace(delta1);
		PrismAsserts.assertAdd(delta1, "add1", "add2");
		PrismAsserts.assertDelete(delta1, "del1", "del2");
	}

	@Test
    public void testPropertyDeltaMerge04() throws Exception {
		final String TEST_NAME="testPropertyDeltaMerge04";
		displayTestTitle(TEST_NAME);

		// GIVEN
		PrismPropertyDefinition propertyDefinition = new PrismPropertyDefinitionImpl(UserType.F_DESCRIPTION,
				DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());

		PropertyDelta<String> delta1 = new PropertyDelta<String>(propertyDefinition, PrismTestUtil.getPrismContext());
		delta1.addValueToAdd(new PrismPropertyValue<>("add1"));
		delta1.addValueToDelete(new PrismPropertyValue<>("del1"));

		PropertyDelta<String> delta2 = new PropertyDelta<String>(propertyDefinition, PrismTestUtil.getPrismContext());
		delta2.addValueToAdd(new PrismPropertyValue<>("add2"));
		delta2.addValueToDelete(new PrismPropertyValue<>("add1"));

		// WHEN
		delta1.merge(delta2);

		// THEN
		System.out.println("Merged delta:");
		System.out.println(delta1.debugDump());

		PrismAsserts.assertNoReplace(delta1);
		PrismAsserts.assertAdd(delta1, "add2");
		PrismAsserts.assertDelete(delta1, "del1");
	}

	@Test
    public void testPropertyDeltaMerge05() throws Exception {
		final String TEST_NAME="testPropertyDeltaMerge05";
		displayTestTitle(TEST_NAME);

		// GIVEN
		PrismPropertyDefinition propertyDefinition = new PrismPropertyDefinitionImpl(UserType.F_DESCRIPTION,
				DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());

		PropertyDelta<String> delta1 = new PropertyDelta<String>(propertyDefinition, PrismTestUtil.getPrismContext());
		delta1.addValueToAdd(new PrismPropertyValue<>("add1"));

		PropertyDelta<String> delta2 = new PropertyDelta<String>(propertyDefinition, PrismTestUtil.getPrismContext());
		delta2.addValueToAdd(new PrismPropertyValue<>("add2"));
		delta2.addValueToDelete(new PrismPropertyValue<>("add1"));

		// WHEN
		delta1.merge(delta2);

		// THEN
		System.out.println("Merged delta:");
		System.out.println(delta1.debugDump());

		PrismAsserts.assertNoReplace(delta1);
		PrismAsserts.assertAdd(delta1, "add2");
		PrismAsserts.assertNoDelete(delta1);
	}

	@Test
    public void testPropertyDeltaMerge06() throws Exception {
		final String TEST_NAME="testPropertyDeltaMerge06";
		displayTestTitle(TEST_NAME);

		// GIVEN
		PrismPropertyDefinition propertyDefinition = new PrismPropertyDefinitionImpl(UserType.F_DESCRIPTION,
				DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());

		PropertyDelta<String> delta1 = new PropertyDelta<String>(propertyDefinition, PrismTestUtil.getPrismContext());
		delta1.addValueToAdd(new PrismPropertyValue<>("add1"));
		delta1.addValueToDelete(new PrismPropertyValue<>("del1"));

		PropertyDelta<String> delta2 = new PropertyDelta<String>(propertyDefinition, PrismTestUtil.getPrismContext());
		delta2.addValueToAdd(new PrismPropertyValue<>("del1"));

		// WHEN
		delta1.merge(delta2);

		// THEN
		System.out.println("Merged delta:");
		System.out.println(delta1.debugDump());

		PrismAsserts.assertNoReplace(delta1);
		PrismAsserts.assertAdd(delta1, "add1");
		PrismAsserts.assertNoDelete(delta1);
	}

	@Test
    public void testPropertyDeltaMerge10() throws Exception {
		final String TEST_NAME="testPropertyDeltaMerge10";
		displayTestTitle(TEST_NAME);

		// GIVEN
		PrismPropertyDefinition propertyDefinition = new PrismPropertyDefinitionImpl(UserType.F_DESCRIPTION,
				DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());

		PropertyDelta<String> delta1 = new PropertyDelta<String>(propertyDefinition, PrismTestUtil.getPrismContext());
		delta1.setValuesToReplace(new PrismPropertyValue<>("r1x"), new PrismPropertyValue<>("r1y"));

		PropertyDelta<String> delta2 = new PropertyDelta<String>(propertyDefinition, PrismTestUtil.getPrismContext());
		delta2.addValueToAdd(new PrismPropertyValue<>("add2"));

		// WHEN
		delta1.merge(delta2);

		// THEN
		System.out.println("Merged delta:");
		System.out.println(delta1.debugDump());

		PrismAsserts.assertReplace(delta1, "r1x", "r1y", "add2");
		PrismAsserts.assertNoAdd(delta1);
		PrismAsserts.assertNoDelete(delta1);
	}

	@Test
    public void testPropertyDeltaMerge11() throws Exception {
		final String TEST_NAME="testPropertyDeltaMerge11";
		displayTestTitle(TEST_NAME);

		// GIVEN
		PrismPropertyDefinition propertyDefinition = new PrismPropertyDefinitionImpl(UserType.F_DESCRIPTION,
				DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());

		PropertyDelta<String> delta1 = new PropertyDelta<String>(propertyDefinition, PrismTestUtil.getPrismContext());
		delta1.setValuesToReplace(new PrismPropertyValue<>("r1x"), new PrismPropertyValue<>("r1y"));

		PropertyDelta<String> delta2 = new PropertyDelta<String>(propertyDefinition, PrismTestUtil.getPrismContext());
		delta2.addValueToAdd(new PrismPropertyValue<>("add2"));
		delta2.addValueToDelete(new PrismPropertyValue<>("r1y"));

		// WHEN
		delta1.merge(delta2);

		// THEN
		System.out.println("Merged delta:");
		System.out.println(delta1.debugDump());

		PrismAsserts.assertReplace(delta1, "r1x", "add2");
		PrismAsserts.assertNoAdd(delta1);
		PrismAsserts.assertNoDelete(delta1);
	}

	@Test
    public void testPropertyDeltaMerge12() throws Exception {
		final String TEST_NAME="testPropertyDeltaMerge12";
		displayTestTitle(TEST_NAME);

		// GIVEN
		PrismPropertyDefinition propertyDefinition = new PrismPropertyDefinitionImpl(UserType.F_DESCRIPTION,
				DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());

		PropertyDelta<String> delta1 = new PropertyDelta<String>(propertyDefinition, PrismTestUtil.getPrismContext());
		delta1.setValuesToReplace(new PrismPropertyValue<>("r1x"), new PrismPropertyValue<>("r1y"));

		PropertyDelta<String> delta2 = new PropertyDelta<String>(propertyDefinition, PrismTestUtil.getPrismContext());
		delta2.addValueToAdd(new PrismPropertyValue<>("add2"));
		delta2.addValueToDelete(new PrismPropertyValue<>("del2"));

		// WHEN
		delta1.merge(delta2);

		// THEN
		System.out.println("Merged delta:");
		System.out.println(delta1.debugDump());

		PrismAsserts.assertReplace(delta1, "r1x", "r1y", "add2");
		PrismAsserts.assertNoAdd(delta1);
		PrismAsserts.assertNoDelete(delta1);
	}

	@Test
    public void testPropertyDeltaMerge13() throws Exception {
		final String TEST_NAME="testPropertyDeltaMerge13";
		displayTestTitle(TEST_NAME);

		// GIVEN
		PrismPropertyDefinition propertyDefinition = new PrismPropertyDefinitionImpl(UserType.F_DESCRIPTION,
				DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());

		PropertyDelta<String> delta1 = new PropertyDelta<String>(propertyDefinition, PrismTestUtil.getPrismContext());
		delta1.setValuesToReplace(new PrismPropertyValue<>("r1x"));

		PropertyDelta<String> delta2 = new PropertyDelta<String>(propertyDefinition, PrismTestUtil.getPrismContext());
		delta2.addValueToDelete(new PrismPropertyValue<>("r1x"));

		// WHEN
		delta1.merge(delta2);

		// THEN
		System.out.println("Merged delta:");
		System.out.println(delta1.debugDump());

		PrismAsserts.assertReplace(delta1);
		PrismAsserts.assertNoAdd(delta1);
		PrismAsserts.assertNoDelete(delta1);
	}

	@Test
    public void testPropertyDeltaMerge20() throws Exception {
		final String TEST_NAME="testPropertyDeltaMerge20";
		displayTestTitle(TEST_NAME);

		// GIVEN
		PrismPropertyDefinition propertyDefinition = new PrismPropertyDefinitionImpl(UserType.F_DESCRIPTION,
				DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());

		PropertyDelta<String> delta1 = new PropertyDelta<String>(propertyDefinition, PrismTestUtil.getPrismContext());
		delta1.addValueToAdd(new PrismPropertyValue<>("add1"));
		delta1.addValueToDelete(new PrismPropertyValue<>("del1"));

		PropertyDelta<String> delta2 = new PropertyDelta<String>(propertyDefinition, PrismTestUtil.getPrismContext());
		delta2.setValuesToReplace(new PrismPropertyValue<>("r2x"), new PrismPropertyValue<>("r2y"));

		// WHEN
		delta1.merge(delta2);

		// THEN
		System.out.println("Merged delta:");
		System.out.println(delta1.debugDump());

		PrismAsserts.assertReplace(delta1, "r2x", "r2y");
		PrismAsserts.assertNoAdd(delta1);
		PrismAsserts.assertNoDelete(delta1);
	}

	@Test
    public void testPropertyDeltaSwallow01() throws Exception {
		final String TEST_NAME="testPropertyDeltaSwallow01";
		displayTestTitle(TEST_NAME);

		// GIVEN
		PrismPropertyDefinition propertyDefinition = new PrismPropertyDefinitionImpl(UserType.F_DESCRIPTION,
				DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());

		PropertyDelta<String> delta1 = new PropertyDelta<String>(propertyDefinition, PrismTestUtil.getPrismContext());
		delta1.addValueToAdd(new PrismPropertyValue<>("add1"));
		ObjectDelta<UserType> objectDelta = new ObjectDelta<>(UserType.class, ChangeType.MODIFY,
            PrismTestUtil.getPrismContext());
		objectDelta.addModification(delta1);

		PropertyDelta<String> delta2 = new PropertyDelta<String>(propertyDefinition, PrismTestUtil.getPrismContext());
		delta2.addValueToAdd(new PrismPropertyValue<>("add2"));

		// WHEN
		objectDelta.swallow(delta2);

		// THEN
		System.out.println("Swallowed delta:");
		System.out.println(objectDelta.debugDump());

		PrismAsserts.assertModifications(objectDelta, 1);
		PropertyDelta<String> modification = (PropertyDelta<String>) objectDelta.getModifications().iterator().next();
		PrismAsserts.assertNoReplace(modification);
		PrismAsserts.assertAdd(modification, "add1", "add2");
		PrismAsserts.assertNoDelete(modification);
	}

	@Test
    public void testSummarize01() throws Exception {
		final String TEST_NAME="testSummarize01";
		displayTestTitle(TEST_NAME);

		// GIVEN
		PrismPropertyDefinition propertyDefinition = new PrismPropertyDefinitionImpl(UserType.F_DESCRIPTION,
				DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());

		PropertyDelta<String> delta1 = new PropertyDelta<String>(propertyDefinition, PrismTestUtil.getPrismContext());
		delta1.addValueToAdd(new PrismPropertyValue<>("add1"));
		ObjectDelta<UserType> objectDelta1 = new ObjectDelta<>(UserType.class, ChangeType.MODIFY,
            PrismTestUtil.getPrismContext());
		objectDelta1.addModification(delta1);

		PropertyDelta<String> delta2 = new PropertyDelta<String>(propertyDefinition, PrismTestUtil.getPrismContext());
		delta2.addValueToAdd(new PrismPropertyValue<>("add2"));
		ObjectDelta<UserType> objectDelta2 = new ObjectDelta<>(UserType.class, ChangeType.MODIFY,
            PrismTestUtil.getPrismContext());
		objectDelta2.addModification(delta2);

		// WHEN
		ObjectDelta<UserType> sumDelta = ObjectDelta.summarize(objectDelta1, objectDelta2);

		// THEN
		System.out.println("Summarized delta:");
		System.out.println(sumDelta.debugDump());

		PrismAsserts.assertModifications(sumDelta, 1);
		PropertyDelta<String> modification = (PropertyDelta<String>) sumDelta.getModifications().iterator().next();
		PrismAsserts.assertNoReplace(modification);
		PrismAsserts.assertAdd(modification, "add1", "add2");
		PrismAsserts.assertNoDelete(modification);
	}

	@Test
    public void testSummarize02() throws Exception {
		final String TEST_NAME="testSummarize02";
		displayTestTitle(TEST_NAME);

		// GIVEN
		PrismPropertyDefinition propertyDefinition = new PrismPropertyDefinitionImpl(UserType.F_DESCRIPTION,
				DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());

		PropertyDelta<String> delta1 = new PropertyDelta<String>(propertyDefinition, PrismTestUtil.getPrismContext());
		delta1.addValueToDelete(new PrismPropertyValue<>("del1"));
		ObjectDelta<UserType> objectDelta1 = new ObjectDelta<>(UserType.class, ChangeType.MODIFY,
            PrismTestUtil.getPrismContext());
		objectDelta1.addModification(delta1);

		PropertyDelta<String> delta2 = new PropertyDelta<String>(propertyDefinition, PrismTestUtil.getPrismContext());
		delta2.addValueToDelete(new PrismPropertyValue<>("del2"));
		ObjectDelta<UserType> objectDelta2 = new ObjectDelta<>(UserType.class, ChangeType.MODIFY,
            PrismTestUtil.getPrismContext());
		objectDelta2.addModification(delta2);

		// WHEN
		ObjectDelta<UserType> sumDelta = ObjectDelta.summarize(objectDelta1, objectDelta2);

		// THEN
		System.out.println("Summarized delta:");
		System.out.println(sumDelta.debugDump());

		PrismAsserts.assertModifications(sumDelta, 1);
		PropertyDelta<String> modification = (PropertyDelta<String>) sumDelta.getModifications().iterator().next();
		PrismAsserts.assertNoReplace(modification);
		PrismAsserts.assertNoAdd(modification);
		PrismAsserts.assertDelete(modification, "del1", "del2");
	}

	@Test
    public void testSummarize05() throws Exception {
		final String TEST_NAME="testSummarize05";
		displayTestTitle(TEST_NAME);

		// GIVEN
		PrismPropertyDefinition propertyDefinition = new PrismPropertyDefinitionImpl(UserType.F_DESCRIPTION,
				DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());

		PropertyDelta<String> delta1 = new PropertyDelta<String>(propertyDefinition, PrismTestUtil.getPrismContext());
		// Let's complicate the things a bit with origin. This should work even though origins do not match.
		delta1.addValueToAdd(new PrismPropertyValue<>("add1", OriginType.OUTBOUND, null));
		ObjectDelta<UserType> objectDelta1 = new ObjectDelta<>(UserType.class, ChangeType.MODIFY,
            PrismTestUtil.getPrismContext());
		objectDelta1.addModification(delta1);

		PropertyDelta<String> delta2 = new PropertyDelta<String>(propertyDefinition, PrismTestUtil.getPrismContext());
		delta2.addValueToAdd(new PrismPropertyValue<>("add2"));
		delta2.addValueToDelete(new PrismPropertyValue<>("add1"));
		ObjectDelta<UserType> objectDelta2 = new ObjectDelta<>(UserType.class, ChangeType.MODIFY,
            PrismTestUtil.getPrismContext());
		objectDelta2.addModification(delta2);

		// WHEN
		ObjectDelta<UserType> sumDelta = ObjectDelta.summarize(objectDelta1, objectDelta2);

		// THEN
		System.out.println("Summarized delta:");
		System.out.println(sumDelta.debugDump());

		PrismAsserts.assertModifications(sumDelta, 1);
		PropertyDelta<String> modification = (PropertyDelta<String>) sumDelta.getModifications().iterator().next();
		PrismAsserts.assertNoReplace(modification);
		PrismAsserts.assertAdd(modification, "add2");
		PrismAsserts.assertNoDelete(modification);
	}

    @Test
    public void testSummarize06() throws Exception {
    	final String TEST_NAME="testSummarize06";
		displayTestTitle(TEST_NAME);

        // GIVEN
        PrismReferenceDefinition referenceDefinition = new PrismReferenceDefinitionImpl(UserType.F_PARENT_ORG_REF,
                OBJECT_REFERENCE_TYPE_QNAME, PrismTestUtil.getPrismContext());

        ReferenceDelta delta1 = new ReferenceDelta(referenceDefinition, PrismTestUtil.getPrismContext());
        delta1.addValueToAdd(new PrismReferenceValue("oid1"));
        ObjectDelta<UserType> objectDelta1 = new ObjectDelta<>(UserType.class, ChangeType.MODIFY,
            PrismTestUtil.getPrismContext());
        objectDelta1.addModification(delta1);

        ReferenceDelta delta2 = new ReferenceDelta(referenceDefinition, PrismTestUtil.getPrismContext());
        delta2.addValueToAdd(new PrismReferenceValue("oid1"));                    // here we add the same value
        ObjectDelta<UserType> objectDelta2 = new ObjectDelta<>(UserType.class, ChangeType.MODIFY,
            PrismTestUtil.getPrismContext());
        objectDelta2.addModification(delta2);

        // WHEN
        ObjectDelta<UserType> sumDelta = ObjectDelta.summarize(objectDelta1, objectDelta2);

        // THEN
        System.out.println("Summarized delta:");
        System.out.println(sumDelta.debugDump());

        PrismAsserts.assertModifications(sumDelta, 1);
        ReferenceDelta modification = (ReferenceDelta) sumDelta.getModifications().iterator().next();
        PrismAsserts.assertNoReplace(modification);
        assertEquals("Invalid number of values to add", 1, modification.getValuesToAdd().size());
        PrismAsserts.assertNoDelete(modification);
    }

	@Test
    public void testAddPropertyMulti() throws Exception {
		final String TEST_NAME="testAddPropertyMulti";
		displayTestTitle(TEST_NAME);
		// GIVEN

		// User
		PrismObject<UserType> user = createUserFooPatlama();

		//Delta
    	ObjectDelta<UserType> userDelta = ObjectDelta.createModificationAddProperty(UserType.class, USER_FOO_OID, UserType.F_ADDITIONAL_NAMES,
    			PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("baz"));

		// WHEN
        userDelta.applyTo(user);

        // THEN
        assertEquals("Wrong OID", USER_FOO_OID, user.getOid());
        PrismAsserts.assertPropertyValue(user, UserType.F_ADDITIONAL_NAMES, PrismTestUtil.createPolyString("baz"), PrismTestUtil.createPolyString("foobar"));
        PrismContainer<AssignmentType> assignment = user.findContainer(UserType.F_ASSIGNMENT);
        assertNotNull("No assignment", assignment);
        assertEquals("Unexpected number of assignment values", 1, assignment.size());
    }

	@Test
    public void testAddAssignmentSameNullIdApplyToObject() throws Exception {
		final String TEST_NAME="testAddAssignmentSameNullIdApplyToObject";
		displayTestTitle(TEST_NAME);
		// GIVEN

		// User
		PrismObject<UserType> user = createUserFooPatlama();

		//Delta
    	PrismContainerValue<AssignmentType> assignmentValue = new PrismContainerValue<>(PrismTestUtil.getPrismContext());
    	// The value id is null
    	assignmentValue.setPropertyRealValue(AssignmentType.F_DESCRIPTION, ASSIGNMENT_PATLAMA_DESCRIPTION, PrismTestUtil.getPrismContext());

		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationAddContainer(UserType.class, USER_FOO_OID,
				UserType.F_ASSIGNMENT, PrismTestUtil.getPrismContext(), assignmentValue);

		// WHEN
        userDelta.applyTo(user);

        // THEN
        System.out.println("User after delta application:");
        System.out.println(user.debugDump());
        assertEquals("Wrong OID", USER_FOO_OID, user.getOid());
        PrismAsserts.assertPropertyValue(user, UserType.F_ADDITIONAL_NAMES, PrismTestUtil.createPolyString("foobar"));
        PrismContainer<AssignmentType> assignment = user.findContainer(UserType.F_ASSIGNMENT);
        assertNotNull("No assignment", assignment);
        assertEquals("Unexpected number of assignment values", 1, assignment.size());
    }

	@Test
    public void testAddAssignmentSameNullIdSwallow() throws Exception {
		final String TEST_NAME="testAddAssignmentSameNullIdSwallow";
		displayTestTitle(TEST_NAME);
		// GIVEN

		//Delta 1
    	PrismContainerValue<AssignmentType> assignmentValue1 = new PrismContainerValue<>(PrismTestUtil.getPrismContext());
    	// The value id is null
    	assignmentValue1.setPropertyRealValue(AssignmentType.F_DESCRIPTION, ASSIGNMENT_PATLAMA_DESCRIPTION, PrismTestUtil.getPrismContext());

		ObjectDelta<UserType> userDelta1 = ObjectDelta.createModificationAddContainer(UserType.class, USER_FOO_OID,
				UserType.F_ASSIGNMENT, PrismTestUtil.getPrismContext(), assignmentValue1);

		//Delta 2
    	PrismContainerValue<AssignmentType> assignmentValue2 = new PrismContainerValue<>(PrismTestUtil.getPrismContext());
    	// The value id is null
    	assignmentValue2.setPropertyRealValue(AssignmentType.F_DESCRIPTION, ASSIGNMENT_PATLAMA_DESCRIPTION, PrismTestUtil.getPrismContext());
    	ContainerDelta<AssignmentType> containerDelta2 = ContainerDelta.createDelta(UserType.F_ASSIGNMENT,getUserTypeDefinition());
    	containerDelta2.addValueToAdd(assignmentValue2);

		// WHEN
        userDelta1.swallow(containerDelta2);

        // THEN
        System.out.println("Delta after swallow:");
        System.out.println(userDelta1.debugDump());
        assertEquals("Wrong OID", USER_FOO_OID, userDelta1.getOid());
        ContainerDelta<AssignmentType> containerDeltaAfter = userDelta1.findContainerDelta(UserType.F_ASSIGNMENT);
        assertNotNull("No assignment delta", containerDeltaAfter);
        PrismAsserts.assertNoDelete(containerDeltaAfter);
        PrismAsserts.assertNoReplace(containerDeltaAfter);
        Collection<PrismContainerValue<AssignmentType>> valuesToAdd = containerDeltaAfter.getValuesToAdd();
        assertEquals("Unexpected number of values to add", 1, valuesToAdd.size());
        assertEquals("Wrong value to add", assignmentValue1, valuesToAdd.iterator().next());
    }

	@Test
    public void testAddAssignmentDifferentNullIdSwallow() throws Exception {
		final String TEST_NAME="testAddAssignmentDifferentNullIdSwallow";
		displayTestTitle(TEST_NAME);
		// GIVEN

		//Delta 1
    	PrismContainerValue<AssignmentType> assignmentValue1 = new PrismContainerValue<>(PrismTestUtil.getPrismContext());
    	// The value id is null
    	assignmentValue1.setPropertyRealValue(AssignmentType.F_DESCRIPTION, ASSIGNMENT_PATLAMA_DESCRIPTION, PrismTestUtil.getPrismContext());

		ObjectDelta<UserType> userDelta1 = ObjectDelta.createModificationAddContainer(UserType.class, USER_FOO_OID,
				UserType.F_ASSIGNMENT, PrismTestUtil.getPrismContext(), assignmentValue1);

		//Delta 2
    	PrismContainerValue<AssignmentType> assignmentValue2 = new PrismContainerValue<>(PrismTestUtil.getPrismContext());
    	// The value id is null
    	assignmentValue2.setPropertyRealValue(AssignmentType.F_DESCRIPTION, ASSIGNMENT_ABRAKADABRA_DESCRIPTION, PrismTestUtil.getPrismContext());
    	ContainerDelta<AssignmentType> containerDelta2 = ContainerDelta.createDelta(UserType.F_ASSIGNMENT,getUserTypeDefinition());
    	containerDelta2.addValueToAdd(assignmentValue2);

		// WHEN
        userDelta1.swallow(containerDelta2);

        // THEN
        System.out.println("Delta after swallow:");
        System.out.println(userDelta1.debugDump());
        assertEquals("Wrong OID", USER_FOO_OID, userDelta1.getOid());
        ContainerDelta<AssignmentType> containerDeltaAfter = userDelta1.findContainerDelta(UserType.F_ASSIGNMENT);
        assertNotNull("No assignment delta", containerDeltaAfter);
        PrismAsserts.assertNoDelete(containerDeltaAfter);
        PrismAsserts.assertNoReplace(containerDeltaAfter);
        Collection<PrismContainerValue<AssignmentType>> valuesToAdd = containerDeltaAfter.getValuesToAdd();
        assertEquals("Unexpected number of values to add", 2, valuesToAdd.size());
        assertTrue("Value "+assignmentValue1+" missing ", valuesToAdd.contains(assignmentValue1));
        assertTrue("Value "+assignmentValue2+" missing ", valuesToAdd.contains(assignmentValue2));
    }

	@Test
    public void testAddAssignmentDifferentFirstIdSwallow() throws Exception {
		final String TEST_NAME="testAddAssignmentDifferentFirstIdSwallow";
		displayTestTitle(TEST_NAME);
		// GIVEN

		//Delta 1
    	PrismContainerValue<AssignmentType> assignmentValue1 = new PrismContainerValue<>(PrismTestUtil.getPrismContext());
    	assignmentValue1.setId(USER_ASSIGNMENT_1_ID);
    	assignmentValue1.setPropertyRealValue(AssignmentType.F_DESCRIPTION, ASSIGNMENT_PATLAMA_DESCRIPTION, PrismTestUtil.getPrismContext());

		ObjectDelta<UserType> userDelta1 = ObjectDelta.createModificationAddContainer(UserType.class, USER_FOO_OID,
				UserType.F_ASSIGNMENT, PrismTestUtil.getPrismContext(), assignmentValue1);

		//Delta 2
    	PrismContainerValue<AssignmentType> assignmentValue2 = new PrismContainerValue<>(PrismTestUtil.getPrismContext());
    	// The value id is null
    	assignmentValue2.setPropertyRealValue(AssignmentType.F_DESCRIPTION, ASSIGNMENT_ABRAKADABRA_DESCRIPTION, PrismTestUtil.getPrismContext());
    	ContainerDelta<AssignmentType> containerDelta2 = ContainerDelta.createDelta(UserType.F_ASSIGNMENT, getUserTypeDefinition());
    	containerDelta2.addValueToAdd(assignmentValue2);

		// WHEN
        userDelta1.swallow(containerDelta2);

        // THEN
        System.out.println("Delta after swallow:");
        System.out.println(userDelta1.debugDump());
        assertEquals("Wrong OID", USER_FOO_OID, userDelta1.getOid());
        ContainerDelta<AssignmentType> containerDeltaAfter = userDelta1.findContainerDelta(UserType.F_ASSIGNMENT);
        assertNotNull("No assignment delta", containerDeltaAfter);
        PrismAsserts.assertNoDelete(containerDeltaAfter);
        PrismAsserts.assertNoReplace(containerDeltaAfter);
        Collection<PrismContainerValue<AssignmentType>> valuesToAdd = containerDeltaAfter.getValuesToAdd();
        assertEquals("Unexpected number of values to add", 2, valuesToAdd.size());
        assertTrue("Value "+assignmentValue1+" missing ", valuesToAdd.contains(assignmentValue1));
        assertTrue("Value "+assignmentValue2+" missing ", valuesToAdd.contains(assignmentValue2));
    }

	@Test
    public void testAddAssignmentDifferentSecondIdSwallow() throws Exception {
		final String TEST_NAME="testAddAssignmentDifferentSecondIdSwallow";
		displayTestTitle(TEST_NAME);
		// GIVEN

		//Delta 1
    	PrismContainerValue<AssignmentType> assignmentValue1 = new PrismContainerValue<>(PrismTestUtil.getPrismContext());
    	// The value id is null
    	assignmentValue1.setPropertyRealValue(AssignmentType.F_DESCRIPTION, ASSIGNMENT_PATLAMA_DESCRIPTION, PrismTestUtil.getPrismContext());

		ObjectDelta<UserType> userDelta1 = ObjectDelta.createModificationAddContainer(UserType.class, USER_FOO_OID,
				UserType.F_ASSIGNMENT, PrismTestUtil.getPrismContext(), assignmentValue1);

		//Delta 2
    	PrismContainerValue<AssignmentType> assignmentValue2 = new PrismContainerValue<>(PrismTestUtil.getPrismContext());
    	assignmentValue2.setId(USER_ASSIGNMENT_2_ID);
    	assignmentValue2.setPropertyRealValue(AssignmentType.F_DESCRIPTION, ASSIGNMENT_ABRAKADABRA_DESCRIPTION, PrismTestUtil.getPrismContext());
    	ContainerDelta<AssignmentType> containerDelta2 = ContainerDelta.createDelta(UserType.F_ASSIGNMENT,getUserTypeDefinition());
    	containerDelta2.addValueToAdd(assignmentValue2);

		// WHEN
        userDelta1.swallow(containerDelta2);

        // THEN
        System.out.println("Delta after swallow:");
        System.out.println(userDelta1.debugDump());
        assertEquals("Wrong OID", USER_FOO_OID, userDelta1.getOid());
        ContainerDelta<AssignmentType> containerDeltaAfter = userDelta1.findContainerDelta(UserType.F_ASSIGNMENT);
        assertNotNull("No assignment delta", containerDeltaAfter);
        PrismAsserts.assertNoDelete(containerDeltaAfter);
        PrismAsserts.assertNoReplace(containerDeltaAfter);
        Collection<PrismContainerValue<AssignmentType>> valuesToAdd = containerDeltaAfter.getValuesToAdd();
        assertEquals("Unexpected number of values to add", 2, valuesToAdd.size());
        assertTrue("Value "+assignmentValue1+" missing ", valuesToAdd.contains(assignmentValue1));
        assertTrue("Value "+assignmentValue2+" missing ", valuesToAdd.contains(assignmentValue2));
    }

	@Test
    public void testAddAssignmentDifferentTwoIdsSwallow() throws Exception {
		final String TEST_NAME="testAddAssignmentDifferentTwoIdsSwallow";
		displayTestTitle(TEST_NAME);
		// GIVEN

		//Delta 1
    	PrismContainerValue<AssignmentType> assignmentValue1 = new PrismContainerValue<>(PrismTestUtil.getPrismContext());
    	assignmentValue1.setId(USER_ASSIGNMENT_1_ID);
    	assignmentValue1.setPropertyRealValue(AssignmentType.F_DESCRIPTION, ASSIGNMENT_PATLAMA_DESCRIPTION, PrismTestUtil.getPrismContext());

		ObjectDelta<UserType> userDelta1 = ObjectDelta.createModificationAddContainer(UserType.class, USER_FOO_OID,
				UserType.F_ASSIGNMENT, PrismTestUtil.getPrismContext(), assignmentValue1);

		//Delta 2
    	PrismContainerValue<AssignmentType> assignmentValue2 = new PrismContainerValue<>(PrismTestUtil.getPrismContext());
    	assignmentValue2.setId(USER_ASSIGNMENT_2_ID);
    	assignmentValue2.setPropertyRealValue(AssignmentType.F_DESCRIPTION, ASSIGNMENT_ABRAKADABRA_DESCRIPTION, PrismTestUtil.getPrismContext());
    	ContainerDelta<AssignmentType> containerDelta2 = ContainerDelta.createDelta(UserType.F_ASSIGNMENT,getUserTypeDefinition());
    	containerDelta2.addValueToAdd(assignmentValue2);

		// WHEN
        userDelta1.swallow(containerDelta2);

        // THEN
        System.out.println("Delta after swallow:");
        System.out.println(userDelta1.debugDump());
        assertEquals("Wrong OID", USER_FOO_OID, userDelta1.getOid());
        ContainerDelta<AssignmentType> containerDeltaAfter = userDelta1.findContainerDelta(UserType.F_ASSIGNMENT);
        assertNotNull("No assignment delta", containerDeltaAfter);
        PrismAsserts.assertNoDelete(containerDeltaAfter);
        PrismAsserts.assertNoReplace(containerDeltaAfter);
        Collection<PrismContainerValue<AssignmentType>> valuesToAdd = containerDeltaAfter.getValuesToAdd();
        assertEquals("Unexpected number of values to add", 2, valuesToAdd.size());
        assertTrue("Value "+assignmentValue1+" missing ", valuesToAdd.contains(assignmentValue1));
        assertTrue("Value "+assignmentValue2+" missing ", valuesToAdd.contains(assignmentValue2));
    }

	@Test
    public void testAddAssignmentDifferentIdSameSwallow() throws Exception {
		final String TEST_NAME="testAddAssignmentDifferentIdSameSwallow";
		displayTestTitle(TEST_NAME);
		// GIVEN

		//Delta 1
    	PrismContainerValue<AssignmentType> assignmentValue1 = new PrismContainerValue<>(PrismTestUtil.getPrismContext());
    	assignmentValue1.setId(USER_ASSIGNMENT_1_ID);
    	assignmentValue1.setPropertyRealValue(AssignmentType.F_DESCRIPTION, ASSIGNMENT_PATLAMA_DESCRIPTION, PrismTestUtil.getPrismContext());

		ObjectDelta<UserType> userDelta1 = ObjectDelta.createModificationAddContainer(UserType.class, USER_FOO_OID,
				UserType.F_ASSIGNMENT, PrismTestUtil.getPrismContext(), assignmentValue1);

		//Delta 2
    	PrismContainerValue<AssignmentType> assignmentValue2 = new PrismContainerValue<>(PrismTestUtil.getPrismContext());
    	assignmentValue2.setId(USER_ASSIGNMENT_1_ID);
    	assignmentValue2.setPropertyRealValue(AssignmentType.F_DESCRIPTION, ASSIGNMENT_PATLAMA_DESCRIPTION, PrismTestUtil.getPrismContext());
    	ContainerDelta<AssignmentType> containerDelta2 = ContainerDelta.createDelta(UserType.F_ASSIGNMENT,getUserTypeDefinition());
    	containerDelta2.addValueToAdd(assignmentValue2);

		// WHEN
        userDelta1.swallow(containerDelta2);

        // THEN
        System.out.println("Delta after swallow:");
        System.out.println(userDelta1.debugDump());
        assertEquals("Wrong OID", USER_FOO_OID, userDelta1.getOid());
        ContainerDelta<AssignmentType> containerDeltaAfter = userDelta1.findContainerDelta(UserType.F_ASSIGNMENT);
        assertNotNull("No assignment delta", containerDeltaAfter);
        PrismAsserts.assertNoDelete(containerDeltaAfter);
        PrismAsserts.assertNoReplace(containerDeltaAfter);
        Collection<PrismContainerValue<AssignmentType>> valuesToAdd = containerDeltaAfter.getValuesToAdd();
        assertEquals("Unexpected number of values to add", 1, valuesToAdd.size());
        assertTrue("Value "+assignmentValue1+" missing ", valuesToAdd.contains(assignmentValue1));
    }

	// MID-1296
	@Test(enabled=false)
    public void testAddAssignmentDifferentIdConflictSwallow() throws Exception {
		final String TEST_NAME="testAddAssignmentDifferentIdConflictSwallow";
		displayTestTitle(TEST_NAME);
		// GIVEN

		//Delta 1
    	PrismContainerValue<AssignmentType> assignmentValue1 = new PrismContainerValue<>(PrismTestUtil.getPrismContext());
    	assignmentValue1.setId(USER_ASSIGNMENT_1_ID);
    	assignmentValue1.setPropertyRealValue(AssignmentType.F_DESCRIPTION, ASSIGNMENT_PATLAMA_DESCRIPTION, PrismTestUtil.getPrismContext());

		ObjectDelta<UserType> userDelta1 = ObjectDelta.createModificationAddContainer(UserType.class, USER_FOO_OID,
				UserType.F_ASSIGNMENT, PrismTestUtil.getPrismContext(), assignmentValue1);

		//Delta 2
    	PrismContainerValue<AssignmentType> assignmentValue2 = new PrismContainerValue<>(PrismTestUtil.getPrismContext());
    	assignmentValue2.setId(USER_ASSIGNMENT_1_ID);
    	assignmentValue2.setPropertyRealValue(AssignmentType.F_DESCRIPTION, ASSIGNMENT_ABRAKADABRA_DESCRIPTION, PrismTestUtil.getPrismContext());
    	ContainerDelta<AssignmentType> containerDelta2 = ContainerDelta.createDelta(UserType.F_ASSIGNMENT, getUserTypeDefinition());
    	containerDelta2.addValueToAdd(assignmentValue2);

		// WHEN
        userDelta1.swallow(containerDelta2);

        AssertJUnit.fail("Unexpected success");
    }

	@Test
    public void testAddDeltaAddAssignmentDifferentNoIdSwallow() throws Exception {
		final String TEST_NAME="testAddDeltaAddAssignmentDifferentNoIdSwallow";
		displayTestTitle(TEST_NAME);
		// GIVEN

		//Delta 1
		PrismObject<UserType> user = createUserFooPatlama();
		ObjectDelta<UserType> userDelta1 = ObjectDelta.createAddDelta(user);

		//Delta 2
    	PrismContainerValue<AssignmentType> assignmentValue2 = new PrismContainerValue<>(PrismTestUtil.getPrismContext());
    	// null container ID
    	assignmentValue2.setPropertyRealValue(AssignmentType.F_DESCRIPTION, ASSIGNMENT_ABRAKADABRA_DESCRIPTION, PrismTestUtil.getPrismContext());
    	ContainerDelta<AssignmentType> containerDelta2 = ContainerDelta.createDelta(UserType.F_ASSIGNMENT,getUserTypeDefinition());
    	containerDelta2.addValueToAdd(assignmentValue2);

		// WHEN
        userDelta1.swallow(containerDelta2);

        // THEN
        System.out.println("Delta after swallow:");
        System.out.println(userDelta1.debugDump());
        assertEquals("Wrong OID", USER_FOO_OID, userDelta1.getOid());
        ContainerDelta<AssignmentType> containerDeltaAfter = userDelta1.findContainerDelta(UserType.F_ASSIGNMENT);
        assertNotNull("No assignment delta", containerDeltaAfter);
        PrismAsserts.assertNoDelete(containerDeltaAfter);
        PrismAsserts.assertNoReplace(containerDeltaAfter);
        Collection<PrismContainerValue<AssignmentType>> valuesToAdd = containerDeltaAfter.getValuesToAdd();
        assertEquals("Unexpected number of values to add", 2, valuesToAdd.size());
        PrismContainer<AssignmentType> user1AssignmentCont = user.findContainer(UserType.F_ASSIGNMENT);
        for (PrismContainerValue<AssignmentType> cval: user1AssignmentCont.getValues()) {
        	assertTrue("Value "+cval+" missing ", valuesToAdd.contains(cval));
        }
        assertTrue("Value "+assignmentValue2+" missing ", valuesToAdd.contains(assignmentValue2));
    }

	@Test
    public void testAddDeltaNoAssignmentAddAssignmentDifferentNoIdSwallow() throws Exception {
		final String TEST_NAME="testAddDeltaNoAssignmentAddAssignmentDifferentNoIdSwallow";
		displayTestTitle(TEST_NAME);
		// GIVEN

		//Delta 1
		PrismObject<UserType> user = createUserFoo();
		ObjectDelta<UserType> userDelta1 = ObjectDelta.createAddDelta(user);

		//Delta 2
    	PrismContainerValue<AssignmentType> assignmentValue2 = new PrismContainerValue<>(PrismTestUtil.getPrismContext());
    	// null container ID
    	assignmentValue2.setPropertyRealValue(AssignmentType.F_DESCRIPTION, ASSIGNMENT_ABRAKADABRA_DESCRIPTION, PrismTestUtil.getPrismContext());
    	ContainerDelta<AssignmentType> containerDelta2 = ContainerDelta.createDelta(UserType.F_ASSIGNMENT,getUserTypeDefinition());
    	containerDelta2.addValueToAdd(assignmentValue2);

		// WHEN
        userDelta1.swallow(containerDelta2);

        // THEN
        System.out.println("Delta after swallow:");
        System.out.println(userDelta1.debugDump());
        assertEquals("Wrong OID", USER_FOO_OID, userDelta1.getOid());
        ContainerDelta<AssignmentType> containerDeltaAfter = userDelta1.findContainerDelta(UserType.F_ASSIGNMENT);
        assertNotNull("No assignment delta", containerDeltaAfter);
        PrismAsserts.assertNoDelete(containerDeltaAfter);
        PrismAsserts.assertNoReplace(containerDeltaAfter);
        Collection<PrismContainerValue<AssignmentType>> valuesToAdd = containerDeltaAfter.getValuesToAdd();
        assertEquals("Unexpected number of values to add", 1, valuesToAdd.size());
        assertTrue("Value "+assignmentValue2+" missing ", valuesToAdd.contains(assignmentValue2));
    }

	@Test
    public void testAddDeltaNoAssignmentAddAssignmentDifferentIdSwallow() throws Exception {
		final String TEST_NAME="testAddDeltaNoAssignmentAddAssignmentDifferentIdSwallow";
		displayTestTitle(TEST_NAME);
		// GIVEN

		//Delta 1
		PrismObject<UserType> user = createUserFoo();
		ObjectDelta<UserType> userDelta1 = ObjectDelta.createAddDelta(user);

		//Delta 2
    	PrismContainerValue<AssignmentType> assignmentValue2 = new PrismContainerValue<>(PrismTestUtil.getPrismContext());
    	assignmentValue2.setId(USER_ASSIGNMENT_2_ID);
    	assignmentValue2.setPropertyRealValue(AssignmentType.F_DESCRIPTION, ASSIGNMENT_ABRAKADABRA_DESCRIPTION, PrismTestUtil.getPrismContext());
    	ContainerDelta<AssignmentType> containerDelta2 = ContainerDelta.createDelta(UserType.F_ASSIGNMENT,getUserTypeDefinition());
    	containerDelta2.addValueToAdd(assignmentValue2);

		// WHEN
        userDelta1.swallow(containerDelta2);

        // THEN
        System.out.println("Delta after swallow:");
        System.out.println(userDelta1.debugDump());
        assertEquals("Wrong OID", USER_FOO_OID, userDelta1.getOid());
        ContainerDelta<AssignmentType> containerDeltaAfter = userDelta1.findContainerDelta(UserType.F_ASSIGNMENT);
        assertNotNull("No assignment delta", containerDeltaAfter);
        PrismAsserts.assertNoDelete(containerDeltaAfter);
        PrismAsserts.assertNoReplace(containerDeltaAfter);
        Collection<PrismContainerValue<AssignmentType>> valuesToAdd = containerDeltaAfter.getValuesToAdd();
        assertEquals("Unexpected number of values to add", 1, valuesToAdd.size());
        assertTrue("Value "+assignmentValue2+" missing ", valuesToAdd.contains(assignmentValue2));
    }

	@Test
    public void testAddAssignmentActivationDifferentNullIdApplyToObject() throws Exception {
		final String TEST_NAME="testAddAssignmentActivationDifferentNullIdApplyToObject";
		displayTestTitle(TEST_NAME);
		// GIVEN

		// User
		PrismObject<UserType> user = createUserFooPatlama();

		//Delta
    	PrismContainerValue<ActivationType> activationValue = new PrismContainerValue<>(PrismTestUtil.getPrismContext());
    	// The value id is null
    	activationValue.setPropertyRealValue(ActivationType.F_ENABLED, true, PrismTestUtil.getPrismContext());

		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationAddContainer(UserType.class, USER_FOO_OID,
				new ItemPath(
						new NameItemPathSegment(UserType.F_ASSIGNMENT),
						// We really need ID here. Otherwise it would not be clear to which assignment to add
						new IdItemPathSegment(123L),
						new NameItemPathSegment(AssignmentType.F_ACTIVATION)),
				PrismTestUtil.getPrismContext(), activationValue);

		// WHEN
        userDelta.applyTo(user);

        // THEN
        System.out.println("User after delta application:");
        System.out.println(user.debugDump());
        assertEquals("Wrong OID", USER_FOO_OID, user.getOid());
        PrismAsserts.assertPropertyValue(user, UserType.F_ADDITIONAL_NAMES, PrismTestUtil.createPolyString("foobar"));
        PrismContainer<AssignmentType> assignment = user.findContainer(UserType.F_ASSIGNMENT);
        assertNotNull("No assignment", assignment);
        assertEquals("Unexpected number of assignment values", 1, assignment.size());

        // TODO
    }

	@Test
    public void testObjectDeltaApplyToAdd() throws Exception {
		final String TEST_NAME="testObjectDeltaApplyToAdd";
		displayTestTitle(TEST_NAME);
		// GIVEN
		PrismObject<UserType> user = PrismTestUtil.parseObject(USER_JACK_FILE_XML);
		//Delta
    	ObjectDelta<UserType> userDelta = ObjectDelta.createModificationAddProperty(UserType.class, USER_FOO_OID,
    			UserType.F_LOCALITY, PrismTestUtil.getPrismContext(), "Caribbean");

		// WHEN
    	userDelta.applyTo(user);

        // THEN

    	PrismAsserts.assertPropertyValue(user, UserType.F_LOCALITY, "Caribbean");
        user.checkConsistence();
    }

	@Test
    public void testObjectDeltaApplyToDelete() throws Exception {
		final String TEST_NAME="testObjectDeltaApplyToDelete";
		displayTestTitle(TEST_NAME);
		// GIVEN
		PrismObject<UserType> user = PrismTestUtil.parseObject(USER_JACK_FILE_XML);
		//Delta
    	ObjectDelta<UserType> userDelta = ObjectDelta.createModificationDeleteProperty(UserType.class, USER_FOO_OID,
    			UserType.F_ADDITIONAL_NAMES, PrismTestUtil.getPrismContext(), "Jackie");

		// WHEN
    	userDelta.applyTo(user);

        // THEN

    	PrismAsserts.assertPropertyValue(user, UserType.F_ADDITIONAL_NAMES, "Captain");
        user.checkConsistence();
    }

	@Test
    public void testObjectDeltaApplyToReplace() throws Exception {
		final String TEST_NAME="testObjectDeltaApplyToReplace";
		displayTestTitle(TEST_NAME);
		// GIVEN
		PrismObject<UserType> user = PrismTestUtil.parseObject(USER_JACK_FILE_XML);
		//Delta
    	ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class, USER_FOO_OID,
    			UserType.F_ADDITIONAL_NAMES, PrismTestUtil.getPrismContext(), "Cpt");

		// WHEN
    	userDelta.applyTo(user);

        // THEN

    	PrismAsserts.assertPropertyValue(user, UserType.F_ADDITIONAL_NAMES, "Cpt");
        user.checkConsistence();
    }

	@Test
    public void testObjectDeltaApplyToReplaceEmpty() throws Exception {
		final String TEST_NAME="testObjectDeltaApplyToReplaceEmpty";
		displayTestTitle(TEST_NAME);
		// GIVEN
		PrismObject<UserType> user = PrismTestUtil.parseObject(USER_JACK_FILE_XML);
		//Delta
    	ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class, USER_FOO_OID,
    			UserType.F_ADDITIONAL_NAMES, PrismTestUtil.getPrismContext());

		// WHEN
    	userDelta.applyTo(user);

        // THEN

    	PrismAsserts.assertNoItem(user, UserType.F_ADDITIONAL_NAMES);
        user.checkConsistence();
    }

	@Test
    public void testObjectDeltaFindItemDeltaModifyProperty() throws Exception {
		final String TEST_NAME="testObjectDeltaFindItemDeltaModifyProperty";
		displayTestTitle(TEST_NAME);
		// GIVEN

    	ObjectDelta<UserType> userDelta = createDeltaForFindItem();
    	ItemPath itemDeltaPath = new ItemPath(UserType.F_GIVEN_NAME);

		// WHEN
    	ItemDelta<PrismValue, ItemDefinition> itemDelta = userDelta.findItemDelta(itemDeltaPath);

        // THEN
    	PrismAsserts.assertInstanceOf(PropertyDelta.class, itemDelta);
    	assertEquals(itemDeltaPath, itemDelta.getPath());
    	PrismAsserts.assertPropertyValues("Wrong replace values in "+itemDelta,
    			((PropertyDelta)itemDelta).getValuesToReplace(), "Guybrush");
    }

	@Test
    public void testObjectDeltaFindItemDeltaModifyPropertyInContainer() throws Exception {
		final String TEST_NAME="testObjectDeltaFindItemDeltaModifyPropertyInContainer";
		displayTestTitle(TEST_NAME);
		// GIVEN
		ObjectDelta<UserType> userDelta = createDeltaForFindItem();
		System.out.println("Object delta:\n"+userDelta.debugDump());

		ItemPath itemDeltaPath = new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ENABLED);

		// WHEN
    	ItemDelta<PrismValue, ItemDefinition> itemDelta = userDelta.findItemDelta(itemDeltaPath);

        // THEN
    	System.out.println("Item delta:\n"+(itemDelta==null?"null":itemDelta.debugDump()));
    	PrismAsserts.assertInstanceOf(PropertyDelta.class, itemDelta);
    	assertEquals(itemDeltaPath, itemDelta.getPath());
    	PrismAsserts.assertPropertyValues("Wrong add values in "+itemDelta,
    			((PropertyDelta)itemDelta).getValuesToAdd(), Boolean.TRUE);
    }

	private ObjectDelta<UserType> createDeltaForFindItem() throws SchemaException {
		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationAddProperty(UserType.class, USER_FOO_OID,
    			UserType.F_LOCALITY, PrismTestUtil.getPrismContext(), "Caribbean");
    	userDelta.addModificationReplaceProperty(UserType.F_GIVEN_NAME, "Guybrush");

    	ContainerDelta<ActivationType> activationDelta = userDelta.createContainerModification(new ItemPath(UserType.F_ACTIVATION));
    	PrismContainerValue<ActivationType> activationCVal = new PrismContainerValue();
    	activationDelta.addValueToAdd(activationCVal);

    	PrismProperty<Boolean> enabledProperty = activationCVal.createProperty(ActivationType.F_ENABLED);
    	enabledProperty.setRealValue(Boolean.TRUE);

    	PrismProperty<XMLGregorianCalendar> validFromProperty = activationCVal.createProperty(ActivationType.F_VALID_FROM);
    	validFromProperty.setRealValue(XmlTypeConverter.createXMLGregorianCalendar(20016, 5, 16, 19, 8, 33));

    	userDelta.addModification(activationDelta);

    	return userDelta;
	}

	/**
	 * MODIFY/add + MODIFY/add
	 */
	@Test
    public void testObjectDeltaUnion01Simple() throws Exception {
		final String TEST_NAME="testObjectDeltaUnion01Simple";
		displayTestTitle(TEST_NAME);
		// GIVEN

		//Delta
    	ObjectDelta<UserType> userDelta1 = ObjectDelta.createModificationAddProperty(UserType.class, USER_FOO_OID,
    			UserType.F_FULL_NAME, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("baz"));
    	ObjectDelta<UserType> userDelta2 = ObjectDelta.createModificationAddProperty(UserType.class, USER_FOO_OID,
    			UserType.F_FULL_NAME, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("baz"));

		// WHEN
        ObjectDelta<UserType> userDeltaUnion = ObjectDelta.union(userDelta1, userDelta2);

        // THEN
        assertUnion01Delta(userDeltaUnion);
    }

	/**
	 * MODIFY/add + MODIFY/add
	 */
	@Test
    public void testObjectDeltaUnion01Metadata() throws Exception {
		final String TEST_NAME="testObjectDeltaUnion01Metadata";
		displayTestTitle(TEST_NAME);
		// GIVEN

    	ObjectDelta<UserType> userDelta1 = ObjectDelta.createModificationAddProperty(UserType.class, USER_FOO_OID,
    			UserType.F_FULL_NAME, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("baz"));


    	PropertyDelta<PolyString> fullNameDelta2 = PropertyDelta.createDelta(UserType.F_FULL_NAME, UserType.class,
    			PrismTestUtil.getPrismContext());
    	PrismPropertyValue<PolyString> fullNameValue2 = new PrismPropertyValue<>(PrismTestUtil.createPolyString("baz"));
    	// Set some metadata to spoil usual equals
    	fullNameValue2.setOriginType(OriginType.OUTBOUND);
    	fullNameDelta2.addValueToAdd(fullNameValue2);
    	ObjectDelta<UserType> userDelta2 = ObjectDelta.createModifyDelta(USER_FOO_OID, fullNameDelta2, UserType.class,
    			PrismTestUtil.getPrismContext());

		// WHEN
        ObjectDelta<UserType> userDeltaUnion = ObjectDelta.union(userDelta1, userDelta2);

        // THEN
        assertUnion01Delta(userDeltaUnion);
    }

	private void assertUnion01Delta(ObjectDelta<UserType> userDeltaUnion) {
		PropertyDelta<PolyString> fullNameDeltaUnion = getCheckedPropertyDeltaFromUnion(userDeltaUnion);
        Collection<PrismPropertyValue<PolyString>> valuesToAdd = fullNameDeltaUnion.getValuesToAdd();
        assertNotNull("No valuesToAdd in fullName delta after union", valuesToAdd);
        assertEquals("Unexpected size of valuesToAdd in fullName delta after union", 1, valuesToAdd.size());
        PrismPropertyValue<PolyString> valueToAdd = valuesToAdd.iterator().next();
        assertEquals("Unexcted value in valuesToAdd in fullName delta after union",
        		PrismTestUtil.createPolyString("baz"), valueToAdd.getValue());
	}

	/**
	 * MODIFY/replace + MODIFY/replace
	 */
	@Test
    public void testObjectDeltaUnion02() throws Exception {
		final String TEST_NAME="testObjectDeltaUnion02";
		displayTestTitle(TEST_NAME);
		// GIVEN

		//Delta
    	ObjectDelta<UserType> userDelta1 = ObjectDelta.createModificationReplaceProperty(UserType.class, USER_FOO_OID,
    			UserType.F_FULL_NAME, PrismTestUtil.getPrismContext());
    	ObjectDelta<UserType> userDelta2 = ObjectDelta.createModificationReplaceProperty(UserType.class, USER_FOO_OID,
    			UserType.F_FULL_NAME, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("baz"));

		// WHEN
        ObjectDelta<UserType> userDeltaUnion = ObjectDelta.union(userDelta1, userDelta2);

        // THEN
        PropertyDelta<PolyString> fullNameDeltaUnion = getCheckedPropertyDeltaFromUnion(userDeltaUnion);
        Collection<PrismPropertyValue<PolyString>> valuesToReplace = fullNameDeltaUnion.getValuesToReplace();
        assertNotNull("No valuesToReplace in fullName delta after union", valuesToReplace);
        assertEquals("Unexpected size of valuesToReplace in fullName delta after union", 1, valuesToReplace.size());
        PrismPropertyValue<PolyString> valueToReplace = valuesToReplace.iterator().next();
        assertEquals("Unexcted value in valueToReplace in fullName delta after union",
        		PrismTestUtil.createPolyString("baz"), valueToReplace.getValue());
    }

	/**
	 * MODIFY/replace + MODIFY/add
	 */
	@Test
    public void testObjectDeltaUnion03() throws Exception {
		final String TEST_NAME="testObjectDeltaUnion03";
		displayTestTitle(TEST_NAME);
		// GIVEN

		//Delta
    	ObjectDelta<UserType> userDelta1 = ObjectDelta.createModificationReplaceProperty(UserType.class, USER_FOO_OID,
    			UserType.F_FULL_NAME, PrismTestUtil.getPrismContext());
    	ObjectDelta<UserType> userDelta2 = ObjectDelta.createModificationAddProperty(UserType.class, USER_FOO_OID,
    			UserType.F_FULL_NAME, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("baz"));

		// WHEN
        ObjectDelta<UserType> userDeltaUnion = ObjectDelta.union(userDelta1, userDelta2);

        // THEN
        PropertyDelta<PolyString> fullNameDeltaUnion = getCheckedPropertyDeltaFromUnion(userDeltaUnion);
        Collection<PrismPropertyValue<PolyString>> valuesToReplace = fullNameDeltaUnion.getValuesToReplace();
        assertNotNull("No valuesToReplace in fullName delta after union", valuesToReplace);
        assertEquals("Unexpected size of valuesToReplace in fullName delta after union", 1, valuesToReplace.size());
        PrismPropertyValue<PolyString> valueToReplace = valuesToReplace.iterator().next();
        assertEquals("Unexcted value in valueToReplace in fullName delta after union",
        		PrismTestUtil.createPolyString("baz"), valueToReplace.getValue());
    }

	private PropertyDelta<PolyString> getCheckedPropertyDeltaFromUnion(ObjectDelta<UserType> userDeltaUnion) {
		userDeltaUnion.checkConsistence();
        assertEquals("Wrong OID", USER_FOO_OID, userDeltaUnion.getOid());
        PrismAsserts.assertIsModify(userDeltaUnion);
        PrismAsserts.assertModifications(userDeltaUnion, 1);
        PropertyDelta<PolyString> fullNameDeltaUnion = userDeltaUnion.findPropertyDelta(UserType.F_FULL_NAME);
        assertNotNull("No fullName delta after union", fullNameDeltaUnion);
        return fullNameDeltaUnion;
	}

	@Test
    public void testObjectDeltaSummarizeModifyAdd() throws Exception {
		final String TEST_NAME="testObjectDeltaSummarizeModifyAdd";
		displayTestTitle(TEST_NAME);
		// GIVEN

    	ObjectDelta<UserType> userDelta1 = ObjectDelta.createModificationAddProperty(UserType.class, USER_FOO_OID,
    			UserType.F_ADDITIONAL_NAMES, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("foo"));
    	ObjectDelta<UserType> userDelta2 = ObjectDelta.createModificationAddProperty(UserType.class, USER_FOO_OID,
    			UserType.F_ADDITIONAL_NAMES, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("bar"));

		// WHEN
        ObjectDelta<UserType> userDeltaSum = ObjectDelta.summarize(userDelta1, userDelta2);

        // THEN
        assertEquals("Wrong OID", USER_FOO_OID, userDeltaSum.getOid());
        PrismAsserts.assertIsModify(userDeltaSum);
        PrismAsserts.assertModifications(userDeltaSum, 1);
        PropertyDelta<PolyString> namesDeltaUnion = userDeltaSum.findPropertyDelta(UserType.F_ADDITIONAL_NAMES);
        assertNotNull("No additionalNames delta after summarize", namesDeltaUnion);
        PrismAsserts.assertAdd(namesDeltaUnion, PrismTestUtil.createPolyString("foo"), PrismTestUtil.createPolyString("bar"));
    }

	@Test
    public void testObjectDeltaSummarizeModifyReplace() throws Exception {
		final String TEST_NAME="testObjectDeltaSummarizeModifyReplace";
		displayTestTitle(TEST_NAME);
		// GIVEN

    	ObjectDelta<UserType> userDelta1 = ObjectDelta.createModificationReplaceProperty(UserType.class, USER_FOO_OID,
    			UserType.F_FULL_NAME, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("foo"));
    	ObjectDelta<UserType> userDelta2 = ObjectDelta.createModificationReplaceProperty(UserType.class, USER_FOO_OID,
    			UserType.F_FULL_NAME, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("bar"));

		// WHEN
        ObjectDelta<UserType> userDeltaSum = ObjectDelta.summarize(userDelta1, userDelta2);

        // THEN
        assertEquals("Wrong OID", USER_FOO_OID, userDeltaSum.getOid());
        PrismAsserts.assertIsModify(userDeltaSum);
        PrismAsserts.assertModifications(userDeltaSum, 1);
        PropertyDelta<PolyString> fullNameDeltaUnion = userDeltaSum.findPropertyDelta(UserType.F_FULL_NAME);
        assertNotNull("No fullName delta after summarize", fullNameDeltaUnion);
        PrismAsserts.assertReplace(fullNameDeltaUnion, PrismTestUtil.createPolyString("bar"));
    }

	@Test
    public void testObjectDeltaSummarizeModifyMix() throws Exception {
		final String TEST_NAME="testObjectDeltaSummarizeModifyMix";
		displayTestTitle(TEST_NAME);
		// GIVEN

    	ObjectDelta<UserType> userDelta1 = ObjectDelta.createModificationAddProperty(UserType.class, USER_FOO_OID,
    			UserType.F_ADDITIONAL_NAMES, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("baz"));
    	ObjectDelta<UserType> userDelta2 = ObjectDelta.createModificationReplaceProperty(UserType.class, USER_FOO_OID,
    			UserType.F_ADDITIONAL_NAMES, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("foo"));
    	ObjectDelta<UserType> userDelta3 = ObjectDelta.createModificationAddProperty(UserType.class, USER_FOO_OID,
    			UserType.F_ADDITIONAL_NAMES, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("bar"));

		// WHEN
        ObjectDelta<UserType> userDeltaSum = ObjectDelta.summarize(userDelta1, userDelta2, userDelta3);

        // THEN
        assertEquals("Wrong OID", USER_FOO_OID, userDeltaSum.getOid());
        PrismAsserts.assertIsModify(userDeltaSum);
        PrismAsserts.assertModifications(userDeltaSum, 1);
        PropertyDelta<PolyString> namesDeltaUnion = userDeltaSum.findPropertyDelta(UserType.F_ADDITIONAL_NAMES);
        assertNotNull("No additionalNames delta after summarize", namesDeltaUnion);
        PrismAsserts.assertReplace(namesDeltaUnion, PrismTestUtil.createPolyString("foo"), PrismTestUtil.createPolyString("bar"));
    }

	@Test
    public void testObjectDeltaSummarizeAddModifyMix() throws Exception {
		final String TEST_NAME="testObjectDeltaSummarizeAddModifyMix";
		displayTestTitle(TEST_NAME);
		// GIVEN

		PrismObject<UserType> user = createUserFooPatlama();
		ObjectDelta<UserType> userDelta0 = ObjectDelta.createAddDelta(user);
    	ObjectDelta<UserType> userDelta1 = ObjectDelta.createModificationAddProperty(UserType.class, USER_FOO_OID,
    			UserType.F_ADDITIONAL_NAMES, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("baz"));
    	ObjectDelta<UserType> userDelta2 = ObjectDelta.createModificationReplaceProperty(UserType.class, USER_FOO_OID,
    			UserType.F_ADDITIONAL_NAMES, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("foo"));
    	ObjectDelta<UserType> userDelta3 = ObjectDelta.createModificationAddProperty(UserType.class, USER_FOO_OID,
    			UserType.F_ADDITIONAL_NAMES, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("bar"));

		// WHEN
        ObjectDelta<UserType> userDeltaSum = ObjectDelta.summarize(userDelta0, userDelta1, userDelta2, userDelta3);

        // THEN
        assertEquals("Wrong OID", USER_FOO_OID, userDeltaSum.getOid());
        PrismAsserts.assertIsAdd(userDeltaSum);
        PrismObject<UserType> userSum = userDeltaSum.getObjectToAdd();
        assert user != userSum : "User was not clonned";
        PrismAsserts.assertPropertyValue(userSum, UserType.F_ADDITIONAL_NAMES,
        		PrismTestUtil.createPolyString("foo"), PrismTestUtil.createPolyString("bar"));
        // TODO
    }

    @Test
    public void testObjectDeltaSummarizeAddModifySameRefValues() throws Exception {
    	final String TEST_NAME="testObjectDeltaSummarizeAddModifySameRefValues";
		displayTestTitle(TEST_NAME);
        // GIVEN

        PrismObjectDefinition<UserType> userDef = getUserTypeDefinition();

        PrismObject<UserType> user = userDef.instantiate();
        user.setOid(USER_FOO_OID);
        user.setPropertyRealValue(UserType.F_NAME, PrismTestUtil.createPolyString("foo"));
        PrismReference parentOrgRef = user.findOrCreateReference(UserType.F_PARENT_ORG_REF);
        parentOrgRef.add(new PrismReferenceValue("oid1"));

        ObjectDelta<UserType> userDelta0 = ObjectDelta.createAddDelta(user);
        ObjectDelta<UserType> userDelta1 = ObjectDelta.createModificationAddReference(UserType.class, USER_FOO_OID,
                UserType.F_PARENT_ORG_REF, PrismTestUtil.getPrismContext(), "oid1");

        System.out.println("userDelta0 = " + userDelta0.debugDump());
        System.out.println("userDelta1 = " + userDelta1.debugDump());

        // WHEN
        ObjectDelta<UserType> userDeltaSum = ObjectDelta.summarize(userDelta0, userDelta1);

        System.out.println("userDeltaSum = " + userDeltaSum.debugDump());

        // THEN
        assertEquals("Wrong OID", USER_FOO_OID, userDeltaSum.getOid());
        PrismAsserts.assertIsAdd(userDeltaSum);
        PrismObject<UserType> userSum = userDeltaSum.getObjectToAdd();
        assert user != userSum : "User was not cloned";
        PrismAsserts.assertReferenceValues(userSum.findOrCreateReference(UserType.F_PARENT_ORG_REF), "oid1");
    }
	@Test
    public void testDeltaComplex() throws Exception {
		final String TEST_NAME="testDeltaComplex";
		displayTestTitle(TEST_NAME);
		// GIVEN

    	ObjectDelta<UserType> delta = ObjectDelta.createModificationAddProperty(UserType.class, USER_FOO_OID,
    			UserType.F_FULL_NAME, PrismTestUtil.getPrismContext(), PrismTestUtil.createPolyString("Foo Bar"));

    	PrismObjectDefinition<UserType> userTypeDefinition = getUserTypeDefinition();

    	PrismContainerDefinition<ActivationType> activationDefinition = userTypeDefinition.findContainerDefinition(UserType.F_ACTIVATION);
    	PrismContainer<ActivationType> activationContainer = activationDefinition.instantiate();
    	PrismPropertyDefinition enabledDef = activationDefinition.findPropertyDefinition(ActivationType.F_ENABLED);
    	PrismProperty<Boolean> enabledProperty = enabledDef.instantiate();
    	enabledProperty.setRealValue(true);
    	activationContainer.add(enabledProperty);
    	delta.addModificationDeleteContainer(UserType.F_ACTIVATION, activationContainer.getValue().clone());

    	PrismContainerDefinition<AssignmentType> assDef = userTypeDefinition.findContainerDefinition(UserType.F_ASSIGNMENT);
    	PrismPropertyDefinition descDef = assDef.findPropertyDefinition(AssignmentType.F_DESCRIPTION);

    	PrismContainerValue<AssignmentType> assVal1 = new PrismContainerValue<>(PrismTestUtil.getPrismContext());
    	assVal1.setId(111L);
    	PrismProperty<String> descProp1 = descDef.instantiate();
    	descProp1.setRealValue("desc 1");
    	assVal1.add(descProp1);

    	PrismContainerValue<AssignmentType> assVal2 = new PrismContainerValue<>(PrismTestUtil.getPrismContext());
    	assVal2.setId(222L);
    	PrismProperty<String> descProp2 = descDef.instantiate();
    	descProp2.setRealValue("desc 2");
    	assVal2.add(descProp2);

    	delta.addModificationAddContainer(UserType.F_ASSIGNMENT, assVal1, assVal2);

		System.out.println("Delta:");
		System.out.println(delta.debugDump());

		// WHEN, THEN
		PrismInternalTestUtil.assertVisitor(delta, 14);

		PrismInternalTestUtil.assertPathVisitor(delta, new ItemPath(UserType.F_FULL_NAME), true, 2);
		PrismInternalTestUtil.assertPathVisitor(delta, new ItemPath(UserType.F_ACTIVATION), true, 4);
		PrismInternalTestUtil.assertPathVisitor(delta, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ENABLED), true, 2);
		PrismInternalTestUtil.assertPathVisitor(delta, new ItemPath(UserType.F_ASSIGNMENT), true, 7);
		PrismInternalTestUtil.assertPathVisitor(delta, new ItemPath(
				new NameItemPathSegment(UserType.F_ASSIGNMENT),
				IdItemPathSegment.WILDCARD), true, 6);

		PrismInternalTestUtil.assertPathVisitor(delta, new ItemPath(UserType.F_FULL_NAME), false, 1);
		PrismInternalTestUtil.assertPathVisitor(delta, new ItemPath(UserType.F_ACTIVATION), false, 1);
		PrismInternalTestUtil.assertPathVisitor(delta, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ENABLED), false, 1);
		PrismInternalTestUtil.assertPathVisitor(delta, new ItemPath(UserType.F_ASSIGNMENT), false, 1);
		PrismInternalTestUtil.assertPathVisitor(delta, new ItemPath(
				new NameItemPathSegment(UserType.F_ASSIGNMENT),
				IdItemPathSegment.WILDCARD), false, 2);
    }


	@Test
    public void testPropertyDeltaNarrow01() throws Exception {
		final String TEST_NAME="testPropertyDeltaNarrow01";
		displayTestTitle(TEST_NAME);

		// GIVEN
		PrismPropertyDefinition propertyDefinition = new PrismPropertyDefinitionImpl(UserType.F_DESCRIPTION,
				DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());

		PropertyDelta<String> delta = new PropertyDelta<String>(propertyDefinition, PrismTestUtil.getPrismContext());
		delta.addValueToAdd(new PrismPropertyValue<>("blabla"));
		delta.addValueToAdd(new PrismPropertyValue<>("bubu"));

		PrismObject<UserType> user = createUserFoo();

		// WHEN
		PropertyDelta<String> narrowedDelta = delta.narrow(user);

		// THEN
		System.out.println("Narrowed delta:");
		System.out.println(narrowedDelta.debugDump());

		PrismAsserts.assertNoReplace(narrowedDelta);
		PrismAsserts.assertAdd(narrowedDelta, "blabla", "bubu");
		PrismAsserts.assertNoDelete(narrowedDelta);
	}

	@Test
    public void testPropertyDeltaNarrow02() throws Exception {
		final String TEST_NAME="testPropertyDeltaNarrow02";
		displayTestTitle(TEST_NAME);

		// GIVEN
		PrismPropertyDefinition propertyDefinition = new PrismPropertyDefinitionImpl(UserType.F_DESCRIPTION,
				DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());

		PropertyDelta<String> delta = new PropertyDelta<String>(propertyDefinition, PrismTestUtil.getPrismContext());
		delta.addValueToAdd(new PrismPropertyValue<>("blabla"));
		delta.addValueToAdd(new PrismPropertyValue<>("bubu"));

		PrismObject<UserType> user = createUserFoo();
		user.setPropertyRealValue(UserType.F_DESCRIPTION, "bubu");

		// WHEN
		PropertyDelta<String> narrowedDelta = delta.narrow(user);

		// THEN
		System.out.println("Narrowed delta:");
		System.out.println(narrowedDelta.debugDump());

		PrismAsserts.assertNoReplace(narrowedDelta);
		PrismAsserts.assertAdd(narrowedDelta, "blabla");
		PrismAsserts.assertNoDelete(narrowedDelta);
	}

	@Test
    public void testPropertyDeltaNarrow03() throws Exception {
		final String TEST_NAME="testPropertyDeltaNarrow03";
		displayTestTitle(TEST_NAME);

		// GIVEN
		PrismPropertyDefinition propertyDefinition = new PrismPropertyDefinitionImpl(UserType.F_DESCRIPTION,
				DOMUtil.XSD_STRING, PrismTestUtil.getPrismContext());

		PropertyDelta<String> delta = new PropertyDelta<String>(propertyDefinition, PrismTestUtil.getPrismContext());
		delta.addValueToAdd(new PrismPropertyValue<>("bubu"));

		PrismObject<UserType> user = createUserFoo();
		user.setPropertyRealValue(UserType.F_DESCRIPTION, "bubu");

		// WHEN
		PropertyDelta<String> narrowedDelta = delta.narrow(user);

		// THEN
		System.out.println("Narrowed delta:");
		System.out.println(narrowedDelta.debugDump());

		PrismAsserts.assertNoReplace(narrowedDelta);
		PrismAsserts.assertNoAdd(narrowedDelta);
		PrismAsserts.assertNoDelete(narrowedDelta);
		assertTrue("Delta not empty", narrowedDelta.isEmpty());
	}
	
	@Test
    public void testObjectDeltaNarrow01() throws Exception {
		final String TEST_NAME="testObjectDeltaNarrow01";
		displayTestTitle(TEST_NAME);

		// GIVEN

		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationAddProperty(UserType.class, USER_FOO_OID,
				UserType.F_ADDITIONAL_NAMES, getPrismContext(), "blabla", "bubu");
		display("userDelta", userDelta);

		PrismObject<UserType> user = createUserFoo();
		display("user", user);

		// WHEN
		displayWhen(TEST_NAME);
		ObjectDelta<UserType> narrowedDelta = userDelta.narrow(user);

		// THEN
		displayThen(TEST_NAME);
		display("Narrowed delta", narrowedDelta);

		PrismAsserts.assertIsModify(narrowedDelta);
        PrismAsserts.assertModifications(narrowedDelta, 1);
        PropertyDelta<String> narrowedAdditionalNamesDelta = narrowedDelta.findPropertyDelta(UserType.F_ADDITIONAL_NAMES);
        assertNotNull("No additionalNames delta after summarize", narrowedAdditionalNamesDelta);
		PrismAsserts.assertNoReplace(narrowedAdditionalNamesDelta);
        PrismAsserts.assertAdd(narrowedAdditionalNamesDelta, "blabla", "bubu");
		PrismAsserts.assertNoDelete(narrowedAdditionalNamesDelta);
	}

	@Test
    public void testObjectDeltaNarrow02() throws Exception {
		final String TEST_NAME="testObjectDeltaNarrow02";
		displayTestTitle(TEST_NAME);

		// GIVEN

		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationAddProperty(UserType.class, USER_FOO_OID,
				UserType.F_ADDITIONAL_NAMES, getPrismContext(), "blabla", "bubu");
		display("userDelta", userDelta);

		PrismObject<UserType> user = createUserFoo();
		user.setPropertyRealValue(UserType.F_ADDITIONAL_NAMES, "bubu");
		display("user", user);

		// WHEN
		displayWhen(TEST_NAME);
		ObjectDelta<UserType> narrowedDelta = userDelta.narrow(user);

		// THEN
		displayThen(TEST_NAME);
		display("Narrowed delta", narrowedDelta);

		PrismAsserts.assertIsModify(narrowedDelta);
        PrismAsserts.assertModifications(narrowedDelta, 1);
        PropertyDelta<String> narrowedAdditionalNamesDelta = narrowedDelta.findPropertyDelta(UserType.F_ADDITIONAL_NAMES);
        assertNotNull("No additionalNames delta after summarize", narrowedAdditionalNamesDelta);
		PrismAsserts.assertNoReplace(narrowedAdditionalNamesDelta);
        PrismAsserts.assertAdd(narrowedAdditionalNamesDelta, "blabla");
		PrismAsserts.assertNoDelete(narrowedAdditionalNamesDelta);
	}

	@Test
    public void testObjectDeltaNarrow03() throws Exception {
		final String TEST_NAME="testObjectDeltaNarrow03";
		displayTestTitle(TEST_NAME);

		// GIVEN

		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationAddProperty(UserType.class, USER_FOO_OID,
				UserType.F_ADDITIONAL_NAMES, getPrismContext(), "blabla", "bubu");
		display("userDelta", userDelta);

		PrismObject<UserType> user = createUserFoo();
		user.setPropertyRealValues(UserType.F_ADDITIONAL_NAMES, "bubu", "blabla");
		display("user", user);

		// WHEN
		displayWhen(TEST_NAME);
		ObjectDelta<UserType> narrowedDelta = userDelta.narrow(user);

		// THEN
		displayThen(TEST_NAME);
		display("Narrowed delta", narrowedDelta);

		PrismAsserts.assertIsModify(narrowedDelta);
        PrismAsserts.assertModifications(narrowedDelta, 0);
	}
	
	@Test
    public void testObjectDeltaNarrowAssignmen01() throws Exception {
		final String TEST_NAME="testObjectDeltaNarrowAssignmen01";
		displayTestTitle(TEST_NAME);

		// GIVEN

		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationDeleteContainer(UserType.class, USER_FOO_OID, 
				UserType.F_ASSIGNMENT, getPrismContext(), createAssignmentValue(null, ASSIGNMENT_PATLAMA_DESCRIPTION));
		display("userDelta", userDelta);

		PrismObject<UserType> user = createUserFoo();
		addAssignment(user, ASSIGNMENT_ABRAKADABRA_ID, ASSIGNMENT_ABRAKADABRA_DESCRIPTION);
		display("user", user);

		// WHEN
		displayWhen(TEST_NAME);
		ObjectDelta<UserType> narrowedDelta = userDelta.narrow(user);

		// THEN
		displayThen(TEST_NAME);
		display("Narrowed delta", narrowedDelta);

		PrismAsserts.assertIsModify(narrowedDelta);
        PrismAsserts.assertModifications(narrowedDelta, 0);
	}
	
	@Test
    public void testObjectDeltaNarrowAssignmen02() throws Exception {
		final String TEST_NAME="testObjectDeltaNarrowAssignmen02";
		displayTestTitle(TEST_NAME);

		// GIVEN

		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationDeleteContainer(UserType.class, USER_FOO_OID, 
				UserType.F_ASSIGNMENT, getPrismContext(), createAssignmentValue(ASSIGNMENT_PATLAMA_ID, null));
		display("userDelta", userDelta);

		PrismObject<UserType> user = createUserFoo();
		addAssignment(user, ASSIGNMENT_ABRAKADABRA_ID, ASSIGNMENT_ABRAKADABRA_DESCRIPTION);
		display("user", user);

		// WHEN
		displayWhen(TEST_NAME);
		ObjectDelta<UserType> narrowedDelta = userDelta.narrow(user);

		// THEN
		displayThen(TEST_NAME);
		display("Narrowed delta", narrowedDelta);

		PrismAsserts.assertIsModify(narrowedDelta);
        PrismAsserts.assertModifications(narrowedDelta, 0);
	}
	
	@Test
    public void testObjectDeltaNarrowAssignmen11() throws Exception {
		final String TEST_NAME="testObjectDeltaNarrowAssignmen11";
		displayTestTitle(TEST_NAME);

		// GIVEN

		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationDeleteContainer(UserType.class, USER_FOO_OID, 
				UserType.F_ASSIGNMENT, getPrismContext(), createAssignmentValue(null, ASSIGNMENT_PATLAMA_DESCRIPTION));
		display("userDelta", userDelta);

		PrismObject<UserType> user = createUserFoo();
		addAssignment(user, ASSIGNMENT_PATLAMA_ID, ASSIGNMENT_PATLAMA_DESCRIPTION);
		addAssignment(user, ASSIGNMENT_ABRAKADABRA_ID, ASSIGNMENT_ABRAKADABRA_DESCRIPTION);
		display("user", user);

		// WHEN
		displayWhen(TEST_NAME);
		ObjectDelta<UserType> narrowedDelta = userDelta.narrow(user);

		// THEN
		displayThen(TEST_NAME);
		display("Narrowed delta", narrowedDelta);

		PrismAsserts.assertIsModify(narrowedDelta);
        PrismAsserts.assertModifications(narrowedDelta, 1);
        assertAssignmentDelete(narrowedDelta, 1);
        assertAssignmentDelete(narrowedDelta, null, ASSIGNMENT_PATLAMA_DESCRIPTION);
        assertAssignmentAdd(narrowedDelta, 0);
        assertAssignmentReplace(narrowedDelta, 0);
	}
	
	@Test
    public void testObjectDeltaNarrowAssignmen12() throws Exception {
		final String TEST_NAME="testObjectDeltaNarrowAssignmen12";
		displayTestTitle(TEST_NAME);

		// GIVEN

		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationDeleteContainer(UserType.class, USER_FOO_OID, 
				UserType.F_ASSIGNMENT, getPrismContext(), createAssignmentValue(ASSIGNMENT_PATLAMA_ID, null));
		display("userDelta", userDelta);

		PrismObject<UserType> user = createUserFoo();
		addAssignment(user, ASSIGNMENT_PATLAMA_ID, ASSIGNMENT_PATLAMA_DESCRIPTION);
		addAssignment(user, ASSIGNMENT_ABRAKADABRA_ID, ASSIGNMENT_ABRAKADABRA_DESCRIPTION);
		display("user", user);

		// WHEN
		displayWhen(TEST_NAME);
		ObjectDelta<UserType> narrowedDelta = userDelta.narrow(user);

		// THEN
		displayThen(TEST_NAME);
		display("Narrowed delta", narrowedDelta);

		PrismAsserts.assertIsModify(narrowedDelta);
        PrismAsserts.assertModifications(narrowedDelta, 1);
        assertAssignmentDelete(narrowedDelta, 1);
        assertAssignmentDelete(narrowedDelta, ASSIGNMENT_PATLAMA_ID, null);
        assertAssignmentAdd(narrowedDelta, 0);
        assertAssignmentReplace(narrowedDelta, 0);
	}

}
