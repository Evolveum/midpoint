/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import java.util.*;
import java.util.function.Supplier;

import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.web.component.data.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.data.column.ObjectNameColumn;
import com.evolveum.midpoint.web.component.search.*;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import static java.util.Collections.singleton;

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
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.session.PageStorage;

/**
 * @author katkav
 */
public abstract class ObjectListPanel<O extends ObjectType> extends ContainerableListPanel<O, SelectableBean<O>> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ObjectListPanel.class);
    private static final String DOT_CLASS = ObjectListPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_CUSTOM_MENU_ITEMS = DOT_CLASS + "loadCustomMenuItems";

    private ObjectTypes type;

    public Class<O> getType() {
        return (Class) type.getClassDefinition();
    }

    public void setType(Class<? extends O> type) {
        this.type = type  != null ? ObjectTypes.getObjectType(type) : null;
    }

    /**
     * @param defaultType specifies type of the object that will be selected by default. It can be changed.
     */
    public ObjectListPanel(String id, Class<? extends O> defaultType) {
        this(id, defaultType, null);
    }

    /**
     * @param defaultType specifies type of the object that will be selected by default. It can be changed.
     */
    public ObjectListPanel(String id, Class<? extends O> defaultType, Collection<SelectorOptions<GetOperationOptions>> options) {
        super(id, defaultType, options);
        this.type = defaultType  != null ? ObjectTypes.getObjectType(defaultType) : null;
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

    protected Search createSearch() {
        return SearchFactory.createSearch(type.getClassDefinition(), isCollectionViewPanelForCompiledView() ? getCollectionNameParameterValue().toString() : null,
                getFixedSearchItems(), null, getPageBase(), true);
    }

    protected List<ItemPath> getFixedSearchItems() {
        List<ItemPath> fixedSearchItems = new ArrayList<>();
        fixedSearchItems.add(ObjectType.F_NAME);
        return fixedSearchItems;
    }

    protected ISelectableDataProvider createProvider() {
        List<O> preSelectedObjectList = getPreselectedObjectList();
        SelectableBeanObjectDataProvider<O> provider = new SelectableBeanObjectDataProvider<O>(
                getPageBase(), (Class) type.getClassDefinition(), preSelectedObjectList == null ? null : new HashSet<>(preSelectedObjectList)) {
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
                List<ObjectOrdering> customOrdering = createCustomOrdering(sortParam);
                if (customOrdering != null) {
                    return customOrdering;
                }
                return super.createObjectOrderings(sortParam);
            }

            @Override
            public ObjectQuery getQuery() {
                return createQuery();
            }

            @Override
            protected CompiledObjectCollectionView getCompiledObjectCollectionView() {
                return getObjectCollectionView();
            }
        };
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
    protected IColumn<SelectableBean<O>, String> createNameColumn(IModel<String> columnNameModel, String itemPath, ExpressionType expression) {
        return new ObjectNameColumn<O>(columnNameModel == null ? createStringResource("ObjectType.name") : columnNameModel,
                itemPath, expression, getPageBase(), StringUtils.isEmpty(itemPath)) {
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

    protected void objectDetailsPerformed(AjaxRequestTarget target, O object){};
}
