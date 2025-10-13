/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.validator;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.IValidatable;

public class AssociationMappingNameValidator extends MappingNameValidator {

    public AssociationMappingNameValidator(IModel<PrismPropertyWrapper<String>> itemModel) {
        super(itemModel);
    }

    @Override
    public void validate(IValidatable<String> validatable) {
        String value = validatable.getValue();
        if (StringUtils.isEmpty(value)) {
            return;
        }

        PrismPropertyWrapper<String> item = getItemModel().getObject();
        if (item == null) {
            return;
        }

        PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> associationType =
                item.getParentContainerValue(ShadowAssociationTypeDefinitionType.class);

        if (associationType == null || associationType.getRealValue() == null) {
            return;
        }

        ShadowAssociationTypeDefinitionType associationTypeBean = associationType.getRealValue();
        alreadyExistMapping(
                associationType,
                LocalizationUtil.translate(
                        "ObjectTypeMappingNameValidator.sameValue",
                        new Object[] { value, GuiDisplayNameUtil.getDisplayName(associationTypeBean) }),
                value,
                validatable);
    }
}
