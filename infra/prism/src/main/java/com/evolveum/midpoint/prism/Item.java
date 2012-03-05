/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;


/**
 * Item is a common abstraction of Property and PropertyContainer.
 * <p/>
 * This is supposed to be a superclass for all items. Items are things
 * that can appear in property containers, which generally means only a property
 * and property container itself. Therefore this is in fact superclass for those
 * two definitions.
 *
 * @author Radovan Semancik
 */
public abstract class Item<V extends PrismValue> implements Dumpable, DebugDumpable, Serializable {

	// The object should basically work without definition and prismContext. This is the
	// usual case when it is constructed "out of the blue", e.g. as a new JAXB object
	// It may not work perfectly, but basic things should work
    protected QName name;
    protected PrismValue parent;
    protected ItemDefinition definition;
    private List<V> values = new ArrayList<V>();
    
    transient protected PrismContext prismContext;

    /**
     * This is used for definition-less construction, e.g. in JAXB beans.
     * 
     * The constructors should be used only occasionally (if used at all).
     * Use the factory methods in the ResourceObjectDefintion instead.
     */
    Item(QName name) {
        super();
        this.name = name;
    }

    /**
     * The constructors should be used only occasionally (if used at all).
     * Use the factory methods in the ResourceObjectDefintion instead.
     */
    Item(QName name, ItemDefinition definition, PrismContext prismContext) {
        super();
        this.name = name;
        this.definition = definition;
        this.prismContext = prismContext;
    }
        
    /**
     * Returns applicable property definition.
     * <p/>
     * May return null if no definition is applicable or the definition is not
     * know.
     *
     * @return applicable property definition
     */
    public ItemDefinition getDefinition() {
        return definition;
    }

    /**
     * Returns the name of the property.
     * <p/>
     * The name is a QName. It uniquely defines a property.
     * <p/>
     * The name may be null, but such a property will not work.
     * <p/>
     * The name is the QName of XML element in the XML representation.
     *
     * @return property name
     */
    public QName getName() {
        return name;
    }

    /**
     * Sets the name of the property.
     * <p/>
     * The name is a QName. It uniquely defines a property.
     * <p/>
     * The name may be null, but such a property will not work.
     * <p/>
     * The name is the QName of XML element in the XML representation.
     *
     * @param name the name to set
     */
    public void setName(QName name) {
        this.name = name;
    }

    /**
     * Sets applicable property definition.
     *
     * @param definition the definition to set
     */
    public void setDefinition(PrismPropertyDefinition definition) {
        this.definition = definition;
    }

	/**
     * Returns a display name for the property type.
     * <p/>
     * Returns null if the display name cannot be determined.
     * <p/>
     * The display name is fetched from the definition. If no definition
     * (schema) is available, the display name will not be returned.
     *
     * @return display name for the property type
     */
    public String getDisplayName() {
        return getDefinition() == null ? null : getDefinition().getDisplayName();
    }

    /**
     * Returns help message defined for the property type.
     * <p/>
     * Returns null if the help message cannot be determined.
     * <p/>
     * The help message is fetched from the definition. If no definition
     * (schema) is available, the help message will not be returned.
     *
     * @return help message for the property type
     */
    public String getHelp() {
        return getDefinition() == null ? null : getDefinition().getHelp();
    }
    
    public PrismContext getPrismContext() {
    	return prismContext;
    }
    
    public PrismValue getParent() {
    	return parent;
    }
    
    public void setParent(PrismValue parentValue) {
    	this.parent = parentValue;
    }
    
    public PropertyPath getPath(PropertyPath pathPrefix) {
    	if (pathPrefix == null) {
    		return new PropertyPath(getName());
    	}
    	return pathPrefix.subPath(getName());
    }
    
    public List<V> getValues() {
		return values;
	}
    
    public V getValue(int index) {
    	if (index < 0) {
    		index = values.size() + index;
    	}
		return values.get(index);
	}
    
    public boolean hasValue(PrismValue value, boolean ignoreMetadata) {
    	return (findValue(value, ignoreMetadata) != null);
    }
    
