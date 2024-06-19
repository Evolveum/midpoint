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
import com.evolveum.midpoint.web.page.admin.certification.CertMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;

import org.apache.wicket.ajax.AjaxRequestTarget;

public abstract class AbstractCertItemAction extends AbstractGuiAction<AccessCertificationWorkItemType> {

    private static final String DOT_CLASS = CertItemResolveAction.class.getName() + ".";
    private static final String OPERATION_RECORD_ACTION = DOT_CLASS + "recordCertItemAction";

    public AbstractCertItemAction() {
        super();
    }

    @Override
    public void onActionPerformed(AccessCertificationWorkItemType workItem, PageBase pageBase, AjaxRequestTarget target) {
        AccessCertificationResponseType response = getResponse();
        OperationResult result = new OperationResult(OPERATION_RECORD_ACTION + "." + response.value());
        Task task = pageBase.createSimpleTask(OPERATION_RECORD_ACTION + "." + response.value());
        CertMiscUtil.recordCertItemResponse(workItem, getResponse(), getComment(), result, task, pageBase);
        target.add(pageBase);
    }

    protected String getComment() {
        return null;
    }

    protected abstract AccessCertificationResponseType getResponse();
}
