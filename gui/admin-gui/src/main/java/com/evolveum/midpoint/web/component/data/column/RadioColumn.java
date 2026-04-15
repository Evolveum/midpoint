/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.data.column;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * A column that renders a single-select radio per row. Works with a parent RadioGroup<T>
 * wrapping the DataTable. The group model holds the currently selected row object.
 * Usage:
 * RadioGroup<T> group = new RadioGroup<>("selection", selectionModel);
 * group.add(dataTable); // DataTable must be inside the group
 * columns.add(new RadioColumn<>(Model.of(""), selectionModel));
 * The column supports:
 * - enabling/disabling radios via setEnabled(boolean)*
 */
public class RadioColumn<T extends Serializable> extends AbstractColumn<T, String> {

    /** Model that stores the currently selected row object in the enclosing RadioGroup. */
    private final IModel<T> selectionModel;

    /** Whether radios are enabled. */
    private final IModel<Boolean> enabled = new Model<>(true);

    public RadioColumn(IModel<String> displayModel, IModel<T> selectionModel) {
        super(displayModel);
        this.selectionModel = selectionModel;
    }

    @Override
    public void populateItem(final @NotNull Item<ICellPopulator<T>> cellItem,
            @NotNull String componentId,
            final @NotNull IModel<T> rowModel) {
        // The Radio must be a descendant of a RadioGroup<T> in the component hierarchy.
        // The group should own the same selectionModel passed to this column.
        IsolatedRadioPanel<T> radio = new IsolatedRadioPanel<>(componentId, rowModel, getEnabled());
        radio.setOutputMarkupId(true);
        cellItem.add(radio);
    }

    /** Per-row enabled model if you need dynamic enabling; override if needed. */
    protected IModel<Boolean> getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled.setObject(enabled);
    }

    @Override
    public String getCssClass() {
        IModel<String> display = getDisplayModel();
        if (display != null && StringUtils.isNotEmpty(display.getObject())) {
            return "align-middle";
        }
        return "icon align-middle";
    }

    @Override
    public Component getHeader(String componentId) {
        return new WebMarkupContainer(componentId);
    }

    /** Convenience: ensures the radio lives under a RadioGroup<T>. */
    @Override
    public void detach() {
        super.detach();
        if (selectionModel != null) {
            selectionModel.detach();
        }
    }
}
