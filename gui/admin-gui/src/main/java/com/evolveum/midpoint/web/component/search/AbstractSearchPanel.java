/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
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
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import com.evolveum.midpoint.web.page.admin.configuration.PageRepositoryQuery;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxModeType;

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

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author honchar
 */
public abstract class AbstractSearchPanel<C extends Containerable> extends BasePanel<Search<C>> {
    private static final long serialVersionUID = 1L;

    private static final String ID_FORM = "form";
    private static final String ID_SEARCH_ITEMS_PANEL = "searchItemsPanel";
    private static final String ID_SEARCH_CONTAINER = "searchContainer";
    private static final String ID_SUBMIT_SEARCH_BUTTON = "submitSearchButton";
    private static final String ID_SEARCH_TYPES_MENU = "searchTypesMenu";
    private static final String ID_SEARCH_TYPE_ITEMS = "searchTypeItems";
    private static final String ID_SEARCH_TYPE = "searchType";
    private static final String ID_BASIC_SEARCH_FRAGMENT = "basicSearchFragment";
    private static final String ID_ADVANCED_SEARCH_FRAGMENT = "advancedSearchFragment";
    private static final String ID_FULLTEXT_SEARCH_FRAGMENT = "fulltextSearchFragment";
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

    private static final Trace LOG = TraceManager.getTrace(AbstractSearchPanel.class);

    private LoadableModel<MoreDialogDto> moreDialogModel;

    public AbstractSearchPanel(String id, IModel<Search<C>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initMoreDialogModel();
        initLayout();
    }

    private void initMoreDialogModel() {
        moreDialogModel = new LoadableModel<MoreDialogDto>(false) {

            private static final long serialVersionUID = 1L;

            @Override
            protected MoreDialogDto load() {
                MoreDialogDto dto = new MoreDialogDto();
                dto.setProperties(createPropertiesList());

                return dto;
            }

            @Override
            public MoreDialogDto getObject() {
                if (AbstractSearchPanel.this.getModelObject() != null && AbstractSearchPanel.this.getModelObject().isTypeChanged()) {
                    reset();
                }
                return super.getObject();
            }
        };
    }

    private List<AbstractSearchItemDefinition> createPropertiesList() {
        List<AbstractSearchItemDefinition> list = new ArrayList<>();

        List<ItemPath> specialItemPaths = new ArrayList<>();
        getModelObject().getSpecialItems().stream().filter(specItem -> (specItem instanceof PropertySearchItem))
                .forEach(specItem -> specialItemPaths.add(((PropertySearchItem<?>) specItem).getPath()));

        Search search = getModelObject();
        search.getAllDefinitions().stream().filter((Predicate<AbstractSearchItemDefinition>) def ->
                (def instanceof PropertySearchItemDefinition) &&
                !ItemPathCollectionsUtil.containsEquivalent(specialItemPaths, ((PropertySearchItemDefinition)def).getPath()))
                .forEach((Consumer<AbstractSearchItemDefinition>) def -> list.add(def));
        Collections.sort(list);

        return list;
    }

    private <S extends SearchItem, T extends Serializable> void initLayout() {
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
            boolean isLongButton = SearchBoxModeType.BASIC.equals(getModelObject().getSearchType())
                    || SearchBoxModeType.AXIOM_QUERY.equals(getModelObject().getSearchType());
            String style = "padding-right: " + (isLongButton ? "23" : "16") + "px;";
            if (getModelObject().getAllowedSearchType().size() == 1) {
                style = style + "border-top-right-radius: 3px; border-bottom-right-radius: 3px;";
            }
            return style;
        };
        submitSearchButton.add(AttributeAppender.append("style", buttonRightPaddingModel));

