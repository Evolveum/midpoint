/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.input;

import com.evolveum.midpoint.web.component.DropDownMultiChoice;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.model.IModel;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author lazyman
 */
public class ListMultipleChoicePanel<T> extends InputPanel {

    private static final long serialVersionUID = 1L;

    public ListMultipleChoicePanel(String id, IModel<List<T>> model, IModel<List<T>> choices) {
        super(id);
        ListMultipleChoice<T> multiple = new ListMultipleChoice<>("input", model, choices);
        add(multiple);
    }

    public ListMultipleChoicePanel(String id, IModel<List<T>> model, IModel<List<T>> choices, IChoiceRenderer<T> renderer,
                                   IModel<Map<String, String>> options){
        super(id);
        DropDownMultiChoice<T> multiChoice = new DropDownMultiChoice<T>("input", model, choices, renderer, options);
        add(multiChoice);
    }

    @Override
    public FormComponent<T> getBaseFormComponent() {
        return (FormComponent<T>) get("input");
    }

    public Collection<T> getModelObject() {
        DropDownMultiChoice<T> input = (DropDownMultiChoice) getBaseFormComponent();
        return input.getModelObject();
    }

}
