/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.validator;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.PrismContext;

import com.evolveum.midpoint.xml.ns._public.prism_schema_3.PrismSchemaType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

public class SchemaDefaultPrefixValidator implements IValidator<String> {

    private final IModel<PrismPropertyWrapper<String>> itemModel;

    public SchemaDefaultPrefixValidator(IModel<PrismPropertyWrapper<String>> itemModel) {
        this.itemModel = itemModel;
    }

    @Override
    public void validate(IValidatable<String> validatable) {
        String value = validatable.getValue();
        if (StringUtils.isBlank(value)) {
            return;
        }

        if (!PrismContext.get().getSchemaRegistry().getNamespacePrefixMapper().containsPrefix(value)) {
            return;
        }

        PrismPropertyWrapper<String> item = itemModel.getObject();
        if (item == null) {
            return;
        }

        PrismContainerValueWrapper<PrismSchemaType> prismSchema = item.getParentContainerValue(PrismSchemaType.class);
        if (prismSchema == null) {
            return;
        }

        PrismSchemaType prismSchemaBean = prismSchema.getRealValue();
        if (prismSchemaBean == null) {
            return;
        }

        if (StringUtils.isBlank(prismSchemaBean.getNamespace())) {
            return;
        }

        String foundPrefix = PrismContext.get().getSchemaRegistry().getNamespacePrefixMapper().getPrefix(prismSchemaBean.getNamespace());
        if (value.equals(foundPrefix)) {
            return;
        }

        ValidationError error = new ValidationError();
        error.addKey("SchemaDefaultPrefixValidator.prefixExist");
        error.setVariable("0", value);
        error.setMessage("Prefix '" + value + "' already exist.");
        validatable.error(error);
    }
}
