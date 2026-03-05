/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.validation;

import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.markup.html.form.FormComponent;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.web.component.prism.InputPanel;

@Component
public class UniqueObjectNameValidatorFactory extends ItemValidatorFactory {

    public static final String IDENTIFIER = "UniqueObjectName";

    public UniqueObjectNameValidatorFactory() {
        super(IDENTIFIER);
    }

    @Override
    public void attachValidator(InputPanel panel, ItemValidationContext context) {
        FormComponent<?> formComponent = panel.getBaseFormComponent();
        if (formComponent instanceof AbstractTextComponent<?> text) {
            UniqueObjectNameValidator<?> validator = new UniqueObjectNameValidator<>(context.type(), context.page());
            text.add(validator);
        }
    }
}
