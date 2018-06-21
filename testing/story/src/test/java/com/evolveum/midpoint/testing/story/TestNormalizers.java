package com.evolveum.midpoint.testing.story;
/*
 * Copyright (c) 2018 Evolveum
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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;

import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.polystring.Ascii7PolyStringNormalizer;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestNormalizers extends AbstractModelIntegrationTest {

	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "normalizers");
	
	public static final File SYSTEM_CONFIGURATION_NORMALIZER_ASCII7_FILE = new File(TEST_DIR, "system-configuration-normalizer-ascii7.xml");
	public static final String SYSTEM_CONFIGURATION_NORMALIZER_ASCII7_OID = SystemObjectsType.SYSTEM_CONFIGURATION.value();

	protected static final File USER_TELEKE_FILE = new File(TEST_DIR, "user-teleke.xml");
	protected static final String USER_TELEKE_OID = "e1b0b0a4-17c6-11e8-bfc9-efaa710b614a";
	protected static final String USER_TELEKE_USERNAME = "Téleké";
	
	protected PrismObject<UserType> userAdministrator;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		// System Configuration
		try {
			repoAddObjectFromFile(getSystemConfigurationFile(), initResult);
		} catch (ObjectAlreadyExistsException e) {
			throw new ObjectAlreadyExistsException("System configuration already exists in repository;" +
					"looks like the previous test haven't cleaned it up", e);
		}
		
		modelService.postInit(initResult);

		// User administrator
		userAdministrator = repoAddObjectFromFile(AbstractStoryTest.USER_ADMINISTRATOR_FILE, initResult);
		repoAddObjectFromFile(AbstractStoryTest.ROLE_SUPERUSER_FILE, initResult);
		login(userAdministrator);
	}
	
	protected File getSystemConfigurationFile() {
		return SYSTEM_CONFIGURATION_NORMALIZER_ASCII7_FILE;
	}

	@Test
	public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
		displayTestTitle(TEST_NAME);
		
		PolyStringNormalizer prismNormalizer = prismContext.getDefaultPolyStringNormalizer();
		assertTrue("Wrong normalizer class, expected Ascii7PolyStringNormalizer, but was "+prismNormalizer.getClass(), 
				prismNormalizer instanceof Ascii7PolyStringNormalizer);
	}
	
	@Test
	public void test100AddUserJack() throws Exception {
		final String TEST_NAME = "test100AddUserJack";
		displayTestTitle(TEST_NAME);
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		// WHEN
		displayWhen(TEST_NAME);
		
		addObject(AbstractStoryTest.USER_JACK_FILE, task, result);
		
		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		PrismObject<UserType> userAfter = getUserFromRepo(AbstractStoryTest.USER_JACK_OID);
		display("User after jack (repo)", userAfter);
		assertPolyString(userAfter, UserType.F_NAME, AbstractStoryTest.USER_JACK_USERNAME, AbstractStoryTest.USER_JACK_USERNAME.toLowerCase());
	}
	
	@Test
	public void test110AddUserTeleke() throws Exception {
		final String TEST_NAME = "test110AddUserTeleke";
		displayTestTitle(TEST_NAME);
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		
		// WHEN
		displayWhen(TEST_NAME);
		
		addObject(USER_TELEKE_FILE, task, result);
		
		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);
		
		PrismObject<UserType> userAfter = getUserFromRepo(USER_TELEKE_OID);
		display("User after (repo)", userAfter);
		assertPolyString(userAfter, UserType.F_NAME, USER_TELEKE_USERNAME, "tlek");
		assertPolyString(userAfter, UserType.F_FULL_NAME, "Grafula Félix Téleké z Tölökö", "grafula flix tlek z tlk");
		assertPolyString(userAfter, UserType.F_HONORIFIC_PREFIX, "Grf.", "grf.");
	}
	
	private void assertPolyString(PrismObject<UserType> user, QName propName, String expectedOrig, String expectedNorm) {
		PrismProperty<PolyString> prop = user.findProperty(propName);
		PolyString polyString = prop.getRealValue();
		assertEquals("Wrong user "+propName.getLocalPart()+".orig", expectedOrig, polyString.getOrig());
		assertEquals("Wrong user \"+propName.getLocalPart()+\".norm", expectedNorm, polyString.getNorm());
	}
}
