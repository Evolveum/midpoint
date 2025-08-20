/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.Toggle;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.tile.TileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.ObjectClassWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.smart.RealResourceStatus;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.smart.ResourceStatus;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.xml.ns._public.prism_schema_3.ComplexTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.PrismSchemaType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.*;

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
     * Loads the current status of a resource, initializing it via smart integration services.
     * Returns a real status or an error status in case of failure.
     */
    public static @NotNull ResourceStatus loadObjectTypeSuggestionStatus(
            @NotNull PageBase pageBase,
            @NotNull String resourceOid,
            @NotNull Task task) {
        var result = task.getResult();
        var smart = pageBase.getSmartIntegrationService();

        try {
            var resource = pageBase.getModelService().getObject(ResourceType.class, resourceOid, null, task, result);
            RealResourceStatus status = new RealResourceStatus(resource);
            smart.listSuggestObjectTypesOperationStatuses(resourceOid, task, result);
            status.initializeSuggestObjectTypesOnly(smart, task, result);
            return status;
        } catch (Throwable t) {
            result.recordException(t);
            LoggingUtils.logException(LOGGER, "Error loading status for resource {}", t, resourceOid);
            return new ResourceStatus.ErrorStatus("Error loading status: " + t.getMessage());
        } finally {
            result.close();
        }

    }

    /**
     * Retrieves the latest available object types suggestion for a given object class.
     * Returns {@code null} if there are no suggestions.
     */
    public static @Nullable StatusInfo<ObjectTypesSuggestionType> loadObjectClassObjectTypeSuggestions(
            @NotNull PageBase pageBase,
            @NotNull String resourceOid,
            @NotNull QName objectClassName,
            @NotNull Task task,
            @NotNull OperationResult result) {

        ResourceStatus rs = loadObjectTypeSuggestionStatus(pageBase, resourceOid, task);
        if (!(rs instanceof RealResourceStatus real)) {
            result.recordFatalError("Unexpected resource status type: " + rs.getClass().getSimpleName());
            return null;
        }
        List<StatusInfo<ObjectTypesSuggestionType>> suggestions = real.getObjectTypesSuggestions(objectClassName);
        if (suggestions == null || suggestions.isEmpty()) {
            // Nothing yet for this object class
            return null;
        }

        // Pick the entry with the most recent 'started' timestamp (handles nulls).
        return findLatestSuggestion(suggestions);
    }

    public static @Nullable List<StatusInfo<ObjectTypesSuggestionType>> loadObjectTypeSuggestions(
            @NotNull PageBase pageBase,
            String resourceOid,
            @NotNull Task task,
            @NotNull OperationResult result) {

        if (resourceOid == null) {
            result.recordWarning("Resource OID is null or empty");
            return null;
        }

        ResourceStatus rs = loadObjectTypeSuggestionStatus(pageBase, resourceOid, task);
        if (!(rs instanceof RealResourceStatus real)) {
            result.recordWarning("Unexpected resource status type: " + rs.getClass().getSimpleName());
            return null;
        }
        List<StatusInfo<ObjectTypesSuggestionType>> suggestions = real.getAllSuggestionsStatuses();
        if (suggestions == null || suggestions.isEmpty()) {
            return null;
        }
        return suggestions;
    }

    /**
     * Finds the most recent suggestion entry based on the 'started' timestamp.
     * Returns {@code null} if the list is empty.
     */
    private static StatusInfo<ObjectTypesSuggestionType> findLatestSuggestion(
            @NotNull List<StatusInfo<ObjectTypesSuggestionType>> suggestions) {
        return suggestions.stream()
                .max(Comparator.comparing(
                        StatusInfo::getRealizationStartTimestamp,
                        Comparator.nullsLast(XMLGregorianCalendar::compare)))
                .orElse(null);
    }

    /**
     * Extracts all {@link ResourceObjectTypeDefinitionType} instances from the given status info entry.
     * Returns an empty list if no object types are present.
     */
    public static @NotNull List<ResourceObjectTypeDefinitionType> extractObjectTypesFromStatusInfo(
            @Nullable StatusInfo<ObjectTypesSuggestionType> objectTypesSuggestionTypeStatusInfo) {
        List<ResourceObjectTypeDefinitionType> suggestedObjectTypes = new ArrayList<>();
        if (objectTypesSuggestionTypeStatusInfo != null && objectTypesSuggestionTypeStatusInfo.getResult() != null) {
            ObjectTypesSuggestionType objectTypesSuggestionResult = objectTypesSuggestionTypeStatusInfo.getResult();

            List<ResourceObjectTypeDefinitionType> objectType = objectTypesSuggestionResult.getObjectType();
            if (objectType != null) {
                suggestedObjectTypes.addAll(objectType);
            }
        }
        return suggestedObjectTypes;
    }

    /**
     * Formats the elapsed time between the suggestion's start and finish (or now if still running)
     * into a human-readable string with days, hours, minutes, seconds, and milliseconds.
     */
    public static @NotNull String formatElapsedTime(StatusInfo<ObjectTypesSuggestionType> s) {
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

    /**
     * Returns a model for the {@link ComplexTypeDefinitionType} matching the given object class name
     * in the resource details model, or an empty model if not found.
     */
    public static @NotNull IModel<PrismContainerValueWrapper<ComplexTypeDefinitionType>> getComplexTypeValueModel(
            ObjectClassWrapper selectedItemModel, @NotNull ResourceDetailsModel resourceDetailsModel) {

        PrismContainerValueWrapper<ComplexTypeDefinitionType> valueDefinition;

        ItemPath containerPath = ItemPath.create(ResourceType.F_SCHEMA, WebPrismUtil.PRISM_SCHEMA, PrismSchemaType.F_COMPLEX_TYPE);
        PrismContainerWrapper<ComplexTypeDefinitionType> complexContainerWrapper;

        try {
            complexContainerWrapper = resourceDetailsModel
                    .getObjectWrapper()
                    .findContainer(containerPath);
        } catch (SchemaException e) {
            throw new RuntimeException("Error while finding complex type definition", e);
        }

        if (complexContainerWrapper == null) {
            return Model.of();
        }
        List<PrismContainerValueWrapper<ComplexTypeDefinitionType>> values = complexContainerWrapper.getValues();
        valueDefinition = values.stream()
                .filter(value -> value.getRealValue().getName() != null &&
                        value.getRealValue().getName().equals(selectedItemModel.getObjectClassName()))
                .findFirst()
                .orElse(null);

        if (valueDefinition == null) {
            return Model.of();
        }

        return () -> valueDefinition;
    }

    /**
     * Creates a loadable model representing the available view toggles (table/tile)
     * for a given {@link TileTablePanel}.
     */
    public static @NotNull IModel<List<Toggle<ViewToggle>>> getListToggleView(TileTablePanel<?, ?> tablePanel) {
        return new LoadableModel<>(false) {
            @Override
            protected @NotNull List<Toggle<ViewToggle>> load() {
                List<Toggle<ViewToggle>> list = new ArrayList<>();

                ViewToggle currentView = tablePanel.getViewToggleModel().getObject();

                Toggle<ViewToggle> asList = new Toggle<>("fa-solid fa-table-list", null);
                asList.setActive(currentView == ViewToggle.TABLE);
                asList.setValue(ViewToggle.TABLE);
                list.add(asList);

                Toggle<ViewToggle> asTile = new Toggle<>("fa-solid fa-table-cells", null);
                asTile.setActive(currentView == ViewToggle.TILE);
                asTile.setValue(ViewToggle.TILE);
                list.add(asTile);

                return list;
            }
        };
    }

    //TODO this is for temporary use only, remove when ai-metadata flag is implemented in the schema
    public static void addAiGeneratedMarkMetadataValue(@NotNull ValueMetadata valueMetadata) {
        ExtensionType ext = new ExtensionType();
        try {
            addExtensionValue(ext, "name", "ai-generated");
            valueMetadata.addMetadataValue(new ValueMetadataType()
                    .extension(ext).asPrismContainerValue());
        } catch (SchemaException e) {
            throw new IllegalStateException("Couldn't mark value as AI-generated", e);
        }

    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    private static <V> void addExtensionValue(
            @NotNull Containerable extContainer, String itemName, V... values) throws SchemaException {
        PrismContainerValue<?> pcv = extContainer.asPrismContainerValue();
        ItemDefinition<?> itemDefinition =
                pcv.getDefinition().findItemDefinition(new ItemName(itemName));
        if (itemDefinition instanceof PrismReferenceDefinition) {
            PrismReference ref = (PrismReference) itemDefinition.instantiate();
            for (V value : values) {
                ref.add(value instanceof PrismReferenceValue
                        ? (PrismReferenceValue) value
                        : ((Referencable) value).asReferenceValue());
            }
            pcv.add(ref);
        } else {
            PrismProperty<V> property = (PrismProperty<V>) itemDefinition.instantiate();
            property.setRealValues(values);
            pcv.add(property);
        }
    }

    public static @NotNull IModel<Badge> getAiBadgeModel() {
        Badge aiBadge = new Badge(
                "badge badge-light-purple",
                "fa fa-magic text-purple",
                "AI",
                "text-purple",
                "This value was generated by AI");
        return Model.of(aiBadge);
    }

    public enum SuggestionUiStyle {
        FATAL("bg-light-danger", "info-badge danger"),
        IN_PROGRESS("bg-light-info", "info-badge text-info"),
        NOT_APPLICABLE("bg-light-secondary", "info-badge secondary"),
        DEFAULT("bg-light-purple", "info-badge purple");

        public final String rowClass;
        public final String badgeClass;

        SuggestionUiStyle(String rowClass, String badgeClass) {
            this.rowClass = rowClass;
            this.badgeClass = badgeClass;
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
            @NotNull StatusInfo<ObjectTypesSuggestionType> statusInfo,
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

    //this is wrong but when we create container from selected model it cause problems with null values filled in
    public static @NotNull PrismContainerValue<ResourceObjectTypeDefinitionType> createNewResourceObjectTypePrismContainerValue(
            @NotNull IModel<PrismContainerWrapper<ResourceObjectTypeDefinitionType>> containerModel,
            @NotNull ResourceObjectTypeDefinitionType suggestion) {

        var kind = suggestion.getKind();
        var intent = suggestion.getIntent();
        var displayName = suggestion.getDisplayName();
        String description = suggestion.getDescription();
        ResourceObjectTypeDelineationType delineation = suggestion.getDelineation();

        PrismContainerWrapper<ResourceObjectTypeDefinitionType> containerWrapper = containerModel.getObject();

        var newValue = containerWrapper.getItem().createNewValue();
        var bean = newValue.asContainerable();

        bean.setDescription(description);
        bean.setDisplayName(displayName);
        bean.setKind(kind);
        bean.setIntent(intent);
        bean.setDelineation(delineation);

        return newValue;
    }
}
