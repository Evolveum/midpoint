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
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

/**
 * Resource object construction that was assigned to the focus.
 */
@Experimental
public class AssignedResourceObjectConstruction<AH extends AssignmentHolderType>
        extends ResourceObjectConstruction<AH, EvaluatedAssignedResourceObjectConstructionImpl<AH>> {

    private static final Trace LOGGER = TraceManager.getTrace(AssignedResourceObjectConstruction.class);

    AssignedResourceObjectConstruction(AssignedConstructionBuilder<AH> builder) {
        super(builder);
    }

    protected void resolveResource(Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
        if (getResolvedResource() != null) {
            throw new IllegalStateException("Resolving the resource twice? In: " + source);
        } else {
            ConstructionResourceResolver resourceResolver = new ConstructionResourceResolver(this, task, result);
            setResolvedResource(resourceResolver.resolveResource());
        }
    }

    protected void initializeDefinitions() throws SchemaException {
        assert getResolvedResource().resource != null;
        assert constructionBean != null;

        RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(getResolvedResource().resource,
                LayerType.MODEL, beans.prismContext);
        if (refinedSchema == null) {
            // Refined schema may be null in some error-related border cases
            throw new SchemaException("No (refined) schema for " + getResolvedResource().resource);
        }

        ShadowKindType kind = defaultIfNull(constructionBean.getKind(), ShadowKindType.ACCOUNT);
        String intent = constructionBean.getIntent();

        RefinedObjectClassDefinition refinedObjectClassDefinition = refinedSchema.getRefinedDefinition(kind, intent);
        if (refinedObjectClassDefinition == null) {
            if (intent != null) {
                throw new SchemaException(
                        "No " + kind + " type '" + intent + "' found in "
                                + getResolvedResource().resource + " as specified in construction in " + getSource());
            } else {
                throw new SchemaException("No default " + kind + " type found in " + getResolvedResource().resource
                        + " as specified in construction in " + getSource());
            }
        }
        setRefinedObjectClassDefinition(refinedObjectClassDefinition);

        for (QName auxiliaryObjectClassName : constructionBean.getAuxiliaryObjectClass()) {
            RefinedObjectClassDefinition auxOcDef = refinedSchema.getRefinedDefinition(auxiliaryObjectClassName);
            if (auxOcDef == null) {
                throw new SchemaException(
                        "No auxiliary object class " + auxiliaryObjectClassName + " found in "
                                + getResolvedResource().resource + " as specified in construction in " + source);
            }
            addAuxiliaryObjectClassDefinition(auxOcDef);
        }
    }

    @Override
    protected EvaluatedAssignedResourceObjectConstructionImpl<AH> createEvaluatedConstruction(ResourceShadowDiscriminator rsd) {
        return new EvaluatedAssignedResourceObjectConstructionImpl<>(this, rsd);
    }
}
