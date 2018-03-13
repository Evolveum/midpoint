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

import static com.evolveum.midpoint.web.AdminGuiTestConstants.*;

import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
public abstract class AbstractInitializedGuiIntegrationTest extends AbstractGuiIntegrationTest {

	private static final Trace LOGGER = TraceManager.getTrace(AbstractInitializedGuiIntegrationTest.class);

	protected DummyResource dummyResource;
	protected DummyResourceContoller dummyResourceCtl;
	protected ResourceType resourceDummyType;
	protected PrismObject<ResourceType> resourceDummy;

	protected PrismObject<UserType> userAdministrator;
	protected String accountJackOid;

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.test.AbstractIntegrationTest#initSystem(com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		LOGGER.trace("initSystem");
		super.initSystem(initTask, initResult);

		modelService.postInit(initResult);
		userAdministrator = repositoryService.getObject(UserType.class, USER_ADMINISTRATOR_OID, null, initResult);
		login(userAdministrator);

		dummyResourceCtl = DummyResourceContoller.create(null);
		dummyResourceCtl.extendSchemaPirate();
		dummyResource = dummyResourceCtl.getDummyResource();
		resourceDummy = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_FILE, RESOURCE_DUMMY_OID, initTask, initResult);
		resourceDummyType = resourceDummy.asObjectable();
		dummyResourceCtl.setResource(resourceDummy);

		repoAddObjectFromFile(USER_JACK_FILE, true, initResult);
		repoAddObjectFromFile(USER_EMPTY_FILE, true, initResult);

		importObjectFromFile(ROLE_MAPMAKER_FILE);	
	}

	@Test
	public void test000PreparationAndSanity() throws Exception {
		final String TEST_NAME = "test000PreparationAndSanity";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        assertNotNull("No model service", modelService);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
		assignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		display(result);
		TestUtil.assertSuccess(result);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
        accountJackOid = getSingleLinkOid(userJack);

	}

}
