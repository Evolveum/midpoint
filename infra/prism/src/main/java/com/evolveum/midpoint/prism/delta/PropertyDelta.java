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

package com.evolveum.midpoint.prism.delta;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

import javax.xml.namespace.QName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Relative difference (delta) of a property values.
 * <p/>
 * This class describes what values are to be added, removed or replaced in the property.
 * The delta can be either add+delete or replace, but not both. It either describes what
 * values to add and delete from the property (add+delete) or what is the new set of values
 * (replace). Add+delete deltas can be merged without a loss. There are ideal for multi-value
 * properties. If replace deltas are merged, only the last value will be present. These are
 * better suited for single-value properties.
 *
 * @author Radovan Semancik
 * @see ObjectDelta
 */
public class PropertyDelta<T extends Object> extends ItemDelta<PrismPropertyValue<T>> {

    public PropertyDelta(PrismPropertyDefinition propertyDefinition) {
        super(propertyDefinition);
    }
    
    public PropertyDelta(QName name, PrismPropertyDefinition propertyDefinition) {
    	super(name, propertyDefinition);
    }

    public PropertyDelta(PropertyPath parentPath, QName name, PrismPropertyDefinition propertyDefinition) {
    	super(parentPath, name, propertyDefinition);
    }

    public PropertyDelta(PropertyPath propertyPath, PrismPropertyDefinition propertyDefinition) {
    	super(propertyPath, propertyDefinition);
    }
    
    PrismPropertyDefinition getPropertyDefinition() {
		return (PrismPropertyDefinition) super.getDefinition();
	}

	void setPropertyDefinition(PrismPropertyDefinition propertyDefinition) {
		super.setDefinition(propertyDefinition);
	}
	
    @Override
	public void applyDefinition(ItemDefinition definition) throws SchemaException {
    	if (!(definition instanceof PrismPropertyDefinition)) {
    		throw new IllegalArgumentException("Cannot apply "+definition+" to a property delta "+this);
    	}
		super.applyDefinition(definition);
	}

	@Override
	public Class<PrismProperty> getItemClass() {
		return PrismProperty.class;
	}

	/**
     * Returns all values regardless of whether they are added or removed or replaced.
     * Useful for iterating over all the changed values.
     */
    public <T> Collection<PrismPropertyValue<T>> getValues(Class<T> type) {
        checkConsistence();
        if (valuesToReplace != null) {
            return (Collection) valuesToReplace;
        }
        return (Collection) MiscUtil.union(valuesToAdd, valuesToDelete);
    }
    
    public <P extends PrismProperty> P instantiateEmptyProperty() {
    	PrismPropertyDefinition propertyDefinition = getPropertyDefinition();
    	if (propertyDefinition == null) {
    		throw new IllegalArgumentException("Cannot instantiate property "+getName()+" from delta "+this+": no definition");
    	}
    	return (P) propertyDefinition.instantiate(getName());
    }
    
	public static <O extends Objectable> PropertyDelta createReplaceDelta(PrismObjectDefinition<O> objectDefinition,
			QName propertyName, Object... realValues) {
		PrismPropertyDefinition propertyDefinition = objectDefinition.findPropertyDefinition(propertyName);
		if (propertyDefinition == null) {
			throw new IllegalArgumentException("No definition for "+propertyName+" in "+objectDefinition);
		}
		PropertyDelta<Object> delta = new PropertyDelta<Object>(propertyName, propertyDefinition);
		Collection<PrismPropertyValue<Object>> valuesToReplace = delta.getValuesToReplace();
		if (valuesToReplace == null) {
			valuesToReplace = new HashSet<PrismPropertyValue<Object>>();
			delta.setValuesToReplace(valuesToReplace);
		}
		for (Object realVal: realValues) {
			valuesToReplace.add(new PrismPropertyValue<Object>(realVal));
		}
		return delta;
	}

	/**
	 * Create delta that deletes all values of the specified property.
	 */
	public static <O extends Objectable> PropertyDelta createReplaceEmptyDelta(PrismObjectDefinition<O> objectDefinition,
			QName propertyName) {
		PrismPropertyDefinition propertyDefinition = objectDefinition.findPropertyDefinition(propertyName);
		if (propertyDefinition == null) {
			throw new IllegalArgumentException("No definition for "+propertyName+" in "+objectDefinition);
		}
		PropertyDelta delta = new PropertyDelta(propertyName, propertyDefinition);
		return delta;
	}
	
