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

import com.evolveum.midpoint.schema.config.MappingConfigItem;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

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
    List<AttributeMapper<AH, ?, ?>> getAttributeMappers(ConstructionEvaluation<AH, ?> constructionEvaluation) {
        var mappers = new ArrayList<AttributeMapper<AH, ?, ?>>();

        ResourceObjectDefinition objectDefinition = construction.getResourceObjectDefinitionRequired();

        for (var attributeDef : objectDefinition.getAttributeDefinitions()) {
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

            // [EP:M:OM] DONE: the construction sits in the resource, so the origin is correct
            var origin = ConfigurationItemOrigin.inResourceOrAncestor(construction.getResource());

            mappers.add(
                    new AttributeMapper<>(
                            constructionEvaluation, attributeDef,
                            MappingConfigItem.of(outboundMappingBean, origin), // [EP:M:OM] DONE
                            OriginType.OUTBOUND, MappingKindType.OUTBOUND));
        }

        return mappers;
    }

    @Override
    List<AssociationMapper<AH>> getAssociationMappers(ConstructionEvaluation<AH, ?> constructionEvaluation) {
        List<AssociationMapper<AH>> mappers = new ArrayList<>();

        ResourceObjectDefinition objectDefinition = construction.getResourceObjectDefinitionRequired();
        for (var associationDefinition : objectDefinition.getAssociationDefinitions()) {
            for (var mappingBean : associationDefinition.getOutboundMappingBeans()) {
                if (associationDefinition.isVisible(constructionEvaluation.task)) {
                    var origin = ConfigurationItemOrigin.inResourceOrAncestor(construction.getResource());
                    mappers.add(
                            new AssociationMapper<>(
                                    constructionEvaluation, associationDefinition,
                                    MappingConfigItem.of(mappingBean, origin),
                                    OriginType.OUTBOUND, MappingKindType.OUTBOUND));
                } else {
                    LOGGER.trace("Skipping processing outbound mapping for association {} because it is not visible in current "
                            + "execution mode", associationDefinition);
                }
            }
        }
        return mappers;
    }
}
