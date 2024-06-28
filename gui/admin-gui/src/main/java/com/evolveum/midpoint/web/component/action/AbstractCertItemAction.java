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

import java.util.List;
import java.util.Map;

public abstract class AbstractCertItemAction extends AbstractGuiAction<AccessCertificationWorkItemType> {

    private static final String DOT_CLASS = CertItemResolveAction.class.getName() + ".";
    private static final String OPERATION_RECORD_ACTION = DOT_CLASS + "recordCertItemAction";

    String comment;

    public AbstractCertItemAction() {
        super();
    }

    public AbstractCertItemAction(AbstractGuiAction<AccessCertificationWorkItemType> preAction) {
        super(preAction);
    }

    @Override
    protected void executeAction(List<AccessCertificationWorkItemType> workItems, PageBase pageBase, AjaxRequestTarget target) {
        AccessCertificationResponseType response = getResponse();
        OperationResult result = new OperationResult(OPERATION_RECORD_ACTION + "." + response.value());
        Task task = pageBase.createSimpleTask(OPERATION_RECORD_ACTION + "." + response.value());

        workItems.forEach(workItem -> {
            OperationResult oneActionResult = result
                    .subresult(result.getOperation() + ".workItemId:" + workItem.getId())
                            .build();
            CertMiscUtil.recordCertItemResponse(workItem, getResponse(), getComment(), oneActionResult, task, pageBase);
        });
        result.computeStatus();
        target.add(pageBase);
    }

    protected String getComment() {
        return comment;
    }

    protected abstract AccessCertificationResponseType getResponse();

    @Override
    protected void processPreActionParametersValues(Map<String, Object> preActionParametersMap) {
        if (preActionParametersMap != null && preActionParametersMap.containsKey("comment")) {
            comment = (String) preActionParametersMap.get("comment");
        }
    }
}
