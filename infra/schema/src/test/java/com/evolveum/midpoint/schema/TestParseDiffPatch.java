/*
 * Copyright (c) 2010-2017 Evolveum
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

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * @author semancik
 *
 */
public class TestParseDiffPatch {

	private static final String TEST_DIR = "src/test/resources/diff/";

	private static final File USER_BEFORE_FILE = new File(TEST_DIR, "user-before.xml");
	private static final File USER_AFTER_FILE = new File(TEST_DIR, "user-after.xml");
	private static final File TASK_BEFORE_FILE = new File(TEST_DIR, "task-before.xml");
	private static final File TASK_AFTER_FILE = new File(TEST_DIR, "task-after.xml");
	private static final File RESOURCE_BEFORE_FILE = new File(TEST_DIR, "resource-before.xml");
	private static final File RESOURCE_AFTER_FILE = new File(TEST_DIR, "resource-after.xml");
	private static final File RESOURCE_AFTER_CONST_FILE = new File(TEST_DIR, "resource-after-const.xml");
	private static final File RESOURCE_AFTER_NS_CHANGE_FILE = new File(TEST_DIR, "resource-after-ns-change.xml");
	private static final String RESOURCE_OID = "ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

    @Test
    public void testUserCredentialsDiff() throws Exception {
        System.out.println("===[ testUserCredentialsDiff ]===");

        PrismObject<UserType> userBefore = PrismTestUtil.parseObject(USER_BEFORE_FILE);
        userBefore.checkConsistence();
        PrismObject<UserType> userAfter = PrismTestUtil.parseObject(USER_AFTER_FILE);
        userAfter.checkConsistence();

        ObjectDelta<UserType> userDelta = userBefore.diff(userAfter);
        System.out.println("DELTA:");
        System.out.println(userDelta.debugDump());

        userBefore.checkConsistence();
        userAfter.checkConsistence();
        userDelta.checkConsistence();
        userDelta.assertDefinitions();

        ItemPath path = new ItemPath(SchemaConstantsGenerated.C_CREDENTIALS,
                CredentialsType.F_PASSWORD, PasswordType.F_FAILED_LOGINS);
        PrismAsserts.assertPropertyAdd(userDelta, path, 1);
        path = new ItemPath(SchemaConstantsGenerated.C_CREDENTIALS,
        		CredentialsType.F_PASSWORD, PasswordType.F_FAILED_LOGINS);
        PropertyDelta propertyDelta = userDelta.findPropertyDelta(path);
        assertNotNull("Property delta for "+path+" not found",propertyDelta);
        assertEquals(1, propertyDelta.getValuesToAdd().size());
    }

    //@Test
    public void testAssignmentActivationDiff() throws Exception {
        System.out.println("===[ testUserCredentialsDiff ]===");

        PrismObject<UserType> userBefore = PrismTestUtil.parseObject(USER_BEFORE_FILE);
        PrismObject<UserType> userAfter = userBefore.clone();
        AssignmentType assignmentBefore = new AssignmentType();
        ActivationType activation = new ActivationType();
        activation.setAdministrativeStatus(ActivationStatusType.DISABLED);
        assignmentBefore.setActivation(activation);
        userBefore.asObjectable().getAssignment().add(assignmentBefore);

        AssignmentType assignmentAfter = new AssignmentType();
        activation = new ActivationType();
        activation.setAdministrativeStatus(ActivationStatusType.ENABLED);
        assignmentAfter.setActivation(activation);
        userAfter.asObjectable().getAssignment().add(assignmentAfter);


        Collection<? extends ItemDelta> userDelta = assignmentBefore.asPrismContainerValue().diff(assignmentAfter.asPrismContainerValue());
//        ObjectDelta<UserType> userDelta = userBefore.diff(userAfter);
        System.out.println("DELTA:");
//        System.out.println(userDelta.debugDump());

//        userBefore.checkConsistence();
//        userAfter.checkConsistence();
//        userDelta.checkConsistence();
//        userDelta.assertDefinitions();

        ItemDelta assignmentDelta = userDelta.iterator().next();
        System.out.println("Assignment delta: " + assignmentDelta);
        System.out.println("Assignment delta: " + assignmentDelta.debugDump());

        ItemPath path = new ItemPath(SchemaConstantsGenerated.C_ASSIGNMENT,
                AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS);
//        PrismAsserts.assertPropertyAdd(assignmentDelta, path, 1);
//        path = new ItemPath(SchemaConstantsGenerated.C_CREDENTIALS,
//        		CredentialsType.F_PASSWORD, PasswordType.F_FAILED_LOGINS);
        PropertyDelta propertyDelta = ItemDelta.findPropertyDelta(userDelta, path);
        assertNotNull("Property delta for "+path+" not found",propertyDelta);
//        assertEquals(1, propertyDelta.getValuesToAdd().size());

        assignmentAfter = new AssignmentType();
        activation = new ActivationType();
        activation.setAdministrativeStatus(null);
        assignmentAfter.setActivation(activation);
        userDelta = assignmentBefore.asPrismContainerValue().diff(assignmentAfter.asPrismContainerValue());
//      ObjectDelta<UserType> userDelta = userBefore.diff(userAfter);
      System.out.println("DELTA:");
//      System.out.println(userDelta.debugDump());

//      userBefore.checkConsistence();
//      userAfter.checkConsistence();
//      userDelta.checkConsistence();
//      userDelta.assertDefinitions();

      assignmentDelta = userDelta.iterator().next();
      System.out.println("Assignment delta: " + assignmentDelta);
      System.out.println("Assignment delta: " + assignmentDelta.debugDump());

      path = new ItemPath(SchemaConstantsGenerated.C_ASSIGNMENT,
              AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS);
//      PrismAsserts.assertPropertyAdd(assignmentDelta, path, 1);
//      path = new ItemPath(SchemaConstantsGenerated.C_CREDENTIALS,
//      		CredentialsType.F_PASSWORD, PasswordType.F_FAILED_LOGINS);
      propertyDelta = ItemDelta.findPropertyDelta(userDelta, path);



      userDelta = assignmentAfter.asPrismContainerValue().diff(assignmentBefore.asPrismContainerValue());
//    ObjectDelta<UserType> userDelta = userBefore.diff(userAfter);
    System.out.println("DELTA:");
//    System.out.println(userDelta.debugDump());

//    userBefore.checkConsistence();
//    userAfter.checkConsistence();
//    userDelta.checkConsistence();
//    userDelta.assertDefinitions();

    assignmentDelta = userDelta.iterator().next();
    System.out.println("Assignment delta: " + assignmentDelta);
    System.out.println("Assignment delta: " + assignmentDelta.debugDump());

    path = new ItemPath(SchemaConstantsGenerated.C_ASSIGNMENT,
            AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS);
//    PrismAsserts.assertPropertyAdd(assignmentDelta, path, 1);
//    path = new ItemPath(SchemaConstantsGenerated.C_CREDENTIALS,
//    		CredentialsType.F_PASSWORD, PasswordType.F_FAILED_LOGINS);
    propertyDelta = ItemDelta.findPropertyDelta(userDelta, path);

    }

