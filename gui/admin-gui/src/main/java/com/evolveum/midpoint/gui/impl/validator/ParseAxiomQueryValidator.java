/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.validator;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.factory.panel.SearchFilterTypeForQueryModel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class ParseAxiomQueryValidator implements IValidator<String> {

    private static final Trace LOGGER = TraceManager.getTrace(ParseAxiomQueryValidator.class);

    private final SearchFilterTypeForQueryModel model;

    public ParseAxiomQueryValidator(SearchFilterTypeForQueryModel model) {
        this.model = model;
    }

    @Override
    public void validate(IValidatable<String> validatable) {
        String value = validatable.getValue();
        if (StringUtils.isBlank(value)) {
            return;
        }

        try {
            model.parseQueryWithoutSetValue(value);
        } catch (Exception e) {
            LOGGER.debug("Cannot parse filter", e);

            ValidationError error = new ValidationError();
            error.setMessage(LocalizationUtil.translate(
                    "ParseAxiomQueryValidator.cannotParseFilter",
                    new Object[]{e.getLocalizedMessage()}));
            validatable.error(error);
        }

    }
}
