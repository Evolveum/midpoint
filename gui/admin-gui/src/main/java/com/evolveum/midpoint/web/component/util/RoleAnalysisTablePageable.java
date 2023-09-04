/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.util;

import org.apache.wicket.markup.html.navigation.paging.IPageable;

import java.io.Serializable;

public class RoleAnalysisTablePageable<T extends Serializable> extends SelectableBeanImpl<T> implements IPageable {

    int pageCount;
    long currentPage;

    public RoleAnalysisTablePageable(int pageCount, int currentPage) {
        this.pageCount = pageCount;
        this.currentPage = currentPage;
    }

    @Override
    public long getCurrentPage() {
        return currentPage;
    }

    @Override
    public void setCurrentPage(long page) {
        this.currentPage = page;
    }

    @Override
    public long getPageCount() {
        return pageCount;
    }
}
