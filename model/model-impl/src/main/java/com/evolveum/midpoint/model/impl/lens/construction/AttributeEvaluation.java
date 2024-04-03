/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import java.util.Collection;
import java.util.Collections;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.config.MappingConfigItem;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Evaluation of an attribute mapping in resource object construction (assigned/plain).
 */
class AttributeEvaluation<AH extends AssignmentHolderType, T>
        extends ItemEvaluation<AH, PrismPropertyValue<T>, ResourceAttributeDefinition<T>> {

    // [EP:M:OM] DONE 2/2
    AttributeEvaluation(
            ConstructionEvaluation<AH, ?> constructionEvaluation,
            ResourceAttributeDefinition<T> attributeDefinition,
            MappingConfigItem mappingConfigItem,
            OriginType origin,
            MappingKindType mappingKind) {
        super(
                constructionEvaluation,
                attributeDefinition.getItemName(),
                ShadowType.F_ATTRIBUTES.append(attributeDefinition.getItemName()),
                attributeDefinition,
                mappingConfigItem, // [EP:M:OM] DONE
                origin,
                mappingKind);
    }

    @Override
    protected String getItemType() {
        return "attribute";
    }

    @Override
    String getLifecycleState() {
        return itemDefinition.getLifecycleState();
    }

    @Override
    ResourceObjectTypeDefinition getAssociationTargetObjectDefinition() {
        return null;
    }
}
