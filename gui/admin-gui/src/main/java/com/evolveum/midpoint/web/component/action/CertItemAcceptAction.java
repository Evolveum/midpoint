/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.action;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.GuiActionType;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.page.admin.certification.CertMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;


@GuiActionType(
        identifier = "certItemAccept",
        applicableForType = AccessCertificationWorkItemType.class,
        display = @PanelDisplay(label = "PageCertDecisions.menu.accept", icon = "fa fa-check", order = 1))
public class CertItemAcceptAction extends AbstractGuiAction<AccessCertificationWorkItemType> {

    private static final String DOT_CLASS = CertItemAcceptAction.class.getName() + ".";
    private static final String OPERATION_RECORD_ACTION = DOT_CLASS + "recordCertItemAction";

    public CertItemAcceptAction() {
        super();
    }

    @Override
    public void onActionPerformed(AccessCertificationWorkItemType workItem, PageBase pageBase) {
        OperationResult result = new OperationResult(OPERATION_RECORD_ACTION);
        Task task = pageBase.createSimpleTask(OPERATION_RECORD_ACTION);
        CertMiscUtil.recordCertItemResponse(workItem, AccessCertificationResponseType.ACCEPT, getComment(), result, task, pageBase);
    }

    protected String getComment() {
        return null;
    }
}
