/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.validator;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.IValidatable;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class ObjectTypeMappingNameValidator extends MappingNameValidator {

    public ObjectTypeMappingNameValidator(IModel<PrismPropertyWrapper<String>> itemModel) {
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

        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> objectType =
                item.getParentContainerValue(ResourceObjectTypeDefinitionType.class);

        if (objectType == null || objectType.getRealValue() == null) {
            return;
        }

        ResourceObjectTypeDefinitionType objectTypeBean = objectType.getRealValue();
        alreadyExistMapping(
                objectType,
                LocalizationUtil.translate(
                        "ObjectTypeMappingNameValidator.sameValue",
                        new Object[] { value, GuiDisplayNameUtil.getDisplayName(objectTypeBean) }),
                value,
                validatable);
    }
}
