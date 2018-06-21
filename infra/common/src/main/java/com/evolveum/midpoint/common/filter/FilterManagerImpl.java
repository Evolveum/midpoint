/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.common.filter;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 *
 * @author Igor Farinic
 *
 */
public class FilterManagerImpl<T extends Filter> implements FilterManager<T> {

	private static final Trace LOGGER = TraceManager.getTrace(FilterManagerImpl.class);
	private Map<String, Class<T>> filterMap;

	@Override
	public void setFilterMapping(Map<String, Class<T>> filterMap) {
		Validate.notNull(filterMap, "Filter mapping must not be null.");
		this.filterMap = filterMap;
	}

	@Override
	public Filter getFilterInstance(String uri) {
		return getFilterInstance(uri, null);
	}

	@Override
	public Filter getFilterInstance(String uri, List<Object> parameters) {
		Validate.notEmpty(uri, "Filter uri must not be null or empty.");
		Class<T> clazz = filterMap.get(uri);
		if (clazz == null) {
			return null;
		}

		Filter filter = null;
		try {
			filter = clazz.newInstance();
			filter.setParameters(parameters);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couln't create filter instance", ex);
		}

		return filter;
	}
}
