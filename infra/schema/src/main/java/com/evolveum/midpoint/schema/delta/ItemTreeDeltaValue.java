/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.delta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ModificationType;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SystemException;

public abstract class ItemTreeDeltaValue<PV extends PrismValue, ITD extends ItemTreeDelta>
        implements DebugDumpable, Visitable {

    private ITD parent;

    // todo improve
    private List<Object> naturalKey;

    private PV value;

    private ModificationType modificationType;

    public ItemTreeDeltaValue(PV value, ModificationType modificationType) {
        this.value = value;
        this.modificationType = modificationType;
    }

    public List<Object> getNaturalKey() {
        return naturalKey;
    }

    public void setNaturalKey(@NotNull List<Object> naturalKey) {
        this.naturalKey = naturalKey;
    }

    public PV getValue() {
        return value;
    }

    public void setValue(PV value) {
        this.value = value;
    }

    public ModificationType getModificationType() {
        return modificationType;
    }

    public void setModificationType(ModificationType modificationType) {
        this.modificationType = modificationType;
    }

    public ITD getParent() {
        return parent;
    }

    public void setParent(ITD parent) {
        this.parent = parent;
    }

    @Override
    public String toString() {
        return debugDump();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();

        DebugUtil.debugDumpLabel(sb, debugDumpShortName(), indent);

        debugDumpIdentifiers(sb);

        if (naturalKey != null) {
            DebugUtil.debugDumpWithLabelLn(sb, "naturalKey", naturalKey, indent + 1);
        } else {
            sb.append("\n");
        }

        DebugUtil.debugDumpWithLabelLn(sb, "modification", modificationType, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "value", value, indent + 1);

        debugDumpChildren(sb, indent);

        return sb.toString();
    }

    protected void debugDumpIdentifiers(StringBuilder sb) {

    }

    protected void debugDumpChildren(StringBuilder sb, int indent) {

    }

    protected String debugDumpShortName() {
        return getClass().getSimpleName();
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    public ItemPath getPath() {
        return parent != null ? parent.getPath() : null;
    }

    @NotNull
    public List<Conflict> getConflictsWith(ItemTreeDeltaValue other, EquivalenceStrategy strategy) {
        if (other == null) {
            return List.of();
        }

        // todo check parent path?

        if (modificationType == null && other.modificationType == null) {
            return List.of();
        }

        if (modificationType != other.modificationType) {
            if (!getParent().getDefinition().isSingleValue()) {
                return List.of(new Conflict(this, other));
            }

            List<ModificationType> list = Arrays.asList(modificationType, other.modificationType);
            if (!list.contains(ModificationType.ADD) || !list.contains(ModificationType.REPLACE)) {
                return List.of(new Conflict(this, other));
            }
        }

        if (value.equals(other.getValue(), strategy)) {
            return List.of();
        }

        return List.of(new Conflict(this, other));
    }

    public boolean hasConflictWith(ItemTreeDeltaValue other, EquivalenceStrategy strategy) {
        return !getConflictsWith(other, strategy).isEmpty();
    }

    protected boolean hasConflictWith(PV otherValue) {
        if (value == null || otherValue == null) {
            return false;
        }

        return !value.equals(otherValue, EquivalenceStrategy.REAL_VALUE);
    }

    public <V extends ItemTreeDeltaValue> boolean match(V other, EquivalenceStrategy strategy) {
        if (other == null) {
            return false;
        }

        if (value == null || other.getValue() == null) {
            return false;
        }

        return value.equals(other.getValue(), strategy);
    }

    public boolean containsModifications() {
        return modificationType != null;
    }

    public Collection<? extends ItemDelta<?, ?>> getNonConflictingModifications(
            ItemTreeDeltaValue other, EquivalenceStrategy strategy) {

        if (other == null) {
            return getModifications();
        }

        // todo check parent path?

        if (modificationType == null && other.modificationType == null) {
            return getModifications();
        }

        if (modificationType != other.modificationType) {
            if (!getParent().getDefinition().isSingleValue()) {
                return List.of();
            }

            List<ModificationType> list = Arrays.asList(modificationType, other.modificationType);
            if (!list.contains(ModificationType.ADD) || !list.contains(ModificationType.REPLACE)) {
                return List.of();
            }
        }

        if (value.equals(other.getValue(), strategy)) {
            return getModifications();
        }

        return List.of();
    }

    public Collection<? extends ItemDelta<?, ?>> getModifications() {
        return getModifications(false);
    }

    public Collection<? extends ItemDelta<?, ?>> getModifications(boolean ignoreItself) {
        ITD parent = getParent();
        if (parent == null) {
            throw new SystemException("No parent defined for this value");
        }

        if (ignoreItself) {
            return new ArrayList<>();
        }

        ItemDelta<?, ?> delta = parent.getDefinition().createEmptyDelta(getParent().getPath());
        TreeDeltaUtils.populateItemDelta(delta, this);

        Collection deltas = new ArrayList<>();
        if (!delta.isEmpty()) {
            deltas.add(delta);
        }

        return deltas;
    }

    public void addValueToDelta(ItemDelta<?, ?> delta) {
        TreeDeltaUtils.populateItemDelta(delta, this);
    }
}
