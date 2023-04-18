/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component;

import java.io.Serializable;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.util.SearchFormEnterBehavior;

/**
 * @author lazyman
 */
public class BasicSearchPanel<T extends Serializable> extends BasePanel<T> {

    private static final String ID_SEARCH_TEXT = "searchText";
    private static final String ID_SEARCH = "search";
    private static final String ID_CLEAR_SEARCH = "clearSearch";
    private static final String ID_LABEL = "label";

    public BasicSearchPanel(String id) {
        this(id, null);
    }

    public BasicSearchPanel(String id, IModel<T> model) {
        super(id, model);
        setRenderBodyOnly(true);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {
        Label label = new Label(ID_LABEL, createTextNameModel());
        add(label);

        AjaxSubmitButton searchButton = new AjaxSubmitButton(ID_SEARCH,
                createStringResource("BasicSearchPanel.search")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                searchPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getFeedbackPanel());
            }
        };
        add(searchButton);

        AjaxSubmitButton clearButton = new AjaxSubmitButton(ID_CLEAR_SEARCH) {

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                clearSearchPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getFeedbackPanel());
            }
        };
        add(clearButton);

        final TextField searchText = new TextField(ID_SEARCH_TEXT, createSearchTextModel());
        searchText.add(AttributeModifier.replace("placeholder", createTextNameModel()));
        searchText.add(new SearchFormEnterBehavior(searchButton));
        add(searchText);
    }

    protected IModel<String> createSearchTextModel() {
        return (IModel) getModel();
    }

    protected IModel<String> createTextNameModel() {
        return createStringResource("BasicSearchPanel.textPlaceholder");
    }

    public Component getSearchButton(){
        return get(ID_SEARCH);
    }

    protected void searchPerformed(AjaxRequestTarget target) {
    }

    protected void clearSearchPerformed(AjaxRequestTarget target) {
    }
}
