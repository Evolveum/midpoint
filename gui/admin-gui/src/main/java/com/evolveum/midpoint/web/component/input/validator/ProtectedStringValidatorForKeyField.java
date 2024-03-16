/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.input.validator;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;

import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.prism.xml.ns._public.types_3.ExternalDataType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.validation.INullAcceptingValidator;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;

import java.util.Optional;

public class ProtectedStringValidatorForKeyField implements INullAcceptingValidator<String> {

    private static final Trace LOGGER = TraceManager.getTrace(ProtectedStringValidatorForKeyField.class);

    private static final long serialVersionUID = 1L;

    private final IModel<PrismPropertyValueWrapper<ProtectedStringType>> protectedValueWrapperModel;
    private final FeedbackAlerts feedback;

    public ProtectedStringValidatorForKeyField(IModel<PrismPropertyValueWrapper<ProtectedStringType>> protectedValueWrapperModel, FeedbackAlerts feedback) {
        this.protectedValueWrapperModel = protectedValueWrapperModel;
        this.feedback = feedback;
    }

    @Override
    public void validate(IValidatable<String> validatable) {
        if (protectedValueWrapperModel == null || protectedValueWrapperModel.getObject() == null
                || protectedValueWrapperModel.getObject().getRealValue() == null) {
            return;
        }

        if (protectedValueWrapperModel.getObject().getRealValue().getExternalData() == null) {
            return;
        }

        ProtectedStringType protectedType = protectedValueWrapperModel.getObject().getRealValue().clone();

        ExternalDataType externalData = protectedType.getExternalData();
        protectedType.clear();

        if (externalData == null) {
            externalData = new ExternalDataType();
        }

        externalData.setKey(validatable.getValue());
        protectedType.setExternalData(externalData);

        try {
            getProtector().decrypt(protectedType);
        } catch (Exception e) {
            LOGGER.debug("Couldn't process secret provider.", e);
            ValidationError err = new ValidationError();
            if (e instanceof CommonException commonException) {
                LocalizableMessage friendlyMessage = commonException.getUserFriendlyMessage();
                if (friendlyMessage != null) {
                    err.setMessage(LocalizationUtil.translateMessage(friendlyMessage));
                } else {
                    err.addKey("ProtectedStringValidator.incorrectValueError");
                }
            } else {
                err.addKey("ProtectedStringValidator.incorrectValueError");
            }
            validatable.error(err);
            if (feedback != null) {
                Optional<AjaxRequestTarget> target = RequestCycle.get().find(AjaxRequestTarget.class);
                if (target.isPresent()) {
                    target.get().add(feedback);
                }
            }
        }
    }

    private Protector getProtector() {
        return ((MidPointApplication) Application.get()).getProtector();
    }
}
