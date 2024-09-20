/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.validator;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.util.AssociationChildWrapperUtil;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.IValidatable;
import org.jetbrains.annotations.NotNull;

public class AssociationMappingNameValidator extends MappingNameValidator {

    private final PageBase pageBase;

    public AssociationMappingNameValidator(IModel<PrismPropertyWrapper<String>> itemModel, PageBase pageBase) {
        super(itemModel);
        this.pageBase = pageBase;
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
        if (alreadyExistMapping(
                associationType,
                LocalizationUtil.translate(
                        "ObjectTypeMappingNameValidator.sameValue",
                        new Object[] { value, GuiDisplayNameUtil.getDisplayName(associationTypeBean) }),
                value,
                validatable)) {
            return;
        }

        ResourceSchema schema = getResourceSchema(pageBase);

        if (schema == null) {
            return;
        }

        ResourceObjectTypeIdentificationType objectType =
                AssociationChildWrapperUtil.getFirstObjectTypeOfSubject(item.getValues().iterator().next());
        if (objectType == null) {
            return;
        }

        ResourceObjectTypeDefinition objectTypeDef = schema.getObjectTypeDefinition(ResourceObjectTypeIdentification.of(objectType));
        if (objectTypeDef == null) {
            return;
        }

        ResourceObjectTypeDefinitionType bean = objectTypeDef.getDefinitionBean().clone();

        String errorMessage = LocalizationUtil.translate(
                "ObjectTypeMappingNameValidator.sameValue",
                new Object[] { value, GuiDisplayNameUtil.getDisplayName(bean)});

        PrismObjectDefinition<ResourceType> resourceDef = PrismContext.get().getSchemaRegistry().findObjectDefinitionByType(ResourceType.COMPLEX_TYPE);
        alreadyExistMapping(
                bean,
                resourceDef.findContainerDefinition(ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE)),
                errorMessage,
                value,
                validatable,
                pageBase);
    }
}
