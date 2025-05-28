/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.visualizer;

import static com.evolveum.midpoint.prism.delta.ChangeType.*;
import static com.evolveum.midpoint.prism.path.ItemPath.EMPTY_PATH;
import static com.evolveum.midpoint.prism.polystring.PolyString.getOrig;
import static com.evolveum.midpoint.schema.GetOperationOptions.createNoFetch;
import static com.evolveum.midpoint.schema.SelectorOptions.createCollection;

import java.util.*;

import com.evolveum.midpoint.prism.impl.PrismContainerImpl;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.api.context.ProjectionContextKey;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItemValue;
import com.evolveum.midpoint.model.impl.visualizer.output.*;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ValueDisplayUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class Visualizer {

    private static final Trace LOGGER = TraceManager.getTrace(Visualizer.class);

    public static final String CLASS_DOT = Visualizer.class.getName() + ".";

    @Autowired
    private List<VisualizationDescriptionHandler> descriptionHandlers;
    @Autowired
    private PrismContext prismContext;
    @Autowired
    private ModelService modelService;
    @Autowired
    private Resolver resolver;

    private static final Map<Class<?>, List<ItemPath>> DESCRIPTIVE_ITEMS = new HashMap<>();

    static {
        DESCRIPTIVE_ITEMS.put(AssignmentType.class, Arrays.asList(
                AssignmentType.F_TARGET_REF,
                AssignmentType.F_CONSTRUCTION.append(ConstructionType.F_RESOURCE_REF),
                AssignmentType.F_CONSTRUCTION.append(ConstructionType.F_KIND),
                AssignmentType.F_CONSTRUCTION.append(ConstructionType.F_INTENT),
                AssignmentType.F_TENANT_REF,
                AssignmentType.F_ORG_REF,
                AssignmentType.F_DESCRIPTION));
        DESCRIPTIVE_ITEMS.put(ShadowType.class, Arrays.asList(
                ShadowType.F_RESOURCE_REF,
                ShadowType.F_KIND,
                ShadowType.F_INTENT));
    }

    public VisualizationImpl visualize(PrismObject<? extends ObjectType> object, Task task, OperationResult parentResult) throws SchemaException, ExpressionEvaluationException {
        return visualize(object, new VisualizationContext(), task, parentResult);
    }

    public VisualizationImpl visualize(PrismObject<? extends ObjectType> object, VisualizationContext context, Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException {
        OperationResult result = parentResult.createSubresult(CLASS_DOT + "visualize");
        try {
            resolver.resolve(object, task, result);
            return visualize(object, null, context, task, result);
        } catch (RuntimeException | SchemaException | ExpressionEvaluationException e) {
            result.recordFatalError("Couldn't visualize data structure: " + e.getMessage(), e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private VisualizationImpl visualize(PrismObject<? extends ObjectType> object, VisualizationImpl owner, VisualizationContext context, Task task, OperationResult result) {
        VisualizationImpl visualization = new VisualizationImpl(owner);
        visualization.setChangeType(null);
        visualization.setName(createVisualizationName(object));
        visualization.setSourceRelPath(EMPTY_PATH);
        visualization.setSourceAbsPath(EMPTY_PATH);
        visualization.setSourceDefinition(object.getDefinition());
        visualization.setSourceValue(object.getValue());
        visualization.setSourceDelta(null);
        visualizeItems(visualization, object.getValue().getItems(), false, context, task, result);

        evaluateDescriptionHandlers(visualization, owner, task, result);

        return visualization;
    }

    public List<Visualization> visualizeDeltas(@NotNull List<ObjectDelta<? extends ObjectType>> deltas, Task task, OperationResult parentResult) throws SchemaException, ExpressionEvaluationException {
        OperationResult result = parentResult.createSubresult(CLASS_DOT + "visualizeDeltas");
        try {
            resolver.resolve(deltas, task, result);
            return visualizeDeltas(deltas, new VisualizationContext(), task, result);
        } catch (RuntimeException | SchemaException | ExpressionEvaluationException e) {
            result.recordFatalError("Couldn't visualize the data structure: " + e.getMessage(), e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    public List<Visualization> visualizeProjectionContexts(@NotNull List<? extends ModelProjectionContext> projectionContexts, Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ConfigurationException {

        final List<Visualization> visualizations = new ArrayList<>();
        final OperationResult result = parentResult.createSubresult(CLASS_DOT + "visualizeProjectionContexts");

        try {
            for (ModelProjectionContext ctx : projectionContexts) {
                Visualization visualization = visualizeProjectionContext(ctx, task, result);
                if (!visualization.isEmpty()) {
                    visualizations.add(visualization);
                }
            }
        } catch (RuntimeException | SchemaException | ExpressionEvaluationException e) {
            result.recordFatalError("Couldn't visualize the data structure: " + e.getMessage(), e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
        }

        return visualizations;
    }

    @NotNull
    public VisualizationImpl visualizeProjectionContext(@NotNull ModelProjectionContext context, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ConfigurationException {

        ObjectDelta<ShadowType> executable = context.getExecutableDelta();
        if (executable != null) {
            SynchronizationPolicyDecision decision = context.getSynchronizationPolicyDecision();
            if (decision != SynchronizationPolicyDecision.BROKEN) {
                return visualizeDelta(executable, task, result);
            }

            if (!executable.isModify()) {
                return visualizeDelta(executable, task, result);
            }

            if (context.getObjectOld() != null || context.getObjectNew() == null) {
                return visualizeDelta(executable, task, result);
            }
        }

        // Looks like, it should be an ADD not MODIFY delta (just a guess work since status is BROKEN)
        ObjectDelta<ShadowType> addDelta = PrismContext.get().deltaFactory().object().create(context.getObjectTypeClass(), ChangeType.ADD);
        ResourceObjectDefinition objectTypeDef = context.getCompositeObjectDefinitionRequired();

        ProjectionContextKey key = context.getKey();

        var newAccount = objectTypeDef.createBlankShadowWithTag(key.getTag());
        addDelta.setObjectToAdd(newAccount.getPrismObject());

        if (executable != null) {
            addDelta.merge(executable);
        }

        VisualizationImpl visualization = visualizeDelta(addDelta, task, result);
        visualization.setBroken(context.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN);

        return visualization;
    }

    private List<Visualization> visualizeDeltas(List<ObjectDelta<? extends ObjectType>> deltas, VisualizationContext context, Task task, OperationResult result)
            throws SchemaException {
        List<Visualization> rv = new ArrayList<>(deltas.size());
        for (ObjectDelta<? extends ObjectType> delta : deltas) {
            if (delta.isEmpty()) {
                continue;
            }

            final VisualizationImpl visualization = visualizeDelta(delta, null, null, context, task, result);
            if (!visualization.isEmpty()) {
                rv.add(visualization);
            }
        }
        return rv;
    }

    @NotNull
    public VisualizationImpl visualizeDelta(@NotNull ObjectDelta<? extends ObjectType> objectDelta, Task task, OperationResult parentResult) throws SchemaException, ExpressionEvaluationException {
        return visualizeDelta(objectDelta, null, task, parentResult);
    }

    @NotNull
    public VisualizationImpl visualizeDelta(@NotNull ObjectDelta<? extends ObjectType> objectDelta, ObjectReferenceType objectRef, Task task, OperationResult parentResult) throws SchemaException, ExpressionEvaluationException {
        return visualizeDelta(objectDelta, objectRef, false, true, task, parentResult);
    }

    @NotNull
    public VisualizationImpl visualizeDelta(@NotNull ObjectDelta<? extends ObjectType> objectDelta, ObjectReferenceType objectRef,
            boolean includeOperationalItems, boolean includeOriginalObject, Task task, OperationResult parentResult) throws SchemaException, ExpressionEvaluationException {
        VisualizationContext visualizationContext = new VisualizationContext();
        if (includeOperationalItems) {
            visualizationContext.setIncludeOperationalItems(includeOperationalItems);
        }
        return visualizeDelta(objectDelta, objectRef, visualizationContext, includeOriginalObject, task, parentResult);
    }

    @NotNull
    public VisualizationImpl visualizeDelta(@NotNull ObjectDelta<? extends ObjectType> objectDelta,
            ObjectReferenceType objectRef, VisualizationContext context, boolean includeOriginalObject, Task task,
            OperationResult parentResult) throws SchemaException, ExpressionEvaluationException {
        OperationResult result = parentResult.createSubresult(CLASS_DOT + "visualizeDelta");
        try {
            resolver.resolve(objectDelta, includeOriginalObject, task, result);
            return visualizeDelta(objectDelta, null, objectRef, context, task, result);
        } catch (RuntimeException | SchemaException | ExpressionEvaluationException e) {
            result.recordFatalError("Couldn't visualize the data structure: " + e.getMessage(), e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private VisualizationImpl visualizeDelta(ObjectDelta<? extends ObjectType> objectDelta, VisualizationImpl owner, ObjectReferenceType objectRef,
            VisualizationContext context, Task task, OperationResult result)
            throws SchemaException {
        VisualizationImpl visualization = new VisualizationImpl(owner);
        visualization.setChangeType(objectDelta.getChangeType());
        visualization.setSourceDelta(objectDelta);
        visualization.setSourceRelPath(ItemPath.EMPTY_PATH);
        visualization.setSourceAbsPath(ItemPath.EMPTY_PATH);
        PrismObject<? extends ObjectType> object;
        if (objectDelta.isAdd()) {
            object = objectDelta.getObjectToAdd();
        } else if (objectDelta.getOid() != null) {
            object = getOldObject(objectDelta.getOid(), objectDelta.getObjectTypeClass(), context, task, result);
        } else {
            // this can occur e.g. when showing secondary deltas for OBJECT ADD operation
            object = null;
        }
        if (object != null) {
            visualization.setName(createVisualizationName(object));
            visualization.setSourceValue(object.getValue());
            visualization.setSourceDefinition(object.getDefinition());
        } else {
            visualization.setName(createVisualizationName(objectDelta.getOid(), objectRef));
        }
        if (objectDelta.isAdd()) {
            if (object == null) {
                throw new IllegalStateException("ADD object delta with no object to add: " + objectDelta);
            }
            visualizeItems(visualization, object.getValue().getItems(), false, context, task, result);
        } else if (objectDelta.isModify()) {
            if (object != null) {
                addDescriptiveItems(visualization, object.getValue(), context, task, result);
            }
            visualizeItemDeltas(visualization, objectDelta.getModifications(), context, task, result);
        } else if (objectDelta.isDelete()) {
            if (object != null) {
                addDescriptiveItems(visualization, object.getValue(), context, task, result);
            }
        } else {
            throw new IllegalStateException("Object delta that is neither ADD, nor MODIFY nor DELETE: " + objectDelta);
        }

        evaluateDescriptionHandlers(visualization, null, task, result);

        return visualization;
    }

    private PrismObject<? extends ObjectType> getOldObject(String oid, Class<? extends ObjectType> objectTypeClass, VisualizationContext context, Task task, OperationResult result) {
        PrismObject<? extends ObjectType> object = context.getOldObject(oid);
        if (object != null) {
            return object;
        }
        return getObject(oid, objectTypeClass, context, task, result);
    }

    private PrismObject<? extends ObjectType> getObject(String oid, Class<? extends ObjectType> objectTypeClass, VisualizationContext context, Task task, OperationResult result) {
        if (oid == null) {
            return null;
        }

        PrismObject<? extends ObjectType> object = context.getCurrentObject(oid);
        if (object != null) {
            return object;
        }
        try {
            if (objectTypeClass == null) {
                LOGGER.warn("No object class for {}, using ObjectType", oid);
                objectTypeClass = ObjectType.class;
            }
            object = modelService.getObject(objectTypeClass, oid, createCollection(createNoFetch()), task, result);
            context.putObject(object);
            return object;
        } catch (ObjectNotFoundException e) {
            // Not a big problem: object does not exist (was already deleted or was not yet created).
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Object {} does not exist", e, oid);
            result.recordHandledError(e);
            return null;
        } catch (RuntimeException | SchemaException | ConfigurationException | CommunicationException |
                SecurityViolationException | ExpressionEvaluationException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't resolve object {}", e, oid);
            result.recordWarning("Couldn't resolve object " + oid + ": " + e.getMessage(), e);
            return null;
        }
    }

    private void visualizeItems(VisualizationImpl visualization, Collection<Item<?, ?>> items, boolean descriptive,
            VisualizationContext context, Task task, OperationResult result) {

        if (items == null) {
            return;
        }

        List<Item<?, ?>> itemsToShow = new ArrayList<>(items);
        itemsToShow.sort(getItemDisplayOrderComparator());

        for (Item<?, ?> item : itemsToShow) {
            if (!context.isPathToBeShown(item.getPath()) || context.isHidden(item.getPath())) {
                continue;
            }
            if (item instanceof PrismProperty) {
                final VisualizationItemImpl visualizationItem = createVisualizationItem((PrismProperty) item, descriptive);
                if (!visualizationItem.isOperational() || context.isIncludeOperationalItems()) {
                    visualization.addItem(visualizationItem);
                }
            } else if (item instanceof PrismReference) {
                final VisualizationItemImpl visualizationItem = createVisualizationItem((PrismReference) item, descriptive, context, task, result);
                if (!visualizationItem.isOperational() || context.isIncludeOperationalItems()) {
                    visualization.addItem(visualizationItem);
                }
            } else if (item instanceof PrismContainer) {
                PrismContainer<?> pc = (PrismContainer<?>) item;
                PrismContainerDefinition<?> def = pc.getDefinition();
                boolean separate = isContainerSingleValued(def, pc) ? context.isSeparateSinglevaluedContainers() : context.isSeparateMultivaluedContainers();
                VisualizationImpl currentVisualization = visualization;
                for (PrismContainerValue<?> pcv : pc.getValues()) {
                    if (separate) {
                        VisualizationImpl si = new VisualizationImpl(visualization);
                        si.setChangeType(visualization.getChangeType());
                        NameImpl name = new NameImpl(item.getElementName().getLocalPart());
                        name.setId(item.getElementName().getLocalPart());
                        if (def != null) {
                            name.setDisplayName(def.getDisplayName());
                        }
                        si.setName(name);
                        if (def != null) {
                            si.setOperational(def.isOperational());
                            si.setSourceDefinition(def);
                            if (si.isOperational() && !context.isIncludeOperationalItems()) {
                                continue;
                            }
                        }
                        si.setSourceRelPath(ItemPath.create(item.getElementName()));
                        si.setSourceAbsPath(visualization.getSourceAbsPath().append(item.getElementName()));
                        si.setSourceDelta(null);
                        si.setSourceValue(pcv);
                        visualization.addPartialVisualization(si);

                        currentVisualization = si;
                    }
                    visualizeItems(currentVisualization, pcv.getItems(), descriptive, context, task, result);

                    evaluateDescriptionHandlers(currentVisualization, visualization, task, result);
                }
            } else {
                throw new IllegalStateException("Not a property nor reference nor container: " + item);
            }
        }
    }

    private boolean isContainerSingleValued(PrismContainerDefinition<?> def, PrismContainer<?> pc) {
        if (def == null) {
            return pc.getValues().size() <= 1;
        } else {
            return def.isSingleValue();
        }
    }

    private void visualizeItemDeltas(VisualizationImpl visualization, Collection<? extends ItemDelta<?, ?>> deltas, VisualizationContext context, Task task,
            OperationResult result) throws SchemaException {
        if (deltas == null) {
            return;
        }
        List<ItemDelta<?, ?>> deltasToShow = new ArrayList<>(deltas);
        for (ItemDelta<?, ?> delta : deltasToShow) {
            if (delta.isMetadataRelated() && !context.isIncludeMetadata()) {
                continue;
            }
            if (delta instanceof ContainerDelta) {
                visualizeContainerDelta((ContainerDelta) delta, visualization, context, task, result);
            } else {
                visualizeAtomicDelta(delta, visualization, context, task, result);
            }
        }
        sortItems(visualization);
        sortPartialVisualizations(visualization);
    }

    private void sortItems(VisualizationImpl visualization) {
        visualization.getItems().sort((Comparator<VisualizationItemImpl>) (o1, o2) -> compareDefinitions(o1.getSourceDefinition(), o2.getSourceDefinition()));
    }

    private void sortPartialVisualizations(VisualizationImpl visualization) {
        visualization.getPartialVisualizations().sort((Comparator<VisualizationImpl>) (s1, s2) -> {

            final PrismContainerDefinition<?> def1 = s1.getSourceDefinition();
            final PrismContainerDefinition<?> def2 = s2.getSourceDefinition();
            int a = compareDefinitions(def1, def2);
            if (a != 0) {
                return a;
            }
            if (def1 == null || def2 == null) {
                return MiscUtil.compareNullLast(def1, def2);
            }
            if (s1.isContainerValue() && s2.isContainerValue()) {
                Long id1 = s1.getSourceContainerValueId();
                Long id2 = s2.getSourceContainerValueId();
                return compareNullableIntegers(id1, id2);
            } else if (s1.isObjectValue() && s2.isObjectValue()) {
                boolean f1 = s1.isFocusObject();
                boolean f2 = s2.isFocusObject();
                if (f1 && !f2) {
                    return -1;
                } else if (f2 && !f1) {
                    return 1;
                } else {
                    return 0;
                }
            }
            if (s1.isObjectValue()) {
                return -1;
            } else if (s2.isObjectValue()) {
                return 1;
            }
            return 0;
        });
    }

    private <C extends Containerable> void visualizeContainerDelta(ContainerDelta<C> delta, VisualizationImpl visualization, VisualizationContext context, Task task, OperationResult result) {
        if (delta.isEmpty()) {
            return;
        }
        if (!context.isPathToBeShown(delta.getPath()) || context.isHidden(delta.getPath())) {
            return;
        }
        PrismContainerDefinition def = delta.getDefinition();
        if (def != null && def.isOperational() && !context.isIncludeOperationalItems() && def.getDisplayHint() != DisplayHint.REGULAR) {
            return;
        }
        Collection<PrismContainerValue<C>> valuesToAdd;
        Collection<PrismContainerValue<C>> valuesToDelete;
        if (!delta.isReplace()) {
            valuesToAdd = delta.getValuesToAdd();
            valuesToDelete = delta.getValuesToDelete();
        } else {
            valuesToAdd = new ArrayList<>();
            valuesToDelete = new ArrayList<>();
            Collection<PrismContainerValue<C>> oldValues = delta.getEstimatedOldValues();
            for (PrismContainerValue<C> newValue : delta.getValuesToReplace()) {
                if (oldValues == null || !oldValues.contains(newValue)) {        // TODO containsEquivalentValue instead?
                    valuesToAdd.add(newValue);
                }
            }
            if (oldValues != null) {
                for (PrismContainerValue<C> oldValue : oldValues) {
                    if (!delta.getValuesToReplace().contains(oldValue)) {        // TODO containsEquivalentValue instead?
                        valuesToDelete.add(oldValue);
                    }
                }
            }
        }
        if (valuesToDelete != null) {
            for (PrismContainerValue<C> value : valuesToDelete) {
                if (isOperationalContainer(value) && !context.isIncludeOperationalItems()) {
                    continue;
                }
                visualizeContainerDeltaValue(value, DELETE, delta, visualization, context, task, result);
            }
        }
        if (valuesToAdd != null) {
            for (PrismContainerValue<C> value : valuesToAdd) {
                if ((isOperationalContainer(value) || containsOnlyOperationalItems(value))
                        && !context.isIncludeOperationalItems()) {
                    continue;
                }
                visualizeContainerDeltaValue(value, ADD, delta, visualization, context, task, result);
            }
        }
    }

    private <C extends Containerable> boolean isOperationalContainer(PrismContainerValue<C> value) {
        return value == null || value.getDefinition() == null || value.getDefinition().isOperational();
    }

    private <C extends Containerable> boolean containsOnlyOperationalItems(PrismContainerValue<C> value) {
        if (value == null) {
            return true;
        }
        for (Item<?, ?> item : value.getItems()) {
            if (item.getDefinition() != null && !item.getDefinition().isOperational()) {
                return false;
            }
        }
        return true;
    }

    private <C extends Containerable> void visualizeContainerDeltaValue(PrismContainerValue<C> value, ChangeType changeType,
            ContainerDelta<C> containerDelta, VisualizationImpl parentVisualization, VisualizationContext context, Task task, OperationResult result) {
        VisualizationImpl visualization = createContainerVisualization(changeType, containerDelta.getPath(), parentVisualization);
        if (value.getId() != null) {
            visualization.getName().setId(String.valueOf(value.getId()));
        }
        // delete-by-id: we supply known values
        if ((value.getItems().isEmpty()) && value.getId() != null) {
            if (containerDelta.getEstimatedOldValues() != null) {
                for (PrismContainerValue<C> oldValue : containerDelta.getEstimatedOldValues()) {
                    if (value.getId().equals(oldValue.getId())) {
                        value = oldValue;
                        break;
                    }
                }
            }
        }
        visualization.setSourceValue(value);
        visualizeItems(visualization, value.getItems(), true, context, task, result);

        parentVisualization.addPartialVisualization(visualization);

        evaluateDescriptionHandlers(visualization, parentVisualization, task, result);
    }

    private void evaluateDescriptionHandlers(
            VisualizationImpl visualization, VisualizationImpl parentVisualization, Task task, OperationResult result) {
        for (VisualizationDescriptionHandler handler : descriptionHandlers) {
            if (handler.match(visualization, parentVisualization)) {
                handler.apply(visualization, parentVisualization, task, result);
                break;
            }
        }
    }

    private VisualizationImpl createContainerVisualization(ChangeType changeType, ItemPath containerPath, VisualizationImpl parentVisualization) {
        VisualizationImpl visualization = new VisualizationImpl(parentVisualization);
        visualization.setChangeType(changeType);

        ItemPath deltaParentItemPath = getDeltaParentItemPath(containerPath);
        PrismContainerDefinition<?> visualizationDefinition = getVisualizationDefinition(visualization, deltaParentItemPath);

        NameImpl name = createNameForContainerDelta(containerPath, visualizationDefinition);
        visualization.setName(name);

        if (visualizationDefinition != null) {
            visualization.setOperational(visualizationDefinition.isOperational());
            visualization.setSourceDefinition(visualizationDefinition);
        }

        ItemPath visualizationRelativePath = containerPath.remainder(parentVisualization.getSourceRelPath());
        visualization.setSourceRelPath(visualizationRelativePath);
        visualization.setSourceAbsPath(containerPath);
        visualization.setSourceDelta(null);
        return visualization;
    }

    private NameImpl createNameForContainerDelta(ItemPath deltaParentPath, PrismContainerDefinition<?> visualizationDefinition) {
        NameImpl name = new NameImpl(deltaParentPath.toString());
        name.setId(String.valueOf(getLastId(deltaParentPath)));
        if (visualizationDefinition != null) {
            name.setDisplayName(visualizationDefinition.getDisplayName());
        }
        return name;
    }

    private ItemPath getDeltaParentItemPath(ItemPath deltaParentPath) {
        if (ItemPath.isId(deltaParentPath.last())) {
            return deltaParentPath.allExceptLast();
        } else {
            return deltaParentPath;
        }
    }

    private Long getLastId(ItemPath deltaParentPath) {
        return ItemPath.toIdOrNull(deltaParentPath.last());
    }

    private PrismContainerDefinition<?> getVisualizationDefinition(VisualizationImpl parent, ItemPath deltaParentItemPath) {
        PrismContainerDefinition<?> rootDefinition = getRootDefinition(parent);
        if (rootDefinition == null) {
            return null;
        } else {
            return rootDefinition.findContainerDefinition(deltaParentItemPath);
        }
    }

    private void visualizeAtomicDelta(ItemDelta<?, ?> delta, VisualizationImpl visualization, VisualizationContext context, Task task, OperationResult result)
            throws SchemaException {
        if (!context.isPathToBeShown(delta.getPath()) || context.isHidden(delta.getPath())) {
            return;
        }
        ItemPath deltaParentPath = delta.getParentPath();
        ItemPath visualizationRelativeItemPath = getDeltaParentItemPath(deltaParentPath).remainder(visualization.getSourceRelPath());
        VisualizationImpl visualizationForItem;
        if (ItemPath.isEmpty(deltaParentPath)) {
            visualizationForItem = visualization;
        } else {
            visualizationForItem = findPartialVisualizationByPath(visualization, deltaParentPath);
            if (visualizationForItem == null) {
                visualizationForItem = createContainerVisualization(MODIFY, deltaParentPath, visualization);
                if (visualizationForItem.isOperational() && !context.isIncludeOperationalItems()) {
                    return;
                }
                PrismContainerValue<?> ownerPCV = visualization.getSourceValue();
                if (ownerPCV != null) {
                    Item<?, ?> item = ownerPCV.findItem(visualizationRelativeItemPath);
                    if (item instanceof PrismContainer) {
                        PrismContainer<?> container = (PrismContainer<?>) item;
                        visualizationForItem.setSourceDefinition(container.getDefinition());
                        Long lastId = getLastId(deltaParentPath);
                        PrismContainerValue<?> visualizationSrcValue;
                        if (lastId == null) {
                            if (container.size() == 1) {
                                visualizationSrcValue = container.getValues().get(0);
                            } else {
                                visualizationSrcValue = null;
                            }
                        } else {
                            visualizationSrcValue = container.findValue(lastId);
                        }
                        if (visualizationSrcValue != null) {
                            visualizationForItem.setSourceValue(visualizationSrcValue);
                            addDescriptiveItems(visualizationForItem, visualizationSrcValue, context, task, result);
                        }
                    }
                }
                ItemPath deltaPathWithoutLast = deltaParentPath.allExceptLast();
                VisualizationImpl parentPartialVisualization = getOrCreateParentPartialVisualization(visualization, deltaPathWithoutLast, true);
                if (parentPartialVisualization != null) {
                    parentPartialVisualization.addPartialVisualization(visualizationForItem);
                    evaluateDescriptionHandlers(parentPartialVisualization, visualization, task, result);
                } else {
                    visualization.addPartialVisualization(visualizationForItem);
                }
            }
        }
        ItemPath itemRelativeItemPath = getDeltaParentItemPath(delta.getPath()).remainder(visualizationForItem.getSourceRelPath());
        if (context.isRemoveExtraDescriptiveItems()) {
            Iterator<? extends VisualizationItemImpl> iterator = visualizationForItem.getItems().iterator();
            while (iterator.hasNext()) {
                VisualizationItemImpl item = iterator.next();
                if (item.isDescriptive() && item.getSourceRelPath() != null && item.getSourceRelPath().equivalent(itemRelativeItemPath)) {
                    iterator.remove();
                    break;
                }
            }
        }
        visualizeAtomicItemDelta(visualizationForItem, delta, context, task, result);

        evaluateDescriptionHandlers(visualizationForItem, visualization, task, result);
    }

    private VisualizationImpl getOrCreateParentPartialVisualization(VisualizationImpl parentVisualization, ItemPath path,
            boolean stopOnSingleSegmentPath) {
        VisualizationImpl parentVis = findParentPartialVisualization(parentVisualization, path);
        if (stopOnSingleSegmentPath && path.size() <= 1) {
            return parentVis;
        }
        if (parentVis == null) {
            ItemPath allExceptLast = path.allExceptLast();
            if (allExceptLast.isEmpty() || ItemPath.equivalent(path.removeIds(), allExceptLast)) {
                parentVis = createContainerVisualization(MODIFY, path, parentVisualization);
                var sourceValue = parentVisualization.getSourceValue();
                //we want to find source value in the parent container by the path
                // the following piece of code looks strange and requires a refactoring
                if (sourceValue != null) {
                    Item<?, ?> visSourceVal = sourceValue.findItem(path.removeIds());
                    if (visSourceVal instanceof PrismContainerImpl<?> pcvi) {
                        var contValue = pcvi.find(ItemPath.create(path.last()));
                        if (contValue != null) {
                            parentVis.setSourceValue((PrismContainerValue<?>) contValue);
                        }
                    }
                }
                parentVisualization.addPartialVisualization(parentVis);
            } else {
                return getOrCreateParentPartialVisualization(parentVisualization, allExceptLast, false);
            }
        }
        return parentVis;
    }

    private VisualizationImpl findParentPartialVisualization(VisualizationImpl visualization, ItemPath path) {
        for (VisualizationImpl pv : visualization.getPartialVisualizations()) {
            ItemPath pvPath = pv.getSourceAbsPath();
            if (pvPath != null && pvPath.equivalent(path)) {
                return pv;
            }
            return findParentPartialVisualization(pv, path);
        }
        return null;
    }

    private void addDescriptiveItems(VisualizationImpl visualization, PrismContainerValue<?> sourceValue, VisualizationContext context, Task task, OperationResult result) {
        // TODO dynamically typed values
        if (sourceValue.getContainer() == null || sourceValue.getContainer().getCompileTimeClass() == null) {
            return;
        }
        Class<?> clazz = sourceValue.getContainer().getCompileTimeClass();
        List<ItemPath> itemPathsToShow = DESCRIPTIVE_ITEMS.get(clazz);
        if (itemPathsToShow == null) {
            return;
        }
        List<Item<?, ?>> itemsToShow = new ArrayList<>();
        for (ItemPath itemPath : itemPathsToShow) {
            Item<?, ?> item = sourceValue.findItem(itemPath);
            if (item != null) {
                itemsToShow.add(item);
            }
        }
        visualizeItems(visualization, itemsToShow, true, context, task, result);
    }

    private PrismContainerDefinition<?> getRootDefinition(VisualizationImpl visualization) {
        while (visualization.getOwner() != null) {
            visualization = visualization.getOwner();
        }
        return visualization.getSourceDefinition();
    }

    private VisualizationImpl findPartialVisualizationByPath(VisualizationImpl visualization, ItemPath deltaParentPath) {
        for (VisualizationImpl partial : visualization.getPartialVisualizations()) {
            if (partial.getSourceAbsPath().equivalent(deltaParentPath) && partial.getChangeType() == MODIFY) {
                return partial;
            }
            return findPartialVisualizationByPath(partial, deltaParentPath);
        }
        return null;
    }

    private void visualizeAtomicItemDelta(VisualizationImpl visualization, ItemDelta<?, ?> delta, VisualizationContext context, Task task, OperationResult result)
            throws SchemaException {
        final VisualizationDeltaItemImpl visualizationDeltaItem;
        if (delta instanceof PropertyDelta) {
            visualizationDeltaItem = createVisualizationDeltaItem((PropertyDelta) delta, visualization, context, task, result);
        } else if (delta instanceof ReferenceDelta) {
            visualizationDeltaItem = createVisualizationDeltaItem((ReferenceDelta) delta, visualization, context, task, result);
        } else {
            throw new IllegalStateException("No property nor reference delta: " + delta);
        }
        if (!visualizationDeltaItem.isOperational() || context.isIncludeOperationalItems()) {
            visualization.addItem(visualizationDeltaItem);
        }
    }

    private Comparator<Item<?, ?>> getItemDisplayOrderComparator() {
        return (o1, o2) -> compareDefinitions(o1.getDefinition(), o2.getDefinition());
    }

    private int compareDefinitions(ItemDefinition d1, ItemDefinition d2) {
        Integer order1 = d1 != null ? d1.getDisplayOrder() : null;
        Integer order2 = d2 != null ? d2.getDisplayOrder() : null;
        return compareNullableIntegers(order1, order2);
    }

    private int compareNullableIntegers(Integer i1, Integer i2) {
        if (i1 == null && i2 == null) {
            return 0;
        } else if (i1 == null) {
            return 1;
        } else if (i2 == null) {
            return -1;
        } else {
            return Integer.compare(i1, i2);
        }
    }

    private int compareNullableIntegers(Long i1, Long i2) {
        if (i1 == null && i2 == null) {
            return 0;
        } else if (i1 == null) {
            return 1;
        } else if (i2 == null) {
            return -1;
        } else {
            return Long.compare(i1, i2);
        }
    }

    private VisualizationItemImpl createVisualizationItemCommon(Item<?, ?> item) {
        VisualizationItemImpl si = new VisualizationItemImpl(createVisualizationItemName(item));
        ItemDefinition<?> def = item.getDefinition();
        if (def != null) {
            si.setOperational(def.isOperational());
        }
        si.setSourceItem(item);
        si.setSourceRelPath(item.getElementName());
        return si;
    }

    private VisualizationItemImpl createVisualizationItem(PrismProperty<?> property, boolean descriptive) {
        VisualizationItemImpl si = createVisualizationItemCommon(property);
        si.setNewValues(toVisualizationItemValues(property.getValues()));
        si.setDescriptive(descriptive);
        return si;
    }

    private VisualizationItemImpl createVisualizationItem(PrismReference reference, boolean descriptive, VisualizationContext context, Task task,
            OperationResult result) {
        VisualizationItemImpl si = createVisualizationItemCommon(reference);
        si.setNewValues(toVisualizationItemValuesRef(reference.getValues(), context, task, result));
        si.setDescriptive(descriptive);
        return si;
    }

    @SuppressWarnings({ "unused", "unchecked" })
    private VisualizationDeltaItemImpl createVisualizationDeltaItem(PropertyDelta<?> delta, VisualizationImpl parent, VisualizationContext context, Task task,
            OperationResult result) throws SchemaException {
        VisualizationDeltaItemImpl si = createVisualizationDeltaItemCommon(delta, parent);
        if (delta.isDelete() && CollectionUtils.isEmpty(delta.getEstimatedOldValues()) &&
                CollectionUtils.isNotEmpty(delta.getValuesToDelete())) {
            delta.setEstimatedOldValues((Collection) delta.getValuesToDelete());
        }
        si.setOldValues(toVisualizationItemValues(delta.getEstimatedOldValues()));

        PrismProperty property = prismContext.itemFactory().createProperty(delta.getElementName());
        if (delta.getEstimatedOldValues() != null) {
            property.addValues(CloneUtil.cloneCollectionMembers(delta.getEstimatedOldValues()));
        }
        try {
            delta.applyToMatchingPath(property);
        } catch (SchemaException e) {
            throw new SystemException("Couldn't visualize property delta: " + delta + ": " + e.getMessage(), e);
        }
        computeAddedDeletedUnchanged(si, delta.getEstimatedOldValues(), property.getValues());
        si.setNewValues(toVisualizationItemValues(property.getValues()));
        return si;
    }

    private <V extends PrismPropertyValue<?>> void computeAddedDeletedUnchanged(VisualizationDeltaItemImpl si, Collection<V> oldValues, Collection<V> newValues) {
        List<V> added = new ArrayList<>();
        List<V> deleted = new ArrayList<>();
        List<V> unchanged = new ArrayList<>();
        computeDifferences(oldValues, newValues, added, deleted, unchanged);
        si.setAddedValues(toVisualizationItemValues(added));
        si.setDeletedValues(toVisualizationItemValues(deleted));
        si.setUnchangedValues(toVisualizationItemValues(unchanged));
    }

    private <V extends PrismValue> void computeDifferences(Collection<V> oldValues, Collection<V> newValues, List<V> added, List<V> deleted, List<V> unchanged) {
        if (oldValues != null) {
            for (V oldValue : oldValues) {
                if (newValues != null && newValues.contains(oldValue)) {
                    unchanged.add(oldValue);
                } else {
                    deleted.add(oldValue);
                }
            }
        }
        if (newValues != null) {
            for (V newValue : newValues) {
                if (oldValues == null || !oldValues.contains(newValue)) {
                    added.add(newValue);
                }
            }
        }
    }

    private void computeAddedDeletedUnchangedRef(VisualizationDeltaItemImpl si, Collection<PrismReferenceValue> oldValues, Collection<PrismReferenceValue> newValues,
            VisualizationContext context, Task task, OperationResult result) {
        List<PrismReferenceValue> added = new ArrayList<>();
        List<PrismReferenceValue> deleted = new ArrayList<>();
        List<PrismReferenceValue> unchanged = new ArrayList<>();
        computeDifferences(oldValues, newValues, added, deleted, unchanged);
        si.setAddedValues(toVisualizationItemValuesRef(added, context, task, result));
        si.setDeletedValues(toVisualizationItemValuesRef(deleted, context, task, result));
        si.setUnchangedValues(toVisualizationItemValuesRef(unchanged, context, task, result));
    }

    @SuppressWarnings("unchecked")
    private <V extends PrismValue, D extends ItemDefinition<?>> VisualizationDeltaItemImpl createVisualizationDeltaItemCommon(ItemDelta<V, D> itemDelta,
            VisualizationImpl parent)
            throws SchemaException {
        String simpleName = itemDelta.getElementName() != null ? itemDelta.getElementName().getLocalPart() : "";
        NameImpl name = new NameImpl(simpleName);
        if (itemDelta.getDefinition() != null) {
            name.setDisplayName(itemDelta.getDefinition().getDisplayName());
        }
        name.setId(simpleName);

        VisualizationDeltaItemImpl si = new VisualizationDeltaItemImpl(name);
        si.setSourceDelta(itemDelta);

        D def = itemDelta.getDefinition();
        if (def != null) {
            Item<V, D> item = (Item<V, D>) def.instantiate();
            if (itemDelta.getEstimatedOldValues() != null) {
                item.addAll(CloneUtil.cloneCollectionMembers(itemDelta.getEstimatedOldValues()));
            }
            si.setSourceItem(item);
            si.setOperational(def.isOperational());
        }
        ItemPath remainder = itemDelta.getPath().remainder(parent.getSourceRelPath());
        if (remainder.startsWithNullId()) {
            remainder = remainder.rest();
        }
        si.setSourceRelPath(remainder);
        return si;
    }

    private NameImpl createVisualizationItemName(Item<?, ?> item) {
        NameImpl name = new NameImpl(item.getElementName().getLocalPart());
        ItemDefinition<?> def = item.getDefinition();
        if (def != null) {
            name.setDisplayName(def.getDisplayName());
            name.setDescription(def.getDocumentation());
        }
        name.setId(item.getElementName().getLocalPart());        // todo reconsider
        return name;
    }

    private VisualizationDeltaItemImpl createVisualizationDeltaItem(ReferenceDelta delta, VisualizationImpl parent, VisualizationContext context, Task task,
            OperationResult result)
            throws SchemaException {
        VisualizationDeltaItemImpl di = createVisualizationDeltaItemCommon(delta, parent);
        if (delta.isDelete() && CollectionUtils.isEmpty(delta.getEstimatedOldValues()) &&
                CollectionUtils.isNotEmpty(delta.getValuesToDelete())) {
            delta.setEstimatedOldValues(delta.getValuesToDelete());
        }
        di.setOldValues(toVisualizationItemValuesRef(delta.getEstimatedOldValues(), context, task, result));

        PrismReference reference = prismContext.itemFactory().createReference(delta.getElementName());
        try {
            if (delta.getEstimatedOldValues() != null) {
                reference.addAll(CloneUtil.cloneCollectionMembers(delta.getEstimatedOldValues()));
            }
            delta.applyToMatchingPath(reference);
        } catch (SchemaException e) {
            throw new SystemException("Couldn't visualize reference delta: " + delta + ": " + e.getMessage(), e);
        }
        computeAddedDeletedUnchangedRef(di, delta.getEstimatedOldValues(), reference.getValues(), context, task, result);
        di.setNewValues(toVisualizationItemValuesRef(reference.getValues(), context, task, result));
        return di;
    }

    private List<VisualizationItemValue> toVisualizationItemValues(Collection<? extends PrismPropertyValue<?>> values) {
        List<VisualizationItemValue> rv = new ArrayList<>();
        if (values != null) {
            for (PrismPropertyValue<?> value : values) {
                if (value != null) {
                    VisualizationItemValueImpl siv = new VisualizationItemValueImpl(ValueDisplayUtil.toStringValue(value));
                    siv.setSourceValue(value);
                    rv.add(siv);
                }
            }
        }
        return rv;
    }

    private List<VisualizationItemValue> toVisualizationItemValuesRef(Collection<PrismReferenceValue> refValues, VisualizationContext context, Task task, OperationResult result) {
        if (refValues == null) {
            return new ArrayList<>();
        }

        List<VisualizationItemValue> rv = new ArrayList<>();
        for (PrismReferenceValue refValue : refValues) {
            if (refValue == null) {
                continue;
            }

            refValue = createRefValueWithObject(refValue, context, task, result);
            String name;
            if (refValue.getObject() != null) {
                name = PolyString.getOrig(refValue.getObject().getName());
            } else if (refValue.getTargetName() != null) {
                name = refValue.getTargetName().getOrig();
            } else {
                name = refValue.getOid();
            }

            String relation;
            if (refValue.getRelation() != null) {
                relation = "[" + refValue.getRelation().getLocalPart() + "]";
            } else {
                relation = null;
            }

            VisualizationItemValueImpl itemValue = new VisualizationItemValueImpl(name, relation);
            itemValue.setSourceValue(refValue);
            rv.add(itemValue);
        }
        return rv;
    }

    @SuppressWarnings("unchecked")
    private PrismReferenceValue createRefValueWithObject(PrismReferenceValue refValue, VisualizationContext context, Task task, OperationResult result) {
        if (refValue.getObject() != null) {
            return refValue;
        }
        PrismObject<? extends ObjectType> object = getObject(refValue.getOid(),
                (Class) refValue.getTargetTypeCompileTimeClass(), context, task, result);
        if (object == null) {
            return refValue;
        }
        refValue = refValue.clone();
        refValue.setObject(object);
        return refValue;
    }

    private NameImpl createVisualizationName(PrismObject<? extends ObjectType> object) {
        NameImpl name = new NameImpl(object.getName() != null ? getOrig(object.getName()) : object.getOid());
        name.setId(object.getOid());
        ObjectType objectType = object.asObjectable();
        name.setDescription(objectType.getDescription());
        if (objectType instanceof UserType) {
            name.setDisplayName(getOrig(((UserType) objectType).getFullName()));
        } else if (objectType instanceof AbstractRoleType) {
            name.setDisplayName(getOrig(((AbstractRoleType) objectType).getDisplayName()));
        }
        return name;
    }

    private NameImpl createVisualizationName(String oid, ObjectReferenceType objectRef) {
        NameImpl nv = new NameImpl(oid);
        nv.setId(oid);
        if (objectRef != null && objectRef.asReferenceValue() != null && objectRef.asReferenceValue().getObject() != null) {
            PrismObject<ObjectType> object = objectRef.asReferenceValue().getObject();
            if (object.asObjectable().getName() != null) {
                nv.setDisplayName(object.asObjectable().getName().getOrig());
            }
        }
        return nv;
    }

}
