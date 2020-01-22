/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.cases;

import com.evolveum.midpoint.schrodinger.component.assignmentholder.AssignmentHolderObjectListPage;

/**
 * Created by Kate Honchar
 */
public class CasesPage extends AssignmentHolderObjectListPage<CasesListTable> {

    @Override
    public CasesListTable table() {
        return new CasesListTable(this, getTableBoxElement());
    }
}
