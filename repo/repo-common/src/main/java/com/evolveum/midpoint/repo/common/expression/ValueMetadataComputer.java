/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.expression;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.ValueMetadata;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;

import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Computes value metadata in given situation.
 */
@Experimental
public interface ValueMetadataComputer {

    ValueMetadata compute(@NotNull List<PrismValue> valuesTuple, @NotNull OperationResult result) throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException;
}
