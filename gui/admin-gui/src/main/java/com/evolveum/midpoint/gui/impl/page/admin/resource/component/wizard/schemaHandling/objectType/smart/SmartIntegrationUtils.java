/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component.SmartStatisticsPanel;
import com.evolveum.midpoint.model.api.TaskService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.processor.NativeResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.schema.util.ShadowObjectClassStatisticsTypeUtil;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.loadAssociationSuggestions;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.loadObjectClassObjectTypeSuggestions;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;

/**
 * Utility methods for smart integration features in resource object type handling.
 * <p>
 * Provides helper functions for:
 * <ul>
 *     <li>Estimating object class sizes</li>
 *     <li>Loading resource statuses and object type suggestions</li>
 *     <li>Extracting data from suggestions</li>
 *     <li>Formatting elapsed execution time</li>
 *     <li>Triggering suggestion-related background actions</li>
 *     <li>Resolving complex type definition models</li>
 *     <li>Providing view toggle state models for tile/table panels</li>
 * </ul>
 */
public class SmartIntegrationUtils {

    private static final Trace LOGGER = TraceManager.getTrace(SmartIntegrationUtils.class);

    private static final int MAX_SIZE_FOR_ESTIMATION = 100;

    /**
     * Estimates the size of a given object class on the resource using smart integration services.
     * Returns {@code null} if estimation fails.
     */
    public static @Nullable ObjectClassSizeEstimationType computeObjectClassSizeEstimationType(
            @NotNull PageBase pageBase,
            @NotNull String resourceOid,
            @NotNull QName objectClassName,
            @NotNull Task task,
            @NotNull OperationResult result) {
        try {
            return pageBase.getSmartIntegrationService().estimateObjectClassSize(
                    resourceOid, objectClassName, MAX_SIZE_FOR_ESTIMATION, task, result);
        } catch (Exception e) {
            result.recordPartialError("Couldn't estimate object class size for " + objectClassName, e);
            LOGGER.warn("Couldn't estimate object class size for {} / {}", resourceOid, objectClassName, e);
            return null;
        }
    }

    /**
     * Returns names of standalone (i.e. not embedded) + structural (i.e. not auxiliary) object classes.
     *
     * Those are the only object classes that can be directly mapped to object types.
     * Also, we can reasonably assume that we can count objects for these classes.
     *
     * NOTE: This method requires that the schema does exist for the resource and that the resource can be fetched
     * via model API (which should be fine even for slightly malformed resources). Otherwise it will return an empty set.
     * Anyway, if we want to e.g. count objects on this resource, it must be at least minimally functional.
     */
    public static @NotNull Set<QName> getStandaloneStructuralObjectClassesNames(
            @NotNull String resourceOid, @NotNull PageBase pageBase, Task task, OperationResult result) {
        NativeResourceSchema schema;
        try {
            var resource = pageBase.getModelService().getObject(ResourceType.class, resourceOid, null, task, result);
            schema = Resource.of(resource).getNativeResourceSchemaRequired();
        } catch (Exception e) {
            result.recordPartialError("Couldn't get native resource schema for resource " + resourceOid, e);
            LOGGER.warn("Couldn't get native resource schema for resource {}", resourceOid, e);
            return Set.of();
        }
        return schema.getObjectClassDefinitions().stream()
                .filter(def -> !def.isEmbedded() && !def.isAuxiliary())
                .map(def -> new QName(NS_RI, def.getName())) // def.getQName is buggy now
                .collect(Collectors.toSet());
    }

