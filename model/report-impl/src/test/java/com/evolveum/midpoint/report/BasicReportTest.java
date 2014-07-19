/*
 * Copyright (c) 2010-2014 Evolveum
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

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import com.evolveum.midpoint.prism.util.PrismAsserts;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.query.JRHibernateQueryExecuterFactory;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.parser.XPathHolder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.report.api.ReportManager;
import com.evolveum.midpoint.report.impl.ReportCreateTaskHandler;
import com.evolveum.midpoint.report.impl.ReportUtils;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrientationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportFieldConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.query_3.OrderDirectionType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.XmlAsStringType;

/**
 * @author garbika
 */
@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class BasicReportTest extends AbstractModelIntegrationTest {
    
	private static final String CLASS_NAME_WITH_DOT = BasicReportTest.class
			.getName() + ".";
	/*private static final String GET_REPORT = CLASS_NAME_WITH_DOT
			+ "getReport";*/
	private static final String GET_REPORT_OUTPUT = CLASS_NAME_WITH_DOT
			+ "getReportOutput";
	private static final String SEARCH_REPORT_OUTPUT = CLASS_NAME_WITH_DOT
			+ "searchReportOutput";
	private static final String IMPORT_USERS = CLASS_NAME_WITH_DOT
			+ "importUsers";
	private static final String CREATE_REPORT = CLASS_NAME_WITH_DOT
			+ "test001CreateReport";
	private static final String CREATE_REPORT_FROM_FILE = CLASS_NAME_WITH_DOT
			+ "test002CreateReportFromFile";
	private static final String COPY_REPORT_WITHOUT_DESIGN = CLASS_NAME_WITH_DOT
			+ "test003CopyReportWithoutDesign";
	private static final String RUN_REPORT = CLASS_NAME_WITH_DOT
			+ "test004RunReport";
	private static final String PARSE_OUTPUT_REPORT = CLASS_NAME_WITH_DOT
			+ "test005ParseOutputReport";
	private static final String RUN_TASK = CLASS_NAME_WITH_DOT
			+ "test006RunTask";
	private static final String COUNT_REPORT = CLASS_NAME_WITH_DOT
			+ "test007CountReport";
	private static final String SEARCH_REPORT = CLASS_NAME_WITH_DOT
			+ "test008SearchReport";
	private static final String MODIFY_REPORT = CLASS_NAME_WITH_DOT
			+ "test009ModifyReport";
	private static final String DELETE_REPORT = CLASS_NAME_WITH_DOT
			+ "test010DeleteReport";
	private static final String CLEANUP_REPORTS = CLASS_NAME_WITH_DOT
			+ "test011CleanupReports";
	/*private static final String AUDIT_REPORT = CLASS_NAME_WITH_DOT
			+ "test012AudtReportWithDeltas";*/
	private static final String CREATE_AUDITLOGS_REPORT_FROM_FILE = CLASS_NAME_WITH_DOT
			+ "test013CreateAuditLogsReportFromFile";
	/*private static final String CREATE_AUDITLOGS_REPORT_WITH_DATASOURCE = CLASS_NAME_WITH_DOT
			+ "test014CreateAuditLogsReportWithDatasource";*/
	private static final String CREATE_USERLIST_REPORT_FROM_FILE = CLASS_NAME_WITH_DOT
			+ "test015CreateUserListReportFromFile";
	private static final String CREATE_RECONCILIATION_REPORT_FROM_FILE = CLASS_NAME_WITH_DOT
			+ "test016CreateReconiciliationReportFromFile";
	private static final String GET_REPORT_DATA = CLASS_NAME_WITH_DOT
			+ "test017GetReportData";
	
	private static final Trace LOGGER = TraceManager
			.getTrace(BasicReportTest.class);

	
	private static final String MIDPOINT_HOME = System.getProperty("midpoint.home");
	
	private static final File REPORTS_DIR = new File("src/test/resources/reports");
	private static final File STYLES_DIR = new File("src/test/resources/styles");
	private static final File COMMON_DIR = new File("src/test/resources/common");
	
	private static String EXPORT_DIR = MIDPOINT_HOME + "/export/";
	
	private static final File SYSTEM_CONFIGURATION_FILE = new File(COMMON_DIR + "/system-configuration.xml");
	private static final File ROLE_SUPERUSER_FILE = new File(COMMON_DIR + "/role-superuser.xml");
	private static final File USER_ADMINISTRATOR_FILE = new File(COMMON_DIR + "/user-administrator.xml");
	private static final File TASK_REPORT_FILE = new File(COMMON_DIR + "/task-report.xml");  
	private static final File REPORTS_FOR_CLEANUP_FILE = new File(COMMON_DIR + "/report-outputs-for-cleanup.xml");
	private static final File TEST_REPORT_WITHOUT_DESIGN_FILE = new File(REPORTS_DIR + "/report-test-without-design.xml");
	private static final File TEST_REPORT_FILE = new File(REPORTS_DIR + "/report-test.xml");
	private static final File REPORT_DATASOURCE_TEST = new File(REPORTS_DIR + "/reportDataSourceTest.jrxml");
	private static final File STYLE_TEMPLATE_DEFAULT = new File(STYLES_DIR	+ "/midpoint_base_styles.jrtx");
	private static final File REPORT_AUDIT_TEST = new File(REPORTS_DIR + "/reportAuditLogs.jrxml");
	private static final File RECONCILIATION_REPORT_FILE = new File(REPORTS_DIR + "/reportReconciliation.xml");
	private static final File AUDITLOGS_REPORT_FILE = new File(REPORTS_DIR + "/reportAuditLogs.xml");
	//private static final File AUDITLOGS_WITH_DATASOURCE_REPORT_FILE = new File(REPORTS_DIR + "/reportAuditLogs-with-datasource.xml");
	private static final File USERLIST_REPORT_FILE = new File(REPORTS_DIR + "/reportUserList.xml");
	private static final File USERROLES_REPORT_FILE = new File(REPORTS_DIR + "/reportUserRoles.xml");
	private static final File USERACCOUNTS_REPORT_FILE = new File(REPORTS_DIR + "/reportUserAccounts.xml");
	private static final File USERORGS_REPORT_FILE = new File(REPORTS_DIR + "/reportUserOrgs.xml");
	
	private static final String REPORT_OID_001 = "00000000-3333-3333-0000-100000000001";
	private static final String REPORT_OID_002 = "00000000-3333-3333-0000-100000000002";
	private static final String TEST_REPORT_OID = "00000000-3333-3333-TEST-10000000000";
	private static final String TEST_WITHOUT_DESIGN_REPORT_OID = "00000000-3333-3333-TEST-20000000000";
	private static final String TASK_REPORT_OID = "00000000-3333-3333-TASK-10000000000";
	private static final String AUDITLOGS_REPORT_OID = "AUDITLOG-3333-3333-TEST-10000000000";
	private static final String RECONCILIATION_REPORT_OID = "RECONCIL-3333-3333-TEST-10000000000";
	private static final String USERLIST_REPORT_OID = "USERLIST-3333-3333-TEST-10000000000";
	//private static final String AUDITLOGS_DATASOURCE_REPORT_OID = "AUDITLOG-3333-3333-TEST-1DATASOURCE";
	
	@Autowired
	private PrismContext prismContext;

	@Autowired
	private ModelService modelService;
	
	@Autowired
	private ReportManager reportManager;
	
	@Autowired
	private ReportCreateTaskHandler reportHandler;
	
	@Autowired
	private SessionFactory sessionFactory;
	
	protected PrismObject<UserType> userAdministrator;
	
	public BasicReportTest() {
		super();
	}
	
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {
		
		LOGGER.trace("initSystem");
		super.initSystem(initTask, initResult);
		modelService.postInit(initResult);
		
		OperationResult result = new OperationResult("System variables");
		// System Configuration
		try {
			repoAddObjectFromFile(SYSTEM_CONFIGURATION_FILE, SystemConfigurationType.class, result);
		} catch (ObjectAlreadyExistsException e) {
			LOGGER.trace("System configuration already exists in repository;");
		}
		
		// Users
		repoAddObjectFromFile(ROLE_SUPERUSER_FILE, RoleType.class, result);
		try {
			userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, UserType.class, result);
		} catch (ObjectAlreadyExistsException e) {
			LOGGER.trace("User administrator already exists in repository;");
		}
		login(userAdministrator);
		
	}
	
	protected Task createTask(String operationName) {
		Task task = taskManager.createTaskInstance(operationName);
		task.setOwner(userAdministrator);
		return task;
	}
	/*
	private PrismObject<ReportType> getReport(String reportOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		Task task = taskManager.createTaskInstance(GET_REPORT);
        OperationResult result = task.getResult();
		PrismObject<ReportType> report = modelService.getObject(ReportType.class, reportOid, null, task, result);
		result.computeStatus();
		TestUtil.assertSuccess("getObject(Report) result not success", result);
		return report;
	}
	*/
	private PrismObject<ReportOutputType> getReportOutput(String reportOutputOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		Task task = taskManager.createTaskInstance(GET_REPORT_OUTPUT);
        OperationResult result = task.getResult();
		PrismObject<ReportOutputType> reportOutput = modelService.getObject(ReportOutputType.class, reportOutputOid, null, task, result);
		result.computeStatus();
		TestUtil.assertSuccess("getObject(ReportOutput) result not success", result);
		return reportOutput;
	}
	
	private List<PrismObject<ReportOutputType>> searchReportOutput(String reportOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		Task task = taskManager.createTaskInstance(SEARCH_REPORT_OUTPUT);
        OperationResult result = task.getResult();
        ObjectFilter filter = RefFilter.createReferenceEqual(ReportOutputType.F_REPORT_REF, ReportOutputType.class, prismContext, reportOid);
        ObjectQuery query = ObjectQuery.createObjectQuery(filter);
		List<PrismObject<ReportOutputType>> reportOutputList = modelService.searchObjects(ReportOutputType.class, query, null, task, result);
		result.computeStatus();
		TestUtil.assertSuccess("getObject(Report) result not success", result);
		return reportOutputList;
	}

	private void importUsers(int from, int to) throws Exception {
		Task task = taskManager.createTaskInstance(IMPORT_USERS);
        OperationResult result = task.getResult();
        OperationResult subResult = null;
		for (int i=from; i<=to; i++)
		{
			UserType user = new UserType();
			prismContext.adopt(user);
			user.setName(new PolyStringType("User_" + Integer.toString(i)));
			ObjectDelta<UserType> objectDelta = ObjectDelta.createAddDelta((PrismObject<UserType>) user.asPrismObject());
			Collection<ObjectDelta<? extends ObjectType>> deltas =	MiscSchemaUtil.createCollection(objectDelta);
			LOGGER.trace("insert user {}", user);
			subResult = result.createSubresult("User : " + user.getName().getOrig());
			modelService.executeChanges(deltas, null, task, subResult);
			subResult.computeStatus();
			TestUtil.assertSuccess("import user result not success", subResult);
		}
		
		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createRaw());
		subResult = result.createSubresult("count users");
		int countUsers = modelService.countObjects(UserType.class, null, options, task, subResult);
		LOGGER.trace("count users {}: ", countUsers);
		assertEquals("Unexpected number of count users", to + 1, countUsers);
		result.computeStatus();
	}

	private Calendar create_2014_01_01_12_00_Calendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2014);
        calendar.set(Calendar.MONTH, Calendar.JANUARY);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }

    private Calendar create_2013_06_01_00_00_Calendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2013);
        calendar.set(Calendar.MONTH, Calendar.JUNE);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }

    private Calendar create_2013_07_01_00_00_Calendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2013);
        calendar.set(Calendar.MONTH, Calendar.JULY);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }

    private Calendar create_2013_08_01_00_00_Calendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2013);
        calendar.set(Calendar.MONTH, Calendar.AUGUST);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }

    private Duration createDuration(Calendar when, long now) throws Exception {
        long seconds = (now - when.getTimeInMillis()) / 1000;
        return DatatypeFactory.newInstance().newDuration("PT" + seconds + "S").negate();
    }

    private CleanupPolicyType createPolicy(Calendar when, long now) throws Exception {
        CleanupPolicyType policy = new CleanupPolicyType();

        Duration duration = createDuration(when, now);
        policy.setMaxAge(duration);

        return policy;
    }


	@Test
	public void test001CreateReport() throws Exception {
		// import vo for cycle cez usertype zrusit vsetky RTable

		final String TEST_NAME = "test001CreateReport";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        ReportType reportType = new ReportType();
		prismContext.adopt(reportType);
		
		reportType.setOid(REPORT_OID_001);

		// description and name
		reportType.setName(new PolyStringType("Test report - Datasource1"));
		reportType.setDescription("TEST Report with DataSource parameter.");

		// file templates
		String template = FileUtils.readFileToString(REPORT_DATASOURCE_TEST, "UTF-8");
		LOGGER.info("tempalte: " + template);
		byte[] encodedTemplate = Base64.encodeBase64(template.getBytes());
		LOGGER.info("encoded tempalte: " + encodedTemplate);
		byte[] decoded = Base64.decodeBase64(encodedTemplate);
		LOGGER.info("decided: " + new String(decoded));
		assertEquals(template, new String(decoded));
//		ReportTemplateType reportTemplate = new ReportTemplateType();
//		reportTemplate.setAny(DOMUtil.parseDocument(template).getDocumentElement());
		reportType.setTemplate(encodedTemplate);

		String templateStyle = FileUtils.readFileToString(STYLE_TEMPLATE_DEFAULT, "UTF-8"); //readFile(STYLE_TEMPLATE_DEFAULT, StandardCharsets.UTF_8);
//		ReportTemplateStyleType reportTemplateStyle = new ReportTemplateStyleType();
//		reportTemplateStyle.setAny(DOMUtil.parseDocument(templateStyle).getDocumentElement());
		byte[] encodedTemplateStyle = Base64.encodeBase64(templateStyle.getBytes());
		reportType.setTemplateStyle(encodedTemplateStyle);
		LOGGER.info("tempalte style: " + templateStyle);
		LOGGER.info("encoded tempalte style: " + encodedTemplateStyle);
/*
		String config_schema = FileUtils.readFileToString(REPORT_DATASOURCE_TEST, "UTF-8");
		
		ReportConfigurationType parameters = new ReportConfigurationType();
		parameters.getAny().add(arg0)
		reportType.setConfiguration(parameters);

 * reportType.s
		PrismContainer<Containerable> configuration = new PrismContainer(null);
		// object class
				reportType.setObjectClass(ObjectTypes.getObjectType(UserType.class)
						.getTypeQName());

				// object query
				ObjectPaging paging = ObjectPaging.createPaging(0, 10);
				ObjectQuery query = ObjectQuery.createObjectQuery(paging);
				QueryType queryType = QueryConvertor.createQueryType(query, prismContext);		
				reportType.setQuery(queryType);
		// parameters
		List<ReportParameterConfigurationType> reportParameters = new ArrayList<ReportParameterConfigurationType>();

		ReportParameterConfigurationType parameter = new ReportParameterConfigurationType();
		parameter.setNameParameter("LOGO_PATH");
		parameter.setValueParameter(REPORTS_DIR.getPath() + "/logo.jpg");
		parameter.setClassTypeParameter(DOMUtil.XSD_ANY);
		reportParameters.add(parameter);

		parameter = new ReportParameterConfigurationType();
		parameter.setNameParameter("BaseTemplateStyles");
		parameter.setValueParameter(STYLE_TEMPLATE_DEFAULT.getPath());
		parameter.setClassTypeParameter(DOMUtil.XSD_STRING);
		reportParameters.add(parameter);
		// object query
		ObjectPaging paging = ObjectPaging.createPaging(0, 10);
		ObjectQuery query = ObjectQuery.createObjectQuery(paging);
<<<<<<< HEAD
		QueryType queryType = new QueryType();
		try {
			queryType = QueryJaxbConvertor.createQueryType(query, prismContext);
		} catch (Exception ex) {
			LOGGER.error("Exception occurred. QueryType", ex);
		}
		try {
			queryType.setPaging(PagingConvertor.createPagingType(query
					.getPaging()));
		} catch (Exception ex) {
			LOGGER.error("Exception occurred. QueryType pagging", ex);
		}
=======
		QueryType queryType = QueryConvertor.createQueryType(query, prismContext);		
>>>>>>> master
		reportType.setQuery(queryType);

		reportType.getReportParameter().addAll(reportParameters);
*/
 
		//use hibernate session
		reportType.setUseHibernateSession(false);
		
		// orientation
		reportType.setOrientation(OrientationType.LANDSCAPE);

		// export
		reportType.setExport(ExportType.PDF);

		// fields
		List<ReportFieldConfigurationType> reportFields = new ArrayList<ReportFieldConfigurationType>();

		ReportFieldConfigurationType field = new ReportFieldConfigurationType();
		ItemPath itemPath;
		ItemPathType xpath;
		Element element;
		ItemDefinition itemDef;
		SchemaRegistry schemaRegistry = prismContext.getSchemaRegistry();

		PrismObjectDefinition<UserType> userDefinition = schemaRegistry
				.findObjectDefinitionByCompileTimeClass(UserType.class);

		field.setNameHeader("Name");
		field.setNameReport("Name");
		itemPath = new ItemPath(UserType.F_NAME);
		xpath = new ItemPathType(itemPath);
		field.setItemPath(xpath);
		field.setSortOrderNumber(1);
		field.setSortOrder(OrderDirectionType.ASCENDING);
		itemDef = userDefinition.findItemDefinition(itemPath);
		field.setWidth(25);
		field.setClassType(itemDef.getTypeName());
		reportFields.add(field);

		field = new ReportFieldConfigurationType();
		field.setNameHeader("First Name");
		field.setNameReport("FirstName");
		itemPath = new ItemPath(UserType.F_GIVEN_NAME);
		xpath = new ItemPathType(itemPath);
		field.setItemPath(xpath);
		field.setSortOrderNumber(null);
		field.setSortOrder(null);
		itemDef = userDefinition.findItemDefinition(itemPath);
		field.setWidth(25);
		field.setClassType(itemDef.getTypeName());
		reportFields.add(field);

		field = new ReportFieldConfigurationType();
		field.setNameHeader("Last Name");
		field.setNameReport("LastName");
		itemPath = new ItemPath(UserType.F_FAMILY_NAME);
		xpath = new ItemPathType(itemPath);
		field.setItemPath(xpath);
		field.setSortOrderNumber(null);
		field.setSortOrder(null);
		itemDef = userDefinition.findItemDefinition(itemPath);
		field.setWidth(25);
		field.setClassType(itemDef.getTypeName());
		reportFields.add(field);

		field = new ReportFieldConfigurationType();
		field.setNameHeader("Activation");
		field.setNameReport("Activation");
		itemPath = new ItemPath(UserType.F_ACTIVATION,
				ActivationType.F_ADMINISTRATIVE_STATUS);
		xpath = new ItemPathType(itemPath);
		field.setItemPath(xpath);
		field.setSortOrderNumber(null);
		field.setSortOrder(null);
		itemDef = userDefinition.findItemDefinition(itemPath);
		field.setWidth(25);
		field.setClassType(itemDef.getTypeName());
		reportFields.add(field);

		reportType.getField().addAll(reportFields);
		
		Task task = taskManager.createTaskInstance(CREATE_REPORT);
		OperationResult result = task.getResult();
		result.addParams(new String[] { "task" }, task.getResult());
		result.addParams(new String[] { "object" }, reportType);

		ObjectDelta<ReportType> objectDelta = ObjectDelta
				.createAddDelta(reportType.asPrismObject());
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil
				.createCollection(objectDelta);
		
		//WHEN
		TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        display(result);
        TestUtil.assertSuccess(result);
		AssertJUnit.assertEquals(REPORT_OID_001, objectDelta.getOid());

		reportType = ReportUtils.getReport(REPORT_OID_001, result, modelService);

		// export xml structure of report type
		//String xmlReportType = prismContext.getPrismDomProcessor().serializeObjectToString(reportType.asPrismObject());
		//LOGGER.warn(xmlReportType);

		// check reportType
		AssertJUnit.assertNotNull(reportType);
		AssertJUnit.assertEquals("Test report - Datasource1", reportType
				.getName().getOrig());
		AssertJUnit.assertEquals("TEST Report with DataSource parameter.",
				reportType.getDescription());
		
		
		//LOGGER.warn("reportTemplate orig ::::::::: " + DOMUtil.serializeDOMToString((Node)reportTemplate.getAny()));
		//LOGGER.warn("reportTemplate DB ::::::::: " + DOMUtil.serializeDOMToString((Node)reportType.getReportTemplate().getAny()));
		
		//LOGGER.warn("reportTemplateStyle orig ::::::::: " + DOMUtil.serializeDOMToString((Node)reportTemplateStyle.getAny()));
		//LOGGER.warn("reportTemplateStyle DB ::::::::: " + DOMUtil.serializeDOMToString((Node)reportType.getReportTemplateStyle().getAny()));
		
		
		//String reportTemplateRepoString = DOMUtil.serializeDOMToString((Node)reportType.getReportTemplate().getAny());
   	 	//InputStream inputStreamJRXML = new ByteArrayInputStream(reportTemplateRepoString.getBytes());
   	 	//JasperDesign jasperDesignRepo = JRXmlLoader.load(inputStreamJRXML);
   	 	
   	 	//String reportTemplateString = DOMUtil.serializeDOMToString((Node)reportTemplate.getAny());
	 	//inputStreamJRXML = new ByteArrayInputStream(reportTemplateString.getBytes());
	 	//JasperDesign jasperDesign = JRXmlLoader.load(inputStreamJRXML);
	 	
	 	//AssertJUnit.assertEquals(jasperDesign, jasperDesignRepo);
		//AssertJUnit.assertEquals(reportTemplateStyle.getAny(), reportType.getReportTemplateStyle().getAny());
		AssertJUnit.assertEquals(OrientationType.LANDSCAPE,
				reportType.getOrientation());
		AssertJUnit.assertEquals(ExportType.PDF, reportType.getExport());

		/*
		 AssertJUnit.assertEquals(ObjectTypes.getObjectType(UserType.class)
				.getTypeQName(), reportType.getObjectClass());
		AssertJUnit.assertEquals(queryType, reportType.getQuery());
*/
		int fieldCount = reportFields.size();
		List<ReportFieldConfigurationType> fieldsRepo = reportType
				.getField();

		ReportFieldConfigurationType fieldRepo = null;
		AssertJUnit.assertEquals(fieldCount, fieldsRepo.size());
		for (int i = 0; i < fieldCount; i++) {
			fieldRepo = fieldsRepo.get(i);
			field = reportFields.get(i);
			AssertJUnit.assertEquals(field.getNameHeader(),
					fieldRepo.getNameHeader());
			AssertJUnit.assertEquals(field.getNameReport(),
					fieldRepo.getNameReport());
			ItemPath fieldPath = field.getItemPath().getItemPath();
			ItemPath fieldRepoPath = fieldRepo.getItemPath().getItemPath();
			PrismAsserts.assertPathEquivalent("Wrong path", fieldPath, fieldRepoPath);
			AssertJUnit.assertEquals(field.getSortOrder(),
					fieldRepo.getSortOrder());
			AssertJUnit.assertEquals(field.getSortOrderNumber(),
					fieldRepo.getSortOrderNumber());
			AssertJUnit.assertEquals(field.getWidth(),
					fieldRepo.getWidth());
			AssertJUnit.assertEquals(field.getClassType(),
					fieldRepo.getClassType());
		}
/*
		int parameterCount = reportParameters.size();
		List<ReportParameterConfigurationType> parametersRepo = reportType
				.getReportParameter();

		ReportParameterConfigurationType parameterRepo = null;
		AssertJUnit.assertEquals(parameterCount, parametersRepo.size());
		for (int i = 0; i < parameterCount; i++) {
			parameterRepo = parametersRepo.get(i);
			parameter = reportParameters.get(i);
			AssertJUnit.assertEquals(parameter.getNameParameter(),
					parameterRepo.getNameParameter());
			AssertJUnit.assertEquals(parameter.getValueParameter(),
					parameterRepo.getValueParameter());
			AssertJUnit.assertEquals(parameter.getClassTypeParameter(),
					parameterRepo.getClassTypeParameter());
		}*/
	}

	@Test 
	public void test002CreateReportFromFile() throws Exception {
		
		try{
		final String TEST_NAME = "test002CreateReportFromFile";
        TestUtil.displayTestTile(this, TEST_NAME);
	
        //GIVEN
		Task task = taskManager.createTaskInstance(CREATE_REPORT_FROM_FILE);
		OperationResult result = task.getResult();
      
		//WHEN 	
		TestUtil.displayWhen(TEST_NAME);
		PrismObject<ReportType> reportType = prismContext.parseObject(TEST_REPORT_FILE);
//		LOGGER.info("report template: " + new String(Base64.decodeBase64(reportType.asObjectable().getTemplate())));
//		LOGGER.info("report template style: " + new String(Base64.decodeBase64(reportType.asObjectable().getTemplateStyle())));
//		LOGGER.info("Parsed: " + reportType.debugDump());
		repoAddObject(ReportType.class, reportType, result);
//		importObjectFromFile(TEST_REPORT_FILE);
		
		PrismObject<ReportType> reportFromRepo = modelService.getObject(ReportType.class, TEST_REPORT_OID, null, task, result);
//		LOGGER.info("REPO: " + reportFromRepo.debugDump());
//		LOGGER.info("report template: " + new String(Base64.decodeBase64(reportFromRepo.asObjectable().getTemplate())));
//		LOGGER.info("report template style: " + new String(Base64.decodeBase64(reportFromRepo.asObjectable().getTemplateStyle())));
		
//		ObjectDelta delta = reportType.diff(reportFromRepo);
//		LOGGER.info("delta: " + delta.debugDump());
//		AssertJUnit.assertTrue("Delta must be null", delta.isEmpty());
		
		
		PrismObject<ReportType> reportFromutils = ReportUtils.getReport(TEST_REPORT_OID, result, modelService).asPrismObject();
//		LOGGER.info("UTILS: " + reportFromutils.debugDump());
//		LOGGER.info("report template: " + new String(Base64.decodeBase64(reportFromutils.asObjectable().getTemplate())));
//		LOGGER.info("report template style: " + new String(Base64.decodeBase64(reportFromutils.asObjectable().getTemplateStyle())));
		
//		ObjectDelta delta2 = reportFromRepo.diff(reportFromutils);
//		LOGGER.info("delta: " + delta2.debugDump());
//		AssertJUnit.assertTrue("Delta must be null", delta2.isEmpty());
		

		
		//THEN  
		TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        display(result);
        TestUtil.assertSuccess(result);
		} catch (Exception ex){
			LOGGER.error("failed to create report: " + ex.getMessage(), ex);
			throw ex;
		}
	}

	@Test
	public void test003CopyReportWithoutDesign() throws Exception {
		final String TEST_NAME = "test003CopyReportWithoutDesign";
        TestUtil.displayTestTile(this, TEST_NAME);

		Task task = taskManager.createTaskInstance(COPY_REPORT_WITHOUT_DESIGN);
		OperationResult result = task.getResult();

		ReportType reportType = ReportUtils.getReport(REPORT_OID_001, result, modelService);

		reportType = reportType.clone();
		reportType.setOid(REPORT_OID_002);
		reportType.setTemplate(null);
		reportType.setTemplateStyle(null);
		reportType.setName(new PolyStringType("Test report - Datasource2"));

		ObjectDelta<ReportType> objectDelta = ObjectDelta
				.createAddDelta(reportType.asPrismObject());
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil
				.createCollection(objectDelta);
		//WHEN 	
		TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		//THEN  
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		display(result);
		TestUtil.assertSuccess(result);
		AssertJUnit.assertEquals(REPORT_OID_002, objectDelta.getOid());
	}
	
	
	@Test
	public void test004RunReport() throws Exception {
		final String TEST_NAME = "test004RunReport";
        TestUtil.displayTestTile(this, TEST_NAME);
		
		// GIVEN
        Task task = createTask(RUN_REPORT);
		OperationResult result = task.getResult();
		importUsers(1,10);
		ReportType reportType = ReportUtils.getReport(TEST_REPORT_OID, result, modelService);
		LOGGER.info("jasper report: " + new String(Base64.decodeBase64(reportType.getTemplate()), "utf-8"));
		LOGGER.info("jasper report template: " + new String(Base64.decodeBase64(reportType.getTemplateStyle())));
		//WHEN 	
		TestUtil.displayWhen(TEST_NAME);
		reportManager.runReport(reportType.asPrismObject(), task, result);
		
		// THEN
        TestUtil.displayThen(TEST_NAME);
        //OperationResult subresult = result.getLastSubresult();
        //TestUtil.assertInProgress("create report result", subresult);
        
        waitForTaskFinish(task.getOid(), false, 50000);
        
     // Task result
        PrismObject<TaskType> reportTaskAfter = getTask(task.getOid());
        OperationResultType reportTaskResult = reportTaskAfter.asObjectable().getResult();
        display("Report task result", reportTaskResult);
        TestUtil.assertSuccess(reportTaskResult);
	}

	
	@Test
	public void test005ParseOutputReport() throws Exception {
		final String TEST_NAME = "test005ParseOutputReport";
        TestUtil.displayTestTile(this, TEST_NAME);
		
		// GIVEN
        Task task = createTask(PARSE_OUTPUT_REPORT);
		OperationResult result = task.getResult();
		
        ReportOutputType reportOutputType = searchReportOutput(TEST_REPORT_OID).get(0).asObjectable();
        LOGGER.trace("read report output {}", reportOutputType);
        
        ReportType reportType = ReportUtils.getReport(TEST_REPORT_OID, result, modelService);
        LOGGER.trace("read report {}", reportType);
        
       // String output = ReportUtils.getReportOutputFilePath(reportType);
        
        AssertJUnit.assertNotNull(reportOutputType);
        assertEquals("Unexpected report reference", MiscSchemaUtil.createObjectReference(reportType.getOid(), ReportType.COMPLEX_TYPE), reportOutputType.getReportRef());
       // assertEquals("Unexpected report file path", output, reportOutputType.getFilePath());
       
        BufferedReader br = null;  
        String line = "";  
        String splitBy = ",";  
        
        br = new BufferedReader(new FileReader(reportOutputType.getFilePath()));  
        int count = 0;
        while ((line = br.readLine()) != null) {  
        	count++;
        	String[] lineDetails = line.split(splitBy); 
        	switch (count)
        	{
        		case 1:  assertEquals("Unexpected name of report", "User Report", lineDetails[3]);
        			break;
        		case 2:  assertEquals("Unexpected second line of report", "Report generated on:", lineDetails[7]);
    				break;
        		case 3:  {
        			assertEquals("Unexpected third line of report", "Number of records:", lineDetails[7]);
        			// if not filter 
        			// assertEquals("Unexpected number of records", 11, Integer.parseInt(lineDetails[8]));
        			assertEquals("Unexpected number of records", 10, Integer.parseInt(lineDetails[8]));
        			}
    				break;
        		case 4: {
        			assertEquals("Unexpected column header of report - name", "Name", lineDetails[1]);
        			assertEquals("Unexpected column header of report - first name", "First name", lineDetails[4]);
        			assertEquals("Unexpected column header of report - last name", "Last name", lineDetails[5]);
        			assertEquals("Unexpected column header of report - activation", "Activation", lineDetails[6]);
        			}
    				break;
    			 
        		case 5:	// if not filter
        			//LOGGER.trace("USERS [name= " + lineDetails[0] + " , first name=" + lineDetails[4] + " , last name=" + lineDetails[5] + " , activation=" + lineDetails[6] + "]");
        			//break;
        		case 6:
        		case 7:
        		case 8:
        		case 9:
        		case 10:
        		case 11:
        		case 12:
        		case 13:
        		case 14:
        		//if not filter 
        			//case 15:
        			LOGGER.trace("USERS [name= " + lineDetails[0] + "]");
        			break;
        		// if not filter 
        			//case 16: {
        		case 15: {
        			assertEquals("Unexpected text", "Page 1 of", lineDetails[9]);
        			String pages = lineDetails[10].trim();
        			assertEquals("Unexpected count pages", 1, Integer.parseInt(pages));
        			}
        			break;
        		default: LOGGER.trace("incorrect]");
        			break;
        	}	
        }  
        if (br != null) br.close();  
        
        LOGGER.trace("Done with reading CSV");  
        //if not filter 
        //assertEquals("Unexpected number of users", 11, count-5);
        assertEquals("Unexpected number of users", 10, count-5);
	}
	
	
	 //import report from xml file without design
	 //import task from xml file
	 //run task
	 //parse output file
	 
	
	
	@Test
	public void test006RunTask() throws Exception {
		final String TEST_NAME = "test006RunTask";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        boolean import10000 = false;
        int countUsers = 11;
        if (import10000){ 
        	importUsers(11, 10000);
        	countUsers = 10001;
        }
        	
        Task task = taskManager.createTaskInstance(RUN_TASK);
		OperationResult result = task.getResult();
		
		//WHEN
		TestUtil.displayWhen(TEST_NAME);
		importObjectFromFile(TEST_REPORT_WITHOUT_DESIGN_FILE);
		
		ReportType reportType = ReportUtils.getReport(TEST_WITHOUT_DESIGN_REPORT_OID, result, modelService);
		
		LOGGER.trace("import report task {}", TASK_REPORT_FILE.getPath());
        importObjectFromFile(TASK_REPORT_FILE);
		
        // THEN
        TestUtil.displayThen(TEST_NAME);
        LOGGER.trace("run report task {}", TASK_REPORT_OID);
        waitForTaskFinish(TASK_REPORT_OID, false);
        
        // Task result
        PrismObject<TaskType> reportTaskAfter = getTask(TASK_REPORT_OID);
        OperationResultType reportTaskResult = reportTaskAfter.asObjectable().getResult();
        display("Report task result", reportTaskResult);
        TestUtil.assertSuccess(reportTaskResult);
        
        ReportOutputType reportOutputType = searchReportOutput(reportType.getOid()).get(0).asObjectable();
        LOGGER.trace("read report output {}", reportOutputType);
        
        //ReportType report = getReport(reportType.getOid()).asObjectable();
        LOGGER.trace("read report {}", reportType);
        
        //String output = ReportUtils.getReportOutputFilePath(reportType);
        
        AssertJUnit.assertNotNull(reportOutputType);
        assertEquals("Unexpected report reference", MiscSchemaUtil.createObjectReference(reportType.getOid(), ReportType.COMPLEX_TYPE), reportOutputType.getReportRef());
        //assertEquals("Unexpected report file path", output, reportOutputType.getFilePath());
           
        BufferedReader br = null;  
        String line = "";  
        String splitBy = ",";  
        
        br = new BufferedReader(new FileReader(reportOutputType.getFilePath()));  
        int count = 0;
        while ((line = br.readLine()) != null) {  
        	count++;
        	String[] lineDetails = line.split(splitBy); 
        	switch (count)
        	{
        		case 1:  assertEquals("Unexpected name of report", true, lineDetails[2].indexOf("DataSource")!=-1);
        			break;
        		case 2:  assertEquals("Unexpected second line of report", "Report generated on:", lineDetails[6]);
    				break;
        		case 3:  {
        			assertEquals("Unexpected third line of report", "Number of records:", lineDetails[6]);
        			assertEquals("Unexpected number of records", countUsers, Integer.parseInt(lineDetails[7]));
        			}
    				break;
        		case 4: {
        			assertEquals("Unexpected column header of report - name", "Name", lineDetails[0]);
        			assertEquals("Unexpected column header of report - first name", "First Name", lineDetails[3]);
        			assertEquals("Unexpected column header of report - last name", "Last Name", lineDetails[4]);
        			assertEquals("Unexpected column header of report - activation", "Activation", lineDetails[5]);
        			}
    				break;
        		case 5:	LOGGER.trace("USERS [name= " + lineDetails[0] + " , first name=" + lineDetails[3] + " , last name=" + lineDetails[4] + " , activation=" + lineDetails[5] + "]");
        			break;
        		case 16: {
        			assertEquals("Unexpected text", "Page 1 of", lineDetails[8]);
        			assertEquals("Unexpected count pages", 1, Integer.parseInt(lineDetails[9].replace("\\s", "")));
        			}
        			break;
        		default: LOGGER.trace("USERS [name= " + lineDetails[0] + "]");//LOGGER.trace("incorrect]");
        			break;
        	}	
        }  
        if (br != null) br.close();  
        
        LOGGER.trace("Done with reading CSV");  
        assertEquals("Unexpected number of users", countUsers, count-5);
	}

	
		@Test
		public void test007CountReport() throws Exception {
			final String TEST_NAME = "test007CountReport";
	        TestUtil.displayTestTile(this, TEST_NAME);

			Task task = taskManager.createTaskInstance(COUNT_REPORT);
			OperationResult result = task.getResult();

			Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions
					.createCollection(GetOperationOptions.createRaw());

			//WHEN 	
			TestUtil.displayWhen(TEST_NAME);
			int count = modelService.countObjects(ReportType.class, null, options, task, result);
			
			//THEN
			TestUtil.displayThen(TEST_NAME);
	        result.computeStatus();
	        display(result);
	        TestUtil.assertSuccess(result);
	        assertEquals("Unexpected number of reports", 4, count);
		}


		@Test
		public void test008SearchReport() throws Exception {
			final String TEST_NAME = "test008SearchReport";
	        TestUtil.displayTestTile(this, TEST_NAME);

			Task task = taskManager.createTaskInstance(SEARCH_REPORT);
			OperationResult result = task.getResult();
			
			Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions
					.createCollection(GetOperationOptions.createRaw());

			//WHEN 	
			TestUtil.displayWhen(TEST_NAME);
			List<PrismObject<ReportType>> listReportType = modelService.searchObjects(ReportType.class, null, options, task, result);
			
			//THEN
			TestUtil.displayThen(TEST_NAME);
			result.computeStatus();
			display(result);
			TestUtil.assertSuccess(result);		
			assertEquals("Unexpected number of searching reports", 4, listReportType.size());
		}
		

	
		@Test
		public void test009ModifyReport() throws Exception {
			final String TEST_NAME = "test009ModifyReport";
	        TestUtil.displayTestTile(this, TEST_NAME);
			
			Task task = taskManager.createTaskInstance(MODIFY_REPORT);
			OperationResult result = task.getResult();

			ReportType reportType = ReportUtils.getReport(REPORT_OID_001, result, modelService);

			reportType.setExport(ExportType.CSV);

			Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
			PrismObject<ReportType> reportTypeOld = modelService.getObject(
					ReportType.class, REPORT_OID_001, null, task, result);
			deltas.add(reportTypeOld.diff(reportType.asPrismObject()));

			//WHEN 	
			TestUtil.displayWhen(TEST_NAME);
			modelService.executeChanges(deltas, null, task, result);
			
			//THEN
			TestUtil.displayThen(TEST_NAME);
			result.computeStatus();
			display(result);
			TestUtil.assertSuccess(result);	
			
			reportType = ReportUtils.getReport(REPORT_OID_001, result, modelService);
			assertEquals("Unexpected export type", ExportType.CSV, reportType.getExport());
		}
		
		
	
		@Test
		public void test010DeleteReport() throws Exception {
			
			final String TEST_NAME = "test010DeleteReport";
	        TestUtil.displayTestTile(this, TEST_NAME);

			Task task = taskManager.createTaskInstance(DELETE_REPORT);
			OperationResult result = task.getResult();

			ObjectDelta<ReportType> delta = ObjectDelta.createDeleteDelta(ReportType.class, REPORT_OID_001, prismContext);
			Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(delta);
			
			// WHEN
			modelService.executeChanges(deltas, null, task, result);
			
			// THEN
	        result.computeStatus();
	        TestUtil.assertSuccess(result);
			
			try {
	        	ReportType report = ReportUtils.getReport(REPORT_OID_001, result, modelService);
	        	AssertJUnit.fail("Report type was not deleted");
	        } catch (ObjectNotFoundException e) {
	        	// This is expected
	        }	
			
		}
		
	
		@Test
		public void test011CleanupReports() throws Exception {
			
			final String TEST_NAME = "test011CleanupReports";
	        TestUtil.displayTestTile(this, TEST_NAME);
			
	        // GIVEN
	        List<PrismObject<? extends Objectable>> elements = prismContext.parseObjects(REPORTS_FOR_CLEANUP_FILE);

	        Task task = taskManager.createTaskInstance(CLEANUP_REPORTS);
			OperationResult result = task.getResult();
			
	        for (int i = 0; i < elements.size(); i++) {
	            PrismObject object = elements.get(i);

	            ObjectDelta objectDelta = ObjectDelta.createAddDelta(object);
	            Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
	            
           		modelService.executeChanges(deltas, null, task, result);
           		
           		result.computeStatus();
                display(result);
                TestUtil.assertSuccess(result);
                String oid = objectDelta.getOid();
                
	            AssertJUnit.assertTrue(StringUtils.isNotEmpty(oid));
	            if (objectDelta.getObjectTypeClass() != ReportType.class)
	            {
	            	ReportOutputType reportOutput = getReportOutput(oid).asObjectable();
	            	reportOutput.setFilePath(reportOutput.getFilePath().replace("${midpointhome}", MIDPOINT_HOME));
	            	
	            	MetadataType metadata = new MetadataType();
	            	Date date = new Date();
	            
	            	if (reportOutput.getName().getOrig().contains("01")){
	            		Calendar calendar = create_2013_06_01_00_00_Calendar();
	            		date.setTime(calendar.getTimeInMillis());
	            	}
	            
	            	if (reportOutput.getName().getOrig().contains("02")){
	            		Calendar calendar = create_2013_07_01_00_00_Calendar();
	            		date.setTime(calendar.getTimeInMillis());
	            	}
	            
	            	if (reportOutput.getName().getOrig().contains("03")){
	            		Calendar calendar = create_2013_08_01_00_00_Calendar();
	            		date.setTime(calendar.getTimeInMillis());
	            	}
	            
	            	metadata.setCreateTimestamp(XmlTypeConverter.createXMLGregorianCalendar(date));
	            	reportOutput.setMetadata(metadata);
	            
	            	deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
	            	PrismObject<ReportOutputType> reportOutputOld = getReportOutput(oid);
	            	deltas.add(reportOutputOld.diff(reportOutput.asPrismObject()));
				
	            	modelService.executeChanges(deltas, null, task, result);
	            	
	            	File file = new File(reportOutput.getFilePath());
	            	boolean blnCreated = false;
	                try
	                {
	                   blnCreated = file.createNewFile();
	                }
	                catch(IOException ex)
	                {
	                   LOGGER.error("Error while creating a new empty file : {}", ex);
	                }
	               
	                LOGGER.trace("Was file " + file.getPath() + " created ? : " + blnCreated);
	            	
	            }
	        }

	        // WHEN
	        // because now we can't move system time (we're not using DI for it) we create policy
	        // which should always point to 2013-05-07T12:00:00.000+02:00
	        final long NOW = System.currentTimeMillis();
	        Calendar when = create_2014_01_01_12_00_Calendar();
	        CleanupPolicyType policy = createPolicy(when, NOW);

	        reportManager.cleanupReports(policy, result);

	        // THEN
	        List<PrismObject<ReportOutputType>> reportOutputs = modelService.searchObjects(ReportOutputType.class, null, null, task, result);
	        AssertJUnit.assertNotNull(reportOutputs);
	        AssertJUnit.assertEquals(2, reportOutputs.size());

	    }
		
	
		@Test
		public void test012AuditReportWithDeltas() throws Exception {
			
			final String TEST_NAME = "test012AuditReportWithDeltas";
	        TestUtil.displayTestTile(this, TEST_NAME);
			
	      
			AuditEventType auditEventType = null;//AuditEventType.ADD_OBJECT;
			int auditEventTypeId = auditEventType != null ? auditEventType.ordinal() : -1;
			String auditEventTypeName = auditEventType == null ? "AuditEventType.null" : auditEventType.toString();
			final long NOW = System.currentTimeMillis();
			Calendar calendar = create_2013_07_01_00_00_Calendar();
			Timestamp dateFrom = new Timestamp(calendar.getTimeInMillis());
			Timestamp dateTo = new Timestamp(NOW);
			
			Map<String, Object> params = new HashMap();
		    params.put("DATE_FROM", dateFrom);
		    params.put("DATE_TO", dateTo);
		    params.put("EVENT_TYPE", auditEventTypeId);
		    params.put("EVENT_TYPE_DESC", auditEventTypeName);
		    //String theQuery = auditEventTypeId != -1 ? "select aer.timestamp as timestamp, aer.initiatorName as initiator, aer.eventType as eventType, aer.eventStage as eventStage, aer.targetName as targetName, aer.targetType as targetType, aer.targetOwnerName as targetOwnerName, aer.outcome as outcome, aer.message as message from RAuditEventRecord as aer where aer.eventType = $P{EVENT_TYPE} and aer.timestamp >= $P{DATE_FROM} and aer.timestamp <= $P{DATE_TO} order by aer.timestamp" : "select aer.timestamp as timestamp, aer.initiatorName as initiator, aer.eventType as eventType, aer.eventStage as eventStage, aer.targetName as targetName, aer.targetType as targetType, aer.targetOwnerName as targetOwnerName, aer.outcome as outcome, aer.message as message from RAuditEventRecord as aer where aer.timestamp >= $P{DATE_FROM} and aer.timestamp <= $P{DATE_TO} order by aer.timestamp";
		    String theQuery = auditEventTypeId != -1 ? 
		    		"select aer.timestamp as timestamp, " +
		        	"aer.initiatorName as initiator, " +
		        	"aer.eventType as eventType, " +
		        	"aer.eventStage as eventStage, " +
		        	"aer.targetName as targetName, " +
		        	"aer.targetType as targetType, " +
		        	"aer.targetOwnerName as targetOwnerName, " +
		        	"aer.outcome as outcome, " +
		        	"aer.message as message, " +
		        	"odo.delta as delta " +
		        	"from RObjectDeltaOperation as odo " +
		        	"join odo.record as aer " +
		        	"where aer.eventType = $P{EVENT_TYPE} and aer.timestamp >= $P{DATE_FROM} and aer.timestamp <= $P{DATE_TO} " +
		        	"order by aer.timestamp" 
		        	: 
		        	"select aer.timestamp as timestamp, " +
		        	"aer.initiatorName as initiator, " +
		        	"aer.eventType as eventType, " +
		        	"aer.eventStage as eventStage, " +
		        	"aer.targetName as targetName, " +
		        	"aer.targetType as targetType, " +
		        	"aer.targetOwnerName as targetOwnerName, " +
		        	"aer.outcome as outcome, " +
		        	"aer.message as message, " +
		        	"odo.delta as delta " +
		        	"from RObjectDeltaOperation as odo " +
		        	"join odo.record as aer " +
		        	"where aer.timestamp >= $P{DATE_FROM} and aer.timestamp <= $P{DATE_TO} " +
		        	"order by aer.timestamp";
		    params.put("QUERY_STRING", theQuery); 		    
		    params.put("LOGO_PATH", REPORTS_DIR.getPath() + "/logo.jpg");
		    params.put("BaseTemplateStyles", STYLE_TEMPLATE_DEFAULT.getPath());

	        byte[] generatedReport = new byte[]{};
	        Session session = null;
	        // Loading template
	        JasperDesign design = JRXmlLoader.load(REPORT_AUDIT_TEST);
	        JasperReport report = JasperCompileManager.compileReport(design);

	        session = sessionFactory.openSession();
	        session.beginTransaction();

	        params.put(JRHibernateQueryExecuterFactory.PARAMETER_HIBERNATE_SESSION, session);
	        JasperPrint jasperPrint = JasperFillManager.fillReport(report, params);
	        
	        String output =  EXPORT_DIR + "AuditReport.pdf";
	        JasperExportManager.exportReportToPdfFile(jasperPrint, output);
	        
	      
	       session.getTransaction().commit();
	       session.close();			
		}

	
		@Test 
		public void test013CreateAuditLogsReportFromFile() throws Exception {
			
			final String TEST_NAME = "test013CreateAuditLogsReportFromFile";
	        TestUtil.displayTestTile(this, TEST_NAME);
		
	        // GIVEN
	        Task task = createTask(CREATE_AUDITLOGS_REPORT_FROM_FILE);
			OperationResult result = task.getResult();
			
			//WHEN 	
			TestUtil.displayWhen(TEST_NAME);
			importObjectFromFile(AUDITLOGS_REPORT_FILE);

			// THEN
			result.computeStatus();
			display("Result after good import", result);
			TestUtil.assertSuccess("Import has failed (result)", result);
			
			ReportType reportType = ReportUtils.getReport(AUDITLOGS_REPORT_OID, result, modelService);
			
			//WHEN 	
			TestUtil.displayWhen(TEST_NAME);
			reportManager.runReport(reportType.asPrismObject(), task, result);
			
			// THEN
	        TestUtil.displayThen(TEST_NAME);
	        
	        waitForTaskFinish(task.getOid(), false, 150000);
	        
	     // Task result
	        PrismObject<TaskType> reportTaskAfter = getTask(task.getOid());
	        OperationResultType reportTaskResult = reportTaskAfter.asObjectable().getResult();
	        display("Report task result", reportTaskResult);
	        TestUtil.assertSuccess(reportTaskResult);
		}
