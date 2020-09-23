/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

/**
 * Evaluation of an attribute mapping in resource object construction (assigned/plain).
 */
class AttributeEvaluation<AH extends AssignmentHolderType> extends ItemEvaluation<AH, PrismPropertyValue<?>, PrismPropertyDefinition<?>, RefinedAttributeDefinition<?>> {

    AttributeEvaluation(ConstructionEvaluation<AH, ?> constructionEvaluation,
            RefinedAttributeDefinition<?> refinedAttributeDefinition, MappingType mappingBean,
            OriginType origin, MappingKindType mappingKind) {
        super(constructionEvaluation, refinedAttributeDefinition.getItemName(), refinedAttributeDefinition,
                refinedAttributeDefinition, mappingBean, origin, mappingKind);
    }

    @Override
    protected String getItemType() {
        return "attribute";
    }

    @Override
    RefinedObjectClassDefinition getAssociationTargetObjectClassDefinition() {
        return null;
    }
}
