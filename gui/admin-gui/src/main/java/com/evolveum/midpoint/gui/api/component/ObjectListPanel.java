/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import java.util.*;
import java.util.function.Supplier;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.ContainerListPanel;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.web.component.search.*;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.*;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.web.component.data.Table;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableListDataProvider;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

import static java.util.Collections.singleton;

/**
 * @author katkav
 */
public abstract class ObjectListPanel<O extends ObjectType> extends ContainerListPanel<O> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ObjectListPanel.class);
    private static final String DOT_CLASS = ObjectListPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_CUSTOM_MENU_ITEMS = DOT_CLASS + "loadCustomMenuItems";

    private ObjectTypes type;

    private Boolean manualRefreshEnabled;

    public Class<O> getType() {
        return (Class) type.getClassDefinition();
    }



    /**
     * @param defaultType specifies type of the object that will be selected by default. It can be changed.
     */
    public ObjectListPanel(String id, Class<? extends O> defaultType, TableId tableId, Collection<SelectorOptions<GetOperationOptions>> options) {
        this(id, defaultType, tableId, options, false);
    }

    /**
     * @param defaultType specifies type of the object that will be selected by default. It can be changed.
     */
    ObjectListPanel(String id, Class<? extends O> defaultType, TableId tableId, boolean multiselect) {
        this(id, defaultType, tableId, null, multiselect);
    }

    public ObjectListPanel(String id, Class<? extends O> defaultType, TableId tableId, Collection<SelectorOptions<GetOperationOptions>> options,
                           boolean multiselect) {
        super(id, defaultType, tableId, options, multiselect);
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

    @Override
    public List<O> getSelectedObjects() {
        BaseSortableDataProvider<? extends SelectableBean<O>> dataProvider = getDataProvider();
        if (dataProvider instanceof SelectableBeanObjectDataProvider) {
            return ((SelectableBeanObjectDataProvider<O>) dataProvider).getSelectedData();
        } else if (dataProvider instanceof SelectableListDataProvider) {
            return ((SelectableListDataProvider) dataProvider).getSelectedObjects();
        }
        return new ArrayList<>();
    }

    protected Search createSearch() {
        return SearchFactory.createSearch(type.getClassDefinition(), isCollectionViewPanelForCompiledView() ? getCollectionNameParameterValue().toString() : null,
                null, getPageBase(), true);
    }

    protected BaseSortableDataProvider<SelectableBean<O>> createProvider() {
        List<O> preSelectedObjectList = getPreselectedObjectList();
        SelectableBeanObjectDataProvider<O> provider = new SelectableBeanObjectDataProvider<O>(
                getPageBase(), (Class) type.getClassDefinition(), preSelectedObjectList == null ? null : new HashSet<>(preSelectedObjectList)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void saveProviderPaging(ObjectQuery query, ObjectPaging paging) {
                String storageKey = getStorageKey();
                if (StringUtils.isNotEmpty(storageKey)) {
                    PageStorage storage = getPageStorage(storageKey);
                    if (storage != null) {
                        storage.setPaging(paging);
                    }
                }
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
                List<ObjectOrdering> customOrdering =  createCustomOrdering(sortParam);
                if (customOrdering != null) {
                    return customOrdering;
                }
                return super.createObjectOrderings(sortParam);
            }

            @Override
            public boolean isOrderingDisabled() {
                return ObjectListPanel.this.isOrderingDisabled();
            }

            @Override
            public boolean isUseObjectCounting(){
                return isCountingEnabled();
            }
        };
        provider.setOptions(createOptions());
        setDefaultSorting(provider);
        provider.setQuery(createQuery());

        return provider;
    }

    protected boolean isCountingEnabled(){
        CompiledObjectCollectionView guiObjectListViewType = getObjectCollectionView();
        if (isAdditionalPanel()){
            if (guiObjectListViewType != null && guiObjectListViewType.getAdditionalPanels() != null &&
                    guiObjectListViewType.getAdditionalPanels().getMemberPanel() != null &&
                    guiObjectListViewType.getAdditionalPanels().getMemberPanel().isDisableCounting() != null){
                return !guiObjectListViewType.getAdditionalPanels().getMemberPanel().isDisableCounting();
            }
        } else {
            if (guiObjectListViewType != null && guiObjectListViewType.isDisableCounting() != null){
                return !guiObjectListViewType.isDisableCounting();
            }
        }
        return true;
    }

    protected List<CompiledObjectCollectionView> getAllApplicableArchetypeViews() {
        return getPageBase().getCompiledGuiProfile().findAllApplicableArchetypeViews(WebComponentUtil.classToQName(getPageBase().getPrismContext(), getType()));
    }

    public void refreshTable(Class<O> newTypeClass, AjaxRequestTarget target) {
        BoxedTablePanel<SelectableBean<O>> table = getTable();
        if (isTypeChanged(newTypeClass)) {
            ObjectTypes newType = newTypeClass != null ? ObjectTypes.getObjectType(newTypeClass) : null;

            BaseSortableDataProvider<SelectableBean<O>> provider = getDataProvider();
            provider.setQuery(createQuery());
            if (newType != null && provider instanceof SelectableBeanObjectDataProvider) {
                ((SelectableBeanObjectDataProvider<O>) provider).setType(newTypeClass);
            }

            ((WebMarkupContainer) table.get("box")).addOrReplace(initSearch("header"));
            if (newType != null && !this.type.equals(newType)) {
                this.type = newType;
                resetSearchModel();
                table.setCurrentPage(null);
            } else {
                saveSearchModel(getCurrentTablePaging());
            }
        }

        target.add((Component) table);
        target.add(getPageBase().getFeedbackPanel());

    }

    public void clearCache() {
        WebComponentUtil.clearProviderCache(getDataProvider());
    }

    protected ObjectQuery createQuery() {
        Search search = getSearchModel().getObject();
        ObjectQuery query = search.createObjectQuery(getPageBase().getPrismContext());
        query = addArchetypeFilter(query);
        query = addFilterToContentQuery(query);
        return query;
    }

    private ObjectQuery addArchetypeFilter(ObjectQuery query){
        if (!isCollectionViewPanel()){
            return query;
        }
        CompiledObjectCollectionView view = getObjectCollectionView();
        if (view == null){
            getFeedbackMessages().add(ObjectListPanel.this, "Unable to load collection view list", 0);
            return query;
        }

        if (view.getFilter() == null) {
            return query;
        }

        if (query == null) {
            query = getPrismContext().queryFactory().createQuery();
        }
        query.addFilter(view.getFilter());
        return query;

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

    private List<GuiObjectColumnType> getGuiObjectColumnTypeList(){
        CompiledObjectCollectionView guiObjectListViewType = getObjectCollectionView();
        return guiObjectListViewType != null ? guiObjectListViewType.getColumns() : null;
    }
}
