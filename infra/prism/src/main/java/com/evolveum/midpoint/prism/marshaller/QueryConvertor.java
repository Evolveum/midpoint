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

package com.evolveum.midpoint.prism.marshaller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.xnode.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectReferenceType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.OrgFilter.Scope;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Note that expressions are not serialized yet.
 */
public class QueryConvertor {
	
	private static final Trace LOGGER = TraceManager.getTrace(QueryConvertor.class);
	
	public static final String NS_QUERY = "http://prism.evolveum.com/xml/ns/public/query-3";

	// TODO removed unused constants eventually
	public static final QName FILTER_ELEMENT_NAME = new QName(NS_QUERY, "filter");
	public static QName KEY_FILTER = new QName(NS_QUERY, "filter");
	public static QName KEY_PAGING = new QName(NS_QUERY, "paging");
	public static QName KEY_CONDITION = new QName(NS_QUERY, "condition");

	// please keep the order of filter clause symbols synchronized with query-3.xsd

	private static final QName CLAUSE_ALL = new QName(NS_QUERY, "all");
	private static final QName CLAUSE_NONE = new QName(NS_QUERY, "none");
	private static final QName CLAUSE_UNDEFINED = new QName(NS_QUERY, "undefined");

	private static final QName CLAUSE_EQUAL = new QName(NS_QUERY, "equal");
	private static final QName CLAUSE_GREATER = new QName(NS_QUERY, "greater");
	private static final QName CLAUSE_GREATER_OR_EQUAL = new QName(NS_QUERY, "greaterOrEqual");
	private static final QName CLAUSE_LESS = new QName(NS_QUERY, "less");
	private static final QName CLAUSE_LESS_OR_EQUAL = new QName(NS_QUERY, "lessOrEqual");
	private static final QName CLAUSE_SUBSTRING = new QName(NS_QUERY, "substring");

	private static final QName CLAUSE_REF = new QName(NS_QUERY, "ref");
	private static final QName CLAUSE_ORG = new QName(NS_QUERY, "org");
	private static final QName CLAUSE_IN_OID = new QName(NS_QUERY, "inOid");
	private static final QName CLAUSE_FULL_TEXT = new QName(NS_QUERY, "fullText");

	private static final QName CLAUSE_AND = new QName(NS_QUERY, "and");
	private static final QName CLAUSE_OR = new QName(NS_QUERY, "or");
	private static final QName CLAUSE_NOT = new QName(NS_QUERY, "not");

	private static final QName CLAUSE_TYPE = new QName(NS_QUERY, "type");
	private static final QName CLAUSE_EXISTS = new QName(NS_QUERY, "exists");

	// common elements
	private static final QName ELEMENT_PATH = new QName(NS_QUERY, "path");
	private static final QName ELEMENT_MATCHING = new QName(NS_QUERY, "matching");
	private static final QName ELEMENT_VALUE = new QName(NS_QUERY, "value");
	private static final QName ELEMENT_RIGHT_HAND_SIDE_PATH = new QName(NS_QUERY, "rightHandSidePath");

	// substring
	private static final QName ELEMENT_ANCHOR_START = new QName(NS_QUERY, "anchorStart");
	private static final QName ELEMENT_ANCHOR_END = new QName(NS_QUERY, "anchorEnd");

	// org
	private static final QName ELEMENT_ORG_REF = new QName(NS_QUERY, "orgRef");
	private static final QName ELEMENT_SCOPE = new QName(NS_QUERY, "scope");
	private static final QName ELEMENT_IS_ROOT = new QName(NS_QUERY, "isRoot");

	// inOid
	private static final QName ELEMENT_OID = new QName(NS_QUERY, "oid");
	private static final QName ELEMENT_CONSIDER_OWNER = new QName(NS_QUERY, "considerOwner");

	// type and exists
	public static final QName ELEMENT_TYPE = new QName(NS_QUERY, "type");
	private static final QName ELEMENT_FILTER = new QName(NS_QUERY, "filter");

	/**
	 * Used by XNodeProcessor and similar code that does not have complete schema for the filter 
	 */
	public static ObjectFilter parseFilter(XNode xnode, PrismContext prismContext) throws SchemaException {
        Validate.notNull(prismContext);
		MapXNode xmap = toMap(xnode);
		return parseFilterInternal(xmap, null, false, prismContext);
	}
	
	public static <O extends Containerable> ObjectFilter parseFilter(MapXNode xmap, PrismContainerDefinition<O> objDef) throws SchemaException {
        Validate.notNull(objDef);
		if (xmap == null) {
			return null;
		}
		return parseFilterInternal(xmap, objDef, false, objDef.getPrismContext());
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
        return parseFilter(filter.getFilterClauseXNode(), objDef);
    }

	private static <C extends Containerable> ObjectFilter parseFilterInternal(
			@NotNull MapXNode filterXMap,
			@Nullable PrismContainerDefinition<C> pcd,
		    boolean preliminaryParsingOnly,
			@NotNull PrismContext prismContext) throws SchemaException {

        Validate.notNull(prismContext);
		Entry<QName, XNode> clauseEntry = filterXMap.getSingleEntryThatDoesNotMatch(SearchFilterType.F_DESCRIPTION);
		QName clauseQName = clauseEntry.getKey();
		XNode clauseContent = clauseEntry.getValue();
		return parseFilterInternal(clauseContent, clauseQName, pcd, preliminaryParsingOnly, prismContext);
	}

