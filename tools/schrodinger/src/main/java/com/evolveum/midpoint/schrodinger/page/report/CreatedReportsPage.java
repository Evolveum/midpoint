/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.report;

import com.evolveum.midpoint.schrodinger.component.assignmentholder.AssignmentHolderObjectListPage;
import com.evolveum.midpoint.schrodinger.component.report.CreatedReportsTable;

/**
 * Created by Viliam Repan (lazyman).
 */
public class CreatedReportsPage extends AssignmentHolderObjectListPage<CreatedReportsTable, ReportPage> {

    @Override
    public CreatedReportsTable table() {
        return new CreatedReportsTable(this, getTableBoxElement());
    }

    @Override
    public ReportPage getObjectDetailsPage() {
        return new ReportPage();
    }

}
