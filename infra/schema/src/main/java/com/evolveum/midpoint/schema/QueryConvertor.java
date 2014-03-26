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

package com.evolveum.midpoint.schema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
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
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProtectedStringType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

public class QueryConvertor {

	public static ObjectQuery createObjectQuery(Class clazz, QueryType queryType, PrismContext prismContext)
			throws SchemaException {

		if (queryType == null){
			return null;
		}
		
		Element criteria = queryType.getFilter();
		
		if (criteria == null && queryType.getPaging() == null){
			return null;
		}
		PrismObjectDefinition objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(clazz);

		if (objDef == null) {
			throw new SchemaException("cannot find obj definition");
		}

		try {
			ObjectQuery query = new ObjectQuery();
			
			Object condition = queryType.getCondition();
			if (condition != null){
				if (!(condition instanceof Element)){
					throw new SchemaException("Bad condition specified.");
				}
				query.setCondition((Element) condition);
			}
			
			if (criteria != null) {
				ObjectFilter filter = parseFilter(objDef, criteria);
				query.setFilter(filter);
			}

			if (queryType.getPaging() != null) {
				ObjectPaging paging = PagingConvertor.createObjectPaging(queryType.getPaging());
				query.setPaging(paging);
			}
			return query;
		} catch (SchemaException ex) {
			throw new SchemaException("Failed to convert query. Reason: " + ex.getMessage(), ex);
		}

	}

	public static QueryType createQueryType(ObjectQuery query, PrismContext prismContext) throws SchemaException{

		ObjectFilter filter = query.getFilter();
		try{
			QueryType queryType = new QueryType();
			Document doc = DOMUtil.getDocument();
			if (filter != null){
				Element filterType = createFilterType(filter, doc, prismContext);
				queryType.setFilter(filterType);
			}
		
		
		queryType.setPaging(PagingConvertor.createPagingType(query.getPaging()));
		queryType.setCondition(query.getCondition());
		return queryType;
		} catch (SchemaException ex){
			throw new SchemaException("Failed to convert query. Reason: " + ex.getMessage(), ex);
		}
		

	}

	public static Element createFilterType(ObjectFilter filter, Document doc, PrismContext prismContext) throws SchemaException{

		if (filter instanceof AndFilter) {
			return createAndFilterType((AndFilter) filter, doc, prismContext);
		}
		if (filter instanceof OrFilter) {
			return createOrFilterType((OrFilter) filter, doc, prismContext);
		}
		if (filter instanceof NotFilter) {
			return createNotFilterType((NotFilter) filter, doc, prismContext);
		}
		if (filter instanceof EqualsFilter) {
			return createEqualsFilterType((EqualsFilter) filter, doc, prismContext);
		}
		if (filter instanceof RefFilter) {
			return createRefFilterType((RefFilter) filter, doc, prismContext);
		}

		if (filter instanceof SubstringFilter) {
			return createSubstringFilterType((SubstringFilter) filter, doc, prismContext);
		}

		if (filter instanceof OrgFilter) {
			return createOrgFilterType((OrgFilter) filter, doc, prismContext);
		}

		throw new UnsupportedOperationException("Unsupported filter type: " + filter);
	}

	private static Element createAndFilterType(AndFilter filter, Document doc, PrismContext prismContext) throws SchemaException{

		Element and = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_AND);

		for (ObjectFilter of : filter.getCondition()) {
			Element element = createFilterType(of, doc, prismContext);
			and.appendChild(element);
		}
		return and;
	}

	private static Element createOrFilterType(OrFilter filter, Document doc, PrismContext prismContext) throws SchemaException{

		Element or = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_OR);
		for (ObjectFilter of : filter.getCondition()) {
			Element element = createFilterType(of, doc, prismContext);
			or.appendChild(element);
		}
		return or;
	}

	private static Element createNotFilterType(NotFilter filter, Document doc, PrismContext prismContext) throws SchemaException{

		Element not = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_NOT);

		Element element = createFilterType(filter.getFilter(), doc, prismContext);
		not.appendChild(element);
		return not;
	}

	private static <T> Element createEqualsFilterType(EqualsFilter<T> filter, Document doc , PrismContext prismContext) throws SchemaException{

		Element equal = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_EQUAL);
		
