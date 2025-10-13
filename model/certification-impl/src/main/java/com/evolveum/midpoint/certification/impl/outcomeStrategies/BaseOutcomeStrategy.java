/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
