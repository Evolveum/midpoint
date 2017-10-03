/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism.query.builder;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * EXPERIMENTAL IMPLEMENTATION.
 *
 * @author mederly
 */
public class R_Filter implements S_FilterEntryOrEmpty, S_AtomicFilterExit {

    final private QueryBuilder queryBuilder;
    final private Class<? extends Containerable> currentClass;          // object we are working on (changes on Exists filter)
    final private OrFilter currentFilter;
    final private LogicalSymbol lastLogicalSymbol;
    final private boolean isNegated;
    final private R_Filter parentFilter;
    final private QName typeRestriction;
    final private ItemPath existsRestriction;
    final private List<ObjectOrdering> orderingList;
    final private List<ObjectGrouping> groupingList;
    final private Integer offset;
    final private Integer maxSize;

    public R_Filter(QueryBuilder queryBuilder) {
        this.queryBuilder = queryBuilder;
        this.currentClass = queryBuilder.getQueryClass();
        this.currentFilter = OrFilter.createOr();
        this.lastLogicalSymbol = null;
        this.isNegated = false;
        this.parentFilter = null;
        this.typeRestriction = null;
        this.existsRestriction = null;
        this.orderingList = new ArrayList<>();
        this.groupingList = new ArrayList<>();
        this.offset = null;
        this.maxSize = null;
    }

    private R_Filter(QueryBuilder queryBuilder, Class<? extends Containerable> currentClass, OrFilter currentFilter, LogicalSymbol lastLogicalSymbol,
                     boolean isNegated, R_Filter parentFilter, QName typeRestriction, ItemPath existsRestriction, List<ObjectOrdering> orderingList, List<ObjectGrouping> groupingList, Integer offset, Integer maxSize) {
        this.queryBuilder = queryBuilder;
        this.currentClass = currentClass;
        this.currentFilter = currentFilter;
        this.lastLogicalSymbol = lastLogicalSymbol;
        this.isNegated = isNegated;
        this.parentFilter = parentFilter;
        this.typeRestriction = typeRestriction;
        this.existsRestriction = existsRestriction;
        if (orderingList != null) {
            this.orderingList = orderingList;
        } else {
            this.orderingList = new ArrayList<>();
        }
        if (groupingList != null) {
            this.groupingList = groupingList;
        } else {
            this.groupingList = new ArrayList<>();
        }
        this.offset = offset;
        this.maxSize = maxSize;
    }

    public static S_FilterEntryOrEmpty create(QueryBuilder builder) {
        return new R_Filter(builder);
    }

    // subfilter might be null
    R_Filter addSubfilter(ObjectFilter subfilter) {
        if (!currentFilter.isEmpty() && lastLogicalSymbol == null) {
            throw new IllegalStateException("lastLogicalSymbol is empty but there is already some filter present: " + currentFilter);
        }
        if (typeRestriction != null && existsRestriction != null) {
            throw new IllegalStateException("Both type and exists restrictions present");
        }
        if (typeRestriction != null) {
            if (!currentFilter.isEmpty()) {
                throw new IllegalStateException("Type restriction with 2 filters?");
            }
            if (isNegated) {
                subfilter = new NotFilter(subfilter);
            }
            return parentFilter.addSubfilter(TypeFilter.createType(typeRestriction, subfilter));
        } else if (existsRestriction != null) {
            if (!currentFilter.isEmpty()) {
                throw new IllegalStateException("Exists restriction with 2 filters?");
            }
            if (isNegated) {
                subfilter = new NotFilter(subfilter);
            }
            return parentFilter.addSubfilter(
                    ExistsFilter.createExists(
                            existsRestriction,
                            parentFilter.currentClass,
                            queryBuilder.getPrismContext(),
                            subfilter));
        } else {
            OrFilter newFilter = appendAtomicFilter(subfilter, isNegated, lastLogicalSymbol);
            return new R_Filter(queryBuilder, currentClass, newFilter, null, false, parentFilter, typeRestriction, existsRestriction, orderingList, groupingList, offset, maxSize);
        }
    }

    private OrFilter appendAtomicFilter(ObjectFilter subfilter, boolean negated, LogicalSymbol logicalSymbol) {
        if (negated) {
            subfilter = new NotFilter(subfilter);
        }
        OrFilter updatedFilter = currentFilter.clone();
        if (logicalSymbol == null || logicalSymbol == LogicalSymbol.OR) {
            updatedFilter.addCondition(AndFilter.createAnd(subfilter));
        } else if (logicalSymbol == LogicalSymbol.AND) {
            ((AndFilter) updatedFilter.getLastCondition()).addCondition(subfilter);
        } else {
            throw new IllegalStateException("Unknown logical symbol: " + logicalSymbol);
        }
        return updatedFilter;
    }

