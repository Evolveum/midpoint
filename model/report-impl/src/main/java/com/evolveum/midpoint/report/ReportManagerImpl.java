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
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
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
import org.w3c.dom.Node;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.LessFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportFieldConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportParameterConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportTemplateType;
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
    
    

	@Autowired
    private HookRegistry hookRegistry;

	@Autowired
    private TaskManager taskManager;

	@PostConstruct
    public void init() {   	
        hookRegistry.registerChangeHook(HOOK_URI, this);
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
    public void cleanupReports(CleanupPolicyType cleanupPolicy, OperationResult parentResult) {
    }
    
   
}
