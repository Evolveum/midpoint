/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.threeway.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugUtil;

public class ContainerTreeDeltaValue<C extends Containerable> extends ItemTreeDeltaValue<PrismContainerValue<C>, ContainerTreeDelta<C>> {

    private List<ItemTreeDelta<?, ?, ?, ?>> deltas;

    public ContainerTreeDeltaValue() {
        this(null, null);
    }

    public ContainerTreeDeltaValue(PrismContainerValue<C> value, ModificationType modificationType) {

        super(value, modificationType);
    }

    public Long getId() {
        PrismContainerValue<C> value = getValue();

        return value != null ? value.getId() : null;
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

        DebugUtil.debugDumpWithLabel(sb, "id", getId(), indent);
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabel(sb, "deltas", deltas, indent + 1);
    }

    public <D extends ItemTreeDelta> D findItemDelta(ItemPath path, Class<D> deltaClass) {
        return findOrCreateItemDelta(path, deltaClass, false);
    }

    public <D extends ItemTreeDelta> D findOrCreateItemDelta(ItemPath path, Class<D> deltaClass) {
        return findOrCreateItemDelta(path, deltaClass, true);
    }

    private <D extends ItemTreeDelta> D findOrCreateItemDelta(ItemPath path, Class<D> deltaClass, boolean createIntermediate) {
        ItemName name = path.firstToNameOrNull();
        if (name == null) {
            throw new IllegalArgumentException("Attempt to get segment without a name from a container delta");
        }

        ItemTreeDelta<?, ?, ?, ?> delta = getDeltas().stream()
                .filter(d -> name.matches(d.getItemName()))
                .findFirst()
                .orElse(null);

        if (delta == null) {
            if (!createIntermediate) {
                return null;
            }

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

    @Override
    public ItemPath getPath() {
        ItemTreeDelta parent = getParent();
        if (parent == null) {
            return getId() != null ? ItemPath.create(getId()) : null;
        }

        return parent.getPath().append(getId());
    }

    @Override
    public void accept(Visitor visitor) {
        super.accept(visitor);

        getDeltas().forEach(d -> d.accept(visitor));
    }

    @Override
    public List<Conflict> getConflictsWith(ItemTreeDeltaValue other, EquivalenceStrategy strategy) {
        List<Conflict> result = super.getConflictsWith(other, strategy);
        if (!result.isEmpty()) {
            return result;
        }

        result = new ArrayList<>();

        ContainerTreeDeltaValue<C> otherValue = (ContainerTreeDeltaValue<C>) other;
        for (ItemTreeDelta delta : getDeltas()) {
            ItemTreeDelta otherDelta = otherValue.findItemDelta(ItemPath.create(delta.getItemName()), delta.getClass());
            if (otherDelta == null) {
                continue;
            }

            List<Conflict> deltaConflicts = delta.getConflictsWith(otherDelta, strategy);
            result.addAll(deltaConflicts);
        }

        return result;
    }

    @Override
    protected boolean hasConflictWith(PrismContainerValue<C> otherValue) {
        if (otherValue == null) {
            return false;
        }

        // todo natural keys

        return Objects.equals(getId(), otherValue.getId());
    }

    @Override
    public <V extends ItemTreeDeltaValue> boolean match(V other) {
        if (other == null) {
            return false;
        }

        PrismContainerValue<C> pcv = (PrismContainerValue<C>) other.getValue();
        if (pcv == null) {
            return false;
        }

        // todo natural keys

        return Objects.equals(getId(), pcv.getId());
    }

    @Override
    public boolean containsModifications() {
        if (super.containsModifications()) {
            return true;
        }

        return getDeltas().stream().anyMatch(ItemTreeDelta::containsModifications);
    }
}
