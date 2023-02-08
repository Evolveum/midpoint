/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * Evaluation of an attribute mapping in resource object construction (assigned/plain).
 */
class AttributeEvaluation<AH extends AssignmentHolderType>
        extends ItemEvaluation<AH, PrismPropertyValue<?>, PrismPropertyDefinition<?>, ResourceAttributeDefinition<?>> {

    AttributeEvaluation(ConstructionEvaluation<AH, ?> constructionEvaluation,
            ResourceAttributeDefinition<?> refinedAttributeDefinition, MappingType mappingBean,
            OriginType origin, MappingKindType mappingKind) {
        super(constructionEvaluation, refinedAttributeDefinition.getItemName(),
                ShadowType.F_ATTRIBUTES.append(refinedAttributeDefinition.getItemName()),
                refinedAttributeDefinition,
                refinedAttributeDefinition, mappingBean, origin, mappingKind);
    }

    @Override
    protected String getItemType() {
        return "attribute";
    }

    @Override
    String getLifecycleState() {
        return itemRefinedDefinition.getLifecycleState();
    }

    @Override
    ResourceObjectTypeDefinition getAssociationTargetObjectClassDefinition() {
        return null;
    }

    @Override
    protected Collection<PrismPropertyValue<?>> getOriginalTargetValuesFromShadow(@NotNull PrismObject<ShadowType> shadow) {
        PrismProperty<?> attribute = shadow.findProperty(itemPath);
        if (attribute != null) {
            //noinspection unchecked
            return (Collection) attribute.getValues();
        } else {
            // Either the projection is fully loaded and the attribute does not exist,
            // or the projection is not loaded (contrary to the fact that loading was requested).
            // In both cases the wisest approach is to return empty list, keeping mapping from failing,
            // and not removing anything. In the future we may consider issuing a warning, if we don't have
            // full shadow, and range specification is present.
            return Collections.emptyList();
        }
    }
}
