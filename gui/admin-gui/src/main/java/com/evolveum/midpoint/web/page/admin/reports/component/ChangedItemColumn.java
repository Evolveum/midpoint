/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.reports.component;

import com.evolveum.midpoint.gui.impl.component.search.SearchValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.ItemPathTypeUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayValueType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.data.column.ConfigurableExpressionColumn;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.PropertySearchItemWrapper;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectColumnType;

public class ChangedItemColumn extends ConfigurableExpressionColumn<SelectableBean<AuditEventRecordType>, AuditEventRecordType> {

    private final IModel<Search<AuditEventRecordType>> searchModel;

    public ChangedItemColumn(
            IModel<String> displayModel,
            GuiObjectColumnType customColumns,
            ExpressionType expressionType,
            IModel<Search<AuditEventRecordType>> searchModel,
            PageBase modelServiceLocator) {

        super(displayModel, null, customColumns, expressionType, modelServiceLocator);

        this.searchModel = searchModel;
    }

    private ItemPath getPath() {
        if (searchModel.getObject() == null) {
            return null;
        }

        // noinspection unchecked
        PropertySearchItemWrapper<ItemPathType> wrapper = searchModel.getObject()
                .findPropertySearchItem(AuditEventRecordType.F_CHANGED_ITEM);
        if (wrapper == null) {
            return null;
        }

        DisplayableValue<ItemPathType> value = wrapper.getValue();
        ItemPathType itemPathType = value.getValue();
        return itemPathType != null ? itemPathType.getItemPath() : null;
    }

    @SuppressWarnings("unused")
    @Override
    public <V> String loadExportableColumnDataModel(
            IModel<SelectableBean<AuditEventRecordType>> rowModel,
            GuiObjectColumnType customColumn,
            ItemPath columnPath,
            ExpressionType expression) {

        // todo show old/new from delta using getPath()
        if (1==1) {
            return getPath() != null ? getPath().toString(): "full delta";
        }

        return super.loadExportableColumnDataModel(rowModel, customColumn, AuditEventRecordType.F_DELTA, expression);
    }
}
