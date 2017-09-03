/*
 * Copyright (c) 2010-2015 Evolveum
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
import java.util.Comparator;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Relative difference (delta) of a property values.
 * <p>
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
public class PropertyDelta<T extends Object> extends ItemDelta<PrismPropertyValue<T>, PrismPropertyDefinition<T>> {

    public PropertyDelta(PrismPropertyDefinition<T> propertyDefinition, PrismContext prismContext) {
        super(propertyDefinition, prismContext);
    }
    
    public PropertyDelta(QName name, PrismPropertyDefinition<T> propertyDefinition, PrismContext prismContext) {
    	super(name, propertyDefinition, prismContext);
    }

    public PropertyDelta(ItemPath parentPath, QName name, PrismPropertyDefinition<T> propertyDefinition, PrismContext prismContext) {
    	super(parentPath, name, propertyDefinition, prismContext);
    }

    public PropertyDelta(ItemPath propertyPath, PrismPropertyDefinition<T> propertyDefinition, PrismContext prismContext) {
    	super(propertyPath, propertyDefinition, prismContext);
    }
    
    public PrismPropertyDefinition<T> getPropertyDefinition() {
		return super.getDefinition();
	}

	void setPropertyDefinition(PrismPropertyDefinition<T> propertyDefinition) {
		super.setDefinition(propertyDefinition);
	}
	
    @Override
	public void setDefinition(PrismPropertyDefinition<T> definition) {
    	if (!(definition instanceof PrismPropertyDefinition)) {
			throw new IllegalArgumentException("Cannot apply "+definition+" to property delta");
		}
		super.setDefinition(definition);
	}

	@Override
	public void applyDefinition(PrismPropertyDefinition<T> definition) throws SchemaException {
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
        if (valuesToReplace != null) {
            return (Collection) valuesToReplace;
        }
        return (Collection) MiscUtil.union(valuesToAdd, valuesToDelete);
    }
    
	public T getAnyRealValue() {
		PrismPropertyValue<T> anyValue = getAnyValue();
		return anyValue.getValue();
	}

    
    public <P extends PrismProperty> P instantiateEmptyProperty() {
    	PrismPropertyDefinition propertyDefinition = getPropertyDefinition();
    	if (propertyDefinition == null) {
    		throw new IllegalArgumentException("Cannot instantiate property "+ getElementName()+" from delta "+this+": no definition");
    	}
    	return (P) propertyDefinition.instantiate(getElementName());
    }
    
    @Override
	protected boolean isApplicableToType(Item item) {
		return item instanceof PrismProperty;
	}
    
	@Override
	public PropertyDelta<T> clone() {
		PropertyDelta<T> clone = new PropertyDelta<T>(getElementName(), getPropertyDefinition(), getPrismContext());
		copyValues(clone);
		return clone;
	}

	protected void copyValues(PropertyDelta<T> clone) {
		super.copyValues(clone);
	}

	// TODO: the same as createModificationReplaceProperty?
    // btw, why was here 'PrismObjectDefinition'?
    public static <O extends Objectable, T> PropertyDelta<T> createReplaceDelta(PrismContainerDefinition<O> containerDefinition,
			QName propertyName, T... realValues) {
		PrismPropertyDefinition<T> propertyDefinition = containerDefinition.findPropertyDefinition(propertyName);
		if (propertyDefinition == null) {
			throw new IllegalArgumentException("No definition for "+propertyName+" in "+containerDefinition);
		}
		PropertyDelta<T> delta = new PropertyDelta<T>(propertyName, propertyDefinition, containerDefinition.getPrismContext());            // hoping the prismContext is there
		Collection<PrismPropertyValue<T>> valuesToReplace = delta.getValuesToReplace();
		if (valuesToReplace == null)
			valuesToReplace = new ArrayList<PrismPropertyValue<T>>(realValues.length);
		for (T realVal: realValues) {
			valuesToReplace.add(new PrismPropertyValue<T>(realVal));
		}
		delta.setValuesToReplace(valuesToReplace);
		return delta;
	}
    
    public static <O extends Objectable, T> PropertyDelta<T> createReplaceDelta(PrismContainerDefinition<O> containerDefinition,
			QName propertyName, PrismPropertyValue<T>... pValues) {
		PrismPropertyDefinition<T> propertyDefinition = containerDefinition.findPropertyDefinition(propertyName);
		if (propertyDefinition == null) {
			throw new IllegalArgumentException("No definition for "+propertyName+" in "+containerDefinition);
		}
		PropertyDelta<T> delta = new PropertyDelta<T>(propertyName, propertyDefinition, containerDefinition.getPrismContext());       // hoping the prismContext is there
		Collection<PrismPropertyValue<T>> valuesToReplace = new ArrayList<PrismPropertyValue<T>>(pValues.length);
		for (PrismPropertyValue<T> pVal: pValues) {
			valuesToReplace.add(pVal);
		}
		delta.setValuesToReplace(valuesToReplace);
		return delta;
	}

    public static <O extends Objectable> PropertyDelta createAddDelta(PrismContainerDefinition<O> containerDefinition,
                                                                          QName propertyName, Object... realValues) {
        PrismPropertyDefinition propertyDefinition = containerDefinition.findPropertyDefinition(propertyName);
        if (propertyDefinition == null) {
            throw new IllegalArgumentException("No definition for "+propertyName+" in "+containerDefinition);
        }
        PropertyDelta delta = new PropertyDelta(propertyName, propertyDefinition, containerDefinition.getPrismContext());       // hoping the prismContext is there
        for (Object realVal: realValues) {
            delta.addValueToAdd(new PrismPropertyValue(realVal));
        }
        return delta;
    }

    public static <O extends Objectable> PropertyDelta createDeleteDelta(PrismContainerDefinition<O> containerDefinition,
                                                                      QName propertyName, Object... realValues) {
        PrismPropertyDefinition propertyDefinition = containerDefinition.findPropertyDefinition(propertyName);
        if (propertyDefinition == null) {
            throw new IllegalArgumentException("No definition for "+propertyName+" in "+containerDefinition);
        }
        PropertyDelta delta = new PropertyDelta(propertyName, propertyDefinition, containerDefinition.getPrismContext());       // hoping the prismContext is there
        for (Object realVal: realValues) {
            delta.addValueToDelete(new PrismPropertyValue(realVal));
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
		PropertyDelta delta = new PropertyDelta(propertyName, propertyDefinition, objectDefinition.getPrismContext());             // hoping the prismContext is there
		delta.setValuesToReplace(new ArrayList<PrismPropertyValue>());
		return delta;
	}

	public static <O extends Objectable> PropertyDelta createReplaceEmptyDelta(PrismObjectDefinition<O> objectDefinition,
																			   ItemPath propertyPath) {
		PrismPropertyDefinition propertyDefinition = objectDefinition.findPropertyDefinition(propertyPath);
		if (propertyDefinition == null) {
			throw new IllegalArgumentException("No definition for "+propertyPath+" in "+objectDefinition);
		}
		PropertyDelta delta = new PropertyDelta(propertyPath, propertyDefinition, objectDefinition.getPrismContext());             // hoping the prismContext is there
		delta.setValuesToReplace(new ArrayList<PrismPropertyValue>());
		return delta;
	}
	
	public static <O extends Objectable, T> PropertyDelta<T> createReplaceDeltaOrEmptyDelta(PrismObjectDefinition<O> objectDefinition,
			QName propertyName, T realValue) {
		
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
     */
    public PrismProperty<T> getPropertyNewMatchingPath() throws SchemaException {
        return (PrismProperty<T>) super.getItemNewMatchingPath(null);
    }
    
    /**
     * Returns the "new" state of the property - the state that would be after the delta
     * is applied.
     */
    public PrismProperty<T> getPropertyNewMatchingPath(PrismProperty<T> propertyOld) throws SchemaException {
        return (PrismProperty<T>) super.getItemNewMatchingPath(propertyOld);
    }
    
    @Override
	public PropertyDelta<T> narrow(PrismObject<? extends Objectable> object) {
		return (PropertyDelta<T>) super.narrow(object);
	}
    
    public PropertyDelta<T> narrow(PrismObject<? extends Objectable> object, final MatchingRule<T> matchingRule) {
		Comparator<PrismPropertyValue<T>> comparator = (o1, o2) -> {
			if (o1.equalsComplex(o2, true, false, matchingRule)) {
				return 0;
			} else {
				return 1;
			}
		};
		return (PropertyDelta<T>) super.narrow(object, comparator);
	}

	public boolean isRedundant(PrismObject<? extends Objectable> object, final MatchingRule<T> matchingRule) {
		Comparator<PrismPropertyValue<T>> comparator = (o1, o2) -> {
			if (o1.equalsComplex(o2, true, false, matchingRule)) {
				return 0;
			} else {
				return 1;
			}
		};
		return super.isRedundant(object, comparator);
	}
    
    public static <O extends Objectable,T> PropertyDelta<T> createDelta(QName propertyName, PrismObjectDefinition<O> objectDefinition) {
    	return createDelta(new ItemPath(propertyName), objectDefinition);
    }

	public static <O extends Objectable,T> PropertyDelta<T> createDelta(ItemPath propertyPath, PrismObjectDefinition<O> objectDefinition) {
    	PrismPropertyDefinition propDef = objectDefinition.findPropertyDefinition(propertyPath);
    	return new PropertyDelta<T>(propertyPath, propDef, objectDefinition.getPrismContext());        // hoping the prismContext is there
    }
	
	public static <O extends Objectable,T> PropertyDelta<T> createDelta(QName propertyName, Class<O> compileTimeClass, PrismContext prismContext) {
		return createDelta(new ItemPath(propertyName), compileTimeClass, prismContext);
	}
    
    public static <O extends Objectable,T> PropertyDelta<T> createDelta(ItemPath propertyPath, Class<O> compileTimeClass, PrismContext prismContext) {
    	PrismObjectDefinition<O> objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(compileTimeClass);
    	PrismPropertyDefinition propDef = objectDefinition.findPropertyDefinition(propertyPath);
    	return new PropertyDelta<T>(propertyPath, propDef, prismContext);
    }
    
    public static <T> PropertyDelta<T> createModificationReplaceProperty(QName propertyName, PrismObjectDefinition<?> objectDefinition, 
    		T... propertyValues) {
    	return createModificationReplaceProperty(new ItemPath(propertyName), objectDefinition, propertyValues);
    }
    
    /**
     * Convenience method for quick creation of object deltas that replace a single object property. This is used quite often
     * to justify a separate method. 
     */
    public static <T> PropertyDelta<T> createModificationReplaceProperty(ItemPath propertyPath, PrismObjectDefinition<?> objectDefinition, 
    		T... propertyValues) {
    	PrismPropertyDefinition propDef = objectDefinition.findPropertyDefinition(propertyPath);
    	PropertyDelta<T> propertyDelta = new PropertyDelta<T>(propertyPath, propDef, objectDefinition.getPrismContext());              // hoping the prismContext is there
    	Collection<PrismPropertyValue<T>> pValues = new ArrayList<PrismPropertyValue<T>>(propertyValues.length);
    	for (T val: propertyValues) {
    		pValues.add(new PrismPropertyValue<T>(val));
    	}
		propertyDelta.setValuesToReplace(pValues);
    	return propertyDelta;
    }

	public static <T> PropertyDelta<T> createModificationReplaceProperty(ItemPath propertyPath, PrismObjectDefinition<?> objectDefinition,
																		 Collection<T> propertyValues) {
		PrismPropertyDefinition propDef = objectDefinition.findPropertyDefinition(propertyPath);
		PropertyDelta<T> propertyDelta = new PropertyDelta<T>(propertyPath, propDef, objectDefinition.getPrismContext());              // hoping the prismContext is there
		Collection<PrismPropertyValue<T>> pValues = new ArrayList<PrismPropertyValue<T>>(propertyValues.size());
		for (T val: propertyValues) {
			pValues.add(new PrismPropertyValue<T>(val));
		}
		propertyDelta.setValuesToReplace(pValues);
		return propertyDelta;
	}
    
    public static <T> PropertyDelta<T> createModificationReplaceProperty(ItemPath propertyPath, PrismPropertyDefinition propertyDefinition, 
    		T... propertyValues) {
    	PropertyDelta<T> propertyDelta = new PropertyDelta<T>(propertyPath, propertyDefinition, propertyDefinition.getPrismContext());             // hoping the prismContext is there
    	Collection<PrismPropertyValue<T>> pValues = new ArrayList<PrismPropertyValue<T>>(propertyValues.length);
    	for (T val: propertyValues) {
    		pValues.add(new PrismPropertyValue<T>(val));
    	}
		propertyDelta.setValuesToReplace(pValues);
    	return propertyDelta;
    }
    
    public static <T> PropertyDelta<T> createModificationAddProperty(ItemPath propertyPath, PrismPropertyDefinition propertyDefinition, 
    		T... propertyValues) {
    	PropertyDelta<T> propertyDelta = new PropertyDelta<T>(propertyPath, propertyDefinition, propertyDefinition.getPrismContext());         // hoping the prismContext is there
    	Collection<PrismPropertyValue<T>> pValues = new ArrayList<PrismPropertyValue<T>>(propertyValues.length);
    	for (T val: propertyValues) {
    		pValues.add(new PrismPropertyValue<T>(val));
    	}
		propertyDelta.addValuesToAdd(pValues);
    	return propertyDelta;
    }
    
    public static <T> PropertyDelta<T> createModificationDeleteProperty(ItemPath propertyPath, PrismPropertyDefinition propertyDefinition, 
    		T... propertyValues) {
    	PropertyDelta<T> propertyDelta = new PropertyDelta<T>(propertyPath, propertyDefinition, propertyDefinition.getPrismContext());             // hoping the prismContext is there
    	Collection<PrismPropertyValue<T>> pValues = new ArrayList<PrismPropertyValue<T>>(propertyValues.length);
    	for (T val: propertyValues) {
    		pValues.add(new PrismPropertyValue<T>(val));
    	}
		propertyDelta.addValuesToDelete(pValues);
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

	public static <T> PropertyDelta<T> findPropertyDelta(Collection<? extends ItemDelta> modifications, ItemPath propertyPath) {
    	for (ItemDelta delta: modifications) {
    		if (delta instanceof PropertyDelta && delta.getPath().equivalent(propertyPath)) {
    			return (PropertyDelta) delta;
    		}
    	}
    	return null;
    }
    
    public static <T> PropertyDelta<T> findPropertyDelta(Collection<? extends ItemDelta> modifications, QName propertyName) {
    	for (ItemDelta delta: modifications) {
    		if (delta instanceof PropertyDelta && delta.getParentPath().isEmpty() &&
    				QNameUtil.match(delta.getElementName(), propertyName)) {
    			return (PropertyDelta) delta;
    		}
    	}
    	return null;
    }

}
