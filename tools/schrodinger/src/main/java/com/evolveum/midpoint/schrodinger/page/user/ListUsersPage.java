/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.user;

import com.evolveum.midpoint.schrodinger.component.assignmentholder.AssignmentHolderObjectListPage;
import com.evolveum.midpoint.schrodinger.component.user.UsersPageTable;
import com.evolveum.midpoint.schrodinger.util.ConstantsUtil;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ListUsersPage extends AssignmentHolderObjectListPage<UsersPageTable> {

    @Override
    public UsersPageTable table() {
        return new UsersPageTable(this, getTableBoxElement());
    }

    @Override
    protected String getTableAdditionalClass(){
        return ConstantsUtil.OBJECT_USER_BOX_COLOR;
    }

}
