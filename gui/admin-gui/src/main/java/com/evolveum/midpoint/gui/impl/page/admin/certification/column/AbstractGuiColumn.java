/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.column;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.application.ColumnType;
import com.evolveum.midpoint.web.component.util.SelectableRow;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectColumnType;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;

import java.io.Serial;
import java.io.Serializable;

public abstract class AbstractGuiColumn<C extends Containerable, S extends SelectableRow<C>> implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    private final GuiObjectColumnType columnConfig;

    public AbstractGuiColumn(GuiObjectColumnType columnConfig) {
        this.columnConfig = columnConfig;
    }

    public abstract IColumn<S, String> createColumn();

    public IModel<String> getColumnLabelModel() {
        return () -> {
            DisplayType display = columnConfig != null ? columnConfig.getDisplay() : null;
            PolyStringType label = display != null ? display.getLabel() : null;
            String labelTranslated = label != null ? LocalizationUtil.translatePolyString(label) : null;
            if (StringUtils.isEmpty(labelTranslated)) {
                ColumnType columnType = AbstractGuiColumn.this.getClass().getAnnotation(ColumnType.class);
                labelTranslated = LocalizationUtil.translate(columnType.display().label());
            }
            return labelTranslated;
        };
    }

    protected boolean isVisible() {
        return columnConfig == null || WebComponentUtil.getElementVisibility(columnConfig.getVisibility());
    }

    protected GuiObjectColumnType getColumnConfig() {
        return columnConfig;
    }

    public String getIdentifier() {
        ColumnType columnType = AbstractGuiColumn.this.getClass().getAnnotation(ColumnType.class);
        return columnType.identifier();
    }
}
