/*
 * Copyright (c) 2010-2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.util.DisplayableValue;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

/**
 * @author Viliam Repan (lazyman)
 */
public class TextPopupPanel extends SearchPopupPanel<DisplayableValue> {

    private static final String ID_TEXT_INPUT = "textInput";

    public TextPopupPanel(String id, IModel model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
        IModel data = new PropertyModel(getModel(), SearchValue.F_VALUE);
        final TextField input = new TextField(ID_TEXT_INPUT, data);
        input.add(new AjaxFormComponentUpdatingBehavior("blur") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                //nothing to do, just update model data
            }
        });
        input.add(new Behavior() {

            @Override
            public void bind(Component component) {
                super.bind(component);

                component.add(AttributeModifier.replace("onkeydown",
                        Model.of("if(event.keyCode == 13) {event.preventDefault();}")));
            }
        });
        input.setOutputMarkupId(true);
        add(input);
    }
}
