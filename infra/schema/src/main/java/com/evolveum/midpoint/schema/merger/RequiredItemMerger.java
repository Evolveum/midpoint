/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger;

import static com.evolveum.midpoint.util.MiscUtil.configCheck;

import com.evolveum.midpoint.prism.delta.ItemMerger;
import com.evolveum.midpoint.prism.key.NaturalKeyDefinition;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.merger.resource.ResourceMergeOperation;
import com.evolveum.midpoint.util.exception.ConfigurationException;

/**
 * Does no actual data movement; just requires that the value does exist in the target object.
 */
public class RequiredItemMerger implements ItemMerger {

    public static final RequiredItemMerger INSTANCE = new RequiredItemMerger();

    @Override
    public NaturalKeyDefinition getNaturalKey() {
        throw new UnsupportedOperationException("IgnoreSourceItemMerger does not support natural keys");
    }

    @Override
    public void merge(@NotNull ItemName itemName, @NotNull PrismContainerValue<?> target, @NotNull PrismContainerValue<?> source)
            throws ConfigurationException {

        configCheck(ResourceMergeOperation.hasValue(target, itemName),
                "Item %s is required but not present in %s", itemName, target);
    }
}
