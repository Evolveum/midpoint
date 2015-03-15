package com.evolveum.midpoint.report.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRTemplate;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.JRXhtmlExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.oasis.JROdsExporter;
import net.sf.jasperreports.engine.export.oasis.JROdtExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRPptxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.xml.JRXmlTemplateLoader;
import net.sf.jasperreports.export.Exporter;
import net.sf.jasperreports.export.ExporterInput;
import net.sf.jasperreports.export.ExporterOutput;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.common.expression.ExpressionFactory;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.report.api.ReportService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.schema.util.ReportTypeUtil;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SubreportType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

@Component
public class ReportCreateTaskHandler implements TaskHandler{

	public static final String REPORT_CREATE_TASK_URI = "http://midpoint.evolveum.com/xml/ns/public/report/create/handler-3";
	private static final Trace LOGGER = TraceManager.getTrace(ReportCreateTaskHandler.class);
    
    private static String PARAMETER_TEMPLATE_STYLES = "baseTemplateStyles";
    private static String PARAMETER_REPORT_OID = "reportOid";
    private static String PARAMETER_OPERATION_RESULT = "operationResult";
    
    private static String MIDPOINT_HOME = System.getProperty("midpoint.home"); 
    private static String EXPORT_DIR = MIDPOINT_HOME + "export/";
    
    @Autowired
    private TaskManager taskManager;
    
    @Autowired
    private ModelService modelService;
    
    @Autowired
	private PrismContext prismContext;
//
//	
//	
//	
//	 @Autowired(required=true)
//	    private ExpressionFactory expressionFactory;
//	 
//	 @Autowired(required=true)
//	    private AuditService auditService;
    
    @Autowired(required = true)
    private ReportService reportService;
    
    @Autowired(required = true)
	private ObjectResolver objectResolver;

	 
	 @PostConstruct
	    private void initialize() {
	        if (LOGGER.isTraceEnabled()) {
	            LOGGER.trace("Registering with taskManager as a handler for " + REPORT_CREATE_TASK_URI);
	        }
	        taskManager.registerHandler(REPORT_CREATE_TASK_URI, this);
	    }

	 
	 
	@Override
	public TaskRunResult run(Task task) {
		// TODO Auto-generated method stub
		OperationResult parentResult = task.getResult();
		OperationResult result = parentResult.createSubresult(ReportCreateTaskHandler.class.getSimpleName() + ".run");
		
		TaskRunResult runResult = new TaskRunResult();
		runResult.setOperationResult(result);
		
    	recordProgress(task, 0, result);
    	long progress = task.getProgress();
		
		try {
			ReportType parentReport = objectResolver.resolve(task.getObjectRef(), ReportType.class, null, "resolving report", result);
			
			JasperReport jasperReport = ReportTypeUtil.loadJasperReport(parentReport);
			
			LOGGER.trace("compile jasper design, create jasper report : {}", jasperReport);
    		Map<String, Object> parameters = completeReport(parentReport, result);
			
    		LOGGER.trace("All Report parameters : {}", parameters);
    		JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters);
    		LOGGER.trace("fill report : {}", jasperPrint);

    		String reportFilePath = generateReport(parentReport, jasperPrint);
    		LOGGER.trace("generate report : {}", reportFilePath);

    		saveReportOutputType(reportFilePath, parentReport, task, result);
    		LOGGER.trace("create report output type : {}", reportFilePath);
    
    		result.computeStatus();
    		
    		
		} catch (Exception ex) {
			LOGGER.error("CreateReport: {}", ex.getMessage(), ex);
			result.recordFatalError(ex.getMessage(), ex);
    		runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
    		runResult.setProgress(progress);
    		return runResult;
		}
		