	private static <C extends Containerable> ObjectFilter parseFilterInternal(XNode clauseContent, QName clauseQName,
			PrismContainerDefinition<C> pcd, boolean preliminaryParsingOnly, PrismContext prismContext) throws SchemaException {

		// trivial filters
		if (QNameUtil.match(clauseQName, CLAUSE_ALL)) {
			return new AllFilter();
		} else if (QNameUtil.match(clauseQName, CLAUSE_NONE)) {
			return new NoneFilter();
		} else if (QNameUtil.match(clauseQName, CLAUSE_UNDEFINED)) {
			return new UndefinedFilter();
		}

		// primitive filters
		MapXNode clauseXMap = toMap(clauseContent);

		if (QNameUtil.match(clauseQName, CLAUSE_EQUAL)
				|| QNameUtil.match(clauseQName, CLAUSE_GREATER)
				|| QNameUtil.match(clauseQName, CLAUSE_GREATER_OR_EQUAL)
				|| QNameUtil.match(clauseQName, CLAUSE_LESS)
				|| QNameUtil.match(clauseQName, CLAUSE_LESS_OR_EQUAL)) {
			return parseComparisonFilter(clauseQName, clauseXMap, pcd, preliminaryParsingOnly, prismContext);
		} else if (QNameUtil.match(clauseQName, CLAUSE_SUBSTRING)) {
			return parseSubstringFilter(clauseXMap, pcd, preliminaryParsingOnly, prismContext);
		} else if (QNameUtil.match(clauseQName, CLAUSE_REF)) {
			return parseRefFilter(clauseXMap, pcd, preliminaryParsingOnly, prismContext);
		} else if (QNameUtil.match(clauseQName, CLAUSE_ORG)) {
			return parseOrgFilter(clauseXMap, pcd, preliminaryParsingOnly, prismContext);
		} else if (QNameUtil.match(clauseQName, CLAUSE_IN_OID)) {
			return parseInOidFilter(clauseXMap, pcd, preliminaryParsingOnly, prismContext);
		} else if (QNameUtil.match(clauseQName, CLAUSE_FULL_TEXT)) {
			return parseFullTextFilter(clauseXMap, pcd, preliminaryParsingOnly, prismContext);
		}

		// logical filters

		if (QNameUtil.match(clauseQName, CLAUSE_AND)) {
			return parseAndFilter(clauseXMap, pcd, preliminaryParsingOnly, prismContext);
		} else if (QNameUtil.match(clauseQName, CLAUSE_OR)) {
			return parseOrFilter(clauseXMap, pcd, preliminaryParsingOnly, prismContext);
		} else if (QNameUtil.match(clauseQName, CLAUSE_NOT)) {
			return parseNotFilter(clauseXMap, pcd, preliminaryParsingOnly, prismContext);
		}
		
		// other complex filters

		if (QNameUtil.match(clauseQName, CLAUSE_TYPE)) {
			return parseTypeFilter(clauseXMap, pcd, preliminaryParsingOnly, prismContext);
		} else if (QNameUtil.match(clauseQName, CLAUSE_EXISTS)) {
			return parseExistsFilter(clauseXMap, pcd, preliminaryParsingOnly, prismContext);
		}

		throw new UnsupportedOperationException("Unsupported query filter " + clauseQName);

	}

	private static <C extends Containerable> AndFilter parseAndFilter(MapXNode clauseXMap, PrismContainerDefinition<C> pcd,
                                                                      boolean preliminaryParsingOnly, PrismContext prismContext) throws SchemaException {
		List<ObjectFilter> subfilters = parseLogicalFilter(clauseXMap, pcd, preliminaryParsingOnly, prismContext);
        if (preliminaryParsingOnly) {
            return null;
        } else {
		    return AndFilter.createAnd(subfilters);
        }
	}

	private static <C extends Containerable> List<ObjectFilter> parseLogicalFilter(MapXNode clauseXMap,
                                                                                   PrismContainerDefinition<C> pcd, boolean preliminaryParsingOnly, PrismContext prismContext) throws SchemaException {
        List<ObjectFilter> subfilters = new ArrayList<ObjectFilter>();
		for (Entry<QName, XNode> entry : clauseXMap.entrySet()) {
			if (entry.getValue() instanceof ListXNode){
				Iterator<XNode> subNodes = ((ListXNode) entry.getValue()).iterator();
				while (subNodes.hasNext()){
					ObjectFilter subFilter = parseFilterInternal(subNodes.next(), entry.getKey(), pcd, preliminaryParsingOnly, prismContext);
					if (!preliminaryParsingOnly) {
                        subfilters.add(subFilter);
                    }
				}
			} else{
				ObjectFilter subfilter = parseFilterInternal(entry.getValue(), entry.getKey(), pcd, preliminaryParsingOnly, prismContext);
                if (!preliminaryParsingOnly) {
				    subfilters.add(subfilter);
                }
			}
		}
		return subfilters;
	}

	private static <C extends Containerable> OrFilter parseOrFilter(MapXNode clauseXMap, PrismContainerDefinition<C> pcd,
                                                                    boolean preliminaryParsingOnly, PrismContext prismContext) throws SchemaException {
		List<ObjectFilter> subfilters = parseLogicalFilter(clauseXMap, pcd, preliminaryParsingOnly, prismContext);
        if (preliminaryParsingOnly) {
            return null;
        } else {
		    return OrFilter.createOr(subfilters);
        }
	}

	private static <C extends Containerable> NotFilter parseNotFilter(MapXNode clauseXMap, PrismContainerDefinition<C> pcd,
                                                                      boolean preliminaryParsingOnly, PrismContext prismContext) throws SchemaException {
		Entry<QName, XNode> entry = singleSubEntry(clauseXMap, "not");
		ObjectFilter subfilter = parseFilterInternal(entry.getValue(), entry.getKey(), pcd, preliminaryParsingOnly, prismContext);
        if (preliminaryParsingOnly) {
            return null;
        } else {
		    return NotFilter.createNot(subfilter);
        }
	}
	
