/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author mederly
 */
@FunctionalInterface
public interface ConstraintViolationConfirmer {

    /**
     * Returns true if the candidate conflicts with the shadow being checked.
     * Returns false if this is not a conflicting shadow.
     */
    boolean confirmViolation(PrismObject<ShadowType> conflictingShadowCandidate)
            throws SchemaException, ConfigurationException;
}
