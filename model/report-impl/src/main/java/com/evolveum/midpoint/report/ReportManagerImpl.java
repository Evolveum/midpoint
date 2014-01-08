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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.LessFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.report.api.ReportManager;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ThreadStopActionType;


/**
 * @author lazyman, garbika
 */
@Component
public class ReportManagerImpl implements ReportManager, ChangeHook {
	
    public static final String HOOK_URI = "http://midpoint.evolveum.com/model/report-hook-1";
    
    private static final Trace LOGGER = TraceManager.getTrace(ReportManagerImpl.class);
    
    private static final String CLASS_NAME_WITH_DOT = ReportManagerImpl.class + ".";
    private static final String CLEANUP_REPORT_OUTPUTS = CLASS_NAME_WITH_DOT + "cleanupReportOutputs";
    
	@Autowired
    private HookRegistry hookRegistry;

	@Autowired
    private TaskManager taskManager;
	
	@Autowired
	private PrismContext prismContext;
	
	@Autowired
	private ModelService modelService;
	
	
	@PostConstruct
    public void init() {   	
        hookRegistry.registerChangeHook(HOOK_URI, this);
    }
	
	public PrismContext getPrismContext() {
        return prismContext;
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
        task.setHandlerUri(ReportCreateTaskHandler.REPORT_CREATE_TASK_URI);
        task.setObjectRef(object.getOid(), ReportType.COMPLEX_TYPE);

        task.setThreadStopAction(ThreadStopActionType.CLOSE);
    	task.makeSingle();
    	
    	taskManager.switchToBackground(task, parentResult);
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
         PrismObject<?> object = null;
         for (Object o : context.getProjectionContexts()) {     
             boolean deletion = false;
             object = ((ModelElementContext<?>) o).getObjectNew();
             if (object == null) {
                 deletion = true;
                 object = ((ModelElementContext<?>) o).getObjectOld();
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
         
         if (isDeletion) {
             LOGGER.trace("invoke() EXITING because operation is DELETION");
             return HookOperationMode.FOREGROUND;
         }

         OperationResult result = parentResult.createSubresult(CLASS_NAME_WITH_DOT + "invoke");
         try {
             ReportType reportType = (ReportType) object.asObjectable();
             JasperDesign jasperDesign = null;
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
             
             //result.computeStatus();
             result.recordSuccessIfUnknown();

         }
         catch (JRException ex) {
             String message = "Cannot load or compile jasper report: " + ex.getMessage();
             LOGGER.error(message);
             result.recordFatalError(message, ex);
         } 
        

        return HookOperationMode.FOREGROUND;
    }
    
    
    @Override
    public void invokeOnException(ModelContext context, Throwable throwable, Task task, OperationResult result) {
    	
    }
  
    @Override
    public void cleanupReports(CleanupPolicyType cleanupPolicy, OperationResult parentResult) {//throws ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
    	OperationResult result = parentResult.createSubresult(CLEANUP_REPORT_OUTPUTS);

        if (cleanupPolicy.getMaxAge() == null) {
            return;
        }

        Duration duration = cleanupPolicy.getMaxAge();
        if (duration.getSign() > 0) {
            duration = duration.negate();
        }
        Date deleteReportOutputsTo = new Date();
        duration.addTo(deleteReportOutputsTo);

        LOGGER.info("Starting cleanup for report outputs deleting up to {} (duration '{}').",
                new Object[]{deleteReportOutputsTo, duration});

        XMLGregorianCalendar timeXml = XmlTypeConverter.createXMLGregorianCalendar(deleteReportOutputsTo.getTime());

        List<PrismObject<ReportOutputType>> obsoleteReportOutputs = new ArrayList<PrismObject<ReportOutputType>>();
        try {
            ObjectQuery obsoleteReportOutputsQuery = ObjectQuery.createObjectQuery(AndFilter.createAnd(
                    LessFilter.createLess(ReportOutputType.F_METADATA, ReportOutputType.class, getPrismContext(), timeXml, true),
                    EqualsFilter.createEqual(TaskType.F_PARENT, TaskType.class, getPrismContext(), null)));

            obsoleteReportOutputs = modelService.searchObjects(ReportOutputType.class, obsoleteReportOutputsQuery, null, null, result);
        } catch (Exception e) {
            //throw new SchemaException("Couldn't get the list of obsolete report outputs: " + e.getMessage(), e);
        }

        LOGGER.debug("Found {} report output tree(s) to be cleaned up", obsoleteReportOutputs.size());

        boolean interrupted = false;
        int deleted = 0;
        int problems = 0;
        int bigProblems = 0;
        
        for (PrismObject<ReportOutputType> reportOutputPrism : obsoleteReportOutputs){
        	ReportOutputType reportOutput = reportOutputPrism.asObjectable();
        	
        	LOGGER.trace("Removing report output {} along with {} file.", reportOutput.getName().getOrig(), reportOutput.getReportFilePath());
        	boolean problem = false;
        	try {
                    deleteReportOutput(reportOutput.getOid(), result);
                } catch (SchemaException e) {
                    LoggingUtils.logException(LOGGER, "Couldn't delete obsolete report output {} due to schema exception", e, reportOutput);
                    problem = true;
                } catch (ObjectNotFoundException e) {
                    LoggingUtils.logException(LOGGER, "Couldn't delete obsolete report output {} due to object not found exception", e, reportOutput);
                    problem = true;
                } catch (RuntimeException e) {
                    LoggingUtils.logException(LOGGER, "Couldn't delete obsolete report output {} due to a runtime exception", e, reportOutput);
                    problem = true;
                } catch (Exception e) {
                	LoggingUtils.logException(LOGGER, "Couldn't delete obsolete report output {} due to a exception", e, reportOutput);
                    problem = true;
                }

                if (problem) {
                    problems++;
                } else {
                    deleted++;
            }
        }
        result.computeStatusIfUnknown();

        LOGGER.info("Report cleanup procedure " + (interrupted ? "was interrupted" : "finished") + ". Successfully deleted {} report outputs; there were problems with deleting {} report ouptuts.", deleted, problems);
        String suffix = interrupted ? " Interrupted." : "";
        if (problems == 0) {
            parentResult.createSubresult(CLEANUP_REPORT_OUTPUTS + ".statistics").recordStatus(OperationResultStatus.SUCCESS, "Successfully deleted " + deleted + " report output(s)." + suffix);
        } else {
            parentResult.createSubresult(CLEANUP_REPORT_OUTPUTS + ".statistics").recordPartialError("Successfully deleted " + deleted + " report output(s), "
                    + "there was problems with deleting " + problems + " report outputs.");
        }
    }
    
    private ReportOutputType getReportOutput(String reportOutputOid, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
  
		OperationResult result = parentResult.createMinorSubresult(CLASS_NAME_WITH_DOT + "getReportOutput"); 
		result.addParam(OperationResult.PARAM_OID, reportOutputOid);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, CLASS_NAME_WITH_DOT);
		
		ReportOutputType reportOutput;
		try {
			reportOutput = modelService.getObject(ReportOutputType.class, reportOutputOid, null, null, result).asObjectable();
        } catch (ObjectNotFoundException e) {
			result.recordFatalError("Report output not found", e);
			throw e;
		} catch (SchemaException e) {
			result.recordFatalError("Report output schema error: "+e.getMessage(), e);
			throw e;
		} catch (SecurityViolationException e) {
			result.recordFatalError("Report output security violation error: "+e.getMessage(), e);
			throw e;
		} catch (CommunicationException e) {
			result.recordFatalError("Report output communication error: "+e.getMessage(), e);
			throw e;
		} catch (ConfigurationException e) {
			result.recordFatalError("Report output configuration error: "+e.getMessage(), e);
			throw e;
		}
		result.recordSuccess();
		return reportOutput;
	}
    
    private void deleteReportOutput(String oid, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
        OperationResult result = parentResult.createSubresult(CLASS_NAME_WITH_DOT + "deleteReportOutput");
        result.addParam("oid", oid);
        try {
            ReportOutputType reportOutput = getReportOutput(oid, result);
            
            //modelService.executeChanges(deltas, options, task, parentResult)repositoryService.deleteObject(TaskType.class, oid, result);
            result.recordSuccessIfUnknown();
        } catch (ObjectNotFoundException e) {
            result.recordFatalError("Cannot delete the report output because it does not exist.", e);
            throw e;
        } catch (SchemaException e) {
            result.recordFatalError("Cannot delete the report output because of schema exception.", e);
            throw e;
        } catch (SecurityViolationException e) {
        	result.recordFatalError("Cannot delete the report output because of security violation exception: ", e);
        	throw e;
        } catch (CommunicationException e) {
        	result.recordFatalError("Cannot delete the report output because of communication exception: ", e);
        	throw e;
        } catch (ConfigurationException e) {
        	result.recordFatalError("Cannot delete the report output because of configuration exception: ", e);
        	throw e;
        } catch (RuntimeException e) {
        	result.recordFatalError("Cannot delete the report output because of a runtime exception.", e);
            throw e;
        }
    }
}
