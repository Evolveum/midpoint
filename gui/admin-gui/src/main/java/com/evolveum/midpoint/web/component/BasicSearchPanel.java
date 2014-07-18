/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.util.SearchFormEnterBehavior;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class BasicSearchPanel<T extends Serializable> extends SimplePanel<T> {

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
    protected void initLayout() {
        Label label = new Label(ID_LABEL, createTextNameModel());
        add(label);

        AjaxSubmitButton searchButton = new AjaxSubmitButton(ID_SEARCH,
                createStringResource("BasicSearchPanel.search")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                searchPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        add(searchButton);

        AjaxSubmitButton clearButton = new AjaxSubmitButton(ID_CLEAR_SEARCH) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                clearSearchPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        add(clearButton);

        final TextField searchText = new TextField(ID_SEARCH_TEXT, createSearchTextModel());
        searchText.add(AttributeModifier.replace("placeholder", createTextNameModel()));
        searchText.add(new SearchFormEnterBehavior(searchButton));
        add(searchText);
    }

    private Component getFeedbackPanel() {
        return getPageBase().getFeedbackPanel();
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
