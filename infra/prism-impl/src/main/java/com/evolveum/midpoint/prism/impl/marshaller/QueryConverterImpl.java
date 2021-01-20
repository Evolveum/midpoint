/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.marshaller;

import java.util.*;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.PrismContextImpl;
import com.evolveum.midpoint.prism.impl.query.*;
import com.evolveum.midpoint.prism.impl.xnode.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.impl.util.PrismUtilInternal;
import com.evolveum.midpoint.prism.xnode.*;
import com.evolveum.prism.xml.ns._public.query_3.PagingType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectReferenceType;
import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.query.OrgFilter.Scope;
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
    private static final QName CLAUSE_TEXT = new QName(NS_QUERY, "text");


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
        MapXNodeImpl xmap = toMap(xnode);
        PrismContainerDefinition<? extends Containerable> pcd = prismContext.getSchemaRegistry()
                .findContainerDefinitionByCompileTimeClass(clazz);
        return parseFilterInternal(xmap, pcd, false, null);
    }

    private <O extends Containerable> ObjectFilter parseFilter(MapXNode xmap, PrismContainerDefinition<O> def) throws SchemaException {
        if (xmap == null) {
            return null;
        }
        return parseFilterInternal((MapXNodeImpl) xmap, def, false, null);
    }

    @Override
    public ObjectFilter parseFilter(@NotNull SearchFilterType filter, @NotNull Class<? extends Containerable> clazz) throws SchemaException {
        PrismContainerDefinition<? extends Containerable> def = prismContext.getSchemaRegistry().findContainerDefinitionByCompileTimeClass(clazz);
        if (def == null) {
            throw new SchemaException("Cannot find container definition for "+clazz);
        }
        return parseFilter(filter, def);
    }

    @Override
    public ObjectFilter parseFilter(@NotNull SearchFilterType filter, @NotNull PrismContainerDefinition<?> def) throws SchemaException {
        if(filter.getText() != null) {
            return parseTextFilter(filter.getText(), def);
        }

        return parseFilter(filter.getFilterClauseXNode(), def);
    }

    private ObjectFilter parseTextFilter(@NotNull String filter, @NotNull PrismContainerDefinition<?> def) throws SchemaException {
        return prismContext.createQueryParser().parseQuery(def, filter);
    }



    private <C extends Containerable> ObjectFilter parseFilterInternal(
            @NotNull MapXNodeImpl filterXMap,
            @Nullable PrismContainerDefinition<C> pcd,
            boolean preliminaryParsingOnly,
            @Nullable ParsingContext pc) throws SchemaException {
        Entry<QName, XNodeImpl> clauseEntry = filterXMap.getSingleEntryThatDoesNotMatch(SearchFilterType.F_DESCRIPTION);
        QName clauseQName = clauseEntry.getKey();
        XNodeImpl clauseContent = clauseEntry.getValue();
        return parseFilterInternal(clauseContent, clauseQName, pcd, preliminaryParsingOnly, pc);
    }

    private <C extends Containerable> ObjectFilter parseFilterInternal(XNodeImpl clauseContent, QName clauseQName,
            PrismContainerDefinition<C> pcd, boolean preliminaryParsingOnly, ParsingContext pc) throws SchemaException {
        ObjectFilter filter = parseFilterInternalUnfrozen(clauseContent, clauseQName, pcd, preliminaryParsingOnly, pc);
        Freezable.freezeNullable(filter);
        return filter;
    }

    private <C extends Containerable> ObjectFilter parseFilterInternalUnfrozen(XNodeImpl clauseContent, QName clauseQName,
            PrismContainerDefinition<C> pcd, boolean preliminaryParsingOnly, ParsingContext pc) throws SchemaException {

        // trivial filters
        if(QNameUtil.match(clauseQName, CLAUSE_TEXT)) {
            return parseTextFilter(clauseContent, pcd,preliminaryParsingOnly);
        }

        if (QNameUtil.match(clauseQName, CLAUSE_ALL)) {
            return AllFilterImpl.createAll();
        } else if (QNameUtil.match(clauseQName, CLAUSE_NONE)) {
            return NoneFilterImpl.createNone();
        } else if (QNameUtil.match(clauseQName, CLAUSE_UNDEFINED)) {
            return UndefinedFilterImpl.createUndefined();
        }

        // primitive filters
        MapXNodeImpl clauseXMap = toMap(clauseContent);

        if (QNameUtil.match(clauseQName, CLAUSE_EQUAL)
                || QNameUtil.match(clauseQName, CLAUSE_GREATER)
                || QNameUtil.match(clauseQName, CLAUSE_GREATER_OR_EQUAL)
                || QNameUtil.match(clauseQName, CLAUSE_LESS)
                || QNameUtil.match(clauseQName, CLAUSE_LESS_OR_EQUAL)
                || QNameUtil.match(clauseQName, CLAUSE_SUBSTRING)) {
            return parseComparisonFilter(clauseQName, clauseXMap, pcd, preliminaryParsingOnly);
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

    private ObjectFilter parseTextFilter(XNodeImpl clauseContent, PrismContainerDefinition<?> pcd, boolean preliminaryParsingOnly) throws SchemaException {
        if(clauseContent instanceof PrimitiveXNode<?>) {
            String filter = ((PrimitiveXNode<?>) clauseContent).getParsedValue(DOMUtil.XSD_STRING, String.class);
            if(filter != null) {
                if(preliminaryParsingOnly) {
                    // FIXME: Run syntax validation
                    return null;
                }
                return parseTextFilter(filter, pcd);
            }
        }
        throw new SchemaException("Element text must contain textual filter");
    }

    private <C extends Containerable> AndFilter parseAndFilter(MapXNodeImpl clauseXMap, PrismContainerDefinition<C> pcd,
            boolean preliminaryParsingOnly, ParsingContext pc) throws SchemaException {
        List<ObjectFilter> subFilters = parseLogicalFilter(clauseXMap, pcd, preliminaryParsingOnly, pc);
        if (preliminaryParsingOnly) {
            return null;
        } else {
            return prismContext.queryFactory().createAnd(subFilters);
        }
    }

    private <C extends Containerable> List<ObjectFilter> parseLogicalFilter(MapXNodeImpl clauseXMap,
            PrismContainerDefinition<C> pcd, boolean preliminaryParsingOnly, ParsingContext pc) throws SchemaException {
        List<ObjectFilter> subFilters = new ArrayList<>();
        for (Entry<QName, XNodeImpl> entry : clauseXMap.entrySet()) {
            if (entry.getValue() instanceof ListXNodeImpl){
                for (XNodeImpl xNode : ((ListXNodeImpl) entry.getValue())) {
                    ObjectFilter subFilter = parseFilterInternal(xNode, entry.getKey(), pcd, preliminaryParsingOnly, pc);
                    if (!preliminaryParsingOnly) {
                        subFilters.add(subFilter);
                    }
                }
            } else{
                ObjectFilter subfilter = parseFilterInternal(entry.getValue(), entry.getKey(), pcd, preliminaryParsingOnly, pc);
                if (!preliminaryParsingOnly) {
                    subFilters.add(subfilter);
                }
            }
        }
        return subFilters;
    }

    private <C extends Containerable> OrFilter parseOrFilter(MapXNodeImpl clauseXMap, PrismContainerDefinition<C> pcd,
            boolean preliminaryParsingOnly, ParsingContext pc) throws SchemaException {
        List<ObjectFilter> subFilters = parseLogicalFilter(clauseXMap, pcd, preliminaryParsingOnly, pc);
        if (preliminaryParsingOnly) {
            return null;
        } else {
            return OrFilterImpl.createOr(subFilters);
        }
    }

    private <C extends Containerable> NotFilter parseNotFilter(MapXNodeImpl clauseXMap, PrismContainerDefinition<C> pcd,
            boolean preliminaryParsingOnly, ParsingContext pc) throws SchemaException {
        Entry<QName, XNodeImpl> entry = singleSubEntry(clauseXMap, "not");
        ObjectFilter subfilter = parseFilterInternal(entry.getValue(), entry.getKey(), pcd, preliminaryParsingOnly, pc);
        if (preliminaryParsingOnly) {
            return null;
        } else {
            return NotFilterImpl.createNot(subfilter);
        }
    }

    private <T,C extends Containerable> ObjectFilter parseComparisonFilter(QName clauseQName, MapXNodeImpl clauseXMap,
            PrismContainerDefinition<C> pcd, boolean preliminaryParsingOnly) throws SchemaException {

        boolean isEq = QNameUtil.match(clauseQName, CLAUSE_EQUAL);
        boolean isGt = QNameUtil.match(clauseQName, CLAUSE_GREATER);
        boolean isGtEq = QNameUtil.match(clauseQName, CLAUSE_GREATER_OR_EQUAL);
        //boolean isLt = QNameUtil.match(clauseQName, CLAUSE_LESS);
        boolean isLtEq = QNameUtil.match(clauseQName, CLAUSE_LESS_OR_EQUAL);
        boolean isSubstring = QNameUtil.match(clauseQName, CLAUSE_SUBSTRING);

        ItemPath itemPath = getPath(clauseXMap);
        if (itemPath == null || itemPath.isEmpty()) {
            throw new SchemaException("Could not convert query, because query does not contain item path.");
        }
        QName itemName = ItemPath.toName(itemPath.last());

        QName matchingRule = getMatchingRule(clauseXMap);

        XNodeImpl valueXnode = clauseXMap.get(ELEMENT_VALUE);
        ItemPath rightSidePath = getPath(clauseXMap, ELEMENT_RIGHT_HAND_SIDE_PATH);

        ItemDefinition<?> itemDefinition = locateItemDefinition(valueXnode, itemPath, pcd);
        if (itemDefinition != null) {
            itemName = itemDefinition.getItemName();
        }

        if (valueXnode != null) {
            if (preliminaryParsingOnly) {
                return null;
            } else {
                RootXNodeImpl valueRoot = new RootXNodeImpl(ELEMENT_VALUE, valueXnode);
                //noinspection rawtypes
                Item item = parseItem(valueRoot, itemName, itemDefinition);
                if (!isEq && item.getValues().size() != 1) {
                    throw new SchemaException("Expected exactly one value, got " + item.getValues().size() + " instead");
                }
                if (isEq) {
                    //noinspection unchecked
                    List<PrismPropertyValue<T>> values = item.getValues();
                    PrismValueCollectionsUtil.clearParent(values);
                    //noinspection unchecked
                    return EqualFilterImpl.createEqual(itemPath, (PrismPropertyDefinition<T>)itemDefinition, matchingRule, prismContext, values);
                } else if (isSubstring) {
                    return SubstringFilterImpl.createSubstring(itemPath, (PrismPropertyDefinition<?>) itemDefinition, prismContext, matchingRule, item.getAnyValue(), getAnchorStart(clauseXMap), getAnchorEnd(clauseXMap));
                }
                //noinspection unchecked
                PrismPropertyValue<T> propertyValue = (PrismPropertyValue<T>) item.getAnyValue();
                propertyValue.clearParent();
                if (isGt || isGtEq) {
                    //noinspection unchecked
                    return GreaterFilterImpl
                            .createGreater(itemPath, (PrismPropertyDefinition<T>) itemDefinition, matchingRule, propertyValue, isGtEq, prismContext);
                } else {
                    //noinspection unchecked
                    return LessFilterImpl.createLess(itemPath, (PrismPropertyDefinition<T>) itemDefinition, matchingRule, propertyValue, isLtEq, prismContext);
                }
            }
        } else if (rightSidePath != null) {
            if (preliminaryParsingOnly) {
                return null;
            } else {
                ItemDefinition<?> rightSideDefinition = pcd != null ? pcd.findItemDefinition(rightSidePath) : null;
                if (isEq) {
                    return EqualFilterImpl.createEqual(itemPath, (PrismPropertyDefinition<?>) itemDefinition, matchingRule, rightSidePath, rightSideDefinition);
                } else if (isGt || isGtEq) {
                    return GreaterFilterImpl.createGreater(itemPath, (PrismPropertyDefinition<?>) itemDefinition, matchingRule, rightSidePath, rightSideDefinition, isGtEq);
                } else {
                    return LessFilterImpl.createLess(itemPath, (PrismPropertyDefinition<?>) itemDefinition, matchingRule, rightSidePath, rightSideDefinition, isLtEq);
                }
            }
        } else {
            Entry<QName, XNodeImpl> expressionEntry = clauseXMap.getSingleEntryThatDoesNotMatch(
                    ELEMENT_VALUE, ELEMENT_MATCHING, ELEMENT_PATH, ELEMENT_ANCHOR_START, ELEMENT_ANCHOR_END);
            if (expressionEntry != null) {
                if (preliminaryParsingOnly) {
                    return null;
                } else {
                    RootXNodeImpl expressionRoot = clauseXMap.getEntryAsRoot(expressionEntry.getKey());
                    PrismPropertyValue<?> expressionPropertyValue = prismContext.parserFor(expressionRoot).parseItemValue();
                    ExpressionWrapper expressionWrapper = new ExpressionWrapper(expressionEntry.getKey(), expressionPropertyValue.getValue());
                    if (isEq) {
                        //noinspection unchecked
                        return EqualFilterImpl.createEqual(itemPath, (PrismPropertyDefinition<T>) itemDefinition, matchingRule, expressionWrapper);
                    } else if (isSubstring) {
                        return SubstringFilterImpl.createSubstring(itemPath, (PrismPropertyDefinition<?>) itemDefinition, matchingRule, expressionWrapper, getAnchorStart(clauseXMap), getAnchorEnd(clauseXMap));
                    } else if (isGt || isGtEq) {
                        //noinspection unchecked
                        return GreaterFilterImpl.createGreater(itemPath, (PrismPropertyDefinition<T>) itemDefinition, matchingRule, expressionWrapper, isGtEq);
                    } else {
                        //noinspection unchecked
                        return LessFilterImpl.createLess(itemPath, (PrismPropertyDefinition<T>) itemDefinition, matchingRule, expressionWrapper, isLtEq);
                    }
                }
            } else {
                if (!isEq && !isSubstring) {
                    throw new SchemaException("Comparison filter (greater, less) requires at least one value expression.");
                }
                if (preliminaryParsingOnly) {
                    return null;
                } else {

                    if (isSubstring) {
                        return SubstringFilterImpl.createSubstring(itemPath, (PrismPropertyDefinition<?>) itemDefinition, matchingRule, null, getAnchorStart(clauseXMap), getAnchorEnd(clauseXMap));
                    } else {
                        //noinspection unchecked
                        return EqualFilterImpl.createEqual(itemPath, (PrismPropertyDefinition<T>) itemDefinition, matchingRule);
                    }
                }
            }
        }
    }

    private InOidFilter parseInOidFilter(MapXNodeImpl clauseXMap) throws SchemaException{
        boolean considerOwner = Boolean.TRUE.equals(clauseXMap.getParsedPrimitiveValue(ELEMENT_CONSIDER_OWNER, DOMUtil.XSD_BOOLEAN));
        XNodeImpl valueXnode = clauseXMap.get(ELEMENT_VALUE);
        if (valueXnode != null) {
            List<String> oids = getStringValues(valueXnode);
            return InOidFilterImpl.createInOid(considerOwner, oids);
        } else {
            ExpressionWrapper expression = parseExpression(clauseXMap);
            if (expression != null) {
                return InOidFilterImpl.createInOid(considerOwner, expression);
            } else {
                return InOidFilterImpl.createInOid(Collections.emptySet());
            }
        }
    }

    private FullTextFilter parseFullTextFilter(MapXNodeImpl clauseXMap) throws SchemaException{
        XNodeImpl valueXnode = clauseXMap.get(ELEMENT_VALUE);
        if (valueXnode != null) {
            List<String> values = getStringValues(valueXnode);
            return FullTextFilterImpl.createFullText(values);
        } else {
            ExpressionWrapper expression = parseExpression(clauseXMap);
            if (expression != null) {
                return FullTextFilterImpl.createFullText(expression);
            } else {
                throw new SchemaException("FullText filter with no values nor expression");
            }
        }
    }

    @NotNull
    private List<String> getStringValues(
            XNodeImpl valueXnode) throws SchemaException {
        List<String> values = new ArrayList<>();
        if (valueXnode instanceof ListXNodeImpl) {
            for (XNodeImpl subnode : (ListXNodeImpl) valueXnode) {
                if (subnode instanceof PrimitiveXNodeImpl) {
                    //noinspection unchecked
                    values.add(((PrimitiveXNodeImpl<String>) subnode).getParsedValue(DOMUtil.XSD_STRING, String.class));
                } else {
                    throw new SchemaException("The value was expected to be present as primitive XNode, instead it is: " + subnode);
                }
            }
        } else if (valueXnode instanceof PrimitiveXNodeImpl) {
            //noinspection unchecked
            values.add(((PrimitiveXNodeImpl<String>) valueXnode).getParsedValue(DOMUtil.XSD_STRING, String.class));
        } else {
            throw new SchemaException("The value was expected to be present as primitive or list XNode, instead it is: " + valueXnode);
        }
        return values;
    }

    private TypeFilter parseTypeFilter(MapXNodeImpl clauseXMap, boolean preliminaryParsingOnly,
            ParsingContext pc) throws SchemaException{
        QName type = clauseXMap.getParsedPrimitiveValue(ELEMENT_TYPE, DOMUtil.XSD_QNAME);
        XNodeImpl subXFilter = clauseXMap.get(ELEMENT_FILTER);
        PrismObjectDefinition<?> def = prismContext.getSchemaRegistry().findObjectDefinitionByType(type);
        ObjectFilter subFilter;
        if (subXFilter instanceof MapXNodeImpl) {
            subFilter = parseFilterInternal((MapXNodeImpl) subXFilter, def, preliminaryParsingOnly, pc);
        } else {
            subFilter = null;
        }
        if (!preliminaryParsingOnly || pc == null || pc.isStrict()) {
            // MID-3614: we want to be strict when importing but forgiving when reading from the repository
            checkExtraElements(clauseXMap, ELEMENT_TYPE, ELEMENT_FILTER);
        }
        if (preliminaryParsingOnly) {
            return null;
        } else {
            return TypeFilterImpl.createType(type, subFilter);
        }
    }

    private ExistsFilter parseExistsFilter(MapXNodeImpl clauseXMap, PrismContainerDefinition<?> pcd,
            boolean preliminaryParsingOnly, ParsingContext pc) throws SchemaException {
        ItemPath path = getPath(clauseXMap);
        XNodeImpl subXFilter = clauseXMap.get(ELEMENT_FILTER);
        ObjectFilter subFilter;
        PrismContainerDefinition<?> subPcd = pcd != null ? pcd.findContainerDefinition(path) : null;
        if (subXFilter instanceof MapXNodeImpl) {
            subFilter = parseFilterInternal((MapXNodeImpl) subXFilter, subPcd, preliminaryParsingOnly, pc);
        } else {
            subFilter = null;
        }
        if (!preliminaryParsingOnly || pc == null || pc.isStrict()) {
            // MID-3614: we want to be strict when importing but forgiving when reading from the repository
            checkExtraElements(clauseXMap, ELEMENT_PATH, ELEMENT_FILTER);
        }
        if (preliminaryParsingOnly) {
            return null;
        } else {
            return ExistsFilterImpl.createExists(path, pcd, subFilter);
        }
    }

    private void checkExtraElements(MapXNodeImpl clauseXMap, QName... expected) throws SchemaException {
        List<QName> expectedList = Arrays.asList(expected);
        for (Entry<QName, XNodeImpl> entry : clauseXMap.entrySet()) {
            if (!QNameUtil.contains(expectedList, entry.getKey())) {
                throw new SchemaException("Unexpected item " + entry.getKey() + ":" +
                        (entry.getValue() != null ? entry.getValue().debugDump() : "null"));
            }
        }
    }

    private <C extends Containerable> RefFilter parseRefFilter(MapXNodeImpl clauseXMap, PrismContainerDefinition<C> pcd,
            boolean preliminaryParsingOnly) throws SchemaException {
        ItemPath itemPath = getPath(clauseXMap);

        if (itemPath == null || itemPath.isEmpty()){
            throw new SchemaException("Cannot convert query, because query does not contain property path.");
        }
        QName itemName = ItemPath.toName(itemPath.last());

        ItemDefinition<?> itemDefinition = null;
        if (pcd != null) {
            itemDefinition = pcd.findItemDefinition(itemPath);
            if (itemDefinition == null && !preliminaryParsingOnly) {
                throw new SchemaException("No definition for item "+itemPath+" in "+pcd);
            }
        }

        // The reference definition in prism data might be single-valued. However, filters allow to look for multiple values.
        if (itemDefinition != null && itemDefinition.getMaxOccurs() != -1) {
            itemDefinition = itemDefinition.clone();
            itemDefinition.toMutable().setMaxOccurs(-1);
        }

        XNodeImpl valueXnode = clauseXMap.get(ELEMENT_VALUE);
        if (valueXnode != null && (!(valueXnode instanceof ListXNode) || !((ListXNode)valueXnode).isEmpty())) {
            if (preliminaryParsingOnly) {
                return null;
            }
            RootXNodeImpl valueRoot = new RootXNodeImpl(ELEMENT_VALUE, valueXnode);
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
            return RefFilterImpl.createReferenceEqual(itemPath, (PrismReferenceDefinition) itemDefinition, PrismValueCollectionsUtil.cloneCollection(ref.getValues()));
        } else {
            ExpressionWrapper expressionWrapper = parseExpression(clauseXMap);
            if (expressionWrapper != null) {
                if (preliminaryParsingOnly) {
                    return null;
                } else {
                    return RefFilterImpl.createReferenceEqual(itemPath,
                            (PrismReferenceDefinition) itemDefinition, expressionWrapper);
                }
            } else {
                if (preliminaryParsingOnly) {
                    return null;
                } else {
                    return RefFilterImpl.createReferenceEqual(itemPath,
                            (PrismReferenceDefinition) itemDefinition, (ExpressionWrapper) null);
                }
            }
        }
    }

    private ExpressionWrapper parseExpression(MapXNodeImpl xmap) throws SchemaException {
        Entry<QName, XNodeImpl> expressionEntry = xmap.getSingleEntryThatDoesNotMatch(
                ELEMENT_VALUE, ELEMENT_MATCHING, ELEMENT_ANCHOR_START, ELEMENT_ANCHOR_END, ELEMENT_PATH);
        return PrismUtilInternal.parseExpression(expressionEntry, prismContext);
    }

    private boolean getAnchorStart(MapXNodeImpl clauseXMap) throws SchemaException {
        Boolean anchorStart = clauseXMap.getParsedPrimitiveValue(ELEMENT_ANCHOR_START, DOMUtil.XSD_BOOLEAN);
        return anchorStart != null && anchorStart;
    }

    private boolean getAnchorEnd(MapXNodeImpl clauseXMap) throws SchemaException {
        Boolean anchorEnd = clauseXMap.getParsedPrimitiveValue(ELEMENT_ANCHOR_END, DOMUtil.XSD_BOOLEAN);
        return anchorEnd != null && anchorEnd;
    }

    private OrgFilter parseOrgFilter(MapXNodeImpl clauseXMap, boolean preliminaryParsingOnly) throws SchemaException {
        if (Boolean.TRUE.equals(clauseXMap.getParsedPrimitiveValue(ELEMENT_IS_ROOT, DOMUtil.XSD_BOOLEAN))) {
            // TODO check if other content is present
            if (preliminaryParsingOnly) {
                return null;
            } else {
                return OrgFilterImpl.createRootOrg();
            }
        }

        XNodeImpl xOrgRefNode = clauseXMap.get(ELEMENT_ORG_REF);
        if (xOrgRefNode == null) {
            throw new SchemaException("No organization reference defined in the search query.");
        }
        MapXNodeImpl xOrgRefMap = toMap(xOrgRefNode);
        String orgOid = xOrgRefMap.getParsedPrimitiveValue(ELEMENT_OID, DOMUtil.XSD_STRING);
        if (orgOid == null || StringUtils.isBlank(orgOid)) {
            throw new SchemaException("No oid attribute defined in the organization reference element.");
        }

        String scopeString = xOrgRefMap.getParsedPrimitiveValue(ELEMENT_SCOPE, DOMUtil.XSD_STRING);        // original (in my opinion incorrect) place
        if (scopeString == null) {
            scopeString = clauseXMap.getParsedPrimitiveValue(ELEMENT_SCOPE, DOMUtil.XSD_STRING);                // here it is placed by the serializer
        }
        Scope scope = scopeString != null ? Scope.valueOf(scopeString) : null;

        if (preliminaryParsingOnly) {
            return null;
        } else {
            return OrgFilterImpl.createOrg(orgOid, scope);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private Entry<QName, XNodeImpl> singleSubEntry(MapXNodeImpl xmap, String filterName) throws SchemaException {
        if (xmap == null || xmap.isEmpty()) {
            return null;
        }
        return xmap.getSingleSubEntry("search filter "+filterName);
    }

    private MapXNodeImpl toMap(XNode xnode) throws SchemaException {
        if (xnode instanceof PrimitiveXNodeImpl && xnode.isEmpty()) {
            return new MapXNodeImpl();          // hack but it might work (todo implement better)
        } else if (!(xnode instanceof MapXNodeImpl)) {
            throw new SchemaException("Cannot parse filter from "+xnode);
        } else {
            return (MapXNodeImpl) xnode;
        }
    }

    private ItemPath getPath(MapXNodeImpl clauseXMap) throws SchemaException {
        return getPath(clauseXMap, ELEMENT_PATH);
    }

    private ItemPath getPath(MapXNodeImpl clauseXMap, QName key) throws SchemaException {
        XNodeImpl xnode = clauseXMap.get(key);
        if (xnode == null) {
            return null;
        }
        if (!(xnode instanceof PrimitiveXNodeImpl<?>)) {
            throw new SchemaException("Expected that field "+key+" will be primitive, but it is "+xnode.getDesc());
        }
        ItemPathType itemPathType = clauseXMap.getParsedPrimitiveValue(key, ItemPathType.COMPLEX_TYPE);
        return itemPathType != null ? prismContext.toUniformPath(itemPathType) : null;
    }

    private QName getMatchingRule(MapXNodeImpl xmap) throws SchemaException{
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

    private Item<?,?> parseItem(RootXNodeImpl root, QName itemName, ItemDefinition<?> itemDefinition) throws SchemaException {
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

    private <C extends Containerable> ItemDefinition<?> locateItemDefinition(XNodeImpl valueXnode, ItemPath itemPath,
            PrismContainerDefinition<C> pcd) {
        if (pcd != null) {
            ItemDefinition<?> itemDefinition = pcd.findItemDefinition(itemPath);
            if (itemDefinition == null) {
                ItemPath rest = itemPath.rest();
                QName first = itemPath.firstToName();
                itemDefinition = ((PrismContextImpl) prismContext).getPrismUnmarshaller().locateItemDefinition(pcd, first, valueXnode);
                if (rest.isEmpty()) {
                    return itemDefinition;
                } else{
                    if (itemDefinition instanceof PrismContainerDefinition) {
                        return locateItemDefinition(valueXnode, rest, (PrismContainerDefinition<?>) itemDefinition);
                    }
                }
            }
            return itemDefinition;
        } else {
            return null;
        }
    }

    @Override
    public MapXNodeImpl serializeFilter(ObjectFilter filter) throws SchemaException{
        return serializeFilter(filter, prismContext.xnodeSerializer());
    }

    private MapXNodeImpl serializeFilter(ObjectFilter filter, PrismSerializer<RootXNode> xnodeSerializer) throws SchemaException{
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
                || filter instanceof LessFilter
                || filter instanceof SubstringFilter) {
            return serializeComparisonFilter((PropertyValueFilter<?>) filter, xnodeSerializer);
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


    private MapXNodeImpl serializeAndFilter(AndFilter filter, PrismSerializer<RootXNode> xnodeSerializer) throws SchemaException{
        return createFilter(CLAUSE_AND, serializeNaryLogicalSubFilters(filter.getConditions(), xnodeSerializer));
    }

    private MapXNodeImpl serializeOrFilter(OrFilter filter, PrismSerializer<RootXNode> xnodeSerializer) throws SchemaException{
        return createFilter(CLAUSE_OR, serializeNaryLogicalSubFilters(filter.getConditions(), xnodeSerializer));
    }

    private MapXNodeImpl serializeNaryLogicalSubFilters(List<ObjectFilter> objectFilters, PrismSerializer<RootXNode> xnodeSerializer) throws SchemaException{
        MapXNodeImpl filters = new MapXNodeImpl();
        for (ObjectFilter of : objectFilters) {
            MapXNodeImpl subFilter = serializeFilter(of, xnodeSerializer);
            filters.merge(subFilter);
        }
        return filters;
    }

    private MapXNodeImpl serializeNotFilter(NotFilter filter, PrismSerializer<RootXNode> xnodeSerializer) throws SchemaException{
        return createFilter(CLAUSE_NOT, serializeFilter(filter.getFilter(), xnodeSerializer));
    }

    @NotNull
    private MapXNodeImpl createFilter(QName clauseNot, MapXNodeImpl value) {
        MapXNodeImpl map = new MapXNodeImpl();
        map.put(clauseNot, value);
        return map;
    }

    private MapXNodeImpl serializeInOidFilter(InOidFilter filter) {
        MapXNodeImpl clauseMap = new MapXNodeImpl();
        if (filter.getOids() != null && !filter.getOids().isEmpty()) {
            ListXNodeImpl valuesNode = new ListXNodeImpl();
            for (String oid : filter.getOids()) {
                XNodeImpl val = createPrimitiveXNode(oid, DOMUtil.XSD_STRING);
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
            clauseMap.put(ELEMENT_CONSIDER_OWNER, new PrimitiveXNodeImpl<>(true));
        }

        return createFilter(CLAUSE_IN_OID, clauseMap);
    }

    private MapXNodeImpl serializeFullTextFilter(FullTextFilter filter) throws SchemaException {
        MapXNodeImpl clauseMap = new MapXNodeImpl();
        if (filter.getValues() != null && !filter.getValues().isEmpty()) {
            ListXNodeImpl valuesNode = new ListXNodeImpl();
            for (String value : filter.getValues()) {
                XNodeImpl val = createPrimitiveXNode(value, DOMUtil.XSD_STRING);
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

    private <T> MapXNodeImpl serializeComparisonFilter(PropertyValueFilter<T> filter, PrismSerializer<RootXNode> xnodeSerializer) throws SchemaException{
        MapXNodeImpl map = new MapXNodeImpl();
        QName clause;
        if (filter instanceof EqualFilter) {
            clause = CLAUSE_EQUAL;
        } else if (filter instanceof GreaterFilter) {
            clause = ((GreaterFilter<?>) filter).isEquals() ? CLAUSE_GREATER_OR_EQUAL : CLAUSE_GREATER;
        } else if (filter instanceof LessFilter) {
            clause = ((LessFilter<?>) filter).isEquals() ? CLAUSE_LESS_OR_EQUAL : CLAUSE_LESS;
        } else if (filter instanceof SubstringFilter) {
            clause = CLAUSE_SUBSTRING;
        } else {
            throw new IllegalStateException();
        }

        MapXNodeImpl content = serializeValueFilter(filter, xnodeSerializer);
        postProcessValueFilter(filter, content);

        map.put(clause, content);
        return map;
    }

    private <V extends PrismValue, D extends ItemDefinition<?>> MapXNodeImpl serializeValueFilter(ValueFilter<V,D> filter, PrismSerializer<RootXNode> xnodeSerializer) throws SchemaException {
        MapXNodeImpl map = new MapXNodeImpl();
        serializeMatchingRule(filter, map);
        serializePath(map, filter.getFullPath(), filter);

        List<V> values = filter.getValues();
        if (values != null) {
            ListXNodeImpl valuesNode = new ListXNodeImpl();
            for (V val : values) {
                if (val.getParent() == null) {
                    val.setParent(filter);
                }
                XNodeImpl valNode = (XNodeImpl) xnodeSerializer.definition(filter.getDefinition()).serialize(val).getSubnode();
                if (filter instanceof RefFilter) {        // TODO shouldn't we do this in all cases?
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
            map.put(ELEMENT_RIGHT_HAND_SIDE_PATH, createPrimitiveXNode(
                    new ItemPathType(filter.getRightHandSidePath()), ItemPathType.COMPLEX_TYPE));
        }

        ExpressionWrapper expression = filter.getExpression();
        if (expression != null) {
            map.merge(PrismUtilInternal.serializeExpression(expression, xnodeSerializer));
        }

        return map;
    }

    private <T> void postProcessValueFilter(PropertyValueFilter<T> filter, MapXNodeImpl mapXNode) {
        if (filter instanceof SubstringFilter) {
            if (((SubstringFilter<?>) filter).isAnchorStart()) {
                mapXNode.put(ELEMENT_ANCHOR_START, new PrimitiveXNodeImpl<>(true));
            }
            if (((SubstringFilter<?>) filter).isAnchorEnd()) {
                mapXNode.put(ELEMENT_ANCHOR_END, new PrimitiveXNodeImpl<>(true));
            }
        }
    }

    private MapXNodeImpl serializeRefFilter(RefFilter filter, PrismSerializer<RootXNode> xnodeSerializer) throws SchemaException {
        return createFilter(CLAUSE_REF, serializeValueFilter(filter, xnodeSerializer));
    }

    private MapXNodeImpl serializeTypeFilter(TypeFilter filter, PrismSerializer<RootXNode> xnodeSerializer) throws SchemaException{
        MapXNodeImpl content = new MapXNodeImpl();
        content.put(ELEMENT_TYPE, createPrimitiveXNode(filter.getType(), DOMUtil.XSD_QNAME));
        if (filter.getFilter() != null){
            content.put(ELEMENT_FILTER, serializeFilter(filter.getFilter(), xnodeSerializer));
        }
        return createFilter(CLAUSE_TYPE, content);
    }

    private MapXNodeImpl serializeExistsFilter(ExistsFilter filter, PrismSerializer<RootXNode> xnodeSerializer) throws SchemaException{
        MapXNodeImpl content = new MapXNodeImpl();
        serializePath(content, filter.getFullPath(), filter);
        if (filter.getFilter() != null){
            content.put(ELEMENT_FILTER, serializeFilter(filter.getFilter(), xnodeSerializer));
        }
        return createFilter(CLAUSE_EXISTS, content);
    }

    private MapXNodeImpl serializeOrgFilter(OrgFilter filter) {
        MapXNodeImpl map = new MapXNodeImpl();

        if (filter.getOrgRef() != null) {
            MapXNodeImpl orgRefMap = new MapXNodeImpl();
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

    private MapXNodeImpl serializeAllFilter() {
        return createFilter(CLAUSE_ALL, new MapXNodeImpl());
    }

    private MapXNodeImpl serializeNoneFilter() {
        return createFilter(CLAUSE_NONE, new MapXNodeImpl());
    }

    private MapXNodeImpl serializeUndefinedFilter() {
        return createFilter(CLAUSE_UNDEFINED, new MapXNodeImpl());
    }

    private void serializeMatchingRule(ValueFilter<?,?> filter, MapXNodeImpl map) {
        if (filter.getMatchingRule() != null){
            PrimitiveXNodeImpl<String> matchingNode = createPrimitiveXNode(filter.getMatchingRule().getLocalPart(), DOMUtil.XSD_STRING);
            map.put(ELEMENT_MATCHING, matchingNode);
        }
    }

    private void serializePath(MapXNodeImpl map, ItemPath path, ObjectFilter filter) {
        if (path == null) {
            throw new IllegalStateException("Cannot serialize filter " + filter + " because it does not contain path");
        }
        map.put(ELEMENT_PATH, createPrimitiveXNode(new ItemPathType(path), ItemPathType.COMPLEX_TYPE));
    }

    private <T> PrimitiveXNodeImpl<T> createPrimitiveXNode(T val, QName type) {
        PrimitiveXNodeImpl<T> xprim = new PrimitiveXNodeImpl<>();
        xprim.setValue(val, type);
        return xprim;
    }

    /**
     * Tries to parse as much from filter as possible, without knowing the definition of object(s) to which the
     * filter will be applied. It is used mainly to parse path specifications, in order to avoid namespace loss
     * when serializing raw (unparsed) paths and QNames - see MID-1969.
     */
    @Override
    public void parseFilterPreliminarily(MapXNode xFilter, ParsingContext pc) throws SchemaException {
        parseFilterInternal((MapXNodeImpl) xFilter, null, true, pc);
    }

    @Override
    public <C extends Containerable> ObjectQuery createObjectQuery(Class<C> clazz, QueryType queryType)
            throws SchemaException {
        if (queryType == null) {
            return null;
        }
        return createObjectQueryInternal(clazz, queryType.getFilter(), queryType.getPaging());
    }

    @Override
    public <C extends Containerable> ObjectQuery createObjectQuery(Class<C> clazz, SearchFilterType filterType)
            throws SchemaException {
        return createObjectQueryInternal(clazz, filterType, null);
    }

    @Override
    public <C extends Containerable> ObjectFilter createObjectFilter(Class<C> clazz, SearchFilterType filterType)
            throws SchemaException {
        ObjectQuery query = createObjectQueryInternal(clazz, filterType, null);
        return query.getFilter();
    }

    @Override
    public <C extends Containerable> ObjectFilter createObjectFilter(PrismContainerDefinition<C> containerDefinition,
            SearchFilterType filterType)
            throws SchemaException {
        ObjectQuery query = createObjectQueryInternal(containerDefinition, filterType, null);
        return query.getFilter();
    }

    @NotNull
    private <O extends Containerable> ObjectQuery createObjectQueryInternal(Class<O> clazz, SearchFilterType filterType,
            PagingType pagingType)
            throws SchemaException {
        PrismContainerDefinition<O> objDef = prismContext.getSchemaRegistry().findContainerDefinitionByCompileTimeClass(clazz);
        if (objDef == null) {
            throw new SchemaException("cannot find obj/container definition for class "+clazz);
        } else {
            return createObjectQueryInternal(objDef, filterType, pagingType);
        }
    }

    @NotNull
    private <O extends Containerable> ObjectQuery createObjectQueryInternal(PrismContainerDefinition<O> objDef,
            SearchFilterType filterType, PagingType pagingType)
            throws SchemaException {

        try {
            ObjectQuery query = ObjectQueryImpl.createObjectQuery();

            if (filterType != null && filterType.containsFilterClause()) {
                MapXNodeImpl rootFilter = (MapXNodeImpl) filterType.getFilterClauseXNode();
                ObjectFilter filter = parseFilter(rootFilter, objDef);
                query.setFilter(filter);
            }

            if (pagingType != null) {
                ObjectPaging paging = PagingConvertor.createObjectPaging(pagingType, prismContext);
                query.setPaging(paging);
            }
            return query;
        } catch (SchemaException ex) {
            throw new SchemaException("Failed to convert query. Reason: " + ex.getMessage(), ex);
        }

    }

    @Override
    public QueryType createQueryType(ObjectQuery query) throws SchemaException {
        if (query == null) {
            return null;
        }
        ObjectFilter filter = query.getFilter();
        QueryType queryType = new QueryType();
        if (filter != null) {
            queryType.setFilter(createSearchFilterType(filter));
        }
        queryType.setPaging(PagingConvertor.createPagingType(query.getPaging()));
        return queryType;
    }

    @Override
    public SearchFilterType createSearchFilterType(ObjectFilter filter) throws SchemaException {
        if (filter == null) {
            return null;
        }
        SearchFilterType filterType = new SearchFilterType();
        MapXNodeImpl filterXNode = serializeFilter(filter);
        filterType.setFilterClauseXNode(filterXNode);
        return filterType;
    }

}
