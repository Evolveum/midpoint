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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;

public class MapXNode extends XNode implements Map<QName,XNode> {
	
	// We want to maintain ordering, hence the List
	private List<Entry> subnodes = new ArrayList<Entry>();

	public int size() {
		return subnodes.size();
	}

	public boolean isEmpty() {
		return subnodes.isEmpty();
	}

	public boolean containsKey(Object key) {
		if (!(key instanceof QName)) {
			throw new IllegalArgumentException("Key must be QName, but it is "+key);
		}
		return findEntry((QName)key) != null;
	}

	public boolean containsValue(Object value) {
		if (!(value instanceof XNode)) {
			throw new IllegalArgumentException("Value must be XNode, but it is "+value);
		}
		return findEntry((XNode)value) != null;
	}

	public XNode get(Object key) {
		if (!(key instanceof QName)) {
			throw new IllegalArgumentException("Key must be QName, but it is "+key);
		}
		Entry entry = findEntry((QName)key);
		if (entry == null) {
			return null;
		}
		return entry.getValue();
	}

	public XNode put(QName key, XNode value) {
		removeEntry(key);
		subnodes.add(new Entry(key, value));
		return value;
	}

	public XNode remove(Object key) {
		if (!(key instanceof QName)) {
			throw new IllegalArgumentException("Key must be QName, but it is "+key);
		}
		return removeEntry((QName)key);
	}

	public void putAll(Map<? extends QName, ? extends XNode> m) {
		for (Map.Entry<?, ?> entry: m.entrySet()) {
			put((QName)entry.getKey(), (XNode)entry.getValue());
		}
	}

	public void clear() {
		subnodes.clear();
	}

	public Set<QName> keySet() {
		Set<QName> keySet = new HashSet<QName>();
		for (Entry entry: subnodes) {
			keySet.add(entry.getKey());
		}
		return keySet;
	}

	public Collection<XNode> values() {
		Collection<XNode> values = new ArrayList<XNode>(subnodes.size());
		for (Entry entry: subnodes) {
			values.add(entry.getValue());
		}
		return values;
	}

	public Set<java.util.Map.Entry<QName, XNode>> entrySet() {
		Set<java.util.Map.Entry<QName, XNode>> entries = new HashSet<Map.Entry<QName,XNode>>();
		for (Entry entry: subnodes) {
			entries.add(entry);
		}
		return entries;
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

	private Entry findEntry(QName qname) {
		for (Entry entry: subnodes) {
			if (QNameUtil.match(qname,entry.getKey())) {
				return entry;
			}
		}
		return null;
	}

	private Entry findEntry(XNode xnode) {
		for (Entry entry: subnodes) {
			if (entry.getValue().equals(xnode)) {
				return entry;
			}
		}
		return null;
	}

	private XNode removeEntry(QName key) {
		Iterator<Entry> iterator = subnodes.iterator();
		while (iterator.hasNext()) {
			Entry entry = iterator.next();
			if (QNameUtil.match(key,entry.getKey())) {
				iterator.remove();
				return entry.getValue();
			}
		}
		return null;
	}
	
	private class Entry implements Map.Entry<QName, XNode> {

		private QName key;
		private XNode value;
		
		public Entry(QName key) {
			super();
			this.key = key;
		}

		public Entry(QName key, XNode value) {
			super();
			this.key = key;
			this.value = value;
		}

		@Override
		public QName getKey() {
			return key;
		}

		@Override
		public XNode getValue() {
			return value;
		}

		@Override
		public XNode setValue(XNode value) {
			this.value = value;
			return value;
		}
		
	}
}
