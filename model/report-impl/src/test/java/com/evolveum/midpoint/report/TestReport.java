/**
 * Copyright (c) 2010-2019 Evolveum
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
import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Basic report tests.
 */
@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestReport extends AbstractReportIntegrationTest {

	protected final static File TEST_DIR = new File("src/test/resources/reports");
	protected final static File EXPORT_DIR = new File("target/midpoint-home/export");
	
	protected final static File REPORT_USER_LIST_FILE = new File(TEST_DIR, "report-user-list.xml"); 
	protected final static String REPORT_USER_LIST_OID = "00000000-0000-0000-0000-000000000110";
	
	protected final static File REPORT_USER_LIST_EXPRESSIONS_CSV_FILE = new File(TEST_DIR, "report-user-list-expressions-csv.xml"); 
	protected final static String REPORT_USER_LIST_EXPRESSIONS_CSV_OID = "8fa48180-4f17-11e9-9eed-3fb4721a135e";
	
	protected final static File REPORT_USER_LIST_SCRIPT_FILE = new File(TEST_DIR, "report-user-list-script.xml"); 
	protected final static String REPORT_USER_LIST_SCRIPT_OID = "222bf2b8-c89b-11e7-bf36-ebd4e4d45a80";

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		repoAddObjectFromFile(REPORT_USER_LIST_FILE, ReportType.class, initResult);
		repoAddObjectFromFile(REPORT_USER_LIST_EXPRESSIONS_CSV_FILE, ReportType.class, initResult);
		repoAddObjectFromFile(REPORT_USER_LIST_SCRIPT_FILE, ReportType.class, initResult);
		
		// Let's make this more interesting by adding a couple of users
		importObjectsFromFileNotRaw(USERS_MONKEY_ISLAND_FILE, initTask, initResult);
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
      
      assertSuccess("Report task result", finishedTask.asObjectable().getResult());
  }
  
  @Test
  public void test110ReportUserListExpressionsCsv() throws Exception {
	  final String TEST_NAME = "test110ReportUserListExpressionsCsv";
      displayTestTitle(TEST_NAME);

      Task task = createTask(TEST_NAME);
      OperationResult result = task.getResult();
      
      PrismObject<ReportType> report = getObject(ReportType.class, REPORT_USER_LIST_EXPRESSIONS_CSV_OID);
      
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
      
      assertSuccess("Report task result", finishedTask.asObjectable().getResult());
      
      checkCsvUserReport(report);
  }
  
  private void checkCsvUserReport(PrismObject<ReportType> report) throws IOException, SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
	  File outputFile = findOutputFile(report);
	  display("Found report file", outputFile);
	  List<String> lines = Files.readAllLines(Paths.get(outputFile.getPath()));
	  display("Report content ("+lines.size()+" lines)", String.join("\n", lines));
	  outputFile.renameTo(new File(outputFile.getParentFile(), "processed-"+outputFile.getName()));
	  
	  Task task = createTask("checkCsvUserReport");
      OperationResult result = task.getResult();
	  SearchResultList<PrismObject<UserType>> currentUsers = modelService.searchObjects(UserType.class, null, null, task, result);
	  display("Current users in midPoint ("+currentUsers.size()+" users)", currentUsers.toString());
	  
	  assertEquals("Unexpected number of report lines", currentUsers.size() + 1, lines.size());
  }
  
  private File findOutputFile(PrismObject<ReportType> report) {
	  String filePrefix = report.getName().getOrig();
	  File[] matchingFiles = EXPORT_DIR.listFiles(new FilenameFilter() {
	      public boolean accept(File dir, String name) {
	          return name.startsWith(filePrefix);
	      }
	  });
	  if (matchingFiles.length != 1) {
		  throw new IllegalStateException("Found unexpected output files for "+report+": "+Arrays.toString(matchingFiles));
	  }
	  return matchingFiles[0];
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
