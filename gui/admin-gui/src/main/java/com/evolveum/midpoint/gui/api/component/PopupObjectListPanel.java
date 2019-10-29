/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

public abstract class PopupObjectListPanel<O extends ObjectType> extends ObjectListPanel<O> {
    private static final long serialVersionUID = 1L;

    /**
     * @param defaultType specifies type of the object that will be selected by default
     */
    public PopupObjectListPanel(String id, Class<? extends O> defaultType, boolean multiselect, PageBase parentPage) {
        super(id, defaultType, null, multiselect);

    }

    public PopupObjectListPanel(String id, Class<? extends O> defaultType, Collection<SelectorOptions<GetOperationOptions>> options,
                                boolean multiselect, PageBase parentPage) {
        super(id, defaultType, null, options, multiselect);
    }

    @Override
    protected IColumn<SelectableBean<O>, String> createCheckboxColumn() {
        if (isMultiselect()) {
            return new CheckBoxHeaderColumn<SelectableBean<O>>() {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdateRow(AjaxRequestTarget target, DataTable table, IModel<SelectableBean<O>> rowModel, IModel<Boolean> selected) {
                    super.onUpdateRow(target, table, rowModel, selected);
                    onUpdateCheckbox(target, rowModel);
                };

                @Override
                protected void onUpdateHeader(AjaxRequestTarget target, boolean selected, DataTable table) {
                    super.onUpdateHeader(target, selected, table);
                    onUpdateCheckbox(target, null);
                }

                @Override
                protected IModel<Boolean> getEnabled(IModel<SelectableBean<O>> rowModel) {
                        return PopupObjectListPanel.this.getCheckBoxEnableModel(rowModel);
                }
            };
        }
        return null;
    }

    @Override
    protected IColumn<SelectableBean<O>, String> createNameColumn(IModel<String> columnNameModel, String itemPath) {
        if (!isMultiselect()) {
            return new LinkColumn<SelectableBean<O>>(
                    columnNameModel == null ? createStringResource("ObjectType.name") : columnNameModel,
                    StringUtils.isEmpty(itemPath) ? ObjectType.F_NAME.getLocalPart() : itemPath,
                    SelectableBeanImpl.F_VALUE + "." +
                            (StringUtils.isEmpty(itemPath) ? "name" : itemPath)) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target, IModel<SelectableBean<O>> rowModel) {
                    O object = rowModel.getObject().getValue();
                    onSelectPerformed(target, object);

                }
            };
        }

        else {
            return new PropertyColumn(
                    columnNameModel == null ? createStringResource("userBrowserDialog.name") : columnNameModel,
                    StringUtils.isEmpty(itemPath) ? ObjectType.F_NAME.getLocalPart() : itemPath,
                    SelectableBeanImpl.F_VALUE + "." +
                            (StringUtils.isEmpty(itemPath) ? "name" : itemPath));
        }
    }

    @Override
    protected List<IColumn<SelectableBean<O>, String>> createColumns() {
        return ColumnUtils.getDefaultColumns(getType());
    }

    protected void onSelectPerformed(AjaxRequestTarget target, O object){

    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        return null;
    }

    @Override
    protected void addCustomActions(@NotNull List<InlineMenuItem> actionsList, SerializableSupplier<Collection<? extends ObjectType>> objectsSupplier) {
    }


    protected void onUpdateCheckbox(AjaxRequestTarget target, IModel<SelectableBean<O>> rowModel){

    }

    protected IModel<Boolean> getCheckBoxEnableModel(IModel<SelectableBean<O>> rowModel){
        return Model.of(true);
    }
}