        submitSearchButton.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                if (SearchBoxModeType.ADVANCED.equals(getModelObject().getSearchType())
                        || SearchBoxModeType.AXIOM_QUERY.equals(getModelObject().getSearchType())) {
                    Search search = getModelObject();
                    PrismContext ctx = getPageBase().getPrismContext();
                    return search.isAdvancedQueryValid(ctx);
                }
                return true;
            }

            @Override
            public boolean isVisible() {
                Search search = getModelObject();
                if (search.getAllowedSearchType().size() == 1
                        && SearchBoxModeType.BASIC.equals(search.getAllowedSearchType().get(0))) {
                    return !search.getItems().isEmpty(); //|| !search.getAvailableDefinitions().isEmpty();
                }
                return true;
            }
        });
        submitSearchButton.setOutputMarkupId(true);
        searchContainer.add(submitSearchButton);
        form.setDefaultButton(submitSearchButton);

        WebMarkupContainer dropdownButton = new WebMarkupContainer(ID_SEARCH_TYPES_MENU);
        dropdownButton.add(new VisibleBehaviour(() -> getModelObject().getAllowedSearchType().size() != 1));
        searchContainer.add(dropdownButton);

        ListView<InlineMenuItem> li = new ListView<InlineMenuItem>(ID_SEARCH_TYPE_ITEMS, Model.ofList(getSearchBoxTypesList())) {

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

    }

    protected void initSearchItemsPanel(RepeatingView searchItemsRepeatingView) {
        BasicSearchFragment basicSearchFragment = new BasicSearchFragment(searchItemsRepeatingView.newChildId(), ID_BASIC_SEARCH_FRAGMENT,
                AbstractSearchPanel.this);
        basicSearchFragment.setOutputMarkupId(true);
        basicSearchFragment.add(new VisibleBehaviour(() -> getModelObject().isBasicSearchMode()));
        searchItemsRepeatingView.add(basicSearchFragment);

        AdvancedSearchFragment advancedSearchFragment = new AdvancedSearchFragment(searchItemsRepeatingView.newChildId(), ID_ADVANCED_SEARCH_FRAGMENT,
                AbstractSearchPanel.this);
        advancedSearchFragment.setOutputMarkupId(true);
        advancedSearchFragment.add(new VisibleBehaviour(() -> getModelObject().isAdvancedSearchMode()));
        searchItemsRepeatingView.add(advancedSearchFragment);

        FulltextSearchFragment fulltextSearchFragment = new FulltextSearchFragment(searchItemsRepeatingView.newChildId(), ID_FULLTEXT_SEARCH_FRAGMENT,
                AbstractSearchPanel.this);
        fulltextSearchFragment.setOutputMarkupId(true);
        fulltextSearchFragment.add(new VisibleBehaviour(() -> getModelObject().isFullTextSearchEnabled()
                && getModelObject().getSearchType().equals(SearchBoxModeType.FULLTEXT)));
        searchItemsRepeatingView.add(fulltextSearchFragment);
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
        for (SearchBoxModeType searchBoxModeType : SearchBoxModeType.values()) {
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
                    return Model.of(getModelObject().isAllowedSearchMode(searchBoxModeType));
                }
            };
            searchItems.add(searchItem);
        }
        return searchItems;
    }

    private void searchBoxTypeUpdated(AjaxRequestTarget target, SearchBoxModeType searchType) {
        if (searchType.equals(getModelObject().getSearchType())) {
            return;
        }
        SearchBoxModeType oldMode = getModelObject().getSearchType();
        getModelObject().setSearchType(searchType);
        if (getModelObject().isTypeChanged() && SearchBoxModeType.OID.equals(oldMode)) {
            getModelObject().setOid(null);
            searchPerformed(target);
            resetMoreDialogModel();
        }

        refreshSearchForm(target);
    }

    private void addOneItemPerformed(AbstractSearchItemDefinition searchItemDef, AjaxRequestTarget target) {
        searchItemDef.setSearchItemDisplayed(true);
//        Search search = getModelObject();
//        SearchItem item = search.addItem(property);
//        item.setEditWhenVisible(true);

        refreshSearchForm(target);
    }

    private void addItemPerformed(AjaxRequestTarget target) {
        Search search = getModelObject();

        MoreDialogDto dto = moreDialogModel.getObject();
        for (AbstractSearchItemDefinition searchItem : dto.getProperties()) {
            if (!searchItem.isSelected()) {
                continue;
            }
            searchItem.setSearchItemDisplayed(true);
//            search.addItem(searchItem);
            searchItem.setSelected(false);
        }
        refreshSearchForm(target);
    }

    private void closeMorePopoverPerformed(AjaxRequestTarget target) {
        String popoverId = get(ID_POPOVER).getMarkupId();
        target.appendJavaScript("$('#" + popoverId + "').toggle();");
    }

    protected abstract void searchPerformed(AjaxRequestTarget target);

    void refreshSearchForm(AjaxRequestTarget target) {
        target.add(get(ID_FORM));
        saveSearch(getModelObject(), target);
    }

    protected void saveSearch(Search search, AjaxRequestTarget target) {
    }

    private VisibleEnableBehaviour createVisibleBehaviour(SearchBoxModeType ... searchType) {
        return new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return getModelObject() != null && getModelObject().getSearchType() != null
                        && Arrays.asList(searchType).contains(getModelObject().getSearchType());
            }
        };
    }

    private IModel<String> getIconLabelByModeModel() {
        return (IModel<String>) () -> {
            if (SearchBoxModeType.ADVANCED.equals(getModelObject().getSearchType())) {
                return "Adv";
            } else if (SearchBoxModeType.FULLTEXT.equals(getModelObject().getSearchType())) {
                return "Full";
            } else if (SearchBoxModeType.OID.equals(getModelObject().getSearchType())) {
                return "Oid";
            } else if (SearchBoxModeType.AXIOM_QUERY.equals(getModelObject().getSearchType())) {
                return "Query";
            } else {
                return "Basic";
            }
        };
    }

    public void resetMoreDialogModel() {
        moreDialogModel.reset();
    }

    public <SIP extends SearchItemPanel, S extends SearchItem> SIP createSearchItemPanel(String panelId, IModel<S> searchItemModel){
        Class<?> panelClass = searchItemModel.getObject().getSearchItemPanelClass();
        Constructor<?> constructor;
        try {
            constructor = panelClass.getConstructor(String.class, IModel.class);
            return (SIP) constructor.newInstance(panelId, searchItemModel);
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new SystemException("Cannot instantiate " + panelClass, e);
        }
    }

    class BasicSearchFragment extends Fragment {
        private static final long serialVersionUID = 1L;

        public BasicSearchFragment(String id, String markupId, AbstractSearchPanel markupProvider) {
            super(id, markupId, markupProvider);
            initBasicSearchLayout();
        }

        private <T extends Serializable> void initBasicSearchLayout() {
            ListView<SearchItem> items = new ListView<>(ID_ITEMS, getModelObject().getItemsModel()) {

                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(ListItem<SearchItem> item) {
//                    AbstractSearchItemPanel searchItem;
//                    if (item.getModelObject() instanceof FilterSearchItem) {
//                        searchItem = new SearchFilterPanel(ID_ITEM, (IModel<FilterSearchItem>) item.getModel()) {
//                            private static final long serialVersionUID = 1L;
//
//                            @Override
//                            protected boolean canRemoveSearchItem() {
//                                return super.canRemoveSearchItem() && AbstractSearchPanel.this.getModelObject().isConfigurable();
//                            }
//
//                            @Override
//                            protected void searchPerformed(AjaxRequestTarget target) {
//                                AbstractSearchPanel.this.searchPerformed(target);
//                            }
//                        };
//                    } else {
//                        searchItem = new SearchPropertyPanel<T>(ID_ITEM, (IModel<PropertySearchItem<T>>) item.getModel()) {
//                            private static final long serialVersionUID = 1L;
//
//                            @Override
//                            protected boolean canRemoveSearchItem() {
//                                return super.canRemoveSearchItem() && AbstractSearchPanel.this.getModelObject().isConfigurable();
//                            }
//
//                            @Override
//                            protected void searchPerformed(AjaxRequestTarget target) {
//                                AbstractSearchPanel.this.searchPerformed(target);
//                            }
//                        };
//                    }
                    if (item.getModelObject().getSearchItemDefinition().isSearchItemDisplayed()) {
                        SearchItemPanel searchItemPanel = createSearchItemPanel(ID_ITEM, item.getModel());
                        item.add(searchItemPanel);
                    }
                }
            };
//            items.add(createVisibleBehaviour(SearchBoxModeType.BASIC));//todo
            add(items);

            WebMarkupContainer moreGroup = new WebMarkupContainer(ID_MORE_GROUP);
//            moreGroup.add(new VisibleBehaviour(() -> createVisibleBehaviour(SearchBoxModeType.BASIC).isVisible()));
            add(moreGroup);

            AjaxLink<Void> more = new AjaxLink<Void>(ID_MORE) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    resetMoreDialogModel();
                    Component popover = BasicSearchFragment.this.get(createComponentPath(ID_POPOVER));
                    Component button = BasicSearchFragment.this.get(createComponentPath(ID_MORE_GROUP, ID_MORE));
                    togglePopover(target, button, popover, 14);
                }
            };
            more.add(new VisibleEnableBehaviour() {

                private static final long serialVersionUID = 1L;

                @Override
                public boolean isVisible() {
                    Search search = getModelObject();
                    return !search.getAllDefinitions().isEmpty();
                }
            });
            more.setOutputMarkupId(true);
            moreGroup.add(more);

            initPopover();
        }

        private void initPopover() {
            WebMarkupContainer popover = new WebMarkupContainer(ID_POPOVER);
            popover.setOutputMarkupId(true);
            add(popover);

            final WebMarkupContainer propList = new WebMarkupContainer(ID_PROP_LIST);
            propList.setOutputMarkupId(true);
            popover.add(propList);

            ListView properties = new ListView<AbstractSearchItemDefinition>(ID_PROPERTIES,
                    new PropertyModel<>(moreDialogModel, MoreDialogDto.F_PROPERTIES)) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(final ListItem<AbstractSearchItemDefinition> item) {
                    CheckBox check = new CheckBox(ID_CHECK,
                            new PropertyModel<>(item.getModel(), SearchItemDefinition.F_SELECTED));
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

                    Label name = new Label(ID_PROP_NAME, new PropertyModel<>(item.getModel(), SearchItemDefinition.F_NAME));
                    name.setRenderBodyOnly(true);
                    propLink.add(name);

                    Label help = new Label(ID_HELP);
                    IModel<String> helpModel = new PropertyModel<>(item.getModel(), SearchItemDefinition.F_HELP);
                    help.add(AttributeModifier.replace("title", createStringResource(helpModel.getObject() != null ? helpModel.getObject() : "")));
                    help.add(new InfoTooltipBehavior() {
                        @Override
                        public String getDataPlacement() {
                            return "left";
                        }
                    });
                    help.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(helpModel.getObject())));
                    item.add(help);

                    item.add(new VisibleBehaviour(() -> !item.getModelObject().isSearchItemDisplayed()));
