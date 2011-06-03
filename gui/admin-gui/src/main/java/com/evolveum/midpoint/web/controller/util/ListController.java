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
	private boolean firstEnabled = true;
	private boolean lastEnabled = true;

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
		offset = -1;
		firstEnabled = true;
		lastEnabled = false;

		return listObjects();
	}

	public String listNext() {
		offset += rowsCount;
		firstEnabled = true;
		lastEnabled = true;

		return listObjects();
	}

	public String listFirst() {
		offset = 0;
		firstEnabled = false;
		lastEnabled = true;

		return listObjects();
	}

	public String listPrevious() {
		if (offset < rowsCount) {
			offset = 0;
		} else {
			offset -= rowsCount;
		}
		lastEnabled = true;
		firstEnabled = true;

		return listObjects();
	}

	public void cleanup() {
		offset = 0;
		getObjects().clear();
	}

	public boolean isFirstEnabled() {
		return firstEnabled;
	}

	public boolean isPreviousEnabled() {
		return firstEnabled;
	}

	public boolean isNextEnabled() {
		return lastEnabled;
	}

	public boolean isLastEnabled() {
		return lastEnabled;
	}

	protected abstract String listObjects();
}
