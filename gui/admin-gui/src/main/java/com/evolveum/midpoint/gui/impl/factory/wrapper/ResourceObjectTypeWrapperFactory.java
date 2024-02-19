/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.duplication.ContainerableDuplicateResolver;
import com.evolveum.midpoint.gui.impl.duplication.DuplicationProcessHelper;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.QNameUtil;

/**
 * @author lskublik
 */
@Component
public class ResourceObjectTypeWrapperFactory extends PrismContainerWrapperFactoryImpl<ResourceObjectTypeDefinitionType>
        implements ContainerableDuplicateResolver<ResourceObjectTypeDefinitionType> {

    @Override
    public boolean match(ItemDefinition<?> def) {
        return QNameUtil.match(def.getTypeName(), ResourceObjectTypeDefinitionType.COMPLEX_TYPE);
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public ResourceObjectTypeDefinitionType duplicateObject(ResourceObjectTypeDefinitionType originalBean) {
        PrismContainerValue<ResourceObjectTypeDefinitionType> originalObject = originalBean.asPrismContainerValue();
        PrismContainerValue<ResourceObjectTypeDefinitionType> duplicate =
                DuplicationProcessHelper.duplicateContainerValueDefault(originalObject);
        @NotNull ResourceObjectTypeDefinitionType duplicatedBean = duplicate.asContainerable();

        String name;
        if (StringUtils.isEmpty(originalBean.getDisplayName())) {
            name = LocalizationUtil.translateEnum(duplicatedBean.getKind()) + "(" + originalBean.getIntent() + ")";
        } else {
            name = originalBean.getDisplayName();
        }

        String copyOf = LocalizationUtil.translate("DuplicationProcessHelper.copyOf", new Object[]{name});

        duplicatedBean
                .displayName(copyOf)
                .lifecycleState(SchemaConstants.LIFECYCLE_PROPOSED)
                .intent(LocalizationUtil.translate("DuplicationProcessHelper.copyOf", new Object[]{originalBean.getIntent()}))
                ._default(null)
                .description(copyOf +
                        (originalBean.getDescription() == null ? "" : (System.lineSeparator() + originalBean.getDescription())));
        return duplicatedBean;
    }

    @Override
    @PostConstruct
    public void register() {
        getRegistry().addToRegistry((ContainerableDuplicateResolver) this);
    }

}
