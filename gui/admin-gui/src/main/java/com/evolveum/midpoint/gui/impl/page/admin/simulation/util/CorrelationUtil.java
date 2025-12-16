/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.util;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.SimulationsGuiUtil;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.simulation.ProcessedObject;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;

import static com.evolveum.midpoint.gui.impl.page.admin.simulation.util.CorrelationUtil.CorrelationStatus.UNCERTAIN;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType.*;

public class CorrelationUtil {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationUtil.class);

    private static final String DOT_CLASS = CorrelationUtil.class.getName() + ".";
    private static final String OPERATION_SEARCH_PROCESSED_OBJECTS = DOT_CLASS + "searchProcessedObjects";

    public enum CorrelationStatus {
        CORRELATED("info-badge success-light", "Correlation.simulation.state.correlated"),
        UNCERTAIN("info-badge warning-light", "Correlation.simulation.state.uncertain"),
        NOT_CORRELATED("info-badge secondary-light", "Correlation.simulation.state.notCorrelated");
        private final String cssClass;
        private final String translationKey;

        CorrelationStatus(String cssClass, String translationKey) {
            this.cssClass = cssClass;
            this.translationKey = translationKey;
        }

        public static CorrelationStatus fromCount(int count) {
            if (count == 1) {
                return CorrelationStatus.CORRELATED;
            } else if (count > 1) {
                return UNCERTAIN;
            } else {
                return CorrelationStatus.NOT_CORRELATED;
            }
        }

        public static CorrelationStatus fromProcessedObject(ProcessedObject<?> object) {
            if (object == null) {
                return CorrelationStatus.NOT_CORRELATED;
            }
            var correlationCandidateModel = getCorrelationCandidateModel(object);
            var candidates = correlationCandidateModel.getObject();
            if (candidates == null) {
                return CorrelationStatus.NOT_CORRELATED;
            }
            return fromCount(candidates.size());
        }

        public static CorrelationStatus fromSimulationResultProcessedObject(
                @NotNull PageBase pageBase,
                @NotNull SimulationResultProcessedObjectType object) {
            ProcessedObject<?> processedObject = SimulationsGuiUtil
                    .parseProcessedObject(object, pageBase);
            return fromProcessedObject(processedObject);
        }

        public String cssClass() {
            return cssClass;
        }

        public String translationKey() {
            return translationKey;
        }
    }

    /**
     * Finds the CorrelationDefinitionType related to the simulation result.
     */
    public static @Nullable CorrelationDefinitionType findCorrelationDefinition(
            @NotNull PageBase page, @NotNull SimulationResultType result) {

        ResourceObjectTypeDefinitionType objectType = findResourceObjectTypeDefinitionType(page, result);
        if (objectType == null) {return null;}

        return objectType.getCorrelation();
    }

    //TODO how to properly identify ref?

    /**
     * Builds a mapping between correlation item paths (as defined in correlationDefinition)
     * and shadow attribute paths (derived from inbound mappings on resource attributes).
     *
     * <p>The result maps:
     * <correlatedItemPath → shadowAttributePath>
     * </p>
     *
     * <p>
     * For each correlation item path (from correlators/items),
     * the method searches the resource object type definition for an attribute whose inbound
     * target path matches that correlation path. If found, the mapping is returned.
     * </p>
     *
     * @param page page context
     * @param result simulation result containing the resource reference
     * @param correlationDefinition correlation definition with correlators/items
     * @return map of correlated item path → shadow attribute path
     */
    public static @NotNull Map<ItemPath, ItemPath> getShadowCorrelationPathMap(
            @NotNull PageBase page,
            @NotNull SimulationResultType result,
            @NotNull CorrelationDefinitionType correlationDefinition) {

        List<ItemPath> correlatedPaths = new ArrayList<>();

        CompositeCorrelatorType correlators = correlationDefinition.getCorrelators();
        if (correlators == null || correlators.getItems() == null) {
            return Collections.emptyMap();
        }

        for (ItemsSubCorrelatorType sub : correlators.getItems()) {
            if (sub.getItem() == null) {
                continue;
            }
            for (CorrelationItemType correlationItem : sub.getItem()) {
                if (correlationItem.getRef() != null) {
                    correlatedPaths.add(correlationItem.getRef().getItemPath());
                }
            }
        }

        if (correlatedPaths.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<ItemPath, ItemPath> correlationMap = new HashMap<>();

        ResourceObjectTypeDefinitionType objectType = findResourceObjectTypeDefinitionType(page, result);
        if (objectType == null || objectType.getAttribute() == null) {
            return correlationMap;
        }

        for (ResourceAttributeDefinitionType attributeDef : objectType.getAttribute()) {
            ItemPathType attributeRef = attributeDef.getRef();
            if (attributeRef == null) {
                continue;
            }

            List<InboundMappingType> inboundMappings = attributeDef.getInbound();
            if (inboundMappings == null) {
                continue;
            }

            ItemPath shadowAttributePath = attributeRef.getItemPath();
            for (InboundMappingType inbound : inboundMappings) {
                if (inbound.getTarget() == null || inbound.getTarget().getPath() == null) {
                    continue;
                }

                ItemPath inboundTargetPath = inbound.getTarget().getPath().getItemPath();
                for (ItemPath correlatedPath : correlatedPaths) {
                    if (correlatedPath.equals(inboundTargetPath)) {
                        correlationMap.put(correlatedPath, shadowAttributePath);
                    }
                }
            }
        }

        return correlationMap;
    }

    /**
     * Loads all SimulationResultProcessedObjectType objects related to the given simulation result OID.
     */
    public static @NotNull List<SimulationResultProcessedObjectType> searchProcessedObjects(
            @NotNull PageBase pageBase,
            @NotNull String resultOid) {

        Task task = pageBase.createSimpleTask(OPERATION_SEARCH_PROCESSED_OBJECTS);
        OperationResult result = task.getResult();

        ObjectQuery query = pageBase.getPrismContext()
                .queryFor(SimulationResultProcessedObjectType.class)
                .ownedBy(SimulationResultType.class, SimulationResultType.F_PROCESSED_OBJECT)
                .id(resultOid)
                .build();

        List<SimulationResultProcessedObjectType> processedObjects = new ArrayList<>();

        ModelService modelService = pageBase.getModelService();

        try {
            modelService.searchContainersIterative(
                    SimulationResultProcessedObjectType.class,
                    query,
                    (o, parentResult) -> {
                        processedObjects.add(o);
                        return true;
                    },
                    null,
                    task,
                    result);
        } catch (SchemaException | ObjectNotFoundException | CommunicationException |
                ConfigurationException | SecurityViolationException | ExpressionEvaluationException e) {
            LOGGER.error("Error loading processed objects for simulation result {}: {}", resultOid, e.getMessage(), e);
        }

        return processedObjects;
    }

    /**
     * Finds the ResourceObjectTypeDefinitionType related to the simulation result.
     */
    public static @Nullable ResourceObjectTypeDefinitionType findResourceObjectTypeDefinitionType(
            @NotNull PageBase page,
            @NotNull SimulationResultType result) {
        PrismObject<TaskType> task = WebModelServiceUtils.loadObject(result.getRootTaskRef(), page);
        if (task == null) {
            LOGGER.warn("Task not found for simulation result {}", result.getOid());
            return null;
        }

        ResourceObjectSetType resourceObjects = findResourceObjects(task);
        if (resourceObjects == null) {
            LOGGER.debug("No resourceObjects section found in task {}", task.getOid());
            return null;
        }

        ResourceObjectTypeDefinitionType objectType =
                findRelatedObjectType(page, resourceObjects);
        if (objectType == null) {
            LOGGER.debug("Related objectType not found for resourceObjects {} in task {}",
                    resourceObjects, task.getOid());
            return null;
        }
        return objectType;
    }

    private static @Nullable ResourceObjectSetType findResourceObjects(
            @NotNull PrismObject<TaskType> task) {
        //TODO return coeelators if exists
        PrismContainer<ResourceObjectSetType> container =
                task.findContainer(ItemPath.create(
                        TaskType.F_ACTIVITY,
                        ActivityDefinitionType.F_WORK,
                        WorkDefinitionsType.F_CORRELATION,
                        CorrelationWorkDefinitionType.F_RESOURCE_OBJECTS
                ));

        return container != null ? container.getRealValue() : null;
    }

    private static @Nullable ResourceObjectTypeDefinitionType findRelatedObjectType(
            @NotNull PageBase page, @NotNull ResourceObjectSetType resourceObjects) {

        ObjectReferenceType resourceRef = resourceObjects.getResourceRef();
        if (resourceRef == null || resourceRef.getOid() == null) {
            LOGGER.warn("resourceRef missing in resourceObjects");
            return null;
        }

        PrismObject<ResourceType> resource =
                WebModelServiceUtils.loadObject(resourceRef, page);

        if (resource == null || resource.asObjectable().getSchemaHandling() == null) {
            LOGGER.debug("Resource {} cannot be loaded or has no schemaHandling",
                    resourceRef.getOid());
            return null;
        }

        SchemaHandlingType handling = resource.asObjectable().getSchemaHandling();

        ShadowKindType kind = resourceObjects.getKind();
        String intent = resourceObjects.getIntent();

        return handling.getObjectType()
                .stream()
                .filter(t -> Objects.equals(t.getKind(), kind)
                        && Objects.equals(t.getIntent(), intent))
                .findFirst()
                .orElse(null);
    }

    public static @NotNull IModel<List<ResourceObjectOwnerOptionType>> getCorrelationCandidateModel(
            @NotNull ProcessedObject<?> processedObject) {
        return new LoadableDetachableModel<>() {

            @Override
            protected List<ResourceObjectOwnerOptionType> load() {
                @Nullable ObjectDelta<?> delta = processedObject.getDelta();
                List<ResourceObjectOwnerOptionType> optionList = parseResourceObjectOwnerOptionsFromDelta(delta);
                if (optionList != null) {return optionList;}

                return Collections.emptyList();
            }
        };
    }

    public static @NotNull List<String> findCorrelatedOwners(@Nullable ObjectDelta<?> delta) {
        List<String> correlatedOwnersOid = new ArrayList<>();
        try {
            if (delta != null) {
                ItemPath path = ShadowType.F_CORRELATION
                        .append(ShadowCorrelationStateType.F_RESULTING_OWNER);

                ItemDelta<?, ?> itemDelta = delta.findItemDelta(path);
                if (itemDelta == null) {
                    return correlatedOwnersOid;
                }
                if (itemDelta instanceof ReferenceDelta referenceDelta) {
                    Collection<PrismReferenceValue> valuesToReplace = referenceDelta.getValuesToReplace();
                    if (valuesToReplace != null) {
                        for (PrismReferenceValue value : valuesToReplace) {
                            correlatedOwnersOid.add(value.getOid());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Error retrieving correlated owners from delta: {}", ex.getMessage(), ex);
        }
        return correlatedOwnersOid;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static @Nullable List<ResourceObjectOwnerOptionType> parseResourceObjectOwnerOptionsFromDelta(
            @Nullable ObjectDelta<?> delta) {
        try {
            if (delta != null) {
                List<ResourceObjectOwnerOptionType> optionList = new ArrayList<>();
                ItemPath path = ShadowType.F_CORRELATION
                        .append(ShadowCorrelationStateType.F_OWNER_OPTIONS);

                ItemDelta<?, ?> itemDelta = delta.findItemDelta(path);

                if (itemDelta instanceof ContainerDelta containerDelta) {
                    Collection<? extends PrismContainerValue<ResourceObjectOwnerOptionsType>> values =
                            (Collection<? extends PrismContainerValue<ResourceObjectOwnerOptionsType>>)
                                    containerDelta.getValuesToReplace();
                    for (PrismContainerValue<ResourceObjectOwnerOptionsType> pcv : values) {
                        ResourceObjectOwnerOptionsType options =
                                pcv.asContainerable(ResourceObjectOwnerOptionsType.class);
                        if (options != null) {
                            List<ResourceObjectOwnerOptionType> option = options.getOption();
                            optionList.addAll(option);
                        }
                    }
                }
                return optionList;
            }
        } catch (Exception ex) {
            LOGGER.error("Error retrieving correlation candidate options from delta: {}", ex.getMessage(), ex);
        }
        return null;
    }

    public static @NotNull Badge createStatusBadge(@NotNull List<ObjectReferenceType> eventMakRefs, @NotNull PageBase pageBase) {
        Set<String> eventMarkOids = new HashSet<>();
        eventMakRefs.forEach(ref -> {
            if (ref.getOid() != null) {
                eventMarkOids.add(ref.getOid());
            }
        });

        if (eventMarkOids.contains(MARK_SHADOW_CORRELATION_OWNER_FOUND.value())) {
            String label = pageBase.getString("Correlation.simulation.state.correlated");
            return new Badge(CorrelationStatus.CORRELATED.cssClass(), label);
        } else if (eventMarkOids.contains(MARK_SHADOW_CORRELATION_OWNER_NOT_CERTAIN.value())) {
            String label = pageBase.getString("Correlation.simulation.state.uncertain");
            return new Badge(CorrelationStatus.UNCERTAIN.cssClass(), label);
        } else {
            String label = pageBase.getString("Correlation.simulation.state.notCorrelated");
            return new Badge(CorrelationStatus.NOT_CORRELATED.cssClass(), label);
        }
    }

    public static class CandidateDisplayData implements Serializable {
        public String text;
        public String icon;

        CandidateDisplayData(String text, String icon) {
            this.text = text;
            this.icon = icon;
        }
    }

    public static @NotNull CandidateDisplayData createCandidateDisplay(
            @NotNull PageBase pageBase,
            @NotNull List<ResourceObjectOwnerOptionType> candidates,
            @NotNull List<String> correlatedOwnersOid) {
        int count = candidates.size();
        CorrelationStatus state = CorrelationStatus.fromCount(count);

        if (state.equals(CorrelationStatus.CORRELATED)) {
            ResourceObjectOwnerOptionType option = candidates.get(0);
            ObjectReferenceType ref = option.getCandidateOwnerRef();

            String name = WebModelServiceUtils.resolveReferenceName(ref, pageBase);
            return new CandidateDisplayData(name, GuiStyleConstants.CLASS_OBJECT_USER_ICON);

        } else if (state.equals(UNCERTAIN)) {
            if (!correlatedOwnersOid.isEmpty()) {
                StringBuilder ownerNames = new StringBuilder();

                ownerNames.append("Matched owner: ");
                for (ResourceObjectOwnerOptionType candidate : candidates) {
                    String candidateOid = candidate.getCandidateOwnerRef().getOid();
                    if (correlatedOwnersOid.contains(candidateOid)) {
                        String name = WebModelServiceUtils.resolveReferenceName(
                                candidate.getCandidateOwnerRef(), pageBase);
                        ownerNames.append(name);
                        ownerNames.append(" ");
                    }
                }
                ownerNames.append("(").append(count).append(" candidates found)");

                return new CandidateDisplayData(
                        ownerNames.toString(),
                        GuiStyleConstants.CLASS_WARNING_ICON + " text-success"
                );

            }

            return new CandidateDisplayData(
                    count + " candidates found",
                    GuiStyleConstants.CLASS_WARNING_ICON + " text-warning"
            );
        } else {
            return new CandidateDisplayData(
                    "No match found",
                    GuiStyleConstants.CLASS_WARNING_ICON + " text-danger"
            );
        }
    }

    /**
     * Returns the display name of the item specified by the given ItemPathType
     * in the context of the given object class.
     */
    public static <O extends Objectable> @NotNull String getItemDisplayName(
            @Nullable ItemPathType pathType, Class<O> objectClass) {

        PrismObjectDefinition<O> parentDefinition = PrismContext.get()
                .getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(objectClass);

        if (pathType == null) {
            return "N/A"; // should be localized e.g. unknown?
        }

        ItemDefinition<?> itemDefinition = parentDefinition.findItemDefinition(pathType.getItemPath());

        String displayName = itemDefinition.getDisplayName();
        return displayName != null ? LocalizationUtil.translate(displayName)
                : pathType.getItemPath().toString();
    }

    public static @NotNull DashboardWidgetType buildWidget(String label, String help, String iconCss, int value) {
        DashboardWidgetType w = new DashboardWidgetType();
        w.beginData()
                .sourceType(DashboardWidgetSourceTypeType.METRIC)
                .storedData(String.valueOf(value))
                .end();

        DisplayType d = new DisplayType();
        d.setLabel(PolyStringType.fromOrig(label));
        d.setHelp(PolyStringType.fromOrig(help));
        d.setIcon(new IconType().cssClass(iconCss));

        w.setDisplay(d);
        return w;
    }
}
