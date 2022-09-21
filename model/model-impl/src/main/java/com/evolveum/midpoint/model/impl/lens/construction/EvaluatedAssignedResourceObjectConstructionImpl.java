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

import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.processor.ResourceAssociationDefinition;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.util.ItemPathTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Evaluated resource object construction that is assigned to the focus.
 */
public class EvaluatedAssignedResourceObjectConstructionImpl<AH extends AssignmentHolderType>
        extends EvaluatedResourceObjectConstructionImpl<AH, AssignedResourceObjectConstruction<AH>> {

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
    protected List<AttributeEvaluation<AH>> getAttributesToEvaluate(ConstructionEvaluation<AH, ?> constructionEvaluation) throws SchemaException {
        List<AttributeEvaluation<AH>> attributesToEvaluate = new ArrayList<>();

        for (ResourceAttributeDefinitionType attributeDefinitionBean : construction.getConstructionBean().getAttribute()) {
            QName attrName = ItemPathTypeUtil.asSingleNameOrFailNullSafe(attributeDefinitionBean.getRef());
            if (attrName == null) {
                throw new SchemaException(
                        "No attribute name (ref) in attribute definition in account construction in "
                                + construction.getSource());
            }
            if (!attributeDefinitionBean.getInbound().isEmpty()) {
                throw new SchemaException("Cannot process inbound section in definition of attribute "
                        + attrName + " in account construction in " + construction.getSource());
            }
            MappingType outboundMappingBean = attributeDefinitionBean.getOutbound();
            if (outboundMappingBean == null) {
                throw new SchemaException("No outbound section in definition of attribute " + attrName
                        + " in account construction in " + construction.getSource());
            }

            ResourceAttributeDefinition<?> refinedAttributeDefinition = construction.findAttributeDefinition(attrName);
            if (refinedAttributeDefinition == null) {
                throw new SchemaException("Attribute " + attrName + " not found in schema for resource object type "
                        + getKind() + "/" + getIntent() + ", " + construction.getResolvedResource().resource
                        + " as defined in " + construction.getSource(), attrName);
            }

            attributesToEvaluate.add(new AttributeEvaluation<>(constructionEvaluation, refinedAttributeDefinition,
                    outboundMappingBean, OriginType.ASSIGNMENTS, MappingKindType.CONSTRUCTION));
        }
        return attributesToEvaluate;
    }

    @Override
    protected List<AssociationEvaluation<AH>> getAssociationsToEvaluate(
            ConstructionEvaluation<AH, ?> constructionEvaluation) throws SchemaException {
        List<AssociationEvaluation<AH>> associationsToEvaluate = new ArrayList<>();
        for (ResourceObjectAssociationType associationDefinitionBean : construction.getConstructionBean().getAssociation()) {
            QName assocName = ItemPathTypeUtil.asSingleNameOrFailNullSafe(associationDefinitionBean.getRef());
            if (assocName == null) {
                throw new SchemaException(
                        "No association name (ref) in association definition in construction in " + construction.getSource());
            }
            MappingType outboundMappingBean = associationDefinitionBean.getOutbound();
            if (outboundMappingBean == null) {
                throw new SchemaException("No outbound section in definition of association " + assocName
                        + " in construction in " + construction.getSource());
            }

            ResourceAssociationDefinition resourceAssociationDefinition = construction.findAssociationDefinition(assocName);
            if (resourceAssociationDefinition == null) {
                throw new SchemaException("Association " + assocName + " not found in schema for resource object type "
                        + getKind() + "/" + getIntent() + ", " + construction.getResolvedResource().resource
                        + " as defined in " + construction.getSource(), assocName);
            }
            associationsToEvaluate.add(new AssociationEvaluation<>(constructionEvaluation, resourceAssociationDefinition,
                    outboundMappingBean, OriginType.ASSIGNMENTS, MappingKindType.CONSTRUCTION));
        }
        return associationsToEvaluate;
    }
}
