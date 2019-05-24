/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.repo.sql.perf;

import com.evolveum.midpoint.repo.api.perf.OperationPerformanceInformation;
import com.evolveum.midpoint.repo.api.perf.OperationRecord;
import com.evolveum.midpoint.repo.api.perf.PerformanceInformation;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RepositoryPerformanceInformationType;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class PerformanceInformationImpl implements PerformanceInformation {

	// operation kind -> performance information
	private final Map<String, OperationPerformanceInformation> operationMap = new ConcurrentHashMap<>();

	public void clear() {
		operationMap.clear();
	}

	@Override
	public Map<String, OperationPerformanceInformation> getAllData() {
		return operationMap;
	}

	@Override
	public RepositoryPerformanceInformationType toRepositoryPerformanceInformationType() {
		RepositoryPerformanceInformationType rv = new RepositoryPerformanceInformationType();
		operationMap.forEach((kind, info) -> rv.getOperation().add(info.toRepositoryOperationPerformanceInformationType(kind)));
		return rv;
	}

	public void register(OperationRecord operation, boolean alsoObjectType) {
		// operationMap.compute is also atomic, but always replaces new value (even if the reference did not change)
		// so I think this is more efficient, even if it creates empty object each time
		String key = operation.getKind();
		if (alsoObjectType) {
			key += "." + operation.getObjectTypeName();
		}
		operationMap.putIfAbsent(key, new OperationPerformanceInformation());
		operationMap.get(key).register(operation);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpLabelLn(sb, "Repository performance information", indent);
		ArrayList<String> operations = new ArrayList<>(operationMap.keySet());
		operations.sort(String::compareTo);
		for (String operation : operations) {
			OperationPerformanceInformation info = operationMap.get(operation);
			if (info != null) {
				DebugUtil.debugDumpWithLabelLn(sb, operation, info.shortDump(), indent+1);
			}
		}
		return sb.toString();
	}
}
