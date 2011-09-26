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
package com.evolveum.midpoint.schema;

import java.util.List;

/**
 * <p>
 * List with extensions to provide more information about the operation result.
 * </p><p>
 * This interface is intended to be used as a result of search or list operations.
 * It contains the results as a normal list would contain, but it also provides more
 * information about the result. E.g. It may return the total number of results that matched
 * the query. The list itself may contain only subset of all objects, e.g. when constrained
 * by paging request. This list result also total count, therefore appropriate GUI controls
 * or progress bars can be displayed.
 * </p>
 * @author lazyman
 * @author Radovan Semancik
 * 
 */
public interface ResultList<T> extends List<T> {

	/**
	 * <p>
	 * Returns total number of all objects that matched a search/list criteria.
	 * </p><p>
	 * It may be different from the list size e.g. if paging control was
	 * used in the request.
	 * </p><p>
	 * totalResultCount may be null. This means that repository cannot determine
	 * the total number of results. Negative value must not be used.
	 * </p>
	 * @return total number of all objects that matched a search/list criteria (or null).
	 */
	Integer getTotalResultCount();

	/**
	 * <p>
	 * Sets total number of all objects that matched a search/list criteria.
	 * </p><p>
	 * It may be different from the list size e.g. if paging control was
	 * used in the request.
	 * </p><p>
	 * totalResultCount may be null. This means that repository cannot determine
	 * the total number of results. Negative value must not be used.
	 * </p>
	 * @param count
	 *            total number of all objects that matched a search/list criteria (or null).
	 */
	void setTotalResultCount(Integer count);
}
