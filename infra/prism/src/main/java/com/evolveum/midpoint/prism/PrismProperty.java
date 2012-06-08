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
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import java.lang.reflect.Array;
import java.util.*;


/**
 * Property is a specific characteristic of an object. It may be considered
 * object "attribute" or "field". For example User has fullName property that
 * contains string value of user's full name.
 * <p/>
 * Properties may be single-valued or multi-valued
 * <p/>
 * Properties may contain primitive types or complex types (defined by XSD
 * schema)
 * <p/>
 * Property values are unordered, implementation may change the order of values
 * <p/>
 * Duplicate values of properties should be silently removed by implementations,
 * but clients must be able tolerate presence of duplicate values.
 * <p/>
 * Operations that modify the objects work with the granularity of properties.
 * They add/remove/replace the values of properties, but do not "see" inside the
 * property.
 * <p/>
 * Property is mutable.
 *
 * @author Radovan Semancik
 */
public class PrismProperty<V> extends Item<PrismPropertyValue<V>> {

    private static final Trace LOGGER = TraceManager.getTrace(PrismProperty.class);

    public PrismProperty(QName name) {
        super(name);
    }

    protected PrismProperty(QName name, PrismPropertyDefinition definition, PrismContext prismContext) {
        super(name, definition, prismContext);
    }

