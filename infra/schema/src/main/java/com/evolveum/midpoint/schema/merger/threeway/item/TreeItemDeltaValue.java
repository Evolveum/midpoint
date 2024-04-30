/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.threeway.item;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ModificationType;
import com.evolveum.midpoint.prism.PrismValue;

public class TreeItemDeltaValue<V extends PrismValue> {

    // todo improve
    private List<Object> naturalKey;

    private V value;

    private ModificationType modificationType;

    public TreeItemDeltaValue(V value, ModificationType modificationType) {
        this.value = value;
        this.modificationType = modificationType;
    }

    @NotNull
    public List<Object> getNaturalKey() {
        if (naturalKey == null) {
            naturalKey = new ArrayList<>();
        }
        return naturalKey;
    }

    public void setNaturalKey(@NotNull List<Object> naturalKey) {
        this.naturalKey = naturalKey;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public ModificationType getModificationType() {
        return modificationType;
    }

    public void setModificationType(ModificationType modificationType) {
        this.modificationType = modificationType;
    }
}
