/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data;

import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.web.session.UserProfileStorage;

/**
 * @author Viliam Repan (lazyman)
 */
public interface Table {

    DataTable getDataTable();

    UserProfileStorage.TableId getTableId();

    boolean enableSavePageSize();

    void setItemsPerPage(int size);

    int getItemsPerPage();

    void setShowPaging(boolean show);

    void setCurrentPage(long page);

    void setCurrentPageAndSort(ObjectPaging paging);
}
