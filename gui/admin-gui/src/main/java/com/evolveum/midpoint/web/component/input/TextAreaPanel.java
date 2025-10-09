/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.input;

import com.evolveum.midpoint.gui.api.component.form.TextArea;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;

public class TextAreaPanel<T> extends InputPanel {

    private static final String ID_INPUT = "input";

    public TextAreaPanel(String id, IModel<T> model) {
        this(id, model, null);
    }

    public TextAreaPanel(String id, IModel<T> model, Integer rowsOverride) {
        super(id);

        final TextArea<T> text = new TextArea<>(ID_INPUT, model) {

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
