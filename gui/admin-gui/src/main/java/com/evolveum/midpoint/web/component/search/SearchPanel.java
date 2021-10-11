/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.menu.cog.MenuLinkPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.PageRepositoryQuery;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxModeType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.time.Duration;

import javax.xml.namespace.QName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Viliam Repan (lazyman)
 */
public class SearchPanel extends BasePanel<Search> {

    private static final Trace LOG = TraceManager.getTrace(SearchPanel.class);

    private static final String ID_FORM = "form";
    private static final String ID_ITEMS = "items";
    private static final String ID_ITEM = "item";
    private static final String ID_SEARCH_CONTAINER = "searchContainer";
    private static final String ID_SEARCH_SIMPLE = "searchSimple";
    private static final String ID_SEARCH_BUTTON_BEFORE_DROPDOWN = "searchButtonBeforeDropdown";
    private static final String ID_SEARCH_DROPDOWN = "searchDropdown";
    private static final String ID_MORE = "more";
    private static final String ID_POPOVER = "popover";
    private static final String ID_ADD_TEXT = "addText";
    private static final String ID_ADD = "add";
    private static final String ID_CLOSE = "close";
    private static final String ID_PROPERTIES = "properties";
    private static final String ID_CHECK = "check";
    private static final String ID_PROP_NAME = "propName";
    private static final String ID_PROP_LINK = "propLink";
    private static final String ID_PROP_LIST = "propList";
    private static final String ID_ADVANCED = "advanced";
    private static final String ID_FULL_TEXT = "fullText";
    private static final String ID_BASIC_SEARCH = "basic";
    private static final String ID_FULL_TEXT_CONTAINER = "fullTextContainer";
    private static final String ID_LINKS_CONTAINER = "linksContainer";
    private static final String ID_FULL_TEXT_FIELD = "fullTextField";
    private static final String ID_ADVANCED_GROUP = "advancedGroup";
    private static final String ID_MORE_GROUP = "moreGroup";
    private static final String ID_ADVANCED_AREA = "advancedArea";
    private static final String ID_ADVANCED_CHECK = "advancedCheck";
    private static final String ID_ADVANCED_ERROR= "advancedError";
    private static final String ID_MENU_ITEM = "menuItem";
    private static final String ID_MENU_ITEM_BODY = "menuItemBody";

    private LoadableModel<MoreDialogDto> moreDialogModel;
    boolean advancedSearch = true;
    boolean queryPlagroundAccessible;

    public SearchPanel(String id, IModel<Search> model) {
        this(id, model, true);
    }

    public SearchPanel(String id, IModel<Search> model, boolean advancedSearch) {
        super(id, model);
        this.advancedSearch = advancedSearch;
        queryPlagroundAccessible = SecurityUtils.isPageAuthorized(PageRepositoryQuery.class);
        initLayout();
    }

