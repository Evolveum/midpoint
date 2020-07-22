/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;

/**
 * @author Viliam Repan (lazyman)
 */
public interface Table {

    DataTable getDataTable();

    String getTableIdKey();

    void setItemsPerPage(int size);

    int getItemsPerPage();

    void setShowPaging(boolean show);

    void setCurrentPage(long page);

    @Deprecated
    void setCurrentPage(ObjectPaging paging);
}
