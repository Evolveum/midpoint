/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.prism.show;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.visualizer.Name;
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItem;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.LocalizableMessage;

/**
 * Artificial implementation of a visualization used to hold a list of deltas.
 * (A bit of hack, unfortunately.)
 */
public class WrapperVisualization implements Visualization {

    private LocalizableMessage displayName;

    private List<Visualization> partialVisualizations;

    public WrapperVisualization(LocalizableMessage displayName, List<Visualization> partialVisualizations) {
        this.displayName = displayName;
        this.partialVisualizations = partialVisualizations;
    }

    @Override
    public Name getName() {
        return new Name() {

            @Override
            public LocalizableMessage getSimpleName() {
                return null;
            }

            @Override
            public LocalizableMessage getDisplayName() {
                return displayName;
            }

            @Override
            public String getId() {
                return null;
            }

            @Override
            public LocalizableMessage getDescription() {
                return null;
            }

            @Override
            public LocalizableMessage getOverview() {
                return null;
            }
        };
    }

    @Override
    public ChangeType getChangeType() {
        return null;
    }

    @NotNull
    @Override
    public List<Visualization> getPartialVisualizations() {
        return partialVisualizations;
    }

    @NotNull
    @Override
    public List<VisualizationItem> getItems() {
        return Collections.emptyList();
    }

    @Override
    public boolean isOperational() {
        return false;
    }

    @Override
    public Visualization getOwner() {
        return null;
    }

    @Override
    public ItemPath getSourceRelPath() {
        return null;
    }

    @Override
    public ItemPath getSourceAbsPath() {
        return null;
    }

    @Override
    public PrismContainerValue<?> getSourceValue() {
        return null;
    }

    @Override
    public PrismContainerDefinition<?> getSourceDefinition() {
        return null;
    }

    @Override
    public ObjectDelta<?> getSourceDelta() {
        return null;
    }

    @Override
    public boolean isBroken() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        if (partialVisualizations == null) {
            return true;
        }
        for (Visualization visualization : partialVisualizations) {
            if (!visualization.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        return DebugUtil.debugDump(partialVisualizations, indent);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}

        WrapperVisualization that = (WrapperVisualization) o;

        if (displayName != null ? !displayName.equals(that.displayName) : that.displayName != null) {return false;}
        return !(partialVisualizations != null ? !partialVisualizations.equals(that.partialVisualizations) : that.partialVisualizations != null);

    }

    @Override
    public int hashCode() {
        int result = displayName != null ? displayName.hashCode() : 0;
        result = 31 * result + (partialVisualizations != null ? partialVisualizations.hashCode() : 0);
        return result;
    }
}
