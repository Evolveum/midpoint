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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.cxf.interceptor.Fault;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.report.impl.ReportWebService;
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
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.RemoteReportParametersType;

/**
 * Basic report tests.
 */
@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestReportWebService extends AbstractReportIntegrationTest {
	
	@Autowired protected ReportWebService reportWebService;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		repoAddObjectFromFile(REPORT_USER_LIST_EXPRESSIONS_CSV_FILE, ReportType.class, initResult);
		repoAddObjectFromFile(REPORT_USER_LIST_EXPRESSIONS_POISONOUS_QUERY_CSV_FILE, ReportType.class, initResult);
		repoAddObjectFromFile(REPORT_USER_LIST_EXPRESSIONS_POISONOUS_FIELD_CSV_FILE, ReportType.class, initResult);
		
		// Let's make this more interesting by adding a couple of users
		importObjectsFromFileNotRaw(USERS_MONKEY_ISLAND_FILE, initTask, initResult);
	}

	@Test
	  public void test000Sanity() throws Exception {
		  final String TEST_NAME = "test000Sanity";
	      displayTestTitle(TEST_NAME);
	      
	      assertNotNull("No web service", reportWebService);
	}
	
	@Test
	public void test100ProcessReportUserList() throws Exception {
		final String TEST_NAME = "test100ProcessReportUserList";
		displayTestTitle(TEST_NAME);

		String query = createAllQueryString(UserType.class);
		RemoteReportParametersType parameters = createReportParameters();
            
		// WHEN
		displayWhen(TEST_NAME);
		ObjectListType userList = reportWebService.processReport(REPORT_USER_LIST_EXPRESSIONS_CSV_OID, query, parameters, null);
		
		// THEN
		displayThen(TEST_NAME);
		display("Returned user list ("+userList.getObject().size()+" objects)", userList);
		
		assertUserList(userList);
	}
	
	@Test
	public void test110ProcessReportUserListNoReportOid() throws Exception {
		final String TEST_NAME = "test110ProcessReportUserListNoReportOid";
		displayTestTitle(TEST_NAME);

		String query = createAllQueryString(UserType.class);
		RemoteReportParametersType parameters = createReportParameters();
            
		try {
			
			// WHEN
			displayWhen(TEST_NAME);
			reportWebService.processReport(null, query, parameters, null);
			
			assertNotReached();
			
		} catch (Fault f) {
			// THEN
			displayThen(TEST_NAME);
			display("Expected fault", f);
		}
	}
	
	@Test
	public void test112ProcessReportUserListInvalidReportOid() throws Exception {
		final String TEST_NAME = "test112ProcessReportUserListInvalidReportOid";
		displayTestTitle(TEST_NAME);

		String query = createAllQueryString(UserType.class);
		RemoteReportParametersType parameters = createReportParameters();
            
		try {
			
			// WHEN
			displayWhen(TEST_NAME);
			reportWebService.processReport("l00n3y", query, parameters, null);
			
			assertNotReached();
			
		} catch (Fault f) {
			// THEN
			displayThen(TEST_NAME);
			display("Expected fault", f);
		}
	}
	
	// TODO: test that violates authorization to run report
	// TODO: test that violates authorization to read report
	// TODO: test that violates safe profile

	private String createAllQueryString(Class<?> type) {
		return "<filter><type><type>"+type.getSimpleName()+"</type></type></filter>";
	}

	private RemoteReportParametersType createReportParameters() {
		RemoteReportParametersType parameters = new RemoteReportParametersType();
		return parameters;
	}

	private void assertUserList(ObjectListType userList) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		Task task = createTask("assertUserList");
		OperationResult result = task.getResult();
		SearchResultList<PrismObject<UserType>> currentUsers = modelService.searchObjects(UserType.class, null, null, task, result);
		display("Current users in midPoint ("+currentUsers.size()+" users)", currentUsers.toString());
		
		assertEquals("Unexpected number of returned objects", currentUsers.size(), userList.getObject().size());
	}

	
}
