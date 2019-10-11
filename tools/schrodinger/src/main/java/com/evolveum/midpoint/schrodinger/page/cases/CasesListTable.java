/**
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.cases;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.assignmentholder.AssignmentHolderObjectListTable;
import com.evolveum.midpoint.schrodinger.page.user.ListUsersPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;

/**
 * Created by Kate Honchar.
 */
public class CasesListTable extends AssignmentHolderObjectListTable<CasesPage, CasePage> {

    public CasesListTable(CasesPage parent, SelenideElement parentElement){
        super(parent, parentElement);
    }

    @Override
    public CasePage getObjectDetailsPage(){
        return new CasePage();
    }
}