	private static <T,C extends Containerable> ObjectFilter parseComparisonFilter(QName clauseQName, MapXNode clauseXMap,
			PrismContainerDefinition<C> pcd, boolean preliminaryParsingOnly, PrismContext prismContext) throws SchemaException {

		boolean isEq = QNameUtil.match(clauseQName, CLAUSE_EQUAL);
		boolean isGt = QNameUtil.match(clauseQName, CLAUSE_GREATER);
		boolean isGtEq = QNameUtil.match(clauseQName, CLAUSE_GREATER_OR_EQUAL);
		boolean isLt = QNameUtil.match(clauseQName, CLAUSE_LESS);
		boolean isLtEq = QNameUtil.match(clauseQName, CLAUSE_LESS_OR_EQUAL);

		ItemPath itemPath = getPath(clauseXMap);
		if (itemPath == null || itemPath.isEmpty()){
			throw new SchemaException("Could not convert query, because query does not contain item path.");	
		}
		QName itemName = ItemPath.getName(itemPath.last());

		QName matchingRule = getMatchingRule(clauseXMap);

		XNode valueXnode = clauseXMap.get(ELEMENT_VALUE);
		ItemPath rightSidePath = getPath(clauseXMap, ELEMENT_RIGHT_HAND_SIDE_PATH);

		ItemDefinition itemDefinition = locateItemDefinition(valueXnode, itemPath, pcd, prismContext);
		if (itemDefinition != null) {
			itemName = itemDefinition.getName();
		}
		
		if (valueXnode != null) {
			if (preliminaryParsingOnly) {
				return null;
            } else {
				RootXNode valueRoot = new RootXNode(ELEMENT_VALUE, valueXnode);
				Item item = parseItem(valueRoot, itemName, itemDefinition, prismContext);
				if (!isEq && item.getValues().size() != 1) {
					throw new SchemaException("Expected exactly one value, got " + item.getValues().size() + " instead");
				}
				if (isEq) {
					List<PrismPropertyValue<T>> values = item.getValues();
					PrismValue.clearParent(values);
					return EqualFilter.createEqual(itemPath,
							(PrismPropertyDefinition<T>)itemDefinition, matchingRule, prismContext, values);
				}
				PrismPropertyValue<T> propertyValue = (PrismPropertyValue<T>) item.getValue(0);
				propertyValue.clearParent();
				if (isGt || isGtEq) {
					return GreaterFilter.createGreater(itemPath, (PrismPropertyDefinition<T>) itemDefinition, isGtEq, prismContext, propertyValue);
				} else {
					return LessFilter.createLess(itemPath, (PrismPropertyDefinition<T>) itemDefinition, prismContext,
							propertyValue, isLtEq);
				}
            }
		} else if (rightSidePath != null) {
			if (preliminaryParsingOnly) {
				return null;
			} else {
				ItemDefinition rightSideDefinition = pcd != null ? pcd.findItemDefinition(rightSidePath) : null;
				if (isEq) {
					return EqualFilter.createEqual(itemPath, (PrismPropertyDefinition) itemDefinition, matchingRule, rightSidePath, rightSideDefinition);
				} else if (isGt || isGtEq) {
					return GreaterFilter.createGreater(itemPath, (PrismPropertyDefinition) itemDefinition, rightSidePath,
							rightSideDefinition, isGtEq);
				} else {
					return LessFilter.createLess(itemPath, (PrismPropertyDefinition) itemDefinition, rightSidePath,
							rightSideDefinition, isLtEq);
				}
			}
		} else {
			Entry<QName,XNode> expressionEntry = clauseXMap.getSingleEntryThatDoesNotMatch(
					ELEMENT_VALUE, ELEMENT_MATCHING, ELEMENT_PATH);
			if (expressionEntry != null) {
                if (preliminaryParsingOnly) {
                    return null;
                } else {
					RootXNode expressionRoot = clauseXMap.getEntryAsRoot(expressionEntry.getKey());
					PrismPropertyValue expressionPropertyValue = prismContext.parserFor(expressionRoot).parseItemValue();
                    ExpressionWrapper expressionWrapper = new ExpressionWrapper(expressionEntry.getKey(), expressionPropertyValue.getValue());
					if (isEq) {
						return EqualFilter.createEqual(itemPath, (PrismPropertyDefinition<T>) itemDefinition, matchingRule, expressionWrapper);
					} else if (isGt || isGtEq) {
						return GreaterFilter.createGreater(itemPath, (PrismPropertyDefinition<T>) itemDefinition,
								expressionWrapper, isGtEq);
					} else {
						return LessFilter.createLess(itemPath, (PrismPropertyDefinition<T>) itemDefinition, expressionWrapper,
								isLtEq);
					}
                }
			} else {
				if (!isEq) {
					throw new SchemaException("Comparison filter (greater, less) requires at least one value expression.");
				}
                if (preliminaryParsingOnly) {
                    return null;
                } else {
					return EqualFilter.createEqual(itemPath, (PrismPropertyDefinition<T>) itemDefinition, matchingRule);
                }
            }
		}
	}
	
	private static InOidFilter parseInOidFilter(MapXNode clauseXMap, PrismContainerDefinition pcd, boolean preliminaryParsingOnly, PrismContext prismContext) throws SchemaException{
		boolean considerOwner = Boolean.TRUE.equals(clauseXMap.getParsedPrimitiveValue(ELEMENT_CONSIDER_OWNER, DOMUtil.XSD_BOOLEAN));
		XNode valueXnode = clauseXMap.get(ELEMENT_VALUE);
		if (valueXnode != null) {
			List<String> oids = getStringValues(valueXnode);
			return InOidFilter.createInOid(considerOwner, oids);
		} else {
			ExpressionWrapper expression = parseExpression(clauseXMap, prismContext);
			if (expression != null) {
				return InOidFilter.createInOid(considerOwner, expression);
			} else {
				throw new SchemaException("InOid filter with no values nor expression");
			}
		}
	}
	
