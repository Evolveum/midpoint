/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchFactory;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.web.component.data.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.data.column.ObjectNameColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SerializableFunction;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author katkav
 */
public abstract class ObjectListPanel<O extends ObjectType> extends ContainerableListPanel<O, SelectableBean<O>> {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = ObjectListPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_CUSTOM_MENU_ITEMS = DOT_CLASS + "loadCustomMenuItems";

    /**
     * @param defaultType specifies type of the object that will be selected by default. It can be changed.
     */
    public ObjectListPanel(String id, Class<O> defaultType, Collection<SelectorOptions<GetOperationOptions>> options) {
        super(id, defaultType, options);
    }

    public ObjectListPanel(String id, Class<O> defaultType, Collection<SelectorOptions<GetOperationOptions>> options, ContainerPanelConfigurationType config) {
        super(id, defaultType, options, config);
    }

    protected String getSearchByNameParameterValue() {
        PageParameters parameters = getPageBase().getPageParameters();
        if (parameters == null) {
            return null;
        }
        StringValue value = parameters.get(PageBase.PARAMETER_SEARCH_BY_NAME);
        if (value == null) {
            return null;
        }

        return value.toString();
    }

    protected final SelectableBeanObjectDataProvider<O> createSelectableBeanObjectDataProvider(SerializableSupplier<ObjectQuery> querySuplier,
            SerializableFunction<SortParam<String>, List<ObjectOrdering>> orderingSuplier) {
        SelectableBeanObjectDataProvider<O> provider = new SelectableBeanObjectDataProvider<>(
                getPageBase(), getSearchModel(), null) {
            private static final long serialVersionUID = 1L;

            @Override
            protected PageStorage getPageStorage() {
                return ObjectListPanel.this.getPageStorage();
            }

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                if (querySuplier == null) {
                    return null;
                }
                return querySuplier.get();
            }

            @Override
            protected List<ObjectOrdering> createObjectOrderings(SortParam<String> sortParam) {
                if (orderingSuplier == null) {
                    return super.createObjectOrderings(sortParam);
                }
                return orderingSuplier.apply(sortParam);
            }

            @Override
            public Set<? extends O> getSelected() {
                List<O> preselectedObjects = getPreselectedObjectList();
                return preselectedObjects == null ? new HashSet<>() : new HashSet<>(preselectedObjects);
            }
        };
        provider.setCompiledObjectCollectionView(getObjectCollectionView());
        provider.setOptions(getOptions());
        return provider;
    }

    protected List<CompiledObjectCollectionView> getAllApplicableArchetypeViews() {
        return getPageBase().getCompiledGuiProfile().findAllApplicableArchetypeViews(WebComponentUtil.classToQName(getPageBase().getPrismContext(), getType()), OperationTypeType.ADD);
    }

    public void clearCache() {
        WebComponentUtil.clearProviderCache(getDataProvider());
    }

    protected void addCustomActions(@NotNull List<InlineMenuItem> actionsList, SerializableSupplier<Collection<? extends O>> objectsSupplier) {
        CompiledObjectCollectionView guiObjectListViewType = getObjectCollectionView();
        if (guiObjectListViewType != null && !guiObjectListViewType.getActions().isEmpty()) {
            actionsList.addAll(WebComponentUtil.createMenuItemsFromActions(guiObjectListViewType.getActions(),
                    OPERATION_LOAD_CUSTOM_MENU_ITEMS, getPageBase(), (Supplier) objectsSupplier));
        }
    }

    public void addPerformed(AjaxRequestTarget target, List<O> selected) {
        getPageBase().hideMainPopup(target);
    }

    @Override
    protected boolean notContainsNameColumn(@NotNull List<IColumn<SelectableBean<O>, String>> columns) {
        return columns.stream().noneMatch(c -> c instanceof ObjectNameColumn);
    }

    @Override
    protected IColumn<SelectableBean<O>, String> createNameColumn(IModel<String> displayModel, GuiObjectColumnType customColumn, ExpressionType expression) {
        ItemPath itemPath = WebComponentUtil.getPath(customColumn);

        return new ObjectNameColumn<>(displayModel == null ? createStringResource("ObjectType.name") : displayModel,
                itemPath, expression, getPageBase(), itemPath == null) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<O>> rowModel) {
                O object = rowModel.getObject().getValue();
                ObjectListPanel.this.objectDetailsPerformed(target, object);
            }

            @Override
            public boolean isClickable(IModel<SelectableBean<O>> rowModel) {
                return ObjectListPanel.this.isObjectDetailsEnabled(rowModel);
            }
        };
    }

    protected boolean isObjectDetailsEnabled(IModel<SelectableBean<O>> rowModel) {
        return true;
    }

    protected void objectDetailsPerformed(AjaxRequestTarget target, O object) {
    }

    @Override
    protected O getRowRealValue(SelectableBean<O> rowModelObject) {
        if (rowModelObject == null) {
            return null;
        }
        return rowModelObject.getValue();
    }

    @Override
    protected IColumn<SelectableBean<O>, String> createIconColumn() {
        return ColumnUtils.createIconColumn(getPageBase());
    }

    @Override
    public List<O> getSelectedRealObjects() {
        return getSelectedObjects().stream().map(SelectableBean::getValue).collect(Collectors.toList());
    }
}
