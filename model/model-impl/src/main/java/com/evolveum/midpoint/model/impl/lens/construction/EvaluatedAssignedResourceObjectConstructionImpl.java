/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.schema.config.MappingConfigItem;
import com.evolveum.midpoint.schema.config.ResourceAttributeDefinitionConfigItem;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingKindType;

import static com.evolveum.midpoint.schema.config.ConfigurationItem.DESC;

/**
 * Evaluated resource object construction that is assigned to the focus.
 */
public class EvaluatedAssignedResourceObjectConstructionImpl<AH extends AssignmentHolderType>
        extends EvaluatedResourceObjectConstructionImpl<AH, AssignedResourceObjectConstruction<AH>> {

    private static final Trace LOGGER = TraceManager.getTrace(EvaluatedAssignedResourceObjectConstructionImpl.class);

    /**
     * Precondition: {@link ResourceObjectConstruction} is already evaluated and not ignored (has resource).
     */
    EvaluatedAssignedResourceObjectConstructionImpl(
            @NotNull AssignedResourceObjectConstruction<AH> construction,
            @NotNull ConstructionTargetKey key) {
        super(construction, key);
    }

    protected void initializeProjectionContext() {
        // projection context may not exist yet (existence might not be yet decided)
        setProjectionContext(
                construction.getLensContext().findFirstProjectionContext(targetKey, false));
    }

    @Override
    List<AttributeMapper<AH, ?, ?>> getAttributeMappers(ConstructionEvaluation<AH, ?> constructionEvaluation)
            throws ConfigurationException {

        List<AttributeMapper<AH, ?, ?>> attributesToEvaluate = new ArrayList<>();

        // [EP:CONST] DONE
        for (ResourceAttributeDefinitionConfigItem attributeConstrDefinitionCI : getTypedConfigItemRequired().getAttributes()) {
            QName attrName = attributeConstrDefinitionCI.getAttributeName();

            attributeConstrDefinitionCI.configCheck(
                    !attributeConstrDefinitionCI.hasInbounds(), "Cannot process inbound section in %s", DESC);

            // [EP:M:OM] DONE, transforming to [EP:CONSTR] (via attribute definition in the construction)
            MappingConfigItem outboundMappingCI =
                    attributeConstrDefinitionCI.configNonNull(
                            attributeConstrDefinitionCI.getOutbound(), "No outbound section in %s", DESC);

            var attributeDef =
                    attributeConstrDefinitionCI.configNonNull(
                            construction.findAttributeDefinition(attrName),
                            "Attribute '%s' not found in schema for resource object type %s on %s; as defined in %s",
                            attrName, getTypeIdentification(), construction.getResolvedResource().resource, DESC);

            if (!attributeDef.isVisible(constructionEvaluation.task.getExecutionMode())) {
                LOGGER.trace("Skipping processing outbound mapping for attribute {} because it is not visible in current "
                                + "execution mode", attributeDef);
                continue;
            }

            attributesToEvaluate.add(
                    new AttributeMapper<>(
                            constructionEvaluation,
                            attributeDef,
                            outboundMappingCI, // [EP:M:OM] DONE
                            OriginType.ASSIGNMENTS,
                            MappingKindType.CONSTRUCTION));
        }
        return attributesToEvaluate;
    }

    @Override
    List<AssociationMapper<AH>> getAssociationMappers(ConstructionEvaluation<AH, ?> constructionEvaluation)
            throws ConfigurationException {

        List<AssociationMapper<AH>> mappers = new ArrayList<>();

        for (var associationConstructionCI : getTypedConfigItemRequired().getAssociations()) {

            associationConstructionCI.configCheck(
                    !associationConstructionCI.hasInbounds(), "Cannot process inbound section in %s", DESC);

            var associationDef =
                    construction.findAssociationDefinitionRequired(
                            associationConstructionCI.getItemName(),
                            associationConstructionCI);

            if (!associationDef.isVisible(constructionEvaluation.task)) {
                LOGGER.trace("Skipping processing outbound mapping for association {} because it is not visible in current "
                        + "execution mode", associationDef);
                continue;
            }
            mappers.add(
                    new AssociationMapper<>(
                            constructionEvaluation,
                            associationDef,
                            associationConstructionCI.getOutboundMappingRequired(), // [EP:M:OM] DONE
                            OriginType.ASSIGNMENTS,
                            MappingKindType.CONSTRUCTION));
        }
        return mappers;
    }
}
