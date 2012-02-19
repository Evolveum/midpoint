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

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.MiscUtil;

import javax.xml.namespace.QName;
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
public class PropertyDelta implements Dumpable, DebugDumpable {

    /**
     * Name of the property
     */
    QName name;
    /**
     * Parent path of the property (path to the property container)
     */
    PropertyPath parentPath;
    PrismPropertyDefinition propertyDefinition;

    Collection<PrismPropertyValue<Object>> valuesToReplace = null;
    Collection<PrismPropertyValue<Object>> valuesToAdd = null;
    Collection<PrismPropertyValue<Object>> valuesToDelete = null;

    public PropertyDelta(PrismPropertyDefinition propertyDefinition) {
        this.name = propertyDefinition.getName();
        this.parentPath = new PropertyPath();
        this.propertyDefinition = propertyDefinition;
    }
    
    public PropertyDelta(QName name, PrismPropertyDefinition propertyDefinition) {
        this.name = name;
        this.parentPath = new PropertyPath();
        this.propertyDefinition = propertyDefinition;
    }

    public PropertyDelta(PropertyPath parentPath, QName name, PrismPropertyDefinition propertyDefinition) {
        this.name = name;
        this.parentPath = parentPath;
        this.propertyDefinition = propertyDefinition;
    }

    public PropertyDelta(PropertyPath propertyPath, PrismPropertyDefinition propertyDefinition) {
        this.name = propertyPath.last().getName();
        this.parentPath = propertyPath.allExceptLast();
        this.propertyDefinition = propertyDefinition;
    }

    public QName getName() {
        return name;
    }

    public void setName(QName name) {
        this.name = name;
    }

    public PropertyPath getParentPath() {
        return parentPath;
    }

    public void setParentPath(PropertyPath parentPath) {
        this.parentPath = parentPath;
    }

    public PropertyPath getPath() {
        return getParentPath().subPath(name);
    }
    
    PrismPropertyDefinition getPropertyDefinition() {
		return propertyDefinition;
	}

	void setPropertyDefinition(PrismPropertyDefinition propertyDefinition) {
		this.propertyDefinition = propertyDefinition;
	}

	public boolean isReplace() {
        return (valuesToReplace != null);
    }
    
    public boolean isAdd() {
        return (valuesToAdd != null && !valuesToAdd.isEmpty());
    }
    
    public boolean isDelete() {
        return (valuesToDelete != null && !valuesToDelete.isEmpty());
    }

    public Collection<PrismPropertyValue<Object>> getValuesToAdd() {
        return valuesToAdd;
    }

    public Collection<PrismPropertyValue<Object>> getValuesToDelete() {
        return valuesToDelete;
    }

    public void clear() {
        if (valuesToAdd != null) {
            valuesToAdd.clear();
        }
        if (valuesToDelete != null) {
            valuesToDelete.clear();
        }
        if (valuesToReplace != null) {
            valuesToReplace.clear();
        }
    }
    
