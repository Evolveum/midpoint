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
package com.evolveum.midpoint.model.impl.lens;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.ScriptHistoryEntry;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestReconScript extends AbstractInternalModelIntegrationTest {

	private static final String TASK_RECON_DUMMY_FILENAME = "src/test/resources/common/task-reconcile-dummy.xml";
	private static final String TASK_RECON_DUMMY_OID = "10000000-0000-0000-5656-565600000004";

	private static final String ACCOUNT_BEFORE_SCRIPT_FILENAME = "src/test/resources/lens/account-before-script.xml";
	private static final String ACCOUNT_BEFORE_SCRIPT_OID = "acc00000-0000-0000-0000-000000001234";

	@Test
	public void text001testReconcileScriptsWhenProvisioning() throws Exception{
		final String TEST_NAME = "text001testReconcileScriptsWhenProvisioning";
        TestUtil.displayTestTitle(this, TEST_NAME);

		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult parentResult = new OperationResult(TEST_NAME);

		ObjectDelta<UserType> delta = createModifyUserAddAccount(USER_JACK_OID, getDummyResourceObject());
		Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();
		deltas.add(delta);

		task.setChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_RECON));
		modelService.executeChanges(deltas, ModelExecuteOptions.createReconcile(), task, parentResult);

		delta = createModifyUserReplaceDelta(USER_JACK_OID, new ItemPath(UserType.F_FULL_NAME), new PolyString("tralala"));
		deltas = new ArrayList<>();
		deltas.add(delta);

		modelService.executeChanges(deltas, ModelExecuteOptions.createReconcile(), task, parentResult);

		delta = createModifyUserReplaceDelta(USER_BARBOSSA_OID, new ItemPath(UserType.F_FULL_NAME), new PolyString("tralala"));
		deltas = new ArrayList<>();
		deltas.add(delta);

		modelService.executeChanges(deltas, ModelExecuteOptions.createReconcile(), task, parentResult);


		for (ScriptHistoryEntry script : getDummyResource().getScriptHistory()){
			String userName = (String) script.getParams().get("midpoint_usercn");
			String idPath = (String) script.getParams().get("midpoint_idpath");
			String tempPath = (String) script.getParams().get("midpoint_temppath");
			LOGGER.trace("userName {} idPath {} tempPath {}", new Object[]{userName,idPath,tempPath});
			if (!idPath.contains(userName)){
				AssertJUnit.fail("Expected that idPath will contain userName [idPath: " + idPath + ", userName " + userName +"]");
			}

			if (!tempPath.contains(userName)){
				AssertJUnit.fail("Expected that tempPath will contain userName [idPath: " + idPath + ", userName " + userName +"]");
			}
		}
	}

	@Test
	public void test002testReconcileScriptsWhenReconciling() throws Exception{
		final String TEST_NAME = "test002testReconcileScriptsWhenReconciling";
        TestUtil.displayTestTitle(this, TEST_NAME);

        getDummyResource().getScriptHistory().clear();

		importObjectFromFile(new File(TASK_RECON_DUMMY_FILENAME));

		waitForTaskStart(TASK_RECON_DUMMY_OID, false, DEFAULT_TASK_WAIT_TIMEOUT);

		waitForTaskNextRunAssertSuccess(TASK_RECON_DUMMY_OID, false, DEFAULT_TASK_WAIT_TIMEOUT);

		waitForTaskFinish(TASK_RECON_DUMMY_OID, false);

		for (ScriptHistoryEntry script : getDummyResource().getScriptHistory()){

			String userName = (String) script.getParams().get("midpoint_usercn");
			String idPath = (String) script.getParams().get("midpoint_idpath");
			String tempPath = (String) script.getParams().get("midpoint_temppath");
			LOGGER.trace("userName {} idPath {} tempPath {}", new Object[]{userName,idPath,tempPath});
			if (!idPath.contains(userName)){
				AssertJUnit.fail("Expected that idPath will contain userName [idPath: " + idPath + ", userName " + userName +"]");
			}

			if (!tempPath.contains(userName)){
				AssertJUnit.fail("Expected that tempPath will contain userName [idPath: " + idPath + ", userName " + userName +"]");
			}
		}

	}

	@Test
	public void test003testReconcileScriptsAddUserAction() throws Exception{
		final String TEST_NAME = "test003testReconcileScriptsAddUserAction";
        TestUtil.displayTestTitle(this, TEST_NAME);

		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult parentResult = new OperationResult(TEST_NAME);

        ShadowType shadow = parseObjectType(new File(ACCOUNT_BEFORE_SCRIPT_FILENAME), ShadowType.class);

        provisioningService.addObject(shadow.asPrismObject(), null, null, task, parentResult);

        getDummyResource().getScriptHistory().clear();

		waitForTaskStart(TASK_RECON_DUMMY_OID, false, DEFAULT_TASK_WAIT_TIMEOUT);

		waitForTaskNextRunAssertSuccess(TASK_RECON_DUMMY_OID, false, DEFAULT_TASK_WAIT_TIMEOUT);

		waitForTaskFinish(TASK_RECON_DUMMY_OID, true);

		PrismObject<ShadowType> afterRecon = repositoryService.getObject(ShadowType.class, ACCOUNT_BEFORE_SCRIPT_OID, null, parentResult);
		AssertJUnit.assertNotNull(afterRecon);

		ShadowType afterReconShadow = afterRecon.asObjectable();

		if (afterReconShadow.getResult() != null) {
			OperationResult beforeScriptResult = OperationResult
					.createOperationResult(afterReconShadow.getResult());
			display("result in shadow: " + beforeScriptResult);
			AssertJUnit.fail("Operation in shadow not null, recocniliation failed. ");
		}

		PrismObject<FocusType> user = repositoryService.searchShadowOwner(ACCOUNT_BEFORE_SCRIPT_OID, null, parentResult);
		AssertJUnit.assertNotNull("Owner for account " + shadow.asPrismObject() + " not found. Some probelm in recon occured.", user);


		for (ScriptHistoryEntry script : getDummyResource().getScriptHistory()){

			String userName = (String) script.getParams().get("midpoint_usercn");
			String idPath = (String) script.getParams().get("midpoint_idpath");
			String tempPath = (String) script.getParams().get("midpoint_temppath");
			LOGGER.trace("userName {} idPath {} tempPath {}", new Object[]{userName,idPath,tempPath});
			if (!idPath.contains(userName)){
				AssertJUnit.fail("Expected that idPath will contain userName [idPath: " + idPath + ", userName " + userName +"]");
			}

			if (!tempPath.contains(userName)){
				AssertJUnit.fail("Expected that tempPath will contain userName [idPath: " + idPath + ", userName " + userName +"]");
			}
		}

	}

	@Test
	public void test005TestDryRunDelete() throws Exception{
		final String TEST_NAME = "test005TestDryRunDelete";
        TestUtil.displayTestTitle(this, TEST_NAME);


		PrismObject<TaskType> task = getTask(TASK_RECON_DUMMY_OID);
		OperationResult parentResult = new OperationResult(TEST_NAME);

		PropertyDelta dryRunDelta = PropertyDelta.createModificationReplaceProperty(new ItemPath(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_DRY_RUN), task.getDefinition(), true);
		Collection<PropertyDelta> modifications = new ArrayList<>();
		modifications.add(dryRunDelta);

		repositoryService.modifyObject(TaskType.class, TASK_RECON_DUMMY_OID, modifications, parentResult);

		getDummyResource().deleteAccountByName("beforeScript");


		waitForTaskStart(TASK_RECON_DUMMY_OID, false);

		waitForTaskNextRunAssertSuccess(TASK_RECON_DUMMY_OID, false);

		waitForTaskFinish(TASK_RECON_DUMMY_OID, false);

		PrismObject<ShadowType> shadow = repositoryService.getObject(ShadowType.class, ACCOUNT_BEFORE_SCRIPT_OID, null, parentResult);
		AssertJUnit.assertNotNull(shadow);

		PrismObject<FocusType> user = repositoryService.searchShadowOwner(ACCOUNT_BEFORE_SCRIPT_OID, null, parentResult);
		AssertJUnit.assertNotNull("Owner for account " + shadow + " not found. Some probelm in dry run occured.", user);


	}

	@Test
	public void test006TestReconDelete() throws Exception{
		final String TEST_NAME = "test006TestReconDelete";
        TestUtil.displayTestTitle(this, TEST_NAME);

		PrismObject<TaskType> task = getTask(TASK_RECON_DUMMY_OID);
		OperationResult parentResult = new OperationResult(TEST_NAME);

		PropertyDelta dryRunDelta = PropertyDelta.createModificationReplaceProperty(new ItemPath(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_DRY_RUN), task.getDefinition(), false);
		Collection<PropertyDelta> modifications = new ArrayList<>();
		modifications.add(dryRunDelta);

		repositoryService.modifyObject(TaskType.class, TASK_RECON_DUMMY_OID, modifications, parentResult);

//		dummyResource.deleteAccount("beforeScript");

		// WHEN
		TestUtil.displayWhen(TEST_NAME);

		waitForTaskStart(TASK_RECON_DUMMY_OID, false);

		waitForTaskNextRunAssertSuccess(TASK_RECON_DUMMY_OID, false);

		waitForTaskFinish(TASK_RECON_DUMMY_OID, false);

		// THEN
		TestUtil.displayThen(TEST_NAME);

		try{
			PrismObject<ShadowType> shadow = repositoryService.getObject(ShadowType.class, ACCOUNT_BEFORE_SCRIPT_OID, null, parentResult);

			display("Unexpected shadow", shadow);
			AssertJUnit.fail("Expected object not found, but haven't got one");
		} catch (ObjectNotFoundException ex){
			//this is ok
		}

		PrismObject<FocusType> user = repositoryService.searchShadowOwner(ACCOUNT_BEFORE_SCRIPT_OID, null, parentResult);
		display("Unexpected owner", user);
		AssertJUnit.assertNull("Owner for account " + ACCOUNT_BEFORE_SCRIPT_OID + " was found, but it should be not.", user);


	}
}
