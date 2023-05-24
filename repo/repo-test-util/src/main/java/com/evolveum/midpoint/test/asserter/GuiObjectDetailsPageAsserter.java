/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectDetailsPageType;
import org.assertj.core.api.Assertions;
import javax.xml.namespace.QName;

public class GuiObjectDetailsPageAsserter<RA> extends AbstractAsserter<RA> {

    private GuiObjectDetailsPageType guiObjectDetailsPage;

    public GuiObjectDetailsPageAsserter(GuiObjectDetailsPageType guiObjectDetailsPage, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.guiObjectDetailsPage = guiObjectDetailsPage;
    }

    public GuiObjectDetailsPageAsserter<RA> assertType(QName type) {
        Assertions.assertThat(QNameUtil.match(type, guiObjectDetailsPage.getType())).isTrue();
        return this;
    }

    public ContainerPanelConfigurationsAsserter<GuiObjectDetailsPageAsserter<RA>> panel() {
        return new ContainerPanelConfigurationsAsserter<>(guiObjectDetailsPage.getPanel(), this, "from object details" + guiObjectDetailsPage);
    }

    @Override
    protected String desc() {
        return "gui object details page";
    }
}
