/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.threeway.item;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ModificationType;
import com.evolveum.midpoint.prism.PrismContainerValue;

public class TreeContainerDeltaValue<C extends Containerable> extends TreeItemDeltaValue<PrismContainerValue<C>> {

    private Long id;

    public TreeContainerDeltaValue(
            PrismContainerValue<C> value, ModificationType modificationType) {

        this(value, modificationType, value != null ? value.getId() : null);
    }

    public TreeContainerDeltaValue(
            PrismContainerValue<C> value, ModificationType modificationType, @Nullable Long id) {

        super(value, modificationType);

        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public static <C extends Containerable> TreeContainerDeltaValue<C> from(
            @NotNull PrismContainerValue<C> value, @NotNull ModificationType modificationType) {
        return new TreeContainerDeltaValue<>(value, modificationType);
    }
}
