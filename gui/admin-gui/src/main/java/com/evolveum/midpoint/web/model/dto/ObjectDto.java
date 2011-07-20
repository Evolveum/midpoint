/*
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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.web.model.dto;

import java.io.Serializable;

import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;

/**
 * 
 * @author semancik
 */
public class ObjectDto<T extends ObjectType> implements Serializable {

	private static final long serialVersionUID = 5319506185994423879L;
	private T xmlObject;

	ObjectDto() {
		xmlObject = null;
	};

	/**
	 * Initialize DTO using XML (JAX-B) object. DTO initialized like this may
	 * only be part of other DTOs. It cannot be submited directly.
	 * 
	 * @param object
	 */
	public ObjectDto(T object) {
		xmlObject = object;
	}

	public T getXmlObject() {
		if (xmlObject != null) {
			return xmlObject;
		}
		throw new IllegalStateException();
	}

	/**
	 * This method is NOT public. It must be used ONLY by this interface
	 * implementation. It MUST NOT be used by the clients of this interface.
	 * 
	 * @param xmlObject
	 */
	public void setXmlObject(T xmlObject) {
		this.xmlObject = xmlObject;
	}

	public String getName() {
		return getXmlObject().getName();
	}

	public void setName(String value) {
		getXmlObject().setName(value);
	}

	public String getOid() {
		return getXmlObject().getOid();
	}

	public void setOid(String value) {
		getXmlObject().setOid(value);
	}

	public String getVersion() {
		return getXmlObject().getVersion();
	}

	public void setVersion(String value) {
		getXmlObject().setVersion(value);
	}
}
