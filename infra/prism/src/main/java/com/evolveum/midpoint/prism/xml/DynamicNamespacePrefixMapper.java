/*
 * Copyright (c) 2010-2014 Evolveum
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

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.DebugDumpable;

import java.util.Map;

/**
 * Maps namespaces to preferred prefixes. Should be used through the code to
 * avoid generation of prefixes.
 * 
 * @see <a href="https://jira.evolveum.com/browse/MID-349">MID-349</a>
 * 
 * @author Igor Farinic
 * @author Radovan Semancik
 */
public interface DynamicNamespacePrefixMapper extends DebugDumpable {

    void registerPrefix(String namespace, String prefix, boolean defaultNamespace);
	
	void registerPrefixLocal(String namespace, String prefix);
	
	String getPrefix(String namespace);
	
	QName setQNamePrefix(QName qname);
	
	/**
	 * Makes sure that there is explicit prefix and not a default namespace prefix.
	 */
	QName setQNamePrefixExplicit(QName qname);
	
	DynamicNamespacePrefixMapper clone();
	
	// Follwing two methods are kind of a hack to force JAXB to always use prefixes.
	// This works around the JAXB bug with default namespaces
	boolean isAlwaysExplicit();

	void setAlwaysExplicit(boolean alwaysExplicit);

    // Specifies that this prefix should be declared by default (at top of XML files)
    void addDeclaredByDefault(String prefix);

    // non-null
    Map<String,String> getNamespacesDeclaredByDefault();
}