	private static FullTextFilter parseFullTextFilter(MapXNode clauseXMap, PrismContainerDefinition pcd,
			boolean preliminaryParsingOnly, PrismContext prismContext) throws SchemaException{
		XNode valueXnode = clauseXMap.get(ELEMENT_VALUE);
		if (valueXnode != null) {
			List<String> values = getStringValues(valueXnode);
			return FullTextFilter.createFullText(values);
		} else {
			ExpressionWrapper expression = parseExpression(clauseXMap, prismContext);
			if (expression != null) {
				return FullTextFilter.createFullText(expression);
			} else {
				throw new SchemaException("FullText filter with no values nor expression");
			}
		}
	}

	@NotNull
	private static List<String> getStringValues(
			XNode valueXnode) throws SchemaException {
		List<String> values = new ArrayList<>();
		if (valueXnode instanceof ListXNode) {
			for (XNode subnode : (ListXNode) valueXnode) {
				if (subnode instanceof PrimitiveXNode) {
					values.add(((PrimitiveXNode<String>) subnode).getParsedValue(DOMUtil.XSD_STRING, String.class));
				} else {
					throw new SchemaException("The value was expected to be present as primitive XNode, instead it is: " + subnode);
				}
			}
		} else if (valueXnode instanceof PrimitiveXNode) {
			values.add(((PrimitiveXNode<String>) valueXnode).getParsedValue(DOMUtil.XSD_STRING, String.class));
		} else {
			throw new SchemaException("The value was expected to be present as primitive or list XNode, instead it is: " + valueXnode);
		}
		return values;
	}

	private static TypeFilter parseTypeFilter(MapXNode clauseXMap, PrismContainerDefinition pcd, boolean preliminaryParsingOnly, PrismContext prismContext) throws SchemaException{
		QName type = clauseXMap.getParsedPrimitiveValue(ELEMENT_TYPE, DOMUtil.XSD_QNAME);
		XNode subXFilter = clauseXMap.get(ELEMENT_FILTER);
		PrismObjectDefinition def = prismContext.getSchemaRegistry().findObjectDefinitionByType(type);
		ObjectFilter subFilter = null;
		if (subXFilter != null && subXFilter instanceof MapXNode) {
			subFilter = parseFilterInternal((MapXNode) subXFilter, def, preliminaryParsingOnly, prismContext);
		}
        if (preliminaryParsingOnly) {
            return null;
        } else {
			// to report exception only when the filter is really to be used
			checkExtraElements(clauseXMap, ELEMENT_TYPE, ELEMENT_FILTER);
		    return new TypeFilter(type, subFilter);
        }
	}

	private static ExistsFilter parseExistsFilter(MapXNode clauseXMap, PrismContainerDefinition pcd, boolean preliminaryParsingOnly, PrismContext prismContext) throws SchemaException{
		ItemPath path = getPath(clauseXMap);
		XNode subXFilter = clauseXMap.get(ELEMENT_FILTER);
		ObjectFilter subFilter = null;
		PrismContainerDefinition subPcd = pcd != null ? pcd.findContainerDefinition(path) : null;
		if (subXFilter != null && subXFilter instanceof MapXNode) {
			subFilter = parseFilterInternal((MapXNode) subXFilter, subPcd, preliminaryParsingOnly, prismContext);
		}
        if (preliminaryParsingOnly) {
            return null;
        } else {
			// to report exception only when the filter is really to be used
			checkExtraElements(clauseXMap, ELEMENT_PATH, ELEMENT_FILTER);
		    return ExistsFilter.createExists(path, pcd, subFilter);
        }
	}

	private static void checkExtraElements(MapXNode clauseXMap, QName... expected) throws SchemaException {
		List<QName> expectedList = Arrays.asList(expected);
		for (Entry<QName, XNode> entry : clauseXMap.entrySet()) {
			if (!QNameUtil.contains(expectedList, entry.getKey())) {
				throw new SchemaException("Unexpected item " + entry.getKey() + ":" +
						(entry.getValue() != null ? entry.getValue().debugDump() : "null"));
			}
		}
	}

	private static <C extends Containerable> RefFilter parseRefFilter(MapXNode clauseXMap, PrismContainerDefinition<C> pcd, boolean preliminaryParsingOnly, PrismContext prismContext) throws SchemaException{
		ItemPath itemPath = getPath(clauseXMap);
		
		if (itemPath == null || itemPath.isEmpty()){
			throw new SchemaException("Cannot convert query, because query does not contain property path.");
		}
		QName itemName = ItemPath.getName(itemPath.last());

		ItemDefinition itemDefinition = null;
		if (pcd != null) {
			itemDefinition = pcd.findItemDefinition(itemPath);
			if (itemDefinition == null && !preliminaryParsingOnly) {
				throw new SchemaException("No definition for item "+itemPath+" in "+pcd);
			}
		}

		// The reference definition in prism data might be single-valued. However, filters allow to look for multiple values.
		if (itemDefinition != null && itemDefinition.getMaxOccurs() != -1) {
			itemDefinition = itemDefinition.clone();
			itemDefinition.setMaxOccurs(-1);
		}

		XNode valueXnode = clauseXMap.get(ELEMENT_VALUE);
		if (valueXnode != null) {
			if (preliminaryParsingOnly) {
				return null;
			}
			RootXNode valueRoot = new RootXNode(ELEMENT_VALUE, valueXnode);
			Item<?,?> item = prismContext.parserFor(valueRoot)
					.name(itemName)
					.definition(itemDefinition)
					.context(ParsingContext.allowMissingRefTypes())
					.parseItem();
			if (!(item instanceof PrismReference)) {
				throw new IllegalStateException("Expected PrismReference, got " + item);
			}
			PrismReference ref = (PrismReference)item;
			if (item.getValues().size() < 1) {
				throw new IllegalStateException("No values to search specified for item " + itemName);
			}
			return RefFilter.createReferenceEqual(itemPath, (PrismReferenceDefinition) itemDefinition, PrismValue.cloneCollection(ref.getValues()));
		} else {
			ExpressionWrapper expressionWrapper = parseExpression(clauseXMap, prismContext);
			if (expressionWrapper != null) {
				if (preliminaryParsingOnly) {
					return null;
				} else {
					return RefFilter.createReferenceEqual(itemPath,
							(PrismReferenceDefinition) itemDefinition, expressionWrapper);
				}
			} else {
				if (preliminaryParsingOnly) {
					return null;
				} else {
					return RefFilter.createReferenceEqual(itemPath,
							(PrismReferenceDefinition) itemDefinition, (ExpressionWrapper) null);
				}
			}
		}
	}
	
