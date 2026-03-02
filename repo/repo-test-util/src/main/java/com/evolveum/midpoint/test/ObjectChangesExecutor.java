/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.test;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Functional interface, intended for use by various TestObject, to allows usage of classes outside of this maven
 * module.
 *
 * It for example allows to use the `executeChanges` method defined in abstract model test to write deltas in the
 * {@link DummyTestResource}, even though it is not directly accessible from this module.
 *
 * NOTE: This interface should not be necessary anymore, if the {@link DummyTestResource} (and similar) are ever
 * moved e.g. to provisioning module.
 */
public interface ObjectChangesExecutor {
    void executeChanges(ObjectDelta<? extends ObjectType> delta) throws SchemaException, ExpressionEvaluationException,
            CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException,
            PolicyViolationException, ObjectAlreadyExistsException;
}
