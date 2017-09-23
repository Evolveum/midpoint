/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.prism.delta;

import java.util.*;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import org.apache.commons.collections4.CollectionUtils;

public class ContainerDelta<V extends Containerable> extends ItemDelta<PrismContainerValue<V>,PrismContainerDefinition<V>> implements PrismContainerable<V> {

	public ContainerDelta(PrismContainerDefinition itemDefinition, PrismContext prismContext) {
		super(itemDefinition, prismContext);
	}

	public ContainerDelta(ItemPath propertyPath, PrismContainerDefinition itemDefinition, PrismContext prismContext) {
		super(propertyPath, itemDefinition, prismContext);
	}

	public ContainerDelta(ItemPath parentPath, QName name, PrismContainerDefinition itemDefinition, PrismContext prismContext) {
		super(parentPath, name, itemDefinition, prismContext);
    	// Extra check. It makes no sense to create container delta with object definition
    	if (itemDefinition instanceof PrismObjectDefinition<?>) {
    		throw new IllegalArgumentException("Cannot apply "+definition+" to container delta");
    	}
	}

	public ContainerDelta(QName name, PrismContainerDefinition itemDefinition, PrismContext prismContext) {
		super(name, itemDefinition, prismContext);
    	// Extra check. It makes no sense to create container delta with object definition
    	if (itemDefinition instanceof PrismObjectDefinition<?>) {
    		throw new IllegalArgumentException("Cannot apply "+definition+" to container delta");
    	}
	}

	@Override
	public Class<PrismContainer> getItemClass() {
		return PrismContainer.class;
	}

	/**
     * Returns all values regardless of whether they are added or removed or replaced.
     * Useful for iterating over all the changed values.
     */
    public <T extends Containerable> Collection<PrismContainerValue<T>> getValues(Class<T> type) {
        checkConsistence();
        if (valuesToReplace != null) {
            return (Collection) valuesToReplace;
        }
        return (Collection) MiscUtil.union(valuesToAdd, valuesToDelete);
    }

    @Override
	public PrismContainerDefinition<V> getDefinition() {
		return (PrismContainerDefinition<V>) super.getDefinition();
	}

    @Override
	public void setDefinition(PrismContainerDefinition<V> definition) {
    	if (!(definition instanceof PrismContainerDefinition)) {
			throw new IllegalArgumentException("Cannot apply "+definition+" to container delta");
		}
    	// Extra check. It makes no sense to create container delta with object definition
    	if (definition instanceof PrismObjectDefinition<?>) {
    		throw new IllegalArgumentException("Cannot apply "+definition+" to container delta");
    	}
		super.setDefinition(definition);
	}

	@Override
	public void applyDefinition(PrismContainerDefinition<V> definition) throws SchemaException {
		if (!(definition instanceof PrismContainerDefinition)) {
			throw new IllegalArgumentException("Cannot apply definition "+definition+" to container delta "+this);
		}
		super.applyDefinition(definition);
	}

	@Override
	public boolean hasCompleteDefinition() {
		if (!super.hasCompleteDefinition()) {
			return false;
		}
		if (!hasCompleteDefinition(getValuesToAdd())) {
			return false;
		}
		if (!hasCompleteDefinition(getValuesToDelete())) {
			return false;
		}
		if (!hasCompleteDefinition(getValuesToReplace())) {
			return false;
		}
		return true;
	}

