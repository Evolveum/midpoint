/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.CounterManager;
import com.evolveum.midpoint.repo.api.CounterSepcification;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyThresholdType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TimeIntervalType;

/**
 * @author katka
 *
 */
@Component
public class CacheCounterManager implements CounterManager {
	
	@Autowired private Clock clock;
	
	private static final Trace LOGGER = TraceManager.getTrace(CacheCounterManager.class);

	private Map<CounterKey, CounterSepcification> countersMap = new ConcurrentHashMap<>();
	
	public synchronized CounterSepcification registerCounter(TaskType task, String policyRuleId, PolicyRuleType policyRule) {
		
		if (task.getOid() == null) {
			LOGGER.trace("Not persistent task, skipping registering counter.");
			return null;
		}
		
		CounterKey key = new CounterKey(task.getOid(), policyRuleId);
		CounterSepcification counterSpec = countersMap.get(key);
		if (counterSpec == null) {	
			return initCleanCounter(key, task, policyRule);
		} 
		
		if (isResetCounter(counterSpec, false)) {
			return refreshCounter(key, counterSpec);
		}
		
		throw new IllegalStateException("Cannot register counter.");
		
	}
	
	private boolean isResetCounter(CounterSepcification counterSpec, boolean removeIfTimeIntervalNotSpecified) {
		
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
	public void cleanupCounters(String taskOid) {
		Set<CounterKey> keys = countersMap.keySet();
		
		Set<CounterKey> counersToRemove = new HashSet<>();
		for (CounterKey key : keys) {
			if (taskOid.equals(key.oid)) {
				counersToRemove.add(key);
			}
		}
				
		for (CounterKey counterToRemove : counersToRemove) {
			CounterSepcification spec = countersMap.get(counterToRemove);
			if (isResetCounter(spec, true)) {
				countersMap.remove(counterToRemove);
			}
		}
	}
	
	private CounterSepcification initCleanCounter(CounterKey key, TaskType task, PolicyRuleType policyRule) {
		CounterSepcification counterSpec = new CounterSepcification(task, key.policyRuleId, policyRule);
		counterSpec.setCounterStart(clock.currentTimeMillis());
		countersMap.put(key, counterSpec);
		return counterSpec;
	}
	
	private CounterSepcification refreshCounter(CounterKey key, CounterSepcification counterSpec) {
		counterSpec.reset(clock.currentTimeMillis());
		countersMap.replace(key, counterSpec);
		return counterSpec;
	}
	
	@Override
	public CounterSepcification getCounterSpec(TaskType task, String policyRuleId, PolicyRuleType policyRule) {
		if (task.getOid() == null) {
			LOGGER.trace("Cannot get counter spec for task without oid");
			return null;
		}
		
		LOGGER.trace("Getting counter spec for {} and {}", task, policyRule);
		CounterKey key = new CounterKey(task.getOid(), policyRuleId);
		CounterSepcification counterSpec = countersMap.get(key);
		
		if (counterSpec == null) {
			return registerCounter(task, policyRuleId, policyRule);
		}
		
		if (isResetCounter(counterSpec, false)) {
			counterSpec = refreshCounter(key, counterSpec);
		}
		
		
		return counterSpec;
	}
	
	@Override
	public Collection<CounterSepcification> listCounters() {
		return countersMap.values();
	}
	
	@Override
	public void removeCounter(CounterSepcification counterSpecification) {
		CounterKey key = new CounterKey(counterSpecification.getTaskOid(), counterSpecification.getPolicyRuleId());
		countersMap.remove(key);
	}
	
	class CounterKey {
		
		private String oid;
		private String policyRuleId;
		
		public CounterKey(String oid, String policyRuleId) {
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
