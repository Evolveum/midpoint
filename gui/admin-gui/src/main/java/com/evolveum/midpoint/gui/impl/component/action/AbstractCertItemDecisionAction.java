/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.action;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CertMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiActionType;

import org.apache.wicket.ajax.AjaxRequestTarget;

import java.util.List;
import java.util.Map;

public abstract class AbstractCertItemDecisionAction extends AbstractGuiAction<AccessCertificationWorkItemType> {

    private static final String DOT_CLASS = CertItemResolveAction.class.getName() + ".";
    private static final String OPERATION_RECORD_ACTION = DOT_CLASS + "recordCertItemAction";

    public AbstractCertItemDecisionAction() {
        super();
    }

    public AbstractCertItemDecisionAction(GuiActionType actionDto) {
        super(actionDto);
    }

    @Override
    protected void executeAction(List<AccessCertificationWorkItemType> workItems, PageBase pageBase, AjaxRequestTarget target) {
        AccessCertificationResponseType response = getResponse();
        OperationResult result = new OperationResult(OPERATION_RECORD_ACTION + "." + response.value());
        Task task = pageBase.createSimpleTask(OPERATION_RECORD_ACTION + "." + response.value());

        //TODO comment
        workItems.forEach(workItem -> {
            OperationResult oneActionResult = result
                    .subresult(result.getOperation() + ".workItemId:" + workItem.getId())
                            .build();
            CertMiscUtil.recordCertItemResponse(workItem, getResponse(), getComment(workItem), oneActionResult, task, pageBase);
        });
        result.computeStatus();
        target.add(pageBase);
    }

    //TODO this is not entirelly correct. it would probably make more sense to collect deltas and sent it as is to certification manager
    //however, it might require rewriting of certification manager
    private String getComment(AccessCertificationWorkItemType workItem) {
        AbstractWorkItemOutputType output = workItem.getOutput();
        if (output == null) {
            return null;
        }
        return output.getComment();
    }

    protected abstract AccessCertificationResponseType getResponse();
}
