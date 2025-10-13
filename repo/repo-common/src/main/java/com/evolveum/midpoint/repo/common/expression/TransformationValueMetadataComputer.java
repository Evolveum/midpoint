/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.expression;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

/**
 * Computes value metadata during expression evaluation or during consolidation.
 *
 * Currently supports only very simplistic evaluation model, where resulting metadata depend only
 * on metadata attached to input values. (Not considering e.g. structuring of these input values
 * to input items in absolute evaluation mode, nor metadata present in other input variables, etc.)
 */
@Experimental
public interface TransformationValueMetadataComputer {

    @NotNull ValueMetadataType compute(@NotNull List<PrismValue> inputValues, @NotNull OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException;

    boolean supportsProvenance() throws SchemaException, ConfigurationException;

}