    /**
     * Formats the elapsed time between the suggestion's start and finish (or now if still running)
     * into a human-readable string with days, hours, minutes, seconds, and milliseconds.
     */
    public static @NotNull String formatElapsedTime(StatusInfo<?> s) {
        if (s == null || s.getRealizationStartTimestamp() == null) {
            return "Elapsed time: unknown";
        }

        long startMillis = s.getRealizationStartTimestamp().toGregorianCalendar().getTimeInMillis();
        long endMillis = (s.getRealizationEndTimestamp() != null
                ? s.getRealizationEndTimestamp().toGregorianCalendar().getTimeInMillis()
                : System.currentTimeMillis());

        long elapsedMillis = endMillis - startMillis;
        if (elapsedMillis < 0) {elapsedMillis = 0;}

        long days = elapsedMillis / 86_400_000;
        elapsedMillis %= 86_400_000;
        long hours = elapsedMillis / 3_600_000;
        elapsedMillis %= 3_600_000;
        long minutes = elapsedMillis / 60_000;
        elapsedMillis %= 60_000;
        long seconds = elapsedMillis / 1_000;
        elapsedMillis %= 1_000;
        long millis = elapsedMillis;

        String timeDisplay;
        if (days > 0) {
            timeDisplay = String.format("%dd %02dh %02dm %02ds %03dms", days, hours, minutes, seconds, millis);
        } else if (hours > 0) {
            timeDisplay = String.format("%dh %02dm %02ds %03dms", hours, minutes, seconds, millis);
        } else if (minutes > 0) {
            timeDisplay = String.format("%dm %02ds %03dms", minutes, seconds, millis);
        } else if (seconds > 0) {
            timeDisplay = String.format("%ds %03dms", seconds, millis);
        } else {
            timeDisplay = millis + "ms";
        }

        return "Elapsed time: " + timeDisplay;
    }

    /**
     * Executes an object type suggestion operation if no suggestion is currently available.
     * If suggestions exist, no background task is started.
     * Returns {@code true} if the task was executed, {@code false} otherwise.
     */
    public static boolean runSuggestionAction(
            @NotNull PageBase pageBase,
            @NotNull String resourceOid,
            @NotNull QName objectClassName,
            @NotNull AjaxRequestTarget target,
            @NotNull String operationName,
            @NotNull Task task) {
        OperationResult opResult = task.getResult();
        StatusInfo<ObjectTypesSuggestionType> suggestions = loadObjectClassObjectTypeSuggestions(
                pageBase, resourceOid, objectClassName, task, opResult);

        if (opResult.isError() || opResult.isFatalError()) {
            opResult.recordFatalError("Error loading object type suggestions: " + opResult.getMessage());
            LOGGER.error("Error loading object type suggestions for resource {} and class {}: {}",
                    resourceOid, objectClassName, opResult.getMessage());
            return false;
        }
        //TBD We need to design the logic when we allow to execute the task action.
        boolean executeTaskAction = suggestions == null
                || suggestions.getResult() == null
                || suggestions.getResult().getObjectType() == null
                || suggestions.getResult().getObjectType().isEmpty();

        if (executeTaskAction) {
            pageBase.taskAwareExecutor(target, operationName)
                    .runVoid((activityTask, activityResult) -> {
                        var oid = pageBase.getSmartIntegrationService().submitSuggestObjectTypesOperation(
                                resourceOid, objectClassName, activityTask, activityResult);
                        activityResult.setBackgroundTaskOid(oid);
                    });
        }
        return executeTaskAction;
    }

    /**
     * Executes an association suggestion operation if no suggestion is currently available.
     * If suggestions exist, no background task is started.
     * Returns {@code true} if the task was executed, {@code false} otherwise.
     */
    public static boolean runAssociationSuggestionAction(
            @NotNull PageBase pageBase,
            @NotNull String resourceOid,
            @NotNull Collection<ResourceObjectTypeIdentification> subjectTypes,
            @NotNull Collection<ResourceObjectTypeIdentification> objectTypes,
            @NotNull AjaxRequestTarget target,
            @NotNull String operationName,
            @NotNull Task task) {

        OperationResult opResult = task.getResult();
        List<StatusInfo<AssociationsSuggestionType>> statuses =
                loadAssociationSuggestions(pageBase, resourceOid, task, opResult);

        if (opResult.isError() || opResult.isFatalError()) {
            opResult.recordFatalError("Error loading association suggestions: " + opResult.getMessage());
            LOGGER.error("Error loading association suggestions for resource {}: {}",
                    resourceOid, opResult.getMessage());
            return false;
        }

        // TBD: fine-tune when we allow to execute the task action
        boolean executeTaskAction = statuses == null
                || statuses.isEmpty()
                || statuses.get(0).getResult() == null
                || statuses.get(0).getResult().getAssociation() == null
                || statuses.get(0).getResult().getAssociation().isEmpty();

        if (executeTaskAction) {
            pageBase.taskAwareExecutor(target, operationName)
                    .runVoid((activityTask, activityResult) -> {
                        var oid = pageBase.getSmartIntegrationService()
                                .submitSuggestAssociationsOperation(resourceOid, subjectTypes, objectTypes,
                                        activityTask, activityResult);
                        activityResult.setBackgroundTaskOid(oid);
                    });
        }

        return executeTaskAction;
    }

