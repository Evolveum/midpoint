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

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.base.JRBasePen;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignField;
import net.sf.jasperreports.engine.design.JRDesignFrame;
import net.sf.jasperreports.engine.design.JRDesignImage;
import net.sf.jasperreports.engine.design.JRDesignLine;
import net.sf.jasperreports.engine.design.JRDesignParameter;
import net.sf.jasperreports.engine.design.JRDesignReportTemplate;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JRDesignStyle;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.EvaluationTimeEnum;
import net.sf.jasperreports.engine.type.HorizontalAlignEnum;
import net.sf.jasperreports.engine.type.ModeEnum;
import net.sf.jasperreports.engine.type.OrientationEnum;
import net.sf.jasperreports.engine.type.PositionTypeEnum;
import net.sf.jasperreports.engine.type.SplitTypeEnum;
import net.sf.jasperreports.engine.type.VerticalAlignEnum;
import net.sf.jasperreports.engine.type.WhenNoDataTypeEnum;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.LoggingConfigurationManager;
import com.evolveum.midpoint.common.ProfilingConfigurationManager;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.model.controller.SystemConfigurationHandler;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportFieldConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportParameterConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemObjectsType;


/**
 * @author lazyman, garbika
 */
@Component
public class ReportManagerImpl implements ReportManager, ChangeHook {
	
    public static final String HOOK_URI = "http://midpoint.evolveum.com/model/report-hook-1";
    
    private static final Trace LOGGER = TraceManager.getTrace(ReportManagerImpl.class);
    
    private static final String DOT_CLASS = ReportManagerImpl.class + ".";
    
	@Autowired
    private HookRegistry hookRegistry;
	
	public ReportManagerImpl() {
		LOGGER.info("construotooooooooooooooooooooooooooooooooooo");
	}
   