    public boolean hasValue(PrismValue value) {
        return hasValue(value, false);
    }
    
    public boolean hasRealValue(PrismValue value) {
    	return hasValue(value, true);
    }
    
    /**
     * Returns value that is equal or equivalent to the provided value.
     * The returned value is an instance stored in this item, while the
     * provided value argument may not be.
     */
    public PrismValue findValue(PrismValue value, boolean ignoreMetadata) {
        for (PrismValue myVal : getValues()) {
            if (myVal.equals(value, ignoreMetadata)) {
                return myVal;
            }
        }
        return null;
    }
    
    /**
     * Returns value that is previous to the specified value.
     * Note that the order is semantically insignificant and this is used only
     * for presentation consistency in order-sensitive formats such as XML or JSON.
     */
    public PrismValue getPreviousValue(PrismValue value) {
    	PrismValue previousValue = null;
    	for (PrismValue myVal : getValues()) {
    		if (myVal == value) {
    			return previousValue;
    		}
    		previousValue = myVal;
    	}
    	throw new IllegalStateException("The value "+value+" is not any of "+this+" values, therefore cannot determine previous value");
    }

    /**
     * Returns values that is following the specified value.
     * Note that the order is semantically insignificant and this is used only
     * for presentation consistency in order-sensitive formats such as XML or JSON.
     */
    public PrismValue getNextValue(PrismValue value) {
    	Iterator<V> iterator = getValues().iterator();
    	while (iterator.hasNext()) {
    		PrismValue myVal = iterator.next();
    		if (myVal == value) {
    			if (iterator.hasNext()) {
    				return iterator.next();
    			} else {
    				return null;
    			}
    		}
    	}
    	throw new IllegalStateException("The value "+value+" is not any of "+this+" values, therefore cannot determine next value");
    }

    
    public boolean addAll(Collection<V> newValues) {
    	boolean changed = false;
    	for (V val: newValues) {
    		if (add(val)) {
    			changed = true;
    		}
    	}
    	return changed;
    }
    
    public boolean add(V newValue) {
    	newValue.setParent(this);
    	return values.add(newValue);
    }
    
    public boolean removeAll(Collection<V> newValues) {
    	return values.removeAll(newValues);
    }

    public boolean remove(V newValue) {
    	return values.remove(newValue);
    }
    
    public V remove(int index) {
    	return values.remove(index);
    }

    public void replaceAll(Collection<V> newValues) {
    	values.clear();
    	addAll(newValues);
    }

    public void replace(V newValue) {
    	values.clear();
    	values.add(newValue);
    }

    public List<Element> asDomElements() {
    	List<Element> elements = new ArrayList<Element>();
    	for (PrismValue pval: getValues()) {
    		elements.add(pval.asDomElement());
    	}
    	return elements;
    }
        
    protected void diffInternal(Item<V> other, PropertyPath pathPrefix, Collection<? extends ItemDelta> deltas, boolean ignoreMetadata) {
    	PropertyPath deltaPath = getPath(pathPrefix);
    	ItemDelta delta = null;
    	if (deltaPath != null && !deltaPath.isEmpty()) {
    		// DeltaPath can be empty in objects. But in that case we don't expect to create any delta at this level anyway.
    		delta = createDelta(deltaPath);
    	}
    	if (other == null) {
    		//other doesn't exist, so delta means delete all values
            for (PrismValue value : getValues()) {
                delta.addValueToDelete(value.clone());
            }
    	} else {
    		// the other exists, this means that we need to compare the values one by one
    		Collection<PrismValue> outstandingOtheValues = new ArrayList<PrismValue>(other.getValues().size());
    		outstandingOtheValues.addAll(other.getValues());
    		for (PrismValue thisValue : getValues()) {
    			Iterator<PrismValue> iterator = outstandingOtheValues.iterator();
    			boolean found = false;
    			while (iterator.hasNext()) {
    				PrismValue otherValue = iterator.next();
    				if (thisValue.representsSameValue(otherValue) || delta == null) {
    					found = true;
    					// Matching IDs, look inside to figure out internal deltas
    					thisValue.diffMatchingRepresentation(otherValue, pathPrefix, deltas, ignoreMetadata);
    					// No need to process this value again
    					iterator.remove();
    				} else if (thisValue.equals(otherValue, ignoreMetadata)) {
    					found = true;
    					// same values. No delta
    					// No need to process this value again
    					iterator.remove();
    				}
    			}
				if (!found) {
					// We have the value and the other does not, this is delete of the entire value
					delta.addValueToDelete(thisValue.clone());
				}
            }
    		// outstandingOtheValues are those values that the other has and we could not
    		// match them to any of our values. These must be new values to add
    		for (PrismValue outstandingOtherValue : outstandingOtheValues) {
    			delta.addValueToAdd(outstandingOtherValue.clone());
            }
    		// Some deltas may need to be polished a bit. E.g. transforming
    		// add/delete delta to a replace delta.
    		delta = fixupDelta(delta, other, pathPrefix, ignoreMetadata);
    	}
    	if (delta != null && !delta.isEmpty()) {
    		((Collection)deltas).add(delta);
    	}
    }
    
