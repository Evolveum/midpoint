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

package com.evolveum.midpoint.schema.delta;

import com.evolveum.midpoint.schema.XsdTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.util.DebugUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType.Value;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.ArrayList;
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

    private void checkConsistence() {
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
    public void applyTo(PropertyContainer propertyContainer) {
        // valueClass is kind of HACK, it should be FIXME
        Class<?> valueClass = getValueClass();
        Property property = propertyContainer.findOrCreateProperty(getParentPath(), getName(), valueClass);
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
    public void applyTo(Property property) {
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
     * Creates delta from PropertyModificationType (XML). The values inside the PropertyModificationType are converted to java.
     * That's the reason this method needs schema and objectType (to locate the appropriate definitions).
     */
    public static PropertyDelta createDelta(PropertyModificationType propMod, Schema schema,
            Class<? extends ObjectType> objectType) throws SchemaException {
        ObjectDefinition<? extends ObjectType> objectDefinition = schema.findObjectDefinition(objectType);
        return createDelta(propMod, objectDefinition);
    }

    public static PropertyDelta createDelta(PropertyModificationType propMod, PropertyContainerDefinition pcDef) throws
            SchemaException {
        if (propMod.getValue() == null) {
            throw new IllegalArgumentException("No value in property modificiation (path " + propMod.getPath() + ") while creating a property delta");
        }
        XPathHolder xpath = new XPathHolder(propMod.getPath());
        PropertyPath parentPath = new PropertyPath(xpath);
        PropertyContainerDefinition containingPcd = pcDef.findPropertyContainerDefinition(parentPath);
        if (containingPcd == null) {
            throw new SchemaException("No container definition for " + parentPath + " (while creating delta for " + pcDef + ")");
        }
        Collection<? extends Item> items = containingPcd.parseItems(propMod.getValue().getAny(), parentPath);
        if (items.size() > 1) {
            throw new SchemaException("Expected presence of a single property (path " + propMod.getPath() + ") in a object modification, but found " + items.size() + " instead");
        }
        if (items.size() < 1) {
            throw new SchemaException("Expected presence of a property value (path " + propMod.getPath() + ") in a object modification, but found nothing");
        }
        Item item = items.iterator().next();
        if (!(item instanceof Property)) {
            throw new SchemaException("Expected presence of a property (" + item.getName() + ",path " + propMod.getPath() + ") in a object modification, but found " + item.getClass().getSimpleName() + " instead", item.getName());
        }
        Property prop = (Property) item;
        PropertyDelta propDelta = new PropertyDelta(parentPath, prop.getName());
        if (propMod.getModificationType() == PropertyModificationTypeType.add) {
            propDelta.addValuesToAdd(prop.getValues());
        } else if (propMod.getModificationType() == PropertyModificationTypeType.delete) {
            propDelta.addValuesToDelete(prop.getValues());
        } else if (propMod.getModificationType() == PropertyModificationTypeType.replace) {
            propDelta.setValuesToReplace(prop.getValues());
        }

        return propDelta;
    }

    /**
     * Converts this delta to PropertyModificationType (XML).
     */
    public Collection<PropertyModificationType> toPropertyModificationTypes() throws SchemaException {
        checkConsistence();
        Collection<PropertyModificationType> mods = new ArrayList<PropertyModificationType>();
        XPathHolder xpath = new XPathHolder(parentPath);
        Document document = DOMUtil.getDocument();
        Element xpathElement = xpath.toElement(SchemaConstants.C_PATH, document);
        if (valuesToReplace != null) {
            PropertyModificationType mod = new PropertyModificationType();
            mod.setPath(xpathElement);
            mod.setModificationType(PropertyModificationTypeType.replace);
            try {
                addModValues(mod, valuesToReplace, document);
            } catch (SchemaException e) {
                throw new SchemaException(e.getMessage() + " while converting property " + name, e);
            }
            mods.add(mod);
        }
        if (valuesToAdd != null) {
            PropertyModificationType mod = new PropertyModificationType();
            mod.setPath(xpathElement);
            mod.setModificationType(PropertyModificationTypeType.add);
            try {
                addModValues(mod, valuesToAdd, document);
            } catch (SchemaException e) {
                throw new SchemaException(e.getMessage() + " while converting property " + name, e);
            }
            mods.add(mod);
        }
        if (valuesToDelete != null) {
            PropertyModificationType mod = new PropertyModificationType();
            mod.setPath(xpathElement);
            mod.setModificationType(PropertyModificationTypeType.delete);
            try {
                addModValues(mod, valuesToDelete, document);
            } catch (SchemaException e) {
                throw new SchemaException(e.getMessage() + " while converting property " + name, e);
            }
            mods.add(mod);
        }
        return mods;
    }

    private void addModValues(PropertyModificationType mod, Collection<PropertyValue<Object>> values,
            Document document) throws SchemaException {
        Value modValue = new Value();
        mod.setValue(modValue);
        for (PropertyValue<Object> value : values) {
        	// Always record xsi:type. This is FIXME, but should work OK for now (until we put definition into deltas)
            modValue.getAny().add(XsdTypeConverter.toXsdElement(value.getValue(), name, document, true));
        }
    }

    /**
     * Returns the "new" state of the property - the state that would be after the delta
     * is applied.
     * Assumes "replace" delta.
     */
    public Property getPropertyNew(PropertyDefinition propertyDefinition, PropertyPath parentPath) {
        if (valuesToAdd != null && valuesToDelete != null) {
            throw new IllegalStateException("Cannot fetch new property state, not a 'replace' delta");
        }
        Property prop = propertyDefinition.instantiate(parentPath);
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