    public Collection<PrismPropertyValue<Object>> getValuesToReplace() {
    	return valuesToReplace;
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

    public void checkConsistence() {
        if (valuesToReplace != null && (valuesToAdd != null || valuesToDelete != null)) {
            throw new IllegalStateException("The delta cannot be both 'replace' and 'add/delete' at the same time");
        }
    }
    
    public <T extends PrismProperty> T instantiateEmptyProperty() {
    	return (T) getPropertyDefinition().instantiate(getName());
    }
    
	public static <T extends Objectable> PropertyDelta createReplaceDelta(PrismObjectDefinition<T> objectDefinition,
			QName propertyName, Object... realValues) {
		PrismPropertyDefinition propertyDefinition = objectDefinition.findPropertyDefinition(propertyName);
		if (propertyDefinition == null) {
			throw new IllegalArgumentException("No definition for "+propertyName+" in "+objectDefinition);
		}
		PropertyDelta delta = new PropertyDelta(propertyName, propertyDefinition);
		Collection<PrismPropertyValue<Object>> valuesToReplace = delta.getValuesToReplace();
		for (Object realVal: realValues) {
			valuesToReplace.add(new PrismPropertyValue<Object>(realVal));
		}
		return delta;
	}

	/**
	 * Create delta that deletes all values of the specified property.
	 */
	public static <T extends Objectable> PropertyDelta createReplaceEmptyDelta(PrismObjectDefinition<T> objectDefinition,
			QName propertyName) {
		PrismPropertyDefinition propertyDefinition = objectDefinition.findPropertyDefinition(propertyName);
		if (propertyDefinition == null) {
			throw new IllegalArgumentException("No definition for "+propertyName+" in "+objectDefinition);
		}
		PropertyDelta delta = new PropertyDelta(propertyName, propertyDefinition);
		return delta;
	}

    /**
     * Merge specified delta to this delta. This delta is assumed to be
     * chronologically earlier.
     */
    public void merge(PropertyDelta deltaToMerge) {
        checkConsistence();
        deltaToMerge.checkConsistence();
        if (deltaToMerge.isEmpty()) {
            return;
        }
        if (deltaToMerge.valuesToReplace != null) {
            if (this.valuesToReplace != null) {
                this.valuesToReplace.clear();
                this.valuesToReplace.addAll(deltaToMerge.valuesToReplace);
            }
            this.valuesToReplace = newValueCollection();
            this.valuesToReplace.addAll(deltaToMerge.valuesToReplace);
        } else {
            addValuesToAdd(deltaToMerge.valuesToAdd);
            addValuesToDelete(deltaToMerge.valuesToDelete);
        }
    }

    /**
     * Apply this delta (path) to a property container.
     */
    public void applyTo(PrismContainer propertyContainer) {
        // valueClass is kind of HACK, it should be FIXME
        Class<?> valueClass = getValueClass();
        PrismProperty property = null; //FIXME propertyContainer.findOrCreateProperty(getParentPath(), getName(), valueClass);
        applyTo(property);
    }

    public Class<?> getValueClass() {
        if (valuesToReplace != null && !valuesToReplace.isEmpty()) {
            return valuesToReplace.iterator().next().getValue().getClass();
        }
        if (valuesToAdd != null && !valuesToAdd.isEmpty()) {
            return valuesToAdd.iterator().next().getValue().getClass();
        }
        if (valuesToDelete != null && !valuesToDelete.isEmpty()) {
            return valuesToDelete.iterator().next().getValue().getClass();
        }
        return null;
    }

    /**
     * Apply this delta (path) to a property.
     */
    public void applyTo(PrismProperty property) {
        if (valuesToReplace != null) {
            property.replaceValues(valuesToReplace);
            return;
        }
        if (valuesToAdd != null) {
            property.addValues(valuesToAdd);
        }
        if (valuesToDelete != null) {
            property.deleteValues(valuesToDelete);
        }
    }

    public void addValuesToAdd(Collection<PrismPropertyValue<Object>> newValues) {
        if (valuesToAdd == null) {
            valuesToAdd = newValueCollection();
        }
        valuesToAdd.addAll(newValues);
    }

    public void addValueToAdd(PrismPropertyValue<Object> newValue) {
        if (valuesToAdd == null) {
            valuesToAdd = newValueCollection();
        }
        valuesToAdd.add(newValue);
    }

    public void addValuesToDelete(Collection<PrismPropertyValue<Object>> newValues) {
        if (valuesToDelete == null) {
            valuesToDelete = newValueCollection();
        }
        valuesToDelete.addAll(newValues);
    }

    public void addValueToDelete(PrismPropertyValue<Object> newValue) {
        if (valuesToDelete == null) {
            valuesToDelete = newValueCollection();
        }
        valuesToDelete.add(newValue);
    }

    public void setValuesToReplace(Collection<PrismPropertyValue<?>> newValues) {
        if (valuesToReplace == null) {
            valuesToReplace = newValueCollection();
        } else {
            valuesToReplace.clear();
        }
        valuesToReplace.addAll((Collection)newValues);
    }
    
    public void setValueToReplace(PrismPropertyValue<?> newValue) {
        if (valuesToReplace == null) {
            valuesToReplace = newValueCollection();
        } else {
            valuesToReplace.clear();
        }
        valuesToReplace.add((PrismPropertyValue<Object>) newValue);
    }

    public boolean isEmpty() {
        if (valuesToAdd == null && valuesToDelete == null && valuesToReplace == null) {
            return true;
        }
        return false;
    }

    private Collection<PrismPropertyValue<Object>> newValueCollection() {
        return new HashSet<PrismPropertyValue<Object>>();
    }

    public boolean isValueToAdd(PrismPropertyValue<?> value) {
        if (valuesToAdd == null) {
            return false;
        }
        return valuesToAdd.contains(value);
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

    public boolean isValueToDelete(PrismPropertyValue<?> value) {
        if (valuesToDelete == null) {
            return false;
        }
        return valuesToDelete.contains(value);
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
    public PrismProperty getPropertyNew() {
        if (valuesToAdd != null && valuesToDelete != null) {
            throw new IllegalStateException("Cannot fetch new property state, not a 'replace' delta");
        }
        PrismProperty prop = propertyDefinition.instantiate();
        if (valuesToReplace == null || valuesToReplace.isEmpty()) {
            return prop;
        }
        prop.getValues().addAll(valuesToReplace);
        return prop;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PropertyDelta(");
        sb.append(parentPath).append(" / ").append(DebugUtil.prettyPrint(name));
        if (valuesToReplace != null) {
            sb.append(", REPLACE");
        }

        if (valuesToAdd != null) {
            sb.append(", ADD");
        }

        if (valuesToDelete != null) {
            sb.append(", DELETE");
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append(INDENT_STRING);
        }
        sb.append("PropertyDelta(");
        sb.append(parentPath).append(" / ").append(DebugUtil.prettyPrint(name)).append(")");

        if (valuesToReplace != null) {
            sb.append("\n");
            dumpValues(sb, "REPLACE", valuesToReplace, indent + 1);
        }

        if (valuesToAdd != null) {
            sb.append("\n");
            dumpValues(sb, "ADD", valuesToAdd, indent + 1);
        }

        if (valuesToDelete != null) {
            sb.append("\n");
            dumpValues(sb, "DELETE", valuesToDelete, indent + 1);
        }

        return sb.toString();

    }

    public String dump() {
        return debugDump();
    }

    private void dumpValues(StringBuilder sb, String label, Collection<PrismPropertyValue<Object>> values, int indent) {
        for (int i = 0; i < indent; i++) {
            sb.append(INDENT_STRING);
        }
        sb.append(label).append(": ");
        if (values == null) {
            sb.append("(null)");
        } else {
            Iterator<PrismPropertyValue<Object>> i = values.iterator();
            while (i.hasNext()) {
                sb.append(DebugUtil.prettyPrint(i.next()));
                if (i.hasNext()) {
                    sb.append(", ");
                }
            }
        }
    }


}
