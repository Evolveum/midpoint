/*
 * Copyright (c) 2010-2013 Evolveum
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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.PagingConvertor;
import com.evolveum.midpoint.schema.QueryConvertor;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExportType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrientationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportFieldConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportParameterConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportTemplateStyleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.query_2.OrderDirectionType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

/**
 * @author garbika
 */
@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
// extends AbstractTestNGSpringContextTests,
public class BasicReportTest extends AbstractModelIntegrationTest {

	
	
	private static final String CLASS_NAME_WITH_DOT = BasicReportTest.class
			.getName() + ".";
	private static final String INIT_SYSTEM_CONFIGURATION = CLASS_NAME_WITH_DOT
			+ "initSystemConfiguration";
	private static final String CREATE_REPORT = CLASS_NAME_WITH_DOT
			+ "test001CreateReport";
	//private static final String CREATE_REPORT_FROM_FILE = CLASS_NAME_WITH_DOT
	//		+ "test002CreateReportFromFile";
	private static final String COPY_REPORT_WITHOUT_JRXML = CLASS_NAME_WITH_DOT
			+ "test003CopyReportWithoutJRXML";
	// private static final String GENERATE_REPORT = CLASS_NAME_WITH_DOT +
	// "test002GenerateReport";
	private static final String COUNT_REPORT = CLASS_NAME_WITH_DOT
			+ "test006CountReport";
	private static final String SEARCH_REPORT = CLASS_NAME_WITH_DOT
			+ "test007SearchReport";
	private static final String MODIFY_REPORT = CLASS_NAME_WITH_DOT
			+ "test008ModifyReport";
	private static final String DELETE_REPORT = CLASS_NAME_WITH_DOT
			+ "test009DeleteReport";
	private static final Trace LOGGER = TraceManager
			.getTrace(BasicReportTest.class);

	private static final File REPORTS_DIR = new File("src/test/resources/reports");
	private static final File STYLES_DIR = new File("src/test/resources/styles");
	private static final File COMMON_DIR = new File("src/test/resources/common");
	
	private static final File SYSTEM_CONFIGURATION_FILE = new File(COMMON_DIR + "/system-configuration.xml");
	
	// private static final File DATA_DIR = new File("src/test/resources/data");
	// private static final File RESOURCE_OPENDJ_FILE = new File(DATA_DIR,
	// "resource-opendj.xml");

	//private static final String TEST_REPORT_FILE = REPORTS_DIR
	//		+ "/report-test.xml";
	private static final String REPORT_DATASOURCE_TEST = REPORTS_DIR
			+ "/reportDataSourceTest.jrxml";
	private static final String STYLE_TEMPLATE_DEFAULT = STYLES_DIR
			+ "/midpoint_base_styles.jrtx";
	/*
	 * private static final String FINAL_EXPORT_REPORT_1 = REPORTS_DIR +
	 * "/reportDataSourceTestFinal_1"; private static final String
	 * FINAL_EXPORT_REPORT_2 = REPORTS_DIR + "/reportDataSourceTestFinal_2";
	 * private static final String FINAL_EXPORT_REPORT_3 = REPORTS_DIR +
	 * "/reportDataSourceTestFinal_3"; private static final String
	 * FINAL_EXPORT_REPORT_RECONCILIATION = REPORTS_DIR +
	 * "/reportDataSourceReconciliation";
	 */
	private static final String REPORT_OID_001 = "00000000-3333-3333-0000-100000000001";
	private static final String REPORT_OID_002 = "00000000-3333-3333-0000-100000000002";
	/*private static final String REPORT_OID_TEST = "00000000-3333-3333-TEST-10000000000";
	
	 * private static final String REPORT_OID_RECONCILIATION =
	 * "00000000-3333-3333-0000-100000000003"; private static final String
	 * RESOURCE_OID = "10000000-0000-0000-0000-000000000003"; private static
	 * final String RESOURCE_NAME = "Localhost OpenDJ"; private static final
	 * String INTENT = "default";
	 */

	@Autowired
	private PrismContext prismContext;

	@Autowired
	protected TaskManager taskManager;

	@Autowired
	private ModelService modelService;

	private static String readFile(String path, Charset encoding)
			throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return encoding.decode(ByteBuffer.wrap(encoded)).toString();
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {
		
		LOGGER.trace("initSystem");
		super.initSystem(initTask, initResult);
			
		//modelService.postInit(initResult);
		
		// System Configuration
		try {
			repoAddObjectFromFile(getSystemConfigurationFile(), SystemConfigurationType.class, initResult);
		} catch (ObjectAlreadyExistsException e) {
			throw new ObjectAlreadyExistsException("System configuration already exists in repository;" +
					"looks like the previous test haven't cleaned it up", e);
		}
	}
    	
	protected File getSystemConfigurationFile() {
		return SYSTEM_CONFIGURATION_FILE;
	}

