/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.model.impl.integrity;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.HashMap;
import java.util.Map;

/**
 * @author mederly
 */
public class ObjectStatistics {

	private int errors = 0;
	private final Map<String,ObjectTypeStatistics> statisticsMap = new HashMap<>();		// key is object class full name

	public Map<String, ObjectTypeStatistics> getStatisticsMap() {
		return statisticsMap;
	}

	public int getErrors() {
		return errors;
	}

	public void record(PrismObject<ObjectType> object) {
		String key = object.asObjectable().getClass().getName();
		ObjectTypeStatistics typeStatistics = statisticsMap.computeIfAbsent(key, (k) -> new ObjectTypeStatistics());
		typeStatistics.register(object);
	}

	public void incrementObjectsWithErrors() {
		errors++;
	}
}
