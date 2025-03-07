/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.impl.PrismContextImpl;
import com.evolveum.midpoint.prism.impl.PrismPropertyImpl;
import com.evolveum.midpoint.prism.impl.PrismReferenceValueImpl;
import com.evolveum.midpoint.prism.impl.xnode.XNodeImpl;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType;

import com.evolveum.prism.xml.ns._public.types_3.RawType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.util.MiscUtil.requireNonNull;

/**
 * Converts item delta bean to item delta.
 *
 * As this is quite complex process, it was refactored into separate class.
 */
class ItemDeltaBeanToNativeConversion<IV extends PrismValue, ID extends ItemDefinition<?>> {

    private static final Trace LOGGER = TraceManager.getTrace(ItemDeltaBeanToNativeConversion.class);
    private static final QName T_RAW_TYPE = new QName(PrismConstants.NS_TYPES, RawType.class.getSimpleName());

    /**
     * Delta to be converted ("XML").
     */
    @NotNull private final ItemDeltaType deltaBean;

    /**
     * Item referred to by the delta.
     */
    @NotNull private final ItemPath itemPath;

    /**
     * Name of the item. (The last part of {@link #itemPath}.)
     */
    @NotNull private final ItemName itemName;

    /**
     * Definition of the container where itemPath starts.
     */
    @NotNull private final PrismContainerDefinition<?> rootContainerDef;

    /**
     * Definition of the item. It is sometimes determined only when parsing.
     */
    private ItemDefinition<?> itemDefinition;

    private final boolean convertUnknownTypes;

    /**
     * Definition of the item parent container. It is used only when itemDefinition cannot be determined directly.
     * As soon as itemDefinition is not null, this one is useless.
     * When parentDefinition and itemDefinition are null, then parsed value will be type RawType.
     */
    private PrismContainerDefinition<?> parentDefinition;

    ItemDeltaBeanToNativeConversion(
            @NotNull ItemDeltaType deltaBean,
            @NotNull PrismContainerDefinition<?> rootContainerDef,
            boolean convertUnknownTypes)
            throws SchemaException {
        this.deltaBean = deltaBean;
        itemPath =
                requireNonNull(deltaBean.getPath(), () -> "Path argument in the item delta has to be specified")
                        .getItemPath();
        itemName = requireNonNull(itemPath.lastName(), () -> "No item name in the item delta: " + itemPath);
        this.rootContainerDef = rootContainerDef;
        this.convertUnknownTypes = convertUnknownTypes;
        itemDefinition = rootContainerDef.findItemDefinition(itemPath);
        findParentDefinitionIfNeeded();
    }

    public ItemDelta<IV, ID> convert() throws SchemaException {
        if (convertUnknownTypes && hasUnknownTypes()) {
            if (itemDefinition != null) {
                // We have unknown type and we can not continue with normal type-safe processing, so we create new runtime property
                // definition, which has LegacyDelta annotation, same item name, but type is raw type, so no additional
                // checks are made on it's content.
                // This allows for GUI to visualize JSON form of data
                var replacementDef = PrismContext.get().definitionFactory()
                        .newPropertyDefinition(itemDefinition.getItemName(), T_RAW_TYPE);
                replacementDef.mutator().setAnnotation(DeltaConvertor.LEGACY_DELTA, DeltaConvertor.LEGACY_DELTA);
                itemDefinition = replacementDef;
            }
        }

        Collection<IV> parsedValues = getParsedValues(deltaBean.getValue());
        ItemDelta<IV, ID> itemDelta = createDelta();
        if (deltaBean.getModificationType() == ModificationTypeType.ADD) {
            itemDelta.addValuesToAdd(parsedValues);
        } else if (deltaBean.getModificationType() == ModificationTypeType.DELETE) {
            itemDelta.addValuesToDelete(parsedValues);
        } else if (deltaBean.getModificationType() == ModificationTypeType.REPLACE) {
            itemDelta.setValuesToReplace(parsedValues);
        }

        if (!deltaBean.getEstimatedOldValue().isEmpty()) {
            itemDelta.setEstimatedOldValues(
                    getParsedValues(deltaBean.getEstimatedOldValue()));
        }
        return itemDelta;
    }

