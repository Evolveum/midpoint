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

import com.evolveum.midpoint.web.component.prism.InputPanel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
public class DropDownChoicePanel<T> extends InputPanel {

    public DropDownChoicePanel(String id, IModel<T> model, IModel<T> choices) {
        super(id);

        add(new DropDownChoice("input", model, choices));
    }
    
    public DropDownChoicePanel(String id, IModel<T> model, IModel<T> choices, IChoiceRenderer<T> renderer) {
        super(id);

        add(new DropDownChoice("input", model, choices, renderer));
    }

    @Override
    public FormComponent getBaseFormComponent() {
        return (FormComponent) get("input");
    }
}
