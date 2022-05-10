/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.input.validator;

import com.evolveum.midpoint.web.component.prism.InputPanel;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.util.convert.IConverter;

import javax.xml.datatype.Duration;

public class DurationValidator extends AbstractFormValidator {

    private static final long serialVersionUID = 1L;

    private InputPanel durationField;
    private IConverter<Duration> converter;

    public DurationValidator(InputPanel durationField, IConverter<Duration> converter) {
        this.durationField = durationField;
        this.converter = converter;
    }

    @Override
    public FormComponent<?>[] getDependentFormComponents() {
        if (durationField == null) {
            return new FormComponent[0];
        }

        return new FormComponent[] { durationField.getBaseFormComponent() };
    }

    @Override
    public void validate(Form<?> form) {
        if (durationField == null) {
            return;
        }

        String durationStr = (String) durationField.getBaseFormComponent().getRawInput();

        if (durationStr == null) {
            return;
        }

        try {
            converter.convertToObject(durationStr, null);
        } catch (Exception ex) {
            form.error(form.getString("DurationPanel.incorrectValueError"));
        }
    }
}
