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

package com.evolveum.midpoint.prism.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

public class QueryConvertor {
	
	public static QName KEY_FILTER = new QName(null, "filter");
	public static QName KEY_PAGING = new QName(null, "paging");
	public static QName KEY_CONDITION = new QName(null, "condition");
	
	public static final QName KEY_FILTER_AND = new QName(null, "and");
	public static final QName KEY_FILTER_OR = new QName(null, "or");
	public static final QName KEY_FILTER_NOT = new QName(null, "not");
	public static final QName KEY_FILTER_EQUAL = new QName(null, "equal");
	public static final QName KEY_FILTER_REF = new QName(null, "ref");
	public static final QName KEY_FILTER_SUBSTRING = new QName(null, "substring");
	public static final QName KEY_FILTER_ORG = new QName(null, "org");
	
	private static final QName KEY_FILTER_EQUALS_PATH = new QName(null, "path");
	private static final QName KEY_FILTER_EQUALS_MATCHING = new QName(null, "matching");
	private static final QName KEY_FILTER_EQUALS_VALUE = new QName(null, "value");
	private static final QName KEY_FILTER_EQUALS_EXPRESSION = new QName(null, "expression");
	private static final QName KEY_FILTER_EQUALS_VALUE_EXPRESSION = new QName(null, "valueExpression"); // deprecated
	
	public static final QName KEY_FILTER_ORG_REF = new QName(null, "orgRef");
	public static final QName KEY_FILTER_ORG_REF_OID = new QName(null, "oid");
	public static final QName KEY_FILTER_ORG_MIN_DEPTH = new QName(null, "minDepth");
	public static final QName KEY_FILTER_ORG_MAX_DEPTH = new QName(null, "maxDepth");
	
	public static final String NS_QUERY = "http://prism.evolveum.com/xml/ns/public/query-2";
	public static final QName FILTER_ELEMENT_NAME = new QName(NS_QUERY, "filter");

	public static <O extends Objectable> ObjectQuery parseQuery(MapXNode xmap, Class<O> clazz, PrismContext prismContext)
			throws SchemaException {

		if (xmap == null){
			return null;
		}
		
		PrismObjectDefinition<O> objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(clazz);

		if (objDef == null) {
			throw new SchemaException("Cannot find obj definition for "+clazz);
		}

		return parseQuery(xmap, objDef);
	}
	
	public static <O extends Objectable> ObjectQuery parseQuery(MapXNode xmap, PrismObjectDefinition<O> objDef) throws SchemaException {
		if (xmap == null){
			return null;
		}
		
		XNode xnodeFilter = xmap.get(KEY_FILTER);
		if (xnodeFilter != null) {
			throw new SchemaException("No filter in query");
		}
		if (!(xnodeFilter instanceof MapXNode)) {
			throw new SchemaException("Cannot parse filter from "+xnodeFilter);
		}
		ObjectFilter filter = parseFilter((MapXNode)xnodeFilter, objDef);
		
		ObjectQuery query = new ObjectQuery();
		query.setFilter(filter);
		
		XNode xnodeCondition = xmap.get(KEY_CONDITION);
		if (xnodeCondition != null) {
			query.setCondition(xnodeCondition);
		}

		XNode xnodePaging = xmap.get(KEY_PAGING);
		if (xnodePaging != null) {
			throw new UnsupportedOperationException("work in progress");
//			ObjectPaging paging = PagingConvertor.parsePaging(xnodePaging);
//			query.setPaging(paging);
		}

		return query;
	}
	
	/**
	 * Used by XNodeProcessor and similar code that does not have complete schema for the filter 
	 */
	public static ObjectFilter parseFilter(XNode xnode, PrismContext prismContext) throws SchemaException {
		MapXNode xmap = toMap(xnode);
		return parseFilterContainer(xmap, null, prismContext);
	}
	
	public static <O extends Objectable> ObjectFilter parseFilter(MapXNode xmap, PrismObjectDefinition<O> objDef) throws SchemaException {
		if (xmap == null) {
			return null;
		}
		return parseFilterContainer(xmap, objDef, objDef.getPrismContext());
	}

