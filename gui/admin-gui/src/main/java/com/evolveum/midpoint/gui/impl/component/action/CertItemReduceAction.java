/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.action;

import com.evolveum.midpoint.web.application.ActionType;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;

@ActionType(
        identifier = "certItemReduce",
        applicableForType = AccessCertificationWorkItemType.class,
        display = @PanelDisplay(label = "PageCertDecisions.menu.reduce", icon = "fa fa-rotate-left text-warning", order = 3))
public class CertItemReduceAction extends AbstractCertItemAction {

    public CertItemReduceAction() {
        super();
    }

    public CertItemReduceAction(GuiActionDto<AccessCertificationWorkItemType> actionDto) {
        super(actionDto);
    }

    @Override
    protected AccessCertificationResponseType getResponse() {
        return AccessCertificationResponseType.REDUCE;
    }

}
