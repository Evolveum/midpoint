/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.validation;

import org.apache.wicket.validation.IValidator;

public abstract class ItemValidatorFactory<T> {

    private final String identifier;

    public ItemValidatorFactory(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public abstract IValidator<T> createValidatorInstance(ItemValidationContext context);
}
