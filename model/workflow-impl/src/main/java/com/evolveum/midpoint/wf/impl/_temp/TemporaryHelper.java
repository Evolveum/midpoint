/*
 * Copyright (c) 2010-2019 Evolveum
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
package com.evolveum.midpoint.wf.impl._temp;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.wf.util.PerformerCommentsFormatter;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;
import static java.util.Collections.emptySet;

/**
 * Handles low-level task operations.
 *
 * @author mederly
 */

@Component
public class TemporaryHelper {

    @Autowired private PrismContext prismContext;
    @Autowired private ProvisioningService provisioningService;
    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private TaskManager taskManager;
    @Autowired private WorkflowManager workflowManager;

	private static final Trace LOGGER = TraceManager.getTrace(TemporaryHelper.class);

    public String getCaseOid(Task task) {
        throw new UnsupportedOperationException("TODO");        // TODO-WF
    }

    public boolean hasModelContext(Task task) {
        return task.getModelOperationContext() != null;
    }

    public boolean hasModelContext(CaseType aCase) {
        return aCase.getModelContext() != null;
    }

    public ModelContext getModelContext(Task task, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
        LensContextType modelContextType = task.getModelOperationContext();
        if (modelContextType == null) {
            return null;
        }
        return LensContext.fromLensContextType(modelContextType, prismContext, provisioningService, task, result);
    }

    public ModelContext getModelContext(CaseType aCase, Task task, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
        LensContextType modelContextType = aCase.getModelContext();
        if (modelContextType == null) {
            return null;
        }
        return LensContext.fromLensContextType(modelContextType, prismContext, provisioningService, task, result);
    }

    public void storeModelContext(Task task, ModelContext context, boolean reduced) throws SchemaException {
        LensContextType modelContext = context != null ? ((LensContext) context).toLensContextType(reduced) : null;
        storeModelContext(task, modelContext);
    }

    public void storeModelContext(Task task, LensContextType context) throws SchemaException {
        task.setModelOperationContext(context);
    }

    // Will be replaced by something else ... like pre-computing approvers when creating the execution task, maybe.
    public Collection<ObjectReferenceType> getApprovedByFromCaseTree(Task task, OperationResult result) throws SchemaException {
        // we use a OID-keyed map to (1) keep not only the OID, but whole reference, but (2) eliminate uncertainty in comparing references
        Map<String,ObjectReferenceType> approvers = new HashMap<>();

        // TODO-WF
//        List<Task> tasks = task.listSubtasksDeeply(result);
//        tasks.add(task);
//        for (Task aTask : tasks) {
//            List<ObjectReferenceType> approvedBy = getApprovedBy(WfContextUtil.getWorkflowContext(aTask.getTaskPrismObject()));
//            approvedBy.forEach(ort -> approvers.put(ort.getOid(), ort));
//        }
        return CloneUtil.cloneCollectionMembers(approvers.values());            // to ensure these are parent-less
    }

    // Will be replaced by something else ... like pre-computing approvers' comments when creating the execution task, maybe.
    public Collection<String> getApproverCommentsFromTaskTree(Task task, OperationResult result) throws SchemaException {
    	Task opTask = taskManager.createTaskInstance();
        Collection<String> rv = new HashSet<>();
        PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(result);
        PerformerCommentsFormattingType formatting = systemConfiguration != null &&
                systemConfiguration.asObjectable().getWorkflowConfiguration() != null ?
                systemConfiguration.asObjectable().getWorkflowConfiguration().getApproverCommentsFormatting() : null;
        PerformerCommentsFormatter formatter = workflowManager.createPerformerCommentsFormatter(formatting);
        //TODO-WF
        //        List<Task> tasks = task.listSubtasksDeeply(result);
//        tasks.add(task);
//        for (Task aTask : tasks) {
//            rv.addAll(getApproverComments(WfContextUtil.getWorkflowContext(aTask.getTaskPrismObject()), formatter, opTask, result));
//        }
        return rv;
    }

    @NotNull
    private static List<ObjectReferenceType> getApprovedBy(CaseType aCase) {
        return aCase == null ? Collections.emptyList() : aCase.getEvent().stream()
                .flatMap(MiscUtil.instancesOf(WorkItemCompletionEventType.class))
                .filter(e -> ApprovalUtils.isApproved(e.getOutput()) && e.getInitiatorRef() != null)
                .map(e -> e.getInitiatorRef())
                .collect(Collectors.toList());
    }

    @NotNull
    private static Collection<String> getApproverComments(CaseType aCase, PerformerCommentsFormatter formatter,
		    Task opTask, OperationResult result) {
        if (aCase == null) {
            return emptySet();
        }
        return aCase.getEvent().stream()
                .flatMap(MiscUtil.instancesOf(WorkItemCompletionEventType.class))
		        .filter(e -> ApprovalUtils.isApproved(e.getOutput()) && e.getInitiatorRef() != null)
		        .map(e -> formatter.formatComment(e, opTask, result))
		        .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
