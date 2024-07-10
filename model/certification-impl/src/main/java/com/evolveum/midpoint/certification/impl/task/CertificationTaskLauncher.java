/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.certification.impl.task;

import com.evolveum.midpoint.model.impl.lens.tasks.TaskOperationalDataManager;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Launches certification activity tasks.
 */
@Component
public class CertificationTaskLauncher {

    @Autowired private TaskManager taskManager;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private TaskOperationalDataManager taskOperationalDataManager;

    private static final Trace LOGGER = TraceManager.getTrace(CertificationTaskLauncher.class);

    public void startRemediationAsynchronousTask(AccessCertificationCampaignType campaign, OperationResult parentResult) {
        LOGGER.info("Launching remediation task handler for campaign {} as asynchronous task", ObjectTypeUtil.toShortString(campaign));

        OperationResult result = parentResult.createSubresult("launch remediation task");
        result.addParam("campaignOid", campaign.getOid());

        Task task = taskManager.createTaskInstance();

        try {
            ActivityDefinitionType activityDef = new ActivityDefinitionType();
            activityDef.beginWork()
                    .beginCertificationRemediation()
                    .certificationCampaignRef(campaign.getOid(), AccessCertificationCampaignType.COMPLEX_TYPE);
            task.setRootActivityDefinition(activityDef);
            task.flushPendingModifications(result);

            // Readable task name
            PolyStringType polyString = new PolyStringType("Remediation for " + campaign.getName());
            task.setName(polyString);

            task.setOwner(repositoryService.getObject(UserType.class, SystemObjectsType.USER_ADMINISTRATOR.value(), null, result));

        } catch (ObjectNotFoundException e) {
            LOGGER.error("Task object not found, expecting it to exist (task {})", task, e);
            result.recordFatalError("Task object not found", e);
            throw new IllegalStateException("Task object not found, expecting it to exist", e);
        } catch (ObjectAlreadyExistsException e) {
            LOGGER.error("Task object wasn't updated (task {})", task, e);
            result.recordFatalError("Task object wasn't updated", e);
            throw new IllegalStateException("Task object wasn't updated", e);
        } catch (SchemaException e) {
            LOGGER.error("Error dealing with schema (task {})", task, e);
            result.recordFatalError("Error dealing with schema", e);
            throw new IllegalStateException("Error dealing with schema", e);
        }

        task.addArchetypeInformation(SystemObjectsType.ARCHETYPE_CERTIFICATION_REMEDIATION_TASK.value());

        taskOperationalDataManager.updateOnElementAdd(task.getUpdatedTaskObject().asObjectable(), result);

        taskManager.switchToBackground(task, result);
        result.setBackgroundTaskOid(task.getOid());
        if (result.isInProgress()) {
            result.recordStatus(OperationResultStatus.IN_PROGRESS, "Remediation task "+task+" was successfully started, please use Server Tasks to see its status.");
        }

        LOGGER.trace("Remediation for {} switched to background, control thread returning with task {}", ObjectTypeUtil.toShortString(campaign), task);
    }
}