	private boolean hasCompleteDefinition(Collection<PrismContainerValue<V>> values) {
		if (values == null) {
			return true;
		}
		for (PrismContainerValue<V> value: values) {
			if (!value.hasCompleteDefinition()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Class<V> getCompileTimeClass() {
		if (getDefinition() != null) {
			return getDefinition().getCompileTimeClass();
		}
		return null;
	}

	@Override
	protected boolean isApplicableToType(Item item) {
		return item instanceof PrismContainer;
	}

	@Override
	public ItemDelta<?,?> getSubDelta(ItemPath path) {
		if (path.isEmpty()) {
			return this;
		}
		Long id = null;
		ItemPathSegment first = path.first();
    	if (first instanceof IdItemPathSegment) {
    		id = ((IdItemPathSegment)first).getId();
    		path = path.rest();
    	}
		ItemDefinition itemDefinition = getDefinition().findItemDefinition(path);
    	if (itemDefinition == null) {
    		throw new IllegalStateException("No definition of " + path + " in " + getDefinition());
		}
		ItemDelta<?,?> itemDelta = itemDefinition.createEmptyDelta(getPath().subPath(path));
		itemDelta.addValuesToAdd(findItemValues(id, path, getValuesToAdd()));
		itemDelta.addValuesToDelete(findItemValues(id, path, getValuesToDelete()));
		itemDelta.setValuesToReplace(findItemValues(id, path, getValuesToReplace()));
		if (itemDelta.isEmpty()) {
			return null;
		}
		return itemDelta;
	}

	private Collection findItemValues(Long id, ItemPath path, Collection<PrismContainerValue<V>> cvalues) {
		if (cvalues == null) {
			return null;
		}
		Collection<PrismValue> subValues = new ArrayList<PrismValue>();
		for (PrismContainerValue<V> cvalue: cvalues) {
			if (id == null || id == cvalue.getId()) {
				Item<?,?> item = cvalue.findItem(path);
				if (item != null) {
					subValues.addAll(PrismValue.cloneCollection(item.getValues()));
				}
			}
		}
		return subValues;
	}

	/**
	 * Post processing of delta to expand missing values from the object. E.g. a delete deltas may
	 * be "id-only" so they contain only id of the value to delete. In such case locate the full value
	 * in the object and fill it into the delta.
	 * This method may even delete id-only values that are no longer present in the object.
	 *
	 * It also fills-in IDs for values to be added/replaced/deleted.
	 */
	public <O extends Objectable> void expand(PrismObject<O> object, Trace logger) throws SchemaException {
		PrismContainer<Containerable> container = null;
		ItemPath path = this.getPath();
		if (object != null) {
			container = object.findContainer(path);
		}
		if (valuesToDelete != null) {
			Iterator<PrismContainerValue<V>> iterator = valuesToDelete.iterator();
			while (iterator.hasNext()) {
				PrismContainerValue<V> deltaCVal = iterator.next();
				if (CollectionUtils.isEmpty(deltaCVal.getItems())) {
					Long id = deltaCVal.getId();
					if (id == null) {
						throw new IllegalArgumentException("No id and no items in value "+deltaCVal+" in delete set in "+this);
					}
					if (container != null) {
						PrismContainerValue<Containerable> containerCVal = container.findValue(id);
						if (containerCVal != null) {
							for (Item<?,?> containerItem: containerCVal.getItems()) {
								deltaCVal.add(containerItem.clone());
							}
						}
					}
					// id-only value with ID that is not in the object any more: delete the value from delta
					iterator.remove();
				} else if (deltaCVal.getId() == null) {
					if (container != null) {
						@SuppressWarnings("unchecked")
						List<PrismContainerValue<Containerable>> containerCVals =
								(List<PrismContainerValue<Containerable>>)
										container.findValuesIgnoreMetadata(deltaCVal);
						if (containerCVals.size() > 1) {
							logger.warn("More values to be deleted are matched by a single value in delete delta: values={}, delta value={}",
									containerCVals, deltaCVal);
						} else if (containerCVals.size() == 1) {
							deltaCVal.setId(containerCVals.get(0).getId());
						}
						// for the time being let's keep non-existent values in the delta
					}
				}
			}
		}
		if (valuesToAdd != null) {
			assert valuesToReplace == null;
			long maxId = getMaxId(container);
			processIdentifiers(maxId, valuesToAdd);
		}
		if (valuesToReplace != null) {
			assert valuesToAdd == null;
			processIdentifiers(0, valuesToReplace);
		}
	}

	private void processIdentifiers(long maxId, Collection<PrismContainerValue<V>> values) {
		for (PrismContainerValue<V> value : values) {
			if (value.getId() != null && value.getId() > maxId) {
				maxId = value.getId();
			}
		}
		for (PrismContainerValue<V> value : values) {
			if (value.getId() == null) {
				value.setId(++maxId);
			}
		}
	}

	private long getMaxId(PrismContainer<?> container) {
		if (container == null) {
			return 0;
		}
		long max = 0;
		for (PrismContainerValue<?> value : container.getValues()) {
			if (value.getId() != null && value.getId() > max) {
				max = value.getId();
			}
		}
		return max;
	}

	@Override
	protected boolean isValueEquivalent(PrismContainerValue<V> a, PrismContainerValue<V> b) {
		if (!super.isValueEquivalent(a, b)) {
			return false;
		}
		if (a.getId() == null || b.getId() == null) {
			return true;
		} else {
			return a.getId().equals(b.getId());
		}
	}

	@Override
    public void checkConsistence(boolean requireDefinition, boolean prohibitRaw, ConsistencyCheckScope scope) {
        super.checkConsistence(requireDefinition, prohibitRaw, scope);
        checkDuplicateId(valuesToAdd);
    }

    private void checkDuplicateId(Collection<PrismContainerValue<V>> valuesToAdd) {
        if (valuesToAdd == null || valuesToAdd.isEmpty()) {
            return;
        }
        Set<Long> idsToAdd = new HashSet<>();
        for (PrismContainerValue<V> valueToAdd : valuesToAdd) {
            Long id = valueToAdd.getId();
            if (id != null) {
                if (idsToAdd.contains(id)) {
                    throw new IllegalArgumentException("Trying to add prism container value with id " + id + " multiple times");
                } else {
                    idsToAdd.add(id);
                }
            }
        }
    }

    @Override
	public ContainerDelta<V> clone() {
		ContainerDelta<V> clone = new ContainerDelta<V>(getElementName(), getDefinition(), getPrismContext());
		copyValues(clone);
		return clone;
	}

	protected void copyValues(ContainerDelta<V> clone) {
		super.copyValues(clone);
	}

	public static <T extends Containerable,O extends Objectable> ContainerDelta<T> createDelta(QName containerName,
			Class<O> type, PrismContext prismContext) {
    	return createDelta(new ItemPath(containerName), type, prismContext);
    }

	public static <T extends Containerable,O extends Objectable> ContainerDelta<T> createDelta(ItemPath containerPath,
			Class<O> type, PrismContext prismContext) {
    	PrismObjectDefinition<O> objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
    	return createDelta(containerPath, objectDefinition);
    }

	public static <T extends Containerable,O extends Objectable> ContainerDelta<T> createDelta(QName containerName,
    		PrismObjectDefinition<O> objectDefinition) {
		return createDelta(new ItemPath(containerName), objectDefinition);
	}

    public static <T extends Containerable,O extends Objectable> ContainerDelta<T> createDelta(ItemPath containerPath,
    		PrismObjectDefinition<O> objectDefinition) {
		PrismContainerDefinition<T> containerDefinition = objectDefinition.findContainerDefinition(containerPath);
		if (containerDefinition == null) {
			throw new IllegalArgumentException("No definition for "+containerPath+" in "+objectDefinition);
		}
		ContainerDelta<T> delta = new ContainerDelta<T>(containerPath, containerDefinition, objectDefinition.getPrismContext());
		return delta;
	}

	public static <T extends Containerable> ContainerDelta<T> createDelta(QName containerName, PrismContainerDefinition<T> containerDefinition) {
		ContainerDelta<T> delta = new ContainerDelta<T>(new ItemPath(containerName), containerDefinition, containerDefinition.getPrismContext());
		return delta;
	}


	public static <T extends Containerable,O extends Objectable> ContainerDelta<T> createModificationAdd(QName containerName,
    		Class<O> type, PrismContext prismContext, T containerable) throws SchemaException {
    	return createModificationAdd(new ItemPath(containerName), type, prismContext, containerable);
    }

    public static <T extends Containerable,O extends Objectable> ContainerDelta<T> createModificationAdd(ItemPath containerPath,
    		Class<O> type, PrismContext prismContext, T containerable) throws SchemaException {
    	return createModificationAdd(containerPath, type, prismContext, containerable.asPrismContainerValue());
    }

    public static <T extends Containerable,O extends Objectable> ContainerDelta<T> createModificationAdd(QName containerName,
    		Class<O> type, PrismContext prismContext, PrismContainerValue<T> cValue) throws SchemaException {
    	return createModificationAdd(new ItemPath(containerName), type, prismContext, cValue);
    }

    public static <T extends Containerable,O extends Objectable> ContainerDelta<T> createModificationAdd(ItemPath containerPath,
    		Class<O> type, PrismContext prismContext, PrismContainerValue<T> cValue) throws SchemaException {
    	ContainerDelta<T> delta = createDelta(containerPath, type, prismContext);
    	prismContext.adopt(cValue, type, containerPath);
    	delta.addValuesToAdd(cValue);
    	return delta;
    }

    public static <T extends Containerable,O extends Objectable> ContainerDelta<T> createModificationDelete(QName containerName,
    		Class<O> type, PrismContext prismContext, T containerable) throws SchemaException {
    	return createModificationDelete(new ItemPath(containerName), type, prismContext, containerable);
    }

    public static <T extends Containerable,O extends Objectable> ContainerDelta<T> createModificationDelete(ItemPath containerPath,
    		Class<O> type, PrismContext prismContext, T containerable) throws SchemaException {
    	return createModificationDelete(containerPath, type, prismContext, containerable.asPrismContainerValue());
    }

    public static <T extends Containerable,O extends Objectable> ContainerDelta<T> createModificationDelete(QName containerName,
    		Class<O> type, PrismContext prismContext, PrismContainerValue<T> cValue) throws SchemaException {
    	return createModificationDelete(new ItemPath(containerName), type, prismContext, cValue);
    }

    public static <T extends Containerable,O extends Objectable> ContainerDelta<T> createModificationDelete(ItemPath containerPath,
    		Class<O> type, PrismContext prismContext, PrismContainerValue<T> cValue) throws SchemaException {
    	ContainerDelta<T> delta = createDelta(containerPath, type, prismContext);
    	prismContext.adopt(cValue, type, containerPath);
    	delta.addValuesToDelete(cValue);
    	return delta;
    }

    public static <T extends Containerable,O extends Objectable> ContainerDelta<T> createModificationReplace(QName containerName,
    		Class<O> type, PrismContext prismContext, T containerable) throws SchemaException {
    	return createModificationReplace(new ItemPath(containerName), type, prismContext, containerable);
    }

	public static <T extends Containerable,O extends Objectable> ContainerDelta<T> createModificationReplace(QName containerName,
																											 Class<O> type, PrismContext prismContext, Collection<T> containerables) throws SchemaException {
		return createModificationReplace(new ItemPath(containerName), type, prismContext, containerables);
	}

    public static <T extends Containerable,O extends Objectable> ContainerDelta<T> createModificationReplace(ItemPath containerPath,
    		Class<O> type, PrismContext prismContext, T containerable) throws SchemaException {
    	return createModificationReplace(containerPath, type, prismContext, containerable.asPrismContainerValue());
    }

	public static <T extends Containerable,O extends Objectable> ContainerDelta<T> createModificationReplace(ItemPath containerPath,
																											 Class<O> type, PrismContext prismContext, Collection<T> containerables) throws SchemaException {
		ContainerDelta<T> delta = createDelta(containerPath, type, prismContext);
		List<PrismContainerValue<T>> pcvs = new ArrayList<>();
		for (Containerable c: containerables) {
			pcvs.add(c.asPrismContainerValue());
		}
		delta.setValuesToReplace(pcvs);
		return delta;
	}

    public static <T extends Containerable,O extends Objectable> ContainerDelta<T> createModificationReplace(QName containerName,
    		Class<O> type, PrismContext prismContext, PrismContainerValue<T> cValue) throws SchemaException {
    	return createModificationReplace(new ItemPath(containerName), type, prismContext, cValue);
    }

    public static <T extends Containerable,O extends Objectable> ContainerDelta<T> createModificationReplace(ItemPath containerPath,
    		Class<O> type, PrismContext prismContext, PrismContainerValue<T> cValue) throws SchemaException {
    	ContainerDelta<T> delta = createDelta(containerPath, type, prismContext);
    	prismContext.adopt(cValue, type, containerPath);
    	delta.setValuesToReplace(cValue);
    	return delta;
    }

	// cValue should be parent-less
	public static <T extends Containerable> ContainerDelta<T> createModificationReplace(QName containerName, PrismContainerDefinition containerDefinition, PrismContainerValue<T> cValue) throws SchemaException {
		ContainerDelta<T> delta = createDelta(containerName, containerDefinition);
		delta.setValuesToReplace(cValue);
		return delta;
	}

	// cValues should be parent-less
	public static Collection<? extends ItemDelta> createModificationReplaceContainerCollection(QName containerName,
																							   PrismObjectDefinition<?> objectDefinition, PrismContainerValue... cValues) {
		Collection<? extends ItemDelta> modifications = new ArrayList<ItemDelta>(1);
		ContainerDelta delta = createDelta(containerName, objectDefinition.findContainerDefinition(containerName));
		delta.setValuesToReplace(cValues);
		((Collection)modifications).add(delta);
		return modifications;
	}

	// cValues should be parent-less
	public static <T extends Containerable> ContainerDelta<T> createModificationReplace(QName containerName, PrismObjectDefinition<?> objectDefinition, PrismContainerValue... cValues) {
		ContainerDelta delta = createDelta(containerName, objectDefinition.findContainerDefinition(containerName));
		delta.setValuesToReplace(cValues);
		return delta;
	}

	@Override
    protected void dumpValues(StringBuilder sb, String label, Collection<PrismContainerValue<V>> values, int indent) {
		DebugUtil.debugDumpLabel(sb, label, indent);
        if (values == null) {
            sb.append(" (null)");
        } else if (values.isEmpty()) {
        	sb.append(" (empty)");
        } else {
        	for (PrismContainerValue<V> val: values) {
        		sb.append("\n");
        		sb.append(val.debugDump(indent+1));
        	}
        }
    }

}
