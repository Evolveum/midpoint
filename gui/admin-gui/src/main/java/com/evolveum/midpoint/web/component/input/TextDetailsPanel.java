/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.input;

import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
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
        details.add(AttributeModifier.replace("title", new IModel<String>() {

            @Override
            public String getObject() {
                return createAssociationTooltip();
            }
        }));
        details.add(new InfoTooltipBehavior(){

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
