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

package com.evolveum.midpoint.util;

import com.evolveum.midpoint.util.aspect.MidpointAspect;
import com.evolveum.midpoint.util.aspect.ObjectFormatter;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;

/**
 *
 * @author semancik
 */
public class DebugUtil implements ObjectFormatter {

	private static int SHOW_LIST_MEMBERS = 3;
	private static String defaultNamespacePrefix = null;

	public static void setDefaultNamespacePrefix(String prefix) {
		defaultNamespacePrefix = prefix;
	}

	public static String dump(Object object) {
		if (object == null) {
			return "null";
		}
		if (object instanceof Dumpable) {
			return ((Dumpable)object).dump();
		}
		if (object instanceof Map) {
			StringBuilder sb = new StringBuilder();
			debugDumpMapMultiLine(sb, (Map)object, 0);
			return sb.toString();
		}
		if (object instanceof Collection) {
			return debugDump((Collection)object);
		}
		return object.toString();
	}

	public static String debugDump(Collection<?> dumpables) {
		return debugDump(dumpables,0);
	}

	public static String debugDump(Collection<?> dumpables, int indent) {
		StringBuilder sb = new StringBuilder();
		sb.append(getCollectionOpeningSymbol(dumpables));
		sb.append("\n");
		for (Object item : dumpables) {
			if (item == null) {
				indentDebugDump(sb, indent + 1);
				sb.append("null");
			} else if (item instanceof DebugDumpable) {
				sb.append(((DebugDumpable)item).debugDump(indent + 1));
			} else {
				indentDebugDump(sb, indent + 1);
				sb.append(item.toString());
			}
		}
		sb.append("\n");
		sb.append(getCollectionClosingSymbol(dumpables));
		return sb.toString();
	}
	
	public static String debugDump(Object object, int indent) {
		if (object instanceof DebugDumpable) {
			return ((DebugDumpable)object).debugDump(indent);
		} else if (object instanceof Collection) {
			return debugDump((Collection<?>)object, indent);
		} else {
			StringBuilder sb = new StringBuilder();
			indentDebugDump(sb, indent + 1);
			sb.append(object.toString());
			return sb.toString();
		}
	}
	
	public static void debugDumpWithLabel(StringBuilder sb, String label, DebugDumpable dd, int indent) {
		indentDebugDump(sb, indent);
		sb.append(label).append(":");
		if (dd == null) {
			sb.append(" null");
		} else {
			sb.append("\n");
			sb.append(dd.debugDump(indent + 1));
		}
	}
	
	public static <K, V extends DebugDumpable> void debugDumpWithLabel(StringBuilder sb, String label, Map<K, V> map, int indent) {
		indentDebugDump(sb, indent);
		sb.append(label).append(":");
		if (map == null) {
			sb.append(" null");
		} else {
			sb.append("\n");
			debugDumpMapMultiLine(sb, map, indent + 1);
		}
	}
	
	public static void debugDumpWithLabelToString(StringBuilder sb, String label, Object object, int indent) {
		indentDebugDump(sb, indent);
		sb.append(label).append(":");
		if (object == null) {
			sb.append(" null");
		} else {
			sb.append(" ");
			sb.append(object.toString());
		}
	}

	public static String debugDumpXsdAnyProperties(Collection<?> xsdAnyCollection, int indent) {
		StringBuilder sb = new StringBuilder();
		indentDebugDump(sb, indent);
		sb.append(getCollectionOpeningSymbol(xsdAnyCollection));
		for (Object element : xsdAnyCollection) {
			sb.append("\n");
			indentDebugDump(sb, indent+1);
			sb.append(prettyPrintElementAsProperty(element));
		}
		sb.append("\n");
		indentDebugDump(sb, indent);
		sb.append(getCollectionClosingSymbol(xsdAnyCollection));
		return sb.toString();
	}

