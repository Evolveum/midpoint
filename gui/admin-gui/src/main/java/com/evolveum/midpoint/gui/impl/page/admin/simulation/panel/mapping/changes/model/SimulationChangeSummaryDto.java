package com.evolveum.midpoint.gui.impl.page.admin.simulation.panel.mapping.changes.model;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItemValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.prism.show.VisualizationDto;
import com.evolveum.midpoint.web.component.prism.show.VisualizationItemDto;
import com.evolveum.midpoint.web.component.prism.show.VisualizationItemLineDto;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * UI summary of value changes for one simulated object.
 *
 * <p>Transforms {@link VisualizationDto} into a simplified structure used by
 * simulation result panels. It resolves the object state, extracts display
 * values, groups them into removed/added/unchanged collections, and determines
 * the appropriate {@link ChangeViewType} for rendering.</p>
 */
public class SimulationChangeSummaryDto implements Serializable {

    private final String objectName;
    private String objectOid;
    private String imageCssClass;

    public enum SimulationItemState {
        ADDED, MODIFIED, DELETED, FAILED, NOT_APPLIED, SKIPPED
    }

    public enum ChangeViewType {
        SIMPLE,
        EMPTY,
        ADD_ONLY,
        DELETE_ONLY,
        MULTI_VALUE
    }

    private final SimulationItemState state;
    private final ItemPath targetItem;
    private ChangeViewType viewType = ChangeViewType.EMPTY;

    private String primaryChangeLabel;
    private String oldValue;
    private String newValue;

    private final List<String> removedValues = new ArrayList<>();
    private final List<String> addedValues = new ArrayList<>();
    private final List<String> unchangedValues = new ArrayList<>();

    private String message;
    private boolean expandable;

    public SimulationChangeSummaryDto(@NotNull VisualizationDto dto, ItemName targetItem) {
        this.objectName = dto.getName();
        this.state = resolveState(dto);
        this.targetItem = targetItem;

        List<VisualizationItemDto> filteredItems = new ArrayList<>();
        filterItems(dto, filteredItems);

        if (filteredItems.isEmpty()) {
            determineViewType();
            this.expandable = false;
            return;
        }

        VisualizationItemDto firstItem = filteredItems.get(0);
        this.primaryChangeLabel = firstItem.getName();

        for (VisualizationItemDto item : filteredItems) {
            List<VisualizationItemLineDto> lines = item.getLines();
            if (lines.isEmpty()) {
                continue;
            }

            for (VisualizationItemLineDto line : lines) {
                String oldVal = toDisplayValue(line.getOldValue());
                String newVal = toDisplayValue(line.getNewValue());

                if (line.isDelta()) {
                    if (oldVal != null && newVal != null) {
                        if (this.oldValue == null) {
                            this.oldValue = oldVal;
                        }
                        if (this.newValue == null) {
                            this.newValue = newVal;
                        }

                        removedValues.add(oldVal);
                        addedValues.add(newVal);

                    } else if (oldVal != null) {
                        if (this.oldValue == null) {
                            this.oldValue = oldVal;
                        }
                        removedValues.add(oldVal);

                    } else if (newVal != null) {
                        if (this.newValue == null) {
                            this.newValue = newVal;
                        }
                        addedValues.add(newVal);
                    }
                } else if (newVal != null) {
                    unchangedValues.add(newVal);
                }
            }
        }

        determineViewType();
        this.expandable = removedValues.size() + addedValues.size() + unchangedValues.size() > 3;
    }

    /**
     * Recursively collect visualization items which matches the target path.
     */
    private void filterItems(@NotNull VisualizationDto dto, @NotNull List<VisualizationItemDto> itemsCollector) {
        List<VisualizationItemDto> items = dto.getItems();
        if (items != null) {
            items.forEach(item -> {
                if (item.getItemRelativePath().endsWith(this.targetItem)) {
                    itemsCollector.add(item);
                }
            });
        }

        // Recursively process sub visualizations (e.g. attributes container in shadows)
        for (VisualizationDto partial : dto.getPartialVisualizations()) {
            filterItems(partial, itemsCollector);
        }
    }

    private SimulationItemState resolveState(@NotNull VisualizationDto dto) {
        if (dto.getVisualization().getChangeType() != null) {
            return switch (dto.getVisualization().getChangeType()) {
                case ADD -> SimulationItemState.ADDED;
                case DELETE -> SimulationItemState.DELETED;
                default -> SimulationItemState.MODIFIED;
            };
        }
        return SimulationItemState.MODIFIED;
    }

    private void determineViewType() {
        if (removedValues.isEmpty() && addedValues.isEmpty() && unchangedValues.isEmpty()) {
            viewType = ChangeViewType.EMPTY;
            return;
        }

        if (removedValues.size() == 1 && addedValues.size() == 1 && unchangedValues.isEmpty()) {
            viewType = ChangeViewType.SIMPLE;
            return;
        }

        if (removedValues.isEmpty() && !addedValues.isEmpty() && unchangedValues.isEmpty()) {
            viewType = ChangeViewType.ADD_ONLY;
            return;
        }

        if (addedValues.isEmpty() && !removedValues.isEmpty() && unchangedValues.isEmpty()) {
            viewType = ChangeViewType.DELETE_ONLY;
            return;
        }

        viewType = ChangeViewType.MULTI_VALUE;
    }

    private String toDisplayValue(VisualizationItemValue value) {
        if (value == null) {
            return null;
        }

        if (value.getText() != null) {
            return LocalizationUtil.translateMessage(value.getText());
        }

        if (value.getAdditionalText() != null) {
            return LocalizationUtil.translateMessage(value.getAdditionalText());
        }

        if (value.getSourceValue() != null) {
            Object realValue = value.getSourceValue().getRealValue();
            return realValue != null ? String.valueOf(realValue) : null;
        }

        return null;
    }

    public boolean hasMessage() {
        return message != null && !message.isBlank();
    }

    public List<String> getTopAddedValues() {
        return addedValues.stream().limit(3).toList();
    }

    public int getMoreAddedCount() {
        return Math.max(0, addedValues.size() - 3);
    }

    public List<String> getTopRemovedValues() {
        return removedValues.stream().limit(3).toList();
    }

    public int getMoreRemovedCount() {
        return Math.max(0, removedValues.size() - 3);
    }

    public List<String> getTopUnchangedValues() {
        return unchangedValues.stream().limit(3).toList();
    }

    public int getMoreUnchangedCount() {
        return Math.max(0, unchangedValues.size() - 3);
    }

    public int getUnchangedCount() {
        return unchangedValues.size();
    }

    public String getObjectName() {
        return objectName;
    }

    public String getObjectOid() {
        return objectOid;
    }

    public void setObjectOid(String objectOid) {
        this.objectOid = objectOid;
    }

    public String getImageCssClass() {
        return imageCssClass;
    }

    public void setImageCssClass(String imageCssClass) {
        this.imageCssClass = imageCssClass;
    }

    public SimulationItemState getState() {
        return state;
    }

    public ChangeViewType getViewType() {
        return viewType;
    }

    public String getPrimaryChangeLabel() {
        return primaryChangeLabel;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public List<String> getRemovedValues() {
        return removedValues;
    }

    public List<String> getAddedValues() {
        return addedValues;
    }

    public List<String> getUnchangedValues() {
        return unchangedValues;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isExpandable() {
        return expandable;
    }

    public void setExpandable(boolean expandable) {
        this.expandable = expandable;
    }
}
