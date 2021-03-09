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
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType;

import com.evolveum.prism.xml.ns._public.types_3.RawType;

import org.jetbrains.annotations.NotNull;

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
    private ID itemDefinition;

    /**
     * Definition of the item parent container. It is used only when itemDefinition cannot be determined directly.
     * As soon as itemDefinition is not null, this one is useless and can be even null.
     * On the other hand, if itemDefinition is null, this one must not be null.
     */
    private PrismContainerDefinition<?> parentDefinition;

    ItemDeltaBeanToNativeConversion(@NotNull ItemDeltaType deltaBean, @NotNull PrismContainerDefinition<?> rootContainerDef)
            throws SchemaException {
        this.deltaBean = deltaBean;
        itemPath =
                requireNonNull(deltaBean.getPath(), () -> "Path argument in the item delta has to be specified")
                        .getItemPath();
        itemName = requireNonNull(itemPath.lastName(), () -> "No item name in the item delta: " + itemPath);
        this.rootContainerDef = rootContainerDef;

        itemDefinition = rootContainerDef.findItemDefinition(itemPath);
        findParentDefinitionIfNeeded();
    }

    public ItemDelta<IV, ID> convert() throws SchemaException {
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
            Collection<IV> parsedOldValues = getParsedValues(deltaBean.getEstimatedOldValue());
            itemDelta.addEstimatedOldValues(parsedOldValues);
        }

        return itemDelta;
    }

    private ItemDelta<IV, ID> createDelta() {
        if (itemDefinition != null) {
            //noinspection unchecked
            return itemDefinition.createEmptyDelta(itemPath);
        } else {
            PrismProperty<?> property = new PrismPropertyImpl<>(itemName, parentDefinition.getPrismContext());
            //noinspection unchecked
            return (ItemDelta<IV, ID>) property.createDelta(itemPath);
        }
    }

    private void findParentDefinitionIfNeeded() throws SchemaException {
        if (itemDefinition == null) {
            parentDefinition = rootContainerDef.findContainerDefinition(itemPath.allUpToLastName());
            if (parentDefinition == null) {
                throw new SchemaException("No definition for " + itemPath.allUpToLastName().lastName() +
                        " (while creating delta for " + rootContainerDef + ")");
            }
        }
    }

    private Collection<IV> getParsedValues(List<RawType> values)
            throws SchemaException {

        List<IV> parsedValues = new ArrayList<>();
        for (RawType rawValue : values) {
            if (itemDefinition == null) {
                assert parentDefinition != null;
                //noinspection unchecked
                itemDefinition = (ID) ((PrismContextImpl) parentDefinition.getPrismContext()).getPrismUnmarshaller()
                        .locateItemDefinition(parentDefinition, itemName, rawValue.getXnode());
            }
            // Note this can be a slight problem if itemDefinition is PRD and the value is a full object.
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
