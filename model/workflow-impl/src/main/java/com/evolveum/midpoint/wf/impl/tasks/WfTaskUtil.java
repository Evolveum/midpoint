/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.wf.impl.tasks;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskBinding;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.WfConfiguration;
import com.evolveum.midpoint.wf.impl.processors.ChangeProcessor;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.PrimaryChangeAspect;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.evolveum.midpoint.prism.util.CloneUtil.cloneListMembers;
import static com.evolveum.midpoint.schema.constants.ObjectTypes.TASK;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.objectReferenceListToPrismReferenceValues;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType.F_WORKFLOW_CONTEXT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WfPrimaryChangeProcessorStateType.F_DELTAS_TO_PROCESS;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WfPrimaryChangeProcessorStateType.F_RESULTING_DELTAS;

/**
 * Handles low-level task operations.
 *
 * @author mederly
 */

@Component
public class WfTaskUtil {

    @Autowired
    private WfConfiguration wfConfiguration;

    @Autowired
    private PrismContext prismContext;

    @Autowired
    private ProvisioningService provisioningService;

	private static final Trace LOGGER = TraceManager.getTrace(WfTaskUtil.class);

    public static final String WAIT_FOR_TASKS_HANDLER_URI = "<<< marker for calling pushWaitForTasksHandlerUri >>>";

    public static WfContextType getWorkflowContextChecked(Task task) {
        if (task == null) {
            throw new IllegalStateException("No task");
        } else if (task.getWorkflowContext() == null) {
            throw new IllegalStateException("No workflow context in " + task);
        } else {
            return task.getWorkflowContext();
        }
    }

	@NotNull
    public PrimaryChangeAspect getPrimaryChangeAspect(Task task, Collection<PrimaryChangeAspect> aspects) {
        WfContextType wfc = getWorkflowContextChecked(task);
        WfProcessorSpecificStateType pss = wfc.getProcessorSpecificState();
        if (!(pss instanceof WfPrimaryChangeProcessorStateType)) {
            throw new IllegalStateException("Expected " + WfPrimaryChangeProcessorStateType.class + " but got " + pss + " in task " + task);
        }
        WfPrimaryChangeProcessorStateType pcps = ((WfPrimaryChangeProcessorStateType) pss);
        String aspectClassName = pcps.getChangeAspect();
        if (aspectClassName == null) {
            throw new IllegalStateException("No wf primary change aspect defined in task " + task);
        }
        for (PrimaryChangeAspect a : aspects) {
            if (aspectClassName.equals(a.getClass().getName())) {
                return a;
            }
        }
        throw new IllegalStateException("Primary change aspect " + aspectClassName + " is not registered.");
    }

	@NotNull
    public ChangeProcessor getChangeProcessor(Task task) {
        String processorClassName = task.getWorkflowContext() != null ? task.getWorkflowContext().getChangeProcessor() : null;
        if (processorClassName == null) {
            throw new IllegalStateException("No change processor defined in task " + task);
        }
        return wfConfiguration.findChangeProcessor(processorClassName);
    }

    public String getProcessId(Task task) {
        if (task.getWorkflowContext() != null) {
            return task.getWorkflowContext().getProcessInstanceId();
        } else {
            return null;
        }
    }

    public boolean hasModelContext(Task task) {
        return task.getModelOperationContext() != null;
    }

    public ModelContext getModelContext(Task task, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException {
        LensContextType modelContextType = task.getModelOperationContext();
        if (modelContextType == null) {
            return null;
        }
        return LensContext.fromLensContextType(modelContextType, prismContext, provisioningService, result);
    }

    public void storeModelContext(Task task, ModelContext context) throws SchemaException {
        LensContextType modelContext = context != null ? ((LensContext) context).toLensContextType() : null;
        storeModelContext(task, modelContext);
    }

    public void storeModelContext(Task task, LensContextType context) throws SchemaException {
        task.setModelOperationContext(context);
    }

    public void storeResultingDeltas(ObjectTreeDeltas deltas, Task task) throws SchemaException {
        ObjectTreeDeltasType deltasType = ObjectTreeDeltas.toObjectTreeDeltasType(deltas);
        if (task.getWorkflowContext().getProcessorSpecificState() == null) {
            throw new IllegalStateException("No processor specific state in task " + task);
        }
        ItemDefinition<?> def = prismContext.getSchemaRegistry()
                .findContainerDefinitionByCompileTimeClass(WfPrimaryChangeProcessorStateType.class)
                .findPropertyDefinition(F_RESULTING_DELTAS);
        ItemPath path = new ItemPath(F_WORKFLOW_CONTEXT, F_PROCESSOR_SPECIFIC_STATE, F_RESULTING_DELTAS);
        task.addModification(DeltaBuilder.deltaFor(TaskType.class, prismContext)
                .item(path, def).replace(deltasType)
                .asItemDelta());
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Stored deltas into task {}:\n{}", task, deltas);      // TODO debug dump
        }
    }

