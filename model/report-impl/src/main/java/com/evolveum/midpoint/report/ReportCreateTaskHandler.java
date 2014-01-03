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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
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
import net.sf.jasperreports.engine.xml.JRXmlLoader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportParameterConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

/**
 * @author lazyman
 */
@Component
public class ReportCreateTaskHandler implements TaskHandler {

    public static final String REPORT_CREATE_TASK_URI = "http://midpoint.evolveum.com/xml/ns/public/report/create/handler-1";

    private static final Trace LOGGER = TraceManager.getTrace(ReportCreateTaskHandler.class);
    
    
    private static final String CLASS_NAME_WITH_DOT = ReportCreateTaskHandler.class
			.getName() + ".";
    
    private static final String CREATE_DATASOURCE = CLASS_NAME_WITH_DOT + "createDatasource";
    
    private static final String CREATE_REPORT_OUTPUT_TYPE = CLASS_NAME_WITH_DOT + "createReportOutputType";
    private static final String MODIFY_REPORT_OUTPUT_TYPE = CLASS_NAME_WITH_DOT + "modifyReportOutputType";
    private static final String SEARCH_REPORT_OUTPUT_TYPE = CLASS_NAME_WITH_DOT + "searchReportOutputType";
    

    @Autowired
    private TaskManager taskManager;
    
    @Autowired
    private ModelService modelService;
    
    @Autowired
	private PrismContext prismContext;

    @Autowired
    private ReportManager reportManager;

    @PostConstruct
    private void initialize() {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Registering with taskManager as a handler for " + REPORT_CREATE_TASK_URI);
        }
        taskManager.registerHandler(REPORT_CREATE_TASK_URI, this);
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
		
		String reportOid = task.getObjectOid();
		opResult.addContext("reportOid", reportOid);

		if (reportOid == null) {
			throw new IllegalArgumentException("Report OID is missing in task extension");
		}

        recordProgress(task, 0, opResult);
        long progress = task.getProgress();
        
        PrismObject<ReportType> report = null;
        try {
        	LOGGER.trace("get report : {}", reportOid);
    		report = modelService.getObject(ReportType.class, reportOid, null, task, opResult); 
		} catch (ObjectNotFoundException ex) {
			LOGGER.error("Report does not exist: {}", ex.getMessage(), ex);
			opResult.recordFatalError("Report does not exist: " + ex.getMessage(), ex);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			runResult.setProgress(progress);
			return runResult;
		} catch (Exception ex) {
			LOGGER.error("CreateReport: {}", ex.getMessage(), ex);
			opResult.recordFatalError("Report: " + ex.getMessage(), ex);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			runResult.setProgress(progress);
			return runResult;
		}
        
        ReportType  reportType = report.asObjectable();
        
    	Map<String, Object> params = new HashMap<String, Object>();
    	JasperDesign jasperDesign;
    	try
    	{    
    		OperationResult subResult = opResult.createSubresult(CREATE_DATASOURCE);
    		
    		LOGGER.trace("create report datasource : {}", reportOid);
    		DataSourceReport reportDataSource = new DataSourceReport(reportType, subResult, prismContext, modelService);
    		
    		params.putAll(getReportParams(reportType, opResult));
    		params.put(JRParameter.REPORT_DATA_SOURCE, reportDataSource);
    		
    		LOGGER.trace("create report params : {}", params);
    		if (reportType.getReportTemplate() == null || reportType.getReportTemplate().getAny() == null)
            {
           	 	jasperDesign = ReportUtils.createJasperDesign(reportType);
           	 	LOGGER.trace("create jasper design : {}", jasperDesign);
            }
            else
            {
           	 	String reportTemplate = DOMUtil.serializeDOMToString((Node)reportType.getReportTemplate().getAny());
           	 	InputStream inputStreamJRXML = new ByteArrayInputStream(reportTemplate.getBytes());
           	 	jasperDesign = JRXmlLoader.load(inputStreamJRXML);
           	 	LOGGER.trace("load jasper design : {}", jasperDesign);
            }
    		
            // Compile template
    		JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
    		LOGGER.trace("compile jasper design, create jasper report : {}", jasperReport);
    		
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
    	}
    	
    	opResult.computeStatus();
		// This "run" is finished. But the task goes on ...
		runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
		runResult.setProgress(progress);
		LOGGER.trace("CreateReportTaskHandler.run stopping");
    	return runResult;
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
        
    private Map<String, Object> getReportParams(ReportType reportType, OperationResult parentResult)
	{
	 	Map<String, Object> params = new HashMap<String, Object>();
	 	
	 	OperationResult subResult = parentResult.createSubresult("get report parameters");
	  	
	  	for(ReportParameterConfigurationType parameterRepo : reportType.getReportParameter())
		{
    		params.put(parameterRepo.getNameParameter(), parameterRepo.getValueParameter());			
    	}
	  	
	    subResult.computeStatus();	
	 
	    return params;
	}
    
    // generate report - export
    private String generateReport(ReportType reportType, JasperPrint jasperPrint) throws JRException
    {
    	String output = ReportUtils.getReportOutputFilePath(reportType);
    	switch (reportType.getReportExport())
        {
        	case PDF : pdf(jasperPrint, output);
          		break;
          	case CSV : csv(jasperPrint, output);
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
    
    
    private void saveReportOutputType(String reportFilePath, ReportType reportType, Task task, OperationResult parentResult) throws Exception
    {
    	
    	String reportOutputName = reportType.getName().getOrig() + " - " + reportType.getReportExport().value();
	
    	ReportOutputType reportOutputType = new ReportOutputType();
    	prismContext.adopt(reportOutputType);
    	reportOutputType.setReportFilePath(reportFilePath);
    	reportOutputType.setReportRef(MiscSchemaUtil.createObjectReference(reportType.getOid(), SchemaConstants.C_REPORT_TYPE));
    	reportOutputType.setName(new PolyStringType(reportOutputName));
    	reportOutputType.setDescription(reportType.getDescription() + " - " + reportType.getReportExport().value());
    	
   		ObjectQuery query = ObjectQueryUtil.createNameQuery(PrismTestUtil.createPolyString(reportOutputName), prismContext);
   		List<PrismObject<ReportOutputType>> reportOutputList = modelService.searchObjects(ReportOutputType.class, query, null, task, parentResult);
    	
   		ObjectDelta<ReportOutputType> objectDelta = null;
   		Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
   		OperationResult subResult = null;
   		if (reportOutputList.isEmpty()) {
   			objectDelta = ObjectDelta.createAddDelta((PrismObject<ReportOutputType>) reportOutputType.asPrismObject());
   			deltas.add(objectDelta);
   			subResult = parentResult.createSubresult(CREATE_REPORT_OUTPUT_TYPE);
		} else {
			subResult = parentResult.createSubresult(SEARCH_REPORT_OUTPUT_TYPE);
			
			PrismObject<ReportOutputType> reportOutputTypeOld = modelService.getObject(ReportOutputType.class, reportOutputList.get(0).getOid(), null, task, subResult);
			subResult.computeStatus();
				
			deltas.add(reportOutputTypeOld.diff(reportOutputType.asPrismObject()));
		
    		subResult = parentResult.createSubresult(MODIFY_REPORT_OUTPUT_TYPE);
		}
    		      		
    	modelService.executeChanges(deltas, null, task, subResult);
    	
    	subResult.computeStatus();    	
   }

}

