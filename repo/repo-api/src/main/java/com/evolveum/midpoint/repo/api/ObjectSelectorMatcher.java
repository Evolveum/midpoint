/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.api;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.selector.eval.ClauseMatchingContext;
import com.evolveum.midpoint.schema.selector.eval.ClauseProcessingContextDescription;
import com.evolveum.midpoint.schema.selector.eval.MatchingTracer;
import com.evolveum.midpoint.schema.selector.eval.ObjectFilterExpressionEvaluator;
import com.evolveum.midpoint.schema.selector.spec.ValueSelector;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSelectorType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This code is independent on particular repository implementation; hence, it is part of the API package.
 *
 * (Currently, it is only a thin layer between repository service and the matching functionality in {@link ValueSelector}.)
 */
class ObjectSelectorMatcher {

    static boolean selectorMatches(
            @Nullable ObjectSelectorType selectorBean,
            @Nullable PrismValue value,
            @Nullable ObjectFilterExpressionEvaluator filterEvaluator,
            @NotNull Trace logger,
            @NotNull String logMessagePrefix,
            @NotNull RepositoryService repositoryService)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        if (selectorBean == null) {
            logger.trace("{} null object specification", logMessagePrefix);
            return false;
        }

        if (value == null) {
            logger.trace("{} null object", logMessagePrefix);
            return false;
        }

        var tracer = new MatchingTracer.LoggerBased(logger, logMessagePrefix);
        var selector = ValueSelector.parse(selectorBean);
        return selector.matches(
                value,
                new ClauseMatchingContext(
                        filterEvaluator,
                        tracer,
                        repositoryService,
                        null,
                        null,
                        null,
                        new ClauseProcessingContextDescription.Default(),
                        null));
    }
}
