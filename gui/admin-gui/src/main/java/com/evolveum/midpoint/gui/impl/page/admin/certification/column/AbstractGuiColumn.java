/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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

    public boolean isVisible() {
        return columnConfig == null || WebComponentUtil.getElementVisibility(columnConfig.getVisibility());
    }

    protected GuiObjectColumnType getColumnConfig() {
        return columnConfig;
    }

    public String getIdentifier() {
        ColumnType columnType = AbstractGuiColumn.this.getClass().getAnnotation(ColumnType.class);
        return columnType.identifier();
    }

    public int getOrder() {
        ColumnType columnType = AbstractGuiColumn.this.getClass().getAnnotation(ColumnType.class);
        return columnType.display() != null ? columnType.display().order() : 0;
    }

    /**
     * method is intended to differentiate between default view columns and custom columns
     * @return
     */
    public boolean isDefaultColumn() {
        return true;
    }
}
