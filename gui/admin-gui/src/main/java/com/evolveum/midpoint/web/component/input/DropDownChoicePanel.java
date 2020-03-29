/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.input;

import java.util.List;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.prism.InputPanel;

import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public class DropDownChoicePanel<T> extends InputPanel {

    private static final long serialVersionUID = 1L;
    private static final String ID_INPUT = "input";

    private boolean sortChoices = true;

    public DropDownChoicePanel(String id, IModel<T> model, IModel<? extends List<? extends T>> choices) {
        this(id, model, choices, false);
    }

    public DropDownChoicePanel(String id, IModel<T> model, IModel<? extends List<? extends T>> choices, boolean allowNull) {
        this(id, model, choices, new ChoiceRenderer(), allowNull);
    }

    public DropDownChoicePanel(String id, IModel<T> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<T> renderer) {
        this(id, model, choices, renderer, false);
    }

    public DropDownChoicePanel(String id, IModel<T> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<T> renderer,
                               boolean allowNull) {
        super(id);

        DropDownChoice<T> input = new DropDownChoice<T>(ID_INPUT, model,
                choices, renderer) {

            private static final long serialVersionUID = 1L;

            @Override
            protected CharSequence getDefaultChoice(String selectedValue) {
                if (allowNull){
                    return super.getDefaultChoice(selectedValue);
                } else {
                    return getString("DropDownChoicePanel.notDefined");
                }
            }

            @Override
            protected String getNullValidDisplayValue() {
                return DropDownChoicePanel.this.getNullValidDisplayValue();
            }

            @Override
            public IModel<? extends List<? extends T>> getChoicesModel() {
                IModel<? extends List<? extends T>> choices = super.getChoicesModel();
                if (sortChoices) {
                    return Model.ofList(WebComponentUtil.sortDropDownChoices(choices, renderer));
                } else {
                    return choices;
                }
            }

            @Override
            public String getModelValue() {
                T object = this.getModelObject();
                if (object != null) {
                    if (QName.class.isAssignableFrom(object.getClass())) {
                        for (int i = 0; i < getChoices().size(); i++) {
                            if (QNameUtil.match((QName) getChoices().get(i), (QName) object)) {
                                return this.getChoiceRenderer().getIdValue(object, i);
                            }
                        }
                    }
                }

                return super.getModelValue();
            }
        };
        input.setNullValid(allowNull);
        add(input);
    }

    @Override
    public DropDownChoice<T> getBaseFormComponent() {
        return (DropDownChoice) get("input");
    }

    public IModel<T> getModel() {
        return getBaseFormComponent().getModel();
    }

    protected String getNullValidDisplayValue() {
        return getString("DropDownChoicePanel.notDefined");
    }

    public boolean isSortChoices() {
        return sortChoices;
    }

    public void setSortChoices(boolean sortChoices) {
        this.sortChoices = sortChoices;
    }
}
