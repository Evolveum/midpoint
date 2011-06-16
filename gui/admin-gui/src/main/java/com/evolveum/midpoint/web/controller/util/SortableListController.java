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

import org.apache.commons.lang.Validate;

/**
 * 
 * @author lazyman
 * 
 */
public abstract class SortableListController<T> extends ListController<T> {

	private static final long serialVersionUID = -8489990467008681729L;
	private String sortColumnName;
	private boolean ascending;
	
	public SortableListController() {
		this("name");
	}

	public SortableListController(String sortColumnName) {
		Validate.notEmpty(sortColumnName, "Sort column name must not be null or empty.");

		this.sortColumnName = sortColumnName;
	}

	/**
	 * Sort the list.
	 */
	protected abstract void sort();

	/**
	 * Gets the sortColumnName column.
	 * 
	 * @return column to sortColumnName
	 */
	public String getSortColumnName() {
		return sortColumnName;
	}

	/**
	 * Sets the sortColumnName column
	 * 
	 * @param sortColumnName
	 *            column to sortColumnName
	 */
	public void setSortColumnName(String sortColumnName) {
		Validate.notEmpty(sortColumnName, "Sort column name must not be null or empty.");
		this.sortColumnName = sortColumnName;
	}

	/**
	 * Is the sortColumnName ascending.
	 * 
	 * @return true if the ascending sortColumnName otherwise false.
	 */
	public boolean isAscending() {
		return ascending;
	}

	/**
	 * Set sortColumnName type.
	 * 
	 * @param ascending
	 *            true for ascending sortColumnName, false for desending
	 *            sortColumnName.
	 */
	public void setAscending(boolean ascending) {
		this.ascending = ascending;
	}
}
