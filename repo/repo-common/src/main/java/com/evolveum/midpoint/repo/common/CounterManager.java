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

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * @author katka
 *
 */
@Component
public class CounterManager {
	
	@Autowired private Clock clock;

	private volatile Map<String, CounterSepcification> countersMap = new HashMap<>();
	
	public synchronized void registerCounter(Task task, boolean timeCounter) {
		CounterSepcification counterSpec = countersMap.get(task.getOid());
		if (counterSpec == null) {	
			counterSpec = new CounterSepcification();
			counterSpec.setCounterStart(clock.currentTimeMillis());
			countersMap.put(task.getOid(), counterSpec);
			return;
		} 
		
		if (timeCounter) {
			long currentInMillis = clock.currentTimeMillis();
			long start = counterSpec.getCounterStart();
			if (start + 3600 * 1000 < currentInMillis) {
				counterSpec = new CounterSepcification();
				counterSpec.setCounterStart(clock.currentTimeMillis());
				countersMap.replace(task.getOid(), counterSpec);
			}
			return;
		}
		
		counterSpec = new CounterSepcification();
		counterSpec.setCounterStart(clock.currentTimeMillis());
		countersMap.replace(task.getOid(), counterSpec);
	}
	
	
	public CounterSepcification getCounterSpec(Task task) {
		return countersMap.get(task.getOid());
	}
}
