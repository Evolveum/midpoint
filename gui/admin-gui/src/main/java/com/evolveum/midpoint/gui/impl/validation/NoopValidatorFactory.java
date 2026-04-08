/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.validation;

import com.evolveum.midpoint.web.component.prism.InputPanel;

import org.springframework.stereotype.Component;

/**
 * Validator that does nothing.
 *
 * It can be used in places where validation was predefined or hardcoded in code, but user that overrides
 * panel (container) item doesn't want any validation to be performed.
 */
@Component
public class NoopValidatorFactory extends ItemValidatorFactory {

    public static final String IDENTIFIER = "Noop";

    public NoopValidatorFactory() {
        super(IDENTIFIER);
    }

    @Override
    public void attachValidator(InputPanel panel, ItemValidationContext context) {
        // intentionally do nothing
    }
}
