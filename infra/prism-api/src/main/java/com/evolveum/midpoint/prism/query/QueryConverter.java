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
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import org.jetbrains.annotations.NotNull;

/**
 * TODO cleanup this interface
 */
public interface QueryConverter {

	// 1. Parsing

	// 1a. Parsing filters

	ObjectFilter parseFilter(XNode xnode, Class<? extends Containerable> clazz) throws SchemaException;

	ObjectFilter parseFilter(@NotNull SearchFilterType filter, @NotNull Class<? extends Containerable> clazz) throws SchemaException;

	ObjectFilter parseFilter(@NotNull SearchFilterType filter, @NotNull PrismContainerDefinition<?> objDef) throws SchemaException;

	/**
	 * Tries to parse as much from filter as possible, without knowing the definition of object(s) to which the
	 * filter will be applied. It is used mainly to parse path specifications, in order to avoid namespace loss
	 * when serializing raw (unparsed) paths and QNames - see MID-1969.
	 * @param xfilter
	 * @param pc
	 */
	void parseFilterPreliminarily(MapXNode xfilter, ParsingContext pc) throws SchemaException;

	// 1b. Parsing queries

	<O extends Objectable> ObjectQuery createObjectQuery(Class<O> clazz, QueryType queryType) throws SchemaException;

	<O extends Objectable> ObjectQuery createObjectQuery(Class<O> clazz, SearchFilterType filterType) throws SchemaException;

	// 2. Serializing

	// 2a. Serializing filters

	SearchFilterType createSearchFilterType(ObjectFilter filter) throws SchemaException;

	<O extends Objectable> ObjectFilter createObjectFilter(Class<O> clazz, SearchFilterType filterType)
			throws SchemaException;

	<O extends Objectable> ObjectFilter createObjectFilter(PrismObjectDefinition<O> objectDefinition, SearchFilterType filterType)
			throws SchemaException;

	MapXNode serializeFilter(ObjectFilter filter) throws SchemaException;

	// 2b. Serializing queries

	QueryType createQueryType(ObjectQuery query) throws SchemaException;

}
