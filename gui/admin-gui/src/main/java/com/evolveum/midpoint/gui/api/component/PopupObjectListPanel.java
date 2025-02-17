/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.impl.util.TableUtil;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;

import com.evolveum.midpoint.web.component.data.SelectableDataTable;
import com.evolveum.midpoint.web.component.data.column.SelectableObjectNameColumn;
import com.evolveum.midpoint.web.session.ObjectListStorage;

import com.evolveum.midpoint.web.session.PageStorage;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DefaultGuiObjectListPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectColumnType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.repeater.Item;
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

import org.jetbrains.annotations.Nullable;

public abstract class PopupObjectListPanel<O extends ObjectType> extends ObjectListPanel<O> {
    private static final long serialVersionUID = 1L;

    private boolean multiselect;
    private ObjectListStorage storage;

    /**
     * @param defaultType specifies type of the object that will be selected by default
     */
    public PopupObjectListPanel(String id, Class<O> defaultType, boolean multiselect) {
        this(id, defaultType, null, multiselect);
        this.multiselect = multiselect;
    }

    public PopupObjectListPanel(String id, Class<O> defaultType, Collection<SelectorOptions<GetOperationOptions>> options,
                                boolean multiselect) {
        super(id, defaultType);
        this.multiselect = multiselect;
    }

    @Override
    protected IColumn<SelectableBean<O>, String> createCheckboxColumn() {
        if (isMultiselect()) {
            return new CheckBoxHeaderColumn<>() {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdateRow(Item<ICellPopulator<SelectableBean<O>>> cellItem, AjaxRequestTarget target, DataTable table, IModel<SelectableBean<O>> rowModel, IModel<Boolean> selected) {
                    super.onUpdateRow(cellItem, target, table, rowModel, selected);
                    updatePreselectedObjects(Arrays.asList(rowModel));
                    onUpdateCheckbox(target, Arrays.asList(rowModel), table);
                    target.add(cellItem.findParent(SelectableDataTable.SelectableRowItem.class));
                }

                @Override
                protected void onUpdateHeader(AjaxRequestTarget target, boolean selected, DataTable table) {
                    super.onUpdateHeader(target, selected, table);
                    updatePreselectedObjects(TableUtil.getAvailableData(table));
                    onUpdateCheckbox(target, TableUtil.getAvailableData(table), table);
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
    protected final ISelectableDataProvider<SelectableBean<O>> createProvider() {
        return createSelectableBeanObjectDataProvider(() -> getCustomizeContentQuery(), null);
    }

    protected abstract ObjectQuery getCustomizeContentQuery();

    @Override
    protected IColumn<SelectableBean<O>, String> createNameColumn(IModel<String> displayModel, GuiObjectColumnType customColumn, ExpressionType expression) {
        return new SelectableObjectNameColumn<>(displayModel == null ? createStringResource("ObjectType.name") : displayModel,
                customColumn, expression, getPageBase()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<O>> rowModel) {
                O object = rowModel.getObject().getValue();
                PopupObjectListPanel.this.onSelectPerformed(target, object);
            }

            @Override
            public boolean isClickable(IModel<SelectableBean<O>> rowModel) {
                return !PopupObjectListPanel.this.isMultiselect();
            }
        };
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

    protected void onUpdateCheckbox(AjaxRequestTarget target, List<IModel<SelectableBean<O>>> rowModel, DataTable table) {
    }

    protected IModel<Boolean> getCheckBoxEnableModel(IModel<SelectableBean<O>> rowModel) {
        return Model.of(true);
    }

    protected String getStorageKey() {
        return null;
    }

    protected void updatePreselectedObjects(List<IModel<SelectableBean<O>>> rowModelList) {
        if (CollectionUtils.isEmpty(rowModelList)) {
            return;
        }
        rowModelList.forEach(rowModel -> {
            SelectableBean<O> selectableBean = rowModel.getObject();
            O selectedObject = selectableBean.getValue();
            List<O> preselectedObjects = getPreselectedObjectList();
            if (selectableBean.isSelected()) {
                preselectedObjects.add(selectedObject);
            } else {
                preselectedObjects.removeIf(o -> selectedObject.getOid().equals(o.getOid()));
            }
        });
    }

    @Override
    public PageStorage getPageStorage() {
        if (storage == null) {
            storage = new ObjectListStorage();
        }
        return storage;
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

    @Override
    protected boolean isCollectionViewPanelForCompiledView() {
        return false;
    }

    @Override
    protected String getCollectionNameFromPageParameters() {
        return null;
    }

    @Override
    protected @Nullable DefaultGuiObjectListPanelConfigurationType getDefaultObjectListConfiguration() {
        return null;
    }
}
