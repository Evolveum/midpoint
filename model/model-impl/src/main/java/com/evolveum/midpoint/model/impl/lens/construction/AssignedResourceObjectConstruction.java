/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.construction;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.model.impl.lens.assignments.AssignmentPathImpl;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Resource object construction that was assigned to the focus.
 */
@Experimental
public class AssignedResourceObjectConstruction<AH extends AssignmentHolderType>
        extends ResourceObjectConstruction<AH, EvaluatedAssignedResourceObjectConstructionImpl<AH>> {

    AssignedResourceObjectConstruction(AssignedConstructionBuilder<AH> builder) {
        super(builder);
    }

    /**
     * For assigned construction the bean is obligatory.
     */
    @Override
    public @NotNull ConstructionType getConstructionBean() {
        return Objects.requireNonNull(constructionBean);
    }

    /**
     * For assigned construction the assignment path is obligatory.
     */
    @Override
    public @NotNull AssignmentPathImpl getAssignmentPath() {
        return Objects.requireNonNull(assignmentPath);
    }

    @Override
    protected void resolveResource(Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
        if (getResolvedResource() != null) {
            throw new IllegalStateException("Resolving the resource twice? In: " + source);
        } else {
            ConstructionResourceResolver resourceResolver = new ConstructionResourceResolver(this, task, result);
            setResolvedResource(resourceResolver.resolveResource());
        }
    }

    @Override
    protected void initializeDefinitions() throws SchemaException {
        ResourceType resource = getResolvedResource().resource;

        assert resource != null; // evaluation without resource is skipped
        assert constructionBean != null;

        RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource,
                LayerType.MODEL, beans.prismContext);
        if (refinedSchema == null) {
            // Refined schema may be null in some error-related border cases
            throw new SchemaException("No (refined) schema for " + resource);
        }

        ShadowKindType kind = defaultIfNull(constructionBean.getKind(), ShadowKindType.ACCOUNT);
        String intent = constructionBean.getIntent();

        RefinedObjectClassDefinition refinedObjectClassDefinition = refinedSchema.getRefinedDefinition(kind, intent);
        if (refinedObjectClassDefinition == null) {
            if (intent != null) {
                throw new SchemaException(
                        "No " + kind + " type '" + intent + "' found in "
                                + resource + " as specified in construction in " + getSource());
            } else {
                throw new SchemaException("No default " + kind + " type found in " + resource
                        + " as specified in construction in " + getSource());
            }
        }
        setRefinedObjectClassDefinition(refinedObjectClassDefinition);

        for (QName auxiliaryObjectClassName : constructionBean.getAuxiliaryObjectClass()) {
            RefinedObjectClassDefinition auxOcDef = refinedSchema.getRefinedDefinition(auxiliaryObjectClassName);
            if (auxOcDef == null) {
                throw new SchemaException(
                        "No auxiliary object class " + auxiliaryObjectClassName + " found in "
                                + resource + " as specified in construction in " + source);
            }
            addAuxiliaryObjectClassDefinition(auxOcDef);
        }
    }

    @Override
    protected EvaluatedAssignedResourceObjectConstructionImpl<AH> createEvaluatedConstruction(ResourceShadowDiscriminator rsd) {
        return new EvaluatedAssignedResourceObjectConstructionImpl<>(this, rsd);
    }
}
