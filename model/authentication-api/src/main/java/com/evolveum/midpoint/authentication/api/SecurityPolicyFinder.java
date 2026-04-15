/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.api;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

public interface SecurityPolicyFinder {

    /**
     * Locate security policy for given focus.
     * The method is used by authentication modules to determine which security policy should be used for given focus.
     */
    <F extends FocusType> SecurityPolicyType locateSecurityPolicyForFocus(
            PrismObject<F> focus,
            PrismObject<SystemConfigurationType> systemConfiguration,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException;
}
