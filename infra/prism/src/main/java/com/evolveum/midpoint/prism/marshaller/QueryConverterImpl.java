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

import java.util.*;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.xnode.*;
import com.evolveum.prism.xml.ns._public.query_3.PagingType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectReferenceType;
import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.OrgFilter.Scope;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.prism.PrismConstants.NS_QUERY;

/**
 * Note that expressions are not serialized yet.
 */
public class QueryConverterImpl implements QueryConverter {

	//private static final Trace LOGGER = TraceManager.getTrace(QueryConverterImpl.class);

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
	private static final QName ELEMENT_TYPE = new QName(NS_QUERY, "type");
	private static final QName ELEMENT_FILTER = new QName(NS_QUERY, "filter");

	@NotNull
	private final PrismContext prismContext;

	public QueryConverterImpl(@NotNull PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	/**
	 * Used by XNodeProcessor and similar code that does not have complete schema for the filter
	 */
	@Override
	public ObjectFilter parseFilter(XNode xnode, Class<? extends Containerable> clazz) throws SchemaException {
		MapXNode xmap = toMap(xnode);
		PrismContainerDefinition<? extends Containerable> pcd = prismContext.getSchemaRegistry()
				.findContainerDefinitionByCompileTimeClass(clazz);
		return parseFilterInternal(xmap, pcd, false, null);
	}

	private <O extends Containerable> ObjectFilter parseFilter(MapXNode xmap, PrismContainerDefinition<O> def) throws SchemaException {
		if (xmap == null) {
			return null;
		}
		return parseFilterInternal(xmap, def, false, null);
	}

    public ObjectFilter parseFilter(@NotNull SearchFilterType filter, @NotNull Class<? extends Containerable> clazz) throws SchemaException {
        PrismContainerDefinition<? extends Containerable> def = prismContext.getSchemaRegistry().findContainerDefinitionByCompileTimeClass(clazz);
        if (def == null) {
            throw new SchemaException("Cannot find container definition for "+clazz);
        }
        return parseFilter(filter, def);
    }

    public ObjectFilter parseFilter(@NotNull SearchFilterType filter, @NotNull PrismContainerDefinition<?> def) throws SchemaException {
        return parseFilter(filter.getFilterClauseXNode(), def);
    }

	private <C extends Containerable> ObjectFilter parseFilterInternal(
			@NotNull MapXNode filterXMap,
			@Nullable PrismContainerDefinition<C> pcd,
			boolean preliminaryParsingOnly,
			@Nullable ParsingContext pc) throws SchemaException {
		Entry<QName, XNode> clauseEntry = filterXMap.getSingleEntryThatDoesNotMatch(SearchFilterType.F_DESCRIPTION);
		QName clauseQName = clauseEntry.getKey();
		XNode clauseContent = clauseEntry.getValue();
		return parseFilterInternal(clauseContent, clauseQName, pcd, preliminaryParsingOnly, pc);
	}

	private <C extends Containerable> ObjectFilter parseFilterInternal(XNode clauseContent, QName clauseQName,
			PrismContainerDefinition<C> pcd, boolean preliminaryParsingOnly, ParsingContext pc) throws SchemaException {

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
			return parseComparisonFilter(clauseQName, clauseXMap, pcd, preliminaryParsingOnly);
		} else if (QNameUtil.match(clauseQName, CLAUSE_SUBSTRING)) {
			return parseSubstringFilter(clauseXMap, pcd, preliminaryParsingOnly);
		} else if (QNameUtil.match(clauseQName, CLAUSE_REF)) {
			return parseRefFilter(clauseXMap, pcd, preliminaryParsingOnly);
		} else if (QNameUtil.match(clauseQName, CLAUSE_ORG)) {
			return parseOrgFilter(clauseXMap, preliminaryParsingOnly);
		} else if (QNameUtil.match(clauseQName, CLAUSE_IN_OID)) {
			return parseInOidFilter(clauseXMap);
		} else if (QNameUtil.match(clauseQName, CLAUSE_FULL_TEXT)) {
			return parseFullTextFilter(clauseXMap);
		}

		// logical filters

		if (QNameUtil.match(clauseQName, CLAUSE_AND)) {
			return parseAndFilter(clauseXMap, pcd, preliminaryParsingOnly, pc);
		} else if (QNameUtil.match(clauseQName, CLAUSE_OR)) {
			return parseOrFilter(clauseXMap, pcd, preliminaryParsingOnly, pc);
		} else if (QNameUtil.match(clauseQName, CLAUSE_NOT)) {
			return parseNotFilter(clauseXMap, pcd, preliminaryParsingOnly, pc);
		}

		// other complex filters

		if (QNameUtil.match(clauseQName, CLAUSE_TYPE)) {
			return parseTypeFilter(clauseXMap, preliminaryParsingOnly, pc);
		} else if (QNameUtil.match(clauseQName, CLAUSE_EXISTS)) {
			return parseExistsFilter(clauseXMap, pcd, preliminaryParsingOnly, pc);
		}

		throw new UnsupportedOperationException("Unsupported query filter " + clauseQName);

	}

