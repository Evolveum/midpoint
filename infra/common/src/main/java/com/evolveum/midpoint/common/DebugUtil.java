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

package com.evolveum.midpoint.common;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugDumpable;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.beanutils.PropertyUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.aspect.MidpointAspect;
import com.evolveum.midpoint.util.aspect.ObjectFormatter;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeAdditionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeDeletionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyAvailableValuesListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyAvailableValuesType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author semancik
 */
public class DebugUtil implements ObjectFormatter {

	private static int SHOW_LIST_MEMBERS = 3;
	
	public static String debugDump(Collection<? extends DebugDumpable> dumpables) {
		return debugDump(dumpables,0);
	}
	
	public static String debugDump(Collection<? extends DebugDumpable> dumpables, int indent) {
		StringBuilder sb = new StringBuilder();
		sb.append(getCollectionOpeningSymbol(dumpables));
		sb.append("\n");
		for (DebugDumpable dd : dumpables) {
			sb.append(dd.debugDump(indent + 1));
		}
		sb.append("\n");
		sb.append(getCollectionClosingSymbol(dumpables));
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

	public static String prettyPrint(PropertyReferenceListType reflist) {
		if (reflist == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("[");
		Iterator<PropertyReferenceType> iterator = reflist.getProperty().iterator();
		while (iterator.hasNext()) {
			sb.append(prettyPrint(iterator.next()));
			if (iterator.hasNext()) {
				sb.append(",");
			}
		}
		sb.append("]");
		return sb.toString();
	}

	public static String prettyPrint(PropertyReferenceType ref) {
		if (ref == null) {
			return "null";
		}
		XPathHolder xpath = new XPathHolder(ref.getProperty());
		return xpath.toString();

	}

	public static String prettyPrint(ObjectType object) {
		return prettyPrint(object, false);
	}

	public static String prettyPrint(ObjectType object, boolean showContent) {

		if (object instanceof AccountShadowType) {
			return prettyPrint((AccountShadowType) object, showContent);
		}

		if (object == null) {
			return "null";
		}

		StringBuilder sb = new StringBuilder();
		sb.append(object.getClass().getSimpleName());
		sb.append("(");
		sb.append(object.getOid());
		sb.append(",");
		sb.append(object.getName());

		if (showContent) {
			// This is just a fallback. Methods with more specific signature
			// should be used instead
			for (PropertyDescriptor desc : PropertyUtils.getPropertyDescriptors(object)) {
				if (!"oid".equals(desc.getName()) && !"name".equals(desc.getName())) {
					try {
						Object value = PropertyUtils.getProperty(object, desc.getName());
						sb.append(desc.getName());
						sb.append("=");
						sb.append(value);
						sb.append(",");
					} catch (IllegalAccessException ex) {
						sb.append(desc.getName());
						sb.append(":");
						sb.append(ex.getClass().getSimpleName());
						sb.append(",");
					} catch (InvocationTargetException ex) {
						sb.append(desc.getName());
						sb.append(":");
						sb.append(ex.getClass().getSimpleName());
						sb.append(",");
					} catch (NoSuchMethodException ex) {
						sb.append(desc.getName());
						sb.append(":");
						sb.append(ex.getClass().getSimpleName());
						sb.append(",");
					}
				}
			}
		}
		sb.append(")");
		return sb.toString();
	}

	public static String prettyPrint(AccountShadowType object, boolean showContent) {
		if (object == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		sb.append(object.getClass().getSimpleName());
		sb.append("(");
		sb.append(object.getOid());
		sb.append(",name=");
		sb.append(object.getName());
		sb.append(",");
		if (showContent) {
			if (object.getResource() != null) {
				sb.append("resource=(@");
				sb.append(object.getResource());
				sb.append("),");
			}
			if (object.getResourceRef() != null) {
				sb.append("resourceRef=(@");
				sb.append(object.getResourceRef());
				sb.append("),");
			}
			sb.append("objectClass=");
			sb.append(object.getObjectClass());
			sb.append(",attributes=(");
			sb.append(prettyPrint(object.getAttributes()));
			sb.append("),...");
			// TODO: more
		}
		sb.append(")");
		return sb.toString();
	}

	public static String prettyPrint(ObjectModificationType objectChange) {
		if (objectChange == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("ObjectChange(");
		sb.append(objectChange.getOid());
		sb.append(",");
		List<PropertyModificationType> changes = objectChange.getPropertyModification();
		sb.append("[");
		Iterator<PropertyModificationType> iterator = changes.iterator();
		while (iterator.hasNext()) {
			sb.append(prettyPrint(iterator.next()));
			if (iterator.hasNext()) {
				sb.append(",");
			}
		}
		sb.append("])");
		return sb.toString();
	}

	public static String prettyPrint(PropertyModificationType change) {
		if (change == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("Change(");
		sb.append(change.getModificationType());
		sb.append(",");
		if (change.getPath() != null) {
			XPathHolder xpath = new XPathHolder(change.getPath());
			sb.append(xpath.toString());
		} else {
			sb.append("xpath=null");
		}
		sb.append(",");

		sb.append(prettyPrint(change.getValue().getAny()));

		return sb.toString();
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
			sb.append(new QName(element.getNamespaceURI(), element.getLocalName()));
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

	public static String prettyPrint(ObjectListType list) {
		if (list == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("ObjectList[");
		Iterator<ObjectType> iterator = list.getObject().iterator();
		int i = 0;
		while (iterator.hasNext()) {
			if (i < SHOW_LIST_MEMBERS) {
				sb.append(prettyPrint(iterator.next()));
				if (iterator.hasNext()) {
					sb.append(",");
				}
			} else {
				sb.append("(and ");
				sb.append(list.getObject().size() - i);
				sb.append(" more)");
				break;
			}
			i++;
		}
		sb.append("]");
		return sb.toString();
	}

	public static String prettyPrint(PropertyAvailableValuesListType propertyAvailableValues) {
		if (propertyAvailableValues == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("PropertyAvailableValues[");
		List<PropertyAvailableValuesType> list = propertyAvailableValues.getAvailableValues();
		Iterator<PropertyAvailableValuesType> iterator = list.iterator();
		while (iterator.hasNext()) {
			PropertyAvailableValuesType values = iterator.next();
			sb.append(prettyPrint(values.getAny()));
		}
		sb.append("]");
		return sb.toString();
	}

	public static String prettyPrint(ResourceObjectShadowListType shadowListType) {
		if (shadowListType == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("ResourceObjectShadowListType(");

		sb.append("ResourceObjectShadow[");
		List<ResourceObjectShadowType> list = shadowListType.getObject();
		Iterator<ResourceObjectShadowType> iterator = list.iterator();
		while (iterator.hasNext()) {
			ResourceObjectShadowType values = iterator.next();
			sb.append(prettyPrint(values.getAny()));
		}
		sb.append("]");

		sb.append(")");
		return sb.toString();
	}

	public static String prettyPrint(QueryType query) {

		if (query == null) {
			return "null";
		}

		Element filter = query.getFilter();

		StringBuilder sb = new StringBuilder("Query(");

		prettyPrintFilter(sb, filter);

		sb.append(")");

		return sb.toString();
	}

	private static void prettyPrintFilter(StringBuilder sb, Element filter) {

		if (filter == null) {
			sb.append("null");
			return;
		}

		String tag = filter.getLocalName();

		sb.append(tag);
		sb.append("(");

		if ("type".equals(tag)) {
			String uri = filter.getAttribute("uri");
			QName typeQname = QNameUtil.uriToQName(uri);
			sb.append(typeQname.getLocalPart());
			sb.append(")");
			return;
		}

		NodeList childNodes = filter.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.TEXT_NODE) {
				sb.append("\"");
				sb.append(node.getTextContent());
				sb.append("\"");
			} else if (node.getNodeType() == Node.ELEMENT_NODE) {
				prettyPrintFilter(sb, (Element) node);
			} else {
				sb.append("!");
				sb.append(node.getNodeType());
			}
			sb.append(",");
		}

		sb.append(")");
	}

	public static String prettyPrint(ResourceObjectShadowChangeDescriptionType change) {
		if (change == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("ResourceObjectShadowChangeDescriptionType(");
		sb.append(prettyPrint(change.getObjectChange()));
		sb.append(",");
		sb.append(change.getSourceChannel());
		sb.append(",");
		sb.append(prettyPrint(change.getShadow()));
		sb.append(",");
		sb.append(prettyPrint(change.getResource()));
		sb.append(")");
		return sb.toString();
	}

	public static String prettyPrint(ObjectChangeType change) {
		if (change == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		if (change instanceof ObjectChangeAdditionType) {
			sb.append("ObjectChangeAdditionType(");
			ObjectChangeAdditionType add = (ObjectChangeAdditionType) change;
			sb.append(prettyPrint(add.getObject(), true));
			sb.append(")");
		} else if (change instanceof ObjectChangeModificationType) {
			sb.append("ObjectChangeModificationType(");
			ObjectChangeModificationType mod = (ObjectChangeModificationType) change;
			sb.append(prettyPrint(mod.getObjectModification()));
			sb.append(")");
		} else if (change instanceof ObjectChangeDeletionType) {
			sb.append("ObjectChangeDeletionType(");
			ObjectChangeDeletionType del = (ObjectChangeDeletionType) change;
			sb.append(del.getOid());
			sb.append(")");
		} else {
			sb.append("Unknown change type ");
			sb.append(change.getClass().getName());
		}
		return sb.toString();
	}
	
	public static String resourceFromShadow(ResourceObjectShadowType shadow) {
		if (shadow == null) {
			return null;
		}
		ResourceType resource = shadow.getResource();
		if (resource != null) {
			return resource.getName();
		}
		ObjectReferenceType resourceRef = shadow.getResourceRef();
		if (resourceRef != null) {
			return resourceRef.getOid();
		}
		return ("ERROR:noResource");
	}

	public static String toReadableString(ResourceObjectShadowType shadow) {
		QName qname = SchemaConstants.I_RESOURCE_OBJECT_SHADOW;
		if (shadow instanceof AccountShadowType) {
			qname = SchemaConstants.I_ACCOUNT;
		}
		Element element;
		try {
			element = JAXBUtil.jaxbToDom(shadow, qname, null);
		} catch (JAXBException ex) {
			return ("Error marshalling the object: " + ex.getLinkedException().getMessage());
		}
		NodeList resourceElements = element.getElementsByTagNameNS(
				SchemaConstants.I_RESOURCE.getNamespaceURI(), SchemaConstants.I_RESOURCE.getLocalPart());
		for (int i = 0; i < resourceElements.getLength(); i++) {
			Node el = (Element) resourceElements.item(i);
			el.setTextContent("[...]");
		}
		return DOMUtil.serializeDOMToString(element);
	}

	public static String toReadableString(UserType user) {
		QName qname = SchemaConstants.I_USER;
		try {
			return JAXBUtil.marshalWrap(user, qname);
		} catch (JAXBException ex) {
			return ("Error marshalling the object: " + ex.getLinkedException().getMessage());
		}
	}

	public static String prettyPrint(JAXBElement<?> element) {
		try {
			return JAXBUtil.marshal(element);
		} catch (JAXBException ex) {
			return ("Error marshalling the object: " + ex.getMessage());
		}
	}

	public static String prettyPrint(Object value) {
		if (value == null) {
			return "null";
		}
		// TODO: temporary hack. Maybe we can use
		// reflection instead of horde of if-s
		if (value instanceof ObjectType) {
			ObjectType object = (ObjectType) value;
			return prettyPrint(object);
		} else {
			return value.toString();
		}
	}

	@Override
	public String format(Object o) {
		if (o instanceof ObjectType) {
			return prettyPrint((ObjectType)o);
		} if (o instanceof ObjectChangeType) {
			return prettyPrint((ObjectChangeType)o);
		} if (o instanceof ObjectModificationType) {
			return prettyPrint((ObjectModificationType)o);
		}
		// TODO: more
		return null;
	}
	
	//static initialization of LoggingAspect - formatters registration
	static {
		ObjectFormatter f = new DebugUtil();
		MidpointAspect.registerFormatter(f);
	}

}