    private R_Filter setLastLogicalSymbol(LogicalSymbol newLogicalSymbol) {
        if (this.lastLogicalSymbol != null) {
            throw new IllegalStateException("Two logical symbols in a sequence");
        }
        return new R_Filter(queryBuilder, currentClass, currentFilter, newLogicalSymbol, isNegated, parentFilter, typeRestriction, existsRestriction, orderingList, groupingList, offset, maxSize);
    }

    private R_Filter setNegated() {
        if (isNegated) {
            throw new IllegalStateException("Double negation");
        }
        return new R_Filter(queryBuilder, currentClass, currentFilter, lastLogicalSymbol, true, parentFilter, typeRestriction, existsRestriction, orderingList, groupingList, offset, maxSize);
    }

    private R_Filter addOrdering(ObjectOrdering ordering) {
        Validate.notNull(ordering);
        List<ObjectOrdering> newList = new ArrayList<>(orderingList);
        newList.add(ordering);
        return new R_Filter(queryBuilder, currentClass, currentFilter, lastLogicalSymbol, isNegated, parentFilter, typeRestriction, existsRestriction, newList, groupingList, offset, maxSize);
    }

    private R_Filter addGrouping(ObjectGrouping grouping) {
        Validate.notNull(grouping);
        List<ObjectGrouping> newList = new ArrayList<>(groupingList);
        newList.add(grouping);
        return new R_Filter(queryBuilder, currentClass, currentFilter, lastLogicalSymbol, isNegated, parentFilter, typeRestriction, existsRestriction, orderingList, newList, offset, maxSize);
    }

    private R_Filter setOffset(Integer n) {
        return new R_Filter(queryBuilder, currentClass, currentFilter, lastLogicalSymbol, isNegated, parentFilter, typeRestriction, existsRestriction, orderingList, groupingList, n, maxSize);
    }

    private R_Filter setMaxSize(Integer n) {
        return new R_Filter(queryBuilder, currentClass, currentFilter, lastLogicalSymbol, isNegated, parentFilter, typeRestriction, existsRestriction, orderingList, groupingList, offset, n);
    }

    @Override
    public S_AtomicFilterExit all() {
        return addSubfilter(AllFilter.createAll());
    }

    @Override
    public S_AtomicFilterExit none() {
        return addSubfilter(NoneFilter.createNone());
    }

    @Override
    public S_AtomicFilterExit undefined() {
        return addSubfilter(UndefinedFilter.createUndefined());
    }
    // TODO .............................................

    @Override
    public S_AtomicFilterExit id(String... identifiers) {
        return addSubfilter(InOidFilter.createInOid(identifiers));
    }

    @Override
    public S_AtomicFilterExit id(long... identifiers) {
        List<String> ids = longsToStrings(identifiers);
        return addSubfilter(InOidFilter.createInOid(ids));
    }

    private List<String> longsToStrings(long[] identifiers) {
        List<String> ids = new ArrayList<>(identifiers.length);
        for (long id : identifiers) {
            ids.add(String.valueOf(id));
        }
        return ids;
    }

    @Override
    public S_AtomicFilterExit ownerId(String... identifiers) {
        return addSubfilter(InOidFilter.createOwnerHasOidIn(identifiers));
    }

    @Override
    public S_AtomicFilterExit ownerId(long... identifiers) {
        return addSubfilter(InOidFilter.createOwnerHasOidIn(longsToStrings(identifiers)));
    }

    @Override
    public S_AtomicFilterExit isDirectChildOf(PrismReferenceValue value) {
        OrgFilter orgFilter = OrgFilter.createOrg(value, OrgFilter.Scope.ONE_LEVEL);
        return addSubfilter(orgFilter);
    }

    @Override
    public S_AtomicFilterExit isChildOf(PrismReferenceValue value) {
        OrgFilter orgFilter = OrgFilter.createOrg(value, OrgFilter.Scope.SUBTREE);
        return addSubfilter(orgFilter);
    }

    @Override
    public S_AtomicFilterExit isParentOf(PrismReferenceValue value) {
        OrgFilter orgFilter = OrgFilter.createOrg(value, OrgFilter.Scope.ANCESTORS);
        return addSubfilter(orgFilter);
    }

    @Override
    public S_AtomicFilterExit isDirectChildOf(String oid) {
        OrgFilter orgFilter = OrgFilter.createOrg(oid, OrgFilter.Scope.ONE_LEVEL);
        return addSubfilter(orgFilter);
    }

    @Override
    public S_AtomicFilterExit isChildOf(String oid) {
        OrgFilter orgFilter = OrgFilter.createOrg(oid, OrgFilter.Scope.SUBTREE);
        return addSubfilter(orgFilter);
    }

