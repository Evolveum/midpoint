/*
 * Copyright (c) 2010-2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.model.impl.visualizer;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.impl.visualizer.output.*;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.evolveum.midpoint.prism.delta.ChangeType.*;
import static com.evolveum.midpoint.prism.path.ItemPath.EMPTY_PATH;
import static com.evolveum.midpoint.prism.path.ItemPath.isNullOrEmpty;
import static com.evolveum.midpoint.prism.polystring.PolyString.getOrig;
import static com.evolveum.midpoint.schema.GetOperationOptions.createNoFetch;
import static com.evolveum.midpoint.schema.SelectorOptions.createCollection;

/**
 * @author mederly
 */

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

	public SceneImpl visualize(PrismObject<? extends ObjectType> object, Task task, OperationResult parentResult) throws SchemaException {
		return visualize(object, new VisualizationContext(), task, parentResult);
	}

	public SceneImpl visualize(PrismObject<? extends ObjectType> object, VisualizationContext context, Task task, OperationResult parentResult)
			throws SchemaException {
		OperationResult result = parentResult.createSubresult(CLASS_DOT + "visualize");
		try {
			resolver.resolve(object, task, result);
			return visualize(object, null, context, task, result);
		} catch (RuntimeException|SchemaException e) {
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
		scene.setSourcePath(EMPTY_PATH);
		scene.setSourceFullPath(EMPTY_PATH);
		scene.setSourceDefinition(object.getDefinition());
		scene.setSourceValue(object.getValue());
		scene.setSourceDelta(null);
		visualizeItems(scene, object.getValue().getItems(), context, task, result);
		return scene;
	}

	private SceneImpl visualize(PrismContainerValue<?> containerValue, SceneImpl owner, VisualizationContext context, Task task, OperationResult result) {
		SceneImpl scene = new SceneImpl(owner);
		scene.setChangeType(null);
		NameImpl name = new NameImpl("id " + containerValue.getId());		// TODO
		scene.setName(name);
		scene.setSourcePath(EMPTY_PATH);
		scene.setSourceFullPath(EMPTY_PATH);
		if (containerValue.getConcreteTypeDefinition() != null) {
			scene.setSourceDefinition(containerValue.getConcreteTypeDefinition());
		} else if (containerValue.getParent() != null && containerValue.getParent().getDefinition() != null) {
			scene.setSourceDefinition(containerValue.getParent().getDefinition());
		}
		scene.setSourceValue(containerValue);
		scene.setSourceDelta(null);
		visualizeItems(scene, containerValue.getItems(), context, task, result);
		return scene;
	}

	public SceneImpl visualizeDeltas(List<ObjectDelta<? extends ObjectType>> deltas, Task task, OperationResult parentResult) throws SchemaException {
		OperationResult result = parentResult.createSubresult(CLASS_DOT + "visualizeDeltas");
		try {
			resolver.resolve(deltas, task, result);
			return visualizeDeltas(deltas, new VisualizationContext(), task, result);
		} catch (RuntimeException|SchemaException e) {
			result.recordFatalError("Couldn't visualize the data structure: " + e.getMessage(), e);
			throw e;
		} finally {
			result.computeStatusIfUnknown();
		}
	}

	private SceneImpl visualizeDeltas(List<ObjectDelta<? extends ObjectType>> deltas, VisualizationContext context, Task task, OperationResult result)
			throws SchemaException {
		SceneImpl root = new SceneImpl(null);
		for (ObjectDelta<? extends ObjectType> delta : deltas) {
			root.addPartialScene(visualizeDelta(delta, root, context, task, result));
		}
		return root;
	}

	public SceneImpl visualizeDelta(ObjectDelta<? extends ObjectType> objectDelta, Task task, OperationResult parentResult) throws SchemaException {
		OperationResult result = parentResult.createSubresult(CLASS_DOT + "visualizeDelta");
		try {
			resolver.resolve(objectDelta, task, result);
			return visualizeDelta(objectDelta, null, new VisualizationContext(), task, result);
		} catch (RuntimeException|SchemaException e) {
			result.recordFatalError("Couldn't visualize the data structure: " + e.getMessage(), e);
			throw e;
		} finally {
			result.computeStatusIfUnknown();
		}
	}

	private SceneImpl visualizeDelta(ObjectDelta<? extends ObjectType> objectDelta, SceneImpl owner, VisualizationContext context, Task task, OperationResult result)
			throws SchemaException {
		SceneImpl scene = new SceneImpl(owner);
		scene.setChangeType(objectDelta.getChangeType());
		scene.setSourceDelta(objectDelta);
		scene.setSourcePath(ItemPath.EMPTY_PATH);
		scene.setSourceFullPath(ItemPath.EMPTY_PATH);
		PrismObject<? extends ObjectType> object =
				objectDelta.isAdd() ? objectDelta.getObjectToAdd() : getOldObject(objectDelta.getOid(), objectDelta.getObjectTypeClass(), context, task, result);
		if (object != null) {
			scene.setName(createSceneName(object));
			scene.setSourceValue(object.getValue());
			scene.setSourceDefinition(object.getDefinition());
		} else {
			scene.setName(createSceneName(objectDelta.getOid()));
		}
		if (objectDelta.isAdd()) {
			if (object == null) {
				throw new IllegalStateException("ADD object delta with no object to add: " + objectDelta);
			}
			visualizeItems(scene, object.getValue().getItems(), context, task, result);
		} else if (objectDelta.isModify()) {
			visualizeItemDeltas(scene, objectDelta.getModifications(), context, task, result);
		} else if (objectDelta.isDelete()) {
			// nothing more to do
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
			object = modelService.getObject(objectTypeClass, oid, createCollection(createNoFetch()), task, result);
			context.putObject(object);
			return object;
		} catch (RuntimeException|SchemaException|ConfigurationException|CommunicationException|SecurityViolationException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't resolve object {}", e, oid);
			result.recordWarning("Couldn't resolve object " + oid + ": " + e.getMessage(), e);
			return null;
		} catch (ObjectNotFoundException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't resolve object {}", e, oid);
			result.recordWarning("Couldn't resolve object " + oid + ": " + e.getMessage(), e);
			return null;
		}
	}

	private void visualizeItems(SceneImpl scene, List<Item<?, ?>> items, VisualizationContext context, Task task, OperationResult result) {
		if (items == null) {
			return;
		}
		List<Item<?, ?>> itemsToShow = new ArrayList<>(items);
		Collections.sort(itemsToShow, getItemDisplayOrderComparator());
		for (Item<?, ?> item : itemsToShow) {
			if (item instanceof PrismProperty) {
				scene.addItem(createSceneItem((PrismProperty) item));
			} else if (item instanceof PrismReference) {
				scene.addItem(createSceneItem((PrismReference) item, context, task, result));
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
						si.setName(name);
						if (def != null) {
							si.setOperational(def.isOperational());
							si.setSourceDefinition(def);
						}
						si.setSourcePath(new ItemPath(item.getElementName()));
						si.setSourceFullPath(scene.getSourceFullPath().subPath(item.getElementName()));
						si.setSourceDelta(null);
						scene.addPartialScene(si);
						currentScene = si;
					}
					visualizeItems(currentScene, pcv.getItems(), context, task, result);
				}
			} else {
				throw new IllegalStateException("Not a property nor reference nor container: " + item);
			}
		}
	}

	private NameImpl createSceneName(PrismContainer<?> pc, PrismContainerValue<?> pcv) {
		String elementName = pc.getElementName().getLocalPart();			// TODO NPE
		String idSuffix = pcv.getId() != null ? " #"+pcv.getId() : "";
		NameImpl name = new NameImpl(elementName + idSuffix);
		name.setDisplayName(pc.getDisplayName() + idSuffix);
		//name.setDescription(pc.getDefinition().getDocumentation());
		if (pcv.getId() != null) {
			name.setId(String.valueOf(pcv.getId()));
		}
		return name;
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
//		Collections.sort(deltasToShow, getDeltaDisplayOrderComparator());
		for (ItemDelta<?, ?> delta : deltasToShow) {
			if (delta instanceof ContainerDelta) {
				visualizeContainerDelta((ContainerDelta) delta, scene, context, task, result);
			} else {
				visualizeAtomicDelta(delta, scene, context, task, result);
			}
		}
	}

	private <C extends Containerable> void visualizeContainerDelta(ContainerDelta<C> delta, SceneImpl scene, VisualizationContext context, Task task, OperationResult result) {
		if (delta.isEmpty()) {
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
				if (oldValues == null || !oldValues.contains(newValue)) {
					valuesToAdd.add(newValue);
				}
			}
			if (oldValues != null) {
				for (PrismContainerValue<C> oldValue : oldValues) {
					if (!delta.getValuesToReplace().contains(oldValue)) {
						valuesToDelete.add(oldValue);
					}
				}
			}
		}
		if (valuesToDelete != null) {
			for (PrismContainerValue<C> value : valuesToDelete) {
				visualizeContainerDeltaValue(value, DELETE, delta.getPath(), scene, context, task, result);
			}
		}
		if (valuesToAdd != null) {
			for (PrismContainerValue<C> value : valuesToAdd) {
				visualizeContainerDeltaValue(value, ADD, delta.getPath(), scene, context, task, result);
			}
		}
	}

	private <C extends Containerable> void visualizeContainerDeltaValue(PrismContainerValue<C> value, ChangeType changeType,
			ItemPath containerPath, SceneImpl owningScene, VisualizationContext context, Task task, OperationResult result) {
		SceneImpl scene = createContainerScene(changeType, containerPath, owningScene);
		scene.setSourceValue(value);
		visualizeItems(scene, value.getItems(), context, task, result);

		owningScene.addPartialScene(scene);
	}

	private SceneImpl createContainerScene(ChangeType changeType, ItemPath containerPath, SceneImpl owningScene) {
		SceneImpl scene = new SceneImpl(owningScene);
		scene.setChangeType(changeType);

		ItemPath deltaParentItemPath = getDeltaParentItemPath(containerPath);
		PrismContainerDefinition<?> sceneDefinition = getSceneDefinition(scene, deltaParentItemPath);

		NameImpl name = createNameForContainerDelta(containerPath, scene);
		scene.setName(name);

		if (sceneDefinition != null) {
			scene.setOperational(sceneDefinition.isOperational());
			scene.setSourceDefinition(sceneDefinition);
		}

		ItemPath sceneRelativePath = containerPath.remainder(owningScene.getSourcePath());
		scene.setSourcePath(sceneRelativePath);
		scene.setSourceFullPath(containerPath);
		scene.setSourceDelta(null);
		return scene;
	}

	private NameImpl createNameForContainerDelta(ItemPath deltaParentPath, SceneImpl ownerScene) {
		NameImpl name = new NameImpl(deltaParentPath.toString());
		name.setId(String.valueOf(getLastId(deltaParentPath)));
		PrismContainerDefinition<?> sceneDefinition = getSceneDefinition(ownerScene, getDeltaParentItemPath(deltaParentPath));
		if (sceneDefinition != null) {
			name.setDisplayName(sceneDefinition.getDisplayName());
		}
		return name;
	}

	private ItemPath getDeltaParentItemPath(ItemPath deltaParentPath) {
		if (deltaParentPath.last() instanceof IdItemPathSegment) {
			return deltaParentPath.allExceptLast();
		} else {
			return deltaParentPath;
		}
	}

	private Long getLastId(ItemPath deltaParentPath) {
		if (deltaParentPath.last() instanceof IdItemPathSegment) {
			return ((IdItemPathSegment) deltaParentPath.last()).getId();
		} else {
			return null;
		}
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
		SceneImpl sceneForItem;
		if (isNullOrEmpty(deltaParentPath)) {
			sceneForItem = scene;
		} else {
			sceneForItem = findPartialSceneByPath(scene, deltaParentPath);
			if (sceneForItem == null) {
				sceneForItem = createContainerScene(MODIFY, deltaParentPath, scene);
				ItemPath sceneRelativeItemPath = getDeltaParentItemPath(deltaParentPath).remainder(scene.getSourcePath());

				PrismContainerValue<?> ownerPCV = scene.getSourceValue();
				if (ownerPCV != null) {
					Item<?,?> item = ownerPCV.findItem(sceneRelativeItemPath);
					if (item instanceof PrismContainer) {
						PrismContainer<?> container = (PrismContainer<?>) item;
						sceneForItem.setSourceDefinition(container.getDefinition());
						Long lastId = getLastId(deltaParentPath);
						if (lastId == null) {
							if (container.size() == 1) {
								sceneForItem.setSourceValue(container.getValues().get(0));
							}
						} else {
							PrismContainerValue<?> pcv = container.findValue(lastId);
							if (pcv != null) {
								sceneForItem.setSourceValue(pcv);
							}
						}
					}
				}
				scene.addPartialScene(sceneForItem);
			}
		}
		visualizeAtomicItemDelta(sceneForItem, delta, context, task, result);
	}

	private PrismContainerDefinition<?> getRootDefinition(SceneImpl scene) {
		while (scene.getOwner() != null) {
			scene = scene.getOwner();
		}
		return scene.getSourceDefinition();
	}

	private SceneImpl findPartialSceneByPath(SceneImpl scene, ItemPath deltaParentPath) {
		for (SceneImpl subscene : scene.getPartialScenes()) {
			if (subscene.getSourceFullPath().equivalent(deltaParentPath) && subscene.getChangeType() == MODIFY) {
				return subscene;
			}
		}
		return null;
	}

	private void visualizeAtomicItemDelta(SceneImpl scene, ItemDelta<?, ?> delta, VisualizationContext context, Task task, OperationResult result)
			throws SchemaException {
		if (delta instanceof PropertyDelta) {
			scene.addItem(createSceneDeltaItem((PropertyDelta) delta, scene, context, task, result));
		} else if (delta instanceof ReferenceDelta) {
			scene.addItem(createSceneDeltaItem((ReferenceDelta) delta, scene, context, task, result));
		} else {
			throw new IllegalStateException("No property nor reference delta: " + delta);
		}
	}

	//	private Comparator<ItemDelta<?, ?>> getDeltaDisplayOrderComparator() {
