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

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.api.context.ProjectionContextKey;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.api.visualizer.Scene;
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

    public SceneImpl visualize(PrismObject<? extends ObjectType> object, Task task, OperationResult parentResult) throws SchemaException, ExpressionEvaluationException {
        return visualize(object, new VisualizationContext(), task, parentResult);
    }

    public SceneImpl visualize(PrismObject<? extends ObjectType> object, VisualizationContext context, Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException {
        OperationResult result = parentResult.createSubresult(CLASS_DOT + "visualize");
        try {
            resolver.resolve(object, task, result);
            return visualize(object, null, context, task, result);
        } catch (RuntimeException | Error | SchemaException | ExpressionEvaluationException e) {
            result.recordFatalError("Couldn't visualize data structure: " + e.getMessage(), e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private SceneImpl visualize(PrismObject<? extends ObjectType> object, SceneImpl owner, VisualizationContext context, Task task, OperationResult result) {
        SceneImpl scene = new SceneImpl(owner);
        scene.setChangeType(null);
        scene.setName(createSceneName(object));
        scene.setSourceRelPath(EMPTY_PATH);
        scene.setSourceAbsPath(EMPTY_PATH);
        scene.setSourceDefinition(object.getDefinition());
        scene.setSourceValue(object.getValue());
        scene.setSourceDelta(null);
        visualizeItems(scene, object.getValue().getItems(), false, context, task, result);
        return scene;
    }

    @SuppressWarnings("unused")
    private SceneImpl visualize(PrismContainerValue<?> containerValue, SceneImpl owner, VisualizationContext context, Task task, OperationResult result) {
        SceneImpl scene = new SceneImpl(owner);
        scene.setChangeType(null);
        NameImpl name = new NameImpl("id " + containerValue.getId());        // TODO
        name.setNamesAreResourceKeys(false);
        scene.setName(name);
        scene.setSourceRelPath(EMPTY_PATH);
        scene.setSourceAbsPath(EMPTY_PATH);
        if (containerValue.getComplexTypeDefinition() != null) {
            // TEMPORARY!!!
            PrismContainerDefinition<?> pcd = prismContext.getSchemaRegistry().findContainerDefinitionByType(containerValue.getComplexTypeDefinition().getTypeName());
            scene.setSourceDefinition(pcd);
        } else if (containerValue.getParent() != null && containerValue.getParent().getDefinition() != null) {
            scene.setSourceDefinition(containerValue.getParent().getDefinition());
        }
        scene.setSourceValue(containerValue);
        scene.setSourceDelta(null);
        visualizeItems(scene, containerValue.getItems(), false, context, task, result);
        return scene;
    }

    public List<Scene> visualizeDeltas(List<ObjectDelta<? extends ObjectType>> deltas, Task task, OperationResult parentResult) throws SchemaException, ExpressionEvaluationException {
        OperationResult result = parentResult.createSubresult(CLASS_DOT + "visualizeDeltas");
        try {
            resolver.resolve(deltas, task, result);
            return visualizeDeltas(deltas, new VisualizationContext(), task, result);
        } catch (RuntimeException | Error | SchemaException | ExpressionEvaluationException e) {
            result.recordFatalError("Couldn't visualize the data structure: " + e.getMessage(), e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    public List<Scene> visualizeProjectionContexts(List<? extends ModelProjectionContext> projectionContexts, Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ConfigurationException {

        final List<Scene> scenes = new ArrayList<>();
        final OperationResult result = parentResult.createSubresult(CLASS_DOT + "visualizeProjectionContexts");

        try {
            for (ModelProjectionContext ctx : projectionContexts) {
                ObjectDelta executableDelta = CloneUtil.clone(ctx.getExecutableDelta());
                Scene scene = visualizeDelta(executableDelta, task, result);
                if (scene != null && !scene.isEmpty()) {
                    scenes.add(scene);
                }
            }
        } catch (RuntimeException | Error | SchemaException | ExpressionEvaluationException e) {
            result.recordFatalError("Couldn't visualize the data structure: " + e.getMessage(), e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
        }

        return scenes;
    }

    @NotNull
    public SceneImpl visualizeProjectionContext(ModelProjectionContext context, VisualizationContext vc, Task task, OperationResult result)
            throws SchemaException, ConfigurationException {

        SynchronizationPolicyDecision decision = context.getSynchronizationPolicyDecision();
        if (decision != SynchronizationPolicyDecision.BROKEN) {
            return visualizeDelta(context.getExecutableDelta(), null, null, vc, task, result);
        }

        ObjectDelta executable = context.getExecutableDelta();
        if (executable == null || !executable.isModify()) {
            return visualizeDelta(context.getExecutableDelta(), null, null, vc, task, result);
        }

        if (context.getObjectOld() != null || context.getObjectNew() == null) {
            return visualizeDelta(context.getExecutableDelta(), null, null, vc, task, result);
        }

        // Looks like, it should be an ADD not MODIFY delta (just a guess work since status is BROKEN
        ObjectDelta<ShadowType> addDelta = PrismContext.get().deltaFactory().object().create(context.getObjectTypeClass(),
                ChangeType.ADD);
        ResourceObjectDefinition objectTypeDef = context.getCompositeObjectDefinition();

        ProjectionContextKey key = context.getKey();
        if (objectTypeDef == null) {
            throw new IllegalStateException("Definition for account type " + key
                    + " not found in the context, but it should be there");
        }

        String resourceOid = null;
        if (context.getResource() != null) {
            resourceOid = context.getResource().getOid();
        } else {
            resourceOid = key.getResourceOid();
        }

        PrismObject<ShadowType> newAccount = objectTypeDef.createBlankShadow(resourceOid, key.getTag());
        addDelta.setObjectToAdd(newAccount);

        if (executable != null) {
            addDelta.merge(executable);
        }

        return visualizeDelta(addDelta, null, null, vc, task, result);
    }

    private List<Scene> visualizeDeltas(List<ObjectDelta<? extends ObjectType>> deltas, VisualizationContext context, Task task, OperationResult result)
            throws SchemaException {
        List<Scene> rv = new ArrayList<>(deltas.size());
        for (ObjectDelta<? extends ObjectType> delta : deltas) {
            if (delta.isEmpty()) {
                continue;
            }

            final SceneImpl scene = visualizeDelta(delta, null, null, context, task, result);
            if (!scene.isEmpty()) {
                rv.add(scene);
            }
        }
        return rv;
    }

    @NotNull
    public SceneImpl visualizeDelta(ObjectDelta<? extends ObjectType> objectDelta, Task task, OperationResult parentResult) throws SchemaException, ExpressionEvaluationException {
        return visualizeDelta(objectDelta, null, task, parentResult);
    }

    @NotNull
    public SceneImpl visualizeDelta(ObjectDelta<? extends ObjectType> objectDelta, ObjectReferenceType objectRef, Task task, OperationResult parentResult) throws SchemaException, ExpressionEvaluationException {
        return visualizeDelta(objectDelta, objectRef, false, true, task, parentResult);
    }

    @NotNull
    public SceneImpl visualizeDelta(ObjectDelta<? extends ObjectType> objectDelta, ObjectReferenceType objectRef,
            boolean includeOperationalItems, boolean includeOriginalObject, Task task, OperationResult parentResult) throws SchemaException, ExpressionEvaluationException {
        OperationResult result = parentResult.createSubresult(CLASS_DOT + "visualizeDelta");
        try {
            resolver.resolve(objectDelta, includeOriginalObject, task, result);
            VisualizationContext visualizationContext = new VisualizationContext();
            if (includeOperationalItems) {
                visualizationContext.setIncludeOperationalItems(includeOperationalItems);
            }
            return visualizeDelta(objectDelta, null, objectRef, visualizationContext, task, result);
        } catch (RuntimeException | Error | SchemaException | ExpressionEvaluationException e) {
            result.recordFatalError("Couldn't visualize the data structure: " + e.getMessage(), e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private SceneImpl visualizeDelta(ObjectDelta<? extends ObjectType> objectDelta, SceneImpl owner, ObjectReferenceType objectRef,
            VisualizationContext context, Task task, OperationResult result)
            throws SchemaException {
        SceneImpl scene = new SceneImpl(owner);
        scene.setChangeType(objectDelta.getChangeType());
        scene.setSourceDelta(objectDelta);
        scene.setSourceRelPath(ItemPath.EMPTY_PATH);
        scene.setSourceAbsPath(ItemPath.EMPTY_PATH);
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
            scene.setName(createSceneName(object));
            scene.setSourceValue(object.getValue());
            scene.setSourceDefinition(object.getDefinition());
        } else {
            scene.setName(createSceneName(objectDelta.getOid(), objectRef));
        }
        if (objectDelta.isAdd()) {
            if (object == null) {
                throw new IllegalStateException("ADD object delta with no object to add: " + objectDelta);
            }
            visualizeItems(scene, object.getValue().getItems(), false, context, task, result);
        } else if (objectDelta.isModify()) {
            if (object != null) {
                addDescriptiveItems(scene, object.getValue(), context, task, result);
            }
            visualizeItemDeltas(scene, objectDelta.getModifications(), context, task, result);
        } else if (objectDelta.isDelete()) {
            if (object != null) {
                addDescriptiveItems(scene, object.getValue(), context, task, result);
            }
        } else {
            throw new IllegalStateException("Object delta that is neither ADD, nor MODIFY nor DELETE: " + objectDelta);
        }
        return scene;
    }

    private PrismObject<? extends ObjectType> getOldObject(String oid, Class<? extends ObjectType> objectTypeClass, VisualizationContext context, Task task, OperationResult result) {
        PrismObject<? extends ObjectType> object = context.getOldObject(oid);
        if (object != null) {
            return object;
        }
        return getObject(oid, objectTypeClass, context, task, result);
    }

    private PrismObject<? extends ObjectType> getObject(String oid, Class<? extends ObjectType> objectTypeClass, VisualizationContext context, Task task, OperationResult result) {
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

    private void visualizeItems(SceneImpl scene, Collection<Item<?, ?>> items, boolean descriptive, VisualizationContext context, Task task, OperationResult result) {
        if (items == null) {
            return;
        }
        List<Item<?, ?>> itemsToShow = new ArrayList<>(items);
        Collections.sort(itemsToShow, getItemDisplayOrderComparator());
        for (Item<?, ?> item : itemsToShow) {
            if (item instanceof PrismProperty) {
                final SceneItemImpl sceneItem = createSceneItem((PrismProperty) item, descriptive);
                if (!sceneItem.isOperational() || context.isIncludeOperationalItems()) {
                    scene.addItem(sceneItem);
                }
            } else if (item instanceof PrismReference) {
                final SceneItemImpl sceneItem = createSceneItem((PrismReference) item, descriptive, context, task, result);
                if (!sceneItem.isOperational() || context.isIncludeOperationalItems()) {
                    scene.addItem(sceneItem);
                }
            } else if (item instanceof PrismContainer) {
                PrismContainer<?> pc = (PrismContainer<?>) item;
                PrismContainerDefinition<?> def = pc.getDefinition();
                boolean separate = isContainerSingleValued(def, pc) ? context.isSeparateSinglevaluedContainers() : context.isSeparateMultivaluedContainers();
                SceneImpl currentScene = scene;
                for (PrismContainerValue<?> pcv : pc.getValues()) {
                    if (separate) {
                        SceneImpl si = new SceneImpl(scene);
                        NameImpl name = new NameImpl(item.getElementName().getLocalPart());
                        name.setId(name.getSimpleName());
                        if (def != null) {
                            name.setDisplayName(def.getDisplayName());
                        }
                        name.setNamesAreResourceKeys(true);
                        si.setName(name);
                        if (def != null) {
                            si.setOperational(def.isOperational());
                            si.setSourceDefinition(def);
                            if (si.isOperational() && !context.isIncludeOperationalItems()) {
                                continue;
                            }
                        }
                        si.setSourceRelPath(ItemPath.create(item.getElementName()));
                        si.setSourceAbsPath(scene.getSourceAbsPath().append(item.getElementName()));
                        si.setSourceDelta(null);
                        scene.addPartialScene(si);
                        currentScene = si;
                    }
                    visualizeItems(currentScene, pcv.getItems(), descriptive, context, task, result);
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

    private void visualizeItemDeltas(SceneImpl scene, Collection<? extends ItemDelta<?, ?>> deltas, VisualizationContext context, Task task,
            OperationResult result) throws SchemaException {
        if (deltas == null) {
            return;
        }
        List<ItemDelta<?, ?>> deltasToShow = new ArrayList<>(deltas);
        for (ItemDelta<?, ?> delta : deltasToShow) {
            if (delta instanceof ContainerDelta) {
                visualizeContainerDelta((ContainerDelta) delta, scene, context, task, result);
            } else {
                visualizeAtomicDelta(delta, scene, context, task, result);
            }
        }
        sortItems(scene);
        sortPartialScenes(scene);
    }

    private void sortItems(SceneImpl scene) {
        Collections.sort(scene.getItems(), new Comparator<SceneItemImpl>() {
            @Override
            public int compare(SceneItemImpl o1, SceneItemImpl o2) {
                return compareDefinitions(o1.getSourceDefinition(), o2.getSourceDefinition());
            }
        });
    }

    private void sortPartialScenes(SceneImpl scene) {
        Collections.sort(scene.getPartialScenes(), new Comparator<SceneImpl>() {
            @Override
            public int compare(SceneImpl s1, SceneImpl s2) {
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
            }
        });
    }

    private <C extends Containerable> void visualizeContainerDelta(ContainerDelta<C> delta, SceneImpl scene, VisualizationContext context, Task task, OperationResult result) {
        if (delta.isEmpty()) {
            return;
        }
        if (delta.getDefinition() != null && delta.getDefinition().isOperational() && !context.isIncludeOperationalItems()) {
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
                visualizeContainerDeltaValue(value, DELETE, delta, scene, context, task, result);
            }
        }
        if (valuesToAdd != null) {
            for (PrismContainerValue<C> value : valuesToAdd) {
                visualizeContainerDeltaValue(value, ADD, delta, scene, context, task, result);
            }
        }
    }

    private <C extends Containerable> void visualizeContainerDeltaValue(PrismContainerValue<C> value, ChangeType changeType,
            ContainerDelta<C> containerDelta, SceneImpl owningScene, VisualizationContext context, Task task, OperationResult result) {
        SceneImpl scene = createContainerScene(changeType, containerDelta.getPath(), owningScene);
        if (value.getId() != null) {
            scene.getName().setId(String.valueOf(value.getId()));
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
        scene.setSourceValue(value);
        visualizeItems(scene, value.getItems(), false, context, task, result);

        owningScene.addPartialScene(scene);
    }

    private SceneImpl createContainerScene(ChangeType changeType, ItemPath containerPath, SceneImpl owningScene) {
        SceneImpl scene = new SceneImpl(owningScene);
        scene.setChangeType(changeType);

        ItemPath deltaParentItemPath = getDeltaParentItemPath(containerPath);
        PrismContainerDefinition<?> sceneDefinition = getSceneDefinition(scene, deltaParentItemPath);

        NameImpl name = createNameForContainerDelta(containerPath, sceneDefinition);
        scene.setName(name);

        if (sceneDefinition != null) {
            scene.setOperational(sceneDefinition.isOperational());
            scene.setSourceDefinition(sceneDefinition);
        }

        ItemPath sceneRelativePath = containerPath.remainder(owningScene.getSourceRelPath());
        scene.setSourceRelPath(sceneRelativePath);
        scene.setSourceAbsPath(containerPath);
        scene.setSourceDelta(null);
        return scene;
    }

    private NameImpl createNameForContainerDelta(ItemPath deltaParentPath, PrismContainerDefinition<?> sceneDefinition) {
        NameImpl name = new NameImpl(deltaParentPath.toString());
        name.setId(String.valueOf(getLastId(deltaParentPath)));
        if (sceneDefinition != null) {
            name.setDisplayName(sceneDefinition.getDisplayName());
        }
        name.setNamesAreResourceKeys(true);            // TODO: ok?
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

    private PrismContainerDefinition<?> getSceneDefinition(SceneImpl ownerScene, ItemPath deltaParentItemPath) {
        PrismContainerDefinition<?> rootDefinition = getRootDefinition(ownerScene);
        if (rootDefinition == null) {
            return null;
        } else {
            return rootDefinition.findContainerDefinition(deltaParentItemPath);
        }
    }

    private void visualizeAtomicDelta(ItemDelta<?, ?> delta, SceneImpl scene, VisualizationContext context, Task task, OperationResult result)
            throws SchemaException {
        ItemPath deltaParentPath = delta.getParentPath();
        ItemPath sceneRelativeItemPath = getDeltaParentItemPath(deltaParentPath).remainder(scene.getSourceRelPath());
        SceneImpl sceneForItem;
        if (ItemPath.isEmpty(deltaParentPath)) {
            sceneForItem = scene;
        } else {
            sceneForItem = findPartialSceneByPath(scene, deltaParentPath);
            if (sceneForItem == null) {
                sceneForItem = createContainerScene(MODIFY, deltaParentPath, scene);
                if (sceneForItem.isOperational() && !context.isIncludeOperationalItems()) {
                    return;
                }
                PrismContainerValue<?> ownerPCV = scene.getSourceValue();
                if (ownerPCV != null) {
                    Item<?, ?> item = ownerPCV.findItem(sceneRelativeItemPath);
                    if (item instanceof PrismContainer) {
                        PrismContainer<?> container = (PrismContainer<?>) item;
                        sceneForItem.setSourceDefinition(container.getDefinition());
                        Long lastId = getLastId(deltaParentPath);
                        PrismContainerValue<?> sceneSrcValue;
                        if (lastId == null) {
                            if (container.size() == 1) {
                                sceneSrcValue = container.getValues().get(0);
                            } else {
                                sceneSrcValue = null;
                            }
                        } else {
                            sceneSrcValue = container.findValue(lastId);
                        }
                        if (sceneSrcValue != null) {
                            sceneForItem.setSourceValue(sceneSrcValue);
                            addDescriptiveItems(sceneForItem, sceneSrcValue, context, task, result);
                        }
                    }
                }
                scene.addPartialScene(sceneForItem);
            }
        }
        ItemPath itemRelativeItemPath = getDeltaParentItemPath(delta.getPath()).remainder(sceneForItem.getSourceRelPath());
        if (context.isRemoveExtraDescriptiveItems()) {
            Iterator<? extends SceneItemImpl> iterator = sceneForItem.getItems().iterator();
            while (iterator.hasNext()) {
                SceneItemImpl sceneItem = iterator.next();
                if (sceneItem.isDescriptive() && sceneItem.getSourceRelPath() != null && sceneItem.getSourceRelPath().equivalent(itemRelativeItemPath)) {
                    iterator.remove();
                    break;
                }
            }
        }
        visualizeAtomicItemDelta(sceneForItem, delta, context, task, result);
    }

    private void addDescriptiveItems(SceneImpl scene, PrismContainerValue<?> sourceValue, VisualizationContext context, Task task, OperationResult result) {
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
        visualizeItems(scene, itemsToShow, true, context, task, result);
    }

    private PrismContainerDefinition<?> getRootDefinition(SceneImpl scene) {
        while (scene.getOwner() != null) {
            scene = scene.getOwner();
        }
        return scene.getSourceDefinition();
    }

    private SceneImpl findPartialSceneByPath(SceneImpl scene, ItemPath deltaParentPath) {
        for (SceneImpl subscene : scene.getPartialScenes()) {
            if (subscene.getSourceAbsPath().equivalent(deltaParentPath) && subscene.getChangeType() == MODIFY) {
                return subscene;
            }
        }
        return null;
    }

    private void visualizeAtomicItemDelta(SceneImpl scene, ItemDelta<?, ?> delta, VisualizationContext context, Task task, OperationResult result)
            throws SchemaException {
        final SceneDeltaItemImpl sceneDeltaItem;
        if (delta instanceof PropertyDelta) {
            sceneDeltaItem = createSceneDeltaItem((PropertyDelta) delta, scene, context, task, result);
        } else if (delta instanceof ReferenceDelta) {
            sceneDeltaItem = createSceneDeltaItem((ReferenceDelta) delta, scene, context, task, result);
        } else {
            throw new IllegalStateException("No property nor reference delta: " + delta);
        }
        if (!sceneDeltaItem.isOperational() || context.isIncludeOperationalItems()) {
            scene.addItem(sceneDeltaItem);
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

    private SceneItemImpl createSceneItemCommon(Item<?, ?> item) {
        SceneItemImpl si = new SceneItemImpl(createSceneItemName(item));
        ItemDefinition<?> def = item.getDefinition();
        if (def != null) {
            si.setOperational(def.isOperational());
        }
        si.setSourceItem(item);
        si.setSourceRelPath(item.getElementName());
        return si;
    }

    private SceneItemImpl createSceneItem(PrismProperty<?> property, boolean descriptive) {
        SceneItemImpl si = createSceneItemCommon(property);
        si.setNewValues(toSceneItemValues(property.getValues()));
        si.setDescriptive(descriptive);
        return si;
    }

    private SceneItemImpl createSceneItem(PrismReference reference, boolean descriptive, VisualizationContext context, Task task,
            OperationResult result) {
        SceneItemImpl si = createSceneItemCommon(reference);
        si.setNewValues(toSceneItemValuesRef(reference.getValues(), context, task, result));
        si.setDescriptive(descriptive);
        return si;
    }

    @SuppressWarnings({ "unused", "unchecked" })
    private SceneDeltaItemImpl createSceneDeltaItem(PropertyDelta<?> delta, SceneImpl owningScene, VisualizationContext context, Task task,
            OperationResult result) throws SchemaException {
        SceneDeltaItemImpl si = createSceneDeltaItemCommon(delta, owningScene);
        if (delta.isDelete() && CollectionUtils.isEmpty(delta.getEstimatedOldValues()) &&
                CollectionUtils.isNotEmpty(delta.getValuesToDelete())) {
            delta.setEstimatedOldValues((Collection) delta.getValuesToDelete());
        }
        si.setOldValues(toSceneItemValues(delta.getEstimatedOldValues()));

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
        si.setNewValues(toSceneItemValues(property.getValues()));
        return si;
    }

    private <V extends PrismPropertyValue<?>> void computeAddedDeletedUnchanged(SceneDeltaItemImpl si, Collection<V> oldValues, Collection<V> newValues) {
        List<V> added = new ArrayList<>();
        List<V> deleted = new ArrayList<>();
        List<V> unchanged = new ArrayList<>();
        computeDifferences(oldValues, newValues, added, deleted, unchanged);
        si.setAddedValues(toSceneItemValues(added));
        si.setDeletedValues(toSceneItemValues(deleted));
        si.setUnchangedValues(toSceneItemValues(unchanged));
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

    private void computeAddedDeletedUnchangedRef(SceneDeltaItemImpl si, Collection<PrismReferenceValue> oldValues, Collection<PrismReferenceValue> newValues,
            VisualizationContext context, Task task, OperationResult result) {
        List<PrismReferenceValue> added = new ArrayList<>();
        List<PrismReferenceValue> deleted = new ArrayList<>();
        List<PrismReferenceValue> unchanged = new ArrayList<>();
        computeDifferences(oldValues, newValues, added, deleted, unchanged);
        si.setAddedValues(toSceneItemValuesRef(added, context, task, result));
        si.setDeletedValues(toSceneItemValuesRef(deleted, context, task, result));
        si.setUnchangedValues(toSceneItemValuesRef(unchanged, context, task, result));
    }

    @SuppressWarnings("unchecked")
    private <V extends PrismValue, D extends ItemDefinition> SceneDeltaItemImpl createSceneDeltaItemCommon(ItemDelta<V, D> itemDelta,
            SceneImpl owningScene)
            throws SchemaException {
        String simpleName = itemDelta.getElementName() != null ? itemDelta.getElementName().getLocalPart() : "";
        NameImpl name = new NameImpl(simpleName);
        if (itemDelta.getDefinition() != null) {
            name.setDisplayName(itemDelta.getDefinition().getDisplayName());
        }
        name.setId(simpleName);
        name.setNamesAreResourceKeys(true);

        SceneDeltaItemImpl si = new SceneDeltaItemImpl(name);
        si.setSourceDelta(itemDelta);

        D def = itemDelta.getDefinition();
        if (def != null) {
            Item<V, D> item = def.instantiate();
            if (itemDelta.getEstimatedOldValues() != null) {
                item.addAll(CloneUtil.cloneCollectionMembers(itemDelta.getEstimatedOldValues()));
            }
            si.setSourceItem(item);
            si.setOperational(def.isOperational());
        }
        ItemPath remainder = itemDelta.getPath().remainder(owningScene.getSourceRelPath());
        if (remainder.startsWithNullId()) {
            remainder = remainder.rest();
        }
        si.setSourceRelPath(remainder);
        return si;
    }

    private NameImpl createSceneItemName(Item<?, ?> item) {
        NameImpl name = new NameImpl(item.getElementName().getLocalPart());
        ItemDefinition<?> def = item.getDefinition();
        if (def != null) {
            name.setDisplayName(def.getDisplayName());
            name.setDescription(def.getDocumentation());
        }
        name.setId(name.getSimpleName());        // todo reconsider
        name.setNamesAreResourceKeys(true);
        return name;
    }

    private SceneDeltaItemImpl createSceneDeltaItem(ReferenceDelta delta, SceneImpl owningScene, VisualizationContext context, Task task,
            OperationResult result)
            throws SchemaException {
        SceneDeltaItemImpl di = createSceneDeltaItemCommon(delta, owningScene);
        if (delta.isDelete() && CollectionUtils.isEmpty(delta.getEstimatedOldValues()) &&
                CollectionUtils.isNotEmpty(delta.getValuesToDelete())) {
            delta.setEstimatedOldValues((Collection) delta.getValuesToDelete());
        }
        di.setOldValues(toSceneItemValuesRef(delta.getEstimatedOldValues(), context, task, result));

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
        di.setNewValues(toSceneItemValuesRef(reference.getValues(), context, task, result));
        return di;
    }

    private List<SceneItemValueImpl> toSceneItemValues(Collection<? extends PrismPropertyValue<?>> values) {
        List<SceneItemValueImpl> rv = new ArrayList<>();
        if (values != null) {
            for (PrismPropertyValue<?> value : values) {
                if (value != null) {
                    SceneItemValueImpl siv = new SceneItemValueImpl(ValueDisplayUtil.toStringValue(value));
                    siv.setSourceValue(value);
                    rv.add(siv);
                }
            }
        }
        return rv;
    }

    private List<SceneItemValueImpl> toSceneItemValuesRef(Collection<PrismReferenceValue> refValues, VisualizationContext context, Task task, OperationResult result) {
        List<SceneItemValueImpl> rv = new ArrayList<>();
        if (refValues != null) {
            for (PrismReferenceValue refValue : refValues) {
                if (refValue != null) {
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
                    SceneItemValueImpl itemValue = new SceneItemValueImpl(name, relation);
                    itemValue.setSourceValue(refValue);
                    rv.add(itemValue);
                }
            }
        }
        return rv;
    }

    @SuppressWarnings("unchecked")
    private PrismReferenceValue createRefValueWithObject(PrismReferenceValue refValue, VisualizationContext context, Task task, OperationResult result) {
        if (refValue.getObject() != null) {
            return refValue;
        }
        PrismObject<? extends ObjectType> object = getObject(refValue.getOid(),
                (Class) refValue.getTargetTypeCompileTimeClass(prismContext), context, task, result);
        if (object == null) {
            return refValue;
        }
        refValue = refValue.clone();
        refValue.setObject(object);
        return refValue;
    }

    private NameImpl createSceneName(PrismObject<? extends ObjectType> object) {
        NameImpl name = new NameImpl(object.getName() != null ? getOrig(object.getName()) : object.getOid());
        name.setId(object.getOid());
        ObjectType objectType = object.asObjectable();
        name.setDescription(objectType.getDescription());
        if (objectType instanceof UserType) {
            name.setDisplayName(getOrig(((UserType) objectType).getFullName()));
        } else if (objectType instanceof AbstractRoleType) {
            name.setDisplayName(getOrig(((AbstractRoleType) objectType).getDisplayName()));
        }
        name.setNamesAreResourceKeys(false);
        return name;
    }

    private NameImpl createSceneName(String oid, ObjectReferenceType objectRef) {
        NameImpl nv = new NameImpl(oid);
        nv.setId(oid);
        if (objectRef != null && objectRef.asReferenceValue() != null && objectRef.asReferenceValue().getObject() != null) {
            PrismObject<ObjectType> object = objectRef.asReferenceValue().getObject();
            if (object.asObjectable().getName() != null) {
                nv.setDisplayName(object.asObjectable().getName().getOrig());
            }
        }
        nv.setNamesAreResourceKeys(false);
        return nv;
    }

}