    @Override
    public S_AtomicFilterExit isInScopeOf(String oid, OrgFilter.Scope scope) {
        return addSubfilter(OrgFilter.createOrg(oid, scope));
    }

    @Override
    public S_AtomicFilterExit isInScopeOf(PrismReferenceValue value, OrgFilter.Scope scope) {
        return addSubfilter(OrgFilter.createOrg(value, scope));
    }

    @Override
    public S_AtomicFilterExit isParentOf(String oid) {
        OrgFilter orgFilter = OrgFilter.createOrg(oid, OrgFilter.Scope.ANCESTORS);
        return addSubfilter(orgFilter);
    }

    @Override
    public S_AtomicFilterExit isRoot() {
        OrgFilter orgFilter = OrgFilter.createRootOrg();
        return addSubfilter(orgFilter);
    }

    @Override
    public S_AtomicFilterExit fullText(String... words) {
        FullTextFilter fullTextFilter = FullTextFilter.createFullText(words);
        return addSubfilter(fullTextFilter);
    }

    @Override
    public S_FilterEntryOrEmpty block() {
        return new R_Filter(queryBuilder, currentClass, OrFilter.createOr(), null, false, this, null, null, null, null, null, null);
    }

    @Override
    public S_FilterEntryOrEmpty type(Class<? extends Containerable> type) {
        ComplexTypeDefinition ctd = queryBuilder.getPrismContext().getSchemaRegistry().findComplexTypeDefinitionByCompileTimeClass(type);
        if (ctd == null) {
            throw new IllegalArgumentException("Unknown type: " + type);
        }
        QName typeName = ctd.getTypeName();
        if (typeName == null) {
            throw new IllegalStateException("No type name for " + ctd);
        }
        return new R_Filter(queryBuilder, type, OrFilter.createOr(), null, false, this, typeName, null, null, null, null, null);
    }

    @Override
    public S_FilterEntryOrEmpty exists(QName... names) {
        if (existsRestriction != null) {
            throw new IllegalStateException("Exists within exists");
        }
        if (names.length == 0) {
            throw new IllegalArgumentException("Empty path in exists() filter is not allowed.");
        }
        ItemPath existsPath = new ItemPath(names);
        PrismContainerDefinition pcd = resolveItemPath(existsPath, PrismContainerDefinition.class);
        Class<? extends Containerable> clazz = pcd.getCompileTimeClass();
        if (clazz == null) {
            throw new IllegalArgumentException("Item path of '" + existsPath + "' in " + currentClass + " does not point to a valid prism container.");
        }
        return new R_Filter(queryBuilder, clazz, OrFilter.createOr(), null, false, this, null, existsPath, null, null,null, null);
    }

    <ID extends ItemDefinition> ID resolveItemPath(ItemPath itemPath, Class<ID> type) {
        Validate.notNull(type, "type");
        ComplexTypeDefinition ctd = queryBuilder.getPrismContext().getSchemaRegistry().findComplexTypeDefinitionByCompileTimeClass(currentClass);
        if (ctd == null) {
            throw new IllegalArgumentException("Definition for " + currentClass + " couldn't be found.");
        }
        ID definition = ctd.findItemDefinition(itemPath, type);
        if (definition == null) {
            throw new IllegalArgumentException("Item path of '" + itemPath + "' in " + currentClass + " does not point to a valid " + type.getSimpleName());
        }
        return definition;
    }

    // END OF TODO .............................................

    @Override
    public S_FilterEntry and() {
        return setLastLogicalSymbol(LogicalSymbol.AND);
    }

    @Override
    public S_FilterEntry or() {
        return setLastLogicalSymbol(LogicalSymbol.OR);
    }

    @Override
    public S_AtomicFilterEntry not() {
        return setNegated();
    }

    @Override
    public S_ConditionEntry item(QName... names) {
        return item(new ItemPath(names));
    }

    @Override
    public S_ConditionEntry item(ItemPath itemPath) {
        ItemDefinition itemDefinition = resolveItemPath(itemPath, ItemDefinition.class);
        return item(itemPath, itemDefinition);
    }

    @Override
    public S_ConditionEntry itemWithDef(ItemDefinition itemDefinition, QName... names) {
        ItemPath itemPath = new ItemPath(names);
        return item(itemPath, itemDefinition);
    }

    @Override
    public S_ConditionEntry item(ItemPath itemPath, ItemDefinition itemDefinition) {
        return R_AtomicFilter.create(itemPath, itemDefinition, this);
    }

    @Override
    public S_ConditionEntry item(PrismContainerDefinition containerDefinition, QName... names) {
        return item(containerDefinition, new ItemPath(names));
    }

    @Override
    public S_ConditionEntry item(PrismContainerDefinition containerDefinition, ItemPath itemPath) {
        ItemDefinition itemDefinition = containerDefinition.findItemDefinition(itemPath);
        if (itemDefinition == null) {
            throw new IllegalArgumentException("No definition of " + itemPath + " in " + containerDefinition);
        }
        return item(itemPath, itemDefinition);
    }

