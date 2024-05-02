/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.threeway.item;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugUtil;

public class ContainerTreeDeltaValue<C extends Containerable> extends ItemTreeDeltaValue<PrismContainerValue<C>, ContainerTreeDelta<C>> {

    private Long id;

    private List<ItemTreeDelta<?, ?, ?, ?>> deltas;

    public ContainerTreeDeltaValue() {
        this(null, null);
    }

    public ContainerTreeDeltaValue(
            PrismContainerValue<C> value, ModificationType modificationType) {

        this(value, modificationType, value != null ? value.getId() : null);
    }

    public ContainerTreeDeltaValue(
            PrismContainerValue<C> value, ModificationType modificationType, @Nullable Long id) {

        super(value, modificationType);

        this.id = id;
    }

    @Override
    public void setValue(PrismContainerValue<C> value) {
        super.setValue(value);

        if (value != null) {
            this.id = value.getId();
        }
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

    @Override
    protected String debugDumpShortName() {
        return "CTDV";
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();

        // todo

        return sb.toString();
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

    public <D extends ItemTreeDelta> D findOrCreateItemDelta(ItemPath path, Class<D> deltaClass) {
        ItemName name = path.firstToNameOrNull();
        if (name == null) {
            throw new IllegalArgumentException("Attempt to get segment without a name from a container delta");
        }

        ItemTreeDelta<?, ?, ?, ?> delta = getDeltas().stream()
                .filter(d -> name.matches(d.getItemName()))
                .findFirst()
                .orElse(null);

        if (delta == null) {
            ItemDefinition def = getParent().getDefinition();
            ItemDefinition itemDefinition = def.findItemDefinition(name, ItemDefinition.class);

            if (itemDefinition instanceof PrismContainerDefinition<?> pcd) {
                delta = new ContainerTreeDelta<>(pcd);
            } else if (itemDefinition instanceof PrismPropertyDefinition<?> ppd) {
                delta = new PropertyTreeDelta<>(ppd);
            } else if (itemDefinition instanceof PrismReferenceDefinition prd) {
                delta = new ReferenceTreeDelta(prd);
            }

            delta.setParent(this);

            getDeltas().add(delta);
        }

        ItemPath rest = path.rest();
        if (rest.isEmpty()) {
            if (!deltaClass.isAssignableFrom(delta.getClass())) {
                throw new IllegalArgumentException("Delta class " + delta.getClass() + " is not compatible with requested class " + deltaClass);
            }
            return (D) delta;
        }

        if (delta instanceof ContainerTreeDelta<?> ctd) {
            return ctd.findOrCreateItemDelta(rest, deltaClass);
        } else {
            throw new IllegalArgumentException("Attempt to find or create delta for non-container item");
        }
    }
}
