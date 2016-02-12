/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.model.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

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
    private static final String ID_SEARCH = "search";
    private static final String ID_MORE = "more";
    private static final String ID_POPOVER = "popover";
    private static final String ID_ADD_TEXT = "addText";
    private static final String ID_ADD = "add";
    private static final String ID_CLOSE = "close";
    private static final String ID_PROPERTIES = "properties";
    private static final String ID_CHECK = "check";
    private static final String ID_PROP_NAME = "propName";
    private static final String ID_PROP_LIST = "propList";

    private LoadableModel<MoreDialogDto> moreDialogModel;

    public SearchPanel(String id, IModel<Search> model) {
        super(id, model);
        initLayout();
    }

    private void initLayout() {
        moreDialogModel = new LoadableModel<MoreDialogDto>(false) {

            @Override
            protected MoreDialogDto load() {
                MoreDialogDto dto = new MoreDialogDto();
                dto.setProperties(createPropertiesList());

                return dto;
            }
        };

        Form form = new Form(ID_FORM);
        add(form);

        ListView items = new ListView<SearchItem>(ID_ITEMS,
                new PropertyModel<List<SearchItem>>(getModel(), Search.F_ITEMS)) {

            @Override
            protected void populateItem(ListItem<SearchItem> item) {
                SearchItemPanel searchItem = new SearchItemPanel(ID_ITEM, item.getModel());
                item.add(searchItem);
            }
        };
        form.add(items);

        AjaxLink more = new AjaxLink(ID_MORE) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                Component button = SearchPanel.this.get(createComponentPath(ID_FORM, ID_MORE));
                Component popover = SearchPanel.this.get(createComponentPath(ID_POPOVER));
                togglePopover(target, button, popover, 14);
            }
        };
        more.setOutputMarkupId(true);
        form.add(more);

        AjaxSubmitLink search = new AjaxSubmitLink(ID_SEARCH) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(form);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                searchPerformed(target);
            }
        };
        form.add(search);

        initPopover();
    }

    private void initPopover() {
        WebMarkupContainer popover = new WebMarkupContainer(ID_POPOVER);
        popover.setOutputMarkupId(true);
        add(popover);

        final WebMarkupContainer propList = new WebMarkupContainer(ID_PROP_LIST);
        propList.setOutputMarkupId(true);
        popover.add(propList);

        ListView properties = new ListView<Property>(ID_PROPERTIES,
                new PropertyModel<List<Property>>(moreDialogModel, MoreDialogDto.F_PROPERTIES)) {

            @Override
            protected void populateItem(final ListItem<Property> item) {
                CheckBox check = new CheckBox(ID_CHECK,
                        new PropertyModel<Boolean>(item.getModel(), Property.F_SELECTED));
                check.add(new AjaxFormComponentUpdatingBehavior("change") {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        //nothing, just update model.
                    }
                });
                item.add(check);

                Label name = new Label(ID_PROP_NAME, new PropertyModel<>(item.getModel(), Property.F_NAME));
                name.setRenderBodyOnly(true);
                item.add(name);

                item.add(new VisibleEnableBehaviour() {

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
        popover.add(addText);
        addText.add(new AjaxFormComponentUpdatingBehavior("keyup") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(propList);
            }
        });
        popover.add(addText);

        AjaxButton add = new AjaxButton(ID_ADD, createStringResource("SearchPanel.add")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addItemPerformed(target);
            }
        };
        popover.add(add);

        AjaxButton close = new AjaxButton(ID_CLOSE, createStringResource("SearchPanel.close")) {

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
}