    @Override
    public S_MatchingRuleEntry itemAs(PrismProperty<?> property) {
        return item(property.getPath(), property.getDefinition()).eq(property);
    }

    @Override
    public S_AtomicFilterExit endBlock() {
        if (parentFilter == null) {
            throw new IllegalStateException("endBlock() call without preceding block() one");
        }
        if (currentFilter != null || parentFilter.hasRestriction()) {
            ObjectFilter simplified = simplify(currentFilter);
            if (simplified != null || parentFilter.hasRestriction()) {
                return parentFilter.addSubfilter(simplified);
            }
        }
        return parentFilter;
    }

    private boolean hasRestriction() {
        return existsRestriction != null || typeRestriction != null;
    }

    @Override
    public S_FilterExit asc(QName... names) {
        if (names.length == 0) {
            throw new IllegalArgumentException("There must be at least one name for asc(...) ordering");
        }
        return addOrdering(ObjectOrdering.createOrdering(new ItemPath(names), OrderDirection.ASCENDING));
    }

    @Override
    public S_FilterExit asc(ItemPath path) {
        if (ItemPath.isNullOrEmpty(path)) {
            throw new IllegalArgumentException("There must be non-empty path for asc(...) ordering");
        }
        return addOrdering(ObjectOrdering.createOrdering(path, OrderDirection.ASCENDING));
    }

    @Override
    public S_FilterExit desc(QName... names) {
        if (names.length == 0) {
            throw new IllegalArgumentException("There must be at least one name for asc(...) ordering");
        }
        return addOrdering(ObjectOrdering.createOrdering(new ItemPath(names), OrderDirection.DESCENDING));
    }

    @Override
    public S_FilterExit desc(ItemPath path) {
        if (ItemPath.isNullOrEmpty(path)) {
            throw new IllegalArgumentException("There must be non-empty path for desc(...) ordering");
        }
        return addOrdering(ObjectOrdering.createOrdering(path, OrderDirection.DESCENDING));
    }

    @Override
    public S_FilterExit group(QName... names) {
        if (names.length == 0) {
            throw new IllegalArgumentException("There must be at least one name for uniq(...) grouping");
        }
        return addGrouping(ObjectGrouping.createGrouping(new ItemPath(names)));
    }

    @Override
    public S_FilterExit group(ItemPath path) {
        if (ItemPath.isNullOrEmpty(path)) {
            throw new IllegalArgumentException("There must be non-empty path for uniq(...) grouping");
        }
        return addGrouping(ObjectGrouping.createGrouping(path));
    }

    @Override
    public S_FilterExit offset(Integer n) {
        return setOffset(n);
    }

    @Override
    public S_FilterExit maxSize(Integer n) {
        return setMaxSize(n);
    }

    @Override
    public ObjectQuery build() {
        if (typeRestriction != null || existsRestriction != null) {
            // unfinished empty type restriction or exists restriction
            return addSubfilter(null).build();
        }
        if (parentFilter != null) {
            throw new IllegalStateException("A block in filter definition was probably not closed.");
        }
        ObjectPaging paging = null;
        if (!orderingList.isEmpty()) {
            paging = createIfNeeded(null);
            paging.setOrdering(orderingList);
        }
        if (offset != null) {
            paging = createIfNeeded(paging);
            paging.setOffset(offset);
        }
        if (maxSize != null) {
            paging = createIfNeeded(paging);
            paging.setMaxSize(maxSize);
        }
        return ObjectQuery.createObjectQuery(simplify(currentFilter), paging);
    }

    private ObjectPaging createIfNeeded(ObjectPaging paging) {
        return paging != null ? paging : ObjectPaging.createEmptyPaging();
    }

    @Override
    public ObjectFilter buildFilter() {
        return build().getFilter();
    }

    private ObjectFilter simplify(OrFilter filter) {

        if (filter == null) {
            return null;
        }

        OrFilter simplified = OrFilter.createOr();

        // step 1 - simplify conjunctions
        for (ObjectFilter condition : filter.getConditions()) {
            AndFilter conjunction = (AndFilter) condition;
            if (conjunction.getConditions().size() == 1) {
                simplified.addCondition(conjunction.getLastCondition());
            } else {
                simplified.addCondition(conjunction);
            }
        }

        // step 2 - simplify disjunction
        if (simplified.getConditions().size() == 0) {
            return null;
        } else if (simplified.getConditions().size() == 1) {
            return simplified.getLastCondition();
        } else {
            return simplified;
        }
    }

    public PrismContext getPrismContext() {
        return queryBuilder.getPrismContext();
    }
}
