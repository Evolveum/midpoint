/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.certification.impl;

import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import static com.evolveum.midpoint.schema.util.CertCampaignTypeUtil.norm;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.toShortString;

import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.LocalizableMessageBuilder;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.cases.api.CaseManager;
import com.evolveum.midpoint.cases.api.util.PerformerCommentsFormatter;
import com.evolveum.midpoint.certification.api.OutcomeUtils;
import com.evolveum.midpoint.model.impl.lens.OperationalDataManager;
import com.evolveum.midpoint.model.impl.lens.OperationalDataManager.CertificationProcessMetadata;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * The task handler for writing assignment/object metadata.
 */
@Component
public class AccessCertificationClosingTaskHandler implements TaskHandler {

    private static final String HANDLER_URI = AccessCertificationConstants.NS_CERTIFICATION_TASK_PREFIX + "/closing/handler-3";

    private static final String CLASS_DOT = AccessCertificationClosingTaskHandler.class.getName() + ".";

    @Autowired private TaskManager taskManager;
    @Autowired private AccCertGeneralHelper helper;
    @Autowired private PrismContext prismContext;
    @Autowired private AccCertQueryHelper queryHelper;
    @Autowired private SystemObjectCache objectCache;
    @Autowired private CaseManager caseManager;
    @Autowired private OperationalDataManager operationalDataManager;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

    private static final Trace LOGGER = TraceManager.getTrace(AccessCertificationClosingTaskHandler.class);

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    @Override
    public TaskRunResult run(@NotNull RunningTask task) {
        LOGGER.debug("Task run starting");

        OperationResult opResult = task.getResult().createSubresult(CLASS_DOT+"run");
        opResult.setSummarizeSuccesses(true);
        TaskRunResult runResult = new TaskRunResult();

        String campaignOid = task.getObjectOid();
        if (campaignOid == null) {
            LOGGER.error("No campaign OID specified in the task");
            opResult.recordFatalError("No campaign OID specified in the task");
            runResult.setOperationResultStatus(OperationResultStatus.FATAL_ERROR);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        }

        opResult.addContext("campaignOid", campaignOid);

        AccessCertificationCampaignType campaign;
        List<AccessCertificationCaseType> caseList;
        PrismObject<SystemConfigurationType> systemConfigurationObject;
        try {
            campaign = helper.getCampaign(campaignOid, null, task, opResult);
            caseList = queryHelper.getAllCurrentIterationCases(campaignOid, norm(campaign.getIteration()), opResult);
            systemConfigurationObject = objectCache.getSystemConfiguration(opResult);
        } catch (ObjectNotFoundException|SchemaException e) {
            opResult.computeStatus();
            runResult.setOperationResultStatus(OperationResultStatus.FATAL_ERROR);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            LoggingUtils.logUnexpectedException(LOGGER, "Closing task couldn't start for campaign {} because of unexpected exception", e, campaignOid);
            return runResult;
        }

        PerformerCommentsFormattingType formatting = systemConfigurationObject != null &&
                systemConfigurationObject.asObjectable().getAccessCertification() != null ?
                systemConfigurationObject.asObjectable().getAccessCertification().getReviewerCommentsFormatting() : null;
        PerformerCommentsFormatter commentsFormatter = caseManager.createPerformerCommentsFormatter(formatting);
        RunContext runContext = new RunContext(task, commentsFormatter);
        caseList.forEach(aCase -> prepareMetadataDeltas(aCase, campaign, runContext, opResult));
        runContext.objectContextMap.forEach((oid, ctx) -> applyMetadataDeltas(ctx, runContext, opResult));

        opResult.computeStatus();
        runResult.setOperationResultStatus(OperationResultStatus.SUCCESS);
        runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
        LOGGER.debug("Task run stopping (campaign {})", toShortString(campaign));
        return runResult;
    }