/*
		@Test 
		public void test014CreateAuditLogsReportWithDatasource() throws Exception {
			final String TEST_NAME = "test014CreateAuditLogsReportWithDatasource";
	        TestUtil.displayTestTile(this, TEST_NAME);
		
	        // GIVEN
			Task task = createTask(CREATE_AUDITLOGS_REPORT_WITH_DATASOURCE);
			OperationResult result = task.getResult();
			
			//WHEN 	
			TestUtil.displayWhen(TEST_NAME);
			importObjectFromFile(AUDITLOGS_WITH_DATASOURCE_REPORT_FILE);

			// THEN
			result.computeStatus();
			display("Result after good import", result);
			TestUtil.assertSuccess("Import has failed (result)", result);
			
			ReportType reportType = getReport(AUDITLOGS_DATASOURCE_REPORT_OID).asObjectable();
			
			
			
			//WHEN 	
			TestUtil.displayWhen(TEST_NAME);
			reportManager.runReport(reportType.asPrismObject(), task, result);
			
			// THEN
	        TestUtil.displayThen(TEST_NAME);
	        
	        waitForTaskFinish(task.getOid(), false);
	        
	     // Task result
	        PrismObject<TaskType> reportTaskAfter = getTask(task.getOid());
	        OperationResultType reportTaskResult = reportTaskAfter.asObjectable().getResult();
	        display("Report task result", reportTaskResult);
	        TestUtil.assertSuccess(reportTaskResult);
			
			
		}*/
		
	
		@Test 
		public void test015CreateUserListReportFromFile() throws Exception {
			
			final String TEST_NAME = "test015CreateUserListReportFromFile";
	        TestUtil.displayTestTile(this, TEST_NAME);
	
	        // GIVEN
	        Task task = createTask(CREATE_USERLIST_REPORT_FROM_FILE);
			OperationResult result = task.getResult();
			
			//WHEN IMPORT ROLES 	
			TestUtil.displayWhen(TEST_NAME + " - import subreport roles");			
			importObjectFromFile(USERROLES_REPORT_FILE);
			
			// THEN
			result.computeStatus();
			display("Result after good import", result);
			TestUtil.assertSuccess("Import has failed (result)", result);
			
			//WHEN INMPORT ORGS
			TestUtil.displayWhen(TEST_NAME + " - import subreport orgs");			
			importObjectFromFile(USERORGS_REPORT_FILE);

			// THEN
			result.computeStatus();
			display("Result after good import", result);
			TestUtil.assertSuccess("Import has failed (result)", result);

			//WHEN IMPORT ACCOUNTS
			TestUtil.displayWhen(TEST_NAME + " - import subreport accounts");			
			importObjectFromFile(USERACCOUNTS_REPORT_FILE);

			// THEN
			result.computeStatus();
			display("Result after good import", result);
			TestUtil.assertSuccess("Import has failed (result)", result);
	    	
			//WHEN IMPORT USER LIST
			TestUtil.displayWhen(TEST_NAME + " - import report user list");			
			importObjectFromFile(USERLIST_REPORT_FILE);

			// THEN
			result.computeStatus();
			display("Result after good import", result);
			TestUtil.assertSuccess("Import has failed (result)", result);
			

			ReportType reportType = ReportUtils.getReport(USERLIST_REPORT_OID, result, modelService);
			
			//WHEN 	
			TestUtil.displayWhen(TEST_NAME);
			reportManager.runReport(reportType.asPrismObject(), task, result);
			
			// THEN
	        TestUtil.displayThen(TEST_NAME);
	        
	        waitForTaskFinish(task.getOid(), false, 75000);
	        
	     // Task result
	        PrismObject<TaskType> reportTaskAfter = getTask(task.getOid());
	        OperationResultType reportTaskResult = reportTaskAfter.asObjectable().getResult();
	        display("Report task result", reportTaskResult);
	        TestUtil.assertSuccess(reportTaskResult);
	
		}				

	
		@Test 
		public void test016CreateReconciliationReportFromFile() throws Exception {
			
			final String TEST_NAME = "test016CreateReconciliationReportFromFile";
	        TestUtil.displayTestTile(this, TEST_NAME);
		
	        // GIVEN
	        Task task = createTask(CREATE_RECONCILIATION_REPORT_FROM_FILE);
			OperationResult result = task.getResult();
			
			//WHEN 	
			TestUtil.displayWhen(TEST_NAME);			
			importObjectFromFile(RECONCILIATION_REPORT_FILE);

			// THEN
			result.computeStatus();
			display("Result after good import", result);
			TestUtil.assertSuccess("Import has failed (result)", result);
			
			ReportType reportType = ReportUtils.getReport(RECONCILIATION_REPORT_OID, result, modelService);
			
			//WHEN 	
			TestUtil.displayWhen(TEST_NAME);
			reportManager.runReport(reportType.asPrismObject(), task, result);
			
			// THEN
	        TestUtil.displayThen(TEST_NAME);
	        
	        waitForTaskFinish(task.getOid(), false, 75000);
	        
	     // Task result
	        PrismObject<TaskType> reportTaskAfter = getTask(task.getOid());
	        OperationResultType reportTaskResult = reportTaskAfter.asObjectable().getResult();
	        display("Report task result", reportTaskResult);
	        TestUtil.assertSuccess(reportTaskResult);
		}
		
	
		@Test 
		public void test017GetReportData() throws Exception {
			
			final String TEST_NAME = "test017GetReportData";
	        TestUtil.displayTestTile(this, TEST_NAME);
		
	        // GIVEN
	        Task task = createTask(GET_REPORT_DATA);
			OperationResult result = task.getResult();
			
			//WHEN 	
			TestUtil.displayWhen(TEST_NAME);
			
			ReportType reportType = ReportUtils.getReport(USERLIST_REPORT_OID, result, modelService);			
			ReportOutputType reportOutputType = searchReportOutput(reportType.getOid()).get(0).asObjectable();
			AssertJUnit.assertNotNull(reportOutputType);

			InputStream reportData = reportManager.getReportOutputData(reportOutputType.getOid(), result);
			
			// THEN
			result.computeStatus();
			display("Result after read report data", result);
			TestUtil.assertSuccess("Read report data has failed (result)", result);
			
		}	
	

}