	/**
     * Returns applicable property definition.
     * <p/>
     * May return null if no definition is applicable or the definition is not
     * know.
     *
     * @return applicable property definition
     */
    public PrismPropertyDefinition getDefinition() {
        return (PrismPropertyDefinition) definition;
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
     * Returns property values.
     * <p/>
     * The values are returned as set. The order of values is not significant.
     *
     * @return property values
     */
    public List<PrismPropertyValue<V>> getValues() {
        return (List<PrismPropertyValue<V>>) super.getValues();
    }
    
    public PrismPropertyValue<V> getValue() {
    	// We are not sure about multiplicity if there is no definition or the definition is dynamic
    	if (getDefinition() != null && !getDefinition().isDynamic()) {
    		if (getDefinition().isMultiValue()) {
    			throw new IllegalStateException("Attempt to get single value from property " + getName()
                        + " with multiple values");
    		}
    	}
        List<PrismPropertyValue<V>> values = getValues();
        if (values == null || values.isEmpty()) {
        	return null;
        }
        if (values.size() == 1) {
        	return values.get(0);
        }
        throw new IllegalStateException("Attempt to get a single value from a multi-valued property "+getName());
    }

    /**
     * Type override, also for compatibility.
     */
    public <T> List<PrismPropertyValue<T>> getValues(Class<T> T) {
        return (List) getValues();
    }

    public Collection<V> getRealValues() {
		Collection<V> realValues = new ArrayList<V>(getValues().size());
		for (PrismPropertyValue<V> pValue: getValues()) {
			realValues.add(pValue.getValue());
		}
		return realValues;
	}
    
    /**
     * Type override, also for compatibility.
     */
	public <T> Collection<T> getRealValues(Class<T> type) {
		Collection<T> realValues = new ArrayList<T>(getValues().size());
		for (PrismPropertyValue<V> pValue: getValues()) {
			realValues.add((T) pValue.getValue());
		}
		return realValues;
	}
	
	public V getRealValue() {
		if (getValue() == null) {
            return null;
        }
		return getValue().getValue();
	}
	
	/**
     * Type override, also for compatibility.
     */
	public <T> T getRealValue(Class<T> type) {
        if (getValue() == null) {
            return null;
        }
		V value = getValue().getValue();
		if (value == null) {
			return null;
		}
		if (type.isAssignableFrom(value.getClass())) {
			return (T)value;
		} else {
			throw new ClassCastException("Cannot cast value of property "+getName()+" which is of type "+value.getClass()+" to "+type);
		}
	}

	/**
     * Type override, also for compatibility.
     */
	public <T> T[] getRealValuesArray(Class<T> type) {
		Object valuesArrary = Array.newInstance(type, getValues().size());
		for (int j = 0; j < getValues().size(); ++j) {
			Object avalue = getValues().get(j).getValue();
			Array.set(valuesArrary, j, avalue);
		}
		return (T[]) valuesArrary;
	}

    /**
     * Type override, also for compatibility.
     */
    public <T> PrismPropertyValue<T> getValue(Class<T> T) {
    	if (getDefinition() != null) {
    		if (getDefinition().isMultiValue()) {
    			throw new IllegalStateException("Attempt to get single value from property " + name
                        + " with multiple values");
    		}
    	}
        if (getValues().size() > 1) {
            throw new IllegalStateException("Attempt to get single value from property " + name
                    + " with multiple values");
        }
        if (getValues().isEmpty()) {
            return null;
        }
        PrismPropertyValue<V> o = getValues().iterator().next();
        return (PrismPropertyValue<T>) o;
    }

    /**
     * Means as a short-hand for setting just a value for single-valued
     * attributes.
     * Will remove all existing values.
     * TODO
     */
    public void setValue(PrismPropertyValue<V> value) {
    	getValues().clear();
        addValue(value);
    }

	public void setRealValue(Object realValue) {
    	setValue(new PrismPropertyValue(realValue));
    }

    public void addValues(Collection<PrismPropertyValue<V>> pValuesToAdd) {
    	for (PrismPropertyValue<V> pValue: pValuesToAdd) {
    		addValue(pValue);
    	}
    }

    public void addValue(PrismPropertyValue<V> pValueToAdd) {
    	Iterator<PrismPropertyValue<V>> iterator = getValues().iterator();
    	while (iterator.hasNext()) {
    		PrismPropertyValue<V> pValue = iterator.next();
    		if (pValue.equalsRealValue(pValueToAdd)) {
    			LOGGER.warn("Adding value to property "+getName()+" that already exists (overwriting), value: "+pValueToAdd);
    			iterator.remove();
    		}
    	}
    	prepareValue(pValueToAdd);
    	pValueToAdd.setParent(this);
    	getValues().add(pValueToAdd);
    }
    
    /**
	 * Prepare the value to be stored in prism by recomputing it or otherwise "initialize" it.
	 */
	private void prepareValue(PrismPropertyValue<V> value) {
		if (value.isRaw()) {
			return;
		}
		V realValue = value.getValue();
		if (realValue == null) {
			return;
		}
		if (realValue instanceof PolyString && getPrismContext() != null) {
			PolyStringNormalizer polyStringNormalizer = getPrismContext().getDefaultPolyStringNormalizer();
			if (polyStringNormalizer != null) {
				((PolyString)realValue).recompute(polyStringNormalizer);
			}
		}
	}

    public boolean deleteValues(Collection<PrismPropertyValue<V>> pValuesToDelete) {
        boolean changed = false;
    	for (PrismPropertyValue<V> pValue: pValuesToDelete) {
            if (!changed) {
    		    changed = deleteValue(pValue);
            } else {
                deleteValue(pValue);
            }
    	}
        return changed;
    }

    public boolean deleteValue(PrismPropertyValue<V> pValueToDelete) {
    	Iterator<PrismPropertyValue<V>> iterator = getValues().iterator();
    	boolean found = false;
    	while (iterator.hasNext()) {
    		PrismPropertyValue<V> pValue = iterator.next();
    		if (pValue.equalsRealValue(pValueToDelete)) {
    			iterator.remove();
    			pValue.setParent(null);
    			found = true;
    		}
    	}
    	if (!found) {
    		LOGGER.warn("Deleting value of property "+getName()+" that does not exist (skipping), value: "+pValueToDelete);
    	}

        return found;
    }

    public void replaceValues(Collection<PrismPropertyValue<V>> valuesToReplace) {
    	getValues().clear();
        addValues(valuesToReplace);
    }

    public boolean hasValue(PrismPropertyValue<V> value) {
        return super.hasValue(value);
    }

    public boolean hasRealValue(PrismPropertyValue<V> value) {
        for (PrismPropertyValue<V> propVal : getValues()) {
            if (propVal.equalsRealValue(value)) {
                return true;
            }
        }

        return false;
    }
    
    @Override
	public PrismPropertyValue<V> getPreviousValue(PrismValue value) {
		return (PrismPropertyValue<V>) super.getPreviousValue(value);
	}

	@Override
	public PrismPropertyValue<V> getNextValue(PrismValue value) {
		return (PrismPropertyValue<V>) super.getNextValue(value);
	}

	public Class<V> getValueClass() {
    	if (getDefinition() != null) {
    		return getDefinition().getTypeClass();
    	}
    	if (!getValues().isEmpty()) {
    		PrismPropertyValue<V> firstPVal = getValues().get(0);
    		if (firstPVal != null) {
    			V firstVal = firstPVal.getValue();
    			if (firstVal != null) {
    				return (Class<V>) firstVal.getClass();
    			}
    		}
    	}
    	// TODO: How to determine value class?????
    	return PrismConstants.DEFAULT_VALUE_CLASS;
    }
    	
    @Override
	public PropertyDelta<V> createDelta(PropertyPath path) {
    	if (path == null || path.isEmpty()) {
    		throw new IllegalArgumentException("Attempt to create delta with null or empty path");
    	}
		return new PropertyDelta<V>(path, getDefinition());
	}
    
    public PropertyDelta<V> diff(PrismProperty<V> other, PropertyPath pathPrefix) {
    	return diff(other, pathPrefix, true);
    }
    
    public PropertyDelta<V> diff(PrismProperty<V> other, PropertyPath pathPrefix, boolean ignoreMetadata) {
    	List<? extends ItemDelta> deltas = new ArrayList<ItemDelta>();
    	diffInternal(other, pathPrefix, deltas, ignoreMetadata);
    	if (deltas.isEmpty()) {
    		return null;
    	}
    	if (deltas.size() > 1) {
    		throw new IllegalStateException("Unexpected number of deltas from property diff: "+deltas);
    	}
    	return (PropertyDelta<V>)deltas.get(0);
    }

	@Override
	protected void checkDefinition(ItemDefinition def) {
		if (!(def instanceof PrismPropertyDefinition)) {
			throw new IllegalArgumentException("Definition "+def+" cannot be applied to property "+this);
		}
	}

	@Override
    public PrismProperty<V> clone() {
        PrismProperty<V> clone = new PrismProperty<V>(getName(), getDefinition(), prismContext);
        copyValues(clone);
        return clone;
    }

    protected void copyValues(PrismProperty<V> clone) {
        super.copyValues(clone);
        for (PrismPropertyValue<V> value : getValues()) {
            clone.addValue(value.clone());
        }
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        return true;
    }
	
	@Override
	protected ItemDelta fixupDelta(ItemDelta delta, Item otherItem, PropertyPath pathPrefix,
			boolean ignoreMetadata) {
		PrismPropertyDefinition def = getDefinition();
		if (def != null && def.isSingleValue() && !delta.isEmpty()) {
        	// Drop the current delta (it was used only to detect that something has changed
        	// Generate replace delta instead of add/delete delta
			PrismProperty<V> other = (PrismProperty<V>)otherItem;
			PropertyDelta<V> propertyDelta = (PropertyDelta<V>)delta;
			delta.clear();
    		Collection<PrismPropertyValue<V>> replaceValues = new ArrayList<PrismPropertyValue<V>>(other.getValues().size());
            for (PrismPropertyValue<V> value : other.getValues()) {
            	replaceValues.add(value.clone());
            }
            propertyDelta.setValuesToReplace(replaceValues);
			return propertyDelta;
        } else {
        	return super.fixupDelta(delta, otherItem, pathPrefix, ignoreMetadata);
        }
	}

	@Override
    public String toString() {
        return getDebugDumpClassName() + "(" + DebugUtil.prettyPrint(getName()) + "):" + getValues();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append(INDENT_STRING);
        }
        sb.append(getDebugDumpClassName()).append(": ").append(DebugUtil.prettyPrint(getName())).append(" = ");
        if (getValues() == null) {
            sb.append("null");
        } else {
            sb.append("[ ");
            Iterator<PrismPropertyValue<V>> iterator = getValues().iterator();
            while(iterator.hasNext()) {
            	PrismPropertyValue<V> value = iterator.next();
                sb.append(DebugUtil.prettyPrint(value));
                if (iterator.hasNext()) {
                	sb.append(", ");
                }
            }
            sb.append(" ]");
        }
        PrismPropertyDefinition def = getDefinition();
        if (def != null) {
            sb.append(" def(");
            sb.append(DebugUtil.prettyPrint(def.getTypeName()));
            if (def.isDynamic()) {
            	sb.append(",dyn");
            }
            sb.append(")");
        }
        return sb.toString();
    }
    
    public String getHumanReadableDump() {
    	StringBuilder sb = new StringBuilder();
    	sb.append(DebugUtil.prettyPrint(getName())).append(" = ");
    	if (getValues() == null) {
            sb.append("null");
        } else {
            sb.append("[ ");
            Iterator<PrismPropertyValue<V>> iterator = getValues().iterator();
            while(iterator.hasNext()) {
            	PrismPropertyValue<V> value = iterator.next();
                sb.append(value.getHumanReadableDump());
                if (iterator.hasNext()) {
                	sb.append(", ");
                }
            }
            sb.append(" ]");
        }
    	return sb.toString();
    }

    /**
     * Return a human readable name of this class suitable for logs.
     */
    @Override
    protected String getDebugDumpClassName() {
        return "PP";
    }

}
