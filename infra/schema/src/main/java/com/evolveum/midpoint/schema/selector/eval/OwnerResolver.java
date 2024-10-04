/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.selector.eval;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.selector.spec.OwnerClause;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.Collection;
import java.util.List;

/**
 * Resolves the owner referenced to by {@link OwnerClause}.
 *
 * @author semancik
 */
public interface OwnerResolver {

    /**
     * Returns the owner of the provided object. The meaning of "owner" is different for, e.g., shadows, tasks, and so on.
     */
    <F extends FocusType, O extends ObjectType> Collection<PrismObject<F>> resolveOwner(PrismObject<O> object)
            throws CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException;

}
