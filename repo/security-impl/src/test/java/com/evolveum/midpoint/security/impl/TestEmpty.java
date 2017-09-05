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
package com.evolveum.midpoint.security.impl;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Empty test class. Not used. Can be used as a template for security tests.
 * @author semancik
 *
 */
@ContextConfiguration(locations = "classpath:ctx-security-test-main.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestEmpty extends AbstractIntegrationTest {

	protected static final File TEST_DIR = MidPointTestConstants.TEST_RESOURCES_DIR;

	public static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");
	public static final String SYSTEM_CONFIGURATION_OID = SystemObjectsType.SYSTEM_CONFIGURATION.value();

	protected static final File SECURITY_POLICY_FILE = new File(TEST_DIR, "security-policy.xml");
	protected static final String SECURITY_POLICY_OID = "28bf845a-b107-11e3-85bc-001e8c717e5b";

	protected static final File USER_JACK_FILE = new File(TEST_DIR, "user-jack.xml");
	protected static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";
	protected static final String USER_JACK_USERNAME = "jack";
	protected static final String USER_JACK_PASSWORD = "deadmentellnotales";

	private static final Trace LOGGER = TraceManager.getTrace(TestEmpty.class);

	@Autowired(required=true)
	private UserProfileServiceMock userProfileService;

	@Autowired(required = true)
	private Clock clock;

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.test.AbstractIntegrationTest#initSystem(com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {

		repoAddObjectFromFile(SYSTEM_CONFIGURATION_FILE, initResult);
		repoAddObjectFromFile(SECURITY_POLICY_FILE, initResult);
		repoAddObjectFromFile(USER_JACK_FILE, initResult);
	}

	@Test
	public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
		TestUtil.displayTestTitle(TEST_NAME);

		// TODO
	}

	@Test
	public void test020UserProfileServiceMockUsername() throws Exception {
		final String TEST_NAME = "test020UserProfileServiceMockUsername";
		TestUtil.displayTestTitle(TEST_NAME);

		MidPointPrincipal principal = userProfileService.getPrincipal(USER_JACK_USERNAME);
		assertPrincipalJack(principal);
	}

	private void assertPrincipalJack(MidPointPrincipal principal) {
		display("principal", principal);
		assertEquals("Bad principal name", USER_JACK_USERNAME, principal.getName().getOrig());
		assertEquals("Bad principal name", USER_JACK_USERNAME, principal.getUsername());
		UserType user = principal.getUser();
		assertNotNull("No user in principal",user);
		assertEquals("Bad name in user in principal", USER_JACK_USERNAME, user.getName().getOrig());
	}


}