//		equal.appendChild(value);
		
		createMatchingRuleElement(filter, equal, doc);
		
//		if (filter.getMatchingRule() != null){
//			Element matching = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_MATCHING);
//			matching.setTextContent(filter.getMatchingRule().getLocalPart());
//			equal.appendChild(matching);
		// }
		//
		Element path = createPathElement(filter, doc);
		equal.appendChild(path);

		QName propertyName = filter.getDefinition().getName();
		
		if (filter.getValues() == null || filter.getValues().isEmpty()){
			equal.appendChild(DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_VALUE));
		}
		
		for (PrismPropertyValue<T> val : filter.getValues()) {
			if (val.getParent() == null) {
				val.setParent(filter);
			}
			Element value = createValueElement(val, propertyName, doc, filter, prismContext);
			equal.appendChild(value);
		}
		return equal;
	}
	
	
	private static Element createValueElement(PrismPropertyValue val, QName propertyName, Document doc, PropertyValueFilter filter, PrismContext prismContext) throws SchemaException{
		Element value = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_VALUE);
		Element element = prismContext.getPrismDomProcessor().serializeValueToDom(val, propertyName, doc);
		if (PolyString.class.equals(filter.getDefinition().getTypeClass()) || PolyStringType.class.equals(filter.getDefinition().getTypeClass())) {
			for (Element e : DOMUtil.listChildElements(element)){
				value.appendChild(e);
			}
		} else {
			value.setTextContent(element.getTextContent());
		}
//		if (XmlTypeConverter.canConvert(val.getClass())){
//			Element propVal = val.asDomElement();
//			value.setTextContent(propVal.getTextContent());
//		} else {
//			value.setTextContent(String.valueOf(((PrismPropertyValue)val).getValue()));
//		}
//		value.setTextContent();
		return value;

	}
	
	private static Element createRefFilterType(RefFilter filter, Document doc, PrismContext prismContext) throws SchemaException {

		Element ref = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_REF);

		Element path = createPathElement(filter, doc);
		ref.appendChild(path);

		List<PrismReferenceValue> values = (List<PrismReferenceValue>) filter.getValues();
		if (values.size() < 1) {
			throw new SchemaException("No values for search in the ref filter.");
		}

		if (values.size() > 1) {
			throw new SchemaException("More than one prism reference value not allowed in the ref filter");
		}

		PrismReferenceValue val = values.get(0);
		if (val.getOid() != null) {
			Element oid = DOMUtil.createElement(doc, PrismConstants.Q_OID);
			oid.setTextContent(String.valueOf(val.getOid()));
			ref.appendChild(oid);
		}
		if (val.getTargetType() != null) {
			Element type = DOMUtil.createElement(doc, PrismConstants.Q_TYPE);
			XPathHolder xtype = new XPathHolder(val.getTargetType());
			type.setTextContent(xtype.getXPath());
			ref.appendChild(type);
		}
		if (val.getRelation() != null) {
			Element relation = DOMUtil.createElement(doc, PrismConstants.Q_RELATION);
			XPathHolder xrelation = new XPathHolder(val.getRelation());
			relation.setTextContent(xrelation.getXPath());
			ref.appendChild(relation);
		}

		return ref;
	}

	private static <T> Element createSubstringFilterType(SubstringFilter<T> filter, Document doc, PrismContext prismContext) throws SchemaException {
		Element substring = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_SUBSTRING);
//		Element value = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_VALUE);
//		substring.appendChild(value);

		Element path = createPathElement(filter, doc);
		substring.appendChild(path);
		
		createMatchingRuleElement(filter, substring, doc);
		
//		if (filter.getMatchingRule() != null){
//			Element matching = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_MATCHING);
//			matching.setTextContent(filter.getMatchingRule().getLocalPart());
//			substring.appendChild(matching);
//		}

		QName propertyName = filter.getDefinition().getName();
		if (filter.getValues() == null || filter.getValues().isEmpty()){
			substring.appendChild(DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_VALUE));
		}
		
		for (PrismPropertyValue<T> val : filter.getValues()) {
			if (val.getParent() == null) {
				val.setParent(filter);
			}
			Element value = createValueElement(val, propertyName, doc, filter, prismContext);
			substring.appendChild(value);
		}
		
