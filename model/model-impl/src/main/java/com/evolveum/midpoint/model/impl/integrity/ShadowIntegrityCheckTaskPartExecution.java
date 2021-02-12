/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.integrity;

import com.evolveum.midpoint.model.impl.tasks.AbstractIterativeModelTaskPartExecution;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.repo.common.task.HandledObjectType;
import com.evolveum.midpoint.repo.common.task.ItemProcessorClass;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskException;
import com.evolveum.midpoint.task.api.TaskUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import javax.xml.namespace.QName;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static com.evolveum.midpoint.model.impl.integrity.ShadowIntegrityCheckItemProcessor.KEY_OWNERS;

@ItemProcessorClass(ShadowIntegrityCheckItemProcessor.class)
@HandledObjectType(ShadowType.class)
public class ShadowIntegrityCheckTaskPartExecution
        extends AbstractIterativeModelTaskPartExecution
        <ShadowType,
                ShadowIntegrityCheckTaskHandler,
                ShadowIntegrityCheckTaskHandler.TaskExecution,
                ShadowIntegrityCheckTaskPartExecution,
                ShadowIntegrityCheckItemProcessor> {

    private final PrismContext prismContext;

    private Configuration configuration;
    private WorkingState workingState;

    public ShadowIntegrityCheckTaskPartExecution(ShadowIntegrityCheckTaskHandler.TaskExecution taskExecution) {
        super(taskExecution);
        setRequiresDirectRepositoryAccess();

        this.prismContext = getTaskHandler().getPrismContext();
    }

    @Override
    protected void initialize(OperationResult opResult)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException, TaskException {
        super.initialize(opResult);
        ensureNoWorkerThreads();

        configuration = new Configuration(localCoordinatorTask);
        workingState = new WorkingState();
        configuration.log("Shadow integrity check is starting with the configuration:");
    }

    private static final String INTENTS = "intents";
    private static final String UNIQUENESS = "uniqueness";
    private static final String NORMALIZATION = "normalization";
    private static final String OWNERS = "owners";
    private static final String FETCH = "fetch";
    private static final String EXTRA_DATA = "extraData";
    private static final String RESOURCE_REF = "resourceRef";
    private static final List<String> KNOWN_KEYS =
            Arrays.asList(INTENTS, UNIQUENESS, NORMALIZATION, OWNERS, FETCH, EXTRA_DATA, RESOURCE_REF);

    private static final String DEFAULT_DUPLICATE_SHADOWS_RESOLVER_CLASS_NAME = DefaultDuplicateShadowsResolver.class.getName();

    class Configuration {

        // derived from task extension diagnose/fix values at instantiation
        boolean checkIntents;
        boolean checkUniqueness;
        boolean checkNormalization;
        final boolean checkFetch;
        final boolean checkOwners;
        boolean checkExtraData;
        final boolean fixIntents;
        final boolean fixUniqueness;
        final boolean fixNormalization;
        final boolean fixExtraData;
        final boolean fixResourceRef;

        boolean checkDuplicatesOnPrimaryIdentifiersOnly = false;

        final boolean dryRun;

        DuplicateShadowsResolver duplicateShadowsResolver;

        private Configuration(Task coordinatorTask) {
            PrismProperty<String> diagnosePrismProperty = coordinatorTask.getExtensionPropertyOrClone(SchemaConstants.MODEL_EXTENSION_DIAGNOSE);
            if (diagnosePrismProperty == null || diagnosePrismProperty.isEmpty()) {
                checkIntents = true;
                checkUniqueness = true;
                checkNormalization = true;
                checkOwners = true;
                checkFetch = false;
                checkExtraData = true;
            } else {
                checkIntents = contains(diagnosePrismProperty, INTENTS);
                checkUniqueness = contains(diagnosePrismProperty, UNIQUENESS);
                checkNormalization = contains(diagnosePrismProperty, NORMALIZATION);
                checkOwners = contains(diagnosePrismProperty, OWNERS);
                checkFetch = contains(diagnosePrismProperty, FETCH);
                checkExtraData = contains(diagnosePrismProperty, EXTRA_DATA);
                checkProperty(diagnosePrismProperty);
            }
            PrismProperty<String> fixPrismProperty = coordinatorTask.getExtensionPropertyOrClone(SchemaConstants.MODEL_EXTENSION_FIX);
            if (fixPrismProperty == null || fixPrismProperty.isEmpty()) {
                fixIntents = false;
                fixUniqueness = false;
                fixNormalization = false;
                fixExtraData = false;
                fixResourceRef = false;
            } else {
                fixIntents = contains(fixPrismProperty, INTENTS);
                fixUniqueness = contains(fixPrismProperty, UNIQUENESS);
                fixNormalization = contains(fixPrismProperty, NORMALIZATION);
                fixExtraData = contains(fixPrismProperty, EXTRA_DATA);
                fixResourceRef = contains(fixPrismProperty, RESOURCE_REF);
                checkProperty(fixPrismProperty);
            }

            if (fixIntents) {
                checkIntents = true;
            }
            if (fixUniqueness) {
                checkUniqueness = true;
            }
            if (fixNormalization) {
                checkNormalization = true;
            }
            if (fixExtraData) {
                checkExtraData = true;
            }

            if (fixUniqueness) {
                PrismProperty<String> duplicateShadowsResolverClass = coordinatorTask.getExtensionPropertyOrClone(SchemaConstants.MODEL_EXTENSION_DUPLICATE_SHADOWS_RESOLVER);
                String duplicateShadowsResolverClassName;
                if (duplicateShadowsResolverClass != null) {
                    duplicateShadowsResolverClassName = duplicateShadowsResolverClass.getRealValue();
                } else {
                    duplicateShadowsResolverClassName = DEFAULT_DUPLICATE_SHADOWS_RESOLVER_CLASS_NAME;
                }
                try {
                    duplicateShadowsResolver = (DuplicateShadowsResolver) Class.forName(duplicateShadowsResolverClassName)
                            .getDeclaredConstructor().newInstance();
                } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | ClassCastException
                        | NoSuchMethodException | InvocationTargetException e) {
                    throw new SystemException("Couldn't instantiate duplicate shadows resolver " + duplicateShadowsResolverClassName);
                }
            }

            PrismProperty<Boolean> checkDuplicatesOnPrimaryIdentifiersOnlyProperty = coordinatorTask.getExtensionPropertyOrClone(SchemaConstants.MODEL_EXTENSION_CHECK_DUPLICATES_ON_PRIMARY_IDENTIFIERS_ONLY);
            if (checkDuplicatesOnPrimaryIdentifiersOnlyProperty != null && checkDuplicatesOnPrimaryIdentifiersOnlyProperty.getRealValue() != null) {
                checkDuplicatesOnPrimaryIdentifiersOnly = checkDuplicatesOnPrimaryIdentifiersOnlyProperty.getRealValue();
            }

            try {
                dryRun = TaskUtil.isDryRun(coordinatorTask);
            } catch (SchemaException e) {
                throw new SystemException("Couldn't get dryRun flag from task " + coordinatorTask);
            }

        }

        private void log(String state) {
            getLogger().info("{}\n" +
                            "- normalization  diagnose={},\tfix={}\n" +
                            "- uniqueness     diagnose={},\tfix={} (primary identifiers only = {})\n" +
                            "- intents        diagnose={},\tfix={}\n" +
                            "- extraData      diagnose={},\tfix={}\n" +
                            "- owners         diagnose={}\n" +
                            "- fetch          diagnose={}\n" +
                            "- resourceRef    fix={}\n\n" +
                            "dryRun = {}\n",
                    state,
                    checkNormalization, fixNormalization,
                    checkUniqueness, fixUniqueness, checkDuplicatesOnPrimaryIdentifiersOnly,
                    checkIntents, fixIntents,
                    checkExtraData, fixExtraData,
                    checkOwners,
                    checkFetch,
                    fixResourceRef,
                    dryRun);
        }

        private void checkProperty(PrismProperty<String> property) {
            for (PrismPropertyValue<String> value : property.getValues()) {
                if (!KNOWN_KEYS.contains(value.getValue())) {
                    throw new IllegalArgumentException("Unknown diagnose/fix keyword: " + value.getValue() + ". Known keys are: " + KNOWN_KEYS);
                }
            }
        }

        private boolean contains(PrismProperty<String> property, String keyword) {
            return property.getRealValues().contains(keyword);
        }
    }

    static class WorkingState {
        private final Map<ContextMapKey, ObjectTypeContext> contextMap = new HashMap<>();

        private final Map<String, PrismObject<ResourceType>> resources = new HashMap<>();

        private final ShadowStatistics statistics = new ShadowStatistics();

        private final Set<String> duplicateShadowsDetected = new HashSet<>();
        private final Set<String> duplicateShadowsDeleted = new HashSet<>();
    }

    @Override
    protected void finish(OperationResult opResult) throws SchemaException {
        super.finish(opResult);

        String uniquenessReport;
        if (configuration.checkUniqueness) {
            uniquenessReport = reportOrFixUniqueness(opResult);
        } else {
            uniquenessReport = null;
        }

        configuration.log("Shadow integrity check finished. It was run with the configuration:");
        ShadowStatistics stats = workingState.statistics;
        getLogger().info("Results:\n" +
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
            getLogger().info("Uniqueness report:\n{}", uniquenessReport);
        }
    }

    private String reportOrFixUniqueness(OperationResult result) {

        Configuration cfg = getConfiguration();

        StringBuilder details = new StringBuilder();
        StringBuilder stat = new StringBuilder();

        for (Map.Entry<ContextMapKey, ObjectTypeContext> entry : workingState.contextMap.entrySet()) {
            String resourceOid = entry.getKey().resourceOid;
            QName objectClassName = entry.getKey().objectClassName;
            ObjectTypeContext ctx = entry.getValue();
            PrismObject<ResourceType> resource = workingState.resources.get(resourceOid);
            if (resource == null) {
                logger.error("No resource for {}", resourceOid);        // should not happen
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
                            shadow = taskHandler.getRepositoryService().getObject(ShadowType.class, shadowOid, null, result);
                        } catch (ObjectNotFoundException e) {
                            logger.debug("Couldn't fetch shadow with OID {}, it was probably already deleted", shadowOid, e);
                        } catch (SchemaException e) {
                            LoggingUtils.logUnexpectedException(logger, "Couldn't fetch shadow with OID {} from the repository", e, shadowOid);
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
                        deleteShadows(instruction, details, localCoordinatorTask, result);
                    }
                }
            }
        }
        stat.append("Duplicate shadows detected: ").append(workingState.duplicateShadowsDetected.size());
        if (cfg.fixUniqueness) {
            stat.append(", deleted: ").append(workingState.duplicateShadowsDeleted.size());
            // TODO report the duplicates that remain
        }

        result.summarize();         // there can be many 'search owner' subresults

        return stat.toString() + "\n" + details.toString();
    }

    // shadowsToDelete do not contain 'already deleted shadows'
    private void deleteShadows(DuplicateShadowsTreatmentInstruction instruction, StringBuilder sb, Task task, OperationResult result) {

        Configuration cfg = getConfiguration();

        logger.trace("Going to delete shadows:\n{}", instruction);
        if (instruction == null || instruction.getShadowsToDelete() == null) {
            return;
        }
        Collection<PrismObject<ShadowType>> shadowsToDelete = instruction.getShadowsToDelete();
        String shadowOidToReplaceDeleted = instruction.getShadowOidToReplaceDeletedOnes();

        for (PrismObject<ShadowType> shadowToDelete : shadowsToDelete) {
            logger.info("Deleting redundant shadow{} {}", skippedForDryRun(), ObjectTypeUtil.toShortString(shadowToDelete));
            sb.append("   --> deleted redundant shadow").append(skippedForDryRun()).append(" ").append(ObjectTypeUtil.toShortString(shadowToDelete)).append("\n");
            String oid = shadowToDelete.getOid();

            List<PrismObject<FocusType>> owners;
            if (cfg.checkOwners) {
                owners = shadowToDelete.getUserData(KEY_OWNERS);
            } else {
                owners = itemProcessor.searchOwners(shadowToDelete, result);
            }

            if (!cfg.dryRun) {
                try {
                    taskHandler.getRepositoryService().deleteObject(ShadowType.class, oid, result);
                    task.recordObjectActionExecuted(shadowToDelete, ChangeType.DELETE, null);
                    workingState.duplicateShadowsDeleted.add(oid);
                } catch (ObjectNotFoundException e) {
                    // suspicious, but not a big deal
                    task.recordObjectActionExecuted(shadowToDelete, ChangeType.DELETE, e);
                    LoggingUtils.logExceptionAsWarning(logger, "Shadow {} couldn't be deleted, because it does not exist anymore", e, ObjectTypeUtil.toShortString(shadowToDelete));
                    continue;
                } catch (RuntimeException e) {
                    task.recordObjectActionExecuted(shadowToDelete, ChangeType.DELETE, e);
                    LoggingUtils.logUnexpectedException(logger, "Shadow {} couldn't be deleted because of an unexpected exception", e, ObjectTypeUtil.toShortString(shadowToDelete));
                    continue;
                }
            }

            if (owners == null || owners.isEmpty()) {
                continue;
            }

            for (PrismObject<FocusType> owner : owners) {
                List<ItemDelta<?, ?>> modifications = new ArrayList<>(2);
                ReferenceDelta deleteDelta = prismContext.deltaFactory().reference().createModificationDelete(FocusType.F_LINK_REF, owner.getDefinition(),
                        prismContext.itemFactory().createReferenceValue(oid, ShadowType.COMPLEX_TYPE));
                modifications.add(deleteDelta);
                if (shadowOidToReplaceDeleted != null) {
                    ReferenceDelta addDelta = prismContext.deltaFactory().reference().createModificationAdd(FocusType.F_LINK_REF, owner.getDefinition(),
                            prismContext.itemFactory().createReferenceValue(shadowOidToReplaceDeleted, ShadowType.COMPLEX_TYPE));
                    modifications.add(addDelta);
                }
                logger.info("Executing modify delta{} for owner {}:\n{}", skippedForDryRun(), ObjectTypeUtil.toShortString(owner), DebugUtil.debugDump(modifications));
                if (!cfg.dryRun) {
                    try {
                        taskHandler.getRepositoryService().modifyObject(owner.getCompileTimeClass(), owner.getOid(), modifications, result);
                        task.recordObjectActionExecuted(owner, ChangeType.MODIFY, null);
                    } catch (ObjectNotFoundException | SchemaException | ObjectAlreadyExistsException | RuntimeException e) {
                        task.recordObjectActionExecuted(owner, ChangeType.MODIFY, e);
                        LoggingUtils.logUnexpectedException(logger, "Focal object {} (owner of {}) couldn't be updated", e, ObjectTypeUtil.toShortString(owner),
                                ObjectTypeUtil.toShortString(shadowToDelete));
                    }
                }
            }

        }
    }


    ShadowStatistics getStatistics() {
        return workingState.statistics;
    }

    public Configuration getConfiguration() {
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

    public void duplicateShadowDetected(String oid) {
        workingState.duplicateShadowsDetected.add(oid);
    }

    public ObjectTypeContext getObjectTypeContext(ContextMapKey key) {
        return workingState.contextMap.get(key);
    }

    public void putObjectTypeContext(ContextMapKey key, ObjectTypeContext context) {
        workingState.contextMap.put(key, context);
    }
}
