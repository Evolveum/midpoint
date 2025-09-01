/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.*;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils.loadObjectClassObjectTypeSuggestions;

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

    private static final int MAX_SIZE_FOR_ESTIMATION = 10_000;

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
        } catch (CommonException e) {
            result.recordPartialError("Couldn't estimate object class size for " + objectClassName, e);
            LOGGER.warn("Couldn't estimate object class size for {} / {}", resourceOid, objectClassName, e);
            return null;
        }
    }

    /**
     * Formats the elapsed time between the suggestion's start and finish (or now if still running)
     * into a human-readable string with days, hours, minutes, seconds, and milliseconds.
     */
    public static @NotNull String formatElapsedTime(StatusInfo<?> s) {
        if (s == null || s.getRealizationStartTimestamp() == null) {
            return "Elapsed time: N/A";
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
            smartIntegrationService.cancelRequest(token, 5000L, task, result);
        } catch (CommonException e) {
            result.recordFatalError("Couldn't suspend suggestion task: " + e.getMessage(), e);
            LOGGER.error("Couldn't suspend suggestion task for token {}: {}", token, e.getMessage(), e);
        }
    }

    //TODO just dummy method (need to be implemented properly, generic, on service side)
    @SuppressWarnings("ReassignedVariable")
    public static void removeObjectTypeSuggestion(
            @NotNull PageBase pageBase,
            @NotNull StatusInfo<ObjectTypesSuggestionType> statusInfo,
            PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> valueWrapper,
            @NotNull Task task,
            @NotNull OperationResult result) {

        final String token = statusInfo.getToken();
        PrismObject<TaskType> taskPo =
                WebModelServiceUtils.loadObject(TaskType.class, token, pageBase, task, result);

        if (taskPo == null) {
            result.recordFatalError("Task with token " + token + " not found");
            LOGGER.error("Task with token {} not found", token);
            return;
        }

        TaskType taskObject = taskPo.asObjectable();
        TaskActivityStateType activityState = taskObject.getActivityState();
        if (activityState == null || activityState.getActivity() == null || activityState.getActivity().getWorkState() == null) {
            result.recordWarning("Task has no activity/workState to update.");
            LOGGER.warn("Task {} has no activity/workState", token);
            return;
        }

        AbstractActivityWorkStateType workState = taskObject.getActivityState().getActivity().getWorkState();
        AbstractActivityWorkStateType workStateClone = workState.clone();

        PrismContainerValue<?> wsPcv = workStateClone.asPrismContainerValue();

        ObjectTypesSuggestionType suggestionsBean = null;
        for (Item<?, ?> it : wsPcv.getItems()) {
            Object real = it.getRealValue();
            if (real instanceof ObjectTypesSuggestionType) {
                suggestionsBean = (ObjectTypesSuggestionType) real;
                break;
            }
        }

        if (suggestionsBean == null) {
            result.recordWarning("No ObjectTypesSuggestionType found in workState. Removing task.");
            LOGGER.warn("No ObjectTypesSuggestionType found in workState for task {}", token);
            deleteWholeTaskObject(pageBase, task, result, token);
            return;
        }

        // Identify the object-type value (by kind + intent) we need to remove.
        ResourceObjectTypeDefinitionType toRemove = null;
        ResourceObjectTypeDefinitionType clicked = valueWrapper != null ? valueWrapper.getRealValue() : null;
        if (clicked == null) {
            result.recordWarning("No value to remove was provided.");
            LOGGER.warn("No valueWrapper/realValue provided for removal in task {}", token);
            return;
        }

        ShadowKindType wantKind = clicked.getKind();
        String wantIntent = clicked.getIntent();

        for (ResourceObjectTypeDefinitionType candidate : suggestionsBean.getObjectType()) {
            if (Objects.equals(wantKind, candidate.getKind())
                    && Objects.equals(wantIntent, candidate.getIntent())) {
                toRemove = candidate;
                break;
            }
        }

        if (toRemove == null) {
            LOGGER.info("Object type kind={} intent={} not found in suggestions for task {}",
                    wantKind, wantIntent, token);
            result.recordSuccessIfUnknown();
            return;
        }

        PrismContainerValue<ResourceObjectTypeDefinitionType> toRemovePcv = toRemove.asPrismContainerValue();
        PrismContainer<ResourceObjectTypeDefinitionType> objTypeCont =
                suggestionsBean.asPrismContainerValue()
                        .findContainer(ObjectTypesSuggestionType.F_OBJECT_TYPE);

        if (objTypeCont != null) {
            boolean removed = objTypeCont.remove(toRemovePcv);
            if (!removed) {
                suggestionsBean.getObjectType().remove(toRemove);
            }
        } else {
            suggestionsBean.getObjectType().remove(toRemove);
        }

        boolean hasSuggestions = suggestionsBean.getObjectType() != null && !suggestionsBean.getObjectType().isEmpty();

        ModelService modelService = pageBase.getModelService();
        try {

            ObjectDelta<TaskType> delta = PrismContext.get().deltaFor(TaskType.class)
                    .item(TaskType.F_ACTIVITY_STATE, TaskActivityStateType.F_ACTIVITY, ActivityStateType.F_WORK_STATE)
                    .replace(workStateClone)
                    .asObjectDelta(token);

            modelService.executeChanges(Collections.singletonList(delta), null, task, result);
            result.recordSuccessIfUnknown();
            LOGGER.info("Removed suggestion kind={} intent={} from task {}", wantKind, wantIntent, token);

        } catch (CommonException e) {
            result.recordFatalError("Couldn't remove object type suggestion: " + e.getMessage(), e);
            LOGGER.error("Couldn't remove object type suggestion for task {}: {}", token, e.getMessage(), e);
        }
    }

    //TODO just dummy method (need to be implemented properly, generic, on service side)
    public static void removeSuggestion(
            @NotNull PageBase pageBase,
            @NotNull StatusInfo<?> statusInfo,
            @NotNull Task task,
            @NotNull OperationResult result) {

        final String token = statusInfo.getToken();
        PrismObject<TaskType> taskPo =
                WebModelServiceUtils.loadObject(TaskType.class, token, pageBase, task, result);

        if (taskPo == null) {
            result.recordFatalError("Task with token " + token + " not found");
            LOGGER.error("Task with token {} not found", token);
            return;
        }

        ModelService modelService = pageBase.getModelService();
        try {
            ObjectDelta<TaskType> delta = PrismContext.get().deltaFactory().object()
                    .createDeleteDelta(TaskType.class, token);

            modelService.executeChanges(Collections.singletonList(delta), null, task, result);
            result.recordSuccessIfUnknown();
            LOGGER.info("Removed correlation suggestion for task {}", token);

        } catch (CommonException e) {
            result.recordFatalError("Couldn't remove suggestion: " + e.getMessage(), e);
            LOGGER.error("Couldn't remove suggestion for task {}: {}", token, e.getMessage(), e);
        }
    }

    private static void deleteWholeTaskObject(@NotNull PageBase pageBase, @NotNull Task task, @NotNull OperationResult result, String token) {
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
}
