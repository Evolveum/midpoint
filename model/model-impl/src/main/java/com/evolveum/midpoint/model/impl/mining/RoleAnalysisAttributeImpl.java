/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class RoleAnalysisAttributeImpl extends RoleAnalysisAttributeDef {

    public RoleAnalysisAttributeImpl(ItemPath path, boolean isContainer, Class<? extends ObjectType> classType) {
        super(path, isContainer, classType);
    }

    public RoleAnalysisAttributeImpl(ItemPath path, boolean isContainer, Class<? extends ObjectType> classType, IdentifierType identifierType) {
        super(path, isContainer, classType, identifierType);
    }

    public RoleAnalysisAttributeImpl(ItemPath path, boolean isContainer, String displayValue, Class<? extends ObjectType> classType, IdentifierType identifierType) {
        super(path, isContainer, displayValue, classType, identifierType);
    }

    public int attributeWithExactValueCount(
            @NotNull ModelService modelService,
            @NotNull Class<FocusType> objectClass,
            @NotNull String value,
            @NotNull Task task,
            @NotNull OperationResult result) {
        try {
            modelService.countObjects(objectClass, getQuery(value), null, task, result);
        } catch (SchemaException | ObjectNotFoundException | SecurityViolationException | ConfigurationException |
                CommunicationException | ExpressionEvaluationException e) {
            throw new RuntimeException("Couldn't count objects with exact attribute value: " + e.getMessage(), e);
        }
        return 0;
    }
}