    public ObjectTreeDeltas retrieveDeltasToProcess(Task task) throws SchemaException {
        PrismProperty<ObjectTreeDeltasType> deltaTypePrismProperty = task.getTaskPrismObject().findProperty(new ItemPath(F_WORKFLOW_CONTEXT, F_PROCESSOR_SPECIFIC_STATE, F_DELTAS_TO_PROCESS));
        if (deltaTypePrismProperty == null) {
            throw new SchemaException("No deltas to process in task extension; task = " + task);
        }
        return ObjectTreeDeltas.fromObjectTreeDeltasType(deltaTypePrismProperty.getRealValue(), prismContext);
    }

    public ObjectTreeDeltas retrieveResultingDeltas(Task task) throws SchemaException {
        PrismProperty<ObjectTreeDeltasType> deltaTypePrismProperty = task.getTaskPrismObject().findProperty(new ItemPath(F_WORKFLOW_CONTEXT, F_PROCESSOR_SPECIFIC_STATE, F_RESULTING_DELTAS));
        if (deltaTypePrismProperty == null) {
            return null;
        }
        return ObjectTreeDeltas.fromObjectTreeDeltasType(deltaTypePrismProperty.getRealValue(), prismContext);
    }

    public boolean isProcessInstanceFinished(Task task) {
        return task.getWorkflowContext() != null && task.getWorkflowContext().getEndTimestamp() != null;
    }

    public void setRootTaskOidImmediate(Task task, String oid, OperationResult result) throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        Collection<PrismReferenceValue> values = new ArrayList<>();
        if (StringUtils.isNotEmpty(oid)) {
            values.add(createObjectRef(oid, TASK).asReferenceValue());
        }
        task.addModificationImmediate(
                DeltaBuilder.deltaFor(TaskType.class, prismContext)
                        .item(F_WORKFLOW_CONTEXT, F_ROOT_TASK_REF).replace(values)
                        .asItemDelta(),
                result);
    }

    public String getRootTaskOid(Task task) {
        ObjectReferenceType ref = task.getWorkflowContext() != null ? task.getWorkflowContext().getRootTaskRef() : null;
        return ref != null ? ref.getOid() : null;
    }

    public void deleteModelOperationContext(Task task) throws SchemaException, ObjectNotFoundException {
        task.setModelOperationContext(null);
    }

    public void addApprovedBy(Task task, ObjectReferenceType referenceType) throws SchemaException {
        addApprovedBy(task, Collections.singletonList(referenceType));
    }

    public void addApprovedBy(Task task, Collection<ObjectReferenceType> referenceTypes) throws SchemaException {
        task.addModification(DeltaBuilder.deltaFor(TaskType.class, prismContext)
                .item(F_WORKFLOW_CONTEXT, F_APPROVED_BY_REF).add(
                        cloneListMembers(objectReferenceListToPrismReferenceValues(referenceTypes)))
                .asItemDelta());
    }

    public void addApprovedBy(Task task, String oid) throws SchemaException {
        ObjectReferenceType objectReferenceType = new ObjectReferenceType();
        objectReferenceType.setOid(oid);
        addApprovedBy(task, objectReferenceType);
    }

    public PrismReference getApprovedBy(Task task) throws SchemaException {
        return task.getTaskPrismObject().findReference(new ItemPath(F_WORKFLOW_CONTEXT, F_APPROVED_BY_REF));
    }

    public List<? extends ObjectReferenceType> getApprovedByFromTaskTree(Task task, OperationResult result) throws SchemaException {
        // we use a OID-keyed map to (1) keep not only the OID, but whole reference, but (2) eliminate uncertainty in comparing references
        Map<String,PrismReferenceValue> approvers = new HashMap<>();

        List<Task> tasks = task.listSubtasksDeeply(result);
        tasks.add(task);
        for (Task aTask : tasks) {
            PrismReference approvedBy = getApprovedBy(aTask);
            if (approvedBy != null) {
                for (PrismReferenceValue referenceValue : approvedBy.getValues()) {
                    approvers.put(referenceValue.getOid(), referenceValue);
                }
            }
        }

        List<ObjectReferenceType> retval = new ArrayList<>(approvers.size());
        for (PrismReferenceValue approverRefValue : approvers.values()) {
            ObjectReferenceType referenceType = new ObjectReferenceType();
            referenceType.setupReferenceValue(approverRefValue.clone());
            retval.add(referenceType);
        }
        return retval;
    }

    // handlers are stored in the list in the order they should be executed; so the last one has to be pushed first
    void pushHandlers(Task task, List<UriStackEntry> handlers) {
        for (int i = handlers.size()-1; i >= 0; i--) {
            UriStackEntry entry = handlers.get(i);
            if (WAIT_FOR_TASKS_HANDLER_URI.equals(entry.getHandlerUri())) {
                task.pushWaitForTasksHandlerUri();
            } else {
                if (!entry.getExtensionDelta().isEmpty()) {
                    throw new UnsupportedOperationException("handlers with extension delta set are not supported yet");
                }
                task.pushHandlerUri(entry.getHandlerUri(), entry.getSchedule(), TaskBinding.fromTaskType(entry.getBinding()), (ItemDelta) null);
            }
        }
    }
}
