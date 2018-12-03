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

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 *
 */
public interface ObjectPaging extends DebugDumpable, Serializable {

	OrderDirection getDirection();

	// TODO rename to getPrimaryOrderingPath
	ItemPath getOrderBy();

	ObjectOrdering getPrimaryOrdering();

	ItemPath getGroupBy();

	ObjectGrouping getPrimaryGrouping();

	// TODO name?
	List<? extends ObjectOrdering> getOrderingInstructions();

	List<? extends ObjectGrouping> getGroupingInstructions();

	boolean hasOrdering();

	void setOrdering(ItemPath orderBy, OrderDirection direction);

	boolean hasGrouping();

	boolean hasCookie();

	void setGrouping(ItemPath groupBy);

	void addOrderingInstruction(ItemPath orderBy, OrderDirection direction);

	@SuppressWarnings("NullableProblems")
	void setOrdering(ObjectOrdering... orderings);

	void setOrdering(Collection<? extends ObjectOrdering> orderings);

	void addGroupingInstruction(ItemPath groupBy);

	void setGrouping(ObjectGrouping... groupings);

	void setGrouping(Collection<ObjectGrouping> groupings);

	Integer getOffset();

	void setOffset(Integer offset);

	Integer getMaxSize();

	void setMaxSize(Integer maxSize);

	/**
	 * Returns the paging cookie. The paging cookie is used for optimization of paged searches.
	 * The presence of the cookie may allow the data store to correlate queries and associate
	 * them with the same server-side context. This may allow the data store to reuse the same
	 * pre-computed data. We want this as the sorted and paged searches may be quite expensive.
	 * It is expected that the cookie returned from the search will be passed back in the options
	 * when the next page of the same search is requested.
	 *
	 * It is OK to initialize a search without any cookie. If the datastore utilizes a re-usable
	 * context it will return a cookie in a search response.
	 */
	String getCookie();

	/**
	 * Sets paging cookie. The paging cookie is used for optimization of paged searches.
	 * The presence of the cookie may allow the data store to correlate queries and associate
	 * them with the same server-side context. This may allow the data store to reuse the same
	 * pre-computed data. We want this as the sorted and paged searches may be quite expensive.
	 * It is expected that the cookie returned from the search will be passed back in the options
	 * when the next page of the same search is requested.
	 *
	 * It is OK to initialize a search without any cookie. If the datastore utilizes a re-usable
	 * context it will return a cookie in a search response.
	 */
	void setCookie(String cookie);

	ObjectPaging clone();

	boolean equals(Object o, boolean exact);
}
