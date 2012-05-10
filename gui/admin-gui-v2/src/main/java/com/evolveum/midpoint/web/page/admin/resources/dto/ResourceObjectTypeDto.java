/*
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

package com.evolveum.midpoint.web.page.admin.resources.dto;

import java.io.Serializable;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;

/**
 * @author lazyman
 */
public class ResourceObjectTypeDto implements Serializable {
	
	private static final long serialVersionUID = 4664988785770149299L;
	private String displayName;
	private String nativeObjectClass;
	private String help;
	private QName type;

	public ResourceObjectTypeDto(ResourceAttributeContainerDefinition definition) {
		Validate.notNull(definition, "Resource object definition can't be null.");

		displayName = definition.getDisplayName();
		nativeObjectClass = definition.getNativeObjectClass();
		help = definition.getHelp();
		if (definition.getTypeName() != null) {
			this.type = definition.getTypeName();
		}
	}

	public String getQualifiedType() {
		if (type == null) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		builder.append("{");
		builder.append(type.getNamespaceURI());
		builder.append("}");
		builder.append(type.getLocalPart());
		return builder.toString();
	}

	public String getDisplayName() {
		if (displayName == null) {
			return "";
		}
		return displayName;
	}

	public String getNativeObjectClass() {
		if (nativeObjectClass == null) {
			return "";
		}
		return nativeObjectClass;
	}

	public String getHelp() {
		if (help == null) {
			return "";
		}
		return help;
	}

	public QName getType() {
		return type;
	}

	public String getSimpleType() {
		if (type == null) {
			return "";
		}
		return type.getLocalPart();
	}
	
}
