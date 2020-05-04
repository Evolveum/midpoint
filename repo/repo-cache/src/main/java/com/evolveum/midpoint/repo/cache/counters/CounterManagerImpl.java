/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.cache.counters;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;
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
 * An implementation of CounterManager. Keeps track of counters used e.g. to implement
 * policy rules thresholds.
 *
 * Note that this class resides in repo-cache module almost by accident and perhaps should
 * be moved to a more appropriate place.
 *
 * @author katka
 */
@Component
public class CounterManagerImpl implements CounterManager {

    @Autowired private Clock clock;

    private static final Trace LOGGER = TraceManager.getTrace(CounterManagerImpl.class);

    private final Map<CounterKey, CounterSpecification> countersMap = new ConcurrentHashMap<>();

    @Override
    public synchronized void cleanupCounters(String taskOid) {
        Set<CounterKey> countersToRemove = new HashSet<>();
        for (Map.Entry<CounterKey, CounterSpecification> entry : countersMap.entrySet()) {
            CounterKey key = entry.getKey();
            if (taskOid.equals(key.oid) && shouldResetCounter(entry.getValue(), true)) {
                countersToRemove.add(key);
            }
        }
        countersMap.keySet().removeAll(countersToRemove);
    }

    @Override
    public synchronized void resetCounters(String taskOid) {
        for (Map.Entry<CounterKey, CounterSpecification> entry : countersMap.entrySet()) {
            if (taskOid.equals(entry.getKey().oid)) {
                entry.getValue().resetCount();
            }
        }
    }

    @NotNull
    private CounterSpecification initCleanCounter(CounterKey key, String taskId, PolicyRuleType policyRule) {
        CounterSpecification counterSpec = new CounterSpecification(taskId, key.policyRuleId, policyRule);
        counterSpec.setCounterStart(clock.currentTimeMillis());
        countersMap.put(key, counterSpec);
        return counterSpec;
    }

    /**
     * This method must be synchronized because of a race condition between getting from
     * countersMap and inserting entries to it.
     */
    @Override
    public synchronized CounterSpecification getCounterSpec(String taskId, String policyRuleId, PolicyRuleType policyRule) {
        if (taskId == null) {
            LOGGER.trace("Cannot get counter spec for task without identifier");
            return null;
        }

        LOGGER.trace("Getting counter spec for {} and {}", taskId, policyRule);
        CounterKey key = new CounterKey(taskId, policyRuleId);

        CounterSpecification counterSpec = countersMap.get(key);
        if (counterSpec == null) {
            return initCleanCounter(key, taskId, policyRule);
        } else {
            if (shouldResetCounter(counterSpec, false)) {
                counterSpec.reset(clock.currentTimeMillis());
            }
            return counterSpec;
        }
    }

    private boolean shouldResetCounter(CounterSpecification counterSpec, boolean resetIfNoTimeInterval) {
        PolicyThresholdType threshold = counterSpec.getPolicyThreshold();
        if (threshold == null) {
            return true;
        } else {
            TimeIntervalType timeInterval = threshold.getTimeInterval();
            if (timeInterval == null || timeInterval.getInterval() == null) {
                return resetIfNoTimeInterval;
            } else {
                Duration interval = timeInterval.getInterval();
                XMLGregorianCalendar counterStart = XmlTypeConverter.createXMLGregorianCalendar(counterSpec.getCounterStart());
                return XmlTypeConverter.isAfterInterval(counterStart, interval, clock.currentTimeXMLGregorianCalendar());
            }
        }
    }

    @Override
    public Collection<CounterSpecification> listCounters() {
        return Collections.unmodifiableCollection(countersMap.values());
    }

    // Must be synchronized because of the other synchronized methods
    @Override
    public synchronized void removeCounter(CounterSpecification counterSpecification) {
        CounterKey key = new CounterKey(counterSpecification.getOid(), counterSpecification.getPolicyRuleId());
        countersMap.remove(key);
    }

    private static class CounterKey {

        private final String oid;
        private final String policyRuleId;

        private CounterKey(String oid, String policyRuleId) {
            this.oid = oid;
            this.policyRuleId = policyRuleId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (o == null || getClass() != o.getClass()) {
                return false;
            }

            CounterKey cacheKey = (CounterKey) o;
            return Objects.equals(policyRuleId, cacheKey.policyRuleId)
                    && Objects.equals(oid, cacheKey.oid);
        }

        @Override
        public int hashCode() {
            int result = policyRuleId != null ? policyRuleId.hashCode() : 0;
            result = 31 * result + (oid != null ? oid.hashCode() : 0);
            return result;
        }
    }
}
