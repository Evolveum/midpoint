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

package com.evolveum.midpoint.wf.impl.jobs;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskBinding;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.WfConfiguration;
import com.evolveum.midpoint.wf.impl.processors.ChangeProcessor;
import com.evolveum.midpoint.wf.impl.processors.primary.ObjectTreeDeltas;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.PrimaryChangeAspect;
import com.evolveum.midpoint.wf.processors.primary.PcpTaskExtensionItemsNames;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.*;

import static com.evolveum.midpoint.schema.constants.ObjectTypes.TASK;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType.F_WORKFLOW_CONTEXT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType.*;

/**
 * Handles low-level task operations, e.g. handling wf* properties in task extension.
 *
 * @author mederly
 *
 */

@Component
//@DependsOn({ "prismContext" })

public class WfTaskUtil {

    @Autowired
    private WfConfiguration wfConfiguration;

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired
    private PrismContext prismContext;

    @Autowired
    private ProvisioningService provisioningService;

	private static final Trace LOGGER = TraceManager.getTrace(WfTaskUtil.class);

    public static final String WAIT_FOR_TASKS_HANDLER_URI = "<<< marker for calling pushWaitForTasksHandlerUri >>>";

    // workflow-related extension properties
    private PrismPropertyDefinition wfPrimaryChangeAspectPropertyDefinition;
    private PrismPropertyDefinition<ObjectTreeDeltasType> wfDeltasToProcessPropertyDefinition;
    private PrismPropertyDefinition<ObjectTreeDeltasType> wfResultingDeltasPropertyDefinition;
    private PrismReferenceDefinition wfApprovedByReferenceDefinition;

    @PostConstruct
    public void init() {

		wfPrimaryChangeAspectPropertyDefinition = prismContext.getSchemaRegistry().findPropertyDefinitionByElementName(PcpTaskExtensionItemsNames.WFPRIMARY_CHANGE_ASPECT_NAME);
        wfDeltasToProcessPropertyDefinition = prismContext.getSchemaRegistry().findPropertyDefinitionByElementName(PcpTaskExtensionItemsNames.WFDELTAS_TO_PROCESS_PROPERTY_NAME);
        wfResultingDeltasPropertyDefinition = prismContext.getSchemaRegistry().findPropertyDefinitionByElementName(PcpTaskExtensionItemsNames.WFRESULTING_DELTAS_PROPERTY_NAME);
        wfApprovedByReferenceDefinition = prismContext.getSchemaRegistry().findReferenceDefinitionByElementName(PcpTaskExtensionItemsNames.WFAPPROVED_BY_REFERENCE_NAME);

        Validate.notNull(wfPrimaryChangeAspectPropertyDefinition, PcpTaskExtensionItemsNames.WFPRIMARY_CHANGE_ASPECT_NAME + " definition was not found");
        Validate.notNull(wfDeltasToProcessPropertyDefinition, PcpTaskExtensionItemsNames.WFDELTAS_TO_PROCESS_PROPERTY_NAME + " definition was not found");
        Validate.notNull(wfResultingDeltasPropertyDefinition, PcpTaskExtensionItemsNames.WFRESULTING_DELTAS_PROPERTY_NAME + " definition was not found");
        Validate.notNull(wfApprovedByReferenceDefinition, PcpTaskExtensionItemsNames.WFAPPROVED_BY_REFERENCE_NAME + " definition was not found");

    }

	void setWfProcessId(Task task, String pid) throws SchemaException {
        Validate.notEmpty(task.getOid(), "Task oid must not be null or empty (task must be persistent).");
        task.addModification(
                DeltaBuilder.deltaFor(TaskType.class, prismContext)
                        .item(F_WORKFLOW_CONTEXT, F_PROCESS_INSTANCE_ID).replace(pid)
                        .asItemDelta());
	}

    public void setPrimaryChangeAspect(Task task, PrimaryChangeAspect aspect) throws SchemaException {
        Validate.notNull(aspect, "Primary change aspect is undefined.");
        PrismProperty<String> w = wfPrimaryChangeAspectPropertyDefinition.instantiate();
        w.setRealValue(aspect.getClass().getName());
        task.setExtensionProperty(w);
    }

