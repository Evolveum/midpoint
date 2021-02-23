/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public abstract class PopupObjectListPanel<O extends ObjectType> extends ObjectListPanel<O> {
    private static final long serialVersionUID = 1L;

    private boolean multiselect;

    /**
     * @param defaultType specifies type of the object that will be selected by default
     */
    public PopupObjectListPanel(String id, Class<O> defaultType, boolean multiselect) {
        this(id, defaultType, null, multiselect);
        this.multiselect = multiselect;
    }

    public PopupObjectListPanel(String id, Class<O> defaultType, Collection<SelectorOptions<GetOperationOptions>> options,
                                boolean multiselect) {
        super(id, defaultType, options);
        this.multiselect = multiselect;
    }

    @Override
    protected IColumn<SelectableBean<O>, String> createCheckboxColumn() {
        if (isMultiselect()) {
            return new CheckBoxHeaderColumn<>() {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdateRow(AjaxRequestTarget target, DataTable table, IModel<SelectableBean<O>> rowModel, IModel<Boolean> selected) {
                    super.onUpdateRow(target, table, rowModel, selected);
                    onUpdateCheckbox(target, rowModel);
                }

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
    protected void objectDetailsPerformed(AjaxRequestTarget target, O object) {
        onSelectPerformed(target, object);
    }

    @Override
    protected boolean isObjectDetailsEnabled(IModel<SelectableBean<O>> rowModel) {
        return !isMultiselect();
    }


    @Override
    protected List<IColumn<SelectableBean<O>, String>> createDefaultColumns() {
        return new ArrayList<>(ColumnUtils.getDefaultColumns(getType(), getPageBase()));
    }

    protected void onSelectPerformed(AjaxRequestTarget target, O object) {

    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        return null;
    }

    @Override
    protected void addCustomActions(@NotNull List<InlineMenuItem> actionsList, SerializableSupplier<Collection<? extends O>> objectsSupplier) {
    }

    protected void onUpdateCheckbox(AjaxRequestTarget target, IModel<SelectableBean<O>> rowModel) {

    }

    protected IModel<Boolean> getCheckBoxEnableModel(IModel<SelectableBean<O>> rowModel) {
        return Model.of(true);
    }

    protected String getStorageKey() {
        return null;
    }

    public boolean isMultiselect() {
        return multiselect;
    }

    @Override
    protected boolean enableSavePageSize() {
        return false;
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return null;
    }
}
