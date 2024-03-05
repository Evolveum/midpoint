/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.schema.config.MappingConfigItem;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ShadowAssociationDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationValueType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Evaluation of an association mapping in resource object construction (assigned/plain).
 */
class AssociationEvaluation<AH extends AssignmentHolderType>
        extends ItemEvaluation<AH, PrismContainerValue<ShadowAssociationValueType>, ShadowAssociationDefinition> {

    // [EP:M:OM] DONE 2/2
    AssociationEvaluation(
            ConstructionEvaluation<AH, ?> constructionEvaluation,
            ShadowAssociationDefinition associationDefinition,
            MappingConfigItem mappingConfigItem,
            OriginType originType,
            MappingKindType mappingKind) {
        super(
                constructionEvaluation,
                associationDefinition.getName(),
                ShadowType.F_ASSOCIATIONS.append(associationDefinition.getName()),
                associationDefinition,
                mappingConfigItem, // [EP:M:OM] DONE
                originType,
                mappingKind);
    }

    @Override
    protected String getItemType() {
        return "association";
    }

    @Override
    String getLifecycleState() {
        return itemDefinition.getLifecycleState();
    }

    @Override
    ResourceObjectTypeDefinition getAssociationTargetObjectClassDefinition() {
        return itemDefinition.getAssociationTarget();
    }
}
