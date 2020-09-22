/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import java.util.ArrayList;
import java.util.Collection;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.context.AssignmentPath;
import com.evolveum.midpoint.model.api.context.EvaluatedResourceObjectConstruction;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.NextRecompute;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

/**
 * Evaluated construction of a resource object.
 *
 * More such objects can stem from single {@link ResourceObjectConstruction} in the presence of multiaccounts.
 */
public abstract class EvaluatedResourceObjectConstructionImpl<AH extends AssignmentHolderType> implements EvaluatedAbstractConstruction<AH>, EvaluatedResourceObjectConstruction {

    @NotNull protected final ResourceObjectConstruction<AH, ?> construction;
    @NotNull protected final ResourceShadowDiscriminator rsd;
    @NotNull protected final Collection<MappingImpl<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>>> attributeMappings = new ArrayList<>();
    @NotNull protected final Collection<MappingImpl<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>>> associationMappings = new ArrayList<>();
    protected LensProjectionContext projectionContext;

    /**
     * Precondition: {@link ResourceObjectConstruction} is already evaluated and not ignored (has resource).
     */
    EvaluatedResourceObjectConstructionImpl(@NotNull final ResourceObjectConstruction<AH, ?> construction, @NotNull final ResourceShadowDiscriminator rsd) {
        this.construction = construction;
        this.rsd = rsd;
    }

    @Override
    public @NotNull ResourceObjectConstruction<AH, ?> getConstruction() {
        return construction;
    }

    public ResourceShadowDiscriminator getResourceShadowDiscriminator() {
        return rsd;
    }

    @Override
    public @NotNull PrismObject<ResourceType> getResource() {
        return construction.getResource().asPrismObject();
    }

    @Override
    public @NotNull ShadowKindType getKind() {
        return rsd.getKind();
    }

    @Override
    public String getIntent() {
        return rsd.getIntent();
    }

    @Override
    public String getTag() {
        return rsd.getTag();
    }

    @Override
    public boolean isDirectlyAssigned() {
        return construction.getAssignmentPath() == null || construction.getAssignmentPath().size() == 1;
    }

    @Override
    public AssignmentPath getAssignmentPath() {
        return construction.getAssignmentPath();
    }

    @Override
    public boolean isWeak() {
        return construction.isWeak();
    }

    public LensProjectionContext getProjectionContext() {
        return projectionContext;
    }

    public @NotNull Collection<MappingImpl<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>>> getAttributeMappings() {
        return attributeMappings;
    }

    public MappingImpl<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> getAttributeMapping(QName attrName) {
        for (MappingImpl<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> myVc : getAttributeMappings()) {
            if (myVc.getItemName().equals(attrName)) {
                return myVc;
            }
        }
        return null;
    }

    protected void addAttributeMapping(
            MappingImpl<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> mapping) {
        getAttributeMappings().add(mapping);
    }

    public @NotNull Collection<MappingImpl<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>>> getAssociationMappings() {
        return associationMappings;
    }

    protected void addAssociationMapping(
            MappingImpl<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>> mapping) {
        getAssociationMappings().add(mapping);
    }

    public abstract NextRecompute evaluate(Task task, OperationResult result) throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException;

    protected void setProjectionContext(LensProjectionContext projectionContext) {
        this.projectionContext = projectionContext;
    }

    protected void initializeProjectionContext() {
        if (projectionContext == null) {
            projectionContext = construction.getLensContext().findProjectionContext(rsd);
            // projection context may not exist yet (existence might not be yet decided)
        }
    }


//    boolean hasValueForAttribute(QName attributeName) {
//        for (MappingImpl<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> attributeConstruction : attributeMappings) {
//            if (attributeName.equals(attributeConstruction.getItemName())) {
//                PrismValueDeltaSetTriple<? extends PrismPropertyValue<?>> outputTriple = attributeConstruction
//                        .getOutputTriple();
//                if (outputTriple != null && !outputTriple.isEmpty()) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }

    protected String getHumanReadableConstructionDescription() {
        return "construction for (" + (construction.getResolvedResource() != null ? construction.getResolvedResource().resource : null)
                + "/" + getKind() + "/" + getIntent() + "/" + getTag() + ") in " + construction.getSource();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, this.getClass().getSimpleName(), indent);
        DebugUtil.debugDumpWithLabelShortDumpLn(sb, "discriminator", rsd, indent + 1);
        // We do not want to dump construction here. This can lead to cycles.
        // We usually dump EvaluatedConstruction is a Construction dump anyway, therefore the context should be quite clear.
        DebugUtil.debugDumpWithLabelToString(sb, "projectionContext", projectionContext, indent + 1);
        if (!attributeMappings.isEmpty()) {
            sb.append("\n");
            DebugUtil.debugDumpLabel(sb, "attribute mappings", indent + 1);
            for (MappingImpl<?, ?> mapping : attributeMappings) {
                sb.append("\n");
                sb.append(mapping.debugDump(indent + 2));
            }
        }
        if (!associationMappings.isEmpty()) {
            sb.append("\n");
            DebugUtil.debugDumpLabel(sb, "association mappings", indent + 1);
            for (MappingImpl<?, ?> mapping : associationMappings) {
                sb.append("\n");
                sb.append(mapping.debugDump(indent + 2));
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "EvaluatedConstructionImpl(" +
                "discriminator=" + rsd +
                ", construction=" + construction +
                ", projectionContext='" + projectionContext +
                ')';
    }
}
