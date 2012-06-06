/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.prism.xml;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.Dumpable;

/**
 * Maps namespaces to preferred prefixes. Should be used through the code to
 * avoid generation of prefixes.
 * 
 * @see MID-349
 * 
 * @author Igor Farinic
 * @author Radovan Semancik
 * 
 */
public interface DynamicNamespacePrefixMapper extends Dumpable {
	
	public void registerPrefix(String namespace, String prefix, boolean defaultNamespace);
	
	public void registerPrefixLocal(String namespace, String prefix);
	
	public String getPrefix(String namespace);
	
	public QName setQNamePrefix(QName qname);
	
	/**
	 * Makes sure that there is explicit prefix and not a default namespace prefix.
	 */
	public QName setQNamePrefixExplicit(QName qname);
	
	public DynamicNamespacePrefixMapper clone();
	
	// Follwing two methods are kind of a hack to force JAXB to always use prefixes.
	// This works around the JAXB bug with default namespaces
	public boolean isAlwaysExplicit();

	public void setAlwaysExplicit(boolean alwaysExplicit);

}
