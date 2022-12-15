/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.page.admin.roles.AbstractRoleCompositedSearchItem;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.util.QNameUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;

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
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.menu.cog.MenuLinkPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.PageRepositoryQuery;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxModeType;

/**
 * @author Viliam Repan (lazyman)
 */
public class SearchPanel<C extends Containerable> extends BasePanel<Search<C>> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOG = TraceManager.getTrace(SearchPanel.class);

    private static final String ID_FORM = "form";
    private static final String ID_ITEMS = "items";
    private static final String ID_ITEM = "item";
    private static final String ID_SPECIAL_ITEMS = "specialItems";
    private static final String ID_COMPOSITED_SPECIAL_ITEMS = "compositedSpecialItems";
    private static final String ID_SPECIAL_ITEM = "specialItem";
    private static final String ID_OID_ITEM = "oidItem";
    private static final String ID_SEARCH_CONTAINER = "searchContainer";
    private static final String ID_DEBUG = "debug";
    private static final String ID_SEARCH_BUTTON_BEFORE_DROPDOWN = "searchButtonBeforeDropdown";
    private static final String ID_SEARCH_BUTTON_DROPDOWN = "menuDropdown";
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
    private static final String ID_FULL_TEXT_CONTAINER = "fullTextContainer";
    private static final String ID_FULL_TEXT_FIELD = "fullTextField";
    private static final String ID_ADVANCED_GROUP = "advancedGroup";
    private static final String ID_MORE_GROUP = "moreGroup";
    private static final String ID_ADVANCED_AREA = "advancedArea";
    private static final String ID_AXIOM_QUERY_FIELD = "axiomQueryField";
    private static final String ID_ADVANCED_CHECK = "advancedCheck";
    private static final String ID_ADVANCED_ERROR_GROUP = "advancedErrorGroup";
    private static final String ID_ADVANCED_ERROR = "advancedError";
    private static final String ID_MENU_ITEM = "menuItem";
    private static final String ID_MENU_ITEM_BODY = "menuItemBody";
    private static final String ID_COLLECTION_REF_PANEL = "collectionRefPanel";
    private static final String ID_TYPE_PANEL = "typePanel";

    private LoadableModel<MoreDialogDto> moreDialogModel;
    boolean advancedSearch;
    boolean queryPlaygroundAccessible;

    public SearchPanel(String id, IModel<Search<C>> model) {
        this(id, model, true);
    }

    public SearchPanel(String id, IModel<Search<C>> model, boolean advancedSearch) {
        super(id, model);
        this.advancedSearch = advancedSearch;
        queryPlaygroundAccessible = SecurityUtils.isPageAuthorized(PageRepositoryQuery.class);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private <S extends SearchItem, T extends Serializable> void initLayout() {
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
                if (SearchPanel.this.getModelObject() != null && SearchPanel.this.getModelObject().isTypeChanged()) {
                    reset();
                }
                return super.getObject();
            }
        };

        MidpointForm<?> form = new MidpointForm<>(ID_FORM);
        add(form);

        AjaxButton debug = new AjaxButton(ID_DEBUG, createStringResource("SearchPanel.debug")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                debugPerformed();
            }
        };
        debug.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return SearchBoxModeType.ADVANCED.equals(getModelObject().getSearchType())
                        && queryPlaygroundAccessible;
            }
        });
        form.add(debug);

        PropertyModel<ObjectCollectionSearchItem> collectionModel = new PropertyModel<>(getModel(), Search.F_COLLECTION);
        SearchObjectCollectionPanel collectionPanel = new SearchObjectCollectionPanel(ID_COLLECTION_REF_PANEL, collectionModel);
        form.add(collectionPanel);
        collectionPanel.add(new VisibleBehaviour(() -> collectionModel != null && collectionModel.getObject() != null
                && getModelObject().isCollectionItemVisible()));

        PropertyModel<ContainerTypeSearchItem> typeModel = new PropertyModel<>(getModel(), Search.F_TYPE);
        SearchTypePanel typePanel = new SearchTypePanel(ID_TYPE_PANEL, typeModel) {
            @Override
            protected void searchPerformed(AjaxRequestTarget target) {
                resetMoreDialogModel();
                SearchPanel.this.searchPerformed(target);
            }
        };
        form.add(typePanel);
        typePanel.add(new VisibleBehaviour(() -> typeModel != null && typeModel.getObject() != null && typeModel.getObject().isVisible()
                && !SearchBoxModeType.OID.equals(getModelObject().getSearchType())));

        ListView<SearchItem> specialItems = createSpecialItemsPanel(ID_SPECIAL_ITEMS, new PropertyModel<>(getModel(), Search.F_SPECIAL_ITEMS));
        form.add(specialItems);

        ListView<SearchItem> compositedItems = createSpecialItemsPanel(ID_COMPOSITED_SPECIAL_ITEMS, new PropertyModel(getModel(), Search.F_COMPOSITED_SPECIAL_ITEMS + "." + AbstractRoleCompositedSearchItem.F_SEARCH_ITEMS));
        form.add(compositedItems);

        ListView<S> items = new ListView<S>(ID_ITEMS,
                new LoadableDetachableModel<>() {
                    @Override
                    protected List<S> load() {
                        return (List<S>) getModelObject().getItems();
                    }
                }) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<S> item) {
                AbstractSearchItemPanel searchItem;
                if (item.getModelObject() instanceof FilterSearchItem) {
                    searchItem = new SearchFilterPanel(ID_ITEM, (IModel<FilterSearchItem>) item.getModel()) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected boolean canRemoveSearchItem() {
                            return super.canRemoveSearchItem() && SearchPanel.this.getModelObject().isCanConfigure();
                        }

                        @Override
                        protected void searchPerformed(AjaxRequestTarget target) {
                            SearchPanel.this.searchPerformed(target);
                        }
                    };
                } else {
                    searchItem = new SearchPropertyPanel<T>(ID_ITEM, (IModel<PropertySearchItem<T>>) item.getModel()) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected boolean canRemoveSearchItem() {
                            return super.canRemoveSearchItem() && SearchPanel.this.getModelObject().isCanConfigure();
                        }

                        @Override
                        protected void searchPerformed(AjaxRequestTarget target) {
                            SearchPanel.this.searchPerformed(target);
                        }
                    };
                }
                item.add(searchItem);
            }
        };
        items.add(createVisibleBehaviour(SearchBoxModeType.BASIC));
        form.add(items);

        SearchSpecialItemPanel oidItem = new SearchSpecialItemPanel<String>(ID_OID_ITEM, new PropertyModel<String>(getModel(), Search.F_OID)) {
            @Override
            protected WebMarkupContainer initSearchItemField(String id) {
                TextPanel<String> inputPanel = new TextPanel<String>(id, getModelValue());
                inputPanel.getBaseFormComponent().add(AttributeAppender.append("style", "width: 220px; max-width: 400px !important;"));
                return inputPanel;
            }

            @Override
            protected IModel<String> createLabelModel() {
                return getPageBase().createStringResource("SearchPanel.oid");
            }

            @Override
            protected IModel<String> createHelpModel() {
                return getPageBase().createStringResource("SearchPanel.oid.help");
            }
        };
        oidItem.add(createVisibleBehaviour(SearchBoxModeType.OID));
        form.add(oidItem);

        WebMarkupContainer moreGroup = new WebMarkupContainer(ID_MORE_GROUP);
        moreGroup.add(new VisibleBehaviour(() -> createVisibleBehaviour(SearchBoxModeType.BASIC).isVisible()));
        form.add(moreGroup);

        AjaxLink<Void> more = new AjaxLink<Void>(ID_MORE) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                resetMoreDialogModel();
                Component popover = SearchPanel.this.get(createComponentPath(ID_POPOVER));
                Component button = SearchPanel.this.get(createComponentPath(ID_FORM, ID_MORE_GROUP, ID_MORE));
                togglePopover(target, button, popover, 14);
            }
        };
        more.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                Search search = getModelObject();
                return !search.getAvailableDefinitions().isEmpty();
            }
        });
        more.setOutputMarkupId(true);
        moreGroup.add(more);

        WebMarkupContainer searchContainer = new WebMarkupContainer(ID_SEARCH_CONTAINER);
        searchContainer.setOutputMarkupId(true);
        form.add(searchContainer);

        final CompositedIconBuilder builder = new CompositedIconBuilder();
        builder.setBasicIcon(GuiStyleConstants.CLASS_ICON_SEARCH, IconCssStyle.IN_ROW_STYLE);
        IconType plusIcon = new IconType();
        plusIcon.setColor("white");
        builder.appendLayerIcon(getIconLabelByModeModel(), plusIcon, LayeredIconCssStyle.BOTTOM_RIGHT_STYLE);
        AjaxCompositedIconSubmitButton searchButtonBeforeDropdown = new AjaxCompositedIconSubmitButton(ID_SEARCH_BUTTON_BEFORE_DROPDOWN, builder.build(),
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
        searchButtonBeforeDropdown.add(AttributeAppender.append("style", buttonRightPaddingModel));

        searchButtonBeforeDropdown.add(new VisibleEnableBehaviour() {

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
                    return !search.getItems().isEmpty() || !search.getAvailableDefinitions().isEmpty();
                }
                return true;
            }
        });
        searchButtonBeforeDropdown.setOutputMarkupId(true);
        searchContainer.add(searchButtonBeforeDropdown);
        form.setDefaultButton(searchButtonBeforeDropdown);

        WebMarkupContainer dropdownButton = new WebMarkupContainer(ID_SEARCH_BUTTON_DROPDOWN);
        dropdownButton.add(new VisibleBehaviour(() -> getModelObject().getAllowedSearchType().size() != 1));
        searchContainer.add(dropdownButton);

        List<InlineMenuItem> searchItems = new ArrayList<>();

        InlineMenuItem searchItem = new InlineMenuItem(
                createStringResource("SearchPanel.basic")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new InlineMenuItemAction() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        searchTypeUpdated(target, SearchBoxModeType.BASIC);
                    }
                };
            }

            @Override
            public IModel<Boolean> getVisible() {
                return Model.of(getModelObject().isAllowedSearchMode(SearchBoxModeType.BASIC));
            }
        };
        searchItems.add(searchItem);

        searchItem = new InlineMenuItem(
                createStringResource("SearchPanel.advanced")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new InlineMenuItemAction() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        searchTypeUpdated(target, SearchBoxModeType.ADVANCED);
                    }
                };
            }

            @Override
            public IModel<Boolean> getVisible() {
                return Model.of(getModelObject().isAllowedSearchMode(SearchBoxModeType.ADVANCED));
            }
        };
        searchItems.add(searchItem);

        searchItem = new InlineMenuItem(
                createStringResource("SearchPanel.fullText")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new InlineMenuItemAction() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        searchTypeUpdated(target, SearchBoxModeType.FULLTEXT);
                    }
                };
            }

            @Override
            public IModel<Boolean> getVisible() {
                return Model.of(isFullTextSearchEnabled()
                        && getModelObject().isAllowedSearchMode(SearchBoxModeType.FULLTEXT));
            }
        };
        searchItems.add(searchItem);

        searchItem = new InlineMenuItem(
                createStringResource("SearchPanel.oid")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new InlineMenuItemAction() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        searchTypeUpdated(target, SearchBoxModeType.OID);
                    }
                };
            }

            @Override
            public IModel<Boolean> getVisible() {
                return Model.of(getModelObject().isOidSearchEnabled()
                        && getModelObject().isAllowedSearchMode(SearchBoxModeType.OID));
            }
        };
        searchItems.add(searchItem);

        searchItem = new InlineMenuItem(
                createStringResource("SearchPanel.axiomQuery")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new InlineMenuItemAction() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        searchTypeUpdated(target, SearchBoxModeType.AXIOM_QUERY);
                    }
                };
            }

            @Override
            public IModel<Boolean> getVisible() {
                return Model.of(WebModelServiceUtils.isEnableExperimentalFeature(getPageBase())
                        && getModelObject().isAllowedSearchMode(SearchBoxModeType.AXIOM_QUERY));
            }
        };
        searchItems.add(searchItem);

        ListView<InlineMenuItem> li = new ListView<InlineMenuItem>(ID_MENU_ITEM, Model.ofList(searchItems)) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<InlineMenuItem> item) {
                WebMarkupContainer menuItemBody = new MenuLinkPanel(ID_MENU_ITEM_BODY, item.getModel());
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

        initPopover();

        WebMarkupContainer fullTextContainer = new WebMarkupContainer(ID_FULL_TEXT_CONTAINER);
        fullTextContainer.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isFullTextSearchEnabled()
                        && getModelObject().getSearchType().equals(SearchBoxModeType.FULLTEXT);
            }
        });
        fullTextContainer.setOutputMarkupId(true);
        form.add(fullTextContainer);

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
        fullTextInput.add(createVisibleBehaviour(SearchBoxModeType.FULLTEXT));
        fullTextContainer.add(fullTextInput);

        WebMarkupContainer advancedGroup = new WebMarkupContainer(ID_ADVANCED_GROUP);
        advancedGroup.add(createVisibleBehaviour(SearchBoxModeType.ADVANCED, SearchBoxModeType.AXIOM_QUERY));
        advancedGroup.add(AttributeAppender.append("class", createAdvancedGroupStyle()));
        advancedGroup.setOutputMarkupId(true);
        form.add(advancedGroup);

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

    private <T extends Serializable> ListView<SearchItem> createSpecialItemsPanel(String id, IModel model) {
        ListView<SearchItem> specialItems = new ListView<SearchItem>(id, model) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<SearchItem> item) {
                WebMarkupContainer searchItem;
                if (item.getModelObject() instanceof SpecialSearchItem) {
                    searchItem = ((SpecialSearchItem) item.getModelObject()).createSpecialSearchPanel(ID_SPECIAL_ITEM);
                } else {
                    IModel itemModel = item.getModel();
                    searchItem = new SearchPropertyPanel<T>(ID_SPECIAL_ITEM, (IModel<PropertySearchItem<T>>) itemModel) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected boolean canRemoveSearchItem() {
                            return super.canRemoveSearchItem() && SearchPanel.this.getModelObject().isCanConfigure();
                        }

                        @Override
                        protected void searchPerformed(AjaxRequestTarget target) {
                            SearchPanel.this.searchPerformed(target);
                        }
                    };
                }
                item.add(searchItem);
            }
        };
        specialItems.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return !SearchBoxModeType.OID.equals(getModelObject().getSearchType());
            }
        });
        return specialItems;
    }

    private IModel<String> getIconLabelByModeModel() {
        return new IModel<String>() {
            @Override
            public String getObject() {
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
            }
        };
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

    private VisibleEnableBehaviour createVisibleBehaviour(SearchBoxModeType... searchType) {
        return new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return getModelObject() != null && getModelObject().getSearchType() != null
                        && Arrays.asList(searchType).contains(getModelObject().getSearchType());
            }
        };
    }

    private void initPopover() {
        WebMarkupContainer popover = new WebMarkupContainer(ID_POPOVER);
        popover.setOutputMarkupId(true);
        add(popover);

        final WebMarkupContainer propList = new WebMarkupContainer(ID_PROP_LIST);
        propList.setOutputMarkupId(true);
        popover.add(propList);

        ListView properties = new ListView<SearchItemDefinition>(ID_PROPERTIES,
                new PropertyModel<>(moreDialogModel, MoreDialogDto.F_PROPERTIES)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<SearchItemDefinition> item) {
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

                item.add(new VisibleEnableBehaviour() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean isVisible() {
                        SearchItemDefinition property = item.getModelObject();

                        Search<C> search = SearchPanel.this.getModelObject();
                        if (!search.getAvailableDefinitions().contains(property)) {
                            return false;
                        }

                        for (SearchItem searchItem : search.getItems()) {
                            if (searchItem.getDefinition().equals(property)) {
                                return false;
                            }
                            if (searchItem instanceof PropertySearchItem) {
                                ItemPath propertyPath = property.getPath();
                                if (propertyPath != null && QNameUtil.match(propertyPath.lastName(), ((PropertySearchItem) searchItem).getPath().lastName())) {
                                    return false;
                                }
                            }
                        }

                        MoreDialogDto dto = moreDialogModel.getObject();
                        String nameFilter = dto.getNameFilter();
                        String propertyName = property.getName().toLowerCase();
                        if (StringUtils.isNotEmpty(nameFilter)
                                && !propertyName.contains(nameFilter.toLowerCase())) {
                            return false;
                        }

                        return true;
                    }
                });
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

    private List<SearchItemDefinition> createPropertiesList() {
        List<SearchItemDefinition> list = new ArrayList<>();

        List<ItemPath> specialItemPaths = new ArrayList<>();
        getModelObject().getSpecialItems().stream().filter(specItem -> (specItem instanceof PropertySearchItem))
                .forEach(specItem -> specialItemPaths.add(((PropertySearchItem<?>) specItem).getPath()));

        Search search = getModelObject();
        search.getAllDefinitions().stream().filter((Predicate<SearchItemDefinition>) def -> !ItemPathCollectionsUtil.containsEquivalent(specialItemPaths, def.getPath()))
                .forEach((Consumer<SearchItemDefinition>) def -> list.add(def));
        Collections.sort(list);

        return list;
    }

    private void addOneItemPerformed(SearchItemDefinition property, AjaxRequestTarget target) {
        Search search = getModelObject();
        SearchItem item = search.addItem(property);
        item.setEditWhenVisible(true);

        refreshSearchForm(target);
    }

    private void addItemPerformed(AjaxRequestTarget target) {
        Search search = getModelObject();

        MoreDialogDto dto = moreDialogModel.getObject();
        for (SearchItemDefinition property : dto.getProperties()) {
            if (!property.isSelected()) {
                continue;
            }

            search.addItem(property);
            property.setSelected(false);
        }

        refreshSearchForm(target);
    }

    private void closeMorePopoverPerformed(AjaxRequestTarget target) {
        String popoverId = get(ID_POPOVER).getMarkupId();
        target.appendJavaScript("$('#" + popoverId + "').toggle();");
    }

    public void searchPerformed(AjaxRequestTarget target) {
    }

    void refreshSearchForm(AjaxRequestTarget target) {
        target.add(get(ID_FORM), get(ID_POPOVER));
        saveSearch(getModelObject(), target);
    }

    protected void saveSearch(Search search, AjaxRequestTarget target) {
    }

    public void togglePopover(AjaxRequestTarget target, Component button, Component popover, int paddingRight) {
        target.appendJavaScript("toggleSearchPopover('"
                + button.getMarkupId() + "','"
                + popover.getMarkupId() + "',"
                + paddingRight + ");");
    }

    private void searchTypeUpdated(AjaxRequestTarget target, SearchBoxModeType searchType) {
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

    private boolean isFullTextSearchEnabled() {
        return getModelObject().isFullTextSearchEnabled();
    }

    public void resetMoreDialogModel() {
        moreDialogModel.reset();
    }
}