	private static <C extends Containerable> ObjectFilter parseFilterContainer(MapXNode xmap, PrismContainerDefinition<C> pcd,
			PrismContext prismContext) throws SchemaException {
		Entry<QName, XNode> entry = singleSubEntry(xmap);
		QName filterQName = entry.getKey();
		XNode xsubnode = entry.getValue();
		return parseFilterContainer(xsubnode, filterQName, pcd, prismContext);
	}

	private static <C extends Containerable> ObjectFilter parseFilterContainer(XNode xsubnode, QName filterQName, 
			PrismContainerDefinition<C> pcd, PrismContext prismContext) throws SchemaException {
		
		if (QNameUtil.match(filterQName, KEY_FILTER_AND)) {
			return parseAndFilter(xsubnode, pcd, prismContext);
		}
		
		if (QNameUtil.match(filterQName, KEY_FILTER_OR)) {
			return parseOrFilter(xsubnode, pcd, prismContext);
		}
		
		if (QNameUtil.match(filterQName, KEY_FILTER_NOT)) {
			return parseNotFilter(xsubnode, pcd, prismContext);
		}
		
		if (QNameUtil.match(filterQName, KEY_FILTER_EQUAL)) {
			return parseEqualFilter(xsubnode, pcd, prismContext);
		}

		if (QNameUtil.match(filterQName, KEY_FILTER_REF)) {
			return parseRefFilter(xsubnode, pcd);
		}

		if (QNameUtil.match(filterQName, KEY_FILTER_SUBSTRING)) {
			return parseSubstringFilter(xsubnode, pcd);
		}

		if (QNameUtil.match(filterQName, KEY_FILTER_ORG)) {
			return parseOrgFilter(xsubnode, pcd);
		}

		throw new UnsupportedOperationException("Unsupported query filter " + filterQName);

	}

	private static <C extends Containerable> AndFilter parseAndFilter(XNode xnode, PrismContainerDefinition<C> pcd,
			PrismContext prismContext) throws SchemaException {
		List<ObjectFilter> subfilters = new ArrayList<ObjectFilter>();
		MapXNode xmap = toMap(xnode);
		for (Entry<QName, XNode> entry : xmap.entrySet()) {
			ObjectFilter subfilter = parseFilterContainer(entry.getValue(), entry.getKey(), pcd, prismContext);
			subfilters.add(subfilter);
		}
		return AndFilter.createAnd(subfilters);
	}

	private static <C extends Containerable> OrFilter parseOrFilter(XNode xnode, PrismContainerDefinition<C> pcd,
			PrismContext prismContext) throws SchemaException {
		List<ObjectFilter> subfilters = new ArrayList<ObjectFilter>();
		MapXNode xmap = toMap(xnode);
		for (Entry<QName, XNode> entry : xmap.entrySet()) {
			ObjectFilter subfilter = parseFilterContainer(entry.getValue(), entry.getKey(), pcd, prismContext);
			subfilters.add(subfilter);
		}
		return OrFilter.createOr(subfilters);
	}

	private static <C extends Containerable> NotFilter parseNotFilter(XNode xnode, PrismContainerDefinition<C> pcd,
			PrismContext prismContext) throws SchemaException {
		List<ObjectFilter> subfilters = new ArrayList<ObjectFilter>();
		MapXNode xmap = toMap(xnode);
		Entry<QName, XNode> entry = singleSubEntry(xmap);
		ObjectFilter subfilter = parseFilterContainer(entry.getValue(), entry.getKey(), pcd, prismContext);
		return NotFilter.createNot(subfilter);
	}
	
