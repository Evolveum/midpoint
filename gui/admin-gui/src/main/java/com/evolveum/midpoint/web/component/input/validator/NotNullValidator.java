/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.input.validator;

import org.apache.wicket.validation.INullAcceptingValidator;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;

public class NotNullValidator<T> implements INullAcceptingValidator<T>{

    private static final long serialVersionUID = 1L;
    private String key;
    private boolean useModel = false;

    public NotNullValidator(String errorMessageKey) {
        this.key = errorMessageKey;
    }

    public void setUseModel(boolean useModel) {
        this.useModel = useModel;
    }

    @Override
    public void validate(IValidatable<T> validatable) {
        if (validatable.getValue() == null) {
            if (useModel && validatable.getModel() != null && validatable.getModel().getObject() != null) {
                useModel = false;
                return;
            }
            ValidationError err = new ValidationError();
            err.addKey(key);
            validatable.error(err);

        }

    }

}
