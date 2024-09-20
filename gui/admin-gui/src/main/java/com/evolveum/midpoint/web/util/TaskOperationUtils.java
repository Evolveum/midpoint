/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.util.ObjectCollectionViewUtil;
import com.evolveum.midpoint.model.api.TaskService;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.task.ActivityStateUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

public class TaskOperationUtils {

    private static final String DOT_CLASS = TaskOperationUtils.class.getName() + ".";
    private static final String OPERATION_SUSPEND_TASKS = DOT_CLASS + "suspendTasks";
    private static final String OPERATION_RESUME_TASKS = DOT_CLASS + "resumeTasks";
    private static final String OPERATION_RUN_NOW_TASKS = DOT_CLASS + "runNowTasks";

    private static final List<String> REPORT_ARCHETYPES = Arrays.asList(
            SystemObjectsType.ARCHETYPE_REPORT_EXPORT_CLASSIC_TASK.value(),
            SystemObjectsType.ARCHETYPE_REPORT_IMPORT_CLASSIC_TASK.value(),
            SystemObjectsType.ARCHETYPE_REPORT_EXPORT_DISTRIBUTED_TASK.value());

    private static final List<String> CERTIFICATION_ARCHETYPES = Arrays.asList(
            SystemObjectsType.ARCHETYPE_CERTIFICATION_OPEN_NEXT_STAGE_TASK.value(),
            SystemObjectsType.ARCHETYPE_CERTIFICATION_REMEDIATION_TASK.value());

    private static final List<String> UTILITY_ARCHETYPES = Arrays.asList(
            SystemObjectsType.ARCHETYPE_SHADOW_INTEGRITY_CHECK_TASK.value(),
            SystemObjectsType.ARCHETYPE_SHADOWS_REFRESH_TASK.value(),
            SystemObjectsType.ARCHETYPE_SHADOWS_DELETE_LONG_TIME_NOT_UPDATED_TASK.value(),
            SystemObjectsType.ARCHETYPE_EXECUTE_CHANGE_TASK.value(),
            SystemObjectsType.ARCHETYPE_EXECUTE_DELTAS_TASK.value(),
            SystemObjectsType.ARCHETYPE_REINDEX_REPOSITORY_TASK.value(),
            SystemObjectsType.ARCHETYPE_OBJECT_INTEGRITY_CHECK_TASK.value(),
            SystemObjectsType.ARCHETYPE_OBJECTS_DELETE_TASK.value());

    private static final List<String> SYSTEM_ARCHETYPES = Arrays.asList(
            SystemObjectsType.ARCHETYPE_VALIDITY_SCANNER_TASK.value(),
            SystemObjectsType.ARCHETYPE_TRIGGER_SCANNER_TASK.value(),
            SystemObjectsType.ARCHETYPE_PROPAGATION_TASK.value(),
            SystemObjectsType.ARCHETYPE_MULTI_PROPAGATION_TASK.value());

    /**
     * Suspends tasks "intelligently" i.e. tries to recognize whether to suspend a single task,
     * or to suspend the whole tree. (Maybe this differentiation should be done by the task manager itself.)
     *
     * It is also questionable whether we should create the task here or it should be done by the caller.
     * For the time being it is done here.
     */
    public static OperationResult suspendTasks(List<TaskType> selectedTasks, PageBase pageBase) {
        Task opTask = pageBase.createSimpleTask(TaskOperationUtils.OPERATION_SUSPEND_TASKS);
        OperationResult result = opTask.getResult();

        try {
            TaskService taskService = pageBase.getTaskService();

            List<TaskType> plainTasks = getPlainTasks(selectedTasks);
            List<TaskType> treeRoots = getTreeRoots(selectedTasks);
            boolean allPlainTasksSuspended = suspendPlainTasks(taskService, plainTasks, result, opTask);
            boolean allTreesSuspended = suspendTrees(taskService, treeRoots, result, opTask);

            result.computeStatus();
            if (result.isSuccess()) {
                if (allPlainTasksSuspended && allTreesSuspended) {
                    result.recordStatus(OperationResultStatus.SUCCESS,
                            pageBase.createStringResource("TaskOperationUtils.message.suspendPerformed.success").getString());
                } else {
                    result.recordWarning(pageBase.createStringResource("TaskOperationUtils.message.suspendPerformed.warning").getString());
                }
            }
        } catch (Throwable t) {
            result.recordFatalError(pageBase.createStringResource("pageTasks.message.suspendTasksPerformed.fatalError").getString(), t);
        }

        return result;
    }

    private static boolean suspendPlainTasks(TaskService taskService, List<TaskType> plainTasks, OperationResult result, Task opTask)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException {
        //noinspection SimplifiableIfStatement
        if (!plainTasks.isEmpty()) {
            return taskService.suspendTasks(ObjectTypeUtil.getOids(plainTasks), PageTasks.WAIT_FOR_TASK_STOP, opTask, result);
        } else {
            return true;
        }
    }

    private static boolean suspendTrees(TaskService taskService, List<TaskType> roots, OperationResult result, Task opTask)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException {
        boolean suspended = true;
        if (!roots.isEmpty()) {
            for (TaskType root : roots) {
                boolean s = taskService.suspendTaskTree(root.getOid(), PageTasks.WAIT_FOR_TASK_STOP, opTask, result);
                suspended = suspended && s;
            }
        }
        return suspended;
    }