    public PrimaryChangeAspect getPrimaryChangeAspect(Task task, Collection<PrimaryChangeAspect> aspects) {
        String aspectClassName = getExtensionValue(String.class, task, PcpTaskExtensionItemsNames.WFPRIMARY_CHANGE_ASPECT_NAME);
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

    public void setChangeProcessor(Task task, ChangeProcessor processor) throws SchemaException {
        Validate.notNull(processor, "Change processor is undefined.");
        task.addModification(DeltaBuilder.deltaFor(TaskType.class, prismContext)
                .item(F_WORKFLOW_CONTEXT, F_CHANGE_PROCESSOR).replace(processor.getClass().getName())
                .asItemDelta());
    }

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

    public ModelContext retrieveModelContext(Task task, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException {
        LensContextType modelContextType = task.getModelOperationContext();
        if (modelContextType == null) {
//            throw new SystemException("No model context information in task " + task);
            return null;
        }
//        Object value = modelContextProperty.getRealValue();
//        if (value instanceof Element || value instanceof JAXBElement) {
//            value = prismContext.getPrismJaxbProcessor().unmarshalObject(value, LensContextType.class);
//        }
//        if (!(value instanceof LensContextType)) {
//            throw new SystemException("Model context information in task " + task + " is of wrong type: " + modelContextProperty.getRealValue().getClass());
//        }
        return LensContext.fromLensContextType(modelContextType, prismContext, provisioningService, result);
    }

    public void storeModelContext(Task task, ModelContext context) throws SchemaException {
        //Validate.notNull(context, "model context cannot be null");
        LensContextType modelContext = context != null ? ((LensContext) context).toLensContextType() : null;
        storeModelContext(task, modelContext);
    }

    public void storeModelContext(Task task, LensContextType context) throws SchemaException {
        task.setModelOperationContext(context);
    }

    public void storeResultingDeltas(ObjectTreeDeltas deltas, Task task) throws SchemaException {
        PrismProperty<ObjectTreeDeltasType> resultingDeltas = wfResultingDeltasPropertyDefinition.instantiate();
        ObjectTreeDeltasType deltasType = ObjectTreeDeltas.toObjectTreeDeltasType(deltas);
        resultingDeltas.setRealValue(deltasType);
        task.setExtensionProperty(resultingDeltas);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Stored deltas into task {}:\n{}", task, deltas);      // TODO debug dump
        }
    }

    public ObjectTreeDeltas retrieveDeltasToProcess(Task task) throws SchemaException {

        PrismProperty<ObjectTreeDeltasType> deltaTypePrismProperty = task.getExtensionProperty(PcpTaskExtensionItemsNames.WFDELTAS_TO_PROCESS_PROPERTY_NAME);
        if (deltaTypePrismProperty == null) {
            throw new SchemaException("No " + PcpTaskExtensionItemsNames.WFDELTAS_TO_PROCESS_PROPERTY_NAME + " in task extension; task = " + task);
        }
        return ObjectTreeDeltas.fromObjectTreeDeltasType(deltaTypePrismProperty.getRealValue(), prismContext);
    }

    // it is possible that resulting deltas are not in the task
    public ObjectTreeDeltas retrieveResultingDeltas(Task task) throws SchemaException {
        PrismProperty<ObjectTreeDeltasType> deltaTypePrismProperty = task.getExtensionProperty(PcpTaskExtensionItemsNames.WFRESULTING_DELTAS_PROPERTY_NAME);
        if (deltaTypePrismProperty == null) {
            return null;
        }
        return ObjectTreeDeltas.fromObjectTreeDeltasType(deltaTypePrismProperty.getRealValue(), prismContext);
    }

    public void setTaskNameIfEmpty(Task t, PolyStringType taskName) {
        if (t.getName() == null || t.getName().toPolyString().isEmpty()) {
            t.setName(taskName);
        }
    }


    private<T> T getExtensionValue(Class<T> clazz, Task task, QName propertyName) {
        PrismProperty<String> property = task.getExtensionProperty(propertyName);
        return property != null ? property.getRealValue(clazz) : null;
    }

    public void setProcessInstanceFinishedImmediate(Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        XMLGregorianCalendar now = XmlTypeConverter.createXMLGregorianCalendar(new Date());
        task.addModificationImmediate(
                DeltaBuilder.deltaFor(TaskType.class, prismContext)
                        .item(F_WORKFLOW_CONTEXT, F_END_TIMESTAMP).replace(now)
                        .asItemDelta(),
                result);
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

    public void deleteModelOperationContext(Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        task.setModelOperationContext(null);
    }

    public void addApprovedBy(Task task, ObjectReferenceType referenceType) throws SchemaException {
        PrismReference wfApprovedBy = wfApprovedByReferenceDefinition.instantiate();
        PrismReferenceValue newValue = new PrismReferenceValue(referenceType.getOid());
        wfApprovedBy.add(newValue);
        task.addExtensionReference(wfApprovedBy);
    }

    public void addApprovedBy(Task task, Collection<ObjectReferenceType> referenceTypes) throws SchemaException {
        PrismReference wfApprovedBy = wfApprovedByReferenceDefinition.instantiate();
        for (ObjectReferenceType referenceType : referenceTypes) {
            wfApprovedBy.add(referenceType.asReferenceValue().clone());
        }
        task.addExtensionReference(wfApprovedBy);
    }

    public void addApprovedBy(Task task, String oid) throws SchemaException {
        ObjectReferenceType objectReferenceType = new ObjectReferenceType();
        objectReferenceType.setOid(oid);
        addApprovedBy(task, objectReferenceType);
    }

    public PrismReference getApprovedBy(Task task) throws SchemaException {
        return task.getExtensionReference(PcpTaskExtensionItemsNames.WFAPPROVED_BY_REFERENCE_NAME);
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

    public PrismPropertyDefinition getWfPrimaryChangeAspectPropertyDefinition() {
        return wfPrimaryChangeAspectPropertyDefinition;
    }

    public PrismPropertyDefinition getWfDeltasToProcessPropertyDefinition() {
        return wfDeltasToProcessPropertyDefinition;
    }

    public PrismPropertyDefinition getWfResultingDeltasPropertyDefinition() {
        return wfResultingDeltasPropertyDefinition;
    }

    public PrismReferenceDefinition getWfApprovedByReferenceDefinition() {
        return wfApprovedByReferenceDefinition;
    }

    void setTaskOwner(Task task, PrismObject<UserType> owner) {
        if (owner == null) {
            throw new IllegalStateException("Couldn't create a job task because the owner is not set.");
        }
        task.setOwner(owner);
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

    /**
     * Makes a task active, i.e. a task that actively queries wf process instance about its status.
     *
     *          OR
     *
     * Creates a passive task, i.e. a task that stores information received from WfMS about a process instance.
     *
     * We expect task to be transient at this moment!
     * @param active
     * @param t
     * @param result
     */
    void pushProcessShadowHandler(boolean active, Task t, long taskStartDelay, OperationResult result) throws SchemaException, ObjectNotFoundException {

        if (active) {

            ScheduleType schedule = new ScheduleType();
            schedule.setInterval(wfConfiguration.getProcessCheckInterval());
            schedule.setEarliestStartTime(MiscUtil.asXMLGregorianCalendar(new Date(System.currentTimeMillis() + taskStartDelay)));
            t.pushHandlerUri(WfProcessInstanceShadowTaskHandler.HANDLER_URI, schedule, TaskBinding.LOOSE);

        } else {

            t.pushHandlerUri(WfProcessInstanceShadowTaskHandler.HANDLER_URI, new ScheduleType(), null);		// note that this handler will not be actively used (at least for now)
            t.makeWaiting();
        }
    }

    void setDefaultTaskOwnerIfEmpty(Task t, OperationResult result, WfTaskController wfTaskController) throws SchemaException, ObjectNotFoundException {
        if (t.getOwner() == null) {
            t.setOwner(repositoryService.getObject(UserType.class, SystemObjectsType.USER_ADMINISTRATOR.value(), null, result));
        }
    }
}