	private static <T,C extends Containerable> EqualsFilter<PrismPropertyDefinition<T>> parseEqualFilter(XNode xnode,
			PrismContainerDefinition<C> pcd, PrismContext prismContext) throws SchemaException {
		MapXNode xmap = toMap(xnode);
		ItemPath itemPath = getPath(xmap);

		if (itemPath == null || itemPath.isEmpty()){
			throw new SchemaException("Could not convert query, because query does not contain item path.");	
		}
		
		QName matchingRule = determineMatchingRule(xmap);
		
		if (itemPath.last() == null){
			throw new SchemaException("Cannot convert query, becasue query does not contian property path.");
		}
		QName itemName = ItemPath.getName(itemPath.last());
		ItemPath parentPath = itemPath.allExceptLast();
		if (parentPath.isEmpty()){
			parentPath = null;
		}
		
		ItemDefinition itemDefinition = null;
		if (pcd != null) {
			itemDefinition = pcd.findItemDefinition(itemPath);
			if (itemDefinition == null) {
				throw new SchemaException("No definition for item "+itemPath+" in "+pcd);
			}
		}

		XNode valueXnode = xmap.get(KEY_FILTER_EQUALS_VALUE);
		
		if (valueXnode != null) {
			Item<PrismValue> item = prismContext.getXnodeProcessor().parseItem(valueXnode, itemName, itemDefinition);

			if (item.getValues().size() < 1 ) {
				throw new IllegalStateException("No values to search specified for item " + itemName);
			}

			return EqualsFilter.createEqual(itemPath, (PrismProperty) item, matchingRule);
			
		} else {
			XNode expressionXnode = xmap.get(KEY_FILTER_EQUALS_EXPRESSION);
			if (expressionXnode == null) {
				expressionXnode = xmap.get(KEY_FILTER_EQUALS_VALUE_EXPRESSION);
			}
			
			return EqualsFilter.createEqual(itemPath, (PrismPropertyDefinition) itemDefinition, matchingRule, expressionXnode);
		}
				
	}
	
	private static <C extends Containerable> RefFilter parseRefFilter(XNode xnode, PrismContainerDefinition<C> pcd) throws SchemaException{
		MapXNode xmap = toMap(xnode);
		ItemPath itemPath = getPath(xmap);
		
		if (itemPath == null || itemPath.isEmpty()){
			throw new SchemaException("Cannot convert query, becasue query does not contian property path.");
		}
		
		if (itemPath.last() == null){
			throw new SchemaException("Cannot convert query, becasue query does not contian property path.");
		}

		QName itemName = ItemPath.getName(itemPath.last());
		ItemPath parentPath = itemPath.allExceptLast();
		if (parentPath.isEmpty()){
			parentPath = null;
		}
		
		ItemDefinition itemDefinition = null;
		if (pcd != null) {
			itemDefinition = pcd.findItemDefinition(itemPath);
			if (itemDefinition == null) {
				throw new SchemaException("No definition for item "+itemPath+" in "+pcd);
			}
		}

		XNode valueXnode = xmap.get(KEY_FILTER_EQUALS_VALUE);
		
		Item<?> item = pcd.getPrismContext().getXnodeProcessor().parseItem(valueXnode, itemName, itemDefinition);
		PrismReference ref = (PrismReference)item;

		if (item.getValues().size() < 1 ) {
			throw new IllegalStateException("No values to search specified for item " + itemName);
		}

		XNode expressionXnode = xmap.get(KEY_FILTER_EQUALS_EXPRESSION);
		if (expressionXnode == null) {
			expressionXnode = xmap.get(KEY_FILTER_EQUALS_VALUE_EXPRESSION);
		}
		
		return RefFilter.createReferenceEqual(itemPath, ref, expressionXnode);
	}

	private static <C extends Containerable> SubstringFilter parseSubstringFilter(XNode xnode, PrismContainerDefinition<C> pcd)
			throws SchemaException {
		MapXNode xmap = toMap(xnode);
		ItemPath itemPath = getPath(xmap);

		if (itemPath == null || itemPath.isEmpty()){
			throw new SchemaException("Could not convert query, because query does not contain item path.");	
		}
		
		QName matchingRule = determineMatchingRule(xmap);
		
		if (itemPath.last() == null){
			throw new SchemaException("Cannot convert query, becasue query does not contian property path.");
		}
		QName itemName = ItemPath.getName(itemPath.last());
		ItemPath parentPath = itemPath.allExceptLast();
		if (parentPath.isEmpty()){
			parentPath = null;
		}
		
		ItemDefinition itemDefinition = null;
		if (pcd != null) {
			itemDefinition = pcd.findItemDefinition(itemPath);
			if (itemDefinition == null) {
				throw new SchemaException("No definition for item "+itemPath+" in "+pcd);
			}
		}

		String substring = xmap.getParsedPrimitiveValue(KEY_FILTER_EQUALS_VALUE, DOMUtil.XSD_STRING);
				
		if (StringUtils.isBlank(substring)) {
			throw new IllegalStateException("No substring values to search specified for item " + itemName);
		}

		return SubstringFilter.createSubstring(itemPath, (PrismPropertyDefinition)itemDefinition, matchingRule, substring);
	}