	@Test
	public void testUser() throws SchemaException, SAXException, IOException, JAXBException {
		System.out.println("===[ testUser ]===");

        PrismObject<UserType> userBefore = PrismTestUtil.parseObject(new File(TEST_DIR, "user-jack-before.xml"));
        userBefore.checkConsistence();
        PrismObject<UserType> userAfter = PrismTestUtil.parseObject(new File(TEST_DIR, "user-jack-after.xml"));
        userAfter.checkConsistence();

        // sanity
        assertFalse("Equals does not work", userBefore.equals(userAfter));

        // WHEN

        ObjectDelta<UserType> userDelta = userBefore.diff(userAfter);

        // THEN

        System.out.println("DELTA:");
        System.out.println(userDelta.debugDump());

        userBefore.checkConsistence();
        userAfter.checkConsistence();
        userDelta.checkConsistence();
        userDelta.assertDefinitions();

        assertEquals("Wrong delta OID", userBefore.getOid(), userDelta.getOid());
        assertEquals("Wrong change type", ChangeType.MODIFY, userDelta.getChangeType());
        Collection<? extends ItemDelta> modifications = userDelta.getModifications();
        assertEquals("Unexpected number of modifications", 3, modifications.size());
        PrismAsserts.assertPropertyReplace(userDelta, new QName(SchemaConstants.NS_C,"fullName"),
                new PolyString("Cpt. Jack Sparrow", "cpt jack sparrow"));
        PrismAsserts.assertPropertyAdd(userDelta, new QName(SchemaConstants.NS_C,"honorificPrefix"),
        		new PolyString("Cpt.", "cpt"));
        PrismAsserts.assertPropertyAdd(userDelta, new QName(SchemaConstants.NS_C,"locality"),
        		new PolyString("Tortuga", "tortuga"));

        ObjectModificationType objectModificationType = DeltaConvertor.toObjectModificationType(userDelta);
        System.out.println("Modification XML:");
        System.out.println(PrismTestUtil.serializeAnyDataWrapped(objectModificationType));
        assertEquals("Wrong delta OID", userBefore.getOid(), objectModificationType.getOid());
        List<ItemDeltaType> propertyModifications = objectModificationType.getItemDelta();
        assertEquals("Unexpected number of modifications", 3, propertyModifications.size());
        PolyStringType polyString = new PolyStringType();
        polyString.setOrig("Cpt. Jack Sparrow");
        polyString.setNorm("cpt jack sparrow");
        assertXmlPolyMod(objectModificationType, new QName(SchemaConstants.NS_C,"fullName"), ModificationTypeType.REPLACE, polyString);
        polyString = new PolyStringType();
        polyString.setOrig("Cpt.");
        polyString.setNorm("cpt");
        assertXmlPolyMod(objectModificationType, new QName(SchemaConstants.NS_C,"honorificPrefix"), ModificationTypeType.ADD, polyString);
        polyString = new PolyStringType();
        polyString.setOrig("Tortuga");
        polyString.setNorm("tortuga");
        assertXmlPolyMod(objectModificationType, new QName(SchemaConstants.NS_C,"locality"), ModificationTypeType.ADD, polyString);

        userBefore.checkConsistence();
        userAfter.checkConsistence();
        userDelta.checkConsistence();
        // ROUNDTRIP

        userDelta.applyTo(userBefore);

        userBefore.checkConsistence();
        userAfter.checkConsistence();
        userDelta.checkConsistence();

        //assertEquals("Round trip failed", userAfter, userBefore);

        assertTrue("Not equivalent",userBefore.equivalent(userAfter));

        ObjectDelta<UserType> roundTripDelta = DiffUtil.diff(userBefore, userAfter);
        System.out.println("roundtrip DELTA:");
        System.out.println(roundTripDelta.debugDump());

        assertTrue("Roundtrip delta is not empty",roundTripDelta.isEmpty());
	}

