/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.integrity;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.impl.sync.SynchronizationService;
import com.evolveum.midpoint.model.impl.tasks.AbstractSearchIterativeModelTaskHandler;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeTaskExecution;
import com.evolveum.midpoint.repo.common.task.PartExecutionClass;
import com.evolveum.midpoint.repo.common.task.TaskExecutionClass;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskWorkBucketProcessingResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Task handler for "Shadow integrity check" task.
 *
 * The purpose of this task is to detect and optionally delete duplicate shadows, i.e. distinct shadows that
 * correspond to the same resource object.
 *
 *  * Task handler for "Normalize attribute/property data" task.
 *
 * The purpose of this task is to normalize data stored in repository when the corresponding matching rule changes
 * (presumably from non-normalizing to normalizing one, e.g. from case sensitive to case insensitive).
 *
 * The reason is that if the data in the repository would be stored in non-normalized form, the would be
 * effectively hidden for any search on that particular attribute.
 */
@Component
@TaskExecutionClass(ShadowIntegrityCheckTaskHandler.TaskExecution.class)
@PartExecutionClass(ShadowIntegrityCheckTaskPartExecution.class)
public class ShadowIntegrityCheckTaskHandler
        extends AbstractSearchIterativeModelTaskHandler
        <ShadowIntegrityCheckTaskHandler, ShadowIntegrityCheckTaskHandler.TaskExecution> {

    public static final String HANDLER_URI = ModelPublicConstants.SHADOW_INTEGRITY_CHECK_TASK_HANDLER_URI;

    public ShadowIntegrityCheckTaskHandler() {
        super("Shadow integrity check", OperationConstants.CHECK_SHADOW_INTEGRITY);
        reportingOptions.setPreserveStatistics(false);
        reportingOptions.setLogErrors(false); // we do log errors ourselves
        reportingOptions.setSkipWritingOperationExecutionRecords(true); // because of performance
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.UTIL;
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();
    }

    /** Just to make Java compiler happy. */
    public static class TaskExecution
            extends AbstractSearchIterativeTaskExecution<ShadowIntegrityCheckTaskHandler, ShadowIntegrityCheckTaskHandler.TaskExecution> {

        public TaskExecution(ShadowIntegrityCheckTaskHandler taskHandler,
                RunningTask localCoordinatorTask, WorkBucketType workBucket,
                TaskPartitionDefinitionType partDefinition,
                TaskWorkBucketProcessingResult previousRunResult) {
            super(taskHandler, localCoordinatorTask, workBucket, partDefinition, previousRunResult);
        }
    }
}
