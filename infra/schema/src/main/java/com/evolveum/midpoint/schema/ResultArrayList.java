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

import java.util.ArrayList;

/**
 * 
 * @author lazyman
 * 
 */
public class ResultArrayList<T> extends ArrayList<T> implements ResultList<T> {

	private static final long serialVersionUID = 7475418036271204310L;
	private Integer totalResultCount;

	public ResultArrayList() {
		this.totalResultCount = null;
	}

	public ResultArrayList(int count) {
		this.totalResultCount = count;
	}

	@Override
	public Integer getTotalResultCount() {
		return totalResultCount;
	}

	@Override
	public void setTotalResultCount(Integer count) {
		if (count != null && count < 0) {
			throw new IllegalArgumentException("Count must be non negative integer value (" + count + ")");
		}

		this.totalResultCount = count;
	}
}
