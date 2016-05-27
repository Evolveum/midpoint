/**
 * Copyright (c) 2016 Evolveum
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

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.web.AdminGuiTestConstants.*;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;

import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.web.component.prism.AssociationWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ItemWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapperFactory;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowIdentifiersType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-admin-gui-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestIntegrationObjectWrapperFactory extends AbstractInitializedGuiIntegrationTest {
	
	private static final String USER_WALLY_NAME = "wally";
	private static final String USER_WALLY_FULLNAME = "Wally B. Feed";
	
	public static final String GROUP_DUMMY_MAPMAKERS_NAME = "mapmakers";
	private String userWallyOid;
	private String accountWallyOid;
	
	@Test
    public void test100CreateWrapperUser() throws Exception {
		final String TEST_NAME = "test100CreateWrapperUser";
		TestUtil.displayTestTile(TEST_NAME);
		
		PrismObject<UserType> user = getUser(USER_JACK_OID);
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
		ObjectWrapperFactory factory = new ObjectWrapperFactory(getServiceLocator());
		ObjectWrapper<UserType> objectWrapper = factory.createObjectWrapper("user display name", "user description", user, 
				ContainerStatus.MODIFYING);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		
		IntegrationTestTools.display("Wrapper after", objectWrapper);
		
		WrapperTestUtil.assertWrapper(objectWrapper, "user display name", "user description", user, ContainerStatus.MODIFYING);
		assertEquals("wrong number of containers in "+objectWrapper, 10, objectWrapper.getContainers().size());
		
		ContainerWrapper mainContainerWrapper = objectWrapper.findContainerWrapper(null);
		WrapperTestUtil.assertWrapper(mainContainerWrapper, "prismContainer.mainPanelDisplayName", (ItemPath)null, user, ContainerStatus.MODIFYING);
		WrapperTestUtil.assertPropertyWrapper(mainContainerWrapper, UserType.F_NAME, PrismTestUtil.createPolyString("jack"));
		WrapperTestUtil.assertPropertyWrapper(mainContainerWrapper, UserType.F_TIMEZONE, null);
		
		ContainerWrapper<ActivationType> activationContainerWrapper = objectWrapper.findContainerWrapper(new ItemPath(UserType.F_ACTIVATION));
		WrapperTestUtil.assertWrapper(activationContainerWrapper, "ActivationType.activation", UserType.F_ACTIVATION, user, ContainerStatus.MODIFYING);
		WrapperTestUtil.assertPropertyWrapper(activationContainerWrapper, ActivationType.F_ADMINISTRATIVE_STATUS, ActivationStatusType.ENABLED);
		WrapperTestUtil.assertPropertyWrapper(activationContainerWrapper, ActivationType.F_LOCKOUT_STATUS, null);
	}
	
	@Test
    public void test150CreateWrapperShadow() throws Exception {
		final String TEST_NAME = "test150CreateWrapperShadow";
		TestUtil.displayTestTile(TEST_NAME);
		
		PrismObject<ShadowType> shadow = getShadowModel(accountJackOid);
		shadow.findReference(ShadowType.F_RESOURCE_REF).getValue().setObject(resourceDummy);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
		ObjectWrapperFactory factory = new ObjectWrapperFactory(getServiceLocator());
		ObjectWrapper<ShadowType> objectWrapper = factory.createObjectWrapper("shadow display name", "shadow description", shadow, 
				ContainerStatus.MODIFYING);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		
		display("Wrapper after", objectWrapper);
		
		WrapperTestUtil.assertWrapper(objectWrapper, "shadow display name", "shadow description", shadow, ContainerStatus.MODIFYING);
		assertEquals("wrong number of containers in "+objectWrapper, 9, objectWrapper.getContainers().size());
		
		ContainerWrapper attributesContainerWrapper = objectWrapper.findContainerWrapper(new ItemPath(ShadowType.F_ATTRIBUTES));
		WrapperTestUtil.assertWrapper(attributesContainerWrapper, "prismContainer.shadow.mainPanelDisplayName", new ItemPath(ShadowType.F_ATTRIBUTES), shadow.findContainer(ShadowType.F_ATTRIBUTES),
				true, ContainerStatus.MODIFYING);
		WrapperTestUtil.assertPropertyWrapper(attributesContainerWrapper, dummyResourceCtl.getAttributeFullnameQName(), USER_JACK_FULL_NAME);
		WrapperTestUtil.assertPropertyWrapper(attributesContainerWrapper, SchemaConstants.ICFS_NAME, USER_JACK_USERNAME);
		assertEquals("wrong number of items in "+attributesContainerWrapper, 16, attributesContainerWrapper.getItems().size());
		
		ContainerWrapper<ActivationType> activationContainerWrapper = objectWrapper.findContainerWrapper(new ItemPath(UserType.F_ACTIVATION));
		WrapperTestUtil.assertWrapper(activationContainerWrapper, "ShadowType.activation", UserType.F_ACTIVATION, shadow, ContainerStatus.MODIFYING);
		WrapperTestUtil.assertPropertyWrapper(activationContainerWrapper, ActivationType.F_ADMINISTRATIVE_STATUS, ActivationStatusType.ENABLED);
		WrapperTestUtil.assertPropertyWrapper(activationContainerWrapper, ActivationType.F_LOCKOUT_STATUS, null);
	}

	@Test
    public void test220AssignRoleLandluberToWally() throws Exception {
		final String TEST_NAME = "test220AssignRoleLandluberToWally";
        TestUtil.displayTestTile(this, TEST_NAME);

        Task task = taskManager.createTaskInstance(TestIntegrationObjectWrapperFactory.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        DummyGroup mapmakers = new DummyGroup(GROUP_DUMMY_MAPMAKERS_NAME);
		dummyResource.addGroup(mapmakers);
        
        PrismObject<UserType> user = createUser(USER_WALLY_NAME, USER_WALLY_FULLNAME, true);
        addObject(user);
        userWallyOid = user.getOid();
        assignRole(userWallyOid, ROLE_MAPMAKER_OID, task, result);
        
        // preconditions
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
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
        TestUtil.displayWhen(TEST_NAME);
        
        ObjectWrapperFactory factory = new ObjectWrapperFactory(getServiceLocator());
		ObjectWrapper<ShadowType> objectWrapper = factory.createObjectWrapper("shadow display name", "shadow description", shadow, 
				ContainerStatus.MODIFYING);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		
		display("Wrapper after", objectWrapper);
		
		WrapperTestUtil.assertWrapper(objectWrapper, "shadow display name", "shadow description", shadow, ContainerStatus.MODIFYING);
		assertEquals("wrong number of containers in "+objectWrapper, 9, objectWrapper.getContainers().size());
		
		ContainerWrapper attributesContainerWrapper = objectWrapper.findContainerWrapper(new ItemPath(ShadowType.F_ATTRIBUTES));
		WrapperTestUtil.assertWrapper(attributesContainerWrapper, "prismContainer.shadow.mainPanelDisplayName", new ItemPath(ShadowType.F_ATTRIBUTES), shadow.findContainer(ShadowType.F_ATTRIBUTES),
				true, ContainerStatus.MODIFYING);
		WrapperTestUtil.assertPropertyWrapper(attributesContainerWrapper, dummyResourceCtl.getAttributeFullnameQName(), USER_WALLY_FULLNAME);
		WrapperTestUtil.assertPropertyWrapper(attributesContainerWrapper, SchemaConstants.ICFS_NAME, USER_WALLY_NAME);
		assertEquals("wrong number of items in "+attributesContainerWrapper, 16, attributesContainerWrapper.getItems().size());
		
		ContainerWrapper<ActivationType> activationContainerWrapper = objectWrapper.findContainerWrapper(new ItemPath(UserType.F_ACTIVATION));
		WrapperTestUtil.assertWrapper(activationContainerWrapper, "ShadowType.activation", UserType.F_ACTIVATION, shadow, ContainerStatus.MODIFYING);
		WrapperTestUtil.assertPropertyWrapper(activationContainerWrapper, ActivationType.F_ADMINISTRATIVE_STATUS, ActivationStatusType.ENABLED);
		WrapperTestUtil.assertPropertyWrapper(activationContainerWrapper, ActivationType.F_LOCKOUT_STATUS, null);
		
		ContainerWrapper<ShadowAssociationType> associationContainerWrapper = objectWrapper.findContainerWrapper(new ItemPath(ShadowType.F_ASSOCIATION));
		assertNotNull("No association container wrapper", associationContainerWrapper);
		assertEquals("wrong number of items in "+associationContainerWrapper, 2, associationContainerWrapper.getItems().size());
		ItemWrapper groupAssociationWrapper = associationContainerWrapper.findPropertyWrapper(RESOURCE_DUMMY_ASSOCIATION_GROUP_QNAME);
		assertNotNull("No group association property wrapper", groupAssociationWrapper);
		assertTrue("Wrong type of group association property wrapper: "+groupAssociationWrapper.getClass(), groupAssociationWrapper instanceof AssociationWrapper);
		List<ValueWrapper> groupAssociationValues = groupAssociationWrapper.getValues();
		assertEquals("wrong number of values in "+groupAssociationWrapper, 1, groupAssociationValues.size());
		ValueWrapper groupAssociationValue = groupAssociationValues.get(0);
		PrismContainerValue<ShadowAssociationType> groupAssociationValuePVal = (PrismContainerValue<ShadowAssociationType>) groupAssociationValue.getValue();
		display("groupAssociationValuePVal", groupAssociationValuePVal);
		assertEquals("wrong number of values in "+groupAssociationValue, ValueStatus.NOT_CHANGED, groupAssociationValue.getStatus());
		assertEquals("Wrong group association name", RESOURCE_DUMMY_ASSOCIATION_GROUP_QNAME, groupAssociationValuePVal.findProperty(ShadowAssociationType.F_NAME).getRealValue());
		PrismContainer<ShadowIdentifiersType> groupAssociationValueIdentifiers = groupAssociationValuePVal.findContainer(ShadowAssociationType.F_IDENTIFIERS);
		PrismProperty<String> groupAssociationUidProp = groupAssociationValueIdentifiers.findProperty(new QName(null,"uid"));
		PrismAsserts.assertPropertyValue(groupAssociationUidProp, GROUP_DUMMY_MAPMAKERS_NAME);
		
	}

}