	private <C extends Containerable> AndFilter parseAndFilter(MapXNode clauseXMap, PrismContainerDefinition<C> pcd,
			boolean preliminaryParsingOnly, ParsingContext pc) throws SchemaException {
		List<ObjectFilter> subfilters = parseLogicalFilter(clauseXMap, pcd, preliminaryParsingOnly, pc);
        if (preliminaryParsingOnly) {
            return null;
        } else {
		    return AndFilter.createAnd(subfilters);
        }
	}

	private <C extends Containerable> List<ObjectFilter> parseLogicalFilter(MapXNode clauseXMap,
			PrismContainerDefinition<C> pcd, boolean preliminaryParsingOnly, ParsingContext pc) throws SchemaException {
        List<ObjectFilter> subfilters = new ArrayList<>();
		for (Entry<QName, XNode> entry : clauseXMap.entrySet()) {
			if (entry.getValue() instanceof ListXNode){
				for (XNode xNode : ((ListXNode) entry.getValue())) {
					ObjectFilter subFilter = parseFilterInternal(xNode, entry.getKey(), pcd, preliminaryParsingOnly, pc);
					if (!preliminaryParsingOnly) {
						subfilters.add(subFilter);
					}
				}
			} else{
				ObjectFilter subfilter = parseFilterInternal(entry.getValue(), entry.getKey(), pcd, preliminaryParsingOnly, pc);
                if (!preliminaryParsingOnly) {
				    subfilters.add(subfilter);
                }
			}
		}
		return subfilters;
	}

	private <C extends Containerable> OrFilter parseOrFilter(MapXNode clauseXMap, PrismContainerDefinition<C> pcd,
			boolean preliminaryParsingOnly, ParsingContext pc) throws SchemaException {
		List<ObjectFilter> subfilters = parseLogicalFilter(clauseXMap, pcd, preliminaryParsingOnly, pc);
        if (preliminaryParsingOnly) {
            return null;
        } else {
		    return OrFilter.createOr(subfilters);
        }
	}

	private <C extends Containerable> NotFilter parseNotFilter(MapXNode clauseXMap, PrismContainerDefinition<C> pcd,
			boolean preliminaryParsingOnly, ParsingContext pc) throws SchemaException {
		Entry<QName, XNode> entry = singleSubEntry(clauseXMap, "not");
		ObjectFilter subfilter = parseFilterInternal(entry.getValue(), entry.getKey(), pcd, preliminaryParsingOnly, pc);
        if (preliminaryParsingOnly) {
            return null;
        } else {
		    return NotFilter.createNot(subfilter);
        }
	}

