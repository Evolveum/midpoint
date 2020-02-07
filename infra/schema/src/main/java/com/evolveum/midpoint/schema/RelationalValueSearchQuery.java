/*
 * Copyright (c) 2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.query.ObjectPaging;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author semancik
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

    public RelationalValueSearchQuery clone() {
        RelationalValueSearchQuery clone = new RelationalValueSearchQuery(column, searchValue, searchType);
        if (this.paging != null) {
            clone.paging = this.paging.clone();
        }
        return clone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RelationalValueSearchQuery that = (RelationalValueSearchQuery) o;
        return Objects.equals(column, that.column) && Objects.equals(searchValue, that.searchValue) && searchType == that.searchType && Objects.equals(paging, that.paging);
    }

    @Override
    public int hashCode() {
        return Objects.hash(column, searchValue, searchType, paging);
    }

    @Override
    public String toString() {
        return "RelationalValueSearchQuery(column=" + column + ", searchValue=" + searchValue
                + ", searchType=" + searchType + ", paging=" + paging + ")";
    }
}
