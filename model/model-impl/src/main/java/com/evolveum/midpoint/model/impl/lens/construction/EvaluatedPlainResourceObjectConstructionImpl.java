/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.processor.ResourceAssociationDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

/**
 * Evaluated resource object construction that is defined in the schemaHandling part of resource definition.
 *
 * @author Radovan Semancik
 */
public class EvaluatedPlainResourceObjectConstructionImpl<AH extends AssignmentHolderType>
        extends EvaluatedResourceObjectConstructionImpl<AH, PlainResourceObjectConstruction<AH>> {

    private static final Trace LOGGER = TraceManager.getTrace(EvaluatedPlainResourceObjectConstructionImpl.class);

    /**
     * Precondition: construction is already evaluated and not ignored (has resource)
     */
    EvaluatedPlainResourceObjectConstructionImpl(
            @NotNull PlainResourceObjectConstruction<AH> construction,
            @NotNull LensProjectionContext projectionContext,
            @NotNull ConstructionTargetKey targetKey) {
        super(construction, targetKey);
        setProjectionContext(projectionContext);
    }

    @Override
    protected @NotNull LensProjectionContext getProjectionContext() {
        return Objects.requireNonNull(super.getProjectionContext());
    }

    @Override
    protected void initializeProjectionContext() {
        // projection context was passed to the constructor
    }

    @Override
    protected List<AttributeEvaluation<AH>> getAttributesToEvaluate(ConstructionEvaluation<AH, ?> constructionEvaluation) {
        List<AttributeEvaluation<AH>> attributesToEvaluate = new ArrayList<>();

        ResourceObjectDefinition objectDefinition = construction.getResourceObjectDefinitionRequired();

        for (ResourceAttributeDefinition<?> attributeDef : objectDefinition.getAttributeDefinitions()) {
            MappingType outboundMappingBean = attributeDef.getOutboundMappingBean();
            if (outboundMappingBean == null) {
                continue;
            }
            if (attributeDef.isIgnored(LayerType.MODEL)) {
                LOGGER.trace("Skipping processing outbound mapping for attribute {} because it is ignored", attributeDef);
                continue;
            }
            if (!attributeDef.isVisible(constructionEvaluation.task.getExecutionMode())) {
                LOGGER.trace("Skipping processing outbound mapping for attribute {} because it is not visible in current "
                                + "execution mode", attributeDef);
                continue;
            }

            var origin = ConfigurationItemOrigin.inResourceOrAncestor(construction.getResource());

            attributesToEvaluate.add(
                    new AttributeEvaluation<>(
                            constructionEvaluation, attributeDef,
                            ConfigurationItem.of(outboundMappingBean, origin),
                            OriginType.OUTBOUND, MappingKindType.OUTBOUND));
        }

        return attributesToEvaluate;
    }

    @Override
    protected List<AssociationEvaluation<AH>> getAssociationsToEvaluate(ConstructionEvaluation<AH, ?> constructionEvaluation) {
        List<AssociationEvaluation<AH>> associationsToEvaluate = new ArrayList<>();

        ResourceObjectDefinition objectDefinition = construction.getResourceObjectDefinitionRequired();
        for (ResourceAssociationDefinition associationDefinition : objectDefinition.getAssociationDefinitions()) {
            MappingType outboundMappingBean = associationDefinition.getOutboundMappingType();
            if (outboundMappingBean == null) {
                continue;
            }
            if (!associationDefinition.isVisible(constructionEvaluation.task.getExecutionMode())) {
                LOGGER.trace("Skipping processing outbound mapping for association {} because it is not visible in current "
                        + "execution mode", associationDefinition);
                continue;
            }

            var origin = ConfigurationItemOrigin.inResourceOrAncestor(construction.getResource());

            associationsToEvaluate.add(
                    new AssociationEvaluation<>(
                            constructionEvaluation, associationDefinition,
                            ConfigurationItem.of(outboundMappingBean, origin),
                            OriginType.OUTBOUND, MappingKindType.OUTBOUND));
        }
        return associationsToEvaluate;
    }
}
