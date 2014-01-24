/*
 * Copyright (c) 2014 Evolveum
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
package com.evolveum.midpoint.prism.xnode;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.DebugUtil;

public class MapXNode extends XNode implements Map<QName,XNode> {
	
	// TODO: change hashmap to something that maintains order
	private Map<QName,XNode> subnodes = new HashMap<QName, XNode>();

	public int size() {
		return subnodes.size();
	}

	public boolean isEmpty() {
		return subnodes.isEmpty();
	}

	public boolean containsKey(Object key) {
		return subnodes.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return subnodes.containsValue(value);
	}

	public XNode get(Object key) {
		return subnodes.get(key);
	}

	public XNode put(QName key, XNode value) {
		return subnodes.put(key, value);
	}

	public XNode remove(Object key) {
		return subnodes.remove(key);
	}

	public void putAll(Map<? extends QName, ? extends XNode> m) {
		subnodes.putAll(m);
	}

	public void clear() {
		subnodes.clear();
	}

	public Set<QName> keySet() {
		return subnodes.keySet();
	}

	public Collection<XNode> values() {
		return subnodes.values();
	}

	public Set<java.util.Map.Entry<QName, XNode>> entrySet() {
		return subnodes.entrySet();
	}

	public boolean equals(Object o) {
		return subnodes.equals(o);
	}

	public int hashCode() {
		return subnodes.hashCode();
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpMapMultiLine(sb, this, indent, true, dumpSuffix());
		return sb.toString();
	}

	@Override
	public String getDesc() {
		return "map";
	}
	
	@Override
	public String toString() {
		return "XNode(map:"+subnodes.size()+" entries)";
	}
}
