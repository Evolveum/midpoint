/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.cache;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.datatype.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.CounterManager;
import com.evolveum.midpoint.repo.api.CounterSpecification;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyThresholdType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TimeIntervalType;

/**
 * @author katka
 *
 */
@Component
public class CacheCounterManager implements CounterManager {

    @Autowired private Clock clock;

    private static final Trace LOGGER = TraceManager.getTrace(CacheCounterManager.class);

    private Map<CounterKey, CounterSpecification> countersMap = new ConcurrentHashMap<>();

    public synchronized CounterSpecification registerCounter(String taskId, String policyRuleId, PolicyRuleType policyRule) {

        if (taskId == null) {
            LOGGER.trace("Not persistent task, skipping registering counter.");
            return null;
        }

        CounterKey key = new CounterKey(taskId, policyRuleId);
        CounterSpecification counterSpec = countersMap.get(key);
        if (counterSpec == null) {
            return initCleanCounter(key, taskId, policyRule);
        }

        if (isResetCounter(counterSpec, false)) {
            return refreshCounter(key, counterSpec);
        }

        throw new IllegalStateException("Cannot register counter.");

    }

    private boolean isResetCounter(CounterSpecification counterSpec, boolean removeIfTimeIntervalNotSpecified) {

        PolicyThresholdType threshold = counterSpec.getPolicyThreshold();
        if (threshold == null) {
            return true;
        }

        TimeIntervalType timeInterval = threshold.getTimeInterval();

        if (timeInterval == null) {
            return removeIfTimeIntervalNotSpecified;
        }
        if (timeInterval.getInterval() == null) {
            return removeIfTimeIntervalNotSpecified;
        }

        Duration interval = timeInterval.getInterval();
        return XmlTypeConverter.isAfterInterval(XmlTypeConverter.createXMLGregorianCalendar(counterSpec.getCounterStart()), interval, clock.currentTimeXMLGregorianCalendar());

    }

    @Override
    public synchronized void cleanupCounters(String taskOid) {
        Set<CounterKey> keys = countersMap.keySet();

        Set<CounterKey> counersToRemove = new HashSet<>();
        for (CounterKey key : keys) {
            if (taskOid.equals(key.oid)) {
                counersToRemove.add(key);
            }
        }

        for (CounterKey counterToRemove : counersToRemove) {
            CounterSpecification spec = countersMap.get(counterToRemove);
            if (isResetCounter(spec, true)) {
                countersMap.remove(counterToRemove);
            }
        }
    }

    @Override
    public synchronized void resetCounters(String taskOid) {
        Set<CounterKey> keys = countersMap.keySet();

        Set<CounterKey> counersToReset = new HashSet<>();
        for (CounterKey key : keys) {
            if (taskOid.equals(key.oid)) {
                counersToReset.add(key);
            }
        }

        for (CounterKey counerToReset : counersToReset) {
            CounterSpecification spec = countersMap.get(counerToReset);
            spec.setCount(0);
        }
    }

    private CounterSpecification initCleanCounter(CounterKey key, String taskId, PolicyRuleType policyRule) {
        CounterSpecification counterSpec = new CounterSpecification(taskId, key.policyRuleId, policyRule);
        counterSpec.setCounterStart(clock.currentTimeMillis());
        countersMap.put(key, counterSpec);
        return counterSpec;
    }

    private CounterSpecification refreshCounter(CounterKey key, CounterSpecification counterSpec) {
        counterSpec.reset(clock.currentTimeMillis());
        countersMap.replace(key, counterSpec);
        return counterSpec;
    }

    @Override
    public CounterSpecification getCounterSpec(String taskId, String policyRuleId, PolicyRuleType policyRule) {
        if (taskId == null) {
            LOGGER.trace("Cannot get counter spec for task without identifier");
            return null;
        }

        LOGGER.trace("Getting counter spec for {} and {}", taskId, policyRule);
        CounterKey key = new CounterKey(taskId, policyRuleId);
        CounterSpecification counterSpec = countersMap.get(key);

        if (counterSpec == null) {
            return registerCounter(taskId, policyRuleId, policyRule);
        }

        if (isResetCounter(counterSpec, false)) {
            counterSpec = refreshCounter(key, counterSpec);
        }


        return counterSpec;
    }

    @Override
    public Collection<CounterSpecification> listCounters() {
        return countersMap.values();
    }

    @Override
    public void removeCounter(CounterSpecification counterSpecification) {
        CounterKey key = new CounterKey(counterSpecification.getOid(), counterSpecification.getPolicyRuleId());
        countersMap.remove(key);
    }

    static class CounterKey {

        private String oid;
        private String policyRuleId;

        CounterKey(String oid, String policyRuleId) {
            this.oid = oid;
            this.policyRuleId = policyRuleId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            CounterKey cacheKey = (CounterKey) o;

            if (policyRuleId != null ? !policyRuleId.equals(cacheKey.policyRuleId) : cacheKey.policyRuleId != null)
                return false;
            return oid != null ? oid.equals(cacheKey.oid) : cacheKey.oid == null;
        }

        @Override
        public int hashCode() {
            int result = policyRuleId != null ? policyRuleId.hashCode() : 0;
            result = 31 * result + (oid != null ? oid.hashCode() : 0);
            LOGGER.trace("hashCode {} for {}{}", result, oid, policyRuleId);
            return result;
        }
    }
}
