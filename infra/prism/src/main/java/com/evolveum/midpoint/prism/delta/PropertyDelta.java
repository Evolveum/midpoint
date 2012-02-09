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

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.PropertyValue;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.MiscUtil;

import javax.xml.namespace.QName;
import java.util.Collection;
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

    Collection<PropertyValue<Object>> valuesToReplace = null;
    Collection<PropertyValue<Object>> valuesToAdd = null;
    Collection<PropertyValue<Object>> valuesToDelete = null;

    public PropertyDelta(QName name) {
        this.name = name;
        parentPath = new PropertyPath();
    }

    public PropertyDelta(PropertyPath parentPath, QName name) {
        this.name = name;
        this.parentPath = parentPath;
    }

    public PropertyDelta(PropertyPath propertyPath) {
        this.name = propertyPath.last();
        this.parentPath = propertyPath.allExceptLast();
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

    public boolean isReplace() {
        return (valuesToReplace != null);
    }

    public Collection<PropertyValue<Object>> getValuesToAdd() {
        return valuesToAdd;
    }

    public Collection<PropertyValue<Object>> getValuesToDelete() {
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
    
    public Collection<PropertyValue<Object>> getValuesToReplace() {
    	return valuesToReplace;
    }

    /**
     * Returns all values regardless of whether they are added or removed or replaced.
     * Useful for iterating over all the changed values.
     */
    public <T> Collection<PropertyValue<T>> getValues(Class<T> type) {
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
        PrismProperty property = propertyContainer.findOrCreateProperty(getParentPath(), getName(), valueClass);
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

    public void addValuesToAdd(Collection<PropertyValue<Object>> newValues) {
        if (valuesToAdd == null) {
            valuesToAdd = newValueCollection();
        }
        valuesToAdd.addAll(newValues);
    }

    public void addValueToAdd(PropertyValue<Object> newValue) {
        if (valuesToAdd == null) {
            valuesToAdd = newValueCollection();
        }
        valuesToAdd.add(newValue);
    }

    public void addValuesToDelete(Collection<PropertyValue<Object>> newValues) {
        if (valuesToDelete == null) {
            valuesToDelete = newValueCollection();
        }
        valuesToDelete.addAll(newValues);
    }

    public void addValueToDelete(PropertyValue<Object> newValue) {
        if (valuesToDelete == null) {
            valuesToDelete = newValueCollection();
        }
        valuesToDelete.add(newValue);
    }

    public void setValuesToReplace(Collection<PropertyValue<Object>> newValues) {
        if (valuesToReplace == null) {
            valuesToReplace = newValueCollection();
        } else {
            valuesToReplace.clear();
        }
        valuesToReplace.addAll(newValues);
    }

    public boolean isEmpty() {
        if (valuesToAdd == null && valuesToDelete == null && valuesToReplace == null) {
            return true;
        }
        return false;
    }

    private Collection<PropertyValue<Object>> newValueCollection() {
        return new HashSet<PropertyValue<Object>>();
    }

    public boolean isValueToAdd(PropertyValue<?> value) {
        if (valuesToAdd == null) {
            return false;
        }
        return valuesToAdd.contains(value);
    }

    public boolean isRealValueToAdd(PropertyValue<?> value) {
        if (valuesToAdd == null) {
            return false;
        }

        for (PropertyValue valueToAdd : valuesToAdd) {
            if (valueToAdd.equalsRealValue(value)) {
                return true;
            }
        }

        return false;
    }

    public boolean isValueToDelete(PropertyValue<?> value) {
        if (valuesToDelete == null) {
            return false;
        }
        return valuesToDelete.contains(value);
    }

    public boolean isRealValueToDelete(PropertyValue<?> value) {
        if (valuesToDelete == null) {
            return false;
        }

        for (PropertyValue valueToAdd : valuesToDelete) {
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
    public PrismProperty getPropertyNew(PrismPropertyDefinition propertyDefinition, PropertyPath parentPath) {
        if (valuesToAdd != null && valuesToDelete != null) {
            throw new IllegalStateException("Cannot fetch new property state, not a 'replace' delta");
        }
        PrismProperty prop = propertyDefinition.instantiate(parentPath);
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

    private void dumpValues(StringBuilder sb, String label, Collection<PropertyValue<Object>> values, int indent) {
        for (int i = 0; i < indent; i++) {
            sb.append(INDENT_STRING);
        }
        sb.append(label).append(": ");
        if (values == null) {
            sb.append("(null)");
        } else {
            Iterator<PropertyValue<Object>> i = values.iterator();
            while (i.hasNext()) {
                sb.append(DebugUtil.prettyPrint(i.next()));
                if (i.hasNext()) {
                    sb.append(", ");
                }
            }
        }
    }

}
