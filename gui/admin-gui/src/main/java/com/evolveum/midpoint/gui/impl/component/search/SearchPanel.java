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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.Containerable;

import com.evolveum.midpoint.web.component.search.SearchValue;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxChannel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
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
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.menu.cog.MenuLinkPanel;
import com.evolveum.midpoint.web.component.search.SearchItem;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.PageRepositoryQuery;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;

public abstract class SearchPanel<C extends Containerable> extends BasePanel<Search<C>> {

    private static final long serialVersionUID = 1L;

    private static final String ID_FORM = "form";
    private static final String ID_SEARCH_ITEMS_PANEL = "searchItemsPanel";
    private static final String ID_SEARCH_CONTAINER = "searchContainer";
    private static final String ID_SUBMIT_SEARCH_BUTTON = "submitSearchButton";
    private static final String ID_SEARCH_TYPES_MENU = "searchTypesMenu";
    private static final String ID_SEARCH_TYPE_ITEMS = "searchTypeItems";
    private static final String ID_SEARCH_TYPE = "searchType";
    private static final String ID_SAVE_SEARCH_CONTAINER = "saveSearchContainer";
    private static final String ID_SAVE_SEARCH_BUTTON = "saveSearchButton";
    private static final String ID_SAVED_SEARCH_MENU = "savedSearchMenu";
    private static final String ID_SAVED_SEARCH_ITEMS = "savedSearchItems";
    private static final String ID_SAVED_SEARCH_ITEM = "savedSearchItem";
    private static final String ID_BASIC_SEARCH_FRAGMENT = "basicSearchFragment";
    private static final String ID_ADVANCED_SEARCH_FRAGMENT = "advancedSearchFragment";
    private static final String ID_FULLTEXT_SEARCH_FRAGMENT = "fulltextSearchFragment";
    private static final String ID_OID_SEARCH_FRAGMENT = "oidSearchFragment";
    private static final String ID_ITEMS = "items";
    private static final String ID_ITEM = "item";
    private static final String ID_MORE_GROUP = "moreGroup";
    private static final String ID_MORE = "more";
    private static final String ID_POPOVER = "popover";
    private static final String ID_ADD_TEXT = "addText";
    private static final String ID_ADD = "add";
    private static final String ID_CLOSE = "close";
    private static final String ID_PROPERTIES = "properties";
    private static final String ID_CHECK = "check";
    private static final String ID_HELP = "help";
    private static final String ID_PROP_NAME = "propName";
    private static final String ID_PROP_LINK = "propLink";
    private static final String ID_PROP_LIST = "propList";
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

        WebMarkupContainer searchContainer = new WebMarkupContainer(ID_SEARCH_CONTAINER);
        searchContainer.setOutputMarkupId(true);
        form.add(searchContainer);

