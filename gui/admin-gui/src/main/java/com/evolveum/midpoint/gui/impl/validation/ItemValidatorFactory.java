/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.validation;

import com.evolveum.midpoint.web.component.prism.InputPanel;

/**
 * TODO documentation @lazyman
 */
public abstract class ItemValidatorFactory {

    private final String identifier;

    public ItemValidatorFactory(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public abstract void attachValidator(InputPanel panel, ItemValidationContext context);

}
