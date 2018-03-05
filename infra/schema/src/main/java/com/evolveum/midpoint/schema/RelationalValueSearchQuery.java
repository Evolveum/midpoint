/**
 * Copyright (c) 2015 Evolveum
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
package com.evolveum.midpoint.schema;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.query.ObjectPaging;

import java.io.Serializable;

/**
 * @author semancik
 *
 */
public class RelationalValueSearchQuery implements Serializable {
	private QName column;
	private String searchValue;
	private RelationalValueSearchType searchType;
	private ObjectPaging paging;

	public RelationalValueSearchQuery(ObjectPaging paging) {
		this.paging = paging;
	}

	public RelationalValueSearchQuery(QName column, String searchValue, RelationalValueSearchType searchType,
			ObjectPaging paging) {
		this.column = column;
		this.searchValue = searchValue;
		this.searchType = searchType;
		this.paging = paging;
	}

	public RelationalValueSearchQuery(QName column, String searchValue, RelationalValueSearchType searchType) {
		this.column = column;
		this.searchValue = searchValue;
		this.searchType = searchType;
	}

	public QName getColumn() {
		return column;
	}

	public void setColumn(QName column) {
		this.column = column;
	}

	public String getSearchValue() {
		return searchValue;
	}

	public void setSearchValue(String searchValue) {
		this.searchValue = searchValue;
	}

	public RelationalValueSearchType getSearchType() {
		return searchType;
	}

	public void setSearchType(RelationalValueSearchType searchType) {
		this.searchType = searchType;
	}

	public ObjectPaging getPaging() {
		return paging;
	}

	public void setPaging(ObjectPaging paging) {
		this.paging = paging;
	}

	@Override
    public RelationalValueSearchQuery clone() {
		RelationalValueSearchQuery clone = new RelationalValueSearchQuery(column, searchValue, searchType);
		if (this.paging != null) {
			clone.paging = this.paging.clone();
		}
		return clone;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((column == null) ? 0 : column.hashCode());
		result = prime * result + ((paging == null) ? 0 : paging.hashCode());
		result = prime * result + ((searchType == null) ? 0 : searchType.hashCode());
		result = prime * result + ((searchValue == null) ? 0 : searchValue.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RelationalValueSearchQuery other = (RelationalValueSearchQuery) obj;
		if (column == null) {
			if (other.column != null)
				return false;
		} else if (!column.equals(other.column))
			return false;
		if (paging == null) {
			if (other.paging != null)
				return false;
		} else if (!paging.equals(other.paging))
			return false;
		if (searchType != other.searchType)
			return false;
		if (searchValue == null) {
			if (other.searchValue != null)
				return false;
		} else if (!searchValue.equals(other.searchValue))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "RelationalValueSearchQuery(column=" + column + ", searchValue=" + searchValue
				+ ", searchType=" + searchType + ", paging=" + paging + ")";
	}
}
