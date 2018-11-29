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

package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterationMethodType;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Collections.singleton;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

/**
 *
 */
public class GetOperationOptionsBuilderImpl implements GetOperationOptionsBuilder, GetOperationOptionsBuilder.Query {

	@NotNull private Set<UniformItemPath> currentPaths;
	private RelationalValueSearchQuery relationalValueSearchQuery;
	private Map<UniformItemPath, GetOperationOptions> options = new HashMap<>();

	private PrismContext prismContext;

	GetOperationOptionsBuilderImpl(PrismContext prismContext) {
		this.prismContext = prismContext;
		currentPaths = singleton(prismContext.emptyPath());
	}

	//region Path setting
	@Override
	public GetOperationOptionsBuilder root() {
		currentPaths = singleton(prismContext.emptyPath());
		return this;
	}

	@Override
	public GetOperationOptionsBuilder items(Object... items) {
		currentPaths = new HashSet<>();
		for (Object item : items) {
			currentPaths.add(pathForItem(item));
		}
		return this;
	}

	@Override
	public GetOperationOptionsBuilder item(ItemPath path) {
		currentPaths = singleton(path.toUniform(prismContext));
		return this;
	}

	@Override
	public GetOperationOptionsBuilder item(Object... components) {
		currentPaths = singleton(prismContext.path(components));
		return this;
	}
	//endregion
	
	//region Individual options except Query
	@Override
	public GetOperationOptionsBuilder retrieve() {
		return retrieve(RetrieveOption.INCLUDE);
	}

	@Override
	public GetOperationOptionsBuilder dontRetrieve() {
		return retrieve(RetrieveOption.EXCLUDE);
	}

	@Override
	public GetOperationOptionsBuilder retrieve(RetrieveOption value) {
		return forPaths(opts -> opts.setRetrieve(value));
	}

	@Override
	public GetOperationOptionsBuilderImpl retrieve(RelationalValueSearchQuery query) {
		return forPaths(opts -> {
			opts.setRetrieve(RetrieveOption.INCLUDE);
			opts.setRelationalValueSearchQuery(query);
		});
	}

	@Override
	public Query retrieveQuery() {
		if (relationalValueSearchQuery != null) {
			throw new IllegalStateException("Already constructing relational value search query");
		}
		relationalValueSearchQuery = new RelationalValueSearchQuery(null);
		return retrieve(relationalValueSearchQuery);
	}

	@Override
	public GetOperationOptionsBuilder resolve() {
		return resolve(true);
	}

	@Override
	public GetOperationOptionsBuilder resolve(Boolean value) {
		return forPaths(opts -> opts.setResolve(value));
	}

	@Override
	public GetOperationOptionsBuilder resolveNames() {
		return resolveNames(true);
	}

	@Override
	public GetOperationOptionsBuilder resolveNames(Boolean value) {
		return forPaths(opts -> opts.setResolveNames(value));
	}

	@Override
	public GetOperationOptionsBuilder noFetch() {
		return noFetch(true);
	}

	@Override
	public GetOperationOptionsBuilder noFetch(Boolean value) {
		return forPaths(opts -> opts.setNoFetch(value));
	}

	@Override
	public GetOperationOptionsBuilder raw() {
		return raw(true);
	}

	@Override
	public GetOperationOptionsBuilder raw(Boolean value) {
		return forPaths(opts -> opts.setRaw(value));
	}

	@Override
	public GetOperationOptionsBuilder tolerateRawData() {
		return tolerateRawData(true);
	}

	@Override
	public GetOperationOptionsBuilder tolerateRawData(Boolean value) {
		return forPaths(opts -> opts.setTolerateRawData(value));
	}

	@Override
	public GetOperationOptionsBuilder doNotDiscovery() {
		return doNotDiscovery(true);
	}

	@Override
	public GetOperationOptionsBuilder doNotDiscovery(Boolean value) {
		return forPaths(opts -> opts.setDoNotDiscovery(value));
	}

	@Override
	public GetOperationOptionsBuilder allowNotFound() {
		return allowNotFound(true);
	}

	@Override
	public GetOperationOptionsBuilder allowNotFound(Boolean value) {
		return forPaths(opts -> opts.setAllowNotFound(value));
	}

	@Override
	public GetOperationOptionsBuilder readOnly() {
		return readOnly(true);
	}

	@Override
	public GetOperationOptionsBuilder readOnly(Boolean value) {
		return forPaths(opts -> opts.setReadOnly(value));
	}

	@Override
	public GetOperationOptionsBuilder pointInTime(PointInTimeType value) {
		return forPaths(opts -> opts.setPointInTimeType(value));
	}

	@Override
	public GetOperationOptionsBuilder staleness(Long value) {
		return forPaths(opts -> opts.setStaleness(value));
	}

	@Override
	public GetOperationOptionsBuilder forceRefresh() {
		return forceRefresh(true);
	}

	@Override
	public GetOperationOptionsBuilder forceRefresh(Boolean value) {
		return forPaths(opts -> opts.setForceRefresh(value));
	}

	@Override
	public GetOperationOptionsBuilder distinct() {
		return distinct(true);
	}

	@Override
	public GetOperationOptionsBuilder distinct(Boolean value) {
		return forPaths(opts -> opts.setDistinct(value));
	}

	@Override
	public GetOperationOptionsBuilder attachDiagData() {
		return attachDiagData(true);
	}

	@Override
	public GetOperationOptionsBuilder attachDiagData(Boolean value) {
		return forPaths(opts -> opts.setAttachDiagData(value));
	}

