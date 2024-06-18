/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.schema.config.MappingConfigItem;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Evaluation of an attribute mapping in resource object construction (assigned/plain).
 */
class AttributeMapper<AH extends AssignmentHolderType, T>
        extends ItemMapper<AH, PrismPropertyValue<T>, ShadowSimpleAttributeDefinition<T>> {

    // [EP:M:OM] DONE 2/2
    AttributeMapper(
            ConstructionEvaluation<AH, ?> constructionEvaluation,
            ShadowSimpleAttributeDefinition<T> attributeDefinition,
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
    ShadowReferenceAttributeDefinition getAssociationDefinition() {
        return null;
    }
}
