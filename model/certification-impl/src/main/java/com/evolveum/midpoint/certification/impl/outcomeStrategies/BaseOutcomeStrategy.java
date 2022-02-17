/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl.outcomeStrategies;

import com.evolveum.midpoint.certification.impl.AccCertResponseComputationHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseOutcomeStrategyType;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseOutcomeStrategy implements OutcomeStrategy {

    @Autowired
    protected AccCertResponseComputationHelper computationHelper;

    protected void register(AccessCertificationCaseOutcomeStrategyType strategy) {
        computationHelper.registerOutcomeStrategy(strategy, this);
    }
}
