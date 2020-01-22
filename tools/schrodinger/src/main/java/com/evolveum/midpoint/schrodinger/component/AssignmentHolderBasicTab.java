/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.component;

import com.codeborne.selenide.SelenideElement;
import com.evolveum.midpoint.schrodinger.component.common.PrismForm;
import com.evolveum.midpoint.schrodinger.page.AssignmentHolderDetailsPage;

/**
 * Created by Viliam Repan (lazyman).
 */
public class AssignmentHolderBasicTab<P extends AssignmentHolderDetailsPage> extends Component<P> {

    public AssignmentHolderBasicTab(P parent, SelenideElement parentElement) {
        super(parent, parentElement);
    }

    public PrismForm<AssignmentHolderBasicTab<P>> form() {
        SelenideElement element = null;
        return new PrismForm<AssignmentHolderBasicTab<P>>(this, element);
    }
}