	private static <C extends Containerable> OrgFilter parseOrgFilter(XNode xnode, PrismContainerDefinition<C> pcd) throws SchemaException {
		MapXNode xmap = toMap(xnode);
		
		XNode xorgrefnode = xmap.get(KEY_FILTER_ORG_REF);
		if (xorgrefnode == null) {
			throw new SchemaException("No organization refenrence defined in the search query.");
		}
		MapXNode xorgrefmap = toMap(xorgrefnode);
		String orgOid = xorgrefmap.getParsedPrimitiveValue(KEY_FILTER_ORG_REF_OID, DOMUtil.XSD_STRING);
		if (orgOid == null || StringUtils.isBlank(orgOid)) {
			throw new SchemaException("No oid attribute defined in the organization reference element.");
		}

		String minDepth = xmap.getParsedPrimitiveValue(KEY_FILTER_ORG_MIN_DEPTH, DOMUtil.XSD_STRING);
		Integer min = null;
		if (!StringUtils.isBlank(minDepth)) {
			min = XsdTypeMapper.multiplicityToInteger(minDepth);
		}

		String maxDepth = xmap.getParsedPrimitiveValue(KEY_FILTER_ORG_MAX_DEPTH, DOMUtil.XSD_STRING);
		Integer max = null;
		if (!StringUtils.isBlank(maxDepth)) {
			max = XsdTypeMapper.multiplicityToInteger(maxDepth);
		}

		return OrgFilter.createOrg(orgOid, min, max);
	}

	
	
	
	
	
	
	private static Entry<QName, XNode> singleSubEntry(MapXNode xmap) throws SchemaException {
		if (xmap == null || xmap.isEmpty()) {
			return null;
		}
		
		if (xmap.size() > 1) {
			throw new SchemaException("More than one element in search filter");
		}
		
		Entry<QName, XNode> entry = xmap.entrySet().iterator().next();
		return entry;
	}

	private static MapXNode toMap(XNode xnode) throws SchemaException {
		if (!(xnode instanceof MapXNode)) {
			throw new SchemaException("Cannot parse filter from "+xnode);
		}
		return (MapXNode)xnode;
	}
	
	private static ItemPath getPath(MapXNode xmap) throws SchemaException {
		return xmap.getParsedPrimitiveValue(KEY_FILTER_EQUALS_PATH, ItemPath.XSD_TYPE);
	}

