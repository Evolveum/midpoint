/*
 * Copyright (c) 2014-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.security.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * This would be more appropriate in the security-impl. But we need it as low as this.
 * Otherwise there is a dependency cycle (task->security->repo-common->task)
 * Moving this to task yields better cohesion. So, it may in fact belong here.
 *
 * @author semancik
 */
public interface OwnerResolver {

    /**
     * Returns the owner of the provided object. The meaning of "owner" is different for, e.g., shadows, tasks, and so on.
     */
    <F extends FocusType, O extends ObjectType> PrismObject<F> resolveOwner(PrismObject<O> object)
            throws CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException;

}
