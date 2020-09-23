/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;

/**
 * Evaluation of an association mapping in resource object construction (assigned/plain).
 */
class AssociationEvaluation<AH extends AssignmentHolderType>
        extends ItemEvaluation<AH, PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>, RefinedAssociationDefinition> {

    AssociationEvaluation(ConstructionEvaluation<AH, ?> constructionEvaluation,
            RefinedAssociationDefinition associationDefinition, MappingType mappingBean,
            OriginType originType, MappingKindType mappingKind) {
        super(constructionEvaluation, associationDefinition.getName(), associationDefinition,
                constructionEvaluation.construction.getAssociationContainerDefinition(),
                mappingBean, originType, mappingKind);
    }

    @Override
    protected String getItemType() {
        return "association";
    }

    @Override
    RefinedObjectClassDefinition getAssociationTargetObjectClassDefinition() {
        return itemRefinedDefinition.getAssociationTarget();
    }
}