	@Test
	public void testUserReal() throws SchemaException, SAXException, IOException, JAXBException {
		System.out.println("===[ testUserReal ]===");

        String userBeforeXml = MiscUtil.readFile(new File(TEST_DIR, "user-real-before.xml"));
        String userAfterXml = MiscUtil.readFile(new File(TEST_DIR, "user-real-after.xml"));

        // WHEN

        ObjectDelta<UserType> userDelta = DiffUtil.diff(userBeforeXml, userAfterXml, UserType.class, getPrismContext());

        // THEN

        System.out.println("DELTA:");
        System.out.println(userDelta.debugDump());

        userDelta.checkConsistence();
        assertEquals("Wrong delta OID", "2f9b9299-6f45-498f-bc8e-8d17c6b93b20", userDelta.getOid());
        assertEquals("Wrong change type", ChangeType.MODIFY, userDelta.getChangeType());
        Collection<? extends ItemDelta> modifications = userDelta.getModifications();
        assertEquals("Unexpected number of modifications", 4, modifications.size());
        PrismAsserts.assertPropertyReplace(userDelta, new QName(SchemaConstants.NS_C,"emailAddress"), "jack@blackpearl.com");
        PrismAsserts.assertPropertyReplace(userDelta, new QName(SchemaConstants.NS_C,"locality"),
        		new PolyString("World's End", "worlds end"));
        PrismAsserts.assertPropertyReplace(userDelta, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, ActivationStatusType.DISABLED);
        PrismAsserts.assertPropertyAdd(userDelta, new QName(SchemaConstants.NS_C,"organizationalUnit"),
        		new PolyString("Brethren of the Coast", "brethren of the coast"));
	}

	@Test
	public void testAddDelta() throws SchemaException, SAXException, IOException {
		System.out.println("===[ testAddDelta ]===");

        // WHEN
        ObjectDelta<UserType> userDelta = DiffUtil.diff(null,new File(TEST_DIR, "user-jack-after.xml"), UserType.class, getPrismContext());

        //THEN
        System.out.println("DELTA:");
        System.out.println(userDelta.debugDump());

        userDelta.checkConsistence();
        assertEquals("Wrong delta OID", "deadbeef-c001-f00d-1111-222233330001", userDelta.getOid());
        assertEquals("Wrong change type", ChangeType.ADD, userDelta.getChangeType());

        // TODO
	}

