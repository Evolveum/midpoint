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
package com.evolveum.midpoint.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * @author semancik
 *
 */
public class PrettyPrinter {
	
	private static String defaultNamespacePrefix = null;
	
	private static List<Class<?>> prettyPrinters = new ArrayList<Class<?>>();

	public static void setDefaultNamespacePrefix(String prefix) {
		defaultNamespacePrefix = prefix;
	}

	static String prettyPrintElementAsProperty(Object element) {
		if (element == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("<");
		QName elementName = JAXBUtil.getElementQName(element);
		sb.append(prettyPrint(elementName));
		sb.append(">");
		if (element instanceof Element) {
			Element domElement = (Element)element;
			// TODO: this is too simplistic, expand later
			sb.append(domElement.getTextContent());
		} else {
			sb.append(element.toString());
		}
		return sb.toString();
	}

	public static String prettyPrint(Collection<?> collection) {
		return prettyPrint(collection, 0);
	}

	public static String prettyPrint(Collection<?> collection, int maxItems) {
		if (collection == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder(DebugUtil.getCollectionOpeningSymbol(collection));
		Iterator<?> iterator = collection.iterator();
		int items = 0;
		while (iterator.hasNext()) {
			sb.append(prettyPrint(iterator.next()));
			items++;
			if (iterator.hasNext()) {
				sb.append(",");
				if (maxItems != 0 && items >= maxItems) {
					sb.append("...");
					break;
				}
			}
		}
		sb.append(DebugUtil.getCollectionClosingSymbol(collection));
		return sb.toString();
	}

	public static String prettyPrint(QName qname) {
		if (qname == null) {
			return "null";
		}
		if (qname.getNamespaceURI() != null) {
			if (qname.getNamespaceURI().equals(XMLConstants.W3C_XML_SCHEMA_NS_URI)) {
				return "{"+DOMUtil.NS_W3C_XML_SCHEMA_PREFIX+":}"+qname.getLocalPart();
			} else if (defaultNamespacePrefix != null && qname.getNamespaceURI().startsWith(defaultNamespacePrefix)) {
				return "{..."+qname.getNamespaceURI().substring(defaultNamespacePrefix.length())+"}"+qname.getLocalPart();
			}
		}
		return qname.toString();
	}

	/**
	 * Assumes that all elements in the lists have the same QName
	 *
	 * @param list
	 * @return
	 */
	public static String prettyPrint(List<Element> list) {
		if (list == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		if (list.size() > 0) {
			Element el0 = list.get(0);
			QName elQName;
			if (el0.getPrefix() != null) {
				elQName = new QName(el0.getNamespaceURI(), el0.getLocalName(), el0.getPrefix());
			} else {
				elQName = new QName(el0.getNamespaceURI(), el0.getLocalName());
			}
			sb.append(elQName);
			sb.append("[");
			Iterator<Element> iterator = list.iterator();
			while (iterator.hasNext()) {
				// TODO: improve for non-strings
				Element el = iterator.next();
				sb.append(prettyPrint(el, false));
				if (iterator.hasNext()) {
					sb.append(",");
				}
				sb.append("]");
			}
		} else {
			sb.append("[]");
		}
		return sb.toString();
	}

	public static String prettyPrint(Node node) {
		if (node instanceof Element) {
			return prettyPrint((Element) node);
		}
		// TODO: Better print
		return "Node:" + node.getNodeName();
	}

	public static String prettyPrint(Element element) {
		return prettyPrint(element, true);
	}

	public static String prettyPrint(Element element, boolean displayTag) {
		if (element == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		if (displayTag) {
			sb.append("<");
			if (element.getLocalName() != null) {
				sb.append(prettyPrint(new QName(element.getNamespaceURI(), element.getLocalName())));
			} else {
				sb.append("<null>");
			}
			sb.append(">");
		}
		NamedNodeMap attributes = element.getAttributes();
		for (int i = 0; i < attributes.getLength(); i++) {
			Node attr = attributes.item(i);
			if ("xmlns".equals(attr.getPrefix())) {
				// Don't display XML NS declarations
				// they are too long for prettyPrint
				continue;
			}
			if ((attr.getPrefix() == null || attr.getPrefix().isEmpty())
					&& "xmlns".equals(attr.getLocalName())) {
				// Skip default ns declaration as well
				continue;
			}
			sb.append("@");
			sb.append(attr.getLocalName());
			sb.append("=");
			sb.append(attr.getTextContent());
			if (i < (attributes.getLength() - 1)) {
				sb.append(",");
			}
		}
		if (attributes.getLength() > 0) {
			sb.append(":");
		}
		StringBuilder content = new StringBuilder();
		Node child = element.getFirstChild();
		while (child != null) {
			if (child.getNodeType() == Node.TEXT_NODE) {
				content.append(((Text) child).getTextContent());
			} else if (child.getNodeType() == Node.COMMENT_NODE) {
				// just ignore this
			} else {
				content = new StringBuilder("[complex content]");
				break;
			}
			child = child.getNextSibling();
		}
	
		sb.append(content);
	
		return sb.toString();
	}

	public static String prettyPrint(Object[] value) {
		return prettyPrint(Arrays.asList(value));
	}
	
	public static String prettyPrint(byte[] value) {
		StringBuilder sb = new StringBuilder();
		sb.append("byte[");
		for(int i=0; i<value.length; i++) {
			sb.append(Byte.toString(value[i]));
			if (i<value.length-1) {
				sb.append(',');
			}
		}
		sb.append("]");
		return sb.toString();
	}
	
	public static String prettyPrint(Object value) {
		if (value == null) {
			return "null";
		}
		String out = null;
		if (value instanceof JAXBElement) {
			Object elementValue = ((JAXBElement)value).getValue();
			out = tryPrettyPrint(elementValue);
			if (out != null) {
				return ("JAXBElement("+((JAXBElement)value).getName()+","+out+")");
			}
		}
		out = tryPrettyPrint(value);
		if (out == null) {
			out = value.toString();
		}
		return out;
	}
	
	private static String tryPrettyPrint(Object value) {
		if (value instanceof Class) {
			Class<?> c = (Class<?>)value;
			if (c.getPackage().getName().equals("com.evolveum.midpoint.xml.ns._public.common.common_2")) {
				return c.getSimpleName();
			}
			return c.getName();
		}
		if (value instanceof Collection) {
			return PrettyPrinter.prettyPrint((Collection<?>)value);
		}
		if (value.getClass().isArray()) {
			Class<?> cclass = value.getClass().getComponentType();
			if (cclass.isPrimitive()) {
				if (cclass == byte.class) {
					return PrettyPrinter.prettyPrint((byte[])value);
				} else {
					// TODO: something better
				}
			} else {
				return PrettyPrinter.prettyPrint((Object[])value);
			}
		}
		if (value instanceof Node) {
			// This is interface, won't catch it using reflection
			return PrettyPrinter.prettyPrint((Node)value);
		}
		for (Class<?> prettyPrinterClass: prettyPrinters) {
			String printerValue = tryPrettyPrint(value, prettyPrinterClass);
			if (printerValue != null) {
				return printerValue;
			}
		}
		// Fallback to this class
		return tryPrettyPrint(value, PrettyPrinter.class);
	}
	
	private static String tryPrettyPrint(Object value, Class<?> prettyPrinterClass) {
		for (Method method : prettyPrinterClass.getMethods()) {
			if (method.getName().equals("prettyPrint")) {
				Class<?>[] parameterTypes = method.getParameterTypes();
				if (parameterTypes.length == 1 && parameterTypes[0].equals(value.getClass())) {
					try {
						return (String)method.invoke(null, value);
					} catch (IllegalArgumentException e) {
						return "###INTERNAL#ERROR### Illegal argument: "+e.getMessage();
					} catch (IllegalAccessException e) {
						return "###INTERNAL#ERROR### Illegal access: "+e.getMessage();
					} catch (InvocationTargetException e) {
						return "###INTERNAL#ERROR### Illegal target: "+e.getMessage();
					} catch (Throwable e) {
						return "###INTERNAL#ERROR### "+e.getClass().getName()+": "+e.getMessage();
					}
				}
			}
		}
		return null;
	}
	
	public static void registerPrettyPrinter(Class<?> printerClass) {
		prettyPrinters.add(printerClass);
	}

}
