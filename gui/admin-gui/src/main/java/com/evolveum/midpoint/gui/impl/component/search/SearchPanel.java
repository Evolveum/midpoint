/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.gui.impl.component.button.SelectableItemListPopoverPanel;
import com.evolveum.midpoint.prism.Containerable;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.CollectionInstance;
import com.evolveum.midpoint.web.component.dialog.DeleteConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.search.SearchValue;

import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxChannel;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.search.SearchItem;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.PageRepositoryQuery;
import com.evolveum.midpoint.web.security.util.SecurityUtils;

public abstract class SearchPanel<C extends Containerable> extends BasePanel<Search<C>> {

    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = SearchPanel.class.getName() + ".";
    private static final Trace LOGGER = TraceManager.getTrace(SearchPanel.class);
    private static final String OPERATION_REMOVE_SAVED_FILTER = DOT_CLASS + "removeSavedFilterFromUserAdminGuiConfiguration";
    private static final String ID_FORM = "form";
    private static final String ID_SEARCH_ITEMS_PANEL = "searchItemsPanel";
    private static final String ID_SEARCH_BUTTON_PANEL = "searchButtonPanel";
    private static final String ID_SUBMIT_SEARCH_BUTTON = "submitSearchButton";
    private static final String ID_SEARCH_TYPES_MENU = "searchTypesMenu";
    private static final String ID_SEARCH_TYPE_ITEMS = "searchTypeItems";
    private static final String ID_SEARCH_TYPE = "searchType";
    private static final String ID_SAVE_SEARCH_CONTAINER = "saveSearchContainer";
    private static final String ID_SAVE_SEARCH_BUTTON = "saveSearchButton";
    private static final String ID_SAVED_SEARCH_MENU = "savedSearchMenu";
    private static final String ID_SAVED_FILTER_MENU = "savedFilterMenu";
    private static final String ID_SAVED_FILTER_ITEM = "savedFilterItem";
    private static final String ID_SAVED_FILTER_NAME = "savedFilterName";
    private static final String ID_SAVED_FILTER_NAME_LABEL = "savedFilterNameLabel";
    private static final String ID_SAVED_FILTER_REMOVE_BUTTON = "savedFilterRemoveButton";
    private static final String ID_BASIC_SEARCH_FRAGMENT = "basicSearchFragment";
    private static final String ID_ADVANCED_SEARCH_FRAGMENT = "advancedSearchFragment";
    private static final String ID_FULLTEXT_SEARCH_FRAGMENT = "fulltextSearchFragment";
    private static final String ID_OID_SEARCH_FRAGMENT = "oidSearchFragment";
    private static final String ID_ITEMS = "items";
    private static final String ID_ITEM = "item";
    private static final String ID_MORE = "more";
    private static final String ID_MORE_PROPERTIES_POPOVER = "morePropertiesPopover";
    private static final String ID_SAVED_FILTERS_POPOVER = "savedFiltersPopover";
    private static final String ID_DEBUG = "debug";
    private static final String ID_ADVANCED_GROUP = "advancedGroup";
    private static final String ID_ADVANCED_AREA = "advancedArea";
    private static final String ID_AXIOM_QUERY_FIELD = "axiomQueryField";
    private static final String ID_ADVANCED_CHECK = "advancedCheck";
    private static final String ID_ADVANCED_ERROR_GROUP = "advancedErrorGroup";
    private static final String ID_ADVANCED_ERROR = "advancedError";
    private static final String ID_FULL_TEXT_CONTAINER = "fullTextContainer";
    private static final String ID_FULL_TEXT_FIELD = "fullTextField";
    private static final String ID_OID_ITEM = "oidItem";

    private LoadableModel<List<AbstractSearchItemWrapper>> basicSearchItemsModel;
    private LoadableModel<List<AbstractSearchItemWrapper>> morePopupModel;
    private LoadableDetachableModel<List<InlineMenuItem>> savedSearchListModel;
    private static final Trace LOG = TraceManager.getTrace(SearchPanel.class);