    public static @NotNull IModel<Badge> getAiBadgeModel() {
        return getAiCustomTextBadgeModel("AI");
    }

    public static @NotNull IModel<Badge> getAiCustomTextBadgeModel(String text) {
        Badge aiBadge = new Badge(
                "badge badge-light-purple d-flex align-items-center",
                "fa fa-wand-magic-sparkles text-purple",
                text,
                "text-purple",
                "This value was generated by AI");
        return Model.of(aiBadge);
    }

    public static @NotNull IModel<Badge> getAiEfficiencyBadgeModel(String text) {
        Badge aiBadge = new Badge(
                "badge badge-purple d-flex align-items-center",
                "fa fa fas fa-bolt",
                text,
                "text-white",
                "This value was generated by AI");
        return Model.of(aiBadge);
    }

    public enum SuggestionUiStyle {
        FATAL("bg-light-danger", "info-badge danger", "border border-danger",
                "SuggestionUiStyle.fatal"),
        IN_PROGRESS("bg-light-info", "info-badge text-info", "border border-info",
                "SuggestionUiStyle.inProgress"),
        UNKNOWN("bg-light-info", "info-badge text-info", "border border-info",
                "SuggestionUiStyle.inProgress"),
        NOT_APPLICABLE("bg-light-secondary", "info-badge secondary", "border border-secondary",
                "SuggestionUiStyle.notApplicable"),
        DEFAULT("bg-light-purple", "info-badge purple", "border border-purple",
                "SuggestionUiStyle.default");

        public final String tileClass;
        public final String rowClass;
        public final String badgeClass;
        public final String textKey;

        SuggestionUiStyle(String rowClass, String badgeClass, String tileClass, String textKey) {
            this.rowClass = rowClass;
            this.badgeClass = badgeClass;
            this.tileClass = tileClass;
            this.textKey = textKey;
        }

        public static SuggestionUiStyle from(StatusInfo<?> s) {
            if (s == null) {return DEFAULT;}

            OperationResultStatusType status = s.getStatus();
            boolean executing = s.isExecuting();
            boolean isFatalError = status == OperationResultStatusType.FATAL_ERROR;
            boolean isSuccess = status == OperationResultStatusType.SUCCESS;
            if (isSuccess) {
                return DEFAULT;
            } else if (executing) {
                return IN_PROGRESS;
            } else if (isFatalError) {
                return FATAL;
            } else {
                return NOT_APPLICABLE;
            }
        }

        public static SuggestionUiStyle from(OperationResultStatusType s) {
            if (s == null) {return DEFAULT;}
            return switch (s) {
                case FATAL_ERROR -> FATAL;
                case IN_PROGRESS -> IN_PROGRESS;
                case UNKNOWN -> UNKNOWN;
                case NOT_APPLICABLE -> NOT_APPLICABLE;
                default -> DEFAULT;
            };
        }
    }

    public static void suspendSuggestionTask(
            @NotNull PageBase pageBase,
            @NotNull StatusInfo<?> statusInfo,
            @NotNull Task task,
            @NotNull OperationResult result) {
        String token = statusInfo.getToken();
        SmartIntegrationService smartIntegrationService = pageBase.getSmartIntegrationService();
        try {
            smartIntegrationService.cancelRequest(token, 2000L, task, result);
        } catch (CommonException e) {
            result.recordFatalError("Couldn't suspend suggestion task: " + e.getMessage(), e);
            LOGGER.error("Couldn't suspend suggestion task for token {}: {}", token, e.getMessage(), e);
        }
    }