    private @NotNull ItemDelta<IV, ID> createDelta() {
        if (itemDefinition != null) {
            //noinspection unchecked
            return (ItemDelta<IV, ID>) itemDefinition.createEmptyDelta(itemPath);
        } else {
            PrismProperty<?> property = new PrismPropertyImpl<>(itemName);
            //noinspection unchecked
            return (ItemDelta<IV, ID>) property.createDelta(itemPath);
        }
    }

    private void findParentDefinitionIfNeeded() throws SchemaException {
        if (itemDefinition == null) {
            parentDefinition = rootContainerDef.findContainerDefinition(itemPath.allUpToLastName());
            if (parentDefinition == null) {
                LOGGER.debug("No definition for {} (while creating delta for {})",
                        itemPath.allUpToLastName().lastName(), rootContainerDef);
            }
        }
    }
    private boolean hasUnknownTypes() {
        for (RawType rawValue : deltaBean.getValue()) {
            if (typeNotExists(rawValue.getExplicitTypeName())) {
                return true;
            }
        }
        return false;
    }

    private Collection<IV> getParsedValues(List<RawType> values)
            throws SchemaException {

        List<IV> parsedValues = new ArrayList<>();
        for (RawType rawValue : values) {
            if (itemDefinition == null && parentDefinition != null) {
                //noinspection unchecked
                itemDefinition = (ID) ((PrismContextImpl) PrismContext.get()).getPrismUnmarshaller()
                        .locateItemDefinition(parentDefinition, itemName, rawValue.getXnode());
            }
            // Note this can be a slight problem if itemDefinition is PRD and the value is a full object.
            if (convertUnknownTypes) {
                if (typeNotExists(rawValue.getExplicitTypeName())) {
                    // Here we should do some rawValue magic or item definition magic
                    if (rawValue.getXnode() != null) {
                        XNodeImpl xnodeReplacement = (XNodeImpl) rawValue.getXnode().clone();
                        xnodeReplacement.setTypeQName(null);
                        rawValue.setRawValue(xnodeReplacement.frozen());
                    }
                }
            }
            PrismValue parsed = rawValue.getParsedValue(itemDefinition, itemName);


            if (parsed != null) {
                PrismValue converted;
                if (itemDefinition instanceof PrismReferenceDefinition) {
                    converted = convertValueForReferenceDelta(parsed);
                } else {
                    converted = parsed;
                }
                //noinspection unchecked
                parsedValues.add((IV) converted.cloneComplex(CloneStrategy.LITERAL));
            }
        }
        return parsedValues;
    }

    private boolean typeNotExists(QName explicitTypeName) {
        if (explicitTypeName == null) {
            return false;
        }
        // Supported XSD types do not have type definition as of 4.9-M3
        if (XmlTypeConverter.canConvert(explicitTypeName)) {
            return false;
        }

        var typeDef = PrismContext.get().getSchemaRegistry().findTypeDefinitionByType(explicitTypeName);
        return typeDef == null;
    }

    private PrismReferenceValue convertValueForReferenceDelta(PrismValue value) {
        if (value instanceof PrismReferenceValue) {
            return (PrismReferenceValue) value;
        } else if (value instanceof PrismContainerValue) {
            // this is embedded (full) object
            Containerable c = ((PrismContainerValue<?>) value).asContainerable();
            if (c instanceof Objectable) {
                PrismReferenceValue ref = new PrismReferenceValueImpl();
                ref.setObject(((Objectable) c).asPrismObject());
                return ref;
            } else {
                throw new IllegalStateException("Content of " + itemDefinition +
                        " is a Containerable but not Objectable: " + c);
            }
        } else {
            throw new IllegalStateException("Content of " + itemDefinition
                    + " is neither PrismReferenceValue nor PrismContainerValue: " + value);
        }
    }
}