	private <T,C extends Containerable> ObjectFilter parseComparisonFilter(QName clauseQName, MapXNode clauseXMap,
			PrismContainerDefinition<C> pcd, boolean preliminaryParsingOnly) throws SchemaException {

		boolean isEq = QNameUtil.match(clauseQName, CLAUSE_EQUAL);
		boolean isGt = QNameUtil.match(clauseQName, CLAUSE_GREATER);
		boolean isGtEq = QNameUtil.match(clauseQName, CLAUSE_GREATER_OR_EQUAL);
		//boolean isLt = QNameUtil.match(clauseQName, CLAUSE_LESS);
		boolean isLtEq = QNameUtil.match(clauseQName, CLAUSE_LESS_OR_EQUAL);

		ItemPath itemPath = getPath(clauseXMap);
		if (itemPath == null || itemPath.isEmpty()){
			throw new SchemaException("Could not convert query, because query does not contain item path.");
		}
		QName itemName = ItemPath.getName(itemPath.last());

		QName matchingRule = getMatchingRule(clauseXMap);

		XNode valueXnode = clauseXMap.get(ELEMENT_VALUE);
		ItemPath rightSidePath = getPath(clauseXMap, ELEMENT_RIGHT_HAND_SIDE_PATH);

		ItemDefinition itemDefinition = locateItemDefinition(valueXnode, itemPath, pcd);
		if (itemDefinition != null) {
			itemName = itemDefinition.getName();
		}

		if (valueXnode != null) {
			if (preliminaryParsingOnly) {
				return null;
            } else {
				RootXNode valueRoot = new RootXNode(ELEMENT_VALUE, valueXnode);
				Item item = parseItem(valueRoot, itemName, itemDefinition);
				if (!isEq && item.getValues().size() != 1) {
					throw new SchemaException("Expected exactly one value, got " + item.getValues().size() + " instead");
				}
				if (isEq) {
					//noinspection unchecked
					List<PrismPropertyValue<T>> values = item.getValues();
					PrismValue.clearParent(values);
					//noinspection unchecked
					return EqualFilter.createEqual(itemPath, (PrismPropertyDefinition<T>)itemDefinition, matchingRule, prismContext, values);
				}
				//noinspection unchecked
				PrismPropertyValue<T> propertyValue = (PrismPropertyValue<T>) item.getValue(0);
				propertyValue.clearParent();
				if (isGt || isGtEq) {
					//noinspection unchecked
					return GreaterFilter.createGreater(itemPath, (PrismPropertyDefinition<T>) itemDefinition, matchingRule, propertyValue, isGtEq, prismContext);
				} else {
					//noinspection unchecked
					return LessFilter.createLess(itemPath, (PrismPropertyDefinition<T>) itemDefinition, matchingRule, propertyValue, isLtEq, prismContext);
				}
            }
		} else if (rightSidePath != null) {
			if (preliminaryParsingOnly) {
				return null;
			} else {
				ItemDefinition rightSideDefinition = pcd != null ? pcd.findItemDefinition(rightSidePath) : null;
				if (isEq) {
					//noinspection unchecked
					return EqualFilter.createEqual(itemPath, (PrismPropertyDefinition) itemDefinition, matchingRule, rightSidePath, rightSideDefinition);
				} else if (isGt || isGtEq) {
					//noinspection unchecked
					return GreaterFilter.createGreater(itemPath, (PrismPropertyDefinition) itemDefinition, matchingRule, rightSidePath, rightSideDefinition, isGtEq);
				} else {
					//noinspection unchecked
					return LessFilter.createLess(itemPath, (PrismPropertyDefinition) itemDefinition, matchingRule, rightSidePath, rightSideDefinition, isLtEq);
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
						//noinspection unchecked
						return EqualFilter.createEqual(itemPath, (PrismPropertyDefinition<T>) itemDefinition, matchingRule, expressionWrapper);
					} else if (isGt || isGtEq) {
						//noinspection unchecked
						return GreaterFilter.createGreater(itemPath, (PrismPropertyDefinition<T>) itemDefinition, matchingRule, expressionWrapper, isGtEq);
					} else {
						//noinspection unchecked
						return LessFilter.createLess(itemPath, (PrismPropertyDefinition<T>) itemDefinition, matchingRule, expressionWrapper, isLtEq);
					}
                }
			} else {
				if (!isEq) {
					throw new SchemaException("Comparison filter (greater, less) requires at least one value expression.");
				}
                if (preliminaryParsingOnly) {
                    return null;
                } else {
	                //noinspection unchecked
	                return EqualFilter.createEqual(itemPath, (PrismPropertyDefinition<T>) itemDefinition, matchingRule);
                }
            }
		}
	}

	private InOidFilter parseInOidFilter(MapXNode clauseXMap) throws SchemaException{
		boolean considerOwner = Boolean.TRUE.equals(clauseXMap.getParsedPrimitiveValue(ELEMENT_CONSIDER_OWNER, DOMUtil.XSD_BOOLEAN));
		XNode valueXnode = clauseXMap.get(ELEMENT_VALUE);
		if (valueXnode != null) {
			List<String> oids = getStringValues(valueXnode);
			return InOidFilter.createInOid(considerOwner, oids);
		} else {
			ExpressionWrapper expression = parseExpression(clauseXMap);
			if (expression != null) {
				return InOidFilter.createInOid(considerOwner, expression);
			} else {
				return InOidFilter.createInOid(Collections.emptySet());
			}
		}
	}

	private FullTextFilter parseFullTextFilter(MapXNode clauseXMap) throws SchemaException{
		XNode valueXnode = clauseXMap.get(ELEMENT_VALUE);
		if (valueXnode != null) {
			List<String> values = getStringValues(valueXnode);
			return FullTextFilter.createFullText(values);
		} else {
			ExpressionWrapper expression = parseExpression(clauseXMap);
			if (expression != null) {
				return FullTextFilter.createFullText(expression);
			} else {
				throw new SchemaException("FullText filter with no values nor expression");
			}
		}
	}

	@NotNull
	private List<String> getStringValues(
			XNode valueXnode) throws SchemaException {
		List<String> values = new ArrayList<>();
		if (valueXnode instanceof ListXNode) {
			for (XNode subnode : (ListXNode) valueXnode) {
				if (subnode instanceof PrimitiveXNode) {
					//noinspection unchecked
					values.add(((PrimitiveXNode<String>) subnode).getParsedValue(DOMUtil.XSD_STRING, String.class));
				} else {
					throw new SchemaException("The value was expected to be present as primitive XNode, instead it is: " + subnode);
				}
			}
		} else if (valueXnode instanceof PrimitiveXNode) {
			//noinspection unchecked
			values.add(((PrimitiveXNode<String>) valueXnode).getParsedValue(DOMUtil.XSD_STRING, String.class));
		} else {
			throw new SchemaException("The value was expected to be present as primitive or list XNode, instead it is: " + valueXnode);
		}
		return values;
	}

	private TypeFilter parseTypeFilter(MapXNode clauseXMap, boolean preliminaryParsingOnly,
			ParsingContext pc) throws SchemaException{
		QName type = clauseXMap.getParsedPrimitiveValue(ELEMENT_TYPE, DOMUtil.XSD_QNAME);
		XNode subXFilter = clauseXMap.get(ELEMENT_FILTER);
		PrismObjectDefinition def = prismContext.getSchemaRegistry().findObjectDefinitionByType(type);
		ObjectFilter subFilter = null;
		if (subXFilter instanceof MapXNode) {
			//noinspection unchecked
			subFilter = parseFilterInternal((MapXNode) subXFilter, def, preliminaryParsingOnly, pc);
		}
		if (!preliminaryParsingOnly || pc == null || pc.isStrict()) {
			// MID-3614: we want to be strict when importing but forgiving when reading from the repository
			checkExtraElements(clauseXMap, ELEMENT_TYPE, ELEMENT_FILTER);
		}
        if (preliminaryParsingOnly) {
            return null;
        } else {
		    return new TypeFilter(type, subFilter);
        }
	}

	private ExistsFilter parseExistsFilter(MapXNode clauseXMap, PrismContainerDefinition pcd,
			boolean preliminaryParsingOnly, ParsingContext pc) throws SchemaException {
		ItemPath path = getPath(clauseXMap);
		XNode subXFilter = clauseXMap.get(ELEMENT_FILTER);
		ObjectFilter subFilter = null;
		PrismContainerDefinition subPcd = pcd != null ? pcd.findContainerDefinition(path) : null;
		if (subXFilter instanceof MapXNode) {
			//noinspection unchecked
			subFilter = parseFilterInternal((MapXNode) subXFilter, subPcd, preliminaryParsingOnly, pc);
		}
		if (!preliminaryParsingOnly || pc == null || pc.isStrict()) {
			// MID-3614: we want to be strict when importing but forgiving when reading from the repository
			checkExtraElements(clauseXMap, ELEMENT_PATH, ELEMENT_FILTER);
		}
		if (preliminaryParsingOnly) {
            return null;
        } else {
			//noinspection unchecked
			return ExistsFilter.createExists(path, pcd, subFilter);
        }
	}

	private void checkExtraElements(MapXNode clauseXMap, QName... expected) throws SchemaException {
		List<QName> expectedList = Arrays.asList(expected);
		for (Entry<QName, XNode> entry : clauseXMap.entrySet()) {
			if (!QNameUtil.contains(expectedList, entry.getKey())) {
				throw new SchemaException("Unexpected item " + entry.getKey() + ":" +
						(entry.getValue() != null ? entry.getValue().debugDump() : "null"));
			}
		}
	}

	private <C extends Containerable> RefFilter parseRefFilter(MapXNode clauseXMap, PrismContainerDefinition<C> pcd,
			boolean preliminaryParsingOnly) throws SchemaException {
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
					.context(prismContext.createParsingContextForAllowMissingRefTypes())
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
			ExpressionWrapper expressionWrapper = parseExpression(clauseXMap);
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

	private ExpressionWrapper parseExpression(MapXNode xmap) throws SchemaException {
		Entry<QName, XNode> expressionEntry = xmap.getSingleEntryThatDoesNotMatch(
				ELEMENT_VALUE, ELEMENT_MATCHING, ELEMENT_ANCHOR_START, ELEMENT_ANCHOR_END, ELEMENT_PATH);
		return PrismUtil.parseExpression(expressionEntry, prismContext);
	}

	private <C extends Containerable> SubstringFilter parseSubstringFilter(MapXNode clauseXMap, PrismContainerDefinition<C> pcd,
			boolean preliminaryParsingOnly)
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
		ItemDefinition itemDefinition = locateItemDefinition(valueXnode, itemPath, pcd);

		if (valueXnode != null) {

			Item item = parseItem(new RootXNode(ELEMENT_VALUE, valueXnode), itemName, itemDefinition);

			if (preliminaryParsingOnly) {
				return null;
			} else {
				List values = item.getValues();
				Object realValue;
				if (values.isEmpty()) {
					realValue = null; // TODO throw an exception?
				} else if (values.size() > 1) {
					throw new IllegalArgumentException("Expected at most 1 value, got " + values);
				} else {
					realValue = ((PrismPropertyValue) values.get(0)).getValue();
				}
				//noinspection unchecked
				return SubstringFilter.createSubstring(itemPath, (PrismPropertyDefinition) itemDefinition, prismContext,
						matchingRule, realValue, anchorStart, anchorEnd);

			}
		} else {
			ExpressionWrapper expressionWrapper = parseExpression(clauseXMap);
			if (expressionWrapper != null) {
				if (preliminaryParsingOnly) {
					return null;
				} else {
					//noinspection unchecked
					return SubstringFilter.createSubstring(itemPath, (PrismPropertyDefinition) itemDefinition, prismContext, matchingRule, expressionWrapper, anchorStart, anchorEnd);
				}
			} else {
				if (preliminaryParsingOnly) {
					return null;
				} else {
					//noinspection unchecked
					return SubstringFilter.createSubstring(itemPath, (PrismPropertyDefinition) itemDefinition, prismContext, matchingRule,
							null, anchorStart, anchorEnd);
				}
			}
		}
	}

	private OrgFilter parseOrgFilter(MapXNode clauseXMap, boolean preliminaryParsingOnly) throws SchemaException {
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

	@SuppressWarnings("SameParameterValue")
	private Entry<QName, XNode> singleSubEntry(MapXNode xmap, String filterName) throws SchemaException {
		if (xmap == null || xmap.isEmpty()) {
			return null;
		}
		return xmap.getSingleSubEntry("search filter "+filterName);
	}

	private MapXNode toMap(XNode xnode) throws SchemaException {
		if (xnode instanceof PrimitiveXNode && xnode.isEmpty()) {
			return new MapXNode();          // hack but it might work (todo implement better)
		} else if (!(xnode instanceof MapXNode)) {
			throw new SchemaException("Cannot parse filter from "+xnode);
		} else {
			return (MapXNode) xnode;
		}
	}

//	private PrimitiveXNode toPrimitive(XNode xnode, XNode context) throws SchemaException {
//		if (!(xnode instanceof PrimitiveXNode)) {
//			throw new SchemaException("Cannot parse filter from "+context+ ": This should be a primitive: "+xnode);
//		}
//		return (PrimitiveXNode)xnode;
//	}

	private ItemPath getPath(MapXNode clauseXMap) throws SchemaException {
		return getPath(clauseXMap, ELEMENT_PATH);
	}

	private ItemPath getPath(MapXNode clauseXMap, QName key) throws SchemaException {
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

	private QName getMatchingRule(MapXNode xmap) throws SchemaException{
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

	private Item parseItem(RootXNode root, QName itemName, ItemDefinition itemDefinition) throws SchemaException{
		Item<?,?> item;
		item = prismContext.parserFor(root)
				.name(itemName)
				.definition(itemDefinition)
				.context(prismContext.createParsingContextForAllowMissingRefTypes())
				.parseItem();

		if (item.getValues().size() < 1 ) {
			throw new IllegalStateException("No values to search specified for item " + itemName);
		}

		return item;
	}

	private <C extends Containerable> ItemDefinition locateItemDefinition(XNode valueXnode, ItemPath itemPath,
			PrismContainerDefinition<C> pcd) throws SchemaException{
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
					if (itemDefinition instanceof PrismContainerDefinition){
						//noinspection unchecked
						return locateItemDefinition(valueXnode, rest, (PrismContainerDefinition) itemDefinition);
					}
				}
			}
		}
		return itemDefinition;
	}

