/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.prism.show;

import java.io.Serializable;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.prism.polystring.PolyString;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.visualizer.Name;
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.model.api.visualizer.VisualizationDeltaItem;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItem;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.LocalizableMessage;

public class VisualizationDto implements Serializable {

    @NotNull private final Visualization visualization;
    private boolean minimized;
    private boolean sorted = false;

    private String boxClassOverride;

    private PolyString nameOverwrite;

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

    public void setNameOverwrite(PolyString nameOverwrite) {
        this.nameOverwrite = nameOverwrite;
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

    public String getName() {
        if (nameOverwrite != null) {
            return LocalizationUtil.translatePolyString(nameOverwrite);
        }

        Name nameObject = visualization.getName();
        if (nameObject == null) {
            return LocalizationUtil.translate("SceneDto.unnamed");
        }

        LocalizableMessage displayName = nameObject.getDisplayName();
        if (displayName == null) {
            return LocalizationUtil.translateMessage(nameObject.getSimpleName());
        }

        String name = LocalizationUtil.translateMessage(displayName);
        ItemPath path = visualization.getSourceAbsPath();
        if (path != null && path.size() > 1) {
            name = name + " (" + path + ")";
        }

        return name;
    }

    public String getDescription() {
        Name name = visualization.getName();
        if (name == null) {
            return "";
        }
        if (visualization.getSourceDefinition() != null && !(visualization.getSourceDefinition() instanceof PrismObjectDefinition)) {
            return "";
        }

        String simpleName = LocalizationUtil.translateMessage(name.getSimpleName());
        if (simpleName != null && !Objects.equals(simpleName, getName())) {
            return "(" + simpleName + ")";
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

    public boolean hasNonOperationalContent() {
        if (getVisualization().isOperational()){
            return false;
        }

        if (hasNonOperationalItems()) {
            return true;
        }

        if (getPartialVisualizations().stream().anyMatch(v -> v.hasNonOperationalContent())) {
            return true;
        }

        return false;
    }

    public boolean hasOperationalContent() {
        if (getVisualization().isOperational()) {
            return true;
        }

        if (hasOperationalItems()) {
            return true;
        }

        if (getPartialVisualizations().stream().anyMatch(v -> v.hasOperationalContent())) {
            return true;
        }

        return false;
    }

    public boolean hasNonOperationalItems() {
        return getItems().stream().anyMatch(i -> !i.isOperational());
    }

    public boolean hasOperationalItems() {
        return items.stream().anyMatch(VisualizationItemDto::isOperational);
    }

    /**
     * minimized is NOT included in equality check - because the VisualizationDto's are compared in order to determine
     * whether they should be redrawn (i.e. their content is important, not the presentation)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}

        VisualizationDto that = (VisualizationDto) o;

        if (!visualization.equals(that.visualization)) {return false;}
        if (!Objects.equals(boxClassOverride, that.boxClassOverride)) {return false;}
        if (items != null ? !items.equals(that.items) : that.items != null) {return false;}
        return partialVisualizations != null ? partialVisualizations.equals(that.partialVisualizations) : that.partialVisualizations == null;
    }

    @Override
    public int hashCode() {
        int result = visualization.hashCode();
        result = 31 * result + (boxClassOverride != null ? boxClassOverride.hashCode() : 0);
        result = 31 * result + (items != null ? items.hashCode() : 0);
        result = 31 * result + (partialVisualizations != null ? partialVisualizations.hashCode() : 0);
        return result;
    }
}
