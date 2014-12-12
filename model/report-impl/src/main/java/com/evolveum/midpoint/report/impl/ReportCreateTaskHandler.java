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

package com.evolveum.midpoint.report.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JRTemplate;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.JExcelApiExporter;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRRtfExporter;
import net.sf.jasperreports.engine.export.JRXhtmlExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.JRXlsExporterParameter;
import net.sf.jasperreports.engine.export.oasis.JROdsExporter;
import net.sf.jasperreports.engine.export.oasis.JROdtExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRPptxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.xml.JRXmlTemplateLoader;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.hibernate.SessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.common.expression.ExpressionFactory;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.report.api.ReportManager;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SubreportType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;


/**
 * @author lazyman
 */
@Component
public class ReportCreateTaskHandler implements TaskHandler, ApplicationContextAware  {

	public static final String REPORT_CREATE_TASK_URI = "http://midpoint.evolveum.com/xml/ns/public/report/create/handler-1";

    private static final Trace LOGGER = TraceManager.getTrace(ReportCreateTaskHandler.class);
    
    
    private static final String CLASS_NAME_WITH_DOT = ReportCreateTaskHandler.class
			.getName() + ".";
    
    private static final String CREATE_REPORT = CLASS_NAME_WITH_DOT + "createReport";
    
    private static final String CREATE_REPORT_OUTPUT_TYPE = CLASS_NAME_WITH_DOT + "createReportOutputType";
    
    private static String PARAMETER_REPORT_OID = "reportOid";
    private static String PARAMETER_OPERATION_RESULT = "operationResult";
    private static String PARAMETER_TEMPLATE_STYLES = "baseTemplateStyles";
    
    

    @Autowired
    private TaskManager taskManager;
    
    @Autowired
    private ModelService modelService;
    
    @Autowired
	private PrismContext prismContext;

    @Autowired
    private ReportManager reportManager;
    
    @Autowired
	private SessionFactory sessionFactory;
    
    @Autowired(required=true)
    private ExpressionFactory expressionFactory;
    
    @Autowired(required=true)
    private ObjectResolver objectResolver;
    
    @Autowired(required=true)
    private MidpointFunctions midpointFunctions;
    
    @Autowired(required=true)
    private AuditService auditService;
    
