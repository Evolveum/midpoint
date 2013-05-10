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
package com.evolveum.midpoint.prism.util;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author semancik
 *
 */
public class PrismUtil {

	/**
	 * Super-mega-giga-ultra hack. This is used to "fortify" XML namespace declaration in a non-standard way.
	 * It is useful in case that someone will try some stupid kind of schema-less XML normalization that removes
	 * "unused" XML namespace declaration. The declarations are usually used, but they are used inside QName values
	 * that the dumb normalization cannot see. Therefore this fortification places XML namespace declaration in
	 * a explicit XML elements. That can be reconstructed later by using unfortification method below.
	 */
	public static void fortifyNamespaceDeclarations(Element definitionElement) {
		for(Element childElement: DOMUtil.listChildElements(definitionElement)) {
			fortifyNamespaceDeclarations(definitionElement, childElement);
		}
	}

	private static void fortifyNamespaceDeclarations(Element definitionElement, Element childElement) {
		Document doc = definitionElement.getOwnerDocument();
		NamedNodeMap attributes = childElement.getAttributes();
		for(int i=0; i<attributes.getLength(); i++) {
			Attr attr = (Attr)attributes.item(i);
			if (DOMUtil.isNamespaceDefinition(attr)) {
				String prefix = DOMUtil.getNamespaceDeclarationPrefix(attr);
				String namespace = DOMUtil.getNamespaceDeclarationNamespace(attr);
				Element namespaceElement = doc.createElementNS(PrismConstants.A_NAMESPACE.getNamespaceURI(), PrismConstants.A_NAMESPACE.getLocalPart());
				namespaceElement.setAttribute(PrismConstants.A_NAMESPACE_PREFIX, prefix);
				namespaceElement.setAttribute(PrismConstants.A_NAMESPACE_URL, namespace);
				definitionElement.insertBefore(namespaceElement, childElement);
			}
		}
	}
	
	public static void unfortifyNamespaceDeclarations(Element definitionElement) {
		Map<String,String> namespaces = new HashMap<String,String>();
		for(Element childElement: DOMUtil.listChildElements(definitionElement)) {
			if (PrismConstants.A_NAMESPACE.equals(DOMUtil.getQName(childElement))) {
				String prefix = childElement.getAttribute(PrismConstants.A_NAMESPACE_PREFIX);
				String namespace = childElement.getAttribute(PrismConstants.A_NAMESPACE_URL);
				namespaces.put(prefix, namespace);
				definitionElement.removeChild(childElement);
			} else {
				unfortifyNamespaceDeclarations(definitionElement, childElement, namespaces);
				namespaces = new HashMap<String,String>();
			}	
		}
	}

	private static void unfortifyNamespaceDeclarations(Element definitionElement, Element childElement,
			Map<String, String> namespaces) {
		for (Entry<String, String> entry: namespaces.entrySet()) {
			String prefix = entry.getKey();
			String namespace = entry.getValue();
			String declaredNamespace = DOMUtil.getNamespaceDeclarationForPrefix(childElement, prefix);
			if (declaredNamespace == null) {
				DOMUtil.setNamespaceDeclaration(childElement, prefix, namespace);
			} else if (!namespace.equals(declaredNamespace)) {
				throw new IllegalStateException("Namespace declaration with prefix '"+prefix+"' that was used to declare "+
						"namespace '"+namespace+"' is now used for namespace '"+declaredNamespace+"', cannot unfortify.");
			}
		}
	}

	public static boolean isEmpty(PolyStringType value) {
		if (value == null) {
			return true;
		}
		if (StringUtils.isNotEmpty(value.getOrig())) {
			return false;
		}
		if (StringUtils.isNotEmpty(value.getNorm())) {
			return false;
		}
		return true;
	}

//    public static void checkRACD(Item item) {
//        if (item instanceof PrismProperty) {
//            // nothing to do
//        } else if (item instanceof PrismReference) {
//            for (PrismReferenceValue prismReferenceValue : ((PrismReference) item).getValues()) {
//                if (prismReferenceValue.getObject() != null) {
//                    checkRACD(prismReferenceValue.getObject());
//                }
//            }
//
//        } else if (item instanceof PrismContainer) {
//            item.getDefinition();       // fails if item is ResourceAttributeContainer and there's a problem
//
//            for (PrismContainerValue<Containerable> prismContainerValue : ((PrismContainer<Containerable>) item).getValues()) {
//                for (Item item1 : prismContainerValue.getItems()) {
//                    checkRACD(item1);
//                }
//            }
//        } else {
//            throw new IllegalStateException("Unknown type of item: " + item);
//        }
//    }


}
