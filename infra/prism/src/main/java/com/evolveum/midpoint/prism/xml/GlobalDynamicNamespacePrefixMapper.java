/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.prism.xml;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.xml.DynamicNamespacePrefixMapper;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Dumpable;
import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

/**
 * Maps namespaces to preferred prefixes. Should be used through the code to
 * avoid generation of prefixes.
 * 
 * Although this is usually used as singleton (static), it can also be instantiated to locally
 * override some namespace mappings. This is useful for prefixes like "tns" (schema) or "ri" (resource schema).
 * 
 * @see MID-349
 * 
 * @author Igor Farinic
 * @author Radovan Semancik
 * 
 */
public class GlobalDynamicNamespacePrefixMapper extends NamespacePrefixMapper implements DynamicNamespacePrefixMapper, Dumpable {

	private static final Map<String, String> globalNamespacePrefixMap = new HashMap<String, String>();
	private Map<String, String> localNamespacePrefixMap = new HashMap<String, String>();
	private String defaultNamespace = null;
	private boolean alwaysExplicit = false;
	
	public String getDefaultNamespace() {
		return defaultNamespace;
	}

	public synchronized void setDefaultNamespace(String defaultNamespace) {
		this.defaultNamespace = defaultNamespace;
	}
	
	@Override
	public boolean isAlwaysExplicit() {
		return alwaysExplicit;
	}

	@Override
	public void setAlwaysExplicit(boolean alwaysExplicit) {
		this.alwaysExplicit = alwaysExplicit;
	}

	@Override
	public synchronized void registerPrefix(String namespace, String prefix, boolean isDefaultNamespace) {
		registerPrefixGlobal(namespace, prefix);
		if (isDefaultNamespace || prefix == null) {
			defaultNamespace = namespace;
		}
	}
	
	public static synchronized void registerPrefixGlobal(String namespace, String prefix) {
		globalNamespacePrefixMap.put(namespace, prefix);
	}
	
	@Override
	public synchronized void registerPrefixLocal(String namespace, String prefix) {
		localNamespacePrefixMap.put(namespace, prefix);
	}
	
	@Override
	public String getPrefix(String namespace) {
		if (defaultNamespace != null && defaultNamespace.equals(namespace) && !alwaysExplicit) {
			return "";
		}
		return getPrefixExplicit(namespace);
	}
	
	public synchronized String getPrefixExplicit(String namespace) {
		String prefix = localNamespacePrefixMap.get(namespace);
		if (prefix == null) {
			return getPreferredPrefix(namespace);
		}
		return prefix;
	}
	
	@Override
	public QName setQNamePrefix(QName qname) {
		String namespace = qname.getNamespaceURI();
		String prefix = getPrefix(namespace);
		if (prefix == null) {
			return qname;
		}
		return new QName(qname.getNamespaceURI(),qname.getLocalPart(),prefix);
	}
	
	@Override
	public QName setQNamePrefixExplicit(QName qname) {
		String namespace = qname.getNamespaceURI();
		String prefix = getPrefixExplicit(namespace);
		if (prefix == null) {
			return qname;
		}
		return new QName(qname.getNamespaceURI(),qname.getLocalPart(),prefix);
	}

	@Override
	public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
		//for JAXB we are mapping midpoint common namespace to default namespace
		if (defaultNamespace != null && defaultNamespace.equals(namespaceUri) && !alwaysExplicit) {
			return "";
		} 
		return getPreferredPrefix(namespaceUri, suggestion);
	}

	/**
	 * 
	 * @param namespace
	 * @return preferred prefix for the namespace, if no prefix is assigned yet,
	 *         then it will assign a prefix and return it.
	 */
	public static synchronized String getPreferredPrefix(String namespace) {
		return getPreferredPrefix(namespace, null);
	}

	/**
	 * @param namespace
	 * @param hintPrefix
	 * @return preferred prefix for the namespace, if no prefix is assigned yet,
	 *         then it assign hint prefix (if it is not assigned yet) or assign
	 *         a new prefix and return it (if hint prefix is already assigned to
	 *         other namespace).
	 */
	public static synchronized String getPreferredPrefix(String namespace, String hintPrefix) {
		String prefix = globalNamespacePrefixMap.get(namespace);

		if (StringUtils.isEmpty(prefix)) {
			if (StringUtils.isEmpty(hintPrefix)) {
				// FIXME: improve new prefix assignment
				prefix = "gen" + (new Random()).nextInt(999);
			} else {
				if (globalNamespacePrefixMap.containsValue(hintPrefix)) {
					// FIXME: improve new prefix assignment
					prefix = "gen" + (new Random()).nextInt(999);
				} else {
					prefix = hintPrefix;
				}
			}
			globalNamespacePrefixMap.put(namespace, prefix);
		}

		return prefix;

	}
	
	@Override
	public synchronized GlobalDynamicNamespacePrefixMapper clone() {
		GlobalDynamicNamespacePrefixMapper clone = new GlobalDynamicNamespacePrefixMapper();
		clone.defaultNamespace = this.defaultNamespace;
		clone.localNamespacePrefixMap = clonePrefixMap(this.localNamespacePrefixMap);
		clone.alwaysExplicit = this.alwaysExplicit;
		return clone;
	}

	private Map<String, String> clonePrefixMap(Map<String, String> map) {
		if (map == null) {
			return null;
		}
		Map<String, String> clone = new HashMap<String,String>();
		for (Entry<String, String> entry: map.entrySet()) {
			clone.put(entry.getKey(), entry.getValue());
		}
		return clone;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.Dumpable#dump()
	 */
	@Override
	public String dump() {
		StringBuilder sb = new StringBuilder("GlobalDynamicNamespacePrefixMapper(");
		sb.append(defaultNamespace);
		sb.append("):\n");
		DebugUtil.indentDebugDump(sb, 1);
		sb.append("Global map:\n");
		DebugUtil.debugDumpMapMultiLine(sb, globalNamespacePrefixMap, 2);
		sb.append("\n");
		DebugUtil.indentDebugDump(sb, 1);
		sb.append("Local map:\n");
		DebugUtil.debugDumpMapMultiLine(sb, localNamespacePrefixMap, 2);
		return sb.toString();
	}

}