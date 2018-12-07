/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.prism.query;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.prism.xnode.XNodeImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;

/**
 *  TODO remove prism context parameter from the methods + set it in all filters created
 */
public class QueryFactoryImpl implements QueryFactory {

	@NotNull private final PrismContextImpl prismContext;

	public QueryFactoryImpl(@NotNull PrismContextImpl prismContext) {
		this.prismContext = prismContext;
	}

	@Override
	public AllFilter createAll() {
		return AllFilterImpl.createAll();
	}

	@Override
	public NoneFilter createNone() {
		return NoneFilterImpl.createNone();
	}

	@Override
	public ObjectFilter createUndefined() {
		return UndefinedFilterImpl.createUndefined();
	}

	@NotNull
	@Override
	public <T> EqualFilter<T> createEqual(@NotNull ItemPath path, @Nullable PrismPropertyDefinition<T> definition,
			@Nullable QName matchingRule) {
		return EqualFilterImpl.createEqual(path, definition, matchingRule);
	}

	// values
	@NotNull
	@Override
	public <T> EqualFilter<T> createEqual(@NotNull ItemPath path, @Nullable PrismPropertyDefinition<T> definition,
			@Nullable QName matchingRule, @NotNull PrismContext prismContext, Object... values) {
		return EqualFilterImpl.createEqual(path, definition, matchingRule, prismContext, values);
	}

	// expression-related
	@NotNull
	@Override
	public <T> EqualFilter<T> createEqual(@NotNull ItemPath path, @Nullable PrismPropertyDefinition<T> definition,
			@Nullable QName matchingRule, @NotNull ExpressionWrapper expression) {
		return EqualFilterImpl.createEqual(path, definition, matchingRule, expression);
	}

	// right-side-related; right side can be supplied later (therefore it's nullable)
	@NotNull
	@Override
	public <T> EqualFilter<T> createEqual(@NotNull ItemPath path, PrismPropertyDefinition<T> definition,
			QName matchingRule, @NotNull ItemPath rightSidePath, ItemDefinition rightSideDefinition) {
		return EqualFilterImpl.createEqual(path, definition, matchingRule, rightSidePath, rightSideDefinition);
	}

	@Override
	@NotNull
	public RefFilter createReferenceEqual(ItemPath path, PrismReferenceDefinition definition,
			Collection<PrismReferenceValue> values) {
		return RefFilterImpl.createReferenceEqual(path, definition, values);
	}

	@Override
	@NotNull
	public RefFilter createReferenceEqual(ItemPath path, PrismReferenceDefinition definition, ExpressionWrapper expression) {
		return RefFilterImpl.createReferenceEqual(path, definition, expression);
	}

	// empty (can be filled-in later)
	@NotNull
	@Override
	public <T> GreaterFilter<T> createGreater(@NotNull ItemPath path, PrismPropertyDefinition<T> definition, boolean equals) {
		return GreaterFilterImpl.createGreater(path, definition, equals);
	}

	// value
	@NotNull
	@Override
	public <T> GreaterFilter<T> createGreater(@NotNull ItemPath path, PrismPropertyDefinition<T> definition,
			QName matchingRule, Object value, boolean equals, @NotNull PrismContext prismContext) {
		return GreaterFilterImpl.createGreater(path, definition, matchingRule, value, equals, prismContext);
	}

	// expression-related
	@NotNull
	@Override
	public <T> GreaterFilter<T> createGreater(@NotNull ItemPath path, PrismPropertyDefinition<T> definition, QName matchingRule,
			@NotNull ExpressionWrapper wrapper, boolean equals) {
		return GreaterFilterImpl.createGreater(path, definition, matchingRule, wrapper, equals);
	}

	// right-side-related
	@NotNull
	@Override
	public <T> GreaterFilter<T> createGreater(@NotNull ItemPath path, PrismPropertyDefinition<T> definition, QName matchingRule,
			@NotNull ItemPath rightSidePath, ItemDefinition rightSideDefinition, boolean equals) {
		return GreaterFilterImpl.createGreater(path, definition, matchingRule, rightSidePath, rightSideDefinition, equals);
	}

	// empty (can be filled-in later)
	@Override
	@NotNull
	public <T> LessFilter<T> createLess(@NotNull ItemPath path, PrismPropertyDefinition<T> definition, boolean equals) {
		return LessFilterImpl.createLess(path, definition, equals);
	}

	// value
	@Override
	@NotNull
	public <T> LessFilter<T> createLess(@NotNull ItemPath path, PrismPropertyDefinition<T> definition,
			QName matchingRule, Object value, boolean equals, @NotNull PrismContext prismContext) {
		return LessFilterImpl.createLess(path, definition, matchingRule, value, equals, prismContext);
	}

	// expression-related
	@Override
	@NotNull
	public <T> LessFilter<T> createLess(@NotNull ItemPath path, PrismPropertyDefinition<T> definition, QName matchingRule,
			@NotNull ExpressionWrapper expressionWrapper, boolean equals) {
		return LessFilterImpl.createLess(path, definition, matchingRule, expressionWrapper, equals);
	}

	// right-side-related
	@Override
	@NotNull
	public <T> LessFilter<T> createLess(@NotNull ItemPath path, PrismPropertyDefinition<T> definition,
			QName matchingRule, @NotNull ItemPath rightSidePath, ItemDefinition rightSideDefinition, boolean equals) {
		return LessFilterImpl.createLess(path, definition, matchingRule, rightSidePath, rightSideDefinition, equals);
	}

	@NotNull
	@Override
	public AndFilter createAnd(ObjectFilter... conditions){
		return AndFilterImpl.createAnd(conditions);
	}

