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
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ModificationType;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.DebugUtil;

public class ContainerTreeDeltaValue<C extends Containerable> extends ItemTreeDeltaValue<PrismContainerValue<C>> {

    private Long id;

    private List<ItemTreeDelta<?, ?, ?, ?>> deltas;

    public ContainerTreeDeltaValue(
            PrismContainerValue<C> value, ModificationType modificationType) {

        this(value, modificationType, value != null ? value.getId() : null);
    }

    public ContainerTreeDeltaValue(
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

    public List<ItemTreeDelta<?, ?, ?, ?>> getDeltas() {
        if (deltas == null) {
            deltas = new ArrayList<>();
        }
        return deltas;
    }

    public void setDeltas(List<ItemTreeDelta<?, ?, ?, ?>> deltas) {
        this.deltas = deltas;
    }

    public static <C extends Containerable> ContainerTreeDeltaValue<C> from(
            @NotNull PrismContainerValue<C> value, @NotNull ModificationType modificationType) {
        return new ContainerTreeDeltaValue<>(value, modificationType);
    }

    @Override
    protected String debugDumpShortName() {
        return "CTDV";
    }

    @Override
    protected void debugDumpTitle(StringBuilder sb, int indent) {
        super.debugDumpTitle(sb, indent);

        DebugUtil.debugDumpWithLabel(sb, "id", id, indent);
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabel(sb, "deltas", deltas, indent + 1);
    }
}
