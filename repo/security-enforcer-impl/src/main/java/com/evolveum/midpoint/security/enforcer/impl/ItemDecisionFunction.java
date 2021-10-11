/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.security.enforcer.impl;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.AccessDecision;

/**
 * @author semancik
 *
 */
@FunctionalInterface
public interface ItemDecisionFunction {

    AccessDecision decide(ItemPath nameOnlyItemPath, boolean removingContainer);

}
