/**
 * Copyright (c) 2012 Evolveum
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
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */
package com.evolveum.midpoint.common.expression.functions;

/**
 * @author semancik
 *
 */
public class FunctionLibrary {
	
	private String variableName;
	private String namespace;
	private Object genericFunctions;
	private Object xmlFunctions;
	
	public String getVariableName() {
		return variableName;
	}
	
	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}
	
	public String getNamespace() {
		return namespace;
	}
	
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}
	
	public Object getGenericFunctions() {
		return genericFunctions;
	}
	
	public void setGenericFunctions(Object genericFunctions) {
		this.genericFunctions = genericFunctions;
	}
	
	public Object getXmlFunctions() {
		return xmlFunctions;
	}
	
	public void setXmlFunctions(Object xmlFunctions) {
		this.xmlFunctions = xmlFunctions;
	}

	
}
