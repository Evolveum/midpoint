/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationKindType;

import java.util.HashSet;
import java.util.Set;

@Experimental
public class Options {
    private final Set<OperationKindType> kindsToShow = new HashSet<>();
    private final Set<OpType> typesToShow = new HashSet<>();
    private final Set<PerformanceCategory> categoriesToShow = new HashSet<>();
    private boolean showAlsoParents;
    private boolean showPerformanceColumns;
    private boolean showReadWriteColumns;

    public boolean isShowAlsoParents() {
        return showAlsoParents;
    }
    public void setShowAlsoParents(boolean showAlsoParents) {
        this.showAlsoParents = showAlsoParents;
    }
    public Set<OpType> getTypesToShow() {
        return typesToShow;
    }

    public Set<OperationKindType> getKindsToShow() {
        return kindsToShow;
    }

    public Set<PerformanceCategory> getCategoriesToShow() {
        return categoriesToShow;
    }
    public boolean isShowPerformanceColumns() {
        return showPerformanceColumns;
    }
    public void setShowPerformanceColumns(boolean showPerformanceColumns) {
        this.showPerformanceColumns = showPerformanceColumns;
    }
    public boolean isShowReadWriteColumns() {
        return showReadWriteColumns;
    }
    public void setShowReadWriteColumns(boolean showReadWriteColumns) {
        this.showReadWriteColumns = showReadWriteColumns;
    }

}