	@Test
	public void testTask() throws SchemaException, SAXException, IOException, JAXBException {
		System.out.println("===[ testTask ]===");

        // WHEN

        ObjectDelta<TaskType> diffDelta = DiffUtil.diff(TASK_BEFORE_FILE,
        		new File(TEST_DIR, "task-after.xml"), TaskType.class, getPrismContext());

        // THEN

        System.out.println("DELTA:");
        System.out.println(diffDelta.debugDump());

        diffDelta.checkConsistence();
        assertEquals("Wrong delta OID", "91919191-76e0-59e2-86d6-3d4f02d3ffff", diffDelta.getOid());
        assertEquals("Wrong change type", ChangeType.MODIFY, diffDelta.getChangeType());
        Collection<? extends ItemDelta> modifications = diffDelta.getModifications();
        assertEquals("Unexpected number of modifications", 1, modifications.size());
        // there is only one property in the container. after deleting this property, all container will be deleted, isn't it right?
        PrismAsserts.assertContainerDeleteGetContainerDelta(diffDelta, new ItemPath(TaskType.F_EXTENSION));
//        PrismAsserts.assertPropertyDelete(diffDelta, new ItemPath(TaskType.F_EXTENSION,
//        		new QName("http://midpoint.evolveum.com/xml/ns/public/provisioning/liveSync-1.xsd","token")), 480);

        // Convert to XML form. This should include xsi:type to pass the type information

        ObjectModificationType objectModificationType =  DeltaConvertor.toObjectModificationType(diffDelta);
        System.out.println("Modification XML:");
        System.out.println(PrismTestUtil.serializeAnyDataWrapped(objectModificationType));

        // Check for xsi:type
//        Element tokenElement = (Element) objectModificationType.getModification().get(0).getValue().getAny().get(0);
//        assertTrue("No xsi:type in token",DOMUtil.hasXsiType(tokenElement));

        // parse back delta
//        ObjectDelta<TaskType> patchDelta = DeltaConvertor.createObjectDelta(objectModificationType,
//        		TaskType.class, PrismTestUtil.getPrismContext());
//        patchDelta.checkConsistence();

        // ROUNDTRIP

        PrismObject<TaskType> taskPatch = PrismTestUtil.parseObject(TASK_BEFORE_FILE);
        taskPatch.checkConsistence();

        // patch
        diffDelta.applyTo(taskPatch);

        System.out.println("Task after roundtrip patching");
        System.out.println(taskPatch.debugDump());

        diffDelta.checkConsistence();
        taskPatch.checkConsistence();
        PrismObject<TaskType> taskAfter = PrismTestUtil.parseObject(TASK_AFTER_FILE);
        taskAfter.checkConsistence();

        assertTrue("Not equivalent",taskPatch.equivalent(taskAfter));

        diffDelta.checkConsistence();
        taskPatch.checkConsistence();
        taskAfter.checkConsistence();

        ObjectDelta<TaskType> roundTripDelta = DiffUtil.diff(taskPatch, taskAfter);
        System.out.println("roundtrip DELTA:");
        System.out.println(roundTripDelta.debugDump());

        assertTrue("Roundtrip delta is not empty",roundTripDelta.isEmpty());

        roundTripDelta.checkConsistence();
        diffDelta.checkConsistence();
        taskPatch.checkConsistence();
        taskAfter.checkConsistence();
	}

	@Test
	public void testResource() throws Exception {
		System.out.println("===[ testResource ]===");

		PrismObject<ResourceType> resourceBefore = PrismTestUtil.parseObject(RESOURCE_BEFORE_FILE);
        PrismObject<ResourceType> resourceAfter = PrismTestUtil.parseObject(RESOURCE_AFTER_FILE);

        resourceBefore.checkConsistence();
        resourceAfter.checkConsistence();

        // sanity
        assertFalse("Equals does not work", resourceBefore.equals(resourceAfter));

        // WHEN

        ObjectDelta<ResourceType> resourceDelta = resourceBefore.diff(resourceAfter);

        // THEN

        System.out.println("DELTA:");
        System.out.println(resourceDelta.debugDump());

        resourceDelta.checkConsistence();
        resourceDelta.assertDefinitions(true);
        resourceBefore.checkConsistence();
        resourceAfter.checkConsistence();

        assertEquals("Wrong delta OID", RESOURCE_OID, resourceDelta.getOid());
        assertEquals("Wrong change type", ChangeType.MODIFY, resourceDelta.getChangeType());
        Collection<? extends ItemDelta> modifications = resourceDelta.getModifications();
        assertEquals("Unexpected number of modifications", 7, modifications.size());
        PrismAsserts.assertContainerDeleteGetContainerDelta(resourceDelta, ResourceType.F_SCHEMA);
        PrismAsserts.assertPropertyReplace(resourceDelta, pathTimeouts("update"), 3);
        PrismAsserts.assertPropertyReplace(resourceDelta, pathTimeouts("scriptOnResource"), 4);
        PrismAsserts.assertPropertyDelete(resourceDelta,
        		new ItemPath(ResourceType.F_CONNECTOR_CONFIGURATION, new QName(SchemaTestConstants.NS_ICFC, "producerBufferSize")),
        		100);
        PrismAsserts.assertPropertyReplaceSimple(resourceDelta, ResourceType.F_SYNCHRONIZATION);
        // Configuration properties changes
        assertConfigurationPropertyChange(resourceDelta, "principal");
        assertConfigurationPropertyChange(resourceDelta, "credentials");

        resourceDelta.checkConsistence();
        resourceBefore.checkConsistence();
        resourceAfter.checkConsistence();
	}

	@Test
	public void testResourceConst() throws Exception {
		System.out.println("===[ testResourceConst ]===");

		PrismObject<ResourceType> resourceBefore = PrismTestUtil.parseObject(RESOURCE_BEFORE_FILE);
        PrismObject<ResourceType> resourceAfter = PrismTestUtil.parseObject(RESOURCE_AFTER_CONST_FILE);

        resourceBefore.checkConsistence();
        resourceAfter.checkConsistence();

        // sanity
        assertFalse("Equals does not work", resourceBefore.equals(resourceAfter));

        // WHEN

        ObjectDelta<ResourceType> resourceDelta = resourceBefore.diff(resourceAfter);

        // THEN

        System.out.println("DELTA:");
        System.out.println(resourceDelta.debugDump());

        resourceDelta.checkConsistence();
        resourceDelta.assertDefinitions(true);
        resourceBefore.checkConsistence();
        resourceAfter.checkConsistence();

        assertEquals("Wrong delta OID", RESOURCE_OID, resourceDelta.getOid());
        assertEquals("Wrong change type", ChangeType.MODIFY, resourceDelta.getChangeType());
        Collection<? extends ItemDelta> modifications = resourceDelta.getModifications();
        assertEquals("Unexpected number of modifications", 2, modifications.size());
        assertConfigurationPropertyChange(resourceDelta, "host");
        assertConfigurationPropertyChange(resourceDelta, "port");

        resourceDelta.checkConsistence();
        resourceBefore.checkConsistence();
        resourceAfter.checkConsistence();
	}