    public static void resumeSuggestionTask(
            @NotNull PageBase pageBase,
            @NotNull StatusInfo<?> statusInfo,
            @NotNull Task task,
            @NotNull OperationResult result) {
        String token = statusInfo.getToken();
        try {
            TaskService taskService = pageBase.getTaskService();
            taskService.resumeTask(token, task, result);
        } catch (CommonException e) {
            result.recordFatalError("Couldn't resume suggestion task: " + e.getMessage(), e);
            LOGGER.error("Couldn't resume suggestion task for token {}: {}", token, e.getMessage(), e);
        }
    }

    /**
     * Strategy bundle describing how to locate, normalize and compare suggestion items
     * within a specific suggestions bean type.
     *
     * @param <E> element type of single suggestion item
     * @param <B> suggestions bean type that holds a list of E
     */
    private record RemoveSuggestionHandler<E, B>(
            Class<B> beanClass,
            Function<B, List<E>> listGetter,
            Function<E, PrismContainerValue<?>> toPrismValue,
            Function<E, E> cloneWithoutId,
            String beanDisplayName,
            String itemDisplayPlural
    ) {
    }

    /**
     * Removes a specific suggestion from the task's activity work state, deleting the task if
     * no suggestions remain. Uses the supplied handler to access and compare items.
     *
     * @param pageBase UI/service entry point
     * @param statusInfo token carrier identifying the task
     * @param suggestionToRemove suggestion element to remove
     * @param task current security/context task
     * @param result operation result to record status
     * @param handler strategy describing how to access and compare suggestions
     */
    private static <E, B> void removeSuggestionCommon(
            @NotNull PageBase pageBase,
            @NotNull StatusInfo<?> statusInfo,
            @NotNull E suggestionToRemove,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull RemoveSuggestionHandler<E, B> handler) {

        String token = statusInfo.getToken();

        PrismObject<TaskType> taskObject =
                WebModelServiceUtils.loadObject(TaskType.class, token, pageBase, task, result);

        if (taskObject == null) {
            result.recordFatalError("Task with token " + token + " not found");
            LOGGER.error("Task with token {} not found", token);
            return;
        }

        TaskActivityStateType activityState = taskObject.asObjectable().getActivityState();
        if (activityState == null
                || activityState.getActivity() == null
                || activityState.getActivity().getWorkState() == null) {
            result.recordWarning("Task has no activity/workState to update.");
            LOGGER.warn("Task {} has no activity/workState", token);
            return;
        }

        AbstractActivityWorkStateType workState = activityState.getActivity().getWorkState();
        AbstractActivityWorkStateType workStateClone = workState.clone();
        PrismContainerValue<?> workStateValue = workStateClone.asPrismContainerValue();

        B suggestionsBean = workStateValue.getItems().stream()
                .map(Item::getRealValue)
                .filter(handler.beanClass()::isInstance)
                .map(handler.beanClass()::cast)
                .findFirst()
                .orElse(null);

        List<E> list = suggestionsBean != null ? handler.listGetter().apply(suggestionsBean) : null;

        if (list == null || list.isEmpty()) {
            result.recordWarning("No " + handler.beanDisplayName() + " found in workState. Removing task.");
            LOGGER.warn("No {} found in workState for task {}", handler.beanDisplayName(), token);
            removeWholeTaskObject(pageBase, task, result, token);
            return;
        }

        WebPrismUtil.cleanupEmptyContainerValue(handler.toPrismValue().apply(suggestionToRemove));

        boolean removed = list.removeIf(s ->
                handler.cloneWithoutId().apply(s).equals(handler.cloneWithoutId().apply(suggestionToRemove)));

        if (!removed) {
            LOGGER.info("Suggestion not found in {} for task {}", handler.itemDisplayPlural(), token);
            result.recordSuccessIfUnknown();
            return;
        }

        if (list.isEmpty()) {
            removeWholeTaskObject(pageBase, task, result, token);
            LOGGER.info("No more {} left, deleted task {}", handler.itemDisplayPlural(), token);
            return;
        }

        try {
            ObjectDelta<TaskType> delta = PrismContext.get().deltaFor(TaskType.class)
                    .item(TaskType.F_ACTIVITY_STATE, TaskActivityStateType.F_ACTIVITY, ActivityStateType.F_WORK_STATE)
                    .replace(workStateClone)
                    .asObjectDelta(token);

            pageBase.getModelService().executeChanges(List.of(delta), null, task, result);
            result.recordSuccessIfUnknown();
            LOGGER.info("Removed suggestion from {} for task {}", handler.itemDisplayPlural(), token);

        } catch (CommonException e) {
            result.recordFatalError("Couldn't remove suggestion: " + e.getMessage(), e);
            LOGGER.error("Couldn't remove suggestion for task {}: {}", token, e.getMessage(), e);
        }
    }

