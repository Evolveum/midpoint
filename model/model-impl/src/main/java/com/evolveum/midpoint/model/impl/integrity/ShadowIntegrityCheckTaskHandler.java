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
import com.evolveum.midpoint.model.impl.util.AbstractSearchIterativeModelTaskHandler;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;

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

 *
 * @author Pavol Mederly
 */
@Component
public class ShadowIntegrityCheckTaskHandler extends AbstractSearchIterativeModelTaskHandler<ShadowType, ShadowIntegrityCheckResultHandler> {

    public static final String HANDLER_URI = ModelPublicConstants.SHADOW_INTEGRITY_CHECK_TASK_HANDLER_URI;

    @Autowired private ProvisioningService provisioningService;
    @Autowired private MatchingRuleRegistry matchingRuleRegistry;
    @Autowired private SynchronizationService synchronizationService;
    @Autowired private SystemObjectCache systemObjectCache;

    public ShadowIntegrityCheckTaskHandler() {
        super("Shadow integrity check", OperationConstants.CHECK_SHADOW_INTEGRITY);
        setLogFinishInfo(true);
        setPreserveStatistics(false);
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    @Override
    protected ShadowIntegrityCheckResultHandler createHandler(TaskPartitionDefinitionType partition, TaskRunResult runResult,
            RunningTask coordinatorTask, OperationResult opResult) {
        return new ShadowIntegrityCheckResultHandler(coordinatorTask, ShadowIntegrityCheckTaskHandler.class.getName(),
                "check shadow integrity", "check shadow integrity", taskManager, prismContext, provisioningService,
                matchingRuleRegistry, repositoryService, synchronizationService, systemObjectCache);
    }

    @Override
    protected Class<? extends ObjectType> getType(Task task) {
        return ShadowType.class;
    }

    @Override
    protected boolean requiresDirectRepositoryAccess(ShadowIntegrityCheckResultHandler resultHandler, TaskRunResult runResult,
            Task coordinatorTask, OperationResult opResult) {
        return true;
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.UTIL;
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();
    }
}
