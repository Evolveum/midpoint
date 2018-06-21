/*
 * Copyright (c) 2013-2018 Evolveum
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

package com.evolveum.midpoint.testing.wstest;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.Holder;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.client.ModelClientUtil;
import com.evolveum.midpoint.test.util.LogfileTestTailer;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultMessage;

/**
 *  Test for generating reports and especially for
 *  internal (intra-cluster) services for downloading
 *  report output. 
 *
 *  @author Radovan Semancik
 * */

@ContextConfiguration(locations = {"classpath:ctx-wstest-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestReportServices extends AbstractWebserviceTest {
	
	public static final File TEST_DIR = new File("src/test/resources/reporting");
	
	public static final String REPORT_DOWNLOAD_ENDPOINT = MIDPOINT_URL_PREFIX + "report";
	public static final String REPORT_DOWNLOAD_PARAM_FNAME = "fname";
	public static final String MIDPOINT_CLUSTER_USERAGENT = "mp-cluster-peer-client";
		
	public static final File TASK_REPORT_USERS_FILE = new File(TEST_DIR, "task-user-report.xml");
 	public static final String TASK_REPORT_USERS_OID = "f9b56bbe-6973-11e8-8b0a-5353b4b79e88";
 	public static final String TASK_REPORT_USERS_NAME = "Users in midPoint report";

	private static final int MAX_CONTENT_LENGTH = 20000000;
 	
 	private String reportOutputOid;
 	private String reportOutputFname;
 	private String nodeOriginalOid;
	
    @Override
	protected void startResources() throws Exception {
		super.startResources();
	}
    
	@Override
	protected void stopResources() throws Exception {
		super.stopResources();
	}
	
	@Test
    public void test010ListNodes() throws Exception {
    	final String TEST_NAME = "test010ListNodes";
    	displayTestTitle(TEST_NAME);
    	
    	Holder<ObjectListType> objectListHolder = new Holder<>();
		Holder<OperationResultType> resultHolder = new Holder<>();
		
        // WHEN
		displayWhen(TEST_NAME);
    	modelPort.searchObjects(ModelClientUtil.getTypeQName(NodeType.class), null, null, objectListHolder, resultHolder);
     
        // THEN
    	displayThen(TEST_NAME);
    	assertSuccess(resultHolder);
        ObjectListType objectList = objectListHolder.value;
        for (ObjectType searchResult: objectList.getObject()) {
        	display(searchResult);
        	if (!(searchResult instanceof NodeType)) {
        		AssertJUnit.fail("Got wrong type "+searchResult);
        	}
        	nodeOriginalOid = searchResult.getOid();
        }
        assertEquals("Unexpected number of nodes", 1, objectList.getObject().size());
    }
	
	@Test
    public void test050AddUserJack() throws Exception {
    	final String TEST_NAME = "test050AddUserJack";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();

        UserType userJack = ModelClientUtil.unmarshallFile(USER_JACK_FILE);
        
        XMLGregorianCalendar startTs = TestUtil.currentTime();
        
        // WHEN
        displayWhen(TEST_NAME);
        String oid = addObject(userJack);
     
        // THEN
        displayThen(TEST_NAME);
        XMLGregorianCalendar endTs = TestUtil.currentTime();
        
        tailer.tail();
        displayAudit(tailer);
        assertAuditLoginLogout(tailer);
        assertAuditIds(tailer);
        assertAuditOperation(tailer, "ADD_OBJECT");
        tailer.assertAudit(4);
        
        // GET user
        UserType userAfter = getObject(UserType.class, USER_JACK_OID);
        display(userAfter);
        assertUser(userAfter, oid, USER_JACK_USERNAME, USER_JACK_GIVEN_NAME, USER_JACK_FAMILY_NAME);
        
        assertCreateMetadata(userAfter, USER_ADMINISTRATOR_OID, startTs, endTs);
        assertPasswordCreateMetadata(userAfter, USER_ADMINISTRATOR_OID, startTs, endTs);
    }
	
	@Test
    public void test100AddTaskReportUsers() throws Exception {
    	final String TEST_NAME = "test100AddTaskReportUsers";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();

        TaskType taskReportUsers = ModelClientUtil.unmarshallFile(TASK_REPORT_USERS_FILE);
        
        // WHEN
        displayWhen(TEST_NAME);
        String oid = addObject(taskReportUsers);
     
        // THEN
        displayThen(TEST_NAME);
        tailer.tail();
        displayAudit(tailer);
        assertAuditLoginLogout(tailer);
        assertAuditIds(tailer);
        assertAuditOperation(tailer, "ADD_OBJECT");
        tailer.assertAudit(4);
        
        TaskType taskAfter = getObject(TaskType.class, TASK_REPORT_USERS_OID);
        display(taskAfter);
        assertTask(taskAfter, oid, TASK_REPORT_USERS_NAME);
    }
	
	@Test
    public void test110WaitForTaskFinish() throws Exception {
    	final String TEST_NAME = "test110WaitForTaskFinish";
    	displayTestTitle(TEST_NAME);
    	
        long startMillis = System.currentTimeMillis();
        TaskExecutionStatusType executionStatus = null;
        TaskType task = null;
        
        // WHEN
        displayWhen(TEST_NAME);
        while (executionStatus != TaskExecutionStatusType.CLOSED) {
        	long currentMillis = System.currentTimeMillis();
        	if ((currentMillis - startMillis) > 10000 ) {
        		throw new RuntimeException("Timeout waiting for task finish");
        	}
	        task = getObject(TaskType.class, TASK_REPORT_USERS_OID);
	        executionStatus = task.getExecutionStatus();
	        display(currentMillis + ": " + executionStatus);
        }
     
        // THEN
        displayThen(TEST_NAME);
        display(task);
    }
	
	@Test
    public void test120ListReportOutputs() throws Exception {
    	final String TEST_NAME = "test120ListReportOutputs";
    	displayTestTitle(TEST_NAME);
    	
    	Holder<ObjectListType> objectListHolder = new Holder<>();
		Holder<OperationResultType> resultHolder = new Holder<>();
		
        // WHEN
		displayWhen(TEST_NAME);
    	modelPort.searchObjects(ModelClientUtil.getTypeQName(ReportOutputType.class), null, null, objectListHolder, resultHolder);
     
        // THEN
    	displayThen(TEST_NAME);
    	assertSuccess(resultHolder);
        ObjectListType objectList = objectListHolder.value;
        for (ObjectType searchResult: objectList.getObject()) {
        	display(searchResult);
        	if (!(searchResult instanceof ReportOutputType)) {
        		AssertJUnit.fail("Got wrong type "+searchResult);
        	}
        	ReportOutputType reportOutput = (ReportOutputType)searchResult;
        	String filePath = reportOutput.getFilePath();
        	reportOutputFname = filePath.substring(filePath.lastIndexOf("/"), filePath.length());
        	display("fname: "+reportOutputFname);
        	reportOutputOid = searchResult.getOid();
        }
        assertEquals("Unexpected number of report outputs", 1, objectList.getObject().size());
    }
	
	
	
	@Test
    public void test140DownloadReportOutputFail() throws Exception {
    	final String TEST_NAME = "test140DownloadReportOutputFail";
    	displayTestTitle(TEST_NAME);
    	
        // WHEN
    	displayWhen(TEST_NAME);
    	HttpResponse response = requestReportHttpGet(reportOutputFname);
     
        // THEN
    	displayThen(TEST_NAME);
    	assertStatus(response, 403);
    	assertNoBody(response);
    }
	
	@Test
    public void test145DownloadReportOutputFail() throws Exception {
    	final String TEST_NAME = "test140DownloadReportOutputFail";
    	displayTestTitle(TEST_NAME);
    	
        // WHEN
    	displayWhen(TEST_NAME);
    	HttpResponse response = requestReportHttpDelete(reportOutputFname);
     
        // THEN
    	displayThen(TEST_NAME);
    	assertStatus(response, 403);
    	assertNoBody(response);
    }
	
	/**
	 * Add a fake "localhost" node to midPoint. This is used to make sure that the
	 * authentication to the report service will work.
	 */
	@Test
    public void test150AddNodeLocalhost() throws Exception {
    	final String TEST_NAME = "test150AddNodeLocalhost";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();

    	NodeType userJack = ModelClientUtil.unmarshallFile(NODE_LOCALHOST_FILE);
        
        // WHEN
        displayWhen(TEST_NAME);
		String oid = addObject(userJack, ModelClientUtil.createRawExecuteOption());
     
        // THEN
        displayThen(TEST_NAME);
        
        tailer.tail();
        displayAudit(tailer);
        assertAuditLoginLogout(tailer);
        assertAuditIds(tailer);
        assertAuditOperation(tailer, "EXECUTE_CHANGES_RAW");
        tailer.assertAudit(4);
        
        // GET user
        NodeType nodeAfter = getObject(NodeType.class, NODE_LOCALHOST_OID);
        display(nodeAfter);
    }
	
	/**
	 * MID-4425
	 */
	@Test
    public void test160DownloadReportOutputFile() throws Exception {
    	final String TEST_NAME = "test160DownloadReportOutputFile";
    	displayTestTitle(TEST_NAME);
    	
        // WHEN
    	displayWhen(TEST_NAME);
    	HttpResponse response = requestReportHttpGet(reportOutputFname);
     
        // THEN
    	displayThen(TEST_NAME);
    	assertStatus(response, 200);
    	// HTML content type means redirect to login page
    	assertContentType(response, "application/pdf");

    	HttpEntity body = response.getEntity();
    	long contentLength = body.getContentLength();
    	display("Content length: "+contentLength);
    	byte[] content = IOUtils.toByteArray(body.getContent());
    	display("Content size: "+content.length);
    	assertTrue("Content seems to be too small: "+content.length+" bytes", content.length > 10000);
    }
	
	/**
	 * MID-4425
	 */
	@Test
    public void test165DeleteReportOutputFile() throws Exception {
    	final String TEST_NAME = "test165DeleteReportOutputFile";
    	displayTestTitle(TEST_NAME);
    	
        // WHEN
    	displayWhen(TEST_NAME);
    	HttpResponse response = requestReportHttpDelete(reportOutputFname);
     
        // THEN
    	displayThen(TEST_NAME);
    	assertStatus(response, 204);
    	assertNoBody(response);
    }
	
	@Test
    public void test169DownloadReportOutputFileNotFound() throws Exception {
    	final String TEST_NAME = "test169DownloadReportOutputFileNotFound";
    	displayTestTitle(TEST_NAME);
    	
        // WHEN
    	displayWhen(TEST_NAME);
    	HttpResponse response = requestReportHttpGet(reportOutputFname);
     
        // THEN
    	displayThen(TEST_NAME);
    	assertStatus(response, 404);
    }
	
	// CLEANUP
	
	@Test
    public void test190DeleteLocalhostNode() throws Exception {
    	final String TEST_NAME = "test190DeleteLocalhostNode";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();

        // WHEN
    	displayWhen(TEST_NAME);
        deleteObject(NodeType.class, NODE_LOCALHOST_OID, ModelClientUtil.createRawExecuteOption());
     
        // THEN
        displayThen(TEST_NAME);
        tailer.tail();
        displayAudit(tailer);
        assertAuditLoginLogout(tailer);
        assertAuditIds(tailer);
        assertAuditOperation(tailer, "EXECUTE_CHANGES_RAW");
        tailer.assertAudit(4);
        try {
        	NodeType node = getObject(NodeType.class, NODE_LOCALHOST_OID);
        	
        	AssertJUnit.fail("Unexpected success of getObject()");
        } catch (FaultMessage fault) {
        	// expected
        }
    }
	
	@Test
    public void test195DeleteTaskReportUsers() throws Exception {
    	final String TEST_NAME = "test195DeleteTaskReportUsers";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();

        // WHEN
    	displayWhen(TEST_NAME);
        deleteObject(TaskType.class, TASK_REPORT_USERS_OID);
     
        // THEN
        displayThen(TEST_NAME);
        tailer.tail();
        displayAudit(tailer);
        assertAuditLoginLogout(tailer);
        assertAuditIds(tailer);
        assertAuditOperation(tailer, "DELETE_OBJECT");
        tailer.assertAudit(4);
        try {
        	TaskType taskAfter = getObject(TaskType.class, TASK_REPORT_USERS_OID);
        	
        	AssertJUnit.fail("Unexpected success of getObject()");
        } catch (FaultMessage fault) {
        	// expected
        }
    }
	
	@Test
    public void test199DeleteTaskReportOutput() throws Exception {
    	final String TEST_NAME = "test199DeleteTaskReportOutput";
    	displayTestTitle(TEST_NAME);
    	
    	LogfileTestTailer tailer = createLogTailer();

        // WHEN
    	displayWhen(TEST_NAME);
        deleteObject(ReportOutputType.class, reportOutputOid);
     
        // THEN
        displayThen(TEST_NAME);
        tailer.tail();
        displayAudit(tailer);
        assertAuditLoginLogout(tailer);
        assertAuditIds(tailer);
        assertAuditOperation(tailer, "DELETE_OBJECT");
        tailer.assertAudit(4);
        try {
        	ReportOutputType reportOutput = getObject(ReportOutputType.class, reportOutputOid);
        	
        	AssertJUnit.fail("Unexpected success of getObject()");
        } catch (FaultMessage fault) {
        	// expected
        }
    }
	
	private HttpResponse requestReportHttpGet(String fname) throws ClientProtocolException, IOException {
		HttpClient client = HttpClients.createDefault();
		String url = REPORT_DOWNLOAD_ENDPOINT + "?" + REPORT_DOWNLOAD_PARAM_FNAME + "=" + URLEncoder.encode(fname, "UTF-8");
		display("HTTP GET request: "+url);
		HttpGet request = new HttpGet(url);
		request.setHeader("User-Agent", MIDPOINT_CLUSTER_USERAGENT);
		HttpResponse response = client.execute(request);
		display("HTTP GET response: "+response);
		return response;
	}
	
	private HttpResponse requestReportHttpDelete(String fname) throws ClientProtocolException, IOException {
		HttpClient client = HttpClients.createDefault();
		String url = REPORT_DOWNLOAD_ENDPOINT + "?" + REPORT_DOWNLOAD_PARAM_FNAME + "=" + URLEncoder.encode(fname, "UTF-8");
		display("HTTP DELETE request: "+url);
		HttpDelete request = new HttpDelete(url);
		request.setHeader("User-Agent", MIDPOINT_CLUSTER_USERAGENT);
		HttpResponse response = client.execute(request);
		display("HTTP DELETE response: "+response);
		return response;
	}
	
	private void assertNoBody(HttpResponse response) {
		HttpEntity body = response.getEntity();
		if (body == null) {
			return;
		}
		assertEquals("Unexpected body length", 0, body.getContentLength());
	}

	private void assertContentType(HttpResponse response, String expectedContentType) {
		String contentType = response.getEntity().getContentType().getValue();
    	display("Content type: "+contentType);
    	assertEquals("Wrong HTTP response content type", expectedContentType, contentType);
	}

	private void assertStatus(HttpResponse response, int expectedCode) {
		StatusLine statusLine = response.getStatusLine();
		assertEquals("Bad HTTP response status code", expectedCode, statusLine.getStatusCode());
	}

}

