/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.component.input;

import java.util.List;

import com.evolveum.midpoint.web.component.prism.InputPanel;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
public class DropDownChoicePanel<T> extends InputPanel {

  private static final long serialVersionUID = 1L;
	private static final String ID_INPUT = "input";

    public DropDownChoicePanel(String id, IModel<T> model, IModel<? extends List<? extends T>> choices) {
        this(id, model, choices, false);
    }

    public DropDownChoicePanel(String id, IModel<T> model, IModel<? extends List<? extends T>> choices, boolean allowNull) {
        super(id);

        DropDownChoice<T> input = new DropDownChoice<T>(ID_INPUT, model, choices) {

          private static final long serialVersionUID = 1L;

			@Override
            protected CharSequence getDefaultChoice(String selectedValue) {
                return getString("DropDownChoicePanel.notDefined");
            }
        };
        input.setNullValid(allowNull);
        add(input);
    }

    public DropDownChoicePanel(String id, IModel<T> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<T> renderer) {
        this(id, model, choices, renderer, false);
    }
    
    public DropDownChoicePanel(String id, IModel<T> model, IModel<? extends List<? extends T>> choices, IChoiceRenderer<T> renderer,
                               boolean allowNull) {
        super(id);

        DropDownChoice<T> input = new DropDownChoice<T>(ID_INPUT, model, choices, renderer) {

        	private static final long serialVersionUID = 1L;

			@Override
            protected String getNullValidDisplayValue() {
                return getString("DropDownChoicePanel.notDefined");
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
}
