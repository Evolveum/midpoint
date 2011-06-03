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
package com.evolveum.midpoint.web.controller.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author lazyman
 * 
 */
public abstract class ListController<T> implements Serializable {

	private static final long serialVersionUID = 4859510243062515510L;
	private List<T> objects;
	private int offset = 0;
	private int rowsCount = 10;

	public List<T> getObjects() {
		if (objects == null) {
			objects = new ArrayList<T>();
		}
		return objects;
	}
	
	public int getOffset() {
		return offset;
	}

	public void setRowsCount(int rowsCount) {
		this.rowsCount = rowsCount;
	}

	public int getRowsCount() {
		return rowsCount;
	}

	public String listLast() {
		offset = 0;
		return listObjects();
	}

	public String listNext() {
		offset += rowsCount;
		return listObjects();
	}

	public String listFirst() {
		offset = 0;
		return listObjects();
	}

	public String listPrevious() {
		if (offset < rowsCount) {
			return null;
		}
		offset -= rowsCount;
		return listObjects();
	}
	
	public void cleanup() {
		offset = 0;
		getObjects().clear();
	}

	protected abstract String listObjects();
}
