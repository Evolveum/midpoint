/*
 * Copyright (c) 2010-2014 Evolveum
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
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.query.ExpressionWrapper;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

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
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.parser.util.XNodeProcessorUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.prism.query.OrgFilter.Scope;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class QueryConvertor {
	
	private static final Trace LOGGER = TraceManager.getTrace(QueryConvertor.class);
	
	public static final String NS_QUERY = "http://prism.evolveum.com/xml/ns/public/query-3";
	public static final QName FILTER_ELEMENT_NAME = new QName(NS_QUERY, "filter");

	public static QName KEY_FILTER = new QName(NS_QUERY, "filter");
	public static QName KEY_PAGING = new QName(NS_QUERY, "paging");
	public static QName KEY_CONDITION = new QName(NS_QUERY, "condition");
	
	public static final QName KEY_FILTER_AND = new QName(NS_QUERY, "and");
	public static final QName KEY_FILTER_OR = new QName(NS_QUERY, "or");
	public static final QName KEY_FILTER_NOT = new QName(NS_QUERY, "not");
	public static final QName KEY_FILTER_EQUAL = new QName(NS_QUERY, "equal");
	public static final QName KEY_FILTER_REF = new QName(NS_QUERY, "ref");
	public static final QName KEY_FILTER_SUBSTRING = new QName(NS_QUERY, "substring");
	public static final QName KEY_FILTER_ORG = new QName(NS_QUERY, "org");
	public static final QName KEY_FILTER_TYPE = new QName(NS_QUERY, "type");
	
	private static final QName KEY_FILTER_EQUAL_PATH = new QName(NS_QUERY, "path");
	private static final QName KEY_FILTER_EQUAL_MATCHING = new QName(NS_QUERY, "matching");
	private static final QName KEY_FILTER_EQUAL_VALUE = new QName(NS_QUERY, "value");

	public static final QName KEY_FILTER_SUBSTRING_ANCHOR_START = new QName(NS_QUERY, "anchorStart");
	public static final QName KEY_FILTER_SUBSTRING_ANCHOR_END = new QName(NS_QUERY, "anchorEnd");
	
	public static final QName KEY_FILTER_TYPE_TYPE = new QName(NS_QUERY, "type");
	public static final QName KEY_FILTER_TYPE_FILTER = new QName(NS_QUERY, "filter");
	
	public static final QName KEY_FILTER_ORG_REF = new QName(NS_QUERY, "orgRef");
	public static final QName KEY_FILTER_ORG_REF_OID = new QName(NS_QUERY, "oid");
    public static final QName KEY_FILTER_ORG_SCOPE = new QName(NS_QUERY, "scope");
	public static final QName KEY_FILTER_ORG_MIN_DEPTH = new QName(NS_QUERY, "minDepth");
	public static final QName KEY_FILTER_ORG_MAX_DEPTH = new QName(NS_QUERY, "maxDepth");
	
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
        Validate.notNull(prismContext);
		MapXNode xmap = toMap(xnode);
		return parseFilterContainer(xmap, null, prismContext);
	}
	
	public static <O extends Objectable> ObjectFilter parseFilter(MapXNode xmap, PrismObjectDefinition<O> objDef) throws SchemaException {
        Validate.notNull(objDef);
		if (xmap == null) {
			return null;
		}
		return parseFilterContainer(xmap, objDef, objDef.getPrismContext());
	}

    public static <O extends Objectable> ObjectFilter parseFilter(SearchFilterType filter, Class<O> clazz, PrismContext prismContext) throws SchemaException {
        PrismObjectDefinition<O> objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(clazz);
        if (objDef == null) {
            throw new SchemaException("Cannot find obj definition for "+clazz);
        }
        return parseFilter(filter, objDef);
    }

    public static ObjectFilter parseFilter(SearchFilterType filter, PrismObjectDefinition objDef) throws SchemaException {
        Validate.notNull(objDef);
        return parseFilter(filter.getFilterClauseXNode(objDef.getPrismContext()), objDef);
    }

    // beware, pcd may be null
	private static <C extends Containerable> ObjectFilter parseFilterContainer(MapXNode xmap, PrismContainerDefinition<C> pcd,
		    PrismContext prismContext) throws SchemaException {
        Validate.notNull(prismContext);
		Entry<QName, XNode> entry = xmap.getSingleEntryThatDoesNotMatch(SearchFilterType.F_DESCRIPTION);
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
			return parseSubstringFilter(xsubnode, pcd, prismContext);
		}

		if (QNameUtil.match(filterQName, KEY_FILTER_ORG)) {
			return parseOrgFilter(xsubnode, pcd);
		}
		
		if (QNameUtil.match(filterQName, KEY_FILTER_TYPE)) {
			return parseTypeFilter(xsubnode, pcd);
		}

		throw new UnsupportedOperationException("Unsupported query filter " + filterQName);

	}

	private static <C extends Containerable> AndFilter parseAndFilter(XNode xnode, PrismContainerDefinition<C> pcd,
			PrismContext prismContext) throws SchemaException {
		
		List<ObjectFilter> subfilters = parseLogicalFilter(xnode, pcd, prismContext);
		return AndFilter.createAnd(subfilters);
	}

	private static <C extends Containerable> List<ObjectFilter> parseLogicalFilter(XNode xnode,
			PrismContainerDefinition<C> pcd, PrismContext prismContext) throws SchemaException {
        List<ObjectFilter> subfilters = new ArrayList<ObjectFilter>();
		MapXNode xmap = toMap(xnode);
		for (Entry<QName, XNode> entry : xmap.entrySet()) {
			if (entry.getValue() instanceof ListXNode){
				Iterator<XNode> subNodes = ((ListXNode) entry.getValue()).iterator();
				while (subNodes.hasNext()){
					ObjectFilter subFilter = parseFilterContainer(subNodes.next(), entry.getKey(), pcd, prismContext);
					subfilters.add(subFilter);
				}
			} else{
				ObjectFilter subfilter = parseFilterContainer(entry.getValue(), entry.getKey(), pcd, prismContext);
				subfilters.add(subfilter);
			}
		}
		return subfilters;
	}

	private static <C extends Containerable> OrFilter parseOrFilter(XNode xnode, PrismContainerDefinition<C> pcd,
			PrismContext prismContext) throws SchemaException {
		List<ObjectFilter> subfilters = parseLogicalFilter(xnode, pcd, prismContext);
//		new ArrayList<ObjectFilter>();
//		MapXNode xmap = toMap(xnode);
//		for (Entry<QName, XNode> entry : xmap.entrySet()) {
//			ObjectFilter subfilter = parseFilterContainer(entry.getValue(), entry.getKey(), pcd, prismContext);
//			subfilters.add(subfilter);
//		}
		return OrFilter.createOr(subfilters);
	}

	private static <C extends Containerable> NotFilter parseNotFilter(XNode xnode, PrismContainerDefinition<C> pcd,
			PrismContext prismContext) throws SchemaException {
		MapXNode xmap = toMap(xnode);
		Entry<QName, XNode> entry = singleSubEntry(xmap, "not");
		ObjectFilter subfilter = parseFilterContainer(entry.getValue(), entry.getKey(), pcd, prismContext);
		return NotFilter.createNot(subfilter);
	}
	
	private static <T,C extends Containerable> EqualFilter<PrismPropertyDefinition<T>> parseEqualFilter(XNode xnode, PrismContainerDefinition<C> pcd, PrismContext prismContext) throws SchemaException{
		LOGGER.trace("Start to parse EQUALS filter");
		MapXNode xmap = toMap(xnode);
		ItemPath itemPath = getPath(xmap, prismContext);
		
		if (itemPath == null || itemPath.isEmpty()){
			throw new SchemaException("Could not convert query, because query does not contain item path.");	
		}
		
		QName matchingRule = determineMatchingRule(xmap);
		
		if (itemPath.last() == null) {
			throw new SchemaException("Cannot convert query, because query does not contain property path.");
		}
		QName itemName = ItemPath.getName(itemPath.last());
		XNode valueXnode = xmap.get(KEY_FILTER_EQUAL_VALUE);
		
		
		
		ItemDefinition itemDefinition = locateItemDefinition(valueXnode, itemPath, pcd, prismContext);
		if (itemDefinition != null){
			itemName = itemDefinition.getName();
		}
		
		if (valueXnode != null) {
			
			Item item = parseItem(valueXnode, itemName, itemDefinition, prismContext);
			return EqualFilter.createEqual(itemPath, (PrismProperty) item, matchingRule);
			
		} else {
			Entry<QName,XNode> expressionEntry = xmap.getSingleEntryThatDoesNotMatch(
					KEY_FILTER_EQUAL_VALUE, KEY_FILTER_EQUAL_MATCHING, KEY_FILTER_EQUAL_PATH);
			if (expressionEntry != null) {
                ExpressionWrapper expressionWrapper = new ExpressionWrapper();
                PrismPropertyValue expressionPropertyValue = prismContext.getXnodeProcessor().parsePrismPropertyFromGlobalXNodeValue(expressionEntry);
                expressionWrapper.setExpression(expressionPropertyValue.getValue());
                return EqualFilter.createEqual(itemPath, (PrismPropertyDefinition) itemDefinition, matchingRule, expressionWrapper);
			} else {
                return EqualFilter.createNullEqual(itemPath, (PrismPropertyDefinition) itemDefinition, matchingRule);
            }
			
		}

	}
	
	private static TypeFilter parseTypeFilter(XNode xnode, PrismContainerDefinition pcd) throws SchemaException{
		MapXNode xmap = toMap(xnode);
		QName type = xmap.getParsedPrimitiveValue(KEY_FILTER_TYPE_TYPE, DOMUtil.XSD_QNAME);
		
		XNode subXFilter = xmap.get(KEY_FILTER_TYPE_FILTER);
		ObjectFilter subFilter = null; 
		if (subXFilter != null){
			PrismContext prismContext = null;
			if (pcd != null && pcd.getPrismContext() != null){
				 prismContext = pcd.getPrismContext();
			}
			subFilter = parseFilter(subXFilter, prismContext);
		}
		
		return new TypeFilter(type, subFilter);
		
	}
	
	
	private static <C extends Containerable> RefFilter parseRefFilter(XNode xnode, PrismContainerDefinition<C> pcd) throws SchemaException{
		MapXNode xmap = toMap(xnode);
		ItemPath itemPath = getPath(xmap, pcd.getPrismContext());
		
		if (itemPath == null || itemPath.isEmpty()){
			throw new SchemaException("Cannot convert query, because query does not contain property path.");
		}
		
		if (itemPath.last() == null){
			throw new SchemaException("Cannot convert query, because query does not contain property path.");
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

		XNode valueXnode = xmap.get(KEY_FILTER_EQUAL_VALUE);
		
		Item<?> item = pcd.getPrismContext().getXnodeProcessor().parseItem(valueXnode, itemName, itemDefinition);
		PrismReference ref = (PrismReference)item;

		if (item.getValues().size() < 1 ) {
			throw new IllegalStateException("No values to search specified for item " + itemName);
		}

		ExpressionWrapper expressionWrapper = null;
        // TODO
		
		return RefFilter.createReferenceEqual(itemPath, ref, expressionWrapper);
	}

	private static <C extends Containerable> SubstringFilter parseSubstringFilter(XNode xnode, PrismContainerDefinition<C> pcd, PrismContext prismContext)
			throws SchemaException {
		MapXNode xmap = toMap(xnode);
		ItemPath itemPath = getPath(xmap, pcd.getPrismContext());

		if (itemPath == null || itemPath.isEmpty()){
			throw new SchemaException("Could not convert query, because query does not contain item path.");	
		}
		
		QName matchingRule = determineMatchingRule(xmap);
		
		if (itemPath.last() == null){
			throw new SchemaException("Cannot convert query, becasue query does not contian property path.");
		}
		QName itemName = ItemPath.getName(itemPath.last());
		
		XNode valueXnode = xmap.get(KEY_FILTER_EQUAL_VALUE);
		
		ItemDefinition itemDefinition = locateItemDefinition(valueXnode, itemPath, pcd, prismContext);
		
		Item item = parseItem(valueXnode, itemName, itemDefinition, prismContext);
		
		Boolean anchorStart = xmap.getParsedPrimitiveValue(KEY_FILTER_SUBSTRING_ANCHOR_START, DOMUtil.XSD_BOOLEAN);
		if (anchorStart == null) {
			anchorStart = false;
		}

		Boolean anchorEnd = xmap.getParsedPrimitiveValue(KEY_FILTER_SUBSTRING_ANCHOR_END, DOMUtil.XSD_BOOLEAN);
		if (anchorEnd == null) {
			anchorEnd = false;
		}

		return SubstringFilter.createSubstring(itemPath, (PrismProperty) item, matchingRule, anchorStart, anchorEnd);
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
		
		XsdTypeMapper.getTypeFromClass(Scope.class);
		
		String scopeString = xorgrefmap.getParsedPrimitiveValue(KEY_FILTER_ORG_SCOPE, DOMUtil.XSD_STRING);
		Scope scope = Scope.valueOf(scopeString);
		
//		String minDepth = xmap.getParsedPrimitiveValue(KEY_FILTER_ORG_MIN_DEPTH, DOMUtil.XSD_STRING);
//		Integer min = null;
//		if (!StringUtils.isBlank(minDepth)) {
//			min = XsdTypeMapper.multiplicityToInteger(minDepth);
//		}
//
//		String maxDepth = xmap.getParsedPrimitiveValue(KEY_FILTER_ORG_MAX_DEPTH, DOMUtil.XSD_STRING);
//		Integer max = null;
//		if (!StringUtils.isBlank(maxDepth)) {
//			max = XsdTypeMapper.multiplicityToInteger(maxDepth);
//		}

        //todo fix scope handling properly
//        OrgFilter.Scope scope;
//        if (min == null && max == null) {
//            scope = OrgFilter.Scope.SUBTREE;
//        } else if (ObjectUtils.equals(min, max) && (min != null && min.intValue() == 1)) {
//            scope = OrgFilter.Scope.ONE_LEVEL;
//        } else {
//            throw new SchemaException("Unsupported min/max (" + min + "/" + max
//                    + ") depth, can't translate it to scope SUBTREE/ONE_LEVEL");
//        }

		return OrgFilter.createOrg(orgOid, scope);
	}
	
	private static Entry<QName, XNode> singleSubEntry(MapXNode xmap, String filterName) throws SchemaException {
		if (xmap == null || xmap.isEmpty()) {
			return null;
		}
		return xmap.getSingleSubEntry("search filter "+filterName);
	}

	private static MapXNode toMap(XNode xnode) throws SchemaException {
		if (!(xnode instanceof MapXNode)) {
			throw new SchemaException("Cannot parse filter from "+xnode);
		}
		return (MapXNode)xnode;
	}
	
	private static ItemPath getPath(MapXNode xmap, PrismContext prismContext) throws SchemaException {
		XNode xnode = xmap.get(KEY_FILTER_EQUAL_PATH);
		if (xnode == null) {
			return null;
		}
		if (!(xnode instanceof PrimitiveXNode<?>)) {
			throw new SchemaException("Expected that field "+KEY_FILTER_EQUAL_PATH+" will be primitive, but it is "+xnode.getDesc());
		}
//		XNodeProcessor processor = PrismUtil.getXnodeProcessor(prismContext);
//		return processor.parseAtomicValue(xnode, new PrismPropertyDefinition<ItemPath>(ItemPathType.COMPLEX_TYPE, ItemPathType.COMPLEX_TYPE, prismContext));
//		return ipt.getItemPath();
		return xmap.getParsedPrimitiveValue(KEY_FILTER_EQUAL_PATH, ItemPath.XSD_TYPE);
//		return itemPathType.getItemPath();
	}

	private static QName determineMatchingRule(MapXNode xmap) throws SchemaException{
		String matchingRuleLocalPart = xmap.getParsedPrimitiveValue(KEY_FILTER_EQUAL_MATCHING, DOMUtil.XSD_STRING);
		if (StringUtils.isNotBlank(matchingRuleLocalPart)){
			return new QName(PrismConstants.NS_MATCHING_RULE, matchingRuleLocalPart);
		} else {
			return null;
		}
	}		
	
	private static Item parseItem(XNode valueXnode, QName itemName, ItemDefinition itemDefinition, PrismContext prismContext) throws SchemaException{
		Item<PrismValue> item;
		if (prismContext == null) {
			item = (Item)XNodeProcessor.parsePrismPropertyRaw(valueXnode, itemName, prismContext);
		} else {
			item = prismContext.getXnodeProcessor().parseItem(valueXnode, itemName, itemDefinition);
		}

		if (item.getValues().size() < 1 ) {
			throw new IllegalStateException("No values to search specified for item " + itemName);
		}
		
		return item;
	}
	
	private static <C extends Containerable> ItemDefinition locateItemDefinition(XNode valueXnode, ItemPath itemPath, PrismContainerDefinition<C> pcd, PrismContext prismContext) throws SchemaException{
//		QName itemName = ItemPath.getName(itemPath.last());     // TODO why using only the last item name? It can be in a container different from 'pcd'
		ItemDefinition itemDefinition = null;
		if (pcd != null) {
			itemDefinition = pcd.findItemDefinition(itemPath);
			if (itemDefinition == null) {
				ItemPath rest = itemPath.tail();
				QName first = ItemPath.getName(itemPath.first());
				itemDefinition = prismContext.getXnodeProcessor().locateItemDefinition(pcd, first, valueXnode);
				if (rest.isEmpty()){
					return itemDefinition;
				} else{
					if (itemDefinition != null && itemDefinition instanceof PrismContainerDefinition){
						return locateItemDefinition(valueXnode, rest, (PrismContainerDefinition) itemDefinition, prismContext);
					}
				}
				// do not throw...it will be saved as raw..
//				if (itemDefinition == null){
//				throw new SchemaException("No definition for item "+itemPath+" in "+pcd);
			}
		}
		return itemDefinition;
	}

    public static SearchFilterType createSearchFilterType(ObjectFilter filter, PrismContext prismContext) throws SchemaException {
        MapXNode xnode = serializeFilter(filter, prismContext);
        return SearchFilterType.createFromXNode(xnode);
    }

	public static MapXNode serializeFilter(ObjectFilter filter, PrismContext prismContext) throws SchemaException{
		return serializeFilter(filter, PrismUtil.getXnodeProcessor(prismContext).createSerializer());
	}
	
	public static MapXNode serializeFilter(ObjectFilter filter, XNodeSerializer xnodeSerilizer) throws SchemaException{
		if (filter == null) {
			return null;
		}
		
		if (filter instanceof AndFilter) {
			return serializeAndFilter((AndFilter) filter, xnodeSerilizer);
		}
		if (filter instanceof OrFilter) {
			return serializeOrFilter((OrFilter) filter, xnodeSerilizer);
		}
		if (filter instanceof NotFilter) {
			return serializeNotFilter((NotFilter) filter, xnodeSerilizer);
		}
		if (filter instanceof EqualFilter) {
			return serializeEqualsFilter((EqualFilter) filter, xnodeSerilizer);
		}
		if (filter instanceof RefFilter) {
			return serializeRefFilter((RefFilter) filter, xnodeSerilizer);
		}

		if (filter instanceof SubstringFilter) {
			return serializeSubstringFilter((SubstringFilter) filter, xnodeSerilizer);
		}

		if (filter instanceof OrgFilter) {
			return serializeOrgFilter((OrgFilter) filter, xnodeSerilizer);
		}
		
		if (filter instanceof TypeFilter) {
			return serializeTypeFilter((TypeFilter) filter, xnodeSerilizer);
		}

		throw new UnsupportedOperationException("Unsupported filter type: " + filter);
	}
	
	
	private static MapXNode serializeAndFilter(AndFilter filter, XNodeSerializer xnodeSerilizer) throws SchemaException{
		MapXNode map = new MapXNode();
		map.put(KEY_FILTER_AND, serializeNaryLogicalSubfilters(filter.getConditions(), xnodeSerilizer));
		return map;
	}

	private static MapXNode serializeOrFilter(OrFilter filter, XNodeSerializer xnodeSerilizer) throws SchemaException{
		MapXNode map = new MapXNode();
		map.put(KEY_FILTER_OR, serializeNaryLogicalSubfilters(filter.getConditions(), xnodeSerilizer));
		return map;	
	}
	
	private static MapXNode serializeNaryLogicalSubfilters(List<ObjectFilter> objectFilters, XNodeSerializer xnodeSerilizer) throws SchemaException{
		MapXNode filters = new MapXNode();
		for (ObjectFilter of : objectFilters) {
			MapXNode subFilter = serializeFilter(of, xnodeSerilizer);
			filters.merge(subFilter);
		}
		return filters;
	}

	private static MapXNode serializeNotFilter(NotFilter filter, XNodeSerializer xnodeSerializer) throws SchemaException{
		MapXNode map = new MapXNode();
		map.put(KEY_FILTER_NOT, serializeFilter(filter.getFilter(), xnodeSerializer));
		return map;
	}

	private static <T> MapXNode serializeEqualsFilter(EqualFilter<T> filter, XNodeSerializer xnodeSerializer) throws SchemaException{

		MapXNode map = new MapXNode();
		map.put(KEY_FILTER_EQUAL, serializeValueFilter(filter, xnodeSerializer));
		return map;
	}
	
	private static <T extends PrismValue> MapXNode serializeValueFilter(PropertyValueFilter<T> filter, XNodeSerializer xnodeSerializer) throws SchemaException {
		MapXNode map = new MapXNode();
		serializeMatchingRule(filter, map);
		
		serializePath(filter, map);

		ListXNode valuesNode = new ListXNode();
		
		List<T> values = filter.getValues();
		if (values != null) {
			for (T val : values) {
				if (val.getParent() == null) {
					val.setParent(filter);
				}
				XNode valNode = null;
				
				valNode = xnodeSerializer.serializeItemValue(val, filter.getDefinition());
				
				valuesNode.add(valNode);
			}
			
			map.put(KEY_FILTER_EQUAL_VALUE, valuesNode);
		}
		
		ExpressionWrapper xexpression = filter.getExpression();
		if (xexpression != null) {
			//map.merge(xexpression);
            //TODO serialize expression
		}
		
		return map;
	}
	
	private static MapXNode serializeRefFilter(RefFilter filter, XNodeSerializer xnodeSerializer) throws SchemaException {

		MapXNode map = new MapXNode();
		
		map.put(KEY_FILTER_REF, serializeValueFilter(filter, xnodeSerializer));
		return map;
	}

	private static <T> MapXNode serializeSubstringFilter(SubstringFilter<T> filter, XNodeSerializer xnodeSerializer) throws SchemaException{
		MapXNode map = new MapXNode();
		map.put(KEY_FILTER_SUBSTRING, serializeValueFilter(filter, xnodeSerializer));
		return map;
	}
	

	private static MapXNode serializeTypeFilter(TypeFilter filter, XNodeSerializer xnodeSerializer) throws SchemaException{
		MapXNode map = new MapXNode();
		map.put(KEY_FILTER_TYPE_TYPE, createPrimitiveXNode(filter.getType(), DOMUtil.XSD_QNAME));
		
		MapXNode subXFilter = null;
		if (filter.getFilter() != null){
			subXFilter = serializeFilter(filter.getFilter(), xnodeSerializer);
			map.put(KEY_FILTER_TYPE_FILTER, subXFilter);
		}
		
		MapXNode xtypeFilter= new MapXNode();
		xtypeFilter.put(KEY_FILTER_TYPE, map);
		return xtypeFilter;
		
	}
	
	private static MapXNode serializeOrgFilter(OrgFilter filter, XNodeSerializer xnodeSerializer) {
		// TODO
		return null;
	}

	private static void serializeMatchingRule(ValueFilter filter, MapXNode map){
		if (filter.getMatchingRule() != null){
			PrimitiveXNode<String> matchingNode = createPrimitiveXNode(filter.getMatchingRule().getLocalPart(), DOMUtil.XSD_STRING);
			map.put(KEY_FILTER_EQUAL_MATCHING, matchingNode);
		}

	}
	
	private static void serializePath(ValueFilter filter, MapXNode map) {
		if (filter.getFullPath() == null){
			throw new IllegalStateException("Cannot serialize filter " + filter +" because it does not contain path");
		}
		PrimitiveXNode<ItemPath> pathNode = createPrimitiveXNode(filter.getFullPath(), ItemPath.XSD_TYPE);
		
		map.put(KEY_FILTER_EQUAL_PATH, pathNode);
	}
	
	private static <T> XNode serializePropertyValue(PrismPropertyValue<T> value, PrismPropertyDefinition<T> definition, PrismBeanConverter beanConverter) throws SchemaException {
			QName typeQName = definition.getTypeName();
			T realValue = value.getValue();
			if (beanConverter.canProcess(typeQName)) {
				return beanConverter.marshall(realValue);
			} else {
				// primitive value
				return createPrimitiveXNode(realValue, typeQName);
			}
		}
	
	private static <T> PrimitiveXNode<T> createPrimitiveXNode(T val, QName type) {
		PrimitiveXNode<T> xprim = new PrimitiveXNode<T>();
		xprim.setValue(val);
		xprim.setTypeQName(type);
		return xprim;
	}
	

	public static void revive (ObjectFilter filter, final PrismContext prismContext) throws SchemaException {
//		Visitor visitor = new Visitor() {
//			@Override
//			public void visit(ObjectFilter filter) {
//				if (filter instanceof PropertyValueFilter<?>) {
//					try {
//						parseExpression((PropertyValueFilter<?>)filter, prismContext);
//					} catch (SchemaException e) {
//						throw new TunnelException(e);
//					}
//				}
//			}
//		};
//		try {
//			filter.accept(visitor);
//		} catch (TunnelException te) {
//			SchemaException e = (SchemaException) te.getCause();
//			throw e;
//		}
	}


}