	@NotNull
	@Override
	public AndFilter createAnd(List<ObjectFilter> conditions){
		return AndFilterImpl.createAnd(conditions);
	}

	@NotNull
	@Override
	public OrFilter createOr(ObjectFilter... conditions) {
		return OrFilterImpl.createOr(conditions);
	}

	@NotNull
	@Override
	public OrFilter createOr(List<ObjectFilter> conditions) {
		return OrFilterImpl.createOr(conditions);
	}

	@NotNull
	@Override
	public NotFilter createNot(ObjectFilter inner) {
		return NotFilterImpl.createNot(inner);
	}

	@NotNull
	@Override
	public <C extends Containerable> ExistsFilter createExists(ItemName path, Class<C> containerType, PrismContext prismContext,
			ObjectFilter inner) {
		return ExistsFilterImpl.createExists(path, containerType, prismContext, inner);
	}

	@NotNull
	@Override
	public InOidFilter createInOid(Collection<String> oids) {
		return InOidFilterImpl.createInOid(oids);
	}

	@NotNull
	@Override
	public InOidFilter createInOid(String... oids) {
		return InOidFilterImpl.createInOid(oids);
	}

	@NotNull
	@Override
	public InOidFilter createOwnerHasOidIn(Collection<String> oids) {
		return InOidFilterImpl.createOwnerHasOidIn(oids);
	}

	@NotNull
	@Override
	public InOidFilter createOwnerHasOidIn(String... oids) {
		return InOidFilterImpl.createOwnerHasOidIn(oids);
	}

	@Override
	@NotNull
	public OrgFilter createOrg(PrismReferenceValue baseOrgRef, OrgFilter.Scope scope) {
		return OrgFilterImpl.createOrg(baseOrgRef, scope);
	}

	@Override
	@NotNull
	public OrgFilter createOrg(String baseOrgOid, OrgFilter.Scope scope) {
		return OrgFilterImpl.createOrg(baseOrgOid, scope);
	}

	@Override
	@NotNull
	public OrgFilter createRootOrg() {
		return OrgFilterImpl.createRootOrg();
	}

	@Override
	@NotNull
	public TypeFilter createType(QName type, ObjectFilter filter) {
		return new TypeFilterImpl(type, filter);
	}

	@Override
	@NotNull
	public ObjectOrdering createOrdering(ItemPath orderBy, OrderDirection direction) {
		return ObjectOrderingImpl.createOrdering(orderBy, direction);
	}

	@Override
	@NotNull
	public ObjectPaging createPaging(Integer offset, Integer maxSize){
		return ObjectPagingImpl.createPaging(offset, maxSize);
	}

	@Override
	@NotNull
	public ObjectPaging createPaging(Integer offset, Integer maxSize, ItemPath orderBy, OrderDirection direction) {
		return ObjectPagingImpl.createPaging(offset, maxSize, orderBy, direction);
	}

	@Override
	@NotNull
	public ObjectPaging createPaging(Integer offset, Integer maxSize, ItemPath groupBy) {
		return ObjectPagingImpl.createPaging(offset, maxSize, groupBy);
	}

	@Override
	@NotNull
	public ObjectPaging createPaging(Integer offset, Integer maxSize, ItemPath orderBy, OrderDirection direction,
			ItemPath groupBy) {
		return ObjectPagingImpl.createPaging(offset, maxSize, orderBy, direction, groupBy);
	}

	@Override
	@NotNull
	public ObjectPaging createPaging(Integer offset, Integer maxSize, List<ObjectOrdering> orderings) {
		return ObjectPagingImpl.createPaging(offset, maxSize, orderings);
	}

	@Override
	@NotNull
	public ObjectPaging createPaging(Integer offset, Integer maxSize, List<ObjectOrdering> orderings,
			List<ObjectGrouping> groupings) {
		return ObjectPagingImpl.createPaging(offset, maxSize, orderings, groupings);
	}

	@Override
	@NotNull
	public ObjectPaging createPaging(ItemPath orderBy, OrderDirection direction) {
		return ObjectPagingImpl.createPaging(orderBy, direction);
	}

	@Override
	@NotNull
	public ObjectPaging createPaging(ItemPath orderBy, OrderDirection direction, ItemPath groupBy) {
		return ObjectPagingImpl.createPaging(orderBy, direction, groupBy);
	}

	@Override
	@NotNull
	public ObjectPaging createPaging(ItemPath groupBy) {
		return ObjectPagingImpl.createPaging(groupBy);
	}

	@Override
	@NotNull
	public ObjectPaging createPaging() {
		return ObjectPagingImpl.createEmptyPaging();
	}

	@Override
	@NotNull
	public ObjectQuery createObjectQuery() {
		return ObjectQueryImpl.createObjectQuery();
	}

	@Override
	@NotNull
	public ObjectQuery createObjectQuery(ObjectFilter filter) {
		return ObjectQueryImpl.createObjectQuery(filter);
	}

	@Override
	@NotNull
	public ObjectQuery createObjectQuery(XNode condition, ObjectFilter filter) {
		return ObjectQueryImpl.createObjectQuery((XNodeImpl) condition, filter);
	}

	@Override
	@NotNull
	public ObjectQuery createObjectQuery(ObjectPaging paging) {
		return ObjectQueryImpl.createObjectQuery(paging);
	}

	@Override
	@NotNull
	public ObjectQuery createObjectQuery(ObjectFilter filter, ObjectPaging paging) {
		return ObjectQueryImpl.createObjectQuery(filter, paging);
	}

}
