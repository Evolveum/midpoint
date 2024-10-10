/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.validator;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

import javax.xml.namespace.QName;

public class AssociationNameValidator implements IValidator<QName> {

    private static final Trace LOGGER = TraceManager.getTrace(AssociationNameValidator.class);

    private final IModel<PrismPropertyWrapper<QName>> itemModel;

    public AssociationNameValidator(IModel<PrismPropertyWrapper<QName>> itemModel) {
        this.itemModel = itemModel;
    }

    @Override
    public void validate(IValidatable<QName> validatable) {
        QName value = validatable.getValue();
        if (value == null) {
            return;
        }

        PrismPropertyWrapper<QName> item = itemModel.getObject();
        if (item == null) {
            return;
        }

        PrismObjectWrapper<ResourceType> objectWrapper = item.findObjectWrapper();

        int numberOfSameRef = WebPrismUtil.getNumberOfSameAssociationNames(objectWrapper.getValue(), value);

        boolean containsSameValue = false;

        try {
            containsSameValue = item.getValue() != null
                    && item.getValue().getRealValue() != null
                    && QNameUtil.match(item.getValue().getRealValue(), value);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't get value of " + item, e);
        }

        if ((containsSameValue && numberOfSameRef > 1) || (!containsSameValue && numberOfSameRef > 0)) {
            ValidationError error = new ValidationError();
            error.setMessage(LocalizationUtil.translate("AssociationNameValidator.sameValue", new Object[] {value}));
            validatable.error(error);
        }
    }
}
