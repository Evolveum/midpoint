/**
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
package com.evolveum.midpoint.report;

import static org.testng.AssertJUnit.assertTrue;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * Basic report tests.
 */
@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestReport extends AbstractReportIntegrationTest {

	protected final static File TEST_DIR = new File("src/test/resources/reports");
	
	protected final static File REPORT_USER_LIST_FILE = new File(TEST_DIR, "report-user-list.xml"); 
	protected final static String REPORT_USER_LIST_OID = "00000000-0000-0000-0000-000000000110";
	
	protected final static File REPORT_USER_LIST_SCRIPT_FILE = new File(TEST_DIR, "report-user-list-script.xml"); 
	protected final static String REPORT_USER_LIST_SCRIPT_OID = "222bf2b8-c89b-11e7-bf36-ebd4e4d45a80";

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		repoAddObjectFromFile(REPORT_USER_LIST_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(REPORT_USER_LIST_SCRIPT_FILE, RoleType.class, initResult);
	}


  @Test
  public void test100ReportUserList() throws Exception {
	  final String TEST_NAME = "test100ReportUserList";
      displayTestTitle(TEST_NAME);

      Task task = createTask(TEST_NAME);
      OperationResult result = task.getResult();
      
      PrismObject<ReportType> report = getObject(ReportType.class, REPORT_USER_LIST_OID);
      
      // WHEN
      displayWhen(TEST_NAME);
      reportManager.runReport(report, null, task, result);
      
      assertInProgress(result);
      
      display("Background task", task);
      
      waitForTaskFinish(task.getOid(), true);

      // THEN
      displayThen(TEST_NAME);
      PrismObject<TaskType> finishedTask = getTask(task.getOid());
      display("Background task", finishedTask);
      
      TestUtil.assertSuccess("Report task result", finishedTask.asObjectable().getResult());
  }
  
  @Test
  public void test200ReportUserListScript() throws Exception {
	  final String TEST_NAME = "test200ReportUserListScript";
      displayTestTitle(TEST_NAME);
      
      if (!isOsUnix()) {
			displaySkip(TEST_NAME);
			return;
		}

      Task task = createTask(TEST_NAME);
      OperationResult result = task.getResult();
      
      PrismObject<ReportType> report = getObject(ReportType.class, REPORT_USER_LIST_SCRIPT_OID);
      
      // WHEN
      displayWhen(TEST_NAME);
      reportManager.runReport(report, null, task, result);
      
      assertInProgress(result);
      
      display("Background task", task);
      
      waitForTaskFinish(task.getOid(), true);

      // THEN
      displayThen(TEST_NAME);
      PrismObject<TaskType> finishedTask = getTask(task.getOid());
      display("Background task", finishedTask);
      
      TestUtil.assertSuccess("Report task result", finishedTask.asObjectable().getResult());
      
      File targetFile = new File(MidPointTestConstants.TARGET_DIR_PATH, "report-users.pdf");
      assertTrue("Target file is not there", targetFile.exists());
  }
}