    /**
     * Resumes tasks "intelligently" i.e. tries to recognize whether to resume a single task,
     * or to resume the whole tree. See {@link #suspendTasks(List, PageBase)}.
     */
    public static OperationResult resumeTasks(List<TaskType> selectedTasks, PageBase pageBase) {
        Task opTask = pageBase.createSimpleTask(OPERATION_RESUME_TASKS);
        OperationResult result = opTask.getResult();

        try {
            TaskService taskService = pageBase.getTaskService();

            List<TaskType> plainTasks = getPlainTasks(selectedTasks);
            List<TaskType> treeRoots = getTreeRoots(selectedTasks);
            taskService.resumeTasks(ObjectTypeUtil.getOids(plainTasks), opTask, result);
            for (TaskType treeRoot : treeRoots) {
                taskService.resumeTaskTree(treeRoot.getOid(), opTask, result);
            }
            result.computeStatus();
            if (result.isSuccess()) {
                result.recordStatus(OperationResultStatus.SUCCESS,
                        pageBase.createStringResource("TaskOperationUtils.message.resumePerformed.success").getString());
            }
        } catch (Throwable t) {
            result.recordFatalError(pageBase.createStringResource("TaskOperationUtils.message.resumePerformed.fatalError").getString(), t);
        }
        return result;
    }

    /**
     * Schedules the tasks for immediate execution.
     *
     * TODO should we distinguish between plain task and task tree roots here?
     */
    public static OperationResult runNowPerformed(List<String> oids, PageBase pageBase) {
        Task opTask = pageBase.createSimpleTask(OPERATION_RUN_NOW_TASKS);
        OperationResult result = opTask.getResult();
        TaskService taskService = pageBase.getTaskService();

        try {
            taskService.scheduleTasksNow(oids, opTask, result);
            result.computeStatus();

            if (result.isSuccess()) {
                result.recordStatus(OperationResultStatus.SUCCESS, pageBase.createStringResource("TaskOperationUtils.message.runNowPerformed.success").getString());
            }
        } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | ExpressionEvaluationException | RuntimeException | CommunicationException | ConfigurationException e) {
            result.recordFatalError(pageBase.createStringResource("TaskOperationUtils.message.runNowPerformed.fatalError").getString(), e);
        }

        return result;
    }

    @NotNull
    private static List<TaskType> getPlainTasks(List<TaskType> selectedTasks) {
        return selectedTasks.stream()
                .filter(task -> !ActivityStateUtil.isManageableTreeRoot(task))
                .collect(Collectors.toList());
    }

    @NotNull
    private static List<TaskType> getTreeRoots(List<TaskType> selectedTasks) {
        return selectedTasks.stream()
                .filter(ActivityStateUtil::isManageableTreeRoot)
                .collect(Collectors.toList());
    }

    public static List<CompiledObjectCollectionView> getAllApplicableArchetypeForNewTask(PageBase pageBase) {
        @NotNull List<CompiledObjectCollectionView> archetypes = pageBase.getCompiledGuiProfile().findAllApplicableArchetypeViews(
                WebComponentUtil.classToQName(PrismContext.get(), TaskType.class), OperationTypeType.ADD);
        archetypes.removeIf(archetype -> archetype.getCollection().getCollectionRef().getOid().equals(SystemObjectsType.ARCHETYPE_UTILITY_TASK.value())
                || archetype.getCollection().getCollectionRef().getOid().equals(SystemObjectsType.ARCHETYPE_SYSTEM_TASK.value()));
        return archetypes;
    }

    public static List<String> getReportArchetypesList() {
        return REPORT_ARCHETYPES;
    }

    public static List<String> getCertificationArchetypesList() {
        return CERTIFICATION_ARCHETYPES;
    }

    public static List<String> getUtilityArchetypesList() {
        return UTILITY_ARCHETYPES;
    }

    public static List<String> getSystemArchetypesList() {
        return SYSTEM_ARCHETYPES;
    }

    public static List<ObjectReferenceType> getArchetypeReferencesList(CompiledObjectCollectionView collectionView) {
        List<ObjectReferenceType> references = new ArrayList<>();
        references.addAll(ObjectCollectionViewUtil.getArchetypeReferencesList(collectionView));
        if (references.get(0) != null) {
            String oid = references.get(0).getOid();
            if (UTILITY_ARCHETYPES.contains(oid)) {
                references.add(new ObjectReferenceType()
                        .type(ArchetypeType.COMPLEX_TYPE)
                        .oid(SystemObjectsType.ARCHETYPE_UTILITY_TASK.value()));
            } else if (SYSTEM_ARCHETYPES.contains(oid)) {
                references.add(new ObjectReferenceType()
                        .type(ArchetypeType.COMPLEX_TYPE)
                        .oid(SystemObjectsType.ARCHETYPE_SYSTEM_TASK.value()));
            }
        }
        return references;
    }

    public static void addArchetypeReferencesList(List<ObjectReferenceType> references) {
        if (CollectionUtils.isEmpty(references)) {
            return;
        }
        if (references.get(0) != null) {
            String oid = references.get(0).getOid();
            if (UTILITY_ARCHETYPES.contains(oid)) {
                references.add(new ObjectReferenceType()
                        .type(ArchetypeType.COMPLEX_TYPE)
                        .oid(SystemObjectsType.ARCHETYPE_UTILITY_TASK.value()));
            } else if (SYSTEM_ARCHETYPES.contains(oid)) {
                references.add(new ObjectReferenceType()
                        .type(ArchetypeType.COMPLEX_TYPE)
                        .oid(SystemObjectsType.ARCHETYPE_SYSTEM_TASK.value()));
            }
        }
    }
}
