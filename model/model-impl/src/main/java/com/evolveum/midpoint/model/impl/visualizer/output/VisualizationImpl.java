/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.visualizer.output;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

public class VisualizationImpl implements Visualization, DebugDumpable {

    private NameImpl name;
    private ChangeType changeType;
    private final List<VisualizationImpl> partialVisualizations = new ArrayList<>();
    private final List<VisualizationItemImpl> items = new ArrayList<>();
    private final VisualizationImpl owner;
    private boolean operational;
    private ItemPath sourceRelPath;
    private ItemPath sourceAbsPath;
    private PrismContainerValue<?> sourceValue;
    private PrismContainerDefinition<?> sourceDefinition;
    private ObjectDelta<?> sourceDelta;
    private boolean broken;

    public VisualizationImpl(VisualizationImpl owner) {
        this.owner = owner;
    }

    @Override
    public NameImpl getName() {
        return name;
    }

    public void setName(NameImpl name) {
        this.name = name;
    }

    @Override
    public ChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(ChangeType changeType) {
        this.changeType = changeType;
    }

    @NotNull
    @Override
    public List<VisualizationImpl> getPartialVisualizations() {
        return new ArrayList<>(partialVisualizations);
    }

    public void addPartialVisualization(VisualizationImpl visualization) {
        partialVisualizations.add(visualization);
    }

    @NotNull
    @Override
    public List<VisualizationItemImpl> getItems() {
        return items;
    }

    public void addItem(VisualizationItemImpl item) {
        items.add(item);
    }

    @Override
    public VisualizationImpl getOwner() {
        return owner;
    }

    @Override
    public boolean isOperational() {
        return operational;
    }

    public void setOperational(boolean operational) {
        this.operational = operational;
    }

    public ItemPath getSourceRelPath() {
        return sourceRelPath;
    }

    public void setSourceRelPath(ItemPath sourceRelPath) {
        this.sourceRelPath = sourceRelPath;
    }

    @Override
    public ItemPath getSourceAbsPath() {
        return sourceAbsPath;
    }

    public void setSourceAbsPath(ItemPath sourceAbsPath) {
        this.sourceAbsPath = sourceAbsPath;
    }

    @Override
    public PrismContainerValue<?> getSourceValue() {
        return sourceValue;
    }

    public void setSourceValue(PrismContainerValue<?> sourceValue) {
        this.sourceValue = sourceValue;
    }

    @Override
    public PrismContainerDefinition<?> getSourceDefinition() {
        return sourceDefinition;
    }

    public void setSourceDefinition(PrismContainerDefinition<?> sourceDefinition) {
        this.sourceDefinition = sourceDefinition;
    }

    @Override
    public ObjectDelta<?> getSourceDelta() {
        return sourceDelta;
    }

    public void setSourceDelta(ObjectDelta<?> sourceDelta) {
        this.sourceDelta = sourceDelta;
    }

    @Override
    public boolean isBroken() {
        return broken;
    }

    public void setBroken(boolean broken) {
        this.broken = broken;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("Visualization: ");
        if (changeType != null) {
            sb.append(changeType).append(": ");
        }
        if (name != null) {
            sb.append(name.toDebugDump());
        } else {
            sb.append("(unnamed)");
        }
        sb.append(" [rel-path: ").append(sourceRelPath).append("]");
        sb.append(" [abs-path: ").append(sourceAbsPath).append("]");
        if (sourceValue != null) {
            sb.append(" VAL");
        }
        if (sourceDefinition != null) {
            sb.append(" DEF(").append(sourceDefinition.getItemName().getLocalPart()).append("/").append(sourceDefinition.getDisplayName()).append(")");
        }
        if (sourceDelta != null) {
            sb.append(" DELTA");
        }
        if (operational) {
            sb.append(" OPER");
        }
        for (VisualizationItemImpl dataItem : items) {
            sb.append("\n");
            sb.append(dataItem.debugDump(indent + 1));
        }
        for (VisualizationImpl dataContext : partialVisualizations) {
            sb.append("\n");
            sb.append(dataContext.debugDump(indent + 1));
        }
        return sb.toString();
    }

    public String getSourceOid() {
        if (sourceValue != null && sourceValue.getParent() instanceof PrismObject) {
            return ((PrismObject) sourceValue.getParent()).getOid();
        } else {
            return null;
        }
    }

    public boolean isObjectValue() {
        return sourceValue != null && sourceValue.getParent() instanceof PrismObject;
    }

    public boolean isContainerValue() {
        return sourceValue != null && !(sourceValue.getParent() instanceof PrismObject);
    }

    public Long getSourceContainerValueId() {
        if (isContainerValue()) {
            return sourceValue.getId();
        } else {
            return null;
        }
    }

    public boolean isFocusObject() {
        return sourceDefinition != null && sourceDefinition.getCompileTimeClass() != null && FocusType.class.isAssignableFrom(sourceDefinition.getCompileTimeClass());
    }

    public boolean isEmpty() {
        if (changeType != ChangeType.MODIFY) {
            return false;        // ADD or DELETE are never 'empty'
        }
        for (VisualizationItemImpl item : getItems()) {
            if (item.isDescriptive()) {
                continue;
            }
            return false;
        }
        for (VisualizationImpl visualization : getPartialVisualizations()) {
            if (!visualization.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    // owner must not be tested here!
    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}

        VisualizationImpl other = (VisualizationImpl) o;

        if (operational != other.operational) {return false;}
        if (broken != other.broken) {return false;}
        if (!Objects.equals(name, other.name)) {return false;}
        if (changeType != other.changeType) {return false;}
        if (partialVisualizations != null ? !partialVisualizations.equals(other.partialVisualizations) : other.partialVisualizations != null) {
            return false;
        }
        if (items != null ? !items.equals(other.items) : other.items != null) {return false;}
        if (!Objects.equals(sourceRelPath, other.sourceRelPath)) {return false;}
        if (!Objects.equals(sourceAbsPath, other.sourceAbsPath)) {return false;}
        if (!Objects.equals(sourceValue, other.sourceValue)) {return false;}
        if (!Objects.equals(sourceDefinition, other.sourceDefinition)) {return false;}
        return Objects.equals(sourceDelta, other.sourceDelta);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (changeType != null ? changeType.hashCode() : 0);
        result = 31 * result + (partialVisualizations != null ? partialVisualizations.hashCode() : 0);
        result = 31 * result + (items != null ? items.hashCode() : 0);
        result = 31 * result + (operational ? 1 : 0);
        result = 31 * result + (sourceRelPath != null ? sourceRelPath.hashCode() : 0);
        result = 31 * result + (sourceAbsPath != null ? sourceAbsPath.hashCode() : 0);
        result = 31 * result + (sourceValue != null ? sourceValue.hashCode() : 0);
        result = 31 * result + (sourceDefinition != null ? sourceDefinition.hashCode() : 0);
        result = 31 * result + (sourceDelta != null ? sourceDelta.hashCode() : 0);
        result = 31 * result + (broken ? 1 : 0);
        return result;
    }
}
