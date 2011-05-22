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
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.web.util;

import java.io.Serializable;

/**
 * 
 * @author lazyman
 */
public abstract class SortableList implements Serializable {

	private static final long serialVersionUID = 2543170021612042446L;
	private String sortColumnName;
	private boolean ascending;

	public SortableList() {
		this("name");
	}

	public SortableList(String defaultSortColumn) {
		sortColumnName = defaultSortColumn;
		ascending = isDefaultAscending(defaultSortColumn);
	}

	/**
	 * Sort the list.
	 */
	public abstract void sort();

	/**
	 * Is the default sortColumnName direction for the given column "ascending"
	 * ?
	 */
	protected boolean isDefaultAscending(String sortColumn) {
		return true;
	}

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