	protected ItemDelta fixupDelta(ItemDelta delta, Item<V> other, PropertyPath pathPrefix,
			boolean ignoreMetadata) {
		return delta;
	}

	public abstract ItemDelta<V> createDelta(PropertyPath path);
    
	/**
     * Serializes property to DOM or JAXB element(s).
     * <p/>
     * The property name will be used as an element QName.
     * The values will be in the element content. Single-value
     * properties will produce one element (on none), multi-valued
     * properies may produce several elements. All of the elements will
     * have the same QName.
     * <p/>
     * The property must have a definition (getDefinition() must not
     * return null).
     *
     * @param parentNode DOM Document
     * @return property serialized to DOM Element or JAXBElement
     * @throws SchemaException No definition or inconsistent definition
     */
//    abstract public void serializeToDom(Node parentNode) throws SchemaException;
    
	void applyDefinition(ItemDefinition definition) throws SchemaException {
		this.definition = definition;
		for (PrismValue pval: getValues()) {
			pval.applyDefinition(definition);
		}
	}
    
    public void revive(PrismContext prismContext) {
    	if (this.prismContext != null) {
    		return;
    	}
    	this.prismContext = prismContext;
    	if (definition != null) {
    		definition.revive(prismContext);
    	}
    }

    public abstract Item clone();

    protected void copyValues(Item clone) {
        clone.name = this.name;
        clone.definition = this.definition;
        clone.prismContext = this.prismContext;
    }
    
    public static <T extends Item> T createNewDefinitionlessItem(QName name, Class<T> type) {
    	T item = null;
		try {
			Constructor<T> constructor = type.getConstructor(QName.class);
			item = constructor.newInstance(name);
		} catch (Exception e) {
			throw new SystemException("Error creating new definitionless "+type.getSimpleName()+": "+e.getClass().getName()+" "+e.getMessage(),e);
		}
    	return item;
    }
    
    public boolean isEmpty() {
        return (getValues() == null || getValues().isEmpty());
    }

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((values == null) ? 0 : MiscUtil.unorderedCollectionHashcode(values));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Item other = (Item) obj;
		if (definition == null) {
			if (other.definition != null)
				return false;
		} else if (!definition.equals(other.definition))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		// Do not compare parent at all. This is not relevant.
		if (values == null) {
			if (other.values != null)
				return false;
		} else if (!MiscUtil.unorderedCollectionEquals(this.values, other.values))
			return false;
		return true;
	}

	@Override
    public String toString() {
        return getClass().getSimpleName() + "(" + getName() + ")";
    }

    @Override
    public String dump() {
        return toString();
    }

    /**
     * Provide terse and readable dump of the object suitable for log (at debug level).
     */
    public String debugDump() {
        return debugDump(0);
    }

    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append(INDENT_STRING);
        }
        sb.append(getDebugDumpClassName()).append(": ").append(DebugUtil.prettyPrint(getName()));
        return sb.toString();
    }

    /**
     * Return a human readable name of this class suitable for logs.
     */
    protected String getDebugDumpClassName() {
        return "Item";
    }


}