	public static <O extends Objectable> PropertyDelta<?> createReplaceDeltaOrEmptyDelta(PrismObjectDefinition<O> objectDefinition,
			QName propertyName, Object realValue) {
		
		if (realValue != null)
			return createReplaceDelta(objectDefinition, propertyName, realValue);
		else
			return createReplaceEmptyDelta(objectDefinition, propertyName);
	}


    public boolean isRealValueToAdd(PrismPropertyValue<?> value) {
        if (valuesToAdd == null) {
            return false;
        }

        for (PrismPropertyValue valueToAdd : valuesToAdd) {
            if (valueToAdd.equalsRealValue(value)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isRealValueToDelete(PrismPropertyValue<?> value) {
        if (valuesToDelete == null) {
            return false;
        }

        for (PrismPropertyValue valueToAdd : valuesToDelete) {
            if (valueToAdd.equalsRealValue(value)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the "new" state of the property - the state that would be after the delta
     * is applied.
     * Assumes "replace" delta.
     */
    public PrismProperty<T> getPropertyNew() {
        return (PrismProperty<T>) super.getItemNew();
    }
    
    public static PropertyDelta createDelta(PropertyPath propertyPath, PrismObjectDefinition<?> objectDefinition) {
    	PrismPropertyDefinition propDef = objectDefinition.findPropertyDefinition(propertyPath);
    	return new PropertyDelta(propertyPath, propDef);
    }
    
    public static <T extends Objectable> PropertyDelta createDelta(PropertyPath propertyPath, Class<T> compileTimeClass, PrismContext prismContext) {
    	PrismObjectDefinition<T> objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(compileTimeClass);
    	PrismPropertyDefinition propDef = objectDefinition.findPropertyDefinition(propertyPath);
    	return new PropertyDelta(propertyPath, propDef);
    }
    
    /**
     * Convenience method for quick creation of object deltas that replace a single object property. This is used quite often
     * to justify a separate method. 
     */
    public static PropertyDelta createModificationReplaceProperty(QName propertyName, PrismObjectDefinition<?> objectDefinition, 
    		Object... propertyValues) {
    	PrismPropertyDefinition propDef = objectDefinition.findPropertyDefinition(propertyName);
    	PropertyDelta<Object> propertyDelta = new PropertyDelta<Object>(propertyName, propDef);
    	Collection<PrismPropertyValue<Object>> pValues = new ArrayList<PrismPropertyValue<Object>>(propertyValues.length);
    	for (Object val: propertyValues) {
    		pValues.add(new PrismPropertyValue<Object>(val));
    	}
		propertyDelta.setValuesToReplace(pValues);
    	return propertyDelta;
    }

    /**
     * Convenience method for quick creation of object deltas that replace a single object property. This is used quite often
     * to justify a separate method. 
     */
    public static Collection<? extends ItemDelta> createModificationReplacePropertyCollection(QName propertyName, 
    		PrismObjectDefinition<?> objectDefinition, Object... propertyValues) {
    	Collection<? extends ItemDelta> modifications = new ArrayList<ItemDelta>(1);
    	PropertyDelta delta = createModificationReplaceProperty(propertyName, objectDefinition, propertyValues);
    	((Collection)modifications).add(delta);
    	return modifications;
    }
    
    public static PropertyDelta findPropertyDelta(Collection<? extends ItemDelta> modifications, PropertyPath propertyPath) {
    	for (ItemDelta delta: modifications) {
    		if (delta instanceof PropertyDelta && delta.getPath().equals(propertyPath)) {
    			return (PropertyDelta) delta;
    		}
    	}
    	return null;
    }
    
    public static PropertyDelta findPropertyDelta(Collection<? extends ItemDelta> modifications, QName propertyName) {
    	for (ItemDelta delta: modifications) {
    		if (delta instanceof PropertyDelta && delta.getParentPath().isEmpty() &&
    			delta.getName().equals(propertyName)) {
    			return (PropertyDelta) delta;
    		}
    	}
    	return null;
    }


}
