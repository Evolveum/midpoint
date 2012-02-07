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

import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugDumpable;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
	
	public static <K, V extends DebugDumpable> void debugDumpMapMultiLine(StringBuilder sb, Map<K, V> map, int indent) {
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
				sb.append("\n");
				sb.append(value.debugDump(indent+1));
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
	
	public static String debugDump(ObjectType objectType, int indent) {
		if (objectType instanceof AccountShadowType) {
			return debugDump((AccountShadowType)objectType,indent);
		}
		StringBuilder sb = new StringBuilder();
		indentDebugDump(sb, indent);
		// TODO: better dumping
		sb.append(prettyPrint(objectType));
		return sb.toString();
	}

	public static String prettyPrint(Collection<?> collection) {
		if (collection == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder(getCollectionOpeningSymbol(collection));
		Iterator<?> iterator = collection.iterator();
		while (iterator.hasNext()) {
			sb.append(prettyPrint(iterator.next()));
			if (iterator.hasNext()) {
				sb.append(",");
			}
		}
		sb.append(getCollectionClosingSymbol(collection));
		return sb.toString();
	}
	
	public static String prettyPrint(QName qname) {
		if (qname == null) {
			return "null";
		}
		if (qname.getNamespaceURI() != null && qname.getNamespaceURI().startsWith(SchemaConstants.NS_MIDPOINT_PUBLIC_PREFIX)) {
			return "{..."+qname.getNamespaceURI().substring(SchemaConstants.NS_MIDPOINT_PUBLIC_PREFIX.length())+"}"+qname.getLocalPart();
		}
		return qname.toString();
	}
	
	public static String prettyPrint(AssignmentType assignmentType) {
		if (assignmentType == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("AssignmentType(");
		if (assignmentType.getTarget() != null) {
			sb.append("target:");
			sb.append(prettyPrint(assignmentType.getTarget()));
		}
		if (assignmentType.getTargetRef() != null) {
			sb.append("target:");
			sb.append(prettyPrint(assignmentType.getTargetRef()));
		}
		if (assignmentType.getAccountConstruction() != null) {
			sb.append(prettyPrint(assignmentType.getAccountConstruction()));
		}
		sb.append(", ");
		if (assignmentType.getActivation() != null) {
			sb.append(prettyPrint(assignmentType.getActivation()));
		}
		sb.append(")");
		return sb.toString();
	}
	
	public static String prettyPrint(AccountConstructionType act) {
		if (act == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("AccountConstructionType(");
		if (act.getResource() != null) {
			sb.append(prettyPrint(act.getResource()));
		}
		if (act.getResourceRef() != null) {
			sb.append(prettyPrint(act.getResourceRef()));
		}
		sb.append(", ");
		if (act.getType() != null) {
			sb.append("type=");
			sb.append(act.getType());
			sb.append(", ");
		}
		if (act.getAttribute() != null) {
			for (ValueConstructionType attrConstr: act.getAttribute()) {
				sb.append(prettyPrint(attrConstr));
			}
		}
		// TODO: Other properties
		sb.append(")");
		return sb.toString();
	}
	
	public static String prettyPrint(ValueConstructionType vc) {
		if (vc == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("ValueConstructionType(");
		if (vc.getRef() != null) {
			sb.append("ref=");
			sb.append(prettyPrint(vc.getRef()));
			sb.append(",");
		}
		
		if (vc.getValueConstructor() != null) {
			prettyPringValueConstructor(sb, vc.getValueConstructor());
			sb.append(",");
		}
		
		if (vc.getSequence() != null) {
			sb.append("[");
			for (JAXBElement vconstr: vc.getSequence().getValueConstructor()) {
				prettyPringValueConstructor(sb,vconstr);
				sb.append(",");
			}
			sb.append("]");
		}
		
		// TODO: Other properties
		sb.append(")");
		return sb.toString();
	}
	
	private static void prettyPringValueConstructor(StringBuilder sb, JAXBElement vconstr) {
		sb.append("ValueConstructor(");
		sb.append(prettyPrint(vconstr));
		sb.append(")");
	}

	public static String prettyPrint(ObjectReferenceType ref) {
		if (ref == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("ref(");
		sb.append(ref.getOid());
		sb.append(",");
		sb.append(prettyPrint(ref.getType()));
		sb.append(")");
		return sb.toString();
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
	
	public static String prettyPrint(ProtectedStringType protectedStringType) {
		if (protectedStringType == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("ProtectedStringType(");
		
		if (protectedStringType.getEncryptedData() != null) {
			sb.append("[encrypted data]");
		}

		if (protectedStringType.getClearValue() != null) {
			sb.append("\"");
			sb.append(protectedStringType.getClearValue());
			sb.append("\"");
		}

		sb.append(")");
		return sb.toString();
	}
	
	public static String prettyPrint(OperationResultType resultType) {
		if (resultType == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("RT(");
		sb.append(resultType.getOperation());
		sb.append(",");
		sb.append(resultType.getStatus());
		sb.append(",");
		sb.append(resultType.getMessage());
		sb.append(")");
		return sb.toString();
	}

	public static String prettyPrint(AccountShadowType object) {
		return prettyPrint(object,false);
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

	public static String debugDump(AccountShadowType object, int indent) {
		StringBuilder sb = new StringBuilder();
		indentDebugDump(sb, indent);
		if (object == null) {
			sb.append("null");
			return sb.toString();
		}
		sb.append(object.getClass().getSimpleName());
		sb.append("(");
		sb.append(object.getOid());
		sb.append(",name=");
		sb.append(object.getName());
		
		if (object.getResource() != null) {
			sb.append("\n");
			indentDebugDump(sb, indent + 1);
			sb.append("resource: ");
			sb.append(ObjectTypeUtil.toShortString(object.getResource()));
		}
		if (object.getResourceRef() != null) {
			sb.append("\n");
			indentDebugDump(sb, indent + 1);
			sb.append("resourceRef: ");
			sb.append(ObjectTypeUtil.toShortString(object.getResourceRef()));
		}
		
		sb.append("\n");
		indentDebugDump(sb, indent + 1);
		sb.append("objectClass: ");
		sb.append(prettyPrint(object.getObjectClass()));

		sb.append("\n");
		indentDebugDump(sb, indent + 1);
		sb.append("attributes:");
		Attributes attributes = object.getAttributes();
		if (attributes == null) {
			sb.append("null");
		} else {
			sb.append("\n");
			sb.append(debugDumpXsdAnyProperties(attributes.getAny(),indent + 2));
		}
		
		
		sb.append("\n");
		indentDebugDump(sb, indent + 1);
		sb.append("activation:");
		if (object.getActivation() == null) {
			sb.append("null");
		} else {
			sb.append("\n");
			sb.append(debugDump(object.getActivation(),indent+2));
		}
		
		// TODO: more
		
		return sb.toString();
	}

	private static String debugDump(ActivationType activation, int indent) {
		StringBuilder sb = new StringBuilder();
		indentDebugDump(sb, indent);
		sb.append("enabled: ");
		sb.append(activation.isEnabled());
		if (activation.getValidFrom() != null) {
			sb.append("\n");
			indentDebugDump(sb, indent);
			sb.append("valid from: ");
			sb.append(activation.getValidFrom());
		}
		if (activation.getValidTo() != null) {
			sb.append("\n");
			indentDebugDump(sb, indent);
			sb.append("valid to: ");
			sb.append(activation.getValidTo());
		}
		return sb.toString();
	}

	public static String prettyPrint(ObjectModificationType objectChange) {
		if (objectChange == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("ObjectModification(");
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
		StringBuilder sb = new StringBuilder("ProperyModification(");
		sb.append(change.getModificationType());
		sb.append(",");
		if (change.getPath() != null) {
			XPathHolder xpath = new XPathHolder(change.getPath());
			sb.append(xpath.toString());
		} else {
			sb.append("xpath=null");
		}
		sb.append(",");

		for (Object element : change.getValue().getAny()) {
			sb.append(prettyPrint(element));
			sb.append(",");
		}

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
			if (element.getLocalName() != null) {
				sb.append(new QName(element.getNamespaceURI(), element.getLocalName()));
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
			ResourceObjectShadowType value = iterator.next();
			sb.append(prettyPrint(value));
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

	public static String prettyPrint(PagingType paging) {

		if (paging == null) {
			return "null";
		}

		StringBuilder sb = new StringBuilder("Paging(");

		if (paging.getOffset() != null) {
			sb.append(paging.getOffset()).append(",");
		} else {
			sb.append(",");
		}

		if (paging.getMaxSize() != null) {
			sb.append(paging.getMaxSize()).append(",");
		} else {
			sb.append(",");
		}

		if (paging.getOrderBy() != null) {
			sb.append(prettyPrint(paging.getOrderBy())).append(",");
		} else {
			sb.append(",");
		}

		if (paging.getOrderDirection() != null) {
			sb.append(paging.getOrderDirection());
		}

		sb.append(")");

		return sb.toString();
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
	
	public static String prettyPrint(ObjectChangeAdditionType change) {
		if (change == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("ObjectChangeAdditionType(");
		sb.append(prettyPrint(change.getObject(), true));
		sb.append(")");
		return sb.toString();
	}
	
	public static String prettyPrint(ObjectChangeModificationType change) {
		if (change == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("ObjectChangeModificationType(");
		sb.append(prettyPrint(change.getObjectModification()));
		sb.append(")");
		return sb.toString();
	}

	public static String prettyPrint(ObjectChangeDeletionType change) {
		if (change == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("ObjectChangeDeletionType(");
		sb.append(change.getOid());
		sb.append(")");
		return sb.toString();
	}

	public static String prettyPrint(ObjectChangeType change) {
		if (change == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder();
		if (change instanceof ObjectChangeAdditionType) {
			return prettyPrint((ObjectChangeAdditionType) change);
		} else if (change instanceof ObjectChangeModificationType) {
			return prettyPrint((ObjectChangeModificationType) change);
		} else if (change instanceof ObjectChangeDeletionType) {
			return prettyPrint((ObjectChangeDeletionType) change);
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
			return prettyPrint(JAXBUtil.toDomElement(element));
		} catch (JAXBException ex) {
			return ("Error marshalling the object: " + ex.getMessage());
		}
	}
	
	public static String prettyPrint(UnknownJavaObjectType xml) {
		if (xml == null) {
			return "null";
		}
		return "Java("+xml.getClazz()+","+xml.getToString()+")";
	}

//	public static String prettyPrint(Object value) {
//		if (value == null) {
//			return "null";
//		}
//		// TODO: temporary hack. Maybe we can use
//		// reflection instead of horde of if-s
//		if (value instanceof ObjectType) {
//			ObjectType object = (ObjectType) value;
//			return prettyPrint(object);
//		} else if (value instanceof JAXBElement) {
//			return prettyPrint((JAXBElement)value);
//		} else {
//			return value.toString();
//		}
//	}
	
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
		if (value instanceof ObjectType) {
			// ObjectType has many subtypes, difficult to sort out using reflection
			// therefore we special-case it
			return prettyPrint((ObjectType)value);
		}
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
