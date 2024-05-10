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
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

public abstract class ItemTreeDeltaValue<PV extends PrismValue, ITD extends ItemTreeDelta> implements DebugDumpable, Visitable {

    private ITD parent;

    // todo improve
    private List<Object> naturalKey;

    private PV value;

    private ModificationType modificationType;

    public ItemTreeDeltaValue(PV value, ModificationType modificationType) {
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

        debugDumpTitle(sb, indent);
        debugDumpContent(sb, indent);

        return sb.toString();
    }

    protected String debugDumpShortName() {
        return getClass().getSimpleName();
    }

    protected void debugDumpTitle(StringBuilder sb, int indent) {
        // todo fix debug dump ,this is a mess
        sb.append(DebugUtil.debugDump(debugDumpShortName(), indent - 1)).append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "modification", modificationType, indent);
    }

    protected void debugDumpContent(StringBuilder sb, int indent) {
        // todo fix debug dump ,this is a mess
        DebugUtil.debugDumpWithLabelLn(sb, "naturalKey", naturalKey, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "value", value, indent + 1);
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

            List<ModificationType> list = List.of(modificationType, other.modificationType);
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

//    // todo fix generics
//    public boolean hasConflictWith(ItemTreeDeltaValue other) {
//        if (other == null) {
//            return false;
//        }
//
//        if (modificationType == null && other.modificationType == null) {
//            return false;
//        }
//
//        if (modificationType != other.modificationType) {
//            if (!getParent().getDefinition().isSingleValue()) {
//                return true;
//            }
//
//            List<ModificationType> list = List.of(modificationType, other.modificationType);
//            if (!list.contains(ModificationType.ADD) || !list.contains(ModificationType.REPLACE)) {
//                return true;
//            }
//        }
//
//        return hasConflictWith(value);
//    }

    protected boolean hasConflictWith(PV otherValue) {
        if (value == null || otherValue == null) {
            return false;
        }

        return !value.equals(otherValue, EquivalenceStrategy.REAL_VALUE);   // todo add equivalence strategy
    }

    public <V extends ItemTreeDeltaValue> boolean match(V other) {
        if (other == null) {
            return false;
        }

        if (value == null || other.getValue() == null) {
            return false;
        }

        return value.equals(other.getValue(), EquivalenceStrategy.REAL_VALUE);
    }

    public boolean containsModifications() {
        return modificationType != null;
    }
}
