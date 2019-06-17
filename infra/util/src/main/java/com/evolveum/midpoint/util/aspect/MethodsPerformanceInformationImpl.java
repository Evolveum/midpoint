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

package com.evolveum.midpoint.util.aspect;

import com.evolveum.midpoint.util.DebugUtil;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class MethodsPerformanceInformationImpl implements MethodsPerformanceInformation {

	// method -> performance information
	private final Map<String, MethodPerformanceInformation> methodsMap = new ConcurrentHashMap<>();

	public void clear() {
		methodsMap.clear();
	}

	@Override
	public Map<String, MethodPerformanceInformation> getAllData() {
		return methodsMap;
	}

	public void register(MethodInvocationRecord invocation) {
		// operationMap.compute is also atomic, but always replaces new value (even if the reference did not change)
		// so I think this is more efficient, even if it creates empty object each time
		String key = invocation.getFullClassName() + "." + invocation.getMethodName();
		methodsMap.putIfAbsent(key, new MethodPerformanceInformation());
		methodsMap.get(key).register(invocation);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpLabelLn(sb, "Methods performance information", indent);
		ArrayList<String> operations = new ArrayList<>(methodsMap.keySet());
		operations.sort(String::compareTo);
		for (String operation : operations) {
			MethodPerformanceInformation info = methodsMap.get(operation);
			if (info != null) {
				DebugUtil.debugDumpWithLabelLn(sb, operation, info.shortDump(), indent+1);
			}
		}
		return sb.toString();
	}
}
