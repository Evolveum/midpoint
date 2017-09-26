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

package com.evolveum.midpoint.model.impl.controller;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * @author mederly
 */
@Component
public class EmulatedSearchProvider {

	@Autowired private MatchingRuleRegistry matchingRuleRegistry;
	@Autowired
	@Qualifier("cacheRepositoryService")
	private transient RepositoryService cacheRepositoryService;

	public <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options,
			OperationResult result) throws SchemaException {

		SearchResultList<PrismObject<T>> allObjects = cacheRepositoryService.searchObjects(type, null, options, result);
		if (query == null) {
			return allObjects;
		}

		List<PrismObject<T>> filtered = doFiltering(query, allObjects, (o) -> o.getValue());
		List<PrismObject<T>> paged = doPaging(query, filtered, (o) -> o.getValue());
		return new SearchResultList<>(paged);
	}

	public <T extends Containerable> SearchResultList<T> searchContainers(Class<T> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options,
			OperationResult result) throws SchemaException {
		if (!CaseWorkItemType.class.equals(type)) {
			throw new UnsupportedOperationException("searchContainers is supported only for CaseWorkItemType, not for " + type);
		}

		List<CaseWorkItemType> allValues = new ArrayList<>();
		SearchResultList<PrismObject<CaseType>> cases = cacheRepositoryService
				.searchObjects(CaseType.class, null, null, result);
		for (PrismObject<CaseType> aCase : cases) {
			allValues.addAll(CloneUtil.cloneCollectionMembers(aCase.asObjectable().getWorkItem()));
		}
		if (query == null) {
			//noinspection unchecked
			return (SearchResultList<T>) new SearchResultList<>(allValues);
		}

		List<CaseWorkItemType> filtered = doFiltering(query, allValues, (v) -> v.asPrismContainerValue());
		List<CaseWorkItemType> paged = doPaging(query, filtered, (v) -> v.asPrismContainerValue());
		//noinspection unchecked
		return (SearchResultList<T>) new SearchResultList<>(paged);
	}

	private <T> List<T> doFiltering(ObjectQuery query, List<T> allObjects, Function<T, PrismContainerValue> pcvExtractor)
			throws SchemaException {
		ObjectFilter filter = query.getFilter();
		if (filter == null) {
			return allObjects;
		}
		List<T> filtered = new ArrayList<>();
		for (T object : allObjects) {
			if (filter.match(pcvExtractor.apply(object), matchingRuleRegistry)) {
				filtered.add(object);
			}
		}
		return filtered;
	}


	private <T> List<T> doPaging(ObjectQuery query, List<T> allObjects, Function<T, PrismContainerValue> pcvExtractor)
			throws SchemaException {
		ObjectPaging paging = query.getPaging();
		if (paging == null) {
			return allObjects;
		}
		if (!paging.getOrderingInstructions().isEmpty()) {
			allObjects.sort((o1, o2) -> {
				PrismContainerValue pcv1 = pcvExtractor.apply(o1);
				PrismContainerValue pcv2 = pcvExtractor.apply(o2);
				for (ObjectOrdering ordering : paging.getOrderingInstructions()) {
					Comparable<Object> c1 = getComparable(pcv1.find(ordering.getOrderBy()));
					Comparable<Object> c2 = getComparable(pcv2.find(ordering.getOrderBy()));
					int result = c1 == null
							? (c2 == null ? 0 : 1)
							: (c2 == null ? -1 : c1.compareTo(c2));
					if (result != 0) {
						return result;
					}
				}
				return 0;
			});
		}
		int start = paging.getOffset() != null ? paging.getOffset() : 0;
		int end = paging.getMaxSize() != null ? Math.min(start + paging.getMaxSize(), allObjects.size()) : allObjects.size();
		if (start == 0 && end == allObjects.size()) {
			return allObjects;
		} else {
			return allObjects.subList(start, end);
		}
	}

	@SuppressWarnings("unchecked")
	private Comparable<Object> getComparable(Object o) {
		if (o instanceof Item) {
			Item item = (Item) o;
			if (item.getValues().size() > 1) {
				throw new IllegalStateException("Couldn't compare multivalued items");
			} else if (item.getValues().size() == 1) {
				o = item.getValues().get(0);
			} else {
				return null;
			}
		}
		if (o instanceof PrismValue) {
			o = ((PrismValue) o).getRealValue();
		}
		if (o instanceof Comparable) {
			return (Comparable<Object>) o;
		}
		return null;
	}

}
