/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
