/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.config.MappingConfigItem;
import com.evolveum.midpoint.schema.processor.ResourceAssociationDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Evaluation of an association mapping in resource object construction (assigned/plain).
 */
public class AssociationEvaluation<AH extends AssignmentHolderType>
        extends ItemEvaluation<AH, PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>, ResourceAssociationDefinition> {

    // [EP:M:OM] DONE 2/2
    AssociationEvaluation(
            ConstructionEvaluation<AH, ?> constructionEvaluation,
            ResourceAssociationDefinition associationDefinition,
            MappingConfigItem mappingConfigItem,
            OriginType originType,
            MappingKindType mappingKind) {
        super(
                constructionEvaluation,
                associationDefinition.getName(),
                ShadowType.F_ASSOCIATION.append(associationDefinition.getName()),
                associationDefinition,
                constructionEvaluation.construction.getAssociationContainerDefinition(),
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
        return itemRefinedDefinition.getLifecycleState();
    }

    @Override
    ResourceObjectTypeDefinition getAssociationTargetObjectClassDefinition() {
        return itemRefinedDefinition.getAssociationTarget();
    }

    @Override
    protected Collection<PrismContainerValue<ShadowAssociationType>> getOriginalTargetValuesFromShadow(
            @NotNull PrismObject<ShadowType> shadow) {
        // Note that it's possible that association values are simply not known. However, returning
        // an empty list is the best we can do in such situations.
        List<ShadowAssociationType> allValues = shadow.asObjectable().getAssociation();

        //noinspection unchecked
        return allValues.stream()
                .filter(value -> QNameUtil.match(value.getName(), itemName))
                .map(value -> (PrismContainerValue<ShadowAssociationType>) value.asPrismContainerValue())
                .collect(Collectors.toList());
    }
}
