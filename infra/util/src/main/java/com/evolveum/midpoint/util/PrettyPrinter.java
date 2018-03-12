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
package com.evolveum.midpoint.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
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

	private static final int BYTE_ARRAY_MAX_LEN = 64;

	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyy-mm-dd hh:mm:ss");

	private static String defaultNamespacePrefix = null;

	private static List<Class<?>> prettyPrinters = new ArrayList<>();

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


	public static String prettyPrint(Date date) {
		if (date == null) {
			return "null";
		}
		return dateFormat.format(date);
	}

	public static String prettyPrint(Object[] value) {
		return prettyPrint(Arrays.asList(value));
	}

	public static String prettyPrint(byte[] value) {
		StringBuilder sb = new StringBuilder();
		sb.append("byte[");
		int printlen = value.length;
		if (printlen > BYTE_ARRAY_MAX_LEN) {
			printlen = BYTE_ARRAY_MAX_LEN;
		}
		for(int i=0; i < printlen; i++) {
			sb.append(String.format("%02x", value[i] & 0xff));
		}
		if (value.length > BYTE_ARRAY_MAX_LEN) {
			sb.append("... ");
			sb.append(value.length);
			sb.append(" bytes total");
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
			if (c.getPackage().getName().equals("com.evolveum.midpoint.xml.ns._public.common.common_3")) {
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
						return "###INTERNAL#ERROR### Illegal argument: "+e.getMessage() + "; prettyPrint method for value "+value;
					} catch (IllegalAccessException e) {
						return "###INTERNAL#ERROR### Illegal access: "+e.getMessage() + "; prettyPrint method for value "+value;
					} catch (InvocationTargetException e) {
						return "###INTERNAL#ERROR### Illegal target: "+e.getMessage() + "; prettyPrint method for value "+value;
					} catch (Throwable e) {
						return "###INTERNAL#ERROR### "+e.getClass().getName()+": "+e.getMessage() + "; prettyPrint method for value "+value;
					}
				}
			}
		}
		return null;
	}
	
	public static String debugDump(Object value, int indent) {
		if (value == null) {
			return "null";
		}
		String out = null;
		if (value instanceof DebugDumpable) {
			return ((DebugDumpable)value).debugDump(indent);
		}
		if (value instanceof Collection) {
			return DebugUtil.debugDump((Collection)value, indent);
		}
		out = tryDebugDumpMethod(value, indent);
		if (out != null) {
			return out;
		}
		StringBuilder sb = DebugUtil.createIndentedStringBuilder(indent);
		sb.append(prettyPrint(value));
		return sb.toString();
	}
	
	private static String tryDebugDumpMethod(Object value, int indent) {
		for (Class<?> prettyPrinterClass: prettyPrinters) {
			String printerValue = tryDebugDumpMethod(value, indent, prettyPrinterClass);
			if (printerValue != null) {
				return printerValue;
			}
		}
		// Fallback to this class
		return tryDebugDumpMethod(value, indent, PrettyPrinter.class);
	}

	private static String tryDebugDumpMethod(Object value, int indent, Class<?> prettyPrinterClass) {
		for (Method method : prettyPrinterClass.getMethods()) {
			if (method.getName().equals("debugDump")) {
				Class<?>[] parameterTypes = method.getParameterTypes();
				if (parameterTypes.length == 2 && parameterTypes[0].equals(value.getClass())) {
					try {
						return (String)method.invoke(null, value, indent);
					} catch (IllegalArgumentException e) {
						return "###INTERNAL#ERROR### Illegal argument: "+e.getMessage() + "; debugDump method for value "+value;
					} catch (IllegalAccessException e) {
						return "###INTERNAL#ERROR### Illegal access: "+e.getMessage() + "; debugDump method for value "+value;
					} catch (InvocationTargetException e) {
						return "###INTERNAL#ERROR### Illegal target: "+e.getMessage() + "; debugDump method for value "+value;
					} catch (Throwable e) {
						return "###INTERNAL#ERROR### "+e.getClass().getName()+": "+e.getMessage() + "; debugDump method for value "+value;
					}
				}
			}
		}
		return null;
	}
	
	public static void shortDump(StringBuilder sb, Object value) {
		if (value == null) {
			sb.append("null");
			return;
		}
		if (value instanceof ShortDumpable) {
			((ShortDumpable)value).shortDump(sb);
			return;
		}
		if (value instanceof Collection) {
			DebugUtil.shortDump(sb, (Collection)value);
			return;
		}
		if (tryShortDumpMethod(sb, value)) {
			return;
		}
		sb.append(prettyPrint(value));
	}
	
	private static boolean tryShortDumpMethod(StringBuilder sb, Object value) {
		for (Class<?> prettyPrinterClass: prettyPrinters) {
			if (tryShortDumpMethod(sb, value, prettyPrinterClass)) {
				return true;
			}
		}
		// Fallback to this class
		return tryShortDumpMethod(sb, value, PrettyPrinter.class);
	}

	private static boolean tryShortDumpMethod(StringBuilder sb, Object value, Class<?> prettyPrinterClass) {
		for (Method method : prettyPrinterClass.getMethods()) {
			if (method.getName().equals("shortDump")) {
				Class<?>[] parameterTypes = method.getParameterTypes();
				if (parameterTypes.length == 2 && parameterTypes[0].equals(StringBuilder.class) && parameterTypes[1].equals(value.getClass())) {
					try {
						method.invoke(null, sb, value);
						return true;
					} catch (IllegalArgumentException e) {
						sb.append("###INTERNAL#ERROR### Illegal argument: "+e.getMessage() + "; shortDump method for value "+value);
					} catch (IllegalAccessException e) {
						sb.append("###INTERNAL#ERROR### Illegal access: "+e.getMessage() + "; shortDump method for value "+value);
					} catch (InvocationTargetException e) {
						sb.append("###INTERNAL#ERROR### Illegal target: "+e.getMessage() + "; shortDump method for value "+value);
					} catch (Throwable e) {
						sb.append("###INTERNAL#ERROR### "+e.getClass().getName()+": "+e.getMessage() + "; shortDump method for value "+value);
					}
				}
			}
		}
		return false;
	}

	public static void registerPrettyPrinter(Class<?> printerClass) {
		prettyPrinters.add(printerClass);
	}

	// For diagnostics only
	public static List<Class<?>> getPrettyPrinters() {
		return prettyPrinters;
	}

}
