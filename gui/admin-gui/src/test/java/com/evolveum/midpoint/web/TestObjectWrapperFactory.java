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

import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ItemWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapperFactory;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
public class TestObjectWrapperFactory extends AbstractGuiUnitTest {
	
	@Test
    public void testCreateWrapperUser() throws Exception {
		final String TEST_NAME = "testCreateWrapperUser";
		TestUtil.displayTestTile(TEST_NAME);
		
		PrismObject<UserType> user = PrismTestUtil.parseObject(USER_JACK_FILE);
		PrismObjectDefinition<UserType> objDef = user.getDefinition();
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
		ObjectWrapperFactory factory = new ObjectWrapperFactory(null);
		ObjectWrapper<UserType> userWrapper = factory.createObjectWrapper("user display name", "user description", user, 
				objDef, null, ContainerStatus.MODIFYING, false);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		
		IntegrationTestTools.display("Wrapper after", userWrapper);
		
		assertWrapper(userWrapper, "user display name", "user description", user, ContainerStatus.MODIFYING);
		assertEquals("wrong number of containers in "+userWrapper, 10, userWrapper.getContainers().size());
		
		ContainerWrapper mainContainerWrapper = userWrapper.findContainerWrapper(null);
		assertWrapper(mainContainerWrapper, "user", (ItemPath)null, user, ContainerStatus.MODIFYING);
		assertPropertyWrapper(mainContainerWrapper, UserType.F_NAME, PrismTestUtil.createPolyString("jack"));
		assertPropertyWrapper(mainContainerWrapper, UserType.F_TIMEZONE, null);
		
		ContainerWrapper<ActivationType> activationContainerWrapper = userWrapper.findContainerWrapper(new ItemPath(UserType.F_ACTIVATION));
		assertWrapper(activationContainerWrapper, "Activation", UserType.F_ACTIVATION, user, ContainerStatus.MODIFYING);
		assertPropertyWrapper(activationContainerWrapper, ActivationType.F_ADMINISTRATIVE_STATUS, ActivationStatusType.ENABLED);
		assertPropertyWrapper(activationContainerWrapper, ActivationType.F_LOCKOUT_STATUS, null);
	}

	private <C extends Containerable,T> void assertPropertyWrapper(ContainerWrapper<C> containerWrapper, QName itemName, T... expectedValues) {
		ItemWrapper itemWrapper = containerWrapper.findPropertyWrapper(itemName);
		assertNotNull("No item wrapper "+itemName+" in "+containerWrapper, itemWrapper);
		List<ValueWrapper> valueWrappers = itemWrapper.getValues();
		assertPropertyWrapperValues("item wrapper "+itemName+" in "+containerWrapper, valueWrappers, expectedValues);
	}
	
	private <C extends Containerable,T> void assertPropertyWrapperValues(String desc, List<ValueWrapper> valueWrappers, T... expectedValues) {
		if (expectedValues == null) {
			expectedValues = (T[]) new Object[] { null };
		}
		assertEquals("Wrong number of values in "+desc+"; was: "+valueWrappers+", expected: "+Arrays.toString(expectedValues), expectedValues.length, valueWrappers.size());
		if (expectedValues.length == 0) {
			return;
		}
		for (ValueWrapper vw: valueWrappers) {
			PrismValue actualPval = vw.getValue();
			if (actualPval instanceof PrismPropertyValue<?>) {
				T actualValue = ((PrismPropertyValue<T>)actualPval).getValue();
				boolean found = false;
				for (T expectedValue: expectedValues) {
					if (MiscUtil.equals(expectedValue, actualValue)) {
						found = true;
					}
				}
				if (!found) {
					AssertJUnit.fail("Unexpected value "+actualValue+" in value wrapper in "+desc+"; was: "+valueWrappers+", expected: "+Arrays.toString(expectedValues));
				}
			} else {
				AssertJUnit.fail("expected PrismPropertyValue in value wrapper in "+desc+", but got "+actualPval.getClass());
			}
			
		}
	}

	private <C extends Containerable, O extends ObjectType> void assertWrapper(ContainerWrapper<C> containerWrapper, String displayName, 
			QName itemName, PrismObject<O> object, ContainerStatus status) {
		assertWrapper(containerWrapper, displayName, itemName==null?null:new ItemPath(itemName), object, status);
	}
	
	private <C extends Containerable, O extends ObjectType> void assertWrapper(ContainerWrapper<C> containerWrapper, String displayName, 
			ItemPath expectedPath, PrismObject<O> object, ContainerStatus status) {
		PrismContainer<C> container;
		if (expectedPath == null) {
			container = (PrismContainer<C>) object;
		} else {
			container = object.findContainer(expectedPath);
		}
		assertWrapper(containerWrapper, displayName, expectedPath, container, expectedPath==null, status);
	}
	
	private <C extends Containerable> void assertWrapper(ContainerWrapper<C> containerWrapper, String displayName, ItemPath expectedPath, 
			PrismContainer<C> container, boolean isMain, ContainerStatus status) {
		assertNotNull("null wrapper", containerWrapper);
		assertEquals("Wrong main flag in wrapper "+containerWrapper, expectedPath, containerWrapper.getPath());
		assertEquals("Wrong main flag in wrapper "+containerWrapper, isMain, containerWrapper.isMain());
		assertEquals("Wrong item in wrapper "+containerWrapper, container, containerWrapper.getItem());
		assertEquals("Wrong displayName in wrapper "+containerWrapper, displayName, containerWrapper.getDisplayName());
		assertEquals("Wrong status in wrapper "+containerWrapper, status, containerWrapper.getStatus());
	}

	private <O extends ObjectType> void assertWrapper(ObjectWrapper<O> objectWrapper, String displayName, String description, PrismObject<O> object,
			ContainerStatus status) {
		assertNotNull("null wrapper", objectWrapper);
		assertEquals("Wrong object in wrapper "+objectWrapper, object, objectWrapper.getObject());
		assertEquals("Wrong old object in wrapper "+objectWrapper, object, objectWrapper.getObjectOld());
		assertFalse("object and old object not clonned in "+objectWrapper, objectWrapper.getObject() == objectWrapper.getObjectOld());
		assertEquals("Wrong displayName in wrapper "+objectWrapper, displayName, objectWrapper.getDisplayName());
		assertEquals("Wrong description in wrapper "+objectWrapper, description, objectWrapper.getDescription());
		assertEquals("Wrong status in wrapper "+objectWrapper, status, objectWrapper.getStatus());
		assertNull("Unexpected old delta in "+objectWrapper, objectWrapper.getOldDelta());
	}

}
