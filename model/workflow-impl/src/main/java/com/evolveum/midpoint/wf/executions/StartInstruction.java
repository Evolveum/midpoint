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

package com.evolveum.midpoint.wf.executions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskBinding;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.wf.processors.ChangeProcessor;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ScheduleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UriStackEntry;
import com.evolveum.midpoint.xml.ns._public.model.model_context_2.LensContextType;
import com.evolveum.prism.xml.ns._public.types_2.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;

/**
 * A generic instruction to start a workflow process.
 * May be subclassed in order to add further information.
 *
 * @author mederly
 */
public class StartInstruction implements DebugDumpable {

    private ExecutionContext executionContext;

    private Map<String,Object> processVariables = new HashMap<String,Object>();
    private Map<QName,Item> taskVariables = new HashMap<QName,Item>();
    private String processName;
    private PolyStringType taskName;
    private boolean simple;
    private boolean noProcess;            // no wf process, only direct execution of specified deltas
    private boolean executeImmediately;     // executes as soon as possible, i.e. usually directly after approval
    private boolean createSuspended;

    private List<UriStackEntry> handlersLast = new ArrayList<UriStackEntry>();      // what should be executed last (i.e. what should go to the URI stack first)

    public StartInstruction(Task parentTask, ChangeProcessor changeProcessor) {
        executionContext = new ExecutionContext(parentTask, changeProcessor);
    }

    public boolean isSimple() {
        return simple;
    }

    public void setSimple(boolean simple) {
        this.simple = simple;
    }

    public void setProcessName(String name) {
        processName = name;
    }

    public String getProcessName() {
        return processName;
    }

    public Map<String, Object> getProcessVariables() {
        return processVariables;
    }

    public void addProcessVariable(String name, Object value) {
        processVariables.put(name, value);
    }

    public PolyStringType getTaskName() {
        return taskName;
    }

    public void setTaskName(PolyStringType taskName) {
        this.taskName = taskName;
    }

    public boolean isExecuteImmediately() {
        return executeImmediately;
    }

    public void setExecuteImmediately(boolean executeImmediately) {
        this.executeImmediately = executeImmediately;
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

    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    public void setExecutionContext(ExecutionContext executionContext) {
        this.executionContext = executionContext;
    }

    public String toString() {
        return "StartInstruction: processName = " + processName + ", simple: " + simple + ", variables: " + processVariables;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();

        DebugUtil.indentDebugDump(sb, indent);
        sb.append("StartInstruction: process: " + processName + " (" +
                (simple ? "simple" : "smart") + ", " +
                (executeImmediately ? "execute-immediately" : "execute-at-end") + ", " +
                (noProcess ? "no-process" : "with-process") +
                "), task = " + taskName + "\n");

        DebugUtil.indentDebugDump(sb, indent);
        sb.append("Process variables:\n");

        for (Map.Entry<String, Object> entry : processVariables.entrySet()) {
            DebugUtil.indentDebugDump(sb, indent);
            sb.append(" - " + entry.getKey() + " = ");
            Object value = entry.getValue();
            if (value instanceof DebugDumpable) {
                sb.append("\n" + ((DebugDumpable) value).debugDump(indent+1));
            } else if (value instanceof Dumpable) {
                sb.append("\n" + ((Dumpable) value).dump());
            } else {
                sb.append(value != null ? value.toString() : "null");
            }
            sb.append("\n");
        }
        return sb.toString();

    }

    public void setCreateSuspended(boolean createSuspended) {
        this.createSuspended = createSuspended;
    }

    public boolean isCreateSuspended() {
        return createSuspended;
    }

    // the last handler entered here via 'executeLast' will really be executed last
    // (handlers will be put onto task handler stack from the last to the first)
    public void executeLast(String handlerUri, ScheduleType scheduleType, TaskBinding taskBinding) {
        UriStackEntry uriStackEntry = new UriStackEntry();
        uriStackEntry.setHandlerUri(handlerUri);
        uriStackEntry.setSchedule(scheduleType);
        uriStackEntry.setBinding(taskBinding != null ? taskBinding.toTaskType() : null);
        handlersLast.add(0, uriStackEntry);
    }

    public List<UriStackEntry> getHandlersLast() {
        return handlersLast;
    }

    public <T> void addTaskVariable(PrismPropertyDefinition<T> definition, T realValue) {
        PrismProperty property = definition.instantiate();
        property.setRealValue(realValue);
        taskVariables.put(definition.getName(), property);
    }

    public <T> void addTaskVariableValues(PrismPropertyDefinition<T> definition, Collection<T> realValues) {
        PrismProperty<T> property = definition.instantiate();
        for (T realValue : realValues) {
            property.addRealValue(realValue);
        }
        taskVariables.put(definition.getName(), property);
    }

    // a bit of hack - why should StartInstruction deal with deltas? - but nevertheless quite useful
    public void addTaskDeltasVariable(PrismPropertyDefinition<ObjectDeltaType> definition, Collection<ObjectDelta> deltas) throws SchemaException {
        List<ObjectDeltaType> deltaTypes = new ArrayList<ObjectDeltaType>(deltas.size());
        for (ObjectDelta<? extends ObjectType> delta : deltas) {
            deltaTypes.add(DeltaConvertor.toObjectDeltaType(delta));
        }
        addTaskVariableValues(definition, deltaTypes);
    }

    public void addTaskDeltasVariable(PrismPropertyDefinition<ObjectDeltaType> definition, ObjectDelta delta) throws SchemaException {
        addTaskDeltasVariable(definition, Arrays.asList(delta));
    }

    public void addTaskModelContext(LensContext lensContext) throws SchemaException {
        Validate.notNull(lensContext, "model context cannot be null");
        PrismContainer<LensContextType> modelContext = lensContext.toPrismContainer();
        taskVariables.put(modelContext.getName(), modelContext);
    }

    public Map<QName, Item> getTaskVariables() {
        return taskVariables;
    }
}