/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.certification.impl;

import com.evolveum.midpoint.certification.api.OutcomeUtils;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.RepositoryService;
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
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.wf.util.PerformerCommentsFormatter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import java.util.*;

import static com.evolveum.midpoint.schema.util.CertCampaignTypeUtil.norm;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.toShortString;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * The task handler for writing assignment/object metadata.
 *
 * @author mederly
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
    @Autowired private WorkflowManager workflowManager;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

    private static final Trace LOGGER = TraceManager.getTrace(AccessCertificationClosingTaskHandler.class);

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    @NotNull
    @Override
    public StatisticsCollectionStrategy getStatisticsCollectionStrategy() {
        return new StatisticsCollectionStrategy()
                .fromZero();
        // implement iteration statistics when needed (along with expected total)
    }

    @Override
    public TaskRunResult run(RunningTask task, TaskPartitionDefinitionType partition) {
        LOGGER.info("Task run starting");

        OperationResult opResult = new OperationResult(CLASS_DOT+"run");
        opResult.setSummarizeSuccesses(true);
        TaskRunResult runResult = new TaskRunResult();
        runResult.setOperationResult(opResult);

        String campaignOid = task.getObjectOid();
        if (campaignOid == null) {
            LOGGER.error("No campaign OID specified in the task");
            opResult.recordFatalError("No campaign OID specified in the task");
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        }

        opResult.addContext("campaignOid", campaignOid);

        AccessCertificationCampaignType campaign;
        List<AccessCertificationCaseType> caseList;
        PrismObject<SystemConfigurationType> systemConfigurationObject;
        try {
            campaign = helper.getCampaign(campaignOid, null, task, opResult);
            caseList = queryHelper.getAllCurrentIterationCases(campaignOid, norm(campaign.getIteration()), null, opResult);
            systemConfigurationObject = objectCache.getSystemConfiguration(opResult);
        } catch (ObjectNotFoundException|SchemaException e) {
            opResult.computeStatus();
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            LoggingUtils.logUnexpectedException(LOGGER, "Closing task couldn't start for campaign {} because of unexpected exception", e, campaignOid);
            return runResult;
        }

        PerformerCommentsFormattingType formatting = systemConfigurationObject != null &&
                systemConfigurationObject.asObjectable().getAccessCertification() != null ?
                systemConfigurationObject.asObjectable().getAccessCertification().getReviewerCommentsFormatting() : null;
        PerformerCommentsFormatter commentsFormatter = workflowManager.createPerformerCommentsFormatter(formatting);
        RunContext runContext = new RunContext(task, commentsFormatter);
        caseList.forEach(aCase -> prepareMetadataDeltas(aCase, campaign, runContext, opResult));
        runContext.objectContextMap.forEach((oid, ctx) -> applyMetadataDeltas(ctx, runContext, opResult));

        opResult.computeStatus();
        runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
        LOGGER.info("Task run stopping (campaign {})", toShortString(campaign));
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
                runContext.task.incrementProgressAndStoreStatsIfNeeded();
            }
        } catch (ObjectNotFoundException | SchemaException | ObjectAlreadyExistsException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't update certification metadata for {}", e, toShortString(object));
        }
    }

    private void prepareMetadataDeltas(AccessCertificationCaseType aCase, AccessCertificationCampaignType campaign,
            RunContext runContext, OperationResult result) {

        // we count progress for each certification case and then for each object updated
        runContext.task.incrementProgressAndStoreStatsIfNeeded();

        String objectOid = aCase.getObjectRef() != null ? aCase.getObjectRef().getOid() : null;
        if (objectOid == null) {
            LOGGER.error("No object OID in certification case {}: skipping metadata recording", aCase);
            return;
        }
        ObjectContext objectCtx = runContext.objectContextMap.get(objectOid);
        if (objectCtx == null) {
            QName objectType = defaultIfNull(aCase.getObjectRef().getType(), ObjectType.COMPLEX_TYPE);
            Class<? extends ObjectType> objectClass = ObjectTypes.getObjectTypeClass(objectType);
            PrismObject<? extends ObjectType> object;
            try {
                object = repositoryService.getObject(objectClass, objectOid, null, result);
            } catch (ObjectNotFoundException|SchemaException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't retrieve object {} {} to have its certification metadata updated", e, objectClass.getSimpleName(), objectOid);
                return;
            }
            objectCtx = new ObjectContext(object.asObjectable());
            runContext.objectContextMap.put(object.getOid(), objectCtx);
        }

        ItemPath pathPrefix;
        if (aCase instanceof AccessCertificationAssignmentCaseType) {
            AccessCertificationAssignmentCaseType assignmentCase = (AccessCertificationAssignmentCaseType) aCase;
            AssignmentType assignment = assignmentCase.getAssignment();
            if (assignment == null) {
                LOGGER.error("No assignment/inducement in assignment-related certification case {}: skipping metadata recording", aCase);
                return;
            } else if (assignment.getId() == null) {
                LOGGER.error("Unidentified assignment/inducement in assignment-related certification case {}: {}: skipping metadata recording", aCase, assignment);
                return;
            }
            QName root = Boolean.TRUE.equals(assignmentCase.isIsInducement()) ? AbstractRoleType.F_INDUCEMENT : FocusType.F_ASSIGNMENT;
            ItemPath assignmentPath = ItemPath.create(root, assignment.getId());
            if (objectCtx.object.asPrismObject().find(assignmentPath) == null) {
                LOGGER.debug("Assignment/inducement {} in {} does not exist. It might be already deleted e.g. by remediation.",
                        assignmentPath, toShortString(objectCtx.object));
                return;
            }
            pathPrefix = assignmentPath.append(AssignmentType.F_METADATA);
        } else {
            pathPrefix = ObjectType.F_METADATA;
        }

        try {
            objectCtx.modifications.addAll(createMetadataDeltas(aCase, campaign, objectCtx.object.getClass(), pathPrefix, runContext, result));
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't create certification metadata for {} {}", e, toShortString(objectCtx.object));
        }
    }

    private List<ItemDelta<?, ?>> createMetadataDeltas(AccessCertificationCaseType aCase,
            AccessCertificationCampaignType campaign, Class<? extends ObjectType> objectClass, ItemPath pathPrefix,
            RunContext runContext, OperationResult result) throws SchemaException {
        String outcome = aCase.getOutcome();
        if (OutcomeUtils.isNoneOrNotDecided(outcome)) {
            return emptyList();
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
        return prismContext.deltaFor(objectClass)
                .item(pathPrefix.append(MetadataType.F_CERTIFICATION_FINISHED_TIMESTAMP)).replace(campaign.getEndTimestamp())
                .item(pathPrefix.append(MetadataType.F_CERTIFICATION_OUTCOME)).replace(outcome)
                .item(pathPrefix.append(MetadataType.F_CERTIFIER_REF)).replaceRealValues(certifiers)
                .item(pathPrefix.append(MetadataType.F_CERTIFIER_COMMENT)).replaceRealValues(comments)
                .asItemDeltas();
    }

    @Override
    public Long heartbeat(Task task) {
        return null;    // not to reset progress information
    }

    @Override
    public void refreshStatus(Task task) {
        // Do nothing. Everything is fresh already.
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.ACCESS_CERTIFICATION;
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_CERTIFICATION_TASK.value();
    }

    public void launch(AccessCertificationCampaignType campaign, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {

        LOGGER.debug("Launching closing task handler for campaign {} as asynchronous task", toShortString(campaign));

        OperationResult result = parentResult.createSubresult(CLASS_DOT + "launch");
        result.addParam("campaignOid", campaign.getOid());

        Task task = taskManager.createTaskInstance();
        task.setHandlerUri(HANDLER_URI);
        task.setName(new PolyStringType("Closing " + campaign.getName()));
        task.setObjectRef(ObjectTypeUtil.createObjectRef(campaign, prismContext));
        task.setOwner(repositoryService.getObject(UserType.class, SystemObjectsType.USER_ADMINISTRATOR.value(), null, result));
        taskManager.switchToBackground(task, result);
        result.setBackgroundTaskOid(task.getOid());
        if (result.isInProgress()) {
            result.recordStatus(OperationResultStatus.IN_PROGRESS, "Closing task "+task+" was successfully started, please use Server Tasks to see its status.");
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
