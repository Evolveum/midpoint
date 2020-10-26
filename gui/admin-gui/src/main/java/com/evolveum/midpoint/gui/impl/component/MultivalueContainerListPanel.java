/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.MultiCompositedButtonPanel;
import com.evolveum.midpoint.web.component.MultiFunctinalButtonDto;

import com.evolveum.midpoint.web.component.data.BoxedTablePanel;

import com.evolveum.midpoint.web.component.data.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.objectdetails.AssignmentHolderTypeMainPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.objectdetails.FocusMainPanel;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchFormPanel;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.web.component.util.MultivalueContainerListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.jetbrains.annotations.NotNull;

/**
 * @author skublik
 */

public abstract class MultivalueContainerListPanel<C extends Containerable>
        extends BasePanel<PrismContainerWrapper<C>> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(MultivalueContainerListPanel.class);

    private static final String ID_LIST_PANEL = "listPanel";

    private LoadableModel<Search> searchModel = null;
    private IModel<PrismContainerWrapper<C>> model;
    private Class<C> type;

    public MultivalueContainerListPanel(String id, IModel<PrismContainerWrapper<C>> model) {
        super(id, model);

        this.model = model;
        searchModel = new LoadableModel<Search>(false) {

            private static final long serialVersionUID = 1L;

            @Override
            protected Search load() {
                if (model == null || model.getObject() == null){
                    return null;
                }
                List<SearchItemDefinition> availableDefs = initSearchableItems(model.getObject());

                Search search = new Search(model.getObject().getCompileTimeClass(), availableDefs);
                search.setCanConfigure(true);
                return search;
            }
        };
        type = model.getObject().getTypeClass();
    }

    public MultivalueContainerListPanel(String id, @NotNull PrismContainerDefinition<C> def) {
//        super(id, def.getCompileTimeClass());
        super(id);

        searchModel = new LoadableModel<Search>(false) {

            private static final long serialVersionUID = 1L;

            @Override
            protected Search load() {
                List<SearchItemDefinition> availableDefs = initSearchableItems(def);

                Search search = new Search(def.getCompileTimeClass(), availableDefs);
                search.setCanConfigure(true);
                return search;
            }

        };
        type = def.getTypeClass();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout(){
        ContainerableListPanel<C, PrismContainerValueWrapper<C>> table =
                new ContainerableListPanel<C, PrismContainerValueWrapper<C>>(ID_LIST_PANEL, type) {
            @Override
            protected List<IColumn<PrismContainerValueWrapper<C>, String>> createColumns() {
                return MultivalueContainerListPanel.this.createColumns();
            }

            @Override
            protected ISelectableDataProvider<C, PrismContainerValueWrapper<C>> createProvider() {
                return MultivalueContainerListPanel.this.createProvider();
            }

            @Override
            protected WebMarkupContainer initButtonToolbar(String id) {
                return MultivalueContainerListPanel.this.initButtonToolbar(id);
            }

            @Override
            protected boolean isListPanelVisible() {
                return MultivalueContainerListPanel.this.isListPanelVisible();
            }

            @Override
            protected WebMarkupContainer getSearchPanel(String contentAreaId) {
                WebMarkupContainer search = MultivalueContainerListPanel.this.getSearchPanel(contentAreaId);
                if (search == null) {
                    return super.getSearchPanel(contentAreaId);
                }
                return search;
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return MultivalueContainerListPanel.this.getTableId();
            }

            @Override
            protected String getStorageKey() {
                return MultivalueContainerListPanel.this.getStorageKey();
            }
        };
        table.setOutputMarkupId(true);
        table.getPageStorage().setPaging(getPrismContext().queryFactory().createPaging(0, (int) getPageBase().getItemsPerPage(getTableId())));
        add(table);
    }

    protected abstract String getStorageKey();

    protected WebMarkupContainer getSearchPanel(String contentAreaId){
        return null;
    }

    protected abstract UserProfileStorage.TableId getTableId();

    protected abstract List<IColumn<PrismContainerValueWrapper<C>, String>> createColumns();

    protected abstract List<SearchItemDefinition> initSearchableItems(PrismContainerDefinition<C> containerDef);

    protected abstract void initPaging();

    protected IModel<List<PrismContainerValueWrapper<C>>> loadValuesModel() {
        if (getModel() == null) {
            LOGGER.info("Parent model is null. Cannot load model for values for table: {}", getListPanel().getTableId());
        }

        return new PropertyModel<>(getModel(), "values");
    }

    protected ISelectableDataProvider<C, PrismContainerValueWrapper<C>> createProvider() {
        MultivalueContainerListDataProvider<C> containersProvider = new MultivalueContainerListDataProvider<C>(this, loadValuesModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            public ObjectQuery getQuery() {
                return MultivalueContainerListPanel.this.createProviderQuery();
            }

            @Override
            protected List<PrismContainerValueWrapper<C>> searchThroughList() {
                List<PrismContainerValueWrapper<C>> resultList = super.searchThroughList();
                return postSearch(resultList);
            }

            @Override
            protected void saveProviderPaging(ObjectQuery query, ObjectPaging paging) {
                PageStorage storage = getPageStorage();
                if (storage != null) {
                    storage.setPaging(paging);
                }
            }

        };
        return containersProvider;
    }

    public ContainerableListPanel getListPanel() {
        return (ContainerableListPanel) get(ID_LIST_PANEL);
    }

    private PageStorage getPageStorage() {
        return getListPanel().getPageStorage();
    }

    protected WebMarkupContainer initButtonToolbar(String id) {
        return getNewItemButton(id);
    }

    public MultiCompositedButtonPanel getNewItemButton(String id) {
        MultiCompositedButtonPanel newObjectIcon =
                new MultiCompositedButtonPanel(id, createNewButtonDescription()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void buttonClickPerformed(AjaxRequestTarget target, AssignmentObjectRelation relationSepc, CompiledObjectCollectionView collectionViews) {
                        newItemPerformed(target, relationSepc);
                    }

                    @Override
                    protected boolean isDefaultButtonVisible(){
                        return getNewObjectGenericButtonVisibility();
                    }

                    @Override
                    protected DisplayType getMainButtonDisplayType() {
                        return getNewObjectButtonDisplayType();
                    }

                    @Override
                    protected DisplayType getDefaultObjectButtonDisplayType() {
                        return getNewObjectButtonDisplayType();
                    }
                };
        newObjectIcon.add(AttributeModifier.append("class", "btn-group btn-margin-right"));
        newObjectIcon.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isCreateNewObjectVisible();
            }

            @Override
            public boolean isEnabled() {
                return isNewObjectButtonEnabled();
            }
        });
        return newObjectIcon;
    }

    protected List<MultiFunctinalButtonDto> createNewButtonDescription() {
        return null;
    }

    protected boolean isNewObjectButtonEnabled(){
        return true;
    }

    protected boolean getNewObjectGenericButtonVisibility(){
        return true;
    }

    protected DisplayType getNewObjectButtonDisplayType(){
        return WebComponentUtil.createDisplayType(GuiStyleConstants.CLASS_ADD_NEW_OBJECT, "green", createStringResource("MainObjectListPanel.newObject").getString());
    }

    protected WebMarkupContainer initSearch(String headerId) {
        SearchFormPanel searchPanel = new SearchFormPanel(headerId, searchModel) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
                MultivalueContainerListPanel.this.searchPerformed(query, target);
            }
        };
        searchPanel.add(new VisibleBehaviour(() -> isSearchEnabled()));
        return searchPanel;
    }

    protected boolean isSearchEnabled(){
        return true;
    }

    private void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
        ContainerableListPanel table = getListPanel();
        table.getTable().setCurrentPage(null);
        target.add((Component) table);
        target.add(getPageBase().getFeedbackPanel());

    }

    private ObjectQuery getQuery() {
        Search search = searchModel.getObject();
        ObjectQuery query = search.createObjectQuery(getPageBase().getPrismContext());
        return query;
    }

    protected abstract List<PrismContainerValueWrapper<C>> postSearch(List<PrismContainerValueWrapper<C>> items);

    private ObjectQuery createProviderQuery() {
        ObjectQuery searchQuery = isSearchEnabled() ? getQuery() : null;

        ObjectQuery customQuery = createQuery();

        if (searchQuery != null && searchQuery.getFilter() != null) {
            if (customQuery != null && customQuery.getFilter() != null) {
                return getPrismContext().queryFactory().createQuery(getPrismContext().queryFactory().createAnd(customQuery.getFilter(), searchQuery.getFilter()));
            }
            return searchQuery;

        }
        return customQuery;
    }

    protected abstract ObjectQuery createQuery();

    protected void newItemPerformed(AjaxRequestTarget target, AssignmentObjectRelation relationSepc){
    };

    public List<PrismContainerValueWrapper<C>> getSelectedItems() {
        BoxedTablePanel<PrismContainerValueWrapper<C>> itemsTable = getListPanel().getTable();
        MultivalueContainerListDataProvider<C> itemsProvider = (MultivalueContainerListDataProvider<C>) itemsTable.getDataTable()
                .getDataProvider();
        return itemsProvider.getAvailableData().stream().filter(a -> a.isSelected()).collect(Collectors.toList());
    }

    public void reloadSavePreviewButtons(AjaxRequestTarget target){
        FocusMainPanel mainPanel = findParent(FocusMainPanel.class);
        if (mainPanel != null) {
            mainPanel.reloadSavePreviewButtons(target);
        }
    }

    public List<PrismContainerValueWrapper<C>> getPerformedSelectedItems(IModel<PrismContainerValueWrapper<C>> rowModel) {
        List<PrismContainerValueWrapper<C>> performedItems = new ArrayList<PrismContainerValueWrapper<C>>();
        List<PrismContainerValueWrapper<C>> listItems = getSelectedItems();
        if((listItems!= null && !listItems.isEmpty()) || rowModel != null) {
            if(rowModel == null) {
                performedItems.addAll(listItems);
                listItems.forEach(itemConfigurationTypeContainerValueWrapper -> {
                    itemConfigurationTypeContainerValueWrapper.setSelected(false);
                });
            } else {
                performedItems.add(rowModel.getObject());
                rowModel.getObject().setSelected(false);
            }
        }
        return performedItems;
    }

    //TODO generalize for properties
    public PrismContainerValueWrapper<C> createNewItemContainerValueWrapper(
            PrismContainerValue<C> newItem,
            PrismContainerWrapper<C> model, AjaxRequestTarget target) {

        return WebPrismUtil.createNewValueWrapper(model, newItem, getPageBase(), target);

    }

    protected abstract void editItemPerformed(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<C>> rowModel, List<PrismContainerValueWrapper<C>> listItems);

    public List<InlineMenuItem> getDefaultMenuActions() {
        List<InlineMenuItem> menuItems = new ArrayList<>();
        menuItems.add(new ButtonInlineMenuItem(createStringResource("pageAdminFocus.button.delete")) {
            private static final long serialVersionUID = 1L;

            public CompositedIconBuilder getIconCompositedBuilder(){
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_DELETE_MENU_ITEM);
            }

            @Override
            public InlineMenuItemAction initAction() {
                return createDeleteColumnAction();
            }
        });

        menuItems.add(new ButtonInlineMenuItem(createStringResource("PageBase.button.edit")) {
            private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder(){
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_EDIT_MENU_ITEM);
            }

            @Override
            public InlineMenuItemAction initAction() {
                return createEditColumnAction();
            }
        });
        return menuItems;
    }


    public <AH extends AssignmentHolderType> PrismObject<AH> getFocusObject(){
        AssignmentHolderTypeMainPanel mainPanel = findParent(AssignmentHolderTypeMainPanel.class);
        if (mainPanel != null) {
            return mainPanel.getObjectWrapper().getObject();
        }
        return null;
    }

    public ColumnMenuAction<PrismContainerValueWrapper<C>> createDeleteColumnAction() {
        return new ColumnMenuAction<PrismContainerValueWrapper<C>>() {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (getRowModel() == null) {
                    deleteItemPerformed(target, new ArrayList<>());
                } else {
                    List<PrismContainerValueWrapper<C>> toDelete = new ArrayList<>();
                    toDelete.add(getRowModel().getObject());
                    deleteItemPerformed(target, toDelete);
                }
            }
        };
    }

    public ColumnMenuAction<PrismContainerValueWrapper<C>> createEditColumnAction() {
        return new ColumnMenuAction<PrismContainerValueWrapper<C>>() {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                editItemPerformed(target, getRowModel(), getSelectedItems());
            }
        };
    }

    protected void deleteItemPerformed(AjaxRequestTarget target, List<PrismContainerValueWrapper<C>> toDelete){
        if (toDelete == null || toDelete.isEmpty()){
            warn(createStringResource("MultivalueContainerListPanel.message.noItemsSelected").getString());
            target.add(getPageBase().getFeedbackPanel());
            return;
        }
        toDelete.forEach(value -> {
            if (value.getStatus() == ValueStatus.ADDED) {
                PrismContainerWrapper<C> wrapper = (PrismContainerWrapper<C>) getModelObject();
                wrapper.getValues().remove(value);
            } else {
                value.setStatus(ValueStatus.DELETED);
            }
            value.setSelected(false);
        });
        getListPanel().refreshTable(target);
        reloadSavePreviewButtons(target);
    }

    protected abstract boolean isCreateNewObjectVisible();

    @Override
    public IModel<PrismContainerWrapper<C>> getModel() {
        return model;
    }

    public boolean isListPanelVisible(){
        return true;
    }

    protected IModel<String> createStyleClassModelForNewObjectIcon() {
        return new IModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return "btn btn-success btn-sm";
            }
        };
    }
}
