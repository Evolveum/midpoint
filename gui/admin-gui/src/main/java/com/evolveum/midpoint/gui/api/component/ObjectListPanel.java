/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import static java.util.Collections.singleton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;

import com.evolveum.midpoint.gui.api.model.LoadableModel;

import org.apache.commons.lang3.StringUtils;
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
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.data.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.data.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.web.component.data.column.ObjectNameColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.search.ContainerTypeSearchItem;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

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

    @Override
    protected Search createSearch(Class<O> type) {
        return SearchFactory.createSearch(new ContainerTypeSearchItem<>(new SearchValue<>(type, "")), isCollectionViewPanelForCompiledView() ? getCollectionNameParameterValue().toString() : null,
                getFixedSearchItems(), null, getPageBase(), null, true, true, Search.PanelType.DEFAULT);
    }

    protected List<ItemPath> getFixedSearchItems() {
        List<ItemPath> fixedSearchItems = new ArrayList<>();
        fixedSearchItems.add(ObjectType.F_NAME);
        return fixedSearchItems;
    }

    @Override
    protected LoadableModel<Search<O>> getSearchModel() {
        return super.getSearchModel();
    }

    protected ISelectableDataProvider<O, SelectableBean<O>> createProvider() {
        List<O> preSelectedObjectList = getPreselectedObjectList();
        SelectableBeanObjectDataProvider<O> provider = new SelectableBeanObjectDataProvider<O>(
                getPageBase(), getSearchModel(), preSelectedObjectList == null ? null : new HashSet<>(preSelectedObjectList)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected PageStorage getPageStorage() {
                return ObjectListPanel.this.getPageStorage();
            }

            @Override
            public SelectableBean<O> createDataObjectWrapper(O obj) {
                SelectableBean<O> bean = super.createDataObjectWrapper(obj);

                List<InlineMenuItem> inlineMenu = createInlineMenu();
                if (inlineMenu != null) {
                    bean.getMenuItems().addAll(inlineMenu);
                }
                if (obj.getOid() != null) {
                    addCustomActions(bean.getMenuItems(), () -> singleton(obj));
                }
                return bean;
            }

            @NotNull
            @Override
            protected List<ObjectOrdering> createObjectOrderings(SortParam<String> sortParam) {
                List<ObjectOrdering> customOrdering = createCustomOrdering();
                if (customOrdering != null) {
                    return customOrdering;
                }
                return super.createObjectOrderings(sortParam);
            }

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                return ObjectListPanel.this.getCustomizeContentQuery();
            }
        };
        provider.setCompiledObjectCollectionView(getObjectCollectionView());
        provider.setOptions(createOptions());
        setDefaultSorting(provider);

        return provider;
    }

    protected List<CompiledObjectCollectionView> getAllApplicableArchetypeViews() {
        return getPageBase().getCompiledGuiProfile().findAllApplicableArchetypeViews(WebComponentUtil.classToQName(getPageBase().getPrismContext(), getType()));
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
    protected IColumn<SelectableBean<O>, String> createNameColumn(IModel<String> columnNameModel, String itemPath) {
        return new ObjectNameColumn<>(columnNameModel == null ? createStringResource("ObjectType.name") : columnNameModel,
                itemPath, null, getPageBase(), StringUtils.isEmpty(itemPath)) {
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

    protected void objectDetailsPerformed(AjaxRequestTarget target, O object){}

    protected ContainerTypeSearchItem getTypeItem(Class<? extends O> type, List<DisplayableValue<Class<? extends O>>> allowedValues){
        @NotNull ObjectTypes objectType = ObjectTypes.getObjectType(type);
        return new ContainerTypeSearchItem<>(new SearchValue<>(objectType.getClassDefinition(),
                "ObjectType." + objectType.getTypeQName().getLocalPart()),
                allowedValues);
    }
}
