/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.visualizer.output;

import com.evolveum.midpoint.model.api.visualizer.Name;
import com.evolveum.midpoint.model.api.visualizer.VisualizationDeltaItem;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItemValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class VisualizationDeltaItemImpl extends VisualizationItemImpl implements VisualizationDeltaItem, DebugDumpable {

    @NotNull private List<VisualizationItemValue> oldValues = Collections.emptyList();
    @NotNull private List<VisualizationItemValue> addedValues = Collections.emptyList();
    @NotNull private List<VisualizationItemValue> deletedValues = Collections.emptyList();
    @NotNull private List<VisualizationItemValue> unchangedValues = Collections.emptyList();
    private ItemDelta<?,?> sourceDelta;

    public VisualizationDeltaItemImpl(Name name) {
        super(name);
    }

    @NotNull
    @Override
    public List<VisualizationItemValue> getOldValues() {
        return oldValues;
    }

    public void setOldValues(@NotNull List<VisualizationItemValue> oldValues) {
        this.oldValues = oldValues;
    }

    @NotNull
    @Override
    public List<VisualizationItemValue> getAddedValues() {
        return addedValues;
    }

    public void setAddedValues(@NotNull List<VisualizationItemValue> addedValues) {
        this.addedValues = addedValues;
    }

    @NotNull
    public List<VisualizationItemValue> getDeletedValues() {
        return deletedValues;
    }

    public void setDeletedValues(@NotNull List<VisualizationItemValue> deletedValues) {
        this.deletedValues = deletedValues;
    }

    @NotNull
    public List<VisualizationItemValue> getUnchangedValues() {
        return unchangedValues;
    }

    public void setUnchangedValues(@NotNull List<VisualizationItemValue> unchangedValues) {
        this.unchangedValues = unchangedValues;
    }

    @Override
    public ItemDelta<?, ?> getSourceDelta() {
        return sourceDelta;
    }

    public void setSourceDelta(ItemDelta<?, ?> sourceDelta) {
        this.sourceDelta = sourceDelta;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = debugDumpCommon(indent);
        if (sourceDelta != null) {
            sb.append(" DELTA");
        }
        sb.append("\n");
        DebugUtil.indentDebugDump(sb, indent+1);
        sb.append("OLD: ").append(oldValues).append("\n");
        DebugUtil.indentDebugDump(sb, indent+1);
        sb.append("NEW: ").append(newValues).append("\n");
        DebugUtil.indentDebugDump(sb, indent+1);
        sb.append("ADDED: ").append(addedValues).append("\n");
        DebugUtil.indentDebugDump(sb, indent+1);
        sb.append("DELETED: ").append(deletedValues).append("\n");
        DebugUtil.indentDebugDump(sb, indent+1);
        sb.append("UNCHANGED: ").append(unchangedValues);
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        VisualizationDeltaItemImpl that = (VisualizationDeltaItemImpl) o;

        if (!oldValues.equals(that.oldValues)) return false;
        if (!addedValues.equals(that.addedValues)) return false;
        if (!deletedValues.equals(that.deletedValues)) return false;
        if (!unchangedValues.equals(that.unchangedValues)) return false;
        return !(sourceDelta != null ? !sourceDelta.equals(that.sourceDelta) : that.sourceDelta != null);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + oldValues.hashCode();
        result = 31 * result + addedValues.hashCode();
        result = 31 * result + deletedValues.hashCode();
        result = 31 * result + unchangedValues.hashCode();
        result = 31 * result + (sourceDelta != null ? sourceDelta.hashCode() : 0);
        return result;
    }
}