    /**
     * Removes a correlation suggestion from the task identified by the status token.
     * Deletes the task if it becomes empty.
     */
    public static void removeCorrelationTypeSuggestionNew(
            @NotNull PageBase pageBase,
            @NotNull StatusInfo<CorrelationSuggestionsType> statusInfo,
            @NotNull CorrelationSuggestionType suggestionToRemove,
            @NotNull Task task,
            @NotNull OperationResult result) {

        RemoveSuggestionHandler<CorrelationSuggestionType, CorrelationSuggestionsType> handler = new RemoveSuggestionHandler<>(
                CorrelationSuggestionsType.class,
                CorrelationSuggestionsType::getSuggestion,
                CorrelationSuggestionType::asPrismContainerValue,
                CorrelationSuggestionType::cloneWithoutId,
                "CorrelationSuggestionsType",
                "correlation suggestions"
        );

        removeSuggestionCommon(pageBase, statusInfo, suggestionToRemove, task, result, handler);
    }

    /**
     * Removes an object-type suggestion from the task identified by the status token.
     * Deletes the task if it becomes empty.
     */
    public static void removeObjectTypeSuggestionNew(
            @NotNull PageBase pageBase,
            @NotNull StatusInfo<ObjectTypesSuggestionType> statusInfo,
            @NotNull ResourceObjectTypeDefinitionType suggestionToRemove,
            @NotNull Task task,
            @NotNull OperationResult result) {

        RemoveSuggestionHandler<ResourceObjectTypeDefinitionType, ObjectTypesSuggestionType> handler = new RemoveSuggestionHandler<>(
                ObjectTypesSuggestionType.class,
                ObjectTypesSuggestionType::getObjectType,
                ResourceObjectTypeDefinitionType::asPrismContainerValue,
                ResourceObjectTypeDefinitionType::cloneWithoutId,
                "ObjectTypesSuggestionType",
                "object type suggestions"
        );
        removeSuggestionCommon(pageBase, statusInfo, suggestionToRemove, task, result, handler);
    }

    public static void removeMappingTypeSuggestionNew(
            @NotNull PageBase pageBase,
            @NotNull StatusInfo<MappingsSuggestionType> statusInfo,
            AttributeMappingsSuggestionType definitionToRemove,
            @NotNull Task task,
            @NotNull OperationResult result) {

        RemoveSuggestionHandler<AttributeMappingsSuggestionType, MappingsSuggestionType> handler =
                new RemoveSuggestionHandler<>(
                        MappingsSuggestionType.class,
                        MappingsSuggestionType::getAttributeMappings,
                        AttributeMappingsSuggestionType::asPrismContainerValue,
                        AttributeMappingsSuggestionType::cloneWithoutId,
                        "MappingsSuggestionType",
                        "attribute mapping definitions"
                );

        removeSuggestionCommon(pageBase, statusInfo, definitionToRemove, task, result, handler);
    }

    /**
     * Removes an association suggestion from the task identified by the status token.
     * Deletes the task if it becomes empty.
     */
    public static void removeAssociationTypeSuggestionNew(
            @NotNull PageBase pageBase,
            @NotNull StatusInfo<AssociationsSuggestionType> statusInfo,
            @NotNull AssociationSuggestionType suggestionToRemove,
            @NotNull Task task,
            @NotNull OperationResult result) {

        RemoveSuggestionHandler<AssociationSuggestionType, AssociationsSuggestionType> handler =
                new RemoveSuggestionHandler<>(
                        AssociationsSuggestionType.class,
                        AssociationsSuggestionType::getAssociation,
                        AssociationSuggestionType::asPrismContainerValue,
                        AssociationSuggestionType::cloneWithoutId,
                        "AssociationsSuggestionType",
                        "association suggestions"
                );

        removeSuggestionCommon(pageBase, statusInfo, suggestionToRemove, task, result, handler);
    }

