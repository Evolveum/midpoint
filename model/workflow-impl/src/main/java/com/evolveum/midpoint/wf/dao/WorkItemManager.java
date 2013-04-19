package com.evolveum.midpoint.wf.dao;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.WfConfiguration;
import com.evolveum.midpoint.wf.WorkflowServiceImpl;
import com.evolveum.midpoint.wf.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.activiti.ActivitiEngineDataHelper;
import com.evolveum.midpoint.wf.api.WorkflowService;
import com.evolveum.midpoint.wf.processes.CommonProcessVariableNames;
import org.activiti.engine.FormService;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mederly
 */

@Component
public class WorkItemManager {

    private static final transient Trace LOGGER = TraceManager.getTrace(WorkItemManager.class);

    @Autowired
    private ActivitiEngine activitiEngine;

    private static final String DOT_CLASS = WorkflowServiceImpl.class.getName() + ".";
    private static final String DOT_INTERFACE = WorkflowService.class.getName() + ".";

    private static final String OPERATION_APPROVE_OR_REJECT_WORK_ITEM = DOT_INTERFACE + "approveOrRejectWorkItem";


    // todo error reporting
    public void approveOrRejectWorkItemWithDetails(String taskId, PrismObject specific, boolean decision, OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(OPERATION_APPROVE_OR_REJECT_WORK_ITEM);
        result.addParam("taskId", taskId);
        result.addParam("decision", decision);
        result.addParam("task-specific data", specific);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Approving/rejecting work item " + taskId);
            LOGGER.trace("Decision: " + decision);
            LOGGER.trace("WorkItem form object (task-specific) = " + (specific != null ? specific.debugDump() : "(none)"));
        }

        FormService formService = activitiEngine.getFormService();
        Map<String,String> propertiesToSubmit = new HashMap<String,String>();
        propertiesToSubmit.put(CommonProcessVariableNames.FORM_FIELD_DECISION, Boolean.toString(decision));

        if (specific != null) {
            TaskFormData data = activitiEngine.getFormService().getTaskFormData(taskId);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("# of form properties: " + data.getFormProperties().size());
            }

            for (FormProperty formProperty : data.getFormProperties()) {

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Processing property " + formProperty.getId() + ":" + formProperty.getName());
                }

                if (formProperty.isWritable()) {

                    Object value;

                    if (!CommonProcessVariableNames.FORM_FIELD_DECISION.equals(formProperty.getId())) {

                        // todo strip [flags] section
                        QName propertyName = new QName(SchemaConstants.NS_WFCF, formProperty.getId());
                        value = specific.getPropertyRealValue(propertyName, Object.class);

                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("Writable property " + formProperty.getId() + " has a value of " + value);
                        }

                        propertiesToSubmit.put(formProperty.getId(), value == null ? "" : value.toString());
                    }
                }
            }
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Submitting " + propertiesToSubmit.size() + " properties");
        }

        formService.submitTaskFormData(taskId, propertiesToSubmit);

        result.recordSuccessIfUnknown();
    }


}