	@Test
	public void test001CreateReport() throws Exception {

		// vytvorit vstup xml file na report type
		// import vo for cycle cez usertype zrusit vsetky RTable
		// paging nastavovat v datasource len nie v xml

		ReportType reportType = new ReportType();
		prismContext.adopt(reportType);

		LOGGER.debug("Creating Test report. DATASOURCE ..... ");

		reportType.setOid(REPORT_OID_001);

		// description and name
		reportType.setName(new PolyStringType("Test report - Datasource1"));
		reportType.setDescription("TEST Report with DataSource parameter.");

		// file templates
		String jrxmlFile = readFile(REPORT_DATASOURCE_TEST, StandardCharsets.UTF_8);
		ReportTemplateType reportTemplate = new ReportTemplateType();
		reportTemplate.setAny(DOMUtil.parseDocument(jrxmlFile).getDocumentElement());
		reportType.setReportTemplate(reportTemplate);

		String jrtxFile = readFile(STYLE_TEMPLATE_DEFAULT, StandardCharsets.UTF_8);
		ReportTemplateStyleType reportTemplateStyle = new ReportTemplateStyleType();
		//reportTemplateStyle.setAny(DOMUtil.parseDocument(jrtxFile).getDocumentElement());

		reportType.setReportTemplateStyle(reportTemplateStyle);

		// orientation
		reportType.setReportOrientation(OrientationType.LANDSCAPE);

		// export
		reportType.setReportExport(ExportType.PDF);

		// object class
		reportType.setObjectClass(ObjectTypes.getObjectType(UserType.class)
				.getTypeQName());

		// object query
		ObjectPaging paging = ObjectPaging.createPaging(0, 10);
		ObjectQuery query = ObjectQuery.createObjectQuery(paging);
		QueryType queryType = new QueryType();
		try {
			queryType = QueryConvertor.createQueryType(query, prismContext);
		} catch (Exception ex) {
			LOGGER.error("Exception occurred. QueryType", ex);
		}
		try {
			queryType.setPaging(PagingConvertor.createPagingType(query
					.getPaging()));
		} catch (Exception ex) {
			LOGGER.error("Exception occurred. QueryType pagging", ex);
		}
		reportType.setQuery(queryType);

		// fields
		List<ReportFieldConfigurationType> reportFields = new ArrayList<ReportFieldConfigurationType>();

		ReportFieldConfigurationType field = new ReportFieldConfigurationType();
		ItemPath itemPath;
		XPathHolder xpath;
		Element element;
		ItemDefinition itemDef;
		SchemaRegistry schemaRegistry = prismContext.getSchemaRegistry();

		PrismObjectDefinition<UserType> userDefinition = schemaRegistry
				.findObjectDefinitionByCompileTimeClass(UserType.class);

		field.setNameHeaderField("Name");
		field.setNameReportField("Name");
		itemPath = new ItemPath(UserType.F_NAME);
		xpath = new XPathHolder(itemPath);
		element = xpath.toElement(SchemaConstants.C_ITEM_PATH_FIELD,
				DOMUtil.getDocument());// (SchemaConstantsGenerated.NS_COMMON,
										// "itemPathField");
		field.setItemPathField(element);
		field.setSortOrderNumber(1);
		field.setSortOrder(OrderDirectionType.ASCENDING);
		itemDef = userDefinition.findItemDefinition(itemPath);
		field.setWidthField(25);
		field.setClassTypeField(itemDef.getTypeName());
		reportFields.add(field);

		field = new ReportFieldConfigurationType();
		field.setNameHeaderField("First Name");
		field.setNameReportField("FirstName");
		itemPath = new ItemPath(UserType.F_GIVEN_NAME);
		xpath = new XPathHolder(itemPath);
		element = xpath.toElement(SchemaConstants.C_ITEM_PATH_FIELD,
				DOMUtil.getDocument());
		field.setItemPathField(element);
		field.setSortOrderNumber(null);
		field.setSortOrder(null);
		itemDef = userDefinition.findItemDefinition(itemPath);
		field.setWidthField(25);
		field.setClassTypeField(itemDef.getTypeName());
		reportFields.add(field);

		field = new ReportFieldConfigurationType();
		field.setNameHeaderField("Last Name");
		field.setNameReportField("LastName");
		itemPath = new ItemPath(UserType.F_FAMILY_NAME);
		xpath = new XPathHolder(itemPath);
		element = xpath.toElement(SchemaConstants.C_ITEM_PATH_FIELD,
				DOMUtil.getDocument());
		field.setItemPathField(element);
		field.setSortOrderNumber(null);
		field.setSortOrder(null);
		itemDef = userDefinition.findItemDefinition(itemPath);
		field.setWidthField(25);
		field.setClassTypeField(itemDef.getTypeName());
		reportFields.add(field);

		field = new ReportFieldConfigurationType();
		field.setNameHeaderField("Activation");
		field.setNameReportField("Activation");
		itemPath = new ItemPath(UserType.F_ACTIVATION,
				ActivationType.F_ADMINISTRATIVE_STATUS);
		xpath = new XPathHolder(itemPath);
		element = xpath.toElement(SchemaConstants.C_ITEM_PATH_FIELD,
				DOMUtil.getDocument());
		field.setItemPathField(element);
		field.setSortOrderNumber(null);
		field.setSortOrder(null);
		itemDef = userDefinition.findItemDefinition(itemPath);
		field.setWidthField(25);
		field.setClassTypeField(itemDef.getTypeName());
		reportFields.add(field);

		reportType.getReportField().addAll(reportFields);

		// parameters
		List<ReportParameterConfigurationType> reportParameters = new ArrayList<ReportParameterConfigurationType>();

		ReportParameterConfigurationType parameter = new ReportParameterConfigurationType();
		parameter.setNameParameter("LOGO_PATH");
		parameter.setValueParameter(REPORTS_DIR.getPath() + "/logo.jpg");
		parameter.setClassTypeParameter(DOMUtil.XSD_ANY);
		reportParameters.add(parameter);

		parameter = new ReportParameterConfigurationType();
		parameter.setNameParameter("BaseTemplateStyles");
		parameter.setValueParameter(STYLE_TEMPLATE_DEFAULT);
		parameter.setClassTypeParameter(DOMUtil.XSD_STRING);
		reportParameters.add(parameter);

		reportType.getReportParameter().addAll(reportParameters);

		Task task = taskManager.createTaskInstance(CREATE_REPORT);
		OperationResult result = task.getResult();
		result.addParams(new String[] { "task" }, task.getResult());
		result.addParams(new String[] { "object" }, reportType);

		ObjectDelta<ReportType> objectDelta = ObjectDelta
				.createAddDelta(reportType.asPrismObject());
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil
				.createCollection(objectDelta);

		modelService.executeChanges(deltas, null, task, result);
		result.computeStatus();

		AssertJUnit.assertEquals(REPORT_OID_001, objectDelta.getOid());

		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions
				.createCollection(GetOperationOptions.createRaw());
		OperationResult subResult = result.createSubresult("readReport");

		reportType = modelService.getObject(ReportType.class, REPORT_OID_001,
				options, null, subResult).asObjectable();
		subResult.computeStatus();

		// export xml structure of report type
		/*String xmlReportType = prismContext.getPrismDomProcessor()
				.serializeObjectToString(reportType.asPrismObject());
		LOGGER.warn(xmlReportType);
*/
		// check reportType
		AssertJUnit.assertNotNull(reportType);
		AssertJUnit.assertEquals("Test report - Datasource1", reportType
				.getName().getOrig());
		AssertJUnit.assertEquals("TEST Report with DataSource parameter.",
				reportType.getDescription());
		
		/*
		LOGGER.warn("reportTemplate orig ::::::::: " + DOMUtil.serializeDOMToString((Node)reportTemplate.getAny()));
		LOGGER.warn("reportTemplate DB ::::::::: " + DOMUtil.serializeDOMToString((Node)reportType.getReportTemplate().getAny()));
		
		LOGGER.warn("reportTemplateStyle orig ::::::::: " + DOMUtil.serializeDOMToString((Node)reportTemplateStyle.getAny()));
		LOGGER.warn("reportTemplateStyle DB ::::::::: " + DOMUtil.serializeDOMToString((Node)reportType.getReportTemplateStyle().getAny()));
		*/
		//AssertJUnit.assertEquals(reportTemplate.getAny(), reportType.getReportTemplate().getAny());
		//AssertJUnit.assertEquals(reportTemplateStyle.getAny(), reportType.getReportTemplateStyle().getAny());
		AssertJUnit.assertEquals(OrientationType.LANDSCAPE,
				reportType.getReportOrientation());
		AssertJUnit.assertEquals(ExportType.PDF, reportType.getReportExport());
		AssertJUnit.assertEquals(ObjectTypes.getObjectType(UserType.class)
				.getTypeQName(), reportType.getObjectClass());
		AssertJUnit.assertEquals(queryType, reportType.getQuery());

		int fieldCount = reportFields.size();
		List<ReportFieldConfigurationType> fieldsRepo = reportType
				.getReportField();

		ReportFieldConfigurationType fieldRepo = null;
		AssertJUnit.assertEquals(fieldCount, fieldsRepo.size());
		for (int i = 0; i < fieldCount; i++) {
			fieldRepo = fieldsRepo.get(i);
			field = reportFields.get(i);
			AssertJUnit.assertEquals(field.getNameHeaderField(),
					fieldRepo.getNameHeaderField());
			AssertJUnit.assertEquals(field.getNameReportField(),
					fieldRepo.getNameReportField());
			ItemPath fieldPath = new XPathHolder(field.getItemPathField())
					.toItemPath();
			ItemPath fieldRepoPath = new XPathHolder(
					fieldRepo.getItemPathField()).toItemPath();
			AssertJUnit.assertEquals(fieldPath, fieldRepoPath);
			AssertJUnit.assertEquals(field.getSortOrder(),
					fieldRepo.getSortOrder());
			AssertJUnit.assertEquals(field.getSortOrderNumber(),
					fieldRepo.getSortOrderNumber());
			AssertJUnit.assertEquals(field.getWidthField(),
					fieldRepo.getWidthField());
			AssertJUnit.assertEquals(field.getClassTypeField(),
					fieldRepo.getClassTypeField());
		}

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
		}
	}

	/*
	 * private void generateUsers() { LOGGER.info("import users");
	 * 
	 * RUser user = new RUser(); user.setName(new RPolyString("vilko",
	 * "vilko")); user.setFamilyName(new RPolyString("repan", "repan"));
	 * user.setGivenName(user.getName()); RActivation activation = new
	 * RActivation();
	 * activation.setAdministrativeStatus(RActivationStatus.ENABLED);
	 * user.setActivation(activation);
	 * 
	 * Session session = sessionFactory.openSession();
	 * session.beginTransaction(); session.save(user);
	 * session.getTransaction().commit(); session.close();
	 * 
	 * LOGGER.info("import vilko");
	 * 
	 * user = new RUser(); user.setName(new RPolyString("gabika", "gabika"));
	 * user.setFamilyName(new RPolyString("polcova", "polcova"));
	 * user.setGivenName(user.getName()); activation = new RActivation();
	 * activation.setAdministrativeStatus(RActivationStatus.ENABLED);
	 * user.setActivation(activation);
	 * 
	 * session = sessionFactory.openSession(); session.beginTransaction();
	 * session.save(user); session.getTransaction().commit(); session.close();
	 * 
	 * LOGGER.info("import gabika");
	 * 
	 * session = sessionFactory.openSession(); session.beginTransaction();
	 * 
	 * Query qCount = session.createQuery("select count(*) from RUser"); long
	 * count = (Long) qCount.uniqueResult(); AssertJUnit.assertEquals(2L,
	 * count);
	 * 
	 * session.getTransaction().commit(); session.close(); }
	 * 
	 * 
	 * private void importReconciliationData() throws Exception {
	 * LOGGER.info("import resource");
	 * 
	 * OperationResult result = new
	 * OperationResult(BasicReportTest.class.getName()+".IMPORT RESOURCE");
	 * PrismObject<ResourceType> resource =
	 * prismContext.getPrismDomProcessor().parseObject(RESOURCE_OPENDJ_FILE);
	 * 
	 * 
	 * 
	 * Session session = sessionFactory.openSession();
	 * session.beginTransaction();
	 * 
	 * Query qCount = session.createQuery("select count(*) from RUser"); long
	 * count = (Long) qCount.uniqueResult(); AssertJUnit.assertEquals(2L,
	 * count);
	 * 
	 * session.getTransaction().commit(); session.close();
	 * 
	 * }
	 *//*
		 * 
		 * @Test public void test002CreateReportFromFile() throws Exception {
		 * 
		 * LOGGER.debug("Create Test report from xml file.  ");
		 * 
		 * try {
		 * 
		 * Task task = taskManager.createTaskInstance(CREATE_REPORT_FROM_FILE);
		 * OperationResult result = task.getResult();
		 * 
		 * 
		 * List<PrismObject<? extends Objectable>> report =
		 * prismContext.getPrismDomProcessor().parseObjects( new
		 * File(TEST_REPORT_FILE));
		 * 
		 * for (PrismObject<? extends Objectable> reportType : report) {
		 * 
		 * try { ObjectDelta<ReportType> objectDelta =
		 * ObjectDelta.createAddDelta((PrismObject<ReportType>) reportType);
		 * Collection<ObjectDelta<? extends ObjectType>> deltas =
		 * MiscSchemaUtil.createCollection(objectDelta);
		 * 
		 * modelService.executeChanges(deltas, null, task, result);
		 * 
		 * AssertJUnit.assertEquals(REPORT_OID_TEST, objectDelta.getOid());
		 * 
		 * result.computeStatus(); } catch (Exception ex) {
		 * LOGGER.error("Exception occurred. Create report", ex); }
		 * 
		 * }
		 * 
		 * } catch (Exception ex) {
		 * 
		 * LOGGER.error("Couldn't create jasper report from file.", ex); throw
		 * ex; } }
		 */

	@Test
	public void test003CopyReportWithoutJRXML() throws Exception {
		LOGGER.debug("Copy Test report without jrxml.");

		Task task = taskManager.createTaskInstance(COPY_REPORT_WITHOUT_JRXML);
		OperationResult result = task.getResult();

		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions
				.createCollection(GetOperationOptions.createRaw());
		OperationResult subResult = result.createSubresult("readReport");

		ReportType reportType = modelService.getObject(ReportType.class,
				REPORT_OID_001, options, null, subResult).asObjectable();
		subResult.computeStatus();

		reportType = reportType.clone();
		reportType.setOid(REPORT_OID_002);
		reportType.setReportTemplate(null);
		reportType.setName(new PolyStringType("Test report - Datasource2"));

		ObjectDelta<ReportType> objectDelta = ObjectDelta
				.createAddDelta(reportType.asPrismObject());
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil
				.createCollection(objectDelta);

		modelService.executeChanges(deltas, null, task, result);
		result.computeStatus();

		AssertJUnit.assertEquals(REPORT_OID_002, objectDelta.getOid());
	}

	/*
	 * @Test public void test003GenerateReportDynamic() throws Exception {
	 * 
	 * Map<String, Object> params = new HashMap<String, Object>();
	 * 
	 * Session session = null; DataSourceReport reportDataSource = null;
	 * 
	 * LOGGER.debug("Generating dynamic Test report. DATASOURCE ..... ");
	 * 
	 * try { session = sessionFactory.openSession(); session.beginTransaction();
	 * 
	 * Task task =
	 * taskManager.createTaskInstance(BasicReportTest.class.getName()
	 * +".test003GenerateReportDynamic"); OperationResult result =
	 * task.getResult(); ReportType reportType =
	 * modelReport.getReportType(REPORT_OID_001, task, result);
	 * 
	 * reportType.setOid(REPORT_OID_002); reportType.setName(new
	 * PolyStringType("Test report - Datasource2"));
	 * 
	 * result.computeStatus();
	 * 
	 * OperationResult subResult = new OperationResult("LOAD DATASOURCE");
	 * reportDataSource = new DataSourceReport(reportType, modelReport,
	 * subResult);
	 * 
	 * params.putAll(modelReport.getReportParams(reportType));
	 * params.put(JRParameter.REPORT_DATA_SOURCE, reportDataSource);
	 * 
	 * // Create template JasperDesign jasperDesign =
	 * modelReport.createJasperDesign(reportType); JasperReport jasperReport =
	 * JasperCompileManager.compileReport(jasperDesign);
	 * 
	 * OutputStream outputStreamJRXML = new ByteArrayOutputStream();
	 * JasperCompileManager.writeReportToXmlStream(jasperReport,
	 * outputStreamJRXML);
	 * reportType.setReportTemplateJRXML(outputStreamJRXML.toString());
	 * JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport,
	 * params);
	 * 
	 * modelReport.generateReport(reportType, jasperPrint,
	 * FINAL_EXPORT_REPORT_2);
	 * 
	 * subResult = task.getResult(); modelReport.addReportType(reportType, task,
	 * subResult);
	 * 
	 * subResult.computeStatus(); session.getTransaction().commit(); } catch
	 * (Exception ex) { if (session != null &&
	 * session.getTransaction().isActive()) {
	 * session.getTransaction().rollback(); }
	 * 
	 * LOGGER.error("Couldn't generate jasper report at runtime.", ex); throw
	 * ex; } finally { if (session != null) { session.close(); } } }
	 */
	/*
	 * @Test public void test004GenerateReconciliationReportDynamic() throws
	 * Exception {
	 * 
	 * importReconciliationData();
	 * 
	 * RReport report = new RReport();
	 * 
	 * LOGGER.debug("Creating Test report. RECONCILIATION DATASOURCE ..... ");
	 * 
	 * report.setOid(REPORT_OID_RECONCILIATION);
	 * 
	 * //description and name report.setName(new
	 * RPolyString("Reconciliation","Reconciliation"));
	 * report.setDescription("Reconciliation report for selected resource.");
	 * 
	 * String jrtxFile = null; try { jrtxFile = readFile(STYLE_TEMPLATE_DEFAULT,
	 * StandardCharsets.UTF_8); } catch (Exception ex) {
	 * LOGGER.error("Exception occurred. STYLE_TEMPLATE_DEFAULT", ex);
	 * 
	 * } report.setReportTemplateStyleJRTX(jrtxFile);
	 * 
	 * //orientation
	 * report.setReportOrientation(RUtil.getRepoEnumValue(OrientationType
	 * .LANDSCAPE, ROrientationType.class));
	 * 
	 * //export report.setReportExport(RUtil.getRepoEnumValue(ExportType.PDF,
	 * RExportType.class));
	 * 
	 * //object class
	 * report.setObjectClass(ObjectTypes.getObjectType(ShadowType.
	 * class).getTypeQName());
	 * 
	 * //object query Task task =
	 * taskManager.createTaskInstance(BasicReportTest.
	 * class.getName()+".test004GenerateReconciliationReportDynamic (Resource)"
	 * ); OperationResult result = task.getResult();
	 * 
	 * QName objectClass = modelReport.getObjectClassForResource(RESOURCE_OID,
	 * task, result); List<ObjectFilter> conditions = new
	 * ArrayList<ObjectFilter>();
	 * 
	 * EqualsFilter eqFilter = EqualsFilter.createEqual(ShadowType.class,
	 * prismContext, ShadowType.F_KIND, ShadowKindType.ACCOUNT);
	 * conditions.add(eqFilter); eqFilter =
	 * EqualsFilter.createEqual(ShadowType.class, prismContext,
	 * ShadowType.F_OBJECT_CLASS, objectClass); conditions.add(eqFilter);
	 * RefFilter refFilter = RefFilter.createReferenceEqual(ShadowType.class,
	 * ShadowType.F_RESOURCE_REF, prismContext, RESOURCE_OID);
	 * conditions.add(refFilter); AndFilter queryFilter =
	 * AndFilter.createAnd(conditions);
	 * 
	 * ObjectPaging paging = ObjectPaging.createPaging(0, 10); ObjectQuery query
	 * = ObjectQuery.createObjectQuery(queryFilter, paging); QueryType queryType
	 * = new QueryType(); try { queryType =
	 * QueryConvertor.createQueryType(query, prismContext); } catch (Exception
	 * ex) { LOGGER.error("Exception occurred. QueryType", ex); } try {
	 * queryType.setPaging(PagingConvertor.createPagingType(query.getPaging()));
	 * } catch (Exception ex) {
	 * LOGGER.error("Exception occurred. QueryType pagging", ex); }
	 * report.setQuery(RUtil.toRepo(queryType, prismContext));
	 * 
	 * //fields List<ReportFieldConfigurationType> reportFields = new
	 * ArrayList<ReportFieldConfigurationType>();
	 * 
	 * ReportFieldConfigurationType field = new ReportFieldConfigurationType();
	 * ItemPath itemPath; XPathHolder xpath; Element element; ItemDefinition
	 * itemDef; SchemaRegistry schemaRegistry =
	 * prismContext.getSchemaRegistry();
	 * 
	 * PrismObjectDefinition<UserType> userDefinition =
	 * schemaRegistry.findObjectDefinitionByCompileTimeClass(UserType.class);
	 * 
	 * field.setNameHeaderField("Name"); field.setNameReportField("Name");
	 * itemPath = new ItemPath(ShadowType.F_NAME); xpath = new
	 * XPathHolder(itemPath); element =
	 * xpath.toElement(SchemaConstants.C_ITEM_PATH_FIELD,
	 * DOMUtil.getDocument()); field.setItemPathField(element);
	 * field.setSortOrderNumber(2);
	 * field.setSortOrder(OrderDirectionType.ASCENDING); itemDef =
	 * userDefinition.findItemDefinition(itemPath); field.setWidthField(25);
	 * field.setClassTypeField(itemDef.getTypeName()); reportFields.add(field);
	 * 
	 * field = new ReportFieldConfigurationType();
	 * field.setNameHeaderField("Situtation");
	 * field.setNameReportField("Situation"); itemPath = new
	 * ItemPath(ShadowType.F_SYNCHRONIZATION_SITUATION); xpath = new
	 * XPathHolder(itemPath); element =
	 * xpath.toElement(SchemaConstants.C_ITEM_PATH_FIELD,
	 * DOMUtil.getDocument()); field.setItemPathField(element);
	 * field.setSortOrderNumber(2);
	 * field.setSortOrder(OrderDirectionType.ASCENDING); itemDef =
	 * userDefinition.findItemDefinition(itemPath); field.setWidthField(25);
	 * field.setClassTypeField(itemDef.getTypeName()); reportFields.add(field);
	 * 
	 * field = new ReportFieldConfigurationType();
	 * field.setNameHeaderField("Owner"); field.setNameReportField("Username");
	 * itemPath = new ItemPath(UserType.F_NAME); xpath = new
	 * XPathHolder(itemPath); element =
	 * xpath.toElement(SchemaConstants.C_ITEM_PATH_FIELD,
	 * DOMUtil.getDocument()); field.setItemPathField(element);
	 * field.setSortOrderNumber(null); field.setSortOrder(null); itemDef =
	 * userDefinition.findItemDefinition(itemPath); field.setWidthField(25);
	 * field.setClassTypeField(itemDef.getTypeName()); reportFields.add(field);
	 * 
	 * field = new ReportFieldConfigurationType();
	 * field.setNameHeaderField("Timestamp");
	 * field.setNameReportField("Timestamp"); itemPath = new
	 * ItemPath(ShadowType.F_SYNCHRONIZATION_TIMESTAMP); xpath = new
	 * XPathHolder(itemPath); element =
	 * xpath.toElement(SchemaConstants.C_ITEM_PATH_FIELD,
	 * DOMUtil.getDocument()); field.setItemPathField(element);
	 * field.setSortOrderNumber(null); field.setSortOrder(null); itemDef =
	 * userDefinition.findItemDefinition(itemPath); field.setWidthField(25);
	 * field.setClassTypeField(itemDef.getTypeName()); reportFields.add(field);
	 * 
	 * report.setReportFields(RUtil.toRepo(reportFields, prismContext));
	 * 
	 * //parameters List<ReportParameterConfigurationType> reportParameters =
	 * new ArrayList<ReportParameterConfigurationType>();
	 * 
	 * ReportParameterConfigurationType parameter = new
	 * ReportParameterConfigurationType();
	 * parameter.setNameParameter("LOGO_PATH");
	 * parameter.setValueParameter(REPORTS_DIR.getPath() + "/logo.jpg");
	 * parameter.setClassTypeParameter(DOMUtil.XSD_ANY);
	 * reportParameters.add(parameter);
	 * 
	 * parameter = new ReportParameterConfigurationType();
	 * parameter.setNameParameter("BaseTemplateStyles");
	 * parameter.setValueParameter(STYLE_TEMPLATE_DEFAULT);
	 * parameter.setClassTypeParameter(DOMUtil.XSD_STRING);
	 * reportParameters.add(parameter);
	 * 
	 * parameter = new ReportParameterConfigurationType();
	 * parameter.setNameParameter("RESOURCE_NAME");
	 * parameter.setValueParameter(RESOURCE_NAME);
	 * parameter.setDescriptionParameter("Resource");
	 * parameter.setClassTypeParameter(DOMUtil.XSD_STRING);
	 * reportParameters.add(parameter);
	 * 
	 * parameter = new ReportParameterConfigurationType();
	 * parameter.setNameParameter("INTENT");
	 * parameter.setValueParameter(INTENT);
	 * parameter.setDescriptionParameter("Intent");
	 * parameter.setClassTypeParameter(DOMUtil.XSD_STRING);
	 * reportParameters.add(parameter);
	 * 
	 * parameter = new ReportParameterConfigurationType();
	 * parameter.setNameParameter("CLASS");
	 * parameter.setValueParameter(objectClass.getLocalPart());
	 * parameter.setDescriptionParameter("Object class");
	 * parameter.setClassTypeParameter(DOMUtil.XSD_STRING);
	 * reportParameters.add(parameter);
	 * 
	 * report.setReportParameters(RUtil.toRepo(reportParameters, prismContext));
	 * 
	 * Map<String, Object> params = new HashMap<String, Object>();
	 * 
	 * Session session = null; DataSourceReport reportDataSource = null;
	 * 
	 * LOGGER.debug("Generating dynamic Test report. RECONCILIATION ..... ");
	 * 
	 * try { session = sessionFactory.openSession(); session.beginTransaction();
	 * 
	 * task = taskManager.createTaskInstance(BasicReportTest.class.getName()+
	 * ".test004GenerateReconciliationReportDynamic (Add Report Type)"); result
	 * = task.getResult(); modelReport.addReportType(report.toJAXB(prismContext,
	 * null), task, result);
	 * 
	 * OperationResult subResult = result.createSubresult("READ REPORT RECORD");
	 * ReportType reportType =
	 * modelReport.getReportType(REPORT_OID_RECONCILIATION, task, subResult);
	 * subResult.computeStatus();
	 * 
	 * result.computeStatus();
	 * 
	 * subResult = new OperationResult("LOAD RECONCILIATION REPORT");
	 * reportDataSource = new DataSourceReport(reportType, modelReport,
	 * subResult);
	 * 
	 * params.putAll(modelReport.getReportParams(reportType));
	 * params.put(JRParameter.REPORT_DATA_SOURCE, reportDataSource);
	 * 
	 * // Create template JasperDesign jasperDesign =
	 * modelReport.createJasperDesign(reportType); JasperReport jasperReport =
	 * JasperCompileManager.compileReport(jasperDesign);
	 * 
	 * OutputStream outputStreamJRXML = new ByteArrayOutputStream();
	 * JasperCompileManager.writeReportToXmlStream(jasperReport,
	 * outputStreamJRXML);
	 * reportType.setReportTemplateJRXML(outputStreamJRXML.toString());
	 * JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport,
	 * params);
	 * 
	 * modelReport.generateReport(reportType, jasperPrint,
	 * FINAL_EXPORT_REPORT_RECONCILIATION);
	 * 
	 * subResult = task.getResult(); modelReport.addReportType(reportType, task,
	 * subResult);
	 * 
	 * subResult.computeStatus(); session.getTransaction().commit(); } catch
	 * (Exception ex) { if (session != null &&
	 * session.getTransaction().isActive()) {
	 * session.getTransaction().rollback(); }
	 * 
	 * LOGGER.error("Couldn't generate jasper report at runtime.", ex); throw
	 * ex; } finally { if (session != null) { session.close(); } } }
	 */
	/*
	 * @Test public void test005GenerateUserListReportDynamic() throws Exception
	 * {
	 * 
	 * Map<String, Object> params = new HashMap<String, Object>();
	 * 
	 * Session session = null; DataSourceReport reportDataSource = null;
	 * 
	 * LOGGER.debug("Generating dynamic Test report. DATASOURCE ..... ");
	 * 
	 * try { session = sessionFactory.openSession(); session.beginTransaction();
	 * 
	 * Task task =
	 * taskManager.createTaskInstance(BasicReportTest.class.getName()
	 * +".test003GenerateReportDynamic"); OperationResult result =
	 * task.getResult(); ReportType reportType =
	 * modelReport.getReportType(REPORT_OID_001, task, result);
	 * 
	 * reportType.setOid(REPORT_OID_002); reportType.setName(new
	 * PolyStringType("Test report - Datasource2"));
	 * 
	 * result.computeStatus();
	 * 
	 * OperationResult subResult = new OperationResult("LOAD DATASOURCE");
	 * reportDataSource = new DataSourceReport(reportType, modelReport,
	 * subResult);
	 * 
	 * params.putAll(modelReport.getReportParams(reportType));
	 * params.put(JRParameter.REPORT_DATA_SOURCE, reportDataSource);
	 * 
	 * // Create template JasperDesign jasperDesign =
	 * modelReport.createJasperDesign(reportType); JasperReport jasperReport =
	 * JasperCompileManager.compileReport(jasperDesign);
	 * 
	 * OutputStream outputStreamJRXML = new ByteArrayOutputStream();
	 * JasperCompileManager.writeReportToXmlStream(jasperReport,
	 * outputStreamJRXML);
	 * reportType.setReportTemplateJRXML(outputStreamJRXML.toString());
	 * JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport,
	 * params);
	 * 
	 * modelReport.generateReport(reportType, jasperPrint,
	 * FINAL_EXPORT_REPORT_2);
	 * 
	 * subResult = task.getResult(); modelReport.addReportType(reportType, task,
	 * subResult);
	 * 
	 * subResult.computeStatus(); session.getTransaction().commit(); } catch
	 * (Exception ex) { if (session != null &&
	 * session.getTransaction().isActive()) {
	 * session.getTransaction().rollback(); }
	 * 
	 * LOGGER.error("Couldn't generate jasper report at runtime.", ex); throw
	 * ex; } finally { if (session != null) { session.close(); } } }
	 * 
	 * @Test public void test006GenerateAuditLOGReportDynamic() throws Exception
	 * {
	 * 
	 * Map<String, Object> params = new HashMap<String, Object>();
	 * 
	 * Session session = null; DataSourceReport reportDataSource = null;
	 * 
	 * LOGGER.debug("Generating dynamic Test report. DATASOURCE ..... ");
	 * 
	 * try { session = sessionFactory.openSession(); session.beginTransaction();
	 * 
	 * Task task =
	 * taskManager.createTaskInstance(BasicReportTest.class.getName()
	 * +".test003GenerateReportDynamic"); OperationResult result =
	 * task.getResult(); ReportType reportType =
	 * modelReport.getReportType(REPORT_OID_001, task, result);
	 * 
	 * reportType.setOid(REPORT_OID_002); reportType.setName(new
	 * PolyStringType("Test report - Datasource2"));
	 * 
	 * result.computeStatus();
	 * 
	 * OperationResult subResult = new OperationResult("LOAD DATASOURCE");
	 * reportDataSource = new DataSourceReport(reportType, modelReport,
	 * subResult);
	 * 
	 * params.putAll(modelReport.getReportParams(reportType));
	 * params.put(JRParameter.REPORT_DATA_SOURCE, reportDataSource);
	 * 
	 * // Create template JasperDesign jasperDesign =
	 * modelReport.createJasperDesign(reportType); JasperReport jasperReport =
	 * JasperCompileManager.compileReport(jasperDesign);
	 * 
	 * OutputStream outputStreamJRXML = new ByteArrayOutputStream();
	 * JasperCompileManager.writeReportToXmlStream(jasperReport,
	 * outputStreamJRXML);
	 * reportType.setReportTemplateJRXML(outputStreamJRXML.toString());
	 * JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport,
	 * params);
	 * 
	 * modelReport.generateReport(reportType, jasperPrint,
	 * FINAL_EXPORT_REPORT_2);
	 * 
	 * subResult = task.getResult(); modelReport.addReportType(reportType, task,
	 * subResult);
	 * 
	 * subResult.computeStatus(); session.getTransaction().commit(); } catch
	 * (Exception ex) { if (session != null &&
	 * session.getTransaction().isActive()) {
	 * session.getTransaction().rollback(); }
	 * 
	 * LOGGER.error("Couldn't generate jasper report at runtime.", ex); throw
	 * ex; } finally { if (session != null) { session.close(); } } }
	 * 
	 * @Test public void test007ExportReport() throws Exception {
	 * 
	 * Map<String, Object> params = new HashMap<String, Object>();
	 * 
	 * Session session = null; DataSourceReport reportDataSource = null;
	 * 
	 * LOGGER.debug("Export Test report into various types. DATASOURCE ..... ");
	 * 
	 * try { session = sessionFactory.openSession(); session.beginTransaction();
	 * 
	 * Task task =
	 * taskManager.createTaskInstance(BasicReportTest.class.getName()
	 * +".test004ExportReport"); OperationResult result = task.getResult();
	 * ReportType reportType = modelReport.getReportType(REPORT_OID_002, task,
	 * result); result.computeStatus();
	 * 
	 * OperationResult subResult = new OperationResult("LOAD DATASOURCE");
	 * reportDataSource = new DataSourceReport(reportType, modelReport,
	 * subResult);
	 * 
	 * params.putAll(modelReport.getReportParams(reportType));
	 * params.put(JRParameter.REPORT_DATA_SOURCE, reportDataSource);
	 * 
	 * //Loading template InputStream inputStreamJRXML = new
	 * ByteArrayInputStream
	 * (reportType.getReportTemplateJRXML().getBytes("UTF-8")); JasperDesign
	 * jasperDesign = JRXmlLoader.load(inputStreamJRXML); JasperReport
	 * jasperReport = JasperCompileManager.compileReport(jasperDesign);
	 * JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport,
	 * params);
	 * 
	 * for(ExportType exportType : ExportType.values()) {
	 * reportType.setReportExport(exportType);
	 * modelReport.generateReport(reportType, jasperPrint,
	 * FINAL_EXPORT_REPORT_3); }
	 * 
	 * subResult.computeStatus(); session.getTransaction().commit(); } catch
	 * (Exception ex) { if (session != null &&
	 * session.getTransaction().isActive()) {
	 * session.getTransaction().rollback(); }
	 * 
	 * LOGGER.error("Couldn't export jasper report.", ex); throw ex; } finally {
	 * if (session != null) { session.close(); } }
	 * 
	 * }
	 */

	@Test
	public void test006CountReport() throws Exception {
		LOGGER.debug("Count Test report..... ");

		Task task = taskManager.createTaskInstance(COUNT_REPORT);
		OperationResult result = task.getResult();

		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions
				.createCollection(GetOperationOptions.createRaw());

		int count = modelService.countObjects(ReportType.class, null, options,
				task, result);
		result.computeStatus();

		AssertJUnit.assertEquals(2, count);
	}

	@Test
	public void test007SearchReport() throws Exception {
		LOGGER.debug("Search Test report..... ");

		Task task = taskManager.createTaskInstance(SEARCH_REPORT);
		OperationResult result = task.getResult();
		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions
				.createCollection(GetOperationOptions.createRaw());

		List<PrismObject<ReportType>> listReportType = modelService
				.searchObjects(ReportType.class, null, options, task, result);
		result.computeStatus();

		AssertJUnit.assertEquals(2, listReportType.size());
	}

	@Test
	public void test008ModifyReport() throws Exception {
		LOGGER.debug("Modify Test report..... ");
		Task task = taskManager.createTaskInstance(MODIFY_REPORT);
		OperationResult result = task.getResult();

		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions
				.createCollection(GetOperationOptions.createRaw());
		OperationResult subResult = result.createSubresult("readReport");

		ReportType reportType = modelService.getObject(ReportType.class,
				REPORT_OID_001, options, null, subResult).asObjectable();
		subResult.computeStatus();

		reportType.setReportExport(ExportType.CSV);

		Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
		PrismObject<ReportType> reportTypeOld = modelService.getObject(
				ReportType.class, REPORT_OID_001, null, task, result);
		deltas.add(reportTypeOld.diff(reportType.asPrismObject()));

		modelService.executeChanges(deltas, null, task, result);
		result.computeStatus();

		options = SelectorOptions.createCollection(GetOperationOptions
				.createRaw());
		subResult = result.createSubresult("readReport");

		reportType = modelService.getObject(ReportType.class, REPORT_OID_001,
				options, null, subResult).asObjectable();
		subResult.computeStatus();

		AssertJUnit.assertEquals(ExportType.CSV, reportType.getReportExport());
	}

	@Test
	public void test009DeleteReport() throws Exception {
		LOGGER.debug("Delete Test report..... ");

		Task task = taskManager.createTaskInstance(DELETE_REPORT);
		OperationResult result = task.getResult();

		ObjectDelta<ReportType> delta = ObjectDelta.createDeleteDelta(
				ReportType.class, REPORT_OID_001, prismContext);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil
				.createCollection(delta);
		modelService.executeChanges(deltas, null, task, result);
		result.computeStatus();

		try {
			OperationResult subResult = result.createSubresult("readReport");
			Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions
					.createCollection(GetOperationOptions.createRaw());
			PrismObject<ReportType> reportTypePrism = modelService.getObject(
					ReportType.class, REPORT_OID_001, options, null, subResult);
			subResult.computeStatus();
			display("Report type after", reportTypePrism);
			assert false : "Report type was not deleted: " + reportTypePrism;
		} catch (ObjectNotFoundException e) {
			// This is expected
		}
	}

}
