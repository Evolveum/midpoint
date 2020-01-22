/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.data.column;

import java.io.Serializable;

public class ColumnTypeDto<T> implements Serializable{

    private static final long serialVersionUID = 1L;

    private String columnName;
    private String columnValue;
    private String sortableColumn;
    private boolean sortable = false;
    private boolean multivalue = false;
    private boolean translated = false;

    public ColumnTypeDto(String columnName, String columnValue, String sortableColumn) {
        super();
        this.columnName = columnName;
        this.columnValue = columnValue;
//        this.sortableColumn = sortableColumn;
    }

    public ColumnTypeDto(String columnName, String sortableColumn, String columnValue, boolean multivalue) {
        this(columnName, sortableColumn, columnValue, multivalue, false);
    }

    public ColumnTypeDto(String columnName, String sortableColumn, String columnValue, boolean multivalue, boolean translated) {
        super();
        this.columnName = columnName;
        this.columnValue = columnValue;
        this.sortableColumn = sortableColumn;
        this.multivalue = multivalue;
        this.translated = translated;
    }

    public String getColumnName() {
        return columnName;
    }
    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }
    public String getColumnValue() {
        return columnValue;
    }
    public void setColumnValue(String columnValue) {
        this.columnValue = columnValue;
    }

    public String getSortableColumn() {
        return sortableColumn;
    }


    public boolean isMultivalue() {
        return multivalue;
    }

    public void setMultivalue(boolean multivalue) {
        this.multivalue = multivalue;
    }

    public boolean isTranslated() {
        return translated;
    }

    public void setTranslated(boolean translated) {
        this.translated = translated;
    }

    public boolean isSortable() {
        return sortableColumn != null;
    }


}
