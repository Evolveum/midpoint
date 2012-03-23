package com.evolveum.midpoint.prism.delta;

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
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public class ContainerDelta<V extends Containerable> extends ItemDelta<PrismContainerValue<V>> implements PrismContainerable<V> {

	public ContainerDelta(PrismContainerDefinition itemDefinition) {
		super(itemDefinition);
	}

	public ContainerDelta(PropertyPath propertyPath, PrismContainerDefinition itemDefinition) {
		super(propertyPath, itemDefinition);
	}

	public ContainerDelta(PropertyPath parentPath, QName name, PrismContainerDefinition itemDefinition) {
		super(parentPath, name, itemDefinition);
	}

	public ContainerDelta(QName name, PrismContainerDefinition itemDefinition) {
		super(name, itemDefinition);
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
	public void applyDefinition(ItemDefinition definition) throws SchemaException {
		if (!(definition instanceof PrismContainerDefinition)) {
			throw new IllegalArgumentException("Cannot apply definition "+definition+" to container delta "+this);
		}
		super.applyDefinition(definition);
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
	
	@Override
	public ContainerDelta<V> clone() {
		ContainerDelta<V> clone = new ContainerDelta<V>(getName(), getDefinition());
		copyValues(clone);
		return clone;
	}
	
	protected void copyValues(ContainerDelta<V> clone) {
		super.copyValues(clone);
	}

	public static <T extends Containerable,O extends Objectable> ContainerDelta<T> createDelta(PrismContext prismContext, Class<O> type,
			QName containerName) {
    	PrismObjectDefinition<O> objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
    	return createDelta(objectDefinition, containerName);
    }
    
    public static <T extends Containerable,O extends Objectable> ContainerDelta<T> createDelta(PrismObjectDefinition<O> objectDefinition,
			QName containerName) {
		PrismContainerDefinition<T> containerDefinition = objectDefinition.findContainerDefinition(containerName);
		if (containerDefinition == null) {
			throw new IllegalArgumentException("No definition for "+containerName+" in "+objectDefinition);
		}
		ContainerDelta<T> delta = new ContainerDelta<T>(containerName, containerDefinition);
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
