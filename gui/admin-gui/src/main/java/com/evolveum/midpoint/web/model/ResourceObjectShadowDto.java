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

package com.evolveum.midpoint.web.model;

import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;
import org.w3c.dom.Element;

/**
 * 
 * @author semancik
 */
public class ResourceObjectShadowDto extends ExtensibleObjectDto {

	private static final long serialVersionUID = 8812191338352845507L;

	public ResourceObjectShadowDto() {
	}

	public ResourceObjectShadowDto(ResourceObjectShadowType object) {
		super(object);
	}

	public ResourceObjectShadowDto(ObjectStage stage) {
		super(stage);
	}

	private ResourceObjectShadowType getResourceObjectShadowType() {
		return (ResourceObjectShadowType) getXmlObject();
	}

	public ObjectReferenceDto getResourceRef() {
		ObjectReferenceType ref = getResourceObjectShadowType().getResourceRef();
		if (ref == null) {
			return null;
		}
		return new ObjectReferenceDto(ref);
	}

	public void setResourceRef(ObjectReferenceDto value) {
		getResourceObjectShadowType().setResourceRef(value.getObjectReferenceType());
	}

	public QName getObjectClass() {
		return getResourceObjectShadowType().getObjectClass();
	}

	public void setObjectClass(QName value) {
		getResourceObjectShadowType().setObjectClass(value);
	}

	public List<Element> getAttributes() {
		List<Element> elements = new ArrayList<Element>();
		if (getResourceObjectShadowType().getAttributes() == null) {
			return Collections.emptyList();
		}

		List<Element> any = getResourceObjectShadowType().getAttributes().getAny();
		for (Object o : any) {
			if (o instanceof Element) {
				elements.add((Element) o);
			}
		}
		return elements;
	}

	public void setAttributes(List<Element> attributes) {
		ResourceObjectShadowType.Attributes attrElement = getResourceObjectShadowType().getAttributes();
		if (attrElement == null) {
			ObjectFactory of = new ObjectFactory();
			attrElement = of.createResourceObjectShadowTypeAttributes();
			getResourceObjectShadowType().setAttributes(attrElement);
		}

		// set means, clear old attributes and set new ones
		attrElement.getAny().clear();
		attrElement.getAny().addAll(attributes);
	}

	public ResourceDto getResource() {
		return new ResourceDto(getResourceObjectShadowType().getResource());
	}
	// There is no setResource and there SHOULD not be setResource
	// Use userManager.addAccount method instead
}