        AjaxCompositedIconSubmitButton submitSearchButton = new AjaxCompositedIconSubmitButton(ID_SUBMIT_SEARCH_BUTTON, getSubmitSearchButtonBuilder(),
                getPageBase().createStringResource("SearchPanel.search")) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(form);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                searchPerformed(target);
            }
        };

        IModel<String> buttonRightPaddingModel = () -> {
            boolean isLongButton = SearchBoxModeType.BASIC.equals(getSearchConfigurationWrapper().getDefaultSearchBoxMode())
                    || SearchBoxModeType.AXIOM_QUERY.equals(getSearchConfigurationWrapper().getDefaultSearchBoxMode());
            String style = "padding-right: " + (isLongButton ? "23" : "16") + "px;";
            if (getSearchConfigurationWrapper().getAllowedModeList().size() == 1) {
                style = style + "border-top-right-radius: 3px; border-bottom-right-radius: 3px;";
            }
            return style;
        };
        submitSearchButton.add(AttributeAppender.append("style", buttonRightPaddingModel));

        submitSearchButton.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                Search search = getModelObject();
                if (SearchBoxModeType.ADVANCED.equals(getModelObject().getSearchMode())
                        || SearchBoxModeType.AXIOM_QUERY.equals(getModelObject().getSearchMode())) {
                    PrismContext ctx = getPageBase().getPrismContext();
                    return search.isAdvancedQueryValid(ctx);
                }
                if (SearchBoxModeType.FULLTEXT.equals(getModelObject().getSearchMode())) {
                    return search.isFullTextSearchEnabled();
                }
                if (SearchBoxModeType.BASIC.equals(getModelObject().getSearchMode())) {
                    return CollectionUtils.isNotEmpty(getModelObject().getItems());
                }
                return true;
            }
        });
        submitSearchButton.setOutputMarkupId(true);
        searchContainer.add(submitSearchButton);
        form.setDefaultButton(submitSearchButton);

        WebMarkupContainer dropdownButton = new WebMarkupContainer(ID_SEARCH_TYPES_MENU);
        dropdownButton.add(new VisibleBehaviour(() -> getSearchConfigurationWrapper().getAllowedModeList().size() != 1));
        searchContainer.add(dropdownButton);

        ListView<InlineMenuItem> li = new ListView<InlineMenuItem>(ID_SEARCH_TYPE_ITEMS, getSearchBoxTypesList()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<InlineMenuItem> item) {
                WebMarkupContainer menuItemBody = new MenuLinkPanel(ID_SEARCH_TYPE, item.getModel());
                menuItemBody.setRenderBodyOnly(true);
                item.add(menuItemBody);
                menuItemBody.add(new VisibleEnableBehaviour() {
                    @Override
                    public boolean isVisible() {
                        return Boolean.TRUE.equals(item.getModelObject().getVisible().getObject());
                    }
                });
            }
        };
        searchContainer.add(li);

        WebMarkupContainer saveSearchContainer = new WebMarkupContainer(ID_SAVE_SEARCH_CONTAINER);
        saveSearchContainer.setOutputMarkupId(true);
        form.add(saveSearchContainer);
        AjaxButton saveSearchButton = new AjaxButton(ID_SAVE_SEARCH_BUTTON) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                SaveSearchPanel<C> panel = new SaveSearchPanel<>(getPageBase().getMainPopupBodyId(),
                        Model.of(SearchPanel.this.getModelObject().createObjectQuery(getPageBase())), SearchPanel.this.getModelObject().getTypeClass());
                getPageBase().showMainPopup(panel, target);
            }
        };
        saveSearchButton.setOutputMarkupId(true);
        saveSearchContainer.add(saveSearchButton);

        WebMarkupContainer savedSearchMenu = new WebMarkupContainer(ID_SAVED_SEARCH_MENU);
        saveSearchContainer.add(savedSearchMenu);

        ListView<InlineMenuItem> savedSearchItems = new ListView<InlineMenuItem>(ID_SAVED_SEARCH_ITEMS, getSavedSearchList()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<InlineMenuItem> item) {
                WebMarkupContainer menuItemBody = new MenuLinkPanel(ID_SAVED_SEARCH_ITEM, item.getModel());
                menuItemBody.setRenderBodyOnly(true);
                item.add(menuItemBody);
                menuItemBody.add(new VisibleEnableBehaviour() {
                    @Override
                    public boolean isVisible() {
                        return Boolean.TRUE.equals(item.getModelObject().getVisible().getObject());
                    }
                });
            }
        };
        saveSearchContainer.add(savedSearchItems);

    }

    protected void initSearchItemsPanel(RepeatingView searchItemsRepeatingView) {
        BasicSearchFragment basicSearchFragment = new BasicSearchFragment(searchItemsRepeatingView.newChildId(), ID_BASIC_SEARCH_FRAGMENT,
                SearchPanel.this);
        basicSearchFragment.setOutputMarkupId(true);
        basicSearchFragment.add(new VisibleBehaviour(() -> SearchBoxModeType.BASIC.equals(getModelObject().getSearchMode())));
        basicSearchFragment.add(AttributeAppender.append("style", "display: flex; flex-direction: row; flex-wrap: wrap;"));
        searchItemsRepeatingView.add(basicSearchFragment);

        AdvancedSearchFragment advancedSearchFragment = new AdvancedSearchFragment(searchItemsRepeatingView.newChildId(), ID_ADVANCED_SEARCH_FRAGMENT,
                SearchPanel.this);
        advancedSearchFragment.setOutputMarkupId(true);
        advancedSearchFragment.add(new VisibleBehaviour(() -> SearchBoxModeType.ADVANCED.equals(getModelObject().getSearchMode()) ||
                SearchBoxModeType.AXIOM_QUERY.equals(getModelObject().getSearchMode())));
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
        builder.setBasicIcon(GuiStyleConstants.CLASS_ICON_SEARCH, IconCssStyle.IN_ROW_STYLE);
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
        if (SearchBoxModeType.OID.equals(searchType)) {
            getModelObject().findOidSearchItemWrapper().setValue(new SearchValue<>());
        }
        getModelObject().setSearchMode(searchType);
        searchPerformed(target);
        refreshSearchForm(target);
    }

    private void addOneItemPerformed(AbstractSearchItemWrapper searchItem, AjaxRequestTarget target) {
        searchItem.setVisible(true);
        refreshSearchForm(target);
    }

    private void addItemPerformed(AjaxRequestTarget target) {
        getModelObject().getItems().forEach(item -> {
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

    private List<InlineMenuItem> getSavedSearchList() {
        ContainerableListPanel listPanel = findParent(ContainerableListPanel.class);
        List<InlineMenuItem> savedSearchItems = new ArrayList<>();
        List<SearchItemType> searchItems = null;
        if (listPanel != null) {
            CompiledObjectCollectionView view = listPanel.getObjectCollectionView();
            searchItems = view != null ? getSearchItemList(view.getSearchBoxConfiguration()) : null;
        } else {
            FocusType principalFocus = getPageBase().getPrincipalFocus();
            GuiObjectListViewType view = WebComponentUtil.getPrincipalUserObjectListView(getPageBase(), principalFocus, getModelObject().getTypeClass(), false);
            searchItems = view != null ? getSearchItemList(view.getSearchBoxConfiguration()) : null;
        }
        if (searchItems != null) {
            searchItems.forEach(item -> {
                InlineMenuItem searchItem = new InlineMenuItem(Model.of(WebComponentUtil.getTranslatedPolyString(item.getDisplayName()))) {
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
                savedSearchItems.add(searchItem);
            });
        }
        return savedSearchItems;
    }

    private List<SearchItemType> getSearchItemList(SearchBoxConfigurationType config) {
        if (config == null) {
            return null;
        }
        SearchItemsType items = config.getSearchItems();
        if (items == null) {
            return null;
        }
        return items.getSearchItem();
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

            WebMarkupContainer moreGroup = new WebMarkupContainer(ID_MORE_GROUP);
            moreGroup.add(createMoreGroupVisibleBehaviour());
            add(moreGroup);

            AjaxLink<Void> more = new AjaxLink<Void>(ID_MORE) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
//                    resetMoreDialogModel();
                    Component popover = BasicSearchFragment.this.get(createComponentPath(ID_POPOVER));
                    Component button = BasicSearchFragment.this.get(createComponentPath(ID_MORE_GROUP, ID_MORE));
                    togglePopover(target, button, popover, 14);
                }
            };
            more.add(new VisibleEnableBehaviour() {

                private static final long serialVersionUID = 1L;

                @Override
                public boolean isVisible() {
                    return CollectionUtils.isNotEmpty(morePopupModel.getObject());
                }
            });
            more.setOutputMarkupId(true);
            moreGroup.add(more);

            initPopover();
        }

        private VisibleBehaviour createMoreGroupVisibleBehaviour() {
            return new VisibleBehaviour(() -> getSearchConfigurationWrapper().isAllowToConfigureSearchItems() &&
                    SearchBoxModeType.BASIC.equals(getModelObject().getSearchMode()));
        }

        private void initPopover() {
            WebMarkupContainer popover = new WebMarkupContainer(ID_POPOVER);
            popover.setOutputMarkupId(true);
            add(popover);

            final WebMarkupContainer propList = new WebMarkupContainer(ID_PROP_LIST);
            propList.setOutputMarkupId(true);
            popover.add(propList);

            IModel<String> searchPropertyModel = Model.of("");
            TextField<?> addText = new TextField<>(ID_ADD_TEXT, searchPropertyModel);
            addText.add(WebComponentUtil.preventSubmitOnEnterKeyDownBehavior());

            popover.add(addText);
            addText.add(new AjaxFormComponentUpdatingBehavior("keyup") {

                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    target.add(propList);
                }
            });
            popover.add(addText);

            ListView<AbstractSearchItemWrapper> properties = new ListView<>(ID_PROPERTIES, morePopupModel) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(final ListItem<AbstractSearchItemWrapper> item) {
                    CheckBox check = new CheckBox(ID_CHECK,
                            new PropertyModel<>(item.getModel(), AbstractSearchItemWrapper.F_SELECTED));
                    check.add(new AjaxFormComponentUpdatingBehavior("change") {

                        private static final long serialVersionUID = 1L;

                        @Override
                        protected void onUpdate(AjaxRequestTarget target) {
                            //nothing, just update model.
                        }
                    });
                    item.add(check);

                    AjaxLink<Void> propLink = new AjaxLink<Void>(ID_PROP_LINK) {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            addOneItemPerformed(item.getModelObject(), target);
                        }
                    };
                    item.add(propLink);

                    Label name = new Label(ID_PROP_NAME, new PropertyModel<>(item.getModel(), AbstractSearchItemWrapper.F_NAME));
                    name.setRenderBodyOnly(true);
                    propLink.add(name);

                    Label help = new Label(ID_HELP);
                    IModel<String> helpModel = new PropertyModel<>(item.getModel(), AbstractSearchItemWrapper.F_HELP);
                    help.add(AttributeModifier.replace("title", createStringResource(helpModel.getObject() != null ? helpModel.getObject() : "")));
                    help.add(new InfoTooltipBehavior() {
                        @Override
                        public String getDataPlacement() {
                            return "left";
                        }
                    });
                    help.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(helpModel.getObject())));
                    item.add(help);

                    item.add(new VisibleBehaviour(() -> isPropertyItemVisible(item.getModelObject(), searchPropertyModel.getObject())));
                }

                private boolean isPropertyItemVisible(AbstractSearchItemWrapper searchItem, String propertySearchText) {
                    String searchItemName = searchItem.getName();
                    String name = searchItemName != null ? createStringResource(searchItemName).getString() : "";
                    return StringUtils.isEmpty(propertySearchText)
                                    || name.toLowerCase().contains(propertySearchText.toLowerCase());
                }
            };
            propList.add(properties);

            AjaxButton add = new AjaxButton(ID_ADD, createStringResource("SearchPanel.add")) {

                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    addItemPerformed(target);
                }
            };
            popover.add(add);

            AjaxButton close = new AjaxButton(ID_CLOSE, createStringResource("SearchPanel.close")) {

                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    closeMorePopoverPerformed(target);
                }
            };
            popover.add(close);
        }

        private void closeMorePopoverPerformed(AjaxRequestTarget target) {
            String popoverId = get(ID_POPOVER).getMarkupId();
            target.appendJavaScript("$('#" + popoverId + "').toggle();");
        }

        public void togglePopover(AjaxRequestTarget target, Component button, Component popover, int paddingRight) {
            target.appendJavaScript("toggleSearchPopover('"
                    + button.getMarkupId() + "','"
                    + popover.getMarkupId() + "',"
                    + paddingRight + ");");
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
            advancedArea.add(new AjaxFormComponentUpdatingBehavior("keyup") {

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    updateAdvancedArea(advancedArea, target);
                }

                @Override
                protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                    super.updateAjaxAttributes(attributes);

                    attributes.setThrottlingSettings(
                            new ThrottlingSettings(ID_ADVANCED_AREA, Duration.ofMillis(500), true));
                }
            });
            advancedArea.add(AttributeAppender.append("placeholder", getPageBase().createStringResource("SearchPanel.insertFilterXml")));
            advancedArea.add(createVisibleBehaviour(SearchBoxModeType.ADVANCED));
            advancedGroup.add(advancedArea);

            TextField<String> queryDslField = new TextField<>(ID_AXIOM_QUERY_FIELD,
                    new PropertyModel<>(getModel(), com.evolveum.midpoint.web.component.search.Search.F_DSL_QUERY));
            queryDslField.add(new AjaxFormComponentUpdatingBehavior("keyup") {

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    updateQueryDSLArea(advancedCheck, advancedGroup, target);
                }

                @Override
                protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                    super.updateAjaxAttributes(attributes);

                    attributes.setThrottlingSettings(
                            new ThrottlingSettings(ID_AXIOM_QUERY_FIELD, Duration.ofMillis(500), true));
                    attributes.setChannel(new AjaxChannel("Drop", AjaxChannel.Type.DROP));
                }
            });
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
            return SecurityUtils.isPageAuthorized(PageRepositoryQuery.class);
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
                    get(createComponentPath(ID_FORM, ID_ADVANCED_GROUP)),
                    get(createComponentPath(ID_FORM, ID_SEARCH_CONTAINER)));
        }

        private void updateQueryDSLArea(Component child, Component parent, AjaxRequestTarget target) {
            Search search = getModelObject();
            PrismContext ctx = getPageBase().getPrismContext();

            search.isAdvancedQueryValid(ctx);

            target.appendJavaScript("$('#" + child.getMarkupId() + "').updateParentClass('fa-check-circle-o', 'has-success',"
                    + " '" + parent.getMarkupId() + "', 'fa-exclamation-triangle', 'has-error');");

            target.add(
                    get(createComponentPath(ID_FORM, ID_ADVANCED_GROUP, ID_ADVANCED_CHECK)),
                    get(createComponentPath(ID_FORM, ID_ADVANCED_GROUP, ID_ADVANCED_ERROR_GROUP)),
                    get(createComponentPath(ID_FORM, ID_SEARCH_CONTAINER)));
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
            add(createSearchItemPanel(ID_OID_ITEM, Model.of(item)));
        }
    }
}