    public static void removeWholeTaskObject(
            @NotNull PageBase pageBase,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull String token) {
        try {
            ObjectDelta<TaskType> deleteDelta =
                    PrismContext.get().deltaFactory().object().createDeleteDelta(TaskType.class, token);

            pageBase.getModelService()
                    .executeChanges(Collections.singleton(deleteDelta), null, task, result);

            result.recordSuccessIfUnknown();
        } catch (CommonException e) {
            result.recordFatalError("Couldn't delete task " + token + ": " + e.getMessage(), e);
            LOGGER.error("Couldn't delete task {}: {}", token, e.getMessage(), e);
        }
    }

    /**
     * Compute the correlation strategy method based on the CorrelationItemType configuration.
     * If no fuzzy search is defined, it defaults to "Exact".
     *
     * @param correlationItemType The CorrelationItemType to analyze.
     * @return A string representing the correlation strategy method.
     */
    //TODO look at getCorrelationStrategyLabel
    public static @NotNull String computeCorrelationStrategyMethod(@NotNull CorrelationItemType correlationItemType) {
        String strategy = "(Exact)";

        ItemSearchDefinitionType search = correlationItemType.getSearch();
        if (search != null) {
            FuzzySearchDefinitionType fuzzy = search.getFuzzy();
            if (fuzzy != null) {
                LevenshteinDistanceSearchDefinitionType lev = fuzzy.getLevenshtein();
                TrigramSimilaritySearchDefinitionType sim = fuzzy.getSimilarity();

                if (lev != null && lev.getThreshold() != null) {
                    strategy = "(Levenshtein)";
                } else if (sim != null && sim.getThreshold() != null) {
                    strategy = "(Trigram)";
                }
            }
        }
        return strategy;
    }

    public static @NotNull AjaxIconButton createStatisticsButton(
            @NotNull String id,
            @NotNull PageBase pageBase,
            @NotNull String resourceOid,
            ResourceObjectTypeDefinitionType objectTypeDefinition) {
        AjaxIconButton statisticsButton = new AjaxIconButton(id,
                Model.of("fa fa-solid fa-chart-bar"),
                pageBase.createStringResource("Statistics.button.label")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                if (objectTypeDefinition == null) {
                    return;
                }

                showStatisticsPanel(target, objectTypeDefinition, pageBase, resourceOid);
            }
        };
        statisticsButton.add(AttributeModifier.replace("class", "btn btn-default rounded mr-2"));
        statisticsButton.setOutputMarkupId(true);
        statisticsButton.showTitleAsLabel(true);
        return statisticsButton;
    }

    public static void showStatisticsPanel(
            @NotNull AjaxRequestTarget target,
            @NotNull ResourceObjectTypeDefinitionType objectTypeDefinition,
            @NotNull PageBase pageBase,
            @NotNull String resourceOid) {
        ResourceObjectTypeDelineationType delineation = objectTypeDefinition.getDelineation();
        if (delineation == null || delineation.getObjectClass() == null) {
            return;
        }

        QName objectClass = delineation.getObjectClass();

        SmartIntegrationService smartIntegrationService = pageBase.getSmartIntegrationService();
        Task pageTask = pageBase.getPageTask();

        ShadowObjectClassStatisticsType statisticsRequired;
        try {
            GenericObjectType latestStatistics = smartIntegrationService
                    .getLatestStatistics(resourceOid, objectClass, pageTask, pageTask.getResult());
            statisticsRequired = ShadowObjectClassStatisticsTypeUtil.getStatisticsRequired(latestStatistics);
        } catch (SchemaException e) {
            throw new RuntimeException("Couldn't get statistics for "
                    + objectClass + " on resource " + resourceOid, e);
        }

        SmartStatisticsPanel statisticsPanel = new SmartStatisticsPanel(
                pageBase.getMainPopupBodyId(), () -> statisticsRequired, resourceOid, objectClass);

        pageBase.showMainPopup(statisticsPanel, target);
    }
}