	@Test
	public void testResourceConstLiteral() throws Exception {
		System.out.println("===[ testResourceConstLiteral ]===");

		PrismObject<ResourceType> resourceBefore = PrismTestUtil.parseObject(RESOURCE_BEFORE_FILE);
        PrismObject<ResourceType> resourceAfter = PrismTestUtil.parseObject(RESOURCE_AFTER_CONST_FILE);

        // WHEN

        ObjectDelta<ResourceType> resourceDelta = resourceBefore.diff(resourceAfter, true, true);

        // THEN

        System.out.println("DELTA:");
        System.out.println(resourceDelta.debugDump());

        resourceDelta.checkConsistence();
        resourceDelta.assertDefinitions(true);
        resourceBefore.checkConsistence();
        resourceAfter.checkConsistence();

        assertEquals("Wrong delta OID", RESOURCE_OID, resourceDelta.getOid());
        assertEquals("Wrong change type", ChangeType.MODIFY, resourceDelta.getChangeType());
        Collection<? extends ItemDelta> modifications = resourceDelta.getModifications();
        assertEquals("Unexpected number of modifications", 2, modifications.size());
        assertConfigurationPropertyChange(resourceDelta, "host");
        assertConfigurationPropertyChange(resourceDelta, "port");

        resourceDelta.checkConsistence();
        resourceBefore.checkConsistence();
        resourceAfter.checkConsistence();
	}

	private void assertConfigurationPropertyChange(ObjectDelta<ResourceType> resourceDelta, String propName) {
		resourceDelta.checkConsistence();
		PropertyDelta propertyDelta = resourceDelta.findPropertyDelta(pathConfigProperties(propName));
		assertNotNull("No delta for configuration property "+propName, propertyDelta);
		// TODO
		resourceDelta.checkConsistence();
	}

	private ItemPath pathConfigProperties(String propName) {
		return new ItemPath(ResourceType.F_CONNECTOR_CONFIGURATION, SchemaTestConstants.ICFC_CONFIGURATION_PROPERTIES,
				new QName(SchemaTestConstants.NS_ICFC_LDAP, propName));
	}

	private ItemPath pathTimeouts(String last) {
		return new ItemPath(ResourceType.F_CONNECTOR_CONFIGURATION, new QName(SchemaTestConstants.NS_ICFC, "timeouts"),
				new QName(SchemaTestConstants.NS_ICFC, last));
	}

	@Test
	public void testResourceRoundTrip() throws SchemaException, SAXException, IOException, JAXBException {
		System.out.println("===[ testResourceRoundTrip ]===");

        PrismObject<ResourceType> resourceBefore = PrismTestUtil.parseObject(RESOURCE_BEFORE_FILE);
        PrismObject<ResourceType> resourceAfter = PrismTestUtil.parseObject(RESOURCE_AFTER_FILE);

        resourceBefore.checkConsistence();
        resourceAfter.checkConsistence();

        // sanity
        assertFalse("Equals does not work", resourceBefore.equals(resourceAfter));

        // WHEN

        ObjectDelta<ResourceType> resourceDelta = resourceBefore.diff(resourceAfter);

        // THEN

        System.out.println("DELTA:");
        System.out.println(resourceDelta.debugDump());

        resourceDelta.checkConsistence();
        resourceDelta.assertDefinitions(true);
        resourceBefore.checkConsistence();
        resourceAfter.checkConsistence();

        // ROUNDTRIP
        resourceDelta.applyTo(resourceBefore);

        System.out.println("Resource after roundtrip:");
        System.out.println(resourceBefore.debugDump());

        resourceDelta.checkConsistence();
        resourceDelta.assertDefinitions(true);
        resourceBefore.checkConsistence();
        resourceAfter.checkConsistence();

        ObjectDelta<ResourceType> roundTripDelta1 = resourceBefore.diff(resourceAfter);
        System.out.println("roundtrip DELTA 1:");
        System.out.println(roundTripDelta1.debugDump());
        assertTrue("Resource roundtrip 1 failed", roundTripDelta1.isEmpty());

        roundTripDelta1.checkConsistence();
        roundTripDelta1.assertDefinitions(true);
        resourceDelta.checkConsistence();
        resourceBefore.checkConsistence();
        resourceAfter.checkConsistence();

        ObjectDelta<ResourceType> roundTripDelta2 = resourceAfter.diff(resourceBefore);
        System.out.println("roundtrip DELTA 2:");
        System.out.println(roundTripDelta2.debugDump());
        assertTrue("Resource roundtrip 2 failed", roundTripDelta2.isEmpty());

        roundTripDelta2.checkConsistence();
        roundTripDelta2.assertDefinitions(true);
        resourceDelta.checkConsistence();
        resourceBefore.checkConsistence();
        resourceAfter.checkConsistence();

        PrismAsserts.assertEquivalent("Resources after roundtrip not equivalent", resourceAfter, resourceBefore);

        resourceDelta.checkConsistence();
        resourceDelta.assertDefinitions(true);
        resourceBefore.checkConsistence();
        resourceAfter.checkConsistence();
	}

