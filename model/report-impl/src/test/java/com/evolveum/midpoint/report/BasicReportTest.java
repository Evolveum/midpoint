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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignFrame;
import net.sf.jasperreports.engine.design.JRDesignImage;
import net.sf.jasperreports.engine.design.JRDesignParameter;
import net.sf.jasperreports.engine.design.JRDesignReportTemplate;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignStyle;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.HorizontalAlignEnum;
import net.sf.jasperreports.engine.type.OrientationEnum;
import net.sf.jasperreports.engine.type.SplitTypeEnum;
import net.sf.jasperreports.engine.type.VerticalAlignEnum;
import net.sf.jasperreports.engine.type.WhenNoDataTypeEnum;
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

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.repo.api.RepositoryService;
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
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
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
    
    private static final String FINAL_REPORT_1 = REPORTS_DIR + "/reportDataSourceTestFinal_1.jrprint";
    private static final String FINAL_EXPORT_REPORT_1 = REPORTS_DIR + "/reportDataSourceTestFinal_1";
    
    private static final String REPORT_DATASOURCE_TEST_2 = REPORTS_DIR + "/reportDataSourceTest_2.jrxml";
    private static final String FINAL_JASPER_REPORT_2 = REPORTS_DIR + "/reportDataSourceTestFinal_2.jasper";
    private static final String FINAL_REPORT_2 = REPORTS_DIR + "/reportDataSourceTestFinal_2.jrprint";
    private static final String FINAL_EXPORT_REPORT_2 = REPORTS_DIR + "/reportDataSourceTestFinal_2";
    
    private static final String REPORT_OID_001 = "00000000-3333-3333-0000-100000000001";
    @Autowired
    private ModelService modelService;

    @Autowired
    private PrismContext prismContext;
    
    @Autowired
    private RepositoryService repositoryService;
    
    @Autowired
    private SessionFactory sessionFactory;


    @Test
    public void sampleTest() {
        AssertJUnit.assertNotNull(modelService);
        LOGGER.info("Model service is available.");
    }
    
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
        AssertJUnit.assertNotNull(report);
        session.getTransaction().commit();
        session.close();

        return report;
    }
    
    private static Class<?> getFieldClassType(QName classtType)
    {
   	// TODO compare with ordinal types        
    	return java.lang.String.class;
    }

    
    private static JasperDesign getJasperDesign(ReportType reportType) throws JRException
	{
		//JasperDesign
		JasperDesign jasperDesign = new JasperDesign();
		jasperDesign.setName("reportDataSource");
		jasperDesign.setOrientation(OrientationEnum.getByName(reportType.getReportOrientation().name()));
		jasperDesign.setPageWidth(842);
		jasperDesign.setPageHeight(595);
		jasperDesign.setColumnWidth(802);
		jasperDesign.setColumnSpacing(0);
		jasperDesign.setWhenNoDataType(WhenNoDataTypeEnum.ALL_SECTIONS_NO_DETAIL);
		jasperDesign.setLeftMargin(20);
		jasperDesign.setRightMargin(20);
		jasperDesign.setTopMargin(20);
		jasperDesign.setBottomMargin(20);
		
	
		//Templates
		/*JRDesignExpression exTemplateStyle = new JRDesignExpression();
		exTemplateStyle.addParameterChunk("BaseTemplateStyles");*/
		JRDesignReportTemplate templateStyle = new JRDesignReportTemplate(new JRDesignExpression("$P{BaseTemplateStyles}"));
		jasperDesign.addTemplate(templateStyle);
		
		//Parameters
		JRDesignParameter parameter = new JRDesignParameter();
		parameter.setName("BaseTemplateStyles");
		parameter.setValueClass(java.lang.String.class);
		jasperDesign.addParameter(parameter);

		parameter = new JRDesignParameter();
		parameter.setName("LOGO_PATH");
		parameter.setValueClass(java.lang.String.class);
		jasperDesign.addParameter(parameter);

		parameter = new JRDesignParameter();
		parameter.setName("column");
		parameter.setValueClass(java.lang.String[].class);
		jasperDesign.addParameter(parameter);

		//Fields
		for(ReportFieldConfigurationType fieldRepo : reportType.getReportFields())
		{
			JRDesignField field = new JRDesignField();
			field.setName(fieldRepo.getNameReportField());
			//field.setValueClass(java.lang.String.class);
			field.setValueClass(getFieldClassType(fieldRepo.getClassTypeField()));	
			jasperDesign.addField(field);
		}

		//Background
		JRDesignBand bandBackground = new JRDesignBand();
		bandBackground.setHeight(30);
		bandBackground.setSplitType(SplitTypeEnum.STRETCH);
		jasperDesign.setBackground(bandBackground);
		
		//Title
		JRDesignBand bandTitle = new JRDesignBand();
		bandTitle.setHeight(110);
		bandTitle.setSplitType(SplitTypeEnum.STRETCH);
		
		JRDesignFrame frame = new JRDesignFrame();
		frame.setX(1);
		frame.setY(0);
		frame.setWidth(799);
		frame.setHeight(67);
		frame.setStyleNameReference("Title");
		bandTitle.addElement(frame);
		
		JRDesignStaticText staticText = new JRDesignStaticText();
		staticText.setX(10);
		staticText.setY(13);
		staticText.setWidth(266);
		staticText.setHeight(38);
		staticText.setStyleNameReference("Title");
		staticText.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
		staticText.setText("DataSource Report");
		frame.addElement(staticText);
		
		JRDesignImage image = new JRDesignImage(new JRDesignStyle().getDefaultStyleProvider());
		image.setX(589);
		image.setY(13);
		image.setWidth(203);
		image.setHeight(45);
		image.setStyleNameReference("Title");
		image.setExpression(new JRDesignExpression("$P{LOGO_PATH}"));
		frame.addElement(image);
		
		staticText = new JRDesignStaticText();
		staticText.setX(400);
		staticText.setY(67);
		staticText.setWidth(150);
		staticText.setHeight(20);
		staticText.setStyleNameReference("Page header");
		staticText.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
		staticText.setText("Report generated on:");
		bandTitle.addElement(staticText);
		
		JRDesignTextField textField = new JRDesignTextField();
		textField.setX(550);
		textField.setY(67);
		textField.setWidth(250);
		textField.setHeight(20);
		textField.setHorizontalAlignment(HorizontalAlignEnum.RIGHT);
		textField.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
		textField.setStyleNameReference("Page header");
		textField.setBold(false);
		textField.setExpression(new JRDesignExpression("new java.util.Date()"));
		bandTitle.addElement(textField);
		jasperDesign.setTitle(bandTitle);
		
	/*	
		JRDesignImage image = new JRDesignImage();
		staticText = new JRDesignStaticText();
		staticText.setX(55);
		staticText.setY(0);
		staticText.setWidth(205);
		staticText.setHeight(15);
		staticText.setForecolor(Color.white);
		staticText.setBackcolor(new Color(0x33, 0x33, 0x33));
		staticText.setMode(ModeEnum.OPAQUE);
		staticText.setStyle(boldStyle);
		staticText.setText("Name");
		frame.addElement(staticText);
		JRDesignTextField textField = new JRDesignTextField();
		textField.setBlankWhenNull(true); 
		textField.setX(0);
		textField.setY(10);
		textField.setWidth(515);
		textField.setHeight(30);
		textField.setHorizontalAlignment(HorizontalAlignEnum.CENTER);
		textField.setStyle(normalStyle);
		textField.setFontSize(22);
		textField.setExpression(new JRDesignExpression("$P{ReportTitle}"));
		band.addElement(textField);
		jasperDesign.setTitle(band);
		
		//Page header
		band = new JRDesignBand();
		band.setHeight(20);
		JRDesignFrame frame = new JRDesignFrame();
		frame.setX(0);
		frame.setY(5);
		frame.setWidth(515);
		frame.setHeight(15);
		frame.setForecolor(new Color(0x33, 0x33, 0x33));
		frame.setBackcolor(new Color(0x33, 0x33, 0x33));
		frame.setMode(ModeEnum.OPAQUE);
		band.addElement(frame);
		JRDesignStaticText staticText = new JRDesignStaticText();
		staticText.setX(0);
		staticText.setY(0);
		staticText.setWidth(55);
		staticText.setHeight(15);
		staticText.setForecolor(Color.white);
		staticText.setBackcolor(new Color(0x33, 0x33, 0x33));
		staticText.setMode(ModeEnum.OPAQUE);
		staticText.setHorizontalAlignment(HorizontalAlignEnum.CENTER);
		staticText.setStyle(boldStyle);
		staticText.setText("ID");
		frame.addElement(staticText);
		staticText = new JRDesignStaticText();
		staticText.setX(55);
		staticText.setY(0);
		staticText.setWidth(205);
		staticText.setHeight(15);
		staticText.setForecolor(Color.white);
		staticText.setBackcolor(new Color(0x33, 0x33, 0x33));
		staticText.setMode(ModeEnum.OPAQUE);
		staticText.setStyle(boldStyle);
		staticText.setText("Name");
		frame.addElement(staticText);
		staticText = new JRDesignStaticText();
		staticText.setX(260);
		staticText.setY(0);
		staticText.setWidth(255);
		staticText.setHeight(15);
		staticText.setForecolor(Color.white);
		staticText.setBackcolor(new Color(0x33, 0x33, 0x33));
		staticText.setMode(ModeEnum.OPAQUE);
		staticText.setStyle(boldStyle);
		staticText.setText("Street");
		frame.addElement(staticText);
		jasperDesign.setPageHeader(band);

		//Column header
		band = new JRDesignBand();
		jasperDesign.setColumnHeader(band);

		//Detail
		band = new JRDesignBand();
		band.setHeight(20);
		textField = new JRDesignTextField();
		textField.setX(0);
		textField.setY(4);
		textField.setWidth(50);
		textField.setHeight(15);
		textField.setHorizontalAlignment(HorizontalAlignEnum.RIGHT);
		textField.setStyle(normalStyle);
		textField.setExpression(new JRDesignExpression("$F{Id}"));
		band.addElement(textField);
		textField = new JRDesignTextField();
		textField.setStretchWithOverflow(true);
		textField.setX(55);
		textField.setY(4);
		textField.setWidth(200);
		textField.setHeight(15);
		textField.setPositionType(PositionTypeEnum.FLOAT);
		textField.setStyle(normalStyle);
		textField.setExpression(new JRDesignExpression("$F{FirstName} + \" \" + $F{LastName}"));
		band.addElement(textField);
		textField = new JRDesignTextField();
		textField.setStretchWithOverflow(true);
		textField.setX(260);
		textField.setY(4);
		textField.setWidth(255);
		textField.setHeight(15);
		textField.setPositionType(PositionTypeEnum.FLOAT);
		textField.setStyle(normalStyle);
		textField.setExpression(new JRDesignExpression("$F{Street}"));
		band.addElement(textField);
		line = new JRDesignLine();
		line.setX(0);
		line.setY(19);
		line.setWidth(515);
		line.setHeight(0);
		line.setForecolor(new Color(0x80, 0x80, 0x80));
		line.setPositionType(PositionTypeEnum.FLOAT);
		band.addElement(line);
		((JRDesignSection)jasperDesign.getDetailSection()).addBand(band);

		//Column footer
		band = new JRDesignBand();
		jasperDesign.setColumnFooter(band);

		//Page footer
		band = new JRDesignBand();
		jasperDesign.setPageFooter(band);

		//Summary
		band = new JRDesignBand();
		jasperDesign.setSummary(band);
		*/
		return jasperDesign;
	}

    @Test
    public void createTestReport001() throws Exception {
    	
         RReport report = new RReport();
         
         LOGGER.debug("Creating Test report. DATASOURCE ..... ");
         
         report.setOid(REPORT_OID_001);
         
         //description and name
         report.setName(new RPolyString("Test report - Datasource","Test report - Datasource"));
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
         field.setClassTypeField(itemDef.getTypeName());    
         reportFields.add(field);
        
         report.setReportFields(RUtil.toRepo(reportFields, prismContext));
         
         
         //parameters
         List<ReportParameterConfigurationType> reportParameters = new ArrayList<ReportParameterConfigurationType>();
         
         ReportParameterConfigurationType parameter = new ReportParameterConfigurationType();
         parameter.setNameParameter("Logo");
         parameter.setValueParameter(REPORTS_DIR.getPath() + "/logo.jpg");
         parameter.setClassTypeParameter(DOMUtil.XSD_ANY);
         reportParameters.add(parameter);
         
         parameter = new ReportParameterConfigurationType();
         parameter.setNameParameter("BaseTemplateStyles");
         parameter.setValueParameter(STYLE_TEMPLATE_DEFAULT);
         parameter.setClassTypeParameter(DOMUtil.XSD_STRING);
         reportParameters.add(parameter);
         
         report.setReportParameters(RUtil.toRepo(reportParameters, prismContext));
         
         OperationResult result = new OperationResult("CREATE REPORT RECORD");
         repositoryService.addObject(report.toJAXB(prismContext, null).asPrismObject(), null, result);
         
         OperationResult subResult = result.createSubresult("READ REPORT RECORD");
         
         ReportType reportType = repositoryService.getObject(ReportType.class, REPORT_OID_001, null, subResult).asObjectable();
         subResult.computeStatus();
         
         // check reportType
         AssertJUnit.assertNotNull(reportType);
         AssertJUnit.assertEquals("Test report - Datasource", reportType.getName().getOrig());
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
         AssertJUnit.assertEquals("Test report - Datasource", report.getName().getOrig());
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
    
    private String[] getColumn(List<ReportFieldConfigurationType> fieldsRepo)
    {
    	String[] column = new String[fieldsRepo.size()];
    	int i=0;
    	for (ReportFieldConfigurationType fieldRepo : fieldsRepo)
    	{
    		column[i] = fieldRepo.getNameHeaderField();
    		i++;
    	}	
    	return column;
    }
    //export report
    private static void pdf(String input, String output) throws JRException
    {
    	JasperExportManager.exportReportToPdfFile(input, output + ".pdf");
    }
    
    private static void csv(String input, String output) throws JRException
    {
    	JasperExportManager.exportReportToPdfFile(input, output + ".csv");
    }
    
    private static void xml(String input, String output) throws JRException
    {
    	String fileName = JasperExportManager.exportReportToXmlFile(input, false);
    }
    
    private static void xmlEmbed(String input, String output) throws JRException
    {
    	String fileName = JasperExportManager.exportReportToXmlFile(input, true);
    }
    
    private static void html(String input, String output) throws JRException
    {
    	JasperExportManager.exportReportToPdfFile(input, output + ".html");
    }
    
    private static void rtf(String input, String output) throws JRException
    {
    	JasperExportManager.exportReportToPdfFile(input, output + ".rtf");
    }
    
    private static void xls(String input, String output) throws JRException
    {
    	JasperExportManager.exportReportToPdfFile(input, output + ".xls");
    }
    
    private static void odt(String input, String output) throws JRException
    {
    	JasperExportManager.exportReportToPdfFile(input, output + ".odt");
    }
    
    private static void ods(String input, String output) throws JRException
    {
    	JasperExportManager.exportReportToPdfFile(input, output + ".ods");
    }
    
    private static void docx(String input, String output) throws JRException
    {
    	JasperExportManager.exportReportToPdfFile(input, output + ".docx");
    }
    
    private static void xlsx(String input, String output) throws JRException
    {
    	JasperExportManager.exportReportToPdfFile(input, output + ".xlsx");
    }
    
    private static void pptx(String input, String output) throws JRException
    {
    	JasperExportManager.exportReportToPdfFile(input, output + ".pptx");
    }
    
    private static void xhtml(String input, String output) throws JRException
    {
    	JasperExportManager.exportReportToPdfFile(input, output + ".xhtml");
    }
    
    private static void jxl(String input, String output) throws JRException
    {
    	JasperExportManager.exportReportToPdfFile(input, output + ".jxl");
    }
    
    
    // generate report - export
    private static void generateReport(ExportType type, String input, String output) throws JRException
    {
    	  switch (type)
          {
          	case PDF : pdf(input, output);
          		break;
          	case CSV : csv(input, output);
      			break;
          	case XML : xml(input, output);
      			break;
          	case XML_EMBED : xmlEmbed(input, output);
          		break;
          	case HTML : html(input, output);
  				break;
          	case RTF : rtf(input, output);
  				break;
          	case XLS : xls(input, output);
  				break;
          	case ODT : odt(input, output);
  				break;
          	case ODS : ods(input, output);
  				break;
          	case DOCX : docx(input, output);
  				break;
          	case XLSX : xlsx(input, output);
  				break;
          	case PPTX : pptx(input, output);
  				break;
          	case XHTML : xhtml(input, output);
  				break;
          	case JXL : jxl(input, output);
          		break; 	
			default:
				break;
          }
    }
      
    private static Map<String, Object> getReportParams(ReportType reportType)
    {
    	Map<String, Object> params = new HashMap<String, Object>();
    	for(ReportParameterConfigurationType parameterRepo : reportType.getReportParameters())
		{
    		params.put(parameterRepo.getNameParameter(),parameterRepo.getValueParameter());
		}
    	
    	return params;
    }
    @Test
    public void generateTestReport002() throws Exception {
    	
    	generateUsers();
        
    	Map<String, Object> params = new HashMap<String, Object>();
    	
        Session session = null;
        DataSourceReport reportDataSource = null;
        
        LOGGER.debug("Generating Test report. DATASOURCE ..... ");
        
        try {
            
            session = sessionFactory.openSession();
            session.beginTransaction();
            
            OperationResult result = new OperationResult("LOAD REPORT RECORD");
        
            ReportType reportType = repositoryService.getObject(ReportType.class, REPORT_OID_001, null, result).asObjectable();
            result.computeStatus();
            
            OperationResult subResult = new OperationResult("LOAD DATASOURCE");
            reportDataSource = new DataSourceReport(modelService, prismContext, reportType, subResult);
            
            params.putAll(getReportParams(reportType));
            params.put(JRParameter.REPORT_DATA_SOURCE, reportDataSource);
        
            // Loading template
            InputStream inputStreamJRXML = new ByteArrayInputStream(reportType.getReportTemplateJRXML().getBytes("UTF-8"));
            JasperDesign design = JRXmlLoader.load(inputStreamJRXML); 
   
            JasperReport report = JasperCompileManager.compileReport(design);
            JasperFillManager.fillReportToFile(report, FINAL_REPORT_1, params);
            generateReport(reportType.getReportExport(), FINAL_REPORT_1, FINAL_EXPORT_REPORT_1);
         
            
            subResult.computeStatus();
        
            session.getTransaction().commit();
        } catch (Exception ex) {
        	if (session != null && session.getTransaction().isActive()) {
        		session.getTransaction().rollback();
        	}

            LOGGER.error("Couldn't create jasper report.", ex);
        	throw ex;
        } finally {
        	if (session != null) {
        		session.close();
        	}
        }
    }
    
		
    
    @Test
    public void generateTestReportDynamic003() throws Exception {
    	
    	Map<String, Object> params = new HashMap<String, Object>();
    	
        Session session = null;
        DataSourceReport reportDataSource = null;
        
        LOGGER.debug("Generating dybamic Test report. DATASOURCE ..... ");
        
        try {
            session = sessionFactory.openSession();
            session.beginTransaction();
               
            OperationResult result = new OperationResult("LOAD REPORT RECORD");
        
            ReportType reportType = repositoryService.getObject(ReportType.class, REPORT_OID_001, null, result).asObjectable();
            result.computeStatus();
            
            OperationResult subResult = new OperationResult("LOAD DATASOURCE");
            reportDataSource = new DataSourceReport(modelService, prismContext, reportType, subResult);
        
            params.putAll(getReportParams(reportType));
            params.put(JRParameter.REPORT_DATA_SOURCE, reportDataSource);
        
            // Create template
            //JasperDesign design = getJasperDesign(reportType);
            InputStream inputStreamJRXML = new ByteArrayInputStream(reportType.getReportTemplateJRXML().getBytes("UTF-8"));
            JasperDesign design = JRXmlLoader.load(inputStreamJRXML); 
            
            JasperCompileManager.compileReportToFile(design, FINAL_JASPER_REPORT_2);
            JasperCompileManager.writeReportToXmlFile(FINAL_JASPER_REPORT_2);
            
            JasperReport report = JasperCompileManager.compileReport(design);
            JasperFillManager.fillReportToFile(report, FINAL_REPORT_2, params);
            generateReport(reportType.getReportExport(), FINAL_REPORT_2, FINAL_EXPORT_REPORT_2);
          
            
            subResult.computeStatus();
        
            session.getTransaction().commit();
        } catch (Exception ex) {
        	if (session != null && session.getTransaction().isActive()) {
        		session.getTransaction().rollback();
        	}

            LOGGER.error("Couldn't create jasper report.", ex);
        	throw ex;
        } finally {
        	if (session != null) {
        		session.close();
        	}
        }

    }

}
