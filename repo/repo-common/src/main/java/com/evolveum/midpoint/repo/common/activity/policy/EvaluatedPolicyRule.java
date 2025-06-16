/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

public interface EvaluatedPolicyRule {

    String getRuleIdentifier();

    int getCount();

    void setCount(int count);

    boolean hasThreshold();
}