//    public static SearchFilterType createSearchFilterType(ObjectFilter filter, PrismContext prismContext) throws SchemaException {
//        MapXNode xnode = serializeFilter(filter, prismContext);
//        return SearchFilterType.createFromSerializedXNode(xnode, prismContext);
//    }

	public MapXNode serializeFilter(ObjectFilter filter) throws SchemaException{
		return serializeFilter(filter, prismContext.xnodeSerializer());
	}

	private MapXNode serializeFilter(ObjectFilter filter, PrismSerializer<RootXNode> xnodeSerializer) throws SchemaException{
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
			//noinspection unchecked
			return serializeComparisonFilter((PropertyValueFilter) filter, xnodeSerializer);
		} else if (filter instanceof SubstringFilter) {
			//noinspection unchecked
			return serializeSubstringFilter((SubstringFilter) filter, xnodeSerializer);
		} else if (filter instanceof RefFilter) {
			return serializeRefFilter((RefFilter) filter, xnodeSerializer);
		} else if (filter instanceof OrgFilter) {
			return serializeOrgFilter((OrgFilter) filter);
		} else if (filter instanceof InOidFilter) {
			return serializeInOidFilter((InOidFilter) filter);
		} else if (filter instanceof FullTextFilter) {
			return serializeFullTextFilter((FullTextFilter) filter);
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


	private MapXNode serializeAndFilter(AndFilter filter, PrismSerializer<RootXNode> xnodeSerilizer) throws SchemaException{
		return createFilter(CLAUSE_AND, serializeNaryLogicalSubfilters(filter.getConditions(), xnodeSerilizer));
	}

	private MapXNode serializeOrFilter(OrFilter filter, PrismSerializer<RootXNode> xnodeSerilizer) throws SchemaException{
		return createFilter(CLAUSE_OR, serializeNaryLogicalSubfilters(filter.getConditions(), xnodeSerilizer));
	}

	private MapXNode serializeNaryLogicalSubfilters(List<ObjectFilter> objectFilters, PrismSerializer<RootXNode> xnodeSerilizer) throws SchemaException{
		MapXNode filters = new MapXNode();
		for (ObjectFilter of : objectFilters) {
			MapXNode subFilter = serializeFilter(of, xnodeSerilizer);
			filters.merge(subFilter);
		}
		return filters;
	}

	private MapXNode serializeNotFilter(NotFilter filter, PrismSerializer<RootXNode> xnodeSerializer) throws SchemaException{
		return createFilter(CLAUSE_NOT, serializeFilter(filter.getFilter(), xnodeSerializer));
	}

	@NotNull
	private MapXNode createFilter(QName clauseNot, MapXNode value) {
		MapXNode map = new MapXNode();
		map.put(clauseNot, value);
		return map;
	}

	private MapXNode serializeInOidFilter(InOidFilter filter) {
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
			// just an empty filter -- ignore it
			// throw new SchemaException("InOid filter with no values nor expression");
		}
		if (filter.isConsiderOwner()) {
			clauseMap.put(ELEMENT_CONSIDER_OWNER, new PrimitiveXNode<>(true));
		}

		return createFilter(CLAUSE_IN_OID, clauseMap);
	}

	private MapXNode serializeFullTextFilter(FullTextFilter filter) throws SchemaException {
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

	private <T> MapXNode serializeComparisonFilter(PropertyValueFilter<T> filter, PrismSerializer<RootXNode> xnodeSerializer) throws SchemaException{
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

	private <V extends PrismValue, D extends ItemDefinition> MapXNode serializeValueFilter(ValueFilter<V,D> filter, PrismSerializer<RootXNode> xnodeSerializer) throws SchemaException {
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

	private MapXNode serializeRefFilter(RefFilter filter, PrismSerializer<RootXNode> xnodeSerializer) throws SchemaException {
		return createFilter(CLAUSE_REF, serializeValueFilter(filter, xnodeSerializer));
	}

	private <T> MapXNode serializeSubstringFilter(SubstringFilter<T> filter, PrismSerializer<RootXNode> xnodeSerializer) throws SchemaException{
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

	private MapXNode serializeTypeFilter(TypeFilter filter, PrismSerializer<RootXNode> xnodeSerializer) throws SchemaException{
		MapXNode content = new MapXNode();
		content.put(ELEMENT_TYPE, createPrimitiveXNode(filter.getType(), DOMUtil.XSD_QNAME));
		if (filter.getFilter() != null){
			content.put(ELEMENT_FILTER, serializeFilter(filter.getFilter(), xnodeSerializer));
		}
		return createFilter(CLAUSE_TYPE, content);
	}

	private MapXNode serializeExistsFilter(ExistsFilter filter, PrismSerializer<RootXNode> xnodeSerializer) throws SchemaException{
		MapXNode content = new MapXNode();
		serializePath(content, filter.getFullPath(), filter);
		if (filter.getFilter() != null){
			content.put(ELEMENT_FILTER, serializeFilter(filter.getFilter(), xnodeSerializer));
		}
		return createFilter(CLAUSE_EXISTS, content);
	}

	private MapXNode serializeOrgFilter(OrgFilter filter) {
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

	private MapXNode serializeAllFilter() {
		return createFilter(CLAUSE_ALL, new MapXNode());
	}

	private MapXNode serializeNoneFilter() {
		return createFilter(CLAUSE_NONE, new MapXNode());
	}

	private MapXNode serializeUndefinedFilter() {
		return createFilter(CLAUSE_UNDEFINED, new MapXNode());
	}

	private void serializeMatchingRule(ValueFilter filter, MapXNode map) {
		if (filter.getMatchingRule() != null){
			PrimitiveXNode<String> matchingNode = createPrimitiveXNode(filter.getMatchingRule().getLocalPart(), DOMUtil.XSD_STRING);
			map.put(ELEMENT_MATCHING, matchingNode);
		}
	}

	private void serializePath(MapXNode map, ItemPath path, ObjectFilter filter) {
		if (path == null) {
			throw new IllegalStateException("Cannot serialize filter " + filter + " because it does not contain path");
		}
		map.put(ELEMENT_PATH, createPrimitiveXNode(path.asItemPathType(), ItemPathType.COMPLEX_TYPE));
	}

//	private <T> XNode serializePropertyValue(PrismPropertyValue<T> value, PrismPropertyDefinition<T> definition, BeanMarshaller beanConverter) throws SchemaException {
//			QName typeQName = definition.getTypeName();
//			T realValue = value.getValue();
//			if (beanConverter.canProcess(typeQName)) {
//				return beanConverter.marshall(realValue);
//			} else {
//				// primitive value
//				return createPrimitiveXNode(realValue, typeQName);
//			}
//		}

	private <T> PrimitiveXNode<T> createPrimitiveXNode(T val, QName type) {
		PrimitiveXNode<T> xprim = new PrimitiveXNode<>();
		xprim.setValue(val, type);
		return xprim;
	}

    /**
     * Tries to parse as much from filter as possible, without knowing the definition of object(s) to which the
     * filter will be applied. It is used mainly to parse path specifications, in order to avoid namespace loss
     * when serializing raw (unparsed) paths and QNames - see MID-1969.
     */
    @Override
    public void parseFilterPreliminarily(MapXNode xfilter, ParsingContext pc) throws SchemaException {
        parseFilterInternal(xfilter, null, true, pc);
    }

	public <O extends Objectable> ObjectQuery createObjectQuery(Class<O> clazz, QueryType queryType)
			throws SchemaException {
		if (queryType == null) {
			return null;
		}
		return createObjectQueryInternal(clazz, queryType.getFilter(), queryType.getPaging());
	}

	public <O extends Objectable> ObjectQuery createObjectQuery(Class<O> clazz, SearchFilterType filterType)
			throws SchemaException {
		return createObjectQueryInternal(clazz, filterType, null);
	}

	public <O extends Objectable> ObjectFilter createObjectFilter(Class<O> clazz, SearchFilterType filterType)
			throws SchemaException {
		ObjectQuery query = createObjectQueryInternal(clazz, filterType, null);
		if (query == null) {
			return null;
		} else {
			return query.getFilter();
		}
	}

	public <O extends Objectable> ObjectFilter createObjectFilter(PrismObjectDefinition<O> objectDefinition,
			SearchFilterType filterType)
			throws SchemaException {
		ObjectQuery query = createObjectQueryInternal(objectDefinition, filterType, null);
		if (query == null) {
			return null;
		} else {
			return query.getFilter();
		}
	}

	private <O extends Containerable> ObjectQuery createObjectQueryInternal(Class<O> clazz, SearchFilterType filterType,
			PagingType pagingType)
			throws SchemaException {
		PrismContainerDefinition<O> objDef = prismContext.getSchemaRegistry().findContainerDefinitionByCompileTimeClass(clazz);
		if (objDef == null) {
			throw new SchemaException("cannot find obj/container definition for class "+clazz);
		}
		return createObjectQueryInternal(objDef, filterType, pagingType);
	}

	private <O extends Containerable> ObjectQuery createObjectQueryInternal(PrismContainerDefinition<O> objDef,
			SearchFilterType filterType, PagingType pagingType)
			throws SchemaException {

		try {
			ObjectQuery query = new ObjectQuery();

			if (filterType != null && filterType.containsFilterClause()) {
				MapXNode rootFilter = filterType.getFilterClauseXNode();
				ObjectFilter filter = parseFilter(rootFilter, objDef);
				query.setFilter(filter);
			}

			if (pagingType != null) {
				ObjectPaging paging = PagingConvertor.createObjectPaging(pagingType);
				query.setPaging(paging);
			}
			return query;
		} catch (SchemaException ex) {
			throw new SchemaException("Failed to convert query. Reason: " + ex.getMessage(), ex);
		}

	}

	public QueryType createQueryType(ObjectQuery query) throws SchemaException {
		ObjectFilter filter = query.getFilter();
		QueryType queryType = new QueryType();
		if (filter != null) {
			queryType.setFilter(createSearchFilterType(filter));
		}
		queryType.setPaging(PagingConvertor.createPagingType(query.getPaging()));
		return queryType;
	}

	public SearchFilterType createSearchFilterType(ObjectFilter filter) throws SchemaException {
		if (filter == null) {
			return null;
		}
		SearchFilterType filterType = new SearchFilterType();
		MapXNode filterXNode = serializeFilter(filter);
		filterType.setFilterClauseXNode(filterXNode);
		return filterType;
	}

}
