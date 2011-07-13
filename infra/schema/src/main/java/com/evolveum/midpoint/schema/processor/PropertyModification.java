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
 * "Portions Copyrighted 2011 [name of copyright owner]"
 * 
 */
package com.evolveum.midpoint.schema.processor;

import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.lang.NotImplementedException;

import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.schema.XPathType;

/**
 * @author Radovan Semancik
 *
 */
public class PropertyModification {
	
	public enum ModificationType {
		
	}
	
	public XPathType getPath() {
		throw new NotImplementedException();
	}
	
	public QName getPropertyName() {
		throw new NotImplementedException();
	}
	
	public Set<Object> getValues() {
		throw new NotImplementedException();
	}
	
	public ModificationType getModificationType() {
		throw new NotImplementedException();
	}
	
	public PropertyModificationType toPropertyModificationType() {
		throw new NotImplementedException();
	}

}
