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

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapperFactory;
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
	}

	private <O extends ObjectType> void assertWrapper(ObjectWrapper<O> objectWrapper, String displayName, String description, PrismObject<O> object,
			ContainerStatus status) {
		assertNotNull("null wrapper", objectWrapper);
		assertEquals("Wrong object in wrapper "+objectWrapper, object, objectWrapper.getObject());
		assertEquals("Wrong object in wrapper "+objectWrapper, object, objectWrapper.getObjectOld());
		assertEquals("Wrong displayName in wrapper "+objectWrapper, displayName, objectWrapper.getDisplayName());
		assertEquals("Wrong description in wrapper "+objectWrapper, description, objectWrapper.getDescription());
		assertEquals("Wrong status in wrapper "+objectWrapper, status, objectWrapper.getStatus());
		assertNull("Unexpected old delta in "+objectWrapper, objectWrapper.getOldDelta());
	}

}
