/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
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
