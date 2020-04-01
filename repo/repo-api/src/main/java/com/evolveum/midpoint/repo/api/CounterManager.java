/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.api;

import java.util.Collection;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;

import org.jetbrains.annotations.Contract;

/**
 * @author katka
 *
 */
public interface CounterManager {

    @Contract("!null, _, _ -> !null; null, _, _ -> null")
    CounterSpecification getCounterSpec(String taskId, String policyRuleId, PolicyRuleType policyRule);

    void cleanupCounters(String oid);
    Collection<CounterSpecification> listCounters();
    void removeCounter(CounterSpecification counterSpecification);
    void resetCounters(String oid);
}
