/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schrodinger.component;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.common.PrismFormWithActionButtons;
import com.evolveum.midpoint.schrodinger.component.common.table.AbstractTableWithPrismView;
import com.evolveum.midpoint.schrodinger.component.common.table.TableWithPageRedirect;
import com.evolveum.midpoint.schrodinger.component.modal.FocusSetProjectionModal;
import com.evolveum.midpoint.schrodinger.component.table.TableHeaderDropDownMenu;
import com.evolveum.midpoint.schrodinger.component.user.ProjectionsDropDown;
import com.evolveum.midpoint.schrodinger.page.AssignmentHolderDetailsPage;
import com.evolveum.midpoint.schrodinger.page.BasicPage;
import com.evolveum.midpoint.schrodinger.page.FocusPage;
import com.evolveum.midpoint.schrodinger.page.cases.CasePage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.schrodinger.util.Schrodinger;
import com.evolveum.midpoint.schrodinger.util.Utils;

import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;

/**
 * Created by honchar
 */
public class CasesTab<P extends FocusPage> extends Component<P> {

    public CasesTab(P parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public TableWithPageRedirect<CasesTab<P>> table() {
        return new TableWithPageRedirect<CasesTab<P>>(this,
                $(Schrodinger.byDataId("taskTable")).waitUntil(Condition.visible, MidPoint.TIMEOUT_DEFAULT_2_S)) {
            @Override
            public CasePage clickByName(String name) {
                return new CasePage();
            }

            @Override
            public TableWithPageRedirect<CasesTab<P>> selectCheckboxByName(String name) {
                return null;
            }

            @Override
            protected TableHeaderDropDownMenu<P> clickHeaderActionDropDown() {
                return null;
            }
        };
    }
}
