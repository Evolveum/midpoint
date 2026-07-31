/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.selector.eval;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.util.exception.*;

/**
 * Functional interface to inject filter evaluation code.
 * May be used to evaluate filter expression at an appropriate place where it is most efficient.
 *
 * @author semancik
 */
@FunctionalInterface
public interface ObjectFilterExpressionEvaluator {

    ObjectFilter evaluate(ObjectFilter filter)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException, SubscriptionComplianceException;

}
