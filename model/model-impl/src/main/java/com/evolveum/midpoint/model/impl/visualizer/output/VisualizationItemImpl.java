/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.visualizer.output;

import com.evolveum.midpoint.model.api.visualizer.VisualizationItem;
import com.evolveum.midpoint.model.api.visualizer.Name;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItemValue;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import static org.apache.commons.lang3.Validate.notNull;

public class VisualizationItemImpl implements VisualizationItem, DebugDumpable {

    protected final Name name;
    protected List<VisualizationItemValue> newValues;
    protected boolean operational;
    protected Item<?,?> sourceItem;
    protected ItemPath sourceRelPath;
    protected boolean descriptive;                    // added only as a description of container value being changed

    public VisualizationItemImpl(Name name) {
        notNull(name);
        this.name = name;
        this.newValues = Collections.emptyList();
    }

    @Override
    public Name getName() {
        return name;
    }

    @Override
    public List<VisualizationItemValue> getNewValues() {
        return newValues;
    }

    public void setNewValues(List<VisualizationItemValue> newValues) {
        this.newValues = newValues;
    }

    @Override
    public boolean isOperational() {
        return operational;
    }

    public void setOperational(boolean operational) {
        this.operational = operational;
    }

    public boolean isDescriptive() {
        return descriptive;
    }

    public void setDescriptive(boolean descriptive) {
        this.descriptive = descriptive;
    }

    public void setSourceItem(Item<?, ?> sourceItem) {
        this.sourceItem = sourceItem;
    }

    public ItemPath getSourceRelPath() {
        return sourceRelPath;
    }

    public void setSourceRelPath(ItemPath sourceRelPath) {
        this.sourceRelPath = sourceRelPath;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = debugDumpCommon(indent);
        if (descriptive) {
            sb.append(" DESC");
        }
        sb.append("\n");
        DebugUtil.indentDebugDump(sb, indent+1);
        sb.append("VALUES: ").append(newValues);
        return sb.toString();
    }

    @NotNull
    protected StringBuilder debugDumpCommon(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("Item: ").append(name).append(" [rel-path: ").append(sourceRelPath).append("]");
        if (sourceItem != null) {
            sb.append(" ITEM");
            final ItemDefinition def = sourceItem.getDefinition();
            if (def != null) {
                sb.append(" DEF(").append(def.getItemName().getLocalPart()).append("/").append(def.getDisplayName()).append(":").append(def.getDisplayOrder()).append(")");
            }
        }
        if (operational) {
            sb.append(" OPER");
        }
        return sb;
    }

    public ItemDefinition<?> getSourceDefinition() {
        return sourceItem != null ? sourceItem.getDefinition() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VisualizationItemImpl other = (VisualizationItemImpl) o;

        if (operational != other.operational) return false;
        if (descriptive != other.descriptive) return false;
        if (name != null ? !name.equals(other.name) : other.name != null) return false;
        if (newValues != null ? !newValues.equals(other.newValues) : other.newValues != null) return false;
        if (sourceItem != null ? !sourceItem.equals(other.sourceItem) : other.sourceItem != null) return false;
        return !(sourceRelPath != null ? !sourceRelPath.equals(other.sourceRelPath) : other.sourceRelPath != null);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (newValues != null ? newValues.hashCode() : 0);
        result = 31 * result + (operational ? 1 : 0);
        result = 31 * result + (sourceItem != null ? sourceItem.hashCode() : 0);
        result = 31 * result + (sourceRelPath != null ? sourceRelPath.hashCode() : 0);
        result = 31 * result + (descriptive ? 1 : 0);
        return result;
    }
}
