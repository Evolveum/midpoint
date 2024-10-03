/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.panel;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchValue;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.FilterableSearchItemWrapper;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.ObjectCollectionListSearchItemWrapper;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.PropertySearchItemWrapper;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.CollectionInstance;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.DeleteConfirmationPanel;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.wicket.util.string.StringValue;

public abstract class SearchPanel<C extends Serializable> extends BasePanel<Search<C>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = SearchPanel.class.getName() + ".";
    private static final Trace LOGGER = TraceManager.getTrace(SearchPanel.class);
    private static final String OPERATION_REMOVE_SAVED_FILTER = DOT_CLASS + "removeSavedFilterFromUserAdminGuiConfiguration";
    private static final String ID_FORM = "form";
    private static final String ID_SEARCH_ITEMS_PANEL = "searchItemsPanel";
    private static final String ID_SEARCH_BUTTON_PANEL = "searchButtonPanel";
    private static final String ID_SAVE_SEARCH_CONTAINER = "saveSearchContainer";
    private static final String ID_SAVE_SEARCH_BUTTON = "saveSearchButton";
    private static final String ID_SAVED_SEARCH_MENU = "savedSearchMenu";
    private static final String ID_SAVED_FILTER_MENU = "savedFilterMenu";
    private static final String ID_SAVED_FILTER_ITEM = "savedFilterItem";
    private static final String ID_SAVED_FILTER_NAME = "savedFilterName";
    private static final String ID_SAVED_FILTER_NAME_LABEL = "savedFilterNameLabel";
    private static final String ID_SAVED_FILTER_REMOVE_BUTTON = "savedFilterRemoveButton";
    private static final String ID_TYPE_SEARCH = "typeSelector";
    private LoadableDetachableModel<List<InlineMenuItem>> savedSearchListModel;
    private static final Trace LOG = TraceManager.getTrace(SearchPanel.class);

    public SearchPanel(String id, IModel<Search<C>> searchModel) {
        super(id, searchModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        MidpointForm<?> form = new MidpointForm<>(ID_FORM);
        add(form);

        initSearchPanel(form);
        IModel<SearchBoxModeType> searchButtonModel = new IModel<>(){

            @Override
            public SearchBoxModeType getObject() {
                return getModelObject().getSearchMode();
            }

            @Override
            public void setObject(SearchBoxModeType searchBoxMode) {
                getModelObject().setSearchMode(searchBoxMode);
            }
        };
        SearchButtonWithDropdownMenu<SearchBoxModeType> searchButtonPanel = new SearchButtonWithDropdownMenu<>(ID_SEARCH_BUTTON_PANEL,
                new PropertyModel<>(getModel(), Search.F_ALLOWED_MODES), searchButtonModel) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void searchPerformed(AjaxRequestTarget target) {
                SearchPanel.this.searchPerformed(target);
            }

            @Override
            protected void menuItemSelected(AjaxRequestTarget target, SearchBoxModeType searchBoxModeType) {
                searchBoxTypeUpdated(target);
            }

            @Override
            public IModel<Boolean> isMenuItemVisible(SearchBoxModeType searchBoxModeType) {
                return Model.of(SearchPanel.this.getModelObject().getAllowedModeList().contains(searchBoxModeType));
            }

            @Override
            protected VisibleEnableBehaviour getSearchButtonVisibleEnableBehavior() {
                return SearchPanel.this.getSearchButtonVisibleEnableBehavior();
            }
        };

        searchButtonPanel.setOutputMarkupId(true);
        form.add(searchButtonPanel);
        form.setDefaultButton(searchButtonPanel.getSearchButton());

        initSavedFiltersContainer(form);
    }

    private void initSearchPanel(Form form) {
        ObjectTypeSearchItemPanel<C> objectTypeSearchItemPanel = new ObjectTypeSearchItemPanel<>(ID_TYPE_SEARCH, new PropertyModel<>(getModel(), Search.F_TYPE));
        objectTypeSearchItemPanel.add(new VisibleBehaviour(this::isTypeSearchVisible));
        form.add(objectTypeSearchItemPanel);

        initSpecificSearchPanel(ID_SEARCH_ITEMS_PANEL, form);
    }

    private boolean isTypeSearchVisible() {
        return getModelObject().getAllowedTypeList().size() > 1 && getModelObject().getSearchMode() != SearchBoxModeType.OID;
    }

    public void initSpecificSearchPanel(String panelId, Form form) {
        SearchBoxModeType modeType = getModelObject().getSearchMode();
        switch (modeType) {
            case BASIC:
                BasicSearchPanel basicSearchPanel = new BasicSearchPanel(panelId, new PropertyModel<>(getModel(), Search.F_BASIC_SEARCH));
                basicSearchPanel.setOutputMarkupId(true);
                form.addOrReplace(basicSearchPanel);
                break;
            case AXIOM_QUERY:
                AxiomSearchPanel axiomSearchPanel = new AxiomSearchPanel(panelId, new PropertyModel<>(getModel(), Search.F_AXIOM_SEARCH));
                axiomSearchPanel.setOutputMarkupId(true);
                form.addOrReplace(axiomSearchPanel);
                break;
            case ADVANCED:
                XmlSearchPanel xmlSearchPanel = new XmlSearchPanel(panelId, new PropertyModel<>(getModel(), Search.F_ADVANCED_SEARCH));
                xmlSearchPanel.setOutputMarkupId(true);
                form.addOrReplace(xmlSearchPanel);
                break;
            case OID:
                OidSearchItemPanel oidSearchItemPanel = new OidSearchItemPanel(panelId, new PropertyModel<>(getModel(), Search.F_OID_SEARCH));
                oidSearchItemPanel.setOutputMarkupId(true);
                form.addOrReplace(oidSearchItemPanel);
                break;
            case FULLTEXT:
                FulltextSearchPanel fulltextSearchPanel = new FulltextSearchPanel(panelId, new PropertyModel<>(getModel(), Search.F_FULLTEXT_SEARCH));
                fulltextSearchPanel.setOutputMarkupId(true);
                form.addOrReplace(fulltextSearchPanel);
                break;
            default:
                Label notSupportedType = new Label(panelId, "Not supported type of search: " + modeType);
                notSupportedType.setOutputMarkupId(true);
                form.addOrReplace(notSupportedType);
        }
    }

    private void initSavedFiltersContainer(MidpointForm form) {
        WebMarkupContainer saveSearchContainer = new WebMarkupContainer(ID_SAVE_SEARCH_CONTAINER);
        saveSearchContainer.add(new VisibleBehaviour(this::isSaveSearchVisible));
        saveSearchContainer.setOutputMarkupId(true);
        form.add(saveSearchContainer);
        savedSearchListModel = new LoadableDetachableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<InlineMenuItem> load() {

                List<InlineMenuItem> menuItemList = new ArrayList<>();
                List<AvailableFilterType> availableFilters = getSavedFilterList();
                if (CollectionUtils.isEmpty(availableFilters)) {
                    return menuItemList;
                }

                menuItemList = availableFilters.stream()
                        .map(filter -> createAvailableFilterInlineMenu(filter))
                                .collect(Collectors.toList());
                return menuItemList;
            }
        };

        AjaxButton saveSearchButton = new AjaxButton(ID_SAVE_SEARCH_BUTTON) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                boolean containesCollectionFilter = false;
                Search<C> search = SearchPanel.this.getModelObject();
                for (FilterableSearchItemWrapper<?> item : search.getItems()) {
                    if (item instanceof ObjectCollectionListSearchItemWrapper && item.getValue() != null && item.getValue().getValue() != null) {
                        containesCollectionFilter = true;
                        break;
                    }
                }
                if (containesCollectionFilter) {
                    warn(getString("SaveSearchPanel.cannot.save.objectCollection.filter"));
                    target.add(getPageBase().getFeedbackPanel());
                    return;
                }
                SaveSearchPanel panel = new SaveSearchPanel(getPageBase().getMainPopupBodyId(),
                        SearchPanel.this.getModel(),
                        SearchPanel.this.getModelObject().getTypeClass(),
                        getCollectionInstanceDefaultIdentifier()) {

                    @Override
                    protected void saveSearchFilterPerformed(AjaxRequestTarget target) {
                        reloadSavedSearchFilters(target);
                    }
                };
                getPageBase().showMainPopup(panel, target);
            }
        };
        saveSearchButton.add(AttributeAppender.append("title", getPageBase().createStringResource("SearchPanel.saveFilterButton.title")));
        saveSearchButton.setOutputMarkupId(true);
        saveSearchContainer.add(saveSearchButton);

        AjaxLink<Void> savedSearchMenu = new AjaxLink<>(ID_SAVED_SEARCH_MENU) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
