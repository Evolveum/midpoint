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
package com.evolveum.midpoint.prism.util;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.parser.DomParser;
import com.evolveum.midpoint.prism.parser.PrismBeanConverter;
import com.evolveum.midpoint.prism.parser.XNodeProcessor;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author semancik
 *
 */
public class PrismUtil {

	public static <T> void recomputeRealValue(T realValue, PrismContext prismContext) {
		if (realValue == null) {
			return;
		}
		// TODO: switch to Recomputable interface instead of PolyString
		if (realValue instanceof PolyString && prismContext != null) {
			PolyString polyStringVal = (PolyString)realValue;
			// Always recompute. Recompute is cheap operation and this avoids a lot of bugs
			PolyStringNormalizer polyStringNormalizer = prismContext.getDefaultPolyStringNormalizer();
			if (polyStringNormalizer != null) {
				polyStringVal.recompute(polyStringNormalizer);
			}
		}
	}
	
	public static <T> void recomputePrismPropertyValue(PrismPropertyValue<T> pValue, PrismContext prismContext) {
		if (pValue == null) {
			return;
		}
		recomputeRealValue(pValue.getValue(), prismContext);
	}
	
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

	public static XNodeProcessor getXnodeProcessor(PrismContext prismContext) {
		if (prismContext == null) {
			return new XNodeProcessor();
		} else {
			return prismContext.getXnodeProcessor();
		}
	}

	public static PrismBeanConverter getBeanConverter(PrismContext prismContext) {
		if (prismContext == null) {
			return new PrismBeanConverter(null);
		} else {
			return prismContext.getBeanConverter();
		}
	}
	
	public static DomParser getDomParser(PrismContext prismContext) {
		if (prismContext == null) {
			return new DomParser(null);
		} else {
			return prismContext.getParserDom();
		}
	}

	public static <T,X> PrismPropertyValue<X> convertPropertyValue(PrismPropertyValue<T> srcVal, PrismPropertyDefinition<T> srcDef, PrismPropertyDefinition<X> targetDef) {
		if (targetDef.getTypeName().equals(srcDef.getTypeName())) {
			return (PrismPropertyValue<X>) srcVal;
		} else {
			Class<X> expectedJavaType = XsdTypeMapper.toJavaType(targetDef.getTypeName());
			X convertedRealValue = JavaTypeConverter.convert(expectedJavaType, srcVal.getValue());
			return new PrismPropertyValue<X>(convertedRealValue);
		}
	}

	public static <T,X> PrismProperty<X> convertProperty(PrismProperty<T> srcProp, PrismPropertyDefinition<X> targetDef) throws SchemaException {
		if (targetDef.getTypeName().equals(srcProp.getDefinition().getTypeName())) {
			return (PrismProperty<X>) srcProp;
		} else {
			PrismProperty<X> targetProp = targetDef.instantiate();
			Class<X> expectedJavaType = XsdTypeMapper.toJavaType(targetDef.getTypeName());
			for (PrismPropertyValue<T> srcPVal: srcProp.getValues()) {
				X convertedRealValue = JavaTypeConverter.convert(expectedJavaType, srcPVal.getValue());
				targetProp.add(new PrismPropertyValue<X>(convertedRealValue));
			}
			return targetProp;
		}
	}

}