    public SearchPanel(String id, IModel<Search<C>> searchModel) {
        super(id, searchModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initBasicSearchItemsModel();
        initMorePopupModel();
        initLayout();
    }

    private void initBasicSearchItemsModel() {
        basicSearchItemsModel = new LoadableModel<List<AbstractSearchItemWrapper>>(true) {
            private static final long serialVersionUID = 1L;
            @Override
            protected List<AbstractSearchItemWrapper> load() {
                return getModelObject().getItems().stream().filter(item
                        -> !(item instanceof OidSearchItemWrapper) && item.isVisible())
                        .collect(Collectors.toList());
            }
        };
    }

    private void initMorePopupModel() {
        morePopupModel = new LoadableModel<List<AbstractSearchItemWrapper>>() {
            @Override
            protected List<AbstractSearchItemWrapper> load() {
                return getModelObject().getItems().stream().filter(item
                        -> !item.isVisible())
                        .collect(Collectors.toList());
            }
        };
    }

    public void displayedSearchItemsModelReset() {
        basicSearchItemsModel.reset();
    }

    private <S extends AbstractSearchItemWrapper, T extends Serializable> void initLayout() {
        setOutputMarkupId(true);

        MidpointForm<?> form = new MidpointForm<>(ID_FORM);
        add(form);

        RepeatingView searchItemsRepeatingView = new RepeatingView(ID_SEARCH_ITEMS_PANEL);
        searchItemsRepeatingView.setOutputMarkupId(true);
        form.add(searchItemsRepeatingView);
        initSearchItemsPanel(searchItemsRepeatingView);

        LoadableDetachableModel<String> labelModel = new LoadableDetachableModel<String>() {
            @Override
            protected String load() {
                return createStringResource(SearchPanel.this.getModelObject().getSearchMode()).getString();
            }
        };
        List<SearchBoxModeType> modesList = getSearchConfigurationWrapper().getAllowedModeList();
        SearchButtonWithDropdownMenu<SearchBoxModeType> searchButtonPanel = new SearchButtonWithDropdownMenu<SearchBoxModeType>(ID_SEARCH_BUTTON_PANEL,
                Model.ofList(modesList), getSearchConfigurationWrapper().getDefaultSearchBoxMode()) {
            private static final long serialVersionUID = 1L;
            @Override
            protected void searchPerformed(AjaxRequestTarget target) {
                SearchPanel.this.searchPerformed(target);
            }


            @Override
            protected void menuItemSelected(AjaxRequestTarget target, SearchBoxModeType searchBoxModeType) {
                searchBoxTypeUpdated(target, searchBoxModeType);
            }


            @Override
            public IModel<Boolean> isMenuItemVisible(SearchBoxModeType searchBoxModeType) {
                return Model.of(getSearchConfigurationWrapper().getAllowedModeList().contains(searchBoxModeType));
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

    private void initSavedFiltersContainer(MidpointForm form) {
        WebMarkupContainer saveSearchContainer = new WebMarkupContainer(ID_SAVE_SEARCH_CONTAINER);
        saveSearchContainer.add(new VisibleBehaviour(() -> !isPopupWindow() && isCollectionInstancePage()));
        saveSearchContainer.setOutputMarkupId(true);
        form.add(saveSearchContainer);
        savedSearchListModel = new LoadableDetachableModel<List<InlineMenuItem>>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<InlineMenuItem> load() {
                List<InlineMenuItem> menuItemList = new ArrayList<>();
                List<AvailableFilterType> availableFilters = getSavedFilterList();
                if (CollectionUtils.isEmpty(availableFilters)) {
                    return menuItemList;
                }
                availableFilters.forEach(filter -> {
                    PolyStringType filterLabel = filter.getDisplay() != null ? filter.getDisplay().getLabel() : null;
                    InlineMenuItem item = new InlineMenuItem(Model.of(filterLabel == null ? "" : WebComponentUtil.getTranslatedPolyString(filterLabel))) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public InlineMenuItemAction initAction() {
                            return new InlineMenuItemAction() {

                                private static final long serialVersionUID = 1L;

                                @Override
                                public void onClick(AjaxRequestTarget target) {
                                }
                            };
                        }

                    };
                    item.setId(Integer.parseInt("" + filter.getId()));
                    menuItemList.add(item);
                });
                return menuItemList;
            }
        };

        AjaxButton saveSearchButton = new AjaxButton(ID_SAVE_SEARCH_BUTTON) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                boolean containesCollectionFilter = false;
                for (AbstractSearchItemWrapper item : SearchPanel.this.getModelObject().getItems()) {
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
                SaveSearchPanel<C> panel = new SaveSearchPanel<>(getPageBase().getMainPopupBodyId(),
                        Model.of(SearchPanel.this.getModelObject()),
                        SearchPanel.this.getModelObject().getTypeClass(),
                        getCollectionInstanceDefaultIdentifier()) {

                    @Override
                    protected void saveSearchFilterPerformed(AjaxRequestTarget target) {
                        SearchPanel.this.saveSearchFilterPerformed(target);
                    }
                };
                getPageBase().showMainPopup(panel, target);
            }
        };
        saveSearchButton.add(AttributeAppender.append("title", getPageBase().createStringResource("SearchPanel.saveFilterButton.title")));
        saveSearchButton.setOutputMarkupId(true);
        saveSearchContainer.add(saveSearchButton);

        AjaxLink<Void> savedSearchMenu = new AjaxLink<Void>(ID_SAVED_SEARCH_MENU) {
            private static final long serialVersionUID = 1L;

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
                AjaxLink<String> itemLabel = new AjaxLink<>(ID_SAVED_FILTER_NAME) {
                    private static final long serialVersionUID = 1L;
                    public void onClick(AjaxRequestTarget target) {
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
            }
        };
        getPageBase().showMainPopup(confirmationPanel, target);
    }

    private void saveSearchFilterPerformed(AjaxRequestTarget target) {
        savedSearchListModel.detach();
        target.add(SearchPanel.this.get(ID_FORM));
    }

    private IModel<String> getDeleteFilterConfirmationMessageModel(AvailableFilterType filter) {
        return createStringResource("SearchPanel.removeSingleAvailableFilter",
                    WebComponentUtil.getTranslatedPolyString(GuiDisplayTypeUtil.getLabel(filter.getDisplay())));
    }

    private void deleteFilterPerformed(AvailableFilterType filter, AjaxRequestTarget target) {
        Task task = getPageBase().createSimpleTask(OPERATION_REMOVE_SAVED_FILTER);
        OperationResult result = task.getResult();
        try {
            ObjectDelta<UserType> delta = getPageBase().getPrismContext().deltaFactory().object().createModificationDeleteContainer
                    (UserType.class, getPageBase().getPrincipalFocus().getOid(),
                            filter.asPrismContainerValue().getPath().allExceptLast(),
                            filter.asPrismContainerValue().clone());
            getPageBase().getModelService().executeChanges(MiscUtil.createCollection(delta), null, task, result);
        } catch (Exception e) {
            LOGGER.error("Cannot remove filter from user admin gui configuration: {}", e.getMessage(), e);
            result.recordPartialError("Cannot remove filter from user admin gui configuration: {}", e);

        }
        result.computeStatusIfUnknown();
        getPageBase().showResult(result);
        target.add(getPageBase().getFeedbackPanel());
        target.add(get(ID_FORM));
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
        SearchButtonWithDropdownMenu searchButton = getSearchButton();
        searchButton.setSelectedValue(filter.getSearchMode());
        target.add(searchButton);
        searchPerformed(target);
    }

    private Component getSavedSearchContainer() {
        return get(ID_FORM).get(ID_SAVE_SEARCH_CONTAINER);
    }

    private Component getSavedSearchMenuButton() {
        return get(ID_FORM).get(ID_SAVE_SEARCH_CONTAINER).get(ID_SAVED_SEARCH_MENU);
    }

    private SearchButtonWithDropdownMenu getSearchButton() {
        return (SearchButtonWithDropdownMenu) get(ID_FORM).get(ID_SEARCH_BUTTON_PANEL);
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

    private boolean isPopupWindow() {
        Component parent = SearchPanel.this.getParent();
        while (parent != null) {
            if (parent instanceof Popupable) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    protected void initSearchItemsPanel(RepeatingView searchItemsRepeatingView) {
        BasicSearchFragment basicSearchFragment = new BasicSearchFragment(searchItemsRepeatingView.newChildId(), ID_BASIC_SEARCH_FRAGMENT,
                SearchPanel.this);
        basicSearchFragment.setOutputMarkupId(true);
        basicSearchFragment.add(new VisibleBehaviour(() -> SearchBoxModeType.BASIC.equals(getModelObject().getSearchMode())));
        basicSearchFragment.add(AttributeAppender.append("style", "display: contents !important;"));
        searchItemsRepeatingView.add(basicSearchFragment);

        AdvancedSearchFragment advancedSearchFragment = new AdvancedSearchFragment(searchItemsRepeatingView.newChildId(), ID_ADVANCED_SEARCH_FRAGMENT,
                SearchPanel.this);
        advancedSearchFragment.setOutputMarkupId(true);
        advancedSearchFragment.add(new VisibleBehaviour(() -> SearchBoxModeType.ADVANCED.equals(getModelObject().getSearchMode()) ||
                SearchBoxModeType.AXIOM_QUERY.equals(getModelObject().getSearchMode())));
        advancedSearchFragment.add(AttributeAppender.append("style", "display: flex;"));
        searchItemsRepeatingView.add(advancedSearchFragment);

        FulltextSearchFragment fulltextSearchFragment = new FulltextSearchFragment(searchItemsRepeatingView.newChildId(), ID_FULLTEXT_SEARCH_FRAGMENT,
                SearchPanel.this);
        fulltextSearchFragment.setOutputMarkupId(true);
        fulltextSearchFragment.add(new VisibleBehaviour(() -> getModelObject().isFullTextSearchEnabled()
                && SearchBoxModeType.FULLTEXT.equals(getModelObject().getSearchMode())));
        searchItemsRepeatingView.add(fulltextSearchFragment);

        if (isOidSearchTypePresent()) {
            OidSearchFragment oidSearchFragment = new OidSearchFragment(searchItemsRepeatingView.newChildId(), ID_OID_SEARCH_FRAGMENT,
                    SearchPanel.this);
            oidSearchFragment.setOutputMarkupId(true);
            oidSearchFragment.add(new VisibleBehaviour(() -> getModelObject().getSearchMode().equals(SearchBoxModeType.OID)));
            searchItemsRepeatingView.add(oidSearchFragment);
        }
    }

    private boolean isOidSearchTypePresent() {
        return getSearchConfigurationWrapper().getAllowedModeList().contains(SearchBoxModeType.OID);
    }

    private CompositedIcon getSubmitSearchButtonBuilder() {
        final CompositedIconBuilder builder = new CompositedIconBuilder();
        builder.setBasicIcon(GuiStyleConstants.CLASS_ICON_SEARCH_FLIP, IconCssStyle.IN_ROW_STYLE);
        IconType plusIcon = new IconType();
        plusIcon.setColor("white");
        builder.appendLayerIcon(getIconLabelByModeModel(), plusIcon, LayeredIconCssStyle.BOTTOM_RIGHT_STYLE);
        return builder.build();
    }

    private List<InlineMenuItem> getSearchBoxTypesList() {
        List<InlineMenuItem> searchItems = new ArrayList<>();
        List<SearchBoxModeType> modeList = getSearchConfigurationWrapper().getAllowedModeList();
        for (SearchBoxModeType searchBoxModeType : modeList) {
            InlineMenuItem searchItem = new InlineMenuItem(createStringResource(searchBoxModeType)) {
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new InlineMenuItemAction() {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            searchBoxTypeUpdated(target, searchBoxModeType);
                        }
                    };
                }

                @Override
                public IModel<Boolean> getVisible() {
                    if (SearchBoxModeType.AXIOM_QUERY.equals(searchBoxModeType)) {
                        return Model.of(WebModelServiceUtils.isEnableExperimentalFeature(getPageBase())
                                && getSearchConfigurationWrapper().getAllowedModeList().contains(searchBoxModeType));
                    }
                    return Model.of(getSearchConfigurationWrapper().getAllowedModeList().contains(searchBoxModeType));
                }
            };
            searchItems.add(searchItem);
        }
        return searchItems;
    }

    private void searchBoxTypeUpdated(AjaxRequestTarget target, SearchBoxModeType searchType) {
        if (searchType.equals(getModelObject().getSearchMode())) {
            return;
        }
        if (SearchBoxModeType.OID.equals(searchType) && getModelObject().findOidSearchItemWrapper() != null) {
            getModelObject().findOidSearchItemWrapper().setValue(new SearchValue<>());
        }
        getModelObject().setSearchMode(searchType);
        searchPerformed(target);
        refreshSearchForm(target);
    }

    private void addItemPerformed(List<AbstractSearchItemWrapper> itemList, AjaxRequestTarget target) {
        if (itemList == null) {
            itemList = morePopupModel.getObject();
        }
        itemList.forEach(item -> {
            if (item.isSelected()) {
                item.setVisible(true);
                item.setSelected(false);
            }
        });
        refreshSearchForm(target);
    }

    protected abstract void searchPerformed(AjaxRequestTarget target);

    void refreshSearchForm(AjaxRequestTarget target) {
        displayedSearchItemsModelReset();
        morePopupModel.reset();
        target.add(get(ID_FORM));
        saveSearch(getModelObject(), target);
    }

    protected void saveSearch(Search search, AjaxRequestTarget target) {
    }

    private VisibleEnableBehaviour createVisibleBehaviour(SearchBoxModeType... searchType) {
        return new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return getModelObject() != null && getModelObject().getSearchMode() != null
                        && Arrays.asList(searchType).contains(getModelObject().getSearchMode());
            }
        };
    }

    private IModel<String> getIconLabelByModeModel() {
        return (IModel<String>) () -> {
            if (SearchBoxModeType.ADVANCED.equals(getModelObject().getSearchMode())) {
                return "Adv";
            } else if (SearchBoxModeType.FULLTEXT.equals(getModelObject().getSearchMode())) {
                return "Full";
            } else if (SearchBoxModeType.OID.equals(getModelObject().getSearchMode())) {
                return "Oid";
            } else if (SearchBoxModeType.AXIOM_QUERY.equals(getModelObject().getSearchMode())) {
                return "Query";
            } else {
                return "Basic";
            }
        };
    }

    private RepeatingView getSearchItemsPanel() {
        return (RepeatingView) get(ID_FORM).get(ID_SEARCH_ITEMS_PANEL);
    }

    public <SIP extends AbstractSearchItemPanel, S extends AbstractSearchItemWrapper> SIP createSearchItemPanel(String panelId, IModel<S> searchItemModel) {
        Class<?> panelClass = searchItemModel.getObject().getSearchItemPanelClass();
        Constructor<?> constructor;
        try {
            constructor = panelClass.getConstructor(String.class, IModel.class);
            return (SIP) constructor.newInstance(panelId, searchItemModel);
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new SystemException("Cannot instantiate " + panelClass, e);
        }
    }

    private SearchConfigurationWrapper getSearchConfigurationWrapper() {
        return getModelObject().getSearchConfigurationWrapper();
    }

    private List<AvailableFilterType> getSavedFilterList() {
        ContainerableListPanel listPanel = findParent(ContainerableListPanel.class);
        List<AvailableFilterType> availableFilterList = null;
//        if (listPanel != null) {
//            CompiledObjectCollectionView view = listPanel.getObjectCollectionView();
//            availableFilterList = view != null ? getAvailableFilterList(view.getSearchBoxConfiguration()) : null;
//        } else {
            FocusType principalFocus = getPageBase().getPrincipalFocus();
            GuiObjectListViewType view = WebComponentUtil.getPrincipalUserObjectListView(getPageBase(), principalFocus, getModelObject().getTypeClass(), false, getCollectionInstanceDefaultIdentifier());
            availableFilterList = view != null ? getAvailableFilterList(view.getSearchBoxConfiguration()) : null;
//        }
        return availableFilterList;
    }

    private void applyFilterToBasicMode(List<SearchItemType> items) {
        getModelObject().getItems().forEach(item -> item.setVisible(false));
        getModelObject().setSearchMode(SearchBoxModeType.BASIC);
        items.forEach(this::applyBasicModeSearchItem);
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
            ObjectFilter objectFilter = getPageBase().getQueryConverter().createObjectFilter(getModelObject().getTypeClass(), axiomSearchItem.getFilter());
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

    private void applyBasicModeSearchItem(SearchItemType searchItem) {
        try {
            ObjectFilter filter = getPageBase().getQueryConverter().parseFilter(searchItem.getFilter(), getModelObject().getTypeClass());
            if (searchItem.getPath() == null && filter instanceof ValueFilter && ((ValueFilter) filter).getPath() == null) {
                return;
            }

            AbstractSearchItemWrapper item = null;
            if (filter instanceof ValueFilter) {
                ItemPath path = searchItem.getPath().getItemPath();
                Iterator<AbstractSearchItemWrapper> it = getModelObject().getItems().iterator();
                while (it.hasNext()) {
                    AbstractSearchItemWrapper itemWrapper = it.next();
                    if (itemWrapper instanceof PropertySearchItemWrapper && ((PropertySearchItemWrapper<?>) itemWrapper).getPath().equivalent(path)) {
                        item = itemWrapper;
                        break;
                    }
                }
                List<? extends PrismValue> values = ((ValueFilter) filter).getValues();
                if (values != null && values.size() > 0) {//todo can it be there multiple values?
                    if (TextSearchItemPanel.class.equals(item.getSearchItemPanelClass())) {
                        item.setValue(new SearchValue<String>(values.get(0).getRealValue().toString()));
                    } else {
                        item.setValue(new SearchValue(values.get(0).getRealValue()));
                    }
                }
            } else if (filter instanceof InOidFilter) {
                item = getModelObject().findOidSearchItemWrapper();
                if (((InOidFilter)filter).getOids() != null) {
                    item.setValue(new SearchValue<String>(StringUtils.join(((InOidFilter) filter).getOids(), " ")));
                }
            }

            if (item == null) {
                return;
            }
            item.setVisible(true);

        } catch (SchemaException e) {
            LOG.error("Unable to parse filter {}, {}", searchItem.getFilter(), e.getLocalizedMessage());
        }
    }

    private List<AvailableFilterType> getAvailableFilterList(SearchBoxConfigurationType config) {
        if (config == null || CollectionUtils.isEmpty(config.getAvailableFilter())) {
            return null;
        }
        return config.getAvailableFilter().stream().sorted((filter1, filter2) -> {
            String label1 = WebComponentUtil.getTranslatedPolyString(GuiDisplayTypeUtil.getLabel(filter1.getDisplay()));
            String label2 = WebComponentUtil.getTranslatedPolyString(GuiDisplayTypeUtil.getLabel(filter2.getDisplay()));
            return String.CASE_INSENSITIVE_ORDER.compare(label1, label2);

        }).collect(Collectors.toList());
    }

    class BasicSearchFragment extends Fragment {
        private static final long serialVersionUID = 1L;

        public BasicSearchFragment(String id, String markupId, SearchPanel markupProvider) {
            super(id, markupId, markupProvider);
            initBasicSearchLayout();
        }

        private <T extends Serializable> void initBasicSearchLayout() {

            ListView<AbstractSearchItemWrapper> items = new ListView<AbstractSearchItemWrapper>(ID_ITEMS, basicSearchItemsModel) {

                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<AbstractSearchItemWrapper> item) {
                    AbstractSearchItemPanel searchItemPanel = createSearchItemPanel(ID_ITEM, item.getModel());
                    item.add(searchItemPanel);
                }
            };
            items.add(createVisibleBehaviour(SearchBoxModeType.BASIC));
            add(items);

            SelectableItemListPopoverPanel<AbstractSearchItemWrapper> popoverPanel =
                    new SelectableItemListPopoverPanel<AbstractSearchItemWrapper>(ID_MORE_PROPERTIES_POPOVER, morePopupModel) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected void addItemsPerformed(List<AbstractSearchItemWrapper> itemList, AjaxRequestTarget target) {
                            addItemPerformed(itemList, target);
                        }

                        @Override
                        protected Component getPopoverReferenceComponent() {
                            return BasicSearchFragment.this.getMoreButtonComponent();
                        }

                        @Override
                        protected String getItemName(AbstractSearchItemWrapper item) {
                            return item.getName();
                        }

                        @Override
                        protected String getItemHelp(AbstractSearchItemWrapper item) {
                            return item.getHelp();
                        }

                        @Override
                        protected IModel<String> getPopoverTitleModel() {
                            return createStringResource("SearchPanel.properties");
                        }
                    };
            add(popoverPanel);

            AjaxLink<Void> more = new AjaxLink<Void>(ID_MORE) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    popoverPanel.togglePopover(target);
                }
            };
            more.add(new VisibleBehaviour(() -> {
                return CollectionUtils.isNotEmpty(morePopupModel.getObject());
            }));
            more.setOutputMarkupId(true);
            add(more);
        }

        private Component getMoreButtonComponent() {
            return BasicSearchFragment.this.get(ID_MORE);
        }

        private void closeMorePopoverPerformed(AjaxRequestTarget target) {
            String popoverId = get(ID_MORE_PROPERTIES_POPOVER).getMarkupId();
            target.appendJavaScript("$('#" + popoverId + "').toggle();");
        }
    }

    class AdvancedSearchFragment extends Fragment {
        private static final long serialVersionUID = 1L;

        public AdvancedSearchFragment(String id, String markupId, SearchPanel markupProvider) {
            super(id, markupId, markupProvider);
            initAdvancedSearchLayout();
        }

        private <S extends SearchItem, T extends Serializable> void initAdvancedSearchLayout() {
            AjaxButton debug = new AjaxButton(ID_DEBUG, createStringResource("SearchPanel.debug")) {

                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    debugPerformed();
                }
            };
            debug.add(new VisibleBehaviour(() -> isQueryPlaygroundAccessible()));
            add(debug);

            WebMarkupContainer advancedGroup = new WebMarkupContainer(ID_ADVANCED_GROUP);
            advancedGroup.add(createVisibleBehaviour(SearchBoxModeType.ADVANCED, SearchBoxModeType.AXIOM_QUERY));
            advancedGroup.add(AttributeAppender.append("class", createAdvancedGroupStyle()));
            advancedGroup.setOutputMarkupId(true);
            add(advancedGroup);

            Label advancedCheck = new Label(ID_ADVANCED_CHECK);
            advancedCheck.add(AttributeAppender.append("class", createAdvancedGroupLabelStyle()));
            advancedCheck.setOutputMarkupId(true);
            advancedGroup.add(advancedCheck);

            TextArea<?> advancedArea = new TextArea<>(ID_ADVANCED_AREA, new PropertyModel<>(getModel(), com.evolveum.midpoint.web.component.search.Search.F_ADVANCED_QUERY));
            advancedArea.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
            advancedArea.add(AttributeAppender.append("placeholder", getPageBase().createStringResource("SearchPanel.insertFilterXml")));
            advancedArea.add(createVisibleBehaviour(SearchBoxModeType.ADVANCED));
            advancedGroup.add(advancedArea);

            TextField<String> queryDslField = new TextField<>(ID_AXIOM_QUERY_FIELD,
                    new PropertyModel<>(getModel(), com.evolveum.midpoint.web.component.search.Search.F_DSL_QUERY));
            queryDslField.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
            queryDslField.add(AttributeAppender.append("placeholder", getPageBase().createStringResource("SearchPanel.insertAxiomQuery")));
            queryDslField.add(createVisibleBehaviour(SearchBoxModeType.AXIOM_QUERY));
            advancedGroup.add(queryDslField);

            WebMarkupContainer advancedErrorGroup = new WebMarkupContainer(ID_ADVANCED_ERROR_GROUP);
            advancedErrorGroup.setOutputMarkupId(true);
            advancedGroup.add(advancedErrorGroup);
            Label advancedError = new Label(ID_ADVANCED_ERROR,
                    new PropertyModel<String>(getModel(), com.evolveum.midpoint.web.component.search.Search.F_ADVANCED_ERROR));
            advancedError.add(new VisibleEnableBehaviour() {

                private static final long serialVersionUID = 1L;

                @Override
                public boolean isVisible() {
                    Search search = getModelObject();

                    if (!isAdvancedMode()) {
                        return false;
                    }

                    return StringUtils.isNotEmpty(search.getAdvancedError());
                }
            });
            advancedErrorGroup.add(advancedError);
        }

        private boolean isAdvancedMode() {
            return SearchBoxModeType.ADVANCED.equals(getModelObject().getSearchMode())
                    || SearchBoxModeType.AXIOM_QUERY.equals(getModelObject().getSearchMode());
        }
        private boolean isQueryPlaygroundAccessible() {
            return SecurityUtils.isPageAuthorized(PageRepositoryQuery.class) && SearchBoxModeType.ADVANCED.equals(getModelObject().getSearchMode())
                    && ObjectType.class.isAssignableFrom(getModelObject().getTypeClass());
        }

        private void debugPerformed() {
            Search search = getModelObject();
            PageRepositoryQuery pageQuery;
            if (search != null) {
                ObjectTypes type = search.getTypeClass() != null ? ObjectTypes.getObjectType(search.getTypeClass().getSimpleName()) : null;
                QName typeName = type != null ? type.getTypeQName() : null;
                String inner = search.getAdvancedQuery();
                if (StringUtils.isNotBlank(inner)) {
                    inner = "\n" + inner + "\n";
                } else if (inner == null) {
                    inner = "";
                }
                pageQuery = new PageRepositoryQuery(typeName, "<query>" + inner + "</query>");
            } else {
                pageQuery = new PageRepositoryQuery();
            }
            SearchPanel.this.setResponsePage(pageQuery);
        }

        private IModel<String> createAdvancedGroupLabelStyle() {
            return new IModel<String>() {

                private static final long serialVersionUID = 1L;

                @Override
                public String getObject() {
                    Search search = getModelObject();

                    return StringUtils.isEmpty(search.getAdvancedError()) ? "fa-check-circle-o" : "fa-exclamation-triangle";
                }
            };
        }

        private IModel<String> createAdvancedGroupStyle() {
            return new IModel<String>() {

                private static final long serialVersionUID = 1L;

                @Override
                public String getObject() {
                    Search search = getModelObject();
                    return StringUtils.isEmpty(search.getAdvancedError()) ? "has-success" : "has-error";
                }
            };
        }

        private void updateAdvancedArea(Component area, AjaxRequestTarget target) {
            Search search = getModelObject();
            PrismContext ctx = getPageBase().getPrismContext();

            search.isAdvancedQueryValid(ctx);

            target.prependJavaScript("storeTextAreaSize('" + area.getMarkupId() + "');");
            target.appendJavaScript("restoreTextAreaSize('" + area.getMarkupId() + "');");

            target.add(
                    SearchPanel.this.get(createComponentPath(ID_FORM, ID_SEARCH_ITEMS_PANEL, "2", ID_ADVANCED_GROUP)),
                    SearchPanel.this.get(createComponentPath(ID_FORM, ID_SEARCH_BUTTON_PANEL)));
        }

        private void updateQueryDSLArea(Component child, Component parent, AjaxRequestTarget target) {
            Search search = getModelObject();
            PrismContext ctx = getPageBase().getPrismContext();

            search.isAdvancedQueryValid(ctx);

            target.appendJavaScript("$('#" + child.getMarkupId() + "').updateParentClass('fa-check-circle-o', 'has-success',"
                    + " '" + parent.getMarkupId() + "', 'fa-exclamation-triangle', 'has-error');");

            target.add(
                    SearchPanel.this.get(createComponentPath(ID_FORM, ID_SEARCH_ITEMS_PANEL, "2", ID_ADVANCED_GROUP, ID_ADVANCED_CHECK)),
                    SearchPanel.this.get(createComponentPath(ID_FORM, ID_SEARCH_ITEMS_PANEL, "2", ID_ADVANCED_GROUP, ID_ADVANCED_ERROR_GROUP)),
                    SearchPanel.this.get(createComponentPath(ID_FORM, ID_SEARCH_BUTTON_PANEL)));
        }

    }

    class FulltextSearchFragment extends Fragment {
        private static final long serialVersionUID = 1L;

        public FulltextSearchFragment(String id, String markupId, SearchPanel markupProvider) {
            super(id, markupId, markupProvider);
            initFulltextLayout();
        }

        private void initFulltextLayout() {
            WebMarkupContainer fullTextContainer = new WebMarkupContainer(ID_FULL_TEXT_CONTAINER);
            fullTextContainer.setOutputMarkupId(true);
            add(fullTextContainer);

            TextField<String> fullTextInput = new TextField<>(ID_FULL_TEXT_FIELD,
                    new PropertyModel<>(getModel(), Search.F_FULL_TEXT));

            fullTextInput.add(new AjaxFormComponentUpdatingBehavior("blur") {

                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                }
            });
            fullTextInput.add(WebComponentUtil.getSubmitOnEnterKeyDownBehavior("searchSimple"));
            fullTextInput.setOutputMarkupId(true);
            fullTextInput.add(new AttributeAppender("placeholder",
                    createStringResource("SearchPanel.fullTextSearch")));
            fullTextContainer.add(fullTextInput);

        }
    }

    class OidSearchFragment extends Fragment {
        private static final long serialVersionUID = 1L;

        public OidSearchFragment(String id, String markupId, SearchPanel markupProvider) {
            super(id, markupId, markupProvider);
            initOidSearchLayout();
        }

        private void initOidSearchLayout() {
            OidSearchItemWrapper item = getModelObject().findOidSearchItemWrapper();
            if (item != null) {
                add(createSearchItemPanel(ID_OID_ITEM, Model.of(item)));
            }
        }
    }
}
