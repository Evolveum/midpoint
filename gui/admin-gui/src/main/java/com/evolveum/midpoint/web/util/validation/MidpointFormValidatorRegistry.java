/*
 * Copyright (c) 2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
