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

import static org.testng.AssertJUnit.assertNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.repo.sql.data.common.RReport;
import com.evolveum.midpoint.repo.sql.data.common.RUser;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RActivation;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.RActivationStatus;
import com.evolveum.midpoint.repo.sql.data.common.enums.RExportType;
import com.evolveum.midpoint.repo.sql.data.common.enums.ROrientationType;
import com.evolveum.midpoint.repo.sql.data.common.id.RContainerId;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.PagingConvertor;
import com.evolveum.midpoint.schema.QueryConvertor;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExportType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrientationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportFieldConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportParameterConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.query_2.OrderDirectionType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
//import ar.com.fdvs.dj.core.DynamicJasperHelper;

/**
 * @author garbika
 */
@ContextConfiguration(locations = {"classpath:ctx-report-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class BasicReportTest extends AbstractTestNGSpringContextTests {

    private static final Trace LOGGER = TraceManager.getTrace(BasicReportTest.class);
    
    private static final File REPORTS_DIR = new File("src/test/resources/reports");
    private static final File STYLES_DIR = new File("src/test/resources/styles");
    
    private static final String REPORT_DATASOURCE_TEST_1 = REPORTS_DIR + "/reportDataSourceTest.jrxml";
    private static final String STYLE_TEMPLATE_DEFAULT = STYLES_DIR + "/midpoint_base_styles.jrtx";
    
    //private static final String FINAL_REPORT_1 = REPORTS_DIR + "/reportDataSourceTestFinal_1.jrprint";
    private static final String FINAL_EXPORT_REPORT_1 = REPORTS_DIR + "/reportDataSourceTestFinal_1";
    
    //private static final String REPORT_DATASOURCE_TEST_2 = REPORTS_DIR + "/reportDataSourceTest_2.jrxml";
    //private static final String FINAL_JASPER_REPORT_2 = REPORTS_DIR + "/reportDataSourceTestFinal_2.jasper";
    //private static final String FINAL_REPORT_2 = REPORTS_DIR + "/reportDataSourceTestFinal_2.jrprint";
    private static final String FINAL_EXPORT_REPORT_2 = REPORTS_DIR + "/reportDataSourceTestFinal_2";
    
    private static final String FINAL_EXPORT_REPORT_3 = REPORTS_DIR + "/reportDataSourceTestFinal_3";
    
    private static final String REPORT_OID_001 = "00000000-3333-3333-0000-100000000001";
    private static final String REPORT_OID_002 = "00000000-3333-3333-0000-100000000002";
    
    @Autowired
    private SessionFactory sessionFactory;
        
    @Autowired
    private PrismContext prismContext;
    
    @Autowired
	protected TaskManager taskManager;
    
    @Autowired
    private ModelReport modelReport;
    
    private static String readFile(String path, Charset encoding) 
			  throws IOException 
	{
			  byte[] encoded = Files.readAllBytes(Paths.get(path));
			  return encoding.decode(ByteBuffer.wrap(encoded)).toString();
	}
 
    private RReport getReport(String oid) {
        Session session = sessionFactory.openSession();
        session.beginTransaction();
        RReport report = (RReport) session.get(RReport.class, new RContainerId(0L, oid));
        session.getTransaction().commit();
        session.close();

        return report;
    }

       
    
    @Test
    public void test001CreateReport() throws Exception {
    	
    	
        RReport report = new RReport();
         
         LOGGER.debug("Creating Test report. DATASOURCE ..... ");
         
         report.setOid(REPORT_OID_001);
         
         //description and name
         report.setName(new RPolyString("Test report - Datasource1","Test report - Datasource1"));
         report.setDescription("TEST Report with DataSource parameter.");
         
         //file templates
         String jrxmlFile = null;
         //JRXmlLoader.Load();
         try 
         {
        	 jrxmlFile = readFile(REPORT_DATASOURCE_TEST_1, StandardCharsets.UTF_8);
        	 //jrxmlFile = JRLoader.loadObjectFromFile(REPORT_DATASOURCE_TEST_1).toString();
         }
         catch (Exception ex)
         {
        	 LOGGER.error("Exception occurred. REPORT_DATASOURCE_TEST", ex);
            
         }
         report.setReportTemplateJRXML(jrxmlFile);
         
         String jrtxFile = null;
         try
         {
        	 jrtxFile = readFile(STYLE_TEMPLATE_DEFAULT, StandardCharsets.UTF_8);
        	 //jrtxFile = JRLoader.loadObjectFromFile(STYLE_TEMPLATE_DEFAULT).toString();
         }
         catch (Exception ex)
         {
        	 LOGGER.error("Exception occurred. STYLE_TEMPLATE_DEFAULT", ex);
            
         }
         report.setReportTemplateStyleJRTX(jrtxFile);
         
         //orientation
         report.setReportOrientation(RUtil.getRepoEnumValue(OrientationType.LANDSCAPE, ROrientationType.class));
         
         //export
     	 report.setReportExport(RUtil.getRepoEnumValue(ExportType.PDF, RExportType.class));
     	 
         //object class
         report.setObjectClass(ObjectTypes.getObjectType(UserType.class).getTypeQName());
         
         //object query
         ObjectPaging paging = ObjectPaging.createPaging(0, 10);
         ObjectQuery query = ObjectQuery.createObjectQuery(paging);
         QueryType queryType = new QueryType();
         try
         {
        	 queryType = QueryConvertor.createQueryType(query, prismContext);
         }
         catch (Exception ex)
         {
        	 LOGGER.error("Exception occurred. QueryType", ex);
         }
         try
         {
        	 queryType.setPaging(PagingConvertor.createPagingType(query.getPaging()));
         }
         catch (Exception ex)
         {
        	 LOGGER.error("Exception occurred. QueryType pagging", ex);
         }  
         report.setQuery(RUtil.toRepo(queryType, prismContext));
          
         //fields
         List<ReportFieldConfigurationType> reportFields = new ArrayList<ReportFieldConfigurationType>();
         
         ReportFieldConfigurationType field = new ReportFieldConfigurationType();
         ItemPath itemPath;
         XPathHolder xpath;
         Element element;
         ItemDefinition itemDef;
         SchemaRegistry schemaRegistry  = prismContext.getSchemaRegistry();  
         
         PrismObjectDefinition<UserType> userDefinition = schemaRegistry.findObjectDefinitionByCompileTimeClass(UserType.class);
       
         field.setNameHeaderField("Name");
         field.setNameReportField("Name");
         itemPath = new ItemPath(UserType.F_NAME);
         xpath = new XPathHolder(itemPath);
         element = xpath.toElement(SchemaConstants.C_ITEM_PATH_FIELD, DOMUtil.getDocument());//(SchemaConstantsGenerated.NS_COMMON, "itemPathField");         
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
         element = xpath.toElement(SchemaConstants.C_ITEM_PATH_FIELD, DOMUtil.getDocument());
         //element = xpath.toElement(SchemaConstantsGenerated.NS_COMMON, "itemPathField");         
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
         element = xpath.toElement(SchemaConstants.C_ITEM_PATH_FIELD, DOMUtil.getDocument());
         //element = xpath.toElement(SchemaConstantsGenerated.NS_COMMON, "itemField");         
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
         itemPath = new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS);
         xpath = new XPathHolder(itemPath);
         element = xpath.toElement(SchemaConstants.C_ITEM_PATH_FIELD, DOMUtil.getDocument());
         //element = xpath.toElement(SchemaConstantsGenerated.NS_COMMON, "itemField");         
         field.setItemPathField(element);         
         field.setSortOrderNumber(null);
         field.setSortOrder(null);
         itemDef = userDefinition.findItemDefinition(itemPath);
         field.setWidthField(25);
         field.setClassTypeField(itemDef.getTypeName());    
         reportFields.add(field);
        
         report.setReportFields(RUtil.toRepo(reportFields, prismContext));
         
         
         //parameters
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
         
         report.setReportParameters(RUtil.toRepo(reportParameters, prismContext));
         
         Task task = taskManager.createTaskInstance(BasicReportTest.class.getName()+".test001CreateReport");
         OperationResult result = task.getResult();
         modelReport.addReportType(report.toJAXB(prismContext, null), task, result);
         
         OperationResult subResult = result.createSubresult("READ REPORT RECORD");
         ReportType reportType = modelReport.getReportType(REPORT_OID_001, task, subResult);
         subResult.computeStatus();
         
         // check reportType
         AssertJUnit.assertNotNull(reportType);
         AssertJUnit.assertEquals("Test report - Datasource1", reportType.getName().getOrig());
         AssertJUnit.assertEquals("TEST Report with DataSource parameter.", reportType.getDescription());
 		 AssertJUnit.assertEquals(jrxmlFile, reportType.getReportTemplateJRXML());
         AssertJUnit.assertEquals(jrtxFile, reportType.getReportTemplateStyleJRTX());
         AssertJUnit.assertEquals(OrientationType.LANDSCAPE, reportType.getReportOrientation());
         AssertJUnit.assertEquals(ExportType.PDF, reportType.getReportExport());
         AssertJUnit.assertEquals(ObjectTypes.getObjectType(UserType.class).getTypeQName(), reportType.getObjectClass());      
         AssertJUnit.assertEquals(queryType, reportType.getQuery());
         
         int fieldCount = reportFields.size();
         List<ReportFieldConfigurationType> fieldsRepo = reportType.getReportFields();
        
         ReportFieldConfigurationType fieldRepo = null;
         AssertJUnit.assertEquals(fieldCount, fieldsRepo.size());
         for (int i=0; i<fieldCount; i++)
         {
        	 fieldRepo = fieldsRepo.get(i);
        	 field = reportFields.get(i);
        	 AssertJUnit.assertEquals(field.getNameHeaderField(), fieldRepo.getNameHeaderField());
        	 AssertJUnit.assertEquals(field.getNameReportField(), fieldRepo.getNameReportField());
        	 ItemPath fieldPath = new XPathHolder(field.getItemPathField()).toItemPath();
        	 ItemPath fieldRepoPath = new XPathHolder(fieldRepo.getItemPathField()).toItemPath();
        	 AssertJUnit.assertEquals(fieldPath, fieldRepoPath);
        	 AssertJUnit.assertEquals(field.getSortOrder(), fieldRepo.getSortOrder());
        	 AssertJUnit.assertEquals(field.getSortOrderNumber(), fieldRepo.getSortOrderNumber());
        	 AssertJUnit.assertEquals(field.getWidthField(), fieldRepo.getWidthField());
        	 AssertJUnit.assertEquals(field.getClassTypeField(), fieldRepo.getClassTypeField());
         }
        
         
         int parameterCount = reportParameters.size();
         List<ReportParameterConfigurationType> parametersRepo = reportType.getReportParameters();
        
         ReportParameterConfigurationType parameterRepo = null;
         AssertJUnit.assertEquals(parameterCount, parametersRepo.size());
         for (int i=0; i<parameterCount; i++)
         {
        	 parameterRepo = parametersRepo.get(i);
        	 parameter = reportParameters.get(i);
        	 AssertJUnit.assertEquals(parameter.getNameParameter(), parameterRepo.getNameParameter());
        	 AssertJUnit.assertEquals(parameter.getValueParameter(), parameterRepo.getValueParameter());
        	 AssertJUnit.assertEquals(parameter.getClassTypeParameter(), parameterRepo.getClassTypeParameter());
         }
        
         report = getReport(REPORT_OID_001);
         
      // check report
         AssertJUnit.assertNotNull(report);
         AssertJUnit.assertEquals("Test report - Datasource1", report.getName().getOrig());
         AssertJUnit.assertEquals("TEST Report with DataSource parameter.", report.getDescription());
 		 AssertJUnit.assertEquals(jrxmlFile, report.getReportTemplateJRXML());
         AssertJUnit.assertEquals(jrtxFile, report.getReportTemplateStyleJRTX());
         AssertJUnit.assertEquals(RUtil.getRepoEnumValue(OrientationType.LANDSCAPE, ROrientationType.class), report.getReportOrientation());
         AssertJUnit.assertEquals(RUtil.getRepoEnumValue(ExportType.PDF, RExportType.class), report.getReportExport());
         AssertJUnit.assertEquals(ObjectTypes.getObjectType(UserType.class).getTypeQName(), report.getObjectClass());      
         AssertJUnit.assertEquals(queryType, RUtil.toJAXB(ReportType.class, new ItemPath(ReportType.F_QUERY), report.getQuery(), QueryType.class, prismContext));
         
         fieldsRepo = null;
         if (StringUtils.isNotEmpty(report.getReportFields())) {
			 fieldsRepo = RUtil.toJAXB(ReportType.class, null, report.getReportFields(), List.class, null, prismContext); 
         }
         AssertJUnit.assertEquals(fieldCount, fieldsRepo.size());
         fieldRepo = null;
         field = null;
         for (int i=0; i<fieldCount; i++)
         {
        	 fieldRepo = fieldsRepo.get(i);
        	 field = reportFields.get(i);
        	 AssertJUnit.assertEquals(field.getNameHeaderField(), fieldRepo.getNameHeaderField());
        	 AssertJUnit.assertEquals(field.getNameReportField(), fieldRepo.getNameReportField());
        	 ItemPath fieldPath = new XPathHolder(field.getItemPathField()).toItemPath();
        	 ItemPath fieldRepoPath = new XPathHolder(fieldRepo.getItemPathField()).toItemPath();
        	 AssertJUnit.assertEquals(fieldPath, fieldRepoPath);
        	 AssertJUnit.assertEquals(field.getSortOrder(), fieldRepo.getSortOrder());
        	 AssertJUnit.assertEquals(field.getSortOrderNumber(), fieldRepo.getSortOrderNumber());
        	 AssertJUnit.assertEquals(field.getClassTypeField(), fieldRepo.getClassTypeField());
         }         
         
         parametersRepo = null;
         if (StringUtils.isNotEmpty(report.getReportParameters())) {
			 parametersRepo = RUtil.toJAXB(ReportType.class, null, report.getReportParameters(), List.class, null, prismContext); 
         }
         AssertJUnit.assertEquals(parameterCount, parametersRepo.size());
         parameterRepo = null;
         parameter = null;
         for (int i=0; i<parameterCount; i++)
         {
        	 parameterRepo = parametersRepo.get(i);
        	 parameter = reportParameters.get(i);
        	 AssertJUnit.assertEquals(parameter.getNameParameter(), parameterRepo.getNameParameter());
        	 AssertJUnit.assertEquals(parameter.getValueParameter(), parameterRepo.getValueParameter());
        	 AssertJUnit.assertEquals(parameter.getClassTypeParameter(), parameterRepo.getClassTypeParameter());
         }
    }
    
    private void generateUsers()
    {
         LOGGER.info("import users");
         
         RUser user = new RUser();
         user.setName(new RPolyString("vilko", "vilko"));
         user.setFamilyName(new RPolyString("repan", "repan"));
         user.setGivenName(user.getName());
         RActivation activation = new RActivation();
         activation.setAdministrativeStatus(RActivationStatus.ENABLED);
         user.setActivation(activation);
        
         Session session = sessionFactory.openSession();
         session.beginTransaction();
         session.save(user);
         session.getTransaction().commit();
         session.close();
         
         LOGGER.info("import vilko");
         
         user = new RUser();
         user.setName(new RPolyString("gabika", "gabika"));
         user.setFamilyName(new RPolyString("polcova", "polcova"));
         user.setGivenName(user.getName());
         activation = new RActivation();
         activation.setAdministrativeStatus(RActivationStatus.ENABLED);
         user.setActivation(activation);
         
         session = sessionFactory.openSession();
         session.beginTransaction();
         session.save(user);
         session.getTransaction().commit();
         session.close();
         
         LOGGER.info("import gabika");
         
         session = sessionFactory.openSession();
         session.beginTransaction();
            
         Query qCount = session.createQuery("select count(*) from RUser");
         long count = (Long) qCount.uniqueResult();
         AssertJUnit.assertEquals(2L, count);
         
         session.getTransaction().commit();
         session.close();
    }
    
    private String[] getColumn(ReportType reportType)
    {
    	List<ReportFieldConfigurationType> fieldsRepo = reportType.getReportFields();
    	
    	String[] column = new String[fieldsRepo.size()];
    	int i=0;
    	for (ReportFieldConfigurationType fieldRepo : fieldsRepo)
    	{
    		column[i] = fieldRepo.getNameHeaderField();
    		i++;
    	}	
    	return column;
    }
          
   
    @Test
    public void test002GenerateReport() throws Exception {
    	
    	generateUsers();
        
    	Map<String, Object> params = new HashMap<String, Object>();
    	
        Session session = null;
        DataSourceReport reportDataSource = null;
        
        LOGGER.debug("Generating Test report. DATASOURCE ..... ");
        
        try {
            
            session = sessionFactory.openSession();
            session.beginTransaction();
                    
            Task task = taskManager.createTaskInstance(BasicReportTest.class.getName()+".test002GenerateReport");
            OperationResult result = task.getResult();
            
            ReportType reportType = modelReport.getReportType(REPORT_OID_001, task, result);
            result.computeStatus();
            
            OperationResult subResult = new OperationResult("LOAD DATASOURCE");
            reportDataSource = new DataSourceReport(reportType, modelReport, subResult);
            
            params.putAll(modelReport.getReportParams(reportType));
            params.put("columns", getColumn(reportType));
            params.put(JRParameter.REPORT_DATA_SOURCE, reportDataSource);
        
            // Loading template
            InputStream inputStreamJRXML = new ByteArrayInputStream(reportType.getReportTemplateJRXML().getBytes("UTF-8"));
            JasperDesign jasperDesign = JRXmlLoader.load(inputStreamJRXML); 
   
            JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params);
 
            modelReport.generateReport(reportType, jasperPrint, FINAL_EXPORT_REPORT_1);
         
            subResult.computeStatus();
        
            session.getTransaction().commit();
        } catch (Exception ex) {
        	if (session != null && session.getTransaction().isActive()) {
        		session.getTransaction().rollback();
        	}

            LOGGER.error("Couldn't generate jasper report.", ex);
        	throw ex;
        } finally {
        	if (session != null) {
        		session.close();
        	}
        }
    }
    
    @Test
    public void test003GenerateReportDynamic() throws Exception {
    	
    	Map<String, Object> params = new HashMap<String, Object>();
    	
        Session session = null;
        DataSourceReport reportDataSource = null;
        
        LOGGER.debug("Generating dynamic Test report. DATASOURCE ..... ");
        
        try {
            session = sessionFactory.openSession();
            session.beginTransaction();
               
            Task task = taskManager.createTaskInstance(BasicReportTest.class.getName()+".test003GenerateReportDynamic");
            OperationResult result = task.getResult();
            ReportType reportType = modelReport.getReportType(REPORT_OID_001, task, result);
            
            reportType.setOid(REPORT_OID_002);
            reportType.setName(new PolyStringType("Test report - Datasource2"));
            
            result.computeStatus();
            
            OperationResult subResult = new OperationResult("LOAD DATASOURCE");
            reportDataSource = new DataSourceReport(reportType, modelReport, subResult);
        
            params.putAll(modelReport.getReportParams(reportType));
            params.put(JRParameter.REPORT_DATA_SOURCE, reportDataSource);
        
            // Create template
            JasperDesign jasperDesign = modelReport.createJasperDesign(reportType);
            JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
          
            OutputStream outputStreamJRXML = new ByteArrayOutputStream();  
            JasperCompileManager.writeReportToXmlStream(jasperReport, outputStreamJRXML);
            reportType.setReportTemplateJRXML(outputStreamJRXML.toString()); 
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params);
            
            modelReport.generateReport(reportType, jasperPrint, FINAL_EXPORT_REPORT_2);
          
            subResult = task.getResult();
            modelReport.addReportType(reportType, task, subResult);
            
            subResult.computeStatus();
            session.getTransaction().commit();
        } catch (Exception ex) {
        	if (session != null && session.getTransaction().isActive()) {
        		session.getTransaction().rollback();
        	}

            LOGGER.error("Couldn't generate jasper report at runtime.", ex);
        	throw ex;
        } finally {
        	if (session != null) {
        		session.close();
        	}
        }
    }
    
    @Test
    public void test004ExportReport() throws Exception {
    	
    	Map<String, Object> params = new HashMap<String, Object>();
    	
        Session session = null;
        DataSourceReport reportDataSource = null;
        
        LOGGER.debug("Export Test report into various types. DATASOURCE ..... ");
        
        try {
            session = sessionFactory.openSession();
            session.beginTransaction();
               
            Task task = taskManager.createTaskInstance(BasicReportTest.class.getName()+".test004ExportReport");
            OperationResult result = task.getResult();
            ReportType reportType = modelReport.getReportType(REPORT_OID_002, task, result);
            result.computeStatus();
            
            OperationResult subResult = new OperationResult("LOAD DATASOURCE");
            reportDataSource = new DataSourceReport(reportType, modelReport, subResult);
        
            params.putAll(modelReport.getReportParams(reportType));
            params.put(JRParameter.REPORT_DATA_SOURCE, reportDataSource);
        
            //Loading template
            InputStream inputStreamJRXML = new ByteArrayInputStream(reportType.getReportTemplateJRXML().getBytes("UTF-8"));
            JasperDesign jasperDesign = JRXmlLoader.load(inputStreamJRXML); 
            JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params);

            for(ExportType exportType : ExportType.values())
            {
            	reportType.setReportExport(exportType);
            	modelReport.generateReport(reportType, jasperPrint, FINAL_EXPORT_REPORT_3);	
            }          
            
            subResult.computeStatus();
            session.getTransaction().commit();
        } catch (Exception ex) {
        	if (session != null && session.getTransaction().isActive()) {
        		session.getTransaction().rollback();
        	}

            LOGGER.error("Couldn't export jasper report.", ex);
        	throw ex;
        } finally {
        	if (session != null) {
        		session.close();
        	}
        }

    }

    @Test
    public void test005ModifyReport() throws Exception {
    	
        Session session = null;
    
        LOGGER.debug("Modify Test report. MODEL REPORT ..... ");
        
        try {
            session = sessionFactory.openSession();
            session.beginTransaction();
               
            Task task = taskManager.createTaskInstance(BasicReportTest.class.getName()+".test005ModifyReport");
            OperationResult result = task.getResult();
            ReportType reportType = modelReport.getReportType(REPORT_OID_002, task, result);
            result.computeStatus();
            
            reportType.setReportExport(ExportType.CSV);
            
            OperationResult subResult = result.createSubresult("MODIFY - MODEL REPORT");
            modelReport.modifyReportType(REPORT_OID_002, reportType, task, subResult);
            subResult.computeStatus();
            
            subResult = result.createSubresult("READ REPORT RECORD");
            reportType = modelReport.getReportType(REPORT_OID_002, task, subResult);
            subResult.computeStatus();
            
            AssertJUnit.assertEquals(ExportType.CSV, reportType.getReportExport());
            
            session.getTransaction().commit();
        } catch (Exception ex) {
        	if (session != null && session.getTransaction().isActive()) {
        		session.getTransaction().rollback();
        	}

            LOGGER.error("Couldn't modify report type.", ex);
        	throw ex;
        } finally {
        	if (session != null) {
        		session.close();
        	}
        }

    }

    @Test
    public void test006DeleteReport() throws Exception
    {	
        Session session = null;
        
        LOGGER.debug("Delete Test report. MODEL REPORT ..... ");
        
        try {
            session = sessionFactory.openSession();
            session.beginTransaction();
               
            Task task = taskManager.createTaskInstance(BasicReportTest.class.getName()+".test006DeleteReport");
            OperationResult result = task.getResult();
        
            modelReport.deleteReportType(REPORT_OID_002, task, result);
            result.computeStatus();
            
            OperationResult subResult = result.createSubresult("READ REPORT RECORD");
            ReportType reportType = modelReport.getReportType(REPORT_OID_002, task, subResult);
            subResult.computeStatus();
            
            AssertJUnit.assertNull("Report is not gone", reportType);
            
            session.getTransaction().commit();
        } catch (Exception ex) {
        	if (session != null && session.getTransaction().isActive()) {
        		session.getTransaction().rollback();
        	}

            LOGGER.error("Couldn't delete report type.", ex);
        	throw ex;
        } finally {
        	if (session != null) {
        		session.close();
        	}
        }

    }

}