		// This "run" is finished. But the task goes on ...
		runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
		runResult.setProgress(progress);
		LOGGER.trace("CreateReportTaskHandler.run stopping");
    	return runResult;
	}
	
	private Map<String, Object> completeReport(ReportType parentReport, OperationResult result) throws SchemaException, ObjectNotFoundException{
		return completeReport(parentReport, null, null, result);
	}
	
	private Map<String, Object> completeReport(ReportType parentReport, JasperReport subReport, String subReportName, OperationResult result) throws SchemaException, ObjectNotFoundException{
		Map<String, Object> params =  new HashMap<String, Object>();
		
		if (subReport != null && StringUtils.isNotBlank(subReportName)){
			params.put(subReportName, subReport);
		}
		
		Map<String, Object> parameters = prepareReportParameters(parentReport, result); 
		params.putAll(parameters);
		LOGGER.trace("create report params : {}", parameters);
		
		Map<String, Object> subreportParameters = processSubreportParameters(parentReport, result);
		params.putAll(subreportParameters);
		return params;
	}
	
	

//	private JasperReport loadJasperReport(ReportType reportType) throws SchemaException{
//		
//			if (reportType.getTemplate() == null) {
//				throw new IllegalStateException("Could not create report. No jasper template defined.");
//			}
//			try	 {
//		    	 	byte[] reportTemplate = Base64.decodeBase64(reportType.getTemplate());
//		    	 	
//		    	 	InputStream inputStreamJRXML = new ByteArrayInputStream(reportTemplate);
//		    	 	JasperDesign jasperDesign = JRXmlLoader.load(inputStreamJRXML);
//		    	 	LOGGER.trace("load jasper design : {}", jasperDesign);
//				 
//				 if (reportType.getTemplateStyle() != null){
//					JRDesignReportTemplate templateStyle = new JRDesignReportTemplate(new JRDesignExpression("$P{" + PARAMETER_TEMPLATE_STYLES + "}"));
//					jasperDesign.addTemplate(templateStyle);
//					JRDesignParameter parameter = new JRDesignParameter();
//					parameter.setName(PARAMETER_TEMPLATE_STYLES);
//					parameter.setValueClass(JRTemplate.class);
//					parameter.setForPrompting(false);
//					jasperDesign.addParameter(parameter);
//				 } 
//				 JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
//				 return jasperReport;
//			 } catch (JRException ex){ 
//				 LOGGER.error("Couldn't create jasper report design {}", ex.getMessage());
//				 throw new SchemaException(ex.getMessage(), ex.getCause());
//			 }
//			 
//			 
//	}
	
	
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
			params.put(ReportService.PARAMETER_REPORT_SERVICE, reportService);
