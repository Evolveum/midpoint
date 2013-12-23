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
import static org.testng.AssertJUnit.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

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
import com.evolveum.midpoint.prism.Objectable;
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
public class BasicReportTest extends AbstractModelIntegrationTest {

	private static final String CLASS_NAME_WITH_DOT = BasicReportTest.class
			.getName() + ".";
	private static final String GET_REPORT = CLASS_NAME_WITH_DOT
			+ "getReport";
	private static final String CREATE_REPORT = CLASS_NAME_WITH_DOT
			+ "test001CreateReport";
	private static final String CREATE_REPORT_FROM_FILE = CLASS_NAME_WITH_DOT
			+ "test002CreateReportFromFile";
	private static final String COPY_REPORT_WITHOUT_JRXML = CLASS_NAME_WITH_DOT
			+ "test003CopyReportWithoutJRXML";
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
	
	private static final String TEST_REPORT_FILE = REPORTS_DIR + "/report-test.xml";
	private static final String REPORT_DATASOURCE_TEST = REPORTS_DIR + "/reportDataSourceTest.jrxml";
	private static final String STYLE_TEMPLATE_DEFAULT = STYLES_DIR	+ "/midpoint_base_styles.jrtx";
	
	private static final String REPORT_OID_001 = "00000000-3333-3333-0000-100000000001";
	private static final String REPORT_OID_002 = "00000000-3333-3333-0000-100000000002";
	private static final String REPORT_OID_TEST = "00000000-3333-3333-TEST-10000000000";
	/*
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

	
	private PrismObject<ReportType> getReport(String reportOid) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
		Task task = taskManager.createTaskInstance(GET_REPORT);
        OperationResult result = task.getResult();
		PrismObject<ReportType> report = modelService.getObject(ReportType.class, reportOid, null, task, result);
		result.computeStatus();
		TestUtil.assertSuccess("getObject(Report) result not success", result);
		return report;
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
		String jrxmlFile = readFile(REPORT_DATASOURCE_TEST, StandardCharsets.UTF_8);
		ReportTemplateType reportTemplate = new ReportTemplateType();
		reportTemplate.setAny(DOMUtil.parseDocument(jrxmlFile).getDocumentElement());
		reportType.setReportTemplate(reportTemplate);

		String jrtxFile = readFile(STYLE_TEMPLATE_DEFAULT, StandardCharsets.UTF_8);
		ReportTemplateStyleType reportTemplateStyle = new ReportTemplateStyleType();
		reportTemplateStyle.setAny(DOMUtil.parseDocument(jrtxFile).getDocumentElement());
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
		
		//WHEN
		TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        display(result);
        TestUtil.assertSuccess(result);
		AssertJUnit.assertEquals(REPORT_OID_001, objectDelta.getOid());

		reportType = getReport(REPORT_OID_001).asObjectable();

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
		
		
		String reportTemplateRepoString = DOMUtil.serializeDOMToString((Node)reportType.getReportTemplate().getAny());
   	 	InputStream inputStreamJRXML = new ByteArrayInputStream(reportTemplateRepoString.getBytes());
   	 	JasperDesign jasperDesignRepo = JRXmlLoader.load(inputStreamJRXML);
   	 	
   	 	String reportTemplateString = DOMUtil.serializeDOMToString((Node)reportTemplate.getAny());
	 	inputStreamJRXML = new ByteArrayInputStream(reportTemplateString.getBytes());
	 	JasperDesign jasperDesign = JRXmlLoader.load(inputStreamJRXML);
	 	*/
	 	//AssertJUnit.assertEquals(jasperDesign, jasperDesignRepo);
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
	
	@Test 
	public void test002CreateReportFromFile() throws Exception {
		
		final String TEST_NAME = "test002CreateReportFromFile";
        TestUtil.displayTestTile(this, TEST_NAME);
	
		Task task = taskManager.createTaskInstance(CREATE_REPORT_FROM_FILE);
		OperationResult result = task.getResult();
		 
		PrismObject<? extends Objectable> reportType=  prismContext.getPrismDomProcessor().parseObject(new File(TEST_REPORT_FILE));
			 
		ObjectDelta<ReportType> objectDelta =
		ObjectDelta.createAddDelta((PrismObject<ReportType>) reportType);
		Collection<ObjectDelta<? extends ObjectType>> deltas =
		MiscSchemaUtil.createCollection(objectDelta);
		
		//WHEN 	
		TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(deltas, null, task, result);
		
		//THEN  
		TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        display(result);
        TestUtil.assertSuccess(result);
		AssertJUnit.assertEquals(REPORT_OID_TEST, objectDelta.getOid());
		
		
	}
	@Test
	public void test003CopyReportWithoutJRXML() throws Exception {
		final String TEST_NAME = "test003CopyReportWithoutJRXML";
        TestUtil.displayTestTile(this, TEST_NAME);

		Task task = taskManager.createTaskInstance(COPY_REPORT_WITHOUT_JRXML);
		OperationResult result = task.getResult();

		ReportType reportType = getReport(REPORT_OID_001).asObjectable();

		reportType = reportType.clone();
		reportType.setOid(REPORT_OID_002);
		reportType.setReportTemplate(null);
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
	public void test006CountReport() throws Exception {
		final String TEST_NAME = "test006CountReport";
        TestUtil.displayTestTile(this, TEST_NAME);

		Task task = taskManager.createTaskInstance(COUNT_REPORT);
		OperationResult result = task.getResult();

		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions
				.createCollection(GetOperationOptions.createRaw());

		//WHEN 	
		TestUtil.displayWhen(TEST_NAME);
		int count = modelService.countObjects(ReportType.class, null, options,
				task, result);
		//THEN
		TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        display(result);
        TestUtil.assertSuccess(result);
        assertEquals("Unexpected number of reports", 3, count);
	}

	@Test
	public void test007SearchReport() throws Exception {
		final String TEST_NAME = "test007SearchReport";
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
		assertEquals("Unexpected number of searching reports", 3, listReportType.size());
	}

	@Test
	public void test008ModifyReport() throws Exception {
		final String TEST_NAME = "test008ModifyReport";
        TestUtil.displayTestTile(this, TEST_NAME);
		
		Task task = taskManager.createTaskInstance(MODIFY_REPORT);
		OperationResult result = task.getResult();

		ReportType reportType = getReport(REPORT_OID_001).asObjectable();

		reportType.setReportExport(ExportType.CSV);

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
		
		reportType = getReport(REPORT_OID_001).asObjectable();
		assertEquals("Unexpected export type", ExportType.CSV, reportType.getReportExport());
	}
	
	/**
	 * Delete report type.
	 */
	@Test
	public void test009DeleteReport() throws Exception {
		
		final String TEST_NAME = "test009DeleteReport";
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
        	PrismObject<ReportType> report = getReport(REPORT_OID_001);
        	AssertJUnit.fail("Report type was not deleted");
        } catch (ObjectNotFoundException e) {
        	// This is expected
        }	
		
	}

}
