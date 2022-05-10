/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.key;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;

/**
 * Natural key consisting of a "singleton" item path (i.e. one that contains a single qualified name).
 * Like the one used in {@link ResourceAttributeDefinitionType}.
 *
 * Assumptions:
 *
 * 1. the type of the key value is {@link ItemPathType}
 * 2. each container value to be merged must have the key value specified
 */
public class SingletonItemPathNaturalKeyImpl implements NaturalKey {

    @NotNull private final ItemName itemName;

    private SingletonItemPathNaturalKeyImpl(@NotNull ItemName itemName) {
        this.itemName = itemName;
    }

    public static SingletonItemPathNaturalKeyImpl of(@NotNull ItemName itemName) {
        return new SingletonItemPathNaturalKeyImpl(itemName);
    }

    @Override
    public boolean valuesMatch(PrismContainerValue<?> targetValue, PrismContainerValue<?> sourceValue)
            throws ConfigurationException {
        return QNameUtil.match(
                getItemName(targetValue),
                getItemName(sourceValue));
    }

    /**
     * If the source contains qualified version of the path, and the target does not, we replace the target
     * value with the qualified version.
     */
    @Override
    public void mergeMatchingKeys(PrismContainerValue<?> targetValue, PrismContainerValue<?> sourceValue)
            throws ConfigurationException {
        if (QNameUtil.isUnqualified(getItemName(targetValue))
                && QNameUtil.isQualified(getItemName(sourceValue))) {
            try {
                targetValue.findProperty(itemName)
                        .replace(
                                sourceValue.findProperty(itemName).getValue().clone());
            } catch (SchemaException e) {
                throw SystemException.unexpected(e, "when updating '" + itemName + "'");
            }
        }
    }

    private @NotNull ItemName getItemName(PrismContainerValue<?> containerValue) throws ConfigurationException {
        PrismProperty<ItemPathType> property =
                MiscUtil.requireNonNull(
                        containerValue.findProperty(itemName),
                        () -> new ConfigurationException("No '" + itemName + "' in " + containerValue));
        try {
            ItemPathType itemPathType = MiscUtil.requireNonNull(
                    property.getRealValue(ItemPathType.class),
                    () -> new ConfigurationException("No '" + itemName + "' in " + containerValue));
            return itemPathType.getItemPath().asSingleNameOrFail();
        } catch (RuntimeException e) {
            throw new ConfigurationException("Couldn't get '" + itemName + "' from " + containerValue, e);
        }
    }
}