    private void initLayout() {
        moreDialogModel = new LoadableModel<MoreDialogDto>(false) {

              private static final long serialVersionUID = 1L;

            @Override
            protected MoreDialogDto load() {
                MoreDialogDto dto = new MoreDialogDto();
                dto.setProperties(createPropertiesList());

                return dto;
            }
        };

        Form<?> form = new com.evolveum.midpoint.web.component.form.Form<>(ID_FORM);
        add(form);

        ListView<SearchItem<?>> items = new ListView<SearchItem<?>>(ID_ITEMS,
            new PropertyModel<>(getModel(), Search.F_ITEMS)) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<SearchItem<?>> item) {
                SearchItemPanel<?> searchItem = new SearchItemPanel(ID_ITEM, item.getModel());
                item.add(searchItem);
            }
        };
        items.add(createVisibleBehaviour(SearchBoxModeType.BASIC));
        form.add(items);

        WebMarkupContainer moreGroup = new WebMarkupContainer(ID_MORE_GROUP);
        moreGroup.add(createVisibleBehaviour(SearchBoxModeType.BASIC));
        form.add(moreGroup);

        AjaxLink<Void> more = new AjaxLink<Void>(ID_MORE) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                Component button = SearchPanel.this.get(createComponentPath(ID_FORM, ID_MORE_GROUP, ID_MORE));
                Component popover = SearchPanel.this.get(createComponentPath(ID_POPOVER));
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

        AjaxSubmitButton searchSimple = new AjaxSubmitButton(ID_SEARCH_SIMPLE) {

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
        searchSimple.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return (SearchBoxModeType.BASIC.equals(getModelObject().getSearchType())
                        || SearchBoxModeType.FULLTEXT.equals(getModelObject().getSearchType())
                        || (SearchBoxModeType.ADVANCED.equals(getModelObject().getSearchType()) && !queryPlagroundAccessible));
           }

            @Override
            public boolean isVisible() {
                return (SearchBoxModeType.BASIC.equals(getModelObject().getSearchType())
                        || SearchBoxModeType.FULLTEXT.equals(getModelObject().getSearchType())
                        || (SearchBoxModeType.ADVANCED.equals(getModelObject().getSearchType()) && !queryPlagroundAccessible));
            }
        });
        searchSimple.setOutputMarkupId(true);
        searchContainer.add(searchSimple);

        WebMarkupContainer searchDropdown = new WebMarkupContainer(ID_SEARCH_DROPDOWN);
        searchDropdown.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;
            @Override
            public boolean isVisible() {
                return SearchBoxModeType.ADVANCED.equals(getModelObject().getSearchType())
                        && queryPlagroundAccessible;
            }
        });
        searchContainer.add(searchDropdown);

        AjaxSubmitButton searchButtonBeforeDropdown = new AjaxSubmitButton(ID_SEARCH_BUTTON_BEFORE_DROPDOWN) {

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
        searchButtonBeforeDropdown.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;
            @Override
            public boolean isEnabled() {
                if (SearchBoxModeType.BASIC.equals(getModelObject().getSearchType())
                        || SearchBoxModeType.FULLTEXT.equals(getModelObject().getSearchType())) {
                    return true;
                }

                Search search = getModelObject();
                PrismContext ctx = getPageBase().getPrismContext();
                return search.isAdvancedQueryValid(ctx);
            }
        });
        searchDropdown.add(searchButtonBeforeDropdown);

        List<InlineMenuItem> searchItems = new ArrayList<>();

        InlineMenuItem searchItem = new InlineMenuItem(
                createStringResource("SearchPanel.search")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new InlineMenuItemAction() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        PrismContext ctx = getPageBase().getPrismContext();
                        if (getModelObject().isAdvancedQueryValid(ctx)) {
                            searchPerformed(target);
                        }
                    }
                };
            }
        };
        searchItems.add(searchItem);

        searchItem = new InlineMenuItem(createStringResource("SearchPanel.debug")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new InlineMenuItemAction() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        debugPerformed();
                    }
                };
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
            }
        };
        searchDropdown.add(li);

        WebMarkupContainer linksContainer = new WebMarkupContainer(ID_LINKS_CONTAINER);
        linksContainer.setOutputMarkupId(true);
        form.add(linksContainer);

        AjaxButton advanced = new AjaxButton(ID_ADVANCED, createStringResource("SearchPanel.advanced")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                searchTypeUpdated(target, SearchBoxModeType.ADVANCED);
            }
        };
        advanced.add(new VisibleEnableBehaviour(){

            private static final long serialVersionUID = 1L;
            @Override
            public boolean isVisible() {
                return !SearchBoxModeType.ADVANCED.equals(getModelObject().getSearchType());
            }
        });
        linksContainer.add(advanced);

        AjaxButton fullTextButton = new AjaxButton(ID_FULL_TEXT, createStringResource("SearchPanel.fullText")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
               searchTypeUpdated(target, SearchBoxModeType.FULLTEXT);
            }
        };
        fullTextButton.add(new VisibleEnableBehaviour(){

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isFullTextSearchEnabled() &&
                        !SearchBoxModeType.FULLTEXT.equals(getModelObject().getSearchType());
            }
        });
        linksContainer.add(fullTextButton);

        AjaxButton basicSearchButton = new AjaxButton(ID_BASIC_SEARCH, createStringResource("SearchPanel.basic")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                searchTypeUpdated(target, SearchBoxModeType.BASIC);
            }
        };
        basicSearchButton.add(new VisibleEnableBehaviour(){

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return !SearchBoxModeType.BASIC.equals(getModelObject().getSearchType());
            }
        });
        linksContainer.add(basicSearchButton);

        advanced.add(new AttributeAppender("style", new LoadableModel<String>() {

            private static final long serialVersionUID = 1L;

            @Override
            public String load() {
                return basicSearchButton.isVisible() ? "" : "display: table-cell; vertical-align: top;";
            }
        }));

        initPopover();

        WebMarkupContainer fullTextContainer = new WebMarkupContainer(ID_FULL_TEXT_CONTAINER);
        fullTextContainer.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isVisible(){
                return isFullTextSearchEnabled()
                        && getModelObject().getSearchType().equals(SearchBoxModeType.FULLTEXT);
            }
        });
        fullTextContainer.setOutputMarkupId(true);
        form.add(fullTextContainer);

        TextField fullTextInput = new TextField(ID_FULL_TEXT_FIELD, new PropertyModel<String>(getModel(),
                Search.F_FULL_TEXT));

        fullTextInput.add(new AjaxFormComponentUpdatingBehavior("blur") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        });
        fullTextInput.add(new Behavior() {

            private static final long serialVersionUID = 1L;

            @Override
            public void bind(Component component) {
                super.bind( component );

                component.add( AttributeModifier.replace( "onkeydown",
                        Model.of("if(event.keyCode == 13) {$('[about=\"searchSimple\"]').click();}") ) );
            }
        });
        fullTextInput.setOutputMarkupId(true);
        fullTextInput.add(new AttributeAppender("placeholder",
                createStringResource("SearchPanel.fullTextSearch")));
        fullTextInput.add(createVisibleBehaviour(SearchBoxModeType.FULLTEXT));
        fullTextContainer.add(fullTextInput);

        WebMarkupContainer advancedGroup = new WebMarkupContainer(ID_ADVANCED_GROUP);
        advancedGroup.add(createVisibleBehaviour(SearchBoxModeType.ADVANCED));
        advancedGroup.add(AttributeAppender.append("class", createAdvancedGroupStyle()));
        advancedGroup.setOutputMarkupId(true);
        form.add(advancedGroup);

        Label advancedCheck = new Label(ID_ADVANCED_CHECK);
        advancedCheck.add(AttributeAppender.append("class", createAdvancedGroupLabelStyle()));
        advancedGroup.add(advancedCheck);

        final TextArea advancedArea = new TextArea(ID_ADVANCED_AREA,
                new PropertyModel(getModel(), Search.F_ADVANCED_QUERY));
        advancedArea.add(new AjaxFormComponentUpdatingBehavior("keyup") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateAdvancedArea(advancedArea, target);
            }

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);

                attributes.setThrottlingSettings(
                        new ThrottlingSettings(ID_ADVANCED_AREA, Duration.milliseconds(500), true));
            }
        });
        advancedGroup.add(advancedArea);

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
        advancedGroup.add(advancedError);
    }

    private void debugPerformed() {
        Search search = getModelObject();
        PageRepositoryQuery pageQuery;
        if (search != null) {
            ObjectTypes type = search.getType() != null ? ObjectTypes.getObjectType(search.getType().getSimpleName()) : null;
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

    private IModel<String> createAdvancedModel() {
        return new IModel<String>() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                Search search = getModelObject();
                String key = search.isShowAdvanced() ? "SearchPanel.basic" : "SearchPanel.advanced";

                return createStringResource(key).getString();
            }
        };
    }

    private VisibleEnableBehaviour createVisibleBehaviour(SearchBoxModeType searchType) {
        return new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return getModelObject() != null && getModelObject().getSearchType().equals(searchType);
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

        ListView properties = new ListView<Property>(ID_PROPERTIES,
            new PropertyModel<>(moreDialogModel, MoreDialogDto.F_PROPERTIES)) {

            @Override
            protected void populateItem(final ListItem<Property> item) {
                CheckBox check = new CheckBox(ID_CHECK,
                    new PropertyModel<>(item.getModel(), Property.F_SELECTED));
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

                Label name = new Label(ID_PROP_NAME, new PropertyModel<>(item.getModel(), Property.F_NAME));
                name.setRenderBodyOnly(true);
                propLink.add(name);

                item.add(new VisibleEnableBehaviour() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean isVisible() {
                        Property property = item.getModelObject();

                        Search search = SearchPanel.this.getModelObject();
                        if (!search.getAvailableDefinitions().contains(property.getDefinition())) {
                            return false;
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

        TextField addText = new TextField(ID_ADD_TEXT, new PropertyModel(moreDialogModel, MoreDialogDto.F_NAME_FILTER));
        addText.add(new Behavior() {

            private static final long serialVersionUID = 1L;
            @Override
            public void bind(Component component) {
                super.bind( component );

                component.add( AttributeModifier.replace( "onkeydown", Model.of("if(event.keyCode == 13) {event.preventDefault();}") ) );
            }
        });

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

    private List<Property> createPropertiesList() {
        List<Property> list = new ArrayList<>();

        Search search = getModelObject();
        List<ItemDefinition> defs = search.getAllDefinitions();
        for (ItemDefinition def : defs) {
            list.add(new Property(def));
        }

        Collections.sort(list);

        return list;
    }

    private void addOneItemPerformed(Property property, AjaxRequestTarget target) {
        Search search = getModelObject();
        SearchItem item = search.addItem(property.getDefinition());
        item.setEditWhenVisible(true);

        moreDialogModel.reset();
        refreshSearchForm(target);
    }

    private void addItemPerformed(AjaxRequestTarget target) {
        Search search = getModelObject();

        MoreDialogDto dto = moreDialogModel.getObject();
        for (Property property : dto.getProperties()) {
            if (!property.isSelected()) {
                continue;
            }

            search.addItem(property.getDefinition());
        }

        moreDialogModel.reset();
        refreshSearchForm(target);
    }

    private void closeMorePopoverPerformed(AjaxRequestTarget target) {
        String popoverId = get(ID_POPOVER).getMarkupId();
        target.appendJavaScript("$('#" + popoverId + "').toggle();");
    }

    void searchPerformed(AjaxRequestTarget target) {
        PageBase page = (PageBase) getPage();
        PrismContext ctx = page.getPrismContext();

        Search search = getModelObject();
        ObjectQuery query = search.createObjectQuery(ctx);
        LOG.debug("Created query: {}", query);
        searchPerformed(query, target);
    }

    void refreshSearchForm(AjaxRequestTarget target) {
        target.add(get(ID_FORM), get(ID_POPOVER));
    }

    public void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
    }

    public void togglePopover(AjaxRequestTarget target, Component button, Component popover, int paddingRight) {
        StringBuilder script = new StringBuilder();
        script.append("toggleSearchPopover('");
        script.append(button.getMarkupId()).append("','");
        script.append(popover.getMarkupId()).append("',");
        script.append(paddingRight).append(");");

        target.appendJavaScript(script.toString());
    }

    private void searchTypeUpdated(AjaxRequestTarget target, SearchBoxModeType searchType) {
        getModelObject().setSearchType(searchType);

        refreshSearchForm(target);
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

    private boolean isFullTextSearchEnabled(){
        return getModelObject().isFullTextSearchEnabled();
    }
}