    private void applyMetadataDeltas(ObjectContext objectCtx, RunContext runContext, OperationResult opResult) {
        ObjectType object = objectCtx.object;
        List<ItemDelta<?, ?>> deltas = objectCtx.modifications;
        try {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("### Updating {} with:\n{}", toShortString(object), DebugUtil.debugDump(deltas));
            }
            if (!deltas.isEmpty()) {
                repositoryService.modifyObject(object.getClass(), object.getOid(), deltas, opResult);
                runContext.task.incrementLegacyProgressAndStoreStatisticsIfTimePassed(opResult);
            }
        } catch (ObjectNotFoundException | SchemaException | ObjectAlreadyExistsException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't update certification metadata for {}", e, toShortString(object));
        }
    }

    private void prepareMetadataDeltas(
            AccessCertificationCaseType aCase, AccessCertificationCampaignType campaign,
            RunContext runContext, OperationResult result) {

        var processMetadata = createProcessMetadata(aCase, campaign, runContext, result);
        if (processMetadata == null) {
            return;
        }

        ObjectType object = null;

        try {
            // we count progress for each certification case and then for each object updated
            runContext.task.incrementLegacyProgressAndStoreStatisticsIfTimePassed(result);

            String objectOid = aCase.getObjectRef() != null ? aCase.getObjectRef().getOid() : null;
            if (objectOid == null) {
                LOGGER.error("No object OID in certification case {}: skipping metadata recording", aCase);
                return;
            }
            ObjectContext objectCtx = runContext.objectContextMap.get(objectOid);
            if (objectCtx == null) {
                QName objectType = defaultIfNull(aCase.getObjectRef().getType(), ObjectType.COMPLEX_TYPE);
                Class<? extends ObjectType> objectClass = ObjectTypes.getObjectTypeClass(objectType);
                try {
                    object = repositoryService.getObject(objectClass, objectOid, null, result).asObjectable();
                } catch (ObjectNotFoundException | SchemaException e) {
                    LoggingUtils.logUnexpectedException(LOGGER,
                            "Couldn't retrieve object {} {} to have its certification metadata updated",
                            e, objectClass.getSimpleName(), objectOid);
                    return;
                }
                objectCtx = new ObjectContext(object);
                runContext.objectContextMap.put(object.getOid(), objectCtx);
            } else {
                object = objectCtx.object;
            }

            if (aCase instanceof AccessCertificationAssignmentCaseType assignmentCase) {
                AssignmentType assignment = assignmentCase.getAssignment();
                if (assignment == null) {
                    LOGGER.error("No assignment/inducement in assignment-related certification case {}: "
                            + "skipping metadata recording", aCase);
                    return;
                } else if (assignment.getId() == null) {
                    LOGGER.error("Unidentified assignment/inducement in assignment-related certification case {}: {}: "
                            + "skipping metadata recording", aCase, assignment);
                    return;
                }
                var assignmentPath = ItemPath.create(
                        isTrue(assignmentCase.isIsInducement()) ? AbstractRoleType.F_INDUCEMENT : FocusType.F_ASSIGNMENT,
                        assignment.getId());
                //noinspection unchecked
                var assignmentPcv = (PrismContainerValue<AssignmentType>) objectCtx.object.asPrismObject().find(assignmentPath);
                if (assignmentPcv == null) {
                    LOGGER.debug("Assignment/inducement {} in {} does not exist. It might be already deleted e.g. by remediation.",
                            assignmentPath, object);
                    return;
                }
                objectCtx.modifications.addAll(
                        operationalDataManager.createAssignmentCertificationMetadataDeltas(
                                object.getClass(), assignmentPath, assignmentPcv, processMetadata));
            } else {
                objectCtx.modifications.addAll(
                        operationalDataManager.createObjectCertificationMetadataDeltas(
                                objectCtx.object, processMetadata));
            }
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't create certification metadata for {} {}", e, object);
            // continuing with other cases
        }
    }

    private CertificationProcessMetadata createProcessMetadata(
            AccessCertificationCaseType aCase, AccessCertificationCampaignType campaign,
            RunContext runContext, OperationResult result) {
        String outcome = aCase.getOutcome();
        if (OutcomeUtils.isNoneOrNotDecided(outcome)) {
            return null;
        }

        Set<ObjectReferenceType> certifiers = new HashSet<>();
        Set<String> comments = new HashSet<>();
        for (AccessCertificationWorkItemType workItem : aCase.getWorkItem()) {
            AbstractWorkItemOutputType output = workItem.getOutput();
            if (workItem.getPerformerRef() == null || output == null) {
                continue;
            }
            boolean commentNotEmpty = StringUtils.isNotEmpty(output.getComment());
            if (commentNotEmpty || !OutcomeUtils.isNoneOrNotDecided(output.getOutcome())) {
                certifiers.add(workItem.getPerformerRef().clone());
                String formattedComment = runContext.commentsFormatter.formatComment(workItem, runContext.task, result);
                if (StringUtils.isNotEmpty(formattedComment)) {
                    comments.add(formattedComment);
                }
            }
        }
        return new CertificationProcessMetadata(campaign.getEndTimestamp(), outcome, certifiers, comments);
    }

    @Override
    public Long heartbeat(Task task) {
        return null; // not to reset progress information
    }

    @Override
    public void refreshStatus(Task task) {
        // Do nothing. Everything is fresh already.
    }

    void launch(AccessCertificationCampaignType campaign, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException {

        LOGGER.debug("Launching closing task handler for campaign {} as asynchronous task", toShortString(campaign));

        OperationResult result = parentResult.createSubresult(CLASS_DOT + "launch");
        result.addParam("campaignOid", campaign.getOid());

        Task task = taskManager.createTaskInstance();
        task.setHandlerUri(HANDLER_URI);
        task.setName(new PolyStringType("Closing " + campaign.getName()));
        task.setObjectRef(ObjectTypeUtil.createObjectRef(campaign));
        task.setOwner(repositoryService.getObject(UserType.class, SystemObjectsType.USER_ADMINISTRATOR.value(), null, result));
        task.addArchetypeInformation(SystemObjectsType.ARCHETYPE_CERTIFICATION_TASK.value());
        taskManager.switchToBackground(task, result);
        result.setBackgroundTaskOid(task.getOid());
        if (result.isInProgress()) {
            result.recordStatus(OperationResultStatus.IN_PROGRESS, "Closing task "+task+" was successfully started, please use Server Tasks to see its status.");
            result.setUserFriendlyMessage(new LocalizableMessageBuilder()
                    .key("PageCertCampaign.taskName.closeCampaign")
                    .arg(campaign.getName())
                    .build());
        }

        LOGGER.trace("Closing task for {} switched to background, control thread returning with task {}", toShortString(campaign), task);
    }

    private static class ObjectContext {
        @NotNull final ObjectType object;
        @NotNull final List<ItemDelta<?, ?>> modifications = new ArrayList<>();

        ObjectContext(@NotNull ObjectType object) {
            this.object = object;
        }
    }

    private static class RunContext {
        final RunningTask task;
        final Map<String, ObjectContext> objectContextMap = new HashMap<>();
        final PerformerCommentsFormatter commentsFormatter;

        RunContext(RunningTask task, PerformerCommentsFormatter commentsFormatter) {
            this.task = task;
            this.commentsFormatter = commentsFormatter;
        }
    }
}