	private static ExpressionWrapper parseExpression(MapXNode xmap, PrismContext prismContext) throws SchemaException {
		Entry<QName, XNode> expressionEntry = xmap.getSingleEntryThatDoesNotMatch(
				ELEMENT_VALUE, ELEMENT_MATCHING, ELEMENT_ANCHOR_START, ELEMENT_ANCHOR_END, ELEMENT_PATH);
		return PrismUtil.parseExpression(expressionEntry, prismContext);
	}

	private static <C extends Containerable> SubstringFilter parseSubstringFilter(MapXNode clauseXMap, PrismContainerDefinition<C> pcd, boolean preliminaryParsingOnly, PrismContext prismContext)
			throws SchemaException {
		ItemPath itemPath = getPath(clauseXMap);

		if (itemPath == null || itemPath.isEmpty()) {
			throw new SchemaException("Could not convert query, because query does not contain item path.");
		}
		QName itemName = ItemPath.getName(itemPath.last());
		QName matchingRule = getMatchingRule(clauseXMap);

		Boolean anchorStart = clauseXMap.getParsedPrimitiveValue(ELEMENT_ANCHOR_START, DOMUtil.XSD_BOOLEAN);
		if (anchorStart == null) {
			anchorStart = false;
		}

		Boolean anchorEnd = clauseXMap.getParsedPrimitiveValue(ELEMENT_ANCHOR_END, DOMUtil.XSD_BOOLEAN);
		if (anchorEnd == null) {
			anchorEnd = false;
		}

		XNode valueXnode = clauseXMap.get(ELEMENT_VALUE);
		ItemDefinition itemDefinition = locateItemDefinition(valueXnode, itemPath, pcd, prismContext);

		if (valueXnode != null) {

			Item item = parseItem(new RootXNode(ELEMENT_VALUE, valueXnode), itemName, itemDefinition, prismContext);

			if (preliminaryParsingOnly) {
				return null;
			} else {
				List values = item.getValues();
				Object realValue;
				if (values == null || values.isEmpty()) {
					realValue = null; // TODO throw an exception?
				} else if (values.size() > 1) {
					throw new IllegalArgumentException("Expected at most 1 value, got " + values);
				} else {
					realValue = ((PrismPropertyValue) values.get(0)).getValue();
				}
				return SubstringFilter.createSubstring(itemPath, (PrismPropertyDefinition) itemDefinition, prismContext,
						matchingRule, realValue, anchorStart, anchorEnd);

			}
		} else {
			ExpressionWrapper expressionWrapper = parseExpression(clauseXMap, prismContext);
			if (expressionWrapper != null) {
				if (preliminaryParsingOnly) {
					return null;
				} else {
					return SubstringFilter.createSubstring(itemPath, (PrismPropertyDefinition) itemDefinition, prismContext, matchingRule, expressionWrapper, anchorStart, anchorEnd);
				}
			} else {
				if (preliminaryParsingOnly) {
					return null;
				} else {
					return SubstringFilter.createSubstring(itemPath, (PrismPropertyDefinition) itemDefinition, prismContext, matchingRule,
							(ExpressionWrapper) null, anchorStart, anchorEnd);
				}
			}
		}
	}

