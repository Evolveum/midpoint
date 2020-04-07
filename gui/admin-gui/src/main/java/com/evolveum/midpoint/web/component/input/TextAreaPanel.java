/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.input;

import com.evolveum.midpoint.web.component.prism.InputPanel;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;

public class TextAreaPanel<T> extends InputPanel {

    private static final String ID_INPUT = "input";

    public TextAreaPanel(String id, IModel<T> model, Integer rowsOverride) {
        super(id);

        final TextArea<T> text = new TextArea<T>(ID_INPUT, model) {

            @Override
            protected boolean shouldTrimInput() {
                return false;
            }

        };
        text.add(AttributeModifier.append("style", "max-width: 100%"));

        if (rowsOverride != null) {
            text.add(new AttributeModifier("rows", rowsOverride));
        }

        add(text);
    }

    @Override
    public FormComponent getBaseFormComponent() {
        return (FormComponent) get(ID_INPUT);
    }
}
