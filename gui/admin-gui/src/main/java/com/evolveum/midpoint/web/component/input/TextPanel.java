/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.input;

import com.evolveum.midpoint.web.component.prism.InputPanel;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;


public class TextPanel<T> extends InputPanel {

    private static final String ID_INPUT = "input";

    public TextPanel(String id, IModel<T> model, boolean shouldTrim) {
        this(id, model, String.class, shouldTrim);
    }

    public TextPanel(String id, IModel<T> model) {
        this(id, model, String.class, true);
    }

    public TextPanel(String id, IModel<T> model, Class clazz) {
        this(id, model, clazz, true);
    }

    public TextPanel(String id, IModel<T> model, Class clazz, boolean shouldTrim) {
        super(id);

        final TextField<T> text = new TextField<T>(ID_INPUT, model) {

            @Override
            protected boolean shouldTrimInput() {
                return shouldTrim;
            }

            @Override
            public void convertInput() {
                T convertedValue = getConvertedInputValue();
                if (convertedValue != null){
                    setConvertedInput(convertedValue);
                } else {
                    super.convertInput();
                }
            }

            @Override
            public <C> IConverter<C> getConverter(Class<C> type) {
                return super.getConverter(type);
            }

            @Override
            public String[] getInputAsArray() {
                String[] values = TextPanel.this.getInputValues();
                if (values == null) {
                    return super.getInputAsArray();
                }
                return values;
            }

        };
        text.setType(clazz);
        add(text);
    }

    protected T getConvertedInputValue(){
        return null;
    }

    @Override
    public FormComponent<T> getBaseFormComponent() {
        return (FormComponent) get(ID_INPUT);
    }

    protected String[] getInputValues() {
        return null;
    }
}