//                savedFiltersPopover.togglePopover(target);
            }
        };
        savedSearchMenu.add(new VisibleBehaviour(() -> CollectionUtils.isNotEmpty(savedSearchListModel.getObject())));
        savedSearchMenu.setOutputMarkupId(true);
        savedSearchMenu.add(AttributeAppender.append("title",
                getPageBase().createStringResource("SearchPanel.savedFiltersListButton.title")));
        saveSearchContainer.add(savedSearchMenu);

        WebMarkupContainer savedFilterMenuContainer = new WebMarkupContainer(ID_SAVED_FILTER_MENU);
        savedFilterMenuContainer.setOutputMarkupId(true);
//        savedFilterMenuContainer.add(AttributeAppender.append("class", ""));
        saveSearchContainer.add(savedFilterMenuContainer);

        ListView<InlineMenuItem> li = new ListView<>(ID_SAVED_FILTER_ITEM, savedSearchListModel) {

            @Override
            protected void populateItem(ListItem<InlineMenuItem> item) {
                AjaxSubmitLink itemLabel = new AjaxSubmitLink(ID_SAVED_FILTER_NAME) {
                    @Serial private static final long serialVersionUID = 1L;
                    @Override
                    public void onSubmit(AjaxRequestTarget target) {
                        selectSavedFilterPerformed(findFilterById(item.getModelObject().getId()), target);
                    }
                };
                itemLabel.add(new Label(ID_SAVED_FILTER_NAME_LABEL, item.getModelObject().getLabel()));
                item.add(itemLabel);

                AjaxLink removeButton = new AjaxLink<>(ID_SAVED_FILTER_REMOVE_BUTTON) {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        removeFilterButtonClicked(findFilterById(item.getModelObject().getId()),
                                item.getModelObject().getLabel() != null ? item.getModelObject().getLabel().getObject() : "",
                                ajaxRequestTarget);
                    }
                };
                final String mouseOverStyle = "color: red;";
                final String mouseLeaveStyle = "color: red; display: none;";
                removeButton.add(AttributeAppender.append("style", mouseLeaveStyle));
                item.add(removeButton);

                item.add(new AjaxEventBehavior("mouseenter") {
                    @Override
                    public void onEvent(AjaxRequestTarget target) {
                        removeButton.add(AttributeModifier.remove("style"));
                        removeButton.add(AttributeAppender.append("style", mouseOverStyle));
                        target.add(removeButton);
                    }
                });
                item.add(new AjaxEventBehavior("mouseleave") {
                    @Override
                    public void onEvent(AjaxRequestTarget target) {
                        removeButton.add(AttributeAppender.append("style", mouseLeaveStyle));
                        target.add(removeButton);
                    }
                });

            }
        };

        savedFilterMenuContainer.add(li);
    }

    private InlineMenuItem createAvailableFilterInlineMenu(AvailableFilterType filter) {
        PolyStringType filterLabel = filter.getDisplay() != null ? filter.getDisplay().getLabel() : null;
        InlineMenuItem item = new InlineMenuItem(Model.of(filterLabel == null ? "" : WebComponentUtil.getTranslatedPolyString(filterLabel))) {
            @Override
            public InlineMenuItemAction initAction() {
                return new InlineMenuItemAction() {

                    public void onClick(AjaxRequestTarget target) {
                    }
                };
            }

        };
        item.setId(Integer.parseInt("" + filter.getId()));
        return item;
    }

    private AvailableFilterType findFilterById(int id) {
        List<AvailableFilterType> availableFilters = getSavedFilterList();
        if (CollectionUtils.isEmpty(availableFilters)) {
            return null;
        }
        for (AvailableFilterType availableFilter : availableFilters) {
            if (availableFilter.getId().toString().equals("" + id)) {
                return availableFilter;
            }
        }
        return null;
    }

    private void removeFilterButtonClicked(AvailableFilterType filter, String filterName, AjaxRequestTarget target) {
        if (filter == null) {
            return;
        }
        DeleteConfirmationPanel confirmationPanel = new DeleteConfirmationPanel(getPageBase().getMainPopupBodyId(),
                createStringResource("OperationalButtonsPanel.deletePerformed", filterName)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                deleteFilterPerformed(filter, target);
                reloadSavedSearchFilters(target);
            }
        };
        getPageBase().showMainPopup(confirmationPanel, target);
    }

    private void reloadSavedSearchFilters(AjaxRequestTarget target) {
        savedSearchListModel.detach();
        getModelObject().reloadSavedFilters(getParentPage());
        refreshSearchForm(target);
    }

    private void deleteFilterPerformed(AvailableFilterType filter, AjaxRequestTarget target) {
        PageBase page = getPageBase();
        Task task = page.createSimpleTask(OPERATION_REMOVE_SAVED_FILTER);
        OperationResult result = task.getResult();
        FocusType principalFocus = page.getPrincipalFocus();
        try {
            ObjectDelta<UserType> delta = page.getPrismContext().deltaFactory().object().createModificationDeleteContainer
                    (UserType.class, principalFocus.getOid(),
                            getAvailableFilterItemPath(principalFocus, filter),
                            filter.asPrismContainerValue().clone());
            page.getModelService().executeChanges(MiscUtil.createCollection(delta), ModelExecuteOptions.create().raw(), task, result);
        } catch (Exception e) {
            LOGGER.error("Cannot remove filter from user admin gui configuration: {}", e.getMessage(), e);
            result.recordPartialError("Cannot remove filter from user admin gui configuration: {}", e);
        }
        result.computeStatusIfUnknown();
        page.showResult(result);
        target.add(page.getFeedbackPanel());
        target.add(get(ID_FORM));
    }

    private ItemPath getAvailableFilterItemPath(FocusType principalFocus, AvailableFilterType filter) {
        if (!(principalFocus instanceof UserType user)) {
            return null;
        }

        OperationResult result = new OperationResult("load user");
        Task task = getPageBase().createSimpleTask("load user");
        PrismObject<UserType> reloadedPrincipalUser = WebModelServiceUtils.loadObject(UserType.class, user.getOid(), getParentPage(),
                task, result);
        if (reloadedPrincipalUser == null) {
            return null;
        }
        user = reloadedPrincipalUser.asObjectable();
        List<GuiObjectListViewType> views = user.getAdminGuiConfiguration().getObjectCollectionViews().getObjectCollectionView();
        if (CollectionUtils.isEmpty(views)) {
            return null;
        }

        StringValue collectionViewParameter = WebComponentUtil.getCollectionNameParameterValue(getPageBase());
        String viewName = collectionViewParameter == null || collectionViewParameter.isNull()
                ? getCollectionInstanceDefaultIdentifier() : collectionViewParameter.toString();
        if (viewName == null) {
            return null;
        }
        for (GuiObjectListViewType view : views) {
            if (viewName.equals(view.getIdentifier())) {
                SearchBoxConfigurationType searchBoxConfigurationType = view.getSearchBoxConfiguration();
                for (AvailableFilterType availableFilter : searchBoxConfigurationType.getAvailableFilter()) {
                    if (availableFilter.equals(filter)) {
                        return availableFilter.asPrismContainerValue().getPath().allExceptLast();
                    }
                }
            }
        }
        return null;
    }

    private VisibleEnableBehaviour getSearchButtonVisibleEnableBehavior() {
        return new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                Search search = getModelObject();
                if (SearchBoxModeType.FULLTEXT.equals(getModelObject().getSearchMode())) {
                    return search.isFullTextSearchEnabled();
                }
                if (SearchBoxModeType.BASIC.equals(getModelObject().getSearchMode())) {
                    return CollectionUtils.isNotEmpty(getModelObject().getItems());
                }
                return true;
            }
        };
    }

    private void selectSavedFilterPerformed(AvailableFilterType filter, AjaxRequestTarget target) {
        if (filter == null) {
            return;
        }
        if (SearchBoxModeType.BASIC.equals(filter.getSearchMode())) {
            applyFilterToBasicMode(filter.getSearchItem());
        } else if (SearchBoxModeType.AXIOM_QUERY.equals(filter.getSearchMode())
                || SearchBoxModeType.ADVANCED.equals(filter.getSearchMode())) {
            applyFilterToAxiomOrAdvancedMode(filter.getSearchItem(), filter.getSearchMode());
        } else if (SearchBoxModeType.FULLTEXT.equals(filter.getSearchMode())) {
            applyFilterToFulltextMode(filter.getSearchItem());
        }
        getModelObject().setSearchMode(filter.getSearchMode());
        refreshSearchForm(target);
        searchPerformed(target);
    }
    private boolean isCollectionInstancePage() {
        return getCollectionInstance() != null;
    }

    private String getCollectionInstanceDefaultIdentifier() {
        CollectionInstance collectionInstance = getCollectionInstance();
        return collectionInstance != null ? collectionInstance.identifier() : null;
    }

    private CollectionInstance getCollectionInstance() {
        PageBase page = findParent(PageBase.class);
        if (page == null) {
            return null;
        }

        return page.getClass().getAnnotation(CollectionInstance.class);
    }

    private boolean isSaveSearchVisible() {
        return  !WebComponentUtil.hasPopupableParent(SearchPanel.this) && isCollectionInstancePage();
    }

    private void searchBoxTypeUpdated(AjaxRequestTarget target) {
        refreshSearchForm(target);
        searchPerformed(target);
    }

    protected abstract void searchPerformed(AjaxRequestTarget target);

    void refreshSearchForm(AjaxRequestTarget target) {
        Form form = (Form) get(ID_FORM);
        initSpecificSearchPanel(ID_SEARCH_ITEMS_PANEL, form);
        target.add(form);
        saveSearch(getModelObject(), target);
    }

    protected void saveSearch(Search search, AjaxRequestTarget target) {
    }

    private List<AvailableFilterType> getSavedFilterList() {
        return getAvailableFilterList(getModelObject().getAvailableFilterTypes());
    }

    private void applyFilterToBasicMode(List<SearchItemType> items) {
        getSearchItems().forEach(item -> item.setVisible(false));
        getModelObject().setSearchMode(SearchBoxModeType.BASIC);
        items.forEach(this::applyBasicModeSearchItem);
    }

    private List<FilterableSearchItemWrapper<?>> getSearchItems() {
        return getModelObject().getItems();
    }

    private void applyFilterToAxiomOrAdvancedMode(List<SearchItemType> items, SearchBoxModeType mode) {
        getModelObject().setSearchMode(mode);
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        SearchItemType axiomSearchItem = items.get(0);
        if (axiomSearchItem.getFilter() == null) {
            return;
        }
        try {
            ObjectFilter objectFilter = getPageBase().getQueryConverter().createObjectFilter(
                    getModelObject().getTypeClass(), axiomSearchItem.getFilter());
            PrismQuerySerialization serializer = PrismContext.get().querySerializer().serialize(objectFilter);
            getModelObject().setDslQuery(serializer.filterText());
        } catch (SchemaException | PrismQuerySerialization.NotSupportedException e) {
            LOG.error("Unable to parse filter {}, {}", axiomSearchItem.getFilter(), e.getLocalizedMessage());
        }
    }

    private void applyFilterToFulltextMode(List<SearchItemType> items) {
        getModelObject().setSearchMode(SearchBoxModeType.FULLTEXT);
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        SearchItemType fulltextSearchItem = items.get(0);
        if (fulltextSearchItem.getFilter() == null) {
            return;
        }
        try {
            ObjectFilter objectFilter = getPageBase().getQueryConverter().createObjectFilter(getModelObject().getTypeClass(), fulltextSearchItem.getFilter());
            if (!(objectFilter instanceof FullTextFilter)) {
                return;
            }
            getModelObject().setFullText(String.join(" ", ((FullTextFilter)objectFilter).getValues()));
        } catch (SchemaException e) {
            LOG.error("Unable to parse filter {}, {}", fulltextSearchItem.getFilter(), e.getLocalizedMessage());
        }
    }

    private boolean noPathDefined(SearchItemType searchItem, ObjectFilter filter) {
        return searchItem.getPath() == null && filter instanceof ValueFilter && ((ValueFilter) filter).getPath() == null;
    }

    private void applyBasicModeSearchItem(SearchItemType searchItem) {
        try {
            ObjectFilter filter = getPageBase().getQueryConverter().parseFilter(searchItem.getFilter(), getModelObject().getTypeClass());
            if (noPathDefined(searchItem, filter)) {
                return;
            }

            FilterableSearchItemWrapper<?> item = prepareFilterableSearchItemWrapper(searchItem, filter);


            item.setVisible(true);

        } catch (SchemaException e) {
            LOG.error("Unable to parse filter {}, {}", searchItem.getFilter(), e.getLocalizedMessage());
        }
    }

    private FilterableSearchItemWrapper<?> prepareFilterableSearchItemWrapper(SearchItemType searchItem, ObjectFilter filter) {
        FilterableSearchItemWrapper item = null;
        if (filter instanceof ValueFilter) {
            item = parseSearchItemFromValueFilter(searchItem, (ValueFilter) filter);
        } else if (filter instanceof InOidFilter) {
            item = parseSearchItemFromInOidFilter((InOidFilter) filter);
        }

        return item;
    }

    private FilterableSearchItemWrapper parseSearchItemFromValueFilter(SearchItemType searchItem, ValueFilter filter) {
        FilterableSearchItemWrapper item = findPropertySearchItemWrapper(searchItem);
        if (item == null) {
            return null;
        }

        setupFilterValues(item, filter);

        return item;
    }

    private FilterableSearchItemWrapper parseSearchItemFromInOidFilter(InOidFilter filter) {
        FilterableSearchItemWrapper item = getModelObject().findOidSearchItemWrapper();
        if (item == null) {
            return null;
        }
        if (filter.getOids() != null) {
            item.setValue(new SearchValue<>(StringUtils.join(filter.getOids(), " ")));
        }
        return item;
    }

    private PropertySearchItemWrapper<?> findPropertySearchItemWrapper(SearchItemType searchItem) {
        ItemPath path = searchItem.getPath().getItemPath();
        for (FilterableSearchItemWrapper<?> itemWrapper : getSearchItems()) {
            if (!(itemWrapper instanceof PropertySearchItemWrapper)) {
                continue;
            }

            PropertySearchItemWrapper propertySearchItemWrapper = (PropertySearchItemWrapper) itemWrapper;
            if (propertySearchItemWrapper.getPath() == null) {
                continue;
            }

            if (propertySearchItemWrapper.getPath().equivalent(path)) {
                return propertySearchItemWrapper;
            }
        }
        return null;
    }

    private void setupFilterValues(FilterableSearchItemWrapper item, ValueFilter filter) {
        List<? extends PrismValue> values = filter.getValues();
        if (values != null && values.size() > 0) {//todo can it be there multiple values?
            if (TextSearchItemPanel.class.equals(item.getSearchItemPanelClass())) {
                item.setValue(new SearchValue<>(values.get(0).getRealValue().toString()));
            } else {
                item.setValue(new SearchValue<>(values.get(0).getRealValue()));
            }
        }
    }
    //TODO shouldn't be in factory?
    private List<AvailableFilterType> getAvailableFilterList(List<AvailableFilterType> availableFilterTypes) {
        if (CollectionUtils.isEmpty(availableFilterTypes)) {
            return null;
        }
        return availableFilterTypes.stream().sorted((filter1, filter2) -> {
            String label1 = LocalizationUtil.translatePolyString(GuiDisplayTypeUtil.getLabel(filter1.getDisplay()));
            String label2 = LocalizationUtil.translatePolyString(GuiDisplayTypeUtil.getLabel(filter2.getDisplay()));
            return String.CASE_INSENSITIVE_ORDER.compare(label1, label2);

        }).collect(Collectors.toList());
    }

}