//		Element propValue = DOMUtil.createElement(doc, propertyName);
		
		return substring;
	}
	
	private static void createMatchingRuleElement(ValueFilter filter, Element filterType, Document doc){
		if (filter.getMatchingRule() != null){
			Element matching = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_MATCHING);
			matching.setTextContent(filter.getMatchingRule().getLocalPart());
			filterType.appendChild(matching);
		}

	}

	private static Element createOrgFilterType(OrgFilter filter, Document doc, PrismContext prismContext) {
		Element org = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_ORG);

		Element orgRef = null;
		if (filter.getOrgRef() != null) {
			orgRef = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_ORG_REF);
			orgRef.setAttribute("oid", filter.getOrgRef().getOid());
			org.appendChild(orgRef);
		}

		Element minDepth = null;
		if (filter.getMinDepth() != null) {
			minDepth = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_MIN_DEPTH);
			minDepth.setTextContent(XsdTypeMapper.multiplicityToString(filter.getMinDepth()));
			org.appendChild(minDepth);
		}

		Element maxDepth = null;
		if (filter.getMaxDepth() != null) {
			maxDepth = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_MAX_DEPTH);
			maxDepth.setTextContent(XsdTypeMapper.multiplicityToString(filter.getMaxDepth()));
			org.appendChild(maxDepth);
		}

		return org;
	}

	private static Element createPathElement(ValueFilter filter, Document doc) {
		Element path = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_PATH);
		XPathHolder xpath = null;
		if (filter.getFullPath() != null) {
			xpath = new XPathHolder(filter.getFullPath());
		} else {
			xpath = new XPathHolder(filter.getDefinition().getName());
		}
		path.setTextContent(xpath.getXPath());
		return path;
	}
	
	public static ObjectFilter parseFilter(PrismContainerDefinition pcd, Node filter) throws SchemaException {

		if (QNameUtil.compareQName(SchemaConstantsGenerated.Q_AND, filter)) {
			return createAndFilter(pcd, filter);
		}

		if (QNameUtil.compareQName(SchemaConstantsGenerated.Q_EQUAL, filter)) {
			return createEqualFilter(pcd, filter);
		}
		
		if (QNameUtil.compareQName(SchemaConstantsGenerated.Q_REF, filter)) {
			return createRefFilter(pcd, filter);
		}

		if (QNameUtil.compareQName(SchemaConstantsGenerated.Q_SUBSTRING, filter)) {
			return createSubstringFilter(pcd, filter);
		}

		if (QNameUtil.compareQName(SchemaConstantsGenerated.Q_ORG, filter)) {
			return createOrgFilter(pcd, filter);
		}

		if (QNameUtil.compareQName(SchemaConstantsGenerated.Q_OR, filter)) {
			return createOrFilter(pcd, filter);
		}

		if (QNameUtil.compareQName(SchemaConstantsGenerated.Q_NOT, filter)) {
			return createNotFilter(pcd, filter);
		}

		throw new UnsupportedOperationException("Unsupported query filter " + DOMUtil.printDom(filter));

	}

	private static AndFilter createAndFilter(PrismContainerDefinition pcd, Node filter) throws SchemaException {
		List<ObjectFilter> objectFilters = new ArrayList<ObjectFilter>();
		for (Element node : DOMUtil.listChildElements(filter)) {
			ObjectFilter objectFilter = parseFilter(pcd, node);
			objectFilters.add(objectFilter);
		}

		return AndFilter.createAnd(objectFilters);
	}

	private static OrFilter createOrFilter(PrismContainerDefinition pcd, Node filter) throws SchemaException {
		List<ObjectFilter> objectFilters = new ArrayList<ObjectFilter>();
		for (Element node : DOMUtil.listChildElements(filter)) {
			ObjectFilter objectFilter = parseFilter(pcd, node);
			objectFilters.add(objectFilter);
		}
		return OrFilter.createOr(objectFilters);
	}

	private static NotFilter createNotFilter(PrismContainerDefinition pcd, Node filter) throws SchemaException {
//		NodeList filters = filter.getChildNodes();
		List<Element> filters = DOMUtil.listChildElements(filter);

		if (filters.size() < 1) {
			throw new SchemaException("NOT filter does not contain any values specified");
		}

		if (filters.size() > 1) {
			throw new SchemaException(
					"NOT filter can have only one value specified. For more value use OR/AND filter as a parent.");
		}

		ObjectFilter objectFilter = parseFilter(pcd, filters.get(0));
		return NotFilter.createNot(objectFilter);
	}

	private static <T> EqualsFilter<PrismPropertyDefinition<T>> createEqualFilter(PrismContainerDefinition pcd, Node filter) throws SchemaException {
		
		ItemPath path = getPath((Element) filter);

		if (path == null || path.isEmpty()){
		throw new SchemaException("Could not convert query, because query does not contain property path.");	
		}
		
		QName matchingRule = determineMatchingRule((Element) filter);
		
//		String matchingRule = null;
//		Element matching = DOMUtil.getChildElement((Element) filter, SchemaConstantsGenerated.Q_MATCHING);
//		if (matching != null){
//			if (!(matching.getTextContent() instanceof String)){
//				throw new SchemaException("Matching type must be string. Fix your query definition");
//		}
//			 matchingRule = matching.getTextContent();
//		}
		
		
		List<Element> values = getValues(filter);
		
		if (values == null || values.isEmpty()){
			Element expression = DOMUtil.findElementRecursive((Element) filter, SchemaConstantsGenerated.C_EXPRESSION);
			if (expression == null){
				expression = DOMUtil.findElementRecursive((Element) filter, SchemaConstantsGenerated.C_VALUE_EXPRESSION);
			}
			PrismPropertyDefinition itemDef = pcd.findPropertyDefinition(path);
			return EqualsFilter.createEqual(path, itemDef, matchingRule, expression);
		}
		
		if (path.last() == null){
			throw new SchemaException("Cannot convert query, becasue query does not contian property path.");
		}
		QName propertyName = ItemPath.getName(path.last());
		ItemPath parentPath = path.allExceptLast();
		if (parentPath.isEmpty()){
			parentPath = null;
		}
		
		PrismProperty item = getItem(values, pcd, parentPath, propertyName, false);
		ItemDefinition itemDef = item.getDefinition();
		if (itemDef == null) {
			throw new SchemaException("Item definition for property " + item.getElementName() + " in container definition " + pcd
					+ " not found.");
		}

		
		if (item.getValues().size() < 1 ) {
			throw new IllegalStateException("No values to search specified for item " + itemDef);
		}

		if (itemDef.isSingleValue()) {
			if (item.getValues().size() > 1) {
				throw new IllegalStateException("Single value property "+itemDef.getName()+"should have specified only one value.");
			}
		}
		return EqualsFilter.createEqual(path, item, matchingRule);
	}
	
	private static QName determineMatchingRule(Element filterType) throws SchemaException{
		String matchingRule = null;
		Element matching = DOMUtil.getChildElement(filterType, SchemaConstantsGenerated.Q_MATCHING);
		if (matching != null){
			if (!(matching.getTextContent() instanceof String)){
				throw new SchemaException("Matching type must be string. Fix your query definition");
		}
			 matchingRule = matching.getTextContent();
		}
		
		if (StringUtils.isNotBlank(matchingRule)){
			return new QName(PrismConstants.NS_MATCHING_RULE, matchingRule);
		}
		
		return null;
	}
	
	private static RefFilter createRefFilter(PrismContainerDefinition pcd, Node filter) throws SchemaException{
		ItemPath path = getPath((Element) filter);
		
		if (path == null || path.isEmpty()){
			throw new SchemaException("Cannot convert query, becasue query does not contian property path.");
		}
		
		List<Element> values = DOMUtil.listChildElements(filter);
		
		if (path.last() == null){
			throw new SchemaException("Cannot convert query, becasue query does not contian property path.");
		}
		
		QName propertyName = ItemPath.getName(path.last());
		ItemPath parentPath = path.allExceptLast();
		if (parentPath.isEmpty()){
			parentPath = null;
		}
		
		PrismReference item = getItem(values, pcd, parentPath, propertyName, true);
		ItemDefinition itemDef = item.getDefinition();
		if (itemDef == null) {
			throw new SchemaException("Item definition for property " + item.getElementName() + " in container definition " + pcd
					+ " not found.");
		}

		Element expression = DOMUtil.findElementRecursive((Element) filter, SchemaConstantsGenerated.C_EXPRESSION);
		
		if (item.getValues().size() < 1 && expression == null) {
			throw new IllegalStateException("No values to search specified for item " + itemDef);
		}

		if (expression != null) {
			return RefFilter.createReferenceEqual(path, item, expression);
		} 
		return RefFilter.createReferenceEqual(path, item);
	}

	private static <I extends Item> I getItem(List<Element> values, PrismContainerDefinition pcd,
			ItemPath path, QName propertyName, boolean reference) throws SchemaException {
		
		if (propertyName ==  null){
			throw new SchemaException("No property name in the search query specified.");
		}

		if (path != null) {
			pcd = pcd.findContainerDefinition(path);
		}
		Collection<I> items = pcd.getPrismContext().getPrismDomProcessor().parseContainerItems(pcd, values, propertyName, reference);

		if (items.size() > 1) {
			throw new SchemaException("Expected presence of a single item (path " + path
					+ ") in a object modification, but found " + items.size() + " instead");
		}
		if (items.size() < 1) {
			throw new SchemaException("Expected presence of a value (path " + path
					+ ") in a object modification, but found nothing");
		}
		return  items.iterator().next();
		
	}

	private static ItemPath getPath(Element filter) {
		Element path = DOMUtil.getChildElement((Element) filter, SchemaConstantsGenerated.Q_PATH);
		XPathHolder xpath = new XPathHolder((Element) path);
		return xpath.toItemPath();
	}

	private static List<Element> getValues(Node filter) {
		return DOMUtil.getChildElements((Element) filter, SchemaConstantsGenerated.Q_VALUE);
//		return DOMUtil.listChildElements(value);
	}

	private static SubstringFilter createSubstringFilter(PrismContainerDefinition pcd, Node filter)
			throws SchemaException {

		ItemPath path = getPath((Element) filter);
		if (path == null || path.isEmpty()){
			throw new SchemaException("Cannot convert query, becasue query does not contian property path.");
		}
		
		Element matching = DOMUtil.getChildElement((Element) filter, SchemaConstantsGenerated.Q_MATCHING);
		
		QName matchingRule = determineMatchingRule((Element) filter); 
		
		List<Element> values = getValues(filter);

		if (path.last() == null){
			throw new SchemaException("Cannot convert query, becasue query does not contian property path.");
		}
		QName propertyName = ItemPath.getName(path.last());
		ItemPath parentPath = path.allExceptLast();
		if (parentPath.isEmpty()){
			parentPath = null;
		}
		
		if (values.size() > 1) {
			throw new SchemaException("More than one value specified in substring filter.");
		}

		if (values.size() < 1) {
			throw new SchemaException("No value specified in substring filter.");
		}

		PrismProperty item = getItem(values, pcd, parentPath, propertyName, false);
//		ItemDefinition itemDef = item.getDefinition();

		String substring = values.get(0).getTextContent();

		return SubstringFilter.createSubstring(path, item.getDefinition(), matchingRule, substring);
	}

	private static OrgFilter createOrgFilter(PrismContainerDefinition pcd, Node filter) throws SchemaException {

		Element orgRef = DOMUtil.getChildElement((Element) filter, SchemaConstantsGenerated.Q_ORG_REF);

		if (orgRef == null) {
			throw new SchemaException("No organization refenrence defined in the search query.");
		}

		String orgOid = orgRef.getAttribute("oid");

		if (orgOid == null || StringUtils.isBlank(orgOid)) {
			throw new SchemaException("No oid attribute defined in the organization reference element.");
		}

		Element minDepth = DOMUtil.getChildElement((Element) filter, SchemaConstantsGenerated.Q_MIN_DEPTH);

		Integer min = null;
		if (minDepth != null) {
			min = XsdTypeMapper.multiplicityToInteger(minDepth.getTextContent());
		}

		Element maxDepth = DOMUtil.getChildElement((Element) filter, SchemaConstantsGenerated.Q_MAX_DEPTH);
		Integer max = null;
		if (maxDepth != null) {
			max = XsdTypeMapper.multiplicityToInteger(maxDepth.getTextContent());
		}

		return OrgFilter.createOrg(orgOid, min, max);
	}

}
