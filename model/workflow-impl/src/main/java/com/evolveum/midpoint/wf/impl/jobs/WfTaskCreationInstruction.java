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
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskBinding;
import com.evolveum.midpoint.task.api.TaskRecurrence;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.wf.impl.processes.ProcessMidPointInterface;
import com.evolveum.midpoint.wf.impl.processes.common.ActivitiUtil;
import com.evolveum.midpoint.wf.impl.processes.common.LightweightObjectRefImpl;
import com.evolveum.midpoint.wf.impl.processors.ChangeProcessor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScheduleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UriStackEntry;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.*;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;
import static com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames.*;

/**
 * A generic instruction to start a workflow process and/or a task (using umbrella term "a job").
 * May be subclassed in order to add further information.
 *
 * @author mederly
 */
public class WfTaskCreationInstruction implements DebugDumpable {

    private ChangeProcessor changeProcessor;

    private String processDefinitionKey;     // name of wf process to be started (e.g. ItemApproval)
    private String processInstanceName;

    private Map<String,Serializable> processVariables = new HashMap<>();     // values of process variables
    private ModelContext taskModelContext;   // model context to be put into the task
    private PrismObject taskObject;          // object to be attached to the task; this object must have its definition available
    private PrismObject<UserType> taskOwner; // if null, owner from parent task will be taken (if there's no parent task, exception will be thrown)
    private PolyStringType taskName;         // name of task to be created/updated (applies only if the task has no name already) - e.g. "Approve adding role R to U"

    private boolean executeModelOperationHandler;       // should the job contain model operation to be executed?
    private boolean noProcess;                          // should the job provide no wf process (only direct execution of model operation)?

    private boolean simple;                             // is workflow task simple? (i.e. such that requires periodic watching of its state)
    private boolean sendStartConfirmation = true;       // should we send explicit "process started" event when the process was started by midPoint?
                                                        // for listener-enabled processes this can be misleading, because "process started" event could come
                                                        // after "process finished" one (for immediately-finishing processes)
                                                        //
                                                        // unfortunately, it seems we have to live with this (unless we define a "process started" listener)

    private boolean createTaskAsSuspended;                // should the task be created in SUSPENDED state?
    private boolean createTaskAsWaiting;                  // should the task be created in WAITING state?

    // what should be executed at a given occasion (in the order of being in this list)
    private List<UriStackEntry> handlersAfterModelOperation = new ArrayList<>();
    private List<UriStackEntry> handlersBeforeModelOperation = new ArrayList<>();
    private List<UriStackEntry> handlersAfterWfProcess = new ArrayList<>();

    //region Constructors
    protected WfTaskCreationInstruction(ChangeProcessor changeProcessor) {
        Validate.notNull(changeProcessor);
        this.changeProcessor = changeProcessor;
    }

    protected WfTaskCreationInstruction(WfTask parentWfTask) {
        this(parentWfTask.getChangeProcessor());
    }

    public static WfTaskCreationInstruction createModelOperationRootJob(ChangeProcessor changeProcessor, ModelContext modelContext) throws SchemaException {
        WfTaskCreationInstruction instruction = new WfTaskCreationInstruction(changeProcessor);
        instruction.setNoProcess(true);
        instruction.addTaskModelContext(modelContext);
        instruction.setExecuteModelOperationHandler(true);
        return instruction;
    }

    public static WfTaskCreationInstruction createNoModelOperationRootJob(ChangeProcessor changeProcessor) throws SchemaException {
        WfTaskCreationInstruction instruction = new WfTaskCreationInstruction(changeProcessor);
        instruction.setNoProcess(true);
        instruction.setExecuteModelOperationHandler(false);
        return instruction;
    }

    public static WfTaskCreationInstruction createWfProcessChildJob(ChangeProcessor changeProcessor) {
        WfTaskCreationInstruction jci = new WfTaskCreationInstruction(changeProcessor);
        prepareWfProcessChildJobInternal(jci);
        return jci;
    }

    public static WfTaskCreationInstruction createWfProcessChildJob(WfTask parentWfTask) {
        WfTaskCreationInstruction jci = new WfTaskCreationInstruction(parentWfTask);
        prepareWfProcessChildJobInternal(jci);
        return jci;
    }

    protected static void prepareWfProcessChildJobInternal(WfTaskCreationInstruction instruction) {
        instruction.setNoProcess(false);
        instruction.initializeCommonProcessVariables();
    }