//		return new Comparator<ItemDelta<?, ?>>() {
//			@Override
//			public int compare(ItemDelta<?, ?> delta1, ItemDelta<?, ?> delta2) {
//
//			}
//		};
//	}


	private Comparator<Item<?, ?>> getItemDisplayOrderComparator() {
		return new Comparator<Item<?, ?>>() {
			@Override
			public int compare(Item<?, ?> o1, Item<?, ?> o2) {
				return compareDefinitions(o1.getDefinition(), o2.getDefinition());
			}
		};
	}

	private int compareDefinitions(ItemDefinition d1, ItemDefinition d2) {
		Integer order1 = d1 != null ? d1.getDisplayOrder() : null;
		Integer order2 = d2 != null ? d2.getDisplayOrder() : null;
		if (order1 == null && order2 == null) {
			return 0;
		} else if (order1 == null || order1 > order2) {
			return 1;
		} else if (order2 == null || order1 < order2) {
			return -1;
		} else {
			return 0;
		}
	}

	private SceneItemImpl createSceneItemCommon(Item<?,?> item) {
		SceneItemImpl si = new SceneItemImpl(createSceneItemName(item));
		ItemDefinition<?> def = item.getDefinition();
		if (def != null) {
			si.setOperational(def.isOperational());
		}
		si.setSourceItem(item);
		si.setSourcePath(new ItemPath(item.getElementName()));
		return si;
	}
	private SceneItemImpl createSceneItem(PrismProperty<?> property) {
		SceneItemImpl si = createSceneItemCommon(property);
		si.setNewValues(toSceneItemValues(property.getValues()));
		return si;
	}

	private SceneItemImpl createSceneItem(PrismReference reference, VisualizationContext context, Task task, OperationResult result) {
		SceneItemImpl si = createSceneItemCommon(reference);
		si.setNewValues(toStringValuesRef(reference.getValues(), context, task, result));
		return si;
	}

	private SceneDeltaItemImpl createSceneDeltaItem(PropertyDelta<?> delta, SceneImpl owningScene, VisualizationContext context, Task task,
			OperationResult result) throws SchemaException {
		SceneDeltaItemImpl si = createSceneDeltaItemCommon(delta, owningScene);
		si.setOldValues(toSceneItemValues(delta.getEstimatedOldValues()));			// TODO what if we don't know them?

		PrismProperty property = new PrismProperty(delta.getElementName(), prismContext);
		property.addValues(CloneUtil.cloneCollectionMembers(delta.getEstimatedOldValues()));
		try {
			delta.applyToMatchingPath(property);
		} catch (SchemaException e) {
			throw new SystemException("Couldn't visualize property delta: " + delta + ": " + e.getMessage(), e);
		}
		si.setNewValues(toSceneItemValues(property.getValues()));
		return si;
	}

	private <V extends PrismValue, D extends ItemDefinition> SceneDeltaItemImpl createSceneDeltaItemCommon(ItemDelta<V, D> itemDelta,
			SceneImpl owningScene)
			throws SchemaException {
		String simpleName = itemDelta.getElementName() != null ? itemDelta.getElementName().getLocalPart() : "";
		NameImpl name = new NameImpl(simpleName);
		if (itemDelta.getDefinition() != null) {
			name.setDisplayName(itemDelta.getDefinition().getDisplayName());
		}
		name.setId(simpleName);

		SceneDeltaItemImpl si = new SceneDeltaItemImpl(name);
		si.setSourceDelta(itemDelta);

		D def = itemDelta.getDefinition();
		if (def != null) {
			Item<V,D> item = def.instantiate();
			if (itemDelta.getEstimatedOldValues() != null) {
				item.addAll(CloneUtil.cloneCollectionMembers(itemDelta.getEstimatedOldValues()));
			}
			si.setSourceItem(item);
		}
		ItemPath remainder = itemDelta.getPath().remainder(owningScene.getSourcePath());
		if (remainder.startsWith(new ItemPath(new IdItemPathSegment()))) {
			remainder = remainder.tail();
		}
		si.setSourcePath(remainder);
		return si;
	}

	private NameImpl createSceneItemName(Item<?,?> item) {
		NameImpl name = new NameImpl(item.getElementName().getLocalPart());
		ItemDefinition<?> def = item.getDefinition();
		if (def != null) {
			name.setDisplayName(def.getDisplayName());
			name.setDescription(def.getDocumentation());
		}
		name.setId(name.getSimpleName());		// todo reconsider
		return name;
	}


	private SceneDeltaItemImpl createSceneDeltaItem(ReferenceDelta delta, SceneImpl owningScene, VisualizationContext context, Task task,
			OperationResult result)
			throws SchemaException {
		SceneDeltaItemImpl di = createSceneDeltaItemCommon(delta, owningScene);
		di.setOldValues(toStringValuesRef(delta.getEstimatedOldValues(), context, task, result));

		PrismReference reference = new PrismReference(delta.getElementName());
		try {
			reference.addAll(CloneUtil.cloneCollectionMembers(delta.getEstimatedOldValues()));
			delta.applyToMatchingPath(reference);
		} catch (SchemaException e) {
			throw new SystemException("Couldn't visualize reference delta: " + delta + ": " + e.getMessage(), e);
		}
		di.setNewValues(toStringValuesRef(reference.getValues(), context, task, result));
		return di;
	}


	private List<SceneItemValueImpl> toSceneItemValues(Collection<? extends PrismPropertyValue<?>> values) {
		List<SceneItemValueImpl> rv = new ArrayList<>();
		if (values != null) {
			for (PrismPropertyValue<?> value : values) {
				if (value != null) {
					SceneItemValueImpl siv = new SceneItemValueImpl(String.valueOf(value.getValue()));
					siv.setSourceValue(value);
					rv.add(siv);
				}
			}
		}
		return rv;
	}

	private List<SceneItemValueImpl> toStringValuesRef(Collection<PrismReferenceValue> refValues, VisualizationContext context, Task task, OperationResult result) {
		List<SceneItemValueImpl> rv = new ArrayList<>();
		if (refValues != null) {
			for (PrismReferenceValue refValue : refValues) {
				if (refValue != null) {
					refValue = createRefValueWithObject(refValue, context, task, result);
					SceneItemValueImpl itemValue = new SceneItemValueImpl(refValue.getTargetName() != null ? getOrig(refValue.getTargetName()) : refValue.getOid());
					itemValue.setSourceValue(refValue);
					rv.add(itemValue);
				}
			}
		}
		return rv;
	}

	private PrismReferenceValue createRefValueWithObject(PrismReferenceValue refValue, VisualizationContext context, Task task, OperationResult result) {
		if (refValue.getObject() != null) {
			return refValue;
		}
		PrismObject<? extends ObjectType> object = getObject(refValue.getOid(), (Class<? extends ObjectType>) refValue.getTargetTypeCompileTimeClass(), context, task, result);
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
		return name;
	}

	private NameImpl createSceneName(String oid, Class<? extends ObjectType> objectTypeClass, VisualizationContext context, Task task, OperationResult result) {
		PrismObject<? extends ObjectType> oldObject = getOldObject(oid, objectTypeClass, context, task, result);
		if (oldObject != null) {
			return createSceneName(oldObject);
		} else {
			return createSceneName(oid);
		}
	}

	private NameImpl createSceneName(String oid) {
		NameImpl nv = new NameImpl(oid);
		nv.setId(oid);
		return nv;
	}

	public SceneImpl visualizeDelta(ItemDelta<?,?> itemDelta) {
		return null;
	}

}
