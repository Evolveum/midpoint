/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.tile.column.ColumnValueProvider;
import com.evolveum.midpoint.web.component.util.SelectableRow;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.List;

/**
 * DTO representing a group of mappings for use in {@code ColumnTileTable}.
 *
 * <p>Provides grouping, selection, and expansion state, and supports both
 * single and multiple mapping values.</p>
 */
public class MappingDataDto implements SelectableRow<MappingDataDto>, Serializable, ColumnValueProvider<PrismContainerValueWrapper<MappingType>> {

    private boolean selected;
    private boolean isExpanded = false;

    private final String groupAttribute;
    private List<PrismContainerValueWrapper<MappingType>> mappings;

    String groupName = "";

    public MappingDataDto(
            @NotNull String groupAttribute,
            @NotNull List<PrismContainerValueWrapper<MappingType>> mappings,String groupName) {
        this.groupAttribute = groupAttribute;
        this.mappings = mappings;
        this.groupName = groupName;
    }

    public @NotNull String getGroupAttribute() {
        return groupAttribute;
    }

    public @NotNull List<PrismContainerValueWrapper<MappingType>> getMappings() {
        return mappings;
    }

    public @Nullable PrismContainerValueWrapper<MappingType> getPrimaryMapping() {
        return mappings.size() != 1 ? null : mappings.get(0);
    }

    public boolean hasMultipleMappings() {
        return mappings.size() > 1;
    }

    public boolean isSingleValue() {
        return mappings.size() == 1;
    }

    public boolean isEmpty() {
        return mappings.isEmpty();
    }

    public int size() {
        return mappings.size();
    }

    @Override
    public PrismContainerValueWrapper<MappingType> getColumnValue() {
        return getPrimaryMapping();
    }

    @Override
    public List<PrismContainerValueWrapper<MappingType>> getColumnsValues() {
        return getMappings();
    }

    @Override
    public String getKeyValue() {
        return getGroupAttribute();
    }

    @Override
    public String getGroupDisplayName() {
        return groupName;
    }

    @Override
    public boolean isSelected() {
        return selected;
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void setMappings(@NotNull List<PrismContainerValueWrapper<MappingType>> mappings) {
        this.mappings = mappings;
    }

    @Override
    public boolean isExpanded() {
        return isExpanded;
    }

    @Override
    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }
}
