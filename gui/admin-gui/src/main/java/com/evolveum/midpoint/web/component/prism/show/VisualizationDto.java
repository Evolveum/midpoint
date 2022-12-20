/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.prism.show;

import java.io.Serializable;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.wicket.Component;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.visualizer.Name;
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.model.api.visualizer.VisualizationDeltaItem;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItem;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ChangeType;

public class VisualizationDto implements Serializable {

    public static final String F_CHANGE_TYPE = "changeType";
    public static final String F_OBJECT_TYPE = "objectType";
    public static final String F_DESCRIPTION = "description";
    public static final String F_ITEMS = "items";
    public static final String F_PARTIAL_VISUALIZATIONS = "partialVisualizations";
    public static final String F_SORTED = "sorted";

    @NotNull private final Visualization visualization;
    private boolean minimized;
    private boolean sorted = false;

    private String boxClassOverride;

    private final List<VisualizationItemDto> items = new ArrayList<>();
    private final List<VisualizationDto> partialVisualizations = new ArrayList<>();

    public VisualizationDto(@NotNull Visualization visualization) {
        this.visualization = visualization;
        for (VisualizationItem item : visualization.getItems()) {
            if (item != null) {
                items.add(new VisualizationItemDto(this, item));
            }
        }
        for (Visualization sub : visualization.getPartialVisualizations()) {
            if (sub != null) {
                partialVisualizations.add(new VisualizationDto(sub));
            }
        }
    }

    public @NotNull Visualization getVisualization() {
        return visualization;
    }

    public boolean isMinimized() {
        return minimized;
    }

    public void setMinimized(boolean minimized) {
        this.minimized = minimized;
    }

    public List<VisualizationDto> getPartialVisualizations() {
        return partialVisualizations;
    }

    public List<VisualizationItemDto> getItems() {
        if (isSorted()) {
            List<VisualizationItemDto> itemsClone = new ArrayList<>(items);
            Collator collator = WebComponentUtil.getCollator();
            Comparator<? super VisualizationItemDto> comparator =
                    (s1, s2) -> {
                        String name1 = PageBase.createStringResourceStatic(s1.getName()).getString();
                        String name2 = PageBase.createStringResourceStatic(s2.getName()).getString();
                        return collator.compare(name1, name2);
                    };
            itemsClone.sort(comparator);
            return itemsClone;
        }
        return items;
    }

    public String getName(Component component) {
        if (visualization.getName() != null) {
            if (visualization.getName().getDisplayName() != null) {
                String name = resolve(visualization.getName().getDisplayName(), component, visualization.getName().namesAreResourceKeys());
                if (visualization.getSourceAbsPath() != null && visualization.getSourceAbsPath().size() > 1) {
                    name = name + " (" + visualization.getSourceAbsPath().toString() + ")";
                }
                return name;
            } else {
                return resolve(visualization.getName().getSimpleName(), component, visualization.getName().namesAreResourceKeys());
            }
        } else {
            return resolve("SceneDto.unnamed", component, true);
        }
    }

    private String resolve(String name, Component component, boolean namesAreResourceKeys) {
        if (namesAreResourceKeys) {
            return PageBase.createStringResourceStatic(name).getString();
        } else {
            return name;
        }
    }

    public String getDescription(Component component) {
        Name name = visualization.getName();
        if (name == null) {
            return "";
        }
        if (visualization.getSourceDefinition() != null && !(visualization.getSourceDefinition() instanceof PrismObjectDefinition)) {
            return "";
        }
        if (name.getSimpleName() != null && !name.getSimpleName().equals(getName(component))) {
            return "(" + name.getSimpleName() + ")";
        }
        return "";
    }

    public ChangeType getChangeType() {
        return visualization.getChangeType();
    }

    public boolean containsDeltaItems() {
        for (VisualizationItem item : visualization.getItems()) {
            if (item instanceof VisualizationDeltaItem) {
                return true;
            }
        }
        return false;
    }

    public boolean isWrapper() {
        return visualization instanceof WrapperVisualization;
    }

    public String getBoxClassOverride() {
        return boxClassOverride;
    }

    public void setBoxClassOverride(String boxClassOverride) {
        this.boxClassOverride = boxClassOverride;
    }

    public boolean isSorted() {
        return sorted;
    }

    public void setSorted(boolean sorted) {
        this.sorted = sorted;
    }

    // minimized is NOT included in equality check - because the VisualizationDto's are compared in order to determine
    // whether they should be redrawn (i.e. their content is important, not the presentation)

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}

        VisualizationDto other = (VisualizationDto) o;

        if (visualization != null ? !visualization.equals(other.visualization) : other.visualization != null) {
            return false;
        }
        if (boxClassOverride != null ? !boxClassOverride.equals(other.boxClassOverride) : other.boxClassOverride != null) {
            return false;
        }
        if (items != null ? !items.equals(other.items) : other.items != null) {return false;}
        return !(partialVisualizations != null ? !partialVisualizations.equals(other.partialVisualizations) : other.partialVisualizations != null);

    }

    @Override
    public int hashCode() {
        int result = visualization != null ? visualization.hashCode() : 0;
        result = 31 * result + (boxClassOverride != null ? boxClassOverride.hashCode() : 0);
        result = 31 * result + (items != null ? items.hashCode() : 0);
        result = 31 * result + (partialVisualizations != null ? partialVisualizations.hashCode() : 0);
        return result;
    }

    public void applyFoldingFrom(@NotNull VisualizationDto source) {
        minimized = source.minimized;
        int partialDst = partialVisualizations.size();
        int partialSrc = source.getPartialVisualizations().size();
        if (partialDst != partialSrc) {
            return;    // shouldn't occur
        }
        for (int i = 0; i < partialDst; i++) {
            partialVisualizations.get(i).applyFoldingFrom(source.getPartialVisualizations().get(i));
        }
    }
}
