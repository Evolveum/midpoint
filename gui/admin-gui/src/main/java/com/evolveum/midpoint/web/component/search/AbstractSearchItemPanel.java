/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Viliam Repan (lazyman)
 * @author lskublik
 */
public abstract class AbstractSearchItemPanel<S extends SearchItem, T extends Serializable> extends BasePanel<S> {

    private static final long serialVersionUID = 1L;

    private static final String ID_SEARCH_ITEM_CONTAINER = "searchItemContainer";
    private static final String ID_SEARCH_ITEM_LABEL = "searchItemLabel";
    private static final String ID_HELP = "help";
    private static final String ID_REMOVE_BUTTON = "removeButton";

    public AbstractSearchItemPanel(String id, IModel<S> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        WebMarkupContainer searchItemContainer = new WebMarkupContainer(ID_SEARCH_ITEM_CONTAINER);
        searchItemContainer.setOutputMarkupId(true);
        add(searchItemContainer);

        IModel<String> labelModel = createLabelModel();
        Label searchItemLabel = new Label(ID_SEARCH_ITEM_LABEL, labelModel);
        searchItemLabel.setOutputMarkupId(true);
        searchItemLabel.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(labelModel.getObject())));

        IModel<String> titleModel = createTitleModel();
        if (StringUtils.isNotEmpty(titleModel.getObject())) {
            searchItemLabel.add(AttributeAppender.append("title", titleModel));
        }
        searchItemContainer.add(searchItemLabel);

        Label help = new Label(ID_HELP);
        IModel<String> helpModel = createHelpModel();
        help.add(AttributeModifier.replace("title",createStringResource(helpModel.getObject() != null ? helpModel.getObject() : "")));
        help.add(new InfoTooltipBehavior(){
            @Override
            public String getDataPlacement() {
                return "left";
            }
        });
        help.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(helpModel.getObject())));
        searchItemContainer.add(help);

        initSearchItemField(searchItemContainer);

        AjaxSubmitButton removeButton = new AjaxSubmitButton(ID_REMOVE_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit(AjaxRequestTarget target) {
                deletePerformed(target);
            }
        };
        removeButton.add(new VisibleBehaviour(() -> !(getModelObject() instanceof FilterSearchItem) && canRemoveSearchItem()));
        removeButton.setOutputMarkupId(true);
        searchItemContainer.add(removeButton);
    }

    private IModel<String> createHelpModel(){
        SearchItem item = getModelObject();
        if (item == null) {
            return Model.of();
        }
        return Model.of(item.getHelp(getPageBase()));
    }

    protected abstract void initSearchItemField(WebMarkupContainer searchItemContainer);

    protected boolean canRemoveSearchItem() {
        return false;
    }

    protected IModel<String> createLabelModel() {
        SearchItem item = getModelObject();
        if (item == null) {
            return Model.of();
        }
        return Model.of(item.getName());
    }

    private IModel<String> createTitleModel() {
        SearchItem item = getModelObject();
        if (item == null) {
            return Model.of();
        }
        return Model.of(item.getTitle(getPageBase()));
    }

    protected void searchPerformed(AjaxRequestTarget target){
    }

    private void deletePerformed(AjaxRequestTarget target) {
        SearchItem item = getModelObject();
        Search search = item.getSearch();
        search.delete(item);

        SearchPanel panel = findParent(SearchPanel.class);
        panel.refreshSearchForm(target);
        panel.searchPerformed(target);
    }

    protected IModel<List<DisplayableValue<Boolean>>> createBooleanChoices() {
        List<DisplayableValue<Boolean>> list = new ArrayList<>();
        list.add(new SearchValue<>(Boolean.TRUE, getString("Boolean.TRUE")));
        list.add(new SearchValue<>(Boolean.FALSE, getString("Boolean.FALSE")));
        return Model.ofList(list);
    }

    protected Component createDropDownChoices(String id, IModel<Object> model, IModel<List<DisplayableValue>> choices) {
        return new DropDownChoicePanel(id, model, choices, new IChoiceRenderer<DisplayableValue>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getDisplayValue(DisplayableValue val) {
                return val.getLabel();
            }

            @Override
            public String getIdValue(DisplayableValue val, int index) {
                return Integer.toString(index);
            }

            @Override
            public DisplayableValue getObject(String id, IModel<? extends List<? extends DisplayableValue>> choices) {
                return StringUtils.isNotBlank(id) ? choices.getObject().get(Integer.parseInt(id)) : null;
            }
        }, true);
    }

}
