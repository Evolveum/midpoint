/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.input.validator;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.impl.component.form.ReferenceAutocompletePanel;
import com.evolveum.midpoint.prism.Referencable;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.util.convert.IConverter;

import com.evolveum.midpoint.web.component.prism.InputPanel;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;

public class ReferenceAutocompleteValidator extends NotNullValidator<Referencable> {

    private static final long serialVersionUID = 1L;

    private AutoCompleteTextPanel referenceAutocompletePanel;

    public ReferenceAutocompleteValidator(AutoCompleteTextPanel referenceAutocompletePanel) {
        super("ReferenceAutocompleteValidator.incorrectValueError");
        this.referenceAutocompletePanel = referenceAutocompletePanel;
    }

    @Override
    public void validate(IValidatable<Referencable> validatable) {
        if (referenceAutocompletePanel == null) {
            return;
        }

        String refName = referenceAutocompletePanel.getBaseFormComponent().getRawInput();

        if (StringUtils.isBlank(refName)) {
            return;
        }

        super.validate(validatable);
    }
}
