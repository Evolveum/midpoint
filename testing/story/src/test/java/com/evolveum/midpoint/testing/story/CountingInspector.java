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
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public class CountingInspector implements InternalInspector, DebugDumpable {

	@SuppressWarnings("rawtypes")
	private Map<Class,Integer> readMap = new HashMap<>();
	
	private Map<NamedObjectKey,Integer> roleEvaluationMap = new HashMap<>();
	
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
	
	@Override
	public <F extends FocusType> void inspectRoleEvaluation(F target, boolean fullEvaluation) {
		NamedObjectKey key = new NamedObjectKey(target);
		Integer i = roleEvaluationMap.get(key);
		if (i == null) {
			i = 0;
		}
		i++;
		roleEvaluationMap.put(key, i);
	}


	public void reset() {
		readMap = new HashMap<>();
		roleEvaluationMap = new HashMap<>();
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		DebugUtil.debugDumpWithLabel(sb, "read", readMap, indent + 1);
		DebugUtil.debugDumpWithLabel(sb, "roleEvaluation", roleEvaluationMap, indent + 1);
		return sb.toString();
	}

	private class NamedObjectKey {
		private String oid;
		private String name;
		
		public NamedObjectKey(ObjectType object) {
			this.oid = object.getOid();
			this.name = object.getName().getOrig();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((oid == null) ? 0 : oid.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			NamedObjectKey other = (NamedObjectKey) obj;
			if (!getOuterType().equals(other.getOuterType())) {
				return false;
			}
			if (name == null) {
				if (other.name != null) {
					return false;
				}
			} else if (!name.equals(other.name)) {
				return false;
			}
			if (oid == null) {
				if (other.oid != null) {
					return false;
				}
			} else if (!oid.equals(other.oid)) {
				return false;
			}
			return true;
		}

		private CountingInspector getOuterType() {
			return CountingInspector.this;
		}

		@Override
		public String toString() {
			return "(" + oid + ":" + name + ")";
		}
		
		
	}
}
