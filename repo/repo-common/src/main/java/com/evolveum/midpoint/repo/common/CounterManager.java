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
package com.evolveum.midpoint.repo.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.datatype.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.task.api.Task;
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
public class CounterManager {
	
	@Autowired private Clock clock;
	
	private static final Trace LOGGER = TraceManager.getTrace(CounterManager.class);

//	private Map<String, CounterSepcification> countersMapOld = new ConcurrentHashMap<>();
//	
	private Map<CounterKey, CounterSepcification> countersMap = new ConcurrentHashMap<>();
//	
//	public synchronized void registerCounter(Task task, boolean timeCounter) {
//		CounterSepcification counterSpec = countersMapOld.get(task.getOid());
//		if (counterSpec == null) {	
//			counterSpec = new CounterSepcification();
//			counterSpec.setCounterStart(clock.currentTimeMillis());
//			countersMapOld.put(task.getOid(), counterSpec);
//			return;
//		} 
//		
//		if (timeCounter) {
//			long currentInMillis = clock.currentTimeMillis();
//			long start = counterSpec.getCounterStart();
//			if (start + 3600 * 1000 < currentInMillis) {
//				counterSpec = new CounterSepcification();
//				counterSpec.setCounterStart(clock.currentTimeMillis());
//				countersMapOld.replace(task.getOid(), counterSpec);
//			}
//			return;
//		}
//		
//		counterSpec = new CounterSepcification();
//		counterSpec.setCounterStart(clock.currentTimeMillis());
//		countersMapOld.replace(task.getOid(), counterSpec);
//	}
	
	public synchronized void registerCounter(Task task, PolicyRuleType policyRule) {
		
		if (task.getOid() == null) {
			LOGGER.trace("Not persistent task, skipping registering counter.");
			return;
		}
		
		CounterKey key = new CounterKey(task.getOid(), policyRule);
		CounterSepcification counterSpec = countersMap.get(key);
		if (counterSpec == null) {	
			initCleanCounter(policyRule, task);
			return;
		} 
		
		if (isResetCounter(counterSpec)) {
			refreshCounter(key, counterSpec);
		}
		
	}
	
	private boolean isResetCounter(CounterSepcification counterSpec) {
		
		PolicyThresholdType threshold = counterSpec.getPolicyThreshold();
		if (threshold == null) {
			return true;
		}
		
		TimeIntervalType timeInterval = threshold.getTimeInterval();
		
		if (timeInterval == null) {
			return true;
		}
		if (timeInterval.getInterval() == null) {
			return true;
		}
		
		Duration interval = timeInterval.getInterval();
		return !XmlTypeConverter.isAfterInterval(XmlTypeConverter.createXMLGregorianCalendar(counterSpec.getCounterStart()), interval, clock.currentTimeXMLGregorianCalendar());
		
	}
	
	private CounterSepcification initCleanCounter(PolicyRuleType policyRule, Task task) {
		CounterSepcification counterSpec = new CounterSepcification();
		counterSpec.setCounterStart(clock.currentTimeMillis());
		counterSpec.setPolicyThreshold(policyRule.getPolicyThreshold());
		countersMap.put(new CounterKey(task.getOid(), policyRule), counterSpec);
		return counterSpec;
	}
	
	private void refreshCounter(CounterKey key, CounterSepcification counterSpec) {
		counterSpec.reset(clock.currentTimeMillis());
		countersMap.replace(key, counterSpec);
	}
	
	public CounterSepcification getCounterSpec(Task task, PolicyRuleType policyRule) {
		if (task.getOid() == null) {
			LOGGER.trace("Cannot get counter spec for task without oid");
			return null;
		}
		
		LOGGER.trace("Getting counter spec for {} and {}", task, policyRule);
		return countersMap.get(new CounterKey(task.getOid(), policyRule));
	}
	
	class CounterKey {
		
		private String oid;
		private PolicyRuleType policyRule;
		
		public CounterKey(String oid, PolicyRuleType policyRule) {
			this.oid = oid;
			this.policyRule = policyRule;
		}
		
		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			CounterKey cacheKey = (CounterKey) o;

			if (policyRule != null ? !policyRule.equals(cacheKey.policyRule) : cacheKey.policyRule != null)
				return false;
			return oid != null ? oid.equals(cacheKey.oid) : cacheKey.oid == null;
		}

		@Override
		public int hashCode() {
			int result = policyRule != null ? policyRule.hashCode() : 0;
			result = 31 * result + (oid != null ? oid.hashCode() : 0);
			LOGGER.trace("hashCode {} for {}{}", result, oid, policyRule);
			return result;
		}
	}
}