	@Test
	public void testResourceNsChange() throws SchemaException, SAXException, IOException, JAXBException {
		System.out.println("===[ testResourceNsChange ]===");

		PrismObject<ResourceType> resourceBefore = PrismTestUtil.parseObject(RESOURCE_BEFORE_FILE);
        PrismObject<ResourceType> resourceAfter = PrismTestUtil.parseObject(RESOURCE_AFTER_NS_CHANGE_FILE);

        resourceBefore.checkConsistence();
        resourceAfter.checkConsistence();

        // WHEN

        ObjectDelta<ResourceType> resourceDelta = resourceBefore.diff(resourceAfter);

        // THEN

        System.out.println("DELTA:");
        System.out.println(resourceDelta.debugDump());

        resourceDelta.checkConsistence();
        resourceDelta.assertDefinitions(true);
        resourceBefore.checkConsistence();
        resourceAfter.checkConsistence();

        if (!resourceDelta.isEmpty()) {
        	AssertJUnit.fail("The delta is not empty; it is "+resourceDelta);
        }

        // "post" sanity
        assertTrue("equals does not work", resourceBefore.equals(resourceAfter));
        assertTrue("equivalent does not work", resourceBefore.equivalent(resourceAfter));
	}

	@Test
	public void testResourceNsChangeLiteral() throws SchemaException, SAXException, IOException, JAXBException {
		System.out.println("===[ testResourceNsChangeLiteral ]===");

		PrismObject<ResourceType> resourceBefore = PrismTestUtil.parseObject(new File(TEST_DIR, "resource-before.xml"));
        PrismObject<ResourceType> resourceAfter = PrismTestUtil.parseObject(new File(TEST_DIR, "resource-after-ns-change.xml"));

        resourceBefore.checkConsistence();
        resourceAfter.checkConsistence();

        // WHEN
        ObjectDelta<ResourceType> resourceDelta = resourceBefore.diff(resourceAfter, true, true);

        // THEN

        System.out.println("DELTA:");
        System.out.println(resourceDelta.debugDump());

        resourceDelta.checkConsistence();
        resourceDelta.assertDefinitions(true);
        resourceBefore.checkConsistence();
        resourceAfter.checkConsistence();

        assertFalse("The delta is empty", resourceDelta.isEmpty());

	}

    @Test
    public void testResourceNsFixUndeclaredPrefixes() throws SchemaException, SAXException, IOException, JAXBException {
        System.out.println("===[ testResourceNsFixUndeclaredPrefixes ]===");

        boolean orig = QNameUtil.isTolerateUndeclaredPrefixes();
        try {
            QNameUtil.setTolerateUndeclaredPrefixes(true);
            PrismObject<ResourceType> resourceBroken = PrismTestUtil.parseObject(new File(TEST_DIR, "resource2-broken.xml"));
            PrismObject<ResourceType> resourceFixed = PrismTestUtil.parseObject(new File(TEST_DIR, "resource2-fixed.xml"));

            resourceBroken.checkConsistence();
            resourceFixed.checkConsistence();

            // WHEN
            String xmlBroken = getPrismContext().serializeObjectToString(resourceBroken, PrismContext.LANG_XML);
            ObjectDelta<ResourceType> resourceDelta = resourceBroken.diff(resourceFixed, true, true);

            // THEN

            System.out.println("DELTA:");
            System.out.println(resourceDelta.debugDump());

            System.out.println("BROKEN RESOURCE:");
            System.out.println(xmlBroken);
            assertTrue("no __UNDECLARED__ flag in broken resource", xmlBroken.contains("__UNDECLARED__"));

            resourceDelta.checkConsistence();
            resourceDelta.assertDefinitions(true);
            resourceBroken.checkConsistence();
            resourceFixed.checkConsistence();

            assertFalse("The delta is empty", resourceDelta.isEmpty());

            PrismObject<ResourceType> resourceUpdated = resourceBroken.clone();
            resourceDelta.applyTo(resourceUpdated);

            String xmlUpdated = getPrismContext().serializeObjectToString(resourceUpdated, PrismContext.LANG_XML);
            System.out.println("UPDATED RESOURCE:");
            System.out.println(xmlUpdated);
            assertFalse("__UNDECLARED__ flag in updated resource", xmlUpdated.contains("__UNDECLARED__"));

            QNameUtil.setTolerateUndeclaredPrefixes(false);
            getPrismContext().parseObject(xmlUpdated);        //should be without exceptions

        } finally {
            QNameUtil.setTolerateUndeclaredPrefixes(orig);
        }


    }

