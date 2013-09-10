package com.evolveum.midpoint.model.lens;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.ScriptHistoryEntry;
import com.evolveum.midpoint.model.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestReconScript extends AbstractInternalModelIntegrationTest{
  
	private static final String TASK_RECON_DUMMY_FILENAME = "src/test/resources/common/task-reconcile-dummy.xml";
	private static final String TASK_RECON_DUMMY_OID = "10000000-0000-0000-5656-565600000004";

	@Test
  public void text001testReconcileScriptsWhenProvisioning() throws Exception{
		
		final String TEST_NAME = "text001testReconcileScriptsWhenProvisioning";
        TestUtil.displayTestTile(this, TEST_NAME);

		Task task = taskManager.createTaskInstance(TEST_NAME);
		OperationResult parentResult = new OperationResult(TEST_NAME);
		
		ObjectDelta<UserType> delta = createModifyUserAddAccount(USER_JACK_OID, resourceDummy);
		Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
		deltas.add(delta);
		
		PrismObject<ResourceType> beforeExecution = resourceDummy.clone();
		task.setChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_RECON));
		modelService.executeChanges(deltas, ModelExecuteOptions.createReconcile(), task, parentResult);
		
		delta = createModifyUserReplaceDelta(USER_JACK_OID, new ItemPath(UserType.F_FULL_NAME), new PolyString("tralala"));
		deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
		deltas.add(delta);
		
		modelService.executeChanges(deltas, ModelExecuteOptions.createReconcile(), task, parentResult);

		delta = createModifyUserReplaceDelta(USER_BARBOSSA_OID, new ItemPath(UserType.F_FULL_NAME), new PolyString("tralala"));
		deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
		deltas.add(delta);

		modelService.executeChanges(deltas, ModelExecuteOptions.createReconcile(), task, parentResult);
		
		
		for (ScriptHistoryEntry script : dummyResource.getScriptHistory()){
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
        TestUtil.displayTestTile(this, TEST_NAME);

//		Task task = taskManager.createTaskInstance(TEST_NAME);
//		OperationResult parentResult = new OperationResult(TEST_NAME);

		dummyResource.getScriptHistory().clear();
		
		importObjectFromFile(new File(TASK_RECON_DUMMY_FILENAME));
		
		waitForTaskStart(TASK_RECON_DUMMY_OID, false, DEFAULT_TASK_WAIT_TIMEOUT);
		
		waitForTaskNextRun(TASK_RECON_DUMMY_OID, false, DEFAULT_TASK_WAIT_TIMEOUT);
		
		
		for (ScriptHistoryEntry script : dummyResource.getScriptHistory()){
			
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
}