	private static QName determineMatchingRule(MapXNode xmap) throws SchemaException{
		String matchingRuleLocalPart = xmap.getParsedPrimitiveValue(KEY_FILTER_EQUALS_MATCHING, DOMUtil.XSD_STRING);
		if (StringUtils.isNotBlank(matchingRuleLocalPart)){
			return new QName(PrismConstants.NS_MATCHING_RULE, matchingRuleLocalPart);
		} else {
			return null;
		}
	}		




	
	public static MapXNode serializeFilter(ObjectFilter filter) {
		// TODO
		return null;
	}
	
	
	
	
	
	
	
	
	
	
	
	
//	
//	
//	
//	
//	
//	private static Element createFilterType(ObjectFilter filter, Document doc, PrismContext prismContext) throws SchemaException{
//
//		if (filter instanceof AndFilter) {
//			return createAndFilterType((AndFilter) filter, doc, prismContext);
//		}
//		if (filter instanceof OrFilter) {
//			return createOrFilterType((OrFilter) filter, doc, prismContext);
//		}
//		if (filter instanceof NotFilter) {
//			return createNotFilterType((NotFilter) filter, doc, prismContext);
//		}
//		if (filter instanceof EqualsFilter) {
//			return createEqualsFilterType((EqualsFilter) filter, doc, prismContext);
//		}
//		if (filter instanceof RefFilter) {
//			return createRefFilterType((RefFilter) filter, doc, prismContext);
//		}
//
//		if (filter instanceof SubstringFilter) {
//			return createSubstringFilterType((SubstringFilter) filter, doc, prismContext);
//		}
//
//		if (filter instanceof OrgFilter) {
//			return createOrgFilterType((OrgFilter) filter, doc, prismContext);
//		}
//
//		throw new UnsupportedOperationException("Unsupported filter type: " + filter);
//	}
//
//	private static Element createAndFilterType(AndFilter filter, Document doc, PrismContext prismContext) throws SchemaException{
//
//		Element and = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_AND);
//
//		for (ObjectFilter of : filter.getCondition()) {
//			Element element = createFilterType(of, doc, prismContext);
//			and.appendChild(element);
//		}
//		return and;
//	}
//
//	private static Element createOrFilterType(OrFilter filter, Document doc, PrismContext prismContext) throws SchemaException{
//
//		Element or = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_OR);
//		for (ObjectFilter of : filter.getCondition()) {
//			Element element = createFilterType(of, doc, prismContext);
//			or.appendChild(element);
//		}
//		return or;
//	}
//
//	private static Element createNotFilterType(NotFilter filter, Document doc, PrismContext prismContext) throws SchemaException{
//
//		Element not = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_NOT);
//
//		Element element = createFilterType(filter.getFilter(), doc, prismContext);
//		not.appendChild(element);
//		return not;
//	}
//
//	private static <T> Element createEqualsFilterType(EqualsFilter<T> filter, Document doc , PrismContext prismContext) throws SchemaException{
//
//		Element equal = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_EQUAL);
//		
////		equal.appendChild(value);
//		
//		createMatchingRuleElement(filter, equal, doc);
//		
////		if (filter.getMatchingRule() != null){
////			Element matching = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_MATCHING);
////			matching.setTextContent(filter.getMatchingRule().getLocalPart());
////			equal.appendChild(matching);
//		// }
//		//
//		Element path = createPathElement(filter, doc);
//		equal.appendChild(path);
//
//		QName propertyName = filter.getDefinition().getName();
//		
//		if (filter.getValues() == null || filter.getValues().isEmpty()){
//			equal.appendChild(DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_VALUE));
//		}
//		
//		for (PrismPropertyValue<T> val : filter.getValues()) {
//			if (val.getParent() == null) {
//				val.setParent(filter);
//			}
//			Element value = createValueElement(val, propertyName, doc, filter, prismContext);
//			equal.appendChild(value);
//		}
//		return equal;
//	}
//	
//	
//	private static Element createValueElement(PrismPropertyValue val, QName propertyName, Document doc, PropertyValueFilter filter, PrismContext prismContext) throws SchemaException{
//		Element value = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_VALUE);
//		Element element = prismContext.getPrismDomProcessor().serializeValueToDom(val, propertyName, doc);
//		if (PolyString.class.equals(filter.getDefinition().getTypeClass()) || PolyStringType.class.equals(filter.getDefinition().getTypeClass())) {
//			for (Element e : DOMUtil.listChildElements(element)){
//				value.appendChild(e);
//			}
//		} else{
//			value.setTextContent(element.getTextContent());
//		}
////		if (XmlTypeConverter.canConvert(val.getClass())){
////			Element propVal = val.asDomElement();
////			value.setTextContent(propVal.getTextContent());
////		} else {
////			value.setTextContent(String.valueOf(((PrismPropertyValue)val).getValue()));
////		}
////		value.setTextContent();
//		return value;
//
//	}
//	
//	private static Element createRefFilterType(RefFilter filter, Document doc, PrismContext prismContext) throws SchemaException {
//
//		Element ref = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_REF);
//
//		Element path = createPathElement(filter, doc);
//		ref.appendChild(path);
//
//		List<PrismReferenceValue> values = (List<PrismReferenceValue>) filter.getValues();
//		if (values.size() < 1) {
//			throw new SchemaException("No values for search in the ref filter.");
//		}
//
//		if (values.size() > 1) {
//			throw new SchemaException("More than one prism reference value not allowed in the ref filter");
//		}
//
//		PrismReferenceValue val = values.get(0);
//		if (val.getOid() != null) {
//			Element oid = DOMUtil.createElement(doc, PrismConstants.Q_OID);
//			oid.setTextContent(String.valueOf(val.getOid()));
//			ref.appendChild(oid);
//		}
//		if (val.getTargetType() != null) {
//			Element type = DOMUtil.createElement(doc, PrismConstants.Q_TYPE);
//			XPathHolder xtype = new XPathHolder(val.getTargetType());
//			type.setTextContent(xtype.getXPath());
//			ref.appendChild(type);
//		}
//		if (val.getRelation() != null) {
//			Element relation = DOMUtil.createElement(doc, PrismConstants.Q_RELATION);
//			XPathHolder xrelation = new XPathHolder(val.getRelation());
//			relation.setTextContent(xrelation.getXPath());
//			ref.appendChild(relation);
//		}
//
//		return ref;
//	}
//
//	private static <T> Element createSubstringFilterType(SubstringFilter<T> filter, Document doc, PrismContext prismContext) throws SchemaException {
//		Element substring = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_SUBSTRING);
////		Element value = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_VALUE);
////		substring.appendChild(value);
//
//		Element path = createPathElement(filter, doc);
//		substring.appendChild(path);
//		
//		createMatchingRuleElement(filter, substring, doc);
//		
////		if (filter.getMatchingRule() != null){
////			Element matching = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_MATCHING);
////			matching.setTextContent(filter.getMatchingRule().getLocalPart());
////			substring.appendChild(matching);
////		}
//
//		QName propertyName = filter.getDefinition().getName();
//		if (filter.getValues() == null || filter.getValues().isEmpty()){
//			substring.appendChild(DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_VALUE));
//		}
//		
//		for (PrismPropertyValue<T> val : filter.getValues()) {
//			if (val.getParent() == null) {
//				val.setParent(filter);
//			}
//			Element value = createValueElement(val, propertyName, doc, filter, prismContext);
//			substring.appendChild(value);
//		}
//		
////		Element propValue = DOMUtil.createElement(doc, propertyName);
//		
//		return substring;
//	}
//	
//	private static void createMatchingRuleElement(ValueFilter filter, Element filterType, Document doc){
//		if (filter.getMatchingRule() != null){
//			Element matching = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_MATCHING);
//			matching.setTextContent(filter.getMatchingRule().getLocalPart());
//			filterType.appendChild(matching);
//		}
//
//	}
//
//	private static Element createOrgFilterType(OrgFilter filter, Document doc, PrismContext prismContext) {
//		Element org = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_ORG);
//
//		Element orgRef = null;
//		if (filter.getOrgRef() != null) {
//			orgRef = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_ORG_REF);
//			orgRef.setAttribute("oid", filter.getOrgRef().getOid());
//			org.appendChild(orgRef);
//		}
//
//		Element minDepth = null;
//		if (filter.getMinDepth() != null) {
//			minDepth = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_MIN_DEPTH);
//			minDepth.setTextContent(XsdTypeMapper.multiplicityToString(filter.getMinDepth()));
//			org.appendChild(minDepth);
//		}
//
//		Element maxDepth = null;
//		if (filter.getMaxDepth() != null) {
//			maxDepth = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_MAX_DEPTH);
//			maxDepth.setTextContent(XsdTypeMapper.multiplicityToString(filter.getMaxDepth()));
//			org.appendChild(maxDepth);
//		}
//
//		return org;
//	}
//
//	private static Element createPathElement(ValueFilter filter, Document doc) {
//		Element path = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_PATH);
//		XPathHolder xpath = null;
//		if (filter.getFullPath() != null) {
//			xpath = new XPathHolder(filter.getFullPath());
//		} else {
//			xpath = new XPathHolder(filter.getDefinition().getName());
//		}
//		path.setTextContent(xpath.getXPath());
//		return path;
//	}
//	
//	
	
	



}