	private static String prettyPrintElementAsProperty(Object element) {
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

	private static String getCollectionOpeningSymbol(Collection<?> col) {
		if (col instanceof List) {
			return "[";
		}
		if (col instanceof Set) {
			return "{";
		}
		return col.getClass().getSimpleName()+"(";
	}

	private static String getCollectionClosingSymbol(Collection<?> col) {
		if (col instanceof List) {
			return "]";
		}
		if (col instanceof Set) {
			return "}";
		}
		return ")";
	}

	public static void indentDebugDump(StringBuilder sb, int indent) {
		for(int i = 0; i < indent; i++) {
			sb.append(DebugDumpable.INDENT_STRING);
		}
	}

	public static <K, V> void debugDumpMapMultiLine(StringBuilder sb, Map<K, V> map, int indent) {
		Iterator<Entry<K, V>> i = map.entrySet().iterator();
		while (i.hasNext()) {
			Entry<K,V> entry = i.next();
			indentDebugDump(sb,indent);
			sb.append(prettyPrint(entry.getKey()));
			sb.append(" => ");
			V value = entry.getValue();
			if (value == null) {
				sb.append("null");
			} else if (value instanceof DebugDumpable) {
				sb.append("\n");
				sb.append(((DebugDumpable)value).debugDump(indent+1));
			} else {
				sb.append(value);
			}
			if (i.hasNext()) {
				sb.append("\n");
			}
		}
	}

	public static <K, V> void debugDumpMapSingleLine(StringBuilder sb, Map<K, V> map, int indent) {
		Iterator<Entry<K, V>> i = map.entrySet().iterator();
		while (i.hasNext()) {
			Entry<K,V> entry = i.next();
			indentDebugDump(sb,indent);
			sb.append(prettyPrint(entry.getKey()));
			sb.append(" => ");
			V value = entry.getValue();
			if (value == null) {
				sb.append("null");
			} else {
				sb.append(value);
			}
			if (i.hasNext()) {
				sb.append("\n");
			}
		}
	}

	public static String prettyPrint(Collection<?> collection) {
		return prettyPrint(collection, 0);
	}

	public static String prettyPrint(Collection<?> collection, int maxItems) {
		if (collection == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder(getCollectionOpeningSymbol(collection));
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
		sb.append(getCollectionClosingSymbol(collection));
		return sb.toString();
	}

	public static String prettyPrint(QName qname) {
		if (qname == null) {
			return "null";
		}
		if (defaultNamespacePrefix != null && qname.getNamespaceURI() != null
				&& qname.getNamespaceURI().startsWith(defaultNamespacePrefix)) {
			return "{..."+qname.getNamespaceURI().substring(defaultNamespacePrefix.length())+"}"+qname.getLocalPart();
		}
		return qname.toString();
	}



//	public static String prettyPrint(ObjectType object, boolean showContent) {
//
//		if (object instanceof AccountShadowType) {
//			return prettyPrint((AccountShadowType) object, showContent);
//		}
//
//		if (object == null) {
//			return "null";
//		}
//
//		StringBuilder sb = new StringBuilder();
//		sb.append(object.getClass().getSimpleName());
//		sb.append("(");
//		sb.append(object.getOid());
//		sb.append(",");
//		sb.append(object.getName());
//
//		if (showContent) {
//			// This is just a fallback. Methods with more specific signature
//			// should be used instead
//			for (PropertyDescriptor desc : PropertyUtils.getPropertyDescriptors(object)) {
//				if (!"oid".equals(desc.getName()) && !"name".equals(desc.getName())) {
//					try {
//						Object value = PropertyUtils.getProperty(object, desc.getName());
//						sb.append(desc.getName());
//						sb.append("=");
//						sb.append(value);
//						sb.append(",");
//					} catch (IllegalAccessException ex) {
//						sb.append(desc.getName());
//						sb.append(":");
//						sb.append(ex.getClass().getSimpleName());
//						sb.append(",");
//					} catch (InvocationTargetException ex) {
//						sb.append(desc.getName());
//						sb.append(":");
//						sb.append(ex.getClass().getSimpleName());
//						sb.append(",");
//					} catch (NoSuchMethodException ex) {
//						sb.append(desc.getName());
//						sb.append(":");
//						sb.append(ex.getClass().getSimpleName());
//						sb.append(",");
//					}
//				}
//			}
//		}
//		sb.append(")");
//		return sb.toString();
//	}
//	
//	public static String prettyPrint(ProtectedStringType protectedStringType) {
//		if (protectedStringType == null) {
//			return "null";
//		}
//		StringBuilder sb = new StringBuilder("ProtectedStringType(");
//		
//		if (protectedStringType.getEncryptedData() != null) {
//			sb.append("[encrypted data]");
//		}
//
//		if (protectedStringType.getClearValue() != null) {
//			sb.append("\"");
//			sb.append(protectedStringType.getClearValue());
//			sb.append("\"");
//		}
//
//		sb.append(")");
//		return sb.toString();
//	}


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
			if (c.getPackage().getName().equals("com.evolveum.midpoint.xml.ns._public.common.common_1")) {
				return c.getSimpleName();
			}
			return c.getName();
		}
		if (value instanceof Collection) {
			return prettyPrint((Collection<?>)value);
		}
//		if (value instanceof ObjectType) {
//			// ObjectType has many subtypes, difficult to sort out using reflection
//			// therefore we special-case it
//			return prettyPrint((ObjectType)value);
//		}
		if (value instanceof Node) {
			// This is interface, won't catch it using reflection
			return prettyPrint((Node)value);
		}
		for (Method method : DebugUtil.class.getMethods()) {
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

	@Override
	public String format(Object o) {
		try {
			return prettyPrint(o);
		} catch (Throwable t) {
			return "###INTERNAL#ERROR### "+t.getClass().getName()+": "+t.getMessage();
		}
	}

	//static initialization of LoggingAspect - formatters registration
	static {
		ObjectFormatter f = new DebugUtil();
		MidpointAspect.registerFormatter(f);
	}

}