	@Override
	public GetOperationOptionsBuilder definitionProcessing(DefinitionProcessingOption value) {
		return forPaths(opts -> opts.setDefinitionProcessing(value));
	}

	@Override
	public GetOperationOptionsBuilder iterationMethod(IterationMethodType value) {
		return forPaths(opts -> opts.setIterationMethod(value));
	}

	@Override
	public GetOperationOptionsBuilder executionPhase() {
		return executionPhase(true);
	}

	@Override
	public GetOperationOptionsBuilder executionPhase(Boolean value) {
		return forPaths(opts -> opts.setExecutionPhase(value));
	}
	//endregion

	//region Query
	@Override
	public Query asc(UniformItemPath path) {
		getOrCreatePaging().addOrderingInstruction(path, OrderDirection.ASCENDING);
		return this;
	}

	@Override
	public Query asc(Object... components) {
		return asc(prismContext.path(components));
	}

	@Override
	public Query desc(UniformItemPath path) {
		getOrCreatePaging().addOrderingInstruction(path, OrderDirection.DESCENDING);
		return this;
	}

	@Override
	public Query desc(Object... components) {
		return desc(prismContext.path(components));
	}

	@Override
	public Query offset(Integer n) {
		getOrCreatePaging().setOffset(n);
		return this;
	}

	@Override
	public Query maxSize(Integer n) {
		getOrCreatePaging().setMaxSize(n);
		return this;
	}

	private ObjectPaging getOrCreatePaging() {
		checkRelationalValueSearchQuery();
		if (relationalValueSearchQuery.getPaging() == null) {
			relationalValueSearchQuery.setPaging(ObjectPaging.createEmptyPaging());
		}
		return relationalValueSearchQuery.getPaging();
	}

	private void checkRelationalValueSearchQuery() {
		if (relationalValueSearchQuery == null) {
			throw new IllegalStateException("Currently not constructing relational value search query");
		}
	}

	@Override
	public Query item(QName column) {
		checkRelationalValueSearchQuery();
		relationalValueSearchQuery.setColumn(column);
		return this;
	}

	@Override
	public Query eq(String value) {
		return comparison(RelationalValueSearchType.EXACT, value);
	}

	@Override
	public Query startsWith(String value) {
		return comparison(RelationalValueSearchType.STARTS_WITH, value);
	}

	@Override
	public Query contains(String value) {
		return comparison(RelationalValueSearchType.SUBSTRING, value);
	}

	private Query comparison(RelationalValueSearchType type, String value) {
		checkRelationalValueSearchQuery();
		relationalValueSearchQuery.setSearchType(type);
		relationalValueSearchQuery.setSearchValue(value);
		return this;
	}

	@Override
	public GetOperationOptionsBuilder end() {
		checkRelationalValueSearchQuery();
		return this;
	}

	//endregion

	//region Loading from options

	@Override
	public GetOperationOptionsBuilder setFrom(Collection<SelectorOptions<GetOperationOptions>> newOptions) {
		options.clear();
		currentPaths = singleton(prismContext.emptyPath());
		relationalValueSearchQuery = null;
		for (SelectorOptions<GetOperationOptions> newOption : emptyIfNull(newOptions)) {
			if (newOption.getOptions() != null) {
				UniformItemPath itemPath = newOption.getItemPath(prismContext.emptyPath());
				if (options.containsKey(itemPath)) {
					throw new IllegalStateException(
							"Options for item path '" + itemPath + "' are defined more than once in " + newOptions);
				} else {
					options.put(itemPath, newOption.getOptions().clone());
				}
			}
		}
		return this;
	}

	@Override
	public GetOperationOptionsBuilder mergeFrom(Collection<SelectorOptions<GetOperationOptions>> newOptions) {
		currentPaths = singleton(prismContext.emptyPath());
		relationalValueSearchQuery = null;
		for (SelectorOptions<GetOperationOptions> newOption : emptyIfNull(newOptions)) {
			if (newOption.getOptions() != null) {
				UniformItemPath itemPath = newOption.getItemPath(prismContext.emptyPath());
				GetOperationOptions currentOptions = options.get(itemPath);
				if (currentOptions != null) {
					currentOptions.merge(newOption.getOptions());
				} else {
					options.put(itemPath, newOption.getOptions().clone());
				}
			}
		}
		return this;
	}

	//endregion

	//region Aux methods
	private UniformItemPath pathForItem(Object item) {
		if (item instanceof QName) {
			return prismContext.path((QName) item);
		} else if (item instanceof UniformItemPath) {
			return ((UniformItemPath) item);
		} else if (item instanceof ItemPath) {
			return ((ItemPath) item).toUniform(prismContext);
		} else {
			throw new IllegalArgumentException("item has to be QName or ItemPath but is " + item);
		}
	}

	private GetOperationOptionsBuilderImpl forPaths(Consumer<GetOperationOptions> modifier) {
		for (UniformItemPath path : currentPaths) {
			GetOperationOptions optionsForPath = options.computeIfAbsent(path, (key) -> new GetOperationOptions());
			modifier.accept(optionsForPath);
		}
		return this;
	}

	@NotNull
	public Collection<SelectorOptions<GetOperationOptions>> build() {
		return options.entrySet().stream()
				.map(e -> new SelectorOptions<>(new ObjectSelector(e.getKey()), e.getValue()))
				.collect(Collectors.toList());
	}
	//endregion

}