    @PostConstruct
    public void init() {   	
        hookRegistry.registerChangeHook(HOOK_URI, this);
        throw new RuntimeException("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
    }
    
    /**
     * Creates and starts task with proper handler, also adds necessary information to task
     * (like ReportType reference and so on).
     *
     * @param report
     * @param task
     * @param parentResult describes report which has to be created
     */
    @Override
    public void runReport(PrismObject<ReportType> object, Task task, OperationResult parentResult) {
    	/*
    	Map<String, Object> params = new HashMap<String, Object>();
    	try
    	{
    		OperationResult subResult = parentResult.createSubresult(RUN_REPORT);
    		ReportType reportType = object.asObjectable();
    		
    		subResult = parentResult.createSubresult("Load Datasource");
            
    		DataSourceReport reportDataSource = new DataSourceReport(object, subResult);
    		
    		params.putAll(getReportParams(object, parentResult));
    		params.put(JRParameter.REPORT_DATA_SOURCE, reportDataSource);
    		
    		// Loading template
    		InputStream inputStreamJRXML = new ByteArrayInputStream(reportType.getReportTemplateJRXML().getBytes("UTF-8"));
    		JasperDesign jasperDesign = JRXmlLoader.load(inputStreamJRXML); 

    		JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
    		JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params);

    		generateReport(reportType, jasperPrint);
     
    		subResult.computeStatus();
    	}
    	catch (Exception ex)
    	{
    		ModelUtils.recordFatalError(parentResult, ex);
    	}*/
    }
    /**
     * Transforms change:
     * 1/ ReportOutputType DELETE to MODIFY some attribute to mark it for deletion.
     * 2/ ReportType ADD and MODIFY should compute jasper design and styles if necessary
     *
     * @param context
     * @param task
     * @param result
     * @return
     * @throws UnsupportedEncodingException 
     */
    @Override
    public HookOperationMode invoke(ModelContext context, Task task, OperationResult parentResult)  {
        if (1==1)throw new RuntimeException("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
    	 ModelState state = context.getState();
         if (state != ModelState.FINAL) {
             if (LOGGER.isTraceEnabled()) {
                 LOGGER.trace("report manager called in state = " + state + ", exiting.");
             }
             return HookOperationMode.FOREGROUND;
         } else {
             if (LOGGER.isTraceEnabled()) {
                 LOGGER.trace("report manager called in state = " + state + ", proceeding.");
             }
         }

         boolean relatesToReport = false;
         boolean isDeletion = false; 
         PrismObject object = null;
         for (Object o : context.getProjectionContexts()) {     
             boolean deletion = false;
             object = ((ModelElementContext) o).getObjectNew();
             if (object == null) {
                 deletion = true;
                 object = ((ModelElementContext) o).getObjectOld();
             }
             if (object == null) {
                 LOGGER.warn("Probably invalid projection context: both old and new objects are null");  
             } else if (object.getCompileTimeClass().isAssignableFrom(ReportType.class)) {
                 relatesToReport = true;
                 isDeletion = deletion;
             }
         }

         if (LOGGER.isTraceEnabled()) {
             LOGGER.trace("change relates to report: " + relatesToReport + ", is deletion: " + isDeletion);
         }

         if (!relatesToReport) {
             LOGGER.trace("invoke() EXITING: Changes not related to report");
             return HookOperationMode.FOREGROUND;
         }

         OperationResult result = parentResult.createSubresult(DOT_CLASS + "invoke");
         try {
             if (isDeletion) {
                 LoggingConfigurationManager.resetCurrentlyUsedVersion();        
                 LOGGER.trace("invoke() EXITING because operation is DELETION");
                 return HookOperationMode.FOREGROUND;
             }
             
             ReportType reportType = (ReportType) object.asObjectable();
             String reportTemplateJRXML =reportType.getReportTemplateJRXML();
             JasperDesign jasperDesign = null;
             if (reportTemplateJRXML == null)
             {
            	 jasperDesign = createJasperDesign(reportType);
             }
             else
             {
            	 // Loading template
            	 InputStream inputStreamJRXML = new ByteArrayInputStream(reportTemplateJRXML.getBytes("UTF-8"));
            	 jasperDesign = JRXmlLoader.load(inputStreamJRXML);
             }
             // Compile template
             JasperCompileManager.compileReport(jasperDesign);
            
             result.computeStatus();

         }
         catch (UnsupportedEncodingException ex)
         {
        	 String message = "Unsupported encoding - jrxml file: " + ex.getMessage();
             LoggingUtils.logException(LOGGER, message, ex);
             result.recordFatalError(message, ex);
         }
         catch (JRException ex) {
             String message = "Cannot load or compile jasper report: " + ex.getMessage();
             LoggingUtils.logException(LOGGER, message, ex);
             result.recordFatalError(message, ex);
         } 
        

        return HookOperationMode.FOREGROUND;
    }
    
    private Class getClassType(QName clazz)
    {
    	return java.lang.String.class;
    }
    
    private JasperDesign createJasperDesign(ReportType reportType) throws JRException
	{
		//JasperDesign
		JasperDesign jasperDesign = new JasperDesign();
		jasperDesign.setName("reportDataSource");
		
		switch (reportType.getReportOrientation())
		{
			case LANDSCAPE :
			default:
			{
				jasperDesign.setOrientation(OrientationEnum.LANDSCAPE);
				jasperDesign.setPageWidth(842);
				jasperDesign.setPageHeight(595);
				jasperDesign.setColumnWidth(802);
			}
			break;
			case PORTRAIT :
			{
				jasperDesign.setOrientation(OrientationEnum.LANDSCAPE);
				jasperDesign.setPageWidth(595);
				jasperDesign.setPageHeight(842);
				jasperDesign.setColumnWidth(555);
			}
			break;
		}
		jasperDesign.setColumnSpacing(0);
		jasperDesign.setWhenNoDataType(WhenNoDataTypeEnum.ALL_SECTIONS_NO_DETAIL);
		jasperDesign.setLeftMargin(20);
		jasperDesign.setRightMargin(20);
		jasperDesign.setTopMargin(20);
		jasperDesign.setBottomMargin(20);
		
	
		//Templates
		JRDesignReportTemplate templateStyle = new JRDesignReportTemplate(new JRDesignExpression("$P{BaseTemplateStyles}"));
		jasperDesign.addTemplate(templateStyle);
		
		//Parameters
		//two parameters are there every time - template styles and logo image and will be excluded
		List<ReportParameterConfigurationType> parameters = new ArrayList<ReportParameterConfigurationType>();
		parameters.addAll(reportType.getReportParameters());
		
		for(ReportParameterConfigurationType parameterRepo : parameters)
		{
			JRDesignParameter parameter = new JRDesignParameter();
			parameter.setName(parameterRepo.getNameParameter());
			parameter.setValueClass(getClassType(parameterRepo.getClassTypeParameter()));
			jasperDesign.addParameter(parameter);
			/*if(parameterRepo.getNameParameter().equals("LOGO_PATH") || parameterRepo.getNameParameter().equals("BaseTemplateStyles")) 
			{
				parameters.remove(parameterRepo);
			}
			*/
		}
				
		//Fields
		for(ReportFieldConfigurationType fieldRepo : reportType.getReportFields())
		{
			JRDesignField field = new JRDesignField();
			field.setName(fieldRepo.getNameReportField());
			field.setValueClass(getClassType(fieldRepo.getClassTypeField()));	
			jasperDesign.addField(field);
		}

		//Background
		JRDesignBand bandBackground = new JRDesignBand();
		bandBackground.setHeight(30);
		bandBackground.setSplitType(SplitTypeEnum.STRETCH);
		jasperDesign.setBackground(bandBackground);
		
		//Title
		//band size depends on the number of parameters
		//two pre-defined parameters were excluded
		int secondColumn = Math.round(jasperDesign.getColumnWidth()/2 - 1);
		JRDesignBand titleBand = new JRDesignBand();
		int height = 70 + Math.max(40, parameters.size()*20);
		titleBand.setHeight(height);
		titleBand.setSplitType(SplitTypeEnum.STRETCH);
		
		JRDesignFrame frame = new JRDesignFrame();
		frame.setX(0);
		frame.setY(0);
		frame.setWidth(jasperDesign.getColumnWidth() - 2);
		frame.setHeight(70);
		frame.setStyleNameReference("Title");
		titleBand.addElement(frame);
		
		JRDesignStaticText staticText = new JRDesignStaticText();
		staticText.setX(10);
		staticText.setY(15);
		staticText.setWidth(266);
		staticText.setHeight(40);
		staticText.setStyleNameReference("Title");
		staticText.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
		staticText.setText("DataSource Report");
		frame.addElement(staticText);
		
		JRDesignImage image = new JRDesignImage(new JRDesignStyle().getDefaultStyleProvider());
		image.setX(589);
		image.setY(15);
		image.setWidth(203);
		image.setHeight(40);
		image.setStyleNameReference("Title");
		image.setExpression(new JRDesignExpression("$P{LOGO_PATH}"));
		frame.addElement(image);
		
		 
		staticText = new JRDesignStaticText();
		staticText.setX(secondColumn);
		staticText.setY(70);
		staticText.setWidth(150);
		staticText.setHeight(20);
		staticText.setStyleNameReference("Page header");
		staticText.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
		staticText.setText("Report generated on:");
		titleBand.addElement(staticText);
		
		JRDesignTextField textField = new JRDesignTextField();
		textField.setX(secondColumn + 150);
		textField.setY(70);
		textField.setWidth(250);
		textField.setHeight(20);
		textField.setHorizontalAlignment(HorizontalAlignEnum.RIGHT);
		textField.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
		textField.setStyleNameReference("Page header");
		textField.setBold(false);
		textField.setExpression(new JRDesignExpression("new java.util.Date()"));
		titleBand.addElement(textField);
		
		staticText = new JRDesignStaticText();
		staticText.setX(secondColumn);
		staticText.setY(90);
		staticText.setWidth(150);
		staticText.setHeight(20);
		staticText.setStyleNameReference("Page header");
		staticText.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
		staticText.setText("Number of records:");
		titleBand.addElement(staticText);
		
		textField = new JRDesignTextField();
		textField.setX(secondColumn + 150);
		textField.setY(90);
		textField.setWidth(250);
		textField.setHeight(20);
		textField.setHorizontalAlignment(HorizontalAlignEnum.RIGHT);
		textField.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
		textField.setStyleNameReference("Page header");
		textField.setBold(false);
		textField.setExpression(new JRDesignExpression("$V{REPORT_COUNT}"));
		titleBand.addElement(textField);
		
		parameters.remove(0);
		parameters.remove(0);
		int y = 70;
		for(ReportParameterConfigurationType parameterRepo : parameters)
		{
			staticText = new JRDesignStaticText();
			staticText.setX(2);
			staticText.setY(y);
			staticText.setWidth(150);
			staticText.setHeight(20);
			staticText.setStyleNameReference("Page header");
			staticText.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
			staticText.setText(parameterRepo.getDescriptionParameter() + ":");
			titleBand.addElement(staticText);
			
			textField = new JRDesignTextField();
			textField.setX(160);
			textField.setY(y);
			textField.setWidth(240);
			textField.setHeight(20);
			textField.setHorizontalAlignment(HorizontalAlignEnum.RIGHT);
			textField.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
			textField.setStyleNameReference("Page header");
			textField.setBold(false);
			textField.setExpression(new JRDesignExpression("$P{"+ parameterRepo.getNameParameter() + "}"));
			titleBand.addElement(textField);
			
			y = y + 20;
		}
		
		jasperDesign.setTitle(titleBand);
	
		//Page header
		JRDesignBand pageHeaderBand = new JRDesignBand();
		pageHeaderBand.setSplitType(SplitTypeEnum.STRETCH);
		jasperDesign.setPageHeader(pageHeaderBand);
		
		//Column header
		JRDesignBand columnHeaderBand = new JRDesignBand();
		columnHeaderBand.setSplitType(SplitTypeEnum.STRETCH);
		columnHeaderBand.setHeight(24);

		frame = new JRDesignFrame();
		frame.setX(0);
		frame.setY(5);
		//frame.setWidth(799);
		frame.setWidth(jasperDesign.getColumnWidth() - 2);
		frame.setHeight(19);
		frame.setStyleNameReference("Column header");
		//frame.setRemoveLineWhenBlank(true);
		
		int x = 0;
		int width = 0;
		for(ReportFieldConfigurationType fieldRepo : reportType.getReportFields())
		{
			staticText = new JRDesignStaticText();
			staticText.setX(x);
			staticText.setY(0);
			width =  Math.round((float) ((frame.getWidth()/100) * fieldRepo.getWidthField()));
			staticText.setWidth(width);
			staticText.setHeight(18);
			staticText.setStyleNameReference("Column header");
			staticText.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
			staticText.setText(fieldRepo.getNameHeaderField());
			frame.addElement(staticText);
			x = x + width;
		}
		
		columnHeaderBand.addElement(frame);
		jasperDesign.setColumnHeader(columnHeaderBand);
		
		//Detail
		JRDesignBand detailBand = new JRDesignBand();
		detailBand.setHeight(20);
		detailBand.setSplitType(SplitTypeEnum.PREVENT);
		
		frame = new JRDesignFrame();
		frame.setX(0);
		frame.setY(1);
		//frame.setWidth(799);
		frame.setWidth(jasperDesign.getColumnWidth() - 2);
		frame.setHeight(19);
		frame.setStyleNameReference("Detail");
		
		x = 0;
		width = 0;
		for(ReportFieldConfigurationType fieldRepo : reportType.getReportFields())
		{
			textField = new JRDesignTextField();
			textField.setX(x);
			textField.setY(1);
			width = Math.round((float) ((frame.getWidth()/100) * fieldRepo.getWidthField())); 
			textField.setWidth(width);
			textField.setHeight(18);
			textField.setStretchWithOverflow(true);
			textField.setBlankWhenNull(true);
			textField.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
			textField.setStyleNameReference("Detail");
			textField.setExpression(new JRDesignExpression("$F{" + fieldRepo.getNameReportField() + "}"));
			frame.addElement(textField);
			x = x + width;
		}
		
		JRDesignLine line = new JRDesignLine();
		line.setX(0);
		line.setY(3);
		//line.setWidth(800);
		line.setWidth(jasperDesign.getColumnWidth()-2);
		line.setHeight(1);
		line.setPositionType(PositionTypeEnum.FIX_RELATIVE_TO_BOTTOM);
		JRBasePen pen = new JRBasePen(line);
		pen.setLineWidth((float) 0.5);
		pen.setLineColor(Color.decode("#999999"));
		frame.addElement(line);
		
		detailBand.addElement(frame);
		((JRDesignSection)jasperDesign.getDetailSection()).addBand(detailBand);		
		
		//Column footer
		JRDesignBand columnFooterBand = new JRDesignBand();
		columnFooterBand.setHeight(7);
		columnFooterBand.setSplitType(SplitTypeEnum.STRETCH);		
		
		line = new JRDesignLine();
		line.setX(0);
		line.setY(3);
		//line.setWidth(800);
		line.setWidth(jasperDesign.getColumnWidth()-2);
		line.setHeight(1);
		line.setPositionType(PositionTypeEnum.FIX_RELATIVE_TO_BOTTOM);
		pen = new JRBasePen(line);
		pen.setLineWidth((float) 0.5);
		pen.setLineColor(Color.decode("#999999"));
		columnFooterBand.addElement(line);
		jasperDesign.setColumnFooter(columnFooterBand);

		//Page footer
		JRDesignBand pageFooterBand = new JRDesignBand();
		pageFooterBand.setHeight(32);
		pageFooterBand.setSplitType(SplitTypeEnum.STRETCH);
		frame = new JRDesignFrame();
		frame.setX(0);
		frame.setY(1);
		//frame.setWidth(800);
		frame.setWidth(jasperDesign.getColumnWidth() - 2);
		frame.setHeight(24);
		frame.setStyleNameReference("Page footer");
		frame.setMode(ModeEnum.TRANSPARENT);
		
		textField = new JRDesignTextField();
		textField.setX(2);
		textField.setY(1);
		textField.setWidth(197);
		textField.setHeight(20);
		textField.setPattern("EEEEE dd MMMMM yyyy");
		textField.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
		textField.setStyleNameReference("Page footer");
		textField.setExpression(new JRDesignExpression("new java.util.Date()"));
		frame.addElement(textField);
		
		textField = new JRDesignTextField();
		textField.setX(680);
		textField.setY(1);
		textField.setWidth(80);
		textField.setHeight(20);
		textField.setHorizontalAlignment(HorizontalAlignEnum.RIGHT);
		textField.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
		textField.setStyleNameReference("Page footer");
		//textField.setExpression(new JRDesignExpression("''Page '' + $V{PAGE_NUMBER} + '' of''"));
		textField.setExpression(new JRDesignExpression("$V{PAGE_NUMBER}"));
		frame.addElement(textField);
		
		textField = new JRDesignTextField();
		textField.setX(760);
		textField.setY(1);
		textField.setWidth(40);
		textField.setHeight(20);
		textField.setEvaluationTime(EvaluationTimeEnum.REPORT);
		textField.setVerticalAlignment(VerticalAlignEnum.MIDDLE);
		textField.setStyleNameReference("Page footer");
		//textField.setExpression(new JRDesignExpression("'' '' + $V{PAGE_NUMBER}"));
		textField.setExpression(new JRDesignExpression("$V{PAGE_NUMBER}"));
		frame.addElement(textField);
		
		pageFooterBand.addElement(frame);
		
		jasperDesign.setPageFooter(pageFooterBand);

		return jasperDesign;
	}

    @Override
    public void invokeOnException(ModelContext context, Throwable throwable, Task task, OperationResult result) {
    	
    }
    /*
    @Override
    public List<PrismObject<ReportType>> searchReports(ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) 
    		throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
			SecurityViolationException {
    	return modelService.searchObjects(ReportType.class, query, options, null, parentResult);
    }

    @Override
    public int countReports(ObjectQuery query, OperationResult parentResult) throws SchemaException 
    {
    	//LOGGER.trace("begin::countReport()");
        int count = 0;
        OperationResult result = parentResult.createSubresult(COUNT_REPORT);
        try {
        	count = modelService.countObjects(ReportType.class, query, null, null, result);
            result.recordSuccess();
        } 
        catch (Exception ex) 
        {
            result.recordFatalError("Couldn't count objects.", ex);
        }
        //LOGGER.trace("end::countReport()");
        return count;
    }

  */
    @Override
    public void cleanupReports(CleanupPolicyType cleanupPolicy, OperationResult parentResult) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
