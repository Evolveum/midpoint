/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

public interface EvaluatedPolicyRule {

    String getName();

    String getRuleIdentifier();

    /**
     * Return true if this rule represents a policy with constraint that uses a counter.
     * For example {@link com.evolveum.midpoint.xml.ns._public.common.common_3.ItemStatePolicyConstraintType}.
     * On the other hand, if rule contains only single
     * @return
     */
    boolean isUsePolicyCounter();

    int getCount();

    void setCount(int count);

    boolean isTriggered();

    boolean hasThreshold();
}
