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
package com.evolveum.midpoint.prism.path;

import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author semancik
 *
 */
public interface UniformItemPath extends Serializable, Cloneable, ShortDumpable, ItemPath {

	@NotNull
	List<ItemPathSegment> getSegments();

	ItemPathSegment first();

	@NotNull
	default UniformItemPath rest() {
		return rest(1);
	}

	@Nullable
	ItemPathSegment last();

	/**
	 * Returns first segment in a form of path.
	 */
	UniformItemPath firstAsPath();

	@NotNull
	UniformItemPath rest(int n);

	/**
	 * Returns a path containing all segments except the last one.
	 */
	@NotNull
	UniformItemPath allExceptLast();

	/**
     * Returns a path containing all segments up to (and not including) the last one.
     */
	@NotNull
	UniformItemPath allUpToLastName();

	UniformItemPath allUpToIncluding(int i);

	/**
	 * Makes the path "normal" by inserting null Id segments where they were omitted.
	 */
	UniformItemPath normalize();

	@NotNull
	UniformItemPath removeIds();

	@NotNull
	UniformItemPath namedSegmentsOnly();

	@NotNull
	UniformItemPath stripVariableSegment();

	@NotNull
	UniformItemPath append(Object... components);

	UniformItemPath remainder(ItemPath prefix);

	/**
     * More strict version of ItemPath comparison. Does not use any normalization
     * nor approximate matching QNames via QNameUtil.match.
     *
     * For semantic-level comparison, please use equivalent(..) method.
     */
	@Override
	boolean equals(Object obj);

	UniformItemPath clone();

	ItemPathType asItemPathType();

	ItemPathSegment getSegment(int i);

	void setNamespaceMap(Map<String, String> namespaceMap);

	Map<String, String> getNamespaceMap();
}
