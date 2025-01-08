/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.schema.config.MappingConfigItem;
import com.evolveum.midpoint.schema.processor.ShadowAssociationDefinition;
import com.evolveum.midpoint.schema.processor.ShadowAssociationValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Evaluation of an association mapping in resource object construction (assigned/plain).
 */
class AssociationMapper<AH extends AssignmentHolderType>
        extends ShadowItemMapper<AH, ShadowAssociationValue, ShadowAssociationDefinition> {

    /**
     * Traditional/legacy association evaluation by a single mapping (just like an attribute is evaluated).
     * Expected to be invoked during assignment processing and "plain" outbound processing.
     *
     * @see ResourceObjectConstruction#evaluate(Task, OperationResult)
     *
     * [EP:M:OM] DONE 2/2
     */
    AssociationMapper(
            ConstructionEvaluation<AH, ?> constructionEvaluation,
            ShadowAssociationDefinition associationDefinition,
            MappingConfigItem mappingConfigItem,
            OriginType originType,
            MappingKindType mappingKind) {
        super(
                constructionEvaluation,
                associationDefinition.getItemName(),
                ShadowType.F_ASSOCIATIONS.append(associationDefinition.getItemName()),
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
    ShadowAssociationDefinition getAssociationDefinition() {
        return itemDefinition;
    }

    @Override
    boolean isItemLoaded(LensProjectionContext projectionContext) throws SchemaException, ConfigurationException {
        return projectionContext.isAssociationLoaded(getItemName());
    }
}