    /**
     * This test illustrates MID-2174.
     *
     * We take a shadow having objectChange set.
     * We delete it via asObjectable().setObjectChange(null).
     * Then we compute the delta via diff.
     * All these operations are done on a shadow that contains PARSED values in objectChange property.
     *
     * The problem of MID-2174 is that (in reality) we then try to apply the delta to the shadow as stored in repository,
     * i.e. to shadow with RAW values in objectChange property.
     *
     * MidPoint uses an approximation there - it compares XNode serializations of values. Sometimes they match,
     * sometimes they do not. In this particular case they fail to match on serialization of c:ObjectReferenceType,
     * because BeanMarshaller is used, and ObjectReferenceType.getFilter() returns empty filter instead of null.
     * This could be fixed; however, it would not help much, because it is almost sure that other similar problems
     * would sooner or later emerge.
     */
    @Test(enabled = false)
    public void testShadowObjectChange() throws SchemaException, SAXException, IOException, JAXBException {
        System.out.println("===[ testShadowObjectChange ]===");

        // WHEN

        PrismContext prismContext = getPrismContext();
        PrismObject<ShadowType> oldObject = getParsedShadowBefore(prismContext);
        PrismObject<ShadowType> newObject = getShadowAfter(oldObject);

        ObjectDelta<ShadowType> diffDelta = DiffUtil.diff(oldObject, newObject);

        // THEN

        System.out.println("DELTA:");
        System.out.println(diffDelta.debugDump());

        diffDelta.checkConsistence();
        assertEquals("Wrong delta OID", "19a27a9d-c7f0-4e41-bcbf-5fa9fc229b10", diffDelta.getOid());
        assertEquals("Wrong change type", ChangeType.MODIFY, diffDelta.getChangeType());
        // ... (not important now) ...

        // ROUNDTRIP

        // without resolving RawTypes!
        PrismObject<ShadowType> shadow = getRawShadowBefore(prismContext);
        shadow.checkConsistence();
        PrismObject<ShadowType> shadowAfter = getShadowAfter(shadow);
        shadowAfter.checkConsistence();

        // patch
        diffDelta.applyTo(shadow);

        System.out.println("Shadow after roundtrip patching");
        System.out.println(shadow.debugDump());

        diffDelta.checkConsistence();
        shadow.checkConsistence();

        assertTrue("Not equivalent", shadow.equivalent(shadowAfter));

        diffDelta.checkConsistence();
        shadow.checkConsistence();
        shadowAfter.checkConsistence();

        ObjectDelta<ShadowType> roundTripDelta = DiffUtil.diff(shadow, shadowAfter);
        System.out.println("roundtrip DELTA:");
        System.out.println(roundTripDelta.debugDump());

        assertTrue("Roundtrip delta is not empty", roundTripDelta.isEmpty());

        roundTripDelta.checkConsistence();
        diffDelta.checkConsistence();
        shadow.checkConsistence();
        shadowAfter.checkConsistence();
    }

    protected PrismObject<ShadowType> getShadowAfter(PrismObject<ShadowType> oldObject) {
        PrismObject<ShadowType> newObject = oldObject.clone();
        newObject.asObjectable().setObjectChange(null);
        return newObject;
    }

    protected PrismObject<ShadowType> getParsedShadowBefore(PrismContext prismContext) throws SchemaException, IOException {
        PrismObject<ShadowType> oldObject = getRawShadowBefore(prismContext);
        // resolve rawtypes
        ObjectDeltaType objectChange = oldObject.asObjectable().getObjectChange();
        for (ItemDeltaType itemDeltaType : objectChange.getItemDelta()) {
            for (RawType rawType : itemDeltaType.getValue()) {
                rawType.getParsedItem(
                        new PrismPropertyDefinitionImpl(itemDeltaType.getPath().getItemPath().lastNamed().getName(),
                                rawType.getXnode().getTypeQName(),
                                prismContext));
            }
        }
        return oldObject;
    }

    protected PrismObject<ShadowType> getRawShadowBefore(PrismContext prismContext) throws SchemaException, IOException {
        PrismObject<ShadowType> oldObject = prismContext.parseObject(new File(TEST_DIR, "shadow-before.xml"));
        return oldObject;
    }

    private void assertXmlPolyMod(ObjectModificationType objectModificationType, QName propertyName,
            ModificationTypeType modType, PolyStringType... expectedValues) throws SchemaException {
    	//FIXME:
        for (ItemDeltaType mod : objectModificationType.getItemDelta()) {
        	 if (!propertyName.equals(mod.getPath().getItemPath().last())) {
               continue;
           }
        	 assertEquals(modType, mod.getModificationType());
            for (RawType val  : mod.getValue()){
            	assertModificationPolyStringValue(val, expectedValues);
            }
        }
    }

