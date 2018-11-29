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

import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterationMethodType;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 *
 */
public interface GetOperationOptionsBuilder {

	GetOperationOptionsBuilder root();
	GetOperationOptionsBuilder items(Object... items);
	GetOperationOptionsBuilder item(ItemPath path);
	GetOperationOptionsBuilder item(Object... components);

	GetOperationOptionsBuilder retrieve();
	GetOperationOptionsBuilder dontRetrieve();
	GetOperationOptionsBuilder retrieve(RetrieveOption value);
	GetOperationOptionsBuilder retrieve(RelationalValueSearchQuery query);
	GetOperationOptionsBuilder.Query retrieveQuery();

	GetOperationOptionsBuilder resolve();
	GetOperationOptionsBuilder resolve(Boolean value);
	GetOperationOptionsBuilder resolveNames();
	GetOperationOptionsBuilder resolveNames(Boolean value);
	GetOperationOptionsBuilder noFetch();
	GetOperationOptionsBuilder noFetch(Boolean value);
	GetOperationOptionsBuilder raw();
	GetOperationOptionsBuilder raw(Boolean value);
	GetOperationOptionsBuilder tolerateRawData();
	GetOperationOptionsBuilder tolerateRawData(Boolean value);
	GetOperationOptionsBuilder doNotDiscovery();
	GetOperationOptionsBuilder doNotDiscovery(Boolean value);
	GetOperationOptionsBuilder allowNotFound();
	GetOperationOptionsBuilder allowNotFound(Boolean value);
	GetOperationOptionsBuilder readOnly();
	GetOperationOptionsBuilder readOnly(Boolean value);
	GetOperationOptionsBuilder pointInTime(PointInTimeType value);
	GetOperationOptionsBuilder staleness(Long value);
	GetOperationOptionsBuilder forceRefresh();
	GetOperationOptionsBuilder forceRefresh(Boolean value);
	GetOperationOptionsBuilder distinct();
	GetOperationOptionsBuilder distinct(Boolean value);
	GetOperationOptionsBuilder attachDiagData();
	GetOperationOptionsBuilder attachDiagData(Boolean value);
	GetOperationOptionsBuilder definitionProcessing(DefinitionProcessingOption value);
	GetOperationOptionsBuilder iterationMethod(IterationMethodType value);
	GetOperationOptionsBuilder executionPhase();
	GetOperationOptionsBuilder executionPhase(Boolean value);

	GetOperationOptionsBuilder setFrom(Collection<SelectorOptions<GetOperationOptions>> options);
	GetOperationOptionsBuilder mergeFrom(Collection<SelectorOptions<GetOperationOptions>> options);

	interface Query {
		Query asc(UniformItemPath path);
		Query asc(Object... components);
		Query desc(UniformItemPath path);
		Query desc(Object... components);
		Query offset(Integer n);
		Query maxSize(Integer n);
		Query item(QName column);
		Query eq(String value);
		Query startsWith(String value);
		Query contains(String value);
		GetOperationOptionsBuilder end();
	}

	@NotNull
	Collection<SelectorOptions<GetOperationOptions>> build();
}
