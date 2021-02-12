/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.cases;

import com.codeborne.selenide.Condition;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.cases.WorkitemsTable;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by honchar
 */
public class WorkitemsPage extends BasicPage {

    public WorkitemsTable<WorkitemsPage> table() {
        return new WorkitemsTable<>(this, $(Schrodinger.byDataId("workitemsTable")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S));
    }
}
