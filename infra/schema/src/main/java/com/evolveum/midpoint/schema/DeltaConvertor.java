/**
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.schema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.Schema;
import com.evolveum.midpoint.prism.XmlTypeConverter;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType.Value;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;

/**
 * @author semancik
 *
 */
public class DeltaConvertor {
	
    /**
     * Creates new delta from the ObjectModificationType (XML). Object type and schema are used to locate definitions
     * needed to convert properties from XML.
     */
    public static <T extends Objectable> ObjectDelta<T> createObjectDelta(ObjectModificationType objectModification,
            Schema schema, Class<T> type) throws SchemaException {
        return createObjectDelta(objectModification, schema.findObjectDefinition(type));
    }

    public static <T extends Objectable> ObjectDelta<T> createObjectDelta(ObjectModificationType objectModification,
            PrismObjectDefinition<T> objDef) throws SchemaException {
        ObjectDelta<T> objectDelta = new ObjectDelta<T>(objDef.getJaxbClass(), ChangeType.MODIFY);
        objectDelta.setOid(objectModification.getOid());

        for (PropertyModificationType propMod : objectModification.getPropertyModification()) {
            PropertyDelta propDelta = createPropertyDelta(propMod, objDef);
            objectDelta.addModification(propDelta);
        }

        return objectDelta;
    }
    
    /**
     * Converts this delta to ObjectModificationType (XML).
     */
    public static <T extends Objectable> ObjectModificationType toObjectModificationType(ObjectDelta<T> delta) throws SchemaException {
        if (delta.getChangeType() != ChangeType.MODIFY) {
            throw new IllegalStateException("Cannot produce ObjectModificationType from delta of type " + delta.getChangeType());
        }
        ObjectModificationType modType = new ObjectModificationType();
        modType.setOid(delta.getOid());
        List<PropertyModificationType> propModTypes = modType.getPropertyModification();
        for (PropertyDelta propDelta : delta.getModifications()) {
            Collection<PropertyModificationType> propPropModTypes;
            try {
                propPropModTypes = toPropertyModificationTypes(propDelta);
            } catch (SchemaException e) {
                throw new SchemaException(e.getMessage() + " in " + delta.toString(), e);
            }
            propModTypes.addAll(propPropModTypes);
        }
        return modType;
    }
    
    /**
     * Creates delta from PropertyModificationType (XML). The values inside the PropertyModificationType are converted to java.
     * That's the reason this method needs schema and objectType (to locate the appropriate definitions).
     */
    public static PropertyDelta createPropertyDelta(PropertyModificationType propMod, Schema schema,
            Class<? extends Objectable> objectType) throws SchemaException {
        PrismObjectDefinition<? extends Objectable> objectDefinition = schema.findObjectDefinition(objectType);
        return createPropertyDelta(propMod, objectDefinition);
    }

    public static PropertyDelta createPropertyDelta(PropertyModificationType propMod, PrismContainerDefinition pcDef) throws
            SchemaException {
        if (propMod.getValue() == null) {
            throw new IllegalArgumentException("No value in property modificiation (path " + propMod.getPath() + ") while creating a property delta");
        }
        XPathHolder xpath = new XPathHolder(propMod.getPath());
        PropertyPath parentPath = xpath.toPropertyPath();
        PrismContainerDefinition containingPcd = pcDef.findPropertyContainerDefinition(parentPath);
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
        if (!(item instanceof PrismProperty)) {
            throw new SchemaException("Expected presence of a property (" + item.getName() + ",path " + propMod.getPath() + ") in a object modification, but found " + item.getClass().getSimpleName() + " instead", item.getName());
        }
        PrismProperty prop = (PrismProperty) item;
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
    public static Collection<PropertyModificationType> toPropertyModificationTypes(PropertyDelta delta) throws SchemaException {
    	delta.checkConsistence();
        Collection<PropertyModificationType> mods = new ArrayList<PropertyModificationType>();
        XPathHolder xpath = new XPathHolder(delta.getParentPath());
        Document document = DOMUtil.getDocument();
        Element xpathElement = xpath.toElement(SchemaConstants.C_PATH, document);
        if (delta.getValuesToReplace() != null) {
            PropertyModificationType mod = new PropertyModificationType();
            mod.setPath(xpathElement);
            mod.setModificationType(PropertyModificationTypeType.replace);
            try {
                addModValues(delta, mod, delta.getValuesToReplace(), document);
            } catch (SchemaException e) {
                throw new SchemaException(e.getMessage() + " while converting property " + delta.getName(), e);
            }
            mods.add(mod);
        }
        if (delta.getValuesToAdd() != null) {
            PropertyModificationType mod = new PropertyModificationType();
            mod.setPath(xpathElement);
            mod.setModificationType(PropertyModificationTypeType.add);
            try {
                addModValues(delta, mod, delta.getValuesToAdd(), document);
            } catch (SchemaException e) {
                throw new SchemaException(e.getMessage() + " while converting property " + delta.getName(), e);
            }
            mods.add(mod);
        }
        if (delta.getValuesToDelete() != null) {
            PropertyModificationType mod = new PropertyModificationType();
            mod.setPath(xpathElement);
            mod.setModificationType(PropertyModificationTypeType.delete);
            try {
                addModValues(delta, mod, delta.getValuesToDelete(), document);
            } catch (SchemaException e) {
                throw new SchemaException(e.getMessage() + " while converting property " + delta.getName(), e);
            }
            mods.add(mod);
        }
        return mods;
    }

    private static void addModValues(PropertyDelta delta, PropertyModificationType mod, Collection<PrismPropertyValue<Object>> values,
            Document document) throws SchemaException {
        Value modValue = new Value();
        mod.setValue(modValue);
        for (PrismPropertyValue<Object> value : values) {
        	// Always record xsi:type. This is FIXME, but should work OK for now (until we put definition into deltas)
            modValue.getAny().add(XmlTypeConverter.toXsdElement(value.getValue(), delta.getName(), document, true));
        }
    }



}
