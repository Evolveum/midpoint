/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.schema.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.exception.SchemaException;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.evolveum.midpoint.prism.marshaller.ItemPathHolder;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoginEventType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScheduleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UnknownJavaObjectType;
import com.evolveum.prism.xml.ns._public.query_3.PagingType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 *
 * @author semancik
 */
public class SchemaDebugUtil {

	private static int SHOW_LIST_MEMBERS = 3;

	public static String debugDump(Collection<? extends DebugDumpable> dumpables) {
		return debugDump(dumpables,0);
	}

	public static String debugDump(Collection<? extends DebugDumpable> dumpables, int indent) {
		StringBuilder sb = new StringBuilder();
		indentDebugDump(sb, indent);
		sb.append(getCollectionOpeningSymbol(dumpables));
		if (!dumpables.isEmpty()) {
			sb.append("\n");
			for (DebugDumpable dd : dumpables) {
				if (dd == null) {
					indentDebugDump(sb, indent + 1);
					sb.append("null");
				} else {
					sb.append(dd.debugDump(indent + 1));
				}
				sb.append("\n");
			}
			indentDebugDump(sb, indent);
		}
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

	public static <K, V extends DebugDumpable> String dumpMapMultiLine(Map<K, V> map) {
		StringBuilder sb = new StringBuilder();
		debugDumpMapMultiLine(sb, map, 0);
		return sb.toString();
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
		if (objectType == null) {
			StringBuilder sb = new StringBuilder();
			DebugUtil.indentDebugDump(sb, indent);
			sb.append("null");
			return sb.toString();
		}
		return objectType.asPrismObject().debugDump(indent);
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
		if (assignmentType.getConstruction() != null) {
			sb.append(prettyPrint(assignmentType.getConstruction()));
		}
		sb.append(", ");
		if (assignmentType.getActivation() != null) {
			sb.append(prettyPrint(assignmentType.getActivation()));
		}
		sb.append(")");
		return sb.toString();
	}

	public static String prettyPrint(ConstructionType act) {
		if (act == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("ConstructionType(");
		if (act.getResource() != null) {
			sb.append(prettyPrint(act.getResource()));
		}
		if (act.getResourceRef() != null) {
			sb.append(prettyPrint(act.getResourceRef()));
		}
		sb.append(", ");
		if (act.getIntent() != null) {
			sb.append("intent=");
			sb.append(act.getIntent());
			sb.append(", ");
		}
		if (act.getAttribute() != null) {
			for (ResourceAttributeDefinitionType attrConstr: act.getAttribute()) {
				sb.append(prettyPrint(attrConstr));
			}
		}
		// TODO: Other properties
		sb.append(")");
		return sb.toString();
	}

	public static String prettyPrint(ResourceAttributeDefinitionType vc) {
		if (vc == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("ResourceAttributeDefinitionType(");
		if (vc.getRef() != null) {
			sb.append("ref=");
			sb.append(prettyPrint(vc.getRef()));

			boolean other = !vc.getInbound().isEmpty();
			if (vc.getOutbound() != null && vc.getOutbound().getExpression() != null ) {
				Object value = SimpleExpressionUtil.getConstantIfPresent(vc.getOutbound().getExpression());
				if (value != null) {
					sb.append(", value='").append(PrettyPrinter.prettyPrint(value)).append("'");
				} else {
					other = true;
				}
			}

			if (other) {
				sb.append(", ...");
			}
		}

		// TODO: Other properties
		sb.append(")");
		return sb.toString();
	}
	
	public static String prettyPrint(ExpressionType expressionType) {
		if (expressionType == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("ExpressionType(");
		appendPropertyIfNotNull(sb, "description", expressionType.getDescription());
		appendPropertyIfNotNull(sb, "extension", expressionType.getExtension());
		appendPropertyIfNotNull(sb, "trace", expressionType.isTrace());
		appendPropertyIfNotNull(sb, "variable", expressionType.getVariable());
		appendPropertyIfNotNull(sb, "returnMultiplicity", expressionType.getReturnMultiplicity());
		appendPropertyIfNotNull(sb, "allowEmptyValues", expressionType.isAllowEmptyValues());
		appendPropertyIfNotNull(sb, "queryInterpretationOfNoValue", expressionType.getQueryInterpretationOfNoValue());
		appendPropertyIfNotNull(sb, "runAsRef", expressionType.getRunAsRef());
		List<JAXBElement<?>> expressionEvaluators = expressionType.getExpressionEvaluator();
		sb.append("evaluator").append("=");
		if (expressionEvaluators.isEmpty()) {
			sb.append("[]");
		} else {
			if (expressionEvaluators.size() > 1) {
				sb.append("[");
			}
			for (JAXBElement<?> expressionEvaluator : expressionEvaluators) {
				sb.append(expressionEvaluator.getName().getLocalPart());
				sb.append(":");
				sb.append(PrettyPrinter.prettyPrint(expressionEvaluator.getValue()));
				if (expressionEvaluators.size() > 1) {
					sb.append(", ");
				}
			}
			if (expressionEvaluators.size() > 1) {
				sb.append("]");
			}
		}
		sb.append(")");
		return sb.toString();
	}
	
	public static String prettyPrint(ConstExpressionEvaluatorType expressionType) {
		if (expressionType == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("ConstExpressionEvaluatorType(");
		sb.append(expressionType.getValue());
		sb.append(")");
		return sb.toString();
	}

	private static void appendPropertyIfNotNull(StringBuilder sb, String propName, Object value) {
		if (value != null) {
			sb.append(propName).append("=").append(value).append(",");
		}
	}

	public static String prettyPrint(CachingMetadataType cachingMetadata) {
		if (cachingMetadata == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("CachingMetadataType(");
		if (cachingMetadata.getSerialNumber() != null) {
			sb.append("serialNumber:");
			sb.append(prettyPrint(cachingMetadata.getSerialNumber()));
		}
		if (cachingMetadata.getRetrievalTimestamp() != null) {
			sb.append("retrievalTimestamp:");
			sb.append(prettyPrint(cachingMetadata.getRetrievalTimestamp()));
		}
		sb.append(")");
		return sb.toString();
	}

	public static String prettyPrint(ScheduleType scheduleType) {
		if (scheduleType == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("ScheduleType(");
		if (scheduleType.getCronLikePattern() != null) {
			sb.append("cronLikePattern:");
			sb.append(scheduleType.getCronLikePattern());
		}

		if (scheduleType.getEarliestStartTime() != null) {
			sb.append("earliestStartTime:");
			sb.append(prettyPrint(scheduleType.getEarliestStartTime()));
		}
		if (scheduleType.getInterval() != null) {
			sb.append("interval:");
			sb.append(prettyPrint(scheduleType.getInterval()));
		}
		if (scheduleType.getLatestStartTime() != null) {
			sb.append("latestStartTime:");
			sb.append(prettyPrint(scheduleType.getLatestStartTime()));
		}
		if (scheduleType.getLatestFinishTime() != null) {
			sb.append("latestFinishTime:");
			sb.append(prettyPrint(scheduleType.getLatestFinishTime()));
		}
		if (scheduleType.getMisfireAction() != null) {
			sb.append("misfireAction:");
			sb.append(prettyPrint(scheduleType.getMisfireAction()));
		}

		sb.append(")");
		return sb.toString();
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
		Iterator<ItemPathType> iterator = reflist.getProperty().iterator();
		while (iterator.hasNext()) {
			ItemPathType xpath = iterator.next();
			sb.append(xpath.toString());
			if (iterator.hasNext()) {
				sb.append(",");
			}
		}
		sb.append("]");
		return sb.toString();
	}

	public static String prettyPrint(ObjectType object) {
		if (object == null) {
			return "null";
		}
		return object.asPrismObject().toString();
	}


	public static String prettyPrint(ProtectedStringType protectedStringType) {
		if (protectedStringType == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("ProtectedStringType(");

		if (protectedStringType.getEncryptedDataType() != null) {
			sb.append("[encrypted data]");
		}

		if (protectedStringType.getHashedDataType() != null) {
			sb.append("[hashed data]");
		}

		if (protectedStringType.getClearValue() != null) {
			sb.append("\"");
			if (InternalsConfig.isAllowClearDataLogging()) {
				sb.append(protectedStringType.getClearValue());
			} else {
				sb.append("[clear data]");
			}
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

	public static String prettyPrint(ItemDeltaType change) throws SchemaException {
		if (change == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("ProperyModification(");
		sb.append(change.getModificationType());
		sb.append(",");
		if (change.getPath() != null) {
			//FIXME : xpath vs itemPath
			ItemPathHolder xpath = new ItemPathHolder(change.getPath().getItemPath());
			sb.append(xpath.toString());
		} else {
			sb.append("xpath=null");
		}
		sb.append(",");

		List<RawType> values = change.getValue();
		for (RawType value : values) {
			sb.append(prettyPrint(value.serializeToXNode()));       // todo implement correctly...
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

//	public static String prettyPrint(QueryType query) {
//
//		if (query == null) {
//			return "null";
//		}
//
//		SearchFilterType filterType = query.getFilter();
//		Element filter = null;
//		if (filterType != null) {
//			filter = filterType.getFilterClause();
//		}
//
//		StringBuilder sb = new StringBuilder("Query(");
//
//		prettyPrintFilter(sb, filter);
//
//		sb.append(")");
//
//		return sb.toString();
//	}

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

	private static void prettyPrintFilter(StringBuilder sb, ObjectFilter filter) {

		if (filter == null) {
			sb.append("null");
			return;
		}
		sb.append("(");
		sb.append(filter.toString());
		sb.append(")");
	}

	private static void prettyPrintPaging(StringBuilder sb, ObjectPaging paging) {

		if (paging == null) {
			sb.append("null");
			return;
		}

		sb.append("(");
		sb.append(paging.toString());
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

	public static String prettyPrint(SynchronizationSituationDescriptionType syncDescType) {
		if (syncDescType == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("SyncDesc(");
		sb.append(syncDescType.getSituation());
		sb.append(",");
		sb.append(syncDescType.getTimestamp());
		if (syncDescType.getChannel() != null) {
			sb.append(",");
			sb.append(syncDescType.getChannel());
		}
		if (syncDescType.isFull() != null && syncDescType.isFull()) {
			sb.append(",full");
		}
		sb.append(")");
		return sb.toString();
	}

	public static String prettyPrint(ObjectDeltaOperationType deltaOpType) {
		if (deltaOpType == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("ObjectDeltaOperationType(");
		sb.append(prettyPrint(deltaOpType.getObjectDelta()));
		sb.append(": ");
		OperationResultType result = deltaOpType.getExecutionResult();
		if (result == null) {
			sb.append("null result");
		} else {
			sb.append(result.getStatus());
		}
		// object, resource?
		sb.append(")");
		return sb.toString();
	}
	
	public static String prettyPrint(LoginEventType loginEventType) {
		if (loginEventType == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder("LoginEventType(");
		sb.append(prettyPrint(loginEventType.getTimestamp()));
		String from = loginEventType.getFrom();
		if (from != null) {
			sb.append(" from ").append(from);
		}
		sb.append(")");
		return sb.toString();
	}

	public static String prettyPrint(JAXBElement<?> element) {
		return "JAXBElement("+PrettyPrinter.prettyPrint(element.getName())+"): "+element.getValue();
	}

	public static String prettyPrint(UnknownJavaObjectType xml) {
		if (xml == null) {
			return "null";
		}
		return "Java("+xml.getClazz()+","+xml.getToString()+")";
	}

//	public static String prettyPrint(OperationProvisioningScriptsType scriptsType) {
//		if (scriptsType == null) {
//			return "null";
//		}
//		StringBuilder sb = new StringBuilder("")
//		for (OperationProvisioningScriptType scriptType: scriptsType.getScript()) {
//
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
			if (c.getPackage().getName().equals("com.evolveum.midpoint.xml.ns._public.common.common_3")) {
				return c.getSimpleName();
			}
			return c.getName();
		}
		if (value instanceof Collection) {
			return prettyPrint((Collection<?>)value);
		}
		if (value instanceof ObjectQuery){
			return prettyPrint((ObjectQuery) value);
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
		for (Method method : SchemaDebugUtil.class.getMethods()) {
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

	public static String prettyPrint(ObjectQuery query){

		return query.toString();
	}

	static {
		PrettyPrinter.registerPrettyPrinter(SchemaDebugUtil.class);
	}

	public static void initialize() {
		// nothing to do here, we just make sure static initialization will take place
	}
}
