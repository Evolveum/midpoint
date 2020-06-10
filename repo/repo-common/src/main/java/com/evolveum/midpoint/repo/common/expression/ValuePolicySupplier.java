/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;

/**
 * Supplies value policy when needed (e.g. in generate expression evaluator).
 *
 * @author semancik
 */
public interface ValuePolicySupplier {

    /**
     * Returns appropriate value policy.
     */
    ValuePolicyType get(OperationResult result);
}
