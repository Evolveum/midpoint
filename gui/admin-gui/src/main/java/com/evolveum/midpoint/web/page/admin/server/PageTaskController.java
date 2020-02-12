/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskEditableState;
import com.evolveum.midpoint.web.util.TaskOperationUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.*;

/**
 * @author mederly
 */
public class PageTaskController implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageTaskController.class);

    private PageTaskEdit parentPage;

    public PageTaskController() {}
    public PageTaskController(PageTaskEdit parentPage) {
        this.parentPage = parentPage;
    }

    public void deleteSyncTokenPerformed(AjaxRequestTarget target) {
        LOGGER.debug("Deleting sync token.");
        OperationResult result = new OperationResult(PageTaskEdit.OPERATION_DELETE_SYNC_TOKEN);

        Task operationTask = parentPage.createSimpleTask(PageTaskEdit.OPERATION_DELETE_SYNC_TOKEN);
        try {
            final TaskDto taskDto = parentPage.getTaskDto();
            final PrismProperty property = taskDto.getExtensionProperty(SchemaConstants.SYNC_TOKEN);
            if (property == null) {
                result.recordWarning(parentPage.createStringResource("PageTaskController.message.deleteSyncTokenPerformed.warning").getString());        // should be treated by isVisible
            } else {
                final ObjectDelta<? extends ObjectType> delta =
                        parentPage.getPrismContext().deltaFor(TaskType.class)
                                .item(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.SYNC_TOKEN), property.getDefinition()).replace()
                                .asObjectDelta(parentPage.getTaskDto().getOid());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Deleting sync token:\n{}", delta.debugDump());
                }
                parentPage.getModelService()
                        .executeChanges(Collections.singleton(delta), null, operationTask, result);
                result.recomputeStatus();
            }
        } catch (Exception ex) {
            result.recomputeStatus();
            result.recordFatalError(parentPage.createStringResource("PageTaskController.message.deleteSyncTokenPerformed.fatalError").getString(), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete sync token from the task.", ex);
        }
        afterStateChangingOperation(target, result);
    }

    void savePerformed(AjaxRequestTarget target) {
        LOGGER.debug("Saving new task.");
        OperationResult result = new OperationResult(PageTaskEdit.OPERATION_SAVE_TASK);

        TaskDto dto = parentPage.getTaskDto();
        Task operationTask = parentPage.createSimpleTask(PageTaskEdit.OPERATION_SAVE_TASK);

        try {
            List<ItemDelta<?, ?>> itemDeltas = getDeltasToExecute(dto);
            ObjectDelta<TaskType> delta = parentPage.getPrismContext().deltaFactory().object()
                    .createModifyDelta(dto.getOid(), itemDeltas, TaskType.class
                    );
            final Collection<ObjectDelta<? extends ObjectType>> deltas = Collections.singletonList(delta);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Saving task modifications:\n{}", DebugUtil.debugDump(deltas));
            }
            parentPage.getModelService().executeChanges(deltas, null, operationTask, result);

            result.recomputeStatus();
        } catch (Exception ex) {
            result.recomputeStatus();
            result.recordFatalError(parentPage.createStringResource("PageTaskController.message.savePerformed.fatalError").getString(), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't save task modifications", ex);
        }
        afterSave(target, result);
    }

    private List<ItemDelta<?, ?>> getDeltasToExecute(TaskDto dto) throws SchemaException {

        List<ItemDelta<?, ?>> rv = new ArrayList<>();

        TaskEditableState orig = dto.getOriginalEditableState();
        TaskEditableState curr = dto.getCurrentEditableState();

        if (!StringUtils.equals(orig.getName(), curr.getName())) {
            String name = curr.getName() != null ? curr.getName() : "";
            addDelta(rv, TaskType.F_NAME, new PolyString(name));
        }

        if (!StringUtils.equals(orig.getDescription(), curr.getDescription())) {
            addDelta(rv, TaskType.F_DESCRIPTION, curr.getDescription());
        }

        ScheduleType origSchedule = orig.getScheduleType();
        ScheduleType currSchedule = curr.getScheduleType();
        if (!origSchedule.equals(currSchedule)) {
            if (dto.getTaskType().getSchedule() != null) {
                currSchedule.setLatestFinishTime(dto.getTaskType().getSchedule().getLatestFinishTime());
            }
            addDelta(rv, TaskType.F_SCHEDULE, currSchedule);
        }

        if (orig.isRecurring() != curr.isRecurring()) {
            addDelta(rv, TaskType.F_RECURRENCE, curr.isRecurring() ? TaskRecurrenceType.RECURRING : TaskRecurrenceType.SINGLE);
        }

        if (orig.isBound() != curr.isBound()) {
            addDelta(rv, TaskType.F_BINDING, curr.isBound() ? TaskBindingType.TIGHT : TaskBindingType.LOOSE);
        }

        if (orig.getThreadStopActionType() != curr.getThreadStopActionType()) {
            addDelta(rv, TaskType.F_THREAD_STOP_ACTION, curr.getThreadStopActionType());
        }

        if (dto.getTaskType().getExecutionConstraints() == null) {
            if (curr.getExecutionGroup() != null || curr.getGroupTaskLimit() != null) {
                TaskExecutionConstraintsType constraints =
                        new TaskExecutionConstraintsType(getPrismContext())
                                .group(curr.getExecutionGroup())
                                .groupTaskLimit(curr.getGroupTaskLimit());
                addDelta(rv, TaskType.F_EXECUTION_CONSTRAINTS, constraints);
            }
        } else {
            if (!ObjectUtils.equals(orig.getExecutionGroup(), curr.getExecutionGroup())) {
                addDelta(rv, TaskType.F_EXECUTION_CONSTRAINTS, TaskExecutionConstraintsType.F_GROUP, curr.getExecutionGroup());
            }
            if (!ObjectUtils.equals(orig.getGroupTaskLimit(), curr.getGroupTaskLimit())) {
                addDelta(rv, TaskType.F_EXECUTION_CONSTRAINTS, TaskExecutionConstraintsType.F_GROUP_TASK_LIMIT, curr.getExecutionGroup());
            }
        }

        if (!ObjectUtils.equals(orig.getWorkerThreads(), curr.getWorkerThreads())) {
            SchemaRegistry registry = parentPage.getPrismContext().getSchemaRegistry();
            PrismPropertyDefinition def = registry.findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_WORKER_THREADS);
            rv.add(parentPage.getPrismContext().deltaFor(TaskType.class)
                    .item(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_WORKER_THREADS), def).replace(curr.getWorkerThreads())
                    .asItemDelta());
        }

        rv.addAll(dto.getHandlerDto().getDeltasToExecute(orig.getHandlerSpecificState(), curr.getHandlerSpecificState(), parentPage.getPrismContext()));

        return rv;
    }

    private PrismContext getPrismContext() {
        return parentPage.getPrismContext();
    }

    private void addDelta(List<ItemDelta<?, ?>> deltas, QName itemName, Object itemRealValue) throws SchemaException {
        deltas.add(parentPage.getPrismContext().deltaFor(TaskType.class)
            .item(itemName).replace(itemRealValue)
            .asItemDelta());
    }

    private void addDelta(List<ItemDelta<?, ?>> deltas, QName itemName1, QName itemName2, Object itemRealValue) throws SchemaException {
        deltas.add(parentPage.getPrismContext().deltaFor(TaskType.class)
            .item(itemName1, itemName2).replace(itemRealValue)
            .asItemDelta());
    }

    void suspendPerformed(AjaxRequestTarget target) {
        String oid = parentPage.getTaskDto().getOid();
        OperationResult result = TaskOperationUtils.suspendPerformed(parentPage.getTaskService(), Collections.singleton(oid), parentPage);
        afterStateChangingOperation(target, result);
    }

    void resumePerformed(AjaxRequestTarget target) {
        String oid = parentPage.getTaskDto().getOid();
        OperationResult result = TaskOperationUtils.resumePerformed(parentPage.getTaskService(), Collections.singletonList(oid), parentPage);
        afterStateChangingOperation(target, result);
    }

    void runNowPerformed(AjaxRequestTarget target) {
        String oid = parentPage.getTaskDto().getOid();
        OperationResult result = TaskOperationUtils.runNowPerformed(parentPage.getTaskService(), Collections.singletonList(oid), parentPage);
        afterStateChangingOperation(target, result);
    }




    private void afterStateChangingOperation(AjaxRequestTarget target, OperationResult result) {
        parentPage.showResult(result);
        parentPage.refresh(target);
        target.add(parentPage.getFeedbackPanel());
    }

    private void afterSave(AjaxRequestTarget target, OperationResult result) {
        parentPage.showResult(result);
        parentPage.setEdit(false);
        parentPage.refresh(target);
        target.add(parentPage.getFeedbackPanel());
        target.add(parentPage.get(PageTaskEdit.ID_SUMMARY_PANEL));
        target.add(parentPage.get(PageTaskEdit.ID_MAIN_PANEL));
        //parentPage.setResponsePage(new PageTasks(false));
    }

    public void backPerformed(AjaxRequestTarget target) {
        parentPage.redirectBack();
    }

    public void cancelEditingPerformed(AjaxRequestTarget target) {
        if (!parentPage.isEdit()) {
            backPerformed(target);            // just for sure
            return;
        }
        parentPage.setEdit(false);
        parentPage.refresh(target);
        target.add(parentPage.getFeedbackPanel());
        target.add(parentPage.get(PageTaskEdit.ID_SUMMARY_PANEL));
        target.add(parentPage.get(PageTaskEdit.ID_MAIN_PANEL));
    }

}