//			params.put(MidPointQueryExecutorFactory.PARAMETER_MIDPOINT_CONNECTION, modelService);
//			params.put(MidPointQueryExecutorFactory.PARAMETER_PRISM_CONTEXT, prismContext);
//			params.put(MidPointQueryExecutorFactory.PARAMETER_TASK_MANAGER, taskManager);
//			params.put(MidPointQueryExecutorFactory.PARAMETER_EXPRESSION_FACTORY, expressionFactory);
//			params.put(MidPointQueryExecutorFactory.PARAMETER_AUDIT_SERVICE, auditService);
//			ReportFunctions reportFunctions = new ReportFunctions(prismContext, modelService, taskManager, auditService);
//    		params.put(MidPointQueryExecutorFactory.PARAMETER_REPORT_FUNCTIONS, reportFunctions);
			
			return params;
	    }
	    
	    private Map<String, Object> processSubreportParameters(ReportType reportType, OperationResult subreportResult) throws SchemaException, ObjectNotFoundException{
	    	Map<String, Object> subreportParameters = new HashMap<String, Object>();
	    	for(SubreportType subreport : reportType.getSubreport())
			{
				Map<String, Object> subreportParam = getSubreportParameters(subreport, subreportResult);
				LOGGER.trace("create subreport params : {}", subreportParam);
				subreportParameters.putAll(subreportParam);
					
			}
	    	return subreportParameters;
	    }
	    
	private Map<String, Object> getSubreportParameters(SubreportType subreportType, OperationResult subResult)
			throws SchemaException, ObjectNotFoundException {
		Map<String, Object> reportParams = new HashMap<String, Object>();
		ReportType reportType = objectResolver.resolve(subreportType.getReportRef(), ReportType.class, null,
				"resolve subreport", subResult);

		Map<String, Object> parameters = prepareReportParameters(reportType, subResult);
		reportParams.putAll(parameters);

		JasperReport jasperReport = ReportTypeUtil.loadJasperReport(reportType);
		reportParams.put(subreportType.getName(), jasperReport);

		Map<String, Object> subReportParams = processSubreportParameters(reportType, subResult);
		reportParams.putAll(subReportParams);

		return reportParams;
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
	 
	 private String generateReport(ReportType reportType, JasperPrint jasperPrint) throws JRException
	    {
	    	String destinationFileName = getDestinationFileName(reportType);
	    	switch (reportType.getExport())
	        {
	        	case PDF : 
	        		JasperExportManager.exportReportToPdfFile(jasperPrint, destinationFileName);
	        		break;
	        	case XML : 
	        		JasperExportManager.exportReportToXmlFile(jasperPrint, destinationFileName, true);
	        		break;
	        	case XML_EMBED :
	        		JasperExportManager.exportReportToXmlFile(jasperPrint, destinationFileName, true);
	        		break;
	        	case HTML :	
	        		JasperExportManager.exportReportToHtmlFile(jasperPrint, destinationFileName);
	        		break;
              	case CSV : 
              	case RTF :
	          	case XLS :
	          	case ODT :
	          	case ODS :
	          	case DOCX :
	          	case XLSX :
	          	case PPTX :
	          	case XHTML :
	          	case JXL : 		          		
	          		ExporterInput input = new SimpleExporterInput(jasperPrint);
	          		ExporterOutput output = new SimpleOutputStreamExporterOutput(destinationFileName);
	          		
	          		Exporter exporter = createExporter(reportType.getExport());
	          		if (exporter == null){
	          			break;
	          		}
	          		exporter.setExporterInput(input);
	          		exporter.setExporterOutput(output);
	          		exporter.exportReport();
	          		break;
				default:
					break;
	        }
	    	return destinationFileName;
	    }
	  
	private Exporter createExporter(ExportType type) {
		switch (type) {
			case CSV:
			case RTF:
			case XLS:
				return new JRXlsExporter();
			case ODT:
				return new JROdtExporter();
			case ODS:
				return new JROdsExporter();
			case DOCX:
				return new JRDocxExporter();
			case XLSX:
				return new JRXlsxExporter();
			case PPTX:
				return new JRPptxExporter();
			case XHTML:
				return new JRXhtmlExporter();
			default:
				return null;
		}
	}

	public static String getDateTime() {
		Date createDate = new Date(System.currentTimeMillis());
		SimpleDateFormat formatDate = new SimpleDateFormat("dd-MM-yyyy hh-mm-ss");
		return formatDate.format(createDate);
	}
	 private static String getDestinationFileName(ReportType reportType){
	        File exportFolder = new File(EXPORT_DIR);
	        if (!exportFolder.exists() || !exportFolder.isDirectory()) {
	            exportFolder.mkdir();
	        }
	    	
	    	String output = EXPORT_DIR +  reportType.getName().getOrig() + " " + getDateTime();
	    	
	    	if (reportType.getExport() == ExportType.XML_EMBED){
	    		return output + "_embed.xml";
	    	}
	    	
	    	return output + "." +reportType.getExport().value();
	    
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
	    	reportOutputType.setExportType(reportType.getExport());
	    	
	   		ObjectDelta<ReportOutputType> objectDelta = null;
	   		Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
	   		OperationResult subResult = null;
	   		
	   		objectDelta = ObjectDelta.createAddDelta((PrismObject<ReportOutputType>) reportOutputType.asPrismObject());
	   		deltas.add(objectDelta);
	   		subResult = parentResult.createSubresult(ReportCreateTaskHandler.class.getName()+ "createRepourtOutput");
				      		
	    	modelService.executeChanges(deltas, null, task, subResult);
	    	
	    	subResult.computeStatus();    	
	   }
	 
	
	@Override
	public Long heartbeat(Task task) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void refreshStatus(Task task) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getCategoryName(Task task) {
		return TaskCategory.REPORT;
	}

	@Override
	public List<String> getCategoryNames() {
		// TODO Auto-generated method stub
		return null;
	}
	
	

}
