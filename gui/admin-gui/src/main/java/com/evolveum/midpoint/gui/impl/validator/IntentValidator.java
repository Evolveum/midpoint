/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.validator;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

import java.util.List;

public class IntentValidator implements IValidator<String> {

    private static final Trace LOGGER = TraceManager.getTrace(IntentValidator.class);

    private final IModel<PrismPropertyWrapper<String>> itemModel;

    public IntentValidator(IModel<PrismPropertyWrapper<String>> itemModel) {
        this.itemModel = itemModel;
    }

    @Override
    public void validate(IValidatable<String> validatable) {
        String value = validatable.getValue();
        if (value == null) {
            return;
        }

        PrismPropertyWrapper<String> item = itemModel.getObject();
        if (item == null) {
            return;
        }

        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> parent =
                item.getParentContainerValue(ResourceObjectTypeDefinitionType.class);

        if (parent == null
                || parent.getParent() == null
                || parent.getParent().getValues().size() == 1) {
            return;
        }

        ShadowKindType kind = null;
        try {
            PrismPropertyWrapper<ShadowKindType> kindProperty = parent.findProperty(ResourceObjectTypeDefinitionType.F_KIND);
            kind = kindProperty.getValue().getRealValue();
        } catch (SchemaException e) {
            LOGGER.error("Couldn't find intent attribute in objectType " + value, e);
        }

        if (kind == null) {
            return;
        }

        List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> objectTypeValues =
                ((PrismContainerWrapper<ResourceObjectTypeDefinitionType>) parent.getParent()).getValues();

        int numberOfSameRef = 0;

        for (PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> objectTypeValue : objectTypeValues) {
            try {
                PrismPropertyWrapper<ShadowKindType> kindProperty = objectTypeValue.findProperty(ResourceObjectTypeDefinitionType.F_KIND);
                ShadowKindType valueKind = kindProperty.getValue().getRealValue();

                if (kind != valueKind) {
                    continue;
                }
            } catch (SchemaException e) {
                LOGGER.error("Couldn't find intent attribute in objectType " + value, e);
            }

            try {
                PrismPropertyWrapper<String> intentProperty = objectTypeValue.findProperty(ResourceObjectTypeDefinitionType.F_INTENT);
                String intent = intentProperty.getValue().getRealValue();

                if (StringUtils.equals(intent, value)) {
                    numberOfSameRef++;
                }
            } catch (SchemaException e) {
                LOGGER.error("Couldn't find intent attribute in objectType " + value, e);
            }
        }

        boolean containsSameValue = false;

        try {
            containsSameValue = item.getValue() != null
                    && item.getValue().getRealValue() != null
                    && StringUtils.equals(item.getValue().getRealValue(), value);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't get value of " + item, e);
        }

        if ((containsSameValue && numberOfSameRef > 1) || (!containsSameValue && numberOfSameRef > 0)) {
            ValidationError error = new ValidationError();
            error.setMessage(LocalizationUtil.translate(
                    "IntentValidator.intentAttributeExists",
                    new Object[]{value}));
            validatable.error(error);
        }
    }
}
