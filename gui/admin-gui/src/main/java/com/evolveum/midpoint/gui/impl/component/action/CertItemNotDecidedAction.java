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
        identifier = "certItemNotDecided",
        applicableForType = AccessCertificationWorkItemType.class,
        display = @PanelDisplay(label = "PageCertDecisions.menu.notDecided", icon = "fa fa-question text-info", order = 4))
public class CertItemNotDecidedAction extends AbstractCertItemAction {

    public CertItemNotDecidedAction() {
        super();
    }

    public CertItemNotDecidedAction(GuiActionDto<AccessCertificationWorkItemType> actionDto) {
        super(actionDto);
    }

    @Override
    protected AccessCertificationResponseType getResponse() {
        return AccessCertificationResponseType.NOT_DECIDED;
    }

}
