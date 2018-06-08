/**
 * Copyright (c) 2016-2017 Evolveum
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
package com.evolveum.midpoint.web;

import static com.evolveum.midpoint.web.AdminGuiTestConstants.RESOURCE_DUMMY_ASSOCIATION_GROUP_QNAME;
import static com.evolveum.midpoint.web.AdminGuiTestConstants.ROLE_MAPMAKER_OID;
import static com.evolveum.midpoint.web.AdminGuiTestConstants.USER_JACK_FULL_NAME;
import static com.evolveum.midpoint.web.AdminGuiTestConstants.USER_JACK_OID;
import static com.evolveum.midpoint.web.AdminGuiTestConstants.USER_JACK_USERNAME;
import static com.evolveum.midpoint.web.AdminGuiTestConstants.USER_EMPTY_OID;
import static com.evolveum.midpoint.web.AdminGuiTestConstants.USER_EMPTY_USERNAME;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.ItemProcessing;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.web.component.prism.ShadowAssociationWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapperFactory;
import com.evolveum.midpoint.web.component.prism.ItemWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapperFactory;
import com.evolveum.midpoint.web.component.prism.PropertyOrReferenceWrapper;
import com.evolveum.midpoint.web.component.prism.ReferenceWrapper;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-admin-gui-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestIntegrationObjectWrapperFactory extends AbstractInitializedGuiIntegrationTest {

	public static final File TEST_DIR = new File("src/test/resources/wrapper");

	protected static final File ROLE_PROP_READ_ALL_MODIFY_SOME_USER_FILE = new File(TEST_DIR, "role-prop-read-all-modify-some-user.xml");
	protected static final String ROLE_PROP_READ_ALL_MODIFY_SOME_USER_OID = "00000000-0000-0000-0000-00000000ae05";

	protected static final File ROLE_PROP_READ_SOME_MODIFY_SOME_USER_FILE = new File(TEST_DIR, "role-prop-read-some-modify-some-user.xml");
	protected static final String ROLE_PROP_READ_SOME_MODIFY_SOME_USER_OID = "00000000-0000-0000-0000-00000000ae08";

	private static final String USER_WALLY_NAME = "wally";
	private static final String USER_WALLY_FULLNAME = "Wally B. Feed";

	public static final String GROUP_DUMMY_MAPMAKERS_NAME = "mapmakers";

	private static final String USER_NEWMAN_USERNAME = "newman";
	private static final String USER_NEWMAN_GIVEN_NAME = "John";
	private static final String USER_NEWMAN_FAMILY_NAME = "Newman";
	private static final String USER_NEWMAN_EMPLOYEE_NUMBER = "N00001";
	private static final String USER_NEWMAN_SHIP = "Nova";
	
	private String userWallyOid;
	private String accountWallyOid;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		repoAddObjectFromFile(ROLE_PROP_READ_ALL_MODIFY_SOME_USER_FILE, initResult);
		repoAddObjectFromFile(ROLE_PROP_READ_SOME_MODIFY_SOME_USER_FILE, initResult);

	}

	@Test
    public void test100CreateWrapperUserJack() throws Exception {
		final String TEST_NAME = "test100CreateWrapperUserJack";
		TestUtil.displayTestTitle(TEST_NAME);
		PrismObject<UserType> user = getUser(USER_JACK_OID);

		// WHEN
		displayWhen(TEST_NAME);

		Task task = taskManager.createTaskInstance(TEST_NAME);

		ObjectWrapperFactory factory = new ObjectWrapperFactory(getServiceLocator(task));
		ObjectWrapper<UserType> objectWrapper = factory.createObjectWrapper("user display name", "user description", user,
				ContainerStatus.MODIFYING, task);

		// THEN
		displayThen(TEST_NAME);

		IntegrationTestTools.display("Wrapper after", objectWrapper);

		WrapperTestUtil.assertWrapper(objectWrapper, "user display name", "user description", user, ContainerStatus.MODIFYING);
		assertEquals("wrong number of containers in "+objectWrapper, 6, objectWrapper.getContainers().size());

		ContainerWrapper<UserType> mainContainerWrapper = objectWrapper.findContainerWrapper(null);
		WrapperTestUtil.assertWrapper(mainContainerWrapper, "prismContainer.mainPanelDisplayName", (ItemPath)null, user, ContainerStatus.MODIFYING);
		assertEquals("wrong number of containers in "+mainContainerWrapper, 1, mainContainerWrapper.getValues().size());
		ContainerValueWrapper<UserType> mainContainerValueWrapper = mainContainerWrapper.getValues().iterator().next();
		WrapperTestUtil.assertPropertyWrapper(mainContainerValueWrapper, UserType.F_NAME, PrismTestUtil.createPolyString(USER_JACK_USERNAME));
		WrapperTestUtil.assertPropertyWrapper(mainContainerValueWrapper, UserType.F_TIMEZONE, null);
		WrapperTestUtil.assertPropertyWrapper(mainContainerValueWrapper, extensionPath(PIRACY_SHIP), AdminGuiTestConstants.USER_JACK_SHIP);

		ContainerWrapper<ActivationType> activationContainerWrapper = objectWrapper.findContainerWrapper(new ItemPath(UserType.F_ACTIVATION));
		WrapperTestUtil.assertWrapper(activationContainerWrapper, "ActivationType.activation", UserType.F_ACTIVATION, user, ContainerStatus.MODIFYING);
		assertEquals("wrong number of containers in "+activationContainerWrapper, 1, activationContainerWrapper.getValues().size());
		ContainerValueWrapper<ActivationType> activationContainerValueWrapper = activationContainerWrapper.getValues().iterator().next();
		WrapperTestUtil.assertPropertyWrapper(activationContainerValueWrapper, ActivationType.F_ADMINISTRATIVE_STATUS, ActivationStatusType.ENABLED);
		WrapperTestUtil.assertPropertyWrapper(activationContainerValueWrapper, ActivationType.F_LOCKOUT_STATUS, null);

		assertEquals("Wrong main container wrapper readOnly", Boolean.FALSE, (Boolean)mainContainerWrapper.isReadonly());

		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_NAME, true);
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_GIVEN_NAME, true);
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_FULL_NAME, true);
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_ADDITIONAL_NAME, false); // not visible, because it is empty
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_LOCALITY, true);
		
		assertItemWrapperProcessing(mainContainerValueWrapper, PIRACY_WEAPON, null);
		assertItemWrapperProcessing(mainContainerValueWrapper, PIRACY_COLORS, ItemProcessing.AUTO);
		assertItemWrapperProcessing(mainContainerValueWrapper, PIRACY_SECRET, ItemProcessing.IGNORE);
		assertItemWrapperProcessing(mainContainerValueWrapper, PIRACY_RANT, ItemProcessing.MINIMAL);

		// WHEN
		objectWrapper.setShowEmpty(true);

		// THEN
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_NAME, true);
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_GIVEN_NAME, true); // emphasized
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_FULL_NAME, true); // emphasized
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_ADDITIONAL_NAME, true); // empty
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_LOCALITY, true); // empty
		
		ObjectDelta<UserType> objectDelta = objectWrapper.getObjectDelta();
		display("Delta", objectDelta);
		assertTrue("non-empty delta produced from wrapper: "+objectDelta, objectDelta.isEmpty());
	}
	
	/**
	 * Create wrapper for brand new empty user.
	 */
	@Test
    public void test110CreateWrapperUserNewEmpty() throws Exception {
		final String TEST_NAME = "test110CreateWrapperUserNew";
		TestUtil.displayTestTitle(TEST_NAME);
		PrismObject<UserType> user = getUserDefinition().instantiate();

		// WHEN
		displayWhen(TEST_NAME);

		Task task = taskManager.createTaskInstance(TEST_NAME);

		ObjectWrapperFactory factory = new ObjectWrapperFactory(getServiceLocator(task));
		ObjectWrapper<UserType> objectWrapper = factory.createObjectWrapper("user display name", "user description", user,
				ContainerStatus.ADDING, task);

		// THEN
		displayThen(TEST_NAME);

		IntegrationTestTools.display("Wrapper after", objectWrapper);

		WrapperTestUtil.assertWrapper(objectWrapper, "user display name", "user description", user, ContainerStatus.ADDING);
		assertEquals("wrong number of containers in "+objectWrapper, 6, objectWrapper.getContainers().size());

		ContainerWrapper<UserType> mainContainerWrapper = objectWrapper.findContainerWrapper(null);
		WrapperTestUtil.assertWrapper(mainContainerWrapper, "prismContainer.mainPanelDisplayName", (ItemPath)null, user, ContainerStatus.ADDING);
		assertEquals("wrong number of containers in "+mainContainerWrapper, 1, mainContainerWrapper.getValues().size());
		ContainerValueWrapper<UserType> mainContainerValueWrapper = mainContainerWrapper.getValues().iterator().next();
		WrapperTestUtil.assertPropertyWrapper(mainContainerValueWrapper, UserType.F_NAME, null);
		WrapperTestUtil.assertPropertyWrapper(mainContainerValueWrapper, UserType.F_TIMEZONE, null);

		ContainerWrapper<ActivationType> activationContainerWrapper = objectWrapper.findContainerWrapper(new ItemPath(UserType.F_ACTIVATION));
		WrapperTestUtil.assertWrapper(activationContainerWrapper, "ActivationType.activation", UserType.F_ACTIVATION, user, ContainerStatus.ADDING);
		assertEquals("wrong number of containers in "+activationContainerWrapper, 1, activationContainerWrapper.getValues().size());
		ContainerValueWrapper<ActivationType> activationContainerValueWrapper = activationContainerWrapper.getValues().iterator().next();
		WrapperTestUtil.assertPropertyWrapper(activationContainerValueWrapper, ActivationType.F_ADMINISTRATIVE_STATUS, null);
		WrapperTestUtil.assertPropertyWrapper(activationContainerValueWrapper, ActivationType.F_LOCKOUT_STATUS, null);

		assertEquals("Wrong main container wrapper readOnly", Boolean.FALSE, (Boolean)mainContainerWrapper.isReadonly());

		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_NAME, true);
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_GIVEN_NAME, true);
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_FULL_NAME, true);
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_ADDITIONAL_NAME, false); // not visible, because it is empty
		
		assertItemWrapperProcessing(mainContainerValueWrapper, PIRACY_WEAPON, null);
		assertItemWrapperProcessing(mainContainerValueWrapper, PIRACY_COLORS, ItemProcessing.AUTO);
		assertItemWrapperProcessing(mainContainerValueWrapper, PIRACY_SECRET, ItemProcessing.IGNORE);
		assertItemWrapperProcessing(mainContainerValueWrapper, PIRACY_RANT, ItemProcessing.MINIMAL);

		// WHEN
		objectWrapper.setShowEmpty(true);

		// THEN
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_NAME, true);
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_GIVEN_NAME, true); // emphasized
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_FULL_NAME, true); // emphasized
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_ADDITIONAL_NAME, true); // empty
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_LOCALITY, true); // empty
		
		ObjectDelta<UserType> objectDelta = objectWrapper.getObjectDelta();
		display("Delta", objectDelta);
		assertTrue("non-add delta produced from wrapper: "+objectDelta, objectDelta.isAdd());
		PrismObject<UserType> objectToAdd = objectDelta.getObjectToAdd();
		assertTrue("non-empty object in add delta produced from wrapper: "+objectDelta, objectToAdd.isEmpty());
	}

	/**
	 * Create wrapper for brand new user, but "fill in" some data.
	 */
	@Test
    public void test112CreateWrapperUserNewman() throws Exception {
		final String TEST_NAME = "test112CreateWrapperUserNewman";
		TestUtil.displayTestTitle(TEST_NAME);
		PrismObject<UserType> user = getUserDefinition().instantiate();

		// WHEN
		displayWhen(TEST_NAME);

		Task task = taskManager.createTaskInstance(TEST_NAME);

		ObjectWrapperFactory factory = new ObjectWrapperFactory(getServiceLocator(task));
		ObjectWrapper<UserType> objectWrapper = factory.createObjectWrapper("user display name", "user description", user,
				ContainerStatus.ADDING, task);

		ContainerWrapper<UserType> mainContainerWrapper = objectWrapper.findContainerWrapper(null);
		ContainerValueWrapper<UserType> mainContainerValueWrapper = mainContainerWrapper.getValues().iterator().next();
		
		WrapperTestUtil.fillInPropertyWrapper(mainContainerValueWrapper, UserType.F_NAME, PrismTestUtil.createPolyString(USER_NEWMAN_USERNAME));
		WrapperTestUtil.fillInPropertyWrapper(mainContainerValueWrapper, UserType.F_GIVEN_NAME, PrismTestUtil.createPolyString(USER_NEWMAN_GIVEN_NAME));
		WrapperTestUtil.fillInPropertyWrapper(mainContainerValueWrapper, UserType.F_FAMILY_NAME, PrismTestUtil.createPolyString(USER_NEWMAN_FAMILY_NAME));
		WrapperTestUtil.fillInPropertyWrapper(mainContainerValueWrapper, UserType.F_EMPLOYEE_NUMBER, USER_NEWMAN_EMPLOYEE_NUMBER);
		WrapperTestUtil.fillInPropertyWrapper(mainContainerValueWrapper, extensionPath(PIRACY_SHIP), USER_NEWMAN_SHIP);
		
		// THEN
		displayThen(TEST_NAME);

		IntegrationTestTools.display("Wrapper after", objectWrapper);

		WrapperTestUtil.assertWrapper(objectWrapper, "user display name", "user description", user, ContainerStatus.ADDING);
		assertEquals("wrong number of containers in "+objectWrapper, 6, objectWrapper.getContainers().size());

		WrapperTestUtil.assertWrapper(mainContainerWrapper, "prismContainer.mainPanelDisplayName", (ItemPath)null, user, ContainerStatus.ADDING);
		assertEquals("wrong number of containers in "+mainContainerWrapper, 1, mainContainerWrapper.getValues().size());
		WrapperTestUtil.assertPropertyWrapper(mainContainerValueWrapper, UserType.F_NAME, PrismTestUtil.createPolyString(USER_NEWMAN_USERNAME));
		WrapperTestUtil.assertPropertyWrapper(mainContainerValueWrapper, UserType.F_TIMEZONE, null);
		WrapperTestUtil.assertPropertyWrapper(mainContainerValueWrapper, extensionPath(PIRACY_SHIP), USER_NEWMAN_SHIP);

		ContainerWrapper<ActivationType> activationContainerWrapper = objectWrapper.findContainerWrapper(new ItemPath(UserType.F_ACTIVATION));
		WrapperTestUtil.assertWrapper(activationContainerWrapper, "ActivationType.activation", UserType.F_ACTIVATION, user, ContainerStatus.ADDING);
		assertEquals("wrong number of containers in "+activationContainerWrapper, 1, activationContainerWrapper.getValues().size());

		assertEquals("Wrong main container wrapper readOnly", Boolean.FALSE, (Boolean)mainContainerWrapper.isReadonly());

		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_NAME, true);
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_GIVEN_NAME, true);
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_FULL_NAME, true);
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_ADDITIONAL_NAME, false); // not visible, because it is empty
		
		assertItemWrapperProcessing(mainContainerValueWrapper, PIRACY_WEAPON, null);
		assertItemWrapperProcessing(mainContainerValueWrapper, PIRACY_COLORS, ItemProcessing.AUTO);
		assertItemWrapperProcessing(mainContainerValueWrapper, PIRACY_SECRET, ItemProcessing.IGNORE);
		assertItemWrapperProcessing(mainContainerValueWrapper, PIRACY_RANT, ItemProcessing.MINIMAL);

		// WHEN
		objectWrapper.setShowEmpty(true);

		// THEN
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_NAME, true);
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_GIVEN_NAME, true); // emphasized
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_FULL_NAME, true); // emphasized
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_ADDITIONAL_NAME, true); // empty
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_LOCALITY, true); // empty
		
		ObjectDelta<UserType> objectDelta = objectWrapper.getObjectDelta();
		display("Delta", objectDelta);
		assertTrue("non-add delta produced from wrapper: "+objectDelta, objectDelta.isAdd());
		PrismObject<UserType> objectToAdd = objectDelta.getObjectToAdd();
		PrismAsserts.assertPropertyValue(objectToAdd, UserType.F_NAME, PrismTestUtil.createPolyString(USER_NEWMAN_USERNAME));
		PrismAsserts.assertPropertyValue(objectToAdd, UserType.F_GIVEN_NAME, PrismTestUtil.createPolyString(USER_NEWMAN_GIVEN_NAME));
		PrismAsserts.assertPropertyValue(objectToAdd, UserType.F_FAMILY_NAME, PrismTestUtil.createPolyString(USER_NEWMAN_FAMILY_NAME));
		PrismAsserts.assertPropertyValue(objectToAdd, UserType.F_EMPLOYEE_NUMBER, USER_NEWMAN_EMPLOYEE_NUMBER);
		PrismAsserts.assertPropertyValue(objectToAdd, extensionPath(PIRACY_SHIP), USER_NEWMAN_SHIP);
		PrismAsserts.assertItems(objectToAdd, 5);
	}
	
	@Test
    public void test102CreateWrapperUserEmpty() throws Exception {
		final String TEST_NAME = "test102CreateWrapperUserEmpty";
		TestUtil.displayTestTitle(TEST_NAME);
		PrismObject<UserType> user = getUser(USER_EMPTY_OID);

		// WHEN
		displayWhen(TEST_NAME);

		Task task = taskManager.createTaskInstance(TEST_NAME);

		ObjectWrapperFactory factory = new ObjectWrapperFactory(getServiceLocator(task));
		ObjectWrapper<UserType> objectWrapper = factory.createObjectWrapper("user display name", "user description", user,
				ContainerStatus.MODIFYING, task);

		// THEN
		displayThen(TEST_NAME);

		IntegrationTestTools.display("Wrapper after", objectWrapper);

		WrapperTestUtil.assertWrapper(objectWrapper, "user display name", "user description", user, ContainerStatus.MODIFYING);
		assertEquals("wrong number of containers in "+objectWrapper, 6, objectWrapper.getContainers().size());

		ContainerWrapper<UserType> mainContainerWrapper = objectWrapper.findContainerWrapper(null);
		WrapperTestUtil.assertWrapper(mainContainerWrapper, "prismContainer.mainPanelDisplayName", (ItemPath)null, user, ContainerStatus.MODIFYING);
		assertEquals("wrong number of containers in "+mainContainerWrapper, 1, mainContainerWrapper.getValues().size());
		ContainerValueWrapper<UserType> mainContainerValueWrapper = mainContainerWrapper.getValues().iterator().next();
		WrapperTestUtil.assertPropertyWrapper(mainContainerValueWrapper, UserType.F_NAME, PrismTestUtil.createPolyString(USER_EMPTY_USERNAME));
		WrapperTestUtil.assertPropertyWrapper(mainContainerValueWrapper, UserType.F_TIMEZONE, null);

		// Not sure about this
//		ContainerWrapper<ActivationType> activationContainerWrapper = objectWrapper.findContainerWrapper(new ItemPath(UserType.F_ACTIVATION));
//		assertNull("Unexpected activation "+activationContainerWrapper, activationContainerWrapper);

		assertEquals("Wrong main container wrapper readOnly", Boolean.FALSE, (Boolean)mainContainerWrapper.isReadonly());

		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_NAME, true);
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_GIVEN_NAME, true); // emphasized
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_FULL_NAME, true); // emphasized
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_ADDITIONAL_NAME, false); // empty
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_LOCALITY, false); // empty

		// WHEN
		objectWrapper.setShowEmpty(true);

		// THEN
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_NAME, true);
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_GIVEN_NAME, true); // emphasized
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_FULL_NAME, true); // emphasized
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_ADDITIONAL_NAME, true); // empty
		assertItemWrapperFullConrol(mainContainerValueWrapper, UserType.F_LOCALITY, true); // empty
		
		ObjectDelta<UserType> objectDelta = objectWrapper.getObjectDelta();
		display("Delta", objectDelta);
		assertTrue("non-empty delta produced from wrapper: "+objectDelta, objectDelta.isEmpty());
	}


	@Test
    public void test150CreateWrapperShadow() throws Exception {
		final String TEST_NAME = "test150CreateWrapperShadow";
		TestUtil.displayTestTitle(TEST_NAME);
		PrismObject<ShadowType> shadow = getShadowModel(accountJackOid);
		shadow.findReference(ShadowType.F_RESOURCE_REF).getValue().setObject(resourceDummy);
		display("Shadow", shadow);

		// WHEN
		displayWhen(TEST_NAME);
		Task task = taskManager.createTaskInstance(TEST_NAME);

		ObjectWrapperFactory factory = new ObjectWrapperFactory(getServiceLocator(task));
		ObjectWrapper<ShadowType> objectWrapper = factory.createObjectWrapper("shadow display name", "shadow description", shadow,
				ContainerStatus.MODIFYING, task);

		// THEN
		displayThen(TEST_NAME);

		display("Wrapper after", objectWrapper);

		WrapperTestUtil.assertWrapper(objectWrapper, "shadow display name", "shadow description", shadow, ContainerStatus.MODIFYING);
		assertEquals("wrong number of containers in "+objectWrapper, 8, objectWrapper.getContainers().size());

		ContainerWrapper<ShadowAttributesType> attributesContainerWrapper = objectWrapper.findContainerWrapper(new ItemPath(ShadowType.F_ATTRIBUTES));
		assertEquals("wrong number of values in "+attributesContainerWrapper, 1, attributesContainerWrapper.getValues().size());
		PrismContainer<ShadowAttributesType> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
		WrapperTestUtil.assertWrapper(attributesContainerWrapper, "attributes", new ItemPath(ShadowType.F_ATTRIBUTES),
				attributesContainer, false, ContainerStatus.MODIFYING);
		ContainerValueWrapper<ShadowAttributesType> attributesContainerValueWrapper = attributesContainerWrapper.getValues().iterator().next();
		WrapperTestUtil.assertPropertyWrapper(attributesContainerValueWrapper, dummyResourceCtl.getAttributeFullnameQName(), USER_JACK_FULL_NAME);
		WrapperTestUtil.assertPropertyWrapper(attributesContainerValueWrapper, SchemaConstants.ICFS_NAME, USER_JACK_USERNAME);
		assertEquals("wrong number of items in "+attributesContainerWrapper, 17, attributesContainerValueWrapper.getItems().size());

		ContainerWrapper<ActivationType> activationContainerWrapper = objectWrapper.findContainerWrapper(new ItemPath(ShadowType.F_ACTIVATION));
		assertEquals("wrong number of values in "+activationContainerWrapper, 1, activationContainerWrapper.getValues().size());
		ContainerValueWrapper<ActivationType> activationContainerValueWrapper = activationContainerWrapper.getValues().iterator().next();
		WrapperTestUtil.assertWrapper(activationContainerWrapper, "ShadowType.activation", UserType.F_ACTIVATION, shadow, ContainerStatus.MODIFYING);
		WrapperTestUtil.assertPropertyWrapper(activationContainerValueWrapper, ActivationType.F_ADMINISTRATIVE_STATUS, ActivationStatusType.ENABLED);
		WrapperTestUtil.assertPropertyWrapper(activationContainerValueWrapper, ActivationType.F_LOCKOUT_STATUS, null);

		assertEquals("Wrong attributes container wrapper readOnly", Boolean.FALSE, (Boolean)attributesContainerWrapper.isReadonly());

		ItemWrapper fullnameWrapper = attributesContainerValueWrapper.findPropertyWrapper(dummyResourceCtl.getAttributeFullnameQName());
		assertEquals("Wrong attribute fullname readOnly", Boolean.FALSE, (Boolean)fullnameWrapper.isReadonly()); // Is this OK?
		assertEquals("Wrong attribute fullname visible", Boolean.TRUE, (Boolean)fullnameWrapper.isVisible());
		ItemDefinition fullNameDefinition = fullnameWrapper.getItemDefinition();
		display("fullname attribute definition", fullNameDefinition);
		assertEquals("Wrong attribute fullname definition.canRead", Boolean.TRUE, (Boolean)fullNameDefinition.canRead());
		assertEquals("Wrong attribute fullname definition.canAdd", Boolean.TRUE, (Boolean)fullNameDefinition.canAdd());
		assertEquals("Wrong attribute fullname definition.canModify", Boolean.TRUE, (Boolean)fullNameDefinition.canModify());
		// MID-3144
		if (fullNameDefinition.getDisplayOrder() == null || fullNameDefinition.getDisplayOrder() < 100 || fullNameDefinition.getDisplayOrder() > 400) {
			AssertJUnit.fail("Wrong fullname definition.displayOrder: " + fullNameDefinition.getDisplayOrder());
		}
		assertEquals("Wrong attribute fullname definition.displayName", "Full Name", fullNameDefinition.getDisplayName());
		
		ObjectDelta<ShadowType> objectDelta = objectWrapper.getObjectDelta();
		display("Delta", objectDelta);
		assertTrue("non-empty delta produced from wrapper: "+objectDelta, objectDelta.isEmpty());

	}
	
	@Test
    public void test160CreateWrapperOrgScummBar() throws Exception {
		final String TEST_NAME = "test160CreateWrapperOrgScummBar";
		TestUtil.displayTestTitle(TEST_NAME);
		PrismObject<OrgType> org = getObject(OrgType.class, ORG_SCUMM_BAR_OID);

		// WHEN
		displayWhen(TEST_NAME);

		Task task = taskManager.createTaskInstance(TEST_NAME);

		ObjectWrapperFactory factory = new ObjectWrapperFactory(getServiceLocator(task));
		ObjectWrapper<OrgType> objectWrapper = factory.createObjectWrapper("org display name", "org description", org,
				ContainerStatus.MODIFYING, task);

		// THEN
		displayThen(TEST_NAME);

		IntegrationTestTools.display("Wrapper after", objectWrapper);

		WrapperTestUtil.assertWrapper(objectWrapper, "org display name", "org description", org, ContainerStatus.MODIFYING);
		assertEquals("wrong number of containers in "+objectWrapper, 12, objectWrapper.getContainers().size());

		ContainerWrapper<OrgType> mainContainerWrapper = objectWrapper.findContainerWrapper(null);
		WrapperTestUtil.assertWrapper(mainContainerWrapper, "prismContainer.mainPanelDisplayName", (ItemPath)null, org, ContainerStatus.MODIFYING);
		assertEquals("wrong number of containers in "+mainContainerWrapper, 1, mainContainerWrapper.getValues().size());
		ContainerValueWrapper<OrgType> mainContainerValueWrapper = mainContainerWrapper.getValues().iterator().next();
		WrapperTestUtil.assertPropertyWrapper(mainContainerValueWrapper, OrgType.F_NAME, PrismTestUtil.createPolyString(ORG_SCUMM_BAR_NAME));
		WrapperTestUtil.assertPropertyWrapper(mainContainerValueWrapper, OrgType.F_TIMEZONE, null);

		ContainerWrapper<ActivationType> activationContainerWrapper = objectWrapper.findContainerWrapper(new ItemPath(OrgType.F_ACTIVATION));
		WrapperTestUtil.assertWrapper(activationContainerWrapper, "ActivationType.activation", OrgType.F_ACTIVATION, org, ContainerStatus.MODIFYING);
		assertEquals("wrong number of containers in "+activationContainerWrapper, 1, activationContainerWrapper.getValues().size());
		ContainerValueWrapper<ActivationType> activationContainerValueWrapper = activationContainerWrapper.getValues().iterator().next();
		WrapperTestUtil.assertPropertyWrapper(activationContainerValueWrapper, ActivationType.F_ADMINISTRATIVE_STATUS, ActivationStatusType.ENABLED);
		WrapperTestUtil.assertPropertyWrapper(activationContainerValueWrapper, ActivationType.F_LOCKOUT_STATUS, null);

		assertEquals("Wrong main container wrapper readOnly", Boolean.FALSE, (Boolean)mainContainerWrapper.isReadonly());

		assertItemWrapperFullConrol(mainContainerValueWrapper, OrgType.F_NAME, true);
		assertItemWrapperFullConrol(mainContainerValueWrapper, OrgType.F_IDENTIFIER, true);
		assertItemWrapperFullConrol(mainContainerValueWrapper, OrgType.F_RISK_LEVEL, false); // not visible, because it is empty
		assertItemWrapperFullConrol(mainContainerValueWrapper, OrgType.F_LOCALITY, true);
		
		assertItemWrapperProcessing(mainContainerValueWrapper, PIRACY_TRANSFORM_DESCRIPTION, null);
		ContainerWrapper<Containerable> transformContainerWrapper = mainContainerValueWrapper.findContainerWrapper(extensionPath(PIRACY_TRANSFORM));
		assertEquals("Wrong processing in item wrapper for "+PIRACY_TRANSFORM, ItemProcessing.MINIMAL, transformContainerWrapper.getProcessing());
		
//		ContainerWrapper<Containerable> transformContainerWrapper = objectWrapper.findContainerWrapper(new ItemPath(PIRACY_TRANSFORM));
//		assertEquals("Wrong processing in item wrapper for "+PIRACY_TRANSFORM, ItemProcessing.MINIMAL, transformContainerWrapper.getProcessing());

		// WHEN
		objectWrapper.setShowEmpty(true);

		// THEN
		assertItemWrapperFullConrol(mainContainerValueWrapper, OrgType.F_NAME, true);
		assertItemWrapperFullConrol(mainContainerValueWrapper, OrgType.F_IDENTIFIER, true);
		assertItemWrapperFullConrol(mainContainerValueWrapper, OrgType.F_RISK_LEVEL, true); // empty
		assertItemWrapperFullConrol(mainContainerValueWrapper, OrgType.F_LOCALITY, true);
		
		ObjectDelta<OrgType> objectDelta = objectWrapper.getObjectDelta();
		display("Delta", objectDelta);
		assertTrue("non-empty delta produced from wrapper: "+objectDelta, objectDelta.isEmpty());
	}

	@Test
    public void test220AssignRoleLandluberToWally() throws Exception {
		final String TEST_NAME = "test220AssignRoleLandluberToWally";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        DummyGroup mapmakers = new DummyGroup(GROUP_DUMMY_MAPMAKERS_NAME);
		dummyResource.addGroup(mapmakers);

        PrismObject<UserType> user = createUser(USER_WALLY_NAME, USER_WALLY_FULLNAME, true);
        addObject(user);
        userWallyOid = user.getOid();
        assignRole(userWallyOid, ROLE_MAPMAKER_OID, task, result);

        // preconditions
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(userWallyOid);
		display("User after change execution", userAfter);
        accountWallyOid = getSingleLinkOid(userAfter);

        PrismObject<ShadowType> shadow = getShadowModel(accountWallyOid);
		shadow.findReference(ShadowType.F_RESOURCE_REF).getValue().setObject(resourceDummy);
		display("Shadow", shadow);

        DummyGroup dummyGroup = dummyResource.getGroupByName(GROUP_DUMMY_MAPMAKERS_NAME);
        assertNotNull("No group on dummy resource", dummyGroup);
        display("Group", dummyGroup);
        assertGroupMember(dummyGroup, USER_WALLY_NAME);

        // WHEN
        displayWhen(TEST_NAME);

        ObjectWrapperFactory factory = new ObjectWrapperFactory(getServiceLocator(task));
		ObjectWrapper<ShadowType> objectWrapper = factory.createObjectWrapper("shadow display name", "shadow description", shadow,
				ContainerStatus.MODIFYING, task);

		// THEN
		displayThen(TEST_NAME);

		display("Wrapper after", objectWrapper);

		WrapperTestUtil.assertWrapper(objectWrapper, "shadow display name", "shadow description", shadow, ContainerStatus.MODIFYING);
		assertEquals("wrong number of containers in "+objectWrapper, 8, objectWrapper.getContainers().size());

		ContainerWrapper<ShadowAttributesType> attributesContainerWrapper = objectWrapper.findContainerWrapper(new ItemPath(ShadowType.F_ATTRIBUTES));
		WrapperTestUtil.assertWrapper(attributesContainerWrapper, "attributes", new ItemPath(ShadowType.F_ATTRIBUTES), shadow.findContainer(ShadowType.F_ATTRIBUTES),
				false, ContainerStatus.MODIFYING);
		assertEquals("wrong number of containers in "+attributesContainerWrapper, 1, attributesContainerWrapper.getValues().size());
		ContainerValueWrapper<ShadowAttributesType> attributesContainerValueWrapper = attributesContainerWrapper.getValues().iterator().next();
		WrapperTestUtil.assertPropertyWrapper(attributesContainerValueWrapper, dummyResourceCtl.getAttributeFullnameQName(), USER_WALLY_FULLNAME);
		WrapperTestUtil.assertPropertyWrapper(attributesContainerValueWrapper, SchemaConstants.ICFS_NAME, USER_WALLY_NAME);
		assertEquals("wrong number of items in "+attributesContainerWrapper, 17, attributesContainerValueWrapper.getItems().size());

		ContainerWrapper<ActivationType> activationContainerWrapper = objectWrapper.findContainerWrapper(new ItemPath(ShadowType.F_ACTIVATION));
		WrapperTestUtil.assertWrapper(activationContainerWrapper, "ShadowType.activation", UserType.F_ACTIVATION, shadow, ContainerStatus.MODIFYING);
		assertEquals("wrong number of containers in "+activationContainerWrapper, 1, activationContainerWrapper.getValues().size());
		ContainerValueWrapper<ActivationType> activationContainerValueWrapper = activationContainerWrapper.getValues().iterator().next();
		WrapperTestUtil.assertPropertyWrapper(activationContainerValueWrapper, ActivationType.F_ADMINISTRATIVE_STATUS, ActivationStatusType.ENABLED);
		WrapperTestUtil.assertPropertyWrapper(activationContainerValueWrapper, ActivationType.F_LOCKOUT_STATUS, null);

		//TODO: fix
		ContainerWrapper<ShadowAssociationType> associationContainerWrapper = objectWrapper.findContainerWrapper(new ItemPath(ShadowType.F_ASSOCIATION));
		assertNotNull("No association container wrapper", associationContainerWrapper);
		assertTrue("Wrong type of group association property wrapper: "+associationContainerWrapper.getClass(), associationContainerWrapper instanceof ShadowAssociationWrapper);
		assertEquals("wrong number of items in "+associationContainerWrapper, 1, associationContainerWrapper.getValues().size());
		ReferenceWrapper groupAssociationWrapper = (ReferenceWrapper) associationContainerWrapper.findPropertyWrapper(RESOURCE_DUMMY_ASSOCIATION_GROUP_QNAME);
		assertNotNull("No group association property wrapper", groupAssociationWrapper);
		List<ValueWrapper> groupAssociationValues = groupAssociationWrapper.getValues();
		assertEquals("wrong number of values in "+groupAssociationWrapper, 1, groupAssociationValues.size());
		ValueWrapper groupAssociationValue = groupAssociationValues.get(0);
		PrismReferenceValue groupAssociationValuePVal = (PrismReferenceValue) groupAssociationValue.getValue();
		display("groupAssociationValuePVal", groupAssociationValuePVal);
		assertEquals("wrong number of values in "+groupAssociationValue, ValueStatus.NOT_CHANGED, groupAssociationValue.getStatus());
		assertEquals("Wrong group association name", RESOURCE_DUMMY_ASSOCIATION_GROUP_QNAME, groupAssociationWrapper.getItemDefinition().getName());
		assertEquals("Wrong group association value", GROUP_DUMMY_MAPMAKERS_NAME, groupAssociationValuePVal.asReferencable().getTargetName().getOrig());
//		PrismContainer<ShadowIdentifiersType> groupAssociationValueIdentifiers = groupAssociationValuePVal.findContainer(ShadowAssociationType.F_IDENTIFIERS);
//		PrismProperty<String> groupAssociationUidProp = groupAssociationValueIdentifiers.findProperty(new QName(null,"uid"));
//		PrismAsserts.assertPropertyValue(groupAssociationValuePVal.asReferencable().getTargetName(), GROUP_DUMMY_MAPMAKERS_NAME);
	}
	
	@Test
    public void test240OrgScummBarModifyTransformDescription() throws Exception {
		final String TEST_NAME = "test240OrgScummBarModifyTransformDescription";
		TestUtil.displayTestTitle(TEST_NAME);
		PrismObject<OrgType> org = getObject(OrgType.class, ORG_SCUMM_BAR_OID);

		Task task = taskManager.createTaskInstance(TEST_NAME);

		ObjectWrapperFactory factory = new ObjectWrapperFactory(getServiceLocator(task));
		ObjectWrapper<OrgType> objectWrapper = factory.createObjectWrapper("org display name", "org description", org,
				ContainerStatus.MODIFYING, task);

		IntegrationTestTools.display("Wrapper before", objectWrapper);

		ContainerWrapper<OrgType> mainContainerWrapper = objectWrapper.findContainerWrapper(null);
		ContainerValueWrapper<OrgType> mainContainerValueWrapper = mainContainerWrapper.getValues().iterator().next();

		modifyPropertyWrapper(mainContainerValueWrapper, PIRACY_TRANSFORM_DESCRIPTION, "Whatever");
		
		IntegrationTestTools.display("Wrapper after", objectWrapper);
		
		// WHEN
		displayWhen(TEST_NAME);
		ObjectDelta<OrgType> objectDelta = objectWrapper.getObjectDelta();
		
		// THEN
		displayThen(TEST_NAME);
		display("Delta", objectDelta);
		ItemPath ahoyPath = new ItemPath(ObjectType.F_EXTENSION, PIRACY_TRANSFORM_DESCRIPTION);
		PrismAsserts.assertPropertyReplace(objectDelta, ahoyPath, "Whatever");
		PrismAsserts.assertModifications(objectDelta, 1);
		
		OperationResult result = task.getResult();
		executeChanges(objectDelta, null, task, result);
		
		assertSuccess(result);
		
		PrismObject<OrgType> orgAfter = getObject(OrgType.class, ORG_MINISTRY_OF_RUM_OID);
		display("Org after", orgAfter);

	}
	
	@Test
    public void test241OrgScummBarModifyTransformProperties() throws Exception {
		final String TEST_NAME = "test241OrgScummBarModifyTransformProperties";
		TestUtil.displayTestTitle(TEST_NAME);
		PrismObject<OrgType> org = getObject(OrgType.class, ORG_SCUMM_BAR_OID);

		Task task = taskManager.createTaskInstance(TEST_NAME);

		ObjectWrapperFactory factory = new ObjectWrapperFactory(getServiceLocator(task));
		ObjectWrapper<OrgType> objectWrapper = factory.createObjectWrapper("org display name", "org description", org,
				ContainerStatus.MODIFYING, task);

		IntegrationTestTools.display("Wrapper before", objectWrapper);

		ContainerWrapper<OrgType> mainContainerWrapper = objectWrapper.findContainerWrapper(null);
		ContainerValueWrapper<OrgType> mainContainerValueWrapper = mainContainerWrapper.getValues().iterator().next();
		ContainerWrapper<Containerable> transformContainerWrapper = mainContainerValueWrapper.findContainerWrapper(extensionPath(PIRACY_TRANSFORM));
		List<ContainerValueWrapper<Containerable>> transformValueWrappers = transformContainerWrapper.getValues();
		assertEquals("Unexpecter number of transform value wrappers", 3, transformValueWrappers.size());
		
		ContainerValueWrapper<Containerable> valueWrapperA = findTransformValueWrapper(transformValueWrappers, "A");
		assertNotNull("No A value wrapper", valueWrapperA);
		display("A value wrapper", valueWrapperA);
		modifyTransformProp(valueWrapperA, PIRACY_REPLACEMENT, "Ahoy");
		
		IntegrationTestTools.display("Wrapper after", objectWrapper);
		
		// WHEN
		displayWhen(TEST_NAME);
		ObjectDelta<OrgType> objectDelta = objectWrapper.getObjectDelta();
		
		// THEN
		displayThen(TEST_NAME);
		display("Delta", objectDelta);
		ItemPath ahoyPath = new ItemPath(ObjectType.F_EXTENSION, PIRACY_TRANSFORM, valueWrapperA.getContainerValue().getId(), PIRACY_REPLACEMENT);
		PrismAsserts.assertPropertyReplace(objectDelta, ahoyPath, "Ahoy");
		PrismAsserts.assertModifications(objectDelta, 1);
		
		OperationResult result = task.getResult();
		executeChanges(objectDelta, null, task, result);
		
		assertSuccess(result);
		
		PrismObject<OrgType> orgAfter = getObject(OrgType.class, ORG_MINISTRY_OF_RUM_OID);
		display("Org after", orgAfter);

	}
	
	@Test
    public void test242OrgScummBarAddTransform() throws Exception {
		final String TEST_NAME = "test242OrgScummBarAddTransform";
		TestUtil.displayTestTitle(TEST_NAME);
		PrismObject<OrgType> org = getObject(OrgType.class, ORG_SCUMM_BAR_OID);

		Task task = taskManager.createTaskInstance(TEST_NAME);

		ObjectWrapperFactory factory = new ObjectWrapperFactory(getServiceLocator(task));
		ObjectWrapper<OrgType> objectWrapper = factory.createObjectWrapper("org display name", "org description", org,
				ContainerStatus.MODIFYING, task);

		IntegrationTestTools.display("Wrapper before", objectWrapper);

		ContainerWrapper<OrgType> mainContainerWrapper = objectWrapper.findContainerWrapper(null);
		ContainerValueWrapper<OrgType> mainContainerValueWrapper = mainContainerWrapper.getValues().iterator().next();
		ContainerWrapper<Containerable> transformContainerWrapper = mainContainerValueWrapper.findContainerWrapper(extensionPath(PIRACY_TRANSFORM));
		List<ContainerValueWrapper<Containerable>> transformValueWrappers = transformContainerWrapper.getValues();
		assertEquals("Unexpecter number of transform value wrappers", 3, transformValueWrappers.size());
		
		ContainerWrapperFactory cwf = new ContainerWrapperFactory(getServiceLocator(task));
		ContainerValueWrapper<Containerable> newContainerValueWrapper = cwf.createContainerValueWrapper(transformContainerWrapper,
				transformContainerWrapper.getItem().createNewValue(), transformContainerWrapper.getObjectStatus(), ValueStatus.ADDED,
				transformContainerWrapper.getPath(), task);
		newContainerValueWrapper.setShowEmpty(true, false);
		transformContainerWrapper.addValue(newContainerValueWrapper);
		modifyTransformProp(newContainerValueWrapper, PIRACY_PATTERN, "D");
		modifyTransformProp(newContainerValueWrapper, PIRACY_REPLACEMENT, "Doubloon");
		
		IntegrationTestTools.display("Wrapper after", objectWrapper);
		
		// WHEN
		displayWhen(TEST_NAME);
		ObjectDelta<OrgType> objectDelta = objectWrapper.getObjectDelta();
		
		// THEN
		displayThen(TEST_NAME);
		display("Delta", objectDelta);
		ItemPath transformPath = new ItemPath(ObjectType.F_EXTENSION, PIRACY_TRANSFORM);
		PrismAsserts.assertModifications(objectDelta, 1);
		ContainerDelta<Containerable> transfromDelta = (ContainerDelta) objectDelta.getModifications().iterator().next();
		assertTrue("Wrong container delta path. Expected "+transformPath+" but was "+transfromDelta.getPath(), transfromDelta.getPath().equivalent(transformPath));
		PrismAsserts.assertNoDelete(transfromDelta);
		PrismAsserts.assertNoReplace(transfromDelta);
		Collection<PrismContainerValue<Containerable>> valuesToAdd = transfromDelta.getValuesToAdd();
		assertEquals("Wrong number of values to add", 1, valuesToAdd.size());
		PrismContainerValue<Containerable> containerValueToAdd = valuesToAdd.iterator().next();
		assertEquals("Unexpected number of items in value to add", 2, containerValueToAdd.getItems().size());
		PrismProperty<String> patternProp = (PrismProperty) containerValueToAdd.findItem(PIRACY_PATTERN);
		PrismAsserts.assertPropertyValue(patternProp, "D");
		PrismProperty<String> replacementProp = (PrismProperty) containerValueToAdd.findItem(PIRACY_REPLACEMENT);
		PrismAsserts.assertPropertyValue(replacementProp, "Doubloon");
		
		OperationResult result = task.getResult();
		executeChanges(objectDelta, null, task, result);
		
		assertSuccess(result);
		
		PrismObject<OrgType> orgAfter = getObject(OrgType.class, ORG_MINISTRY_OF_RUM_OID);
		display("Org after", orgAfter);

		
	}
	
	/**
	 * Ministry of rum has no extension container.
	 */
	@Test
    public void test250OrgMinistryOrRumModifyTransformDescription() throws Exception {
		final String TEST_NAME = "test250OrgMinistryOrRumModifyTransformDescription";
		TestUtil.displayTestTitle(TEST_NAME);
		PrismObject<OrgType> org = getObject(OrgType.class, ORG_MINISTRY_OF_RUM_OID);

		Task task = taskManager.createTaskInstance(TEST_NAME);

		ObjectWrapperFactory factory = new ObjectWrapperFactory(getServiceLocator(task));
		ObjectWrapper<OrgType> objectWrapper = factory.createObjectWrapper("org display name", "org description", org,
				ContainerStatus.MODIFYING, task);

		IntegrationTestTools.display("Wrapper before", objectWrapper);

		ContainerWrapper<OrgType> mainContainerWrapper = objectWrapper.findContainerWrapper(null);
		ContainerValueWrapper<OrgType> mainContainerValueWrapper = mainContainerWrapper.getValues().iterator().next();

		modifyPropertyWrapper(mainContainerValueWrapper, PIRACY_TRANSFORM_DESCRIPTION, "Whatever");
		
		IntegrationTestTools.display("Wrapper after", objectWrapper);
		
		// WHEN
		displayWhen(TEST_NAME);
		ObjectDelta<OrgType> objectDelta = objectWrapper.getObjectDelta();
		
		// THEN
		displayThen(TEST_NAME);
		display("Delta", objectDelta);
		ItemPath ahoyPath = new ItemPath(ObjectType.F_EXTENSION, PIRACY_TRANSFORM_DESCRIPTION);
		PrismAsserts.assertPropertyReplace(objectDelta, ahoyPath, "Whatever");
		PrismAsserts.assertModifications(objectDelta, 1);
		
		OperationResult result = task.getResult();
		executeChanges(objectDelta, null, task, result);
		
		assertSuccess(result);
		
		PrismObject<OrgType> orgAfter = getObject(OrgType.class, ORG_MINISTRY_OF_RUM_OID);
		display("Org after", orgAfter);
		
	}

	private void modifyPropertyWrapper(ContainerValueWrapper<OrgType> mainContainerValueWrapper, QName propQName, String newValue) {
		PropertyOrReferenceWrapper propertyWrapper = mainContainerValueWrapper.findPropertyWrapper(propQName);
		List<ValueWrapper> values = propertyWrapper.getValues();
		if (values.size() == 1) {
			PrismPropertyValue<String> pval = (PrismPropertyValue<String>) values.get(0).getValue();
			pval.setValue(newValue);
		} else if (values.isEmpty()) {
			PrismPropertyValue<String> pval = new PrismPropertyValue<>(newValue);
			ValueWrapper newValueWrapper = new ValueWrapper<>(propertyWrapper, pval);
			values.add(newValueWrapper);
			newValueWrapper.setStatus(ValueStatus.ADDED);
		} else {
			throw new IllegalArgumentException("Cannot use on multivalue props");
		}
	}
	
	private ContainerValueWrapper<Containerable> findTransformValueWrapper(
			List<ContainerValueWrapper<Containerable>> transformValueWrappers, String pattern) {
		for (ContainerValueWrapper<Containerable> transformValueWrapper: transformValueWrappers) {
			PropertyOrReferenceWrapper patternPropWrapper = transformValueWrapper.findPropertyWrapper(PIRACY_PATTERN);
			PrismProperty<String> patternProperty = (PrismProperty<String>) patternPropWrapper.getItem();
			if (pattern.equals(patternProperty.getRealValue())) {
				return transformValueWrapper;
			}
		}
		return null;
	}

	private void modifyTransformProp(ContainerValueWrapper<Containerable> transformValueWrapper, QName prop, String newReplacement) {
		PropertyOrReferenceWrapper replacementPropWrapper = transformValueWrapper.findPropertyWrapper(prop);
		List<ValueWrapper> values = replacementPropWrapper.getValues();
		PrismPropertyValue<String> prismValue = (PrismPropertyValue<String>) values.get(0).getValue();
		prismValue.setValue(newReplacement);
	}
	
	/**
	 * MID-3126
	 */
	@Test
    public void test800EditSchemaJackPropReadAllModifySomeUser() throws Exception {
		final String TEST_NAME = "test800EditSchemaJackPropReadAllModifySomeUser";
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PROP_READ_ALL_MODIFY_SOME_USER_OID);
        login(USER_JACK_USERNAME);

        Task task = createTask(TEST_NAME);
        ObjectWrapperFactory factory = new ObjectWrapperFactory(getServiceLocator(task));
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("user before", user);

        // WHEN
        displayWhen(TEST_NAME);

		ObjectWrapper<UserType> objectWrapper = factory.createObjectWrapper("user display name", "user description", user,
				ContainerStatus.MODIFYING, task);

		// THEN
		displayThen(TEST_NAME);

		IntegrationTestTools.display("Wrapper after", objectWrapper);
		assertEquals("Wrong object wrapper readOnly", Boolean.FALSE, (Boolean)objectWrapper.isReadonly());

		ContainerWrapper<UserType> mainContainerWrapper = objectWrapper.findMainContainerWrapper();
		assertEquals("Wrong main container wrapper readOnly", Boolean.FALSE, (Boolean)mainContainerWrapper.isReadonly());

		ContainerValueWrapper<UserType> mainContainerValueWrapper = mainContainerWrapper.getValues().iterator().next();
		
		ItemWrapper nameWrapper = mainContainerValueWrapper.findPropertyWrapper(UserType.F_NAME);
		assertEquals("Wrong name readOnly", Boolean.TRUE, (Boolean)nameWrapper.isReadonly()); // Is this OK?
		assertEquals("Wrong name visible", Boolean.TRUE, (Boolean)nameWrapper.isVisible());
		assertEquals("Wrong name definition.canRead", Boolean.TRUE, (Boolean)nameWrapper.getItemDefinition().canRead());
		assertEquals("Wrong name definition.canAdd", Boolean.FALSE, (Boolean)nameWrapper.getItemDefinition().canAdd());
		assertEquals("Wrong name definition.canModify", Boolean.FALSE, (Boolean)nameWrapper.getItemDefinition().canModify());

		ItemWrapper givenNameWrapper = mainContainerValueWrapper.findPropertyWrapper(UserType.F_GIVEN_NAME);
		assertEquals("Wrong givenName readOnly", Boolean.TRUE, (Boolean)givenNameWrapper.isReadonly()); // Is this OK?
		assertEquals("Wrong givenName visible", Boolean.TRUE, (Boolean)givenNameWrapper.isVisible());
		assertEquals("Wrong givenName definition.canRead", Boolean.TRUE, (Boolean)givenNameWrapper.getItemDefinition().canRead());
		assertEquals("Wrong givenName definition.canAdd", Boolean.FALSE, (Boolean)givenNameWrapper.getItemDefinition().canAdd());
		assertEquals("Wrong givenName definition.canModify", Boolean.FALSE, (Boolean)givenNameWrapper.getItemDefinition().canModify());

		ItemWrapper fullNameWrapper = mainContainerValueWrapper.findPropertyWrapper(UserType.F_FULL_NAME);
		assertEquals("Wrong fullName readOnly", Boolean.FALSE, (Boolean)fullNameWrapper.isReadonly());
		assertEquals("Wrong fullName visible", Boolean.TRUE, (Boolean)fullNameWrapper.isVisible());
		assertEquals("Wrong fullName definition.canRead", Boolean.TRUE, (Boolean)fullNameWrapper.getItemDefinition().canRead());
		assertEquals("Wrong fullName definition.canAdd", Boolean.FALSE, (Boolean)fullNameWrapper.getItemDefinition().canAdd());
		assertEquals("Wrong fullName definition.canModify", Boolean.TRUE, (Boolean)fullNameWrapper.getItemDefinition().canModify());

		ItemWrapper additionalNameWrapper = mainContainerValueWrapper.findPropertyWrapper(UserType.F_ADDITIONAL_NAME);
		assertEquals("Wrong additionalName readOnly", Boolean.TRUE, (Boolean)additionalNameWrapper.isReadonly()); // Is this OK?
		assertEquals("Wrong additionalName visible", Boolean.FALSE, (Boolean)additionalNameWrapper.isVisible()); // not visible, because it is empty
		assertEquals("Wrong additionalName definition.canRead", Boolean.TRUE, (Boolean)additionalNameWrapper.getItemDefinition().canRead());
		assertEquals("Wrong additionalName definition.canAdd", Boolean.FALSE, (Boolean)additionalNameWrapper.getItemDefinition().canAdd());
		assertEquals("Wrong additionalName definition.canModify", Boolean.FALSE, (Boolean)additionalNameWrapper.getItemDefinition().canModify());

		ItemWrapper localityNameWrapper = mainContainerValueWrapper.findPropertyWrapper(UserType.F_LOCALITY);
		assertEquals("Wrong locality readOnly", Boolean.TRUE, (Boolean)localityNameWrapper.isReadonly());
		assertEquals("Wrong locality visible", Boolean.TRUE, (Boolean)localityNameWrapper.isVisible());
		assertEquals("Wrong locality definition.canRead", Boolean.TRUE, (Boolean)localityNameWrapper.getItemDefinition().canRead());
		assertEquals("Wrong locality definition.canAdd", Boolean.FALSE, (Boolean)localityNameWrapper.getItemDefinition().canAdd());
		assertEquals("Wrong locality definition.canModify", Boolean.FALSE, (Boolean)localityNameWrapper.getItemDefinition().canModify());

		// WHEN
		objectWrapper.setShowEmpty(true);
		mainContainerWrapper.setShowEmpty(true, true);

		// THEN
		additionalNameWrapper = mainContainerWrapper.findPropertyWrapper(UserType.F_ADDITIONAL_NAME);
		assertEquals("Wrong additionalName visible", Boolean.TRUE, (Boolean)additionalNameWrapper.isVisible()); // visible, because show empty

	}

	/**
	 * MID-3126
	 */
	@Test
    public void test802EditSchemaJackPropReadSomeModifySomeUser() throws Exception {
		final String TEST_NAME = "test800EditSchemaJackPropReadAllModifySomeUser";
        displayTestTitle(TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PROP_READ_SOME_MODIFY_SOME_USER_OID);
        login(USER_JACK_USERNAME);

        Task task = createTask(TEST_NAME);
        ObjectWrapperFactory factory = new ObjectWrapperFactory(getServiceLocator(task));
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("user before", user);

        // WHEN
        displayWhen(TEST_NAME);

		ObjectWrapper<UserType> objectWrapper = factory.createObjectWrapper("user display name", "user description", user,
				ContainerStatus.MODIFYING, task);

		// THEN
		displayThen(TEST_NAME);

		IntegrationTestTools.display("Wrapper after", objectWrapper);
		assertEquals("Wrong object wrapper readOnly", Boolean.FALSE, (Boolean)objectWrapper.isReadonly());

		ContainerWrapper<UserType> mainContainerWrapper = objectWrapper.findMainContainerWrapper();
		assertEquals("Wrong main container wrapper readOnly", Boolean.FALSE, (Boolean)mainContainerWrapper.isReadonly());

		ContainerValueWrapper<UserType> mainContainerValueWrapper = mainContainerWrapper.getValues().iterator().next();
		ItemWrapper nameWrapper = mainContainerValueWrapper.findPropertyWrapper(UserType.F_NAME);
		assertEquals("Wrong name readOnly", Boolean.TRUE, (Boolean)nameWrapper.isReadonly());
		assertEquals("Wrong name visible", Boolean.TRUE, (Boolean)nameWrapper.isVisible());
		assertEquals("Wrong name definition.canRead", Boolean.TRUE, (Boolean)nameWrapper.getItemDefinition().canRead());
		assertEquals("Wrong name definition.canAdd", Boolean.FALSE, (Boolean)nameWrapper.getItemDefinition().canAdd());
		assertEquals("Wrong name definition.canModify", Boolean.FALSE, (Boolean)nameWrapper.getItemDefinition().canModify());

		ItemWrapper givenNameWrapper = mainContainerValueWrapper.findPropertyWrapper(UserType.F_GIVEN_NAME);
		assertEquals("Wrong givenName readOnly", Boolean.TRUE, (Boolean)givenNameWrapper.isReadonly());
		// Emphasized property. But the role given no access to this. Therefore is should not be visible.
		// MID-3206
		assertEquals("Wrong givenName visible", Boolean.FALSE, (Boolean)givenNameWrapper.isVisible());
		assertEquals("Wrong givenName definition.canRead", Boolean.FALSE, (Boolean)givenNameWrapper.getItemDefinition().canRead());
		assertEquals("Wrong givenName definition.canAdd", Boolean.FALSE, (Boolean)givenNameWrapper.getItemDefinition().canAdd());
		assertEquals("Wrong givenName definition.canModify", Boolean.FALSE, (Boolean)givenNameWrapper.getItemDefinition().canModify());

		ItemWrapper fullNameWrapper = mainContainerValueWrapper.findPropertyWrapper(UserType.F_FULL_NAME);
		assertEquals("Wrong fullName readOnly", Boolean.FALSE, (Boolean)fullNameWrapper.isReadonly());
		assertEquals("Wrong fullName visible", Boolean.TRUE, (Boolean)fullNameWrapper.isVisible());
		assertEquals("Wrong fullName definition.canRead", Boolean.TRUE, (Boolean)fullNameWrapper.getItemDefinition().canRead());
		assertEquals("Wrong fullName definition.canAdd", Boolean.FALSE, (Boolean)fullNameWrapper.getItemDefinition().canAdd());
		assertEquals("Wrong fullName definition.canModify", Boolean.TRUE, (Boolean)fullNameWrapper.getItemDefinition().canModify());

		ItemWrapper additionalNameWrapper = mainContainerValueWrapper.findPropertyWrapper(UserType.F_ADDITIONAL_NAME);
		assertEquals("Wrong additionalName readOnly", Boolean.FALSE, (Boolean)additionalNameWrapper.isReadonly());
		assertEquals("Wrong additionalName visible", Boolean.FALSE, (Boolean)additionalNameWrapper.isVisible());
		assertEquals("Wrong additionalName definition.canRead", Boolean.FALSE, (Boolean)additionalNameWrapper.getItemDefinition().canRead());
		assertEquals("Wrong additionalName definition.canAdd", Boolean.FALSE, (Boolean)additionalNameWrapper.getItemDefinition().canAdd());
		assertEquals("Wrong additionalName definition.canModify", Boolean.TRUE, (Boolean)additionalNameWrapper.getItemDefinition().canModify());

		ItemWrapper localityNameWrapper = mainContainerValueWrapper.findPropertyWrapper(UserType.F_LOCALITY);
		assertEquals("Wrong locality readOnly", Boolean.TRUE, (Boolean)localityNameWrapper.isReadonly()); // Is this OK?
		assertEquals("Wrong locality visible", Boolean.FALSE, (Boolean)localityNameWrapper.isVisible());
		assertEquals("Wrong locality definition.canRead", Boolean.FALSE, (Boolean)localityNameWrapper.getItemDefinition().canRead());
		assertEquals("Wrong locality definition.canAdd", Boolean.FALSE, (Boolean)localityNameWrapper.getItemDefinition().canAdd());
		assertEquals("Wrong locality definition.canModify", Boolean.FALSE, (Boolean)localityNameWrapper.getItemDefinition().canModify());
	}

	private <C extends Containerable> void assertItemWrapperFullConrol(ContainerValueWrapper<C> containerWrapper, QName propName,
			boolean visible) {
		ItemWrapper itemWrapper = containerWrapper.findPropertyWrapper(propName);
		assertEquals("Wrong "+propName+" readOnly", Boolean.FALSE, (Boolean)itemWrapper.isReadonly());
		assertEquals("Wrong "+propName+" visible", visible, itemWrapper.isVisible());
		assertEquals("Wrong "+propName+" definition.canRead", Boolean.TRUE, (Boolean)itemWrapper.getItemDefinition().canRead());
		assertEquals("Wrong "+propName+" definition.canAdd", Boolean.TRUE, (Boolean)itemWrapper.getItemDefinition().canAdd());
		assertEquals("Wrong "+propName+" definition.canModify", Boolean.TRUE, (Boolean)itemWrapper.getItemDefinition().canModify());
	}
	
	private <F extends FocusType> void assertItemWrapperProcessing(ContainerValueWrapper<F> containerWrapper,
			QName propName, ItemProcessing expectedProcessing) {
		ItemWrapper itemWrapper = containerWrapper.findPropertyWrapper(propName);
		if (expectedProcessing == ItemProcessing.IGNORE) {
			assertNull("Unexpected ignored item wrapper for "+propName, itemWrapper);
		} else {
			assertEquals("Wrong processing in item wrapper for "+propName, expectedProcessing, itemWrapper.getProcessing());
		}
	}


	private void cleanupAutzTest(String userOid) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException, IOException {
		login(userAdministrator);
        unassignAllRoles(userOid);
	}
}