    private ApplicationContext context;

    
    @PostConstruct
    private void initialize() {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Registering with taskManager as a handler for " + REPORT_CREATE_TASK_URI);
        }
        taskManager.registerHandler(REPORT_CREATE_TASK_URI, this);
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }
    
    private Map<String, Object> getSubreportParameters(SubreportType subreportType, OperationResult subResult)
    {
    	Map<String, Object> reportParams = new HashMap<String, Object>();
    	try
		{
			ReportType reportType = ReportUtils.getReport(subreportType.getReportRef().getOid(), subResult, modelService);
			
			PrismSchema reportSchema = ReportUtils.getParametersSchema(reportType, prismContext);
    		PrismContainer<Containerable> parameterConfiguration = ReportUtils.getParametersContainer(reportType, reportSchema);    		

    		Map<String, Object> parameters = prepareReportParameters(reportType, subResult);
    		reportParams.putAll(parameters);
		   	
		   	JasperReport jasperReport = ReportUtils.getJasperReport(reportType, parameterConfiguration, reportSchema);
		   	reportParams.put(subreportType.getName(), jasperReport);
       	 		
       		Map<String, Object> subReportParams = processSubreportParameters(reportType, subResult);
       		reportParams.putAll(subReportParams);
       		
       	 		
		} catch (Exception ex) {
				LOGGER.error("Error read subreport parameter {} :", ex);
		}
		 
		return reportParams;
    }
	
   
    
    public Map<String, Object> getReportParameters(ReportType reportType, PrismContext prismContext, OperationResult parentResult) throws Exception
    {
    	Map<String, Object> parameters = new HashMap<String, Object>();
    	try
    	{
    			
			
    	} catch (Exception ex) {
    		throw ex;
    	}
    	
    
    	return parameters;
    }
    @Override
    public TaskRunResult run(Task task) {
        //here a jasper magic should be done which creates PDF file (or something else) in midpoint directory
        //also as a result ReportOutputType should be created and stored to DB.
    	
    	LOGGER.trace("ReportCreateTaskHandler.run starting");
    	
    	OperationResult opResult = new OperationResult(OperationConstants.CREATE_REPORT_FILE);
		opResult.setStatus(OperationResultStatus.IN_PROGRESS);
		
		TaskRunResult runResult = new TaskRunResult();
		runResult.setOperationResult(opResult);
		
    	recordProgress(task, 0, opResult);
    	long progress = task.getProgress();
    	
		Map<String, Object> params = new HashMap<String, Object>();
    	JasperReport jasperReport;
    	try
    	{  
    		OperationResult subResult = opResult.createSubresult(CREATE_REPORT);
    		
    		ReportType reportType = ReportUtils.getReport(task.getObjectOid(), subResult, modelService);
    		
    		PrismSchema reportSchema = ReportUtils.getParametersSchema(reportType, prismContext);
    		PrismContainer<Containerable> parameterConfiguration = ReportUtils.getParametersContainer(reportType, reportSchema);    		

    		// Compile template
    		jasperReport = ReportUtils.getJasperReport(reportType, parameterConfiguration, reportSchema);
    		LOGGER.trace("compile jasper design, create jasper report : {}", jasperReport);
    		
    		
    		Map<String, Object> parameters = prepareReportParameters(reportType, opResult); 
//    				ReportUtils.getReportParameters(modelService, taskManager, prismContext, reportType, parameterConfiguration, reportSchema, subResult);
    		params.putAll(parameters);
    		LOGGER.trace("create report params : {}", parameters);
    		
    		OperationResult subreportResult = opResult.createSubresult("get report subreport");
    		Map<String, Object> subreportParameters = processSubreportParameters(reportType, subreportResult);
    		subreportResult.computeStatus();
    		
    		//params.put("subreportParameters", subreportParameters);   		
    		params.putAll(subreportParameters);
    		ReportFunctions reportFunctions = new ReportFunctions(prismContext, modelService, taskManager, auditService);
    		params.put(MidPointQueryExecutorFactory.PARAMETER_REPORT_FUNCTIONS, reportFunctions);
    		
    		LOGGER.trace("All Report parameters : {}", params);
    		JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params);
    		LOGGER.trace("fill report : {}", jasperPrint);

    		String reportFilePath = generateReport(reportType, jasperPrint);
    		LOGGER.trace("generate report : {}", reportFilePath);

    		saveReportOutputType(reportFilePath, reportType, task, subResult);
    		LOGGER.trace("create report output type : {}", reportFilePath);
    
    		subResult.computeStatus();
    			
    	} catch (Exception ex) {
    		LOGGER.error("CreateReport: {}", ex.getMessage(), ex);
    		opResult.recordFatalError(ex.getMessage(), ex);
    		runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
    		runResult.setProgress(progress);
    		return runResult;
    	}
    	
    	opResult.computeStatus();
		// This "run" is finished. But the task goes on ...
		runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
		runResult.setProgress(progress);
		LOGGER.trace("CreateReportTaskHandler.run stopping");
    	return runResult;
    }
   
    private Map<String, Object> prepareReportParameters(ReportType reportType, OperationResult parentResult){
    	Map<String, Object> params = new HashMap<String, Object>();
		if (reportType.getTemplateStyle() != null)
		{	 
			byte[] reportTemplateStyleBase64 = reportType.getTemplateStyle();
			byte[] reportTemplateStyle = Base64.decodeBase64(reportTemplateStyleBase64);
			try{
				LOGGER.trace("Style template string {}", new String(reportTemplateStyle));
		    	InputStream inputStreamJRTX = new ByteArrayInputStream(reportTemplateStyle);
	    		JRTemplate templateStyle = JRXmlTemplateLoader.load(inputStreamJRTX);
				params.put(PARAMETER_TEMPLATE_STYLES, templateStyle);
				LOGGER.trace("Style template parameter {}", templateStyle);
				
			} catch (Exception ex) {
				LOGGER.error("Error create style template parameter {}", ex.getMessage());
				throw new SystemException(ex);
			}
			
		 } 
		
		// for our special datasource
		params.put(PARAMETER_REPORT_OID, reportType.getOid());
		params.put(PARAMETER_OPERATION_RESULT, parentResult);
		params.put(MidPointQueryExecutorFactory.PARAMETER_MIDPOINT_CONNECTION, modelService);
		params.put(MidPointQueryExecutorFactory.PARAMETER_PRISM_CONTEXT, prismContext);
		params.put(MidPointQueryExecutorFactory.PARAMETER_TASK_MANAGER, taskManager);
		params.put(MidPointQueryExecutorFactory.PARAMETER_EXPRESSION_FACTORY, expressionFactory);
		params.put(MidPointQueryExecutorFactory.PARAMETER_AUDIT_SERVICE, auditService);
		
		return params;
    }
    
    private Map<String, Object> processSubreportParameters(ReportType reportType, OperationResult subreportResult){
    	Map<String, Object> subreportParameters = new HashMap<String, Object>();
    	for(SubreportType subreport : reportType.getSubreport())
		{
			Map<String, Object> subreportParam = getSubreportParameters(subreport, subreportResult);
			LOGGER.trace("create subreport params : {}", subreportParam);
			subreportParameters.putAll(subreportParam);
				
		}
    	return subreportParameters;
    }
    
    @Override
    public Long heartbeat(Task task) {
        return null;
    }

    @Override
    public void refreshStatus(Task task) {
    }

    @Override
    public List<String> getCategoryNames() {
        return null;
    }
    
    @Override
    public String getCategoryName(Task task) {
    	return TaskCategory.REPORT;
    }
        
   

    // generate report - export
    private String generateReport(ReportType reportType, JasperPrint jasperPrint) throws JRException
    {
    	String output = ReportUtils.getReportOutputFilePath(reportType);
    	switch (reportType.getExport())
        {
        	case PDF : pdf(jasperPrint, output);
          		break;
          	case CSV : {
          			csv(jasperPrint, output);
          			//pdf(jasperPrint, output.substring(0, output.lastIndexOf(".")) + ".pdf");
          	}
          	
          		break;
          	case XML : xml(jasperPrint, output);
          		break;
          	case XML_EMBED : xmlEmbed(jasperPrint, output);
          		break;
          	case HTML :	html(jasperPrint, output);
          		break;
          	case RTF :	rtf(jasperPrint, output);
          		break;
          	case XLS :	xls(jasperPrint, output);
  				break;
          	case ODT : 	odt(jasperPrint, output);
  				break;
          	case ODS : 	ods(jasperPrint, output);
          		break;
          	case DOCX : docx(jasperPrint, output);
          		break;
          	case XLSX :	xlsx(jasperPrint, output);
  				break;
          	case PPTX : pptx(jasperPrint, output);
  				break;
          	case XHTML : xhtml(jasperPrint, output);
  				break;
          	case JXL : jxl(jasperPrint, output);
          		break; 	
			default:
				break;
        }
    	return output;
    }
  
    //export report
    private void pdf(JasperPrint jasperPrint, String output) throws JRException
    {
    	JasperExportManager.exportReportToPdfFile(jasperPrint, output);
    }
    
    private static void csv(JasperPrint jasperPrint, String output) throws JRException
    {
		File destFile = new File(output);
		
		JRCsvExporter exporter = new JRCsvExporter();
		
		exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
		exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, destFile.toString());
		
		exporter.exportReport();
    }
    
    private void xml(JasperPrint jasperPrint, String output) throws JRException
    {
    	JasperExportManager.exportReportToXmlFile(jasperPrint, output, false);
    }
    
    private void xmlEmbed(JasperPrint jasperPrint, String output) throws JRException
    {
    	JasperExportManager.exportReportToXmlFile(jasperPrint, output, true);
    }
    
    private void html(JasperPrint jasperPrint, String output) throws JRException
    {
    	JasperExportManager.exportReportToHtmlFile(jasperPrint, output);
    }
    
    private void rtf(JasperPrint jasperPrint, String output) throws JRException
    {
		File destFile = new File(output);
		
		JRRtfExporter exporter = new JRRtfExporter();
		
		exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
		exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, destFile.toString());
		
		exporter.exportReport();
    }
    
    private void xls(JasperPrint jasperPrint, String output) throws JRException
    {
		File destFile = new File(output);
		
		JRXlsExporter exporter = new JRXlsExporter();
		
		exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
		exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, destFile.toString());
		exporter.setParameter(JRXlsExporterParameter.IS_ONE_PAGE_PER_SHEET, Boolean.FALSE);
		
		exporter.exportReport();
    }
    
    private void odt(JasperPrint jasperPrint, String output) throws JRException
    {
		File destFile = new File(output);
		
		JROdtExporter exporter = new JROdtExporter();
		
		exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
		exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, destFile.toString());
		
		exporter.exportReport();
    }
    
    private void ods(JasperPrint jasperPrint, String output) throws JRException
    {
		File destFile = new File(output);
		
		JROdsExporter exporter = new JROdsExporter();
		
		exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
		exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, destFile.toString());
		exporter.setParameter(JRXlsExporterParameter.IS_ONE_PAGE_PER_SHEET, Boolean.TRUE);
		
		exporter.exportReport();
    }
    
    private void docx(JasperPrint jasperPrint, String output) throws JRException
    {
		File destFile = new File(output);
		
		JRDocxExporter exporter = new JRDocxExporter();
		
		exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
		exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, destFile.toString());
		
		exporter.exportReport();
    }
    
    private void xlsx(JasperPrint jasperPrint, String output) throws JRException
    {
		File destFile = new File(output);
		
		JRXlsxExporter exporter = new JRXlsxExporter();
		
		exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
		exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, destFile.toString());
		exporter.setParameter(JRXlsExporterParameter.IS_ONE_PAGE_PER_SHEET, Boolean.FALSE);
		
		exporter.exportReport();
    }
    
    private void pptx(JasperPrint jasperPrint, String output) throws JRException
    {
		File destFile = new File(output);
		
		JRPptxExporter exporter = new JRPptxExporter();
		
		exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
		exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, destFile.toString());

		exporter.exportReport();
    }
    
    private void xhtml(JasperPrint jasperPrint, String output) throws JRException
    {
		File destFile = new File(output);
		
		JRXhtmlExporter exporter = new JRXhtmlExporter();
		
		exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
		exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, destFile.toString());
		
		exporter.exportReport();

    }
    
    private void jxl(JasperPrint jasperPrint, String output) throws JRException
    {
		File destFile = new File(output);

		JExcelApiExporter exporter = new JExcelApiExporter();

		exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
		exporter.setParameter(JRExporterParameter.OUTPUT_FILE_NAME, destFile.toString());
		exporter.setParameter(JRXlsExporterParameter.IS_ONE_PAGE_PER_SHEET, Boolean.TRUE);

		exporter.exportReport();
    }

   

    private void recordProgress(Task task, long progress, OperationResult opResult) {
        try {
            task.setProgressImmediate(progress, opResult);
        } catch (ObjectNotFoundException e) {             // these exceptions are of so little probability and harmless, so we just log them and do not report higher
            LoggingUtils.logException(LOGGER, "Couldn't record progress to task {}, probably because the task does not exist anymore", e, task);
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Couldn't record progress to task {}, due to unexpected schema exception", e, task);
        }
    }
    
    
    private void saveReportOutputType(String filePath, ReportType reportType, Task task, OperationResult parentResult) throws Exception
    {
    
    	String fileName = FilenameUtils.getBaseName(filePath);
    	String reportOutputName = fileName + " - " + reportType.getExport().value() ;
    	
    	ReportOutputType reportOutputType = new ReportOutputType();
    	prismContext.adopt(reportOutputType);
    	
    	reportOutputType.setFilePath(filePath);
    	reportOutputType.setReportRef(MiscSchemaUtil.createObjectReference(reportType.getOid(), ReportType.COMPLEX_TYPE));
    	reportOutputType.setName(new PolyStringType(reportOutputName));
    	reportOutputType.setDescription(reportType.getDescription() + " - " + reportType.getExport().value());
    	
   		ObjectDelta<ReportOutputType> objectDelta = null;
   		Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
   		OperationResult subResult = null;
   		
   		objectDelta = ObjectDelta.createAddDelta((PrismObject<ReportOutputType>) reportOutputType.asPrismObject());
   		deltas.add(objectDelta);
   		subResult = parentResult.createSubresult(CREATE_REPORT_OUTPUT_TYPE);
			      		
    	modelService.executeChanges(deltas, null, task, subResult);
    	
    	subResult.computeStatus();    	
   }


}

