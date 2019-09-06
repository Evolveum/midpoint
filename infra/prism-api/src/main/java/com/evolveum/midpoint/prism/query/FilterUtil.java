/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.query;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 *
 */
public class FilterUtil {

	// TODO move to SearchFilterType
	public static boolean isFilterEmpty(SearchFilterType filter) {
		return filter == null || (filter.getDescription() == null && !filter.containsFilterClause());
	}
}
