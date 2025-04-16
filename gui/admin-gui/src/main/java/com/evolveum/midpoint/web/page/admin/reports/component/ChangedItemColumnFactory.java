/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.reports.component;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;

import com.evolveum.midpoint.gui.impl.page.admin.certification.column.AbstractGuiColumn;
import com.evolveum.midpoint.model.common.util.DefaultColumnUtils;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectColumnType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserInterfaceElementVisibilityType;

/**
 * TODO improve later, this doesn't work well with previous implementnation using {@link DefaultColumnUtils}
 */
//@ColumnType(identifier = "changedItemColumn",
//        applicableForType = AuditEventRecordType.class,
//        display = @PanelDisplay(label = "", order = 90))
@SuppressWarnings("unused")
public class ChangedItemColumnFactory extends AbstractGuiColumn<AuditEventRecordType, SelectableBean<AuditEventRecordType>> {

    private AuditLogViewerContext context;

    @SuppressWarnings("unused")
    public ChangedItemColumnFactory(GuiObjectColumnType columnConfig, AuditLogViewerContext context) {
        super(columnConfig);

        this.context = context;
    }

    @Override
    public IColumn<SelectableBean<AuditEventRecordType>, String> createColumn() {
        GuiObjectColumnType guiObjectColumn = getColumnConfig();

        if (!context.isChangedItemSearchItemVisible()
                && (guiObjectColumn != null && guiObjectColumn.getVisibility() != UserInterfaceElementVisibilityType.VISIBLE)) {
            return null;
        }
        return new ChangedItemColumn(null, guiObjectColumn, context.getSearchModel());
    }

    @Override
    public boolean isVisible() {
        if (context == null || !context.isChangedItemSearchItemVisible()) {
            return false;
        }

        GuiObjectColumnType guiObjectColumn = getColumnConfig();
        return guiObjectColumn == null || guiObjectColumn.getVisibility() == UserInterfaceElementVisibilityType.VISIBLE;
    }

    @Override
    public boolean isDefaultColumn() {
        return false;
    }
}
