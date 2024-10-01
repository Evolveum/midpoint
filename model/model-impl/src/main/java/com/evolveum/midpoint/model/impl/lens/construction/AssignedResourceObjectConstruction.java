/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.construction;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.assignments.AssignmentPathImpl;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ConfigurationException;
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
    protected void initializeDefinitions() throws SchemaException, ConfigurationException {
        ResourceType resource = getResolvedResource().resource;

        assert resource != null; // evaluation without resource is skipped
        assert constructionBean != null;

        // Schema may be null in some error-related border cases
        ResourceSchema refinedSchema = ResourceSchemaFactory.getCompleteSchemaRequired(resource, LayerType.MODEL);

        setResourceObjectDefinition(
                refinedSchema.findDefinitionForConstructionRequired(
                        constructionBean,
                        () -> resource + " as specified in construction in " + getSource()));

        for (QName auxiliaryObjectClassName : constructionBean.getAuxiliaryObjectClass()) {
            addAuxiliaryObjectClassDefinition(
                    MiscUtil.requireNonNull(
                            refinedSchema.findDefinitionForObjectClass(auxiliaryObjectClassName),
                            () -> "No auxiliary object class %s found in %s as specified in construction in %s".formatted(
                                    auxiliaryObjectClassName, resource, source)));
        }
    }

    protected ModelBeans getBeans() {
        return ModelBeans.get();
    }

    @Override
    protected EvaluatedAssignedResourceObjectConstructionImpl<AH> createEvaluatedConstruction(
            @NotNull ConstructionTargetKey key) {
        return new EvaluatedAssignedResourceObjectConstructionImpl<>(this, key);
    }
}
