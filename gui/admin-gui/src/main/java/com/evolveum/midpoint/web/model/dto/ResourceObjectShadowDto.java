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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;

/**
 * 
 * @author semancik
 */
public class ResourceObjectShadowDto<T extends ResourceObjectShadowType> extends ExtensibleObjectDto<T> {

	private static final long serialVersionUID = 8812191338352845507L;

	public ResourceObjectShadowDto() {
	}

	public ResourceObjectShadowDto(ResourceObjectShadowType object) {
		super(object);
	}

	public ObjectReferenceDto getResourceRef() {
		ObjectReferenceType ref = getXmlObject().getResourceRef();
		if (ref == null) {
			return null;
		}
		return new ObjectReferenceDto(ref);
	}

	public void setResourceRef(ObjectReferenceDto value) {
		getXmlObject().setResourceRef(value.getObjectReferenceType());
	}

	public QName getObjectClass() {
		return getXmlObject().getObjectClass();
	}

	public void setObjectClass(QName value) {
		getXmlObject().setObjectClass(value);
	}

	public List<Element> getAttributes() {
		List<Element> elements = new ArrayList<Element>();
		if (getXmlObject().getAttributes() == null) {
			return Collections.emptyList();
		}

		List<Element> any = getXmlObject().getAttributes().getAny();
		for (Object o : any) {
			elements.add((Element) o);
		}
		return elements;
	}

	public void setAttributes(List<Element> attributes) {
		ResourceObjectShadowType.Attributes attrElement = getXmlObject().getAttributes();
		if (attrElement == null) {
			ObjectFactory of = new ObjectFactory();
			attrElement = of.createResourceObjectShadowTypeAttributes();
			getXmlObject().setAttributes(attrElement);
		}

		// set means, clear old attributes and set new ones
		attrElement.getAny().clear();
		attrElement.getAny().addAll(attributes);
	}

	public ResourceDto getResource() {
		return new ResourceDto(getXmlObject().getResource());
	}
	// There is no setResource and there SHOULD not be setResource
	// Use userManager.addAccount method instead
}
