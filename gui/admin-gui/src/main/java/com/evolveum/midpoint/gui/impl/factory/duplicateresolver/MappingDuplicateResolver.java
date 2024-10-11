/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.duplicateresolver;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.duplication.DuplicationProcessHelper;
import com.evolveum.midpoint.schema.constants.SchemaConstants;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lskublik
 */
@Component
public class MappingDuplicateResolver extends ContainerDuplicateResolver<MappingType> {

    @Override
    public <C extends Containerable> boolean match(ItemDefinition<?> def, PrismContainerValue<C> parent) {
        if (!super.match(def) || parent == null) {
            return false;
        }
        if ((QNameUtil.match(def.getItemName(),ResourceAttributeDefinitionType.F_OUTBOUND)
                || QNameUtil.match(def.getItemName(),ResourceAttributeDefinitionType.F_INBOUND))
                && ItemPath.create(ResourceType.F_SCHEMA_HANDLING,
                    SchemaHandlingType.F_OBJECT_TYPE,
                    ResourceObjectTypeDefinitionType.F_ATTRIBUTE).equivalent(parent.getPath().namedSegmentsOnly())) {
            return true;
        }

        if (QNameUtil.match(def.getItemName(),MappingsType.F_MAPPING)
                && (ItemPath.create(AbstractRoleType.F_INDUCEMENT,
                AssignmentType.F_FOCUS_MAPPINGS).equivalent(parent.getPath().namedSegmentsOnly()))
                || ItemPath.create(AssignmentHolderType.F_ASSIGNMENT,
                AssignmentType.F_FOCUS_MAPPINGS).equivalent(parent.getPath().namedSegmentsOnly())) {
            return true;
        }

        return false;
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public MappingType duplicateObject(MappingType originalBean, PageBase pageBase) {
        PrismContainerValue<MappingType> originalObject = originalBean.asPrismContainerValue();
        PrismContainerValue<MappingType> duplicate =
                DuplicationProcessHelper.duplicateContainerValueDefault(originalObject);
        @NotNull MappingType duplicatedBean = duplicate.asContainerable();

        String copyOf = LocalizationUtil.translate("DuplicationProcessHelper.copyOf", new Object[]{originalBean.getName()});

        duplicatedBean
                .name(copyOf)
                .lifecycleState(SchemaConstants.LIFECYCLE_PROPOSED)
                .description(copyOf +
                        (originalBean.getDescription() == null ? "" : (System.lineSeparator() + originalBean.getDescription())));

        return duplicatedBean;
    }
}
