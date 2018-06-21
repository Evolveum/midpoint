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

import com.evolveum.midpoint.web.component.DropDownMultiChoice;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.model.IModel;

import java.util.List;
import java.util.Map;

/**
 * @author lazyman
 */
public class ListMultipleChoicePanel<T> extends InputPanel {

    public ListMultipleChoicePanel(String id, IModel<List<T>> model, IModel<List<T>> choices) {
        super(id);
        ListMultipleChoice<T> multiple = new ListMultipleChoice<>("input", model, choices);
        add(multiple);
    }

    public ListMultipleChoicePanel(String id, IModel<List<T>> model, IModel<List<T>> choices, IChoiceRenderer renderer,
                                   IModel<Map<String, String>> options){
        super(id);
        DropDownMultiChoice multiChoice = new DropDownMultiChoice<T>("input", model, choices, renderer, options);
        add(multiChoice);
    }

    @Override
    public FormComponent getBaseFormComponent() {
        return (FormComponent) get("input");
    }
}