    public static WfTaskCreationInstruction createModelOperationChildJob(WfTask parentWfTask, ModelContext modelContext) throws SchemaException {
        WfTaskCreationInstruction instruction = new WfTaskCreationInstruction(parentWfTask);
        instruction.setNoProcess(true);
        instruction.addTaskModelContext(modelContext);
        instruction.setExecuteModelOperationHandler(true);
        return instruction;
    }
    //endregion

    // region Simple getters and setters
    public boolean isSimple() {
        return simple;
    }

    public void setSimple(boolean simple) {
        this.simple = simple;
    }

    public boolean isSendStartConfirmation() {
        return sendStartConfirmation;
    }

    public void setSendStartConfirmation(boolean sendStartConfirmation) {
        this.sendStartConfirmation = sendStartConfirmation;
    }

    public void setProcessDefinitionKey(String name) {
        processDefinitionKey = name;
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public String getProcessInstanceName() {
        return processInstanceName;
    }

    public Map<String,Object> getProcessVariables() {
        return Collections.<String,Object>unmodifiableMap(processVariables);
    }

    public void addProcessVariable(String name, Serializable value) {
        processVariables.put(name, value);
    }

    protected void removeProcessVariable(String name) {
        processVariables.remove(name);
    }

    public PolyStringType getTaskName() {
        return taskName;
    }

    public void setTaskName(PolyStringType taskName) {
        this.taskName = taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = new PolyStringType(taskName);
    }

    public boolean isNoProcess() {
        return noProcess;
    }

    public boolean startsWorkflowProcess() {
        return !noProcess;
    }

    public void setNoProcess(boolean noProcess) {
        this.noProcess = noProcess;
    }

    public void setCreateTaskAsSuspended(boolean createTaskAsSuspended) {
        this.createTaskAsSuspended = createTaskAsSuspended;
    }

    public boolean isCreateTaskAsSuspended() {
        return createTaskAsSuspended;
    }

    public List<UriStackEntry> getHandlersAfterModelOperation() {
        return handlersAfterModelOperation;
    }

    public List<UriStackEntry> getHandlersBeforeModelOperation() {
        return handlersBeforeModelOperation;
    }

    public List<UriStackEntry> getHandlersAfterWfProcess() {
        return handlersAfterWfProcess;
    }

    public void setExecuteModelOperationHandler(boolean executeModelOperationHandler) {
        this.executeModelOperationHandler = executeModelOperationHandler;
    }

    public boolean isExecuteModelOperationHandler() {
        return executeModelOperationHandler;
    }

    public void setTaskObject(PrismObject taskObject) {
        this.taskObject = taskObject;
    }

    public PrismObject getTaskObject() {
        return taskObject;
    }

    public void setCreateTaskAsWaiting(boolean createTaskAsWaiting) {
        this.createTaskAsWaiting = createTaskAsWaiting;
    }

    public boolean isCreateTaskAsWaiting() {
        return createTaskAsWaiting;
    }

    public ChangeProcessor getChangeProcessor() {
        return changeProcessor;
    }

    public PrismObject<UserType> getTaskOwner() {
        return taskOwner;
    }

    public void setTaskOwner(PrismObject<UserType> taskOwner) {
        this.taskOwner = taskOwner;
    }

    public ModelContext getTaskModelContext() {
        return taskModelContext;
    }

    public void setTaskModelContext(ModelContext taskModelContext) {
        this.taskModelContext = taskModelContext;
    }
    //endregion

    //region Setters for handlers
    public void setHandlersBeforeModelOperation(String... handlerUri) {
        setHandlers(handlersBeforeModelOperation, createUriStackEntries(handlerUri));
    }

    public void setHandlersAfterModelOperation(String... handlerUri) {
        setHandlers(handlersAfterModelOperation, createUriStackEntries(handlerUri));
    }

    public void setHandlersAfterWfProcess(String... handlerUri) {
        setHandlers(handlersAfterWfProcess, createUriStackEntries(handlerUri));
    }

    public void addHandlersAfterWfProcessAtEnd(String... handlerUriArray) {
        addHandlersAtEnd(handlersAfterWfProcess, createUriStackEntries(handlerUriArray));
    }

    private List<UriStackEntry> createUriStackEntries(String[] handlerUriArray) {
        List<UriStackEntry> retval = new ArrayList<UriStackEntry>();
        for (String handlerUri : handlerUriArray) {
            retval.add(createUriStackEntry(handlerUri));
        }
        return retval;
    }

    private void addAtBeginningOfExecutionList(List<UriStackEntry> list, String handlerUri, TaskRecurrence recurrence, ScheduleType scheduleType, TaskBinding taskBinding) {
        list.add(0, createUriStackEntry(handlerUri, recurrence, scheduleType, taskBinding));
    }

    private void addAtEndOfExecutionList(List<UriStackEntry> list, String handlerUri, TaskRecurrence recurrence, ScheduleType scheduleType, TaskBinding taskBinding) {
        list.add(createUriStackEntry(handlerUri, recurrence, scheduleType, taskBinding));
    }

    private UriStackEntry createUriStackEntry(String handlerUri, TaskRecurrence recurrence, ScheduleType scheduleType, TaskBinding taskBinding) {
        UriStackEntry uriStackEntry = new UriStackEntry();
        uriStackEntry.setHandlerUri(handlerUri);
        uriStackEntry.setRecurrence(recurrence != null ? recurrence.toTaskType() : null);
        uriStackEntry.setSchedule(scheduleType);
        uriStackEntry.setBinding(taskBinding != null ? taskBinding.toTaskType() : null);
        return uriStackEntry;
    }

    private void setHandlers(List<UriStackEntry> list, List<UriStackEntry> uriStackEntry) {
        list.clear();
        list.addAll(uriStackEntry);
    }

    private void addHandlersAtEnd(List<UriStackEntry> list, List<UriStackEntry> uriStackEntry) {
        list.addAll(uriStackEntry);
    }

    private UriStackEntry createUriStackEntry(String handlerUri) {
        return createUriStackEntry(handlerUri, TaskRecurrence.SINGLE, new ScheduleType(), null);
    }
    //endregion

    //region Setters for task variables
    public void addTaskModelContext(ModelContext modelContext) throws SchemaException {
        Validate.notNull(modelContext, "model context cannot be null");
        setTaskModelContext(modelContext);
    }
    //endregion

    //region Setters for process variables
    public void initializeCommonProcessVariables() {
        addProcessVariable(VARIABLE_UTIL, new ActivitiUtil());
        addProcessVariable(VARIABLE_CHANGE_PROCESSOR, changeProcessor.getClass().getName());
        addProcessVariable(VARIABLE_START_TIME, new Date());
    }

    public void setRequesterOidAndRefInProcess(PrismObject<UserType> requester) {
        addProcessVariable(VARIABLE_REQUESTER_REF, new LightweightObjectRefImpl(createObjectRef(requester)));
    }

    public void setProcessInstanceName(String name) {
        processInstanceName = name;
        addProcessVariable(VARIABLE_PROCESS_INSTANCE_NAME, name);
    }

    public void setProcessInterfaceBean(ProcessMidPointInterface processInterfaceBean) {
        addProcessVariable(VARIABLE_PROCESS_INTERFACE_BEAN_NAME, processInterfaceBean.getBeanName());
    }
    //endregion

    //region Diagnostics
    public String toString() {
        return "WfTaskCreationInstruction: processDefinitionKey = " + processDefinitionKey + ", simple: " + simple + ", variables: " + processVariables;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();

        DebugUtil.indentDebugDump(sb, indent);
        sb.append("WfTaskCreationInstruction: process: " + processDefinitionKey + " (" +
                (simple ? "simple" : "smart") + ", " +
                (noProcess ? "no-process" : "with-process") +
                "), task = " + taskName + "\n");

        if (!noProcess) {
            DebugUtil.indentDebugDump(sb, indent);
            sb.append("Process variables:\n");

            for (Map.Entry<String, Serializable> entry : processVariables.entrySet()) {
                DebugUtil.indentDebugDump(sb, indent);
                sb.append(" - " + entry.getKey() + " = ");
                Object value = entry.getValue();
                if (value instanceof DebugDumpable) {
                    sb.append("\n" + ((DebugDumpable) value).debugDump(indent+1));
                } else {
                    sb.append(value != null ? value.toString() : "null");
                }
                sb.append("\n");
            }
        }
        return sb.toString();

    }
    //endregion

    public void tailorTask(Task task) throws SchemaException {
    }
}