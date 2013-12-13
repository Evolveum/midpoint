/*
 * Copyright (c) 2010-2013 Evolveum
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContainerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public class ContainerDelta<V extends Containerable> extends ItemDelta<PrismContainerValue<V>> implements PrismContainerable<V> {

	public ContainerDelta(PrismContainerDefinition itemDefinition) {
		super(itemDefinition);
	}

	public ContainerDelta(ItemPath propertyPath, PrismContainerDefinition itemDefinition) {
		super(propertyPath, itemDefinition);
	}

	public ContainerDelta(ItemPath parentPath, QName name, PrismContainerDefinition itemDefinition) {
		super(parentPath, name, itemDefinition);
    	// Extra check. It makes no sense to create container delta with object definition
    	if (itemDefinition instanceof PrismObjectDefinition<?>) {
    		throw new IllegalArgumentException("Cannot apply "+definition+" to container delta");
    	}
	}

	public ContainerDelta(QName name, PrismContainerDefinition itemDefinition) {
		super(name, itemDefinition);
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
	public void setDefinition(ItemDefinition definition) {
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
	public void applyDefinition(ItemDefinition definition) throws SchemaException {
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
	public void applyTo(Item item) throws SchemaException {
		if (!(item instanceof PrismContainer)) {
			throw new SchemaException("Cannot apply container delta "+this+" to item "+item+" of type "+item.getClass());
		}
		super.applyTo(item);
	}
	
	public ItemDelta<?> findItemDelta(ItemPath path) {
		if (path.isEmpty()) {
			return this;
		}
		ItemDefinition itemDefinition = getDefinition().findItemDefinition(path);
		ItemDelta<?> itemDelta = itemDefinition.createEmptyDelta(getPath().subPath(path));
		itemDelta.addValuesToAdd(findItemValues(path, getValuesToAdd()));
		itemDelta.addValuesToDelete(findItemValues(path, getValuesToDelete()));
		itemDelta.setValuesToReplace(findItemValues(path, getValuesToReplace()));
		if (itemDelta.isEmpty()) {
			return null;
		}
		return itemDelta;
	}
	
	private Collection findItemValues(ItemPath path, Collection<PrismContainerValue<V>> cvalues) {
		if (cvalues == null) {
			return null;
		}
		Collection<PrismValue> subValues = new ArrayList<PrismValue>();
		for (PrismContainerValue<V> cvalue: cvalues) {
			Item<?> item = cvalue.findItem(path);
			if (item != null) {
				subValues.addAll(item.getValues());
			}
		}
		return subValues;
	}

	@Override
	public ContainerDelta<V> clone() {
		ContainerDelta<V> clone = new ContainerDelta<V>(getName(), getDefinition());
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
		ContainerDelta<T> delta = new ContainerDelta<T>(containerPath, containerDefinition);
		return delta;
	}
    
    public static <T extends Containerable,O extends Objectable> ContainerDelta<T> createModificationAdd(QName containerName, 
    		Class<O> type, PrismContext prismContext, T containerable) throws SchemaException {
    	return createModificationAdd(new ItemPath(containerName), type, prismContext, containerable);
    }
    
    public static <T extends Containerable,O extends Objectable> ContainerDelta<T> createModificationAdd(ItemPath containerPath, 
    		Class<O> type, PrismContext prismContext, T containerable) throws SchemaException {
    	ContainerDelta<T> delta = createDelta(containerPath, type, prismContext);
    	PrismContainerValue<T> cval = containerable.asPrismContainerValue();
    	prismContext.adopt(cval, type, containerPath);
    	delta.addValuesToAdd(cval);
    	return delta;
    }
    
    @Override
    protected void dumpValues(StringBuilder sb, String label, Collection<PrismContainerValue<V>> values, int indent) {
        for (int i = 0; i < indent; i++) {
            sb.append(INDENT_STRING);
        }
        sb.append(label).append(":");
        if (values == null) {
            sb.append(" (null)");
        } else {
        	sb.append("\n");
            Iterator<PrismContainerValue<V>> i = values.iterator();
            while (i.hasNext()) {
                sb.append(i.next().debugDump(indent+1));
                if (i.hasNext()) {
                    sb.append("\n");
                }
            }
        }
    }
	
}
