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
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeSubjectDefinitionType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

import javax.xml.namespace.QName;
import java.util.List;

public class AssociationRefAttributeValidator implements IValidator<String> {

    private static final Trace LOGGER = TraceManager.getTrace(AssociationRefAttributeValidator.class);

    private final IModel<PrismPropertyWrapper<ItemPathType>> itemModel;

    public AssociationRefAttributeValidator(IModel<PrismPropertyWrapper<ItemPathType>> itemModel) {
        this.itemModel = itemModel;
    }

    @Override
    public void validate(IValidatable<String> validatable) {
        String value = validatable.getValue();
        if (StringUtils.isBlank(value)) {
            return;
        }

        QName qName = WebPrismUtil.convertStringWithPrefixToQName(value);

        if (qName == null) {
            return;
        }

        ItemName path = ItemName.fromQName(qName);


        PrismPropertyWrapper<ItemPathType> item = itemModel.getObject();
        if (item == null) {
            return;
        }

        PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> associationTypeValue =
                item.getParentContainerValue(ShadowAssociationTypeDefinitionType.class);
        if (associationTypeValue == null
                || associationTypeValue.getParent() == null
                || associationTypeValue.getParent().getValues().size() == 1) {
            return;
        }

        List<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> associationValues =
                ((PrismContainerWrapper<ShadowAssociationTypeDefinitionType>) associationTypeValue.getParent()).getValues();

        int numberOfSameRef = 0;

        for (PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> associationValue : associationValues) {
            try {
                PrismPropertyWrapper<ItemPathType> refProperty = associationValue.findProperty(
                        ItemPath.create(
                                ShadowAssociationTypeDefinitionType.F_SUBJECT,
                                ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION,
                                ShadowAssociationDefinitionType.F_REF));
                ItemPathType refItemName = refProperty.getValue().getRealValue();

                if (refItemName != null && path.equivalent(refItemName.getItemPath())) {
                    numberOfSameRef++;
                }
            } catch (SchemaException e) {
                LOGGER.error("Couldn't find association ref attribute in associationType " + value, e);
            }
        }

        boolean containsSameValue = false;

        try {
            containsSameValue = item.getValue() != null
                    && item.getValue().getRealValue() != null
                    && item.getValue().getRealValue().getItemPath().equivalent(path);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't get value of " + item, e);
        }

        if ((containsSameValue && numberOfSameRef > 1) || (!containsSameValue && numberOfSameRef > 0)) {
            ValidationError error = new ValidationError();
            error.setMessage(LocalizationUtil.translate(
                    "AssociationRefAttributeValidator.refAttributeExists",
                    new Object[]{value}));
            validatable.error(error);
        }
    }
}
