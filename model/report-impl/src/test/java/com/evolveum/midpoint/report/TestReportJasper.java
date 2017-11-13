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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.report.impl.ReportCreateTaskHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Looks like this test goes directly to Jasper.
 * 
 * This test does not work. It is NOT ENABLED in testng XML files.
 */
@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestReportJasper extends AbstractReportIntegrationTest {

	@Autowired(required=true)
	ExpressionFactory expressionFactory;

	protected static DummyResource dummyResource;
	protected static DummyResourceContoller dummyResourceCtl;
	protected ResourceType resourceDummyType;
	protected PrismObject<ResourceType> resourceDummy;

	@Autowired(required = true)
	ReportCreateTaskHandler reportTaskHandler;

	@Override
    protected void startResources() throws Exception {
        openDJController.startCleanServer();
    }

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		importObjectFromFile(RESOURCE_OPENDJ_FILE, initResult);

//		caseIgnoreMatchingRule = matchingRuleRegistry.getMatchingRule(StringIgnoreCaseMatchingRule.NAME, DOMUtil.XSD_STRING);

//		importSystemTasks(initResult);
	}


  @Test
  public void f() throws Exception{
	  OperationResult initResult = new OperationResult("generate report");
	  JasperDesign jd = JRXmlLoader.load(new File("src/test/resources/reports/report-users-ds.jrxml"));
	  JasperReport jr = JasperCompileManager.compileReport(jd);

	  Task task = taskManager.createTaskInstance();
	  Map<String, Object> params = new HashMap<String, Object>();
//	  params.put(MidPointQueryExecutorFactory.PARAMETER_MIDPOINT_CONNECTION, modelService);
//	  params.put(MidPointQueryExecutorFactory.PARAMETER_PRISM_CONTEXT, prismContext);
//	  params.put(MidPointQueryExecutorFactory.PARAMETER_TASK_MANAGER, taskManager);
//	  params.put(MidPointQueryExecutorFactory.PARAMETER_EXPRESSION_FACTORY, expressionFactory);
	  params.put("userName", new PolyString("administrator"));


	  reportTaskHandler.run(task);

//	  byte[] reportTemplatebase64 = "".getBytes();
//	 	byte[] reportTemplate = Base64.decodeBase64("UEdwaGMzQmxjbEpsY0c5eWRDQU5DaUFnSUNBSkNYaHRiRzV6UFNKb2RIUndPaTh2YW1GemNHVnljbVZ3YjNKMGN5NXpiM1Z5WTJWbWIzSm5aUzV1WlhRdmFtRnpjR1Z5Y21Wd2IzSjBjeUlnRFFvSkNRbDRiV3h1Y3pwNGMyazlJbWgwZEhBNkx5OTNkM2N1ZHpNdWIzSm5Mekl3TURFdldFMU1VMk5vWlcxaExXbHVjM1JoYm1ObElpQU5DZ2tKQ1hoemFUcHpZMmhsYldGTWIyTmhkR2x2YmowaWFIUjBjRG92TDJwaGMzQmxjbkpsY0c5eWRITXVjMjkxY21ObFptOXlaMlV1Ym1WMEwycGhjM0JsY25KbGNHOXlkSE1nYUhSMGNEb3ZMMnBoYzNCbGNuSmxjRzl5ZEhNdWMyOTFjbU5sWm05eVoyVXVibVYwTDNoelpDOXFZWE53WlhKeVpYQnZjblF1ZUhOa0lpQU5DZ2tKQ1c1aGJXVTlJbkpsY0c5eWRGVnpaWEpCWTJOdmRXNTBjeUlnRFFvSkNRbGpiMngxYlc1RGIzVnVkRDBpTWlJZ0RRb0pDUWx3WVdkbFYybGtkR2c5SWpFNE1DSWdEUW9KQ1Fsd1lXZGxTR1ZwWjJoMFBTSXhPQ0lnRFFvSkNRbDNhR1Z1VG05RVlYUmhWSGx3WlQwaVFXeHNVMlZqZEdsdmJuTk9iMFJsZEdGcGJDSWdEUW9KQ1FsamIyeDFiVzVYYVdSMGFEMGlPRGtpSUEwS0NRa0pZMjlzZFcxdVUzQmhZMmx1WnowaU1TSWdEUW9KQ1Fsc1pXWjBUV0Z5WjJsdVBTSXdJaUFOQ2drSkNYSnBaMmgwVFdGeVoybHVQU0l3SWlBTkNna0pDWFJ2Y0UxaGNtZHBiajBpTUNJZ0RRb0pDUWxpYjNSMGIyMU5ZWEpuYVc0OUlqQWlJQTBLQ1FrSmRYVnBaRDBpTmpkbE5EWTFZelV0TkRabFlTMDBNR1F5TFdKbFlUQXRORFk1WXpaalpqTTRPVE0zSWo0TkNna0pDVHh3Y205d1pYSjBlU0J1WVcxbFBTSnVaWFF1YzJZdWFtRnpjR1Z5Y21Wd2IzSjBjeTVoZDNRdWFXZHViM0psTG0xcGMzTnBibWN1Wm05dWRDSWdkbUZzZFdVOUluUnlkV1VpTHo0TkNna0pDVHh3Y205d1pYSjBlU0J1WVcxbFBTSnVaWFF1YzJZdWFtRnpjR1Z5Y21Wd2IzSjBjeTVsZUhCdmNuUXVjR1JtTG1admNtTmxMbXhwYm1WaWNtVmhheTV3YjJ4cFkza2lJSFpoYkhWbFBTSjBjblZsSWk4K0RRb0pDUWs4YzNSNWJHVWdabTl1ZEU1aGJXVTlJa1JsYW1GV2RTQlRZVzV6SWlCbWIyNTBVMmw2WlQwaU1UQWlJR2hCYkdsbmJqMGlUR1ZtZENJZ2FYTkVaV1poZFd4MFBTSjBjblZsSWlCcGMxQmtaa1Z0WW1Wa1pHVmtQU0owY25WbElpQU5DZ2tKQ1FrZ0lDQnVZVzFsUFNKQ1lYTmxJaUJ3WkdaRmJtTnZaR2x1WnowaVNXUmxiblJwZEhrdFNDSWdjR1JtUm05dWRFNWhiV1U5SWtSbGFtRldkVk5oYm5NdWRIUm1JaUIyUVd4cFoyNDlJazFwWkdSc1pTSStEUW9KQ1FrOEwzTjBlV3hsUGcwS0NRa0pQSE4wZVd4bElHbHpRbTlzWkQwaVptRnNjMlVpSUdselJHVm1ZWFZzZEQwaVptRnNjMlVpSUc1aGJXVTlJa1JsZEdGcGJDSWdjM1I1YkdVOUlrSmhjMlVpTHo0TkNna0pDVHh3WVhKaGJXVjBaWElnYm1GdFpUMGlkWE5sY2s5cFpDSWdZMnhoYzNNOUltcGhkbUV1YkdGdVp5NVRkSEpwYm1jaUx6NE5DZ2tKQ1R4d1lYSmhiV1YwWlhJZ2JtRnRaVDBpYUhGc1VYVmxjbmxCWTJOdmRXNTBjeUlnWTJ4aGMzTTlJbXBoZG1FdWJHRnVaeTVUZEhKcGJtY2lMejROQ2drSkNUeHhkV1Z5ZVZOMGNtbHVaeUJzWVc1bmRXRm5aVDBpYUhGc0lqNDhJVnREUkVGVVFWc2tVQ0Y3YUhGc1VYVmxjbmxCWTJOdmRXNTBjMzFkWFQ0OEwzRjFaWEo1VTNSeWFXNW5QZzBLQ1FrSlBHWnBaV3hrSUc1aGJXVTlJbUZqWTI5MWJuUk9ZVzFsSWlCamJHRnpjejBpYW1GMllTNXNZVzVuTGxOMGNtbHVaeUl2UGcwS0NRa0pQR1pwWld4a0lHNWhiV1U5SW5KbGMyOTFjbU5sVG1GdFpTSWdZMnhoYzNNOUltcGhkbUV1YkdGdVp5NVRkSEpwYm1jaUx6NE5DZ2tKQ1R4a1pYUmhhV3crRFFvSkNRa0pQR0poYm1RZ2FHVnBaMmgwUFNJeE9DSWdjM0JzYVhSVWVYQmxQU0pUZEhKbGRHTm9JajROQ2drSkNRa0pQR1p5WVcxbFBnMEtDUWtKQ1FrSlBISmxjRzl5ZEVWc1pXMWxiblFnZFhWcFpEMGlNMlU0Wm1Sa05tUXRZVFptWmkwME5EQTNMVGxoTVdVdE5XUTJZalEzTURZek1EQmhJaUJ3YjNOcGRHbHZibFI1Y0dVOUlrWnNiMkYwSWlCemRIbHNaVDBpUkdWMFlXbHNJaUJ0YjJSbFBTSlBjR0Z4ZFdVaUlIZzlJakFpSUhrOUlqRWlJSGRwWkhSb1BTSXhPREFpSUdobGFXZG9kRDBpTVRjaUx6NE5DZ2tKQ1FrSkNUeHNhVzVsUGcwS0NRa0pDUWtKQ1R4eVpYQnZjblJGYkdWdFpXNTBJSFYxYVdROUlqUTNaamt4T0RBeExXTm1OV1l0TkdKbFpDMWlNVGxqTFdOaE16a3pNV05pWmprNFpDSWdjRzl6YVhScGIyNVVlWEJsUFNKR2FYaFNaV3hoZEdsMlpWUnZWRzl3SWlCNFBTSXdJaUI1UFNJd0lpQjNhV1IwYUQwaU1UZ3dJaUJvWldsbmFIUTlJakVpSUdadmNtVmpiMnh2Y2owaUl6TXpNek16TXlJK0RRb0pDUWtKQ1FrSkNUeHdjbWx1ZEZkb1pXNUZlSEJ5WlhOemFXOXVQandoVzBORVFWUkJXMjVsZHlCcVlYWmhMbXhoYm1jdVFtOXZiR1ZoYmlnb2FXNTBLU1JXZTFKRlVFOVNWRjlEVDFWT1ZIMHVhVzUwVm1Gc2RXVW9LU0U5TVNsZFhUNDhMM0J5YVc1MFYyaGxia1Y0Y0hKbGMzTnBiMjQrRFFvSkNRa0pDUWtKUEM5eVpYQnZjblJGYkdWdFpXNTBQZzBLQ1FrSkNRa0pDVHhuY21Gd2FHbGpSV3hsYldWdWRENE5DZ2tKQ1FrSkNRa0pQSEJsYmlCc2FXNWxWMmxrZEdnOUlqQXVOU0lnYkdsdVpVTnZiRzl5UFNJak9UazVPVGs1SWk4K0RRb0pDUWtKQ1FrSlBDOW5jbUZ3YUdsalJXeGxiV1Z1ZEQ0TkNna0pDUWtKQ1R3dmJHbHVaVDROQ2drSkNRa0pDVHgwWlhoMFJtbGxiR1FnYVhOVGRISmxkR05vVjJsMGFFOTJaWEptYkc5M1BTSjBjblZsSWo0TkNna0pDUWtKQ1FrOGNtVndiM0owUld4bGJXVnVkQ0IxZFdsa1BTSmxZbUZsWmpFMlpDMHlPVEF6TFRRd01qa3RPV0UyWWkxa05HUXlORFExTlRoaFpUa2lJSEJ2YzJsMGFXOXVWSGx3WlQwaVJteHZZWFFpSUhOMGNtVjBZMmhVZVhCbFBTSlNaV3hoZEdsMlpWUnZWR0ZzYkdWemRFOWlhbVZqZENJZ2MzUjViR1U5SWtSbGRHRnBiQ0lnZUQwaU1DSWdlVDBpTWlJZ2QybGtkR2c5SWpFNE1DSWdhR1ZwWjJoMFBTSXhNeUl2UGcwS0NRa0pDUWtKQ1R4MFpYaDBSV3hsYldWdWRDQjJaWEowYVdOaGJFRnNhV2R1YldWdWREMGlUV2xrWkd4bElpOCtJQTBLQ1FrSkNRa0pDVHgwWlhoMFJtbGxiR1JGZUhCeVpYTnphVzl1UGp3aFcwTkVRVlJCV3lSR2UzSmxjMjkxY21ObFRtRnRaWDBySUNJNklDSWdLeUFrUm50aFkyTnZkVzUwVG1GdFpYMWRYVDQ4TDNSbGVIUkdhV1ZzWkVWNGNISmxjM05wYjI0K0RRb0pDUWtKQ1FrOEwzUmxlSFJHYVdWc1pENE5DZ2tKQ1FrSlBDOW1jbUZ0WlQ0TkNna0pDUWs4TDJKaGJtUStEUW9KQ1FrOEwyUmxkR0ZwYkQ0TkNna0pQQzlxWVhOd1pYSlNaWEJ2Y25RKw==".getBytes());
//
//	 	byte[] decoded = Base64.decodeBase64(reportTemplate);
//	 	String s = new String(decoded);
//	 	System.out.println(s);
	 	File f = new File("src/test/resources/reports/reportAccounts.txt");
//	 	DataOutputStream fos = new DataOutputStream(new FileOutputStream(f));
//	 	fos.writeUTF(s);
//	 	fos.close();
	  JasperPrint jasperPrint = JasperFillManager.fillReport(jr, params);

	  f = new File("src/test/resources/reports/report5.pdf");
	  JasperExportManager.exportReportToPdfFile(jasperPrint, f.getAbsolutePath());
//	  System.out.println("output: " + output);
//	  jr.
//	  jr.getQuery().getLanguage();
//	  jr.getQuery().getText();
  }
}
