/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.integrity.shadows;

import static com.evolveum.midpoint.model.impl.integrity.shadows.ShadowIntegrityCheckItemProcessor.KEY_OWNERS;

import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.SearchBasedActivityRun;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityReportingCharacteristics;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public final class ShadowIntegrityCheckActivityRun
        extends SearchBasedActivityRun
        <ShadowType, ShadowIntegrityCheckWorkDefinition, ShadowIntegrityCheckActivityHandler, AbstractActivityWorkStateType> {

    private ShadowCheckConfiguration configuration;
    private WorkingState workingState;
    private ShadowIntegrityCheckItemProcessor itemProcessor;

    private static final Trace LOGGER = TraceManager.getTrace(ShadowIntegrityCheckActivityRun.class);

    ShadowIntegrityCheckActivityRun(
            @NotNull ActivityRunInstantiationContext<ShadowIntegrityCheckWorkDefinition, ShadowIntegrityCheckActivityHandler> context) {
        super(context, "Shadow integrity check");
        setInstanceReady();
    }

    @Override
    public boolean doesRequireDirectRepositoryAccess() {
        return true;
    }

    @Override
    public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
        return super.createReportingCharacteristics()
                .actionsExecutedStatisticsSupported(true)
                .logErrors(false) // we do log errors ourselves
                .skipWritingOperationExecutionRecords(true); // because of performance
    }

    @Override
    public boolean beforeRun(OperationResult result)
            throws CommonException, ActivityRunException {
        if (!super.beforeRun(result)) {
            return false;
        }

        ensureNoWorkerThreads();
        ensureNoPreviewNorDryRun();

        ExecutionModeType executionMode = getActivityDefinition().getExecutionMode();
        configuration = new ShadowCheckConfiguration(LOGGER, getWorkDefinition(), executionMode);
        workingState = new WorkingState();
        configuration.log("Shadow integrity check is starting with the configuration:");

        itemProcessor = new ShadowIntegrityCheckItemProcessor(this);
        return true;
    }

    @Override
    public boolean processItem(@NotNull ShadowType shadow,
            @NotNull ItemProcessingRequest<ShadowType> request, RunningTask workerTask, OperationResult result)
            throws CommonException, ActivityRunException {
        return itemProcessor.processObject(shadow.asPrismObject(), workerTask, result);
    }

    private static class WorkingState {
        private final Map<ContextMapKey, ObjectTypeContext> contextMap = new HashMap<>();

        private final Map<String, PrismObject<ResourceType>> resources = new HashMap<>();

        private final ShadowStatistics statistics = new ShadowStatistics();

        private final Set<String> duplicateShadowsDetected = new HashSet<>();
        private final Set<String> duplicateShadowsDeleted = new HashSet<>();
    }

    @Override
    public void afterRun(OperationResult result) throws SchemaException {

        String uniquenessReport;
        if (configuration.checkUniqueness) {
            uniquenessReport = reportOrFixUniqueness(result);
        } else {
            uniquenessReport = null;
        }

        configuration.log("Shadow integrity check finished. It was run with the configuration:");
        ShadowStatistics stats = workingState.statistics;
        LOGGER.info("Results:\n" +
                        "    Shadows processed: {} ({} resources),\n" +
                        "    Shadows with no problems: {}\n" +
                        "    Shadows with warnings: {}\n" +
                        "    Shadows with errors: {}\n" +
                        "    Details:\n{}",
                stats.getShadows(), stats.getResources(),
                stats.getShadows() - stats.getShadowsWithErrors() - stats.getShadowsWithWarnings(),
                stats.getShadowsWithWarnings(), stats.getShadowsWithErrors(),
                stats.getDetailsFormatted(configuration.dryRun));

        if (uniquenessReport != null) {
            LOGGER.info("Uniqueness report:\n{}", uniquenessReport);
        }
    }

    private String reportOrFixUniqueness(OperationResult result) {

        ShadowCheckConfiguration cfg = getConfiguration();

        StringBuilder details = new StringBuilder();
        StringBuilder stat = new StringBuilder();

        for (Map.Entry<ContextMapKey, ObjectTypeContext> entry : workingState.contextMap.entrySet()) {
            String resourceOid = entry.getKey().resourceOid;
            QName objectClassName = entry.getKey().objectClassName;
            ObjectTypeContext ctx = entry.getValue();
            PrismObject<ResourceType> resource = workingState.resources.get(resourceOid);
            if (resource == null) {
                LOGGER.error("No resource for {}", resourceOid);        // should not happen
                continue;
            }
            for (Map.Entry<QName, Map<String, Set<String>>> idValEntry : ctx.getIdentifierValueMap().entrySet()) {
                QName identifier = idValEntry.getKey();
                boolean first = true;
                for (Map.Entry<String, Set<String>> valListEntry : idValEntry.getValue().entrySet()) {
                    Set<String> shadowsOids = valListEntry.getValue();
                    if (shadowsOids.size() <= 1) {
                        continue;
                    }
                    if (first) {
                        details.append("Duplicates for ").append(ObjectTypeUtil.toShortString(resource));
                        details.append(", object class = ").append(objectClassName);
                        details.append(", identifier = ").append(identifier).append(":\n");
                        first = false;
                    }
                    details.append(" - value: ").append(valListEntry.getKey()).append(", shadows: ").append(shadowsOids.size()).append("\n");
                    List<PrismObject<ShadowType>> shadowsToConsider = new ArrayList<>();
                    for (String shadowOid : shadowsOids) {
                        PrismObject<ShadowType> shadow = null;
                        try {
                            shadow = getRepositoryService().getObject(ShadowType.class, shadowOid, null, result);
                        } catch (ObjectNotFoundException e) {
                            LOGGER.debug("Couldn't fetch shadow with OID {}, it was probably already deleted", shadowOid, e);
                        } catch (SchemaException e) {
                            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't fetch shadow with OID {} from the repository", e, shadowOid);
                            continue;
                        }
                        details.append("   - ").append(shadow != null ? ObjectTypeUtil.toShortString(shadow) : shadowOid);
                        if (shadow != null) {
                            details.append("; sync situation = ").append(shadow.asObjectable().getSynchronizationSituation()).append("\n");
                            PrismContainer<ShadowAttributesType> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
                            if (attributesContainer != null && !attributesContainer.isEmpty()) {
                                for (Item<?, ?> item : attributesContainer.getValue().getItems()) {
                                    details.append("     - ").append(item.getElementName().getLocalPart()).append(" = ");
                                    details.append(item.getRealValues());
                                    details.append("\n");
                                }
                            }
                        }
                        if (workingState.duplicateShadowsDeleted.contains(shadowOid)) {
                            details.append("     (already deleted)\n");
                        } else if (shadow == null) {
                            details.append("     (inaccessible)\n");
                        } else {
                            shadowsToConsider.add(shadow);
                        }
                    }

                    if (cfg.fixUniqueness && shadowsToConsider.size() > 1) {
                        DuplicateShadowsTreatmentInstruction instruction = cfg.duplicateShadowsResolver.determineDuplicateShadowsTreatment(shadowsToConsider);
                        deleteShadows(instruction, details, getRunningTask(), result);
                    }
                }
            }
        }
        stat.append("Duplicate shadows detected: ").append(workingState.duplicateShadowsDetected.size());
        if (cfg.fixUniqueness) {
            stat.append(", deleted: ").append(workingState.duplicateShadowsDeleted.size());
            // TODO report the duplicates that remain
        }

        result.summarize(); // there can be many 'search owner' subresults

        return stat + "\n" + details;
    }

    // shadowsToDelete do not contain 'already deleted shadows'
    private void deleteShadows(DuplicateShadowsTreatmentInstruction instruction, StringBuilder sb, Task task, OperationResult result) {

        ShadowCheckConfiguration cfg = getConfiguration();

        LOGGER.trace("Going to delete shadows:\n{}", instruction);
        if (instruction == null || instruction.getShadowsToDelete() == null) {
            return;
        }
        Collection<PrismObject<ShadowType>> shadowsToDelete = instruction.getShadowsToDelete();
        String shadowOidToReplaceDeleted = instruction.getShadowOidToReplaceDeletedOnes();

        for (PrismObject<ShadowType> shadowToDelete : shadowsToDelete) {
            LOGGER.info("Deleting redundant shadow{} {}", skippedForDryRun(), ObjectTypeUtil.toShortString(shadowToDelete));
            sb.append("   --> deleted redundant shadow").append(skippedForDryRun()).append(" ").append(ObjectTypeUtil.toShortString(shadowToDelete)).append("\n");
            String oid = shadowToDelete.getOid();

            List<PrismObject<FocusType>> owners;
            if (cfg.checkOwners) {
                owners = shadowToDelete.getUserData(KEY_OWNERS);
            } else {
                owners = searchOwners(shadowToDelete, result);
            }

            if (!cfg.dryRun) {
                try {
                    getRepositoryService().deleteObject(ShadowType.class, oid, result);
                    task.recordObjectActionExecuted(shadowToDelete, ChangeType.DELETE, null);
                    workingState.duplicateShadowsDeleted.add(oid);
                } catch (ObjectNotFoundException e) {
                    // suspicious, but not a big deal
                    task.recordObjectActionExecuted(shadowToDelete, ChangeType.DELETE, e);
                    LoggingUtils.logExceptionAsWarning(LOGGER, "Shadow {} couldn't be deleted, because it does not exist anymore", e, ObjectTypeUtil.toShortString(shadowToDelete));
                    continue;
                } catch (RuntimeException e) {
                    task.recordObjectActionExecuted(shadowToDelete, ChangeType.DELETE, e);
                    LoggingUtils.logUnexpectedException(LOGGER, "Shadow {} couldn't be deleted because of an unexpected exception", e, ObjectTypeUtil.toShortString(shadowToDelete));
                    continue;
                }
            }

            if (owners == null || owners.isEmpty()) {
                continue;
            }

            for (PrismObject<FocusType> owner : owners) {
                List<ItemDelta<?, ?>> modifications = new ArrayList<>(2);
                ReferenceDelta deleteDelta = PrismContext.get().deltaFactory().reference().createModificationDelete(FocusType.F_LINK_REF, owner.getDefinition(),
                        PrismContext.get().itemFactory().createReferenceValue(oid, ShadowType.COMPLEX_TYPE));
                modifications.add(deleteDelta);
                if (shadowOidToReplaceDeleted != null) {
                    ReferenceDelta addDelta = PrismContext.get().deltaFactory().reference().createModificationAdd(FocusType.F_LINK_REF, owner.getDefinition(),
                            PrismContext.get().itemFactory().createReferenceValue(shadowOidToReplaceDeleted, ShadowType.COMPLEX_TYPE));
                    modifications.add(addDelta);
                }
                LOGGER.info("Executing modify delta{} for owner {}:\n{}", skippedForDryRun(), ObjectTypeUtil.toShortString(owner), DebugUtil.debugDump(modifications));
                if (!cfg.dryRun) {
                    try {
                        getRepositoryService().modifyObject(owner.asObjectable().getClass(), owner.getOid(), modifications, result);
                        task.recordObjectActionExecuted(owner, ChangeType.MODIFY, null);
                    } catch (ObjectNotFoundException | SchemaException | ObjectAlreadyExistsException | RuntimeException e) {
                        task.recordObjectActionExecuted(owner, ChangeType.MODIFY, e);
                        LoggingUtils.logUnexpectedException(LOGGER, "Focal object {} (owner of {}) couldn't be updated", e, ObjectTypeUtil.toShortString(owner),
                                ObjectTypeUtil.toShortString(shadowToDelete));
                    }
                }
            }
        }
    }

    List<PrismObject<FocusType>> searchOwners(PrismObject<ShadowType> shadow, OperationResult result) {
        try {
            ObjectQuery ownerQuery = PrismContext.get().queryFor(FocusType.class)
                    .item(FocusType.F_LINK_REF).ref(shadow.getOid())
                    .build();
            List<PrismObject<FocusType>> owners = getRepositoryService().searchObjects(FocusType.class, ownerQuery, null, result);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Owners for {}: {}", ObjectTypeUtil.toShortString(shadow), owners);
            }
            return owners;
        } catch (SchemaException|RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't create/execute owners query for shadow {}", e, ObjectTypeUtil.toShortString(shadow));
            return null;
        }
    }

    private RepositoryService getRepositoryService() {
        return getBeans().repositoryService;
    }

    ShadowStatistics getStatistics() {
        return workingState.statistics;
    }

    public ShadowCheckConfiguration getConfiguration() {
        return configuration;
    }

    PrismObject<ResourceType> getCachedResource(String resourceOid) {
        return workingState.resources.get(resourceOid);
    }

    public void cacheResource(PrismObject<ResourceType> resource) {
        workingState.resources.put(resource.getOid(), resource);
    }

    String skippedForDryRun() {
        if (configuration.dryRun) {
            return " (skipped because of dry run)";
        } else {
            return "";
        }
    }

    void duplicateShadowDetected(String oid) {
        workingState.duplicateShadowsDetected.add(oid);
    }

    public ObjectTypeContext getObjectTypeContext(ContextMapKey key) {
        return workingState.contextMap.get(key);
    }

    public void putObjectTypeContext(ContextMapKey key, ObjectTypeContext context) {
        workingState.contextMap.put(key, context);
    }
}