    private void assertModificationPolyStringValue(RawType value, PolyStringType... expectedValues) throws SchemaException {
    	XNode xnode = value.serializeToXNode();
        assertFalse(xnode.isEmpty());
        PolyStringType valueAsPoly = value.getPrismContext().parserFor(new RootXNode(new QName("dummy"), xnode)).parseRealValue(PolyStringType.class);
        boolean found = false;
        for (PolyStringType expectedValue: expectedValues) {
            if (expectedValue.getOrig().equals(valueAsPoly.getOrig()) && expectedValue.getNorm().equals(valueAsPoly.getNorm())) {
                found = true;
            }
        }
        assertTrue(found);
    }

    private boolean equal(String value, Element element) {
        if (value == null && element == null) {
            return true;
        }

        if ((value == null && element != null) || (value != null && element == null)) {
            return false;
        }

        return value.equals(element.getTextContent());
    }

//	private void assertXmlMod(ObjectModificationType objectModificationType, QName propertyName,
//			ModificationTypeType modType, String... expectedValues) {
//		for (ItemDeltaType mod: objectModificationType.getItemDelta()) {
//			assertEquals(modType, mod.getModificationType());
//			for (RawType val : mod.getValue()){
//				List<Object> elements = val.getContent();
//				assertFalse(elements.isEmpty());
//				Object first = elements.get(0);
////				QName elementQName = JAXBUtil.getElementQName(first);
//				if (propertyName.equals(mod.getPath().getItemPath().last())) {
//
//					assertEquals(expectedValues.length, elements.size());
//					for (Object element: elements) {
//						boolean found = false;
//						for (String expectedValue: expectedValues) {
//							Element domElement = (Element)element;
//							if (expectedValue.equals(domElement.getTextContent())) {
//								found = true;
//							}
//						}
//						assertTrue(found);
//					}
//				}
//			}
//		}
//	}

    // this is a simple test of applying delta (don't know where to put it)
	// MID-3828
    @Test
    public void testCampaign() throws SchemaException, SAXException, IOException, JAXBException {
        System.out.println("===[ testCampaign ]===");

        PrismObject<AccessCertificationCampaignType> campaign = PrismTestUtil.parseObject(new File(TEST_DIR, "campaign-1.xml"));
        campaign.checkConsistence();
        assertEquals("Wrong # of triggers", 2, campaign.asObjectable().getTrigger().size());

		// WHEN
		TriggerType triggerToDelete = new TriggerType(getPrismContext());
		triggerToDelete.setId(3L);			// non-existing ID
		triggerToDelete.setTimestamp(XmlTypeConverter.createXMLGregorianCalendar("2017-03-17T23:43:49.705+01:00"));
		triggerToDelete.setHandlerUri("http://midpoint.evolveum.com/xml/ns/public/certification/trigger/close-stage/handler-3");

		@SuppressWarnings({"unchecked", "raw"})
        ObjectDelta<AccessCertificationCampaignType> delta = (ObjectDelta<AccessCertificationCampaignType>)
				DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, getPrismContext())
				.item(AccessCertificationCampaignType.F_TRIGGER).delete(triggerToDelete)
				.asObjectDelta(campaign.getOid());

        // THEN
		delta.applyTo(campaign);
		System.out.println("Campaign after:\n" + campaign.debugDump());

		assertEquals("Wrong # of triggers", 2, campaign.asObjectable().getTrigger().size());
	}

	@Test
    public void testReplaceMultivalueDiff() throws Exception {
        PrismObject<UserType> before = PrismTestUtil.parseObject(new File(TEST_DIR, "user-extension-before.xml"));

        ObjectDelta<UserType> delta = ObjectDelta.createEmptyModifyDelta(UserType.class, before.getOid(), getPrismContext());
        PropertyDelta pd = delta.addModificationReplaceProperty(
                new ItemPath(UserType.F_EXTENSION, new QName("badLuck")),
                123L);


        // this is causing the problem, if commented out delta is ok
        PrismValue v = (PrismValue) pd.getValuesToReplace().iterator().next();
        v.setOriginType(OriginType.USER_POLICY);
        v.setOriginObject(before.asObjectable());
        // end

        PrismPropertyValue v1 = (PrismPropertyValue) PrismPropertyValue.fromRealValue(123L);
        v1.setParent(pd);
        PrismPropertyValue v2 = (PrismPropertyValue) PrismPropertyValue.fromRealValue(456L);
        v2.setParent(pd);
        pd.addEstimatedOldValues(v1, v2);
        getPrismContext().adopt(delta);

        Collection<? extends ItemDelta> modifications = delta.getModifications();

        PrismObject changed = before.clone();
        ItemDelta.applyTo(modifications, changed);
        Collection<? extends ItemDelta> processedModifications = before.diffModifications(changed, true, true);

        assertEquals(1, processedModifications.size());
        ItemDelta d = processedModifications.iterator().next();
        assertTrue(d.isDelete());
        assertFalse(d.isAdd());
        assertFalse(d.isReplace());
    }
}
