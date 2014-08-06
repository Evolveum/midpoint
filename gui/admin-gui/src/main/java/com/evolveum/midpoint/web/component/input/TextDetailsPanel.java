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

import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

/**
 *  @author shood
 * */
public class TextDetailsPanel<T> extends InputPanel{

    private static final String ID_INPUT = "input";
    private static final String ID_DETAILS = "details";

    public TextDetailsPanel(String id, IModel<T> model){
        this(id, model, String.class);
    }

    public TextDetailsPanel(String id, IModel<T> model, Class clazz){
        super(id);

        final TextField<T> text = new TextField<>(ID_INPUT, model);
        text.setType(clazz);
        add(text);

        Label details = new Label(ID_DETAILS);
        details.add(AttributeModifier.replace("title", new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return createAssociationTooltip();
            }
        }));
        details.add(new TooltipBehavior(){

            @Override
            public String getDataPlacement(){
                return "bottom";
            }
        });
        add(details);
    }

    public String createAssociationTooltip(){
        return "";
    }

    @Override
    public FormComponent getBaseFormComponent() {
        return (FormComponent) get(ID_INPUT);
    }
}
