/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.input;

import javax.xml.datatype.Duration;

import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;

import com.evolveum.midpoint.web.component.prism.InputPanel;

public class DurationPanel extends InputPanel {

    protected static final String ID_INPUT = "input";

    public DurationPanel(String id, IModel<Duration> model) {
        super(id);

        final TextField<Duration> text = new TextField<>(ID_INPUT, model) {

            @Override
            public void convertInput() {
                String durationStr = getBaseFormComponent().getRawInput();

                if (durationStr == null) {
                    setConvertedInput(null);
                }

                try {
                    setConvertedInput(getPageBase().getConverter(Duration.class).convertToObject(durationStr, getPageBase().getLocale()));
                } catch (Exception ex) {
                    this.error(getPageBase().getString("DurationPanel.incorrectValueError", getLabel().getObject()));
                }
            }

            @Override
            protected IConverter<?> createConverter(Class<?> type) {
                return getPageBase().getConverter(Duration.class);
            }

        };
        text.setType(Duration.class);
        add(text);

    }

    @SuppressWarnings("unchecked")
    @Override
    public FormComponent<Duration> getBaseFormComponent() {
        return (FormComponent<Duration>) get(ID_INPUT);
    }
}
