/**
 * Copyright (c) 2017 Evolveum
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
package com.evolveum.midpoint.testing.story;

import static org.testng.AssertJUnit.assertEquals;

import java.util.HashMap;
import java.util.Map;

import com.evolveum.midpoint.schema.internals.InternalInspector;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public class RepoReadInspector implements InternalInspector, DebugDumpable {

	@SuppressWarnings("rawtypes")
	Map<Class,Integer> readMap = new HashMap<>();
	
	@Override
	public <O extends ObjectType> void inspectRepositoryRead(Class<O> type, String oid) {
		Integer i = readMap.get(type);
		if (i == null) {
			i = 0;
		}
		i++;
		readMap.put(type, i);
	}
	
	public <O extends ObjectType> void assertRead(Class<O> type, int expectedCount) {
		assertEquals("Unexpected number of reads of "+type.getSimpleName(), (Integer)expectedCount, readMap.get(type));
	}

	public void reset() {
		readMap = new HashMap<>();
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		DebugUtil.debugDumpWithLabel(sb, "read", readMap, indent + 1);
		return sb.toString();
	}

}
