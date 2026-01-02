/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.input.validator;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;

import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.prism.PrismContainerValue;

import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.INullAcceptingValidator;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;

import java.io.Serial;

public class NotNullValidator<T> implements INullAcceptingValidator<T>{

    @Serial private static final long serialVersionUID = 1L;
    private final String key;
    private boolean useModel = false;
    private IModel<PrismPropertyWrapper<T>> propertyWrapper = null;
    private boolean forceValidation;

    public NotNullValidator(String errorMessageKey) {
        this.key = errorMessageKey;
    }

    public NotNullValidator(String errorMessageKey, IModel<PrismPropertyWrapper<T>> propertyWrapper, boolean forceValidation) {
        this.propertyWrapper = propertyWrapper;
        this.key = errorMessageKey;
        this.forceValidation = forceValidation;
    }

    public void setUseModel(boolean useModel) {
        this.useModel = useModel;
    }

    @Override
    public void validate(IValidatable<T> validatable) {
        if(skipValidation()) {
            return;
        }

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

    private boolean skipValidation() {
        if (forceValidation) {
            return false;
        }
        if (propertyWrapper == null) {
            return false;
        }
        PrismContainerValueWrapper parentContainer = propertyWrapper.getObject().getParent();
        if (parentContainer == null || parentContainer.getNewValue() == null
                || (parentContainer.getParent() != null && parentContainer.getParent().isMultiValue())) {
            return false;
        }
        PrismContainerValue cleanedUpValue =
                WebPrismUtil.cleanupEmptyContainerValue(parentContainer.getNewValue().clone());
        return cleanedUpValue == null || cleanedUpValue.isEmpty();
    }
}
