/*
 * Copyright (c) 2015 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.util.validation;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author semancik
 *
 */
public class MidpointFormValidatorRegistry {

    private Collection<MidpointFormValidator> validators = new ArrayList<>();

    public void registerValidator(MidpointFormValidator validator) {
        validators.add(validator);
    }

    public Collection<MidpointFormValidator> getValidators() {
        return validators;
    }

}