	private static <C extends Containerable> OrgFilter parseOrgFilter(MapXNode clauseXMap, PrismContainerDefinition<C> pcd, boolean preliminaryParsingOnly, PrismContext prismContext) throws SchemaException {
		if (Boolean.TRUE.equals(clauseXMap.getParsedPrimitiveValue(ELEMENT_IS_ROOT, DOMUtil.XSD_BOOLEAN))) {
			// TODO check if other content is present
			if (preliminaryParsingOnly) {
				return null;
			} else {
				return OrgFilter.createRootOrg();
			}
		}
		
		XNode xorgrefnode = clauseXMap.get(ELEMENT_ORG_REF);
		if (xorgrefnode == null) {
			throw new SchemaException("No organization reference defined in the search query.");
		}
		MapXNode xorgrefmap = toMap(xorgrefnode);
		String orgOid = xorgrefmap.getParsedPrimitiveValue(ELEMENT_OID, DOMUtil.XSD_STRING);
		if (orgOid == null || StringUtils.isBlank(orgOid)) {
			throw new SchemaException("No oid attribute defined in the organization reference element.");
		}
		
		String scopeString = xorgrefmap.getParsedPrimitiveValue(ELEMENT_SCOPE, DOMUtil.XSD_STRING);		// original (in my opinion incorrect) place
		if (scopeString == null) {
			scopeString = clauseXMap.getParsedPrimitiveValue(ELEMENT_SCOPE, DOMUtil.XSD_STRING);				// here it is placed by the serializer
		}
		Scope scope = scopeString != null ? Scope.valueOf(scopeString) : null;
		
        if (preliminaryParsingOnly) {
            return null;
        } else {
		    return OrgFilter.createOrg(orgOid, scope);
        }
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

	private static PrimitiveXNode toPrimitive(XNode xnode, XNode context) throws SchemaException {
		if (!(xnode instanceof PrimitiveXNode)) {
			throw new SchemaException("Cannot parse filter from "+context+ ": This should be a primitive: "+xnode);
		}
		return (PrimitiveXNode)xnode;
	}
	
	private static ItemPath getPath(MapXNode clauseXMap) throws SchemaException {
		return getPath(clauseXMap, ELEMENT_PATH);
	}

	private static ItemPath getPath(MapXNode clauseXMap, QName key) throws SchemaException {
		XNode xnode = clauseXMap.get(key);
		if (xnode == null) {
			return null;
		}
		if (!(xnode instanceof PrimitiveXNode<?>)) {
			throw new SchemaException("Expected that field "+key+" will be primitive, but it is "+xnode.getDesc());
		}
		ItemPathType itemPathType = clauseXMap.getParsedPrimitiveValue(key, ItemPathType.COMPLEX_TYPE);
		return itemPathType != null ? itemPathType.getItemPath() : null;
	}

	private static QName getMatchingRule(MapXNode xmap) throws SchemaException{
		String matchingRuleString = xmap.getParsedPrimitiveValue(ELEMENT_MATCHING, DOMUtil.XSD_STRING);
		if (StringUtils.isNotBlank(matchingRuleString)){
			if (QNameUtil.isUri(matchingRuleString)) {
				return QNameUtil.uriToQName(matchingRuleString);
			} else {
				return new QName(PrismConstants.NS_MATCHING_RULE, matchingRuleString);
			}
		} else {
			return null;
		}
	}		
	
	private static Item parseItem(RootXNode root, QName itemName, ItemDefinition itemDefinition, PrismContext prismContext) throws SchemaException{
		Item<?,?> item;
		if (prismContext == null) {
			item = (Item) PrismProperty.createRaw(root.getSubnode(), itemName, null);
		} else {
			item = prismContext.parserFor(root)
					.name(itemName)
					.definition(itemDefinition)
					.context(ParsingContext.allowMissingRefTypes())
					.parseItem();
		}

		if (item.getValues().size() < 1 ) {
			throw new IllegalStateException("No values to search specified for item " + itemName);
		}
		
		return item;
	}
	
	private static <C extends Containerable> ItemDefinition locateItemDefinition(XNode valueXnode, ItemPath itemPath, PrismContainerDefinition<C> pcd, PrismContext prismContext) throws SchemaException{
		ItemDefinition itemDefinition = null;
		if (pcd != null) {
			itemDefinition = pcd.findItemDefinition(itemPath);
			if (itemDefinition == null) {
				ItemPath rest = itemPath.tail();
				QName first = ItemPath.getName(itemPath.first());
				itemDefinition = ((PrismContextImpl) prismContext).getPrismUnmarshaller().locateItemDefinition(pcd, first, valueXnode);
				if (rest.isEmpty()) {
					return itemDefinition;
				} else{
					if (itemDefinition != null && itemDefinition instanceof PrismContainerDefinition){
						return locateItemDefinition(valueXnode, rest, (PrismContainerDefinition) itemDefinition, prismContext);
					}
				}
			}
		}
		return itemDefinition;
	}

    public static SearchFilterType createSearchFilterType(ObjectFilter filter, PrismContext prismContext) throws SchemaException {
        MapXNode xnode = serializeFilter(filter, prismContext);
        return SearchFilterType.createFromXNode(xnode, prismContext);
    }

	public static MapXNode serializeFilter(ObjectFilter filter, PrismContext prismContext) throws SchemaException{
		return serializeFilter(filter, prismContext.xnodeSerializer());
	}
	
	private static MapXNode serializeFilter(ObjectFilter filter, PrismSerializer<RootXNode> xnodeSerializer) throws SchemaException{
		// null or primitive filters
		if (filter == null) {
			return null;
		} else if (filter instanceof AllFilter) {
			return serializeAllFilter();
		} else if (filter instanceof NoneFilter) {
			return serializeNoneFilter();
		} else if (filter instanceof UndefinedFilter) {
			return serializeUndefinedFilter();
		} else if (filter instanceof EqualFilter
				|| filter instanceof GreaterFilter
				|| filter instanceof LessFilter) {
			return serializeComparisonFilter((PropertyValueFilter) filter, xnodeSerializer);
		} else if (filter instanceof SubstringFilter) {
			return serializeSubstringFilter((SubstringFilter) filter, xnodeSerializer);
		} else if (filter instanceof RefFilter) {
			return serializeRefFilter((RefFilter) filter, xnodeSerializer);
		} else if (filter instanceof OrgFilter) {
			return serializeOrgFilter((OrgFilter) filter, xnodeSerializer);
		} else if (filter instanceof InOidFilter) {
			return serializeInOidFilter((InOidFilter) filter, xnodeSerializer);
		} else if (filter instanceof FullTextFilter) {
			return serializeFullTextFilter((FullTextFilter) filter, xnodeSerializer);
		}

		// complex filters
		if (filter instanceof AndFilter) {
			return serializeAndFilter((AndFilter) filter, xnodeSerializer);
		} else if (filter instanceof OrFilter) {
			return serializeOrFilter((OrFilter) filter, xnodeSerializer);
		} else if (filter instanceof NotFilter) {
			return serializeNotFilter((NotFilter) filter, xnodeSerializer);
		}

		if (filter instanceof TypeFilter) {
			return serializeTypeFilter((TypeFilter) filter, xnodeSerializer);
		} else if (filter instanceof ExistsFilter) {
			return serializeExistsFilter((ExistsFilter) filter, xnodeSerializer);
		}
		
		throw new UnsupportedOperationException("Unsupported filter type: " + filter);
	}
	
	
	private static MapXNode serializeAndFilter(AndFilter filter, PrismSerializer<RootXNode> xnodeSerilizer) throws SchemaException{
		return createFilter(CLAUSE_AND, serializeNaryLogicalSubfilters(filter.getConditions(), xnodeSerilizer));
	}

	private static MapXNode serializeOrFilter(OrFilter filter, PrismSerializer<RootXNode> xnodeSerilizer) throws SchemaException{
		MapXNode map = createFilter(CLAUSE_OR, serializeNaryLogicalSubfilters(filter.getConditions(), xnodeSerilizer));
		return map;	
	}
	
	private static MapXNode serializeNaryLogicalSubfilters(List<ObjectFilter> objectFilters, PrismSerializer<RootXNode> xnodeSerilizer) throws SchemaException{
		MapXNode filters = new MapXNode();
		for (ObjectFilter of : objectFilters) {
			MapXNode subFilter = serializeFilter(of, xnodeSerilizer);
			filters.merge(subFilter);
		}
		return filters;
	}

	private static MapXNode serializeNotFilter(NotFilter filter, PrismSerializer<RootXNode> xnodeSerializer) throws SchemaException{
		return createFilter(CLAUSE_NOT, serializeFilter(filter.getFilter(), xnodeSerializer));
	}

	@NotNull
	private static MapXNode createFilter(QName clauseNot, MapXNode value) {
		MapXNode map = new MapXNode();
		map.put(clauseNot, value);
		return map;
	}

	private static MapXNode serializeInOidFilter(InOidFilter filter, PrismSerializer<RootXNode> xnodeSerializer) throws SchemaException {
		MapXNode clauseMap = new MapXNode();
		if (filter.getOids() != null && !filter.getOids().isEmpty()) {
			ListXNode valuesNode = new ListXNode();
			for (String oid : filter.getOids()) {
				XNode val = createPrimitiveXNode(oid, DOMUtil.XSD_STRING);
				valuesNode.add(val);
			}
			clauseMap.put(ELEMENT_VALUE, valuesNode);
		} else if (filter.getExpression() != null) {
			// TODO serialize expression
		} else {
			throw new SchemaException("InOid filter with no values nor expression");
		}
		if (filter.isConsiderOwner()) {
			clauseMap.put(ELEMENT_CONSIDER_OWNER, new PrimitiveXNode<>(true));
		}

		return createFilter(CLAUSE_IN_OID, clauseMap);
	}

	private static MapXNode serializeFullTextFilter(FullTextFilter filter, PrismSerializer<RootXNode> xnodeSerializer) throws SchemaException {
		MapXNode clauseMap = new MapXNode();
		if (filter.getValues() != null && !filter.getValues().isEmpty()) {
			ListXNode valuesNode = new ListXNode();
			for (String value : filter.getValues()) {
				XNode val = createPrimitiveXNode(value, DOMUtil.XSD_STRING);
				valuesNode.add(val);
			}
			clauseMap.put(ELEMENT_VALUE, valuesNode);
		} else if (filter.getExpression() != null) {
			// TODO serialize expression
		} else {
			throw new SchemaException("FullText filter with no values nor expression");
		}
		return createFilter(CLAUSE_FULL_TEXT, clauseMap);
	}

	private static <T> MapXNode serializeComparisonFilter(PropertyValueFilter<T> filter, PrismSerializer<RootXNode> xnodeSerializer) throws SchemaException{
		MapXNode map = new MapXNode();
		QName clause;
		if (filter instanceof EqualFilter) {
			clause = CLAUSE_EQUAL;
		} else if (filter instanceof GreaterFilter) {
			clause = ((GreaterFilter) filter).isEquals() ? CLAUSE_GREATER_OR_EQUAL : CLAUSE_GREATER;
		} else if (filter instanceof LessFilter) {
			clause = ((LessFilter) filter).isEquals() ? CLAUSE_LESS_OR_EQUAL : CLAUSE_LESS;
		} else {
			throw new IllegalStateException();
		}
		map.put(clause, serializeValueFilter(filter, xnodeSerializer));
		return map;
	}
	
	private static <V extends PrismValue, D extends ItemDefinition> MapXNode serializeValueFilter(ValueFilter<V,D> filter, PrismSerializer<RootXNode> xnodeSerializer) throws SchemaException {
		MapXNode map = new MapXNode();
		serializeMatchingRule(filter, map);
		serializePath(map, filter.getFullPath(), filter);

		List<V> values = filter.getValues();
		if (values != null) {
			ListXNode valuesNode = new ListXNode();
			for (V val : values) {
				if (val.getParent() == null) {
					val.setParent(filter);
				}
				XNode valNode = xnodeSerializer.definition(filter.getDefinition()).serialize(val).getSubnode();
				if (filter instanceof RefFilter) {		// TODO shouldn't we do this in all cases?
					valNode.setExplicitTypeDeclaration(true);
					if (valNode.getTypeQName() == null) {
						valNode.setTypeQName(ObjectReferenceType.COMPLEX_TYPE);
					}
				}
				valuesNode.add(valNode);
			}
			map.put(ELEMENT_VALUE, valuesNode);
		}
		if (filter.getRightHandSidePath() != null) {
			map.put(ELEMENT_RIGHT_HAND_SIDE_PATH, createPrimitiveXNode(filter.getRightHandSidePath().asItemPathType(), ItemPathType.COMPLEX_TYPE));
		}
		
		ExpressionWrapper xexpression = filter.getExpression();
		if (xexpression != null) {
			map.merge(PrismUtil.serializeExpression(xexpression, xnodeSerializer));
		}
		
		return map;
	}
	
	private static MapXNode serializeRefFilter(RefFilter filter, PrismSerializer<RootXNode> xnodeSerializer) throws SchemaException {
		MapXNode map = createFilter(CLAUSE_REF, serializeValueFilter(filter, xnodeSerializer));
		return map;
	}

	private static <T> MapXNode serializeSubstringFilter(SubstringFilter<T> filter, PrismSerializer<RootXNode> xnodeSerializer) throws SchemaException{
		MapXNode map = new MapXNode();
		MapXNode content = serializeValueFilter(filter, xnodeSerializer);
		if (filter.isAnchorStart()) {
			content.put(ELEMENT_ANCHOR_START, new PrimitiveXNode<>(true));
		}
		if (filter.isAnchorEnd()) {
			content.put(ELEMENT_ANCHOR_END, new PrimitiveXNode<>(true));
		}
		map.put(CLAUSE_SUBSTRING, content);
		return map;
	}
	
	private static MapXNode serializeTypeFilter(TypeFilter filter, PrismSerializer<RootXNode> xnodeSerializer) throws SchemaException{
		MapXNode content = new MapXNode();
		content.put(ELEMENT_TYPE, createPrimitiveXNode(filter.getType(), DOMUtil.XSD_QNAME));
		if (filter.getFilter() != null){
			content.put(ELEMENT_FILTER, serializeFilter(filter.getFilter(), xnodeSerializer));
		}
		return createFilter(CLAUSE_TYPE, content);
	}

	private static MapXNode serializeExistsFilter(ExistsFilter filter, PrismSerializer<RootXNode> xnodeSerializer) throws SchemaException{
		MapXNode content = new MapXNode();
		serializePath(content, filter.getFullPath(), filter);
		if (filter.getFilter() != null){
			content.put(ELEMENT_FILTER, serializeFilter(filter.getFilter(), xnodeSerializer));
		}
		return createFilter(CLAUSE_EXISTS, content);
	}

	private static MapXNode serializeOrgFilter(OrgFilter filter, PrismSerializer<RootXNode> xnodeSerializer) {
		MapXNode map = new MapXNode();

		if (filter.getOrgRef() != null) {
			MapXNode orgRefMap = new MapXNode();
			orgRefMap.put(ELEMENT_OID, createPrimitiveXNode(filter.getOrgRef().getOid(), DOMUtil.XSD_STRING));
			map.put(ELEMENT_ORG_REF, orgRefMap);
		}
		if (filter.getScope() != null) {
			map.put(ELEMENT_SCOPE, createPrimitiveXNode(filter.getScope().name(), DOMUtil.XSD_STRING));
		}
		if (filter.isRoot()) {
			map.put(ELEMENT_IS_ROOT, createPrimitiveXNode(Boolean.TRUE, DOMUtil.XSD_BOOLEAN));
		}

		return createFilter(CLAUSE_ORG, map);
	}

	private static MapXNode serializeAllFilter() {
		return createFilter(CLAUSE_ALL, new MapXNode());
	}

	private static MapXNode serializeNoneFilter() {
		return createFilter(CLAUSE_NONE, new MapXNode());
	}

	private static MapXNode serializeUndefinedFilter() {
		return createFilter(CLAUSE_UNDEFINED, new MapXNode());
	}

	private static void serializeMatchingRule(ValueFilter filter, MapXNode map) {
		if (filter.getMatchingRule() != null){
			PrimitiveXNode<String> matchingNode = createPrimitiveXNode(filter.getMatchingRule().getLocalPart(), DOMUtil.XSD_STRING);
			map.put(ELEMENT_MATCHING, matchingNode);
		}
	}
	
	private static void serializePath(MapXNode map, ItemPath path, ObjectFilter filter) {
		if (path == null) {
			throw new IllegalStateException("Cannot serialize filter " + filter + " because it does not contain path");
		}
		map.put(ELEMENT_PATH, createPrimitiveXNode(path.asItemPathType(), ItemPathType.COMPLEX_TYPE));
	}
	
	private static <T> XNode serializePropertyValue(PrismPropertyValue<T> value, PrismPropertyDefinition<T> definition, BeanMarshaller beanConverter) throws SchemaException {
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
		PrimitiveXNode<T> xprim = new PrimitiveXNode<>();
		xprim.setValue(val, type);
		return xprim;
	}
	

	// TODO what with this? [med]
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

    /**
     * Tries to parse as much from filter as possible, without knowing the definition of object(s) to which the
     * filter will be applied. It is used mainly to parse path specifications, in order to avoid namespace loss
     * when serializing raw (unparsed) paths and QNames - see MID-1969.
     *  
     * @param xfilter
     * @param prismContext
     */
    public static void parseFilterPreliminarily(MapXNode xfilter, PrismContext prismContext) throws SchemaException {
        parseFilterInternal(xfilter, null, true, prismContext);
    }

//	public PrismPropertyValue parsePrismPropertyFromGlobalXNodeValue(Entry<QName, XNode> entry, ParsingContext pc) throws SchemaException {
//		Validate.notNull(entry);
//
//		QName globalElementName = entry.getKey();
//		if (globalElementName == null) {
//			throw new SchemaException("No global element name to look for");
//		}
//		ItemDefinition itemDefinition = getSchemaRegistry().resolveGlobalItemDefinition(globalElementName);
//		if (itemDefinition == null) {
//			throw new SchemaException("No definition for item " + globalElementName);
//		}
//
//		if (itemDefinition instanceof PrismPropertyDefinition) {
//			PrismProperty prismProperty = parsePrismProperty(entry.getValue(), globalElementName, (PrismPropertyDefinition) itemDefinition, pc);
//			if (prismProperty.size() > 1) {
//				throw new SchemaException("Retrieved more than one value from globally defined element " + globalElementName);
//			} else if (prismProperty.size() == 0) {
//				return null;
//			} else {
//				return (PrismPropertyValue) prismProperty.getValues().get(0);
//			}
//		} else {
//			throw new IllegalArgumentException("Parsing global elements with definitions other than PrismPropertyDefinition is not supported yet: element = " + globalElementName + " definition kind = " + itemDefinition.getClass().getSimpleName());
//		}
//	}

}