//                    item.add(new VisibleEnableBehaviour() {
//
//                        private static final long serialVersionUID = 1L;
//
//                        @Override
//                        public boolean isVisible() {
//                            AbstractSearchItemDefinition property = item.getModelObject();

//                            Search<C> search = AbstractSearchPanel.this.getModelObject();
//                            if (!search.getAvailableDefinitions().contains(property)) {
//                                return false;
//                            }
//
//                            for (SearchItem searchItem : search.getItems()) {
//                                if (searchItem.getSearchItemDefinition().equals(property)) {
//                                    return false;
//                                }
//                                if (searchItem instanceof AttributeSearchItem) {
//                                    ItemPath propertyPath = property.getPath();
//                                    if (propertyPath != null && QNameUtil.match(propertyPath.lastName(), ((AttributeSearchItem) searchItem).getPath().lastName())) {
//                                        return false;
//                                    }
//                                }
//                            }

//                            MoreDialogDto dto = moreDialogModel.getObject();
//                            String nameFilter = dto.getNameFilter();
//                            String propertyName = property.getName().toLowerCase();
//                            if (StringUtils.isNotEmpty(nameFilter)
//                                    && !propertyName.contains(nameFilter.toLowerCase())) {
//                                return false;
//                            }
//
//                            return true;
//                        }
//                    });
                }
            };
            propList.add(properties);

            TextField<?> addText = new TextField<>(ID_ADD_TEXT, new PropertyModel<>(moreDialogModel, MoreDialogDto.F_NAME_FILTER));
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

        public void togglePopover(AjaxRequestTarget target, Component button, Component popover, int paddingRight) {
            target.appendJavaScript("toggleSearchPopover('"
                    + button.getMarkupId() + "','"
                    + popover.getMarkupId() + "',"
                    + paddingRight + ");");
        }
    }

    class AdvancedSearchFragment extends Fragment {
        private static final long serialVersionUID = 1L;

        public AdvancedSearchFragment(String id, String markupId, AbstractSearchPanel markupProvider) {
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

            TextArea<?> advancedArea = new TextArea<>(ID_ADVANCED_AREA, new PropertyModel<>(getModel(), Search.F_ADVANCED_QUERY));
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
                    new PropertyModel<>(getModel(), Search.F_DSL_QUERY));
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
                    new PropertyModel<String>(getModel(), Search.F_ADVANCED_ERROR));
            advancedError.add(new VisibleEnableBehaviour() {

                private static final long serialVersionUID = 1L;

                @Override
                public boolean isVisible() {
                    Search search = getModelObject();

                    if (!search.isShowAdvanced()) {
                        return false;
                    }

                    return StringUtils.isNotEmpty(search.getAdvancedError());
                }
            });
            advancedErrorGroup.add(advancedError);
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
            AbstractSearchPanel.this.setResponsePage(pageQuery);
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

        public FulltextSearchFragment(String id, String markupId, AbstractSearchPanel markupProvider) {
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
}
